package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TravelNotificationTextTest {
    private val day = LocalDate.of(2026, 11, 26)
    private val now = java.time.Instant.parse("2026-11-27T12:00:00Z")
    private fun plan(snapshot: ForecastSnapshot? = null): TravelPlan {
        val plan = TravelPlan(city = "Ankara", startDate = day, endDate = day.plusDays(3))
        return plan.copy(lastForecastSnapshot = snapshot?.copy(timezone = "Europe/Istanbul", binding = createTripForecastBinding(plan, plan.startDate, plan.endDate)))
    }
    private fun text(snapshot: ForecastSnapshot?, days: Int = 0, ongoing: Boolean = true) =
        plan(snapshot).let { buildTravelNotificationText(it, days, ongoing, buildTravelNotificationData(it, days, now), day.plusDays(1)).second }
    @Test fun `partial temperatures never render null degrees`() {
        listOf(ForecastSnapshot(conditionSummary = "Bulutlu"), ForecastSnapshot(conditionSummary = "Bulutlu", maxTemp = 20.0)).forEach {
            listOf(true, false).forEach { ongoing -> assertFalse(text(it, ongoing = ongoing).contains("null")) }
        }
    }
    @Test fun `period snapshot is never described as todays measured weather`() {
        val result = text(ForecastSnapshot(conditionSummary = "Yağmurlu", minTemp = 10.0, maxTemp = 20.0))
        assertFalse(result.contains("Bugün hava")); assertTrue(result.contains("dönemi tahmini"))
    }
    @Test fun `missing forecast cannot claim analysis ready tomorrow or later`() {
        listOf(1, 5).forEach { days ->
            val result = text(null, days, false)
            assertFalse(result, result.contains("hazır")); assertTrue(result, result.contains("eksik"))
        }
    }
    @Test fun `valid temperature survives missing condition summary`() {
        val result = text(ForecastSnapshot(minTemp = 10.0, maxTemp = 20.0))
        assertTrue(result, result.contains("10°C") && result.contains("20°C"))
    }
    @Test fun `ongoing day uses supplied forecast location date`() {
        val result = text(ForecastSnapshot())
        assertTrue(result, result.contains("Gün: 2"))
    }
    @Test fun `comparison presence alone cannot claim changed weather`() {
        val value = plan().copy(analyses = listOf(TravelWeatherAnalysis(comparisonText = "Karşılaştırma için veri eksik")))
        val result = buildTravelNotificationText(value, 3, false, buildTravelNotificationData(value, 3, now), day).second
        assertFalse(result, result.contains("değişiklikler var"))
    }
}