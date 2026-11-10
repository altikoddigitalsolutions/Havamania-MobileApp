package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class AccountDataObserverTest {
    @Test fun readFailureIsReportedAndRetryCanLoadWithoutLosingLastData() = runBlocking {
        var data = "last good"
        var error: Exception? = null
        observeAccountData(flow<String> { throw IOException("disk") }, "A", { "A" },
            { data = it }, { error = it })
        assertEquals("last good", data)
        assertTrue(error is IOException)
        observeAccountData(flowOf("recovered"), "A", { "A" },
            { data = it; error = null }, { error = it })
        assertEquals("recovered", data)
        assertNull(error)
    }

    @Test fun changedAccountReceivesNeitherOldDataNorOldError() = runBlocking {
        var uid = "A"
        var published = false
        val source = flow {
            uid = "B"
            emit("A private trip")
            throw IOException("A failure")
        }
        observeAccountData(source, "A", { uid }, { published = true }, { published = true })
        assertFalse(published)
    }

    @Test fun cancelledObserverDoesNotReportAUserError() = runBlocking {
        var failed = false
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            observeAccountData(flow<String> { awaitCancellation() }, "A", { "A" }, {}, { failed = true })
        }
        collector.cancelAndJoin()
        assertTrue(collector.isCancelled)
        assertFalse(failed)
    }

    @Test fun transformationFailureIsReportedWithoutPublishingPartialList() = runBlocking {
        var published = false
        var failed = false
        val source = flowOf(listOf("2026-11-01", "broken-date"))
            .map { list -> list.map { java.time.LocalDate.parse(it) } }
        observeAccountData(source, "A", { "A" }, { published = true }, { failed = true })
        assertFalse(published)
        assertTrue(failed)
    }
}
