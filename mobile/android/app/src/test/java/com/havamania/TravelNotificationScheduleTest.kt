package com.havamania

import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit.HOURS
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelNotificationScheduleTest {
    @Test fun usesTodayAtNineUntilTheTargetPasses() {
        val zone = ZoneId.of("Europe/Istanbul")
        assertEquals(HOURS.toMillis(1), travelNotificationInitialDelay(Instant.parse("2026-11-10T05:00:00Z"), zone))
        assertEquals(0L, travelNotificationInitialDelay(Instant.parse("2026-11-10T06:00:00Z"), zone))
        assertEquals(HOURS.toMillis(23), travelNotificationInitialDelay(Instant.parse("2026-11-10T07:00:00Z"), zone))
    }

    @Test fun elapsedDelayAccountsForDaylightSavingChange() {
        val zone = ZoneId.of("Europe/Berlin")
        assertEquals(HOURS.toMillis(22), travelNotificationInitialDelay(Instant.parse("2026-03-28T09:00:00Z"), zone))
        assertEquals(HOURS.toMillis(24), travelNotificationInitialDelay(Instant.parse("2026-10-24T08:00:00Z"), zone))
    }
}
