package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountResultTest {
    @Test fun changedOwnerDoesNotReceiveOldResult() = runBlocking {
        var published = false
        publishAccountResult("A", { "B" }) { published = true }
        assertFalse(published)
    }

    @Test fun currentOwnerReceivesResult() = runBlocking {
        var published = false
        publishAccountResult("A", { "A" }) { published = true }
        assertTrue(published)
    }

    @Test fun cancelledSessionCannotPublishEvenAfterSameAccountReturns() = runBlocking {
        var published = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } catch (_: CancellationException) {
                // Simulate an API that catches cancellation and returns a late result.
            }
            publishAccountResult("A", { "A" }) { published = true }
        }
        job.cancelAndJoin()
        assertFalse(published)
    }
}
