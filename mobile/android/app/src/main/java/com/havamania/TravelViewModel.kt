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
        "cankiri" to Pair(40.6013, 33.6134)
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

            // Trigger auto-analysis for plans within window
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
            val shouldAutoAnalyze = isWithinWindow && (
                status == TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW ||
                status == TravelWeatherAnalysisStatus.WEATHER_FAILED
            )

            Log.i(AUTO_TAG, "TripId=${plan.id}")
            Log.i(AUTO_TAG, "City=${plan.city}")
            Log.i(AUTO_TAG, "StartDate=${plan.startDate}")
            Log.i(AUTO_TAG, "Today=$today")
            Log.i(AUTO_TAG, "DaysUntilTrip=$daysUntil")
            Log.i(AUTO_TAG, "OldStatus=$status")
            Log.i(AUTO_TAG, "ShouldAutoAnalyze=$shouldAutoAnalyze")

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
            Log.d(TAG, "STATE CHANGE: ${plan.weatherAnalysisStatus} -> LOADING")

            // UI Update: Clear previous failed state and set to loading
            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(
                    isAnalyzing = true,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.LOADING,
                    lastWeatherAnalysisText = "Hava verisi alınıyor..."
                ) else it
            }

            // Fresh analysis
            val updatedPlan = performAnalysis(plan)

            Log.d(TAG, "STATE CHANGE: LOADING -> ${updatedPlan.weatherAnalysisStatus}")
            if (updatedPlan.weatherAnalysisStatus == TravelWeatherAnalysisStatus.WEATHER_FAILED) {
                Log.e(TAG, "STATE CHANGE -> FAILED for ${plan.city}")
                Log.e(TAG, "Stacktrace for FAILED state:\n${Log.getStackTraceString(Throwable())}")
            }

            Log.i(AUTO_TAG, "TripId=${plan.id} FinalStatus=${updatedPlan.weatherAnalysisStatus} Error=${updatedPlan.lastWeatherAnalysisText}")

            // Save to DB
            dao.insertTravelPlan(updatedPlan.toEntity())

            // Update the state flow directly for immediate UI update without triggering checkAndTriggerAutoAnalysis again
            _plans.value = _plans.value.map {
                if (it.id == plan.id) updatedPlan else it
            }
        }
    }

    suspend fun performAnalysis(plan: TravelPlan): TravelPlan {
        val today = LocalDate.now()
        val isPastTrip = plan.endDate.isBefore(today)
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()
        val isWithinWindow = daysUntil <= 15

        Log.i(TAG, "TravelId=${plan.id}")
        Log.i(TAG, "City=${plan.city}")
        Log.i(TAG, "TripStart=${plan.startDate}")
        Log.i(TAG, "TripEnd=${plan.endDate}")
        Log.i(TAG, "DaysUntilTrip=$daysUntil")
        Log.i(TAG, "AnalysisWindowCheck=${if (isWithinWindow) "WITHIN_WINDOW" else "TOO_EARLY"}")

        if (isPastTrip) {
            Log.i(TAG, "FinalStatus=WEATHER_READY_ANALYSIS_READY (Past Trip)")
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
            Log.i(TAG, "FinalStatus=WAITING_FOR_WINDOW")
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WAITING_FOR_WINDOW,
                lastWeatherAnalysisText = "Seyahatiniz 15 günden daha uzak. Hava tahminleri yaklaştığında analiz edilecektir.",
                aiSuggestion = null,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        return try {
            var lat = plan.latitude
            var lon = plan.longitude
            var geocodingSource = "plan"

            Log.d(TAG, "GeocodingStarted=true")
            if (lat == 0.0 && lon == 0.0) {
                val normalized = normalizeCityName(plan.city)
                val fallback = CITY_FALLBACKS[normalized]
                if (fallback != null) {
                    lat = fallback.first
                    lon = fallback.second
                    geocodingSource = "static_fallback"
                } else {
                    Log.d(TAG, "GeocodingRepository.resolveCity() for ${plan.city}")
                    val geoResults = try { repository.searchCity(plan.city) } catch(e: Exception) { emptyList() }
                    if (geoResults.isNotEmpty()) {
                        lat = geoResults[0].latitude
                        lon = geoResults[0].longitude
                        geocodingSource = "geocoding_api"
                    }
                }
            }

            Log.i(TAG, "GeocodingSuccess=${lat != 0.0}")
            Log.i(TAG, "Latitude=$lat")
            Log.i(TAG, "Longitude=$lon")
            Log.i(TAG, "GeocodingSource=$geocodingSource")

            if (lat == 0.0 && lon == 0.0) {
                Log.e(TAG, "FinalStatus=WEATHER_FAILED (Reason: GEOCODING)")
                return plan.copy(
                    isAnalyzing = false,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_FAILED,
                    lastWeatherAnalysisText = "Şehir koordinatları bulunamadı. Lütfen şehri listeden seçerek tekrar ekleyin.",
                    lastWeatherAnalysisDate = System.currentTimeMillis()
                )
            }

            Log.i(TAG, "WeatherRequestStarted=true")
            Log.d(TAG, "WeatherRepository.getForecast() for ${plan.city} (Lat=$lat, Lon=$lon)")
            val response = try {
                apiService.getFullWeather(lat = lat, lon = lon, days = 16)
            } catch (e: Exception) {
                Log.e(TAG, "WeatherRequestFailed=true message=${e.message}")
                Log.e(TAG, "WeatherRepository error stacktrace:\n${Log.getStackTraceString(e)}")
                null
            }

            Log.i(TAG, "WeatherResponseReceived=${response != null}")

            val daily = response?.daily
            val hourly = response?.hourly
            val current = response?.current

            Log.i(TAG, "DailyCount=${daily?.time?.size ?: 0}")
            Log.i(TAG, "HourlyCount=${hourly?.time?.size ?: 0}")
            Log.i(TAG, "CurrentWeatherAvailable=${current != null}")

            var snapshot: ForecastSnapshot? = null
            var analysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY

            // 1. Try Daily Overlap - Strict Comparison
            if (daily != null) {
                val tripDates = daily.time.map { LocalDate.parse(it) }
                val overlapIndices = tripDates.indices.filter { i ->
                    val date = tripDates[i]
                    // date is within [plan.startDate, plan.endDate]
                    !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
                }

                if (overlapIndices.isNotEmpty()) {
                    snapshot = ForecastSnapshot(
                        precipitationProbability = overlapIndices.mapNotNull { i -> daily.precipProbMax?.getOrNull(i) }.maxOrNull(),
                        minTemp = overlapIndices.mapNotNull { i -> daily.tempMin.getOrNull(i) }.minOrNull(),
                        maxTemp = overlapIndices.mapNotNull { i -> daily.tempMax.getOrNull(i) }.maxOrNull(),
                        windSpeed = overlapIndices.mapNotNull { i -> daily.windSpeedMax.getOrNull(i) }.maxOrNull(),
                        uvIndex = overlapIndices.mapNotNull { i -> daily.uvIndexMax?.getOrNull(i) }.maxOrNull(),
                        conditionSummary = WeatherMapper.getWeatherCondition(
                            overlapIndices.mapNotNull { i -> daily.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                        ),
                        generatedAt = System.currentTimeMillis()
                    )
                    Log.i(TAG, "SnapshotProducedFrom=daily")
                }
            }

            // 2. Fallback to Hourly - Try nearest day if no exact overlap
            if (snapshot == null && hourly != null) {
                val hourlyDates = hourly.time.map { LocalDateTime.parse(it).toLocalDate() }
                var overlapIndices = hourlyDates.indices.filter { i ->
                    val date = hourlyDates[i]
                    !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
                }

                // If seyahat dates are ahead of forecast window, use the latest possible data (nearest forecast)
                if (overlapIndices.isEmpty() && hourlyDates.isNotEmpty()) {
                    Log.w(TAG, "No exact hourly overlap, using latest available forecast data as fallback")
                    val latestForecastDate = hourlyDates.last()
                    overlapIndices = hourlyDates.indices.filter { hourlyDates[it] == latestForecastDate }
                }

                if (overlapIndices.isNotEmpty()) {
                    snapshot = ForecastSnapshot(
                        precipitationProbability = overlapIndices.mapNotNull { i -> hourly.precipitationProbability?.getOrNull(i) }.maxOrNull(),
                        minTemp = overlapIndices.mapNotNull { i -> hourly.temperature.getOrNull(i) }.minOrNull(),
                        maxTemp = overlapIndices.mapNotNull { i -> hourly.temperature.getOrNull(i) }.maxOrNull(),
                        windSpeed = null,
                        uvIndex = null,
                        conditionSummary = WeatherMapper.getWeatherCondition(
                            overlapIndices.mapNotNull { i -> hourly.weatherCode.getOrNull(i) }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                        ),
                        generatedAt = System.currentTimeMillis()
                    )
                    Log.i(TAG, "SnapshotProducedFrom=hourly")
                    analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
                }
            }

            // 3. Fallback to Repository Cache or Current API
            if (snapshot == null) {
                val lastWeather = repository.currentWeatherState.value
                if (lastWeather != null && (lastWeather.cityName.lowercase().contains(plan.city.lowercase()) || normalizeCityName(lastWeather.cityName) == normalizeCityName(plan.city))) {
                   snapshot = ForecastSnapshot(
                       precipitationProbability = lastWeather.precipitationProbability,
                       minTemp = lastWeather.low.filter { it.isDigit() || it == '-' }.toDoubleOrNull(),
                       maxTemp = lastWeather.high.filter { it.isDigit() || it == '-' }.toDoubleOrNull(),
                       windSpeed = lastWeather.windSpeed,
                       uvIndex = lastWeather.uvIndex?.toDouble(),
                       conditionSummary = lastWeather.condition,
                       generatedAt = System.currentTimeMillis()
                   )
                   Log.i(TAG, "SnapshotProducedFrom=repository_cache")
                   analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
                } else if (current != null) {
                    snapshot = ForecastSnapshot(
                        precipitationProbability = null,
                        minTemp = current.temperature,
                        maxTemp = current.temperature,
                        windSpeed = current.windSpeed,
                        uvIndex = null,
                        conditionSummary = WeatherMapper.getWeatherCondition(current.weatherCode ?: 0),
                        generatedAt = System.currentTimeMillis()
                    )
                    Log.i(TAG, "SnapshotProducedFrom=current_api")
                    analysisStatus = TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY
                }
            }

            if (snapshot == null) {
                Log.e(TAG, "FinalStatus=WEATHER_FAILED (Reason: NO_DATA_OVERLAP)")
                return plan.copy(
                    isAnalyzing = false,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_FAILED,
                    lastWeatherAnalysisText = "Hava verisi seyahat tarihleri için alınamadı. Yeniden analiz deneyin.",
                    lastWeatherAnalysisDate = System.currentTimeMillis()
                )
            }

            Log.i(TAG, "AnalysisGenerationStarted=true")
            Log.d(TAG, "GeminiRepository.generateAnalysis() / AI Helper triggered for ${plan.city}")
            val previousAnalysis = plan.analyses.lastOrNull()
            val score = calculateTravelScore(snapshot, plan.tripType)
            val comparisonText = if (previousAnalysis != null) generateComparisonSummary(previousAnalysis, score, snapshot) else null

            var aiGenerationFailed = false
            val aiResult = try {
                TravelAiHelper.generateTravelAiSuggestion(plan.city, plan.tripType, snapshot, plan.lastForecastSnapshot, daysUntil)
            } catch (e: Exception) {
                Log.e(TAG, "AnalysisGenerationFailed=true message=${e.message}")
                Log.e(TAG, "AI Helper stacktrace:\n${Log.getStackTraceString(e)}")
                aiGenerationFailed = true
                "Hava verisi alındı, ancak AI analizi geçici olarak hazırlanamadı. Mevcut tahmin: ${snapshot.conditionSummary}, ${snapshot.maxTemp?.toInt()}°."
            }

            if (aiGenerationFailed && analysisStatus == TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY) {
                analysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_AI_FAILED
            }

            val newAnalysis = TravelWeatherAnalysis(
                tripId = plan.id,
                travelScore = score,
                rainRiskPercent = snapshot.precipitationProbability,
                windRiskPercent = snapshot.windSpeed?.let { (it * 2).toInt().coerceAtMost(100) },
                uvRiskPercent = snapshot.uvIndex?.let { (it * 10).toInt().coerceAtMost(100) },
                averageTemperature = ((snapshot.minTemp ?: 0.0) + (snapshot.maxTemp ?: 0.0)) / 2.0,
                summary = "Hava '${snapshot.conditionSummary}', yağmur ihtimali %${snapshot.precipitationProbability ?: "Bilinmiyor"}",
                recommendation = aiResult,
                comparisonText = comparisonText,
                previousAnalysisId = previousAnalysis?.id
            )

            Log.i(TAG, "AnalysisGenerationSuccess=${!aiGenerationFailed}")
            Log.i(TAG, "FinalStatus=$analysisStatus")

            return plan.copy(
                isAnalyzing = false,
                lastWeatherAnalysisText = when(analysisStatus) {
                    TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY -> "Temel hava analizi hazır"
                    TravelWeatherAnalysisStatus.WEATHER_READY_AI_FAILED -> "Hava verisi alındı (AI kısıtlı)"
                    else -> "Gelişmiş analiz hazır"
                },
                aiSuggestion = newAnalysis.recommendation,
                lastWeatherAnalysisDate = System.currentTimeMillis(),
                previousForecastSnapshot = plan.lastForecastSnapshot,
                lastForecastSnapshot = snapshot,
                weatherAnalysisStatus = analysisStatus,
                analyses = plan.analyses + newAnalysis,
                latitude = lat,
                longitude = lon
            )
        } catch (e: Exception) {
            Log.e(TAG, "FinalStatus=WEATHER_FAILED (Exception: ${e.javaClass.simpleName})")
            Log.e(TAG, "Exception=${e.message}")
            Log.e(TAG, "StackTrace=${Log.getStackTraceString(e)}")

            val errorMessage = when (e) {
                is java.net.UnknownHostException -> "Hava verisi alınamadı: İnternet bağlantınızı kontrol edin."
                else -> "Hava verisi alınamadı. Yeniden analiz deneyin."
            }

            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_FAILED,
                lastWeatherAnalysisText = errorMessage,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }
    }

    private fun calculateTravelScore(snapshot: ForecastSnapshot, type: TripType): Int {
        var score = 90
        val precip = snapshot.precipitationProbability ?: 0
        val wind = snapshot.windSpeed ?: 0.0
        val temp = snapshot.maxTemp ?: 20.0

        if (precip > 50) score -= 30 else if (precip > 20) score -= 10
        if (wind > 40) score -= 20 else if (wind > 25) score -= 10

        when(type) {
            TripType.BEACH -> if (temp < 25) score -= 20 else if (temp > 35) score -= 5
            TripType.WINTER -> if (temp > 5) score -= 30
            TripType.CAMPING -> if (temp < 10) score -= 25
            else -> {}
        }

        return score.coerceIn(0, 100)
    }

    private fun generateComparisonSummary(old: TravelWeatherAnalysis, newScore: Int, newSnapshot: ForecastSnapshot): String {
        val sb = StringBuilder()
        val scoreDiff = newScore - old.travelScore
        if (scoreDiff != 0) {
            val direction = if (scoreDiff > 0) "iyileşti" else "düştü"
            sb.append("Seyahat skoru önceki analize göre %${kotlin.math.abs(scoreDiff)} $direction. ")
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
            sb.append("Yağış riski %${kotlin.math.abs(rainDiff)} $direction.")
        }

        return sb.toString().trim()
    }

    fun savePlan(plan: TravelPlan) {
        viewModelScope.launch {
            dao.insertTravelPlan(plan.toEntity())
            analyzeTravelWeather(plan)
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
