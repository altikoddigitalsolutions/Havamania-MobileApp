package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class BackendChatbotAskRequest(
    val question: String
)

@Serializable
data class BackendChatbotAskResponse(
    val answer: String,
    val used_messages_today: Int,
    val remaining_messages_today: Int,
    val is_premium: Boolean
)

interface BackendChatbotService {
    @POST("v1/chatbot/ask")
    suspend fun ask(
        @Header("Authorization") authorization: String,
        @Body request: BackendChatbotAskRequest
    ): Response<BackendChatbotAskResponse>
}

object BackendChatbotFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(): BackendChatbotService {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.havamania.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendChatbotService::class.java)
    }
}
