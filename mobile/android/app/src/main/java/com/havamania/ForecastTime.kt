package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Forecast timestamps belong to the forecast location, never the device timezone. */
internal object ForecastTime {
    fun zone(timezone: String): ZoneId = try { ZoneId.of(timezone) } catch (_: Exception) { ZoneOffset.UTC }
    fun localNow(timezone: String, now: Instant = Instant.now()): LocalDateTime = now.atZone(zone(timezone)).toLocalDateTime()
    fun parseHour(hour: HourlyWeather): LocalDateTime? =
        try { LocalDateTime.parse(hour.fullTime) } catch (_: Exception) { null }

    fun hoursForDate(data: WeatherData, date: LocalDate, now: Instant = Instant.now()): List<HourlyWeather> {
        val locationNow = localNow(data.timezone, now)
        val cutoff = locationNow.truncatedTo(ChronoUnit.HOURS)
        return data.hourlyForecast.mapNotNull { hour -> parseHour(hour)?.let { it to hour } }
            .filter { (time, _) -> time.toLocalDate() == date && zone(data.timezone).rules.getValidOffsets(time).isNotEmpty() &&
                (date != locationNow.toLocalDate() || !time.isBefore(cutoff)) }
            .sortedBy { it.first }.map { it.second }
    }

    fun dailyFromToday(data: WeatherData, now: Instant = Instant.now()): List<DailyForecast> {
        val today = localNow(data.timezone, now).toLocalDate()
        return data.dailyForecast.mapNotNull { day ->
            val date = try { LocalDate.parse(day.date) } catch (_: Exception) { return@mapNotNull null }
            if (date.isBefore(today)) null else day.copy(isToday = date == today)
        }
    }
}
