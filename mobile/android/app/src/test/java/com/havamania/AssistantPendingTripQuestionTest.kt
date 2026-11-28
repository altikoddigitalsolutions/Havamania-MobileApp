package com.havamania

import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantPendingTripQuestionTest {
    private val day = LocalDate.of(2026, 11, 28)
    private val trip = TravelPlan(id = "a", userId = "owner", city = "Balıkesir", district = "Ayvalık",
        startDate = day, endDate = day.plusDays(1))
    private val question = assistantTripWeatherQuestion(listOf(trip), "owner", "a", day)!!
    private fun current(plans: List<TravelPlan>, owner: String = "owner", today: LocalDate = day) =
        assistantTripQuestionStillCurrent(plans, owner, "a", question, today)

    @Test fun unchangedSelectionSurvivesListReorderingAndUnrelatedEdits() {
        assertTrue(current(listOf(trip.copy(userNote = "changed note"), trip.copy(id = "b"))))
        assertTrue(current(listOf(trip.copy(id = "b"), trip)))
    }
    @Test fun changedPlaceOrDateCannotReleaseOldQuestionAfterConsent() {
        for (edited in listOf(trip.copy(city = "İzmir"), trip.copy(district = "Edremit"),
            trip.copy(startDate = day.plusDays(1)), trip.copy(endDate = day.plusDays(2)))) {
            assertFalse(current(listOf(edited)))
        }
    }
    @Test fun deletedArchivedForeignOrExpiredSelectionNeverFallsBackToAnotherTrip() {
        val other = trip.copy(id = "other")
        assertFalse(current(listOf(other)))
        assertFalse(current(listOf(trip.copy(isArchived = true), other)))
        assertFalse(current(listOf(trip.copy(userId = "foreign"), other)))
        assertFalse(current(listOf(trip, other), owner = "foreign"))
        assertFalse(current(listOf(trip), today = day.plusDays(2)))
    }
    @Test fun selectedIdentitySurvivesSavedMessageAndRetryLookup() {
        val id = java.util.UUID.randomUUID().toString()
        val saved = AltikodChatMessage(id = id, text = question, isUser = true, selectedTripId = trip.id)
        val restored = Json.decodeFromString<AltikodChatMessage>(Json.encodeToString(saved))
        val retry = assistantMessageForSend(listOf(restored), question, true, id)!!
        assertEquals(trip.id, retry.selectedTripId)
        assertFalse(assistantTripQuestionStillCurrent(listOf(trip.copy(district = "Edremit")),
            "owner", retry.selectedTripId!!, retry.text, day))
    }
    @Test fun oldMessagesWithoutSelectionRemainReadableAndGenericQuestionsHaveNoSelection() {
        val old = Json.decodeFromString<AltikodChatMessage>("""{"text":"Eski soru","isUser":true}""")
        assertNull(old.selectedTripId)
        assertNull(AltikodChatMessage(text = "Merhaba", isUser = true).selectedTripId)
    }
}
