package com.havamania

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val photoURL: String? = null,
    val aboutMe: String = "",
    val defaultCity: String = "",
    val temperatureUnit: String = "Celsius",
    val assistantTone: String = "Dengeli",
    val personalizationEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val profileCompleted: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val personalizationProfile: FirestorePersonalizationProfile? = null
)

data class FirestorePersonalizationProfile(
    val selectedInterests: List<String> = emptyList(),
    val travelStyles: List<String> = emptyList(),
    val weatherPreferences: WeatherPreferences? = null,
    val lastUpdated: Long = 0
)
