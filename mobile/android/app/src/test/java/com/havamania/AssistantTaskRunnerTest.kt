package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AssistantTaskRunnerTest {
    @Test fun doubleTapIsRejectedBeforeDispatchAndFailureAllowsRetry() = runBlocking {
        var busy = false
        var starts = 0
        var failures = 0
        val runner = AssistantTaskRunner(this, { "A" }, { busy = it })
        assertTrue(runner.start(onStart = { starts++ }, operation = { error("offline") }, onSuccess = {}, onError = { failures++ }))
        assertFalse(runner.start(onStart = { starts++ }, operation = { "duplicate" }, onSuccess = {}, onError = {}))
        assertTrue(busy)
        yield()
        assertFalse(busy)
        assertEquals(1, starts)
        assertEquals(1, failures)
        var reply = ""
        runner.start(operation = { "retry" }, onSuccess = { reply = it }, onError = {})
        yield()
        assertEquals("retry", reply)
    }

    @Test fun accountSwitchSuppressesLateResultAndOldFinallyCannotClearNewLoading() = runBlocking {
        var uid = "A"
        var busy = false
        var published = ""
        var errors = 0
        val oldReply = CompletableDeferred<String>()
        val newReply = CompletableDeferred<String>()
        val runner = AssistantTaskRunner(this, { uid }, { busy = it })
        runner.start(operation = { withContext(NonCancellable) { oldReply.await() } }, onSuccess = { published = it }, onError = { errors++ })
        yield()
        uid = "B"
        runner.reset()
        runner.start(operation = { newReply.await() }, onSuccess = { published = it }, onError = { errors++ })
        yield()
        oldReply.complete("A private message")
        yield(); yield()
        assertEquals("", published)
        assertTrue(busy)
        assertEquals(0, errors)
        newReply.complete("B response")
        yield(); yield()
        assertEquals("B response", published)
        assertFalse(busy)
    }

    @Test fun identityChangeBeforeAuthCallbackBlocksPublication() = runBlocking {
        var uid = "A"
        val response = CompletableDeferred<String>()
        var published = false
        val runner = AssistantTaskRunner(this, { uid }, {})
        runner.start(operation = { response.await() }, onSuccess = { published = true }, onError = { published = true })
        yield()
        uid = "B"
        response.complete("old")
        yield()
        assertFalse(published)
    }

    @Test fun resetOfSameAccountInvalidatesQueuedGreetingAndHistory() = runBlocking {
        var ran = false
        val runner = AssistantTaskRunner(this, { "A" }, {})
        runner.start(operation = { ran = true; "greeting" }, onSuccess = { ran = true }, onError = {})
        runner.reset()
        yield()
        assertFalse(ran)
        var owner = ""
        runner.start(operation = { owner = it; "history" }, onSuccess = {}, onError = {})
        yield()
        assertEquals("A", owner)
    }

    @Test fun timeoutClearsLoadingAndProducesRetryableError() = runBlocking {
        var busy = false
        val error = CompletableDeferred<Exception>()
        val runner = AssistantTaskRunner(this, { "A" }, { busy = it }, timeoutMillis = 20)
        runner.start(operation = { awaitCancellation() }, onSuccess = {}, onError = { error.complete(it) })
        assertTrue(withTimeout(2000) { error.await() }.message!!.contains("zaman aşımı"))
        yield()
        assertFalse(busy)
    }
}
