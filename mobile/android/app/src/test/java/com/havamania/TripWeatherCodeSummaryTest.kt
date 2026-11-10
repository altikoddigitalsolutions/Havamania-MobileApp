package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TripWeatherCodeSummaryTest {
    private val date = LocalDate.of(2026, 11, 1)
    private fun snapshot(codes: List<Int>): ForecastSnapshot {
        val daily = DailyDto(time = codes.indices.map { date.plusDays(it.toLong()).toString() },
            weatherCode = codes, tempMin = codes.map { 15.0 }, tempMax = codes.map { 24.0 },
            windSpeedMax = codes.map { 10.0 }, precipProbMax = codes.map { 10 })
        return buildTripForecastSnapshot(daily, date, date.plusDays(codes.size.toLong() - 1))!!
    }

    @Test fun oneStormDayOverridesMajorityClearDays() {
        val result = snapshot(listOf(0, 0, 0, 0, 95, 0, 0))
        assertEquals(95, result.weatherCode)
        assertTrue(result.conditionSummary!!.contains("fırtına"))
    }

    @Test fun differentHazardsAreAllPreservedAndDuplicateLabelsAreCollapsed() {
        val result = snapshot(listOf(0, 56, 57, 71, 95, 45))
        val text = result.conditionSummary!!
        assertTrue(text.contains("buzlanma"))
        assertTrue(text.contains("Kar yağışı"))
        assertTrue(text.contains("fırtına"))
        assertTrue(text.contains("Sis"))
        assertEquals(1, Regex("Donan yağış").findAll(text).count())
    }

    @Test fun cautionWeatherAlsoSurvivesSunnyMajorityAndClearWeatherKeepsItsMode() {
        assertEquals(45, snapshot(listOf(0, 0, 45)).weatherCode)
        assertEquals(61, snapshot(listOf(0, 61, 0)).weatherCode)
        assertEquals(0, snapshot(listOf(0, 0, 3)).weatherCode)
        assertNull(tripWeatherWarning(snapshot(listOf(0, 0, 3))))
    }

    @Test fun allAssistantTonesKeepSnowAndFreezingWarningsInsteadOfIdealWeather() {
        val forecast = snapshot(listOf(0, 0, 56, 71))
        AssistantTone.entries.forEach { tone ->
            val reply = TravelAiHelper.generateTravelAiSuggestion("Ankara", TripType.VACATION,
                forecast, null, 2, tone = tone)
            val weather = reply.substringBefore("[SEP]")
            assertTrue(tone.name, weather.contains("buzlanma"))
            assertTrue(tone.name, weather.contains("kar yağışı"))
            assertFalse(tone.name, weather.contains("harika"))
            assertFalse(tone.name, weather.contains("elverişli"))
        }
    }

    @Test fun summaryIsStableWhenProviderDateOrderChanges() {
        val codes = listOf(0, 61, 71, 95, 56, 0)
        assertEquals(orderedTripWeatherCodes(codes), orderedTripWeatherCodes(codes.reversed()))
    }
}
