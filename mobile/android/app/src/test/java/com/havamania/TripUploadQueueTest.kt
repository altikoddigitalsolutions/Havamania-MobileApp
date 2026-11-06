package com.havamania

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TripUploadQueueTest {
    private val requests = listOf("failed", "timed-out", "saved").map { TripUploadRequest("owner", it, 1) }

    @Test fun failingRecordsRemainPendingWithoutBlockingLaterUploads() = runBlocking {
        val pending = requests.toMutableList()
        var failures = 0
        retryQueuedTripUploads(requests, onFailure = { failures++ }) { request ->
            when (request.recordId) {
                "failed" -> throw IOException("test")
                "timed-out" -> withTimeout(1) { awaitCancellation() }
                else -> pending.remove(request)
            }
        }
        assertEquals(2, failures)
        assertEquals(requests.take(2), pending)
    }

    @Test fun cancelledUploadStopsQueueWithoutReportingAFailure() = runBlocking {
        val cancellation = CancellationException("session changed")
        val attempted = mutableListOf<TripUploadRequest>()
        var failures = 0
        val thrown = runCatching {
            retryQueuedTripUploads(requests, onFailure = { failures++ }) {
                attempted += it
                throw cancellation
            }
        }.exceptionOrNull()
        assertSame(cancellation, thrown)
        assertEquals(listOf(requests.first()), attempted)
        assertEquals(0, failures)
    }
}
