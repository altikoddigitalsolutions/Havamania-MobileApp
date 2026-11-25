package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AssistantSelectedInterestPrivacyTest {
    private val health = listOf("migren", "uv_hassasiyeti", "polen", "basinc_hassasiyeti")
    @Test fun `no independent health consent strips structured sensitive interests`() {
        val request = BackendChatbotAskRequest("Local", personalization_enabled = true, selected_interests = health + "kamp")
        assertEquals(listOf("kamp"), applyAssistantHealthPrivacy(request, false).selected_interests)
    }
    @Test fun `valid independent consent preserves opted in interests`() {
        val request = BackendChatbotAskRequest("Local", personalization_enabled = true, health_consent = true,
            health_consent_version = 2, selected_interests = health + "kamp")
        assertEquals(request.selected_interests, applyAssistantHealthPrivacy(request, false).selected_interests)
    }
    @Test fun `health withdrawal filters retry payload and preserves request identity`() {
        val request = BackendChatbotAskRequest("Local", request_id = "same-id", personalization_enabled = true,
            health_consent = true, health_consent_version = 2, selected_interests = health + "kamp")
        val result = applyAssistantHealthPrivacy(request, true)
        assertEquals(listOf("kamp"), result.selected_interests)
        assertEquals("same-id", result.request_id); assertEquals(request.question, result.question)
        assertFalse(result.health_consent); assertNull(result.health_consent_version)
    }
    @Test fun `personalization optout strips trips interests and health flags`() {
        val request = BackendChatbotAskRequest("Local", personalization_enabled = false, health_consent = true,
            health_consent_version = 2, selected_interests = health + "kamp",
            context = AssistantClientContext(city = "Bursa", trips = "PRIVATE_TRIP"))
        val result = applyAssistantHealthPrivacy(request, false)
        assertNull(result.selected_interests); assertNull(result.context?.trips)
        assertEquals("Bursa", result.context?.city); assertFalse(result.health_consent)
    }
}
