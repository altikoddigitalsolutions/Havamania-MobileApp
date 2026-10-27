package com.havamania

// The app uses (0, 0) as an unresolved location; a zero on just one axis is valid.
internal fun hasResolvedTripCoordinates(latitude: Double?, longitude: Double?): Boolean =
    latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        (latitude != 0.0 || longitude != 0.0)

internal fun tripLocationError(plan: TravelPlan): String? = when {
    !hasResolvedTripCoordinates(plan.latitude, plan.longitude) -> "Hedef konumunu arama önerilerinden seçin."
    !plan.originCity.isNullOrBlank() && !hasResolvedTripCoordinates(plan.originLatitude, plan.originLongitude) ->
        "Kalkış noktasını arama önerilerinden seçin veya alanı boş bırakın."
    else -> null
}
