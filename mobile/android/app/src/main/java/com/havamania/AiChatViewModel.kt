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
import kotlinx.coroutines.ensureActive
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
    private val accountTasks = AccountTaskScope(viewModelScope) { currentUid }

    private val _messages = MutableStateFlow<List<AltikodChatMessage>>(emptyList())
    val messages: StateFlow<List<AltikodChatMessage>> = _messages.asStateFlow()

    private val _requestState = MutableStateFlow(AssistantRequestState.IDLE)
    val requestState: StateFlow<AssistantRequestState> = _requestState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val requestRunner = AssistantTaskRunner(viewModelScope, { currentUid }, { busy ->
        _isLoading.value = busy
        _isSending.value = busy
    })
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
        accountTasks.reset()
        requestRunner.reset()
        fetchJob?.cancel()
        _isLoading.value = false
        _isSending.value = false
        _requestState.value = AssistantRequestState.IDLE
        _activeTravels.value = emptyList()
        userAboutMe = ""
        userInterests = emptySet()
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
if (BuildConfig.DEBUG) {
            android.util.Log.i("ASSISTANT_TRACE", "AiChatViewModel init (Assistant mounted)")
}
        auth.addAuthStateListener(authListener)
        loadConfig()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        accountTasks.close()
        weatherJob?.cancel()
        fetchJob?.cancel()
        requestRunner.reset()
    }

    private suspend fun ensureOwner(owner: String) {
        kotlinx.coroutines.currentCoroutineContext().let { context ->
            context[kotlinx.coroutines.Job]?.ensureActive()
        }
        if (owner != currentUid) throw kotlinx.coroutines.CancellationException("Account changed")
    }

    private fun loadActiveTravels(uid: String = currentUid) {
        accountTasks.launch { owner ->
            if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "loadActiveTravels started for $uid")
            try {
                val entities = dao.getUserTravelPlans(owner)
                ensureOwner(owner)
                val today = LocalDate.now()
                val active = entities.map { it.toDomain() }.filter {
                    !it.isArchived && !it.endDate.isBefore(today)
                }
                _activeTravels.value = active
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    android.util.Log.e("ASSISTANT_TRACE", "loadActiveTravels FAILED: ${e.message}", e)
}
            }
        }
    }

    private fun observeWeatherState(uid: String) {
        weatherJob?.cancel()
        weatherJob = accountTasks.launch { owner ->
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
                ensureOwner(owner)
                _weatherData.value = data
                if (data != null) {
                    _weatherUiState.value = WeatherUiState.Success(data)
                }
            }
        }
    }

    private fun tryAutoFetch(uid: String) {
        fetchJob?.cancel()
        fetchJob = accountTasks.launch { owner ->
            if (BuildConfig.DEBUG) android.util.Log.i("ASSISTANT_TRACE", "tryAutoFetch started for $uid")
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val defaultCity = kotlinx.coroutines.withTimeoutOrNull(3000) {
                    ThemeManager.getDefaultCity(getApplication(), owner).firstOrNull()
                }

                ensureOwner(owner)
                if (defaultCity != null) {
                    kotlinx.coroutines.withTimeout(10000) {
                        repository.getWeatherData(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
                            .collect {
                                ensureOwner(owner)
                                _weatherData.value = it
                                _weatherUiState.value = WeatherUiState.Success(it)
                            }
                    }
                } else {
                    _weatherUiState.value = WeatherUiState.NoCity
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                ensureOwner(owner)
                _weatherUiState.value = WeatherUiState.Error("Hava verisi isteği zaman aşımına uğradı.")
            } catch (e: Exception) {
                ensureOwner(owner)
                if (e is kotlinx.coroutines.CancellationException) throw e
                _weatherUiState.value = WeatherUiState.Error("Hava verisi alınamadı.")
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
        val daily = data.dailyForecast.take(3).joinToString(", ") {
            "${it.date}: ${it.minTemp}/${it.maxTemp}C"
        }
        return "Forecast for $label: $daily"
    }

    private fun cityDataBlock(cityName: String): String? {
        val info = TravelAiHelper.getCityDescription(cityName)
        return if (info.contains("Keşfedilmeyi bekleyen")) null
               else "About $cityName: $info"
    }

    private fun buildFollowUpContext(cityName: String?): String {
        val sb = StringBuilder()
        val trips = _activeTravels.value
        if (trips.isNotEmpty()) {
            sb.append("User's upcoming trips:\n")
            trips.forEach { t ->
                sb.append("- ${t.city} (${t.startDate} to ${t.endDate})")
                t.weatherSummary?.let { sb.append(". Summary: $it") }
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    private suspend fun buildWeatherContext(userPrompt: String, cityName: String?): String {
        val sb = StringBuilder()
        sb.append("Current time: ${java.time.LocalDateTime.now()}\n")

        val targetCity = cityName ?: contextCity
        if (targetCity != null) {
            val data = resolveCityWeather(targetCity)
            if (data != null) {
                sb.append("Weather in ${data.cityName}: ${data.temperature}C, ${data.condition}\n")
                if (userPrompt.contains("tahmin", true) || userPrompt.contains("yarın", true)) {
                    sb.append(forecastBlock(data, targetCity))
                }
                contextCity = targetCity
            }
        } else {
            _weatherData.value?.let { data ->
                sb.append("Weather at user's location (${data.cityName}): ${data.temperature}C, ${data.condition}\n")
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
        if (userPrompt.isBlank()) return
        val conversationId = currentConversationId
        requestRunner.start(
            onStart = {
                _requestState.value = AssistantRequestState.LOADING
                _messages.value = _messages.value + AltikodChatMessage(text = userPrompt, isUser = true)
            },
            operation = {
                val greetings = listOf("merhaba", "selam", "hi", "hello", "hey", "günaydın", "iyi günler")
                val greeting = "Merhaba! Hava durumu, seyahat planların veya güzergâh koşulları hakkında bana soru sorabilirsin. Sana nasıl yardımcı olabilirim?"
                when {
                    greetings.any { userPrompt.lowercase().trim() == it } -> greeting
                    AiIntentParser.isWeatherQuery(userPrompt) || AiIntentParser.detectDate(userPrompt) != null -> {
                        val targetCity = AiIntentParser.detectCity(userPrompt) ?: _weatherData.value?.cityName
                        if (targetCity == null) {
                            "Hangi şehir hakkında bilgi almak istediğinizi belirtir misiniz?"
                        } else {
                            val data = resolveCityWeather(targetCity)
                            if (data == null) "$targetCity için hava durumu verilerine şu an ulaşılamıyor."
                            else AiWeatherReply.datedReply(userPrompt, data)
                                ?: "${data.cityName} şehrinde hava ${data.condition}, sıcaklık ${data.temperature} (Hissedilen: ${data.feelsLike})."
                        }
                    }
                    else -> {
                        // The API expects the original prompt; avoid unused context/network requests.
                        when (val result = assistantRepository.getAssistantResponse(userPrompt, conversationId)) {
                            is AssistantResult.Success -> result.content
                            is AssistantResult.QuestionRejected -> {
                                if (greetings.any { userPrompt.lowercase().contains(it) }) greeting
                                else throw Exception("Lütfen geçerli bir soru sorunuz.")
                            }
                            is AssistantResult.HttpError -> throw Exception(
                                if (result.code == 429) "Çok fazla istek gönderildi."
                                else "Asistan sunucusuna ulaşılamadı (Hata: ${result.code}).")
                            is AssistantResult.NetworkError -> throw Exception("İnternet bağlantısı sorunu.")
                            else -> throw Exception("Asistan şu anda yanıt veremiyor.")
                        }
                    }
                }
            },
            onSuccess = { reply ->
                _messages.value = _messages.value + AltikodChatMessage(text = reply, isUser = false)
                _requestState.value = AssistantRequestState.SUCCESS
            },
            onError = { handleError(it, userPrompt) }
        )
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
        requestRunner.reset()
        _messages.value = emptyList()
        currentConversationId = java.util.UUID.randomUUID().toString()
        _requestState.value = AssistantRequestState.IDLE
    }

    fun loadConversation(conversationId: String) {
        requestRunner.reset()
        requestRunner.start(
            onStart = { _requestState.value = AssistantRequestState.LOADING },
            operation = { owner -> dao.getAiHistoryItem(conversationId, owner) },
            onSuccess = { history ->
                if (history != null) {
                    currentConversationId = conversationId
                    _messages.value = history.messages
                    _requestState.value = AssistantRequestState.SUCCESS
                } else {
                    _requestState.value = AssistantRequestState.ERROR
                }
            },
            onError = { _requestState.value = AssistantRequestState.ERROR }
        )
    }
}
