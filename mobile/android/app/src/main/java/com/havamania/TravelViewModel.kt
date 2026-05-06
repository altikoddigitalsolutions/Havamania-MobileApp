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

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            val entities = dao.getAllTravelPlans()
            _plans.value = entities.map { it.toDomain() }.sortedBy { it.startDate }
            _isLoading.value = false
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
        val daysUntil = ChronoUnit.DAYS.between(today, plan.startDate).toInt()

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
                lastWeatherAnalysisText = "Bu tarih için hava durumu tahmini henüz erken. Seyahate 15 gün kala hava analizini başlatacağım.",
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
                    weatherAnalysisStatus = TravelWeatherAnalysisStatus.READY,
                    lastWeatherAnalysisText = "Bu tarih için hava verisi henüz API tarafından sağlanmıyor.",
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

            val daysText = when {
                daysUntil == 0 -> "bugün"
                daysUntil < 0 -> "geçmişte"
                daysUntil == 1 -> "yarın"
                else -> "$daysUntil gün"
            }

            val analysisText = "Seyahatine $daysText kaldı. Tahminlere göre hava '${currentSnapshot.conditionSummary}', " +
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
                lastForecastSnapshot = currentSnapshot,
                weatherAnalysisStatus = if (plan.lastForecastSnapshot == null) TravelWeatherAnalysisStatus.ANALYZED else TravelWeatherAnalysisStatus.UPDATED
            )
        } catch (e: Exception) {
            android.util.Log.e("TravelViewModel", "Hava durumu analizi başarısız", e)

            val errorMessage = when {
                e is java.net.UnknownHostException -> "İnternet bağlantısı kurulamadı."
                else -> "Analiz hatası oluştu."
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
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = try { TravelWeatherAnalysisStatus.valueOf(weatherAnalysisStatus) } catch (e: Exception) { TravelWeatherAnalysisStatus.TOO_EARLY }
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
        nextAnalysisEligibleDate = nextAnalysisEligibleDate,
        weatherAnalysisStatus = weatherAnalysisStatus.name
    )
}
