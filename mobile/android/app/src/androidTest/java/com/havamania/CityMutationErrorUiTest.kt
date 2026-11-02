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

/** Pure fixture: no Firebase or preference writes. */
class CityMutationErrorUiTest {
    @get:Rule val compose = createComposeRule()
    private fun verify(theme: AppTheme, retry: Boolean = false, details: String? = null) {
        val visible = mutableStateOf(true)
        var dismisses = 0
        var retries = 0
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout { MaterialTheme {
                    CityMutationErrorDialog(visible.value,
                        onDismiss = { visible.value = false; dismisses++ },
                        onRetry = if (retry) ({ visible.value = false; retries++ }) else null, details = details)
                } }
            }
        }
        compose.onNodeWithText("Konum işlemi tamamlanamadı").assertIsDisplayed()
        compose.onNodeWithText(details ?: "Değişiklik cihazınıza kaydedilmiş olabilir; bulut eşitlemesi tamamlanmamış olabilir. Bağlantınızı ve kayıtlı konumlarınızı kontrol edin.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, retries) }
        compose.onNodeWithText(if (retry) "Eşitlemeyi tekrar dene" else "Tamam").assertIsDisplayed().performClick()
        compose.runOnIdle { assertFalse(visible.value); assertEquals(if (retry) 0 else 1, dismisses); assertEquals(if (retry) 1 else 0, retries) }
    }
    @Test fun waitingAdviceFitsLightLargeText() = verify(AppTheme.LIGHT, true,
        citySyncErrorText(CitySyncWaitingException(null, 161), 100))
    @Test fun rejectionAdviceFitsDarkLargeText() = verify(AppTheme.DARK, true,
        citySyncErrorText(CitySyncWaitingException("REJECTED", 0), 100))
    @Test fun darkLargeTextRetryIsExplicit() = verify(AppTheme.DARK, true)
    @Test fun lightLargeTextRetryIsExplicit() = verify(AppTheme.LIGHT, true)
    @Test fun darkLargeTextErrorCanBeDismissed() = verify(AppTheme.DARK)
    @Test fun lightLargeTextErrorCanBeDismissed() = verify(AppTheme.LIGHT)
}
