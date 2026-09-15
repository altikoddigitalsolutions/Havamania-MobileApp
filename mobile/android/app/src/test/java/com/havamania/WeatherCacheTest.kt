package com.havamania

import java.lang.reflect.Proxy
import java.time.Instant
import java.util.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WeatherCacheTest {
    private fun <T> proxy(type: Class<T>, handler: (String, Array<out Any?>) -> Any?): T =
        type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        })

    @Test fun sameNameAtDifferentCoordinatesDoesNotReuseForecast() = runBlocking {
        val cache = mutableMapOf<String, WeatherCacheEntity>()
        var requests = 0
        val dao = proxy(WeatherDao::class.java) { method, args ->
            when (method) {
                "getCachedWeather" -> cache[args[0]]
                "insertWeather" -> { val row = args[0] as WeatherCacheEntity; cache[row.cityName] = row; Unit }
                else -> error(method)
            }
        }
        val api = proxy(WeatherApiService::class.java) { _, args ->
            requests++
            OpenMeteoResponse(args[0] as Double, args[1] as Double,
                current = CurrentWeatherDto(temperature = requests * 10.0))
        }
        val repository = WeatherRepository(api, dao)
        assertEquals("10°", repository.getWeatherData(41.0, 29.0, "Merkez").toList().last().temperature)
        assertEquals("20°", repository.getWeatherData(42.0, 30.0, "Merkez").toList().last().temperature)
        assertEquals("10°", repository.getWeatherData(41.0, 29.0, " MERKEZ ").toList().last().temperature)
        assertEquals(2, requests)
    }

    @Test fun diskFailureStillDeliversValidNetworkWeather() = runBlocking {
        val dao = proxy(WeatherDao::class.java) { _, _ -> throw IllegalStateException("disk unavailable") }
        val api = proxy(WeatherApiService::class.java) { _, _ ->
            OpenMeteoResponse(41.0, 29.0, current = CurrentWeatherDto(temperature = 23.0))
        }
        assertEquals("23°", WeatherRepository(api, dao).getWeatherData(41.0, 29.0, "Test").toList().single().temperature)
    }

    @Test fun futureOrExpiredCacheIsNotFresh() {
        assertFalse(weatherCacheIsFresh(1001, 1000))
        assertFalse(weatherCacheIsFresh(0, 900000))
        assertTrue(weatherCacheIsFresh(1000, 1000))
    }

    @Test fun selectedHourUsesWeatherLocationEvenWhenDeviceIsInAnotherTimezone() {
        val old = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        try {
            val domain = WeatherMapper.mapToDomain(OpenMeteoResponse(41.0, 29.0, timezone = "Europe/Istanbul",
                hourly = HourlyDto(time = listOf("2026-09-15T05:00", "2026-09-15T15:00"),
                    temperature = listOf(15.0, 25.0), weatherCode = listOf(0, 0))), "Istanbul",
                now = Instant.parse("2026-09-15T12:35:00Z"))
            assertFalse(domain.hourlyForecast[0].isSelected)
            assertTrue(domain.hourlyForecast[1].isSelected)
        } finally {
            TimeZone.setDefault(old)
        }
    }
    @Test fun offlineWithExpiredCacheKeepsWeatherAndMarksItStale() = runBlocking {
        val weather = WeatherMapper.mapToDomain(
            OpenMeteoResponse(41.0, 29.0, current = CurrentWeatherDto(temperature = 19.0)), "Test")
        val cached = WeatherCacheEntity("unused", Json.encodeToString(weather), 1L)
        val dao = proxy(WeatherDao::class.java) { _, _ -> cached }
        val api = proxy(WeatherApiService::class.java) { _, _ -> throw java.io.IOException("offline") }
        val results = WeatherRepository(api, dao).getWeatherData(41.0, 29.0, "Test").toList()
        assertEquals(1, results.size)
        assertEquals("19°", results.single().temperature)
        assertTrue(results.single().isStale)
        assertEquals(1L, results.single().timestamp)
    }

    @Test fun offlineWithoutCachePropagatesFailureInsteadOfInventingWeather() = runBlocking {
        val dao = proxy(WeatherDao::class.java) { _, _ -> null }
        val api = proxy(WeatherApiService::class.java) { _, _ -> throw java.io.IOException("offline") }
        val repository = WeatherRepository(api, dao)
        val result = runCatching { repository.getWeatherData(41.0, 29.0, "Test").toList() }
        assertTrue(result.isFailure)
        assertNull(repository.currentWeatherState.value)
    }

    @Test fun cancellationIsNotSwallowedAsOfflineCacheFallback() = runBlocking {
        val weather = WeatherMapper.mapToDomain(OpenMeteoResponse(41.0, 29.0), "Test")
        val dao = proxy(WeatherDao::class.java) { _, _ ->
            WeatherCacheEntity("unused", Json.encodeToString(weather), 1L)
        }
        val api = proxy(WeatherApiService::class.java) { _, _ ->
            throw kotlinx.coroutines.CancellationException("account changed")
        }
        val result = runCatching { WeatherRepository(api, dao).getWeatherData(41.0, 29.0, "Test").toList() }
        assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
    }

}
