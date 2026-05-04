package com.havamania

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    suspend fun clearCache(cityName: String) {
        try {
            weatherDao.deleteWeather(cityName)
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    /**
     * Önce cache verisini döner, sonra API'den güncel veriyi çeker
     */
    fun getWeatherData(lat: Double, lon: Double, cityName: String, districtName: String? = null): Flow<WeatherData> = flow {
        var hasEmitted = false
        // unique key for cache
        val cacheKey = if (districtName != null) "$cityName-$districtName" else cityName

        // 1. Cache'den oku
        val cachedEntity = weatherDao.getCachedWeather(cacheKey)
        if (cachedEntity != null) {
            try {
                val cachedData = json.decodeFromString<WeatherData>(cachedEntity.jsonData)
                // Eğer kritik veriler null değilse cache'i dön
                if (cachedData.humidity != null && cachedData.pressure != null && cachedData.windSpeed != null) {
                    emit(cachedData)
                    hasEmitted = true
                }
            } catch (e: Exception) {
                // Cache bozuksa görmezden gel
            }
        }

        // 2. Network'ten çek
        try {
            val currentFields = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_gusts_10m,wind_direction_10m,surface_pressure,visibility,dew_point_2m,precipitation,cloud_cover"
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
            // Eğer hiçbir veri dönemediysek hata fırlat
            if (!hasEmitted) throw e
        }
    }
}
