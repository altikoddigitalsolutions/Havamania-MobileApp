package com.havamania

/** Update edited preferences without overwriting the separate opt-in or future profile fields. */
internal fun personalizationPreferenceUpdates(owner: String, interests: List<String>, styles: List<String>,
    weather: WeatherPreferences, now: Long = System.currentTimeMillis()): Map<String, Any> = mapOf(
    "personalizationProfile.uid" to owner,
    "personalizationProfile.selectedInterests" to interests.toList(),
    "personalizationProfile.travelStyles" to styles.toList(),
    "personalizationProfile.weatherPreferences" to weather,
    "personalizationProfile.lastUpdated" to now,
    "onboardingCompleted" to true,
    "updatedAt" to now
)
