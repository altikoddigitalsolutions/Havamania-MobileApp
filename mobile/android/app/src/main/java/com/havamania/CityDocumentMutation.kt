package com.havamania

internal fun singleCitySyncChange(changes: List<CitySyncChange>, id: Long): CitySyncChange? {
    val matching = changes.filter { it.city.id == id }
    if (matching.size > 1) throw CityIdentityConflictException()
    return matching.singleOrNull()
}

/** Called inside the actual SDK transaction on every attempt. Never changes UI state. */
internal fun stageCityDocumentMutation(change: CitySyncChange, isCurrent: () -> Boolean,
    read: () -> GeocodingResultDto?, write: () -> Unit, delete: () -> Unit): Boolean {
    if (!isCurrent()) return false
    val existing = read()
    if (!isCurrent()) return false
    if (existing != null && !sameCitySyncTarget(change.city, existing)) throw CityIdentityConflictException()
    if (!isCurrent()) return false
    if (change.deleted) { if (existing != null) delete() } else write()
    return true
}