package com.havamania

import kotlinx.serialization.Serializable

@Serializable
data class PersonalizationProfile(
    val uid: String = "",
    val selectedInterests: List<String> = emptyList(),
    val travelStyles: List<String> = emptyList(),
    val weatherPreferences: WeatherPreferences = WeatherPreferences(),
    val personalizationEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class WeatherPreferences(
    val likesHeat: Boolean = false,
    val likesCool: Boolean = false,
    val rainSensitive: Boolean = false,
    val windSensitive: Boolean = false,
    val uvSensitive: Boolean = false,
    val humiditySensitive: Boolean = false
)

object PersonalizationDefaults {
    val ALL_INTERESTS = listOf(
        "Günlük Hava Takibi", "Seyahat", "Kamp", "Doğa Yürüyüşü",
        "Deniz ve Plaj", "Bisiklet", "Koşu", "Fotoğrafçılık",
        "Kış Sporları", "Şehir Gezileri", "Açık Hava Etkinlikleri",
        "Araçla Seyahat", "Sağlık Hassasiyeti", "Çocuklu Aile", "Evcil Hayvan"
    )

    val TRAVEL_STYLES = listOf(
        "Ekonomik", "Konforlu", "Macera", "Kültür",
        "Gastronomi", "Deniz Tatili", "Doğa", "İş Seyahati"
    )
}
