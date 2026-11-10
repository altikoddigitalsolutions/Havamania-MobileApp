package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test

class AccountProfileSyncTest {
    @Test fun deletionPreventsTransactionStart() = runBlocking {
        createMissingAccountProfile("a", { "a" }, { false }) { fail("No transaction") }
    }
    @Test fun deletionDuringTransactionReadPreventsStaging() {
        var allowed = true
        assertFalse(stageMissingAccountProfile({ allowed }, { allowed = false; false }, { fail("No write") }))
    }
    @Test fun ownerChangeDuringTransactionReadPreventsStaging() = runBlocking {
        var uid = "a"
        createMissingAccountProfile("a", { uid }, { true }) { live ->
            stageMissingAccountProfile(live, { uid = "b"; false }, { fail("No stale write") })
        }
    }
    @Test fun onlyMissingDocumentStagesCreation() {
        var writes = 0
        assertFalse(stageMissingAccountProfile({ true }, { true }, { writes++ }))
        assertTrue(stageMissingAccountProfile({ true }, { false }, { writes++ }))
        assertEquals(1, writes)
    }
    @Test fun cancelledSessionInvalidatesSdkRetryCallback() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val work = launch {
            createMissingAccountProfile("a", { "a" }, { true }) { live ->
                entered.complete(Unit)
                withContext(NonCancellable) {
                    release.await()
                    assertFalse(stageMissingAccountProfile(live, { fail("No read after cancellation"); false }, { fail("No write") }))
                }
            }
        }
        entered.await(); work.cancel(); release.complete(Unit); work.join()
        assertTrue(work.isCancelled)
    }
    @Test fun retryAfterAnotherClientCommitsDoesNotOverwriteProfile() {
        var staged = false
        stageMissingAccountProfile({ true }, { false }, { staged = true })
        assertTrue(staged) // First transaction attempt will conflict and be discarded.
        val committedByOtherClient = "Existing customized profile"
        var profile = committedByOtherClient
        assertFalse(stageMissingAccountProfile({ true }, { profile.isNotEmpty() }, { profile = "Default" }))
        assertEquals(committedByOtherClient, profile)
    }
    @Test fun atomicAdapterAllowsOnlyOneOfTwentyClientsToCreate() = runBlocking {
        val lock = Mutex()
        var profile: String? = null
        var creates = 0
        coroutineScope {
            (1..20).map { client -> async(Dispatchers.Default) {
                createMissingAccountProfile("a", { "a" }, { true }) { live ->
                    lock.withLock {
                        stageMissingAccountProfile(live, { profile != null }, { profile = "Client $client"; creates++ })
                    }
                }
            } }.awaitAll()
        }
        assertEquals(1, creates)
        assertNotNull(profile)
    }
    @Test fun failedTransactionReadCannotStageCreation() {
        try {
            stageMissingAccountProfile({ true }, { throw IOException("offline") }, { fail("No write") })
            fail("Expected read failure")
        } catch (_: IOException) { }
    }
}
