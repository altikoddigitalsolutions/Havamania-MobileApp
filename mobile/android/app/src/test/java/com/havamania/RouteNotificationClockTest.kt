package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class RouteNotificationClockTest {
    private fun plan(date: LocalDate = LocalDate.of(2026, 11, 26), time: String = "09:00") = TravelPlan(id = "trip", userId = "A",
        city = "Tokyo", startDate = date, endDate = date.plusDays(2), departureTime = time,
        originLatitude = 40.7, originLongitude = -74.0, lastForecastSnapshot = ForecastSnapshot(timezone = "Asia/Tokyo"))
    private val now = Instant.parse("2026-11-25T16:00:00Z")
    @Test fun `departure uses origin instead of device or destination`() {
        assertEquals(Instant.parse("2026-11-26T14:00:00Z"), routeNotificationClock(plan(), "America/New_York", now)!!.departure)
    }
    @Test fun `device timezone changes cannot move origin clock`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val first = routeNotificationClock(plan(), "America/New_York", now)
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            assertEquals(first, routeNotificationClock(plan(), "America/New_York", now))
        } finally { TimeZone.setDefault(previous) }
    }
    @Test fun `DST gap cannot silently shift departure`() {
        assertNull(routeNotificationClock(plan(LocalDate.of(2026, 3, 29), "01:30"), "Europe/London", now))
    }
    @Test fun `DST overlap cannot silently choose an offset`() {
        assertNull(routeNotificationClock(plan(LocalDate.of(2026, 10, 25), "01:30"), "Europe/London", now))
    }
    @Test fun `unknown origin timezone cannot borrow destination or device`() {
        assertNull(routeNotificationClock(plan(), null, now))
        assertNull(routeNotificationClock(plan(), "Invalid/Zone", now))
    }
    @Test fun `origin midnight controls daily key`() {
        val before = routeNotificationClock(plan(), "America/New_York", Instant.parse("2026-11-26T04:59:59Z"))!!
        val after = routeNotificationClock(plan(), "America/New_York", Instant.parse("2026-11-26T05:00:00Z"))!!
        assertNotEquals(before.dailyKey, after.dailyKey)
    }
    @Test fun `48 hour window uses elapsed time from origin`() {
        val departure = routeNotificationClock(plan(), "America/New_York", now)!!.departure
        val expected = Instant.parse("2026-11-26T14:00:00Z")
        assertTrue(TravelStatusResolver.isWithinRouteWeatherWindow(departure, expected.minusSeconds(48 * 3600)))
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(departure, expected.minusSeconds(48 * 3600 + 1)))
    }
}