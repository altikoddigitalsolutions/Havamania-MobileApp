package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LocalNotificationDeliveryTest {
    private val item = NotificationItem(id = "test", userId = "a", deduplicationKey = "event")

    @Test fun acknowledgementFollowsSuccessfulPostAndFailureKeepsJournalForRecovery() = runBlocking {
        val trace = mutableListOf<String>()
        assertEquals(LocalNotificationDelivery.POSTED, deliverLocalNotification(item, { 1L },
            { _, _ -> fail("No cleanup after post") }, complete = { trace.add("ack") }) {
            trace.add("post"); true
        })
        assertEquals(listOf("post", "ack"), trace)
        assertTrue(runCatching {
            deliverLocalNotification(item, { 1L }, { _, _ -> fail("Keep uncertain delivery for recovery") },
                complete = { throw java.io.IOException("ack failed") }) { true }
        }.isFailure)
    }

    @Test fun acknowledgedPostIsNotLostToCancellationAtCompletion() = runBlocking {
        var acknowledged = false
        val job = launch {
            deliverLocalNotification(item, { 1L }, { _, _ -> fail("Already posted") },
                complete = { currentCoroutineContext().ensureActive(); yield(); acknowledged = true }) {
                currentCoroutineContext().cancel(); true
            }
        }
        job.join(); assertTrue(job.isCancelled); assertTrue(acknowledged)
    }

    @Test fun blockedAndExistingReservationsNeverPostOrDelete() = runBlocking {
        for ((reservation, expected) in listOf(-1L to LocalNotificationDelivery.SKIPPED,
            0L to LocalNotificationDelivery.ALREADY_RECORDED)) {
            assertEquals(expected, deliverLocalNotification(item, { reservation },
                { _, _ -> fail("No owned reservation") }) { fail("No post"); true })
        }
    }

    @Test fun falseOrFailedPostCleansOnlyOwnedRecordAndSuccessKeepsIt() = runBlocking {
        var deleted = 0
        val cleanup: suspend (String, List<String>) -> Unit = { uid, ids ->
            assertEquals("a", uid); assertEquals(listOf("test"), ids); deleted++
        }
        assertEquals(LocalNotificationDelivery.SKIPPED, deliverLocalNotification(item, { 1L }, cleanup) { false })
        val result = runCatching { deliverLocalNotification(item, { 1L }, cleanup) { throw SecurityException() } }
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(2, deleted)
        assertEquals(LocalNotificationDelivery.POSTED, deliverLocalNotification(item, { 1L }, cleanup) { true })
        assertEquals(2, deleted)
    }

    @Test fun cancelledPreparationCleansBeforeNextAttempt() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var deleted = false
        val job = launch {
            deliverLocalNotification(item, { 1L }, { _, _ -> yield(); deleted = true }) {
                entered.complete(Unit); CompletableDeferred<Boolean>().await()
            }
        }
        entered.await(); job.cancelAndJoin()
        assertTrue(deleted)
        assertEquals(LocalNotificationDelivery.POSTED, withTimeout(2000) {
            deliverLocalNotification(item, { 1L }, { _, _ -> }) { true }
        })
    }

    @Test fun overlappingAttemptWaitsForFailedAttemptCleanupBeforeReserving() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var reserved = false
        var claims = 0
        val reserve: suspend (NotificationItem) -> Long = { claims++; if (reserved) 0L else { reserved = true; 1L } }
        val cleanup: suspend (String, List<String>) -> Unit = { _, _ -> reserved = false }
        val first = async { deliverLocalNotification(item, reserve, cleanup) {
            entered.complete(Unit); release.await(); false
        } }
        entered.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) { deliverLocalNotification(item, reserve, cleanup) { true } }
        assertFalse(second.isCompleted); assertEquals(1, claims)
        release.complete(Unit)
        assertEquals(LocalNotificationDelivery.SKIPPED, first.await())
        assertEquals(LocalNotificationDelivery.POSTED, second.await())
        assertEquals(2, claims)
    }
}
