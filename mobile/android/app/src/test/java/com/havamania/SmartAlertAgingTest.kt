package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SmartAlertAgingTest {
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val config = SmartAlertConfig()
    private val value = WeatherData(timezone = "UTC", timestamp = now.toEpochMilli(), mappingVersion = 1,
        temperature = "36.5°", low = "-0.5°", windSpeed = 50.0, uvIndex = 8, weatherCode = 95)
    @Test fun `all current warnings disappear when record expires or is marked stale`() {
        assertTrue(SmartAlertEngine.generateAlerts(value, config, now = now).isNotEmpty())
        assertTrue(SmartAlertEngine.generateAlerts(value, config, now = now.plusSeconds(901)).isEmpty())
        assertTrue(SmartAlertEngine.generateAlerts(value.copy(isStale = true), config, now = now).isEmpty())
    }
    @Test fun `decimal values preserve actual temperature rather than concatenating digits`() {
        val mild = value.copy(temperature = "3.6°", low = "0.5°")
        val alerts = SmartAlertEngine.generateAlerts(mild, config, now = now)
        assertFalse(alerts.any { it.id == "heat" || it.id == "frost" })
        assertTrue(SmartAlertEngine.generateAlerts(value, config, now = now).first { it.id == "frost" }.description.contains("-0.5°C"))
    }
    @Test fun `invalid measurements do not generate numerical warnings`() {
        val alerts = SmartAlertEngine.generateAlerts(value.copy(temperature = "NaN°", low = "--",
            windSpeed = Double.POSITIVE_INFINITY, uvIndex = null, weatherCode = -1), config, now = now)
        assertTrue(alerts.isEmpty())
    }
    @Test fun `rain alert window expires as clock advances before data TTL`() {
        val start = Instant.parse("2026-11-12T12:59:00Z")
        val data = value.copy(timestamp = start.toEpochMilli(), hourlyForecast = listOf(
            HourlyWeather("12:00", "2026-11-12T12:00", "10°", precipitationProbability = 80)))
        assertTrue(SmartAlertEngine.generateAlerts(data, config, now = start).any { it.id == "rain" })
        assertFalse(SmartAlertEngine.generateAlerts(data, config, now = start.plusSeconds(120)).any { it.id == "rain" })
    }
}
