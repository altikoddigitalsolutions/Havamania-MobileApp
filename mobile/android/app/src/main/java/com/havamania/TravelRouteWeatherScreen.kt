package com.havamania

import androidx.activity.compose.BackHandler

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.Property
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.style.expressions.Expression

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelRouteWeatherScreen(
    tripId: String?,
    onBack: () -> Unit,
    viewModel: TravelRouteViewModel = viewModel()
) {
    val colors = HavamaniaTheme.colors
    val styles = HavamaniaTheme.styles
    val responsive = LocalResponsiveValues.current
    val windowSize = LocalWindowSize.current
    val config = LocalConfiguration.current
    val context = LocalContext.current

    val useTwoColumns = (windowSize.isTablet || windowSize.isLargeTablet) && config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val routeState by viewModel.routeState.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val startWeather by viewModel.startWeather.collectAsStateWithLifecycle()
    val endWeather by viewModel.endWeather.collectAsStateWithLifecycle()
    val analyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val hasIncompleteForecast by viewModel.hasIncompleteForecast.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val analyzed by viewModel.isAnalyzed.collectAsStateWithLifecycle()
    val departureMillis by viewModel.departureMillis.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val mapView = rememberMapViewWithLifecycle()
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }
    val waypointsByMarkerId = remember(mapRef) { mutableMapOf<Long, RouteWaypoint>() }
    var overlappingWaypoints by remember(tripId, routeState, waypoints) { mutableStateOf(emptyList<RouteWaypoint>()) }
    var selected by remember(tripId, routeState, waypoints) { mutableStateOf<RouteWaypoint?>(null) }

    LaunchedEffect(tripId) {
        if (tripId != null) {
            viewModel.loadTrip(tripId)
        }
    }

    LaunchedEffect(mapRef, styleRef, routeState, waypoints) {
        val map = mapRef ?: return@LaunchedEffect
        val style = styleRef ?: return@LaunchedEffect

        val sourceId = "route-waypoints-source"
        val layerId = "route-waypoints-layer"
        val existingSource = style.getSource(sourceId) as? GeoJsonSource
        waypointsByMarkerId.clear()
        map.clear()
        val state = routeState as? RouteResult.Success
        if (state == null) {
            existingSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return@LaunchedEffect
        }
        val points = state.route.points.map { LatLng(it.latitude, it.longitude) }

        if (points.isNotEmpty()) {
            try {
                map.addPolyline(PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.parseColor("#3B82F6"))
                    .width(5f)
                )

                waypoints.forEach { wp ->
                    val marker = map.addMarker(MarkerOptions()
                        .position(LatLng(wp.location.latitude, wp.location.longitude))
                        .title(wp.placeName ?: "Ara Nokta")
                    )
                    waypointsByMarkerId[marker.id] = wp
                }

                // Update labels to match the current route waypoints.
                val validWaypoints = waypoints.filter { !it.placeName.isNullOrBlank() }
                val features = validWaypoints.map { wp ->
                    val point = Point.fromLngLat(wp.location.longitude, wp.location.latitude)
                    val feature = Feature.fromGeometry(point)
                    feature.addStringProperty("name", wp.placeName ?: "")
                    feature
                }

                val featureCollection = FeatureCollection.fromFeatures(features)
                if (existingSource != null) {
                    existingSource.setGeoJson(featureCollection)
                } else {
                    style.addSource(GeoJsonSource(sourceId, featureCollection))
                }

                if (style.getLayer(layerId) == null) {
                    val symbolLayer = SymbolLayer(layerId, sourceId).withProperties(
                        PropertyFactory.textField(Expression.get("name")),
                        PropertyFactory.textSize(13f),
                        PropertyFactory.textColor(android.graphics.Color.parseColor("#111827")),
                        PropertyFactory.textHaloColor(android.graphics.Color.parseColor("#FFFFFF")),
                        PropertyFactory.textHaloWidth(2.5f),
                        PropertyFactory.textOffset(arrayOf(0f, -1.5f)),
                        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true),
                        PropertyFactory.textFont(arrayOf("Noto Sans Regular"))
                    )
                    style.addLayer(symbolLayer)

                    val layerIndex = style.layers.indexOf(symbolLayer)
                    val totalLayers = style.layers.size
                    if (BuildConfig.DEBUG) {
                        Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "LAYER_EXISTS=true (created)")
                        Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "FONT_STACK=[Noto Sans Regular]")
                        Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "TEXT_FIELD=Expression.get(\"name\")")
if (BuildConfig.DEBUG) {
                            Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "TEXT_ANCHOR=TEXT_ANCHOR_BOTTOM")
}
if (BuildConfig.DEBUG) {
                            Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "TEXT_OFFSET=[0f, -1.5f]")
}
if (BuildConfig.DEBUG) {
                            Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "CUSTOM_LAYER_INDEX=$layerIndex")
}
if (BuildConfig.DEBUG) {
                            Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "TOTAL_LAYER_COUNT=$totalLayers")
}
if (BuildConfig.DEBUG) {
                            Log.d("HAVAMANIA_MAP_LABEL_DEBUG", "GLYPH_CONFIG_SOURCE=OpenFreeMap Liberty existing style layer")
}
                    }
                }

            } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                    Log.e("MapRoute", "Error drawing route", e)
}
            }
        }
    }

    val latestSelection by rememberUpdatedState(selected)
    LaunchedEffect(mapRef, styleRef, routeState) {
        val map = mapRef ?: return@LaunchedEffect
        if (styleRef == null) return@LaunchedEffect
        val route = (routeState as? RouteResult.Success)?.route ?: return@LaunchedEffect
        if (route.points.size < 2) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        if (latestSelection != null) return@LaunchedEffect

        try {
            val bounds = LatLngBounds.Builder().apply {
                route.points.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 2000)
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG) Log.w("MapRoute", "Could not fit route bounds", e)
        }
    }

    RouteMarkerSelection(mapRef, waypointsByMarkerId) { matches ->
        if (matches.size == 1) {
            selected = matches.single()
        } else {
            overlappingWaypoints = matches
        }
    }
    if (overlappingWaypoints.isNotEmpty()) {
        RoutePassageDialog(overlappingWaypoints, onDismiss = { overlappingWaypoints = emptyList() }) {
            overlappingWaypoints = emptyList()
            selected = it
        }
    }

    LaunchedEffect(selected) {
        selected?.let { wp ->
            mapRef?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(wp.location.latitude, wp.location.longitude), 13.0), 1000)
        }
    }

    val scope = rememberCoroutineScope()

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "GÜZERGAH HAVASI",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadTrip(tripId ?: "") }) {
                        Icon(Icons.Rounded.Refresh, "Rotayı yenile", tint = colors.accent)
                    }
                }
            )
        }
    ) { padding ->
        val contentModifier = Modifier
            .padding(padding)
            .fillMaxSize()

        if (useTwoColumns) {
            Row(modifier = contentModifier) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .padding(styles.spacingMD)
                        .clip(RoundedCornerShape(styles.radiusLarge))
                        .border(1.dp, colors.border.copy(alpha = 0.1f), RoundedCornerShape(styles.radiusLarge))
                ) {
                    MapLayer(mapView, onMapReady = { map, style ->
                        mapRef = map
                        styleRef = style
                    })

                    if (routeState == null && errorMessage == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(styles.spacingMD)
                        .padding(horizontal = 16.dp)
                ) {
                    errorMessage?.let { msg ->
                        RouteErrorState(
                            message = msg,
                            onRetry = { viewModel.loadTrip(tripId ?: "") },
                            onOpenSettings = {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                                        Log.e("RouteScreen", "Failed to open location settings", e)
}
                                }
                            },
                            onPickOrigin = onBack // Return to planner to pick origin
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (errorMessage == null) {
                        RouteContent(
                            trip = trip,
                            routeState = routeState,
                            waypoints = waypoints,
                            startWeather = startWeather,
                            endWeather = endWeather,
                            departureMillis = departureMillis,
                            analyzing = analyzing,
                            analyzed = analyzed,
                            hasIncompleteForecast = hasIncompleteForecast,
                            analysisError = analysisError,
                            onRetryRoute = { viewModel.loadTrip(tripId ?: "") },
                            onAnalyze = { viewModel.analyzeWeather() },
                            onSelect = { selected = it }
                        )
                    }
                }
            }
        } else {
            Column(modifier = contentModifier.verticalScroll(rememberScrollState())) {
                errorMessage?.let { msg ->
                    RouteErrorState(
                        message = msg,
                        modifier = Modifier.padding(horizontal = responsive.pagePadding),
                        onRetry = { viewModel.loadTrip(tripId ?: "") },
                        onOpenSettings = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                                    Log.e("RouteScreen", "Failed to open location settings", e)
}
                            }
                        },
                        onPickOrigin = onBack
                    )
                    Spacer(Modifier.height(16.dp))
                }

                if (errorMessage == null) {
                    RouteContent(
                        trip = trip,
                        routeState = routeState,
                        waypoints = waypoints,
                        startWeather = startWeather,
                        endWeather = endWeather,
                        departureMillis = departureMillis,
                        analyzing = analyzing,
                        analyzed = analyzed,
                        hasIncompleteForecast = hasIncompleteForecast,
                        analysisError = analysisError,
                        onRetryRoute = { viewModel.loadTrip(tripId ?: "") },
                        onAnalyze = { viewModel.analyzeWeather() },
                        onSelect = { selected = it },
                        showTimeline = false
                    )
                }

                Spacer(Modifier.height(styles.spacingMD))

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(if (windowSize.isTablet || windowSize.isLargeTablet) 500.dp else 350.dp)
                    .padding(horizontal = responsive.pagePadding)
                    .clip(RoundedCornerShape(styles.radiusLarge))
                    .border(1.dp, colors.border.copy(alpha = 0.1f), RoundedCornerShape(styles.radiusLarge))
                ) {
                    MapLayer(mapView, onMapReady = { map, style ->
                        mapRef = map
                        styleRef = style
                    })

                    if (routeState == null && errorMessage == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    } else if (errorMessage != null) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.03f)), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Başlangıç noktası seçildiğinde rota burada gösterilecek.",
                                style = HavamaniaTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                                    .background(colors.surface, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            )
                        }
                    } else if (routeState is RouteResult.Error || routeState is RouteResult.NoRoute) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Text(
                                "Harita verisi şu an yüklenemedi",
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                                    .background(colors.surface, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(styles.spacingLG))

                if (trip != null && routeState is RouteResult.Success) {
                    Column(modifier = Modifier.padding(horizontal = responsive.pagePadding)) {
                        Text(
                            "GÜZERGAH DETAYLARI",
                            style = HavamaniaTheme.typography.sectionTitle,
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                        )

                        TimelineContent(
                            trip = trip!!,
                            route = (routeState as RouteResult.Success).route,
                            waypoints = waypoints,
                            startWeather = startWeather,
                            endWeather = endWeather,
                            departureMillis = departureMillis,
                            analyzed = analyzed,
                            onSelect = { selected = it }
                        )
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        // Waypoint Detail Popup Overlay
        selected?.let { wp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                RouteWaypointDetailCard(wp, analyzed, onClose = { selected = null })
            }
        }
    }
}

@Composable
internal fun RouteWaypointDetailCard(wp: RouteWaypoint, analyzed: Boolean, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val colors = HavamaniaTheme.colors
    val weather = wp.weather
    val timeStr = wp.etaEpochMillis?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: "--:--"

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.2f)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = wp.placeName ?: "Ara Nokta",
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tahmini Geçiş: $timeStr",
                        style = HavamaniaTheme.typography.caption,
                        color = colors.textSecondary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Güzergâh ayrıntısını kapat", tint = colors.textMuted)
                }
            }

            if (weather != null) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = WeatherUtils.getWeatherEmoji(weather.weatherCode),
                            fontSize = 28.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = routeWeatherDisplayName(weather.weatherCode, wp.etaEpochMillis),
                                style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Sıcaklık: ${weather.temperatureC.toInt()}°C",
                                style = HavamaniaTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                if (weather.risk != RouteRisk.OK && !weather.riskReason.isNullOrBlank()) {
                    Surface(
                        color = colors.warning.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "⚠️ ${weather.riskReason}",
                            style = HavamaniaTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            color = colors.warning,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (analyzed) "Tahmin alınamadı" else "Analiz tamamlanmadı",
                    style = HavamaniaTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun RouteContent(
    trip: TravelPlan?,
    routeState: RouteResult?,
    waypoints: List<RouteWaypoint>,
    startWeather: WaypointWeather?,
    endWeather: WaypointWeather?,
    departureMillis: Long?,
    analyzing: Boolean,
    analyzed: Boolean,
    hasIncompleteForecast: Boolean,
    analysisError: String?,
    onRetryRoute: () -> Unit,
    onAnalyze: suspend () -> Unit,
    onSelect: (RouteWaypoint) -> Unit,
    showTimeline: Boolean = true
) {
    val colors = HavamaniaTheme.colors
    val scope = rememberCoroutineScope()
    val trLocale = remember { Locale("tr") }
    val turkishDateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", trLocale) }

    trip?.let { t ->
        JourneyHero(
            origin = t.originCity,
            destination = t.city,
            date = t.startDate.format(turkishDateFormatter),
            departureTime = t.departureTime
        )

        Spacer(Modifier.height(16.dp))

        val now = java.time.Instant.now()
        val tripStatus = TravelStatusResolver.getStatus(t.startDate, t.endDate, now.atZone(ZoneId.systemDefault()).toLocalDate())
        val isPast = tripStatus == TravelStatus.PAST
        val departureDateTime = t.departureDateTime
        val isWeatherReady = TravelStatusResolver.isRouteWeatherEligible(t, now)

        if (routeState is RouteResult.Error || routeState is RouteResult.NoRoute) {
            Text(
                text = if (routeState is RouteResult.NoRoute) "Sürüş rotası bulunamadı."
                    else "Rota yüklenemedi.",
                color = colors.textPrimary
            )
            TextButton(onClick = onRetryRoute) { Text("Rotayı tekrar yükle") }
        } else if (isPast) {
            RoutePastTripCard(t, colors)
        } else if (t.departureTime == null) {
            RouteTimeMissingCard(colors)
        } else if (!isWeatherReady && departureDateTime != null) {
            RouteWeatherNotReadyCard(departureDateTime, colors)
        } else if (!isWeatherReady && departureDateTime == null) {
            RouteTimeMissingCard(colors)
        } else if (!analyzed && routeState is RouteResult.Success) {
            analysisError?.let { Text(it, color = colors.textPrimary) }
            RouteAnalysisActionCard(analyzing, onAnalyze = { scope.launch { onAnalyze() } }, colors)
        } else if (routeState is RouteResult.Success) {
            val route = routeState.route
            if (hasIncompleteForecast) {
                Text(
                    text = "Bazı güzergâh noktaları için hava tahmini alınamadı. Bu kesimlerde hava riski değerlendirilemedi.",
                    color = colors.textPrimary
                )
            }
            val arrivalMillis = departureMillis?.let { it + (route.durationSeconds * 1000).toLong() }

            RouteMetricRow(
                distance = "${(route.distanceMeters / 1000).toInt()} km",
                duration = "${(route.durationSeconds / 3600).toInt()} sa ${(route.durationSeconds % 3600 / 60).toInt()} dk",
                arrival = arrivalMillis?.let {
                    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                    dt.format(DateTimeFormatter.ofPattern("HH:mm"))
                }
            )

            if (showTimeline) {
                Spacer(Modifier.height(24.dp))
                TimelineContent(t, route, waypoints, startWeather, endWeather, departureMillis, analyzed, onSelect)
            }
        }
    }
}

@Composable
internal fun TimelineContent(
    trip: TravelPlan,
    route: RoutePath,
    waypoints: List<RouteWaypoint>,
    startWeather: WaypointWeather?,
    endWeather: WaypointWeather?,
    departureMillis: Long?,
    analyzed: Boolean,
    onSelect: (RouteWaypoint) -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    val depTime = if (departureMillis != null) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(departureMillis), ZoneId.systemDefault()).format(timeFormatter)
    } else "--:--"

    val missingForecastLabel = if (analyzed) "Tahmin alınamadı" else "Analiz tamamlanmadı"
    val startName = trip.originCity ?: "Başlangıç"
    TimelineWaypointItem(startName, depTime, startWeather, isFirst = true, missingForecastLabel = missingForecastLabel, etaEpochMillis = departureMillis) {
        route.origin?.let { onSelect(RouteWaypoint(it, 0.0, departureMillis, startName, startWeather)) }
    }

    waypoints.forEach { wp ->
        val eta = if (wp.etaEpochMillis != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(wp.etaEpochMillis), ZoneId.systemDefault()).format(timeFormatter)
        } else "--:--"

        TimelineWaypointItem(wp.placeName ?: "Ara Nokta", eta, wp.weather, missingForecastLabel = missingForecastLabel, etaEpochMillis = wp.etaEpochMillis) { onSelect(wp) }
    }

    val arrivalMillis = departureMillis?.plus((route.durationSeconds * 1000).toLong())
    val arrTime = if (arrivalMillis != null) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(arrivalMillis), ZoneId.systemDefault()).format(timeFormatter)
    } else "--:--"

    TimelineWaypointItem(trip.city, arrTime, endWeather, isLast = true, missingForecastLabel = missingForecastLabel, etaEpochMillis = arrivalMillis) {
        route.destination?.let {
            onSelect(RouteWaypoint(it, route.distanceMeters, arrivalMillis, trip.city, endWeather))
        }
    }
}

@Composable
private fun RoutePastTripCard(trip: TravelPlan?, c: HavamaniaColors) {
    Surface(
        color = c.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.border.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, null, tint = c.textMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Seyahat Tamamlandı",
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Bu seyahat geçmişte kaldığı için canlı güzergâh hava analizi yapılamaz.",
                style = HavamaniaTheme.typography.bodySmall,
                color = c.textSecondary
            )

            trip?.weatherSummary?.let { summary ->
                Spacer(Modifier.height(16.dp))
                Text("KAYDEDİLEN ÖZET", style = HavamaniaTheme.typography.caption, color = c.accent)
                Text(summary, style = HavamaniaTheme.typography.bodyMedium, color = c.textPrimary)
            }
        }
    }
}

@Composable
private fun RouteTimeMissingCard(c: HavamaniaColors) {
    Surface(
        color = Color(0xFFFBBF24).copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = Color(0xFFFBBF24))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Kalkış Saati Gerekli",
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary
                )
                Text(
                    text = "Güzergâh boyunca anlık hava durumunu görebilmek için seyahat planına bir kalkış saati eklemelisin.",
                    style = HavamaniaTheme.typography.bodySmall,
                    color = c.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RouteWeatherNotReadyCard(departure: LocalDateTime, c: HavamaniaColors) {
    val trLocale = remember { Locale("tr") }
    val formatter = DateTimeFormatter.ofPattern("d MMMM HH:mm", trLocale)
    Surface(
        color = c.accent.copy(alpha = 0.05f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Tahmin Henüz Hazır Değil",
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                    color = c.textPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Güzergâh hava analizi seyahat saatine 48 saat kala aktifleşir. Kalkış saatiniz: ${departure.format(formatter)}",
                style = HavamaniaTheme.typography.bodySmall,
                color = c.textSecondary
            )
        }
    }
}

@Composable
private fun RouteErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onPickOrigin: (() -> Unit)? = null
) {
    Surface(
        color = HavamaniaTheme.colors.error.copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, HavamaniaTheme.colors.error.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (message.contains("konum", true)) Icons.Rounded.LocationOff else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = HavamaniaTheme.colors.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = HavamaniaTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (message.contains("Kalkış", true) || message.contains("konum", true)) {
                HavamaniaPrimaryButton(
                    text = "KONUMU AÇ",
                    onClick = { onOpenSettings?.invoke() },
                    height = 44.dp
                )
                Spacer(Modifier.height(12.dp))
                HavamaniaSecondaryButton(
                    text = "KALKIŞ NOKTASI SEÇ",
                    onClick = { onPickOrigin?.invoke() },
                    height = 44.dp
                )
            } else {
                HavamaniaPrimaryButton(
                    text = "TEKRAR DENE",
                    onClick = onRetry,
                    height = 44.dp
                )
            }
        }
    }
}

@Composable
private fun RouteAnalysisActionCard(analyzing: Boolean, onAnalyze: () -> Unit, c: HavamaniaColors) {
    Surface(
        color = c.accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = c.accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Akıllı Güzergâh Analizi",
                style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Black),
                color = c.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Yolculuk saatinize göre güzergâh üzerindeki hava koşullarını analiz edelim.",
                style = HavamaniaTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            HavamaniaPrimaryButton(
                text = "ANALİZİ BAŞLAT",
                onClick = onAnalyze,
                isLoading = analyzing,
                height = 44.dp,
                fillMaxWidth = false
            )
        }
    }
}

@Composable
internal fun RoutePassageDialog(
    waypoints: List<RouteWaypoint>,
    onDismiss: () -> Unit,
    onSelect: (RouteWaypoint) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bu noktadan geçişler") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                waypoints.forEachIndexed { index, waypoint ->
                    val time = waypoint.etaEpochMillis?.let {
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    TextButton(onClick = {
                        onSelect(waypoint)
                    }) {
                        Text("${index + 1}. geçiş" + (time?.let { " · $it" } ?: ""))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
internal fun RouteMarkerSelection(
    map: MapLibreMap?,
    waypointsByMarkerId: Map<Long, RouteWaypoint>,
    onSelect: (List<RouteWaypoint>) -> Unit
) {
    val select by rememberUpdatedState(onSelect)
    DisposableEffect(map, waypointsByMarkerId) {
        map?.setOnMarkerClickListener { marker ->
            val waypoint = waypointsByMarkerId[marker.id]
            if (waypoint != null) {
                select(waypointsByMarkerId.values.filter { it.location == waypoint.location }
                    .sortedBy { it.cumulativeDistanceMeters })
            }
            waypoint != null
        }
        onDispose { map?.setOnMarkerClickListener(null) }
    }
}

@Composable
fun MapLayer(mapView: MapView, onMapReady: (MapLibreMap, Style) -> Unit) {
    val latestOnMapReady by rememberUpdatedState(onMapReady)
    androidx.compose.ui.viewinterop.AndroidView(factory = { mapView })
    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.setStyle(MapStyleProvider.currentStyle()) { style ->
                latestOnMapReady(map, style)
            }
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleObserver = remember {
        androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                // Disposal owns destruction, including navigation away while the activity lives.
                Lifecycle.Event.ON_DESTROY -> Unit
                else -> {}
            }
        }
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}
