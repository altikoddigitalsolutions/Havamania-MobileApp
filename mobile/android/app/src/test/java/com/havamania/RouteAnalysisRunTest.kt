package com.havamania

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAnalysisRunTest {
    @Test
    fun cancelledAnalysisFinishingLateKeepsReplacementRunning() = runBlocking {
        val analysis = RouteAnalysisRun()
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val first = launch {
            val current = coroutineContext.job
            analysis.start(current)
            try {
                withContext(NonCancellable) {
                    firstStarted.complete(Unit)
                    releaseFirst.await()
                }
            } finally {
                analysis.finish(current)
            }
        }
        firstStarted.await()
        analysis.cancel()

        val secondStarted = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()
        val second = launch {
            val current = coroutineContext.job
            analysis.start(current)
            try {
                secondStarted.complete(Unit)
                releaseSecond.await()
            } finally {
                analysis.finish(current)
            }
        }
        secondStarted.await()
        releaseFirst.complete(Unit)
        first.join()
        assertTrue(first.isCancelled)
        assertTrue(analysis.running.value)

        releaseSecond.complete(Unit)
        second.join()
        assertFalse(analysis.running.value)
    }
}
