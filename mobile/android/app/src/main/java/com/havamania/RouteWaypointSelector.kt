package com.havamania

/** Never discard sampled hazards or unknown weather to satisfy a visual point budget. */
internal object RouteWaypointSelector {
    fun select(normal: List<RouteWaypoint>, dense: List<RouteWaypoint>, normalBudget: Int = 7): List<RouteWaypoint> {
        val required = dense.filter { it.weather?.risk != RouteRisk.OK }
            .distinctBy { it.cumulativeDistanceMeters }
        val selected = required.toMutableList()
        val available = (normalBudget - required.size).coerceAtLeast(0)
        var added = 0
        for (point in normal.sortedBy { it.cumulativeDistanceMeters }) {
            if (added >= available) break
            // Use route distance: a looping road can revisit the same coordinates at a different time.
            if (selected.none { kotlin.math.abs(it.cumulativeDistanceMeters - point.cumulativeDistanceMeters) < 50_000 }) {
                selected.add(point)
                added++
            }
        }
        return selected.sortedBy { it.cumulativeDistanceMeters }
    }
}
