package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

import java.time.temporal.ChronoUnit

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()

    private val repository = WeatherRepository.getInstance(application)

    private val apiService = NetworkModule.apiService
    private val aiService = AltikodChatFactory.create()
    private val botId = "1"

    private val _plans = MutableStateFlow<List<TravelPlan>>(emptyList())
    val plans: StateFlow<List<TravelPlan>> = _plans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _citySuggestions = MutableStateFlow<List<GeocodingResultDto>>(emptyList())
    val citySuggestions: StateFlow<List<GeocodingResultDto>> = _citySuggestions.asStateFlow()

    private val TAG = "TravelVM"

    init {
        seedInitialDataIfNeeded()
    }

    fun seedInitialDataIfNeeded(force: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = if (force) emptyList() else dao.getAllTravelPlans()
            if (entities.isEmpty()) {
                if (force) dao.clearAllTravelPlans()
                val today = LocalDate.now()
                // Yılları dinamik ayarla: Bazıları geçmişte kalsın, bazıları gelecekte
                val currentYear = today.year

                val seedPlans = listOf(
                    // Yaklaşanlar (Bugün veya Gelecek)
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
                    // Geçmiş (Bitiş tarihi bugünden önce) - Tarihleri bugüne göre ayarla
                    TravelPlan(
                        city = "İzmir",
                        latitude = 38.4192,
                        longitude = 27.1287,
                        tripType = TripType.SHOPPING,
                        startDate = today.minusMonths(2),
                        endDate = today.minusMonths(2).plusDays(15),
                        lastForecastSnapshot = ForecastSnapshot(
                            precipitationProbability = 10,
                            minTemp = 14.0,
                            maxTemp = 26.0,
                            windSpeed = 12.0,
                            uvIndex = 5.0,
                            conditionSummary = "Güneşli"
                        )
                    ),
                    TravelPlan(
                        city = "Balıkesir",
                        latitude = 39.6484,
                        longitude = 27.8826,
                        tripType = TripType.VACATION,
                        startDate = today.minusMonths(1),
                        endDate = today.minusMonths(1).plusDays(9),
                        lastForecastSnapshot = ForecastSnapshot(
                            precipitationProbability = 45,
                            minTemp = 10.0,
                            maxTemp = 18.0,
                            windSpeed = 22.0,
                            uvIndex = 3.0,
                            conditionSummary = "Hafif Yağmurlu"
                        )
                    ),
                    TravelPlan(
                        city = "Ankara",
                        latitude = 39.9334,
                        longitude = 32.8597,
                        tripType = TripType.ROAD_TRIP,
                        startDate = today.minusDays(20),
                        endDate = today.minusDays(18),
                        lastForecastSnapshot = ForecastSnapshot(
                            precipitationProbability = 5,
                            minTemp = 6.0,
                            maxTemp = 15.0,
                            windSpeed = 8.0,
                            uvIndex = 2.0,
                            conditionSummary = "Açık"
                        )
                    ),
                    // Arşiv
                    TravelPlan(
                        city = "Çankırı",
                        latitude = 40.6013,
                        longitude = 33.6134,
                        tripType = TripType.CAMPING,
                        startDate = LocalDate.of(currentYear, 5, 8),
                        endDate = LocalDate.of(currentYear, 5, 9),
                        isArchived = true,
                        lastForecastSnapshot = ForecastSnapshot(
                            precipitationProbability = 60,
                            minTemp = 8.0,
                            maxTemp = 16.0,
                            windSpeed = 28.0,
                            uvIndex = 4.0,
                            conditionSummary = "Gök Gürültülü Sağanak"
                        )
                    )
                )

                seedPlans.forEach { plan ->
                    dao.insertTravelPlan(plan.toEntity())
                }
                android.util.Log.d(TAG, "Seed trips inserted: ${seedPlans.size}")
            }
            loadPlans()
        }
    }

    fun loadPlans() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoading.value = true
            val entities = dao.getAllTravelPlans()
            val domainPlans = entities.map { it.toDomain() }.sortedBy { it.startDate }
            _plans.value = domainPlans
            _isLoading.value = false
            android.util.Log.d(TAG, "Trip UI list size: ${domainPlans.size}")
        }
    }

    fun analyzeTravelWeather(plan: TravelPlan) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 3
            var success = false
            var finalPlan = plan

            // Set status to LOADING immediately for UI feedback
            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(isAnalyzing = true, weatherAnalysisStatus = TravelWeatherAnalysisStatus.LOADING) else it
            }

            while (attempts < maxAttempts && !success) {
                attempts++
                android.util.Log.d(TAG, "[TravelAnalysis] Analyzing plan for ${plan.city}, attempt $attempts/$maxAttempts")

                val result = performAnalysis(plan)

                if (result.weatherAnalysisStatus == TravelWeatherAnalysisStatus.ANALYZED &&
                    !result.analysis.isNullOrBlank()) {
                    finalPlan = result
                    success = true
                    android.util.Log.i(TAG, "[TravelAnalysis] Success for ${plan.city} at attempt $attempts")
                } else {
                    finalPlan = result
                    if (attempts < maxAttempts) {
                        android.util.Log.w(TAG, "[TravelAnalysis] Attempt $attempts failed, retrying in 2s...")
                        delay(2000) // Wait before retry
                    }
                }
            }

            // Save final result to DB
            try {
                dao.insertTravelPlan(finalPlan.toEntity())
                android.util.Log.d(TAG, "[TravelAnalysis] Final plan state saved to database for ${plan.city}")
                loadPlans()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "[TravelRepositoryError] Failed to save analysis result to database: ${e.message}", e)
            }

            if (!success) {
                android.util.Log.e(TAG, "[TravelAnalysis] Analysis failed after $maxAttempts attempts for ${plan.city}")
            }
        }
    }

    suspend fun performAnalysis(plan: TravelPlan): TravelPlan {
        val today = LocalDate.now()
        val isPastTrip = plan.endDate.isBefore(today)
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()

        // Case: Past trip
        if (isPastTrip) {
            val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city,
                tripType = plan.tripType,
                forecastSnapshot = null,
                previousSnapshot = null,
                daysUntilTrip = daysUntil,
                isPastTrip = true,
                endDate = plan.endDate
            )
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.ANALYZED,
                lastWeatherAnalysisText = suggestion,
                aiSuggestion = suggestion,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        // Case: Within 15 days (Current Data) or more (15-day rule)
        val snapshot = if (daysUntil <= 15) {
            try {
                val response = apiService.getFullWeather(
                    lat = plan.latitude,
                    lon = plan.longitude,
                    days = 16
                )
                val daily = response.daily
                if (daily != null) {
                    val tripDates = daily.time.map { LocalDate.parse(it) }
                    val overlapIndices = tripDates.indices.filter { i ->
                        val date = tripDates[i]
                        !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
                    }

                    if (overlapIndices.isNotEmpty()) {
                        ForecastSnapshot(
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
                    } else null
                } else null
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Weather fetch failed for analysis: ${e.message}")
                null
            }
        } else null

        // 15-day rule check: Only perform AI analysis if trip is within 15 days
        if (daysUntil > 15) {
             val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city,
                tripType = plan.tripType,
                forecastSnapshot = null,
                previousSnapshot = null,
                daysUntilTrip = daysUntil
            )
            return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.TOO_EARLY,
                lastWeatherAnalysisText = suggestion,
                analysis = suggestion,
                aiSuggestion = null, // Clear aiSuggestion to avoid double text
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        // Trigger Real AI Analysis via Altikod service
        return try {
            val prompt = buildAnalysisPrompt(plan, snapshot, daysUntil)
            android.util.Log.d(TAG, "[AI_REQUEST] Sending prompt for ${plan.city}: $prompt")

            val response = aiService.sendMessage(botId, AltikodChatRequest(prompt, UUID.randomUUID().toString()))
            val rawAnswer = response.answer

            // Clean up AI response
            var cleanAnswer = rawAnswer.replace(Regex("\\*|_|#|-"), "").trim()

            // "Lütfen geçerli bir soru sorunuz" gibi chatbot hata mesajlarını filtrele
            if (cleanAnswer.contains("geçerli bir soru", ignoreCase = true) || cleanAnswer.isBlank()) {
                android.util.Log.e(TAG, "[AI_FILTER] AI returned invalid question error or blank: $cleanAnswer")
                throw Exception("Invalid AI response")
            }

            android.util.Log.d(TAG, "[AI_RESPONSE] Received for ${plan.city}: $cleanAnswer")

            plan.copy(
                isAnalyzing = false,
                lastWeatherAnalysisText = cleanAnswer,
                analysis = cleanAnswer, // Kalıcı analiz alanına kaydet
                aiSuggestion = null, // Clear aiSuggestion to avoid double text
                lastWeatherAnalysisDate = System.currentTimeMillis(),
                lastForecastSnapshot = snapshot ?: plan.lastForecastSnapshot,
                previousForecastSnapshot = if (snapshot != null) plan.lastForecastSnapshot else plan.previousForecastSnapshot,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.ANALYZED
            )
        } catch (e: Exception) {
            val errorType = when (e) {
                is java.net.UnknownHostException, is java.net.ConnectException -> "NETWORK_ERROR"
                is retrofit2.HttpException -> "API_ERROR_${e.code()}"
                is kotlinx.serialization.SerializationException -> "PARSE_ERROR"
                else -> "UNKNOWN_ERROR"
            }

            val errorDetail = when (e) {
                is retrofit2.HttpException -> "Status: ${e.code()}, Message: ${e.message()}"
                else -> e.message ?: "No error message"
            }

            android.util.Log.e(TAG, "[TravelAnalysisError] [$errorType] ${plan.city}: $errorDetail", e)

            // Local Fallback if AI fails
            val fallbackSuggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city,
                tripType = plan.tripType,
                forecastSnapshot = snapshot,
                previousSnapshot = plan.lastForecastSnapshot,
                daysUntilTrip = daysUntil
            )

            plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.ANALYZED, // Error olsa bile fallback ile ANALYZED gösteriyoruz ki kullanıcıya temiz metin gitsin
                lastWeatherAnalysisText = fallbackSuggestion,
                analysis = fallbackSuggestion,
                aiSuggestion = null, // Clear aiSuggestion to avoid double text
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }
    }

    private fun buildAnalysisPrompt(plan: TravelPlan, snapshot: ForecastSnapshot?, daysUntil: Int): String {
        val dateStr = "${plan.startDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))} - ${plan.endDate.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))}"
        val weatherInfo = if (snapshot != null) {
            """
            HAVA TAHMİNİ VERİLERİ:
            Durum: ${snapshot.conditionSummary}
            Sıcaklık: ${snapshot.minTemp?.toInt() ?: "?"}-${snapshot.maxTemp?.toInt() ?: "?"}°C
            Yağış İhtimali: %${snapshot.precipitationProbability ?: 0}
            Rüzgar: ${snapshot.windSpeed ?: 0} km/s
            UV İndeksi: ${snapshot.uvIndex ?: 0}
            """.trimIndent()
        } else {
            "Hava durumu verisi henüz net değil (Seyahate $daysUntil gün var)."
        }

        return """
            ${plan.city} şehri için $dateStr tarihleri arasında ${plan.tripType.label} amaçlı bir seyahat planlanıyor. Seyahate $daysUntil gün kaldı.

            Aşağıdaki hava durumu verilerini analiz ederek kullanıcıya doğal, premium ve samimi bir dille kısa bir seyahat analizi hazırla:

            $weatherInfo

            TALİMATLAR:
            1. "Seyahatine $daysUntil gün kaldı" cümlesiyle başla.
            2. Hava koşullarına göre kıyafet ve valiz önerisi yap.
            3. ${plan.tripType.label} seyahat tipine uygun aktivite veya dikkat edilmesi gereken bir risk varsa belirt.
            4. Markdown sembolleri (*, #, _, -) kullanma, sadece düz metin olsun.
            5. En fazla 4 cümle olsun.
            6. Yanıtını doğrudan analiz metni olarak ver, "İşte analiziniz" gibi girişler yapma.

            Bu bilgilere dayanarak seyahat için profesyonel bir öneri hazırlar mısın?
        """.trimIndent()
    }

    fun savePlan(plan: TravelPlan) {
        viewModelScope.launch {
            try {
                // Save the plan to DB
                dao.insertTravelPlan(plan.toEntity())

                // Trigger automatic analysis
                analyzeTravelWeather(plan)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database save hatası: Seyahat planı kaydedilemedi.", e)
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
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.TOO_EARLY },
        isArchived = isArchived,
        analysis = analysis
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
        analysis = analysis
    )
}
