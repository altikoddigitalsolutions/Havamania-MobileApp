package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Error UI only. Does not create production ViewModels or modify preferences. */
class SettingsIssueUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(theme: AppTheme, state: MutableState<SettingsIssue?>, reload: () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout { MaterialTheme {
                    SettingsIssueDialog(state.value, { state.value = null }, reload)
                } }
            }
        }
    }

    @Test fun saveErrorKeepsSafeMessageAndDoesNotReplayStaleWrite() {
        val state = mutableStateOf<SettingsIssue?>(SettingsIssue.SAVE)
        var reloads = 0
        show(AppTheme.DARK, state) { reloads++ }
        compose.onNodeWithText("Ayar kaydedilemedi").assertIsDisplayed()
        compose.onNodeWithText("Değişiklik kaydedilemedi. Lütfen ayarı yeniden seçin.").assertIsDisplayed()
        compose.onNodeWithText("Yeniden yükle").assertDoesNotExist()
        compose.onNodeWithText("Tamam").assertIsDisplayed().performClick()
        compose.runOnIdle { assertNull(state.value); assertEquals(0, reloads) }
    }

    @Test fun loadErrorOffersExplicitReloadInLightLargeText() {
        val state = mutableStateOf<SettingsIssue?>(SettingsIssue.LOAD)
        var reloads = 0
        show(AppTheme.LIGHT, state) { reloads++; state.value = null }
        compose.onNodeWithText("Ayarlar yüklenemedi").assertIsDisplayed()
        compose.onNodeWithText("Yeniden yükle").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, reloads); assertNull(state.value) }
    }
}
