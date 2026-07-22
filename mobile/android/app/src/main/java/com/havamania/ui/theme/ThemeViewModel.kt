package com.havamania.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.havamania.UserProfile
import com.havamania.WeatherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val repository = WeatherRepository.getInstance(application)
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val _currentTheme = MutableStateFlow(AppTheme.DARK)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _animationsEnabled = MutableStateFlow(true)
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled.asStateFlow()

    private val _tempUnit = MutableStateFlow(TemperatureUnit.CELSIUS)
    val tempUnit: StateFlow<TemperatureUnit> = _tempUnit.asStateFlow()

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

    private val _registeredCities = MutableStateFlow<List<com.havamania.GeocodingResultDto>>(emptyList())
    val registeredCities: StateFlow<List<com.havamania.GeocodingResultDto>> = _registeredCities.asStateFlow()

    private val _defaultCity = MutableStateFlow<com.havamania.GeocodingResultDto?>(null)
    val defaultCity: StateFlow<com.havamania.GeocodingResultDto?> = _defaultCity.asStateFlow()

    private val _tiltEffectEnabled = MutableStateFlow(true)
    val tiltEffectEnabled: StateFlow<Boolean> = _tiltEffectEnabled.asStateFlow()

    private val _liveEffectsEnabled = MutableStateFlow(true)
    val liveEffectsEnabled: StateFlow<Boolean> = _liveEffectsEnabled.asStateFlow()

    private val _isSafeMode = MutableStateFlow(false)
    val isSafeMode: StateFlow<Boolean> = _isSafeMode.asStateFlow()

    private val _userEffectIntensity = MutableStateFlow(com.havamania.WeatherEffectIntensity.MEDIUM)
    val userEffectIntensity: StateFlow<com.havamania.WeatherEffectIntensity> = _userEffectIntensity.asStateFlow()

    init {
        loadSettings()

        // Watch for auth changes to reload user data
        viewModelScope.launch {
            auth.addAuthStateListener {
                loadSettings()
            }
        }
    }

    fun loadSettings() {
        val uid = currentUid
        viewModelScope.launch { ThemeManager.getTheme(getApplication(), uid).collect { _currentTheme.value = it } }
        viewModelScope.launch { ThemeManager.getAnimationsEnabled(getApplication(), uid).collect { _animationsEnabled.value = it } }
        viewModelScope.launch { ThemeManager.getTempUnit(getApplication(), uid).collect { _tempUnit.value = it } }
        viewModelScope.launch { ThemeManager.getLanguage(getApplication(), uid).collect { _language.value = it } }

        viewModelScope.launch { ThemeManager.getNotificationsEnabled(getApplication(), uid).collect { _notificationsEnabled.value = it } }
        viewModelScope.launch { ThemeManager.getPersonalizationEnabled(getApplication(), uid).collect { _personalizationEnabled.value = it } }
        viewModelScope.launch { ThemeManager.getAssistantTone(getApplication(), uid).collect { _assistantTone.value = it } }
        viewModelScope.launch { ThemeManager.getUserName(getApplication(), uid).collect { _userName.value = it } }
        viewModelScope.launch { ThemeManager.getUserBio(getApplication(), uid).collect { _userBio.value = it } }
        viewModelScope.launch { ThemeManager.getUserImageUriByUid(getApplication(), uid).collect { _userImageUri.value = it } }
        viewModelScope.launch { ThemeManager.getUserInterests(getApplication(), uid).collect { _userInterests.value = it } }
        viewModelScope.launch { ThemeManager.getUserAboutMe(getApplication(), uid).collect { _userAboutMe.value = it } }
        viewModelScope.launch { ThemeManager.getRegisteredCities(getApplication(), uid).collect { _registeredCities.value = it } }
        viewModelScope.launch { ThemeManager.getDefaultCity(getApplication(), uid).collect { _defaultCity.value = it } }

        viewModelScope.launch { ThemeManager.getTiltEffectEnabled(getApplication(), uid).collect { _tiltEffectEnabled.value = it } }
        viewModelScope.launch { ThemeManager.getLiveEffects(getApplication(), uid).collect { _liveEffectsEnabled.value = it } }
        viewModelScope.launch {
            ThemeManager.getEffectIntensity(getApplication(), uid).collect { intensityStr ->
                _userEffectIntensity.value = try { com.havamania.WeatherEffectIntensity.valueOf(intensityStr) } catch (e: Exception) { com.havamania.WeatherEffectIntensity.MEDIUM }
            }
        }
    }
    fun setTheme(theme: AppTheme) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveTheme(getApplication(), theme, uid)
            _currentTheme.value = theme
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveAnimationsEnabled(getApplication(), enabled, uid)
            _animationsEnabled.value = enabled
        }
    }

    fun setTempUnit(unit: TemperatureUnit) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveTempUnit(getApplication(), unit, uid)
            _tempUnit.value = unit
        }
    }

    fun setLanguage(lang: String) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveLanguage(getApplication(), lang, uid)
            _language.value = lang
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveNotificationsEnabled(getApplication(), currentUid, enabled)
            _notificationsEnabled.value = enabled
        }
    }

    fun setPersonalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.savePersonalizationEnabled(getApplication(), currentUid, enabled)
            _personalizationEnabled.value = enabled
        }
    }

    fun setAssistantTone(tone: AssistantTone) {
        viewModelScope.launch {
            ThemeManager.saveAssistantTone(getApplication(), currentUid, tone)
            _assistantTone.value = tone
        }
    }

    fun setUserImageUri(uri: String?) {
        viewModelScope.launch {
            ThemeManager.saveUserImageUriByUid(getApplication(), currentUid, uri)
            _userImageUri.value = uri
        }
    }

    fun addCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            val uid = currentUid
            val current = _registeredCities.value.toMutableList()
            if (current.none { it.id == city.id || (it.name == city.name && it.admin1 == city.admin1) }) {
                current.add(city)
                ThemeManager.saveRegisteredCities(getApplication(), uid, current)
                _registeredCities.value = current
            }
        }
    }

    fun removeCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            val uid = currentUid
            val current = _registeredCities.value.toMutableList()
            if (current.size > 1 && current.any { it.id == city.id }) {
                current.removeAll { it.id == city.id }
                ThemeManager.saveRegisteredCities(getApplication(), uid, current)
                _registeredCities.value = current

                if (_defaultCity.value?.id == city.id) {
                    setDefaultCity(current.first())
                }
            }
        }
    }

    fun setDefaultCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            ThemeManager.saveDefaultCity(getApplication(), currentUid, city)
            _defaultCity.value = city
        }
    }

    fun setTiltEffectEnabled(enabled: Boolean) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveTiltEffectEnabled(getApplication(), enabled, uid)
            _tiltEffectEnabled.value = enabled
        }
    }

    fun setLiveEffectsEnabled(enabled: Boolean) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveLiveEffects(getApplication(), enabled, uid)
            _liveEffectsEnabled.value = enabled
        }
    }

    fun setEffectIntensity(intensity: com.havamania.WeatherEffectIntensity) {
        val uid = currentUid
        viewModelScope.launch {
            ThemeManager.saveEffectIntensity(getApplication(), intensity.name, uid)
            _userEffectIntensity.value = intensity
        }
    }

    fun resetCities() {
        viewModelScope.launch {
            val uid = currentUid
            ThemeManager.clearRegisteredCities(getApplication(), uid)
            _registeredCities.value = emptyList()
            _defaultCity.value = null
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val uid = currentUid
            ThemeManager.resetAll(getApplication(), uid)
            loadSettings()
        }
    }

    fun syncWithFirebase(profile: com.havamania.UserProfile) {
        viewModelScope.launch {
            val uid = currentUid
            android.util.Log.i("PHOTO", "[PHOTO] Step 11 OK: syncWithFirebase started for $uid. PhotoURL in doc: ${profile.photoURL}")

            try {
                if (profile.name.isNotBlank()) {
                    ThemeManager.saveUserName(getApplication(), uid, profile.name)
                    _userName.value = profile.name
                }
                if (profile.bio.isNotBlank()) {
                    ThemeManager.saveUserBio(getApplication(), uid, profile.bio)
                    _userBio.value = profile.bio
                }

                android.util.Log.i("PHOTO", "[PHOTO] Step 11.1: Saving PhotoURL to DataStore and StateFlow: ${profile.photoURL}")
                ThemeManager.saveUserImageUriByUid(getApplication(), uid, profile.photoURL)
                _userImageUri.value = profile.photoURL

                if (profile.aboutMe.isNotBlank()) {
                    ThemeManager.saveUserAboutMe(getApplication(), uid, profile.aboutMe)
                    _userAboutMe.value = profile.aboutMe
                }

                profile.personalizationProfile?.let {
                    ThemeManager.saveUserInterests(getApplication(), uid, it.selectedInterests.toSet())
                    _userInterests.value = it.selectedInterests.toSet()
                }

                android.util.Log.i("PHOTO", "[PHOTO] Step 11.2 OK: syncWithFirebase complete")
            } catch (e: Exception) {
                android.util.Log.e("PHOTO", "[PHOTO] Step 11 FAILED: ${e.message}", e)
            }
        }
    }

    fun clearLocalUserData() {
        // Oturum kapatıldığında state temizlenir ama DataStore'daki kalıcı veri silinmez (talimat gereği)
        _userName.value = ""
        _userBio.value = ""
        _userImageUri.value = null
        _userAboutMe.value = ""
        _userInterests.value = emptySet()
        _defaultCity.value = null
        _registeredCities.value = emptyList()
        repository.clearCurrentWeather()
    }
}
