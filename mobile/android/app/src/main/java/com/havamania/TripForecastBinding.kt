package com.havamania

import java.time.Instant
import java.time.LocalDate

@androidx.annotation.Keep
@kotlinx.serialization.Serializable
@com.google.firebase.firestore.IgnoreExtraProperties
data class TripForecastBinding(
    val tripId: String = "", val ownerUid: String = "", val city: String = "", val district: String? = null,
    val latitude: Double = 0.0, val longitude: Double = 0.0,
    val plannedStartDate: String = "", val plannedEndDate: String = "",
    val coveredStartDate: String = "", val coveredEndDate: String = ""
)

internal fun createTripForecastBinding(plan: TravelPlan, start: LocalDate, end: LocalDate): TripForecastBinding? {
    if (plan.city.isBlank() || plan.endDate.isBefore(plan.startDate) || start.isBefore(plan.startDate) ||
        end != plan.endDate || end.isBefore(start) || !plan.latitude.isFinite() || plan.latitude !in -90.0..90.0 ||
        !plan.longitude.isFinite() || plan.longitude !in -180.0..180.0) return null
    return TripForecastBinding(plan.id, plan.userId, plan.city, plan.district, plan.latitude, plan.longitude,
        plan.startDate.toString(), plan.endDate.toString(), start.toString(), end.toString())
}

internal fun tripForecastMatchesPlan(plan: TravelPlan, snapshot: ForecastSnapshot?, now: Instant): Boolean {
    val binding = snapshot?.binding ?: return false
    val zone = snapshot.timezone?.let(::validTripForecastZone) ?: return false
    val today = now.atZone(zone).toLocalDate()
    if (plan.endDate.isBefore(today)) return false
    val start = runCatching { LocalDate.parse(binding.coveredStartDate) }.getOrNull() ?: return false
    val end = runCatching { LocalDate.parse(binding.coveredEndDate) }.getOrNull() ?: return false
    val expected = createTripForecastBinding(plan, start, end) ?: return false
    // Allow an older complete record to include elapsed days, but never omit a remaining day.
    return binding == expected && !start.isAfter(maxOf(plan.startDate, today))
}
