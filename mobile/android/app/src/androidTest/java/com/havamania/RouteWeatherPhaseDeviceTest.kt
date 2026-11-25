package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test

class RouteWeatherPhaseDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun sampledSolarPhaseUpdatesBothLabelAndIcon() {
        var weather by mutableStateOf(WaypointWeather(0, 20.0, null, null, null, null, RouteRisk.OK, null))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp)) {
                        TimelineWaypointItem("Ara nokta", "09:00", weather, isFirst = true, isLast = true,
                            missingForecastLabel = "Tahmin yok", etaEpochMillis = 0L) {}
                    }
                }
            }
        }
        compose.onNodeWithText("Açık").assertIsDisplayed()
        compose.onNodeWithText("🌡️").assertExists()
        compose.runOnIdle { weather = weather.copy(isDay = false) }
        compose.onNodeWithText("Açık").assertDoesNotExist()
        compose.onNodeWithText("Açık gece").assertIsDisplayed()
        compose.onNodeWithText("🌙").assertExists()
        compose.runOnIdle { weather = weather.copy(isDay = true) }
        compose.onNodeWithText("Açık gece").assertDoesNotExist()
        compose.onNodeWithText("Güneşli").assertIsDisplayed()
        compose.onNodeWithText("☀️").assertExists()
    }
}