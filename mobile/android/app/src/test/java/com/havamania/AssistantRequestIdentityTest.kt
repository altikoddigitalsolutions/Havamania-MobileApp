package com.havamania

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AssistantRequestIdentityTest {
    @Test fun retryUsesOriginalMessageWhileANewIdenticalQuestionGetsANewId() {
        val first = assistantMessageForSend(emptyList(), "Bir öneri sun", false, null)!!
        val retry = assistantMessageForSend(listOf(first), first.text, true, first.id)!!
        val another = assistantMessageForSend(listOf(first), first.text, false, null)!!
        assertEquals(first, retry)
        assertNotEquals(first.id, another.id)
        val request = BackendChatbotAskRequest(first.text, retry.id)
        val serialized = Json.encodeToString(request)
        assertEquals(first.id, Json.decodeFromString<BackendChatbotAskRequest>(serialized).request_id)
    }

    @Test fun retryCannotBorrowAnotherMessageOrPreviousAccountsMissingMessage() {
        val first = AltikodChatMessage(text = "Soru", isUser = true)
        assertNull(assistantMessageForSend(emptyList(), first.text, true, first.id))
        assertNull(assistantMessageForSend(listOf(first), "Başka soru", true, first.id))
        assertNull(assistantMessageForSend(listOf(first), first.text, true, null))
        assertNull(assistantMessageForSend(listOf(first), first.text, true, "broken-id"))
        assertNull(assistantMessageForSend(listOf(first.copy(isUser = false)), first.text, true, first.id))
    }

    @Test fun savedErrorRetainsRequestIdAndOlderMessagesRemainReadable() {
        val user = AltikodChatMessage(text = "Soru", isUser = true)
        val error = AltikodChatMessage(text = "Ağ hatası", isFallback = true,
            retryPrompt = user.text, retryRequestId = user.id)
        val restored = Json.decodeFromString<AltikodChatMessage>(Json.encodeToString(error))
        assertEquals(user.id, restored.retryRequestId)
        assertNull(Json.decodeFromString<AltikodChatMessage>("""{"text":"Eski mesaj"}""").retryRequestId)
    }

    @Test fun conflictCodesAreDistinctAndMalformedErrorsRemainSafe() {
        assertEquals(AssistantResult.DuplicateRequest,
            assistantHttpFailure(409, """{"error":"duplicate_request"}"""))
        assertEquals(AssistantResult.RequestExpired,
            assistantHttpFailure(409, """{"error":"request_expired"}"""))
        assertEquals(AssistantResult.HttpError(409), assistantHttpFailure(409, "not-json"))
        assertEquals(AssistantResult.Unavailable,
            assistantHttpFailure(503, """{"error":"duplicate_request"}"""))
    }
    @Test fun dailyQuotaAndTransientRateLimitRemainDistinct() {
        assertEquals(AssistantResult.DailyLimit,
            assistantHttpFailure(429, """{"error":"daily_limit"}""", "60"))
        assertEquals(AssistantResult.RateLimited(12),
            assistantHttpFailure(429, """{"error":"rate_limited"}""", "12"))
        assertEquals(AssistantResult.RateLimited(null), assistantHttpFailure(429, null))
        listOf("-1", "0", "invalid", "99999999999999999999999", "86401").forEach {
            assertEquals(AssistantResult.RateLimited(null), assistantHttpFailure(429, "not-json", it))
        }
    }

    @Test fun serviceAndGatewayTimeoutHaveUsefulDistinctResults() {
        assertEquals(AssistantResult.Unavailable, assistantHttpFailure(502, null))
        assertEquals(AssistantResult.Unavailable, assistantHttpFailure(503, "private diagnostic"))
        assertEquals(AssistantResult.Timeout, assistantHttpFailure(504, null))
        assertEquals(AssistantResult.HttpError(401), assistantHttpFailure(401, null))
    }
}
