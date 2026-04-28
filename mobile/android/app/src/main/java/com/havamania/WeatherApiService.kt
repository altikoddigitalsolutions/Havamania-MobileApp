package com.havamania

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getFullWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,surface_pressure,visibility",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,precipitation_probability",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,uv_index_max,sunrise,sunset",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 10
    ): OpenMeteoResponse

    @GET
    suspend fun searchCity(
        @Url url: String = "https://geocoding-api.open-meteo.com/v1/search",
        @Query("name") cityName: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "tr",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}
