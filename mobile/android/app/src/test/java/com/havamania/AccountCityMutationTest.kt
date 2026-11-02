package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.*
import org.junit.Test

class AccountCityMutationTest {
    @Test fun concurrentEditsReadTheLatestList() = runBlocking {
        val lock = Mutex()
        val gate = CompletableDeferred<Unit>()
        var cities = listOf(1)
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            mutateAccountCities(lock, "a", { "a" }, {
                val old = cities; gate.await(); cities = old + 2; true
            }, {}, { fail("Unexpected failure") })
        }
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            mutateAccountCities(lock, "a", { "a" }, {
                cities = cities + 3; true
            }, {}, { fail("Unexpected failure") })
        }
        gate.complete(Unit); joinAll(first, second)
        assertEquals(listOf(1, 2, 3), cities)
    }

    @Test fun slowRemoteAcknowledgementDoesNotBlockNextLocalEdit() = runBlocking {
        val lock = Mutex()
        val waiting = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var secondStored = false
        val first = launch {
            mutateAccountCities(lock, "a", { "a" }, { true }, {
                waiting.complete(Unit); release.await()
            }, { fail("Unexpected failure") })
        }
        waiting.await()
        try {
            withTimeout(2000) {
                mutateAccountCities(lock, "a", { "a" }, { secondStored = true; true }, {}, {})
            }
            assertTrue(secondStored)
        } finally { release.complete(Unit); first.join() }
    }

    @Test fun changedAccountAfterLocalWriteDoesNotStartRemoteWrite() = runBlocking {
        var uid = "a"
        mutateAccountCities(Mutex(), "a", { uid }, { uid = "b"; true },
            { fail("No stale remote request") }, { fail("No stale error") })
    }

    @Test fun localFailureStopsRemoteAndReportsError() = runBlocking {
        var failed = false
        mutateAccountCities(Mutex(), "a", { "a" }, { throw IOException("private") },
            { fail("No remote write") }, { failed = true })
        assertTrue(failed)
    }

    @Test fun remoteFailureIsVisibleButDoesNotRollbackLocalWrite() = runBlocking {
        var stored = false
        var failed = false
        mutateAccountCities(Mutex(), "a", { "a" }, { stored = true; true },
            { throw IOException("private") }, { failed = true })
        assertTrue(stored); assertTrue(failed)
    }

    @Test fun cancelledSessionCannotStartRemoteEvenWhenUidReturns() = runBlocking {
        var uid = "a"
        val tasks = AccountTaskScope(this) { uid }
        val entered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        try {
            val work = tasks.launch { owner ->
                mutateAccountCities(Mutex(), owner, { uid }, {
                    entered.complete(Unit)
                    withContext(NonCancellable) { gate.await() }; true
                }, { fail("No stale remote write") }, { fail("No stale error") })
            }
            entered.await(); uid = "b"; tasks.reset(); uid = "a"; tasks.reset()
            gate.complete(Unit); work.join()
            assertTrue(work.isCancelled)
        } finally { tasks.close() }
    }
}
