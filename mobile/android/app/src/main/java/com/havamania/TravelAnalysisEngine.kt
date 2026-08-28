package com.havamania

import android.content.Context
import android.util.Log
import com.havamania.ui.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        // Deterministic base score based on city name or trip id to keep it stable but slightly varied
        var score = 88
        val precip = snapshot.precipitationProbability ?: 0
        val wind = snapshot.windSpeed ?: 0.0
        val temp = snapshot.maxTemp ?: 20.0

        // Impact of precipitation
        if (precip > 70) score -= 35
        else if (precip > 40) score -= 20
        else if (precip > 15) score -= 8

        // Impact of wind
        if (wind > 50) score -= 25
        else if (wind > 30) score -= 12
        else if (wind > 20) score -= 5

        // Impact of temperature based on trip type
        when(type) {
            TripType.BEACH -> {
                if (temp < 22) score -= 30
                else if (temp < 26) score -= 15
                else if (temp > 38) score -= 10
            }
            TripType.WINTER -> {
                if (temp > 8) score -= 25
                else if (temp < -10) score -= 15
            }
            TripType.CAMPING -> {
                if (temp < 12) score -= 25
                else if (temp > 35) score -= 15
                if (precip > 30) score -= 20
            }
            TripType.SPORTS -> {
                if (temp > 32) score -= 20
                if (wind > 25) score -= 15
            }
            else -> {
                if (temp < 5) score -= 20
                else if (temp > 35) score -= 15
            }
        }

        return score.coerceIn(40, 100)
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
