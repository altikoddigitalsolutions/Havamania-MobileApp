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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import com.havamania.ui.theme.AssistantTone
import com.havamania.ui.theme.LocationMode
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

internal suspend fun resolveAutomaticWeatherCity(
    savedCity: GeocodingResultDto?,
    locate: suspend () -> GeocodingResultDto?,
    display: (GeocodingResultDto?) -> Unit,
    offerCityPicker: (() -> Unit)? = null,
    waitForPicker: suspend () -> Unit = { delay(5_000) }
): Unit = coroutineScope {
    if (savedCity != null) display(savedCity)
    val picker = if (savedCity == null && offerCityPicker != null) launch {
        waitForPicker()
        ensureActive()
        offerCityPicker()
    } else null
    val currentCity = try {
        locate()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    } finally {
        picker?.cancel()
    }
    currentCoroutineContext().ensureActive()
    if (savedCity == null || (currentCity != null && currentCity != savedCity)) display(currentCity)
}

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

    private data class WeatherSelection(
        val uid: String,
        val latitude: Double,
        val longitude: Double,
        val city: String,
        val district: String?
    )

    private var weatherSelection: WeatherSelection? = null

    private var fetchJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null
    private val accountTasks = AccountTaskScope(viewModelScope) { currentUid }
    private var locationJob: kotlinx.coroutines.Job? = null

    private val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        accountTasks.reset()
        val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
        if (BuildConfig.DEBUG) Log.d("WeatherVM", "Auth changed. Re-initializing weather.")

        // Clear UI state to prevent showing old user's data
        fetchJob?.cancel()
        weatherSelection = null
        locationJob?.cancel()
        searchJob?.cancel()
        _isRefreshing.value = false
        _citySearchStatus.value = LocationSearchStatus.IDLE
        _citySuggestions.value = emptyList()
        currentUserInterests = emptySet()
        currentUserAboutMe = null
        currentPersonalization = null
        repository.onAccountChanged(firebaseAuth.currentUser?.uid)
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
                ThemeManager.getAssistantTone(getApplication(), uid).catch { emit(AssistantTone.DENGELI) }.collect { tone ->
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

            launch {
                ThemeManager.observeWeatherLocation(getApplication(), uid).catch {
                    showWeatherPreferenceError(uid)
                }.collect { (mode, city) ->
                    if (uid != currentUid) return@collect
                    locationJob?.cancel()
                    _locationMode.value = mode
                    if (BuildConfig.DEBUG) {
                        Log.d("WeatherVM", "Data update: mode=$mode, city=${city?.name}")
                    }

                    if (mode == LocationMode.AUTO) {
                        refreshWeatherWithCurrentLocation(city)
                    } else if (city != null) {
                        loadSelectedCity(city.latitude, city.longitude, city.name, city.district)
                    } else if (uid == "legacy") {
                        // Guest user starts with Balıkesir if no default set
                        loadSelectedCity(39.6484, 27.8826, "Balıkesir")
                    } else {
                        clearWeatherSelection()
                    }
                }
            }

        }
    }

    private fun showWeatherPreferenceError(uid: String) {
        if (uid == currentUid && weatherSelection == null && _uiState.value is WeatherUiState.Loading) {
            _uiState.value = WeatherUiState.Error(
                "Şehir ayarları okunamadı. Lütfen tekrar deneyin.", retryNeedsInternet = false
            )
        }
    }

    private fun refreshWeatherWithCurrentLocation(savedCity: GeocodingResultDto?) {
        locationJob?.cancel()
        val uid = currentUid
        locationJob = viewModelScope.launch {
            resolveAutomaticWeatherCity(
                savedCity,
                locate = { locationTracker.getCurrentCity() },
                display = { city ->
                    if (uid == currentUid) {
                        if (city != null) {
                            loadSelectedCity(city.latitude, city.longitude, city.getSafeCity(), city.getSafeDistrict())
                        } else {
                            clearWeatherSelection()
                        }
                    }
                },
                offerCityPicker = if (uid == "legacy") null else ({
                    if (uid == currentUid && weatherSelection == null && _uiState.value is WeatherUiState.Loading) {
                        clearWeatherSelection()
                    }
                })
            )
        }
    }

    fun fetchWeather(lat: Double, lon: Double, cityName: String, districtName: String? = null) {
        locationJob?.cancel()
        loadSelectedCity(lat, lon, cityName, districtName)
    }

    private fun loadSelectedCity(lat: Double, lon: Double, cityName: String, districtName: String? = null) {
        val selection = WeatherSelection(currentUid, lat, lon, cityName, districtName)
        if (fetchJob?.isActive == true && weatherSelection == selection) return
        weatherSelection = selection
        loadWeather(selection, forceRefresh = false)
    }

    private fun clearWeatherSelection() {
        fetchJob?.cancel()
        weatherSelection = null
        _isRefreshing.value = false
        _uiState.value = WeatherUiState.NoCity
    }

    fun refreshWeather() {
        val selection = weatherSelection?.takeIf { it.uid == currentUid }
        if (selection == null) {
            if (_uiState.value is WeatherUiState.Error) {
                _uiState.value = WeatherUiState.Loading
                observeUserData(currentUid)
            }
            return
        }
        if (!isOnline.value || _isRefreshing.value) return
        loadWeather(selection, forceRefresh = true)
    }

    private fun loadWeather(selection: WeatherSelection, forceRefresh: Boolean) {
        fetchJob?.cancel()
        _isRefreshing.value = forceRefresh
        fetchJob = accountTasks.launch { ownerUid ->
            if (!forceRefresh) _uiState.value = WeatherUiState.Loading
            try {
                val completed = withTimeoutOrNull(20000) {
                    var receivedForecast = false
                    repository.getWeatherData(
                        selection.latitude, selection.longitude, selection.city, selection.district,
                        forceRefresh = forceRefresh
                    ).collect { data ->
                        receivedForecast = true
                        _uiState.value = WeatherUiState.Success(data)

                        launch {
                            val config = ThemeManager.getSmartAlertConfig(getApplication(), ownerUid).first()
                            _smartAlerts.value = SmartAlertEngine.generateAlerts(data, config, ownerUid)
                        }
                        selectInitialForecast(data)
                        updateRecommendation()
                    }
                    receivedForecast
                }
                when (completed) {
                    null -> showWeatherError(selection, "Hava durumu isteği zaman aşımına uğradı. Lütfen tekrar deneyin.")
                    false -> showWeatherError(selection, "Hava durumu verisi alınamadı. Lütfen tekrar deneyin.")
                    true -> Unit
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                showWeatherError(selection,
                    if (isOnline.value) "Hava durumu şu anda alınamıyor. Lütfen tekrar deneyin."
                    else "İnternet bağlantısı kurulamadı. Lütfen bağlantınızı kontrol edin."
                )
            } finally {
                if (weatherSelection === selection) _isRefreshing.value = false
            }
        }
    }

    private fun showWeatherError(selection: WeatherSelection, message: String) {
        if (weatherSelection === selection && selection.uid == currentUid && _uiState.value !is WeatherUiState.Success) {
            _uiState.value = WeatherUiState.Error(message)
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
