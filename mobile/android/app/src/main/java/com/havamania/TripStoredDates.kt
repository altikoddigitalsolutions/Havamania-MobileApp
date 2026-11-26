package com.havamania

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun knownStoredTripDates(entity: TravelPlanEntity): Pair<LocalDate, LocalDate>? {
    fun date(text: String?) = text?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val start = date(entity.startDateIso)
    val end = date(entity.endDateIso)
    if (start != null && end != null) return start to end
    val binding = entity.lastForecastSnapshot?.binding
    if (binding != null && binding.tripId == entity.id && binding.ownerUid == entity.userId && binding.city == entity.city &&
        binding.district == entity.district && binding.latitude == entity.latitude && binding.longitude == entity.longitude) {
        val boundStart = date(binding.plannedStartDate)
        val boundEnd = date(binding.plannedEndDate)
        if (boundStart != null && boundEnd != null && !boundEnd.isBefore(boundStart)) return boundStart to boundEnd
    }
    return null
}

internal fun storedTripDates(entity: TravelPlanEntity, legacyZone: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate> =
    knownStoredTripDates(entity) ?: (Instant.ofEpochMilli(entity.startDate).atZone(legacyZone).toLocalDate() to
        Instant.ofEpochMilli(entity.endDate).atZone(legacyZone).toLocalDate())

internal fun freezeTripDates(entity: TravelPlanEntity, legacyZone: ZoneId = ZoneId.systemDefault(), previous: TravelPlanEntity? = null): TravelPlanEntity {
    // A replay of the same legacy timestamps must not reinterpret an already frozen date.
    val retained = previous?.takeIf { it.id == entity.id && it.userId == entity.userId &&
        it.startDate == entity.startDate && it.endDate == entity.endDate }?.let(::knownStoredTripDates)
    val (start, end) = knownStoredTripDates(entity) ?: retained ?: storedTripDates(entity, legacyZone)
    return entity.copy(startDateIso = start.toString(), endDateIso = end.toString())
}
