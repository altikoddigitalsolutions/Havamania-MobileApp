package com.havamania

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ForecastTimeTest {
    private fun hour(iso: String, label: String = "label") = HourlyWeather(label, iso, "20°")

    @Test fun forecastDateCanBeTomorrowEvenWhenUtcDateIsYesterday() {
        val now = Instant.parse("2026-10-22T22:30:00Z")
        assertEquals(LocalDate.parse("2026-10-23"), ForecastTime.localNow("Europe/Istanbul", now).toLocalDate())
        val data = WeatherData(timezone = "Europe/Istanbul", hourlyForecast = listOf(
            hour("2026-10-23T00:00", "23 Eki"), hour("2026-10-23T01:00"), hour("2026-10-23T02:00")))
        assertEquals(listOf("2026-10-23T01:00", "2026-10-23T02:00"),
            ForecastTime.hoursForDate(data, LocalDate.parse("2026-10-23"), now).map { it.fullTime })
    }

    @Test fun midnightUsesIsoTimestampInsteadOfFormattedDateLabel() {
        val now = Instant.parse("2026-10-22T21:20:00Z")
        val midnight = hour("2026-10-23T00:00", "23 Eki")
        val data = WeatherData(timezone = "Europe/Istanbul", hourlyForecast = listOf(midnight))
        assertEquals(midnight, ForecastTime.hoursForDate(data, LocalDate.parse("2026-10-23"), now).single())
        assertEquals(0, ForecastTime.parseHour(midnight)?.hour)
    }

    @Test fun expiredCacheNeverFallsBackToMorningAsCurrentHour() {
        val data = WeatherData(timezone = "UTC", hourlyForecast = listOf(hour("2026-10-22T08:00")))
        assertTrue(ForecastTime.hoursForDate(data, LocalDate.parse("2026-10-22"), Instant.parse("2026-10-22T22:00:00Z")).isEmpty())
    }

    @Test fun cachedTodayFlagIsRecomputedFromDate() {
        val data = WeatherData(timezone = "Europe/Istanbul", dailyForecast = listOf(
            DailyForecast("Old", "2026-10-22", 10, 20, isToday = true),
            DailyForecast("New", "2026-10-23", 10, 20), DailyForecast("Invalid", "bad", 10, 20)))
        val days = ForecastTime.dailyFromToday(data, Instant.parse("2026-10-22T22:00:00Z"))
        assertEquals(1, days.size)
        assertTrue(days.single().isToday)
        assertEquals("2026-10-23", days.single().date)
    }

    @Test fun invalidTimezoneUsesUtcAndMalformedHoursAreExcluded() {
        val data = WeatherData(timezone = "bad/zone", hourlyForecast = listOf(hour("bad"), hour("2026-10-22T14:00")))
        val now = Instant.parse("2026-10-22T14:20:00Z")
        assertEquals(14, ForecastTime.localNow(data.timezone, now).hour)
        assertEquals(1, ForecastTime.hoursForDate(data, LocalDate.parse("2026-10-22"), now).size)
    }

    @Test fun springDstGapDoesNotOfferNonexistentLocalHour() {
        val data = WeatherData(timezone = "America/New_York", hourlyForecast = listOf(
            hour("2026-03-08T01:00"), hour("2026-03-08T02:00"), hour("2026-03-08T03:00")))
        val results = ForecastTime.hoursForDate(data, LocalDate.parse("2026-03-08"), Instant.parse("2026-03-08T06:30:00Z"))
        assertEquals(listOf("2026-03-08T01:00", "2026-03-08T03:00"), results.map { it.fullTime })
    }

    @Test fun mapperDoesNotLabelFirstFutureDayAsToday() {
        val response = OpenMeteoResponse(0.0, 0.0, timezone = "UTC", daily = DailyDto(
            time = listOf("2026-10-23"), weatherCode = listOf(0), tempMin = listOf(10.0), tempMax = listOf(20.0)))
        val data = WeatherMapper.mapToDomain(response, "Test", now = Instant.parse("2026-10-22T12:00:00Z"))
        assertFalse(data.dailyForecast.single().isToday)
    }
}
