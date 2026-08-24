package com.havamania

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID

class AiAssistantRepository(
    private val api: AltikodChatService = AltikodChatFactory.create()
) {
    private val botId = "6" // RCA FIX: Verified integer ID

    suspend fun getBotConfig(): AltikodBotConfig? = withContext(Dispatchers.IO) {
        try {
            api.getConfig(botId)
        } catch (e: Exception) {
            Log.e("ASSISTANT_DEBUG", "Config loading Error: ${e.message}")
            null
        }
    }

    /**
     * Bot, gönderilen metni temizledikten sonra geriye anlamlı bir soru kalmadıysa
     * (boş/yalnızca boşluk gönderim ya da satır başı girintili — markdown kod bloğu
     * sayılan — payload) cevap yerine bu sabit uyarıyı döndürüyor. Canlı bot üzerinde
     * doğrulandı; baloncukta "cevap" gibi göstermek yerine hata olarak ele alıyoruz.
     */
    private fun isRejectionNotice(content: String): Boolean {
        val normalized = content.lowercase(java.util.Locale("tr")).trim().trimEnd('.', '!', ' ')
        return normalized == "lütfen geçerli bir soru sorunuz"
    }

    suspend fun getAssistantResponse(
        question: String,
        sessionId: String
    ): AssistantResult = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()

        // 1. Config Validation
        if (botId.isBlank() || botId == "YOUR_BOT_ID") {
            return@withContext AssistantResult.ConfigurationError
        }

        Log.d("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_START | requestId=$requestId | botId=$botId | messageLength=${question.length}")

        try {
            val request = AltikodChatRequest(question = question, session_id = sessionId)
            val response = api.sendMessage(botId, request)

            val content = response.answer.trim()

            if (content.isBlank()) {
                Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=EMPTY_RESPONSE")
                return@withContext AssistantResult.EmptyResponse
            }

            if (isRejectionNotice(content)) {
                Log.e(
                    "ASSISTANT_DEBUG",
                    "ASSISTANT_REQUEST_FAILED | requestId=$requestId | stage=QUESTION_REJECTED | " +
                        "questionLength=${question.length} | tail=${question.takeLast(80).replace("\n", "\\n")}"
                )
                return@withContext AssistantResult.QuestionRejected
            }

            Log.d("ASSISTANT_DEBUG", "ASSISTANT_HTTP_RESULT | requestId=$requestId | httpCode=200 | successful=true | contentPresent=true")
            AssistantResult.Success(content)

        } catch (e: HttpException) {
            val code = e.code()
            Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | errorClass=HttpException | httpCode=$code | stage=HTTP")
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
