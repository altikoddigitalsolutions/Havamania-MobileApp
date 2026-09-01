package com.havamania

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AiAssistantRepository(
    private val api: AltikodChatService = AltikodChatFactory.create()
) {
    private val botId = "6" // P3.8.5 RCA FIX: Verified INTEGER ID for this backend

    suspend fun getBotConfig(): AltikodBotConfig? = withContext(Dispatchers.IO) {
        try {
            api.getConfig(botId)
        } catch (e: Exception) {
            Log.e("ASSISTANT_DEBUG", "Config loading Error: ${e.message}")
            null
        }
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

        if (botId.isBlank() || botId == "YOUR_BOT_ID") {
            return@withContext AssistantResult.ConfigurationError
        }

        Log.d("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_START | requestId=$requestId | botId=$botId | messageLength=${question.length}")
        Log.v("ASSISTANT_DEBUG", "PAYLOAD_TRACE | question=${question.take(100)}... | session=$sessionId")

        try {
            val request = AltikodChatRequest(question = question, session_id = sessionId)
            val requestJson = Json.encodeToString(request)

            if (BuildConfig.DEBUG) {
                Log.d("ASSISTANT_DEBUG", "RAW_INPUT=[$question]")
                Log.d("ASSISTANT_DEBUG", "FINAL_QUESTION=[$question]")
                Log.d("ASSISTANT_DEBUG", "REQUEST_JSON=$requestJson")
            }

            val response = api.sendMessage(botId, request)

            if (BuildConfig.DEBUG) {
                Log.d("ASSISTANT_DEBUG", "HTTP_STATUS=200")
                Log.d("ASSISTANT_DEBUG", "RAW_RESPONSE=answer=[${response.answer.take(100)}...], session=${response.session_id}")
            }

            val content = response.answer.trim()

            if (content.isBlank()) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=EMPTY_RESPONSE")
                return@withContext AssistantResult.EmptyResponse
            }

            if (isRejectionNotice(content)) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=QUESTION_REJECTED")
                return@withContext AssistantResult.QuestionRejected
            }

            Log.d("ASSISTANT_DEBUG", "ASSISTANT_HTTP_RESULT | requestId=$requestId | httpCode=200 | successful=true")
            AssistantResult.Success(content)

        } catch (e: HttpException) {
            val code = e.code()
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | errorClass=HttpException | httpCode=$code | body=$errorBody")
            AssistantResult.HttpError(code)
        } catch (e: SocketTimeoutException) {
            Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | errorClass=SocketTimeoutException | stage=TIMEOUT")
            AssistantResult.Timeout
        } catch (e: IOException) {
            Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | errorClass=IOException | stage=NETWORK")
            AssistantResult.NetworkError
        } catch (e: Exception) {
            Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | errorClass=${e.javaClass.simpleName} | stage=UNKNOWN")
            AssistantResult.UnknownError(e.javaClass.simpleName)
        }
    }
}
