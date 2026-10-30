package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FcmPersistenceTest {
    private val event = FcmEnvelope("d3667c20-5d15-42f3-a448-2ae58774b96b", "Test", "Local only", 2000)
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
