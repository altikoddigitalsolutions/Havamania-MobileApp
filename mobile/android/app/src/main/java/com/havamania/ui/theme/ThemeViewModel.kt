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

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repository = WeatherRepository.getInstance(application)
    private val userProfileRepository = com.havamania.UserProfileRepository.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val accountTasks = com.havamania.AccountTaskScope(viewModelScope) { currentUid }

    private var citiesListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null

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

    private var settingsJob: kotlinx.coroutines.Job? = null
    private var profileSyncJob: kotlinx.coroutines.Job? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        val user = firebaseAuth.currentUser
        val newUid = user?.uid ?: "legacy"
if (BuildConfig.DEBUG) {
            Log.d("ThemeVM", "Auth state changed. New UID: $newUid")
}

        clearLocalUserData()

        loadSettings()
        observeFirestoreUserDoc(newUid)
        observeFirestoreCities(newUid)
    }

    init {
        loadSettings()
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        citiesListener?.remove()
        userDocListener?.remove()
    }

    private fun observeFirestoreCities(uid: String) {
        citiesListener?.remove()
        if (uid == "legacy") return

if (BuildConfig.DEBUG) {
            Log.d("ThemeVM", "Starting Cities listener for $uid")
}
        citiesListener = db.collection("users").document(uid).collection("cities")
            .addSnapshotListener { snapshot, e ->
                if (uid != currentUid) return@addSnapshotListener
                if (e != null) {
if (BuildConfig.DEBUG) {
                        Log.w("ThemeVM", "Cities listen failed.", e)
}
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    accountTasks.launch(kotlinx.coroutines.Dispatchers.IO) launch@ { currentUid ->
                        try {
                            val remoteCities = snapshot.documents.mapNotNull {
                                it.toObject(com.havamania.GeocodingResultDto::class.java)
                            }
if (BuildConfig.DEBUG) {
                                Log.d("ThemeVM", "Firestore cities received: ${remoteCities.size}")
}

                            if (remoteCities != _registeredCities.value) {
                                ThemeManager.saveRegisteredCities(getApplication(), uid, remoteCities)
                                _registeredCities.value = remoteCities
                            }
                        } catch (ex: Exception) {
                            if (ex is kotlinx.coroutines.CancellationException) throw ex
if (BuildConfig.DEBUG) {
                                Log.e("ThemeVM", "Error parsing cities snapshot", ex)
}
                        }
                    }
                }
            }
    }

    private fun observeFirestoreUserDoc(uid: String) {
        profileSyncJob?.cancel()
        if (uid == "legacy") return

        userProfileRepository.startObserving(uid)

        profileSyncJob = accountTasks.launch launch@ { currentUid ->
            userProfileRepository.profile.collect { profile ->
                if (uid != this@ThemeViewModel.currentUid || profile?.uid != uid) return@collect
                profile?.let { p ->
                    // Sync defaultCity
                    val remoteDefaultCityName = p.defaultCity
                    if (remoteDefaultCityName != null && remoteDefaultCityName != _defaultCity.value?.name) {
                        val cityToSet = _registeredCities.value.find { it.name == remoteDefaultCityName }
                        if (cityToSet != null) {
                            ThemeManager.saveDefaultCity(getApplication(), uid, cityToSet)
                            _defaultCity.value = cityToSet
                        }
                    }
                }
            }
        }
    }


    fun loadSettings() {
        settingsJob?.cancel()
        val uid = currentUid
        settingsJob = accountTasks.launch { _ ->
            launch { ThemeManager.getTheme(getApplication(), uid).collect { _currentTheme.value = it } }
            launch { ThemeManager.getAnimationsEnabled(getApplication(), uid).collect { _animationsEnabled.value = it } }
            launch { ThemeManager.getTempUnit(getApplication(), uid).collect { _tempUnit.value = it } }
            launch { ThemeManager.getWindUnit(getApplication(), uid).collect { _windUnit.value = it } }
            launch { ThemeManager.getPressureUnit(getApplication(), uid).collect { _pressureUnit.value = it } }
            launch { ThemeManager.getLanguage(getApplication(), uid).collect { _language.value = it } }

            launch { ThemeManager.getNotificationsEnabled(getApplication(), uid).collect { _notificationsEnabled.value = it } }
            launch { ThemeManager.getPersonalizationEnabled(getApplication(), uid).collect { _personalizationEnabled.value = it } }
            launch { ThemeManager.getAssistantTone(getApplication(), uid).collect { _assistantTone.value = it } }
            launch { ThemeManager.getUserName(getApplication(), uid).collect { _userName.value = it } }
            launch { ThemeManager.getUserBio(getApplication(), uid).collect { _userBio.value = it } }
            launch { ThemeManager.getUserImageUriByUid(getApplication(), uid).collect { _userImageUri.value = it } }
            launch { ThemeManager.getUserInterests(getApplication(), uid).collect { _userInterests.value = it } }
            launch { ThemeManager.getUserAboutMe(getApplication(), uid).collect { _userAboutMe.value = it } }
            launch { ThemeManager.getRegisteredCities(getApplication(), uid).collect { _registeredCities.value = it } }
            launch { ThemeManager.getDefaultCity(getApplication(), uid).collect { _defaultCity.value = it } }
            launch { ThemeManager.getLocationMode(getApplication(), uid).collect { _locationMode.value = it } }

            launch { ThemeManager.getTiltEffectEnabled(getApplication(), uid).collect { _tiltEffectEnabled.value = it } }
            launch { ThemeManager.getLiveEffects(getApplication(), uid).collect { _liveEffectsEnabled.value = it } }
            launch {
                ThemeManager.getEffectIntensity(getApplication(), uid).collect { intensityStr ->
                    _userEffectIntensity.value = try { com.havamania.WeatherEffectIntensity.valueOf(intensityStr) } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        com.havamania.WeatherEffectIntensity.MEDIUM
                    }
                }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveTheme(getApplication(), theme, uid)
            _currentTheme.value = theme
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveAnimationsEnabled(getApplication(), enabled, uid)
            _animationsEnabled.value = enabled
        }
    }

    fun setTempUnit(unit: TemperatureUnit) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveTempUnit(getApplication(), unit, uid)
            _tempUnit.value = unit
        }
    }

    fun setWindUnit(unit: WindSpeedUnit) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveWindUnit(getApplication(), unit, uid)
            _windUnit.value = unit
        }
    }

    fun setPressureUnit(unit: PressureUnit) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.savePressureUnit(getApplication(), unit, uid)
            _pressureUnit.value = unit
        }
    }

    fun setLanguage(lang: String) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveLanguage(getApplication(), lang, uid)
            _language.value = lang
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveNotificationsEnabled(getApplication(), currentUid, enabled)
            _notificationsEnabled.value = enabled
        }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.savePersonalizationEnabled(getApplication(), currentUid, enabled)
            _personalizationEnabled.value = enabled
        }
    }

    fun setAssistantTone(tone: AssistantTone) {
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveAssistantTone(getApplication(), currentUid, tone)
            _assistantTone.value = tone
        }
    }

    fun setUserImageUri(uri: String?) {
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveUserImageUriByUid(getApplication(), currentUid, uri)
            _userImageUri.value = uri
        }
    }

    fun addCity(city: GeocodingResultDto) {
        accountTasks.launch launch@ { currentUid ->
            try {
                val uid = currentUid
                val current = _registeredCities.value.toMutableList()
                if (current.none { it.id == city.id || (it.name == city.name && it.admin1 == city.admin1) }) {
                    current.add(city)
                    ThemeManager.saveRegisteredCities(getApplication(), uid, current)
                    _registeredCities.value = current

                    if (uid != "legacy") {
                        db.collection("users").document(uid).collection("cities")
                            .document(city.id.toString()).set(city)
                            .await()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    Log.e("ThemeVM", "Failed to add city", e)
}
                _uiEvent.emit("Şehir şu anda kaydedilemedi.")
            }
        }
    }

    fun removeCity(city: GeocodingResultDto) {
        accountTasks.launch launch@ { currentUid ->
            try {
                val uid = currentUid
                val current = _registeredCities.value.toMutableList()
                if (current.size > 1 && current.any { it.id == city.id }) {
                    current.removeAll { it.id == city.id }
                    ThemeManager.saveRegisteredCities(getApplication(), uid, current)
                    _registeredCities.value = current

                    if (_defaultCity.value?.id == city.id) {
                        setDefaultCity(current.first())
                    }

                    if (uid != "legacy") {
                        db.collection("users").document(uid).collection("cities")
                            .document(city.id.toString()).delete()
                            .await()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    Log.e("ThemeVM", "Failed to remove city", e)
}
                _uiEvent.emit("Şehir şu anda silinemedi.")
            }
        }
    }

    fun setDefaultCity(city: GeocodingResultDto) {
        accountTasks.launch launch@ { currentUid ->
            try {
                val uid = currentUid
                ThemeManager.saveDefaultCity(getApplication(), uid, city)
                _defaultCity.value = city

                if (uid != "legacy") {
                    db.collection("users").document(uid)
                        .set(mapOf("defaultCity" to city.name), SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    Log.e("ThemeVM", "Failed to sync default city", e)
}
            }
        }
    }

    fun setLocationMode(mode: LocationMode) {
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveLocationMode(getApplication(), currentUid, mode)
            _locationMode.value = mode
        }
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
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveTiltEffectEnabled(getApplication(), enabled, uid)
            _tiltEffectEnabled.value = enabled
        }
    }

    fun setLiveEffectsEnabled(enabled: Boolean) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveLiveEffects(getApplication(), enabled, uid)
            _liveEffectsEnabled.value = enabled
        }
    }

    fun setEffectIntensity(intensity: com.havamania.WeatherEffectIntensity) {
        val uid = currentUid
        accountTasks.launch launch@ { currentUid ->
            ThemeManager.saveEffectIntensity(getApplication(), intensity.name, uid)
            _userEffectIntensity.value = intensity
        }
    }

    fun resetCities() {
        accountTasks.launch launch@ { currentUid ->
            val uid = currentUid
            ThemeManager.clearRegisteredCities(getApplication(), uid)
            _registeredCities.value = emptyList()
            _defaultCity.value = null
        }
    }

    fun resetAllData() {
        accountTasks.launch launch@ { currentUid ->
            val uid = currentUid
            ThemeManager.resetAll(getApplication(), uid)
            loadSettings()
        }
    }

    fun syncWithFirebase(profile: com.havamania.UserProfile) {
        accountTasks.launch launch@ { currentUid ->
            if (profile.uid != currentUid) return@launch
            val uid = currentUid
            if (BuildConfig.DEBUG) Log.i("PHOTO", "[PHOTO] syncWithFirebase started for $uid")

            try {
                if (profile.name.isNotBlank()) {
                    ThemeManager.saveUserName(getApplication(), uid, profile.name)
                    _userName.value = profile.name
                }
                if (profile.bio.isNotBlank()) {
                    ThemeManager.saveUserBio(getApplication(), uid, profile.bio)
                    _userBio.value = profile.bio
                }

                ThemeManager.saveUserImageUriByUid(getApplication(), uid, profile.photoURL)
                _userImageUri.value = profile.photoURL

                if (profile.aboutMe.isNotBlank()) {
                    ThemeManager.saveUserAboutMe(getApplication(), uid, profile.aboutMe)
                    _userAboutMe.value = profile.aboutMe
                }

                _isPremium.value = profile.isPremium

                profile.personalizationProfile?.let {
                    ThemeManager.saveUserInterests(getApplication(), uid, it.selectedInterests.toSet())
                    _userInterests.value = it.selectedInterests.toSet()
                }

if (BuildConfig.DEBUG) {
                    Log.i("PHOTO", "[PHOTO] Step 11.2 OK: syncWithFirebase complete")
}
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                    Log.e("PHOTO", "[PHOTO] Step 11 FAILED: ${e.message}", e)
}
            }
        }
    }

    fun clearLocalUserData() {
        // Oturum kapatıldığında state temizlenir ama DataStore'daki kalıcı veri silinmez (talimat gereği)
        _isPremium.value = false
        _userName.value = ""
        _userBio.value = ""
        _userImageUri.value = null
        _userAboutMe.value = ""
        _userInterests.value = emptySet()
        _defaultCity.value = null
        _registeredCities.value = emptyList()
        repository.clearCurrentWeather()

        userDocListener?.remove()
        userDocListener = null
    }
}
