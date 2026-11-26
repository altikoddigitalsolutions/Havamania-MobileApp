package com.havamania

import java.time.Instant
import java.time.LocalDate

internal data class TravelAdvicePlanSelection(val plan: TravelPlan?, val today: LocalDate?, val needsTimezone: Boolean)

internal fun selectTravelAdvicePlan(plans: List<TravelPlan>, now: Instant): TravelAdvicePlanSelection {
    val candidates = plans.filter { !it.isArchived }
    if (candidates.any { it.lastForecastSnapshot?.timezone?.let(::validTripForecastZone) == null })
        return TravelAdvicePlanSelection(null, null, true)
    val selected = candidates.mapNotNull { plan ->
        val zone = plan.lastForecastSnapshot?.timezone?.let(::validTripForecastZone) ?: return@mapNotNull null
        val today = now.atZone(zone).toLocalDate()
        if (plan.endDate.isBefore(today)) null else plan to today
    }.minByOrNull { it.first.startDate }
    return TravelAdvicePlanSelection(selected?.first, selected?.second, false)
}
