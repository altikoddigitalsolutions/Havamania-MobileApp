package com.havamania

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelRouteForecastSummaryTest {
    private val clear = WaypointWeather(0, 20.0, null, null, null, null, RouteRisk.OK, null)

    @Test
    fun incompleteForecastDoesNotReportClearRoute() {
        val summary = requireNotNull(travelRouteForecastSummary(listOf(clear, null)))
        assertTrue(summary.contains("tahmin alınamadı"))
        assertFalse(summary.contains("önemli bir hava riski görülmedi"))
        assertNull(travelRouteForecastSummary(listOf(null, null)))
    }

    @Test
    fun incompleteForecastStillReportsDanger() {
        val danger = clear.copy(risk = RouteRisk.DANGER)
        val summary = requireNotNull(travelRouteForecastSummary(listOf(clear, danger, null)))
        assertTrue(summary.contains("1 noktada tehlikeli"))
        assertTrue(summary.contains("tahmin alınamadı"))
    }

    @Test
    fun unavailableForecastGetsAtMostTwoRetries() {
        assertTrue(shouldRetryRouteForecast(unavailable = true, attempt = 0))
        assertTrue(shouldRetryRouteForecast(unavailable = true, attempt = 1))
        assertFalse(shouldRetryRouteForecast(unavailable = true, attempt = 2))
        assertFalse(shouldRetryRouteForecast(unavailable = false, attempt = 0))
    }
}
