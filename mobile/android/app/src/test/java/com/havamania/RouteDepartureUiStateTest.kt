package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RouteDepartureUiStateTest {
    private val now = Instant.parse("2026-11-24T14:00:00Z")
    private fun plan() = TravelPlan(city = "Tokyo", startDate = LocalDate.of(2026, 11, 26),
        endDate = LocalDate.of(2026, 11, 28), departureTime = "09:00")
    @Test fun `unknown clock never opens analysis`() { assertFalse(routeClockEligible(plan(), null, now)) }
    @Test fun `UI shares exact 48 elapsed hour boundary`() {
        val clock = routeNotificationClock(plan(), "America/New_York", now)!!
        assertTrue(routeClockEligible(plan(), clock, now))
        assertFalse(routeClockEligible(plan(), clock, now.minusSeconds(1)))
    }
    @Test fun `ongoing access is retained and past trip closes in origin zone`() {
        val clock = routeNotificationClock(plan(), "America/New_York", now)!!
        assertTrue(routeClockEligible(plan(), clock, Instant.parse("2026-11-27T12:00:00Z")))
        assertFalse(routeClockEligible(plan(), clock, Instant.parse("2026-11-29T12:00:00Z")))
    }
    @Test fun `displayed moment follows named origin zone across device changes`() {
        val clock = routeNotificationClock(plan(), "America/New_York", now)!!
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val first = formatRouteMoment(clock.departure.toEpochMilli(), clock)
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            assertEquals(first, formatRouteMoment(clock.departure.toEpochMilli(), clock))
            assertEquals("26 Kas 09:00", first)
        } finally { TimeZone.setDefault(previous) }
    }
    @Test fun `missing moment or zone displays unknown`() {
        assertEquals("Saat bilinmiyor", formatRouteMoment(0, null))
        assertEquals("Saat bilinmiyor", formatRouteMoment(null, routeNotificationClock(plan(), "UTC", now)))
    }
    @Test fun `DST ambiguity has explicit error without instant`() = runBlocking {
        val value = plan().copy(startDate = LocalDate.of(2026, 10, 25), departureTime = "01:30")
        val state = resolveRouteScreenClock(value, GeoPoint(51.5, -0.1), now) { _, _ -> "Europe/London" }
        assertNull(state.clock); assertTrue(state.error.orEmpty().contains("belirsiz"))
    }
    @Test fun `resolved actual origin overrides stored origin for timezone lookup`() = runBlocking {
        var requested: Pair<Double, Double>? = null
        val state = resolveRouteScreenClock(plan().copy(originLatitude = 35.0, originLongitude = 139.0), GeoPoint(40.7, -74.0), now) { lat, lon ->
            requested = lat to lon; "America/New_York"
        }
        assertEquals(40.7 to -74.0, requested); assertEquals("America/New_York", state.clock?.zone?.id)
    }
    @Test fun `unknown provider timezone has explicit error without device fallback`() = runBlocking {
        val state = resolveRouteScreenClock(plan(), GeoPoint(40.7, -74.0), now) { _, _ -> null }
        assertNull(state.clock); assertNotNull(state.error); assertFalse(state.loading)
    }
}