package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AiHistoryDetailLoaderTest {
    @Test fun emptyInitialListDoesNotMeanMissingWhileReadIsPending() = runBlocking {
        val result = CompletableDeferred<AiHistoryEntity?>()
        val loader = AiHistoryDetailLoader(this) { "A" }
        loader.load { result.await() }
        yield()
        assertEquals(AiHistoryDetailState.Loading, loader.state.value)
        result.complete(AiHistoryEntity(id = "record", userId = "A"))
        yield()
        assertEquals("record", (loader.state.value as AiHistoryDetailState.Ready).item.id)
    }

    @Test fun confirmedAbsenceShowsMissing() = runBlocking {
        val loader = AiHistoryDetailLoader(this) { "A" }
        loader.load { null }
        yield()
        assertEquals(AiHistoryDetailState.Missing, loader.state.value)
    }

    @Test fun failedReadShowsErrorAndRetryCanRecover() = runBlocking {
        val loader = AiHistoryDetailLoader(this) { "A" }
        loader.load { error("offline") }
        yield()
        assertEquals(AiHistoryDetailState.Failed, loader.state.value)
        loader.load { AiHistoryEntity(id = "cached") }
        yield()
        assertTrue(loader.state.value is AiHistoryDetailState.Ready)
    }

    @Test fun oldDetailCannotReplaceNewNavigationEvenWhenCancellationIsIgnored() = runBlocking {
        val old = CompletableDeferred<AiHistoryEntity?>()
        val loader = AiHistoryDetailLoader(this) { "A" }
        loader.load { withContext(NonCancellable) { old.await() } }
        yield()
        loader.load { AiHistoryEntity(id = "new") }
        yield()
        old.complete(AiHistoryEntity(id = "old"))
        yield(); yield()
        assertEquals("new", (loader.state.value as AiHistoryDetailState.Ready).item.id)
    }

    @Test fun accountChangeDoesNotPublishPreviousOwnersRecord() = runBlocking {
        var uid = "A"
        val result = CompletableDeferred<AiHistoryEntity?>()
        val loader = AiHistoryDetailLoader(this) { uid }
        loader.load { result.await() }
        yield()
        uid = "B"
        loader.reset()
        result.complete(AiHistoryEntity(id = "private", userId = "A"))
        yield()
        assertEquals(AiHistoryDetailState.Loading, loader.state.value)
    }
}
