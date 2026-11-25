package com.havamania

import org.junit.Assert.*
import org.junit.Test

class ProfileLabelsTest {
    @Test fun `place names and unrelated word parts cannot create badges`() {
        listOf("Ankara'da yaşıyorum.", "Karaman ve Kars arasında çalışıyorum.",
            "Kararları rotasyonla yönetiyorum.", "Kampüste ders veriyorum.").forEach {
            assertTrue(it, generateBadges(it).isEmpty())
        }
    }
    @Test fun `rain does not imply snow preference`() {
        val badges = generateBadges("Yağmuru izlemeyi seviyorum.")
        assertTrue(badges.toString(), badges.contains("Yağmur meraklısı"))
        assertFalse(badges.any { it.contains("Kar") || it.contains("Kış") })
    }
    @Test fun `negative statements cannot create positive identity`() {
        listOf("Kampı sevmiyorum.", "Doğa yürüyüşlerinden hoşlanmıyorum.",
            "Motosiklet kullanmıyorum.", "Yağmurdan nefret ediyorum.",
            "Kar sevmem.", "Fırtınaları takip etmiyorum.").forEach {
            assertTrue(it, generateBadges(it).isEmpty())
            assertEquals(it, "Hava Meraklısı", generateAiPersonality(emptySet(), it))
        }
    }
    @Test fun `contrasting clauses preserve only affirmative topic`() {
        val badges = generateBadges("Kamp sevmiyorum ama fotoğraf çekmeyi seviyorum.")
        assertFalse(badges.contains("Doğa dostu"))
        assertTrue(badges.toString(), badges.contains("Fotoğraf meraklısı"))
    }
    @Test fun `Turkish storm and family forms are recognized`() {
        assertEquals("Atmosfer Avcısı", generateAiPersonality(emptySet(), "FIRTINALARI TAKİP EDİYORUM."))
        assertEquals("Aile Meteoroloğu", generateAiPersonality(emptySet(), "Çocuklarımla etkinlik planlıyorum."))
    }
    @Test fun `correct motorcycle spelling and legacy selected id both work`() {
        assertEquals("Yol Savaşçısı", generateAiPersonality(emptySet(), "Motosiklet kullanıyorum."))
        assertEquals("Yol Savaşçısı", generateAiPersonality(setOf("motorsiklet"), ""))
    }
    @Test fun `unknown information never implies expertise`() {
        assertTrue(generateBadges("Ben öğretmenim ve Ankara'da yaşıyorum.").isEmpty())
        assertEquals("Hava Meraklısı", generateAiPersonality((1..30).map { "unknown-$it" }.toSet(), ""))
    }
    @Test fun `a bare topic does not imply preference`() {
        listOf("kamp", "kar", "yağmur", "fotoğraf", "Motosiklet fiyatı nedir?").forEach {
            assertTrue(it, generateBadges(it).isEmpty())
        }
    }
    @Test fun `structured selection is authoritative and legacy ids are preserved`() {
        assertEquals("Doğa Gezgini", generateAiPersonality(setOf("kamp"), "Fırtınaları takip ediyorum."))
        assertTrue(generateBadges("Kamp sevmiyorum.", setOf("kamp")).contains("Doğa dostu"))
        assertTrue(generateBadges("", setOf("motorsiklet")).contains("Yol meraklısı"))
    }
    @Test fun `badge order is stable distinct and bounded`() {
        val ids = setOf("kamp", "trekking", "motorsiklet", "kayak", "drone", "meteoroloji")
        val first = generateBadges("Doğa yürüyüşü yapıyorum.", ids)
        assertEquals(3, first.size)
        assertEquals(first.distinct(), first)
        assertEquals(first, generateBadges("Doğa yürüyüşü yapıyorum.", ids.reversed().toSet()))
    }
    @Test fun `locale and decomposed Turkish letters do not change labels`() {
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            val first = generateAiPersonality(emptySet(), "ÇOCUKLARIMLA ETKİNLİK PLANLIYORUM.")
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            assertEquals(first, generateAiPersonality(emptySet(), "ÇOCUKLARIMLA ETKİNLİK PLANLIYORUM."))
            assertEquals("Aile Meteoroloğu", first)
        } finally { java.util.Locale.setDefault(previous) }
    }
    @Test fun `later negative clauses cancel earlier text inference`() {
        listOf("Kamp yapıyorum ama sevmiyorum.", "Kampı seviyorum ama kamp yapmıyorum.", "Kamp sevmiyorum ama kamp yapıyorum.").forEach {
            assertTrue(it, generateBadges(it).isEmpty())
            assertEquals("Hava Meraklısı", generateAiPersonality(emptySet(), it))
        }
        assertTrue(generateBadges("Fotoğraf seviyorum ama kamp sevmiyorum.").contains("Fotoğraf meraklısı"))
    }
    @Test fun `common positive suffixes and travel verbs remain supported`() {
        assertTrue(generateBadges("Yağmurdan hoşlanıyorum.").contains("Yağmur meraklısı"))
        assertTrue(generateBadges("Fotoğrafçılıkla ilgileniyorum.").contains("Fotoğraf meraklısı"))
        assertEquals("Yol Savaşçısı", generateAiPersonality(emptySet(), "Karavanla seyahat ediyorum."))
        assertFalse(generateBadges("Karavanla seyahat ediyorum.").contains("Kış meraklısı"))
    }
}
