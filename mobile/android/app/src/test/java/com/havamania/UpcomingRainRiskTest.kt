package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class UpcomingRainRiskTest {
    private val now = Instant.parse("2026-11-12T20:30:00Z") // 23:30 Istanbul
    private fun row(time: String, rain: Int = 80) = HourlyWeather(time, time, "10°", precipitationProbability = rain, weatherCode = 0)
    private fun weather(vararg rows: HourlyWeather) = WeatherData(timezone = "Europe/Istanbul",
        timestamp = now.toEpochMilli(), hourlyForecast = rows.toList())
    @Test fun `past rows and beyond horizon do not count`() {
        assertFalse(rainRiskInNextTwoHours(weather(row("2026-11-12T21:00"), row("2026-11-13T03:00")), now))
    }
    @Test fun `window crosses local midnight and does not depend on list order`() {
        assertTrue(rainRiskInNextTwoHours(weather(row("2026-11-13T03:00", 0), row("2026-11-13T00:00")), now))
        assertTrue(rainRiskInNextTwoHours(weather(row("2026-11-12T23:00")), now))
    }
    @Test fun `exact end boundary and expired current bucket are excluded`() {
        val exact = Instant.parse("2026-11-12T20:00:00Z")
        assertFalse(rainRiskInNextTwoHours(weather(row("2026-11-13T01:00")).copy(timestamp = exact.toEpochMilli()), exact))
        assertFalse(rainRiskInNextTwoHours(weather(row("2026-11-12T22:00")).copy(timestamp = exact.toEpochMilli()), exact))
    }
    @Test fun `old invalid and duplicate measurements cannot trigger rain warning`() {
        val value = weather(row("2026-11-13T00:00"))
        assertFalse(rainRiskInNextTwoHours(value.copy(isStale = true), now))
        assertFalse(rainRiskInNextTwoHours(value.copy(timestamp = now.minusSeconds(901).toEpochMilli()), now))
        assertFalse(rainRiskInNextTwoHours(weather(row("2026-11-13T00:00", 101)), now))
        assertFalse(rainRiskInNextTwoHours(weather(row("2026-11-13T00:00"), row("2026-11-13T00:00", 0)), now))
    }
    @Test fun `ambiguous DST hour and invalid zone are not guessed`() {
        val dstNow = Instant.parse("2026-11-01T04:30:00Z")
        val value = weather(row("2026-11-01T01:00")).copy(timezone = "America/New_York", timestamp = dstNow.toEpochMilli())
        assertFalse(rainRiskInNextTwoHours(value, dstNow))
        assertFalse(rainRiskInNextTwoHours(value.copy(timezone = "invalid"), dstNow))
    }
    @Test fun `engine honors rain setting and uses location date for deduplication`() {
        val localTomorrow = Instant.parse("2026-11-12T21:30:00Z")
        val value = weather(row("2026-11-13T01:00")).copy(timestamp = localTomorrow.toEpochMilli())
        val alert = SmartAlertEngine.generateAlerts(value, SmartAlertConfig(), "B", localTomorrow).single { it.id == "rain" }
        assertTrue(alert.deduplicationKey.endsWith("|rain|2026-11-13"))
        assertTrue(alert.deduplicationKey.startsWith("B|"))
        assertFalse(SmartAlertEngine.generateAlerts(value, SmartAlertConfig(rainEnabled = false), "B", localTomorrow).any { it.id == "rain" })
    }
}
