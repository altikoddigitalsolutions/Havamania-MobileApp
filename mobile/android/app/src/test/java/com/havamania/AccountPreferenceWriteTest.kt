package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountPreferenceWriteTest {
    @Test fun successIsPublishedOnlyAfterWriteCompletes() = runBlocking {
        val done = CompletableDeferred<Unit>()
        var stored = false
        var published = false
        val work = launch(start = CoroutineStart.UNDISPATCHED) {
            persistAccountPreference("a", { "a" }, { owner ->
                assertEquals("a", owner); done.await(); stored = true
            }, { assertTrue(stored); published = true }, { fail("Unexpected error") })
        }
        assertFalse(published)
        done.complete(Unit); work.join()
        assertTrue(published)
    }

    @Test fun writeFailureReportsErrorWithoutPublishingNewPreference() = runBlocking {
        var failed = false
        var value = "previous"
        persistAccountPreference("a", { "a" }, { throw IOException("PRIVATE_STORAGE_DETAIL") },
            { value = "new" }, { failed = true })
        assertTrue(failed)
        assertEquals("previous", value)
    }

    @Test fun supersededOwnerNeverStartsWrite() = runBlocking {
        persistAccountPreference("a", { "b" }, { fail("No stale write") },
            { fail("No stale UI") }, { fail("No stale error") })
    }

    @Test fun ownerRemainsCapturedButLateSuccessCannotPublishToNewUser() = runBlocking {
        var uid = "a"
        val done = CompletableDeferred<Unit>()
        val targets = mutableListOf<String>()
        var published = false
        val work = launch(start = CoroutineStart.UNDISPATCHED) {
            persistAccountPreference("a", { uid }, { owner -> targets.add(owner); done.await() },
                { published = true }, { fail("No error") })
        }
        uid = "b" // Firebase changed before its auth callback cancels work.
        done.complete(Unit); work.join()
        assertEquals(listOf("a"), targets)
        assertFalse(published)
    }

    @Test fun lateWriteErrorCannotShowOnNewAccount() = runBlocking {
        var uid = "a"
        val done = CompletableDeferred<Unit>()
        var failed = false
        val work = launch(start = CoroutineStart.UNDISPATCHED) {
            persistAccountPreference("a", { uid }, { done.await(); throw IOException("local-test") },
                { fail("No success") }, { failed = true })
        }
        uid = "b"; done.complete(Unit); work.join()
        assertFalse(failed)
    }

    @Test fun returningToSameUidCannotReviveCancelledWriteResult() = runBlocking {
        var uid = "a"
        val tasks = AccountTaskScope(this) { uid }
        val started = CompletableDeferred<Unit>()
        val done = CompletableDeferred<Unit>()
        var published = false
        var failed = false
        try {
            val work = tasks.launch { owner ->
                persistAccountPreference(owner, { uid }, {
                    started.complete(Unit)
                    withContext(NonCancellable) { done.await() }
                }, { published = true }, { failed = true })
            }
            started.await()
            uid = "b"; tasks.reset()
            uid = "a"; tasks.reset()
            done.complete(Unit); work.join()
            assertTrue(work.isCancelled)
            assertFalse(published)
            assertFalse(failed)
        } finally { tasks.close() }
    }
}
