package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CurrentLocationRecoveryTest {
    @Test fun earlyEmptyResultGetsOneRecovery() = runBlocking {
        var calls = 0; var waited = 0; var retries = 0; var now = 0L
        val result = recoverEarlyEmptyPosition({ calls++; now += 100; if (calls == 1) null else "position" },
            { true }, { now }, { waited++; now += 1500 }, { retries++ })
        assertEquals("position", result); assertEquals(2, calls); assertEquals(1, waited); assertEquals(1, retries)
    }
    @Test fun slowEmptyResultIsNotRetried() = runBlocking {
        var calls = 0; var now = 0L
        assertNull(recoverEarlyEmptyPosition<String>({ calls++; now = 15_000; null }, { true }, { now },
            { error("No delay after a slow result") }))
        assertEquals(1, calls)
    }
    @Test fun permissionOrProviderFailurePreventsRecovery() = runBlocking {
        var calls = 0
        assertNull(recoverEarlyEmptyPosition<String>({ calls++; null }, { false }, { 0L }, { error("No retry") }))
        assertEquals(1, calls)
    }
    @Test fun losingPermissionOrGpsDuringDelayPreventsSecondRequest() = runBlocking {
        var allowed = true; var calls = 0
        assertNull(recoverEarlyEmptyPosition<String>({ calls++; null }, { allowed }, { 0L }, { allowed = false }))
        assertEquals(1, calls)
    }
    @Test fun cancellationDuringRecoveryDelayStopsFurtherRequests() = runBlocking {
        var calls = 0
        val waiting = CompletableDeferred<Unit>()
        val job = launch {
            recoverEarlyEmptyPosition<String>({ calls++; null }, { true }, { 0L },
                { waiting.complete(Unit); awaitCancellation() })
        }
        waiting.await(); job.cancelAndJoin()
        assertEquals(1, calls)
    }
    @Test fun successAndRepeatedEmptyResultsNeverPollContinuously() = runBlocking {
        var calls = 0
        assertEquals("position", recoverEarlyEmptyPosition({ calls++; "position" }, { true }, { 0L },
            { error("No delay after success") }))
        assertEquals(1, calls)
        calls = 0
        assertNull(recoverEarlyEmptyPosition<String>({ calls++; null }, { true }, { 0L }, {}))
        assertEquals(2, calls)
    }
    @Test fun lateSecondResultAfterCancellationIsNotDelivered() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        lateinit var late: kotlin.coroutines.Continuation<String?>
        var calls = 0; var delivered = false
        val job = launch {
            recoverEarlyEmptyPosition({
                if (++calls == 1) null else kotlin.coroutines.suspendCoroutine { continuation ->
                    late = continuation; entered.complete(Unit)
                }
            }, { true }, { 0L }, {})
            delivered = true
        }
        entered.await(); job.cancel(); late.resumeWith(Result.success("late position")); job.join()
        assertFalse(delivered)
    }
}
