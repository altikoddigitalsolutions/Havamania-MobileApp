package com.havamania

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Akıllı Güzergâh Hava Durumu — Aşama 3.
 *
 * Rota geometrisini belirli aralıklarla örnekler, her ara nokta için tahmini geçiş
 * saatini hesaplar. Reverse geocode ([ReverseGeocoder]) ve hava bağlama sonraki adımlar.
 */

/** Rota üzerindeki tek bir ara nokta (mavi marker + ileride hava/risk verisi). */
data class RouteWaypoint(
    val location: GeoPoint,
    /** Başlangıçtan bu noktaya kadarki kümülatif mesafe (metre). */
    val cumulativeDistanceMeters: Double,
    /** Tahmini geçiş anı (epoch ms); ETA atanana kadar null. */
    val etaEpochMillis: Long? = null,
    /** Reverse geocode ile bulunan yer adı; bulunana kadar null. */
    val placeName: String? = null,
    /** Bu noktanın çözümlenmiş hava + risk verisi; analiz edilene kadar null. */
    val weather: WaypointWeather? = null
)

object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** İki nokta arası büyük-daire mesafesi (metre) — Haversine. */
    fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }
}

object RouteSampler {
    /** Varsayılan örnekleme aralığı (metre) — PRD: ~50–80 km. */
    const val DEFAULT_INTERVAL_METERS = 65_000.0

    /**
     * Rota geometrisi boyunca yaklaşık [intervalMeters] aralıkla ara noktalar üretir.
     * Başlangıç ve varış hariç tutulur (onlar yeşil/kırmızı marker olarak ayrıca çizilir).
     * Segments içinde doğrusal enterpolasyon yapılır.
     */
    fun sample(route: RoutePath): List<RouteWaypoint> {
        return adaptiveSample(route)
    }

    /** Intermediate point count based on duration. */
    fun getTargetIntermediateCount(durationHrs: Double): Int = when {
        durationHrs < 2.0 -> 0
        durationHrs < 5.0 -> 2
        durationHrs < 9.0 -> 3
        durationHrs < 15.0 -> 4
        else -> 6
    }

    private fun adaptiveSample(route: RoutePath): List<RouteWaypoint> {
        val pts = route.points
        if (pts.size < 2) return emptyList()

        val durationHrs = route.durationSeconds / 3600.0
        val intermediateCount = getTargetIntermediateCount(durationHrs)

        if (intermediateCount == 0) return emptyList()

        val result = mutableListOf<RouteWaypoint>()
        val intervalMeters = route.distanceMeters / (intermediateCount + 1)

        var cumulative = 0.0
        var nextThreshold = intervalMeters

        for (i in 1 until pts.size) {
            val segStart = pts[i - 1]
            val segEnd = pts[i]
            val segLen = GeoMath.haversineMeters(segStart, segEnd)
            if (segLen <= 0.0) continue

            while (nextThreshold < cumulative + segLen && result.size < intermediateCount) {
                val t = ((nextThreshold - cumulative) / segLen).coerceIn(0.0, 1.0)
                val lat = segStart.latitude + (segEnd.latitude - segStart.latitude) * t
                val lon = segStart.longitude + (segEnd.longitude - segStart.longitude) * t
                result.add(RouteWaypoint(GeoPoint(lat, lon), nextThreshold))
                nextThreshold += intervalMeters
            }
            cumulative += segLen
        }
        return result
    }

    /**
     * Denser sampling for analysis purposes (~every 45 mins of driving).
     * Hazards will be extracted from this list.
     */
    fun denseSampleForAnalysis(route: RoutePath): List<RouteWaypoint> {
        val pts = route.points
        if (pts.size < 2) return emptyList()

        val durationMins = route.durationSeconds / 60.0
        val sampleEveryMins = 45.0
        val count = (durationMins / sampleEveryMins).toInt().coerceIn(1, 20)

        val result = mutableListOf<RouteWaypoint>()
        val intervalMeters = route.distanceMeters / (count + 1)

        var cumulative = 0.0
        var nextThreshold = intervalMeters

        for (i in 1 until pts.size) {
            val segStart = pts[i - 1]
            val segEnd = pts[i]
            val segLen = GeoMath.haversineMeters(segStart, segEnd)
            if (segLen <= 0.0) continue

            while (nextThreshold < cumulative + segLen && result.size < count) {
                val t = ((nextThreshold - cumulative) / segLen).coerceIn(0.0, 1.0)
                val lat = segStart.latitude + (segEnd.latitude - segStart.latitude) * t
                val lon = segStart.longitude + (segEnd.longitude - segStart.longitude) * t
                result.add(RouteWaypoint(GeoPoint(lat, lon), nextThreshold))
                nextThreshold += intervalMeters
            }
            cumulative += segLen
        }
        return result
    }
}

object EtaCalculator {
    /**
     * Her ara noktaya tahmini geçiş saatini atar.
     *
     * Rotanın toplam süresi kümülatif mesafeyle orantılı dağıtılır — OSRM vertex başına
     * süre vermediği için MVP yaklaşımı. (Aşama 5+: leg bazlı gerçek süre kullanılabilir.)
     */
    fun assignEtas(
        route: RoutePath,
        waypoints: List<RouteWaypoint>,
        departureEpochMillis: Long?
    ): List<RouteWaypoint> {
        if (departureEpochMillis == null) return waypoints
        val total = route.distanceMeters
        if (total <= 0.0) return waypoints
        return waypoints.map { wp ->
            val frac = (wp.cumulativeDistanceMeters / total).coerceIn(0.0, 1.0)
            val eta = departureEpochMillis + (route.durationSeconds * frac * 1000).toLong()
            wp.copy(etaEpochMillis = eta)
        }
    }
}
