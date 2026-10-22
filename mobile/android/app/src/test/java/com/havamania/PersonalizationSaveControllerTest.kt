package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PersonalizationSaveControllerTest {
    @Test fun waitsForPersistenceAndRejectsDoubleTap() = runBlocking {
        val response = CompletableDeferred<Unit>()
        val controller = PersonalizationSaveController(this, { "A" })
        var writes = 0
        controller.save { writes++; response.await() }
        controller.save { writes++ }
        assertEquals(ProfileSaveState.Saving, controller.state.value)
        yield()
        assertEquals(1, writes)
        response.complete(Unit)
        yield()
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }

    @Test fun failedWriteCanBeRetriedWithoutResettingProfile() = runBlocking {
        val controller = PersonalizationSaveController(this, { "A" })
        controller.save { error("offline") }
        yield()
        assertTrue(controller.state.value is ProfileSaveState.Failed)
        controller.save { }
        yield()
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }

    @Test fun resetAndAccountChangeSuppressUncooperativeOldWrite() = runBlocking {
        var uid = "A"
        var owner = ""
        val response = CompletableDeferred<Unit>()
        val controller = PersonalizationSaveController(this, { uid })
        controller.save { owner = it; withContext(NonCancellable) { response.await() } }
        yield()
        uid = "B"
        controller.reset()
        response.complete(Unit)
        yield(); yield()
        assertEquals("A", owner)
        assertEquals(ProfileSaveState.Idle, controller.state.value)
        controller.save { owner = it }
        yield()
        assertEquals("B", owner)
        assertEquals(ProfileSaveState.Saved, controller.state.value)
    }

    @Test fun timeoutReportsMissingAcknowledgementInsteadOfSuccess() = runBlocking {
        val controller = PersonalizationSaveController(this, { "A" }, timeoutMillis = 20)
        controller.save { awaitCancellation() }
        withTimeout(2000) {
            while (controller.state.value == ProfileSaveState.Saving) delay(5)
        }
        assertTrue(controller.state.value is ProfileSaveState.Failed)
        controller.reset()
        assertEquals(ProfileSaveState.Idle, controller.state.value)
    }
}
