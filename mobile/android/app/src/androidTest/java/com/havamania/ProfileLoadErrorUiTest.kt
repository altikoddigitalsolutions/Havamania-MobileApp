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

/** Isolated composable: no default Firebase or real account access. */
class ProfileLoadErrorUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(theme: AppTheme, message: String, owner: State<String?>, retry: () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout { MaterialTheme { ProfileLoadErrorDialog(message, owner.value, retry) } }
            }
        }
    }
    @Test fun dismissingDataErrorDoesNotRetryInLightLargeText() {
        var retries = 0
        show(AppTheme.LIGHT, PROFILE_DATA_ERROR, mutableStateOf("a")) { retries++ }
        compose.onNodeWithText(PROFILE_DATA_ERROR).assertIsDisplayed()
        compose.onNodeWithText("Vazgeç").assertIsDisplayed().performClick()
        compose.onNodeWithText("Profil yüklenemedi").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, retries) }
    }
    @Test fun retryAndDismissAreSeparateInDarkLargeText() {
        var retries = 0
        show(AppTheme.DARK, PROFILE_CONNECTION_ERROR, mutableStateOf("a")) { retries++ }
        compose.onNodeWithText("Tekrar dene").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Vazgeç").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Profil yüklenemedi").assertDoesNotExist()
    }
    @Test fun dismissStateDoesNotHideAnotherAccountsError() {
        val owner = mutableStateOf<String?>("a")
        show(AppTheme.DARK, PROFILE_DATA_ERROR, owner) { fail("Unexpected retry") }
        compose.onNodeWithText("Vazgeç").performClick()
        compose.runOnIdle { owner.value = "b" }
        compose.onNodeWithText("Profil yüklenemedi").assertIsDisplayed()
    }
}
