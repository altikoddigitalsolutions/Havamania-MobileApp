package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripDisplayMetricsTest {
    private val day = LocalDate.of(2026, 11, 26)
    private val old = TravelWeatherAnalysis(averageTemperature = 35.0, rainRiskPercent = 5, travelScore = 99, summary = "Harika hava")
    private val now = java.time.Instant.parse("2026-11-26T12:00:00Z")
    private fun metrics(plan: TravelPlan) = tripDisplayMetrics(plan, now)
    private fun summary(plan: TravelPlan) = tripDisplaySummary(plan, now)
    private fun plan(snapshot: ForecastSnapshot? = null, analysis: TravelWeatherAnalysis = old): TravelPlan {
        val plan = TravelPlan(city = "Ankara", startDate = day, endDate = day, analyses = listOf(analysis))
        return plan.copy(lastForecastSnapshot = snapshot?.copy(timezone = "Europe/Istanbul", binding = createTripForecastBinding(plan, day, day)))
    }
    @Test fun `snapshot is sole source when present`() {
        val result = metrics(plan(ForecastSnapshot(averageTemperature = -2.0, precipitationProbability = 90, travelScore = 15)))
        assertEquals(TripDisplayMetrics(-2.0, 90, 15), result)
    }
    @Test fun `partial snapshot never borrows old analysis values`() {
        assertEquals(TripDisplayMetrics(null, null, null), metrics(plan(ForecastSnapshot())))
        val text = summary(plan(ForecastSnapshot()))
        assertFalse(text.contains("35")); assertFalse(text.contains("Harika")); assertTrue(text.contains("bilinmiyor"))
    }
    @Test fun `invalid snapshot fields remain unknown`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -101.0, 66.0).forEach {
            assertEquals(TripDisplayMetrics(null, null, null), metrics(plan(ForecastSnapshot(
                averageTemperature = it, precipitationProbability = 101, travelScore = -1))))
        }
    }
    @Test fun `legacy analysis cannot become current metrics`() {
        assertEquals(TripDisplayMetrics(null, null, null), metrics(plan()))
        assertEquals(TripDisplayMetrics(null, null, null), metrics(plan(analysis = old.copy(
            averageTemperature = Double.NaN, rainRiskPercent = -1, travelScore = 101))))
    }
    @Test fun `real zero values are not treated as missing`() {
        assertEquals(TripDisplayMetrics(0.0, 0, 0), metrics(plan(ForecastSnapshot(
            averageTemperature = 0.0, precipitationProbability = 0, travelScore = 0))))
    }
    @Test fun `snapshot hazard replaces old optimistic summary`() {
        val text = summary(plan(ForecastSnapshot(weatherCode = 71)))
        assertTrue(text.contains("kar yağışı")); assertFalse(text.contains("Harika"))
    }
    @Test fun `new snapshot updates every displayed metric together`() {
        val first = plan(ForecastSnapshot(averageTemperature = 10.0, precipitationProbability = 80, travelScore = 30))
        val next = first.copy(lastForecastSnapshot = ForecastSnapshot(averageTemperature = 20.0, precipitationProbability = 0, travelScore = 85, timezone = "Europe/Istanbul", binding = first.lastForecastSnapshot!!.binding))
        assertEquals(TripDisplayMetrics(20.0, 0, 85), metrics(next))
        assertFalse(summary(next).contains("%80"))
    }
    @Test fun `legacy explicit hazard stays outside current summary`() {
        val text = summary(plan(analysis = old.copy(summary = "Fırtına bekleniyor.")))
        assertEquals(UNVERIFIED_TRIP_FORECAST_TEXT, text)
        assertFalse(text.contains("Fırtına"))
    }
}