package com.havamania

import org.junit.Assert.*
import org.junit.Test

class PublishedTravelGuideTest {
    private val guide = PublishedTravelGuide("İzmir", "Urla", 2, 10_000,
        "Urla rehberi", "Klazomenai", "Enginar", "Sanat Sokağı",
        listOf(PublishedGuideExperience("Üretimi keşfet", "Yerel yaşam", "Klazomenai'ye uğra.",
            "https://goturkiye.com/culturaljourneys/urla", listOf("CULTURE"))))

    @Test fun locationExpiryAndVersionAreValidated() {
        assertTrue(guide.validFor("izmir", "URLA", 1000))
        assertFalse(guide.validFor("İzmir", null, 1000))
        assertFalse(guide.validFor("İzmir", "Çeşme", 1000))
        assertFalse(guide.validFor("İzmir", "Urla", 10_000))
        assertFalse(guide.copy(version = 0).validFor("İzmir", "Urla", 1000))
    }

    @Test fun unsafeUrlsUnknownTypesAndExcessiveContentAreRejected() {
        for (url in listOf("http://goturkiye.com", "https://goturkiye.com.evil.test", "javascript:alert(1)",
            "https://user@goturkiye.com/path")) {
            assertFalse(officialGuideUrl(url))
            assertFalse(guide.copy(experiences = listOf(guide.experiences.first().copy(sourceUrl = url)))
                .validFor("İzmir", "Urla", 1000))
        }
        assertFalse(guide.copy(description = "x".repeat(4001)).validFor("İzmir", "Urla", 1000))
        assertFalse(guide.copy(experiences = listOf(guide.experiences.first().copy(tripTypes = listOf("UNKNOWN"))))
            .validFor("İzmir", "Urla", 1000))
        assertEquals("Klazomenai", guide.guide().visit)
        assertEquals("Üretimi keşfet", guide.selected(TripType.CULTURE).first().title)
    }
}
