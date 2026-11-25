package com.havamania

import java.time.Instant
import java.time.LocalDate

internal data class TravelDailyIdentity(val day: LocalDate, val notificationId: String, val deduplicationKey: String, val eventAt: Long)
internal fun travelDailyIdentity(plan: TravelPlan, now: Instant): TravelDailyIdentity? {
    val zone = plan.lastForecastSnapshot?.timezone?.let(::validTripForecastZone) ?: return null
    val day = now.atZone(zone).toLocalDate()
    return TravelDailyIdentity(day, "travel_${plan.userId}_${plan.id}_$day",
        "travel_daily_${plan.userId}_${plan.id}_$day", plan.startDate.atStartOfDay(zone).toInstant().toEpochMilli())
}