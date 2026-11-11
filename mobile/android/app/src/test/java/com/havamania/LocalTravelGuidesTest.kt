package com.havamania

import org.junit.Assert.*
import org.junit.Test

class LocalTravelGuidesTest {
    @Test fun districtUsesItsOwnPlacesAndFoodThroughTheNormalGenerator() {
        val result = TravelAiHelper.generateTravelAiSuggestion(
            city = "İzmir", tripType = TripType.VACATION,
            forecastSnapshot = null, previousSnapshot = null, daysUntilTrip = 2,
            district = "Çeşme"
        )
        assertTrue(result.contains("Çeşme Kalesi"))
        assertTrue(result.contains("kumru"))
        assertFalse(result.contains("Kemeraltı"))
        assertFalse(result.contains("en meşhur yerel yemeği"))
    }

    @Test fun missingDistrictDoesNotSilentlyReceiveCityCentreRecommendations() {
        val result = TravelAiHelper.generateTravelAiSuggestion(
            city = "İzmir", tripType = TripType.VACATION,
            forecastSnapshot = null, previousSnapshot = null, daysUntilTrip = 2,
            district = "Bilinmeyen ilçe"
        )
        assertTrue(result.contains("Bilinmeyen ilçe, İzmir"))
        assertFalse(result.contains("Kemeraltı"))
        assertNull(LocalTravelGuides.find("İzmir", "Çeşmealtı"))
        assertNull(LocalTravelGuides.find("Bursa", "Çeşme"))
        assertNull(LocalTravelGuides.find("İzmirli", null))
    }

    @Test fun namesAcceptTurkishAndAsciiSpellingsWithoutSubstringMatching() {
        assertEquals(LocalTravelGuides.find("İzmir", "Çeşme"), LocalTravelGuides.find(" IZMIR ", " CESME "))
        assertEquals(LocalTravelGuides.find("İstanbul", null), LocalTravelGuides.find("ISTANBUL", "İstanbul"))
        assertEquals(LocalTravelGuides.find("Balıkesir", "Ayvalık"), LocalTravelGuides.find("Balikesir", "Ayvalik"))
        assertNotNull(LocalTravelGuides.find("İzmir", "Urla"))
        assertNotNull(LocalTravelGuides.find("İzmir", "Alaçatı"))
    }

    @Test fun visibleGuideUsesTheSameDistrictContentAsTheAnalysis() {
        val content = CityPersonalityProvider.getPersonality("İzmir", "Çeşme")
        assertTrue(content.mustSee.single().contains("Çeşme Kalesi"))
        assertTrue(content.food.single().contains("kumru"))
        assertFalse(content.mustSee.any { it.contains("Şehir Merkezi") })
        assertTrue(CityPersonalityProvider.getPersonality("İzmir", "Bilinmeyen ilçe").mustSee.isEmpty())
        assertTrue(CityPersonalityProvider.getPersonality("Bursa").mustSee.single().contains("Koza Han"))
    }

    @Test fun tripTypePrioritizesDifferentExperiencesWithoutChangingTheDestination() {
        val photography = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.PHOTOGRAPHY)
        val culture = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE)
        assertTrue(photography.experiences.first().title.contains("fotoğrafla"))
        assertTrue(culture.experiences.any { it.title.contains("Atlas Pasajı") })
        assertFalse(culture.experiences.any { it.title.contains("fotoğrafla") })
        assertTrue(LocalTravelGuides.experiences("İzmir", "Bilinmeyen ilçe", TripType.CULTURE).isEmpty())
    }

    @Test fun analysisAndVisibleCardsUseTheSameExperienceSelection() {
        val visible = CityPersonalityProvider.getPersonality("İstanbul", tripType = TripType.CULTURE)
        val generated = TravelAiHelper.generateTravelAiSuggestion(
            "İstanbul", TripType.CULTURE, null, null, 2
        ).substringAfter("YEREL TAVSİYE|")
        visible.experiences.forEach {
            assertTrue(generated.contains(it.title))
            assertTrue(generated.contains(it.description))
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
        assertFalse(generated.contains("fotoğrafla"))
    }
}
