package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncRequestGateTest {
    @Test fun burstKeepsOneActiveActionAndOneFollowUp() = runBlocking {
        val gate = CitySyncRequestGate()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val work = launch {
            gate.run("city") {
                calls++
                if (calls == 1) { entered.complete(Unit); release.await() }
            }
        }
        entered.await()
        repeat(1000) { gate.run("city") { fail("No extra active action") } }
        assertEquals(1, calls)
        release.complete(Unit); work.join()
        assertEquals(2, calls)
    }

    @Test fun failureDoesNotAutomaticallyRetryAndAllowsExplicitRetry() = runBlocking {
        val gate = CitySyncRequestGate()
        var calls = 0
        try { gate.run("city") { calls++; throw IOException("test") }; fail("Expected failure") }
        catch (_: IOException) { }
        assertEquals(1, calls)
        gate.run("city") { calls++ }
        assertEquals(2, calls)
    }

    @Test fun cancellationReleasesResourceWithoutReplayingDirtyWork() = runBlocking {
        val gate = CitySyncRequestGate()
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        val work = launch { gate.run("city") { calls++; entered.complete(Unit); awaitCancellation() } }
        entered.await(); gate.run("city") { fail("No second active sender") }
        work.cancelAndJoin()
        assertEquals(1, calls)
        gate.run("city") { calls++ }
        assertEquals(2, calls)
    }

    @Test fun anotherResourceCanProceedWhileOneWaits() = runBlocking {
        val gate = CitySyncRequestGate()
        val entered = CompletableDeferred<Unit>()
        val work = launch { gate.run("a") { entered.complete(Unit); awaitCancellation() } }
        entered.await()
        try { withTimeout(2000) { gate.run("b") { } } }
        finally { work.cancelAndJoin() }
    }
}
