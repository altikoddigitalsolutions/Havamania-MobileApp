package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AssistantHealthPrivacyTest {
    private val doc = AssistantConsentDocument(2, "Test", "a".repeat(64),
        health = AssistantHealthConsent(2, "Health test", "b".repeat(64)), accepted = true, health_accepted = true)
    private class Store : AssistantWithdrawalStore {
        val health = mutableMapOf<String, Pair<Long,Boolean>>()
        val full = mutableMapOf<String,Boolean>()
        override suspend fun pending(owner: String) = full[owner] == true
        override suspend fun setPending(owner: String, value: Boolean) { full[owner] = value }
        override suspend fun healthBlocked(owner: String) = health[owner]?.second == true
        override suspend fun beginHealthUpdate(owner: String, allow: Boolean): Long {
            val old = health[owner] ?: (0L to false)
            val revision = old.first + 1
            health[owner] = revision to (old.second || !allow)
            return revision
        }
        override suspend fun confirmHealthUpdate(owner: String, revision: Long, allow: Boolean): Boolean {
            if (health[owner]?.first != revision) return false
            health[owner] = revision to !allow
            return true
        }
    }
    private open class Server(var doc: AssistantConsentDocument) : AssistantConsentGateway {
        var failure = false
        override suspend fun load(owner: String) = doc
        override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
            if (failure) throw java.io.IOException("local offline")
            doc = doc.copy(health_accepted = health)
            return doc
        }
        override suspend fun revoke(owner: String) { doc = doc.copy(accepted = false, health_accepted = false) }
    }
    @Test fun failedHealthOptOutBlocksOnlyHealthAndSurvivesNewGateway() = runBlocking {
        val store = Store()
        val server = Server(doc).apply { failure = true }
        val gateway = WithdrawalTrackingConsentGateway(server, store)
        try { gateway.save("A", doc, false); fail("offline") } catch (_: java.io.IOException) { }
        assertTrue(store.healthBlocked("A"))
        assertFalse(store.pending("A"))
        val reopened = WithdrawalTrackingConsentGateway(server, store)
        val restored = reopened.load("A")
        assertTrue(restored.accepted)
        assertFalse(restored.health_accepted)
        assertTrue(restored.health_sync_required)
        assertTrue(restored.health_review_required)
        assertTrue(reopened.load("B").health_accepted)
        server.failure = false
        val confirmed = reopened.save("A", restored, false)
        assertFalse(confirmed.health_accepted)
        assertFalse(confirmed.health_sync_required)
        assertTrue(store.healthBlocked("A"))
    }
    @Test fun optInNeverUnblocksUntilServerConfirmsCurrentDecision() = runBlocking {
        val store = Store()
        store.beginHealthUpdate("A", false)
        val server = Server(doc).apply { failure = true }
        val gateway = WithdrawalTrackingConsentGateway(server, store)
        try { gateway.save("A", doc, true); fail("offline") } catch (_: java.io.IOException) { }
        assertTrue(store.healthBlocked("A"))
        server.failure = false
        assertTrue(gateway.save("A", doc, true).health_accepted)
        assertFalse(store.healthBlocked("A"))
    }
    @Test fun lateOldOptInResponseCannotUndoNewOptOut() = runBlocking {
        val store = Store()
        store.beginHealthUpdate("A", false)
        val waiting = CompletableDeferred<Unit>()
        val late = CompletableDeferred<AssistantConsentDocument>()
        val server = object : Server(doc) {
            override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
                if (health) { waiting.complete(Unit); return late.await() }
                return document.copy(health_accepted = false)
            }
        }
        val gateway = WithdrawalTrackingConsentGateway(server, store)
        val old = async { gateway.save("A", doc, true) }
        waiting.await()
        gateway.save("A", doc, false)
        late.complete(doc)
        assertFalse(old.await().health_accepted)
        assertTrue(store.healthBlocked("A"))
    }
    @Test fun mismatchedConfirmationCannotUnblockAndFullRevokeInvalidatesHealthApproval() = runBlocking {
        val store = Store()
        store.beginHealthUpdate("A", false)
        val server = object : Server(doc) {
            override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean) = document.copy(health_accepted = false)
        }
        val gateway = WithdrawalTrackingConsentGateway(server, store)
        try { gateway.save("A", doc, true); fail("must reject mismatched response") }
        catch (_: AssistantConsentServiceException) { }
        assertTrue(store.healthBlocked("A"))
        val oldRevision = store.beginHealthUpdate("A", true)
        gateway.revoke("A")
        assertFalse(store.confirmHealthUpdate("A", oldRevision, true))
        assertTrue(store.healthBlocked("A"))
    }
    @Test fun firstSendAnd401RetryStripHealthWithoutChangingQuestionOrRequestIdentity() {
        val original = BackendChatbotAskRequest("Question", request_id = "stable-request", health_consent = true,
            health_consent_version = 2, personalization_enabled = true)
        val retry = applyAssistantHealthPrivacy(original, true)
        assertFalse(retry.health_consent)
        assertNull(retry.health_consent_version)
        assertEquals(original.question, retry.question)
        assertEquals(original.request_id, retry.request_id)
        assertTrue(retry.personalization_enabled)
        assertFalse(applyAssistantHealthPrivacy(retry, false).health_consent)
    }
}
