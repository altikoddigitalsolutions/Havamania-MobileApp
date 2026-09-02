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
    private val geocoder = ReverseGeocoder(application)
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
        _routeState.value = null
        _waypoints.value = emptyList()
        _startWeather.value = null
        _endWeather.value = null
        _isAnalyzing.value = false
        _isAnalyzed.value = false
        _errorMessage.value = null
        _departureMillis.value = null

        viewModelScope.launch {
            Log.d("RouteVM", "Observing trip: $tripId")
            dao.getTravelPlanByIdFlow(tripId).collect { entity ->
                if (entity != null) {
                    val plan = entity.toDomain()
                    _trip.value = plan

                    val departureDateTime = plan.departureDateTime
                    if (departureDateTime != null) {
                        _departureMillis.value = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } else {
                        _departureMillis.value = null
                    }

                    // Only recalculate route if essential fields changed or first load
                    calculateRoute(plan)
                } else {
                    _errorMessage.value = "Seyahat bulunamadı."
                }
            }
        }
    }

    private fun calculateRoute(plan: TravelPlan) {
        Log.d("RouteVM", "Calculating route. OriginCity=${plan.originCity}, OriginLat=${plan.originLatitude}, OriginLon=${plan.originLongitude}")
        viewModelScope.launch {
            var origin = plan.originPoint?.let { GeoPoint(it.first, it.second) }

            if (origin == null) {
                try {
                    val currentLoc = locationTracker.getCurrentCity()
                    if (currentLoc != null) {
                        origin = GeoPoint(currentLoc.latitude, currentLoc.longitude)
                    } else {
                        _errorMessage.value = "Rotayı oluşturmak için başlangıç konumuna ihtiyacımız var. Lütfen cihaz konumunu açın veya bir kalkış yeri seçin."
                        _routeState.value = RouteResult.Error("Konum alınamadı")
                        return@launch
                    }
                } catch (e: SecurityException) {
                    _errorMessage.value = "Konum izni verilmedi. Rotayı görmek için izin vermelisin."
                    _routeState.value = RouteResult.Error("İzin hatası")
                    return@launch
                } catch (e: Exception) {
                    _errorMessage.value = "Konum hizmetine ulaşılamıyor."
                    _routeState.value = RouteResult.Error("GPS hatası")
                    return@launch
                }
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
            // 1. Denser sampling for thorough risk analysis
            val denseWaypoints = RouteSampler.denseSampleForAnalysis(routeResult.route)
            val denseWithEtas = EtaCalculator.assignEtas(routeResult.route, denseWaypoints, depMillis)

            // 2. Prepare batch points (Start + Dense + End)
            val allPoints = mutableListOf<GeoPoint>()
            val allEtas = mutableListOf<Long?>()

            routeResult.route.origin?.let { allPoints.add(it) }
            allEtas.add(depMillis)

            denseWithEtas.forEach {
                allPoints.add(it.location)
                allEtas.add(it.etaEpochMillis)
            }

            routeResult.route.destination?.let { allPoints.add(it) }
            allEtas.add(depMillis + (routeResult.route.durationSeconds * 1000).toLong())

            // 3. Fetch weather for ALL points
            val weatherResults = weatherProvider.weatherAtBatch(allPoints, allEtas)
            if (weatherResults.isEmpty()) return

            _startWeather.value = weatherResults.first()
            _endWeather.value = weatherResults.last()

            val middleWeather = weatherResults.drop(1).dropLast(1)
            val denseWithWeather = denseWithEtas.mapIndexed { idx, wp ->
                wp.copy(weather = middleWeather.getOrNull(idx))
            }

            // 4. Identify Hazards
            val hazards = denseWithWeather.filter { it.weather?.risk != RouteRisk.OK }

            // 5. Normal Adaptive Sampling (the "skeleton" of the timeline)
            val normalIntermediate = RouteSampler.sample(routeResult.route)
            val normalWithEtas = EtaCalculator.assignEtas(routeResult.route, normalIntermediate, depMillis)
            val normalWithWeather = normalWithEtas.map { wp ->
                // Find nearest weather from dense list to avoid extra network calls
                val nearest = denseWithWeather.minByOrNull { GeoMath.haversineMeters(it.location, wp.location) }
                wp.copy(weather = nearest?.weather)
            }

            // 6. Merge & Deduplicate (Strategic sampling)
            val combined = (normalWithWeather + hazards).sortedBy { it.cumulativeDistanceMeters }
            val finalWaypoints = mutableListOf<RouteWaypoint>()

            combined.forEach { wp ->
                // Use a larger threshold (50km) for normal points, but smaller (20km) to keep hazards
                val threshold = if (wp.weather?.risk != RouteRisk.OK) 20_000.0 else 50_000.0
                val tooClose = finalWaypoints.any {
                    GeoMath.haversineMeters(it.location, wp.location) < threshold
                }
                if (!tooClose) finalWaypoints.add(wp)
            }

            // Limit total intermediate points to 7 to prevent overflow/noise
            val limitedWaypoints = finalWaypoints.take(7)

            // 7. Resolve Location Names (only for final visible points)
            val geocodedWaypoints = limitedWaypoints.map { wp ->
                val name = geocoder.placeName(wp.location) ?: "Ara Nokta"
                wp.copy(placeName = name)
            }

            _waypoints.value = geocodedWaypoints
            _isAnalyzed.value = true

        } catch (e: Exception) {
            Log.e("RouteVM", "Analysis failed", e)
            _errorMessage.value = "Hava durumu analizi başarısız oldu."
        } finally {
            _isAnalyzing.value = false
        }
    }
}
