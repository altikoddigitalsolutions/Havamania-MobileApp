package com.havamania

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSamplerTest {

    // ---- Haversine ----

    @Test
    fun haversine_oneDegreeLatitude_isAboutOneEleventhKm() {
        // 1° enlem ≈ 111.19 km
        val d = GeoMath.haversineMeters(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0))
        assertEquals(111_195.0, d, 500.0)
    }

    @Test
    fun haversine_samePoint_isZero() {
        val d = GeoMath.haversineMeters(GeoPoint(41.0, 29.0), GeoPoint(41.0, 29.0))
        assertEquals(0.0, d, 0.0001)
    }

    // ---- Sampler ----

    @Test
    fun sample_placesWaypointsAtEachInterval() {
        // (0,0)->(1,0) ≈ 111_195 m. 50 km aralık → 50k ve 100k eşikleri.
        val route = RoutePath(
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)),
            distanceMeters = 111_195.0,
            durationSeconds = 3600.0
        )
        val wps = RouteSampler.sample(route, intervalMeters = 50_000.0)

        assertEquals(2, wps.size)
        assertEquals(50_000.0, wps[0].cumulativeDistanceMeters, 0.01)
        assertEquals(100_000.0, wps[1].cumulativeDistanceMeters, 0.01)
        // İlk örnek noktası segmentin ~%45'inde olmalı → enlem ~0.45
        assertTrue(wps[0].location.latitude in 0.44..0.46)
    }

    @Test
    fun sample_returnsEmpty_whenTooFewPoints() {
        val route = RoutePath(points = listOf(GeoPoint(0.0, 0.0)), distanceMeters = 0.0, durationSeconds = 0.0)
        assertTrue(RouteSampler.sample(route).isEmpty())
    }

    @Test
    fun sample_returnsEmpty_whenIntervalNonPositive() {
        val route = RoutePath(
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)),
            distanceMeters = 111_195.0,
            durationSeconds = 3600.0
        )
        assertTrue(RouteSampler.sample(route, intervalMeters = 0.0).isEmpty())
    }

    // ---- ETA ----

    @Test
    fun assignEtas_distributesTimeProportionally() {
        val route = RoutePath(
            points = listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)),
            distanceMeters = 100_000.0,
            durationSeconds = 3600.0 // 1 saat
        )
        val wp = RouteWaypoint(GeoPoint(0.5, 0.0), cumulativeDistanceMeters = 50_000.0)
        val result = EtaCalculator.assignEtas(route, listOf(wp), departureEpochMillis = 0L)

        // Mesafenin yarısında → sürenin yarısı = 1800 s = 1_800_000 ms
        assertEquals(1_800_000L, result[0].etaEpochMillis)
    }

    @Test
    fun assignEtas_returnsUnchanged_whenNoDistance() {
        val route = RoutePath(points = emptyList(), distanceMeters = 0.0, durationSeconds = 3600.0)
        val wp = RouteWaypoint(GeoPoint(0.5, 0.0), cumulativeDistanceMeters = 50_000.0)
        val result = EtaCalculator.assignEtas(route, listOf(wp), departureEpochMillis = 1000L)
        assertEquals(null, result[0].etaEpochMillis)
    }
}
