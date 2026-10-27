package com.havamania

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class RouteWeatherWindowTest {
    private val now = Instant.parse("2026-10-27T10:00:00Z")

    @Test fun exactBoundaryAccepts48HoursButRejectsOneNanosecondMore() {
        assertTrue(TravelStatusResolver.isWithinRouteWeatherWindow(now.plusSeconds(48 * 3600), now))
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(now.plusSeconds(48 * 3600).plusNanos(1), now))
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(now.plusSeconds(48 * 3600 + 59 * 60), now))
    }

    @Test fun departureNowIsAllowedButPastSecondsAreNot() {
        assertTrue(TravelStatusResolver.isWithinRouteWeatherWindow(now, now))
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(now.minusNanos(1), now))
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(now.minusSeconds(59 * 60), now))
    }

    @Test fun daylightSavingUsesElapsedTimeRatherThanLocalHourDifference() {
        val zone = ZoneId.of("Europe/Berlin")
        val departure = LocalDateTime.of(2026, 10, 26, 12, 0).atZone(zone).toInstant()
        val before = LocalDateTime.of(2026, 10, 24, 12, 0).atZone(zone).toInstant()
        assertEquals(49L, Duration.between(before, departure).toHours())
        assertFalse(TravelStatusResolver.isWithinRouteWeatherWindow(departure, before))
        assertTrue(TravelStatusResolver.isWithinRouteWeatherWindow(departure, before.plusSeconds(3600)))
    }

    @Test fun upcomingTripUsesSameWindowWhileOngoingAccessIsPreserved() {
        val date = LocalDate.of(2026, 10, 29)
        val upcoming = TravelPlan(city = "Ankara", startDate = date, endDate = date, departureTime = "10:01")
        assertFalse(TravelStatusResolver.isRouteWeatherEligible(upcoming, now, ZoneOffset.UTC))
        assertTrue(TravelStatusResolver.isRouteWeatherEligible(upcoming.copy(departureTime = "10:00"), now, ZoneOffset.UTC))
        val ongoing = upcoming.copy(startDate = LocalDate.of(2026, 10, 27), departureTime = "09:00")
        assertTrue(TravelStatusResolver.isRouteWeatherEligible(ongoing, now, ZoneOffset.UTC))
        assertFalse(TravelStatusResolver.isRouteWeatherEligible(upcoming.copy(departureTime = null), now, ZoneOffset.UTC))
        assertFalse(TravelStatusResolver.isRouteWeatherEligible(ongoing.copy(endDate = LocalDate.of(2026, 10, 26)), now, ZoneOffset.UTC))
    }
}
