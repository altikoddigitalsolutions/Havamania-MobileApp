package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TripAnalysisDisplayTest {
    private val now = Instant.parse("2026-11-25T10:00:00Z")
    private val snapshot = ForecastSnapshot(generatedAt = now.minusSeconds(3600).toEpochMilli(), timezone = "Europe/Istanbul")
    private fun plan(status: TravelWeatherAnalysisStatus, cached: Boolean = true) = TravelPlan(city = "Ankara", startDate = java.time.LocalDate.of(2026, 11, 26), endDate = java.time.LocalDate.of(2026, 11, 27),
        weatherAnalysisStatus = status, lastForecastSnapshot = snapshot.takeIf { cached })
    @Test fun `partial refresh visibly labels retained data and timestamp`() {
        val text = tripAnalysisDisplayLabel(plan(TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY), now)
        assertTrue(text, text.contains("son kayıtlı")); assertTrue(text, text.contains("eksik veya yenilenemedi"))
        assertTrue(text, text.contains("25 Kasım 2026 12:00"))
    }
    @Test fun `failed refresh does not hide previous day warning`() {
        val value = plan(TravelWeatherAnalysisStatus.WEATHER_FAILED).copy(lastForecastSnapshot = snapshot.copy(generatedAt = now.minusSeconds(86400).toEpochMilli()))
        val text = tripAnalysisDisplayLabel(value, now)
        assertTrue(text.contains("yenilenemedi")); assertTrue(text.contains("Önceki güne ait"))
    }
    @Test fun `loading with cache differs from first load`() {
        assertTrue(tripAnalysisDisplayLabel(plan(TravelWeatherAnalysisStatus.LOADING), now).contains("son kayıtlı"))
        assertFalse(tripAnalysisDisplayLabel(plan(TravelWeatherAnalysisStatus.LOADING, false), now).contains("son kayıtlı"))
    }
    @Test fun `ready update removes refresh failure warning`() {
        val old = plan(TravelWeatherAnalysisStatus.WEATHER_PARTIAL_READY)
        assertTrue(tripAnalysisDisplayLabel(old, now).contains("yenilenemedi"))
        val updated = old.copy(weatherAnalysisStatus = TravelWeatherAnalysisStatus.WEATHER_READY_ANALYSIS_READY,
            lastForecastSnapshot = snapshot.copy(generatedAt = now.toEpochMilli()))
        val text = tripAnalysisDisplayLabel(updated, now)
        assertFalse(text.contains("yenilenemedi")); assertTrue(text.contains("13:00"))
    }
    @Test fun `first failure cannot claim cached data`() {
        val text = tripAnalysisDisplayLabel(plan(TravelWeatherAnalysisStatus.WEATHER_FAILED, false), now)
        assertTrue(text.contains("alınamadı")); assertFalse(text.contains("son kayıtlı"))
    }
    @Test fun `assistant failure and unknown timestamp are explicit`() {
        val text = tripAnalysisDisplayLabel(plan(TravelWeatherAnalysisStatus.WEATHER_READY_AI_FAILED, false), now)
        assertTrue(text.contains("asistan analizi tamamlanamadı")); assertTrue(text.contains("zamanı bilinmiyor"))
    }
}