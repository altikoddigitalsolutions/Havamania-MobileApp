package com.havamania

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Route measurements can be absent at individual hours without discarding other hazards. */
@Serializable
data class RouteForecastResponse(val latitude: Double? = null, val longitude: Double? = null,
    val timezone: String? = null, val hourly: RouteHourlyDto? = null)

@Serializable
data class RouteHourlyDto(
    val time: List<String?> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @SerialName("apparent_temperature") val apparentTemperature: List<Double?>? = null,
    @SerialName("wind_speed_10m") val windSpeed: List<Double?>? = null,
    @SerialName("relative_humidity_2m") val humidity: List<Int?>? = null,
    @SerialName("is_day") val isDay: List<Int?>? = null,
    @SerialName("wind_gusts_10m") val windGusts: List<Double?>? = null
)
