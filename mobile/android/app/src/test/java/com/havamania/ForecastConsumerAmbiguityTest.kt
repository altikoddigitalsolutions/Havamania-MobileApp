package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class ForecastConsumerAmbiguityTest {
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val day = DailyForecast("Bugün", "2026-11-12", 10, 20)
    @Test fun `cache decode normalization protects direct consumers and is idempotent`() {
        val raw = WeatherData(dailyForecast = listOf(day, day.copy(maxTemp = 30), day.copy(date = "2026-11-13")),
            hourlyForecast = listOf(HourlyWeather("a", "2026-11-12T12:00", "10°"),
                HourlyWeather("b", "2026-11-12T12:00:00", "20°"), HourlyWeather("bad", "invalid", "30°")))
        val normalized = normalizeCachedForecastLists(raw)
        assertTrue(normalized.hourlyForecast.isEmpty())
        assertEquals("2026-11-13", normalized.dailyForecast.single().date)
        assertEquals(normalized, normalizeCachedForecastLists(normalized))
    }
    @Test fun `dated assistant reply refuses ambiguous raw day`() {
        val raw = WeatherData(cityName = "Bursa", timezone = "UTC", dailyForecast = listOf(day, day.copy(maxTemp = 30)))
        val text = AiWeatherReply.datedReply("Bugün hava nasıl?", raw, now)!!
        assertTrue(text, text.contains("verisi bulunmuyor"))
        assertFalse(text.contains("20°C"))
    }
    @Test fun `duplicate and reversed solar records cannot classify an hour as daytime`() {
        fun mapped(duplicate: Boolean): WeatherData {
            val dates = if (duplicate) listOf("2026-11-12", "2026-11-12") else listOf("2026-11-12")
            return WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = DailyDto(
                time = dates, weatherCode = dates.map { 0 }, tempMin = dates.map { 10.0 }, tempMax = dates.map { 20.0 },
                sunrise = dates.map { "$it" + if (duplicate) "T06:00" else "T18:00" },
                sunset = dates.map { "$it" + if (duplicate) "T18:00" else "T06:00" }),
                hourly = HourlyDto(time = listOf("2026-11-12T12:00"), temperature = listOf(15.0), weatherCode = listOf(0))),
                "Bursa", now = now)
        }
        assertNull(mapped(true).hourlyForecast.single().isDay)
        assertNull(mapped(false).hourlyForecast.single().isDay)
    }
}
