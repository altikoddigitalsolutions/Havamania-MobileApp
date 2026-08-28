package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.havamania.ui.theme.AssistantTone
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate

enum class AssistantRequestState { IDLE, LOADING, SUCCESS, ERROR }

private const val FORECAST_DAYS = 10
private const val CITY_WEATHER_TTL_MS = 30 * 60 * 1000L // 30 minutes

class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val assistantRepository = AiAssistantRepository()
    var currentConversationId: String = java.util.UUID.randomUUID().toString()

    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val repository = WeatherRepository.getInstance(application)
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()

    private val _messages = MutableStateFlow<List<AltikodChatMessage>>(emptyList())
    val messages: StateFlow<List<AltikodChatMessage>> = _messages.asStateFlow()

    private val _requestState = MutableStateFlow(AssistantRequestState.IDLE)
    val requestState: StateFlow<AssistantRequestState> = _requestState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentJob: Job? = null
    var lastRequestId: String? = null

    private val _config = MutableStateFlow<AltikodBotConfig?>(null)
    val config: StateFlow<AltikodBotConfig?> = _config.asStateFlow()

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val _activeTravels = MutableStateFlow<List<TravelPlan>>(emptyList())
    val activeTravels: StateFlow<List<TravelPlan>> = _activeTravels.asStateFlow()

    var contextCity: String? = null
    var heavyContextSentFor: String? = null

    var userAboutMe: String = ""
    var userInterests: Set<String> = emptySet()
    var assistantTone: AssistantTone = AssistantTone.DENGELI
    var language: String = "TR"

    private var weatherJob: Job? = null
    private var fetchJob: Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val newUid = user?.uid ?: "legacy"
        if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "Auth state changed. New UID: $newUid")
        _messages.value = emptyList()
        _weatherData.value = null
        _weatherUiState.value = WeatherUiState.Loading
        currentConversationId = java.util.UUID.randomUUID().toString()
        contextCity = null
        heavyContextSentFor = null

        loadActiveTravels(newUid)
        observeWeatherState(newUid)
    }

    init {
        android.util.Log.i("ASSISTANT_TRACE", "AiChatViewModel init (Assistant mounted)")
        auth.addAuthStateListener(authListener)
        loadConfig()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        weatherJob?.cancel()
        fetchJob?.cancel()
        currentJob?.cancel()
    }

    private fun loadActiveTravels(uid: String = currentUid) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "loadActiveTravels started for $uid")
            try {
                val entities = dao.getUserTravelPlans(uid)
                val today = LocalDate.now()
                val active = entities.map { it.toDomain() }.filter {
                    !it.isArchived && !it.endDate.isBefore(today)
                }
                _activeTravels.value = active
            } catch (e: Exception) {
                android.util.Log.e("ASSISTANT_TRACE", "loadActiveTravels FAILED: ${e.message}", e)
            }
        }
    }

    private fun observeWeatherState(uid: String) {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "observeWeatherState started for $uid")

            val current = repository.currentWeatherState.value
            if (current != null) {
                if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "Initial weather found: ${current.cityName}")
                _weatherData.value = current
                _weatherUiState.value = WeatherUiState.Success(current)
            } else {
                if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "Initial weather NULL. Triggering tryAutoFetch.")
                tryAutoFetch(uid)
            }

            repository.currentWeatherState.collect { data ->
                _weatherData.value = data
                if (data != null) {
                    _weatherUiState.value = WeatherUiState.Success(data)
                }
            }
        }
    }

    private fun tryAutoFetch(uid: String) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "tryAutoFetch started for $uid")
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val defaultCity = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    ThemeManager.getDefaultCity(getApplication(), uid).firstOrNull()
                }

                if (defaultCity != null) {
                    kotlinx.coroutines.withTimeout(10000) {
                        repository.getWeatherData(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
                            .collect {
                                _weatherUiState.value = WeatherUiState.Success(it)
                            }
                    }
                } else {
                    _weatherUiState.value = WeatherUiState.NoCity
                }
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.message ?: "Hava verisi alınamadı")
            }
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val cfg = assistantRepository.getBotConfig()
            if (cfg != null) {
                _config.value = cfg
            }
        }
    }

    private data class CityWeatherEntry(val data: WeatherData, val fetchedAt: Long)
    private val cityWeatherCache = mutableMapOf<String, CityWeatherEntry>()

    private suspend fun resolveCityWeather(city: String): WeatherData? {
        val entry = cityWeatherCache[city]
        if (entry != null && System.currentTimeMillis() - entry.fetchedAt < CITY_WEATHER_TTL_MS) {
            return entry.data
        }

        val geo = repository.searchCity(city)
        if (geo.isEmpty()) return null

        val data = repository.fetchWeatherSnapshot(geo[0].latitude, geo[0].longitude, geo[0].name, geo[0].district)
        if (data != null) {
            cityWeatherCache[city] = CityWeatherEntry(data, System.currentTimeMillis())
        }
        return data
    }

    private fun forecastBlock(data: WeatherData, label: String): String {
        val daily = data.dailyForecast.take(3).joinToString("\n") {
            "- ${it.date}: ${it.minTemp}°/${it.maxTemp}°"
        }
        return "\n--- $label 3 Günlük Tahmin ---\n$daily\n"
    }

    private fun cityDataBlock(cityName: String): String? {
        val info = TravelAiHelper.getCityDescription(cityName)
        return if (info.contains("Keşfedilmeyi bekleyen")) null
               else "\n--- $cityName Hakkında ---\n$info\n"
    }

    private fun buildFollowUpContext(cityName: String?): String {
        val sb = StringBuilder()
        val trips = _activeTravels.value
        if (trips.isNotEmpty()) {
            sb.append("\n[Kullanıcının Yaklaşan Seyahatleri]\n")
            trips.forEach { t ->
                sb.append("- ${t.city} (${t.startDate} - ${t.endDate})")
                t.weatherSummary?.let { sb.append(" | Hava: $it") }
                sb.append("\n")
            }
        }

        cityName?.let { city ->
            val info = TravelAiHelper.getCityDescription(city)
            if (!info.contains("Keşfedilmeyi bekleyen")) {
                sb.append("\n[Destinasyon Bilgisi: $city]\n$info\n")
            }
        }

        return sb.toString()
    }

    private suspend fun buildWeatherContext(userPrompt: String, cityName: String?): String {
        val sb = StringBuilder()
        sb.append("[Sistem Zamanı: ${java.time.LocalDateTime.now()}]\n")

        val targetCity = cityName ?: contextCity
        if (targetCity != null) {
            val data = resolveCityWeather(targetCity)
            if (data != null) {
                sb.append("\n[Hava Durumu: ${data.cityName}]\n")
                sb.append("Şu an: ${data.temperature}, ${data.condition}, Nem %${data.humidity}, Rüzgar ${data.windSpeed} km/s\n")

                if (userPrompt.contains("tahmin", true) || userPrompt.contains("yarın", true) || userPrompt.contains("hafta", true)) {
                    sb.append(forecastBlock(data, targetCity))
                }
                contextCity = targetCity
            }
        } else {
            _weatherData.value?.let { data ->
                sb.append("\n[Hava Durumu: ${data.cityName} (Mevcut Konum)]\n")
                sb.append("Şu an: ${data.temperature}, ${data.condition}\n")
                if (userPrompt.contains("tahmin", true)) {
                    sb.append(forecastBlock(data, data.cityName))
                }
            }
        }

        return sb.toString()
    }

    private fun buildToneInstruction(tone: AssistantTone): String {
        return when (tone) {
            AssistantTone.SAMIMI -> "Samimi, sıcak, neşeli ve arkadaş canlısı bir üslupla cevap ver. 'canım', 'tatlım' gibi ifadeler kullanabilirsin."
            AssistantTone.RESMI -> "Ciddi, profesyonel, mesafeli ve bilgilendirici bir üslup kullan. Gereksiz samimiyetten kaçın."
            AssistantTone.KISA_NET -> "Çok kısa, öz ve sadece istenen bilgiyi veren cevaplar üret. Cümleleri minimumda tut."
            AssistantTone.DETAYLI_UZMAN -> "Meteorolojik terimler içeren, teknik detaylara giren, neden-sonuç ilişkisi kuran uzman bir dille cevap ver."
            else -> "Dengeli, yardımcı ve nazik bir üslupla cevap ver."
        }
    }

    fun sendMessage(userPrompt: String, systemContext: String? = null, isRetry: Boolean = false) {
        if (isSending.value || userPrompt.isBlank()) return

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _isSending.value = true
            _requestState.value = AssistantRequestState.LOADING
            _isLoading.value = true

            val userMsg = AltikodChatMessage(text = userPrompt, isUser = true)
            _messages.value = _messages.value + userMsg

            try {
                val weatherCtx = buildWeatherContext(userPrompt, null)
                val followUpCtx = buildFollowUpContext(null)
                val toneInst = buildToneInstruction(assistantTone)

                val finalPrompt = buildString {
                    append("SORU: ")
                    append(userPrompt)
                    append("\n\n---")
                    append("\n[BAĞLAM]")
                    append("\nDil: $language")
                    append("\nÜslup: $toneInst")
                    if (weatherCtx.isNotBlank()) append("\n$weatherCtx")
                    if (followUpCtx.isNotBlank()) append("\n$followUpCtx")
                    if (!systemContext.isNullOrBlank()) append("\n$systemContext")
                    append("\n---")
                }

                val result = assistantRepository.getAssistantResponse(finalPrompt, currentConversationId)

                when (result) {
                    is AssistantResult.Success -> {
                        val botMsg = AltikodChatMessage(text = result.content, isUser = false)
                        _messages.value = _messages.value + botMsg
                        _requestState.value = AssistantRequestState.SUCCESS
                    }
                    is AssistantResult.QuestionRejected -> {
                        val greetings = listOf("merhaba", "selam", "hi", "hello", "hey", "günaydın", "iyi günler")
                        val isGreeting = greetings.any { userPrompt.lowercase().contains(it) }

                        if (isGreeting) {
                            val botMsg = AltikodChatMessage(
                                text = "Merhaba! Hava durumu, seyahat planların veya güzergâh koşulları hakkında bana soru sorabilirsin. Sana nasıl yardımcı olabilirim?",
                                isUser = false
                            )
                            _messages.value = _messages.value + botMsg
                            _requestState.value = AssistantRequestState.SUCCESS
                        } else {
                            handleError(Exception("Lütfen geçerli bir soru sorunuz."), userPrompt)
                        }
                    }
                    is AssistantResult.HttpError -> {
                        if (result.code == 429) {
                            handleError(Exception("Çok fazla istek gönderildi."), userPrompt)
                        } else {
                            handleError(Exception("Asistan sunucusuna ulaşılamadı (Hata: ${result.code})."), userPrompt)
                        }
                    }
                    is AssistantResult.NetworkError -> {
                        handleError(Exception("İnternet bağlantısı sorunu."), userPrompt)
                    }
                    else -> {
                        handleError(Exception("Asistan şu anda yanıt veremiyor."), userPrompt)
                    }
                }
            } catch (e: Exception) {
                Log.e("ASSISTANT_DEBUG", "SendMessage FATAL EXCEPTION: ${e.message}", e)
                handleError(e, userPrompt)
            } finally {
                _isLoading.value = false
                _isSending.value = false
            }
        }
    }

    private fun handleError(e: Exception, userPrompt: String) {
        val errorText = e.message ?: "Bilinmeyen bir hata oluştu."
        _requestState.value = AssistantRequestState.ERROR
        addErrorMessage(errorText, userPrompt)
    }

    private fun addErrorMessage(text: String, retryPrompt: String) {
        val errorMsg = AltikodChatMessage(
            text = text,
            isUser = false,
            isFallback = true,
            retryPrompt = retryPrompt
        )
        _messages.value = _messages.value + errorMsg
    }

    fun resetChat() {
        _messages.value = emptyList()
        currentConversationId = java.util.UUID.randomUUID().toString()
        _requestState.value = AssistantRequestState.IDLE
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val history = dao.getAiHistoryItem(conversationId)
                if (history != null) {
                    currentConversationId = conversationId
                    _messages.value = history.messages
                    _requestState.value = AssistantRequestState.SUCCESS
                }
            } catch (e: Exception) {
                Log.e("AiChatVM", "Failed to load conversation $conversationId", e)
            }
        }
    }
}
