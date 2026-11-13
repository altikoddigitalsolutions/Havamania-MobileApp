package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Separate QA package, fixture only; no account, location or Firebase mutations. */
class TurkeyFirstCitySearchDeviceTest {
    @get:Rule val compose = createComposeRule()
    private fun verify(theme: AppTheme) {
        val rows = runBlocking {
            turkeyFirstLocationSearch("bal") { country ->
                if (country == "TR") listOf(GeocodingResultDto(2, "Balıkesir", 39.6, 27.8, "Türkiye", "TR"))
                else listOf(GeocodingResultDto(1, "Bali", -8.4, 115.1, "Endonezya", "ID"))
            }
        }
        var selected: String? = null
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    WeatherCitySearchContent("bal", {}, rows, LocationSearchStatus.RESULTS, {},
                        onSelect = { selected = it.name })
                } }
            }
        }
        val domestic = compose.onNodeWithText("Balıkesir").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        val global = compose.onNodeWithText("Bali").assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top
        assertTrue(domestic < global)
        compose.onNodeWithText("Balıkesir").performClick()
        compose.runOnIdle { assertEquals("Balıkesir", selected) }
    }
    @Test fun turkeyAppearsAboveGlobalInLightTheme() = verify(AppTheme.LIGHT)
    @Test fun turkeyAppearsAboveGlobalInDarkTheme() = verify(AppTheme.DARK)
}