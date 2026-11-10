package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class FcmPersistenceTest {
    private val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Test", "Local only", 2000)

    @Test fun successfulFcmPostAcknowledgesBeforeReturningAndFailedAckRetainsRecord() = runBlocking {
        val trace = mutableListOf<String>()
        assertTrue(deliverFcmRecord("a", event, { true }, { 1L }, { _, _ -> fail("No cleanup") },
            complete = { trace.add("ack") }) { trace.add("post"); true })
        assertEquals(listOf("post", "ack"), trace)
        assertTrue(runCatching {
            deliverFcmRecord("a", event, { true }, { 1L }, { _, _ -> fail("Do not erase uncertain delivery") },
                complete = { throw java.io.IOException("ack failed") }) { true }
        }.isFailure)
    }

    @Test fun skippedPostCanRetryButSuccessfulPostCannotRepeat() = runBlocking {
        val records = mutableSetOf<String>()
        var posts = 0
        val insert: suspend (NotificationItem) -> Long = { if (records.add(it.id)) 1L else -1L }
        val delete: suspend (String, List<String>) -> Unit = { _, ids -> records.removeAll(ids.toSet()); Unit }
        assertFalse(deliverFcmRecord("a", event, { true }, insert, delete) { false })
        assertTrue(records.isEmpty())
        assertTrue(deliverFcmRecord("a", event, { true }, insert, delete) { posts++; true })
        assertFalse(deliverFcmRecord("a", event, { true }, insert, delete) { posts++; true })
        assertEquals(1, posts); assertEquals(1, records.size)
    }

    @Test fun failedPostRemovesOnlyItsOwnedRecord() = runBlocking {
        val records = mutableSetOf("other-account-record")
        val result = runCatching {
            deliverFcmRecord("a", event, { true }, { records.add(it.id); 1L }, { uid, ids ->
                assertEquals("a", uid); records.removeAll(ids.toSet()); Unit
            }) { throw SecurityException("permission changed") }
        }
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(setOf("other-account-record"), records)
    }

    @Test fun cancellationAfterInsertStillCleansUp() = runBlocking {
        var deleted = false
        val child = launch {
            persistFcmRecord("a", event, { true }, {
                currentCoroutineContext().cancel(); 1L
            }, { _, _ -> currentCoroutineContext().ensureActive(); yield(); deleted = true })
        }
        child.join()
        assertTrue(child.isCancelled); assertTrue(deleted)
    }

    @Test fun cancellationDuringPreparationCleansUpAndPreservesCancellation() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        var deleted = false
        var posts = 0
        val child = launch {
            deliverFcmRecord("a", event, { true }, { 1L }, { _, _ ->
                currentCoroutineContext().ensureActive(); yield(); deleted = true
            }) { entered.complete(Unit); never.await(); posts++; true }
        }
        entered.await(); child.cancelAndJoin()
        assertTrue(child.isCancelled); assertTrue(deleted); assertEquals(0, posts)
    }

    @Test fun overlappingDuplicateDoesNotDeleteActiveAttempt() = runBlocking {
        val records = mutableSetOf<String>()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var posts = 0
        val insert: suspend (NotificationItem) -> Long = { if (records.add(it.id)) 1L else -1L }
        val delete: suspend (String, List<String>) -> Unit = { _, ids -> records.removeAll(ids.toSet()); Unit }
        val first = async {
            deliverFcmRecord("a", event, { true }, insert, delete) {
                entered.complete(Unit); release.await(); posts++; true
            }
        }
        entered.await()
        assertFalse(deliverFcmRecord("a", event, { true }, insert, delete) { fail("duplicate post"); true })
        assertEquals(1, records.size)
        release.complete(Unit); assertTrue(first.await())
        assertEquals(1, records.size); assertEquals(1, posts)
    }
    @Test fun switchDuringDiskWriteRemovesOldRecordAndCannotPublish() = runBlocking {
        var owns = true
        val removed = mutableListOf<String>()
        val result = persistFcmRecord("a", event, { owns }, { owns = false; 1L }, { uid, ids ->
            assertEquals("a", uid); removed.addAll(ids)
        })
        assertNull(result)
        assertEquals(listOf("fcm_a_${event.eventId}"), removed)
    }
    @Test fun repeatedEventHasStableIdentityAndCannotPublishTwice() = runBlocking {
        val records = mutableSetOf<String>()
        val insert: suspend (NotificationItem) -> Long = { if (records.add(it.id)) 1L else -1L }
        val first = persistFcmRecord("a", event, { true }, insert, { _, _ -> fail("No cleanup needed") })
        assertNotNull(first)
        assertNull(persistFcmRecord("a", event, { true }, insert, { _, _ -> }))
        assertNotNull(persistFcmRecord("b", event, { true }, insert, { _, _ -> }))
        assertEquals(2, records.size)
    }
    @Test fun invalidSessionNeverWritesAndCancelledInsertCannotContinue() = runBlocking {
        assertNull(persistFcmRecord("a", event, { false }, { fail("Must not write"); 1L }, { _, _ -> }))
        try {
            persistFcmRecord("a", event, { true }, { throw CancellationException("test") }, { _, _ -> })
            fail("Cancellation must propagate before publication")
        } catch (_: CancellationException) { }
    }
}
