package com.havamania

import java.lang.reflect.Proxy
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RouteWeatherProviderTest {
    private val response = OpenMeteoResponse(41.0, 29.0, hourly = HourlyDto(
        time = listOf("2026-09-15T12:00"), temperature = listOf(20.0), weatherCode = listOf(0)
    ))
    private val eta = Instant.parse("2026-09-15T12:00:00Z").toEpochMilli()

    private fun api(handler: (String, Array<out Any?>) -> Any?): WeatherApiService =
        Proxy.newProxyInstance(WeatherApiService::class.java.classLoader,
            arrayOf(WeatherApiService::class.java)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        } as WeatherApiService

    @Test
    fun `batch coordinates use comma separated query parameters`() = runBlocking {
        val provider = RouteWeatherProvider(api { method, args ->
            assertEquals("getBatchRouteHourly", method)
            assertEquals("41.0,42.0", args[0])
            assertEquals("29.0,30.0", args[1])
            assertEquals("UTC", args[3])
            listOf(response, response)
        })
        val result = provider.weatherAtBatch(listOf(GeoPoint(41.0, 29.0), GeoPoint(42.0, 30.0)),
            listOf(eta, eta))
        assertEquals(2, result.filterNotNull().size)
    }

    @Test
    fun `dates outside forecast horizon do not display obsolete weather`() = runBlocking {
        val provider = RouteWeatherProvider(api { _, _ -> response })
        assertNull(provider.weatherAt(GeoPoint(41.0, 29.0),
            Instant.parse("2026-10-15T12:00:00Z").toEpochMilli()))
    }

    @Test
    fun `single point uses object response endpoint`() = runBlocking {
        val provider = RouteWeatherProvider(api { method, args ->
            assertEquals("getRouteHourly", method)
            assertEquals("UTC", args[3])
            response
        })
        assertNotNull(provider.weatherAtBatch(listOf(GeoPoint(41.0, 29.0)), listOf(eta)).single())
    }

    @Test fun `unknown ETA does not substitute midnight weather`() = runBlocking {
        assertNull(RouteWeatherProvider(api { _, _ -> response }).weatherAt(GeoPoint(41.0, 29.0), null))
    }
}
