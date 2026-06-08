package com.havamania.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
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

    private val _userName = MutableStateFlow("Gezgin")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBio = MutableStateFlow("Hava durumu meraklısı 🌤️")
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _userImageUri = MutableStateFlow<String?>(null)
    val userImageUri: StateFlow<String?> = _userImageUri.asStateFlow()

    private val _userInterests = MutableStateFlow<Set<String>>(emptySet())
    val userInterests: StateFlow<Set<String>> = _userInterests.asStateFlow()

    private val _userAboutMe = MutableStateFlow("")
    val userAboutMe: StateFlow<String> = _userAboutMe.asStateFlow()

    private val _registeredCities = MutableStateFlow<List<com.havamania.GeocodingResultDto>>(emptyList())
    val registeredCities: StateFlow<List<com.havamania.GeocodingResultDto>> = _registeredCities.asStateFlow()

    private val _defaultCity = MutableStateFlow(com.havamania.GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul"))
    val defaultCity: StateFlow<com.havamania.GeocodingResultDto> = _defaultCity.asStateFlow()

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
    }

    private fun loadSettings() {
        viewModelScope.launch {
            ThemeManager.getTheme(getApplication()).collect { _currentTheme.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getAnimationsEnabled(getApplication()).collect { _animationsEnabled.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getTempUnit(getApplication()).collect { _tempUnit.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getLanguage(getApplication()).collect { _language.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getNotificationsEnabled(getApplication()).collect { _notificationsEnabled.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getUserName(getApplication()).collect { _userName.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getUserBio(getApplication()).collect { _userBio.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getUserImageUri(getApplication()).collect { _userImageUri.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getUserInterests(getApplication()).collect { _userInterests.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getUserAboutMe(getApplication()).collect { _userAboutMe.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getRegisteredCities(getApplication()).collect { _registeredCities.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getDefaultCity(getApplication()).collect { _defaultCity.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getTiltEffectEnabled(getApplication()).collect { _tiltEffectEnabled.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getLiveEffects(getApplication()).collect { _liveEffectsEnabled.value = it }
        }
        viewModelScope.launch {
            ThemeManager.getEffectIntensity(getApplication()).collect { intensityStr ->
                _userEffectIntensity.value = try {
                    com.havamania.WeatherEffectIntensity.valueOf(intensityStr)
                } catch (e: Exception) {
                    com.havamania.WeatherEffectIntensity.MEDIUM
                }
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            ThemeManager.saveTheme(getApplication(), theme)
            _currentTheme.value = theme
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveAnimationsEnabled(getApplication(), enabled)
            _animationsEnabled.value = enabled
        }
    }

    fun setTempUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            ThemeManager.saveTempUnit(getApplication(), unit)
            _tempUnit.value = unit
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            ThemeManager.saveLanguage(getApplication(), lang)
            _language.value = lang
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveNotificationsEnabled(getApplication(), enabled)
            _notificationsEnabled.value = enabled
        }
    }

    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            ThemeManager.saveUserName(getApplication(), name)
            ThemeManager.saveUserBio(getApplication(), bio)
            _userName.value = name
            _userBio.value = bio
        }
    }

    fun setUserAboutMe(aboutMe: String) {
        viewModelScope.launch {
            ThemeManager.saveUserAboutMe(getApplication(), aboutMe)
            _userAboutMe.value = aboutMe
        }
    }

    fun setUserImageUri(uri: String?) {
        viewModelScope.launch {
            ThemeManager.saveUserImageUri(getApplication(), uri)
            _userImageUri.value = uri
        }
    }

    fun toggleInterest(interest: String) {
        viewModelScope.launch {
            val current = _userInterests.value.toMutableSet()
            if (current.contains(interest)) current.remove(interest)
            else current.add(interest)
            ThemeManager.saveUserInterests(getApplication(), current)
            _userInterests.value = current
        }
    }

    fun addCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            val current = _registeredCities.value.toMutableList()
            if (current.none { it.id == city.id || (it.name == city.name && it.admin1 == city.admin1) }) {
                current.add(city)
                ThemeManager.saveRegisteredCities(getApplication(), current)
                _registeredCities.value = current
            }
        }
    }

    fun removeCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            val current = _registeredCities.value.toMutableList()
            if (current.size > 1 && current.any { it.id == city.id }) {
                current.removeAll { it.id == city.id }
                ThemeManager.saveRegisteredCities(getApplication(), current)
                _registeredCities.value = current

                if (_defaultCity.value.id == city.id) {
                    setDefaultCity(current.first())
                }
            }
        }
    }

    fun setDefaultCity(city: com.havamania.GeocodingResultDto) {
        viewModelScope.launch {
            ThemeManager.saveDefaultCity(getApplication(), city)
            _defaultCity.value = city
        }
    }

    fun setTiltEffectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveTiltEffectEnabled(getApplication(), enabled)
            _tiltEffectEnabled.value = enabled
        }
    }

    fun setLiveEffectsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveLiveEffects(getApplication(), enabled)
            _liveEffectsEnabled.value = enabled
        }
    }

    fun setEffectIntensity(intensity: com.havamania.WeatherEffectIntensity) {
        viewModelScope.launch {
            ThemeManager.saveEffectIntensity(getApplication(), intensity.name)
            _userEffectIntensity.value = intensity
        }
    }

    fun resetCities() {
        viewModelScope.launch {
            ThemeManager.clearRegisteredCities(getApplication())
            val default = com.havamania.GeocodingResultDto(0, "İstanbul", 41.0082, 28.9784, "Turkey", "TR", "İstanbul")
            _registeredCities.value = listOf(default)
            _defaultCity.value = default
            ThemeManager.saveDefaultCity(getApplication(), default)
        }
    }

    fun removeProfileImage() {
        viewModelScope.launch {
            ThemeManager.saveUserImageUri(getApplication(), null)
            _userImageUri.value = null
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            ThemeManager.resetAll(getApplication())
            loadSettings()
        }
    }
}
