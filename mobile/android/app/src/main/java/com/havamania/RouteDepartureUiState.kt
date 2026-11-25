package com.havamania

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class RouteDepartureUiState(val clock: RouteNotificationClock? = null, val loading: Boolean = false, val error: String? = null)
internal val LocalRouteDepartureState = staticCompositionLocalOf { RouteDepartureUiState() }
internal suspend fun resolveRouteScreenClock(plan: TravelPlan, origin: GeoPoint, now: Instant,
    fetch: suspend (Double, Double) -> String?): RouteDepartureUiState {
    if (plan.departureDateTime == null) return RouteDepartureUiState(error = "Geçerli bir kalkış saati seçin.")
    val withOrigin = plan.copy(originLatitude = origin.latitude, originLongitude = origin.longitude)
    val zone = routeOriginZone(withOrigin, fetch) ?: return RouteDepartureUiState(error = "Çıkış konumunun saat dilimi alınamadı. Tekrar deneyin.")
    val clock = routeNotificationClock(withOrigin, zone.id, now)
        ?: return RouteDepartureUiState(error = "Kalkış saati saat değişimi nedeniyle belirsiz veya geçersiz. Kalkış saatini düzenleyin.")
    return RouteDepartureUiState(clock = clock)
}
internal fun routeClockEligible(plan: TravelPlan, clock: RouteNotificationClock?, now: Instant): Boolean {
    if (clock == null) return false
    val status = TravelStatusResolver.getStatus(plan.startDate, plan.endDate, now.atZone(clock.zone).toLocalDate())
    return status != TravelStatus.PAST && (status == TravelStatus.ONGOING || TravelStatusResolver.isWithinRouteWeatherWindow(clock.departure, now))
}
internal fun formatRouteMoment(epoch: Long?, clock: RouteNotificationClock?): String {
    if (epoch == null || clock == null) return "Saat bilinmiyor"
    return Instant.ofEpochMilli(epoch).atZone(clock.zone).format(DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.forLanguageTag("tr-TR")))
}