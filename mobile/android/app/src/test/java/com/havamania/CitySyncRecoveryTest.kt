package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncRecoveryTest {
    private val ready = CitySyncChange(GeocodingResultDto(1, "Paris", 48.8, 2.3), true)
    @Test fun cooldownUsesElapsedClockDespiteWallJumpThenSendsOnce() = runBlocking {
        var owner = "a"
        var clock = CitySyncClock(1_000, 100, "boot")
        val pending = ready.copy(retryNotBefore = 1_020).withRetryClock(clock, 20)
        clock = clock.copy(wall = 9_000)
        val pauses = mutableListOf<Long>()
        var sends = 0
        runCitySyncRecovery("a", { owner }, { true }, { listOf(CitySyncRecoveryWork(pending)) }, { clock },
            send = { sends++; owner = "b" }, onFailure = { throw it },
            pause = { pauses += it; clock = clock.copy(elapsed = clock.elapsed + it / 1_000) })
        assertEquals(listOf(20_000L), pauses)
        assertEquals(1, sends)
    }
    @Test fun deletionGateBlocksReadsAndAcknowledgedOrBlockedItemsAreNotSent() = runBlocking {
        var owner = "a"; var allowed = false; var reads = 0; var pauses = 0
        runCitySyncRecovery("a", { owner }, { allowed }, read = {
            reads++; listOf(CitySyncRecoveryWork(ready.copy(acknowledged = true)),
                CitySyncRecoveryWork(ready.copy(blockedReason = "UNKNOWN")))
        }, clock = { CitySyncClock(0, 0, "b") }, send = { fail("Protected item sent") }, onFailure = { throw it },
            pause = { pauses++; if (pauses == 1) allowed = true else owner = "b" })
        assertEquals(1, reads); assertEquals(2, pauses)
    }
    @Test fun accountChangeDuringReadCannotPublishOrSend() = runBlocking {
        var owner = "a"
        runCitySyncRecovery("a", { owner }, { true }, read = {
            owner = "b"; listOf(CitySyncRecoveryWork(ready))
        }, clock = { CitySyncClock(0, 0, "b") }, send = { fail("Old owner sent") },
            onFailure = { fail("Old owner error") }, pause = { fail("Old owner kept alive") })
    }
    @Test fun oneFailureDoesNotPreventNextDueItem() = runBlocking {
        var owner = "a"; var failures = 0; val sent = mutableListOf<Boolean>()
        runCitySyncRecovery("a", { owner }, { true },
            { listOf(CitySyncRecoveryWork(ready), CitySyncRecoveryWork(ready, true)) }, { CitySyncClock(0, 0, "b") },
            send = { sent += it.defaultCity; if (!it.defaultCity) throw IOException() else owner = "b" },
            onFailure = { failures++ }, pause = { fail("Unexpected wait") })
        assertEquals(listOf(false, true), sent); assertEquals(1, failures)
    }
    @Test fun cancellationStopsRecoveryWithoutErrorPublication() = runBlocking {
        val entered = CompletableDeferred<Unit>(); var sends = 0
        val job = launch {
            runCitySyncRecovery("a", { "a" }, { true }, { listOf(CitySyncRecoveryWork(ready)) },
                { CitySyncClock(0, 0, "b") }, send = { sends++; entered.complete(Unit); awaitCancellation() },
                onFailure = { fail("Cancellation published") }, pause = { fail("Cancelled loop continued") })
        }
        entered.await(); job.cancelAndJoin(); assertEquals(1, sends)
    }
    @Test fun gateClosingDuringSendPausesRatherThanKillingRecovery() = runBlocking {
        var owner = "a"; var allowed = true; var sends = 0; var pauses = 0
        runCitySyncRecovery("a", { owner }, { allowed }, { listOf(CitySyncRecoveryWork(ready)) },
            { CitySyncClock(0, 0, "b") }, send = {
                sends++; if (sends == 1) { allowed = false; throw CancellationException("Gate closed") } else owner = "b"
            }, onFailure = { throw it }, pause = { pauses++; allowed = true })
        assertEquals(2, sends); assertEquals(1, pauses)
    }
    @Test fun guestAndUnknownQueueNeverStartAutomaticWrites() = runBlocking {
        runCitySyncRecovery("legacy", { "legacy" }, { true }, { fail("Guest read"); emptyList() },
            { CitySyncClock(0, 0, "b") }, { fail("Guest send") }, { fail("Guest error") }, { fail("Guest wait") })
        var errors = 0
        runCitySyncRecovery("a", { "a" }, { true }, { throw IllegalStateException("Corrupt queue") },
            { CitySyncClock(0, 0, "b") }, { fail("Invalid send") }, { errors++ }, { fail("Unsafe read retried") })
        assertEquals(1, errors)
    }
    @Test fun cancelledAttemptDoesNotStopAnOtherwiseActiveAccountWorker() = runBlocking {
        var owner = "a"; var sends = 0; var pauses = 0
        runCitySyncRecovery("a", { owner }, { true }, { listOf(CitySyncRecoveryWork(ready)) },
            { CitySyncClock(0, 0, "b") }, send = {
                sends++
                if (sends == 1) throw CancellationException("Individual attempt superseded")
                owner = "b"
            }, onFailure = { fail("Cancellation shown as a network error") }, pause = { pauses++ })
        assertEquals(2, sends)
        assertEquals(1, pauses)
    }
}
