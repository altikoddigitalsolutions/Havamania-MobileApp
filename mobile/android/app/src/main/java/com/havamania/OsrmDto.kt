package com.havamania

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OSRM `/route/v1/driving` yanıtı için DTO'lar (Aşama 2).
 *
 * İstek `geometries=geojson` ile yapılır; bu yüzden geometri GeoJSON LineString
 * biçiminde gelir ve koordinatlar **[boylam, enlem]** sırasındadır (GeoJSON standardı).
 * Domain'e ([RoutePath]) çevirirken bu sıra ters çevrilir.
 */
@Keep
@Serializable
data class OsrmRouteResponse(
    // "Ok", "NoRoute", "NoSegment" ...
    val code: String = "",
    val routes: List<OsrmRoute> = emptyList()
)

@Keep
@Serializable
data class OsrmRoute(
    val geometry: OsrmGeometry = OsrmGeometry(),
    /** Toplam mesafe (metre). */
    val distance: Double = 0.0,
    /** Tahmini sürüş süresi (saniye). */
    val duration: Double = 0.0
)

@Keep
@Serializable
data class OsrmGeometry(
    /** GeoJSON LineString koordinatları: her eleman [lon, lat]. */
    val coordinates: List<List<Double>> = emptyList(),
    @SerialName("type") val type: String = "LineString"
)
