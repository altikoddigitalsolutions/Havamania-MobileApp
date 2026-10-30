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

/** Pure dialog fixture: no production ViewModel, account mutation or remote requests. */
class AssistantConsentDialogTest {
    @get:Rule val compose = createComposeRule()
    private val doc = AssistantConsentDocument(2, "Yerel test metni. ".repeat(80), "a".repeat(64),
        AssistantHealthConsent(2, "İsteğe bağlı sağlık test metni.", "b".repeat(64)))

    private fun show(theme: AppTheme, large: Boolean, onSave: (Boolean) -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, if (large) 2f else 1f)) {
                ProvideResponsiveLayout {
                    MaterialTheme { AssistantConsentDialog(AssistantConsentState(document = doc, visible = true),
                        onSave, {}, {}, {}) }
                }
            }
        }
    }

    @Test fun darkLargeTextKeepsOptionalCheckboxAndApprovalReachable() {
        var selected: Boolean? = null
        show(AppTheme.DARK, true) { selected = it }
        compose.onNodeWithText("Profil sağlık hassasiyetlerimin asistana gönderilmesine onay veriyorum.")
            .performScrollTo()
        compose.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Onayla ve devam et").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, selected) }
        compose.onNodeWithText("Vazgeç").performScrollTo().assertIsDisplayed()
    }

    @Test fun lightThemeAllowsApprovalWithoutHealthAndWithdrawalIsReachable() {
        var selected: Boolean? = null
        show(AppTheme.LIGHT, false) { selected = it }
        compose.onNodeWithText("Onayla ve devam et").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, selected) }
        compose.onNodeWithText("Uzak asistan izinlerini geri çek").performScrollTo().assertIsDisplayed()
    }
}
