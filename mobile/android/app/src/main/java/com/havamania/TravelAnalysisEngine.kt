package com.havamania

import android.content.Context
import android.util.Log
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * P1 Refactor: Extraction of analysis logic from ViewModel to allow Worker usage
 * without ViewModel instantiation.
 */
object TravelAnalysisEngine {
    private const val TAG = "TravelAnalysisEngine"

    suspend fun performAnalysis(
        context: Context,
        plan: TravelPlan,
        currentUid: String,
        apiService: WeatherApiService,
        repository: WeatherRepository
    ): TravelPlan {
        val today = LocalDate.now()
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
        val status = TravelStatusResolver.getStatus(plan.startDate, plan.endDate, today)

        val tone = ThemeManager.getAssistantTone(context, currentUid).first()

        // 1. TAMAMLANMIŞ SEYAHAT KONTROLÜ
        if (status == TravelStatus.PAST) {
            val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city, tripType = plan.tripType, forecastSnapshot = null,
                previousSnapshot = null, daysUntilTrip = daysUntil, isPastTrip = true,
                endDate = plan.endDate, tone = tone
            )
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
                aiSuggestion = suggestion,
                lastAnalysisAt = System.currentTimeMillis()
            )
        }

        // 2. 10 GÜN KURALI (Business Rule 1)
        if (status == TravelStatus.UPCOMING && daysUntil > TRIP_ANALYSIS_WINDOW_DAYS) {
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW,
                aiSuggestion = null,
                lastAnalysisAt = System.currentTimeMillis()
            )
        }

        // 3. KOORDİNAT VE API ÇAĞRISI (Yalnızca gerekiyorsa)
        var lat = plan.latitude
        var lon = plan.longitude

        if (lat == 0.0 && lon == 0.0) {
            val normalized = normalizeCityName(plan.city)
            val fallback = CITY_FALLBACKS[normalized]
            if (fallback != null) {
                lat = fallback.first
                lon = fallback.second
            } else {
                val geoResults = try {
                    withTimeout(5000) { repository.searchCity(plan.city) }
                } catch (e: Exception) { emptyList() }

                if (geoResults.isNotEmpty()) {
                    lat = geoResults[0].latitude
                    lon = geoResults[0].longitude
                }
            }
        }

        if (lat == 0.0 && lon == 0.0) throw Exception("Koordinat bulunamadı")

        // API Çağrısı (Timeout ile)
        val response = try {
            withTimeout(15000) {
                apiService.getFullWeather(lat = lat, lon = lon, days = 16)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Weather API fail", e)
            null
        }

        if (response == null) {
            // Offline/Hata durumunda mevcut veriyi koru
            if (plan.lastForecastSnapshot != null) {
                return plan.copy(isAnalyzing = false, weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY)
            }
            throw Exception("API hatası")
        }

        // 4. ANALİZ ÜRETİMİ
        val daily = response.daily
        var snapshot: ForecastSnapshot? = null

        if (daily != null) {
            val tripDates = daily.time.map { LocalDate.parse(it) }
            val overlapIndices = tripDates.indices.filter { i ->
                val date = tripDates[i]
                !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
            }

            if (overlapIndices.isNotEmpty()) {
                val maxCode = overlapIndices.mapNotNull { i -> daily.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                val avgMin = overlapIndices.mapNotNull { i -> daily.tempMin.getOrNull(i) }.average()
                val avgMax = overlapIndices.mapNotNull { i -> daily.tempMax.getOrNull(i) }.average()

                snapshot = ForecastSnapshot(
                    precipitationProbability = overlapIndices.mapNotNull { i -> daily.precipProbMax?.getOrNull(i) }.maxOrNull(),
                    minTemp = avgMin,
                    maxTemp = avgMax,
                    windSpeed = overlapIndices.mapNotNull { i -> daily.windSpeedMax.getOrNull(i) }.maxOrNull(),
                    uvIndex = overlapIndices.mapNotNull { i -> daily.uvIndexMax?.getOrNull(i) }.maxOrNull(),
                    conditionSummary = WeatherMapper.getWeatherCondition(maxCode),
                    weatherCode = maxCode,
                    travelScore = calculateTravelScore(ForecastSnapshot(minTemp = avgMin, maxTemp = avgMax), plan.tripType)
                )
            }
        }

        // Kişiselleştirme
        val interests = ThemeManager.getUserInterests(context, currentUid).first()
        val personalization = PersonalizationProfile(uid = currentUid, selectedInterests = interests.toList())

        val aiResult = TravelAiHelper.generateTravelAiSuggestion(
            plan.city, plan.tripType, snapshot, plan.lastForecastSnapshot,
            daysUntil, tone = tone, personalization = personalization
        )

        // Parse AI Result to separate fields for the model (Stability)
        val sections = aiResult.split("[SEP]")
        var weatherSum: String? = null
        var pack: String? = null
        var must: String? = null
        var food: String? = null
        var local: String? = null

        sections.forEach { s ->
            when {
                s.contains("HAVA ÖZETİ|") -> weatherSum = s.split("|").last().trim()
                s.contains("VALİZ TAVSİYESİ|") -> pack = s.split("|").last().trim()
                s.contains("MUTLAKA GÖR|") -> must = s.split("|").last().trim()
                s.contains("DENEMEDEN DÖNME|") -> food = s.split("|").last().trim()
                s.contains("YEREL TAVSİYE|") -> local = s.split("|").last().trim()
            }
        }

        return plan.copy(
            isAnalyzing = false,
            aiSuggestion = aiResult,
            weatherSummary = weatherSum,
            packingAdvice = pack,
            mustSee = must,
            foodAdvice = food,
            localAdvice = local,
            comfortScore = snapshot?.travelScore,
            lastAnalysisAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastForecastSnapshot = snapshot,
            previousForecastSnapshot = plan.lastForecastSnapshot,
            weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
            latitude = lat,
            longitude = lon,
            analyses = plan.analyses + TravelWeatherAnalysis(
                tripId = plan.id,
                travelScore = snapshot?.travelScore ?: 0,
                rainRiskPercent = snapshot?.precipitationProbability,
                averageTemperature = ((snapshot?.minTemp ?: 0.0) + (snapshot?.maxTemp ?: 0.0)) / 2.0,
                summary = weatherSum ?: "Hava durumu verisi alındı.",
                recommendation = aiResult,
                comparisonText = if (plan.lastForecastSnapshot != null && snapshot != null)
                    TravelAiHelper.generateComparisonText(plan.lastForecastSnapshot, snapshot)
                    else null
            )
        )
    }

    fun calculateTravelScore(snapshot: ForecastSnapshot, type: TripType): Int {
        val minT = snapshot.minTemp ?: 15.0
        val maxT = snapshot.maxTemp ?: 25.0
        val avgTemp = (minT + maxT) / 2.0

        val precip = snapshot.precipitationProbability ?: 0
        val wind = snapshot.windSpeed ?: 10.0

        // 1. Temperature Comfort (0..100) based on TripType ideal temperature using averageTemperature
        val idealTemp = when(type) {
            TripType.BEACH -> 28.0
            TripType.WINTER -> 0.0
            TripType.CAMPING, TripType.SPORTS -> 20.0
            else -> 23.0
        }
        val tempDiff = kotlin.math.abs(avgTemp - idealTemp)
        val tempComfort = (100.0 - (tempDiff * 3.0)).coerceIn(0.0, 100.0)

        // 2. Precipitation Comfort (0..100) with trip-type sensitivity
        val precipSensitivity = when(type) {
            TripType.NATURE, TripType.CAMPING, TripType.SPORTS -> 1.2
            else -> 0.8
        }
        val precipComfort = (100.0 - (precip * precipSensitivity)).coerceIn(0.0, 100.0)

        // 3. Wind Comfort (0..100)
        val windSensitivity = when(type) {
            TripType.NATURE, TripType.CAMPING -> 1.5
            else -> 1.0
        }
        val windComfort = (100.0 - (wind * windSensitivity)).coerceIn(0.0, 100.0)

        val finalScore = (tempComfort * 0.45) + (precipComfort * 0.35) + (windComfort * 0.20)
        val rounded = finalScore.roundToInt().coerceIn(0, 100)

        if (BuildConfig.DEBUG) {
            Log.d(
                "HAVAMANIA_TRAVEL_SCORE_DEBUG",
                "TRIP_TYPE=$type | TEMP_METRIC_NAME=averageTemperature | TEMP_INPUT_EXACT=$avgTemp | PRECIP_METRIC_NAME=precipitationProbability | PRECIP_INPUT_EXACT=$precip | WIND_METRIC_NAME=windSpeed | WIND_INPUT_EXACT=$wind | TEMP_IDEAL=$idealTemp | TEMP_COMFORT_EXACT=$tempComfort | PRECIP_SENSITIVITY=$precipSensitivity | PRECIP_COMFORT_EXACT=$precipComfort | WIND_SENSITIVITY=$windSensitivity | WIND_COMFORT_EXACT=$windComfort | WEIGHT_TEMP=0.45 | WEIGHT_PRECIP=0.35 | WEIGHT_WIND=0.20 | RAW_SCORE=$finalScore | ROUNDED_SCORE=$rounded"
            )
        }

        return rounded
    }

    private fun normalizeCityName(name: String): String {
        return name.trim().lowercase(java.util.Locale("tr"))
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace("i\u0307", "i")
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
    }

    private val CITY_FALLBACKS = mapOf(
        "istanbul" to Pair(41.0082, 28.9784),
        "ankara" to Pair(39.9334, 32.8597),
        "izmir" to Pair(38.4237, 27.1428),
        "antalya" to Pair(36.8969, 30.7133),
        "balikesir" to Pair(39.6484, 27.8826),
        "trabzon" to Pair(41.0027, 39.7168),
        "mardin" to Pair(37.3129, 40.7350),
        "gaziantep" to Pair(37.0662, 37.3833),
        "batman" to Pair(37.8812, 41.1322),
        "bali" to Pair(-8.4095, 115.1889)
    )
}
