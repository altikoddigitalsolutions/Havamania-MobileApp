package com.havamania

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
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
    val isPremium: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val registeredCities: List<GeocodingResultDto> = emptyList(),
    val personalizationProfile: FirestorePersonalizationProfile? = null
)

@Keep
@IgnoreExtraProperties
data class FirestorePersonalizationProfile(
    val selectedInterests: List<String> = emptyList(),
    val travelStyles: List<String> = emptyList(),
    val weatherPreferences: WeatherPreferences? = null,
    val lastUpdated: Long = 0
)

