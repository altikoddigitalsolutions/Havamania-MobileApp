package com.havamania

import org.junit.Assert.*
import org.junit.Test

class InterestSelectionTest {
    @Test fun retryUsesLatestListAndPreservesAnotherSelection() {
        val afterOtherClient = updateInterestSelection(listOf("weather"), "travel", true)
        assertEquals(listOf("weather", "travel", "photo"), updateInterestSelection(afterOtherClient, "photo", true))
    }
    @Test fun repeatedIntentIsIdempotent() {
        val selected = updateInterestSelection(emptyList(), "photo", true)
        assertEquals(selected, updateInterestSelection(selected, "photo", true))
        assertEquals(emptyList<String>(), updateInterestSelection(updateInterestSelection(selected, "photo", false), "photo", false))
    }
    @Test fun removalPreservesUnrelatedPreferences() {
        assertEquals(listOf("weather", "travel"), updateInterestSelection(listOf("weather", "photo", "travel"), "photo", false))
    }
    @Test fun invalidIdCannotCorruptCommaSeparatedLocalCache() {
        for (id in listOf("", "a,b", "x".repeat(101))) {
            try { updateInterestSelection(emptyList(), id, true); fail("Expected rejection") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
