package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountProfileSyncTest {
    @Test fun blockedDeletionDoesNotReadOrCreate() = runBlocking {
        createMissingAccountProfile("a", { "a" }, { false },
            { fail("No read"); false }, { fail("No create") })
    }
    @Test fun deletionDuringReadPreventsProfileRecreation() = runBlocking {
        var allowed = true
        createMissingAccountProfile("a", { "a" }, { allowed },
            { allowed = false; false }, { fail("No create after deletion") })
    }
    @Test fun accountChangeDuringReadPreventsProfileRecreation() = runBlocking {
        var uid = "a"
        createMissingAccountProfile("a", { uid }, { true },
            { uid = "b"; false }, { fail("No stale-account create") })
    }
    @Test fun onlyConfirmedMissingProfileIsCreated() = runBlocking {
        var created = 0
        createMissingAccountProfile("a", { "a" }, { true }, { true }, { created++ })
        assertEquals(0, created)
        createMissingAccountProfile("a", { "a" }, { true }, { false }, { created++ })
        assertEquals(1, created)
        try {
            createMissingAccountProfile("a", { "a" }, { true }, { throw IOException("offline") }, { created++ })
            fail("Expected read error")
        } catch (_: IOException) { }
        assertEquals(1, created)
    }
    @Test fun cancelledReadCannotCreateAfterReturningToSameUid() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val work = launch {
            createMissingAccountProfile("a", { "a" }, { true }, {
                entered.complete(Unit); withContext(NonCancellable) { release.await() }; false
            }, { fail("No create from cancelled session") })
        }
        entered.await(); work.cancel(); release.complete(Unit); work.join()
        assertTrue(work.isCancelled)
    }
}
