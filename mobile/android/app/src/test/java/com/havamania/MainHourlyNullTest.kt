package com.havamania

import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MainHourlyNullTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val now = Instant.parse("2026-11-25T10:00:00Z")
    @Test fun `JSON null hour does not discard current weather or neighboring hour`() {
        val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"Europe/Istanbul","current":{"temperature_2m":17.0},"hourly":{"time":["2026-11-25T13:00","2026-11-25T14:00"],"temperature_2m":[null,18.0],"weather_code":[0,3],"precipitation_probability":[null,25]}}"""
        val value = WeatherMapper.mapToDomain(json.decodeFromString(OpenMeteoResponse.serializer(), wire), "İstanbul", now = now)
        assertEquals("17°", value.temperature); assertEquals(1, value.hourlyForecast.size)
        assertEquals("2026-11-25T14:00", value.hourlyForecast.single().fullTime)
        assertEquals("18°", value.hourlyForecast.single().temp)
    }
    @Test fun `empty hourly object does not reject a valid current observation`() {
        val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"UTC","current":{"temperature_2m":17.0},"hourly":{}}"""
        val value = WeatherMapper.mapToDomain(json.decodeFromString(OpenMeteoResponse.serializer(), wire), "İstanbul", now = now)
        assertEquals("17°", value.temperature); assertTrue(value.hourlyForecast.isEmpty())
    }
    @Test fun `null timestamp and condition remain gaps without shifting aligned values`() {
        val wire = """{"latitude":41.0,"longitude":29.0,"timezone":"UTC","hourly":{"time":[null,"2026-11-25T11:00","2026-11-25T12:00"],"temperature_2m":[9.0,10.0,11.0],"weather_code":[0,null,61],"precipitation_probability":[0,10,80]}}"""
        val value = WeatherMapper.mapToDomain(json.decodeFromString(OpenMeteoResponse.serializer(), wire), "İstanbul", now = now)
        assertEquals(1, value.hourlyForecast.size); assertEquals("11°", value.hourlyForecast.single().temp)
        assertEquals(80, value.hourlyForecast.single().precipitationProbability)
    }
    @Test fun `untrusted hourly code and extreme temperature are not displayable rows`() {
        val response = OpenMeteoResponse(41.0, 29.0, hourly = HourlyDto(time = listOf("2026-11-25T12:00","2026-11-25T13:00"),
            temperature = listOf(999.0,20.0), weatherCode = listOf(0,-1)))
        assertTrue(WeatherMapper.mapToDomain(response, "İstanbul", now = now).hourlyForecast.isEmpty())
    }
    @Test fun `genuine zero temperature and probability remain valid`() {
        val value = OpenMeteoResponse(41.0, 29.0, timezone = "UTC", hourly = HourlyDto(time = listOf("2026-11-25T12:00"),
            temperature = listOf(0.0), weatherCode = listOf(0), precipitationProbability = listOf(0)))
        val hour = WeatherMapper.mapToDomain(value, "İstanbul", now = now).hourlyForecast.single()
        assertEquals("0°", hour.temp); assertEquals(0, hour.precipitationProbability)
    }
}
