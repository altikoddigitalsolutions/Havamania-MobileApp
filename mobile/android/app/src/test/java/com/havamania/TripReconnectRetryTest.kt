package com.havamania

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TripReconnectRetryTest {
    @Test fun reconnectWaitsForActiveAttemptBeforeRetrying() = runBlocking {
        val activeAttempt = Job()
        var retries = 0
        val reconnect = launch(start = CoroutineStart.UNDISPATCHED) {
            retryTripQueueAfterActiveAttempt(activeAttempt, { true }) { retries++ }
        }
        assertEquals(0, retries)
        activeAttempt.complete()
        reconnect.join()
        assertEquals(1, retries)
    }

    @Test fun newSessionOrSecondDisconnectPreventsDeferredRetry() = runBlocking {
        val activeAttempt = Job()
        var currentSession = true
        var retries = 0
        val reconnect = launch(start = CoroutineStart.UNDISPATCHED) {
            retryTripQueueAfterActiveAttempt(activeAttempt, { currentSession }) { retries++ }
        }
        val disconnected = launch(start = CoroutineStart.UNDISPATCHED) {
            retryTripQueueAfterActiveAttempt(activeAttempt, { true }) { retries++ }
        }
        disconnected.cancelAndJoin()
        currentSession = false
        activeAttempt.complete()
        reconnect.join()
        assertEquals(0, retries)
    }
}
