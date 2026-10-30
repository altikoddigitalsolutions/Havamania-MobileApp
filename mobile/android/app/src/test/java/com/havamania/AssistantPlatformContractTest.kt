package com.havamania

import com.havamania.ui.theme.AssistantTone
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantPlatformContractTest {
    private val now = 1_793_318_400_000L
    private fun trip() = TravelPlan(id = "trip", userId = "A", city = "Ankara", latitude = 39.9, longitude = 32.8,
        startDate = LocalDate.of(2026, 10, 30), endDate = LocalDate.of(2026, 11, 1), userNote = "PRIVATE_NOTE")

    @Test fun tonesUsePlatformEnumsInsteadOfPromptInstructions() {
        assertEquals("samimi", AssistantTone.SAMIMI.platformTone())
        assertEquals("resmi", AssistantTone.RESMI.platformTone())
        assertEquals("dengeli", AssistantTone.DENGELI.platformTone())
        assertEquals("kisa", AssistantTone.KISA_NET.platformTone())
        assertEquals("uzman", AssistantTone.DETAYLI_UZMAN.platformTone())
    }

    @Test fun serializedRequestKeepsConversationAndContextSeparateFromQuestion() {
        val request = BackendChatbotAskRequest("  Özgün soru?  ", request_id = "request", conversation_id = "conversation",
            tone = "kisa", personalization_enabled = true, selected_interests = listOf("Mimari"),
            context = AssistantClientContext(city = "Ankara"))
        val wire = Json.encodeToString(request)
        val restored = Json.decodeFromString<BackendChatbotAskRequest>(wire)
        assertEquals("  Özgün soru?  ", restored.question)
        assertEquals("conversation", restored.conversation_id)
        assertEquals("Ankara", restored.context!!.city)
        assertEquals(listOf("Mimari"), restored.selected_interests)
        assertFalse(wire.contains("api_key"))
        val cleared = Json.encodeToString(request.copy(selected_interests = emptyList()))
        assertTrue(cleared.contains("\"selected_interests\":[]"))
    }

    @Test fun staleOrDifferentCityWeatherCannotBePresentedAsCurrent() {
        val weather = WeatherData(cityName = "Ankara", temperature = "18°", timestamp = now, timezone = "Europe/Istanbul")
        val good = assistantClientContext("Ankara", weather, emptyList(), false, now)
        assertTrue(good.weather!!.contains("18.0 °C"))
        assertTrue(good.weather!!.contains("Europe/Istanbul"))
        val stale = assistantClientContext("Ankara", weather.copy(isStale = true), emptyList(), false, now)
        val different = assistantClientContext("İzmir", weather, emptyList(), false, now)
        val expired = assistantClientContext("Ankara", weather, emptyList(), false, now + 31 * 60 * 1000)
        listOf(stale, different, expired).forEach { assertFalse(it.weather!!.contains("18.0")) }
    }

    @Test fun optOutAndContextSnapshotExcludePrivateNotesAndCoordinates() {
        val optedOut = assistantClientContext("Ankara", null, listOf(trip()), false, now)
        assertNull(optedOut.trips)
        val allowed = assistantClientContext("Ankara", null, listOf(trip()), true, now)
        assertTrue(allowed.trips!!.contains("2026-10-30"))
        val wire = Json.encodeToString(allowed)
        assertFalse(wire.contains("PRIVATE_NOTE"))
        assertFalse(wire.contains("39.9"))
        assertFalse(wire.contains("userId"))
    }

    @Test fun replyMetadataSurvivesSavedHistoryAndOldMessagesRemainReadable() {
        val saved = AltikodChatMessage(text = "Cevap", platformMessageId = "message", disclaimer = "Güvenlik uyarısı")
        val restored = Json.decodeFromString<AltikodChatMessage>(Json.encodeToString(saved))
        assertEquals("message", restored.platformMessageId)
        assertEquals("Güvenlik uyarısı", restored.disclaimer)
        assertNull(Json.decodeFromString<AltikodChatMessage>("""{"text":"Eski"}""").platformMessageId)
    }

    @Test fun conversationConflictAndDisabledModeAreNotGenericRetryableErrors() {
        assertEquals(AssistantResult.ConversationConflict, assistantHttpFailure(409, """{"error":"conversation_conflict"}"""))
        assertEquals(AssistantResult.ModeDisabled, assistantHttpFailure(409, """{"error":"mode_disabled"}"""))
        assertEquals(AssistantResult.TooLong, assistantHttpFailure(413, null))
    }
}
