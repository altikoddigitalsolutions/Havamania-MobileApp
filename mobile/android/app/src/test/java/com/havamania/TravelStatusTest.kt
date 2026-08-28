package com.havamania

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TravelStatusTest {

    private val today = LocalDate.of(2026, 8, 28)

    @Test
    fun testPastTrips() {
        // 25-25 Aug -> PAST
        assertEquals(TravelStatus.PAST, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 25), today))
        // 25-27 Aug -> PAST
        assertEquals(TravelStatus.PAST, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 27), today))
    }

    @Test
    fun testOngoingTrips() {
        // 25-28 Aug -> ONGOING (Ends today)
        assertEquals(TravelStatus.ONGOING, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 8, 28), today))
        // 28-28 Aug -> ONGOING (Starts and ends today)
        assertEquals(TravelStatus.ONGOING, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 28), today))
        // 28-30 Aug -> ONGOING (Starts today)
        assertEquals(TravelStatus.ONGOING, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 8, 30), today))
    }

    @Test
    fun testUpcomingTrips() {
        // 29-31 Aug -> UPCOMING
        assertEquals(TravelStatus.UPCOMING, TravelStatusResolver.getStatus(LocalDate.of(2026, 8, 29), LocalDate.of(2026, 8, 31), today))
    }
}
