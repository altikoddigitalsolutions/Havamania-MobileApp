package com.havamania

internal fun locationSuggestionKey(row: GeocodingResultDto): String =
    if (row.id > 0) "id:" + row.id
    else listOf(row.countryCode?.trim()?.uppercase(java.util.Locale.ROOT),
        row.name.trim(), row.latitude, row.longitude).joinToString(":")

internal fun locationSupportingText(row: GeocodingResultDto): String {
    val region = row.admin1?.trim()?.takeIf { it.isNotEmpty() && !it.equals(row.name.trim(), ignoreCase = true) }
    val country = row.country.trim().takeIf { it.isNotEmpty() }
        ?: row.countryCode?.trim()?.uppercase(java.util.Locale.ROOT)?.takeIf { it.isNotEmpty() }
        ?: "Ülke bilgisi yok"
    return listOfNotNull(region, country).distinct().joinToString(" · ")
}
internal fun sameLocationRecord(first: GeocodingResultDto, second: GeocodingResultDto?): Boolean =
    second != null && locationSuggestionKey(first) == locationSuggestionKey(second)

internal fun matchesWeatherSelection(latitude: Double, longitude: Double, city: String,
    district: String?, candidate: GeocodingResultDto): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
        candidate.latitude.isFinite() && candidate.longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        kotlin.math.abs(latitude - candidate.latitude) < 0.000001 &&
        kotlin.math.abs(longitude - candidate.longitude) < 0.000001 &&
        AiIntentParser.normalizeTurkish(city) == AiIntentParser.normalizeTurkish(candidate.getSafeCity()) &&
        AiIntentParser.normalizeTurkish(district.orEmpty()) == AiIntentParser.normalizeTurkish(candidate.getSafeDistrict().orEmpty())