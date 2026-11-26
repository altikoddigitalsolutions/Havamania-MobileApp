package com.havamania

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TravelFilterChoicesDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun filtersWrapAsControlsInsteadOfSplittingLabelsAcrossThemesAndSizes() {
        val configuration = mutableStateOf(Triple(AppTheme.DARK, 280, 1f))
        val selected = mutableStateOf(TravelFilter.UPCOMING)
        val clicks = mutableListOf<TravelFilter>()
        compose.setContent {
            val (theme, width, scale) = configuration.value
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale),
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                Box(Modifier.width(width.dp)) {
                    TravelFilterChoices(selected.value, { selected.value = it; clicks.add(it) })
                }
            }
        }
        val themes = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SPRING, AppTheme.SUMMER, AppTheme.AUTUMN, AppTheme.WINTER)
        for (theme in themes) for ((width, scale) in listOf(280 to 1f, 320 to 2f, 760 to 1f)) {
            compose.runOnIdle { configuration.value = Triple(theme, width, scale) }
            listOf("Yaklaşanlar", "Geçmiş", "Arşiv").forEach { text ->
                val node = compose.onNodeWithText(text)
                node.assertIsDisplayed().performClick().assertIsSelected()
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(text, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue(layouts.isNotEmpty())
                assertTrue("Split label: $theme/$width/$text", layouts.all { it.lineCount == 1 && !it.didOverflowHeight })
                val bounds = node.getUnclippedBoundsInRoot()
                assertTrue(bounds.bottom - bounds.top >= 48.dp)
            }
        }
        compose.runOnIdle { assertEquals(54, clicks.size) }
    }
}
