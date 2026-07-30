package com.havamania

/**
 * Akıllı Güzergâh Hava Durumu — rota domain modelleri (Aşama 2).
 *
 * Ağ katmanı DTO'larından (bkz. [OsrmDto]) bağımsız, uygulama içinde kullanılan
 * sade tipler. Harita çizimi ve ileride örnekleme/ETA hesapları bu tipler üzerinden yapılır.
 */

/** Tek bir coğrafi nokta (WGS84). */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Hesaplanmış tek bir sürüş rotası.
 *
 * @param points Rota geometrisi (başlangıçtan varışa sıralı nokta dizisi).
 * @param distanceMeters Toplam mesafe (metre).
 * @param durationSeconds Tahmini sürüş süresi (saniye).
 */
data class RoutePath(
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double
) {
    val origin: GeoPoint? get() = points.firstOrNull()
    val destination: GeoPoint? get() = points.lastOrNull()
}

/** Rota hesaplama sonucu — hata durumları çağıran tarafta ayrıştırılabilsin diye sealed. */
sealed interface RouteResult {
    data class Success(val route: RoutePath) : RouteResult
    /** Koordinatlar arasında sürülebilir yol bulunamadı (OSRM code != "Ok"). */
    data object NoRoute : RouteResult
    /** Ağ / sunucu hatası. */
    data class Error(val message: String, val cause: Throwable? = null) : RouteResult
}
