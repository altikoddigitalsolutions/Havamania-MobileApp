package com.havamania

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.*

/**
 * Modern Travel Planner Models
 */
const val TRIP_ANALYSIS_WINDOW_DAYS = 10

enum class TripType(val label: String, val icon: ImageVector) {
    BUSINESS("💼 İş", Icons.Rounded.BusinessCenter),
    VACATION("🏖 Tatil", Icons.Rounded.BeachAccess),
    FAMILY("👨‍👩‍👧 Aile", Icons.Rounded.People),
    SPORTS("🏃 Spor", Icons.Rounded.Sports),
    CAMPING("⛺ Kamp", Icons.Rounded.Terrain),
    CULTURE("🏛 Kültür Gezisi", Icons.Rounded.Museum),
    NATURE("🌿 Doğa", Icons.Rounded.Nature),
    ROMANTIC("💖 Romantik", Icons.Rounded.Favorite),
    GASTRONOMY("🍽 Gastronomi", Icons.Rounded.Restaurant),
    BEACH("🌊 Deniz Tatili", Icons.Rounded.Pool),
    WINTER("❄️ Kış Tatili", Icons.Rounded.AcUnit),
    ADVENTURE("🧗 Macera", Icons.Rounded.Hiking),
    PHOTOGRAPHY("📷 Fotoğraf", Icons.Rounded.CameraAlt),
    SHOPPING("🛍 Alışveriş", Icons.Rounded.ShoppingBag),
    WEEKEND("🗓 Hafta Sonu", Icons.Rounded.Weekend),
    HEALTH("🧘 Sağlık / Spa", Icons.Rounded.Spa),
    EVENT("🎉 Festival", Icons.Rounded.Event),
    ROAD_TRIP("🚗 Yolculuk", Icons.Rounded.DirectionsCar),
    OTHER("✨ Diğer", Icons.Rounded.MoreHoriz)
}

enum class TripStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED
}

enum class TravelWeatherAnalysisStatus {
    WAITING_FOR_WINDOW,
    LOADING,
    WEATHER_READY_ANALYSIS_READY,
    WEATHER_READY_AI_FAILED,
    WEATHER_PARTIAL_READY,
    WEATHER_FAILED
}

@Keep
@Serializable
@IgnoreExtraProperties
data class ForecastSnapshot(
    val precipitationProbability: Int? = null,
    val minTemp: Double? = null,
    val maxTemp: Double? = null,
    val windSpeed: Double? = null,
    val uvIndex: Double? = null,
    val cloudCover: Int? = null,
    val feelsLike: Double? = null,
    val conditionSummary: String? = null,
    val weatherCode: Int? = 0,
    val travelScore: Int? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

@Keep
@Serializable
@IgnoreExtraProperties
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

@Keep
@Serializable
@IgnoreExtraProperties
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

@Keep
@Serializable
@IgnoreExtraProperties
data class TravelWeatherAnalysis(
    val id: String = UUID.randomUUID().toString(),
    val tripId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val travelScore: Int = 0,
    val rainRiskPercent: Int? = null,
    val windRiskPercent: Int? = null,
    val uvRiskPercent: Int? = null,
    val averageTemperature: Double = 0.0,
    val summary: String = "",
    val recommendation: String = "",
    val comparisonText: String? = null,
    val previousAnalysisId: String? = null
)

data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "legacy",
    val city: String,
    /** Seçilen yer bir ilçe/semt ise adı; il merkezi seçildiyse null. */
    val district: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    // Opsiyonel kalkış noktası. Boşsa güzergâh cihazın mevcut konumundan başlar.
    val originCity: String? = null,
    val originDistrict: String? = null,
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,

    val tripType: TripType = TripType.OTHER,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastAnalysisAt: Long? = null,
    val archivedAt: Long? = null,

    // AI & Weather Data
    val weatherSummary: String? = null,
    val packingAdvice: String? = null,
    val mustSee: String? = null,
    val foodAdvice: String? = null,
    val localAdvice: String? = null,
    val aiSuggestion: String? = null, // Full raw suggestion
    val comfortScore: Int? = null,

    // User Input
    val userNote: String? = null,
    val userRating: Int? = 0,

    // Status & Logic
    val isAnalyzing: Boolean = false,
    val weatherAnalysisStatus: TravelWeatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW,
    val isArchived: Boolean = false,
    val isDemo: Boolean = false,

    // Detailed History
    val analyses: List<TravelWeatherAnalysis> = emptyList(),
    val lastForecastSnapshot: ForecastSnapshot? = null,
    val previousForecastSnapshot: ForecastSnapshot? = null,

    // Notifications tracking
    val lastDailyNotificationDate: String? = null, // YYYY-MM-DD

    /** Yola çıkış saati (HH:mm formatında). */
    val departureTime: String? = null
) {
    /** Kartlarda ve bot bağlamında gösterilecek ad: ilçe seçildiyse "İlçe, İl". */
    val displayName: String
        get() = district?.takeIf { it.isNotBlank() && it != city }?.let { "$it, $city" } ?: city

    /** Kalkış noktası seçildiyse okunabilir adı. */
    val originDisplayName: String?
        get() = originCity?.let { city ->
            originDistrict?.takeIf { it.isNotBlank() && it != city }?.let { "$it, $city" } ?: city
        }

    /** Kullanıcı kalkış noktası seçtiyse koordinatı; seçmediyse null (mevcut konum kullanılır). */
    val originPoint: Pair<Double, Double>?
        get() {
            val lat = originLatitude ?: return null
            val lon = originLongitude ?: return null
            return if (lat == 0.0 && lon == 0.0) null else lat to lon
        }
}
