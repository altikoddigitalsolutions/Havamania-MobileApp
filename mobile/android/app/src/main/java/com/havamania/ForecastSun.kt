package com.havamania

import java.time.LocalDateTime
import java.time.LocalTime

internal object ForecastSun {
    // Reject shifted or malformed provider arrays instead of borrowing another day's sun times.
    fun timeForDate(iso: String?, date: String): String? = try {
        LocalDateTime.parse(iso).takeIf { it.toLocalDate().toString() == date }
            ?.toLocalTime()?.toString()
    } catch (_: Exception) { null }

    fun daylightMidpoint(sunrise: String?, sunset: String?): String? = try {
        val rise = LocalTime.parse(sunrise).toSecondOfDay()
        val set = LocalTime.parse(sunset).toSecondOfDay()
        if (set <= rise) null else LocalTime.ofSecondOfDay(((rise + set) / 2).toLong())
            .withSecond(0).toString()
    } catch (_: Exception) { null }
}
