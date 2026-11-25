package com.havamania

import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test

class TravelComparisonEvidenceTest {
    @Test fun `blank summary rejects invalid numeric fields`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -101.0, 66.0).forEach {
            val text = RecommendationEngine.getShortWeatherSummaryFromAnalysis(TravelWeatherAnalysis(averageTemperature = it, rainRiskPercent = 101))
            assertTrue(text, text.contains("sıcaklık bilinmiyor")); assertTrue(text, text.contains("Yağış olasılığı bilinmiyor"))
            assertFalse(text.contains("0°C")); assertFalse(text.contains("%101"))
        }
    }
    @Test fun `invalid score cannot produce improvement or overflow change in any tone`() {
        AssistantTone.entries.forEach { tone ->
            val text = TravelAiHelper.generateComparisonText(ForecastSnapshot(travelScore = Int.MIN_VALUE),
                ForecastSnapshot(travelScore = Int.MAX_VALUE), tone)
            assertFalse(text, text.contains("arttı") || text.contains("iyileş") || text.contains("düştü"))
            assertTrue(text, text.contains("karşılaştır", true))
            val invalid = TravelAiHelper.generateComparisonText(ForecastSnapshot(travelScore = -1), ForecastSnapshot(travelScore = 110), tone)
            assertFalse(invalid, invalid.contains("arttı") || invalid.contains("iyileş"))
        }
    }
    @Test fun `score rise never implies better weather and keeps known hazard`() {
        AssistantTone.entries.forEach { tone ->
            val text = TravelAiHelper.generateComparisonText(ForecastSnapshot(travelScore = 30),
                ForecastSnapshot(travelScore = 50, weatherCode = 95), tone)
            assertTrue(text, text.contains("fırtına", true))
            assertFalse(text, text.contains("koşullar daha iyi") || text.contains("Termal stabilite"))
        }
    }
    @Test fun `falling probability does not imply clear sky or cloud change`() {
        AssistantTone.entries.forEach { tone ->
            val text = TravelAiHelper.generateComparisonText(ForecastSnapshot(precipitationProbability = 80),
                ForecastSnapshot(precipitationProbability = 20), tone)
            assertFalse(text, text.contains("Hava açıyor") || text.contains("Bulut kapalılık"))
            assertTrue(text, text.contains("azaldı"))
        }
    }
    @Test fun `older incoming snapshot cannot be a new improvement`() {
        val text = TravelAiHelper.generateComparisonText(ForecastSnapshot(travelScore = 10, generatedAt = 200),
            ForecastSnapshot(travelScore = 90, generatedAt = 100))
        assertTrue(text, text.contains("eski")); assertFalse(text.contains("arttı"))
    }
    @Test fun `known zero values and explicit historical warning survive`() {
        val text = RecommendationEngine.getShortWeatherSummaryFromAnalysis(TravelWeatherAnalysis(averageTemperature = 0.0, rainRiskPercent = 0))
        assertTrue(text.contains("0°C")); assertTrue(text.contains("%0"))
        assertEquals("Fırtına bekleniyor.", RecommendationEngine.getShortWeatherSummaryFromAnalysis(TravelWeatherAnalysis(summary = "Fırtına bekleniyor.")))
    }
}