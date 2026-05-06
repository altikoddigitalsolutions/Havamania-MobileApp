package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

@Serializable
data class AltikodChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
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

@Serializable
data class AiHistoryItem(
    val id: String,
    val title: String,
    val summary: String,
    val messages: List<AltikodChatMessage>,
    val createdAt: Long
)

interface AltikodChatService {
    @GET("api/widget/{bot_id}/config")
    suspend fun getConfig(@Path("bot_id") botId: String): AltikodBotConfig

    @POST("api/widget/{bot_id}/chat")
    suspend fun sendMessage(@Path("bot_id") botId: String, @Body request: AltikodChatRequest): AltikodChatResponse
}

object AltikodChatFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(): AltikodChatService {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logger).build()

        return Retrofit.Builder()
            .baseUrl("https://chatbot.altikodtech.com.tr/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AltikodChatService::class.java)
    }
}
