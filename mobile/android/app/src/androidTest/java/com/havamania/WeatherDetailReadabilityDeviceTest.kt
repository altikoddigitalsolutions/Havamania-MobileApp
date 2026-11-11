package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test

class WeatherDetailReadabilityDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun longMetricTextRemainsReadableAtDoubleFontSize() {
        val detail = WeatherDetailData("Yağış olasılığı", "%88", "Yağmur olasılığı gün boyunca yüksek", "WaterDrop", "#0088FF")
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                MaterialTheme {
                    Column(Modifier.width(180.dp).height(600.dp).verticalScroll(rememberScrollState())) {
                        WeatherDetailCard(detail)
                    }
                }
            }
        }
        compose.onNodeWithText(detail.title.uppercase()).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(detail.value).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(detail.description).performScrollTo().assertIsDisplayed()
    }
}
