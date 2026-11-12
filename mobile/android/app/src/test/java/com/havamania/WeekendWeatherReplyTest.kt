package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WeekendWeatherReplyTest {
    private val friday = LocalDate.parse("2026-11-13")
    private val sat = DailyForecast("Yanlış etiket", "2026-11-14", 10, 20, 10, weatherCode = 0)
    private val sun = DailyForecast("Yanlış etiket", "2026-11-15", 10, 20, 80, weatherCode = 0)
    private fun reply(days: List<DailyForecast>, today: LocalDate = friday, stale: Boolean = false) =
        weekendWeatherReply(WeatherData(dailyForecast = days, isStale = stale), today)

    @Test fun `dates override labels and wetter Sunday is not selected by default`() {
        val text = reply(listOf(sat, sun))
        assertTrue(text, text.contains("Pazar için risk"))
        assertFalse(text.contains("Pazar günü daha uygun"))
    }
    @Test fun `different weekends and duplicate dates never form a pair`() {
        assertTrue(reply(listOf(sat, sun.copy(date = "2026-11-22"))).contains("veri yetersiz"))
        assertTrue(reply(listOf(sat, sat, sun)).contains("veri yetersiz"))
    }
    @Test fun `two risky days and equal chances do not force a winner`() {
        assertTrue(reply(listOf(sat.copy(weatherCode = 95), sun)).contains("İki günde"))
        assertTrue(reply(listOf(sat, sun.copy(precipitationProbability = 10))).contains("eşit"))
    }
    @Test fun `missing chance and stale records do not pick a day`() {
        assertTrue(reply(listOf(sat.copy(precipitationProbability = null), sun)).contains("eksik"))
        assertTrue(reply(listOf(sat, sun), stale = true).contains("güncel değil"))
    }
    @Test fun `Sunday does not recommend elapsed Saturday`() {
        val text = reply(listOf(sat, sun), friday.plusDays(2))
        assertTrue(text.contains("Bugünkü pazar"))
        assertFalse(text.contains("Cumartesinin saatlik"))
    }
    @Test fun `travel fallback never promises good weather in any tone`() {
        AssistantTone.entries.forEach { tone ->
            val text = RecommendationEngine.generateAssistantFallbackReply("Ankara seyahat", WeatherData(cityName = "Bursa"), tone = tone)
            assertTrue(text, text.contains("güzergâh analizi"))
            assertFalse(text.contains("elverişli"))
            assertFalse(text.contains("hava uygun"))
        }
    }
}
