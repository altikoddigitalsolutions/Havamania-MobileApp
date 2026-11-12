package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmartAlertViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"
    private val tasks = AccountTaskScope(viewModelScope) { currentUid }
    private var generation = 0L
    private var observedUid: String? = null
    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _config = MutableStateFlow(SmartAlertConfig())
    val config: StateFlow<SmartAlertConfig> = _config.asStateFlow()

    private val _activeAlerts = MutableStateFlow<List<SmartAlert>>(emptyList())
    val activeAlerts: StateFlow<List<SmartAlert>> = _activeAlerts.asStateFlow()

    private var configJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener {
        loadConfig()
    }

    init {
        loadConfig()
        auth.addAuthStateListener(authListener)
    }

    private fun loadConfig(force: Boolean = false) {
        val uid = currentUid
        if (!force && uid == observedUid) return
        observedUid = uid
        val epoch = ++generation
        tasks.reset()
        configJob?.cancel()
        _config.value = SmartAlertConfig()
        _activeAlerts.value = emptyList()
        _ready.value = false
        _saving.value = false
        _error.value = null
        configJob = viewModelScope.launch {
            observeCurrentWeatherSettings(ThemeManager.getSmartAlertConfig(getApplication(), uid),
                { epoch == generation && uid == currentUid },
                { _config.value = it; _ready.value = true; _error.value = null },
                { _ready.value = false; _activeAlerts.value = emptyList(); _error.value = "Uyarı tercihleri okunamadı. Yeniden deneyin." })
        }
    }
    fun retryLoading() { loadConfig(force = true) }
    fun dismissError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        configJob?.cancel()
        tasks.close()
    }

    fun toggleAlert(alertId: String, enabled: Boolean) {
        if (!_ready.value || _saving.value || alertId !in setOf("rain", "wind", "heat", "frost", "fog", "storm", "uv", "pollen", "aqi")) return
        val epoch = generation
        val ownerAtClick = observedUid?.takeIf { it == currentUid } ?: return
        _saving.value = true
        tasks.launch { owner ->
            try {
                val result = writeOwnedPreference(owner, { epoch == generation && owner == ownerAtClick && owner == currentUid }) { uid ->
                    ThemeManager.saveSingleAlertSetting(getApplication(), uid, alertId, enabled)
                }
                if (result == PreferenceWriteResult.FAILED) _error.value = "Tercih kaydedilemedi. Lütfen yeniden deneyin."
            } finally {
                if (epoch == generation && owner == currentUid) _saving.value = false
            }
        }
    }

    fun calculateActiveAlerts(weather: WeatherData) {
        _activeAlerts.value = if (_ready.value) SmartAlertEngine.generateAlerts(weather, _config.value, currentUid) else emptyList()
    }
}
