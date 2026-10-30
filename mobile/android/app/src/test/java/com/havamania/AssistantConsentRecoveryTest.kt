package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AssistantConsentRecoveryTest {
    private fun document() = AssistantConsentDocument(2, "Local text", "a".repeat(64), accepted = true)
    private open class Gateway : AssistantConsentGateway {
        var loads = 0
        var revokes = 0
        var saves = 0
        var failure: Exception? = null
        var doc = AssistantConsentDocument(2, "Local text", "a".repeat(64), accepted = true)
        override suspend fun load(owner: String): AssistantConsentDocument { loads++; return doc }
        override suspend fun save(owner: String, document: AssistantConsentDocument, health: Boolean): AssistantConsentDocument {
            saves++; return doc
        }
        override suspend fun revoke(owner: String) {
            revokes++
            failure?.let { throw it }
            doc = doc.copy(accepted = false, health_accepted = false)
        }
    }
    private class Store : AssistantWithdrawalStore {
        val values = mutableMapOf<String, Boolean>()
        val health = mutableMapOf<String, Pair<Long, Boolean>>()
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
        override suspend fun pending(owner: String) = values[owner] == true
        override suspend fun setPending(owner: String, value: Boolean) { values[owner] = value }
    }

    @Test fun retryAfterWithdrawalFailureRetriesRevokeAndNeverReloadsConsent() = runBlocking {
        val gateway = Gateway()
        val controller = AssistantConsentController(this, { "A" }, gateway)
        try {
            controller.load(); yield()
            gateway.failure = consentHttpFailure(503)
            controller.revoke(); yield()
            assertFalse(controller.state.value.allowed)
            assertTrue(controller.state.value.withdrawalPending)
            assertEquals(ConsentRetry.REVOKE, controller.state.value.retry)
            assertNull(controller.state.value.notice)
            controller.dismiss()
            assertFalse(controller.state.value.allowed)
            controller.open(); yield()
            assertTrue(controller.state.value.visible)
            assertEquals(1, gateway.revokes)
            assertEquals(1, gateway.loads)
            controller.save(false); yield()
            assertEquals(0, gateway.saves)
            gateway.failure = null
            controller.retry(); controller.retry(); yield()
            assertEquals(2, gateway.revokes)
            assertEquals(1, gateway.loads)
            assertFalse(controller.state.value.allowed)
            assertFalse(controller.state.value.withdrawalPending)
            assertNotNull(controller.state.value.notice)
            assertTrue(controller.state.value.visible)
        } finally { controller.close() }
    }

    @Test fun persistedWithdrawalBlocksFreshControllerAndOtherAccountRemainsSeparate() = runBlocking {
        val disk = Store()
        val server = Gateway().apply { failure = java.io.IOException("private detail") }
        try { WithdrawalTrackingConsentGateway(server, disk).revoke("A"); fail("must fail") } catch (_: java.io.IOException) { }
        assertTrue(disk.pending("A"))
        val reopened = WithdrawalTrackingConsentGateway(server, disk)
        val controller = AssistantConsentController(this, { "A" }, reopened)
        try {
            controller.load(); yield()
            assertFalse(controller.state.value.allowed)
            assertEquals(ConsentRetry.REVOKE, controller.state.value.retry)
            assertEquals(0, server.loads)
            assertTrue(reopened.load("B").accepted)
            try { reopened.save("A", document(), false); fail("pending privacy intent must block approval") }
            catch (_: AssistantWithdrawalPendingException) { }
            server.failure = null
            controller.retry(); yield()
            assertFalse(disk.pending("A"))
            assertFalse(controller.state.value.allowed)
        } finally { controller.close() }
    }

    @Test fun privacyIntentMustBeWrittenBeforeNetworkAndFailedWritePreventsCall() = runBlocking {
        val server = Gateway()
        val store = object : AssistantWithdrawalStore {
            override suspend fun healthBlocked(owner: String) = false
            override suspend fun beginHealthUpdate(owner: String, allow: Boolean): Long = error("must not reach")
            override suspend fun confirmHealthUpdate(owner: String, revision: Long, allow: Boolean) = false
            override suspend fun pending(owner: String) = false
            override suspend fun setPending(owner: String, value: Boolean) { error("disk failure") }
        }
        try { WithdrawalTrackingConsentGateway(server, store).revoke("A"); fail("must fail closed") }
        catch (_: IllegalStateException) { }
        assertEquals(0, server.revokes)
        val disk = Store()
        val checkedServer = object : Gateway() {
            override suspend fun revoke(owner: String) { assertTrue(disk.pending(owner)); super.revoke(owner) }
        }
        WithdrawalTrackingConsentGateway(checkedServer, disk).revoke("A")
        assertFalse(disk.pending("A"))
    }

    @Test fun repeatedLoadFailureClearsBusyAndRevokeTimeoutNeverLooksSuccessful() = runBlocking {
        val server = object : Gateway() {
            override suspend fun load(owner: String): AssistantConsentDocument { loads++; throw consentHttpFailure(404) }
            override suspend fun revoke(owner: String) { revokes++; awaitCancellation() }
        }
        val controller = AssistantConsentController(this, { "A" }, server, timeoutMillis = 5)
        try {
            controller.open(); yield(); controller.retry(); yield()
            assertEquals(2, server.loads)
            assertFalse(controller.state.value.busy)
            assertFalse(controller.state.value.error!!.contains("İnternet"))
            controller.revoke()
            withTimeout(1000) { while (controller.state.value.busy) delay(1) }
            assertEquals(ConsentRetry.REVOKE, controller.state.value.retry)
            assertFalse(controller.state.value.allowed)
            assertNull(controller.state.value.notice)
        } finally { controller.close() }
    }

    @Test fun httpFailuresAreSafeAndDoNotBlameConnectionForMissingService() {
        for (code in listOf(401,403,404,405,429,500,502,503,504)) {
            assertTrue(consentFailureMessage(consentHttpFailure(code)).isNotBlank())
            assertFalse(consentFailureMessage(consentHttpFailure(code)).contains("PRIVATE"))
        }
        assertEquals(ConsentFailure.AUTH, consentHttpFailure(401).failure)
        assertEquals(ConsentFailure.FORBIDDEN, consentHttpFailure(403).failure)
        assertEquals(ConsentFailure.UNAVAILABLE, consentHttpFailure(404).failure)
        assertEquals(ConsentFailure.RATE_LIMIT, consentHttpFailure(429).failure)
        assertFalse(consentFailureMessage(consentHttpFailure(503)).contains("İnternet"))
        assertFalse(consentFailureMessage(java.io.IOException("PRIVATE_TOKEN")).contains("PRIVATE_TOKEN"))
    }
}
