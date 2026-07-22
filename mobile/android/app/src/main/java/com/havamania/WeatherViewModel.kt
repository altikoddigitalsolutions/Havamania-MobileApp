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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.havamania.ui.theme.AssistantTone

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
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val notificationRepository: NotificationRepository by lazy {
        val database = NotificationDatabase.getDatabase(application)
        NotificationRepository(database.notificationDao())
    }

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private var lastCity = "Balıkesir"
    private var lastDistrict: String? = null
    private var lastLat = 39.6484
    private var lastLon = 27.8826

    init {
        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid ?: "legacy"
                Log.d("WeatherVM", "Auth changed. Re-initializing for $newUid")
                _uiState.value = WeatherUiState.Loading
                _todayRecommendation.value = null
                observeUserData(newUid)
            }
        }
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

            // City observation
            launch {
                com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication(), uid).collect { defaultCity ->
                    Log.d("WeatherVM", "Default city observed for $uid: ${defaultCity?.name}")
                    if (defaultCity != null) {
                        fetchWeather(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
                    } else {
                        if (uid != "legacy") {
                            _uiState.value = WeatherUiState.NoCity
                        } else {
                            // Guest user starts with Balıkesir if no default set
                            fetchWeather(39.6484, 27.8826, "Balıkesir")
                        }
                    }
                }
            }

            // Safety check: If nothing emitted NoCity or Success after a while, check again
            launch {
                kotlinx.coroutines.delay(8000)
                if (_uiState.value is WeatherUiState.Loading) {
                    val currentCity = com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication(), uid).first()
                    if (currentCity == null && uid != "legacy") {
                        _uiState.value = WeatherUiState.NoCity
                    }
                }
            }
        }
    }

    fun fetchWeather(lat: Double, lon: Double, cityName: String, districtName: String? = null) {
        lastLat = lat
        lastLon = lon
        lastCity = cityName
        lastDistrict = districtName

        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            try {
                // Wrap in timeout to prevent infinite loading
                kotlinx.coroutines.withTimeout(20000) {
                    repository.getWeatherData(lat, lon, cityName, districtName)
                        .catch { e ->
                            android.util.Log.e("WeatherVM", "Fetch error", e)
                            _uiState.value = WeatherUiState.Error(
                                if (!isOnline.value) "İnternet bağlantınız yok. Lütfen kontrol edin."
                                else if (e is java.net.UnknownHostException || e is java.io.IOException) "Meteoroloji sunucularına şu an ulaşılamıyor. Lütfen birazdan tekrar dene."
                                else "Hava durumu verileri hazırlanırken beklenmedik bir durum oluştu."
                            )
                        }
                        .collect { data ->
                            _uiState.value = WeatherUiState.Success(data)
                            // Reset to today
                            val todayDate = LocalDate.now()
                            _selectedForecastDate.value = todayDate
                            val todayForecast = data.dailyForecast.find { it.date == todayDate.toString() } ?: data.dailyForecast.firstOrNull()
                            _selectedDailyForecast.value = todayForecast

                            // Initial hourly selection: current hour or next available
                            val nowHour = java.time.LocalTime.now().hour
                            val todayHourly = data.hourlyForecast.filter { it.fullTime.startsWith(todayDate.toString()) }
                            _selectedHourlyWeather.value = todayHourly.find {
                                val h = it.time.split(":")[0].toIntOrNull() ?: 0
                                h >= nowHour
                            } ?: todayHourly.firstOrNull()

                            updateRecommendation()
                        }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = WeatherUiState.Error("Zaman aşımı: Hava durumu verileri çok uzun sürdü.")
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Hava durumu yüklenirken bir hata oluştu.")
            }
        }
    }

    fun refreshWeather() {
        if (!isOnline.value) {
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            val cacheKey = if (lastDistrict != null) "$lastCity-$lastDistrict" else lastCity
            repository.clearCache(cacheKey)

            repository.getWeatherData(lastLat, lastLon, lastCity, lastDistrict)
                .catch { e ->
                    _isRefreshing.value = false
                }
                .collect { data ->
                    _uiState.value = WeatherUiState.Success(data)
                    val todayDate = LocalDate.now()
                    _selectedForecastDate.value = todayDate
                    val todayForecast = data.dailyForecast.find { it.date == todayDate.toString() } ?: data.dailyForecast.firstOrNull()
                    _selectedDailyForecast.value = todayForecast

                    // Initial hourly selection: current hour or next available
                    val nowHour = java.time.LocalTime.now().hour
                    val todayHourly = data.hourlyForecast.filter { it.fullTime.startsWith(todayDate.toString()) }
                    _selectedHourlyWeather.value = todayHourly.find {
                        val h = it.time.split(":")[0].toIntOrNull() ?: 0
                        h >= nowHour
                    } ?: todayHourly.firstOrNull()

                    _isRefreshing.value = false
                    updateRecommendation()
                }
        }
    }

    fun selectDailyForecast(day: DailyForecast) {
        val date = LocalDate.parse(day.date)
        _selectedForecastDate.value = date
        _selectedDailyForecast.value = day

        // Select first appropriate hour for the selected day
        val currentUiState = _uiState.value
        if (currentUiState is WeatherUiState.Success) {
            val dayHourly = currentUiState.data.hourlyForecast.filter {
                it.fullTime.startsWith(day.date)
            }

            if (day.isToday) {
                val nowHour = java.time.LocalTime.now().hour
                val futureHourly = dayHourly.filter {
                    val h = it.time.split(":")[0].toIntOrNull() ?: 0
                    h >= nowHour
                }
                _selectedHourlyWeather.value = futureHourly.firstOrNull()
            } else {
                _selectedHourlyWeather.value = dayHourly.firstOrNull()
            }
        }
    }

    fun selectHour(hour: HourlyWeather?) {
        _selectedHourlyWeather.value = hour
    }

    fun updateRecommendation(interests: Set<String>? = null, aboutMe: String? = null) {
        if (interests != null) {
            currentUserInterests = interests
        }
        if (aboutMe != null) {
            currentUserAboutMe = aboutMe
        }

        val currentUiState = _uiState.value
        if (currentUiState is WeatherUiState.Success) {
            val weather = currentUiState.data

            viewModelScope.launch {
                _todayRecommendation.value = RecommendationEngine.generateTodayRecommendation(
                    weatherData = weather,
                    userInterests = currentUserInterests,
                    aboutMe = currentUserAboutMe,
                    tone = _assistantTone.value
                )
            }
        }
    }

    fun searchCity(query: String) {
        if (query.length < 2) {
            _citySuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _citySuggestions.value = repository.searchCity(query)
        }
    }
}
