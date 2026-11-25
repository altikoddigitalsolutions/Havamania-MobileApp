package com.havamania

import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MainDailyNullTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val now = Instant.parse("2026-11-25T12:00:00Z")
    private val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"UTC","current":{"temperature_2m":17.0},"daily":{"time":["2026-11-25","2026-11-26"],"weather_code":[0,61],"temperature_2m_min":[null,12.0],"temperature_2m_max":[20.0,22.0],"precipitation_probability_max":[null,80],"wind_speed_10m_max":[5,10],"sunrise":[null,"2026-11-26T07:00"],"sunset":[null,"2026-11-26T17:00"]}}"""
    @Test fun `null daily measure preserves current and valid neighbor without shifting indices`() {
        val value = WeatherMapper.mapToDomain(json.decodeFromString(OpenMeteoResponse.serializer(), wire), "İstanbul", now = now)
        assertEquals("17°", value.temperature); assertEquals(1, value.dailyForecast.size)
        val day = value.dailyForecast.single(); assertEquals("2026-11-26", day.date); assertEquals(12, day.minTemp)
        assertEquals(22, day.maxTemp); assertEquals(80, day.precipitationProbability)
    }
    @Test fun `empty daily object preserves valid observation`() {
        val value = json.decodeFromString(OpenMeteoResponse.serializer(), """{"latitude":41.0,"longitude":29.0,"current":{"temperature_2m":17.0},"daily":{}}""")
        val mapped = WeatherMapper.mapToDomain(value, "İstanbul", now = now)
        assertEquals("17°", mapped.temperature); assertTrue(mapped.dailyForecast.isEmpty())
    }
    @Test fun `null dates and codes do not shift another days measurements`() {
        val value = json.decodeFromString(OpenMeteoResponse.serializer(), """{"latitude":41.0,"longitude":29.0,"daily":{"time":[null,"2026-11-25","2026-11-26"],"weather_code":[0,null,61],"temperature_2m_min":[5,6,7],"temperature_2m_max":[15,16,17]}}""")
        val day = WeatherMapper.mapToDomain(value, "İstanbul", now = now).dailyForecast.single()
        assertEquals("2026-11-26", day.date); assertEquals(7, day.minTemp); assertEquals(17, day.maxTemp)
    }
    @Test fun `trip snapshot rejects partial interval and retains complete subinterval`() {
        val value = json.decodeFromString(OpenMeteoResponse.serializer(), wire)
        val day = LocalDate.of(2026,11,25)
        assertNull(buildTripForecastSnapshot(value.daily, day, day.plusDays(1), "UTC", now))
        assertNotNull(buildTripForecastSnapshot(value.daily, day.plusDays(1), day.plusDays(1), "UTC", now))
    }
    @Test fun `unknown code and extreme daily temperatures cannot become rows`() {
        val value = OpenMeteoResponse(41.0,29.0,daily=DailyDto(time=listOf("2026-11-25","2026-11-26"),weatherCode=listOf(0,-1),tempMin=listOf(900.0,10.0),tempMax=listOf(999.0,20.0)))
        assertTrue(WeatherMapper.mapToDomain(value,"İstanbul",now=now).dailyForecast.isEmpty())
    }
    @Test fun `genuine daily zero temperatures remain valid`() {
        val value = OpenMeteoResponse(41.0,29.0,daily=DailyDto(time=listOf("2026-11-25"),weatherCode=listOf(0),tempMin=listOf(0.0),tempMax=listOf(0.0)))
        val day = WeatherMapper.mapToDomain(value,"İstanbul",now=now).dailyForecast.single()
        assertEquals(0,day.minTemp); assertEquals(0,day.maxTemp)
    }
}