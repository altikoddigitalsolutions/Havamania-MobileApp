package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ForecastLayoutDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun lightLargeTextPreservesTemperaturesAndPrecipitation() = verify(AppTheme.LIGHT, 2f)
    @Test fun darkNormalTextPreservesSelectionAndValues() = verify(AppTheme.DARK, 1f)
    @Test fun wideDailyRowKeepsDayAndTemperaturesOnOneLine() {
        compose.setContent { Fixture(AppTheme.DARK, 1f) {
            Box(Modifier.width(560.dp)) {
                DailyForecastRow(DailyForecast("Çarşamba", "2026-11-25", -18, 42)) {}
            }
        } }
        val day = compose.onNodeWithText("Çarşamba", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        val high = compose.onNodeWithText("42°", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(high.left > day.right)
        assertTrue(kotlin.math.abs(high.top.value - day.top.value) < 24f)
        saveImage("forecast-wide-row-20261125")
    }

    @Test fun dailyPanelExpandsRestoresAndCollapsesAtLargeFont() {
        val restore = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        val first = java.time.LocalDate.of(2026, 11, 25)
        val days = (0..9).map { index -> DailyForecast("Gün${index + 1}", first.plusDays(index.toLong()).toString(), index, index + 10) }
        var selected by mutableStateOf("")
        var callbacks = 0
        restore.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Column(Modifier.width(320.dp).height(500.dp).verticalScroll(rememberScrollState())) {
                DailyForecastPanel(days, selected) { selected = it.date; callbacks++ }
            }
        } }
        compose.onNodeWithText("02/12").assertDoesNotExist()
        compose.onNode(hasClickAction() and hasText("Tüm günleri göster"))
            .performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("02/12").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("02/12").assertExists()
        compose.onNode(isSelectable() and hasText("02/12")).performScrollTo().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, callbacks); assertEquals("2026-12-02", selected) }
        compose.onNode(hasClickAction() and hasText("7 güne düşür")).performScrollTo().assertHeightIsAtLeast(48.dp)
        saveImage("forecast-panel-expanded-large-20261125")
        compose.onNode(hasClickAction() and hasText("7 güne düşür")).performClick()
        compose.onNodeWithText("02/12").assertDoesNotExist()
    }

    @Test fun hourlyListSeparatesRepeatedClockTimesAcrossTwoDays() {
        val first = java.time.LocalDateTime.of(2026, 11, 25, 0, 0)
        val hours = (0..47).map { index ->
            val date = first.plusHours(index.toLong())
            HourlyWeather(date.toLocalTime().toString(), date.toString(), "${index}°", precipProb = "%100")
        }
        var selected by mutableIntStateOf(0)
        val callbacks = mutableListOf<Int>()
        compose.setContent { Fixture(AppTheme.LIGHT, 2f) {
            Box(Modifier.width(320.dp)) {
                HourlyForecastContent(items = hours.mapIndexed { index, hour -> hour.copy(isSelected = index == selected) },
                    onItemSelect = { selected = it; callbacks.add(it) }, currentTheme = AppTheme.LIGHT)
            }
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(25)
        compose.onNode(isSelectable() and hasText("01:00")).assertIsNotSelected().performClick().assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(listOf(25), callbacks) }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
        compose.onNode(isSelectable() and hasText("01:00")).assertIsNotSelected()
    }

    private fun verify(theme: AppTheme, scale: Float) {
        var dailySelected by mutableStateOf(false)
        var hourlySelected by mutableStateOf(false)
        var dailyClicks = 0
        var hourlyClicks = 0
        compose.setContent {
            Fixture(theme, scale) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    DailyForecastRow(DailyForecast("Çarşamba", "2026-11-25", -18, 42), dailySelected) {
                        dailySelected = true; dailyClicks++
                    }
                    HourlyForecastItem(HourlyWeather("10:00", "2026-11-25T10:00", "-18°",
                        precipProb = "%100", isSelected = hourlySelected), currentTheme = theme) {
                        hourlySelected = true; hourlyClicks++
                    }
                }
            }
        }
        compose.onNodeWithText("Çarşamba").assertIsDisplayed()
        compose.onNodeWithText("42°").assertIsDisplayed()
        compose.onAllNodes(isSelectable())[0].assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithText("%100", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                action(layouts)
                assertTrue(layouts.isNotEmpty())
                val layout = layouts.single()
                assertEquals("%100", layout.layoutInput.text.text)
                assertFalse("Precipitation must not clip size=${layout.size} lines=${layout.lineCount} width=${layout.didOverflowWidth} height=${layout.didOverflowHeight} text=${layout.layoutInput.text}", layout.hasVisualOverflow)
            }
        compose.onAllNodes(isSelectable())[1].performScrollTo().assertIsNotSelected().performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, dailyClicks); assertEquals(1, hourlyClicks) }
        saveImage("forecast-${theme.name.lowercase()}-20261125")

    }
    private fun saveImage(name: String) {
        val resolver = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Havamania-QA")
        }
        val uri = checkNotNull(resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        resolver.openOutputStream(uri)!!.use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
        }

    }
    @Composable private fun Fixture(theme: AppTheme, scale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        val colors = ThemeFactory.createColors(theme)
        CompositionLocalProvider(LocalDensity provides Density(density, scale), LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout { MaterialTheme { Box(Modifier.background(colors.background)) { content() } } }
        }
    }
}
