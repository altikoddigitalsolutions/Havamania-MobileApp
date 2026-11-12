package com.havamania

import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.*
import org.junit.Test

class WeatherSettingsObserverTest {
    @Test fun `preference toggles update existing weather without reloading it`() = runBlocking {
        val now = Instant.parse("2026-11-12T12:00:00Z")
        val weather = WeatherData(timestamp = now.toEpochMilli(), timezone = "UTC", hourlyForecast = listOf(
            HourlyWeather("12:00", "2026-11-12T12:00", "10°", precipitationProbability = 80)))
        val settings = Channel<SmartAlertConfig>(Channel.UNLIMITED)
        var alerts = emptyList<SmartAlert>()
        var updates = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            observeCurrentWeatherSettings(settings.receiveAsFlow(), { true }, {
                updates++; alerts = SmartAlertEngine.generateAlerts(weather, it, now = now)
            }, { fail("settings should be available") })
        }
        try {
            settings.send(SmartAlertConfig()); yield()
            assertTrue(alerts.any { it.id == "rain" })
            settings.send(SmartAlertConfig(rainEnabled = false)); yield()
            assertFalse(alerts.any { it.id == "rain" })
            settings.send(SmartAlertConfig(rainEnabled = false)); yield()
            assertEquals(2, updates)
            settings.send(SmartAlertConfig()); yield()
            assertTrue(alerts.any { it.id == "rain" })
        } finally { job.cancelAndJoin(); settings.close() }
    }
    @Test fun `identity change rejects old events and cancellation stops subscription`() = runBlocking {
        val settings = Channel<Boolean>(Channel.UNLIMITED)
        var uid = "A"
        val values = mutableListOf<Boolean>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            observeCurrentWeatherSettings(settings.receiveAsFlow(), { uid == "A" }, { values += it }, { fail("unexpected failure") })
        }
        settings.send(true); yield()
        uid = "B"
        settings.send(false); yield()
        assertEquals(listOf(true), values)
        job.cancelAndJoin()
        uid = "A"
        settings.send(true); yield()
        assertEquals(listOf(true), values)
        settings.close()
        Unit
    }
    @Test fun `settings read failure clears current state without failing parent`() = runBlocking {
        var enabled = false
        observeCurrentWeatherSettings(flow {
            emit(true)
            throw java.io.IOException("settings unavailable")
        }, { true }, { enabled = it }, { enabled = false })
        assertFalse(enabled)
    }
    @Test fun `obsolete settings failure cannot clear new account state`() = runBlocking {
        var cleared = false
        observeCurrentWeatherSettings(flow<Boolean> { throw java.io.IOException("old account") },
            { false }, { fail("obsolete event") }, { cleared = true })
        assertFalse(cleared)
    }
    @Test fun `upstream cancellation is not treated as unavailable settings`() = runBlocking {
        var cleared = false
        try {
            observeCurrentWeatherSettings(flow<Boolean> { throw CancellationException("cancel") },
                { true }, { fail("no event") }, { cleared = true })
            fail("cancellation must propagate")
        } catch (_: CancellationException) { }
        assertFalse(cleared)
    }
}
