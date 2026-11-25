package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class TravelDailyIdentityTest {
    private val day = LocalDate.of(2026, 11, 26)
    private val now = Instant.parse("2026-11-25T16:00:00Z")
    private fun plan(zone: String? = "Asia/Tokyo") = TravelPlan(id = "trip", userId = "A", city = "Tokyo", startDate = day,
        endDate = day.plusDays(2), lastForecastSnapshot = ForecastSnapshot(timezone = zone))
    @Test fun `destination day owns identity`() {
        assertEquals(day, travelDailyIdentity(plan(), now)!!.day)
        assertEquals(day.minusDays(1), travelDailyIdentity(plan("America/Los_Angeles"), now)!!.day)
    }
    @Test fun `changing device timezone cannot change identity`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val first = travelDailyIdentity(plan(), now)
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertEquals(first, travelDailyIdentity(plan(), now))
        } finally { TimeZone.setDefault(previous) }
    }
    @Test fun `destination midnight starts new daily identity`() {
        val before = travelDailyIdentity(plan(), Instant.parse("2026-11-25T14:59:59Z"))!!
        val after = travelDailyIdentity(plan(), Instant.parse("2026-11-25T15:00:00Z"))!!
        assertNotEquals(before.notificationId, after.notificationId)
        assertNotEquals(before.deduplicationKey, after.deduplicationKey)
    }
    @Test fun `UTC midnight inside same destination day keeps deduplication key`() {
        val before = travelDailyIdentity(plan(), Instant.parse("2026-11-25T23:59:59Z"))!!
        val after = travelDailyIdentity(plan(), Instant.parse("2026-11-26T00:00:00Z"))!!
        assertEquals(before.deduplicationKey, after.deduplicationKey)
    }
    @Test fun `event timestamp uses destination start of day`() {
        assertEquals(Instant.parse("2026-11-25T15:00:00Z").toEpochMilli(), travelDailyIdentity(plan(), now)!!.eventAt)
    }
    @Test fun `unknown zone cannot silently use device zone`() {
        assertNull(travelDailyIdentity(plan(null), now)); assertNull(travelDailyIdentity(plan("Invalid/Zone"), now))
    }
    @Test fun `identity remains user and trip scoped`() {
        val a = travelDailyIdentity(plan(), now)!!
        assertNotEquals(a.notificationId, travelDailyIdentity(plan().copy(userId = "B"), now)!!.notificationId)
        assertNotEquals(a.deduplicationKey, travelDailyIdentity(plan().copy(id = "other"), now)!!.deduplicationKey)
    }
}