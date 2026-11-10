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

class WeatherCacheClearUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(theme: AppTheme, state: MutableState<WeatherCacheClearState>, clear: () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout {
                    MaterialTheme { WeatherCacheSettingsAction(state.value, clear) { state.value = WeatherCacheClearState.IDLE } }
                }
            }
        }
    }

    @Test fun largeTextBusyStateDisablesRepeatAndSuccessAppearsOnlyAfterCompletion() {
        val state = mutableStateOf(WeatherCacheClearState.IDLE)
        var calls = 0
        show(AppTheme.DARK, state) { calls++; state.value = WeatherCacheClearState.CLEARING }
        compose.onNodeWithText("Önbelleği Temizle").assertIsDisplayed().performClick()
        compose.onNodeWithText("Önbelleği Temizle").assertIsNotEnabled()
        compose.onNodeWithText("Hava durumu önbelleği temizlendi.").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, calls); state.value = WeatherCacheClearState.SUCCESS }
        compose.onNodeWithText("Hava durumu önbelleği temizlendi.").assertIsDisplayed()
        compose.onNodeWithText("Tamam").performClick()
        compose.runOnIdle { assertEquals(WeatherCacheClearState.IDLE, state.value) }
    }

    @Test fun errorOffersRetryWithoutFalseSuccessInLightTheme() {
        val state = mutableStateOf(WeatherCacheClearState.ERROR)
        var calls = 0
        show(AppTheme.LIGHT, state) { calls++; state.value = WeatherCacheClearState.CLEARING }
        compose.onNodeWithText("Önbellek temizlenemedi. Lütfen tekrar deneyin.").assertIsDisplayed()
        compose.onNodeWithText("Hava durumu önbelleği temizlendi.").assertDoesNotExist()
        compose.onNodeWithText("Tekrar dene").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithText("Önbelleği Temizle").assertIsNotEnabled()
    }
}
