package com.havamania

import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TravelPlannerSavedStateTest {
    private val today = LocalDate.of(2026, 11, 26)
    @Test fun sameOwnerRestoresDateFilterAndConsumedFocus() {
        val state = TravelPlannerSavedState("a", "2026-12-03", TravelFilter.PAST, "old-notification")
        assertEquals(state, restoreTravelPlannerState("a", Json.encodeToString(state), today))
    }
    @Test fun foreignOrInvalidStateUsesSafeDefaults() {
        for (encoded in listOf("broken", Json.encodeToString(TravelPlannerSavedState("b", "2026-12-03")),
            Json.encodeToString(TravelPlannerSavedState("a", "bad-date")))) {
            assertEquals(TravelPlannerSavedState("a", today.toString()), restoreTravelPlannerState("a", encoded, today))
        }
    }
}
