package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncDispatcherTest {
    private fun change(name: String) = CitySyncChange(GeocodingResultDto(id = 1, name = name), false)

    @Test fun waitingSenderReadsLatestVersionAndDoesNotResendAcknowledgedValue() = runBlocking {
        var pending = change("first")
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val sent = mutableListOf<String>()
        suspend fun dispatch() = CitySyncDispatcher.sendLatest("serial-test", "a", { "a" }, { pending }, {
            sent.add(it.city.name)
            if (it.city.name == "first") { entered.complete(Unit); release.await() }
        }, { if (pending.revision == it.revision) pending = pending.copy(acknowledged = true) })
        val first = launch { dispatch() }
        entered.await()
        pending = change("second")
        val second = launch(start = CoroutineStart.UNDISPATCHED) { dispatch() }
        pending = change("latest")
        assertEquals(listOf("first"), sent)
        release.complete(Unit); joinAll(first, second)
        dispatch()
        assertEquals(listOf("first", "latest"), sent)
        assertTrue(pending.acknowledged)
    }

    @Test fun ownerChangeDuringReadCannotSend() = runBlocking {
        var uid = "a"
        CitySyncDispatcher.sendLatest("owner-test", "a", { uid }, { uid = "b"; change("x") },
            { fail("No stale request") }, { fail("No acknowledgement") })
    }

    @Test fun failedSendDoesNotAcknowledgeAndReleasesLock() = runBlocking {
        val pending = change("x")
        var acknowledgements = 0
        try {
            CitySyncDispatcher.sendLatest("failure-test", "a", { "a" }, { pending },
                { throw IOException("test") }, { acknowledgements++ })
            fail("Expected failure")
        } catch (_: IOException) { }
        withTimeout(2000) {
            CitySyncDispatcher.sendLatest("failure-test", "a", { "a" }, { pending }, {}, { acknowledgements++ })
        }
        assertEquals(1, acknowledgements)
    }

    @Test fun cancelledSendNeverAcknowledgesItsResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val work = launch {
            CitySyncDispatcher.sendLatest("cancel-test", "a", { "a" }, { change("x") }, {
                entered.complete(Unit); withContext(NonCancellable) { release.await() }
            }, { fail("No cancelled acknowledgement") })
        }
        entered.await(); work.cancel(); release.complete(Unit); work.join()
        assertTrue(work.isCancelled)
    }
}
