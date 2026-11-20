@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.havamania

import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Production form content with local callbacks only; no AuthViewModel, account or browser calls. */
class AuthFormsUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(scale: Float = 2f, content: @Composable () -> Unit) {
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge()
            it.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            val density = LocalDensity.current.density
            val configuration = Configuration(LocalConfiguration.current).apply {
                screenWidthDp = 320; screenHeightDp = 640
            }
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, scale), LocalConfiguration provides configuration) {


                ProvideResponsiveLayout { MaterialTheme(colorScheme = ThemeManager.getColorScheme(AppTheme.LIGHT)) {
                    Box(Modifier.requiredSize(320.dp, 640.dp).clipToBounds().background(colors.background)) { content() }
                } }
            }
        }
    }
    private fun field(index: Int) = compose.onAllNodes(hasSetTextAction())[index]

    @Test fun loginImeMovesFocusAndSubmitsTrimmedEmailWithoutChangingPassword() {
        val state = mutableStateOf<AuthState>(AuthState.Idle)
        var submitted: Pair<String, String>? = null
        show(scale = 1f) {
            LoginContent(state.value, { email, password -> submitted = email to password; state.value = AuthState.Loading },
                { state.value = AuthState.Idle }, {}, {}, {})
        }
        field(0).performScrollTo().performTouchInput { click() }
        compose.waitUntil(8_000) { compose.runOnUiThread { ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true } }
        field(0).performTextInput(" test@example.invalid ")
        field(0).performImeAction()
        field(1).assertIsFocused().performTextInput(" test-password ")
        field(1).performImeAction()
        compose.runOnIdle { assertEquals("test@example.invalid" to " test-password ", submitted) }
        compose.onNodeWithContentDescription("GİRİŞ YAP").assertIsNotEnabled()
        compose.onNodeWithText("E-posta").assertIsNotEnabled()
        compose.waitUntil(8_000) { compose.runOnUiThread { ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false } }
    }
    @Test fun registerImeCannotBypassMismatchOrTermsAndLargeTextSubmitRemainsReachable() {
        var calls = 0
        var result: Triple<String, String, String>? = null
        show {
            RegisterContent(AuthState.Idle, { name, email, password -> calls++; result = Triple(name, email, password) }, {}, {}, {})
        }
        field(0).performScrollTo().performTextInput(" Demo Gezgin ")
        field(0).performImeAction()
        field(1).assertIsFocused().performTextInput(" demo@example.invalid ")
        field(1).performImeAction()
        field(2).assertIsFocused().performTextInput("test-password")
        field(2).performImeAction()
        field(3).assertIsFocused().performTextInput("wrong")
        field(3).performImeAction()
        compose.onNodeWithText("Şifreler uyuşmuyor.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("KAYIT OL").assertIsNotEnabled()
        field(3).performScrollTo().performTextReplacement("test-password")
        field(3).performImeAction()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText("KAYIT OL").assertIsNotEnabled()
        compose.onAllNodes(isToggleable())[0].performScrollTo().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("KAYIT OL").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(Triple("Demo Gezgin", "demo@example.invalid", "test-password"), result) }
    }
    @Test fun loginErrorCanBeDismissedAndNavigationLinksRemainReachableAtLargeText() {
        val state = mutableStateOf<AuthState>(AuthState.Error("Bağlantı kurulamadı."))
        var cleared = 0; var register = 0; var forgot = 0
        show {
            LoginContent(state.value, { _, _ -> fail("Unexpected sign in") }, { cleared++; state.value = AuthState.Idle },
                {}, { register++ }, { forgot++ })
        }
        compose.onNodeWithText("Bağlantı kurulamadı.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Kapat").performScrollTo().performClick()
        compose.onNodeWithText("Bağlantı kurulamadı.").assertDoesNotExist()
        compose.onNodeWithText("Şifremi Unuttum").performScrollTo().performClick()
        compose.onNodeWithText(" Kayıt Ol").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, cleared); assertEquals(1, register); assertEquals(1, forgot) }
    }
    @Test fun welcomeLargeTextKeepsBothActionsAndLegalLabelsReachable() {
        var login = 0; var register = 0
        show { AuthWelcomeScreen({ login++ }, { register++ }) }
        compose.onNodeWithText("Giriş Yap").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Kayıt Ol").performScrollTo().assertIsDisplayed().performClick()
        for (label in listOf("KVKK", "Gizlilik Politikası", "Kullanım Koşulları"))
            compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, login); assertEquals(1, register) }
    }
    @Test fun passwordResetWaitsForConfirmationAndAllowsRetryAfterFailure() {
        val state = mutableStateOf<AuthState>(AuthState.Idle)
        val addresses = mutableListOf<String>()
        var back = 0
        show {
            ForgotPasswordContent(state.value, { addresses += it; state.value = AuthState.Loading },
                { state.value = AuthState.Idle }, { back++ })
        }
        field(0).performScrollTo().performTextInput(" demo@example.invalid ")
        compose.onNodeWithText("BAĞLANTI GÖNDER").performScrollTo().performClick()
        compose.onNodeWithText("E-posta Gönderildi").assertDoesNotExist()
        compose.onNodeWithContentDescription("BAĞLANTI GÖNDER").assertIsNotEnabled()
        compose.onNodeWithText("E-posta").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf("demo@example.invalid"), addresses) }
        compose.runOnIdle { state.value = AuthState.Error("İnternet bağlantısı kurulamadı.") }
        compose.onNodeWithText("E-posta Gönderildi").assertDoesNotExist()
        compose.onNodeWithText("İnternet bağlantısı kurulamadı.").performScrollTo().assertIsDisplayed()
        field(0).assertTextContains(" demo@example.invalid ").assertIsEnabled()
        compose.onNodeWithContentDescription("Kapat").performScrollTo().performClick()
        compose.onNodeWithText("İnternet bağlantısı kurulamadı.").assertDoesNotExist()
        compose.onNodeWithText("BAĞLANTI GÖNDER").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, addresses.size); state.value = AuthState.PasswordResetSent }
        compose.onNodeWithText("E-posta Gönderildi").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("GERİ DÖN").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, back); assertEquals(2, addresses.size) }
    }

    @Test fun passwordResetImeRejectsBlankAndWaitsForServiceWhileKeyboardCloses() {
        val state = mutableStateOf<AuthState>(AuthState.Idle)
        val addresses = mutableListOf<String>()
        show(scale = 1f) {
            ForgotPasswordContent(state.value, { addresses += it; state.value = AuthState.Loading }, {}, {})
        }
        compose.onNodeWithText("BAĞLANTI GÖNDER").assertIsNotEnabled()
        field(0).performTextInput("   ")
        field(0).performImeAction()
        compose.runOnIdle { assertTrue(addresses.isEmpty()) }
        compose.onNodeWithText("BAĞLANTI GÖNDER").assertIsNotEnabled()
        field(0).performScrollTo().performTouchInput { click() }
        compose.waitUntil(8_000) { compose.runOnUiThread {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
        } }
        field(0).performTextReplacement(" demo@example.invalid ")
        field(0).performImeAction()
        compose.runOnIdle { assertEquals(listOf("demo@example.invalid"), addresses) }
        compose.onNodeWithText("E-posta Gönderildi").assertDoesNotExist()
        compose.onNodeWithText("E-posta").assertIsNotEnabled()
        compose.waitUntil(8_000) { compose.runOnUiThread {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        } }
    }
}