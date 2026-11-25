package com.havamania

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun buildTripForecastSnapshot(daily: DailyDto?, start: LocalDate, end: LocalDate,
    timezone: String? = null, now: java.time.Instant = java.time.Instant.now()): ForecastSnapshot? {
    if (daily == null || end.isBefore(start)) return null
    val zone = timezone?.let { validTripForecastZone(it) ?: return null }
    val dates = daily.time.mapIndexedNotNull { index, raw ->
        val date = raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapIndexedNotNull null
        if (date.isBefore(start) || date.isAfter(end)) return@mapIndexedNotNull null
        date to index
    }
    if (dates.size.toLong() != ChronoUnit.DAYS.between(start, end) + 1 || dates.map { it.first }.distinct().size != dates.size) return null
    data class Row(val code: Int, val min: Double, val max: Double, val rain: Int, val wind: Double, val uv: Double?)
    val rows = dates.map { (_, index) ->
        val code = daily.weatherCode.getOrNull(index)?.takeIf(WeatherUtils::isKnownCode) ?: return null
        val min = daily.tempMin.getOrNull(index)?.takeIf { it.isFinite() && it in -100.0..65.0 } ?: return null
        val max = daily.tempMax.getOrNull(index)?.takeIf { it.isFinite() && it in -100.0..65.0 } ?: return null
        if (min > max) return null
        val wind = daily.windSpeedMax.getOrNull(index)?.takeIf { it.isFinite() && it in 0.0..500.0 } ?: return null
        val rain = daily.precipProbMax?.getOrNull(index)?.takeIf { it in 0..100 } ?: return null
        val uv = daily.uvIndexMax?.getOrNull(index)?.takeIf { it.isFinite() && it >= 0 }
        Row(code, min, max, rain, wind, uv)
    }
    val codes = orderedTripWeatherCodes(rows.map { it.code })
    val code = codes.first()
    val risks = codes.mapNotNull(::tripWeatherRiskLabel).distinct()
    val summary = risks.takeIf { it.isNotEmpty() }?.joinToString(" / ") ?: WeatherMapper.getWeatherCondition(code)
    return ForecastSnapshot(minTemp = rows.minOf { it.min }, maxTemp = rows.maxOf { it.max },
        averageTemperature = rows.map { it.min / 2.0 + it.max / 2.0 }.average(),
        precipitationProbability = rows.maxOf { it.rain }, windSpeed = rows.maxOf { it.wind },
        uvIndex = rows.mapNotNull { it.uv }.maxOrNull(), conditionSummary = summary,
        weatherCode = code, generatedAt = now.toEpochMilli(), timezone = zone?.id)
}