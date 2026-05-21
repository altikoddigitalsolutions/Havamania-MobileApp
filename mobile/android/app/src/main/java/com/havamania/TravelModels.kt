package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

/**
 * Modern Travel Planner Models
 */
enum class TripType(val label: String, val icon: ImageVector) {
    BUSINESS("İş", Icons.Rounded.BusinessCenter),
    VACATION("Tatil", Icons.Rounded.BeachAccess),
    FAMILY("Aile", Icons.Rounded.People),
    SPORTS("Spor", Icons.Rounded.Sports),
    CAMPING("Kamp", Icons.Rounded.Terrain),
    CULTURE("Kültür Gezisi", Icons.Rounded.Museum),
    NATURE("Doğa", Icons.Rounded.Nature),
    ROMANTIC("Romantik", Icons.Rounded.Favorite),
    GASTRONOMY("Gastronomi", Icons.Rounded.Restaurant),
    BEACH("Deniz Tatili", Icons.Rounded.Pool),
    WINTER("Kış Tatili", Icons.Rounded.AcUnit),
    ADVENTURE("Macera", Icons.Rounded.Hiking),
    PHOTOGRAPHY("Fotoğraf", Icons.Rounded.CameraAlt),
    SHOPPING("Alışveriş", Icons.Rounded.ShoppingBag),
    WEEKEND("Hafta Sonu Kaçamağı", Icons.Rounded.Weekend),
    HEALTH("Sağlık / Spa", Icons.Rounded.Spa),
    EVENT("Festival / Etkinlik", Icons.Rounded.Event),
    ROAD_TRIP("Yolculuk / Road Trip", Icons.Rounded.DirectionsCar),
    OTHER("Diğer", Icons.Rounded.MoreHoriz)
}

enum class TravelWeatherAnalysisStatus {
    TOO_EARLY,
    READY,
    LOADING,
    ANALYZED,
    UPDATED,
    ERROR
}

@Serializable
data class ForecastSnapshot(
    val precipitationProbability: Int?,
    val minTemp: Double?,
    val maxTemp: Double?,
    val windSpeed: Double?,
    val uvIndex: Double?,
    val conditionSummary: String?,
    val generatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class TravelNotificationData(
    val travelId: String,
    val destination: String,
    val travelStartDate: String,
    val travelEndDate: String,
    val daysLeft: Int,
    val weatherSummary: String?,
    val rainProbability: Int?,
    val minTemp: Double?,
    val maxTemp: Double?,
    val windRisk: String?,
    val previousAnalysisSummary: String? = null,
    val comparisonText: String? = null,
    val recommendedItems: List<String> = emptyList()
)

@Serializable
data class TravelHistorySummary(
    val averageTemp: Int?,
    val minTemp: Int?,
    val maxTemp: Int?,
    val rainyDays: Int,
    val sunnyDays: Int,
    val cloudyDays: Int,
    val riskDayText: String,
    val comfortScore: Int,
    val summaryText: String,
    val packingAdvice: String,
    val nextTripAdvice: String,
    val durationDays: Int
)

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val tripType: TripType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
    val weatherSummary: String? = null,
    val aiSuggestion: String? = null,
    val isAnalyzing: Boolean = false,
    val lastWeatherAnalysisText: String? = null,
    val lastWeatherAnalysisDate: Long? = null,
    val lastForecastSnapshot: ForecastSnapshot? = null,
    val previousForecastSnapshot: ForecastSnapshot? = null,
    val nextAnalysisEligibleDate: Long? = null,
    val weatherAnalysisStatus: TravelWeatherAnalysisStatus = TravelWeatherAnalysisStatus.TOO_EARLY,
    val isArchived: Boolean = false
)
