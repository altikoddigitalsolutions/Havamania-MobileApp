package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TravelAdviceEvidenceTest {
    private val forecast = ForecastSnapshot(minTemp = 25.0, maxTemp = 35.0, weatherCode = 3,
        conditionSummary = "Bulutlu", precipitationProbability = 10, windSpeed = 10.0)

    @Test fun hotTemperatureDoesNotInventSunOrUvInAnyTone() {
        AssistantTone.entries.forEach { tone ->
            val weather = TravelAiHelper.generateTravelAiSuggestion("Ankara", TripType.VACATION,
                forecast, null, 2, tone = tone).substringBefore("[SEP]")
            assertFalse(tone.name, weather.contains("güneş", ignoreCase = true))
            assertFalse(tone.name, weather.contains("açık", ignoreCase = true))
            assertFalse(tone.name, weather.contains("UV"))
        }
    }

    @Test fun uvNoticeRequiresActualUvDataAndDoesNotDependOnPersonalization() {
        val weather = TravelAiHelper.generateTravelAiSuggestion("Ankara", TripType.VACATION,
            forecast.copy(uvIndex = 8.0), null, 2,
            personalization = PersonalizationProfile(personalizationEnabled = false)).substringBefore("[SEP]")
        assertTrue(weather.contains("UV indeksi: 8.0"))
        val missing = TravelAiHelper.generateTravelAiSuggestion("Ankara", TripType.VACATION,
            forecast.copy(uvIndex = Double.NaN), null, 2).substringBefore("[SEP]")
        assertFalse(missing.contains("UV"))
    }

    @Test fun historicalAnalysisCountIsNotInventedWeatherDayCount() {
        val day = LocalDate.of(2026, 10, 1)
        val plan = TravelPlan(city = "Ankara", startDate = day, endDate = day.plusDays(1),
            analyses = listOf(TravelWeatherAnalysis(averageTemperature = 35.0, rainRiskPercent = null, summary = "Kaydedilen tahmin"),
                TravelWeatherAnalysis(averageTemperature = 35.0, rainRiskPercent = null, summary = "Kaydedilen tahmin")))
        val summary = TravelAiHelper.generateHistorySummary(plan)
        assertEquals(-1, summary.sunnyDays)
        assertEquals(-1, summary.rainyDays)
        assertFalse(summary.summaryText.contains("güneşli"))
        assertTrue(summary.summaryText.contains("yağış olasılığı bilgisi bulunmuyor"))
    }
}
