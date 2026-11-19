package com.havamania.ui.theme

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.havamania.BuildConfig
import com.havamania.UserProfile
import com.havamania.WeatherRepository
import com.havamania.GeocodingResultDto
import com.havamania.SaveResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ThemeViewModel internal constructor(
    application: Application,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val userProfileRepository: com.havamania.UserProfileRepository,
    private val weatherAccountChanged: (String?) -> Unit,
    private val syncGate: (() -> Boolean)? = null
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance(), com.havamania.UserProfileRepository.getInstance(),
        { WeatherRepository.getInstance(application).onAccountChanged(it) })




    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val _activeUid = MutableStateFlow(auth.currentUser?.uid)
    internal val activeUid = _activeUid.asStateFlow()
    private val accountTasks = com.havamania.AccountTaskScope(viewModelScope) { currentUid }

    private var citySyncRequests = com.havamania.CitySyncRequestGate()
    private fun citySyncAllowed(): Boolean {
        syncGate?.let { return it() }
        val state = com.havamania.AccountDeletionManager.getInstance(getApplication()).state.value
        return !state.pending && !state.busy
    }
    private val cityMutationLock = kotlinx.coroutines.sync.Mutex()
    private val _cityMutationError = MutableStateFlow(false)
    val cityMutationError = _cityMutationError.asStateFlow()
    private val _cityMutationErrorDetails = MutableStateFlow<String?>(null)
    val cityMutationErrorDetails = _cityMutationErrorDetails.asStateFlow()
    private val _cityMutationCanRetry = MutableStateFlow(false)
    val cityMutationCanRetry = _cityMutationCanRetry.asStateFlow()
    private fun reportCityError(error: Exception) {
        _cityMutationCanRetry.value = com.havamania.canManuallyRetryCitySync(error)
        _cityMutationErrorDetails.value = com.havamania.citySyncErrorText(error)
        _cityMutationError.value = true
    }

    @Volatile private var citiesListenerVersion = 0L
    private var citiesListener: ListenerRegistration? = null
    private var citiesSnapshotJob: kotlinx.coroutines.Job? = null

    private val _currentTheme = MutableStateFlow(AppTheme.DARK)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _animationsEnabled = MutableStateFlow(true)
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()

    private val _tempUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val tempUnit: StateFlow<TemperatureUnit> = _tempUnit.asStateFlow()

    private val _windUnit = MutableStateFlow(WindSpeedUnit.KMH)
    val windUnit: StateFlow<WindSpeedUnit> = _windUnit.asStateFlow()

    private val _pressureUnit = MutableStateFlow(PressureUnit.HPA)
    val pressureUnit: StateFlow<PressureUnit> = _pressureUnit.asStateFlow()

    private val _language = MutableStateFlow("TR")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _personalizationEnabled = MutableStateFlow(true)
    val personalizationEnabled: StateFlow<Boolean> = _personalizationEnabled.asStateFlow()

    private val _assistantTone = MutableStateFlow(AssistantTone.DENGELI)
    val assistantTone: StateFlow<AssistantTone> = _assistantTone.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBio = MutableStateFlow("")
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _userImageUri = MutableStateFlow<String?>(null)
    val userImageUri: StateFlow<String?> = _userImageUri.asStateFlow()

    private val _userInterests = MutableStateFlow<Set<String>>(emptySet())
    val userInterests: StateFlow<Set<String>> = _userInterests.asStateFlow()

    private val _userAboutMe = MutableStateFlow("")
    val userAboutMe: StateFlow<String> = _userAboutMe.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _registeredCities = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val registeredCities: StateFlow<List<GeocodingResultDto>> = _registeredCities.asStateFlow()

    private val _defaultCity = MutableStateFlow<GeocodingResultDto?>(null)
    val defaultCity: StateFlow<GeocodingResultDto?> = _defaultCity.asStateFlow()

    private val _tiltEffectEnabled = MutableStateFlow(true)
    val tiltEffectEnabled: StateFlow<Boolean> = _tiltEffectEnabled.asStateFlow()

    private val _liveEffectsEnabled = MutableStateFlow(true)
    val liveEffectsEnabled: StateFlow<Boolean> = _liveEffectsEnabled.asStateFlow()

    private val _isSafeMode = MutableStateFlow(false)
    val isSafeMode: StateFlow<Boolean> = _isSafeMode.asStateFlow()

    private val _userEffectIntensity = MutableStateFlow(com.havamania.WeatherEffectIntensity.MEDIUM)
    val userEffectIntensity: StateFlow<com.havamania.WeatherEffectIntensity> = _userEffectIntensity.asStateFlow()

    private val _locationMode = MutableStateFlow(LocationMode.MANUAL)
    val locationMode: StateFlow<LocationMode> = _locationMode.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()
    private val _settingsIssue = MutableStateFlow<com.havamania.SettingsIssue?>(null)
    internal val settingsIssue = _settingsIssue.asStateFlow()
    fun dismissSettingsIssue() { _settingsIssue.value = null }

    private var settingsJob: kotlinx.coroutines.Job? = null
    private var profileSyncJob: kotlinx.coroutines.Job? = null
    private var cityRecoveryJob: kotlinx.coroutines.Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        citySyncRequests = com.havamania.CitySyncRequestGate()
        val user = firebaseAuth.currentUser
        val newUid = user?.uid ?: "legacy"
if (BuildConfig.DEBUG) {
            Log.d("ThemeVM", "Auth state changed.")
}

        clearLocalUserData()
        _activeUid.value = user?.uid

        loadSettings()
        observeFirestoreUserDoc(newUid)
        observeFirestoreCities(newUid)
        prepareCitySyncRetry(authOnly = true)
        startCitySyncRecovery()
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        cityRecoveryJob?.cancel()
        auth.removeAuthStateListener(authListener)
        citiesListenerVersion++
        citiesSnapshotJob?.cancel()
        citiesListener?.remove()
    }

    private fun observeFirestoreCities(uid: String) {
        val listenerVersion = ++citiesListenerVersion
        citiesSnapshotJob?.cancel()
        citiesListener?.remove()
        if (uid == "legacy") return

if (BuildConfig.DEBUG) {
            Log.d("ThemeVM", "Starting cities listener.")
}
        val queue = com.havamania.CitySnapshotQueue<com.google.firebase.firestore.QuerySnapshot>()
        citiesSnapshotJob = accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) {
            queue.consume { snapshot ->
                try {
                    val remoteCities = snapshot.documents.mapNotNull {
                        it.toObject(com.havamania.GeocodingResultDto::class.java)
                    }
                    com.havamania.applyCitySnapshot(uid, listenerVersion,
                        { this@ThemeViewModel.currentUid }, { citiesListenerVersion },
                        save = { ThemeManager.mergeRemoteCities(getApplication(), uid, remoteCities) },
                        publish = {})
                } catch (ex: Exception) {
                    if (ex is kotlinx.coroutines.CancellationException) throw ex
                    com.havamania.publishAccountResult(uid, { this@ThemeViewModel.currentUid }) {
                        reportCityError(ex)
                    }
                }
            }
        }.also { job -> job.invokeOnCompletion { queue.close() } }
        citiesListener = db.collection("users").document(uid).collection("cities")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (uid != currentUid || listenerVersion != citiesListenerVersion) return@addSnapshotListener
                if (error != null) {
                    reportCityError(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
                    queue.offer(snapshot)
                }
            }
    }

    private fun observeFirestoreUserDoc(uid: String) {
        profileSyncJob?.cancel()
        if (uid == "legacy") return

        userProfileRepository.startObserving(uid)

        profileSyncJob = accountTasks.launch {
            launch {
                userProfileRepository.profile.collect { profile ->
                    if (uid == this@ThemeViewModel.currentUid) {
                        _isPremium.value = profile?.takeIf { it.uid == uid }?.isPremium ?: false
                    }
                }
            }
            combine(userProfileRepository.serverDefaultCity, registeredCities) { remote, cities -> remote to cities }
                .collect { (remote, cities) ->
                    if (remote?.uid != uid) return@collect
                    try {
                        com.havamania.publishAccountResult(uid, { currentUid }) {
                            ThemeManager.mergeServerDefaultCity(getApplication(), uid, remote.name,
                                com.havamania.resolveDefaultCityProfile(remote.name, remote.location, cities))
                        }
                    } catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        com.havamania.publishAccountResult(uid, { currentUid }) { reportCityError(error) }
                    }
                }
        }
    }


    fun loadSettings() {
        settingsJob?.cancel()
        _settingsIssue.value = null
        settingsJob = accountTasks.launch { uid ->
            loadSetting(uid, ThemeManager.getTheme(getApplication(), uid)) { _currentTheme.value = it }
            loadSetting(uid, ThemeManager.getAnimationsEnabled(getApplication(), uid)) { _animationsEnabled.value = it }
            loadSetting(uid, ThemeManager.getTempUnit(getApplication(), uid)) { _tempUnit.value = it }
            loadSetting(uid, ThemeManager.getWindUnit(getApplication(), uid)) { _windUnit.value = it }
            loadSetting(uid, ThemeManager.getPressureUnit(getApplication(), uid)) { _pressureUnit.value = it }
            loadSetting(uid, ThemeManager.getLanguage(getApplication(), uid)) { _language.value = it }
            loadSetting(uid, ThemeManager.getNotificationsEnabled(getApplication(), uid)) { _notificationsEnabled.value = it }
            loadSetting(uid, ThemeManager.getPersonalizationEnabled(getApplication(), uid)) { _personalizationEnabled.value = it }
            loadSetting(uid, ThemeManager.getAssistantTone(getApplication(), uid)) { _assistantTone.value = it }
            loadSetting(uid, ThemeManager.getUserName(getApplication(), uid)) { _userName.value = it }
            loadSetting(uid, ThemeManager.getUserBio(getApplication(), uid)) { _userBio.value = it }
            loadSetting(uid, ThemeManager.getUserImageUriByUid(getApplication(), uid)) { _userImageUri.value = it }
            loadSetting(uid, ThemeManager.getUserInterests(getApplication(), uid)) { _userInterests.value = it }
            loadSetting(uid, ThemeManager.getUserAboutMe(getApplication(), uid)) { _userAboutMe.value = it }
            loadSetting(uid, ThemeManager.getRegisteredCities(getApplication(), uid)) { _registeredCities.value = it }
            loadSetting(uid, ThemeManager.getDefaultCity(getApplication(), uid)) { _defaultCity.value = it }
            loadSetting(uid, ThemeManager.getLocationMode(getApplication(), uid)) { _locationMode.value = it }
            loadSetting(uid, ThemeManager.getTiltEffectEnabled(getApplication(), uid)) { _tiltEffectEnabled.value = it }
            loadSetting(uid, ThemeManager.getLiveEffects(getApplication(), uid)) { _liveEffectsEnabled.value = it }
            loadSetting(uid, ThemeManager.getEffectIntensity(getApplication(), uid)) { intensityStr ->
                _userEffectIntensity.value = try { com.havamania.WeatherEffectIntensity.valueOf(intensityStr) }
                catch (_: IllegalArgumentException) { com.havamania.WeatherEffectIntensity.MEDIUM }
            }
        }
    }

    private fun <T> CoroutineScope.loadSetting(owner: String, source: Flow<T>, apply: (T) -> Unit) = launch {
        com.havamania.observeAccountData(source, owner, { currentUid }, apply) {
            _settingsIssue.value = com.havamania.SettingsIssue.LOAD
        }
    }

    private fun savePreference(write: suspend (String) -> Unit, onApplied: () -> Unit) {
        accountTasks.launch { owner ->
            com.havamania.publishAccountResult(owner, { currentUid }) { _settingsIssue.value = null }
            com.havamania.persistAccountPreference(owner, { currentUid }, write, onApplied) {
                _settingsIssue.value = com.havamania.SettingsIssue.SAVE
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        savePreference({ uid -> ThemeManager.saveTheme(getApplication(), theme, uid) }) { _currentTheme.value = theme }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        savePreference({ uid -> ThemeManager.saveAnimationsEnabled(getApplication(), enabled, uid) }) { _animationsEnabled.value = enabled }
    }

    fun setTempUnit(unit: TemperatureUnit) {
        savePreference({ uid -> ThemeManager.saveTempUnit(getApplication(), unit, uid) }) { _tempUnit.value = unit }
    }

    fun setWindUnit(unit: WindSpeedUnit) {
        savePreference({ uid -> ThemeManager.saveWindUnit(getApplication(), unit, uid) }) { _windUnit.value = unit }
    }

    fun setPressureUnit(unit: PressureUnit) {
        savePreference({ uid -> ThemeManager.savePressureUnit(getApplication(), unit, uid) }) { _pressureUnit.value = unit }
    }

    fun setLanguage(lang: String) {
        if (!AppLanguageSupport.isSupported(lang)) return
        val supportedLanguage = AppLanguageSupport.resolve(lang)
        savePreference({ uid -> ThemeManager.saveLanguage(getApplication(), supportedLanguage, uid) }) { _language.value = supportedLanguage }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        savePreference({ uid -> ThemeManager.saveNotificationsEnabled(getApplication(), uid, enabled) }) { _notificationsEnabled.value = enabled }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        savePreference({ uid -> ThemeManager.savePersonalizationEnabled(getApplication(), uid, enabled) }) { _personalizationEnabled.value = enabled }
    }

    fun setAssistantTone(tone: AssistantTone) {
        savePreference({ uid -> ThemeManager.saveAssistantTone(getApplication(), uid, tone) }) { _assistantTone.value = tone }
    }

    fun setUserImageUri(uri: String?) {
        savePreference({ uid -> ThemeManager.saveUserImageUriByUid(getApplication(), uid, uri) }) { _userImageUri.value = uri }
    }

    fun dismissCityMutationError() { _cityMutationCanRetry.value = false; _cityMutationError.value = false; _cityMutationErrorDetails.value = null }
    fun retryCitySync() {
        if (!_cityMutationCanRetry.value) return
        dismissCityMutationError()
        prepareCitySyncRetry(authOnly = false)
    }

    private fun prepareCitySyncRetry(authOnly: Boolean) {
        accountTasks.launch { owner ->
            try {
                // A transport retry must not release unrelated identity/rejection blocks.
                if (authOnly) ThemeManager.releaseCitySyncBlocks(getApplication(), owner, true)
                retryPendingCityChanges()
                retryPendingDefaultCity()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                com.havamania.publishAccountResult(owner, { currentUid }) { reportCityError(error) }
            }
        }
    }

    private fun mutateCities(local: suspend (String) -> Boolean, remote: suspend (String) -> Unit) {
        accountTasks.launch { owner ->
            com.havamania.mutateAccountCities(cityMutationLock, owner, { currentUid },
                local = { local(owner) },
                remote = { if (owner != "legacy") remote(owner) },
                onFailure = { reportCityError(it) })
        }
    }

    private fun startCitySyncRecovery() {
        cityRecoveryJob?.cancel()
        cityRecoveryJob = accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) { owner ->
            if (owner == "legacy") return@launch
            // The existing startup attempt runs immediately; this loop takes over later recovery.
            kotlinx.coroutines.delay(30_000)
            com.havamania.runCitySyncRecovery(owner, { currentUid }, ::citySyncAllowed,
                read = {
                    val pending = ThemeManager.pendingCityChanges(getApplication(), owner)
                        .map { com.havamania.CitySyncRecoveryWork(it) } +
                        listOfNotNull(ThemeManager.pendingDefaultCity(getApplication(), owner)
                            ?.let { com.havamania.CitySyncRecoveryWork(it, true) })
                    pending.filter { !it.change.acknowledged && it.change.blockedReason == null }
                        .mapNotNull { work ->
                            ThemeManager.refreshCitySyncClock(getApplication(), owner, work.change, work.defaultCity)
                                ?.let { work.copy(change = it) }
                        }
                }, clock = { com.havamania.CitySyncClocks.capture(getApplication()) },
                send = { work ->
                    if (work.defaultCity) sendPendingDefaultCity(owner)
                    else sendCityChange(owner, work.change.city.id)
                }, onFailure = ::reportCityError)
        }
    }

    private fun retryPendingCityChanges() {
        accountTasks.launch { owner ->
            if (owner == "legacy") return@launch
            com.havamania.mutateAccountCities(cityMutationLock, owner, { currentUid },
                local = { true }, remote = {
                    com.havamania.runCitySyncBatch(
                        ThemeManager.pendingCityChanges(getApplication(), owner), owner, { currentUid },
                        send = { sendCityChange(owner, it.city.id) }, onFailure = ::reportCityError)
                }, onFailure = { reportCityError(it) })
        }
    }

    private suspend fun sendCityChange(uid: String, cityId: Long) =
        citySyncRequests.run("$uid:city:$cityId") {
        com.havamania.CitySyncDispatcher.sendLatest("$uid:city:$cityId", uid, { currentUid },
            read = { com.havamania.singleCitySyncChange(ThemeManager.pendingCityChanges(getApplication(), uid), cityId) },
            send = { change ->
                val document = db.collection("users").document(uid).collection("cities").document(cityId.toString())
                val requestJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
                val applied = db.runTransaction { transaction ->
                    com.havamania.stageCityDocumentMutation(change,
                        isCurrent = { requestJob?.isActive != false && uid == currentUid && citySyncAllowed() },
                        read = {
                            val snapshot = transaction.get(document)
                            if (!snapshot.exists()) null else {
                                try { snapshot.toObject(com.havamania.GeocodingResultDto::class.java)
                                    ?: throw com.havamania.CityIdentityConflictException() }
                                catch (_: Exception) { throw com.havamania.CityIdentityConflictException() }
                            }
                        },
                        write = { transaction.set(document, change.city) },
                        delete = { transaction.delete(document) })
                }.await()
                if (!applied) throw kotlinx.coroutines.CancellationException("City operation is no longer current")
            }, acknowledge = { ThemeManager.acknowledgeCityChange(getApplication(), uid, it) },
            recordFailure = { sent, error -> ThemeManager.recordCitySyncFailure(getApplication(), uid, sent, error, false) },
            reserve = { change, now -> ThemeManager.reserveCitySyncAttempt(getApplication(), uid, change, false, now) },
            isAllowed = ::citySyncAllowed,
            clock = { com.havamania.CitySyncClocks.capture(getApplication()) },
            refreshClock = { ThemeManager.refreshCitySyncClock(getApplication(), uid, it, false) })
    }

    fun addCity(city: GeocodingResultDto) {
        mutateCities(local = { uid ->
            ThemeManager.recordCityChange(getApplication(), uid, city, deleted = false)
        }, remote = { uid -> sendCityChange(uid, city.id) })
    }

    fun removeCity(city: GeocodingResultDto) {
        mutateCities(local = { uid ->
            ThemeManager.recordCityChange(getApplication(), uid, city, deleted = true)
        }, remote = { uid -> sendCityChange(uid, city.id) })
    }

    private fun retryPendingDefaultCity() {
        accountTasks.launch { owner ->
            if (owner == "legacy") return@launch
            try { sendPendingDefaultCity(owner) }
            catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                com.havamania.publishAccountResult(owner, { currentUid }) { reportCityError(error) }
            }
        }
    }

    private suspend fun sendPendingDefaultCity(uid: String) =
        citySyncRequests.run("$uid:default-city") {
        com.havamania.CitySyncDispatcher.sendLatest("$uid:default-city", uid, { currentUid },
            read = { ThemeManager.pendingDefaultCity(getApplication(), uid) },
            send = { change ->
                db.collection("users").document(uid)
                    .set(com.havamania.defaultCityProfileFields(change.city), SetOptions.merge()).await()
            }, acknowledge = { change ->
                ThemeManager.acknowledgeDefaultCity(getApplication(), uid, change)
                val remote = userProfileRepository.serverDefaultCity.value
                if (remote?.uid == uid) {
                    ThemeManager.mergeServerDefaultCity(getApplication(), uid, remote.name,
                        com.havamania.resolveDefaultCityProfile(remote.name, remote.location, _registeredCities.value))
                }
            }, recordFailure = { sent, error ->
                ThemeManager.recordCitySyncFailure(getApplication(), uid, sent, error, true)
            }, reserve = { change, now -> ThemeManager.reserveCitySyncAttempt(getApplication(), uid, change, true, now) }, isAllowed = ::citySyncAllowed,
            clock = { com.havamania.CitySyncClocks.capture(getApplication()) },
            refreshClock = { ThemeManager.refreshCitySyncClock(getApplication(), uid, it, true) })
    }

    fun setDefaultCity(city: GeocodingResultDto, useManualLocation: Boolean = false) {
        mutateCities(local = { uid ->
            ThemeManager.recordDefaultCity(getApplication(), uid, city, useManualLocation)
            true
        }, remote = { uid ->
            sendPendingDefaultCity(uid)
        })
    }

    fun setLocationMode(mode: LocationMode) {
        savePreference({ uid -> ThemeManager.saveLocationMode(getApplication(), uid, mode) }) { _locationMode.value = mode }
    }

    fun checkInitialLocationMode() {
        accountTasks.launch launch@ { currentUid ->
            val uid = currentUid
            val context = getApplication<Application>()

            val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val onboardingCompleted = ThemeManager.getOnboardingCompleted(context, uid).first()

            if (!onboardingCompleted && (fineLocation || coarseLocation)) {
                setLocationMode(LocationMode.AUTO)
            }
        }
    }

    fun setTiltEffectEnabled(enabled: Boolean) {
        savePreference({ uid -> ThemeManager.saveTiltEffectEnabled(getApplication(), enabled, uid) }) { _tiltEffectEnabled.value = enabled }
    }

    fun setLiveEffectsEnabled(enabled: Boolean) {
        savePreference({ uid -> ThemeManager.saveLiveEffects(getApplication(), enabled, uid) }) { _liveEffectsEnabled.value = enabled }
    }

    fun setEffectIntensity(intensity: com.havamania.WeatherEffectIntensity) {
        savePreference({ uid -> ThemeManager.saveEffectIntensity(getApplication(), intensity.name, uid) }) { _userEffectIntensity.value = intensity }
    }

    fun clearLocalUserData() {
        _cityMutationCanRetry.value = false
        _cityMutationErrorDetails.value = null
        _cityMutationError.value = false
        _settingsIssue.value = null
        // Oturum kapatıldığında state temizlenir ama DataStore'daki kalıcı veri silinmez (talimat gereği)
        _isPremium.value = false
        _userName.value = ""
        _userBio.value = ""
        _userImageUri.value = null
        _userAboutMe.value = ""
        _userInterests.value = emptySet()
        _defaultCity.value = null
        _registeredCities.value = emptyList()
        weatherAccountChanged(auth.currentUser?.uid)

    }
}
