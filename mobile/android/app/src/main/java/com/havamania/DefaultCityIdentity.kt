package com.havamania

internal class DefaultCityNameAmbiguousException : IllegalStateException("Default city name is ambiguous")

internal fun defaultCityNameMatches(name: String?, city: GeocodingResultDto): Boolean =
    !name.isNullOrBlank() && AiIntentParser.normalizeTurkish(name) == AiIntentParser.normalizeTurkish(city.name)

internal fun resolveNamedDefaultCity(name: String?, cities: List<GeocodingResultDto>): GeocodingResultDto? {
    if (name.isNullOrBlank()) return null
    val matches = cities.filter { defaultCityNameMatches(name, it) }.distinct()
    if (matches.size > 1) throw DefaultCityNameAmbiguousException()
    return matches.singleOrNull()
}

internal fun canConfirmDefaultCity(change: CitySyncChange, name: String?, resolved: GeocodingResultDto?): Boolean =
    change.acknowledged && resolved != null && defaultCityNameMatches(name, change.city) &&
        defaultCityNameMatches(name, resolved) && sameCitySyncTarget(change.city, resolved)