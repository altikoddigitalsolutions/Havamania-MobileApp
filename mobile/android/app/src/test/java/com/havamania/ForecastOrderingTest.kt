package com.havamania

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ForecastOrderingTest {
    private val now = Instant.parse("2026-11-12T08:00:00Z")
    @Test fun `mapper sorts hours and removes all conflicting timestamp candidates`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, hourly = HourlyDto(
            time = listOf("2026-11-12T12:00", "2026-11-12T10:00", "2026-11-12T10:00:00", "2026-11-12T09:00"),
            temperature = listOf(12.0, 10.0, 30.0, 9.0), weatherCode = listOf(0, 0, 0, 0)
        )), "Bursa", now = now)
        assertEquals(listOf("2026-11-12T09:00", "2026-11-12T12:00"), value.hourlyForecast.map { it.fullTime })
    }
    @Test fun `mapper sorts days and does not choose one duplicate date`() {
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = DailyDto(
            time = listOf("2026-11-15", "2026-11-13", "2026-11-13", "2026-11-12"),
            tempMin = listOf(1.0, 1.0, 1.0, 1.0), tempMax = listOf(5.0, 6.0, 9.0, 8.0), weatherCode = listOf(0, 0, 0, 0)
        )), "Bursa", now = now)
        assertEquals(listOf("2026-11-12", "2026-11-15"), value.dailyForecast.map { it.date })
    }
    @Test fun `legacy cached lists get the same ordering and ambiguity rules`() {
        fun hour(time: String) = HourlyWeather(time, "2026-11-12T$time", "10°")
        val value = WeatherData(timezone = "UTC", hourlyForecast = listOf(hour("12:00"), hour("10:00"), hour("10:00"), hour("09:00")),
            dailyForecast = listOf(DailyForecast("Later", "2026-11-15", 1, 5), DailyForecast("Today", "2026-11-12", 1, 5),
                DailyForecast("Conflict", "2026-11-15", 1, 8)))
        assertEquals(listOf("09:00", "12:00"), ForecastTime.hoursForDate(value, LocalDate.parse("2026-11-12"), now).map { it.time })
        assertEquals(listOf("2026-11-12"), ForecastTime.dailyFromToday(value, now).map { it.date })
    }
}
