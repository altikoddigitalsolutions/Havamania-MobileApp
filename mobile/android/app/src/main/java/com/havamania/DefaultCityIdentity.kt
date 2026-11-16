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

/** Versioned companion to the legacy name; never infer missing coordinates as zero. */
internal fun defaultCityProfileFields(city: GeocodingResultDto): Map<String, Any?> {
    require(city.name.isNotBlank() && city.latitude.isFinite() && city.longitude.isFinite() &&
        city.latitude in -90.0..90.0 && city.longitude in -180.0..180.0)
    return mapOf("defaultCity" to city.name, "defaultCityLocation" to mapOf(
        "version" to 1L, "id" to city.id, "name" to city.name,
        "latitude" to city.latitude, "longitude" to city.longitude,
        "country" to city.country, "country_code" to city.countryCode,
        "admin1" to city.admin1, "admin2" to city.admin2, "admin3" to city.admin3))
}

internal fun resolveDefaultCityProfile(name: String?, raw: Any?, cities: List<GeocodingResultDto>): GeocodingResultDto? {
    if (raw == null) return resolveNamedDefaultCity(name, cities)
    val fields = raw as? Map<*, *> ?: throw CityIdentityConflictException()
    fun number(key: String): Double = (fields[key] as? Number)?.toDouble()
        ?.takeIf { it.isFinite() } ?: throw CityIdentityConflictException()
    fun optionalText(key: String): String? {
        val value = fields[key] ?: return null
        return value as? String ?: throw CityIdentityConflictException()
    }
    if (number("version") != 1.0) throw CityIdentityConflictException()
    // Firestore integers are Long; accepting floating IDs would silently lose identity precision.
    val id = fields["id"] as? Long ?: throw CityIdentityConflictException()
    val lat = number("latitude")
    val lon = number("longitude")
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) throw CityIdentityConflictException()
    val city = GeocodingResultDto(id, optionalText("name") ?: throw CityIdentityConflictException(),
        lat, lon, optionalText("country") ?: "", optionalText("country_code"),
        optionalText("admin1"), optionalText("admin2"), optionalText("admin3"))
    if (!defaultCityNameMatches(name, city)) throw CityIdentityConflictException()
    cities.filter { it.id == id }.forEach {
        if (!sameCitySyncTarget(city, it)) throw CityIdentityConflictException()
    }
    return city
}
