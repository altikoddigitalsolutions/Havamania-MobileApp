package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Connected real controller and dialog, fake gateway only; no account or remote requests. */
class AssistantConsentInteractionDeviceTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: AssistantConsentController
    private class Gateway : AssistantConsentGateway {
        val loads = AtomicInteger()
        val revokes = AtomicInteger()
        val loadRelease = CompletableDeferred<Unit>()
        val revokeRelease = CompletableDeferred<Unit>()
        override suspend fun load(owner: String): AssistantConsentDocument {
            if (loads.incrementAndGet() > 1) loadRelease.await()
            throw IOException("synthetic connection failure")
        }
        override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument =
            throw AssertionError("No approval expected")
        override suspend fun revoke(owner: String) {
            if (revokes.incrementAndGet() == 1) { revokeRelease.await(); throw IOException("synthetic failure") }
        }
    }

    private fun show(gateway: Gateway) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val current = remember { AssistantConsentController(scope, { "test-owner" }, gateway) }
            controller = current
            val state by current.state.collectAsState()
            DisposableEffect(current) { onDispose { current.close() } }
            LaunchedEffect(current) { current.open() }
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    AssistantConsentDialog(state, current::save, current::revoke, current::retry, current::dismiss)
                } }
            }
        }
        compose.waitUntil(5000) { ::controller.isInitialized && controller.state.value.error != null }
    }

    @Test fun retryUsesTouchAndBusyDialogCanCloseWithoutReopeningAfterFailure() {
        val gateway = Gateway(); show(gateway)
        compose.onNodeWithText("Tekrar dene").performScrollTo().performTouchInput { click() }
        compose.waitUntil(5000) { gateway.loads.get() == 2 }
        compose.onNodeWithText("İzin metni yükleniyor…").assertIsDisplayed()
        compose.onNodeWithText("Kapat").performScrollTo().assertIsEnabled().performTouchInput { click() }
        compose.onNodeWithTag("assistant_consent_dialog").assertDoesNotExist()
        gateway.loadRelease.complete(Unit)
        compose.waitUntil(5000) { !controller.state.value.busy }
        compose.onNodeWithTag("assistant_consent_dialog").assertDoesNotExist()
    }

    @Test fun revokeUsesTouchAndClosingDoesNotUndoPendingWithdrawal() {
        val gateway = Gateway(); show(gateway)
        compose.onNodeWithText("Uzak asistan izinlerini geri çek").performScrollTo().performTouchInput { click() }
        compose.waitUntil(5000) { gateway.revokes.get() == 1 }
        compose.onNodeWithText("İzinlerin geri çekilmesi doğrulanıyor…").assertIsDisplayed()
        compose.onNodeWithText("Kapat").performScrollTo().performTouchInput { click() }
        gateway.revokeRelease.complete(Unit)
        compose.waitUntil(5000) { !controller.state.value.busy }
        compose.runOnIdle { assertTrue(controller.state.value.withdrawalPending); assertFalse(controller.state.value.allowed); controller.open() }
        compose.onNodeWithText("İzni geri çekmeyi tekrar dene").performScrollTo().performTouchInput { click() }
        compose.waitUntil(5000) { gateway.revokes.get() == 2 }
        compose.onNodeWithText("Uzak asistan izinleriniz geri çekildi.").assertIsDisplayed()
    }

    @Test fun cancelInConnectionErrorClosesImmediatelyWithoutAnotherRequest() {
        val gateway = Gateway(); show(gateway)
        compose.onNodeWithText("Vazgeç").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("assistant_consent_dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, gateway.loads.get()); assertEquals(0, gateway.revokes.get()) }
    }

    @Test fun immediateConnectionFailureShowsThatRetryReallyRan() {
        val gateway = Gateway().apply { loadRelease.complete(Unit) }
        show(gateway)
        compose.onNodeWithText("Tekrar dene").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("Deneme 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tekrar dene").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("Deneme 3").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(3, gateway.loads.get()); assertFalse(controller.state.value.busy) }
        compose.onNodeWithText("Vazgeç").performScrollTo().performTouchInput { click() }
        compose.onNodeWithTag("assistant_consent_dialog").assertDoesNotExist()
    }
}
