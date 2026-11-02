package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySnapshotQueueTest {
    @Test fun slowSaveCannotFinishAfterANewerSave() = runBlocking {
        val queue = CitySnapshotQueue<Int>()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val latest = CompletableDeferred<Unit>()
        val saved = mutableListOf<Int>()
        val consumer = launch {
            queue.consume { value ->
                if (value == 1) { started.complete(Unit); release.await() }
                saved.add(value)
                if (value == 4) latest.complete(Unit)
            }
        }
        try {
            queue.offer(1); started.await()
            queue.offer(2); queue.offer(3); queue.offer(4)
            assertTrue(saved.isEmpty())
            release.complete(Unit)
            withTimeout(2000) { latest.await() }
            assertEquals(listOf(1, 4), saved)
        } finally { consumer.cancelAndJoin(); queue.close() }
    }

    @Test fun burstBeforeConsumerStartsRetainsOnlyNewestSnapshot() = runBlocking {
        val queue = CitySnapshotQueue<Int>()
        repeat(1000) { queue.offer(it) }
        val result = CompletableDeferred<Int>()
        val consumer = launch { queue.consume { result.complete(it) } }
        try { assertEquals(999, withTimeout(2000) { result.await() }) }
        finally { consumer.cancelAndJoin(); queue.close() }
    }

    @Test fun cancelledConsumerCannotApplyQueuedSnapshot() = runBlocking {
        val queue = CitySnapshotQueue<Int>()
        val started = CompletableDeferred<Unit>()
        val saved = mutableListOf<Int>()
        val consumer = launch {
            queue.consume {
                started.complete(Unit); awaitCancellation()
                @Suppress("UNREACHABLE_CODE")
                saved.add(it)
            }
        }
        queue.offer(1); started.await(); queue.offer(2)
        consumer.cancelAndJoin(); queue.close(); queue.offer(3)
        assertTrue(saved.isEmpty())
    }
}
