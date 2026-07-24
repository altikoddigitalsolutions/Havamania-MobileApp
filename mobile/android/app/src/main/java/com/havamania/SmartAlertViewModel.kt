package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmartAlertViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val _config = MutableStateFlow(SmartAlertConfig())
    val config: StateFlow<SmartAlertConfig> = _config.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<SmartAlert>>(emptyList())
    val activeAlerts: StateFlow<List<SmartAlert>> = _activeAlerts.asStateFlow()

    init {
        loadConfig()

        // Reload when auth changes
        viewModelScope.launch {
            auth.addAuthStateListener {
                loadConfig()
            }
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            ThemeManager.getSmartAlertConfig(getApplication(), currentUid).collect {
                _config.value = it
            }
        }
    }

    fun toggleAlert(alertId: String, enabled: Boolean) {
        viewModelScope.launch {
            ThemeManager.saveSingleAlertSetting(getApplication(), currentUid, alertId, enabled)
            // Local state update
            _config.update { current ->
                when(alertId) {
                    "rain" -> current.copy(rainEnabled = enabled)
                    "wind" -> current.copy(windEnabled = enabled)
                    "heat" -> current.copy(heatEnabled = enabled)
                    "frost" -> current.copy(frostEnabled = enabled)
                    "fog" -> current.copy(fogEnabled = enabled)
                    "storm" -> current.copy(stormEnabled = enabled)
                    "uv" -> current.copy(uvEnabled = enabled)
                    "pollen" -> current.copy(pollenEnabled = enabled)
                    "aqi" -> current.copy(airQualityEnabled = enabled)
                    else -> current
                }
            }
        }
    }

    fun calculateActiveAlerts(weather: WeatherData) {
        _activeAlerts.value = SmartAlertEngine.generateAlerts(weather, _config.value)
    }
}
