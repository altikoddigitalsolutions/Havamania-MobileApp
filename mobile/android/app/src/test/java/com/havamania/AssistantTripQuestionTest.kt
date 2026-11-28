package com.havamania

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AssistantTripQuestionTest {
    private val today = LocalDate.of(2026, 11, 28)
    private fun trip(id: String, owner: String = "A") = TravelPlan(id = id, userId = owner,
        city = "Balıkesir", district = "Ayvalık", startDate = today, endDate = today.plusDays(2))
    @Test fun selectionUsesExactTripDistrictAndEntireDateRange() {
        val selected = trip("second").copy(startDate = today.plusDays(4), endDate = today.plusDays(6))
        val prompt = assistantTripWeatherQuestion(listOf(trip("first"), selected), "A", "second", today)!!
        assertTrue(prompt.contains("Ayvalık, Balıkesir"))
        assertTrue(prompt.contains("2026-12-02–2026-12-04"))
        assertFalse(prompt.contains("2026-11-28"))
    }
    @Test fun deletedForeignArchivedDemoExpiredAndInvalidRangeNeverFallBackToFirst() {
        val a = trip("first")
        for (bad in listOf(trip("target", "B"), trip("target").copy(isArchived = true),
            trip("target").copy(isDemo = true), trip("target").copy(endDate = today.minusDays(1)),
            trip("target").copy(startDate = today.plusDays(3)))) {
            assertNull(assistantTripWeatherQuestion(listOf(a, bad), "A", "target", today))
        }
        assertNull(assistantTripWeatherQuestion(listOf(a), "A", "deleted", today))
    }
    @Test fun singleDayAndOngoingTripAreSupportedWithoutInventingForecast() {
        val one = trip("one").copy(endDate = today)
        assertTrue(assistantTripWeatherQuestion(listOf(one), "A", "one", today)!!.contains("2026-11-28 tarihinde"))
        assertNotNull(assistantTripWeatherQuestion(listOf(trip("ongoing").copy(startDate = today.minusDays(1))), "A", "ongoing", today))
    }
    @Test fun duplicateIdentityIsRejectedAndEditedRecordIsReadAgain() {
        val old = trip("same")
        assertNull(assistantTripWeatherQuestion(listOf(old, old), "A", "same", today))
        val edited = old.copy(district = "Edremit")
        assertTrue(assistantTripWeatherQuestion(listOf(edited), "A", "same", today)!!.contains("Edremit"))
    }
}
