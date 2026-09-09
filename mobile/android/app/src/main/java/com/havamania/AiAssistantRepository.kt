package com.havamania

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class AiAssistantRepository(
    private val api: BackendChatbotService = BackendChatbotFactory.create()
) {
    suspend fun getBotConfig(): AltikodBotConfig? = withContext(Dispatchers.IO) {
        null
    }

    private fun isRejectionNotice(content: String): Boolean {
        val normalized = content.lowercase(java.util.Locale("tr")).trim().trimEnd('.', '!', ' ')
        return normalized == "lütfen geçerli bir soru sorunuz"
    }

    suspend fun getAssistantResponse(
        question: String,
        sessionId: String
    ): AssistantResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return@withContext AssistantResult.ConfigurationError
        }

        if (question.isBlank() || question.length > 4000) {
            return@withContext AssistantResult.QuestionRejected
        }

        if (BuildConfig.DEBUG) {
            Log.d("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_START | requestId=$requestId | messageLength=${question.length}")
        }

        try {
            var token = try {
                currentUser.getIdToken(false).await()?.token
            } catch (e: Exception) {
                null
            }

            if (token.isNullOrBlank()) {
                return@withContext AssistantResult.ConfigurationError
            }

            val request = BackendChatbotAskRequest(question = question)
            var response = api.ask("Bearer $token", request)

            // Handle 401 token refresh (max 1 retry)
            if (response.code() == 401) {
                try {
                    token = currentUser.getIdToken(true).await()?.token
                } catch (e: Exception) {
                    token = null
                }
                if (!token.isNullOrBlank()) {
                    response = api.ask("Bearer $token", request)
                }
            }

            if (!response.isSuccessful) {
                val code = response.code()
if (BuildConfig.DEBUG) {
                    Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | httpCode=$code")
}
                return@withContext AssistantResult.HttpError(code)
            }

            val body = response.body()
            val content = body?.answer?.trim() ?: ""

            if (content.isBlank()) {
if (BuildConfig.DEBUG) {
                    Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=EMPTY_RESPONSE")
}
                return@withContext AssistantResult.EmptyResponse
            }

            if (isRejectionNotice(content)) {
if (BuildConfig.DEBUG) {
                    Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=QUESTION_REJECTED")
}
                return@withContext AssistantResult.QuestionRejected
            }

            AssistantResult.Success(content)

        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
if (BuildConfig.DEBUG) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=TIMEOUT")
}
            AssistantResult.Timeout
        } catch (e: IOException) {
if (BuildConfig.DEBUG) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=NETWORK")
}
            AssistantResult.NetworkError
        } catch (e: Exception) {
if (BuildConfig.DEBUG) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=UNKNOWN")
}
            AssistantResult.UnknownError(e.javaClass.simpleName)
        }
    }
}
