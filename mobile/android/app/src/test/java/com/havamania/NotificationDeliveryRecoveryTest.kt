package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class NotificationDeliveryRecoveryTest {
    private val item = NotificationItem(id = "event", userId = "a")

    @Test fun cancellationIsPreservedWhenCleanupAlsoFailsAndCleanupRemainsRetryable() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        var failing = true
        var deletes = 0
        val error = runCatching {
            deliverLocalNotification(item, { 1L }, { _, _ -> deletes++; if (failing) throw IOException("delete") },
                recovery = recovery) { throw CancellationException("cancelled post") }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertTrue(error!!.suppressed.single() is IOException)
        failing = false; recovery.retryForOwner("a")
        assertEquals(2, deletes)
    }

    @Test fun fcmPostErrorIsPreservedWhenCleanupAlsoFails() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Synthetic", "Local", 2000)
        val error = runCatching {
            deliverFcmRecord("a", event, { true }, { 1L }, { _, _ -> throw IOException("delete") },
                recovery = recovery) { throw SecurityException("post") }
        }.exceptionOrNull()
        assertTrue(error is SecurityException)
        assertTrue(error!!.suppressed.single() is IOException)
    }

    @Test fun cancelledFcmAfterInsertPreservesCancellationDespiteCleanupFailure() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Synthetic", "Local", 2000)
        val seen = CompletableDeferred<Throwable>()
        val job = launch {
            try {
                persistFcmRecord("a", event, { true }, { currentCoroutineContext().cancel(); 1L },
                    { _, _ -> throw IOException("delete") }, recovery)
            } catch (error: Throwable) { seen.complete(error) }
        }
        job.join()
        val error = seen.await()
        assertTrue(job.isCancelled); assertTrue(error is CancellationException)
        assertTrue(error.suppressed.single() is IOException)
    }

    @Test fun failedAckRetriesOnlyAckAndDoesNotPostAgain() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        var reserved = false
        var acknowledged = false
        var attempts = 0
        var posts = 0
        val reserve: suspend (NotificationItem) -> Long = {
            if (reserved) { if (acknowledged) 0L else -1L } else { reserved = true; 1L }
        }
        val complete: suspend (NotificationItem) -> Unit = {
            if (++attempts == 1) throw IOException("ack failed")
            acknowledged = true
        }
        assertTrue(runCatching { deliverLocalNotification(item, reserve, { _, _ -> fail("Keep posted row") },
            complete, recovery) { posts++; true } }.isFailure)
        assertEquals(LocalNotificationDelivery.ALREADY_RECORDED,
            deliverLocalNotification(item, reserve, { _, _ -> fail("No cleanup") }, complete, recovery) { posts++; true })
        assertEquals(1, posts); assertEquals(2, attempts); assertTrue(acknowledged)
    }

    @Test fun failedCleanupIsRetriedBeforeAnotherReservation() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        var reserved = false
        var deletes = 0
        var claims = 0
        val reserve: suspend (NotificationItem) -> Long = {
            claims++; if (reserved) -1L else { reserved = true; 1L }
        }
        val delete: suspend (String, List<String>) -> Unit = { _, _ ->
            if (++deletes == 1) throw IOException("cleanup failed")
            reserved = false
        }
        assertTrue(runCatching { deliverLocalNotification(item, reserve, delete, recovery = recovery) { false } }.isFailure)
        assertEquals(LocalNotificationDelivery.POSTED,
            deliverLocalNotification(item, reserve, delete, recovery = recovery) { true })
        assertEquals(2, claims); assertEquals(2, deletes)
    }

    @Test fun failedRetryAndAccountDiscardPreserveOtherOwnersWork() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        var a = 0
        var b = 0
        var failing = true
        runCatching { recovery.runOrRemember("a", "same") { a++; if (failing) throw IOException() } }
        runCatching { recovery.runOrRemember("b", "same") { b++; if (failing) throw IOException() } }
        assertTrue(runCatching { recovery.retryForOwner("a") }.isFailure)
        assertEquals(2, a); assertEquals(1, b)
        recovery.discardOwner("a"); failing = false
        recovery.retryForOwner("a"); recovery.retryForOwner("b"); recovery.retryForOwner("b")
        assertEquals(2, a); assertEquals(2, b)
    }

    @Test fun concurrentRetryDoesNotExecuteSuccessfulOperationTwice() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        runCatching { recovery.runOrRemember("a", "event") {
            if (++calls == 1) throw IOException()
            entered.complete(Unit); release.await()
        } }
        val first = launch { recovery.retryForOwner("a") }
        entered.await()
        val second = launch(start = CoroutineStart.UNDISPATCHED) { recovery.retryForOwner("a") }
        assertFalse(second.isCompleted); assertEquals(2, calls)
        release.complete(Unit); first.join(); second.join()
        assertEquals(2, calls)
    }

    @Test fun cancellationDuringRetryRetainsTheOperationAndReleasesGate() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        val entered = CompletableDeferred<Unit>()
        var calls = 0
        runCatching { recovery.runOrRemember("a", "event") {
            when (++calls) {
                1 -> throw IOException()
                2 -> { entered.complete(Unit); CompletableDeferred<Unit>().await() }
            }
        } }
        val job = launch { recovery.retryForOwner("a") }
        entered.await(); job.cancelAndJoin()
        withTimeout(2000) { recovery.retryForOwner("a") }
        assertEquals(3, calls)
    }

    @Test fun fcmRetryRepairsAckWithoutRepostingEvent() = runBlocking {
        val recovery = NotificationDeliveryRecovery()
        var inserted = false
        var acks = 0
        var posts = 0
        val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Synthetic", "Local", 2000)
        suspend fun attempt() = deliverFcmRecord("a", event, { true },
            { if (inserted) -1L else { inserted = true; 1L } }, { _, _ -> fail("No delete") },
            complete = { if (++acks == 1) throw IOException() }, recovery = recovery) { posts++; true }
        assertTrue(runCatching { attempt() }.isFailure)
        assertFalse(attempt())
        assertEquals(1, posts); assertEquals(2, acks)
    }
}
