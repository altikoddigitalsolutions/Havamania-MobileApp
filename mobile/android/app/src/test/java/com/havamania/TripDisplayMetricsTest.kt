package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripDisplayMetricsTest {
    private val day = LocalDate.of(2026, 11, 26)
    private val old = TravelWeatherAnalysis(averageTemperature = 35.0, rainRiskPercent = 5, travelScore = 99, summary = "Harika hava")
    private fun plan(snapshot: ForecastSnapshot? = null, analysis: TravelWeatherAnalysis = old) =
        TravelPlan(city = "Ankara", startDate = day, endDate = day, lastForecastSnapshot = snapshot, analyses = listOf(analysis))
    @Test fun `snapshot is sole source when present`() {
        val result = tripDisplayMetrics(plan(ForecastSnapshot(averageTemperature = -2.0, precipitationProbability = 90, travelScore = 15)))
        assertEquals(TripDisplayMetrics(-2.0, 90, 15), result)
    }
    @Test fun `partial snapshot never borrows old analysis values`() {
        assertEquals(TripDisplayMetrics(null, null, null), tripDisplayMetrics(plan(ForecastSnapshot())))
        val text = tripDisplaySummary(plan(ForecastSnapshot()))
        assertFalse(text.contains("35")); assertFalse(text.contains("Harika")); assertTrue(text.contains("bilinmiyor"))
    }
    @Test fun `invalid snapshot fields remain unknown`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -101.0, 66.0).forEach {
            assertEquals(TripDisplayMetrics(null, null, null), tripDisplayMetrics(plan(ForecastSnapshot(
                averageTemperature = it, precipitationProbability = 101, travelScore = -1))))
        }
    }
    @Test fun `legacy analysis is validated when snapshot absent`() {
        assertEquals(TripDisplayMetrics(35.0, 5, 99), tripDisplayMetrics(plan()))
        assertEquals(TripDisplayMetrics(null, null, null), tripDisplayMetrics(plan(analysis = old.copy(
            averageTemperature = Double.NaN, rainRiskPercent = -1, travelScore = 101))))
    }
    @Test fun `real zero values are not treated as missing`() {
        assertEquals(TripDisplayMetrics(0.0, 0, 0), tripDisplayMetrics(plan(ForecastSnapshot(
            averageTemperature = 0.0, precipitationProbability = 0, travelScore = 0))))
    }
    @Test fun `snapshot hazard replaces old optimistic summary`() {
        val text = tripDisplaySummary(plan(ForecastSnapshot(weatherCode = 71)))
        assertTrue(text.contains("kar yağışı")); assertFalse(text.contains("Harika"))
    }
    @Test fun `new snapshot updates every displayed metric together`() {
        val first = plan(ForecastSnapshot(averageTemperature = 10.0, precipitationProbability = 80, travelScore = 30))
        val next = first.copy(lastForecastSnapshot = ForecastSnapshot(averageTemperature = 20.0, precipitationProbability = 0, travelScore = 85))
        assertEquals(TripDisplayMetrics(20.0, 0, 85), tripDisplayMetrics(next))
        assertFalse(tripDisplaySummary(next).contains("%80"))
    }
    @Test fun `legacy explicit hazard is preserved as recorded analysis`() {
        val text = tripDisplaySummary(plan(analysis = old.copy(summary = "Fırtına bekleniyor.")))
        assertEquals("Kayıtlı analiz: Fırtına bekleniyor.", text)
    }
}