package com.havamania

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class WeatherConnectionRecoveryTest {
    private val now = Instant.parse("2026-11-12T12:00:00Z")
    private val data = WeatherData(timestamp = now.toEpochMilli(), mappingVersion = 1)
    @Test fun `fresh data does not request recovery but old data does`() {
        assertFalse(weatherNeedsConnectionRecovery(WeatherUiState.Success(data), now))
        assertTrue(weatherNeedsConnectionRecovery(WeatherUiState.Success(data.copy(isStale = true)), now))
        assertTrue(weatherNeedsConnectionRecovery(WeatherUiState.Success(data), now.plusSeconds(901)))
    }
    @Test fun `day rollover also needs recovery within freshness window`() {
        val before = Instant.parse("2026-11-12T20:59:00Z")
        val value = data.copy(timezone = "Europe/Istanbul", timestamp = before.toEpochMilli())
        assertTrue(weatherNeedsConnectionRecovery(WeatherUiState.Success(value), before.plusSeconds(120)))
    }
    @Test fun `only recoverable errors qualify and empty loading states do not`() {
        assertTrue(weatherNeedsConnectionRecovery(WeatherUiState.Error("Ağ"), now))
        assertFalse(weatherNeedsConnectionRecovery(WeatherUiState.Error("Tercih", retryNeedsInternet = false), now))
        assertFalse(weatherNeedsConnectionRecovery(WeatherUiState.NoCity, now))
        assertFalse(weatherNeedsConnectionRecovery(WeatherUiState.Loading, now))
    }
}
