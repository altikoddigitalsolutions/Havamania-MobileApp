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
    val time: String? = null,
    val interval: Int? = null,
    @SerialName("temperature_2m")
    val temperature: Double? = null,
    @SerialName("relative_humidity_2m")
    val humidity: Int? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: Double? = null,
    @SerialName("weather_code")
    val weatherCode: Int? = null,
    @SerialName("is_day")
    val isDay: Int? = null,
    @SerialName("wind_speed_10m")
    val windSpeed: Double? = null,
    @SerialName("surface_pressure")
    val pressure: Double? = null,
    @SerialName("visibility")
    val visibility: Double? = null,
    @SerialName("wind_gusts_10m")
    val windGusts: Double? = null,
    @SerialName("wind_direction_10m")
    val windDirection: Double? = null,
    @SerialName("dew_point_2m")
    val dewPoint: Double? = null,
    @SerialName("precipitation")
    val precipitation: Double? = null,
    @SerialName("cloud_cover")
    val cloudCover: Int? = null
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
    @SerialName("precipitation_probability_max")
    val precipProbMax: List<Int>? = null,
    @SerialName("uv_index_max")
    val uvIndexMax: List<Double>? = null,
    @SerialName("sunrise")
    val sunrise: List<String> = emptyList(),
    @SerialName("sunset")
    val sunset: List<String> = emptyList(),
    @SerialName("wind_speed_10m_max")
    val windSpeedMax: List<Double> = emptyList(),
    @SerialName("wind_gusts_10m_max")
    val windGustsMax: List<Double> = emptyList()
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
