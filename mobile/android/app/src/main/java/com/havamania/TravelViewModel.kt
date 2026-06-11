package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()
    private val repository = WeatherRepository.getInstance(application)
    private val apiService = NetworkModule.apiService

    private val _plans = MutableStateFlow<List<TravelPlan>>(emptyList())
    val plans: StateFlow<List<TravelPlan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val TAG = "TravelAnalysisDebug"
    private val AUTO_TAG = "TravelAutoAnalysis"
    private val FLOW_TAG = "TripCreateFlow"

    private val CITY_FALLBACKS = mapOf(
        "ankara" to Pair(39.9334, 32.8597),
        "istanbul" to Pair(41.0082, 28.9784),
        "izmir" to Pair(38.4237, 27.1428),
        "balikesir" to Pair(39.6533, 27.8903),
        "antalya" to Pair(36.8969, 30.7133),
        "trabzon" to Pair(41.0027, 39.7168),
        "kars" to Pair(40.6013, 43.0975),
        "edirne" to Pair(41.6771, 26.5557),
        "batman" to Pair(37.8812, 41.1351),
        "cankiri" to Pair(40.6013, 33.6134),
        "mardin" to Pair(37.3212, 40.7245),
        "gaziantep" to Pair(37.0662, 37.3833),
        "ordu" to Pair(40.9839, 37.8764)
    )

    init {
        seedInitialDataIfNeeded()
    }

    private fun normalizeCityName(name: String): String {
        return name.trim().lowercase(Locale("tr"))
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace("i\u0307", "i")
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
    }

    fun seedInitialDataIfNeeded(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = if (force) emptyList() else dao.getAllTravelPlans()
            if (entities.isEmpty()) {
                if (force) dao.clearAllTravelPlans()
                val today = LocalDate.now()
                val currentYear = today.year

                val seedPlans = listOf(
                    TravelPlan(
                        city = "Batman",
                        latitude = 37.8812,
                        longitude = 41.1322,
                        tripType = TripType.EVENT,
                        startDate = LocalDate.of(currentYear, 5, 15),
                        endDate = LocalDate.of(currentYear, 5, 16)
                    ),
                    TravelPlan(
                        city = "Bali",
                        latitude = -8.4095,
                        longitude = 115.1889,
                        tripType = TripType.WEEKEND,
                        startDate = LocalDate.of(currentYear, 5, 28),
                        endDate = LocalDate.of(currentYear, 5, 30)
                    ),
                    TravelPlan(
                        city = "Trabzon",
                        latitude = 41.0027,
                        longitude = 39.7168,
                        tripType = TripType.SHOPPING,
                        startDate = LocalDate.of(currentYear, 5, 18),
                        endDate = LocalDate.of(currentYear, 5, 21)
                    ),
                    TravelPlan(
                        city = "Balıkesir",
                        latitude = 39.6533,
                        longitude = 27.8903,
                        tripType = TripType.GASTRONOMY,
                        startDate = LocalDate.of(currentYear, 6, 10),
                        endDate = LocalDate.of(currentYear, 6, 11)
                    ),
                    TravelPlan(
                        city = "Mardin",
                        latitude = 37.3212,
                        longitude = 40.7245,
                        tripType = TripType.CULTURE,
                        startDate = today.plusDays(7),
                        endDate = today.plusDays(9)
                    )
                )

                seedPlans.forEach { plan ->
                    dao.insertTravelPlan(plan.toEntity())
                }
            }
            loadPlans()
        }
    }

    fun loadPlans() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = dao.getAllTravelPlans()
            Log.d(TAG, "Source: Room DB, Loaded ${entities.size} entities")

            val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }
            _plans.value = domainPlans
            _isLoading.value = false

            checkAndTriggerAutoAnalysis(domainPlans)
        }
    }

    private fun checkAndTriggerAutoAnalysis(plans: List<TravelPlan>) {
        val today = LocalDate.now()
        plans.forEach { plan ->
            if (plan.isArchived) return@forEach

            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isWithinWindow = daysUntil <= 15
            val isOver = today.isAfter(plan.endDate)

            if (isOver) return@forEach

            val status = plan.weatherAnalysisStatus
            // Be more aggressive: retry if FAILED or MISSING AI but still within window
            val shouldAutoAnalyze = isWithinWindow && (
                status == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW ||
                status == TravelWeatherAnalysisStatus.WEATHER_FAILED ||
                plan.aiSuggestion == null
            )

            Log.i(AUTO_TAG, "TripId=${plan.id} City=${plan.city} DaysUntil=$daysUntil Status=$status ShouldAnalyze=$shouldAutoAnalyze")

            if (shouldAutoAnalyze) {
                Log.i(AUTO_TAG, "AutoAnalysisStarted=true")
                analyzeTravelWeather(plan)
            }
        }
    }

    fun analyzeTravelWeather(plan: TravelPlan) {
        viewModelScope.launch {
            Log.d(TAG, "--------------------------------------------------")
            Log.d(TAG, "RE-ANALYZE TRIGGERED for ${plan.city} (Id=${plan.id})")

            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(
                    isAnalyzing = true,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.LOADING,
                    lastWeatherAnalysisText = "Analiz hazırlanıyor..."
                ) else it
            }

            val updatedPlan = performAnalysis(plan)

            Log.i(TAG, "AnalysisFinished TripId=${plan.id} FinalStatus=${updatedPlan.weatherAnalysisStatus}")

            dao.insertTravelPlan(updatedPlan.toEntity())

            _plans.value = _plans.value.map {
                if (it.id == plan.id) updatedPlan else it
            }
        }
    }

    suspend fun performAnalysis(plan: TravelPlan): TravelPlan {
        val today = LocalDate.now()
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
        val isWithinWindow = daysUntil <= 15

        Log.i(TAG, "performAnalysis TripId=${plan.id} City=${plan.city} DaysUntil=$daysUntil")

        if (plan.endDate.isBefore(today)) {
             val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city, tripType = plan.tripType, forecastSnapshot = null,
                previousSnapshot = null, daysUntilTrip = daysUntil, isPastTrip = true, endDate = plan.endDate
            )
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
                lastWeatherAnalysisText = "Bu seyahat geçmişte tamamlandı.",
                aiSuggestion = suggestion,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        if (!isWithinWindow) {
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW,
                lastWeatherAnalysisText = "Seyahatiniz 15 günden daha uzak. Hava tahminleri yaklaştığında analiz edilecektir.",
                aiSuggestion = null,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        var coordinateSource = "none"
        var lat = plan.latitude
        var lon = plan.longitude

        if (lat == 0.0 && lon == 0.0) {
            val normalized = normalizeCityName(plan.city)
            val fallback = CITY_FALLBACKS[normalized]
            if (fallback != null) {
                lat = fallback.first
                lon = fallback.second
                coordinateSource = "internal_fallback"
            } else {
                val geoResults = try { repository.searchCity(plan.city) } catch(e: Exception) { emptyList() }
                if (geoResults.isNotEmpty()) {
                    lat = geoResults[0].latitude
                    lon = geoResults[0].longitude
                    coordinateSource = "geocoding_api"
                }
            }
        } else {
            coordinateSource = "existing_plan"
        }

        Log.i(TAG, "CoordinateSource=$coordinateSource Lat=$lat Lon=$lon")

        var weatherSuccess = false
        var snapshot: ForecastSnapshot? = null
        var analysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY

        if (lat != 0.0 && lon != 0.0) {
            val response = try {
                apiService.getFullWeather(lat = lat, lon = lon, days = 16)
            } catch (e: Exception) {
                Log.e(TAG, "WeatherRequestFailed message=${e.message}")
                null
            }

            if (response != null) {
                weatherSuccess = true
                val daily = response.daily
                val hourly = response.hourly

                // 1. Try Daily Overlap
                if (daily != null) {
                    val tripDates = daily.time.map { LocalDate.parse(it) }
                    val overlapIndices = tripDates.indices.filter { i ->
                        val date = tripDates[i]
                        !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
                    }

                    if (overlapIndices.isNotEmpty()) {
                        val maxCode = overlapIndices.mapNotNull { i -> daily.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                        snapshot = ForecastSnapshot(
                            precipitationProbability = overlapIndices.mapNotNull { i -> daily.precipProbMax?.getOrNull(i) }.maxOrNull(),
                            minTemp = overlapIndices.mapNotNull { i -> daily.tempMin.getOrNull(i) }.minOrNull(),
                            maxTemp = overlapIndices.mapNotNull { i -> daily.tempMax.getOrNull(i) }.maxOrNull(),
                            windSpeed = overlapIndices.mapNotNull { i -> daily.windSpeedMax.getOrNull(i) }.maxOrNull(),
                            uvIndex = overlapIndices.mapNotNull { i -> daily.uvIndexMax?.getOrNull(i) }.maxOrNull(),
                            conditionSummary = WeatherMapper.getWeatherCondition(maxCode),
                            weatherCode = maxCode,
                            generatedAt = System.currentTimeMillis()
                        )
                    }
                }

                // 2. Fallback to Hourly
                if (snapshot == null && hourly != null) {
                    val hourlyDates = hourly.time.map { LocalDateTime.parse(it).toLocalDate() }
                    var overlapIndices = hourlyDates.indices.filter { i ->
                        val date = hourlyDates[i]
                        !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
                    }

                    if (overlapIndices.isEmpty() && hourlyDates.isNotEmpty()) {
                        val latestForecastDate = hourlyDates.last()
                        overlapIndices = hourlyDates.indices.filter { hourlyDates[it] == latestForecastDate }
                    }

                    if (overlapIndices.isNotEmpty()) {
                        val maxCode = overlapIndices.mapNotNull { i -> hourly.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                        snapshot = ForecastSnapshot(
                            precipitationProbability = overlapIndices.mapNotNull { i -> hourly.precipitationProbability?.getOrNull(i) }.maxOrNull(),
                            minTemp = overlapIndices.mapNotNull { i -> hourly.temperature.getOrNull(i) }.minOrNull(),
                            maxTemp = overlapIndices.mapNotNull { i -> hourly.temperature.getOrNull(i) }.maxOrNull(),
                            windSpeed = null,
                            uvIndex = null,
                            conditionSummary = WeatherMapper.getWeatherCondition(maxCode),
                            weatherCode = maxCode,
                            generatedAt = System.currentTimeMillis()
                        )
                        analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
                    }
                }
            }
        }

        // 3. Fallback to Cache/Current Weather if Snapshot still null
        if (snapshot == null) {
            val lastWeather = repository.currentWeatherState.value
            if (lastWeather != null && (normalizeCityName(lastWeather.cityName) == normalizeCityName(plan.city))) {
               snapshot = ForecastSnapshot(
                   precipitationProbability = lastWeather.precipitationProbability,
                   minTemp = lastWeather.low.filter { it.isDigit() || it == '-' }.toDoubleOrNull(),
                   maxTemp = lastWeather.high.filter { it.isDigit() || it == '-' }.toDoubleOrNull(),
                   windSpeed = lastWeather.windSpeed,
                   uvIndex = lastWeather.uvIndex?.toDouble(),
                   conditionSummary = lastWeather.condition,
                   weatherCode = lastWeather.weatherCode,
                   generatedAt = System.currentTimeMillis()
               )
               analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
               Log.i(TAG, "FallbackUsed=weather_cache")
            } else {
                // If even cache fails, use Current Weather API as a last online resort
                if (lat != 0.0 && lon != 0.0) {
                     val currentResponse = try { apiService.getFullWeather(lat = lat, lon = lon, current = "temperature_2m,weather_code,wind_speed_10m") } catch(e: Exception) { null }
                     val current = currentResponse?.current
                     if (current != null) {
                         snapshot = ForecastSnapshot(
                             precipitationProbability = null,
                             minTemp = current.temperature,
                             maxTemp = current.temperature,
                             windSpeed = current.windSpeed,
                             uvIndex = null,
                             conditionSummary = WeatherMapper.getWeatherCondition(current.weatherCode ?: 0),
                             weatherCode = current.weatherCode ?: 0,
                             generatedAt = System.currentTimeMillis()
                         )
                         analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
                         Log.i(TAG, "FallbackUsed=current_weather_api")
                     }
                }
            }
        }

        // FINAL STEP: Always generate an analysis if within 15 days, even if snapshot is null (Offline Fallback)
        val aiResult = TravelAiHelper.generateTravelAiSuggestion(plan.city, plan.tripType, snapshot, plan.lastForecastSnapshot, daysUntil)

        val score = if (snapshot != null) calculateTravelScore(snapshot, plan.tripType) else 75
        val avgTemp = if (snapshot != null) (((snapshot.minTemp ?: 0.0) + (snapshot.maxTemp ?: 0.0)) / 2.0) else 0.0

        val estimatedRainRisk = if (snapshot?.precipitationProbability != null) {
            snapshot.precipitationProbability
        } else {
            estimateRainRisk(snapshot?.conditionSummary)
        }

        val rainInfo = WeatherUtils.getPrecipitationRiskText(estimatedRainRisk, 0.0, snapshot?.weatherCode ?: 0)
        val summaryText = if (snapshot != null) {
             val cityNormalized = plan.city.trim()
             val cond = snapshot.conditionSummary?.lowercase() ?: "açık"
             val assistantTone = when {
                 (snapshot.weatherCode ?: 0) >= 95 -> "Hava fırtınalı görünüyor, yağış riski yüksek."
                 (snapshot.weatherCode ?: 0) >= 80 -> "Hava sağanak yağışlı görünüyor, yağış riski yüksek."
                 (estimatedRainRisk ?: 0) > 60 -> {
                    val formatted = WeatherUtils.formatRainProbability(estimatedRainRisk)
                    "$cityNormalized seyahatinde yanına mutlaka bir şemsiye almalısın, yağmur ihtimali $formatted."
                 }
                 (snapshot.maxTemp ?: 20.0) > 30 -> "$cityNormalized seni güneşli ve pırıl pırıl bir havayla karşılayacak, tam gezmelik!"
                 (snapshot.maxTemp ?: 20.0) < 12 -> "$cityNormalized serin bir havayla seni bekliyor, valizine kalın bir şeyler eklemeyi unutma."
                 else -> {
                    val formatted = WeatherUtils.formatRainProbability(estimatedRainRisk)
                    "$cityNormalized seyahatin için hava oldukça ideal ve $cond görünüyor, yağmur ihtimali $formatted."
                 }
             }
             assistantTone
        } else {
            "Hava verisi sınırlı olsa da seyahatiniz için temel öneriler hazırlandı."
        }

        val newAnalysis = TravelWeatherAnalysis(
            tripId = plan.id,
            travelScore = score,
            rainRiskPercent = estimatedRainRisk,
            windRiskPercent = snapshot?.windSpeed?.let { (it * 2).toInt().coerceAtMost(100) },
            uvRiskPercent = snapshot?.uvIndex?.let { (it * 10).toInt().coerceAtMost(100) },
            averageTemperature = avgTemp,
            summary = summaryText,
            recommendation = aiResult,
            comparisonText = null,
            previousAnalysisId = plan.analyses.lastOrNull()?.id
        )

        Log.i(TAG, "AnalysisGenerated=true WeatherSuccess=$weatherSuccess FinalStatus=$analysisStatus")

        return plan.copy(
            isAnalyzing = false,
            lastWeatherAnalysisText = "Gelişmiş analiz hazır",
            aiSuggestion = newAnalysis.recommendation,
            lastWeatherAnalysisDate = System.currentTimeMillis(),
            previousForecastSnapshot = plan.lastForecastSnapshot,
            lastForecastSnapshot = snapshot,
            weatherAnalysisStatus = analysisStatus,
            analyses = plan.analyses + newAnalysis,
            latitude = lat,
            longitude = lon
        )
    }

    private fun estimateRainRisk(condition: String?): Int {
        if (condition == null) return 10 // "Düşük"

        val c = condition.lowercase(Locale("tr"))
        return when {
            c.contains("güneşli") || c.contains("açık") -> 5
            c.contains("parçalı bulutlu") -> 15
            c.contains("bulutlu") -> 30
            c.contains("sisli") || c.contains("puslu") -> 20
            c.contains("yağmurlu") -> 70
            c.contains("sağanak") -> 85
            c.contains("fırtınalı") || c.contains("gök gürültülü") -> 90
            c.contains("karlı") -> 75
            else -> 10 // "Düşük"
        }
    }

    private fun calculateTravelScore(snapshot: ForecastSnapshot, type: TripType): Int {
        var score = 85 + (Math.random() * 10).toInt() // Base score between 85-95
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

    private fun generateComparisonSummary(old: TravelWeatherAnalysis, newScore: Int, newSnapshot: ForecastSnapshot): String {
        val sb = StringBuilder()
        val scoreDiff = newScore - old.travelScore
        if (scoreDiff != 0) {
            val direction = if (scoreDiff > 0) "iyileşti" else "düştü"
            val formatted = WeatherUtils.formatRainProbability(kotlin.math.abs(scoreDiff))
            sb.append("Seyahat skoru önceki analize göre $formatted $direction. ")
        }

        val oldTemp = old.averageTemperature
        val newTemp = ((newSnapshot.minTemp ?: 0.0) + (newSnapshot.maxTemp ?: 0.0)) / 2.0
        val tempDiff = (newTemp - oldTemp).toInt()

        if (tempDiff != 0) {
            val sign = if (tempDiff > 0) "+" else ""
            sb.append("Ortalama sıcaklık $sign$tempDiff° değişti. ")
        }

        val oldRain = old.rainRiskPercent ?: 0
        val newRain = newSnapshot.precipitationProbability ?: 0
        val rainDiff = newRain - oldRain

        if (kotlin.math.abs(rainDiff) > 10) {
            val direction = if (rainDiff > 0) "arttı" else "azaldı"
            val formatted = WeatherUtils.formatRainProbability(kotlin.math.abs(rainDiff))
            sb.append("Yağış riski $formatted $direction.")
        }

        return sb.toString().trim()
    }

    fun savePlan(plan: TravelPlan) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val today = LocalDate.now()
            val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
            val isUpcoming = !plan.startDate.isBefore(today)

            Log.i(FLOW_TAG, "TripCreated=true TripId=${plan.id} City=${plan.city} DaysUntil=$daysUntil")

            dao.insertTravelPlan(plan.toEntity())

            val entities = dao.getAllTravelPlans()
            val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _plans.value = domainPlans
                Log.i(FLOW_TAG, "RefreshTripsCalled=true TripsCount=${domainPlans.size}")

                if (isUpcoming && daysUntil <= 15) {
                    Log.i(FLOW_TAG, "AutoAnalysisStarted=true")
                    analyzeTravelWeather(plan)
                }
            }
        }
    }

    fun deletePlan(id: String) {
        viewModelScope.launch {
            dao.deleteTravelPlan(id)
            loadPlans()
        }
    }

    fun clearAllPlans() {
        viewModelScope.launch {
            dao.clearAllTravelPlans()
            _plans.value = emptyList()
        }
    }

    fun searchCity(query: String) {
        if (query.length < 2) {
            _citySuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            _citySuggestions.value = repository.searchCity(query)
        }
    }

    fun archiveTrip(id: String) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = true)
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    fun unarchiveTrip(id: String) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(isArchived = false)
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    fun updateTripNoteAndRating(id: String, note: String, rating: Int) {
        viewModelScope.launch {
            val plan = _plans.value.find { it.id == id } ?: return@launch
            val updated = plan.copy(userNote = note, userRating = rating)
            dao.insertTravelPlan(updated.toEntity())
            loadPlans()
        }
    }

    private fun TravelPlanEntity.toDomain() = TravelPlan(
        id = id,
        city = city,
        latitude = latitude,
        longitude = longitude,
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        endDate = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion,
        userNote = userNote,
        userRating = userRating,
        lastWeatherAnalysisText = lastWeatherAnalysisText,
        lastWeatherAnalysisDate = lastWeatherAnalysisDate,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW },
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate
    )

    private fun TravelPlan.toEntity() = TravelPlanEntity(
        id = id,
        city = city,
        latitude = latitude,
        longitude = longitude,
        tripType = tripType.name,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion,
        userNote = userNote,
        userRating = userRating,
        lastWeatherAnalysisText = lastWeatherAnalysisText,
        lastWeatherAnalysisDate = lastWeatherAnalysisDate,
        lastForecastSnapshot = lastForecastSnapshot,
        previousForecastSnapshot = previousForecastSnapshot,
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = weatherAnalysisStatus.name,
        isArchived = isArchived,
        analyses = analyses,
        lastDailyNotificationDate = lastDailyNotificationDate
    )
}
