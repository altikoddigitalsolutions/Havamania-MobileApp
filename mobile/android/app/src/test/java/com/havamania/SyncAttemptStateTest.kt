package com.havamania

import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.*

class SyncAttemptStateTest {
    @Test fun cancelledWriteStopsBusyIndicatorAndNewSessionCanRetry() = runBlocking {
        val state = SyncAttemptState()
        val ticket = state.ticket()
        val work = launch(start = CoroutineStart.UNDISPATCHED) {
            state.run(ticket) { awaitCancellation() }
        }
        assertTrue(state.running.value)
        work.cancelAndJoin()
        assertFalse(state.running.value)
        state.reset()
        var ran = false
        state.run(state.ticket()) { ran = true }
        assertTrue(ran)
        assertFalse(state.running.value)
    }

    @Test fun oldFailureAndCompletionCannotAffectNewAccountDuringSuspendedWork() = runBlocking {
        val state = SyncAttemptState()
        val old = state.ticket()
        val release = CompletableDeferred<Unit>()
        val work = launch(start = CoroutineStart.UNDISPATCHED) {
            state.run(old) {
                release.await()
                state.fail(old, "Old account failure")
            }
        }
        state.reset()
        val current = state.ticket()
        state.begin(current)
        release.complete(Unit)
        work.join()
        assertNull(state.error.value)
        assertTrue(state.running.value)
        state.finish(current)
        var staleStarted = false
        state.run(old) { staleStarted = true }
        assertFalse(staleStarted)
    }

    @Test fun failureRetainsErrorButAlwaysReleasesBusyState() = runBlocking {
        val state = SyncAttemptState()
        val ticket = state.ticket()
        try {
            state.run(ticket) {
                state.fail(ticket, "Connection interrupted")
                throw java.io.IOException("offline")
            }
            fail("Expected connection error")
        } catch (_: java.io.IOException) { }
        assertFalse(state.running.value)
        assertEquals("Connection interrupted", state.error.value)
        state.run(ticket) { assertNull(state.error.value) }
    }

    @Test fun oldSessionCannotStartFailOrFinishNewSession() {
        val state = SyncAttemptState()
        val old = state.ticket()
        state.begin(old)
        state.reset()
        val current = state.ticket()
        assertTrue(state.begin(current))
        assertFalse(state.begin(old))
        state.fail(old, "old error")
        state.finish(old)
        assertTrue(state.running.value)
        assertNull(state.error.value)
    }
    @Test fun errorSurvivesFinishUntilRetryOrReset() {
        val state = SyncAttemptState()
        val ticket = state.ticket()
        state.begin(ticket)
        state.fail(ticket, "offline")
        state.finish(ticket)
        assertFalse(state.running.value)
        assertEquals("offline", state.error.value)
        state.begin(ticket)
        assertNull(state.error.value)
        state.reset()
        assertFalse(state.running.value)
    }

    @Test fun overlappingWritesKeepIndicatorUntilLastCompletion() {
        val state = SyncAttemptState()
        val ticket = state.ticket()
        state.begin(ticket)
        state.fail(ticket, "first failed")
        state.begin(ticket)
        assertEquals("first failed", state.error.value)
        state.finish(ticket)
        assertTrue(state.running.value)
        state.finish(ticket)
        assertFalse(state.running.value)
        assertEquals("first failed", state.error.value)
    }
    @Test fun resetDropsOldActiveWorkWithoutAffectingNewWork() {
        val state = SyncAttemptState()
        val old = state.ticket()
        state.begin(old)
        state.begin(old)
        state.reset()
        val current = state.ticket()
        state.begin(current)
        state.finish(old)
        state.finish(old)
        assertTrue(state.running.value)
        state.finish(current)
        assertFalse(state.running.value)
    }
}
