package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TripMutationGateTest {
    @Test fun deletionWaitsForExistingWriteAndBlocksLateWrite() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        var deleted = false
        val writing = launch {
            TripMutationGate.run {
                entered.complete(Unit)
                release.await()
                events.add("write")
            }
        }
        entered.await()
        val deleting = launch(start = CoroutineStart.UNDISPATCHED) {
            TripMutationGate.run { deleted = true; events.add("delete") }
        }
        assertTrue(events.isEmpty())
        release.complete(Unit)
        joinAll(writing, deleting)
        TripMutationGate.run { if (!deleted) events.add("late-write") }
        assertEquals(listOf("write", "delete"), events)
    }
    @Test fun failureReleasesGate() = runBlocking {
        try { TripMutationGate.run { error("failed") } } catch (_: IllegalStateException) { }
        assertEquals("ready", TripMutationGate.run { "ready" })
    }
}
