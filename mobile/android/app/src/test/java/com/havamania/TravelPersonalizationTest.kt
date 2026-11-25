package com.havamania

import org.junit.Assert.*
import org.junit.Test

class TravelPersonalizationTest {
    private val photo = PersonalizationProfile(uid = "A", selectedInterests = listOf("bulut_fotografciligi", "kamp"))
    private val snapshot = ForecastSnapshot(precipitationProbability = 65, minTemp = 10.0, maxTemp = 18.0,
        windSpeed = 30.0, weatherCode = 61, conditionSummary = "Yağmurlu")
    private fun suggestion(profile: PersonalizationProfile?) = TravelAiHelper.generateTravelAiSuggestion(
        "İstanbul", TripType.CULTURE, snapshot, null, 2, personalization = profile)
    @Test fun `optout discards every selected interest`() {
        assertEquals(suggestion(null), suggestion(photo.copy(personalizationEnabled = false)))
        assertTrue(photo.copy(personalizationEnabled = false).travelInterests().isEmpty())
    }
    @Test fun `stored photography id contributes concrete packing`() {
        val answer = suggestion(photo)
        assertTrue(answer, answer.contains("Yedek piller"))
        assertTrue(answer, answer.contains("Uyku tulumu"))
    }
    @Test fun `personal interest changes a verified experience while preserving trip purpose`() {
        val items = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE, personalization = photo).experiences
        assertEquals(2, items.size)
        assertTrue(items.first().preferredTripTypes.contains(TripType.CULTURE))
        assertTrue(items.any { it.preferredTripTypes.contains(TripType.PHOTOGRAPHY) })
    }
    @Test fun `disabled guide uses the ordinary selection`() {
        val original = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE)
        assertEquals(original, CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE,
            personalization = photo.copy(personalizationEnabled = false)))
    }
    @Test fun `personalization cannot invent a missing district guide`() {
        assertTrue(CityPersonalityProvider.getPersonality("İzmir", "Bilinmeyen ilçe", TripType.CULTURE, photo).experiences.isEmpty())
    }
    @Test fun `published and bundled experiences use the same selector`() {
        val candidates = listOf(
            TravelGuideExperience("Kültür", "test", "local", "https://goturkiye.com/a", setOf(TripType.CULTURE)),
            TravelGuideExperience("Diğer kültür", "test", "local", "https://goturkiye.com/b", setOf(TripType.CULTURE)),
            TravelGuideExperience("Fotoğraf", "test", "local", "https://goturkiye.com/c", setOf(TripType.PHOTOGRAPHY)))
        val guide = PublishedTravelGuide("İstanbul", version = 1, validUntilEpochMs = Long.MAX_VALUE,
            description = "local", visit = "local", food = "local", experience = "local",
            experiences = candidates.map { PublishedGuideExperience(it.title, it.category, it.description, it.sourceUrl, it.preferredTripTypes.map { type -> type.name }) })
        assertEquals(selectGuideExperiences(candidates, TripType.CULTURE, photo.travelInterests()), guide.selected(TripType.CULTURE, photo.travelInterests()))
    }
    @Test fun `unknown and health identifiers do not change experience affinity`() {
        val base = LocalTravelGuides.experiences("İstanbul", null, TripType.CULTURE)
        assertEquals(base, LocalTravelGuides.experiences("İstanbul", null, TripType.CULTURE, setOf("migren", "polen", "unknown")))
    }
    @Test fun `analysis and visible guide select the same experiences`() {
        val visible = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE, personalization = photo)
        val answer = suggestion(photo).substringAfter("YEREL TAVSİYE|")
        visible.experiences.forEach { assertTrue(answer, answer.contains(it.title)) }
        assertFalse(answer.contains("Atlas Pasajı"))
    }
    @Test fun `personal packing is shared and removed immediately on optout`() {
        assertTrue(suggestion(photo).contains(personalizedTravelPacking(photo)))
        assertEquals("", personalizedTravelPacking(photo.copy(personalizationEnabled = false)))
    }
    @Test fun `opting out never suppresses general weather preparation`() {
        val result = TravelAiHelper.generateTravelAiSuggestion("İstanbul", TripType.CULTURE,
            snapshot.copy(minTemp = 5.0, maxTemp = 25.0), null, 2,
            personalization = photo.copy(personalizationEnabled = false))
        assertTrue(result, result.contains("sıcaklık farkı fazla"))
        assertFalse(result.contains("Uyku tulumu"))
        assertFalse(result.contains("Yedek piller"))
    }
}
