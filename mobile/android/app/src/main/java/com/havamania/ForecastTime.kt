package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Forecast timestamps belong to the forecast location, never the device timezone. */
internal object ForecastTime {
    fun knownZone(timezone: String): ZoneId? = runCatching { ZoneId.of(timezone) }.getOrNull()
    fun zone(timezone: String): ZoneId = try { ZoneId.of(timezone) } catch (_: Exception) { ZoneOffset.UTC }
    fun localNow(timezone: String, now: Instant = Instant.now()): LocalDateTime = now.atZone(zone(timezone)).toLocalDateTime()
    fun parseHour(hour: HourlyWeather): LocalDateTime? =
        try { LocalDateTime.parse(hour.fullTime) } catch (_: Exception) { null }

    fun hoursForDate(data: WeatherData, date: LocalDate, now: Instant = Instant.now()): List<HourlyWeather> {
        val locationZone = knownZone(data.timezone) ?: return emptyList()
        val locationNow = now.atZone(locationZone).toLocalDateTime()
        val cutoff = locationNow.truncatedTo(ChronoUnit.HOURS)
        return data.hourlyForecast.mapNotNull { hour -> parseHour(hour)?.let { it to hour } }
            .filter { (time, _) -> time.toLocalDate() == date && locationZone.rules.getValidOffsets(time).isNotEmpty() &&
                (date != locationNow.toLocalDate() || !time.isBefore(cutoff)) }
            .let { uniqueForecastEntries(it) { entry -> entry.first } }.map { it.second }
    }

    fun dailyFromToday(data: WeatherData, now: Instant = Instant.now()): List<DailyForecast> {
        val today = knownZone(data.timezone)?.let { now.atZone(it).toLocalDate() }
        val valid = data.dailyForecast.mapNotNull { day ->
            val date = try { LocalDate.parse(day.date) } catch (_: Exception) { return@mapNotNull null }
            if (today != null && date.isBefore(today)) null else day.copy(isToday = date == today)
        }
        return uniqueForecastEntries(valid) { LocalDate.parse(it.date) }
    }
}

/** A repeated timestamp is ambiguous: discard every candidate rather than pick arbitrary data. */
internal fun <T, K : Comparable<K>> uniqueForecastEntries(values: List<T>, key: (T) -> K): List<T> =
    values.groupBy(key).filterValues { it.size == 1 }.toSortedMap().values.map { it.single() }

/** Sanitize old serialized lists before direct consumers (assistant, alerts, widgets) see them. */
internal fun normalizeCachedForecastLists(data: WeatherData): WeatherData {
    val knownZone = ForecastTime.knownZone(data.timezone) != null
    val hours = data.hourlyForecast.mapNotNull { hour -> ForecastTime.parseHour(hour)?.let { it to if (knownZone) hour else hour.copy(isSelected = false) } }
    val days = data.dailyForecast.mapNotNull { day ->
        val date = try { LocalDate.parse(day.date) } catch (_: Exception) { return@mapNotNull null }
        val (rise, set) = ForecastSun.validTimes(day.sunriseTime, day.sunsetTime)
        date to day.copy(sunriseTime = rise, sunsetTime = set, isToday = knownZone && day.isToday)
    }
    val (rise, set) = ForecastSun.validTimes(data.sunriseTime, data.sunsetTime)
    return data.copy(
        sunriseTime = rise, sunsetTime = set, solarNoon = ForecastSun.daylightMidpoint(rise, set),
        hourlyForecast = uniqueForecastEntries(hours) { it.first }.map { it.second },
        dailyForecast = uniqueForecastEntries(days) { it.first }.map { it.second }
    )
}
