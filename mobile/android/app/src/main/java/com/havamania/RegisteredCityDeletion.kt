package com.havamania

/** Matches the persisted ID-based queue while counting only distinct visible records. */
internal fun canDeleteRegisteredCity(cities: List<GeocodingResultDto>, default: GeocodingResultDto?,
    candidate: GeocodingResultDto): Boolean {
    val unique = cities.distinctBy(::locationSuggestionKey)
    if (hasAmbiguousCityIds(cities)) return false
    if (unique.size <= 1 || default?.id == candidate.id) return false
    val sameQueueId = unique.filter { it.id == candidate.id }
    return sameQueueId.size == 1 && sameLocationRecord(sameQueueId.single(), candidate)
}
internal fun hasAmbiguousCityIds(cities: List<GeocodingResultDto>): Boolean =
    cities.groupBy { it.id }.values.any { group -> group.distinctBy { locationSuggestionKey(it.copy(id = 0)) }.size > 1 }