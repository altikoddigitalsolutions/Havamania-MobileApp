package com.havamania

import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalTravelPersonalization = staticCompositionLocalOf<PersonalizationProfile?> { null }
internal fun PersonalizationProfile?.travelInterests(): Set<String> =
    this?.takeIf { it.personalizationEnabled }?.selectedInterests.orEmpty().toSet()

/** Rank only existing verified experiences. Reserve one slot for the explicit trip purpose. */
internal fun selectGuideExperiences(items: List<TravelGuideExperience>, type: TripType?, interests: Set<String>): List<TravelGuideExperience> {
    val affinities = interests.flatMap { id -> when (id) {
        "kamp", "trekking", "dagcilik", "acik_hava", "balikcilik" -> listOf(TripType.NATURE, TripType.CAMPING, TripType.ADVENTURE)
        "bulut_fotografciligi", "gun_batimi" -> listOf(TripType.PHOTOGRAPHY)
        "cocuklar_icin" -> listOf(TripType.FAMILY)
        "motorsiklet", "motosiklet", "karavan", "off_road", "uzun_yol" -> listOf(TripType.ROAD_TRIP)
        "kayak", "snowboard", "kis_surusu" -> listOf(TripType.WINTER, TripType.SPORTS)
        "hafta_sonu" -> listOf(TripType.WEEKEND)
        else -> emptyList()
    } }.toSet()
    val unique = items.distinct()
    if (affinities.isEmpty()) return unique.sortedByDescending { type in it.preferredTripTypes }.take(2)
    val ranked = unique.sortedWith(compareByDescending<TravelGuideExperience> { experience ->
        experience.preferredTripTypes.any { it in affinities }
    }.thenByDescending { type in it.preferredTripTypes })
    val first = ranked.firstOrNull { type in it.preferredTripTypes } ?: ranked.firstOrNull() ?: return emptyList()
    return listOf(first) + ranked.filter { it != first }.take(1)
}

internal fun personalizedTravelPacking(profile: PersonalizationProfile?): String {
    val interests = profile.travelInterests()
    return buildList {
        if ("kamp" in interests) add("Uyku tulumu ve matını kontrol etmeyi unutma.")
        if (interests.any { it in setOf("trekking", "dagcilik") }) add("Yürüyüş ayakkabılarını ve yedek çoraplarını hazırla.")
        if (interests.any { it in setOf("bulut_fotografciligi", "gun_batimi") }) add("Yedek piller ve lens temizleme kitini hazırla.")
    }.joinToString(" ")
}
