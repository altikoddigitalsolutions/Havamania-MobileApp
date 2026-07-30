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
     * Segment içinde doğrusal enterpolasyon yapılır (kısa segmentlerde hata ihmal edilebilir).
     */
    fun sample(route: RoutePath, intervalMeters: Double = DEFAULT_INTERVAL_METERS): List<RouteWaypoint> {
        val pts = route.points
        if (pts.size < 2 || intervalMeters <= 0.0) return emptyList()

        val result = mutableListOf<RouteWaypoint>()
        var cumulative = 0.0
        var nextThreshold = intervalMeters

        for (i in 1 until pts.size) {
            val segStart = pts[i - 1]
            val segEnd = pts[i]
            val segLen = GeoMath.haversineMeters(segStart, segEnd)
            if (segLen <= 0.0) continue

            // Bu segmentin kapsadığı tüm eşikleri yerleştir.
            while (nextThreshold <= cumulative + segLen) {
                val t = (nextThreshold - cumulative) / segLen
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
        departureEpochMillis: Long
    ): List<RouteWaypoint> {
        val total = route.distanceMeters
        if (total <= 0.0) return waypoints
        return waypoints.map { wp ->
            val frac = (wp.cumulativeDistanceMeters / total).coerceIn(0.0, 1.0)
            val eta = departureEpochMillis + (route.durationSeconds * frac * 1000).toLong()
            wp.copy(etaEpochMillis = eta)
        }
    }
}
