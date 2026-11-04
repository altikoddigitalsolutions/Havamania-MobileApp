package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AssistantConsentDismissTest {
    private val doc = AssistantConsentDocument(1, "Local", "a".repeat(64))
    private open inner class Gateway : AssistantConsentGateway {
        override suspend fun load(owner: String) = doc
        override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean) = doc
        override suspend fun revoke(owner: String) = Unit
    }

    @Test fun immediateRepeatedFailuresAreDistinctAndAttemptResetsForOtherOperationAndAccount() = runBlocking {
        val controller = AssistantConsentController(this, { "a" }, object : Gateway() {
            override suspend fun load(owner: String): AssistantConsentDocument = throw IOException("offline")
            override suspend fun revoke(owner: String) { throw IOException("offline") }
        })
        try {
            controller.open(); yield(); assertEquals(1, controller.state.value.attempt)
            val initial = controller.state.value
            controller.retry(); yield()
            assertEquals(2, controller.state.value.attempt)
            assertNotEquals(initial, controller.state.value)
            controller.revoke(); yield(); assertEquals(1, controller.state.value.attempt)
            controller.retry(); yield(); assertEquals(2, controller.state.value.attempt)
            assertEquals(ConsentRetry.REVOKE, controller.state.value.retry)
            assertFalse(controller.state.value.allowed)
            controller.reset(); assertEquals(0, controller.state.value.attempt)
        } finally { controller.close() }
    }

    @Test fun closingLoadingDialogStaysClosedAfterLateFailureAndReopenIsSingleFlight() = runBlocking {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val controller = AssistantConsentController(this, { "a" }, object : Gateway() {
            override suspend fun load(owner: String): AssistantConsentDocument {
                loads++; release.await(); throw IOException("offline")
            }
        })
        try {
            controller.open(); yield()
            controller.dismiss(); assertFalse(controller.state.value.visible)
            controller.open(); yield(); assertEquals(1, loads)
            assertTrue(controller.state.value.visible); assertTrue(controller.state.value.busy)
            controller.dismiss(); release.complete(Unit); yield()
            assertFalse(controller.state.value.visible); assertFalse(controller.state.value.busy)
            assertNotNull(controller.state.value.error)
            controller.retry(); yield(); assertEquals(2, loads); assertTrue(controller.state.value.visible)
        } finally { release.complete(Unit); controller.close() }
    }

    @Test fun closingWithdrawalDoesNotCancelPrivacyIntentOrReopenOnFailure() = runBlocking {
        val release = CompletableDeferred<Unit>()
        var revokes = 0
        val controller = AssistantConsentController(this, { "a" }, object : Gateway() {
            override suspend fun revoke(owner: String) {
                if (++revokes == 1) { release.await(); throw IOException("offline") }
            }
        })
        try {
            controller.open(); yield(); controller.revoke(); yield()
            controller.dismiss(); assertFalse(controller.state.value.visible)
            assertTrue(controller.state.value.withdrawalPending); assertFalse(controller.state.value.allowed)
            release.complete(Unit); yield()
            assertFalse(controller.state.value.visible); assertTrue(controller.state.value.withdrawalPending)
            controller.open(); assertTrue(controller.state.value.visible)
            controller.retry(); yield()
            assertEquals(2, revokes); assertNotNull(controller.state.value.notice)
            assertFalse(controller.state.value.allowed)
        } finally { release.complete(Unit); controller.close() }
    }

    @Test fun staleSaveResponseCannotReopenDismissedDialogOrGrantConsent() = runBlocking {
        val release = CompletableDeferred<Unit>()
        var loads = 0
        val controller = AssistantConsentController(this, { "a" }, object : Gateway() {
            override suspend fun load(owner: String) = doc.copy(version = ++loads)
            override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
                release.await(); throw AssistantConsentStaleException()
            }
        })
        try {
            controller.open(); yield(); controller.save(false); yield()
            controller.dismiss(); release.complete(Unit); yield()
            assertFalse(controller.state.value.visible); assertFalse(controller.state.value.allowed)
            assertNotNull(controller.state.value.error); assertEquals(2, controller.state.value.document?.version)
        } finally { release.complete(Unit); controller.close() }
    }
}
