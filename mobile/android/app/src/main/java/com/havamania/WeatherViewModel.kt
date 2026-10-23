package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import java.time.LocalDate
import com.havamania.ui.theme.AssistantTone
import com.havamania.ui.theme.LocationMode
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.Flow

/**
 * Hava durumu ekranı için ViewModel - Network Awareness eklendi
 */
class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeatherRepository.getInstance(application)
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid ?: "legacy"

    private val networkMonitor: NetworkMonitor = ConnectivityManagerNetworkMonitor(application)

    private val locationTracker: LocationTracker by lazy {
        DefaultLocationTracker(
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(application),
            application
        )
    }

    // İnternet durumunu flow olarak takip et
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _selectedForecastDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedForecastDate: StateFlow<LocalDate> = _selectedForecastDate.asStateFlow()

    private val _selectedDailyForecast = MutableStateFlow<DailyForecast?>(null)
    val selectedDailyForecast: StateFlow<DailyForecast?> = _selectedDailyForecast.asStateFlow()

    private val _selectedHourlyWeather = MutableStateFlow<HourlyWeather?>(null)
    val selectedHourlyWeather: StateFlow<HourlyWeather?> = _selectedHourlyWeather.asStateFlow()

    private val _todayRecommendation = MutableStateFlow<HavamaniaRecommendation?>(null)
    val todayRecommendation: StateFlow<HavamaniaRecommendation?> = _todayRecommendation.asStateFlow()

    private val _assistantTone = MutableStateFlow<AssistantTone>(AssistantTone.DENGELI)
    val assistantTone: StateFlow<AssistantTone> = _assistantTone.asStateFlow()

    private var currentUserInterests: Set<String> = emptySet()
    private var currentUserAboutMe: String? = null
    private var currentPersonalization: PersonalizationProfile? = null
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _citySearchStatus = MutableStateFlow(LocationSearchStatus.IDLE)
    val citySearchStatus = _citySearchStatus.asStateFlow()
    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val notificationRepository: NotificationRepository by lazy {
        val database = NotificationDatabase.getDatabase(application)
        NotificationRepository(database.notificationDao())
    }

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private val _smartAlerts = MutableStateFlow<List<SmartAlert>>(emptyList())
    val smartAlerts: StateFlow<List<SmartAlert>> = _smartAlerts.asStateFlow()

    private val _locationMode = MutableStateFlow(LocationMode.MANUAL)
    val locationMode: StateFlow<LocationMode> = _locationMode.asStateFlow()

    private var lastCity = "Balıkesir"
    private var lastDistrict: String? = null
    private var lastLat = 39.6484
    private var lastLon = 27.8826

    private var fetchJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private val accountTasks = AccountTaskScope(viewModelScope) { currentUid }
    private var locationJob: kotlinx.coroutines.Job? = null

    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        if (BuildConfig.DEBUG) Log.d("WeatherVM", "Auth changed. Re-initializing for $newUid")

        // Clear UI state to prevent showing old user's data
        fetchJob?.cancel()
        locationJob?.cancel()
        searchJob?.cancel()
        _isRefreshing.value = false
        _citySearchStatus.value = LocationSearchStatus.IDLE
        _citySuggestions.value = emptyList()
        currentUserInterests = emptySet()
        currentUserAboutMe = null
        currentPersonalization = null
        repository.clearCurrentWeather()
        _uiState.value = WeatherUiState.Loading
        _todayRecommendation.value = null
        _smartAlerts.value = emptyList()

        observeUserData(newUid)
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        userDataJob?.cancel()
        fetchJob?.cancel()
    }

    private var userDataJob: kotlinx.coroutines.Job? = null

    private fun observeUserData(uid: String) {
        userDataJob?.cancel()
        userDataJob = viewModelScope.launch {
            // Tone observation
            launch {
                com.havamania.ui.theme.ThemeManager.getAssistantTone(getApplication(), uid).collect { tone ->
                    _assistantTone.value = tone
                    updateRecommendation()
                }
            }

            // Notifications observation
            launch {
                notificationRepository.getUnreadCount(uid)
                    .catch { emit(0) }
                    .collect { _unreadNotificationCount.value = it }
            }

            // City and Location Mode observation combined for better performance
            launch {
                ThemeManager.getLocationMode(getApplication(), uid).combine(
                    ThemeManager.getDefaultCity(getApplication(), uid)
                ) { mode, city ->
                    mode to city
                }.collect { (mode, city) ->
                    if (uid != currentUid) return@collect
                    locationJob?.cancel()
                    _locationMode.value = mode
                    if (BuildConfig.DEBUG) {
                        Log.d("WeatherVM", "Data update: mode=$mode, city=${city?.name}")
                    }

                    if (mode == LocationMode.AUTO) {
                        refreshWeatherWithCurrentLocation()
                    } else if (city != null) {
                        fetchWeather(city.latitude, city.longitude, city.name, city.district)
                    } else if (uid == "legacy") {
                        // Guest user starts with Balıkesir if no default set
                        fetchWeather(39.6484, 27.8826, "Balıkesir")
                    } else {
                        _uiState.value = WeatherUiState.NoCity
                    }
                }
            }

            // Safety check: If nothing emitted NoCity or Success after a while, check again
            launch {
                kotlinx.coroutines.delay(5000) // Reduced to 5s
                if (_uiState.value is WeatherUiState.Loading) {
                    val currentCity = ThemeManager.getDefaultCity(getApplication(), uid).firstOrNull()
                    if (currentCity == null && uid != "legacy") {
                        _uiState.value = WeatherUiState.NoCity
                    }
                }
            }
        }
    }

    fun refreshWeatherWithCurrentLocation() {
        locationJob?.cancel()
        val uid = currentUid
        locationJob = viewModelScope.launch {
            val city = locationTracker.getCurrentCity()
            if (uid != currentUid) return@launch
            if (city != null) {
                fetchWeather(city.latitude, city.longitude, city.name, city.admin1)
            } else {
                // If failed, fall back to default city from settings
                val defaultCity = com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication(), uid).firstOrNull()
                if (uid != currentUid) return@launch
                if (defaultCity != null) {
                    fetchWeather(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
                } else {
                    _uiState.value = WeatherUiState.NoCity
                }
            }
        }
    }

    fun fetchWeather(lat: Double, lon: Double, cityName: String, districtName: String? = null) {
        // Business Rule 5: Deduplication
        if (fetchJob?.isActive == true && lastLat == lat && lastLon == lon) return

        lastLat = lat
        lastLon = lon
        lastCity = cityName
        lastDistrict = districtName

        fetchJob?.cancel()
        fetchJob = accountTasks.launch { ownerUid ->
            _uiState.value = WeatherUiState.Loading

            try {
                // Wrap in timeout to prevent infinite loading
                kotlinx.coroutines.withTimeout(20000) {
                    repository.getWeatherData(lat, lon, cityName, districtName)
                        .catch { e ->
if (BuildConfig.DEBUG) {
                                android.util.Log.e("WeatherVM", "Fetch error", e)
}
                            _uiState.value = WeatherUiState.Error(
                                if (!isOnline.value) "İnternet bağlantısı kurulamadı. Lütfen bağlantınızı kontrol edin."
                                else "Hava durumu sunucularına şu an ulaşılamıyor. Lütfen daha sonra tekrar deneyin."
                            )
                        }
                        .collect { data ->
                            _uiState.value = WeatherUiState.Success(data)

                            // Calculate Smart Alerts
                            launch {
                                val config = com.havamania.ui.theme.ThemeManager.getSmartAlertConfig(getApplication(), ownerUid).first()
                                _smartAlerts.value = SmartAlertEngine.generateAlerts(data, config, ownerUid)
                            }

                            selectInitialForecast(data)

                            updateRecommendation()
                        }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = WeatherUiState.Error("Zaman aşımı: Hava durumu verileri çok uzun sürdü.")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = WeatherUiState.Error("Hava durumu yüklenirken bir hata oluştu.")
            }
        }
    }

    fun refreshWeather() {
        if (!isOnline.value || _isRefreshing.value) {
            return
        }

        fetchJob?.cancel()
        fetchJob = accountTasks.launch { ownerUid ->
            _isRefreshing.value = true
            try {
            repository.getWeatherData(lastLat, lastLon, lastCity, lastDistrict, forceRefresh = true)
                .catch { e ->
                    _isRefreshing.value = false
                }
                .collect { data ->
                    _uiState.value = WeatherUiState.Success(data)

                    // Calculate Smart Alerts
                    launch {
                        val config = com.havamania.ui.theme.ThemeManager.getSmartAlertConfig(getApplication(), ownerUid).first()
                        _smartAlerts.value = SmartAlertEngine.generateAlerts(data, config, ownerUid)
                    }

                    selectInitialForecast(data)

                    _isRefreshing.value = false
                    updateRecommendation()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun selectInitialForecast(data: WeatherData) {
        val now = java.time.Instant.now()
        val today = ForecastTime.localNow(data.timezone, now).toLocalDate()
        val days = ForecastTime.dailyFromToday(data, now)
        val day = days.firstOrNull { it.date == today.toString() } ?: days.firstOrNull()
        val date = day?.let { LocalDate.parse(it.date) } ?: today
        _selectedForecastDate.value = date
        _selectedDailyForecast.value = day
        _selectedHourlyWeather.value = ForecastTime.hoursForDate(data, date, now).firstOrNull()
    }

    fun selectDailyForecast(day: DailyForecast) {
        val date = try { LocalDate.parse(day.date) } catch (_: Exception) { return }
        _selectedForecastDate.value = date
        _selectedDailyForecast.value = day
        val data = (_uiState.value as? WeatherUiState.Success)?.data ?: return
        _selectedHourlyWeather.value = ForecastTime.hoursForDate(data, date).firstOrNull()
    }

    fun selectHour(hour: HourlyWeather?) {
        _selectedHourlyWeather.value = hour
    }

    fun updateRecommendation(
        interests: Set<String>? = null,
        aboutMe: String? = null,
        personalization: PersonalizationProfile? = null
    ) {
        if (interests != null) {
            currentUserInterests = interests
        }
        if (aboutMe != null) {
            currentUserAboutMe = aboutMe
        }
        if (personalization != null) {
            currentPersonalization = personalization
        }

        val currentUiState = _uiState.value
        if (currentUiState is WeatherUiState.Success) {
            val weather = currentUiState.data

            accountTasks.launch { _ ->
                _todayRecommendation.value = RecommendationEngine.generateTodayRecommendation(
                    weatherData = weather,
                    userInterests = currentUserInterests,
                    aboutMe = currentUserAboutMe,
                    tone = _assistantTone.value,
                    personalization = currentPersonalization
                )
            }
        }
    }

    fun searchCity(query: String) {
        searchJob?.cancel()
        _citySearchStatus.value = LocationSearchStatus.IDLE
        _citySuggestions.value = emptyList()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) {
            return
        }
        _citySearchStatus.value = LocationSearchStatus.LOADING
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            val result = fetchLocationSuggestions { repository.searchCityOrThrow(normalizedQuery) }
            coroutineContext.ensureActive()
            _citySuggestions.value = result.items
            _citySearchStatus.value = result.status
        }
    }
}
