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

class TravelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()

    private val repository: WeatherRepository by lazy {
        WeatherRepository(weatherDao = dao)
    }

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
            _plans.value = entities.map { it.toDomain() }
            _isLoading.value = false
        }
    }

    fun savePlan(plan: TravelPlan) {
        viewModelScope.launch {
            dao.insertTravelPlan(plan.toEntity())
            loadPlans()
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
        tripType = try { TripType.valueOf(tripType) } catch (e: Exception) { TripType.OTHER },
        startDate = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        endDate = Instant.ofEpochMilli(endDate).atZone(ZoneId.systemDefault()).toLocalDate(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion
    )

    private fun TravelPlan.toEntity() = TravelPlanEntity(
        id = id,
        city = city,
        tripType = tripType.name,
        startDate = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        endDate = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        createdAt = createdAt,
        weatherSummary = weatherSummary,
        aiSuggestion = aiSuggestion
    )
}
