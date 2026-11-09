package com.havamania

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsrmRouteProviderTest {
    private val start = GeoPoint(40.0, 29.0)
    private val end = GeoPoint(41.0, 30.0)

    private fun provider(coordinates: List<List<Double>>) = OsrmRouteProvider(object : OsrmApiService {
        override suspend fun getRoute(
            coordinatesPath: String, overview: String, geometries: String, steps: Boolean
        ): OsrmRouteResponse {
            assertEquals("29.0,40.0;30.0,41.0", coordinatesPath)
            return OsrmRouteResponse("Ok", listOf(OsrmRoute(OsrmGeometry(coordinates), 10.0, 1.0)))
        }
    })

    @Test
    fun validCoordinatesKeepTheirOrder() = runBlocking {
        val result = provider(listOf(listOf(29.0, 40.0), listOf(30.0, 41.0))).getRoute(start, end)
        assertEquals(listOf(start, end), (result as RouteResult.Success).route.points)
    }

    @Test
    fun malformedIntermediateCoordinateRejectsTheRoute() = runBlocking {
        val result = provider(listOf(listOf(29.0, 40.0), listOf(31.0, 91.0), listOf(30.0, 41.0)))
            .getRoute(start, end)
        assertTrue(result is RouteResult.Error)
    }
}
