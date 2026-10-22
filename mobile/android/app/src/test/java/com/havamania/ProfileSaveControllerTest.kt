package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ProfileSaveControllerTest {
    @Test fun serverFailureDoesNotReportSuccessAndCanBeRetried() = runBlocking {
        val controller = ProfileSaveController()
        controller.save("Ada", "Bio") { _, _ -> error("offline") }
        assertTrue(controller.state.value is ProfileSaveState.Failed)
        controller.save("Ada", "Bio") { _, _ -> }
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }

    @Test fun saveWaitsForAcknowledgementAndRejectsDoubleTap() = runBlocking {
        val controller = ProfileSaveController()
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        var writes = 0
        val first = launch {
            controller.save("Ada", "Bio") { _, _ -> writes++; started.complete(Unit); response.await() }
        }
        started.await()
        assertEquals(ProfileSaveState.Saving, controller.state.value)
        controller.save("Other", "") { _, _ -> writes++ }
        assertEquals(1, writes)
        response.complete(Unit)
        first.join()
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }

    @Test fun invalidNameNeverReachesPersistence() = runBlocking {
        val controller = ProfileSaveController()
        var writes = 0
        controller.save("  ", "Bio") { _, _ -> writes++ }
        assertEquals(0, writes)
        assertTrue(controller.state.value is ProfileSaveState.Failed)
    }

    @Test fun cancellationCannotAcknowledgeOldAccountSave() = runBlocking {
        val controller = ProfileSaveController()
        val started = CompletableDeferred<Unit>()
        val work = launch {
            controller.save("Ada", "Bio") { _, _ -> started.complete(Unit); awaitCancellation() }
        }
        started.await()
        work.cancelAndJoin()
        controller.reset()
        assertEquals(ProfileSaveState.Idle, controller.state.value)
    }

    @Test fun timeoutLeavesRetryableError() = runBlocking {
        val controller = ProfileSaveController(timeoutMillis = 20)
        controller.save("Ada", "Bio") { _, _ -> awaitCancellation() }
        assertTrue(controller.state.value is ProfileSaveState.Failed)
    }

    @Test fun trimsAndBoundsUserInput() = runBlocking {
        val controller = ProfileSaveController()
        controller.save("  " + "a".repeat(60), "b".repeat(510) + "  ") { name, bio ->
            assertEquals(50, name.length)
            assertEquals(500, bio.length)
        }
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }
}
