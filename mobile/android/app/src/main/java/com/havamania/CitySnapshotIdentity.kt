package com.havamania

internal class CityIdentityConflictException : IllegalStateException("Conflicting city identities")

internal fun isCityIdentityConflict(error: Throwable): Boolean =
    generateSequence(error) { it.cause }.take(8).any { it is CityIdentityConflictException }

internal fun sameCitySyncTarget(expected: GeocodingResultDto, candidate: GeocodingResultDto): Boolean {
    if (expected.id != candidate.id) return false
    if (expected.id <= 0) return sameLocationRecord(expected, candidate)
    val firstCode = expected.countryCode?.trim()?.uppercase(java.util.Locale.ROOT)?.takeIf { it.isNotBlank() }
    val secondCode = candidate.countryCode?.trim()?.uppercase(java.util.Locale.ROOT)?.takeIf { it.isNotBlank() }
    return expected.latitude == candidate.latitude && expected.longitude == candidate.longitude &&
        (firstCode == null || secondCode == null || firstCode == secondCode)
}

internal fun validateCitySnapshotMerge(local: List<GeocodingResultDto>, remote: List<GeocodingResultDto>,
    changes: List<CitySyncChange>) {
    if (hasAmbiguousCityIds(local) || hasAmbiguousCityIds(remote) || hasAmbiguousCityIds(changes.map { it.city }))
        throw CityIdentityConflictException()
    changes.forEach { change ->
        remote.filter { it.id == change.city.id }.forEach { candidate ->
            if (!sameCitySyncTarget(change.city, candidate)) throw CityIdentityConflictException()
        }
    }
}