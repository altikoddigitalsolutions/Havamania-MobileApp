package com.havamania

import org.junit.Assert.*
import org.junit.Test

class RouteWaypointSelectorTest {
    private fun point(km: Int, risk: RouteRisk? = RouteRisk.OK) = RouteWaypoint(
        GeoPoint(41.0, 29.0), km * 1000.0, etaEpochMillis = km * 60_000L,
        weather = risk?.let { WaypointWeather(0, 20.0, null, null, null, null, it, null) }
    )

    @Test fun hazardAfterFirstSevenPointsSurvives() {
        val normal = (1..9).map { point(it * 60) }
        val hazard = point(590, RouteRisk.DANGER)
        assertTrue(RouteWaypointSelector.select(normal, normal + hazard).contains(hazard))
    }

    @Test fun visualBudgetNeverTruncatesHazardsOrUnknownWeather() {
        val hazards = (1..9).map { point(it * 10, RouteRisk.DANGER) } + point(95, null)
        assertEquals(hazards, RouteWaypointSelector.select(emptyList(), hazards))
    }

    @Test fun nearbySafePointCannotHideDanger() {
        val safe = point(100)
        val hazard = point(101, RouteRisk.DANGER)
        val selected = RouteWaypointSelector.select(listOf(safe), listOf(safe, hazard))
        assertEquals(listOf(hazard), selected)
    }

    @Test fun revisitingSameCoordinatesAtDifferentTimesKeepsBothHazards() {
        val first = point(1, RouteRisk.CAUTION)
        val later = point(100, RouteRisk.DANGER)
        assertEquals(listOf(first, later), RouteWaypointSelector.select(emptyList(), listOf(first, later)))
    }

    @Test fun providerDistanceMismatchStillSamplesEntireGeometryWithCorrectEtas() {
        val route = RoutePath(listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)),
            distanceMeters = 1_000_000.0, durationSeconds = 7200.0)
        val sampled = RouteSampler.denseSampleForAnalysis(route)
        assertEquals(2, sampled.size)
        assertEquals(1.0 / 3, sampled[0].location.latitude, 0.001)
        assertEquals(2.0 / 3, sampled[1].location.latitude, 0.001)
        val timed = EtaCalculator.assignEtas(route, sampled, 0)
        assertEquals(2_400_000L, timed[0].etaEpochMillis)
        assertEquals(4_800_000L, timed[1].etaEpochMillis)
    }

    @Test fun invalidRouteCannotProduceRepeatedZeroDistanceSamples() {
        val route = RoutePath(listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 0.0, 7200.0)
        assertTrue(RouteSampler.denseSampleForAnalysis(route).isEmpty())
    }
}
