package com.havamania

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentWeatherDto? = null,
    val daily: DailyDto? = null,
    val hourly: HourlyDto? = null
)

@Serializable
data class CurrentWeatherDto(
    val time: String,
    @SerialName("temperature_2m")
    val temperature: Double,
    @SerialName("relative_humidity_2m")
    val humidity: Int? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("wind_speed_10m")
    val windSpeed: Double? = null,
    @SerialName("surface_pressure")
    val pressure: Double? = null,
    @SerialName("visibility")
    val visibility: Double? = null
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
    @SerialName("temperature_2m_max")
    val tempMax: List<Double>,
    @SerialName("temperature_2m_min")
    val tempMin: List<Double>,
    @SerialName("uv_index_max")
    val uvIndexMax: List<Double>? = null,
    @SerialName("sunrise")
    val sunrise: List<String> = emptyList(),
    @SerialName("sunset")
    val sunset: List<String> = emptyList()
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature: List<Double>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
    @SerialName("precipitation_probability")
    val precipitationProbability: List<Int>? = null
)
