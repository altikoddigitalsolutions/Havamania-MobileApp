package com.havamania

import java.time.Instant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Test

class SunAndSettingsRecoveryTest {
    @Test fun `reversed solar intervals are removed from current daily and cached cards`() {
        val date = "2026-11-12"
        val value = WeatherMapper.mapToDomain(OpenMeteoResponse(40.0, 29.0, daily = DailyDto(
            time = listOf(date), weatherCode = listOf(0), tempMin = listOf(10.0), tempMax = listOf(20.0),
            sunrise = listOf("${date}T18:00"), sunset = listOf("${date}T06:00"))), "Bursa",
            now = Instant.parse("2026-11-12T12:00:00Z"))
        assertNull(value.sunriseTime); assertNull(value.sunsetTime); assertNull(value.solarNoon)
        assertNull(value.dailyForecast.single().sunriseTime)
        assertNull(value.dailyForecast.single().sunsetTime)
        val cached = normalizeCachedForecastLists(value.copy(sunriseTime = "18:00", sunsetTime = "06:00", solarNoon = "12:00",
            dailyForecast = listOf(value.dailyForecast.single().copy(sunriseTime = "18:00", sunsetTime = "06:00"))))
        assertNull(cached.sunriseTime); assertNull(cached.solarNoon)
        assertNull(cached.dailyForecast.single().sunsetTime)
    }
    @Test fun `single valid solar measurement is retained without invented midpoint`() {
        assertEquals("06:00" to null, ForecastSun.validTimes("06:00", null))
        assertEquals(null to "18:00", ForecastSun.validTimes("invalid", "18:00"))
        assertEquals(null to null, ForecastSun.validTimes("06:00", "06:00"))
    }
    @Test fun `transient settings error recovers even when next value equals last value`() = runBlocking {
        var attempts = 0
        val published = mutableListOf<Boolean>()
        val waits = mutableListOf<Long>()
        observeCurrentWeatherSettings(flow {
            attempts++
            emit(true)
            if (attempts == 1) throw java.io.IOException("temporary")
        }, { true }, { published += it }, { published += false }, { waits += it })
        assertEquals(listOf(true, false, true), published)
        assertEquals(listOf(1000L), waits)
        assertEquals(2, attempts)
    }
    @Test fun `persistent IO failure is bounded while non IO failure is not retried`() = runBlocking {
        var attempts = 0
        val waits = mutableListOf<Long>()
        observeCurrentWeatherSettings(flow<Boolean> { attempts++; throw java.io.IOException("unavailable") },
            { true }, { fail("no value") }, {}, { waits += it })
        assertEquals(3, attempts)
        assertEquals(listOf(1000L, 2000L), waits)
        attempts = 0
        observeCurrentWeatherSettings(flow<Boolean> { attempts++; throw IllegalStateException("invalid") },
            { true }, { fail("no value") }, {}, { fail("must not retry") })
        assertEquals(1, attempts)
    }
    @Test fun `cancel during retry wait stops resubscription`() = runBlocking {
        var attempts = 0
        val waiting = CompletableDeferred<Unit>()
        val job = launch {
            observeCurrentWeatherSettings(flow<Boolean> { attempts++; throw java.io.IOException("temporary") },
                { true }, { fail("no value") }, {}, { waiting.complete(Unit); awaitCancellation() })
        }
        waiting.await()
        job.cancelAndJoin()
        assertEquals(1, attempts)
    }
}
