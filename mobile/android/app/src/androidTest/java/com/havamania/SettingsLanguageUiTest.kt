package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Pure dialog; no real preferences, ViewModels or account calls. */
class SettingsLanguageUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(theme: AppTheme, saved: String, onSelected: (String) -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout {
                    MaterialTheme { LanguageSelectionDialog(saved, onSelected, {}) }
                }
            }
        }
    }

    @Test fun legacyEnglishSelectionShowsActualLanguageAndCannotSelectUnavailableOption() {
        var calls = 0
        show(AppTheme.LIGHT, "EN") { calls++ }
        compose.onNodeWithText("Türkçe").assertIsSelected()
        compose.onNodeWithText("English").assertIsNotSelected().assertIsNotEnabled()
        compose.onNodeWithText("Henüz kullanılamıyor").assertIsDisplayed()
        compose.onNodeWithText("English").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun darkLargeTextKeepsSupportedLanguageAndCloseReachable() {
        var selected: String? = null
        var calls = 0
        show(AppTheme.DARK, "TR") { selected = it; calls++ }
        compose.onNodeWithText("Türkçe").assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("TR", selected); assertEquals(1, calls) }
        compose.onNodeWithText("Kapat").assertIsDisplayed()
    }
}
