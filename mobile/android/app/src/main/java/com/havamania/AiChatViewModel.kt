package com.havamania

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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


class AiChatViewModel(application: Application) : AndroidViewModel(application) {
    private val assistantRepository = AiAssistantRepository()
    var currentConversationId: String = java.util.UUID.randomUUID().toString()

    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    internal val assistantConsent = AssistantConsentController(viewModelScope, { currentUid })
    private val pendingMessages = AssistantPendingMessages()
    var tripQuestionError by mutableStateOf<String?>(null)
        private set

    fun revokeAssistantConsent() {
        tripQuestionError = null
        pendingMessages.clear()
        clearPhotos()
        if (loadingHistoryId == null) {
            requestRunner.reset()
            if (_requestState.value == AssistantRequestState.LOADING) {
                _requestState.value = AssistantRequestState.IDLE
            }
        }
        assistantConsent.revoke()
    }
    fun retryAssistantConsent() {
        if (assistantConsent.state.value.withdrawalPending || assistantConsent.state.value.retry == ConsentRetry.REVOKE) {
            revokeAssistantConsent()
        } else assistantConsent.retry()
    }
    fun dismissAssistantConsent() { tripQuestionError = null; pendingMessages.clear(); assistantConsent.dismiss() }
    fun dismissTripQuestionError() { tripQuestionError = null }
    fun resumeAfterConsent() {
        if (_isSending.value || !assistantConsent.state.value.allowed || assistantConsent.state.value.visible) return
        val pending = pendingMessages.take(currentUid, currentConversationId) ?: return
        if (pending.owner == currentUid && pending.conversation == currentConversationId) {
            if (pending.tripId != null && !assistantTripQuestionStillCurrent(
                    _activeTravels.value, currentUid, pending.tripId, pending.prompt, LocalDate.now())) {
                tripQuestionError = ASSISTANT_TRIP_CHANGED_MESSAGE
                return
            }
            sendMessage(pending.prompt, pending.context, pending.retry, pending.requestId, pending.fromDraft,
                tripId = pending.tripId)
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

    private var loadingHistoryId: String? = null
    private val requestRunner = AssistantTaskRunner(viewModelScope, { currentUid }, { busy ->
        if (!busy) loadingHistoryId = null
        _isLoading.value = busy
        _isSending.value = busy
    }, timeoutMillis = 210_000)
    var lastRequestId: String? = null

    private val _config = MutableStateFlow<AltikodBotConfig?>(null)
    val config: StateFlow<AltikodBotConfig?> = _config.asStateFlow()

    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    private val travelDate = MutableStateFlow(LocalDate.now())
    fun refreshTravelDate() { travelDate.value = LocalDate.now() }

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

    var selectedPhotos by mutableStateOf<List<AssistantPhoto>>(emptyList())
        private set
    var preparingPhotos by mutableStateOf(false)
        private set
    var photoError by mutableStateOf<String?>(null)
        private set
    var photosEnabled by mutableStateOf(true)
        private set
    private var photoJob: Job? = null
    private var photoGeneration = 0L
    var messagePhotos by mutableStateOf<Map<String, List<AssistantPhoto>>>(emptyMap())
        private set
    private val pendingPhotos = mutableMapOf<String, List<AssistantPhoto>>()

    private fun clearPhotos() {
        photoGeneration++
        photoJob?.cancel()
        photoJob = null
        selectedPhotos = emptyList()
        pendingPhotos.clear()
        messagePhotos = emptyMap()
        preparingPhotos = false
        photoError = null
    }

    fun photoPickerUnavailable() {
        photoError = "Fotoğraf seçici açılamadı. Lütfen daha sonra tekrar deneyin."
    }

    fun photoSelectionToken(): String = "$currentUid:$photoGeneration"

    fun addPhotos(uris: List<android.net.Uri>, selectionToken: String = photoSelectionToken()) {
        if (selectionToken != photoSelectionToken()) return
        if (_isSending.value || preparingPhotos || !photosEnabled || assistantConsent.state.value.visible) return
        val available = ASSISTANT_MAX_PHOTOS - selectedPhotos.size
        if (available <= 0) { photoError = "Bir mesaja en fazla 4 fotoğraf ekleyebilirsin."; return }
        if (uris.isEmpty()) return
        val owner = currentUid
        val epoch = photoGeneration
        preparingPhotos = true
        photoError = null
        photoJob = viewModelScope.launch {
            try {
                val prepared = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    uris.take(available).map { uri ->
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        prepareAssistantPhoto(getApplication<Application>().contentResolver, uri)
                    }
                }
                if (owner == currentUid && epoch == photoGeneration) {
                    selectedPhotos = selectedPhotos + prepared
                    if (uris.size > available) photoError = "Bir mesaja en fazla 4 fotoğraf ekleyebilirsin."
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: OutOfMemoryError) {
                if (owner == currentUid && epoch == photoGeneration) photoError = "Fotoğraf çok büyük. Daha küçük bir fotoğraf seçin."
            } catch (_: Exception) {
                if (owner == currentUid && epoch == photoGeneration) photoError = "Fotoğraf hazırlanamadı. Daha küçük bir JPEG, PNG veya WebP seçin."
            } finally {
                if (epoch == photoGeneration) preparingPhotos = false
            }
        }
    }

    fun removePhoto(id: String) {
        if (!_isSending.value && !preparingPhotos && !assistantConsent.state.value.visible) {
            selectedPhotos = selectedPhotos.filterNot { it.id == id }
            photoError = null
        }
    }

    var draft by mutableStateOf("")
        private set

    fun updateDraft(text: String) {
        pendingMessages.onDraftChanged(draft, text)
        draft = text
    }

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val newUid = user?.uid ?: "legacy"
        accountTasks.reset()
        assistantConsent.reset()
        pendingMessages.clear()
        if (newUid != "legacy") assistantConsent.load()
        requestRunner.reset()
        fetchJob?.cancel()
        _isLoading.value = false
        _isSending.value = false
        _requestState.value = AssistantRequestState.IDLE
        draft = ""
        clearPhotos()
        photosEnabled = true
        _activeTravels.value = emptyList()
        tripQuestionError = null
        userAboutMe = ""
        userInterests = emptySet()
        _messages.value = emptyList()
        _weatherData.value = null
        _weatherUiState.value = WeatherUiState.Loading
        currentConversationId = java.util.UUID.randomUUID().toString()
        contextCity = null
        heavyContextSentFor = null

        loadActiveTravels()
        observeWeatherState()
    }

    init {
        auth.addAuthStateListener(authListener)
        loadConfig()
    }

    override fun onCleared() {
        clearPhotos()
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

    private fun loadActiveTravels() {
        accountTasks.launch { owner ->
            try {
                observeAssistantTravels(
                    dao.getAllTravelPlansFlow(owner).map { rows -> rows.map { it.toDomain() } },
                    travelDate, owner
                ).collect { active ->
                    ensureOwner(owner)
                    _activeTravels.value = active
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                ensureOwner(owner)
                _activeTravels.value = emptyList()
                if (BuildConfig.DEBUG) Log.e("ASSISTANT_TRACE", "Could not load active trips")
            }
        }
    }

    private fun observeWeatherState() {
        weatherJob?.cancel()
        weatherJob = accountTasks.launch { owner ->
            val current = repository.currentWeatherState.value
            if (current != null) {
                _weatherData.value = current
                _weatherUiState.value = WeatherUiState.Success(current)
            } else {
                tryAutoFetch()
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

    private fun tryAutoFetch() {
        fetchJob?.cancel()
        fetchJob = accountTasks.launch { owner ->
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

    fun sendDraft(prompt: String) {
        if (prompt == draft) sendMessage(if (prompt.isBlank() && selectedPhotos.isNotEmpty()) "Bu fotoğrafı yorumlar mısın?" else prompt, fromDraft = true)
    }

    fun askTripWeather(id: String): Boolean {
        val question = assistantTripWeatherQuestion(_activeTravels.value, currentUid, id, LocalDate.now())
            ?: return false
        if (assistantConsent.state.value.visible || pendingMessages.hasPending) return false
        return sendMessage(question, tripId = id)
    }

    fun sendRecommendation(prompt: String, context: String?): Boolean {
        if (pendingMessages.hasPending || assistantConsent.state.value.visible) return false
        return sendMessage(prompt, systemContext = context)
    }

    fun sendMessage(userPrompt: String, systemContext: String? = null, isRetry: Boolean = false,
                    retryRequestId: String? = null, fromDraft: Boolean = false, tripId: String? = null): Boolean {
        if (_isSending.value || preparingPhotos || userPrompt.isBlank() || assistantQuestionTooLong(userPrompt)) return false
        val retryMessage = _messages.value.firstOrNull { it.isUser && it.id == retryRequestId }
        val selectedTripId = tripId ?: retryMessage?.selectedTripId?.takeIf { isRetry }
        if (selectedTripId != null && !assistantTripQuestionStillCurrent(
                _activeTravels.value, currentUid, selectedTripId, userPrompt, LocalDate.now())) {
            tripQuestionError = ASSISTANT_TRIP_CHANGED_MESSAGE
            return false
        }
        if (isRetry && _messages.value.any { it.retryRequestId == retryRequestId &&
                (it.retryNotBefore ?: 0L) > System.currentTimeMillis() }) return false
        val originalMessage = assistantMessageForSend(_messages.value, userPrompt, isRetry, retryRequestId)
            ?: return false
        val intentVersion = pendingMessages.beginAcceptedSend()
        tripQuestionError = null
        val hasPhotos = if (isRetry) (retryMessage?.photoCount ?: 0) > 0 else fromDraft && selectedPhotos.isNotEmpty()
        if ((!isLocalAssistantPrompt(userPrompt) || hasPhotos) && !assistantConsent.state.value.allowed) {
            pendingMessages.queue(PendingAssistantMessage(userPrompt, systemContext, isRetry,
                retryRequestId, currentConversationId, currentUid, fromDraft, selectedTripId), intentVersion)
            assistantConsent.load(showLoading = true)
            return true
        }
        val userMessage = originalMessage.copy(
            photoCount = if (!isRetry && hasPhotos) selectedPhotos.size else originalMessage.photoCount,
            selectedTripId = selectedTripId)
        if (!isRetry && hasPhotos) {
            pendingPhotos[userMessage.id] = selectedPhotos.toList()
            messagePhotos = retainAssistantPreviews(messagePhotos, userMessage.id, selectedPhotos)
        }
        val conversationId = currentConversationId
        var platformReply: AssistantResult.Success? = null
        return requestRunner.start(
            onStart = {
                if (fromDraft) { draft = ""; selectedPhotos = emptyList() }
                _requestState.value = AssistantRequestState.LOADING
                _messages.value = _messages.value.filterNot {
                    it.isFallback && it.retryRequestId == userMessage.id
                } + if (isRetry) emptyList() else listOf(userMessage)
            },
            operation = { owner ->
                assistantPromptReply(userPrompt, hasAttachments = hasPhotos) {
                        val attachmentIds = uploadAssistantPhotoBatch(userMessage.photoCount,
                            (_messages.value.firstOrNull { it.id == userMessage.id } ?: userMessage).attachmentIds,
                            pendingPhotos[userMessage.id].orEmpty(),
                            upload = { photo ->
                                ensureOwner(owner)
                                val id = assistantRepository.uploadPhoto(photo.bytes, owner)
                                ensureOwner(owner)
                                id
                            },
                            onUploaded = { photo, ids ->
                                _messages.value = _messages.value.map { if (it.id == userMessage.id) it.copy(attachmentIds = ids) else it }
                                pendingPhotos[userMessage.id] = pendingPhotos[userMessage.id].orEmpty().filterNot { it.id == photo.id }
                            })
                        pendingPhotos.remove(userMessage.id)

                        val preferences = ThemeManager.getPersonalizationSnapshot(getApplication<Application>(), owner).first()
                        val personalized = preferences.personalizationEnabled
                        val interests = preferences.selectedInterests.sorted().take(20).map { it.take(50) }
                        ensureOwner(owner)
                        val context = assistantClientContext(contextCity,
                            _weatherData.value, _activeTravels.value, personalized, owner = owner)
                        when (val result = assistantRepository.getAssistantResponse(userPrompt, conversationId, userMessage.id,
                            assistantConsent.state.value.document ?: throw AssistantConsentRequiredException(),
                            assistantTone.platformTone(), personalized, interests, context, attachmentIds)) {
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
                            is AssistantResult.QuestionRejected -> throw Exception("Lütfen geçerli bir soru sorunuz.")
                            is AssistantResult.HttpError -> if (hasPhotos && result.code == 400) throw TerminalAssistantException(
                                "Fotoğrafın süresi dolmuş veya fotoğraf kullanılamıyor. Yeni bir mesaja tekrar ekleyin.") else throw Exception(
                                if (result.code == 429) "Çok fazla istek gönderildi."
                                else "Asistan sunucusuna ulaşılamadı (Hata: ${result.code}).")
                            is AssistantResult.NetworkError -> throw Exception("İnternet bağlantısı sorunu.")
                            else -> throw Exception("Asistan şu anda yanıt veremiyor.")
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
                    pendingMessages.queue(PendingAssistantMessage(userPrompt, systemContext, true,
                        userMessage.id, conversationId, currentUid, tripId = selectedTripId), intentVersion)
                    _requestState.value = AssistantRequestState.IDLE
                    assistantConsent.open()
                } else handleError(it, userPrompt, userMessage.id)
            }
        )
    }

    private fun handleError(e: Exception, userPrompt: String, requestId: String) {
        if (e is PhotosDisabledException) { photosEnabled = false; clearPhotos() }
        if (e is TerminalAssistantException) pendingPhotos.remove(requestId)
        val errorText = e.message ?: "Bilinmeyen bir hata oluştu."
        _requestState.value = AssistantRequestState.ERROR
        val retryNotBefore = (e as? RateLimitedAssistantException)?.seconds?.let {
            System.currentTimeMillis() + it * 1000
        }
        addErrorMessage(errorText, userPrompt, requestId.takeUnless { e is TerminalAssistantException || e is PhotosDisabledException }, retryNotBefore)
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
        tripQuestionError = null
        clearPhotos()
        draft = ""
        pendingMessages.clear()
        requestRunner.reset()
        _messages.value = emptyList()
        currentConversationId = java.util.UUID.randomUUID().toString()
        _requestState.value = AssistantRequestState.IDLE
    }

    fun loadConversation(conversationId: String) {
        // The route effect runs again on recreation, while this conversation stays in the ViewModel.
        if (currentConversationId == conversationId || loadingHistoryId == conversationId) return
        tripQuestionError = null
        pendingMessages.clear()
        clearPhotos()
        requestRunner.reset()
        requestRunner.start(
            onStart = {
                loadingHistoryId = conversationId
                _requestState.value = AssistantRequestState.LOADING
            },
            operation = { owner ->
                readVisibleHistoryDetail(isDeleted = { dao.hasHistoryDeletion(owner, conversationId) }) {
                    dao.getAiHistoryItem(conversationId, owner)
                }
            },
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
