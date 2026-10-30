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
    internal val assistantConsent = AssistantConsentController(viewModelScope, { currentUid })
    private data class PendingConsentMessage(val prompt: String, val context: String?,
        val retry: Boolean, val requestId: String?, val conversation: String, val owner: String)
    private var pendingConsentMessage: PendingConsentMessage? = null

    fun revokeAssistantConsent() { pendingConsentMessage = null; requestRunner.reset(); assistantConsent.revoke() }
    fun retryAssistantConsent() {
        if (assistantConsent.state.value.withdrawalPending || assistantConsent.state.value.retry == ConsentRetry.REVOKE) {
            revokeAssistantConsent()
        } else assistantConsent.retry()
    }
    fun dismissAssistantConsent() { pendingConsentMessage = null; assistantConsent.dismiss() }
    fun resumeAfterConsent() {
        if (!assistantConsent.state.value.allowed || assistantConsent.state.value.visible) return
        val pending = pendingConsentMessage ?: return
        pendingConsentMessage = null
        if (pending.owner == currentUid && pending.conversation == currentConversationId) {
            sendMessage(pending.prompt, pending.context, pending.retry, pending.requestId)
        }
    }

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
    }, timeoutMillis = 75_000)
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
        assistantConsent.reset()
        pendingConsentMessage = null
        if (newUid != "legacy") assistantConsent.load()
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
        assistantConsent.close()
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

    fun sendMessage(userPrompt: String, systemContext: String? = null, isRetry: Boolean = false, retryRequestId: String? = null) {
        if (userPrompt.isBlank()) return
        if (!isLocalAssistantPrompt(userPrompt) && !assistantConsent.state.value.allowed) {
            pendingConsentMessage = PendingConsentMessage(userPrompt, systemContext, isRetry,
                retryRequestId, currentConversationId, currentUid)
            assistantConsent.load(showLoading = true)
            return
        }
        if (isRetry && _messages.value.any { it.retryRequestId == retryRequestId &&
                (it.retryNotBefore ?: 0L) > System.currentTimeMillis() }) return
        val userMessage = assistantMessageForSend(_messages.value, userPrompt, isRetry, retryRequestId)
            ?: return
        val conversationId = currentConversationId
        var platformReply: AssistantResult.Success? = null
        requestRunner.start(
            onStart = {
                _requestState.value = AssistantRequestState.LOADING
                _messages.value = _messages.value.filterNot {
                    it.isFallback && it.retryRequestId == userMessage.id
                } + if (isRetry) emptyList() else listOf(userMessage)
            },
            operation = { owner ->
                val greetings = listOf("merhaba", "selam", "hi", "hello", "hey", "günaydın", "iyi günler")
                val greeting = "Merhaba! Hava durumu, seyahat planların veya güzergâh koşulları hakkında bana soru sorabilirsin. Sana nasıl yardımcı olabilirim?"
                when {
                    greetings.any { userPrompt.lowercase(java.util.Locale.forLanguageTag("tr")).trim() == it } -> greeting
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
                        val personalized = ThemeManager.getPersonalizationEnabled(getApplication(), owner).first()
                        val interests = if (personalized) ThemeManager.getUserInterests(getApplication(), owner)
                            .first().sorted().take(20).map { it.take(50) } else emptyList()
                        ensureOwner(owner)
                        val context = assistantClientContext(AiIntentParser.detectCity(userPrompt) ?: contextCity,
                            _weatherData.value, _activeTravels.value, personalized)
                        when (val result = assistantRepository.getAssistantResponse(userPrompt, conversationId, userMessage.id,
                            assistantConsent.state.value.document ?: throw AssistantConsentRequiredException(),
                            assistantTone.platformTone(), personalized, interests, context)) {
                            is AssistantResult.ConsentRequired -> throw AssistantConsentRequiredException()
                            is AssistantResult.RequestInProgress -> throw Exception(
                                "İsteğiniz hâlâ işleniyor. Biraz sonra aynı isteği tekrar kontrol edebilirsiniz.")
                            is AssistantResult.AccountDeleted -> throw TerminalAssistantException(
                                "Bu hesap için asistan erişimi silme nedeniyle kapalı.")
                            is AssistantResult.RequestConflict -> throw TerminalAssistantException(
                                "İstek içeriği veya kimliği doğrulanamadı. Yeni bir mesaj gönderebilirsiniz.")
                            is AssistantResult.AccountDeletionPending -> throw TerminalAssistantException(
                                "Hesap silme işlemi devam ediyor. Bu hesapla asistana yeni mesaj gönderilemez.")
                            is AssistantResult.DailyLimit -> throw TerminalAssistantException(
                                "Günlük asistan hakkınız doldu. Limit yenilendiğinde tekrar kullanabilirsiniz.")
                            is AssistantResult.RateLimited -> throw RateLimitedAssistantException(result.retryAfterSeconds)
                            is AssistantResult.Unavailable -> throw Exception(
                                "Asistan şu anda hizmet veremiyor. Lütfen daha sonra tekrar deneyin.")
                            is AssistantResult.Timeout -> throw Exception(
                                "Asistan zamanında cevap veremedi. İsteği tekrar kontrol edebilirsiniz.")
                            is AssistantResult.DuplicateRequest -> throw TerminalAssistantException(
                                "Bu istek sunucuya daha önce ulaştı. Yeniden işlenmedi. Önceki yanıt şu anda alınamıyor.")
                            is AssistantResult.RequestExpired -> throw TerminalAssistantException(
                                "İstek süresi doldu. İsterseniz yeni bir mesaj gönderebilirsiniz.")
                            is AssistantResult.ConversationConflict -> throw TerminalAssistantException(
                                "Bu sohbet kullanılamıyor. Lütfen yeni bir sohbet başlatın.")
                            is AssistantResult.ModeDisabled -> throw TerminalAssistantException("Bu asistan modu şu anda kullanılamıyor.")
                            is AssistantResult.TooLong -> throw TerminalAssistantException("Mesaj veya bağlam çok uzun. Lütfen kısaltın.")
                            is AssistantResult.Success -> { platformReply = result; result.content }
                            is AssistantResult.QuestionRejected -> {
                                if (greetings.any { userPrompt.lowercase(java.util.Locale.forLanguageTag("tr")).contains(it) }) greeting
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
                _messages.value = _messages.value + AltikodChatMessage(text = reply, isUser = false,
                    platformMessageId = platformReply?.messageId, disclaimer = platformReply?.disclaimer)
                _requestState.value = AssistantRequestState.SUCCESS
            },
            onError = {
                if (it is AssistantConsentRequiredException) {
                    pendingConsentMessage = PendingConsentMessage(userPrompt, systemContext, true,
                        userMessage.id, conversationId, currentUid)
                    _requestState.value = AssistantRequestState.IDLE
                    assistantConsent.open()
                } else handleError(it, userPrompt, userMessage.id)
            }
        )
    }

    private fun handleError(e: Exception, userPrompt: String, requestId: String) {
        val errorText = e.message ?: "Bilinmeyen bir hata oluştu."
        _requestState.value = AssistantRequestState.ERROR
        val retryNotBefore = (e as? RateLimitedAssistantException)?.seconds?.let {
            System.currentTimeMillis() + it * 1000
        }
        addErrorMessage(errorText, userPrompt, requestId.takeUnless { e is TerminalAssistantException }, retryNotBefore)
    }

    private fun addErrorMessage(text: String, retryPrompt: String, requestId: String?, retryNotBefore: Long?) {
        val errorMsg = AltikodChatMessage(
            text = text,
            isUser = false,
            isFallback = true,
            retryPrompt = retryPrompt.takeIf { requestId != null },
            retryRequestId = requestId,
            retryNotBefore = retryNotBefore
        )
        _messages.value = _messages.value + errorMsg
    }

    fun resetChat() {
        pendingConsentMessage = null
        requestRunner.reset()
        _messages.value = emptyList()
        currentConversationId = java.util.UUID.randomUUID().toString()
        _requestState.value = AssistantRequestState.IDLE
    }

    fun loadConversation(conversationId: String) {
        pendingConsentMessage = null
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
