package com.havamania

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
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
    suspend fun uploadPhoto(bytes: ByteArray, owner: String): String = withContext(Dispatchers.IO) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            ?: throw TerminalAssistantException("Fotoğraf göndermek için giriş yapın.")
        suspend fun checkOwner() {
            check(user.uid == owner && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid == owner)
            if (AndroidAssistantWithdrawalStore().pending(owner)) throw AssistantConsentRequiredException()
        }
        checkOwner()
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = okhttp3.MultipartBody.Part.createFormData("file", "photo.jpg", body)
        var token = user.getIdToken(false).await()?.token ?: throw java.io.IOException()
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        checkOwner()
        var response = api.uploadPhoto("Bearer $token", part)
        if (response.code() == 401) {
            token = user.getIdToken(true).await()?.token ?: throw java.io.IOException()
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            checkOwner()
            response = api.uploadPhoto("Bearer $token", part)
        }
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        checkOwner()
        if (!response.isSuccessful) {
            val error = response.errorBody()?.string().orEmpty()
            when {
                response.code() == 400 -> throw TerminalAssistantException("Fotoğraf doğrulanamadı. Yeni bir mesaja başka bir fotoğraf ekleyin.")
                response.code() == 428 -> throw AssistantConsentRequiredException()
                response.code() == 409 && error.contains("photos_disabled") -> throw PhotosDisabledException()
                response.code() == 409 -> throw TerminalAssistantException("Bu hesapla fotoğraf gönderilemedi. Yeni sohbet başlatın.")
                response.code() == 413 -> throw TerminalAssistantException("Fotoğraf çok büyük. Daha küçük bir fotoğraf seçin.")
                response.code() == 415 -> throw TerminalAssistantException("Fotoğraf biçimi desteklenmiyor. Başka bir fotoğraf seçin.")
                response.code() == 503 && error.contains("busy") -> throw RateLimitedAssistantException(
                    response.headers()["Retry-After"]?.toLongOrNull()?.takeIf { it in 1..86400 } ?: 5,
                    "Fotoğraf hizmeti yoğun. Biraz sonra tekrar deneyin.")
                response.code() == 429 -> throw RateLimitedAssistantException(response.headers()["Retry-After"]?.toLongOrNull()?.takeIf { it in 1..86400 })
                else -> throw java.io.IOException("Fotoğraf yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin.")
            }
        }
        val receipt = response.body() ?: throw java.io.IOException("Fotoğraf yüklemesi doğrulanamadı.")
        if (receipt.size !in 1..(8 * 1024 * 1024) || receipt.media_type !in setOf("image/jpeg", "image/png", "image/webp", "image/gif"))
            throw java.io.IOException("Fotoğraf yüklemesi doğrulanamadı.")
        runCatching { java.util.UUID.fromString(receipt.attachment_id).toString() }
            .getOrElse { throw java.io.IOException("Fotoğraf yüklemesi doğrulanamadı.") }
    }

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
        context: AssistantClientContext,
        attachmentIds: List<String> = emptyList()
    ): AssistantResult = withContext(Dispatchers.IO) {

        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return@withContext AssistantResult.ConfigurationError
        }

        if (assistantQuestionTooLong(question)) return@withContext AssistantResult.TooLong
        if (question.isBlank()) {
            return@withContext AssistantResult.QuestionRejected
        }

        if (BuildConfig.DEBUG) {
            Log.d("ASSISTANT_DEBUG", "ASSISTANT_REQUEST_START | requestId=$requestId")
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
                selected_interests = if (personalization) interests else null, context = context,
                attachment_ids = attachmentIds.takeIf { it.isNotEmpty() })
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
                val error = response.errorBody()?.string()
                if (code == 409 && error?.contains("photos_disabled") == true) throw PhotosDisabledException()
                return@withContext assistantHttpFailure(code, error, response.headers()["Retry-After"])
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
        } catch (e: PhotosDisabledException) {
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
