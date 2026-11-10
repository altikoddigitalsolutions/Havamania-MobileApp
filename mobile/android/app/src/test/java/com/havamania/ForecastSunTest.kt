package com.havamania

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test

class ForecastSunTest {
    @Test fun eachForecastDayKeepsItsOwnSunTimes() {
        val response = OpenMeteoResponse(0.0, 0.0, timezone = "UTC", daily = DailyDto(
            time = listOf("2026-10-22", "2026-10-23"), weatherCode = listOf(0, 0),
            tempMin = listOf(10.0, 10.0), tempMax = listOf(20.0, 20.0),
            sunrise = listOf("2026-10-22T06:00", "2026-10-23T07:00"),
            sunset = listOf("2026-10-22T19:00", "2026-10-23T18:00")))
        val data = WeatherMapper.mapToDomain(response, "Test", now = Instant.parse("2026-10-23T12:00:00Z"))
        assertEquals("06:00", data.dailyForecast[0].sunriseTime)
        assertEquals("07:00", data.dailyForecast[1].sunriseTime)
        assertEquals("18:00", data.dailyForecast[1].sunsetTime)
        assertEquals("07:00", data.sunriseTime)
        assertEquals("12:30", data.solarNoon)
    }

    @Test fun wrongDateAndMalformedSunTimesAreNotReused() {
        assertNull(ForecastSun.timeForDate("2026-10-22T06:00", "2026-10-23"))
        assertNull(ForecastSun.timeForDate("2026-10-23T29:00", "2026-10-23"))
        assertNull(ForecastSun.timeForDate(null, "2026-10-23"))
    }

    @Test fun oldCacheWithoutDailySunFieldsStillDecodes() {
        val day = Json.decodeFromString<DailyForecast>("""{"day":"Tomorrow","date":"2026-10-23","minTemp":10,"maxTemp":20}""")
        assertNull(day.sunriseTime)
        assertNull(day.sunsetTime)
    }

    @Test fun midpointDoesNotInventTimesForMissingOrPolarData() {
        assertNull(ForecastSun.daylightMidpoint(null, "18:00"))
        assertNull(ForecastSun.daylightMidpoint("00:00", "00:00"))
        assertNull(ForecastSun.daylightMidpoint("18:00", "06:00"))
        assertEquals("12:30", ForecastSun.daylightMidpoint("06:15", "18:45"))
    }

    @Test fun sunsetBoundaryMatchesHourlyNightClassification() {
        val rise = LocalTime.of(7, 0)
        val set = LocalTime.of(18, 0)
        assertEquals("Güneşli", WeatherUtils.getWeatherDisplayName(0, LocalDateTime.parse("2026-10-23T07:00"), rise, set))
        assertEquals("Açık Gece", WeatherUtils.getWeatherDisplayName(0, LocalDateTime.parse("2026-10-23T18:00"), rise, set))
    }
}
