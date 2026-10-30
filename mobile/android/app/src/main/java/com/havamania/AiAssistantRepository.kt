package com.havamania

import android.util.Log
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.net.SocketTimeoutException
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
        sessionId: String,
        requestId: String,
        consent: AssistantConsentDocument,
        tone: String,
        personalization: Boolean,
        interests: List<String>,
        context: AssistantClientContext
    ): AssistantResult = withContext(Dispatchers.IO) {

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return@withContext AssistantResult.ConfigurationError
        }

        if (question.length > 4000) return@withContext AssistantResult.TooLong
        if (question.isBlank()) {
            return@withContext AssistantResult.QuestionRejected
        }

        if (BuildConfig.DEBUG) {
            Log.d("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_START | requestId=$requestId | messageLength=${question.length}")
        }

        try {
            var token = try {
                currentUser.getIdToken(false).await()?.token
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                null
            }

            if (token.isNullOrBlank()) {
                return@withContext AssistantResult.ConfigurationError
            }

            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == currentUser.uid)
            var request = BackendChatbotAskRequest(question = question, request_id = requestId,
                consent_version = consent.version, health_consent = consent.health_accepted && personalization,
                health_consent_version = consent.health?.version.takeIf { consent.health_accepted && personalization },
                conversation_id = sessionId, tone = tone, personalization_enabled = personalization,
                selected_interests = if (personalization) interests else null, context = context)
            if (AndroidAssistantWithdrawalStore().pending(currentUser.uid)) return@withContext AssistantResult.ConsentRequired
            request = applyAssistantHealthPrivacy(request, AndroidAssistantWithdrawalStore().healthBlocked(currentUser.uid))
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == currentUser.uid)
            var response = api.ask("Bearer $token", request)

            // Handle 401 token refresh (max 1 retry)
            if (response.code() == 401) {
                try {
                    token = currentUser.getIdToken(true).await()?.token
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    token = null
                }
                if (!token.isNullOrBlank()) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == currentUser.uid)
                    if (AndroidAssistantWithdrawalStore().pending(currentUser.uid)) return@withContext AssistantResult.ConsentRequired
                    request = applyAssistantHealthPrivacy(request, AndroidAssistantWithdrawalStore().healthBlocked(currentUser.uid))
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    check(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == currentUser.uid)
                    response = api.ask("Bearer $token", request)
                }
            }

            if (!response.isSuccessful) {
                val code = response.code()
if (BuildConfig.DEBUG) {
                    Log.e("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_FAILED | requestId=$requestId | httpCode=$code")
}
                return@withContext assistantHttpFailure(code, response.errorBody()?.string(), response.headers()["Retry-After"])
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

            if (body?.conversation_id != null && body.conversation_id != sessionId) {
                return@withContext AssistantResult.ParseError
            }
            AssistantResult.Success(content, body?.message_id, body?.disclaimer)

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
