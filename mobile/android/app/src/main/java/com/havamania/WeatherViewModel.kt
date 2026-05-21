package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Hava durumu ekranı için ViewModel - Network Awareness eklendi
 */
class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = WeatherRepository.getInstance(application)

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

    private var currentUserInterests: Set<String> = emptySet()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val notificationRepository: NotificationRepository by lazy {
        val database = NotificationDatabase.getDatabase(application)
        NotificationRepository(database.notificationDao())
    }

    val unreadNotificationCount: StateFlow<Int> = notificationRepository.unreadCount
        .catch { emit(0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private var lastCity = "İstanbul"
    private var lastDistrict: String? = null
    private var lastLat = 41.0082
    private var lastLon = 28.9784

    init {
        viewModelScope.launch {
            com.havamania.ui.theme.ThemeManager.getDefaultCity(getApplication()).collect { defaultCity ->
                fetchWeather(defaultCity.latitude, defaultCity.longitude, defaultCity.name, defaultCity.district)
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
            repository.getWeatherData(lat, lon, cityName, districtName)
                .catch { e ->
                    _uiState.value = WeatherUiState.Error(
                        if (!isOnline.value) "İnternet bağlantınız yok. Lütfen kontrol edin."
                        else e.message ?: "Veriler alınırken bir hata oluştu."
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

    fun updateRecommendation(interests: Set<String>? = null) {
        if (interests != null) {
            currentUserInterests = interests
        }

        val currentUiState = _uiState.value
        if (currentUiState is WeatherUiState.Success) {
            val weather = currentUiState.data

            viewModelScope.launch {
                _todayRecommendation.value = RecommendationEngine.generateTodayRecommendation(
                    weatherData = weather,
                    userInterests = currentUserInterests
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
