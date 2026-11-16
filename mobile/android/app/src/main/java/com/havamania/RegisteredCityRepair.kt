package com.havamania

internal enum class CityRepairStatus { UNCHANGED, COMPACTED, CONFLICT, BLOCKED, INVALID }
internal data class CityRepairPlan(val cities: List<GeocodingResultDto>, val status: CityRepairStatus)

/** Only byte-equivalent domain records can be dropped automatically. Never choose between conflicting identities. */
internal fun planRegisteredCityRepair(cities: List<GeocodingResultDto>): CityRepairPlan {
    if (hasAmbiguousCityIds(cities)) return CityRepairPlan(cities, CityRepairStatus.CONFLICT)
    val compacted = cities.distinct()
    return CityRepairPlan(compacted, if (compacted.size == cities.size) CityRepairStatus.UNCHANGED else CityRepairStatus.COMPACTED)
}