package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RouteNumericRiskDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun missingTemperatureAndCombinedWindWarningRemainVisible() {
        val input = RouteHourlyDto(time = listOf("2026-11-25T12:00"), temperature = listOf(Double.NaN), weatherCode = listOf(95),
            precipitationProbability = listOf(10), windSpeed = listOf(80.0), windGusts = listOf(90.0))
        var weather by mutableStateOf(input.routeWeatherSample(0)!!)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp).height(650.dp).verticalScroll(rememberScrollState())) {
                        TimelineWaypointItem("Riskli nokta", "15:00", weather, isFirst = true, isLast = true,
                            missingForecastLabel = "Tahmin yok", etaEpochMillis = 0L) {}
                    }
                }
            }
        }
        compose.onNodeWithText("Bilgi yok").assertExists()
        compose.onNodeWithText("Fırtınalı").assertExists()
        compose.onNodeWithText("rüzgar", substring = true, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>(); action(layouts)
                val layout = layouts.single()
                assertTrue("Wrong layout: ${layout.layoutInput.text}", layout.layoutInput.text.text.contains("rüzgar"))
                assertTrue("Risk must wrap: ${layout.layoutInput.text}", layout.lineCount > 1)
                assertFalse("Risk clipped: ${layout.size}", layout.hasVisualOverflow)
            }
        compose.runOnIdle { weather = input.copy(temperature = listOf(20.0), weatherCode = listOf(0), precipitationProbability = listOf(0),
            windSpeed = listOf(5.0), windGusts = listOf(8.0)).routeWeatherSample(0)!! }
        compose.onNodeWithText("Bilgi yok").assertDoesNotExist()
        compose.onNodeWithText("rüzgar", substring = true, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("20°").assertExists()
    }
}