package com.havamania

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

import java.time.temporal.ChronoUnit

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()

    private val repository: WeatherRepository by lazy {
        WeatherRepository(weatherDao = dao)
    }

    private val apiService = NetworkModule.apiService

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
            // Set status to LOADING immediately in the list
            _plans.value = _plans.value.map {
                if (it.id == plan.id) it.copy(isAnalyzing = true, weatherAnalysisStatus = TravelWeatherAnalysisStatus.LOADING) else it
            }

            val updatedPlan = performAnalysis(plan)

            // Save updated plan to DB
            dao.insertTravelPlan(updatedPlan.toEntity())

            // Refresh the entire list from DB to ensure UI is in sync
            loadPlans()
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
                lastWeatherAnalysisText = "Bu seyahat geçmişte tamamlandı.",
                aiSuggestion = suggestion,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        // Case: More than 15 days until trip
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
                lastWeatherAnalysisText = "Bu seyahat için güvenilir hava tahmini henüz erken. Seyahate 15 gün kala hava analizini başlatacağım.",
                aiSuggestion = suggestion,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }

        // Case: Within 15 days (or past)
        return try {
            if (plan.latitude == 0.0 && plan.longitude == 0.0) {
                return plan.copy(
                    isAnalyzing = false,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.ERROR,
                    lastWeatherAnalysisText = "Şehir koordinatları bulunamadı. Lütfen şehri listeden seçerek tekrar ekleyin.",
                    lastWeatherAnalysisDate = System.currentTimeMillis()
                )
            }

            // Open-Meteo free tier provides up to 16 days forecast
            val response = apiService.getFullWeather(
                lat = plan.latitude,
                lon = plan.longitude,
                days = 16
            )

            val daily = response.daily ?: return plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.ERROR,
                lastWeatherAnalysisText = "Hava verisi şu an alınamıyor.",
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
            val tripDates = daily.time.map { LocalDate.parse(it) }

            // Find indices that overlap with trip duration
            val overlapIndices = tripDates.indices.filter { i ->
                val date = tripDates[i]
                !date.isBefore(plan.startDate) && !date.isAfter(plan.endDate)
            }

            if (overlapIndices.isEmpty()) {
                val suggestion = TravelAiHelper.generateTravelAiSuggestion(
                    city = plan.city,
                    tripType = plan.tripType,
                    forecastSnapshot = null,
                    previousSnapshot = null,
                    daysUntilTrip = daysUntil
                )
                return plan.copy(
                    isAnalyzing = false,
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.ERROR,
                    lastWeatherAnalysisText = "Güncel hava verisi şu anda alınamadı.",
                    aiSuggestion = suggestion,
                    lastWeatherAnalysisDate = System.currentTimeMillis()
                )
            }

            // Generate snapshot
            val currentSnapshot = ForecastSnapshot(
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

            val start = plan.startDate
            val end = plan.endDate

            val tripStatusText = when {
                end.isBefore(today) -> "Bu seyahat tamamlandı."
                start.isBefore(today) && !end.isBefore(today) -> "Seyahatiniz devam ediyor."
                start.isEqual(today) -> "Seyahatiniz bugün başlıyor."
                start.isEqual(today.plusDays(1)) -> "Seyahatinize yarın çıkıyorsunuz."
                start.isAfter(today) -> "Seyahatinize ${ChronoUnit.DAYS.between(today, start)} gün kaldı."
                else -> ""
            }

            val analysisText = "$tripStatusText Tahminlere göre hava '${currentSnapshot.conditionSummary}', " +
                    "yağmur ihtimali %${currentSnapshot.precipitationProbability ?: 0}, " +
                    "sıcaklık ${currentSnapshot.minTemp?.toInt() ?: 0}-${currentSnapshot.maxTemp?.toInt() ?: 0}° aralığında."

            val aiSuggestion = TravelAiHelper.generateTravelAiSuggestion(
                city = plan.city,
                tripType = plan.tripType,
                forecastSnapshot = currentSnapshot,
                previousSnapshot = plan.lastForecastSnapshot,
                daysUntilTrip = daysUntil
            )

            plan.copy(
                isAnalyzing = false,
                lastWeatherAnalysisText = analysisText,
                aiSuggestion = aiSuggestion,
                lastWeatherAnalysisDate = System.currentTimeMillis(),
                previousForecastSnapshot = plan.lastForecastSnapshot,
                lastForecastSnapshot = currentSnapshot,
                weatherAnalysisStatus = if (plan.lastForecastSnapshot == null) TravelWeatherAnalysisStatus.ANALYZED else TravelWeatherAnalysisStatus.UPDATED
            )
        } catch (e: Exception) {
            android.util.Log.e("TravelViewModel", "Hava durumu analizi başarısız", e)

            val errorMessage = when {
                e is java.net.UnknownHostException -> "İnternet bağlantısı kurulamadı."
                e.message?.contains("API") == true || e.message?.contains("weather") == true -> "Bu tarih için güncel hava verisi henüz alınamadı."
                else -> "Gelişmiş analiz şu an hazırlanamadı. Temel seyahat önerilerini yine de görüntüleyebilirsin."
            }

            plan.copy(
                isAnalyzing = false,
                weatherAnalysisStatus = TravelWeatherAnalysisStatus.ERROR,
                lastWeatherAnalysisText = errorMessage,
                lastWeatherAnalysisDate = System.currentTimeMillis()
            )
        }
    }

    fun savePlan(plan: TravelPlan) {
        viewModelScope.launch {
            // Save the plan to DB
            dao.insertTravelPlan(plan.toEntity())

            // Trigger automatic analysis
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
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.TOO_EARLY },
        isArchived = isArchived
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
        isArchived = isArchived
    )
}
