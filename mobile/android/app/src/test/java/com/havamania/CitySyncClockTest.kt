package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CitySyncClockTest {
    private val original = CitySyncChange(GeocodingResultDto(id = 1, name = "A"), false,
        retryNotBefore = 130).withRetryClock(CitySyncClock(100, 10, "boot:1"), 30)
    @Test fun wallClockChangesCannotShortenOrExtendSameBootWait() {
        assertEquals(20L, original.retrySecondsLeft(CitySyncClock(1000000, 20, "boot:1")))
        assertEquals(20L, original.retrySecondsLeft(CitySyncClock(1, 20, "boot:1")))
        assertEquals(0L, original.retrySecondsLeft(CitySyncClock(1, 40, "boot:1")))
    }
    @Test fun rebootAndLegacyMetadataRequireConservativeRebase() {
        assertTrue(original.retryClockNeedsReset(CitySyncClock(1000000, 50, "boot:2")))
        assertTrue(original.copy(retryBoot = "").retryClockNeedsReset(CitySyncClock(101, 20, "boot:1")))
        assertTrue(original.retryClockNeedsReset(CitySyncClock(101, 1, "boot:1")))
        assertEquals(CITY_SYNC_UNCERTAIN_SECONDS, original.retrySecondsLeft(CitySyncClock(1000000, 50, "boot:2")))
    }
    @Test fun expiredElapsedDeadlineCanProceedEvenIfWallClockMovedBackwards() = runBlocking {
        var sent = false
        CitySyncDispatcher.sendLatest("elapsed-ready", "a", { "a" }, { original }, { sent = true }, {},
            now = { 1 }, clock = { CitySyncClock(1, 40, "boot:1") })
        assertTrue(sent)
    }
    @Test fun forwardWallClockJumpCannotStartNetworkEarly() = runBlocking {
        try {
            CitySyncDispatcher.sendLatest("elapsed-wait", "a", { "a" }, { original },
                { fail("No early network") }, {}, now = { 1000000 }, clock = { CitySyncClock(1000000, 20, "boot:1") })
            fail("Expected waiting")
        } catch (_: CitySyncWaitingException) { }
    }
}
