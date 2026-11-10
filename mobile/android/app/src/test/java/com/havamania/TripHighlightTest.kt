package com.havamania

import org.junit.Assert.*
import org.junit.Test

class TripHighlightTest {
    @Test fun missingSearchDoesNotHighlightEveryCard() {
        listOf(null, "", "   ").forEach { assertFalse(isTripHighlighted("trip", "Batman", null, it)) }
    }
    @Test fun explicitTargetOrMatchingSearchHighlightsCard() {
        assertTrue(isTripHighlighted("trip", "Batman", "trip", null))
        assertTrue(isTripHighlighted("trip", "Batman", null, " bat "))
        assertFalse(isTripHighlighted("trip", "Batman", "other", "Ankara"))
    }
}
