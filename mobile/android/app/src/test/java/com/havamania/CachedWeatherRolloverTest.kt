package com.havamania

import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class CachedWeatherRolloverTest {
    private val before = Instant.parse("2026-11-12T20:55:00Z") // 23:55 Istanbul
    private val data = WeatherData(cityName = "Bursa", timezone = "Europe/Istanbul", temperature = "18°",
        high = "25°", low = "12°", uvIndex = 4, precipitationProbability = 80,
        timestamp = before.toEpochMilli(), mappingVersion = 1,
        weatherSuitabilityScore = 100, weatherSuitabilityText = "Uygun")
    @Test fun `crossing local midnight invalidates daily fields even after ten minutes`() {
        val now = before.plusSeconds(600)
        assertFalse(cachedWeatherSameLocalDay(data, now))
        val result = presentCachedWeather(data, now)
        assertTrue(result.isStale)
        assertEquals("--", result.high)
        assertNull(result.uvIndex)
        assertNull(result.precipitationProbability)
        assertEquals(-1, result.weatherSuitabilityScore)
        assertEquals("18°", result.temperature)
    }
    @Test fun `fresh same local day record is unchanged`() {
        assertEquals(data, presentCachedWeather(data, before.plusSeconds(60)))
    }
    @Test fun `expired same day keeps observations but cannot retain favorable assessment`() {
        val result = presentCachedWeather(data.copy(isStale = true), before.plusSeconds(60))
        assertEquals(-1, result.weatherSuitabilityScore)
        assertEquals(80, result.precipitationProbability)
        assertTrue(result.weatherSuitabilityDesc.contains("güncel değil"))
    }
    @Test fun `unknown code never becomes sunny in daytime or at night`() {
        listOf(-1, 999).forEach { code ->
            listOf(2, 12).forEach { hour ->
                assertEquals("Veri yok", WeatherUtils.getWeatherDisplayName(code,
                    LocalDateTime.of(2026, 11, 12, hour, 0), null, null))
            }
        }
        val mapped = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0), "Bursa")
        assertEquals(-1, mapped.weatherCode)
        assertEquals("Veri yok", mapped.condition)
    }
}
