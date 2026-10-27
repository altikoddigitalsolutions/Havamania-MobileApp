package com.havamania

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun buildTripForecastSnapshot(daily: DailyDto?, start: LocalDate, end: LocalDate,
    timezone: String? = null, now: java.time.Instant = java.time.Instant.now()): ForecastSnapshot? {
    if (daily == null || end.isBefore(start)) return null
    val zone = timezone?.let { validTripForecastZone(it) ?: return null }
    val validCodes = setOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
    val rows = daily.time.mapIndexedNotNull { index, raw ->
        val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return@mapIndexedNotNull null
        if (date.isBefore(start) || date.isAfter(end)) return@mapIndexedNotNull null
        date to index
    }
    // Duplicate dates and incomplete trip coverage must not look like a full-trip analysis.
    if (rows.size.toLong() != ChronoUnit.DAYS.between(start, end) + 1 || rows.map { it.first }.distinct().size != rows.size) return null
    val indices = rows.map { it.second }
    if (indices.any { index ->
        val min = daily.tempMin.getOrNull(index)
        val max = daily.tempMax.getOrNull(index)
        val wind = daily.windSpeedMax.getOrNull(index)
        val rain = daily.precipProbMax?.getOrNull(index)
        daily.weatherCode.getOrNull(index) !in validCodes || min == null || max == null ||
            !min.isFinite() || !max.isFinite() || min > max || wind == null || !wind.isFinite() || wind < 0 ||
            rain == null || rain !in 0..100
    }) return null
    val codes = orderedTripWeatherCodes(indices.map { daily.weatherCode[it] })
    val code = codes.first()
    val risks = codes.mapNotNull(::tripWeatherRiskLabel).distinct()
    val summary = risks.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: WeatherMapper.getWeatherCondition(code)
    return ForecastSnapshot(
        minTemp = indices.minOf { daily.tempMin[it] },
        maxTemp = indices.maxOf { daily.tempMax[it] },
        averageTemperature = indices.map { daily.tempMin[it] / 2.0 + daily.tempMax[it] / 2.0 }.average(),
        precipitationProbability = indices.maxOf { daily.precipProbMax!![it] },
        windSpeed = indices.maxOf { daily.windSpeedMax[it] },
        uvIndex = indices.mapNotNull { daily.uvIndexMax?.getOrNull(it)?.takeIf { value -> value.isFinite() && value >= 0 } }.maxOrNull(),
        conditionSummary = summary, weatherCode = code, generatedAt = now.toEpochMilli(), timezone = zone?.id
    )
}
