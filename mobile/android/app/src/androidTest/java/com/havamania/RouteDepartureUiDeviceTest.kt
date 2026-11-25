package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

class RouteDepartureUiDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unresolvedClockCannotEnableAnalysisAndRetryCanRecover() {
        val local = Instant.now().plusSeconds(3600).atZone(ZoneId.of("America/New_York"))
        val trip = TravelPlan(city = "Tokyo", startDate = local.toLocalDate(), endDate = local.toLocalDate().plusDays(1),
            departureTime = local.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
        var state by mutableStateOf(RouteDepartureUiState(loading = true))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalRouteDepartureState provides state) {
                ProvideResponsiveLayout { MaterialTheme {
                    Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                        RouteContent(trip, RouteResult.Success(RoutePath(emptyList(), 1000.0, 600.0)), emptyList(), null, null,
                            state.clock?.departure?.toEpochMilli(), false, false, false, null,
                            onRetryRoute = { state = RouteDepartureUiState(clock = routeNotificationClock(trip, "America/New_York", Instant.now())) },
                            onAnalyze = {}, onSelect = {})
                    }
                } }
            }
        }
        compose.onNodeWithText("Çıkış saat dilimi belirleniyor…").assertExists()
        compose.onNodeWithText("Akıllı Güzergâh Analizi").assertDoesNotExist()
        compose.runOnIdle { state = RouteDepartureUiState(error = "Saat dilimi alınamadı.") }
        compose.onNodeWithText("Çıkış saatini tekrar çöz").performScrollTo().performClick()
        compose.onNodeWithText("Saat dilimi alınamadı.").assertDoesNotExist()
        compose.onNodeWithText("Akıllı Güzergâh Analizi").performScrollTo().assertIsDisplayed()
    }
    @Test fun timelineNamesOriginBasisAndKeepsDateAndTimeWithLargeFont() {
        val trip = TravelPlan(city = "Tokyo", originCity = "New York", startDate = LocalDate.of(2026, 11, 26),
            endDate = LocalDate.of(2026, 11, 27), departureTime = "09:00")
        val clock = routeNotificationClock(trip, "America/New_York", Instant.parse("2026-11-25T12:00:00Z"))!!
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            val density = LocalDensity.current
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalRouteDepartureState provides RouteDepartureUiState(clock = clock), LocalDensity provides Density(density.density, 1.6f)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                        TimelineContent(trip, RoutePath(listOf(GeoPoint(40.7, -74.0), GeoPoint(35.6, 139.7)), 1000.0, 3600.0),
                            emptyList(), null, null, clock.departure.toEpochMilli(), false) {}
                    }
                }
            }
        }
        compose.onNodeWithText("Tüm saatler çıkış saat diliminde: America/New_York.").assertExists()
        compose.onNodeWithText("26 Kas 09:00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("26 Kas 10:00").performScrollTo().assertIsDisplayed()
    }
}