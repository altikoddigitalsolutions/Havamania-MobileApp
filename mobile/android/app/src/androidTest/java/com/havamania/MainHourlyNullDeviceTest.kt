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

class MainHourlyNullDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun validHourStillRendersAndSelectsAfterNullNeighbor() {
        val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"Europe/Istanbul","current":{"temperature_2m":17.0},"hourly":{"time":["2026-11-25T13:00","2026-11-25T14:00"],"temperature_2m":[null,18.0],"weather_code":[0,3],"precipitation_probability":[null,25]}}"""
        val response = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString(OpenMeteoResponse.serializer(), wire)
        val data = WeatherMapper.mapToDomain(response, "İstanbul", now = Instant.parse("2026-11-25T10:00:00Z"))
        var selected: Int? = null
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors, LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                MaterialTheme {
                    Column(Modifier.width(320.dp)) {
                        HourlyForecastContent(items = data.hourlyForecast, currentTheme = AppTheme.LIGHT, onItemSelect = { selected = it })
                    }
                }
            }
        }
        compose.onNodeWithText("14:00").assertIsDisplayed()
        compose.onNodeWithText("18°").assertIsDisplayed()
        compose.onNodeWithText("13:00").assertDoesNotExist()
        compose.onNodeWithText("0°").assertDoesNotExist()
        compose.onNodeWithText("14:00").performClick()
        compose.runOnIdle { assertEquals(0, selected); assertEquals("17°", data.temperature) }
    }
}