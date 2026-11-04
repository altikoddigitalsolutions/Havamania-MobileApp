package com.havamania

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AssistantWeatherSummaryDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowLayoutKeepsAllFeatureCardsVisibleAndClickableAtLargeFontScale() {
        val prompts = mutableListOf<String>()
        val hasTrip = mutableStateOf(false)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                Box(Modifier.width(320.dp)) { FeatureCards(colors, hasTrip.value) { prompts.add(it) } }
            }
        }
        compose.onNodeWithText("Seyahat havasını değerlendir").assertDoesNotExist()
        compose.onNodeWithText("Hava durumunu yorumla").assertIsDisplayed().performClick()
        compose.runOnIdle { hasTrip.value = true }
        compose.onNodeWithText("Seyahat havasını değerlendir").assertIsDisplayed().performClick()
        compose.onNodeWithText("Günlük öneri ver").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf("Bugün hava nasıl?", "Seyahatim için hava uygun mu?", "Bugün ne giymeliyim?"), prompts)
        }
    }

    @Test fun lightThemeShowsCurrentSummaryAtLargeFontScale() {
        show(AppTheme.LIGHT, stale = false)
        compose.onNodeWithText("Bugünün hava özeti").assertIsDisplayed()
        assertSummaryVisible()
    }

    @Test fun darkThemeLabelsCachedSummaryAtLargeFontScale() {
        show(AppTheme.DARK, stale = true)
        compose.onNodeWithText("Son kaydedilen hava özeti").assertIsDisplayed()
        assertSummaryVisible()
    }

    private fun show(theme: AppTheme, stale: Boolean) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                TodaySummarySection(WeatherData(cityName = "Balıkesir", condition = "SAĞANAK YAĞIŞ",
                    temperature = "18°", isStale = stale), colors)
            }
        }
    }

    private fun assertSummaryVisible() {
        compose.onNodeWithText("Balıkesir için hava durumu sağanak yağış. Sıcaklık 18°.")
            .assertIsDisplayed()
    }
}
