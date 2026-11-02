package com.havamania

import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FcmRetryPersistenceFailureTest {
    private enum class Fault { BEFORE, AFTER }
    private class Disk : FcmJournalStorage {
        var bytes: String? = null
        var fault: Fault? = null
        override fun read() = bytes
        override fun write(value: String) {
            if (fault == Fault.BEFORE) throw IOException("local-write-failure")
            bytes = value
            if (fault == Fault.AFTER) throw IOException("local-commit-uncertain")
        }
    }

    @Test fun failedRetryDeadlineWriteCannotSendEarlyInMemoryOrAfterReopen() = runBlocking {
        for (fault in Fault.entries) {
            val disk = Disk()
            val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
            var calls = 0
            try {
                runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
                    calls++
                    disk.fault = fault
                    FcmGatewayResult(FcmGatewayAction.RETRY, 120)
                }
                fail("Persistence error must not be hidden")
            } catch (_: IOException) { }
            disk.fault = null
            for (recovered in listOf(queue, FcmTokenJournal(disk))) {
                assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(recovered,
                    { "a" }, { 1 }, { false }, { 1001 }) { _, _ ->
                    calls++
                    fail("Retry deadline must survive persistence uncertainty")
                    FcmGatewayResult(FcmGatewayAction.APPLIED)
                })
                assertTrue(recovered.snapshot().pending)
            }
            assertEquals(1, calls)
        }
    }

    @Test fun failedPreflightWriteNeverStartsHttp() = runBlocking {
        for (fault in Fault.entries) {
            val disk = Disk()
            val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
            disk.fault = fault
            var calls = 0
            try {
                runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
                    calls++; FcmGatewayResult(FcmGatewayAction.APPLIED)
                }
                fail("Preflight write must fail")
            } catch (_: IOException) { }
            assertEquals(0, calls)
            disk.fault = null
            assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1001 }) { _, _ ->
                calls++; FcmGatewayResult(FcmGatewayAction.APPLIED)
            })
            assertEquals(1, calls)
            assertFalse(queue.snapshot().pending)
        }
    }

    @Test fun failedAcknowledgementCannotImmediatelyDuplicateRegistration() = runBlocking {
        for (fault in Fault.entries) {
            val disk = Disk()
            val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
            var calls = 0
            assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
                calls++; disk.fault = fault; FcmGatewayResult(FcmGatewayAction.APPLIED)
            })
            disk.fault = null
            for (recovered in listOf(queue, FcmTokenJournal(disk))) {
                runFcmSyncAttempt(recovered, { "a" }, { 1 }, { false }, { 1001 }) { _, _ ->
                    calls++; fail("No duplicate after uncertain ack"); FcmGatewayResult(FcmGatewayAction.APPLIED)
                }
            }
            assertEquals(1, calls)
        }
    }

    @Test fun unknownNetworkOutcomeRetainsFenceAndResumesWithSameIdentityAtExpiry() = runBlocking {
        val disk = Disk()
        val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
        val original = queue.snapshot()
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            throw IOException("local-lost-response")
        })
        val reopened = FcmTokenJournal(disk)
        assertEquals(87580L, reopened.snapshot().retryNotBeforeEpochSeconds)
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(reopened, { "a" }, { 1 }, { false }, { 87579 }) { _, _ ->
            fail("Fence still active"); FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(reopened, { "a" }, { 1 }, { false }, { 87580 }) { sent, _ ->
            assertEquals(original.installationId, sent.installationId)
            assertEquals(original.revision, sent.revision)
            assertEquals(original.token, sent.token)
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        assertFalse(reopened.snapshot().pending)
    }

    @Test fun knownTransientResponseReplacesFenceWithShortBackoff() = runBlocking {
        val queue = FcmTokenJournal(Disk()).apply { update("a", "local-test-token") }
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.RETRY, 30)
        })
        assertEquals(1030L, queue.snapshot().retryNotBeforeEpochSeconds)
    }

    @Test fun maximumRetryAfterIncludesTimeSpentWaitingForResponse() = runBlocking {
        val disk = Disk()
        val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
        var now = 1000L
        try {
            runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { now }) { _, _ ->
                now = 1050; disk.fault = Fault.BEFORE
                FcmGatewayResult(FcmGatewayAction.RETRY, 86400)
            }
            fail("Injected disk fault")
        } catch (_: IOException) { }
        disk.fault = null
        now = 87449
        for (recovered in listOf(queue, FcmTokenJournal(disk))) {
            assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(recovered, { "a" }, { 1 }, { false }, { now }) { _, _ ->
                fail("Maximum Retry-After still active"); FcmGatewayResult(FcmGatewayAction.APPLIED)
            })
        }
    }

    @Test fun localTimeoutRetainsFenceWithoutSwallowingParentCancellation() = runBlocking {
        val queue = FcmTokenJournal(Disk()).apply { update("a", "local-test-token") }
        val result = runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            kotlinx.coroutines.withTimeout(1) { kotlinx.coroutines.delay(30) }
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        }
        assertEquals(FcmWorkDecision.RETRY, result)
        assertTrue(queue.snapshot().pending)
        assertEquals(87580L, queue.snapshot().retryNotBeforeEpochSeconds)
    }
}
