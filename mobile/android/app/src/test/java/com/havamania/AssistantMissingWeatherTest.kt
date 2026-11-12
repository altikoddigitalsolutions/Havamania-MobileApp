package com.havamania

import com.havamania.ui.theme.AssistantTone
import org.junit.Assert.*
import org.junit.Test

class AssistantMissingWeatherTest {
    private val weather = WeatherData(cityName = "Bursa", temperature = "20°", weatherCode = 0, feelsLike = "18°",
        windSpeed = 5.0, humidity = 50, precipitationProbability = 0, uvIndex = 1)
    private fun reply(prompt: String, value: WeatherData = weather, tone: AssistantTone = AssistantTone.DENGELI) =
        RecommendationEngine.generateAssistantFallbackReply(prompt, value, tone = tone)

    @Test fun `clothing and activity do not fill missing observations in any tone`() {
        AssistantTone.entries.forEach { tone ->
            listOf("Bugün ne giymeliyim?", "Dışarı çıkmak uygun mu?").forEach { prompt ->
                val text = reply(prompt, weather.copy(temperature = "--", windSpeed = null), tone)
                assertTrue("$tone $text", text.contains("eksik"))
                assertFalse(text.contains("20°C"))
                assertFalse(text.contains("hiçbir engel"))
            }
        }
    }
    @Test fun `clothing uses observed apparent temperature`() {
        val text = reply("Bugün ne giymeliyim?", weather.copy(feelsLike = "7°"), AssistantTone.DETAYLI_UZMAN)
        assertTrue(text, text.contains("7°C"))
        assertFalse(text.contains("Termal algı 20°C"))
    }
    @Test fun `missing rain chance never means no rain`() {
        AssistantTone.entries.forEach { tone ->
            val text = reply("Bugün yağmur yağacak mı?", weather.copy(precipitationProbability = null), tone)
            assertTrue(text, text.contains("bulunmuyor"))
            assertFalse(text.contains("%0"))
            assertFalse(text.contains("gerek yok"))
        }
    }
    @Test fun `rain probability is never certainty`() {
        AssistantTone.entries.forEach { tone ->
            val high = reply("Bugün yağmur yağacak mı?", weather.copy(precipitationProbability = 90), tone)
            assertFalse(high.contains("kaçınılmaz"))
            val low = reply("Bugün yağmur yağacak mı?", weather, tone)
            assertFalse(low.contains("gerek yok"))
            assertFalse(low.contains("yağmur beklemiyoruz"))
        }
    }
    @Test fun `other city and future date do not reuse current measurements`() {
        val other = reply("Ankara hava nasıl?")
        assertTrue(other, other.contains("Mevcut veriler Bursa"))
        val future = reply("Yarın ne giymeliyim?")
        assertTrue(future, future.contains("ilgili günün tahminini"))
    }
    @Test fun `high UV prevents ideal activity verdict`() {
        val text = reply("Dışarı çıkmak uygun mu?", weather.copy(uvIndex = 10))
        assertFalse(text, text.contains("oldukça uygun"))
        assertTrue(text, text.contains("UV"))
    }
    @Test fun `today question uses observation rather than daily midpoint`() {
        val date = java.time.LocalDate.now().toString()
        val value = weather.copy(temperature = "8°", dailyForecast = listOf(
            DailyForecast(day = "Bugün", date = date, minTemp = 20, maxTemp = 30)))
        val text = reply("Bugün ne giymeliyim?", value, AssistantTone.KISA_NET)
        assertTrue(text, text.contains("8°"))
        assertFalse(text.contains("25°"))
    }
}
