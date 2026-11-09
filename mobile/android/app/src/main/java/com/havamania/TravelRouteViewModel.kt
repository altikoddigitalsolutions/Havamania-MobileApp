package com.havamania

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.Job
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

    private val analysis = RouteAnalysisRun()
    val isAnalyzing = analysis.running

    private val _isAnalyzed = MutableStateFlow(false)
    val isAnalyzed: StateFlow<Boolean> = _isAnalyzed.asStateFlow()

    private val _hasIncompleteForecast = MutableStateFlow(false)
    val hasIncompleteForecast: StateFlow<Boolean> = _hasIncompleteForecast.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _departureMillis = MutableStateFlow<Long?>(null)
    val departureMillis: StateFlow<Long?> = _departureMillis.asStateFlow()

    private var tripJob: kotlinx.coroutines.Job? = null
    private var routeJob: kotlinx.coroutines.Job? = null

    override fun onCleared() {
        super.onCleared()
        analysis.cancel()
        tripJob?.cancel()
        routeJob?.cancel()
    }

    fun loadTrip(tripId: String) {
        tripJob?.cancel()
        resetRoute()
        _trip.value = null
        _departureMillis.value = null

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "legacy"
        tripJob = viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                Log.d("RouteVM", "Observing trip: $tripId")
            }
            dao.getTravelPlanByIdFlow(tripId, uid).collect { entity ->
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
                    resetRoute()
                    _trip.value = null
                    _routeState.value = null
                    _waypoints.value = emptyList()
                    _errorMessage.value = "Seyahat bulunamadı."
                }
            }
        }
    }

    private fun resetRoute() {
        analysis.cancel()
        routeJob?.cancel()
        routeJob = null
        _routeState.value = null
        _waypoints.value = emptyList()
        _startWeather.value = null
        _endWeather.value = null
        _isAnalyzed.value = false
        _hasIncompleteForecast.value = false
        _analysisError.value = null
        _errorMessage.value = null
    }

    private fun calculateRoute(plan: TravelPlan) {
        if (BuildConfig.DEBUG) {
            Log.d("RouteVM", "Calculating route.")
        }
        resetRoute()
        routeJob = viewModelScope.launch {
            var origin = plan.originPoint?.let { GeoPoint(it.first, it.second) }

            if (origin == null) {
                try {
                    origin = resolveDeviceRouteOrigin(locationTracker)
                    if (origin == null) {
                        _errorMessage.value = "Rotayı oluşturmak için başlangıç konumuna ihtiyacımız var. Lütfen cihaz konumunu açın veya bir kalkış yeri seçin."
                        _routeState.value = RouteResult.Error("Konum alınamadı")
                        return@launch
                    }
                } catch (e: SecurityException) {
                    _errorMessage.value = "Konum izni verilmedi. Rotayı görmek için izin vermelisin."
                    _routeState.value = RouteResult.Error("İzin hatası")
                    return@launch
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _errorMessage.value = "Konum hizmetine ulaşılamıyor."
                    _routeState.value = RouteResult.Error("GPS hatası")
                    return@launch
                }
            }

            val dest = GeoPoint(plan.latitude, plan.longitude)
            coroutineContext.ensureActive()
            val result = routeProvider.getRoute(origin, dest)
            coroutineContext.ensureActive()
            _routeState.value = result

            if (result is RouteResult.Success) {
                val sampled = RouteSampler.sample(result.route)
                _waypoints.value = sampled
            }
        }
    }

    suspend fun analyzeWeather(): Unit = coroutineScope {
        if (isAnalyzing.value) return@coroutineScope
        val analysisRouteJob = routeJob ?: return@coroutineScope
        val plan = _trip.value ?: return@coroutineScope
        val routeResult = _routeState.value as? RouteResult.Success ?: return@coroutineScope
        val depMillis = _departureMillis.value ?: return@coroutineScope

        if (!TravelStatusResolver.isRouteWeatherEligible(plan)) {
            _analysisError.value = "Bu seyahatin güzergâh hava analizi henüz kullanılamıyor. Kalkış tarihini ve saatini kontrol edin."
            return@coroutineScope
        }

        _analysisError.value = null
        val currentAnalysis = coroutineContext.job
        analysis.start(currentAnalysis)
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
            if (routeJob !== analysisRouteJob) return@coroutineScope
            if (weatherResults.all { it == null }) {
                _analysisError.value = "Güzergâh için hava tahmini alınamadı. Daha sonra tekrar deneyin."
                return@coroutineScope
            }

            val middleWeather = weatherResults.drop(1).dropLast(1)
            val denseWithWeather = denseWithEtas.mapIndexed { idx, wp ->
                wp.copy(weather = middleWeather.getOrNull(idx))
            }

            // Keep location and ETA together with the weather that was actually sampled.
            val normalWithWeather = RouteSampler.sample(routeResult.route).mapNotNull { point ->
                denseWithWeather.minByOrNull {
                    kotlin.math.abs(it.cumulativeDistanceMeters - point.cumulativeDistanceMeters)
                }
            }.distinctBy { it.cumulativeDistanceMeters }
            val limitedWaypoints = RouteWaypointSelector.select(normalWithWeather, denseWithWeather)

            // 7. Resolve Location Names (only for final visible points)
            val geocodedWaypoints = limitedWaypoints.map { wp ->
                val name = geocoder.placeName(wp.location) ?: "Ara Nokta"
                wp.copy(placeName = name)
            }

            if (routeJob !== analysisRouteJob) return@coroutineScope
            _startWeather.value = weatherResults.first()
            _endWeather.value = weatherResults.last()
            _waypoints.value = geocodedWaypoints
            _hasIncompleteForecast.value = weatherResults.any { it == null }
            _isAnalyzed.value = true

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
if (BuildConfig.DEBUG) {
                Log.e("RouteVM", "Analysis failed", e)
}
            if (routeJob === analysisRouteJob) {
                _analysisError.value = "Hava durumu analizi başarısız oldu."
            }
        } finally {
            analysis.finish(currentAnalysis)
        }
    }
}

internal suspend fun resolveDeviceRouteOrigin(tracker: LocationTracker): GeoPoint? =
    tracker.getCurrentLocation()?.let { GeoPoint(it.latitude, it.longitude) }

internal class RouteAnalysisRun {
    private var job: Job? = null
    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    fun start(current: Job) {
        job = current
        _running.value = true
    }

    fun cancel() {
        val previous = job
        job = null
        _running.value = false
        previous?.cancel()
    }

    fun finish(current: Job) {
        if (job !== current) return
        job = null
        _running.value = false
    }
}
