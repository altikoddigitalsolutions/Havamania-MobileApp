package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HistoryDeletionTest {
    @Test fun localCopyRemainsUntilServerAcknowledges() = runBlocking {
        val ack = CompletableDeferred<Unit>()
        var removed = false
        val job = launch { deleteHistoryWithAcknowledgement({ ack.await() }, { removed = true }) }
        yield()
        assertFalse(removed)
        ack.complete(Unit)
        job.join()
        assertTrue(removed)
    }

    @Test fun remoteFailureKeepsLocalCopyAndRetryCanComplete() = runBlocking {
        var removed = false
        try {
            deleteHistoryWithAcknowledgement({ error("offline") }, { removed = true })
            fail("Expected failure")
        } catch (_: IllegalStateException) { }
        assertFalse(removed)
        deleteHistoryWithAcknowledgement({}, { removed = true })
        assertTrue(removed)
    }

    @Test fun cancellationAfterNonCooperativeRemoteCallDoesNotDeleteLocalCopy() = runBlocking {
        val ack = CompletableDeferred<Unit>()
        var removed = false
        val job = launch {
            deleteHistoryWithAcknowledgement({ withContext(NonCancellable) { ack.await() } }, { removed = true })
        }
        yield()
        job.cancel()
        ack.complete(Unit)
        job.join()
        assertFalse(removed)
    }
}
