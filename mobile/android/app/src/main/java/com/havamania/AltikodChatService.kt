package com.havamania

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://chatbot.altikodtech.com.tr/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AltikodChatService::class.java)
    }
}
