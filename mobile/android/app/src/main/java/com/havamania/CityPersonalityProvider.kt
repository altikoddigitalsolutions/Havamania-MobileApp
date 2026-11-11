package com.havamania

data class CityPersonality(
    val slogan: String,
    val description: String,
    val mustSee: List<String>,
    val food: List<String>,
    val tip: String,
    val experiences: List<TravelGuideExperience> = emptyList()
)

object CityPersonalityProvider {
    fun getPersonality(cityName: String, district: String? = null, tripType: TripType? = null): CityPersonality {
        val guide = LocalTravelGuides.find(cityName, district)
        if (guide != null) {
            return CityPersonality(
                slogan = guide.description,
                description = guide.description,
                mustSee = listOf(guide.visit),
                food = listOf(guide.food),
                tip = guide.experience,
                experiences = LocalTravelGuides.experiences(cityName, district, tripType)
            )
        }
        val location = district?.takeIf { it.isNotBlank() }?.let { "$it, $cityName" } ?: cityName
        val unavailable = "$location için doğrulanmış yerel deneyim rehberi henüz hazır değil."
        return CityPersonality(
            slogan = location,
            description = unavailable,
            mustSee = emptyList(),
            food = emptyList(),
            tip = ""
        )
    }
}
