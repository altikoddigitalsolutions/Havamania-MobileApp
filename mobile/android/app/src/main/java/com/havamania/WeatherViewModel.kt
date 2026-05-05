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

/**
 * Hava durumu ekranı için ViewModel - Network Awareness eklendi
 */
class WeatherViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: WeatherRepository by lazy {
        val database = WeatherDatabase.getDatabase(application)
        WeatherRepository(weatherDao = database.weatherDao())
    }

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

    private val _selectedHour = MutableStateFlow<HourlyForecastData?>(null)
    val selectedHour: StateFlow<HourlyForecastData?> = _selectedHour.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

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
                }
        }
    }

    fun refreshWeather() {
        if (!isOnline.value) {
            return
        }

        viewModelScope.launch {
            _isRefreshing.value = true
            // unique key for cache
            val cacheKey = if (lastDistrict != null) "$lastCity-$lastDistrict" else lastCity
            repository.clearCache(cacheKey)

            repository.getWeatherData(lastLat, lastLon, lastCity, lastDistrict)
                .catch { e ->
                    _isRefreshing.value = false
                }
                .collect { data ->
                    _uiState.value = WeatherUiState.Success(data)
                    _selectedHour.value = null // Reset selection on refresh
                    _isRefreshing.value = false
                }
        }
    }

    fun selectHour(hour: HourlyForecastData?) {
        _selectedHour.value = hour
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
