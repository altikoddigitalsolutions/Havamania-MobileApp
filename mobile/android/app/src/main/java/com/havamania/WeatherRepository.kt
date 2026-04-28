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

    /**
     * Önce cache verisini döner, sonra API'den güncel veriyi çeker
     */
    fun getWeatherData(lat: Double, lon: Double, cityName: String): Flow<WeatherData> = flow {
        // 1. Cache'den oku
        val cachedEntity = weatherDao.getCachedWeather(cityName)
        if (cachedEntity != null) {
            try {
                val cachedData = json.decodeFromString<WeatherData>(cachedEntity.jsonData)
                emit(cachedData)
            } catch (e: Exception) {
                // Cache bozuksa görmezden gel
            }
        }

        // 2. Network'ten çek
        try {
            val response = apiService.getFullWeather(lat, lon)
            val domainData = WeatherMapper.mapToDomain(response, cityName)

            // 3. Cache'i güncelle
            val jsonString = json.encodeToString(domainData)
            weatherDao.insertWeather(WeatherCacheEntity(cityName, jsonString))

            // 4. Güncel veriyi dön
            emit(domainData)
        } catch (e: Exception) {
            // Eğer cache yoksa ve network hatası varsa hata fırlatılır (catch bloklarında yakalanır)
            if (cachedEntity == null) throw e
        }
    }
}
