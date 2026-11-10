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

    @Test fun openingDuringBackgroundLoadKeepsAcceptedPermissionsVisible() = runBlocking {
        val accepted = doc.copy(accepted = true)
        val result = CompletableDeferred<AssistantConsentDocument>()
        var loads = 0
        val controller = AssistantConsentController(this, { "a" }, object : Gateway() {
            override suspend fun load(owner: String): AssistantConsentDocument {
                loads++
                return result.await()
            }
        })
        try {
            controller.load()
            yield()
            controller.load()
            assertFalse(controller.state.value.visible)
            controller.open()
            assertTrue(controller.state.value.visible)
            result.complete(accepted)
            yield()
            assertEquals(1, loads)
            assertEquals(accepted, controller.state.value.document)
            assertTrue(controller.state.value.visible)
        } finally { controller.close() }
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

    @Test fun accountChangeBeforeListenerResetDiscardsOldConsentResponse() = runBlocking {
        var owner = "A"
        val result = CompletableDeferred<AssistantConsentDocument>()
        val controller = AssistantConsentController(this, { owner }, object : Gateway() {
            override suspend fun load(owner: String) = if (owner == "A") result.await() else doc
        })
        try {
            controller.open()
            yield()
            owner = "B"
            result.complete(doc.copy(accepted = true))
            yield()
            assertNull(controller.state.value.document)
            assertFalse(controller.state.value.allowed)

            controller.reset()
            controller.open()
            yield()
            assertEquals(doc, controller.state.value.document)
            assertFalse(controller.state.value.allowed)
        } finally { controller.close() }
    }

    @Test fun actionsFromPreviousAccountCannotMutateTheNewAccount() = runBlocking {
        var owner = "A"
        val mutations = mutableListOf<String>()
        val controller = AssistantConsentController(this, { owner }, object : Gateway() {
            override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
                mutations.add(owner)
                return doc
            }
            override suspend fun revoke(owner: String) {
                mutations.add(owner)
                throw IOException("offline")
            }
        })
        try {
            controller.open(); yield()
            owner = "B"
            controller.save(false); yield()
            assertNull(controller.state.value.document)
            assertFalse(controller.state.value.visible)

            owner = "A"
            controller.open(); yield()
            owner = "B"
            controller.revoke(); yield()
            assertTrue(mutations.isEmpty())

            owner = "A"
            controller.open(); yield()
            controller.revoke(); yield()
            assertEquals(ConsentRetry.REVOKE, controller.state.value.retry)
            owner = "B"
            controller.retry(); yield()
            assertEquals(listOf("A"), mutations)
            assertFalse(controller.state.value.withdrawalPending)
            controller.open(); yield()
            assertEquals(doc, controller.state.value.document)
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
