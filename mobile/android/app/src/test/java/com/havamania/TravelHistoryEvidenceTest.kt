package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TravelHistoryEvidenceTest {
    private val day = LocalDate.of(2026, 10, 1)
    private fun history(vararg items: TravelWeatherAnalysis) = TravelAiHelper.generateHistorySummary(
        TravelPlan(city = "Ankara", startDate = day, endDate = day.plusDays(1), analyses = items.toList()))
    @Test fun `invalid temperature never becomes zero or extreme integer`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -101.0, 66.0).forEach {
            val result = history(TravelWeatherAnalysis(averageTemperature = it, summary = "Kayıtlı"))
            assertNull(result.averageTemp); assertFalse(result.summaryText.contains("0°C"))
            assertTrue(result.summaryText.contains("sıcaklık bilgisi bulunmuyor"))
        }
    }
    @Test fun `invalid score and probability remain unknown`() {
        val result = history(TravelWeatherAnalysis(travelScore = 101, rainRiskPercent = -1, summary = "Kayıtlı"))
        assertNull(result.comfortScore)
        assertFalse(result.summaryText.contains("%-1")); assertTrue(result.summaryText.contains("yağış olasılığı bilgisi bulunmuyor"))
    }
    @Test fun `valid numerical analysis survives empty summary`() {
        val result = history(TravelWeatherAnalysis(averageTemperature = 18.0, rainRiskPercent = 0, travelScore = 75))
        assertEquals(18, result.averageTemp); assertEquals(75, result.comfortScore)
        assertFalse(result.summaryText.contains("analizi bulunmuyor"))
    }
    @Test fun `forecast revision averages are not observed temperature extremes`() {
        val result = history(TravelWeatherAnalysis(averageTemperature = 12.0, summary = "A"),
            TravelWeatherAnalysis(averageTemperature = 25.0, summary = "B"))
        assertNull(result.minTemp); assertNull(result.maxTemp)
        assertEquals(-1, result.rainyDays); assertEquals(-1, result.sunnyDays)
    }
    @Test fun `latest timestamp wins over list arrival order`() {
        val newer = TravelWeatherAnalysis(createdAt = 200, averageTemperature = 10.0, summary = "Yeni")
        val older = TravelWeatherAnalysis(createdAt = 100, averageTemperature = 30.0, summary = "Eski")
        assertEquals(10, history(newer, older).averageTemp)
        assertEquals(history(newer, older), history(older, newer))
    }
    @Test fun `missing history is not a measured zero score`() {
        val result = history()
        assertNull(result.averageTemp); assertNull(result.comfortScore)
    }
    @Test fun `new partial history never borrows older temperature`() {
        val result = history(TravelWeatherAnalysis(createdAt = 100, averageTemperature = 20.0, summary = "Eski"),
            TravelWeatherAnalysis(createdAt = 200, averageTemperature = Double.NaN, rainRiskPercent = 80, summary = "Yeni"))
        assertNull(result.averageTemp); assertTrue(result.summaryText.contains("%80"))
        assertFalse(result.summaryText.contains("20°C"))
    }
}