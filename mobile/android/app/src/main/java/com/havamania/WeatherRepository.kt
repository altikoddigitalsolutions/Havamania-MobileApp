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
     * Eğer cache yeterince yeniyse (örneğin < 15 dk) network isteği atmaz.
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

        // 1. Cache'den oku
        val cachedEntity = weatherDao.getCachedWeather(cacheKey)
        var isCacheValid = false

        if (cachedEntity != null) {
            try {
                val cachedData = json.decodeFromString<WeatherData>(cachedEntity.jsonData)
                val isRecent = (System.currentTimeMillis() - cachedEntity.timestamp) < cacheTimeoutMillis

                // Eğer kritik veriler null değilse cache'i dön
                if (cachedData.humidity != null && cachedData.pressure != null && cachedData.windSpeed != null) {
                    emit(cachedData)
                    hasEmitted = true
                    if (isRecent && !forceRefresh) {
                        isCacheValid = true
                    }
                }
            } catch (e: Exception) {
                // Cache bozuksa görmezden gel
            }
        }

        // 2. Network'ten çek (Sadece forceRefresh true ise veya cache eskiyse/yoksa)
        if (!isCacheValid || forceRefresh) {
            try {
                val currentFields = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,surface_pressure,visibility,dew_point_2m,precipitation,cloud_cover,uv_index"
                val dailyFields = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,sunrise,sunset,wind_speed_10m_max,wind_gusts_10m_max"

                val response = apiService.getFullWeather(
                    lat = lat,
                    lon = lon,
                    current = currentFields,
                    daily = dailyFields
                )
                val domainData = WeatherMapper.mapToDomain(response, cityName, districtName)

                // 3. Cache'i güncelle
                val jsonString = json.encodeToString(domainData)
                weatherDao.insertWeather(WeatherCacheEntity(cacheKey, jsonString))

                // 4. Güncel veriyi dön
                emit(domainData)
                hasEmitted = true
            } catch (e: Exception) {
                // Eğer hiçbir veri dönemediysek (ne cache ne network) hata fırlat
                if (!hasEmitted) throw e
            }
        }
    }.onEach { _currentWeatherState.value = it }
}
