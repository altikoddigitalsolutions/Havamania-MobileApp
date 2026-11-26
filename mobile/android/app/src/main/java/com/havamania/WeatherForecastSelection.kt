package com.havamania

import java.time.Instant
import java.time.LocalDate

internal data class WeatherForecastSelection(val date: LocalDate?, val day: DailyForecast?, val hour: HourlyWeather?)

internal fun initialWeatherForecastSelection(data: WeatherData, now: Instant): WeatherForecastSelection {
    val today = ForecastTime.knownZone(data.timezone)?.let { now.atZone(it).toLocalDate() }
    // Without a location clock, retain dated data for manual browsing, not an automatic forecast.
    if (today == null) return WeatherForecastSelection(null, null, null)
    val days = ForecastTime.dailyFromToday(data, now)
    val day = days.firstOrNull { it.date == today.toString() } ?: days.firstOrNull()
    val date = day?.let { LocalDate.parse(it.date) } ?: today
    return WeatherForecastSelection(date, day, ForecastTime.hoursForDate(data, date, now).firstOrNull())
}

internal fun weatherSelectionCondition(data: WeatherData, hour: HourlyWeather?, day: DailyForecast?): String = when {
    hour != null -> WeatherUtils.describeWeatherPhase(hour.weatherCode, hour.isDay)
    day != null -> WeatherUtils.describeWeatherPhase(day.weatherCode, null)
    else -> data.condition
}

internal fun weatherUpdatedTimeLabel(data: WeatherData): String {
    val zone = ForecastTime.knownZone(data.timezone) ?: return "Saat dilimi doğrulanamadı"
    return Instant.ofEpochMilli(data.timestamp).atZone(zone).format(
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("tr-TR")))
}

internal fun weatherSelectionDaylight(data: WeatherData, hour: HourlyWeather?, day: DailyForecast?): Boolean? = when {
    hour != null -> hour.isDay
    day != null || data.isStale -> null
    else -> data.verifiedIsDay
}
