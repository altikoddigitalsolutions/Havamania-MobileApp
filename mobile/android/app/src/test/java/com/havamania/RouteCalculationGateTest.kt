package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RouteCalculationGateTest {
    private val day = LocalDate.of(2026, 11, 26)
    private fun plan() = TravelPlan(id = "trip", userId = "A", city = "Ankara", startDate = day, endDate = day,
        latitude = 39.9, longitude = 32.8, originLatitude = 41.0, originLongitude = 29.0, departureTime = "09:00")
    @Test fun `first input calculates and identical emission does not`() {
        val gate = RouteCalculationGate(); val value = plan()
        assertTrue(gate.shouldRecalculate(value)); assertFalse(gate.shouldRecalculate(value.copy()))
    }
    @Test fun `metadata and destination forecast updates keep route and analysis`() {
        val gate = RouteCalculationGate(); val value = plan(); gate.shouldRecalculate(value)
        assertFalse(gate.shouldRecalculate(value.copy(userNote = "Not", userRating = 5, updatedAt = value.updatedAt + 1,
            lastDailyNotificationDate = "2026-11-25", weatherSummary = "Yeni hava", lastForecastSnapshot = ForecastSnapshot(timezone = "Asia/Tokyo"))))
    }
    @Test fun `geometry dates and departure changes invalidate`() {
        val value = plan()
        listOf(value.copy(latitude = 40.0), value.copy(longitude = 33.0), value.copy(originLatitude = 42.0),
            value.copy(originLongitude = 30.0), value.copy(startDate = day.plusDays(1)), value.copy(endDate = day.plusDays(1)),
            value.copy(departureTime = "10:00")).forEach { changed ->
            val gate = RouteCalculationGate(); gate.shouldRecalculate(value); assertTrue(gate.shouldRecalculate(changed))
        }
    }
    @Test fun `owner and trip changes invalidate`() {
        val gate = RouteCalculationGate(); val value = plan(); gate.shouldRecalculate(value)
        assertTrue(gate.shouldRecalculate(value.copy(userId = "B")))
        assertTrue(gate.shouldRecalculate(value.copy(id = "other")))
    }
    @Test fun `explicit retry resets completed or failed input`() {
        val gate = RouteCalculationGate(); val value = plan(); gate.shouldRecalculate(value); gate.reset()
        assertTrue(gate.shouldRecalculate(value)); assertFalse(gate.shouldRecalculate(value))
    }
    @Test fun `changing from device origin to selected origin invalidates`() {
        val gate = RouteCalculationGate(); val value = plan().copy(originLatitude = null, originLongitude = null)
        gate.shouldRecalculate(value); assertTrue(gate.shouldRecalculate(plan()))
    }
}