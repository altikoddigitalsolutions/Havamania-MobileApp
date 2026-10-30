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

    private fun show(theme: AppTheme, large: Boolean,
        state: AssistantConsentState = AssistantConsentState(document = doc, visible = true),
        onRetry: () -> Unit = {}, currentState: (() -> AssistantConsentState)? = null, onSave: (Boolean) -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, if (large) 2f else 1f)) {
                ProvideResponsiveLayout {
                    MaterialTheme { AssistantConsentDialog(currentState?.invoke() ?: state, onSave, {}, onRetry, {}) }
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

    @Test fun withdrawalErrorHasSpecificRetryAndCompactTabletDialog() {
        var retries = 0
        show(AppTheme.LIGHT, false, state = AssistantConsentState(visible = true,
            error = "İzinlerin geri çekildiği doğrulanamadı.", retry = ConsentRetry.REVOKE, withdrawalPending = true),
            onRetry = { retries++ }) { }
        compose.onNodeWithText("İzni geri çekmeyi tekrar dene").assertIsDisplayed().performClick()
        compose.onNodeWithText("Kapat").assertIsDisplayed()
        compose.onNodeWithText("Uzak asistan izinlerini geri çek").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries) }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        if (context.resources.configuration.smallestScreenWidthDp >= 600) {
            val height = compose.onNodeWithTag("assistant_consent_dialog").fetchSemanticsNode().boundsInRoot.height
            assertTrue("Short error must not fill the tablet", height < context.resources.displayMetrics.heightPixels * 0.75f)
        }
    }

    @Test fun successfulWithdrawalShowsConfirmationAndNoImmediateApprovalButton() {
        show(AppTheme.DARK, false, state = AssistantConsentState(document = doc.copy(accepted = false), visible = true,
            notice = "Uzak asistan izinleriniz geri çekildi.")) { }
        compose.onNodeWithText("Uzak asistan izinleriniz geri çekildi.").assertIsDisplayed()
        compose.onNodeWithText("Kapat").assertIsDisplayed()
        compose.onNodeWithText("Onayla ve devam et").assertDoesNotExist()
    }

    @Test fun healthOptOutShowsPendingServerStateAndKeepsCheckboxOff() {
        var selected: Boolean? = null
        show(AppTheme.DARK, true, state = AssistantConsentState(
            document = doc.copy(accepted = true, health_accepted = false, health_sync_required = true), visible = true)) {
            selected = it
        }
        compose.onNodeWithText("Sağlık izninin sunucuda kaldırıldığı henüz doğrulanmadı. Bu cihaz profil sağlık bilgilerini göndermiyor. Seçimleri kaydederek yeniden deneyebilirsiniz.")
            .performScrollTo().assertIsDisplayed()
        compose.onNode(isToggleable()).performScrollTo().assertIsOff()
        compose.onNodeWithText("Seçimleri kaydet").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false, selected) }
    }

    @Test fun refreshedPrivacyDecisionResetsStaleCheckedStateWithoutTextVersionChange() {
        var current by mutableStateOf(AssistantConsentState(
            document = doc.copy(accepted = true, health_accepted = true), visible = true))
        show(AppTheme.LIGHT, false, currentState = { current }) { }
        compose.onNode(isToggleable()).performScrollTo().assertIsOn()
        compose.runOnIdle {
            current = current.copy(document = doc.copy(accepted = true, health_accepted = false, health_sync_required = true))
        }
        compose.onNode(isToggleable()).performScrollTo().assertIsOff()
    }
}
