package com.havamania

import java.time.Instant
import java.time.ZoneId

internal fun rainRiskInNextTwoHours(weather: WeatherData, now: Instant): Boolean {
    if (weather.isStale || !weatherCacheIsFresh(weather.timestamp, now.toEpochMilli())) return false
    val zone = try { ZoneId.of(weather.timezone) } catch (_: Exception) { return false }
    val until = now.plusSeconds(7200)
    val candidates = weather.hourlyForecast.mapNotNull { row ->
        ForecastTime.parseHour(row)?.let { it to row }
    }
    return uniqueForecastEntries(candidates) { it.first }.any { (time, row) ->
        // Offset-free timestamps cannot disambiguate a repeated DST hour.
        val offset = zone.rules.getValidOffsets(time).singleOrNull() ?: return@any false
        val start = time.toInstant(offset)
        val overlaps = start.isBefore(until) && start.plusSeconds(3600).isAfter(now)
        overlaps && (row.precipitationProbability?.let { it in 41..100 } == true ||
            row.weatherCode in setOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99))
    }
}
