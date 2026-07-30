package com.havamania

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.havamania.ui.theme.HavamaniaColors
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Akıllı Güzergâh Hava Durumu — Aşama 2.
 *
 * Boş harita (Aşama 1) üzerine gerçek OSRM sürüş rotasını çizer:
 *  - Polyline (yol geometrisi)
 *  - Yeşil başlangıç / kırmızı varış markerları
 *  - Kamera rotaya sığdırılır
 *  - Alt köşede mesafe + süre özeti
 *
 * NOT: Origin/destination şimdilik demo (Balıkesir → Ankara). Aşama 4'te seyahat kartındaki
 * "Güzergâhı Gör" butonu gerçek başlangıç/varış koordinatlarını [tripId] üzerinden geçirecek.
 * Ara hava noktaları (mavi marker) ve risk analizi sonraki aşamalarda eklenecek.
 */
private const val OPENFREEMAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID = "route-layer"
private const val ENDPOINTS_SOURCE_ID = "endpoints-source"
private const val ENDPOINTS_LAYER_ID = "endpoints-layer"
private const val WAYPOINTS_SOURCE_ID = "waypoints-source"
private const val WAYPOINTS_LAYER_ID = "waypoints-layer"

private val ROUTE_COLOR = Color.parseColor("#2962FF")
private val START_COLOR = Color.parseColor("#2E7D32")    // yeşil
private val END_COLOR = Color.parseColor("#C62828")      // kırmızı
private val WAYPOINT_COLOR = Color.parseColor("#1E88E5") // mavi (analiz edilmemiş ara nokta)

// Risk renkleri (analiz sonrası marker rengi)
private val RISK_OK_COLOR = Color.parseColor("#10B981")      // yeşil — uygun
private val RISK_CAUTION_COLOR = Color.parseColor("#F59E0B") // amber — dikkat
private val RISK_DANGER_COLOR = Color.parseColor("#EF4444")  // kırmızı — tehlikeli

@Composable
fun TravelRouteWeatherScreen(
    tripId: String,
    onBack: () -> Unit = {}
) {
    val mapView = rememberMapViewWithLifecycle()
    val provider = remember { OsrmRouteProvider() }
    val context = LocalContext.current
    val geocoder = remember { ReverseGeocoder(context) }

    // Seyahati tripId ile yükle — varış koordinatı ve kalkış tarihi buradan gelir.
    val travelViewModel: TravelViewModel = viewModel()
    val plans by travelViewModel.plans.collectAsStateWithLifecycle()
    val trip = remember(plans, tripId) { plans.find { it.id == tripId } }

    // Başlangıç = kullanıcının mevcut konumu (seyahat kaydında ayrı origin yok).
    val locationTracker = remember {
        DefaultLocationTracker(
            LocationServices.getFusedLocationProviderClient(context),
            context.applicationContext as Application
        )
    }

    var routeState by remember { mutableStateOf<RouteResult?>(null) }
    var waypoints by remember { mutableStateOf<List<RouteWaypoint>>(emptyList()) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleRef by remember { mutableStateOf<Style?>(null) }

    // --- Hava analizi durumu ---
    val scope = rememberCoroutineScope()
    val weatherProvider = remember { RouteWeatherProvider() }
    var analyzing by remember { mutableStateOf(false) }
    var analyzed by remember { mutableStateOf(false) }
    var startWeather by remember { mutableStateOf<WaypointWeather?>(null) }
    var endWeather by remember { mutableStateOf<WaypointWeather?>(null) }

    // --- Runtime konum izni ---
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }
    var permissionAsked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
        permissionAsked = true
    }

    // İzin yoksa ekran açılınca bir kez iste (izin diyaloğu sistem tarafından gösterilir).
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Trip + izin hazır olunca: mevcut konumu al, varışa rota hesapla.
    LaunchedEffect(trip, permissionGranted, permissionAsked) {
        val t = trip ?: return@LaunchedEffect // planlar henüz yükleniyor
        if (t.latitude == 0.0 && t.longitude == 0.0) {
            routeState = RouteResult.Error("Bu seyahatin konum bilgisi yok.")
            return@LaunchedEffect
        }
        if (!permissionGranted) {
            // İzin isteği hâlâ açık olabilir; yalnızca reddedildiyse mesaj göster.
            if (permissionAsked) {
                routeState = RouteResult.Error("Konum izni verilmedi. Başlangıç noktası için konum izni gerekli.")
            }
            return@LaunchedEffect
        }
        routeState = null // "hesaplanıyor…" durumuna dön
        val loc = locationTracker.getCurrentLocation()
        if (loc == null) {
            routeState = RouteResult.Error("Konum alınamadı. Cihazın konum (GPS) servisinin açık olduğundan emin olun.")
            return@LaunchedEffect
        }
        routeState = provider.getRoute(
            origin = GeoPoint(loc.latitude, loc.longitude),
            destination = GeoPoint(t.latitude, t.longitude)
        )
    }

    // Rota gelince örnekle + ETA ata; ardından yer adlarını arka planda doldur.
    LaunchedEffect(routeState) {
        val state = routeState
        if (state is RouteResult.Success) {
            val departureMillis = trip?.startDate
                ?.atTime(8, 0)
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
            val sampled = EtaCalculator.assignEtas(
                state.route,
                RouteSampler.sample(state.route),
                departureMillis
            )
            waypoints = sampled
            // Reverse geocode — markerlar zaten çizildi; isimler geldikçe güncellenir.
            waypoints = sampled.map { wp -> wp.copy(placeName = geocoder.placeName(wp.location)) }
        } else {
            waypoints = emptyList()
        }
    }

    // Harita + stil + rota hazır olduğunda çiz.
    LaunchedEffect(mapRef, styleRef, routeState, waypoints) {
        val map = mapRef
        val style = styleRef
        val state = routeState
        if (map != null && style != null && style.isFullyLoaded && state is RouteResult.Success) {
            drawRoute(map, style, state.route, waypoints)
        }
    }

    // "Analiz Et" aksiyonu: başlangıç, ara noktalar ve varış için tahmini geçiş anına göre
    // hava durumunu paralel çeker, risk atar, markerları yeniden boyar.
    fun analyzeWeather() {
        val route = (routeState as? RouteResult.Success)?.route ?: return
        if (analyzing) return
        scope.launch {
            analyzing = true
            val departureMillis = trip?.startDate
                ?.atTime(8, 0)
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
            val arrivalMillis = departureMillis + (route.durationSeconds * 1000).toLong()
            val snapshot = waypoints
            coroutineScope {
                val startDeferred = async { route.origin?.let { weatherProvider.weatherAt(it, departureMillis) } }
                val endDeferred = async { route.destination?.let { weatherProvider.weatherAt(it, arrivalMillis) } }
                val wpDeferred = snapshot.map { wp ->
                    async { wp.copy(weather = weatherProvider.weatherAt(wp.location, wp.etaEpochMillis)) }
                }
                startWeather = startDeferred.await()
                endWeather = endDeferred.await()
                waypoints = wpDeferred.awaitAll()
            }
            analyzed = true
            analyzing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView.also { mv ->
                mv.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(OPENFREEMAP_STYLE)) { style ->
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(39.0, 35.0))
                            .zoom(4.5)
                            .build()
                        mapRef = map
                        styleRef = style
                    }
                }
            } }
        )

        // Geri butonu (bu ekran tab bar olmadan açılır).
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(50),
            color = ComposeColor(0xCC000000),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = ComposeColor.White,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Durum / özet overlay.
        when (val state = routeState) {
            null -> StatusBadge(
                text = "Rota hesaplanıyor…",
                showSpinner = true,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            is RouteResult.NoRoute -> StatusBadge(
                text = "Bu iki nokta arasında sürülebilir rota bulunamadı.",
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            is RouteResult.Error -> StatusBadge(
                text = state.message,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            is RouteResult.Success -> RouteWeatherPanel(
                route = state.route,
                waypoints = waypoints,
                startWeather = startWeather,
                endWeather = endWeather,
                destinationName = trip?.city,
                analyzing = analyzing,
                analyzed = analyzed,
                onAnalyze = ::analyzeWeather,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/** Rota özeti + hava analizi aksiyonu / sonucu (tasarım sistemine uygun alt panel). */
@Composable
private fun RouteWeatherPanel(
    route: RoutePath,
    waypoints: List<RouteWaypoint>,
    startWeather: WaypointWeather?,
    endWeather: WaypointWeather?,
    destinationName: String?,
    analyzing: Boolean,
    analyzed: Boolean,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = HavamaniaTheme.colors
    Surface(
        color = c.surfaceGlass,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        border = BorderStroke(1.dp, c.border),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Rota özeti — mesafe • süre
            Text(
                text = "${formatDistance(route.distanceMeters)}  •  ${formatDuration(route.durationSeconds)}",
                color = c.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))

            if (!analyzed) {
                Text(
                    text = "Güzergâh üzerindeki ${waypoints.size} nokta için tahmini geçiş saatine göre hava durumunu ve sürüş riskini görün.",
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAnalyze,
                    enabled = !analyzing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent,
                        contentColor = c.onAccent,
                        disabledContainerColor = c.accent.copy(alpha = 0.6f),
                        disabledContentColor = c.onAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (analyzing) {
                        CircularProgressIndicator(
                            color = c.onAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Analiz ediliyor…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.Cloud, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Güzergâh Hava Durumunu Analiz Et", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val summary = routeRiskSummary(waypoints, startWeather, endWeather)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(riskColor(summary.worst, c))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = summary.text,
                        color = c.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        WeatherChip(label = "Kalkış", weather = startWeather, c = c)
                    }
                    items(waypoints) { wp ->
                        WeatherChip(
                            label = wp.placeName ?: formatEta(wp.etaEpochMillis),
                            weather = wp.weather,
                            c = c
                        )
                    }
                    item {
                        WeatherChip(
                            label = destinationName ?: "Varış",
                            weather = endWeather,
                            c = c,
                            highlight = true
                        )
                    }
                }
            }
        }
    }
}

/** Tek bir güzergâh noktasının hava kartı (emoji + sıcaklık + risk halkası). */
@Composable
private fun WeatherChip(
    label: String,
    weather: WaypointWeather?,
    c: HavamaniaColors,
    highlight: Boolean = false
) {
    val ringColor = if (weather != null) riskColor(weather.risk, c) else c.textMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) c.accent.copy(alpha = 0.12f) else c.surface.copy(alpha = 0.55f))
            .border(1.5.dp, ringColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(
            text = label,
            color = c.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (weather != null) WeatherUtils.getWeatherEmoji(weather.weatherCode) else "—",
            fontSize = 22.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (weather != null) "${weather.temperatureC.toInt()}°" else "—",
            color = c.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class RiskSummary(val worst: RouteRisk, val text: String)

/** Tüm nokta risklerini tek bir özet cümlesine indirger. */
private fun routeRiskSummary(
    waypoints: List<RouteWaypoint>,
    start: WaypointWeather?,
    end: WaypointWeather?
): RiskSummary {
    val all = listOfNotNull(start, end) + waypoints.mapNotNull { it.weather }
    if (all.isEmpty()) return RiskSummary(RouteRisk.OK, "Hava verisi alınamadı, bağlantıyı kontrol edin.")
    val danger = all.count { it.risk == RouteRisk.DANGER }
    val caution = all.count { it.risk == RouteRisk.CAUTION }
    return when {
        danger > 0 -> RiskSummary(RouteRisk.DANGER, "$danger noktada tehlikeli koşul (kar/buzlanma/fırtına). Dikkatli sürün.")
        caution > 0 -> RiskSummary(RouteRisk.CAUTION, "$caution noktada dikkat gereken hava (yağmur/sis).")
        else -> RiskSummary(RouteRisk.OK, "Güzergâh boyunca hava sürüş için uygun.")
    }
}

private fun riskColor(risk: RouteRisk, c: HavamaniaColors): ComposeColor = when (risk) {
    RouteRisk.OK -> c.success
    RouteRisk.CAUTION -> c.warning
    RouteRisk.DANGER -> c.error
}

private fun formatEta(millis: Long?): String {
    if (millis == null) return "—"
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return String.format("%02d:%02d", dt.hour, dt.minute)
}

/** Rota geometrisini, ara noktaları ve başlangıç/varış markerlarını stile ekler (idempotent). */
private fun drawRoute(map: MapLibreMap, style: Style, route: RoutePath, waypoints: List<RouteWaypoint>) {
    val start = route.origin ?: return
    val end = route.destination ?: return

    // --- Polyline ---
    val lineFeature = Feature.fromGeometry(
        LineString.fromLngLats(route.points.map { Point.fromLngLat(it.longitude, it.latitude) })
    )
    val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
    if (routeSource != null) {
        routeSource.setGeoJson(lineFeature)
    } else {
        style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, lineFeature))
        style.addLayer(
            LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineColor(ROUTE_COLOR),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    }

    // --- Ara noktalar (risk rengi; analiz edilmemişse mavi) ---
    if (waypoints.isNotEmpty()) {
        val waypointFc = FeatureCollection.fromFeatures(
            waypoints.map { wp ->
                Feature.fromGeometry(Point.fromLngLat(wp.location.longitude, wp.location.latitude))
                    .apply { addStringProperty("risk", wp.weather?.risk?.name ?: "NONE") }
            }
        )
        val waypointSource = style.getSourceAs<GeoJsonSource>(WAYPOINTS_SOURCE_ID)
        if (waypointSource != null) {
            waypointSource.setGeoJson(waypointFc)
        } else {
            style.addSource(GeoJsonSource(WAYPOINTS_SOURCE_ID, waypointFc))
            style.addLayer(
                CircleLayer(WAYPOINTS_LAYER_ID, WAYPOINTS_SOURCE_ID).withProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleStrokeColor(Color.WHITE),
                    PropertyFactory.circleColor(
                        Expression.match(
                            Expression.get("risk"),
                            Expression.literal("OK"), Expression.color(RISK_OK_COLOR),
                            Expression.literal("CAUTION"), Expression.color(RISK_CAUTION_COLOR),
                            Expression.literal("DANGER"), Expression.color(RISK_DANGER_COLOR),
                            Expression.color(WAYPOINT_COLOR) // NONE / analiz edilmedi
                        )
                    )
                )
            )
        }
    }

    // --- Başlangıç / varış markerları (renk data-driven) ---
    val startFeature = Feature.fromGeometry(Point.fromLngLat(start.longitude, start.latitude))
        .apply { addStringProperty("point_type", "start") }
    val endFeature = Feature.fromGeometry(Point.fromLngLat(end.longitude, end.latitude))
        .apply { addStringProperty("point_type", "end") }
    val endpointFc = FeatureCollection.fromFeatures(listOf(startFeature, endFeature))

    val endpointSource = style.getSourceAs<GeoJsonSource>(ENDPOINTS_SOURCE_ID)
    if (endpointSource != null) {
        endpointSource.setGeoJson(endpointFc)
    } else {
        style.addSource(GeoJsonSource(ENDPOINTS_SOURCE_ID, endpointFc))
        style.addLayer(
            CircleLayer(ENDPOINTS_LAYER_ID, ENDPOINTS_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleColor(
                    Expression.switchCase(
                        Expression.eq(Expression.get("point_type"), Expression.literal("start")),
                        Expression.color(START_COLOR),
                        Expression.color(END_COLOR)
                    )
                )
            )
        )
    }

    // --- Kamerayı rotaya sığdır ---
    val boundsBuilder = LatLngBounds.Builder()
    route.points.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
    runCatching {
        map.easeCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120), 800)
    }
}

@Composable
private fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ComposeColor(0xE6000000),
        modifier = modifier.padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = ComposeColor.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(end = 12.dp).size(16.dp)
                )
            }
            Text(
                text = text,
                color = ComposeColor.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDistance(meters: Double): String {
    val km = meters / 1000.0
    return if (km >= 10) "${km.toInt()} km" else String.format("%.1f km", km)
}

private fun formatDuration(seconds: Double): String {
    val totalMinutes = (seconds / 60).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours sa $minutes dk" else "$minutes dk"
}

/** MapView'ı Compose yaşam döngüsüne bağlar (MapLibre native view lifecycle gerektirir). */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    // MapLibre init — MapView oluşturulmadan önce zorunlu, idempotent.
    MapLibre.getInstance(context)

    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}
