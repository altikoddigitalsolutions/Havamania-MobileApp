package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class WeatherCacheClearerTest {
    @Test fun repeatedTapRunsOnceAndSuccessWaitsForStorageCompletion() = runBlocking {
        val storageDone = CompletableDeferred<Unit>()
        var calls = 0
        val clearer = WeatherCacheClearer { calls++; storageDone.await() }
        val first = launch(start = CoroutineStart.UNDISPATCHED) { clearer.clear() }
        assertEquals(WeatherCacheClearState.CLEARING, clearer.state.value)
        clearer.clear()
        assertEquals(1, calls)
        assertEquals(WeatherCacheClearState.CLEARING, clearer.state.value)
        storageDone.complete(Unit)
        first.join()
        assertEquals(WeatherCacheClearState.SUCCESS, clearer.state.value)
    }

    @Test fun failureIsNotSuccessAndRetryCanComplete() = runBlocking {
        var fail = true
        val clearer = WeatherCacheClearer { if (fail) throw java.io.IOException("PRIVATE_STORAGE_DETAIL") }
        clearer.clear()
        assertEquals(WeatherCacheClearState.ERROR, clearer.state.value)
        fail = false
        clearer.clear()
        assertEquals(WeatherCacheClearState.SUCCESS, clearer.state.value)
        clearer.dismiss()
        assertEquals(WeatherCacheClearState.IDLE, clearer.state.value)
    }

    @Test fun cancellationDoesNotReportSuccessOrLeaveButtonBusy() = runBlocking {
        var wait = true
        val clearer = WeatherCacheClearer { if (wait) awaitCancellation() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { clearer.clear() }
        job.cancelAndJoin()
        assertEquals(WeatherCacheClearState.IDLE, clearer.state.value)
        wait = false
        clearer.clear()
        assertEquals(WeatherCacheClearState.SUCCESS, clearer.state.value)
    }
}
