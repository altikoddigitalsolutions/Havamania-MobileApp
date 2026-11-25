package com.havamania

import java.lang.reflect.Proxy
import java.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RouteWeatherPhaseTest {
    private val eta = Instant.parse("2026-11-25T12:00:00Z").toEpochMilli()
    private fun response(phase: Int?) = RouteForecastResponse(41.0, 29.0, timezone = "UTC", hourly = RouteHourlyDto(
        time = listOf("2026-11-25T12:00"), temperature = listOf(20.0), weatherCode = listOf(0), isDay = phase?.let { listOf(it) }))
    private fun api(handler: (String, Array<out Any?>) -> Any?) = Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
        arrayOf(WeatherApiService::class.java)) { _, method, args -> handler(method.name, args ?: emptyArray()) } as WeatherApiService
    @Test fun `missing solar evidence keeps clear weather neutral across clocks`() {
        val previous = TimeZone.getDefault()
        try {
            listOf("Asia/Tokyo", "America/New_York").forEach { TimeZone.setDefault(TimeZone.getTimeZone(it)); assertEquals("Açık", routeWeatherDisplayName(0, null)) }
        } finally { TimeZone.setDefault(previous) }
    }
    @Test fun `missing solar evidence never invents cloudy night`() { assertFalse(routeWeatherDisplayName(2, null).contains("gece", true)) }
    @Test fun `missing ETA cannot hide known storm`() { assertEquals("Fırtınalı", routeWeatherDisplayName(95, null)) }
    @Test fun `explicit solar evidence controls clear label and icon`() {
        assertEquals("Güneşli", routeWeatherDisplayName(0, true)); assertEquals("Açık gece", routeWeatherDisplayName(0, false))
        assertEquals("🌙", routeWeatherEmoji(0, false)); assertEquals("🌡️", routeWeatherEmoji(0, null))
    }
    @Test fun `batch keeps phase bound to each sampled point`() = runBlocking {
        val provider = RouteWeatherProvider(api { method, args ->
            assertEquals("getBatchRouteHourly", method); assertTrue(args[2].toString().contains("is_day"))
            listOf(response(0), response(1))
        })
        val result = provider.weatherAtBatch(listOf(GeoPoint(41.0, 29.0), GeoPoint(42.0, 30.0)), listOf(eta, eta))
        assertEquals(false, result[0]?.isDay); assertEquals(true, result[1]?.isDay)
    }
    @Test fun `missing and invalid provider phase remains unknown`() = runBlocking {
        listOf(null, -1, 2).forEach { phase ->
            val provider = RouteWeatherProvider(api { _, args -> assertTrue(args[2].toString().contains("is_day")); response(phase) })
            val weather = provider.weatherAt(GeoPoint(41.0, 29.0), eta)
            assertNotNull(weather); assertNull(weather!!.isDay)
        }
    }
}