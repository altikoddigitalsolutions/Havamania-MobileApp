package com.havamania

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
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory

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
    val analyzed by viewModel.isAnalyzed.collectAsStateWithLifecycle()
    val departureMillis by viewModel.departureMillis.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val mapView = rememberMapViewWithLifecycle()
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }
    var selected by remember { mutableStateOf<RouteWaypoint?>(null) }

    LaunchedEffect(tripId) {
        if (tripId != null) {
            viewModel.loadTrip(tripId)
        }
    }

    LaunchedEffect(mapRef, styleRef, routeState) {
        val map = mapRef ?: return@LaunchedEffect
        if (styleRef == null) return@LaunchedEffect

        val state = routeState as? RouteResult.Success ?: return@LaunchedEffect

        map.clear()
        val points = state.route.points.map { LatLng(it.latitude, it.longitude) }

        if (points.isNotEmpty()) {
            try {
                map.addPolyline(PolylineOptions()
                    .addAll(points)
                    .color(android.graphics.Color.parseColor("#3B82F6"))
                    .width(5f)
                )

                val builder = LatLngBounds.Builder()
                points.forEach { builder.include(it) }
                val bounds = builder.build()

                // Add a small delay to ensure MapView layout is finalized
                kotlinx.coroutines.delay(500)
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 2000)
            } catch (e: Exception) {
                Log.e("MapRoute", "Error fitting bounds", e)
            }
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
                        Icon(Icons.Rounded.Refresh, null, tint = colors.accent)
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

                    if (routeState == null) {
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
                                    Log.e("RouteScreen", "Failed to open location settings", e)
                                }
                            },
                            onPickOrigin = onBack // Return to planner to pick origin
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    RouteContent(
                        trip = trip,
                        routeState = routeState,
                        waypoints = waypoints,
                        startWeather = startWeather,
                        endWeather = endWeather,
                        departureMillis = departureMillis,
                        analyzing = analyzing,
                        analyzed = analyzed,
                        onAnalyze = { viewModel.analyzeWeather() },
                        onSelect = { selected = it }
                    )
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
                                Log.e("RouteScreen", "Failed to open location settings", e)
                            }
                        },
                        onPickOrigin = onBack
                    )
                    Spacer(Modifier.height(16.dp))
                }

                RouteContent(
                    trip = trip,
                    routeState = routeState,
                    waypoints = waypoints,
                    startWeather = startWeather,
                    endWeather = endWeather,
                    departureMillis = departureMillis,
                    analyzing = analyzing,
                    analyzed = analyzed,
                    onAnalyze = { viewModel.analyzeWeather() },
                    onSelect = { selected = it },
                    showTimeline = false
                )

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

                    if (routeState == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    } else if (routeState is RouteResult.Error || routeState is RouteResult.NoRoute) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Text("Harita verisi yüklenemedi", color = colors.textMuted)
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
    }
}

@Composable
private fun RouteContent(
    trip: TravelPlan?,
    routeState: RouteResult?,
    waypoints: List<RouteWaypoint>,
    startWeather: WaypointWeather?,
    endWeather: WaypointWeather?,
    departureMillis: Long?,
    analyzing: Boolean,
    analyzed: Boolean,
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

        val tripStatus = remember(t) {
            TravelStatusResolver.getStatus(t.startDate, t.endDate)
        }
        val isPast = tripStatus == TravelStatus.PAST
        val departureDateTime = t.departureDateTime
        val now = LocalDateTime.now()
        val isWeatherReady = departureDateTime != null && !isPast && ChronoUnit.HOURS.between(now, departureDateTime) <= 48

        if (isPast) {
            RoutePastTripCard(t, colors)
        } else if (t.departureTime == null) {
            RouteTimeMissingCard(colors)
        } else if (!isWeatherReady && departureDateTime != null) {
            RouteWeatherNotReadyCard(departureDateTime, colors)
        } else if (!isWeatherReady && departureDateTime == null) {
            RouteTimeMissingCard(colors)
        } else if (!analyzed) {
            RouteAnalysisActionCard(analyzing, onAnalyze = { scope.launch { onAnalyze() } }, colors)
        } else if (routeState is RouteResult.Success) {
            val route = routeState.route
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
private fun TimelineContent(
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

    TimelineWaypointItem(trip.originCity ?: "Başlangıç", depTime, startWeather, isFirst = true) {}

    waypoints.forEach { wp ->
        val eta = if (wp.etaEpochMillis != null) {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(wp.etaEpochMillis), ZoneId.systemDefault()).format(timeFormatter)
        } else "--:--"

        TimelineWaypointItem(wp.placeName ?: "Ara Nokta", eta, wp.weather) { onSelect(wp) }
    }

    val arrTime = if (departureMillis != null) {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(departureMillis + (route.durationSeconds * 1000).toLong()), ZoneId.systemDefault()).format(timeFormatter)
    } else "--:--"

    TimelineWaypointItem(trip.city, arrTime, endWeather, isLast = true) {}
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
fun MapLayer(mapView: MapView, onMapReady: (MapLibreMap, Style) -> Unit) {
    androidx.compose.ui.viewinterop.AndroidView(factory = { mapView }) { mv ->
        mv.getMapAsync { map ->
            map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                onMapReady(map, style)
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
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
    }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return mapView
}
