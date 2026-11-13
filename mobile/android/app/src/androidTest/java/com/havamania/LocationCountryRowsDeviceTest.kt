package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Pure search fixtures, no account or preference mutation. */
class LocationCountryRowsDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val rows = listOf(
        GeocodingResultDto(name = "Paris", latitude = 48.8, longitude = 2.3, country = "Fransa"),
        GeocodingResultDto(name = "Paris", latitude = 33.6, longitude = -95.5, country = "ABD"))
    private fun host(content: @Composable () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme { content() } }
            }
        }
    }
    @Test fun managementDoesNotMarkBothZeroIdLocationsAsDefault() {
        var selected: String? = null
        host {
            CityManagementContent("", {}, emptyList(), rows, rows.first(),
                onAdd = {}, onDelete = {}, onSetDefault = { selected = it.country })
        }
        compose.onAllNodesWithText("Paris").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Paris varsayılan konum yap").assertCountEquals(1)
        compose.onNodeWithContentDescription("Paris varsayılan konum yap").performClick()
        compose.runOnIdle { assertEquals("ABD", selected) }
    }
    @Test fun managementRetainsBothZeroIdSuggestions() {
        var selected: String? = null
        host {
            CityManagementContent("par", {}, rows, emptyList(), null,
                onAdd = { selected = it.country }, onDelete = {}, onSetDefault = {},
                searchStatus = LocationSearchStatus.RESULTS)
        }
        compose.onAllNodesWithText("Paris").assertCountEquals(2)
        compose.onNodeWithText("ABD").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("ABD", selected) }
    }
    @Test fun weatherRowsDistinguishSameNamesWithoutIds() {
        var country: String? = null
        host {
            WeatherCitySearchContent("par", {}, rows, LocationSearchStatus.RESULTS, {}, onSelect = { country = it.country })
        }
        compose.onAllNodesWithText("Paris").assertCountEquals(2)
        compose.onNodeWithText("Fransa").assertIsDisplayed()
        compose.onNodeWithText("ABD").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("ABD", country) }
    }
    @Test fun travelRowsDistinguishSameNamesWithoutIds() {
        var country: String? = null
        host {
            LocationSearchField("par", {}, rows, { country = it.country }, "Varış")
        }
        compose.onAllNodesWithText("Paris").assertCountEquals(2)
        compose.onNodeWithText("Fransa").assertIsDisplayed()
        compose.onNodeWithText("ABD").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("ABD", country) }
    }
}