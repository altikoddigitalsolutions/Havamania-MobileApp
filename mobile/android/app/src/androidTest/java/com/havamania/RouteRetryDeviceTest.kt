package com.havamania

import android.location.Location
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.havamania.ui.theme.AppTheme
import com.havamania.ui.theme.LocalHavamaniaColors
import com.havamania.ui.theme.LocalHavamaniaTypography
import com.havamania.ui.theme.ThemeFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RouteRetryDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun retryHidesAnalysisUntilRouteIsReady() {
        val departure = LocalDateTime.now().plusHours(2)
        val trip = TravelPlan(
            city = "İzmir",
            startDate = departure.toLocalDate(),
            endDate = departure.toLocalDate(),
            departureTime = departure.format(DateTimeFormatter.ofPattern("HH:mm"))
        )
        val route = mutableStateOf<RouteResult?>(RouteResult.Error("Test"))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.AUTO)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                RouteContent(
                    trip = trip,
                    routeState = route.value,
                    waypoints = emptyList(),
                    startWeather = null,
                    endWeather = null,
                    departureMillis = null,
                    analyzing = false,
                    analyzed = false,
                    hasIncompleteForecast = false,
                    analysisError = null,
                    onRetryRoute = { route.value = null },
                    onAnalyze = {},
                    onSelect = {},
                    showTimeline = false
                )
            }
        }
        compose.onNodeWithText("Rota yüklenemedi.").assertIsDisplayed()
        compose.onNodeWithText("Akıllı Güzergâh Analizi").assertDoesNotExist()
        compose.onNodeWithText("Rotayı tekrar yükle").performClick()
        compose.onNodeWithText("Rota yüklenemedi.").assertDoesNotExist()
        compose.onNodeWithText("Akıllı Güzergâh Analizi").assertDoesNotExist()
        compose.runOnIdle {
            route.value = RouteResult.Success(RoutePath(emptyList(), 10.0, 1.0))
        }
        compose.onNodeWithText("Akıllı Güzergâh Analizi").assertIsDisplayed()
    }

    @Test
    fun deviceRouteOriginDoesNotRequireAnAddress() = runBlocking {
        val location = Location("test").apply {
            latitude = 39.92
            longitude = 32.85
        }
        var currentLocation: Location? = location
        val tracker = object : LocationTracker {
            override suspend fun getCurrentLocation() = currentLocation
            override suspend fun getCurrentCity(): GeocodingResultDto? =
                error("Route origin must not request an address")
        }
        assertEquals(GeoPoint(39.92, 32.85), resolveDeviceRouteOrigin(tracker))
        currentLocation = null
        assertNull(resolveDeviceRouteOrigin(tracker))
    }

    @Test
    fun timelineOpensEachForecastIndependently() {
        val date = LocalDateTime.now().toLocalDate()
        val trip = TravelPlan(city = "İzmir", originCity = "Ankara", startDate = date, endDate = date)
        val point = GeoPoint(39.0, 31.0)
        val weather = WaypointWeather(0, 20.0, null, null, null, null, RouteRisk.OK, null)
        val first = RouteWaypoint(point, 100.0, 1000L, "İlk geçiş", weather)
        val second = first.copy(cumulativeDistanceMeters = 200.0, etaEpochMillis = 2000L, placeName = "İkinci geçiş")
        val route = RoutePath(listOf(GeoPoint(40.0, 32.0), point, GeoPoint(38.0, 27.0)), 300.0, 3.0)
        val analyzed = mutableStateOf(false)
        val missing = second.copy(placeName = "Eksik tahmin", weather = null)
        var selected: RouteWaypoint? = null
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.AUTO)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                Column {
                    TimelineContent(trip, route, listOf(first, second, missing), weather, weather, 0L, analyzed.value) {
                        selected = it
                    }
                }
            }
        }
        compose.onNodeWithText("Analiz tamamlanmadı").assertIsDisplayed()
        compose.runOnIdle { analyzed.value = true }
        compose.onNodeWithText("Tahmin alınamadı").assertIsDisplayed()
        compose.onNodeWithText("Analiz tamamlanmadı").assertDoesNotExist()
        compose.onNodeWithText("İlk geçiş").performClick()
        compose.runOnIdle { assertEquals(first, selected) }
        compose.onNodeWithText("İkinci geçiş").performClick()
        compose.runOnIdle { assertEquals(second, selected) }
        compose.onNodeWithText("Ankara").performClick()
        compose.runOnIdle { assertEquals(RouteWaypoint(route.points.first(), 0.0, 0L, "Ankara", weather), selected) }
        compose.onNodeWithText("İzmir").performClick()
        compose.runOnIdle { assertEquals(RouteWaypoint(route.points.last(), 300.0, 3000L, "İzmir", weather), selected) }
    }

    @Test
    fun routeDetailCanScrollAndCloseWithLargeText() {
        val reason = "Yoğun kar ve buzlanma nedeniyle güzergâhın bu bölümünde görüş mesafesi düşebilir."
        val weather = WaypointWeather(73, -2.0, null, null, null, null, RouteRisk.DANGER, reason)
        val waypoint = RouteWaypoint(GeoPoint(40.0, 29.0), 0.0, placeName = "Uzun bir güzergâh ara noktası adı", weather = weather)
        val visible = mutableStateOf(true)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.AUTO)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, 2f)
            ) {
                Box(Modifier.size(320.dp, 250.dp)) {
                    if (visible.value) RouteWaypointDetailCard(waypoint, analyzed = true) { visible.value = false }
                }
            }
        }
        compose.onNodeWithContentDescription("Güzergâh ayrıntısını kapat").assertIsDisplayed()
        compose.onNodeWithText(reason, substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Güzergâh ayrıntısını kapat").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Güzergâh ayrıntısını kapat").assertDoesNotExist()
    }

    @Test
    fun systemBackClosesDetailBeforeLeavingRoute() {
        val detailOpen = mutableStateOf(true)
        val routeOpen = mutableStateOf(true)
        val waypoint = RouteWaypoint(GeoPoint(40.0, 29.0), 0.0)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.AUTO)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                if (routeOpen.value) {
                    BackHandler { routeOpen.value = false }
                    Column {
                        TextButton(onClick = { detailOpen.value = true }) { Text("Rota ekranı") }
                        if (detailOpen.value) RouteWaypointDetailCard(waypoint, analyzed = false) {
                            detailOpen.value = false
                        }
                    }
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.onNodeWithContentDescription("Güzergâh ayrıntısını kapat").assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithContentDescription("Güzergâh ayrıntısını kapat").assertDoesNotExist()
        compose.onNodeWithText("Rota ekranı").assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Rota ekranı").assertDoesNotExist()
    }
}
