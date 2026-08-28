package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

class TravelRouteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WeatherDatabase.getDatabase(application)
    private val dao = database.weatherDao()
    private val routeProvider = OsrmRouteProvider()
    private val weatherProvider = RouteWeatherProvider()
    private val locationTracker: LocationTracker = DefaultLocationTracker(
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(application),
        application
    )

    private val _trip = MutableStateFlow<TravelPlan?>(null)
    val trip: StateFlow<TravelPlan?> = _trip.asStateFlow()

    private val _routeState = MutableStateFlow<RouteResult?>(null)
    val routeState: StateFlow<RouteResult?> = _routeState.asStateFlow()

    private val _waypoints = MutableStateFlow<List<RouteWaypoint>>(emptyList())
    val waypoints: StateFlow<List<RouteWaypoint>> = _waypoints.asStateFlow()

    private val _startWeather = MutableStateFlow<WaypointWeather?>(null)
    val startWeather: StateFlow<WaypointWeather?> = _startWeather.asStateFlow()

    private val _endWeather = MutableStateFlow<WaypointWeather?>(null)
    val endWeather: StateFlow<WaypointWeather?> = _endWeather.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isAnalyzed = MutableStateFlow(false)
    val isAnalyzed: StateFlow<Boolean> = _isAnalyzed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _departureMillis = MutableStateFlow<Long?>(null)
    val departureMillis: StateFlow<Long?> = _departureMillis.asStateFlow()

    fun loadTrip(tripId: String) {
        _trip.value = null
        _routeState.value = null
        _waypoints.value = emptyList()
        _startWeather.value = null
        _endWeather.value = null
        _isAnalyzing.value = false
        _isAnalyzed.value = false
        _errorMessage.value = null
        _departureMillis.value = null

        viewModelScope.launch {
            Log.d("RouteVM", "Loading trip: $tripId")
            val entity = dao.getTravelPlanById(tripId)
            if (entity != null) {
                val plan = entity.toDomain()
                _trip.value = plan

                val departureDateTime = plan.departureDateTime
                if (departureDateTime != null) {
                    _departureMillis.value = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                calculateRoute(plan)
            } else {
                _errorMessage.value = "Seyahat bulunamadı."
            }
        }
    }

    private fun calculateRoute(plan: TravelPlan) {
        viewModelScope.launch {
            var origin = plan.originPoint?.let { GeoPoint(it.first, it.second) }

            if (origin == null) {
                val currentLoc = locationTracker.getCurrentCity()
                if (currentLoc != null) {
                    origin = GeoPoint(currentLoc.latitude, currentLoc.longitude)
                }
            }

            if (origin == null) {
                Log.e("RouteVM", "No origin available")
                _routeState.value = RouteResult.Error("Kalkış noktası belirlenemedi. Lütfen bir kalkış yeri seçin.")
                return@launch
            }

            val dest = GeoPoint(plan.latitude, plan.longitude)
            val result = routeProvider.getRoute(origin, dest)
            _routeState.value = result

            if (result is RouteResult.Success) {
                val sampled = RouteSampler.sample(result.route)
                _waypoints.value = sampled
            }
        }
    }

    suspend fun analyzeWeather() {
        val plan = _trip.value ?: return
        val routeResult = _routeState.value as? RouteResult.Success ?: return
        val depMillis = _departureMillis.value ?: return

        _isAnalyzing.value = true
        try {
            val waypointsWithEtas = EtaCalculator.assignEtas(
                routeResult.route,
                _waypoints.value,
                depMillis
            )

            val points = mutableListOf<GeoPoint>()
            val etas = mutableListOf<Long?>()

            routeResult.route.origin?.let { points.add(it) }
            etas.add(depMillis)

            waypointsWithEtas.forEach {
                points.add(it.location)
                etas.add(it.etaEpochMillis)
            }

            routeResult.route.destination?.let { points.add(it) }
            etas.add(depMillis + (routeResult.route.durationSeconds * 1000).toLong())

            val weatherResults = weatherProvider.weatherAtBatch(points, etas)

            if (weatherResults.isNotEmpty()) {
                _startWeather.value = weatherResults[0]
                _endWeather.value = weatherResults.last()

                val middleWeather = weatherResults.drop(1).dropLast(1)
                _waypoints.value = waypointsWithEtas.mapIndexed { index, wp ->
                    wp.copy(weather = middleWeather.getOrNull(index))
                }
                _isAnalyzed.value = true
            }
        } catch (e: Exception) {
            Log.e("RouteVM", "Analysis failed", e)
            _errorMessage.value = "Hava durumu analizi başarısız oldu."
        } finally {
            _isAnalyzing.value = false
        }
    }
}
