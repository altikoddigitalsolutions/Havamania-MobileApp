package com.havamania

import kotlinx.serialization.Serializable

@Serializable
internal data class CitySyncChange(
    val city: GeocodingResultDto, val deleted: Boolean,
    val revision: String = java.util.UUID.randomUUID().toString(),
    val acknowledged: Boolean = false,
    val failedAttempts: Int = 0,
    val retryNotBefore: Long = 0,
    val blockedReason: String? = null,
    val retryDuration: Long = 0,
    val retryElapsedStart: Long = 0,
    val retryBoot: String = ""
)

/** Pending desired values override snapshots until a server snapshot confirms them. */
internal fun mergeCityChanges(
    remote: List<GeocodingResultDto>, changes: List<CitySyncChange>
): List<GeocodingResultDto> {
    val result = remote.associateByTo(linkedMapOf()) { it.id }
    changes.forEach { if (it.deleted) result.remove(it.city.id) else result[it.city.id] = it.city }
    return result.values.toList()
}

internal fun unconfirmedCityChanges(
    remote: List<GeocodingResultDto>, changes: List<CitySyncChange>
): List<CitySyncChange> = changes.filter { change ->
    if (!change.acknowledged) true
    else if (change.deleted) remote.any { it.id == change.city.id }
    else remote.none { it == change.city }
}
