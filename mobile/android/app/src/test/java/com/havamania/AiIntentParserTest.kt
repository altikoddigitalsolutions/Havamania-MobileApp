package com.havamania

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiIntentParserTest {

    @Test
    fun `detectCity should return the correct city when it exists`() {
        assertEquals("Balıkesir", AiIntentParser.detectCity("Balıkesir hava durumu"))
        assertEquals("Ankara", AiIntentParser.detectCity("Ankara seyahatim nasıl?"))
        assertEquals("Uşak", AiIntentParser.detectCity("Uşak hava durumu"))
        assertEquals("Muş", AiIntentParser.detectCity("Muş hava durumu"))
    }

    @Test
    fun `detectCity should return null for similar substrings that are not cities`() {
        assertNull(AiIntentParser.detectCity("Hava nasıl olmuş?"))
        assertNull(AiIntentParser.detectCity("Konuşmuş olabiliriz"))
        assertNull(AiIntentParser.detectCity("Dolmuş geçer mi?"))
        assertNull(AiIntentParser.detectCity("Mutlaka şemsiye almalı mıyım?"))
        assertNull(AiIntentParser.detectCity("Hayvan dostu hava nasıl?"))
        assertNull(AiIntentParser.detectCity("Laboratuvar sonuçları"))
        assertNull(AiIntentParser.detectCity("Kuşak gibi rüzgar var"))
    }

    @Test
    fun `detectCity should handle multi-word inputs but match tokens`() {
        // "Muş" is in "Hava durumu muş" (if it was a token, which it isn't here, it's a suffix)
        assertNull(AiIntentParser.detectCity("Gidilmiş yerler"))
    }

    @Test
    fun `detectCity should return null when no city is present`() {
        assertNull(AiIntentParser.detectCity("Hava bugün nasıl?"))
    }
}
