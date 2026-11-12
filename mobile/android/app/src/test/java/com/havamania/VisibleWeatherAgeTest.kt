package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class VisibleWeatherAgeTest {
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val data = WeatherData(timestamp = now.toEpochMilli(), mappingVersion = 1,
        weatherCode = 0, temperature = "20°", high = "25°", precipitationProbability = 5,
        weatherSuitabilityScore = 100, weatherSuitabilityText = "Uygun")
    @Test fun `visible data expires once and aging is idempotent`() {
        assertEquals(data, ageVisibleWeather(data, now.plusSeconds(60)))
        val aged = ageVisibleWeather(data, now.plusSeconds(901))
        assertTrue(aged.isStale)
        assertEquals(-1, aged.weatherSuitabilityScore)
        assertEquals(aged, ageVisibleWeather(aged, now.plusSeconds(931)))
    }
    @Test fun `visible local day rollover removes yesterday fields`() {
        val value = data.copy(timezone = "Europe/Istanbul", timestamp = Instant.parse("2026-11-12T20:59:00Z").toEpochMilli())
        val aged = ageVisibleWeather(value, Instant.parse("2026-11-12T21:01:00Z"))
        assertTrue(aged.isStale)
        assertEquals("--", aged.high)
        assertNull(aged.precipitationProbability)
    }
    @Test fun `unknown condition cannot produce complete suitability or ideal recommendation`() {
        val daily = DailyDto(time = listOf("2026-11-12"), weatherCode = listOf(0), tempMin = listOf(10.0),
            tempMax = listOf(25.0), precipProbMax = listOf(0))
        val mapped = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = daily,
            current = CurrentWeatherDto(temperature = 20.0, windSpeed = 5.0, uvIndex = 1.0, visibility = 10000.0)),
            "Bursa", now = now)
        assertEquals(-1, mapped.weatherSuitabilityScore)
        val reply = RecommendationEngine.generateTodayRecommendation(data.copy(weatherCode = -1,
            windSpeed = 5.0, uvIndex = 1)).message
        assertTrue(reply, reply.contains("eksik"))
        assertFalse(reply.contains("elverişli"))
    }
}
