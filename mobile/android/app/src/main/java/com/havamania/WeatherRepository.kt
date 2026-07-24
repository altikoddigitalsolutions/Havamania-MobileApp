package com.havamania

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Hava durumu verilerini sağlayan Repository - Cache ve Network koordinasyonu
 */
class WeatherRepository(
    private val apiService: WeatherApiService = NetworkModule.apiService,
    private val weatherDao: WeatherDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _currentWeatherState = MutableStateFlow<WeatherData?>(null)
    val currentWeatherState: StateFlow<WeatherData?> = _currentWeatherState.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: WeatherRepository? = null

        fun getInstance(application: android.app.Application): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                val database = WeatherDatabase.getDatabase(application)
                val instance = WeatherRepository(weatherDao = database.weatherDao())
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun clearCache(cityName: String) {
        try {
            weatherDao.deleteWeather(cityName)
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    fun clearCurrentWeather() {
        _currentWeatherState.value = null
    }

    /**
     * Şehir arama fonksiyonu
     */
    suspend fun searchCity(query: String): List<GeocodingResultDto> {
        return try {
            val response = apiService.searchCity(cityName = query)
            response.results ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Önce cache verisini döner, sonra API'den güncel veriyi çeker.
     * Stale-while-revalidate stratejisi (Business Rule 6)
     */
    fun getWeatherData(
        lat: Double,
        lon: Double,
        cityName: String,
        districtName: String? = null,
        forceRefresh: Boolean = false
    ): Flow<WeatherData> = flow {
        var hasEmitted = false
        val cacheKey = if (districtName != null) "$cityName-$districtName" else cityName
        val cacheTimeoutMillis = 15 * 60 * 1000L // 15 dakika

        // 1. Her zaman önce Cache'den oku ve varsa anında dön (Business Rule 7: Offline-first)
        val cachedEntity = weatherDao.getCachedWeather(cacheKey)
        var isCacheFresh = false

        if (cachedEntity != null) {
            try {
                val cachedData = json.decodeFromString<WeatherData>(cachedEntity.jsonData)
                val age = System.currentTimeMillis() - cachedEntity.timestamp
                isCacheFresh = age < cacheTimeoutMillis

                // KURAL 6.1: Eski de olsa cache verisini anında göster
                emit(cachedData)
                hasEmitted = true
                android.util.Log.d("WeatherRepo", "Cache emitted for $cacheKey (Age: ${age/1000}s, Fresh: $isCacheFresh)")
            } catch (e: Exception) {
                android.util.Log.e("WeatherRepo", "Cache decode failed", e)
            }
        }

        // 2. Network'ten çek (Cache taze değilse VEYA forceRefresh ise)
        if (!isCacheFresh || forceRefresh) {
            try {
                android.util.Log.i("WeatherRepo", "Fetching from Network for $cacheKey (Reason: ${if (forceRefresh) "Force" else "Stale"})")
                val currentFields = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,surface_pressure,visibility,dew_point_2m,precipitation,cloud_cover,uv_index"
                val dailyFields = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset,wind_speed_10m_max,wind_gusts_10m_max"

                val response = apiService.getFullWeather(
                    lat = lat,
                    lon = lon,
                    current = currentFields,
                    daily = dailyFields
                )
                val domainData = WeatherMapper.mapToDomain(response, cityName, districtName)

                // 3. Cache'i ve State'i güncelle
                val jsonString = json.encodeToString(domainData)
                weatherDao.insertWeather(WeatherCacheEntity(cacheKey, jsonString))

                emit(domainData)
                hasEmitted = true
            } catch (e: Exception) {
                android.util.Log.e("WeatherRepo", "Network fetch failed", e)
                // Eğer hiçbir veri dönemediysek (ne cache ne network) hata fırlat
                if (!hasEmitted) throw e
            }
        }
    }.onEach { _currentWeatherState.value = it }
}
