package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MainDailyNullDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun validDayStillRendersAndSelectsAfterNullNeighbor() {
        val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"Europe/Istanbul","current":{"temperature_2m":17.0},"daily":{"time":["2026-11-25","2026-11-26"],"temperature_2m_min":[null,12.0],"temperature_2m_max":[20.0,22.0],"weather_code":[0,61],"precipitation_probability_max":[null,80]}}"""
        val response = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString(OpenMeteoResponse.serializer(), wire)
        val data = WeatherMapper.mapToDomain(response, "İstanbul", now = Instant.parse("2026-11-25T10:00:00Z"))
        var selected: String? = null
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp)) {
                        DailyForecastPanel(forecasts = data.dailyForecast, onDayClick = { selected = it.date })
                    }
                }
            }
        }
        compose.onNodeWithText("26/11").assertIsDisplayed()
        compose.onNodeWithText("22°").assertIsDisplayed()
        compose.onNodeWithText("25/11").assertDoesNotExist()
        compose.onNodeWithText("0°").assertDoesNotExist()
        compose.onNodeWithText("26/11").performClick()
        compose.runOnIdle { assertEquals("2026-11-26", selected); assertEquals("17°", data.temperature) }
    }
}