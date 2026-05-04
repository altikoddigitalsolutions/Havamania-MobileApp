package com.havamania

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Ana Hava Durumu Veri Modeli - Cache için Serializable yapıldı
 */
@Serializable
data class WeatherData(
    val cityName: String = "İstanbul",
    val temperature: String = "12°",
    val condition: String = "Parçalı Bulutlu",
    val weatherCode: Int = 0,
    val isDay: Boolean = true,
    val high: String = "17°",
    val low: String = "3°",
    val feelsLike: String = "8°",
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
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
    val hourlyForecast: List<HourlyForecastData> = emptyList(),
    val dailyForecast: List<DailyForecastData> = emptyList(),
    val details: List<WeatherDetailData> = emptyList()
)

@Serializable
data class HourlyForecastData(
    val time: String,
    val temp: String,
    val condition: String = "Bulutlu",
    val weatherCode: Int = 0,
    val isDay: Boolean = true,
    val precipProb: String? = null,
    val iconName: String = "Cloud",
    val isSelected: Boolean = false
)

@Serializable
data class DailyForecastData(
    val day: String,
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

/**
 * AI Mesaj Modelleri - Merkezi ve Serileştirilebilir
 * Not: Java UUID serileştirme sorunları çıkarabildiği için id string olarak yönetilir.
 */
@Serializable
data class StructuredAiContent(
    val summary: String? = null,
    val suggestions: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val packingTips: List<String> = emptyList(),
    val rawText: String? = null
)

@Serializable
data class ChatMessage(
    val id: String = "",
    val text: String? = null,
    val structuredContent: StructuredAiContent? = null,
    val isUser: Boolean,
    val timestamp: Long = 0L
)

/**
 * UI State Tanımı (Sealed Class ile)
 */
sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
