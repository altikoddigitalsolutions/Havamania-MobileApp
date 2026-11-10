package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HistorySaveControllerTest {
    @Test fun waitsForAcknowledgementAndIgnoresDoubleSubmit() = runBlocking {
        val ack = CompletableDeferred<Unit>()
        var writes = 0
        val controller = HistorySaveController(this) { "A" }
        controller.save { _, _ -> writes++; ack.await() }
        controller.save { _, _ -> writes++ }
        yield()
        assertEquals(1, writes)
        assertEquals(ProfileSaveState.Saving, controller.state.value)
        ack.complete(Unit)
        yield()
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }
    @Test fun retryReusesRecordIdAfterFailure() = runBlocking {
        val ids = mutableListOf<String>()
        val controller = HistorySaveController(this) { "A" }
        controller.save { _, id -> ids += id; error("offline") }
        yield()
        assertTrue(controller.state.value is ProfileSaveState.Failed)
        controller.save { _, id -> ids += id }
        yield()
        assertEquals(ids[0], ids[1])
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }
    @Test fun accountResetRejectsOldAcknowledgement() = runBlocking {
        var uid = "A"
        val ack = CompletableDeferred<Unit>()
        val controller = HistorySaveController(this) { uid }
        controller.save { _, _ -> withContext(NonCancellable) { ack.await() } }
        yield()
        uid = "B"
        controller.reset()
        ack.complete(Unit)
        yield(); yield()
        assertEquals(ProfileSaveState.Idle, controller.state.value)
    }
    @Test fun leavingScreenCancelsAcknowledgementButRetainsRetryIdentity() = runBlocking {
        val ack = CompletableDeferred<Unit>()
        val ids = mutableListOf<String>()
        val controller = HistorySaveController(this) { "A" }
        controller.save { _, id -> ids += id; ack.await() }
        yield()
        controller.cancel()
        controller.save { _, id -> ids += id }
        yield()
        assertEquals(ids[0], ids[1])
    }
    @Test fun aNewExplicitConversationCannotReusePreviousCancelledConversationsId() = runBlocking {
        val ids = mutableListOf<String>()
        val controller = HistorySaveController(this) { "A" }
        controller.save("first-conversation") { _, id -> ids += id }
        yield()
        controller.cancel()
        controller.save("second-conversation") { _, id -> ids += id }
        yield()
        assertEquals(listOf("first-conversation", "second-conversation"), ids)
        controller.reset()
    }
}
