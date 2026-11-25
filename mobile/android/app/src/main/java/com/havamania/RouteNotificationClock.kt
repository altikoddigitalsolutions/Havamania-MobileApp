package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class RouteNotificationClock(val zone: ZoneId, val departure: Instant, val day: LocalDate,
    val dailyKey: String, val notificationId: String)
internal fun routeNotificationClock(plan: TravelPlan, originTimezone: String?, now: Instant): RouteNotificationClock? {
    val zone = originTimezone?.let(::validTripForecastZone) ?: return null
    val local = plan.departureDateTime ?: return null
    val offsets = zone.rules.getValidOffsets(local)
    if (offsets.size != 1) return null // No silent shift through a DST gap or arbitrary overlap offset.
    val departure = local.toInstant(offsets.single())
    val day = now.atZone(zone).toLocalDate()
    val key = "route_daily_${plan.userId}_${plan.id}_$day"
    return RouteNotificationClock(zone, departure, day, key, notificationEventRecordId(plan.userId, "route_daily", key))
}
@kotlinx.serialization.Serializable
data class LocationTimezoneResponse(val timezone: String? = null)

internal fun routeOriginCoordinates(plan: TravelPlan): Pair<Double, Double>? = plan.originPoint?.takeIf {
    it.first.isFinite() && it.second.isFinite() && it.first in -90.0..90.0 && it.second in -180.0..180.0
}

internal suspend fun routeOriginZone(plan: TravelPlan, fetch: suspend (Double, Double) -> String?): ZoneId? {
    val point = routeOriginCoordinates(plan) ?: return null
    return fetch(point.first, point.second)?.let(::validTripForecastZone)
}
