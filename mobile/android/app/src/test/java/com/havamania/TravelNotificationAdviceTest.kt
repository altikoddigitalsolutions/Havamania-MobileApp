package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TravelNotificationAdviceTest {
    private val day = LocalDate.of(2026, 11, 26)
    private fun data(snapshot: ForecastSnapshot?): TravelNotificationData {
        val plan = TravelPlan(city = "Ankara", startDate = day, endDate = day)
        val bound = snapshot?.copy(timezone = "Europe/Istanbul", binding = createTripForecastBinding(plan, day, day))
        return buildTravelNotificationData(plan.copy(lastForecastSnapshot = bound), 1, java.time.Instant.parse("2026-11-26T12:00:00Z"))
    }
    @Test fun `missing weather does not invent equipment or measurements`() {
        val result = data(null)
        assertTrue(result.recommendedItems.isEmpty()); assertNull(result.minTemp); assertNull(result.maxTemp)
    }
    @Test fun `nonfinite and out of range fields cannot reach notification payload`() {
        val result = data(ForecastSnapshot(minTemp = Double.NEGATIVE_INFINITY, maxTemp = Double.NaN,
            precipitationProbability = 101, windSpeed = Double.POSITIVE_INFINITY, uvIndex = 100.0))
        assertNull(result.minTemp); assertNull(result.maxTemp); assertNull(result.rainProbability)
        assertFalse(result.windRisk.orEmpty().contains("2147483647")); assertTrue(result.recommendedItems.isEmpty())
    }
    @Test fun `known rain code produces rain gear without probability`() {
        assertTrue(data(ForecastSnapshot(weatherCode = 61)).recommendedItems.contains("Yağmurluk"))
    }
    @Test fun `snow and freezing minima survive mild maximum`() {
        assertTrue(data(ForecastSnapshot(weatherCode = 71, minTemp = -2.0, maxTemp = 20.0)).recommendedItems.contains("Kaymayan ayakkabı"))
    }
    @Test fun `wind alone produces wind protection`() {
        assertTrue(data(ForecastSnapshot(windSpeed = 50.0)).recommendedItems.contains("Rüzgarlık"))
    }
    @Test fun `temperature alone does not invent UV protection`() {
        assertFalse(data(ForecastSnapshot(maxTemp = 35.0)).recommendedItems.any { it.equals("Güneş kremi", ignoreCase = true) })
        assertTrue(data(ForecastSnapshot(maxTemp = 10.0, uvIndex = 8.0)).recommendedItems.contains("Güneş kremi"))
    }
    @Test fun `concurrent hazards keep every preparation category`() {
        val items = data(ForecastSnapshot(minTemp = -2.0, maxTemp = 10.0, precipitationProbability = 80,
            windSpeed = 50.0, uvIndex = 8.0)).recommendedItems
        listOf("Yağmurluk", "Kaymayan ayakkabı", "Rüzgarlık", "Güneş kremi").forEach { assertTrue(items.toString(), it in items) }
        assertEquals(items.distinct(), items)
    }
}