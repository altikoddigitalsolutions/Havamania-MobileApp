package com.havamania

import org.junit.Assert.*
import org.junit.Test

class TravelGuideSelectionTest {
    @Test fun restoredChoiceKeepsClosedAndPreferredIdsForSameOwner() {
        val state = TravelGuideSelection().close("a").open("b")
        assertEquals(state, restoreGuideSelection("owner-a", encodeGuideSelection("owner-a", state)))
    }
    @Test fun savedChoicesCannotTransferToAnotherOwner() {
        val encoded = encodeGuideSelection("owner-a", TravelGuideSelection().close("a"))
        assertEquals(TravelGuideSelection(), restoreGuideSelection("owner-b", encoded))
        assertEquals(TravelGuideSelection(), restoreGuideSelection("owner-a", "broken saved state"))
    }
    @Test fun existingTripOpensWhenDelayedPlansArrive() {
        val state = TravelGuideSelection()
        assertNull(state.visibleId(emptyList(), true))
        assertEquals("a", state.visibleId(listOf("a"), true))
    }
    @Test fun closeSurvivesReloadAndDateRoundTrip() {
        val state = TravelGuideSelection().close("a")
        assertNull(state.visibleId(listOf("a"), true))
        assertEquals("b", state.visibleId(listOf("b"), true))
        assertNull(state.visibleId(listOf("a"), true))
    }
    @Test fun selectionUsesOnlyVisibleTripsAndExplicitOpenWins() {
        val state = TravelGuideSelection().open("b")
        assertEquals("b", state.visibleId(listOf("a", "b"), true))
        assertEquals("a", state.visibleId(listOf("a"), true))
        assertNull(state.visibleId(emptyList(), true))
    }
    @Test fun pastAndArchiveRequireExplicitOpen() {
        assertNull(TravelGuideSelection().visibleId(listOf("a"), false))
        assertEquals("a", TravelGuideSelection().open("a").visibleId(listOf("a"), false))
    }
    @Test fun savedOrManuallyReopenedTripOverridesEarlierClose() {
        val state = TravelGuideSelection().close("a").open("a")
        assertEquals("a", state.visibleId(listOf("a"), true))
    }
    @Test fun closingOneGuideDoesNotOpenAnotherVisibleTrip() {
        val state = TravelGuideSelection().open("b").close("b")
        assertNull(state.visibleId(listOf("a", "b"), true))
    }
    @Test fun freshAccountSessionHasNoPriorChoice() {
        val old = TravelGuideSelection().close("a").open("b")
        val fresh = TravelGuideSelection()
        assertEquals("b", old.visibleId(listOf("a", "b"), true))
        assertEquals("a", fresh.visibleId(listOf("a", "b"), true))
    }
}
