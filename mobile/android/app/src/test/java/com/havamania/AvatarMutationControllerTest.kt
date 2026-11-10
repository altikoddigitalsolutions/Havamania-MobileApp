package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AvatarMutationControllerTest {
    @Test fun uploadAndDeleteCannotRunTogether() = runBlocking {
        var busy = false
        val finished = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var successes = 0
        val controller = AvatarMutationController(this, { "a" }, { true },
            { busy = it; if (!it) finished.complete(Unit) }, { successes++ }, { fail(it) })
        assertTrue(controller.start { entered.complete(Unit); release.await() })
        entered.await(); assertTrue(busy)
        assertFalse(controller.start { fail("No overlapping delete") })
        release.complete(Unit); withTimeout(2000) { finished.await() }
        assertFalse(busy); assertEquals(1, successes)
    }
    @Test fun resetStopsOldResultFromClearingNewBusyState() = runBlocking {
        var busy = false
        var successes = 0
        val entered = CompletableDeferred<Unit>()
        val oldRelease = CompletableDeferred<Unit>()
        val newEntered = CompletableDeferred<Unit>()
        val newRelease = CompletableDeferred<Unit>()
        val controller = AvatarMutationController(this, { "a" }, { true }, { busy = it }, { successes++ }, { fail(it) })
        controller.start { entered.complete(Unit); withContext(NonCancellable) { oldRelease.await() } }
        entered.await(); controller.reset()
        controller.start { newEntered.complete(Unit); newRelease.await() }
        newEntered.await(); oldRelease.complete(Unit); yield()
        assertTrue(busy); assertEquals(0, successes)
        newRelease.complete(Unit)
        withTimeout(2000) { while (busy) yield() }
        assertEquals(1, successes)
    }
    @Test fun deletionDuringRemoteWorkSuppressesSuccessAndError() = runBlocking {
        var allowed = true
        val finished = CompletableDeferred<Unit>()
        val controller = AvatarMutationController(this, { "a" }, { allowed },
            { if (!it) finished.complete(Unit) }, { fail("No deleted success") }, { fail("No deleted error") })
        controller.start { allowed = false }
        withTimeout(2000) { finished.await() }
    }
    @Test fun signedOutOrDeletingUserCannotStartOperation() = runBlocking {
        val signedOut = AvatarMutationController(this, { null }, { true }, {}, {}, { fail(it) })
        assertFalse(signedOut.start { fail("No signed-out write") })
        val deleting = AvatarMutationController(this, { "a" }, { false }, {}, {}, { fail(it) })
        assertFalse(deleting.start { fail("No deleting write") })
    }
    @Test fun errorsNeverExposeServiceMessageOrPhotoToken() {
        val private = "https://private.example/avatar?token=SECRET"
        for (error in listOf(Exception(private), java.io.IOException(private), SecurityException(private))) {
            val message = avatarErrorMessage(error)
            assertFalse(message.contains("SECRET")); assertFalse(message.contains("private.example"))
        }
        assertTrue(avatarErrorMessage(AvatarImagePreparationException()).contains("başka bir fotoğraf"))
    }
}
