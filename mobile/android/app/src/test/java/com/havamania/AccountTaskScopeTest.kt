package com.havamania

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class AccountTaskScopeTest {
    @Test fun accountChangeCancelsPendingWorkBeforeItPublishes() = runBlocking {
        var uid = "A"
        val tasks = AccountTaskScope(this) { uid }
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<String>()
        var published: String? = null
        try {
            val old = tasks.launch { owner -> started.complete(Unit); published = owner + response.await() }
            started.await()
            uid = "B"
            tasks.reset()
            response.complete("private data")
            old.join()
            assertTrue(old.isCancelled)
            assertNull(published)
            tasks.launch { owner -> published = owner }.join()
            assertEquals("B", published)
        } finally { tasks.close() }
    }

    @Test fun queuedWorkDoesNotStartWhenFirebaseIdentityAlreadyChanged() = runBlocking {
        var uid = "A"
        val tasks = AccountTaskScope(this) { uid }
        var ran = false
        try {
            val work = tasks.launch { ran = true }
            uid = "B" // Auth callback has not yet reset the task scope.
            work.join()
            assertFalse(ran)
        } finally { tasks.close() }
    }

    @Test fun operationRetainsOriginalOwnerAcrossSuspension() = runBlocking {
        var uid = "A"
        val tasks = AccountTaskScope(this) { uid }
        val response = CompletableDeferred<Unit>()
        var target: String? = null
        try {
            val work = tasks.launch { owner -> response.await(); target = owner }
            yield()
            uid = "B"
            response.complete(Unit)
            work.join()
            assertEquals("A", target)
        } finally { tasks.close() }
    }

    @Test fun returningToSameAccountDoesNotRevivePreviousWork() = runBlocking {
        var uid = "A"
        val tasks = AccountTaskScope(this) { uid }
        var ran = false
        try {
            val work = tasks.launch { ran = true }
            uid = "B"; tasks.reset()
            uid = "A"; tasks.reset()
            work.join()
            assertTrue(work.isCancelled)
            assertFalse(ran)
        } finally { tasks.close() }
    }
    @Test fun clearingViewModelScopeCancelsAccountWork() = runBlocking {
        val parentJob = kotlinx.coroutines.Job()
        val parent = kotlinx.coroutines.CoroutineScope(coroutineContext + parentJob)
        val tasks = AccountTaskScope(parent) { "A" }
        val started = CompletableDeferred<Unit>()
        var published = false
        val work = tasks.launch {
            started.complete(Unit)
            kotlinx.coroutines.awaitCancellation()
        }
        started.await()
        parentJob.cancel()
        work.join()
        tasks.launch { published = true }.join()
        assertTrue(work.isCancelled)
        assertFalse(published)
        tasks.close()
    }

}
