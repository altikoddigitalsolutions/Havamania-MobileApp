package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FcmSyncAttemptTest {
    private class Disk : FcmJournalStorage {
        var bytes: String? = null
        override fun read() = bytes
        override fun write(value: String) { bytes = value }
    }
    private fun journal() = FcmTokenJournal(Disk()).apply { update("a", "local-test-token") }

    @Test fun staleOwnerOrDeletionDoesNotRetryEvenBeforeDeadline() = runBlocking {
        for (deleted in listOf(false, true)) {
            val queue = journal()
            queue.deferRetry(queue.snapshot(), 1000, 120)
            val result = runFcmSyncAttempt(queue, { if (deleted) "a" else "b" }, { 1 }, { deleted }, { 1001 }) { _, _ ->
                fail("No network for stale/deleted owner"); FcmGatewayResult(FcmGatewayAction.APPLIED)
            }
            assertEquals(FcmWorkDecision.FINISH, result)
            assertTrue(queue.snapshot().pending)
        }
    }

    @Test fun validOwnerWaitsWithoutAuthOrHttpBeforeDeadline() = runBlocking {
        val queue = journal()
        queue.deferRetry(queue.snapshot(), 1000, 120)
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1119 }) { _, _ ->
            fail("Wait must precede auth/HTTP"); FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
    }

    @Test fun retryDeadlineUsesResponseTimeAndSurvivesRecreation() = runBlocking {
        val disk = Disk()
        val queue = FcmTokenJournal(disk).apply { update("a", "local-test-token") }
        var now = 1000L
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { now }) { _, _ ->
            now = 1050
            FcmGatewayResult(FcmGatewayAction.RETRY, 120)
        })
        assertEquals(1170L, FcmTokenJournal(disk).snapshot().retryNotBeforeEpochSeconds)
    }

    @Test fun oldResponseCannotAcknowledgeOrDeferNewOwner() = runBlocking {
        val queue = journal()
        var uid = "a"
        var epoch = 1L
        val result = runFcmSyncAttempt(queue, { uid }, { epoch }, { false }, { 1000 }) { _, current ->
            uid = "b"; epoch++
            queue.update("b", "local-test-token")
            assertFalse(current())
            FcmGatewayResult(FcmGatewayAction.RETRY, 120)
        }
        assertEquals(FcmWorkDecision.FINISH, result)
        assertEquals("b", queue.snapshot().ownerUid)
        assertEquals(0L, queue.snapshot().retryNotBeforeEpochSeconds)
        assertTrue(queue.snapshot().pending)
    }

    @Test fun permanentFailureFinishesJobWithoutAcknowledgementAndValidReceiptCompletes() = runBlocking {
        val queue = journal()
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.STOP)
        })
        assertTrue(queue.snapshot().pending)
        assertEquals(FcmWorkDecision.FINISH, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 87580 }) { _, _ ->
            FcmGatewayResult(FcmGatewayAction.APPLIED)
        })
        assertFalse(queue.snapshot().pending)
    }

    @Test fun offlineFailureRetriesButCancellationPropagates() = runBlocking {
        val queue = journal()
        assertEquals(FcmWorkDecision.RETRY, runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 1000 }) { _, _ ->
            throw java.io.IOException("local-test")
        })
        try {
            runFcmSyncAttempt(queue, { "a" }, { 1 }, { false }, { 87580 }) { _, _ -> throw CancellationException("cancel") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertTrue(queue.snapshot().pending)
    }
}
