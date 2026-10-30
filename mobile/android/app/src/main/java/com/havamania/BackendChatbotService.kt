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
import retrofit2.http.GET

@Serializable
data class BackendChatbotAskRequest(
    val question: String,
    val request_id: String? = null,
    val consent_version: Int? = null,
    val health_consent: Boolean = false,
    val health_consent_version: Int? = null,
    val conversation_id: String? = null,
    val mode: String = "genel",
    val tone: String? = null,
    val personalization_enabled: Boolean = false,
    val selected_interests: List<String>? = null,
    val context: AssistantClientContext? = null
)

@Serializable
data class BackendChatbotAskResponse(
    val answer: String,
    val conversation_id: String? = null,
    val message_id: String? = null,
    val disclaimer: String? = null,
    val used_messages_today: Int,
    val remaining_messages_today: Int,
    val is_premium: Boolean
)

interface BackendChatbotService {
    @GET("v1/chatbot/consent")
    suspend fun consent(@Header("Authorization") authorization: String): Response<AssistantConsentDocument>

    @POST("v1/chatbot/consent")
    suspend fun updateConsent(@Header("Authorization") authorization: String,
        @Body request: AssistantConsentUpdate): Response<AssistantConsentUpdateResult>

    @GET("v1/account/deletion/availability")
    suspend fun deletionAvailability(): Response<AccountDeletionAvailability>

    @POST("v1/account/deletion")
    suspend fun deleteAccount(
        @Header("Authorization") authorization: String?,
        @Body request: AccountDeletionRequest
    ): Response<AccountDeletionResponse>

    @POST("v1/chatbot/ask")
    suspend fun ask(
        @Header("Authorization") authorization: String,
        @Body request: BackendChatbotAskRequest
    ): Response<BackendChatbotAskResponse>
}

@Serializable
data class AssistantConsentUpdateResult(val assistant_accepted: Boolean, val health_accepted: Boolean)

@Serializable
data class AccountDeletionRequest(val continuation_key: String)

@Serializable
data class AccountDeletionResponse(val status: String)

@Serializable
data class AccountDeletionAvailability(val available: Boolean)

object BackendChatbotFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(): BackendChatbotService {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .callTimeout(75, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder()
                .header("X-App-Version", BuildConfig.VERSION_NAME).build()) }
            .addInterceptor(logger)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(65, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.havamania.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BackendChatbotService::class.java)
    }
}
