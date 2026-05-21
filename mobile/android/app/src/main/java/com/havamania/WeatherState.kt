package com.havamania

import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ana Hava Durumu Veri Modeli - Cache için Serializable yapıldı
 */
@Serializable
data class WeatherData(
    val cityName: String = "İstanbul",
    val districtName: String? = null,
    val temperature: String = "12°",
    val condition: String = "Parçalı Bulutlu",
    val weatherCode: Int = 0,
    val isDay: Boolean = true,
    val high: String = "17°",
    val low: String = "3°",
    val feelsLike: String = "8°",
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
    val timezone: String = "UTC",
    val solarNoon: String? = null,
    val windSpeed: Double? = null,
    val windGust: Double? = null,
    val windDirectionDegrees: Int? = null,
    val windDirectionLabel: String? = null,
    val windChill: Double? = null,
    val dewPoint: Double? = null,
    val precipitationProbability: Int? = null,
    val precipitationAmount: Double? = null,
    val cloudCover: Int? = null,
    val visibilityKm: Double? = null,
    val humidity: Int? = null,
    val pressure: Int? = null,
    val uvIndex: Int? = null,
    val weatherSuitabilityScore: Int = 100,
    val weatherSuitabilityText: String = "",
    val weatherSuitabilityDesc: String = "",
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val dailyForecast: List<DailyForecast> = emptyList(),
    val details: List<WeatherDetailData> = emptyList()
)

@Serializable
data class HourlyWeather(
    val time: String, // HH:mm format for display
    val fullTime: String = "", // ISO format for logic (YYYY-MM-DDTHH:mm)
    val temp: String,
    val condition: String = "Bulutlu",
    val weatherCode: Int = 0,
    val isDay: Boolean = true,
    val precipProb: String? = null,
    val iconName: String = "Cloud",
    val isSelected: Boolean = false
)

@Serializable
data class DailyForecast(
    val day: String, // Display name (e.g. "Monday")
    val date: String = "", // ISO date (YYYY-MM-DD)
    val minTemp: Int,
    val maxTemp: Int,
    val iconName: String = "WbSunny",
    val weatherCode: Int = 0,
    val isToday: Boolean = false
)

@Serializable
data class WeatherDetailData(
    val title: String,
    val value: String,
    val description: String,
    val iconName: String = "Thermostat",
    val accentColorHex: String = "#38BDF8",
    val progress: Float? = null,
    val isSelected: Boolean = false
)

enum class DayPhase { NIGHT, DAWN, DAY, EVENING }

sealed class WeatherCondition {
    object Clear : WeatherCondition()
    object MostlySunny : WeatherCondition()
    object PartlyCloudy : WeatherCondition()
    object Cloudy : WeatherCondition()
    object Rain : WeatherCondition()
    object Thunderstorm : WeatherCondition()
    object Snow : WeatherCondition()
    object Fog : WeatherCondition()
    object NightClear : WeatherCondition()
}

/**
 * Recommendation Models
 */
enum class RecommendationType {
    GENERAL, SPORT, TRAVEL, WARNING, COMFORT, OUTDOOR, HEALTH
}

enum class RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class HavamaniaRecommendation(
    val title: String = "HAVAMANIA ÖNERİSİ",
    val message: String,
    val type: RecommendationType,
    val highlightedWords: List<String>,
    val priority: RecommendationPriority
)

/**
 * UI State Tanımı (Sealed Class ile)
 */
sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
