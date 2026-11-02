package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySnapshotApplicationTest {
    @Test fun currentSnapshotIsSavedBeforePublication() = runBlocking {
        val calls = mutableListOf<String>()
        applyCitySnapshot("a", 1, { "a" }, { 1 },
            { calls.add("save") }, { calls.add("publish") })
        assertEquals(listOf("save", "publish"), calls)
    }

    @Test fun oldListenerCannotWriteAfterReturningToSameUid() = runBlocking {
        applyCitySnapshot("a", 1, { "a" }, { 3 },
            { fail("No old-listener disk write") }, { fail("No publication") })
    }

    @Test fun differentOwnerCannotWrite() = runBlocking {
        applyCitySnapshot("a", 1, { "b" }, { 1 },
            { fail("No old-account disk write") }, { fail("No publication") })
    }

    @Test fun listenerReplacementDuringDiskWriteSuppressesPublication() = runBlocking {
        var version = 1L
        applyCitySnapshot("a", 1, { "a" }, { version },
            { version = 2 }, { fail("No stale publication") })
    }

    @Test fun accountChangeDuringDiskWriteSuppressesPublication() = runBlocking {
        var owner = "a"
        applyCitySnapshot("a", 1, { owner }, { 1 },
            { owner = "b" }, { fail("No stale publication") })
    }

    @Test fun cancelledSessionCannotPublishEvenAfterNonCancellableSave() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val work = launch {
            applyCitySnapshot("a", 1, { "a" }, { 1 }, {
                started.complete(Unit)
                withContext(NonCancellable) { finish.await() }
            }, { fail("No cancelled publication") })
        }
        started.await(); work.cancel(); finish.complete(Unit); work.join()
        assertTrue(work.isCancelled)
    }
}
