package com.havamania

import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CitySyncAttemptSafetyTest {
    private val original = CitySyncChange(GeocodingResultDto(id = 1, name = "A"), false)

    @Test fun preflightDiskFailurePreventsNetwork() = runBlocking {
        try {
            CitySyncDispatcher.sendLatest("preflight-fail", "a", { "a" }, { original },
                { fail("No network") }, { fail("No acknowledgement") },
                reserve = { _, _ -> throw IOException("disk") })
            fail("Expected disk failure")
        } catch (_: IOException) { }
    }
    @Test fun localTimeoutKeepsFenceAndBatchContinues() = runBlocking {
        var stored = original
        val visited = mutableListOf<Int>()
        runCitySyncBatch(listOf(1, 2), "a", { "a" }, { item ->
            visited.add(item)
            if (item == 1) CitySyncDispatcher.sendLatest("timeout-safe", "a", { "a" }, { stored },
                { awaitCancellation() }, { fail("No timeout acknowledgement") },
                recordFailure = { _, _ -> fail("Timeout must retain fence") }, now = { 100 },
                reserve = { change, now -> change.copy(retryNotBefore = now + CITY_SYNC_UNCERTAIN_SECONDS).also { stored = it } },
                timeoutMillis = 10)
        }, { assertTrue(it is CitySyncTimeoutException) })
        assertEquals(listOf(1, 2), visited)
        assertEquals(100 + CITY_SYNC_UNCERTAIN_SECONDS, stored.retryNotBefore)
        assertFalse(stored.acknowledged)
    }
    @Test fun failureRecordingErrorCannotErasePreflightFence() = runBlocking {
        var stored = original
        try {
            CitySyncDispatcher.sendLatest("failure-record-safe", "a", { "a" }, { stored },
                { throw IOException("network") }, { fail("No acknowledgement") },
                recordFailure = { _, _ -> throw IOException("disk") }, now = { 100 },
                reserve = { change, now -> change.copy(retryNotBefore = now + CITY_SYNC_UNCERTAIN_SECONDS).also { stored = it } })
            fail("Expected disk failure")
        } catch (_: IOException) { }
        assertEquals(100 + CITY_SYNC_UNCERTAIN_SECONDS, stored.retryNotBefore)
    }
    @Test fun supersededReservationCannotStartNetwork() = runBlocking {
        CitySyncDispatcher.sendLatest("superseded-reservation", "a", { "a" }, { original },
            { fail("No network") }, { fail("No acknowledgement") }, reserve = { _, _ -> null })
    }
}
