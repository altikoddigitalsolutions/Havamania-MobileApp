package com.havamania

import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun assistantMessageForSend(
    messages: List<AltikodChatMessage>, prompt: String, isRetry: Boolean, requestId: String?
): AltikodChatMessage? {
    if (!isRetry) return AltikodChatMessage(text = prompt, isUser = true)
    if (requestId == null || runCatching { UUID.fromString(requestId) }.isFailure) return null
    return messages.firstOrNull { it.isUser && it.id == requestId && it.text == prompt }
}

internal fun assistantHttpFailure(status: Int, body: String?, retryAfter: String? = null): AssistantResult {
    val code = runCatching {
        Json.parseToJsonElement(body.orEmpty()).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()
    return when {
        status == 409 && code == "conversation_conflict" -> AssistantResult.ConversationConflict
        status == 409 && code == "mode_disabled" -> AssistantResult.ModeDisabled
        status == 413 -> AssistantResult.TooLong
        status == 428 -> AssistantResult.ConsentRequired
        status == 429 && code == "daily_limit" -> AssistantResult.DailyLimit
        status == 429 -> AssistantResult.RateLimited(retryAfter?.trim()?.toLongOrNull()?.takeIf { it in 1..86400 })
        status == 502 || status == 503 -> AssistantResult.Unavailable
        status == 504 -> AssistantResult.Timeout
        status == 409 && code == "duplicate_request" -> AssistantResult.DuplicateRequest
        status == 409 && code == "request_expired" -> AssistantResult.RequestExpired
        else -> AssistantResult.HttpError(status)
    }
}

internal class TerminalAssistantException(message: String) : Exception(message)

internal class RateLimitedAssistantException(val seconds: Long?) : Exception(
    seconds?.let { "Çok hızlı mesaj gönderdiniz. $it saniye sonra tekrar deneyebilirsiniz." }
        ?: "Çok hızlı mesaj gönderdiniz. Lütfen kısa bir süre bekleyin."
)

internal fun isLocalAssistantPrompt(prompt: String): Boolean =
    prompt.trim().lowercase(java.util.Locale.forLanguageTag("tr")) in listOf("merhaba", "selam", "hi", "hello", "hey", "günaydın", "iyi günler") ||
        AiIntentParser.isWeatherQuery(prompt) || AiIntentParser.detectDate(prompt) != null
