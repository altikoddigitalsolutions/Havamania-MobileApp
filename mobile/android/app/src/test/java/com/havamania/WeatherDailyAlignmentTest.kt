package com.havamania

import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class WeatherDailyAlignmentTest {
    private val now = Instant.parse("2026-11-12T22:00:00Z") // 13 November in Istanbul.
    private val daily = DailyDto(time = listOf("2026-11-12", "2026-11-13"),
        weatherCode = listOf(0, 0), tempMin = listOf(1.0, 10.0), tempMax = listOf(8.0, 20.0),
        precipProbMax = listOf(90, 5), uvIndexMax = listOf(8.0, 2.0))
    private fun map(days: DailyDto) = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0,
        timezone = "Europe/Istanbul", daily = days), "Bursa", now = now)

    @Test fun `hero uses city local today rather than first entry`() {
        val value = map(daily)
        assertEquals("20°", value.high)
        assertEquals("10°", value.low)
        assertEquals(5, value.precipitationProbability)
        assertEquals(2, value.uvIndex)
        assertTrue(value.dailyForecast.last().isToday)
        assertEquals(2, value.dailyForecast.size)
    }
    @Test fun `absent or duplicate today does not borrow another day`() {
        listOf(daily.copy(time = listOf("2026-11-14", "2026-11-15")),
            daily.copy(time = listOf("2026-11-13", "2026-11-13"))).forEach {
            val value = map(it)
            assertEquals("--", value.high)
            assertNull(value.precipitationProbability)
            assertNull(value.uvIndex)
        }
    }
    @Test fun `invalid daily UV does not become integer observation`() {
        val value = map(daily.copy(uvIndexMax = listOf(1.0, Double.POSITIVE_INFINITY)))
        assertNull(value.uvIndex)
        assertEquals("Veri yok", value.details.first { it.title == "UV İndeksi" }.value)
    }
    @Test fun `legacy cached score is neutralized without deleting measurements`() {
        val old = Json.decodeFromString<WeatherData>("""{"cityName":"Bursa","temperature":"18°","weatherSuitabilityScore":100,"weatherSuitabilityText":"Açık hava için uygun"}""")
        val normalized = normalizeCachedWeatherAssessment(old)
        assertEquals(-1, normalized.weatherSuitabilityScore)
        assertEquals("18°", normalized.temperature)
        assertEquals("Bursa", normalized.cityName)
        assertTrue(normalized.weatherSuitabilityText.contains("güncellenmeli"))
        val current = map(daily)
        assertEquals(current, normalizeCachedWeatherAssessment(current))
    }
}
