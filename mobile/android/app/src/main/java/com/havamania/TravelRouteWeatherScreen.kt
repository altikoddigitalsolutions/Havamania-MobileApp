package com.havamania

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RectF
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import java.time.LocalDateTime
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
private const val SELECTED_SOURCE_ID = "selected-source"
private const val SELECTED_LAYER_ID = "selected-layer"

private val ROUTE_COLOR = Color.parseColor("#2962FF")
private val START_COLOR = Color.parseColor("#2E7D32")    // yeşil
private val END_COLOR = Color.parseColor("#C62828")      // kırmızı
private val WAYPOINT_COLOR = Color.parseColor("#1E88E5") // mavi (analiz edilmemiş ara nokta)
private val SELECTED_RING_COLOR = Color.parseColor("#00C2FF") // seçili nokta halkası

// Risk renkleri (analiz sonrası marker rengi)
private val RISK_OK_COLOR = Color.parseColor("#10B981")      // yeşil — uygun
private val RISK_CAUTION_COLOR = Color.parseColor("#F59E0B") // amber — dikkat
private val RISK_DANGER_COLOR = Color.parseColor("#EF4444")  // kırmızı — tehlikeli

// Rota uzunluğundan bağımsız hedeflenen ara nokta sayısı (kalabalığı önler).
private const val TARGET_WAYPOINTS = 6

/** Detay sheet'te gösterilecek seçili nokta (kalkış / ara nokta / varış). */
private data class RoutePointSelection(
    val title: String,
    val time: String?,
    val weather: WaypointWeather?,
    val location: GeoPoint?
)

@OptIn(ExperimentalMaterial3Api::class)
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

    // Dokunulan noktanın detay sheet'i (çip veya harita marker'ı).
    var selected by remember { mutableStateOf<RoutePointSelection?>(null) }
    // Dinleyici bir kez kurulur; en güncel state'i rememberUpdatedState ile okur.
    val waypointsLatest by rememberUpdatedState(waypoints)
    val startWeatherLatest by rememberUpdatedState(startWeather)
    val endWeatherLatest by rememberUpdatedState(endWeather)
    val destNameLatest by rememberUpdatedState(trip?.city)

    // Kalkış anı — seyahatin başlangıç günü 08:00 (yoksa şimdi).
    val departureMillis = remember(trip) {
        trip?.startDate
            ?.atTime(8, 0)
            ?.atZone(ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
    }

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
    // Kalkış noktası planda seçiliyse konum iznine hiç gerek yok.
    LaunchedEffect(trip) {
        if (!permissionGranted && trip?.originPoint == null) {
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
        // Planda kalkış noktası seçildiyse onu kullan; seçilmediyse cihazın konumuna düş.
        val plannedOrigin = t.originPoint
        val origin = if (plannedOrigin != null) {
            routeState = null // "hesaplanıyor…" durumuna dön
            GeoPoint(plannedOrigin.first, plannedOrigin.second)
        } else {
            if (!permissionGranted) {
                // İzin isteği hâlâ açık olabilir; yalnızca reddedildiyse mesaj göster.
                if (permissionAsked) {
                    routeState = RouteResult.Error(
                        "Konum izni verilmedi. Seyahati düzenleyip bir kalkış noktası seçebilir " +
                            "ya da konum izni verebilirsin."
                    )
                }
                return@LaunchedEffect
            }
            routeState = null
            val loc = locationTracker.getCurrentLocation()
            if (loc == null) {
                routeState = RouteResult.Error(
                    "Konum alınamadı. GPS'i açabilir ya da seyahati düzenleyip kalkış noktası seçebilirsin."
                )
                return@LaunchedEffect
            }
            GeoPoint(loc.latitude, loc.longitude)
        }

        routeState = provider.getRoute(
            origin = origin,
            destination = GeoPoint(t.latitude, t.longitude)
        )
    }

    // Rota gelince örnekle + ETA ata; ardından yer adlarını arka planda doldur.
    LaunchedEffect(routeState) {
        val state = routeState
        if (state is RouteResult.Success) {
            // Nokta sayısını rota uzunluğuna göre hedefle (~6 anlamlı nokta),
            // çok kısa/uzun rotalarda mantıklı aralıkta kal.
            val interval = (state.route.distanceMeters / (TARGET_WAYPOINTS + 1))
                .coerceIn(40_000.0, 150_000.0)
            val sampled = EtaCalculator.assignEtas(
                state.route,
                RouteSampler.sample(state.route, interval),
                departureMillis
            )
            waypoints = sampled
            // Reverse geocode — markerlar zaten çizildi; isimler geldikçe güncellenir.
            val named = sampled.map { wp -> wp.copy(placeName = geocoder.placeName(wp.location)) }
            // Ardışık aynı-isimli noktaları tekilleştir (aynı ilçede birden çok nokta olmasın).
            waypoints = named.filterIndexed { i, wp ->
                i == 0 || wp.placeName == null || wp.placeName != named[i - 1].placeName
            }
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

    // Seçili noktayı haritada vurgulama halkasıyla işaretle (seçim değişince güncellenir).
    LaunchedEffect(selected, styleRef, routeState, waypoints) {
        val style = styleRef ?: return@LaunchedEffect
        if (style.isFullyLoaded) drawSelectedHighlight(style, selected?.location)
    }

    // Haritadaki marker'a dokununca o noktanın detay sheet'ini aç.
    LaunchedEffect(mapRef) {
        val map = mapRef ?: return@LaunchedEffect
        map.addOnMapClickListener { latLng ->
            val screen = map.projection.toScreenLocation(latLng)
            val tol = 26f
            val rect = RectF(screen.x - tol, screen.y - tol, screen.x + tol, screen.y + tol)
            val f = map.queryRenderedFeatures(rect, WAYPOINTS_LAYER_ID, ENDPOINTS_LAYER_ID).firstOrNull()
                ?: return@addOnMapClickListener false
            val geom = f.geometry()
            val loc = (geom as? Point)?.let { GeoPoint(it.latitude(), it.longitude()) }
            when {
                f.hasProperty("wp_index") -> {
                    waypointsLatest.getOrNull(f.getNumberProperty("wp_index").toInt())?.let { wp ->
                        selected = RoutePointSelection(
                            wp.placeName ?: "Ara nokta",
                            formatEta(wp.etaEpochMillis),
                            wp.weather,
                            wp.location
                        )
                    }
                    true
                }
                f.hasProperty("point_type") -> {
                    selected = if (f.getStringProperty("point_type") == "start") {
                        RoutePointSelection("Kalkış", null, startWeatherLatest, loc)
                    } else {
                        RoutePointSelection(destNameLatest ?: "Varış", null, endWeatherLatest, loc)
                    }
                    true
                }
                else -> false
            }
        }
    }

    // "Analiz Et" aksiyonu: başlangıç, ara noktalar ve varış için tahmini geçiş anına göre
    // hava durumunu paralel çeker, risk atar, markerları yeniden boyar.
    fun analyzeWeather() {
        val route = (routeState as? RouteResult.Success)?.route ?: return
        if (analyzing) return
        scope.launch {
            analyzing = true
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
                destinationName = trip?.displayName,
                originName = trip?.originDisplayName,
                departureMillis = departureMillis,
                analyzing = analyzing,
                analyzed = analyzed,
                onAnalyze = ::analyzeWeather,
                onSelect = { selected = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Nokta detay sheet'i (çip veya marker seçimi).
    selected?.let { sel ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            containerColor = HavamaniaTheme.colors.surface
        ) {
            WaypointDetailSheet(sel, HavamaniaTheme.colors)
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
    originName: String?,
    departureMillis: Long,
    analyzing: Boolean,
    analyzed: Boolean,
    onAnalyze: () -> Unit,
    onSelect: (RoutePointSelection) -> Unit,
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

                // En riskli ara nokta (varsa) — nerede/ne zaman/neden.
                val worst = waypoints
                    .filter { it.weather != null && it.weather.risk != RouteRisk.OK }
                    .maxByOrNull { it.weather!!.risk.ordinal }
                worst?.weather?.let { ww ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "En dikkatli: ${worst.placeName ?: "ara nokta"} (~${formatEta(worst.etaEpochMillis)}) — ${ww.riskReason ?: ""}",
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Risk zaman şeridi
                Spacer(Modifier.height(12.dp))
                RouteRiskTimeline(
                    startRisk = startWeather?.risk,
                    waypointRisks = waypoints.map { it.weather?.risk },
                    endRisk = endWeather?.risk,
                    startTime = formatEta(departureMillis),
                    endTime = formatEta(departureMillis + (route.durationSeconds * 1000).toLong()),
                    c = c
                )

                Spacer(Modifier.height(14.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        WeatherChip(
                            title = "Kalkış", time = null, weather = startWeather, c = c,
                            onClick = { onSelect(RoutePointSelection(originName ?: "Kalkış", null, startWeather, route.origin)) }
                        )
                    }
                    items(waypoints) { wp ->
                        val title = wp.placeName ?: "Ara nokta"
                        val time = formatEta(wp.etaEpochMillis)
                        WeatherChip(
                            title = title,
                            time = time,
                            weather = wp.weather,
                            c = c,
                            onClick = { onSelect(RoutePointSelection(title, time, wp.weather, wp.location)) }
                        )
                    }
                    item {
                        val title = destinationName ?: "Varış"
                        WeatherChip(
                            title = title,
                            time = null,
                            weather = endWeather,
                            c = c,
                            highlight = true,
                            onClick = { onSelect(RoutePointSelection(title, null, endWeather, route.destination)) }
                        )
                    }
                }
            }
        }
    }
}

/** Tek bir güzergâh noktasının hava kartı (yer + saat + emoji + °C + yağış% + risk halkası). */
@Composable
private fun WeatherChip(
    title: String,
    time: String?,
    weather: WaypointWeather?,
    c: HavamaniaColors,
    highlight: Boolean = false,
    onClick: () -> Unit = {}
) {
    val ringColor = if (weather != null) riskColor(weather.risk, c) else c.textMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(94.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) c.accent.copy(alpha = 0.12f) else c.surface.copy(alpha = 0.55f))
            .border(1.5.dp, ringColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(
            text = title,
            color = c.textPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = time ?: " ",
            color = c.textMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (weather != null) WeatherUtils.getWeatherEmoji(weather.weatherCode) else "—",
            fontSize = 24.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (weather != null) "${weather.temperatureC.toInt()}°" else "—",
            color = c.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        val prob = weather?.precipProbability
        if (prob != null && prob > 0) {
            Text(
                text = "💧%$prob",
                color = c.textSecondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
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

/** Rota boyunca (kalkış → ara noktalar → varış) risk renklerini gösteren yatay şerit. */
@Composable
private fun RouteRiskTimeline(
    startRisk: RouteRisk?,
    waypointRisks: List<RouteRisk?>,
    endRisk: RouteRisk?,
    startTime: String,
    endTime: String,
    c: HavamaniaColors
) {
    val risks = listOf(startRisk) + waypointRisks + listOf(endRisk)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            risks.forEach { r ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (r != null) riskColor(r, c) else c.textMuted.copy(alpha = 0.4f))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(startTime, color = c.textMuted, style = MaterialTheme.typography.labelSmall)
            Text(endTime, color = c.textMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatEta(millis: Long?): String {
    if (millis == null) return "—"
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return String.format("%02d:%02d", dt.hour, dt.minute)
}

/** Seçili nokta için detay içeriği (ModalBottomSheet içinde gösterilir). */
@Composable
private fun WaypointDetailSheet(sel: RoutePointSelection, c: HavamaniaColors) {
    val w = sel.weather
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = sel.title,
            color = c.textPrimary,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (sel.time != null) {
            Text(
                text = "Tahmini geçiş saati: ${sel.time}",
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(20.dp))

        if (w == null) {
            Text(
                text = "Bu nokta için hava verisi alınamadı.",
                color = c.textSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        // Emoji + durum + sıcaklık
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = WeatherUtils.getWeatherEmoji(w.weatherCode), fontSize = 44.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = WeatherUtils.getWeatherDisplayName(w.weatherCode, LocalDateTime.now(), null, null),
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val feels = w.apparentTempC
                Text(
                    text = "${w.temperatureC.toInt()}°" + if (feels != null) "  (hissedilen ${feels.toInt()}°)" else "",
                    color = c.textSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Risk rozeti
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(riskColor(w.risk, c))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (w.risk) {
                    RouteRisk.OK -> "Sürüş için uygun"
                    RouteRisk.CAUTION -> "Dikkat: ${w.riskReason ?: "hava koşulları"}"
                    RouteRisk.DANGER -> "Tehlikeli: ${w.riskReason ?: "olumsuz koşullar"}"
                },
                color = c.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.border)
        )
        Spacer(Modifier.height(8.dp))

        DetailRow("Yağış ihtimali", w.precipProbability?.let { "%$it" } ?: "—", c)
        DetailRow("Rüzgar", WeatherUtils.formatWindWithLevel(w.windSpeedKmh), c)
        DetailRow("Nem", w.humidity?.let { "%$it" } ?: "—", c)
    }
}

@Composable
private fun DetailRow(label: String, value: String, c: HavamaniaColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = c.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            color = c.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
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
            waypoints.mapIndexed { i, wp ->
                Feature.fromGeometry(Point.fromLngLat(wp.location.longitude, wp.location.latitude))
                    .apply {
                        addStringProperty("risk", wp.weather?.risk?.name ?: "NONE")
                        addNumberProperty("wp_index", i)
                    }
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

/** Seçili noktayı, marker'ın etrafına çizilen bir vurgu halkasıyla işaretler (null → temizler). */
private fun drawSelectedHighlight(style: Style, point: GeoPoint?) {
    val fc = if (point != null) {
        FeatureCollection.fromFeatures(
            listOf(Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude)))
        )
    } else {
        FeatureCollection.fromFeatures(emptyList<Feature>())
    }
    val src = style.getSourceAs<GeoJsonSource>(SELECTED_SOURCE_ID)
    if (src != null) {
        src.setGeoJson(fc)
    } else {
        style.addSource(GeoJsonSource(SELECTED_SOURCE_ID, fc))
        style.addLayer(
            CircleLayer(SELECTED_LAYER_ID, SELECTED_SOURCE_ID).withProperties(
                PropertyFactory.circleRadius(13f),
                PropertyFactory.circleColor(Color.argb(0, 0, 0, 0)), // şeffaf dolgu → marker görünür
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(SELECTED_RING_COLOR)
            )
        )
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
