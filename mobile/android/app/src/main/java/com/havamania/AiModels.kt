package com.havamania

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AltikodChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isFallback: Boolean = false,
    val retryPrompt: String? = null,
    val action: AssistantAction? = null
)

@Serializable
enum class AssistantActionType {
    CREATE_TRAVEL_PLAN,
    NONE
}

@Serializable
data class AssistantAction(
    val type: AssistantActionType,
    val label: String,
    val city: String? = null,
    val startDate: String? = null, // YYYY-MM-DD
    val tripName: String? = null
)

@Serializable
data class AltikodBotConfig(
    val name: String? = null,
    val welcome_message: String? = null,
    val theme_color: String? = null,
    val example_questions: List<String>? = null
)

@Serializable
data class AltikodChatRequest(
    val question: String,
    val session_id: String,
    val attachment_url: String? = null
)

@Serializable
data class AltikodChatResponse(
    val answer: String,
    val session_id: String
)
