package com.havamania

import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RouteOriginZoneTest {
    private val day = LocalDate.of(2026, 11, 26)
    private fun plan() = TravelPlan(city = "Tokyo", startDate = day, endDate = day,
        latitude = 35.6, longitude = 139.7, originLatitude = 40.7, originLongitude = -74.0)
    @Test fun `timezone lookup uses selected origin coordinates`() = runBlocking {
        var requested: Pair<Double, Double>? = null
        val zone = routeOriginZone(plan()) { lat, lon -> requested = lat to lon; "America/New_York" }
        assertEquals(40.7 to -74.0, requested); assertEquals("America/New_York", zone?.id)
    }
    @Test fun `invalid origin never calls provider`() = runBlocking {
        var calls = 0
        val zone = routeOriginZone(plan().copy(originLatitude = Double.NaN)) { _, _ -> calls++; "UTC" }
        assertNull(zone); assertEquals(0, calls)
    }
    @Test fun `missing timezone response is unknown rather than UTC`() {
        val response = Json { ignoreUnknownKeys = true }.decodeFromString(LocationTimezoneResponse.serializer(), """{"latitude":40.7}""")
        assertNull(response.timezone)
    }
    @Test fun `lookup cancellation is propagated`() = runBlocking {
        try { routeOriginZone(plan()) { _, _ -> throw CancellationException("test") }; fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
    }
}