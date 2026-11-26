package com.havamania

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.net.ssl.SSLException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

/** Public messages are fixed; upstream bodies and transport exception text are never echoed. */
internal class PhotoUploadFailure(message: String) : IOException(message)

internal fun assistantPhotoHttpFailure(status: Int, body: String?, retryAfter: String?): Exception {
    val code = runCatching {
        val json = Json.parseToJsonElement(body.orEmpty()) as? JsonObject
        (json?.get("error") ?: (json?.get("detail") as? JsonObject)?.get("error"))?.jsonPrimitive?.content
    }.getOrNull()
    val seconds = retryAfter?.trim()?.toLongOrNull()?.takeIf { it in 1..86400 }
    return when {
        status == 428 -> AssistantConsentRequiredException()
        status == 409 && code == "photos_disabled" -> PhotosDisabledException()
        status == 409 && code == "user_deleted" -> TerminalAssistantException("Bu hesap için fotoğraf gönderimi silme nedeniyle kapalı.")
        status == 409 && code == "account_deletion_pending" -> TerminalAssistantException("Hesap silme işlemi devam ederken fotoğraf gönderilemez.")
        status == 400 || status == 422 -> TerminalAssistantException("Fotoğraf isteği doğrulanamadı. Yeni bir mesaja başka bir fotoğraf ekleyin.")
        status == 401 -> TerminalAssistantException("Oturumunuz doğrulanamadı. Lütfen yeniden giriş yapın.")
        status == 403 -> TerminalAssistantException("Bu hesap için fotoğraf gönderme izni verilmiyor.")
        status == 409 -> TerminalAssistantException("Fotoğraf gönderme isteği doğrulanamadı. Yeni bir mesaja tekrar ekleyin.")
        status == 413 -> TerminalAssistantException("Fotoğraf çok büyük. Daha küçük bir fotoğraf seçin.")
        status == 415 -> TerminalAssistantException("Fotoğraf biçimi desteklenmiyor. Başka bir fotoğraf seçin.")
        status == 429 -> RateLimitedAssistantException(seconds)
        status == 503 && code == "busy" -> RateLimitedAssistantException(seconds ?: 5, "Fotoğraf hizmeti yoğun. Biraz sonra tekrar deneyin.")
        status == 503 && code == "consent_sync_required" -> PhotoUploadFailure("Fotoğraf izin metni sunucuda güncelleniyor. Biraz sonra tekrar deneyin.")
        status == 503 && code == "bridge_auth_failed" -> PhotoUploadFailure("Fotoğraf hizmetinin bağlantı ayarları doğrulanamadı. Daha sonra tekrar deneyin.")
        status == 503 && code == "upload_endpoint_unavailable" -> PhotoUploadFailure("Fotoğraf yükleme hizmeti sunucuda kullanılamıyor. Daha sonra tekrar deneyin.")
        status == 503 && code == "not_configured" -> PhotoUploadFailure("Fotoğraf hizmeti sunucuda henüz hazır değil. Daha sonra tekrar deneyin.")
        status in setOf(404, 405, 501) -> PhotoUploadFailure("Fotoğraf gönderme hizmeti bu adreste kullanılamıyor. Daha sonra tekrar deneyin.")
        status == 408 || status == 504 -> PhotoUploadFailure("Fotoğraf yüklemesi zamanında tamamlanamadı. Biraz sonra tekrar deneyin.")
        status == 502 -> PhotoUploadFailure("Fotoğraf hizmetinden geçerli yanıt alınamadı. Daha sonra tekrar deneyin.")
        else -> PhotoUploadFailure("Fotoğraf hizmetinde geçici bir sorun oluştu. Daha sonra tekrar deneyin.")
    }
}

internal suspend fun requestAssistantPhoto(
    api: BackendChatbotService, authorization: String, bytes: ByteArray
): Response<AssistantAttachmentReceipt> {
    if (bytes.isEmpty() || bytes.size > 8 * 1024 * 1024)
        throw TerminalAssistantException("Fotoğraf en fazla 8 MB olabilir.")
    val part = MultipartBody.Part.createFormData("file", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
    return try {
        api.uploadPhoto(authorization, part)
    } catch (e: CancellationException) { throw e }
    catch (_: SerializationException) { throw PhotoUploadFailure("Fotoğraf yükleme yanıtı doğrulanamadı.") }
    catch (_: SocketTimeoutException) { throw PhotoUploadFailure("Fotoğraf yüklemesi zamanında tamamlanamadı. Biraz sonra tekrar deneyin.") }
    catch (_: SSLException) { throw PhotoUploadFailure("Fotoğraf hizmetiyle güvenli bağlantı kurulamadı. Daha sonra tekrar deneyin.") }
    catch (_: IOException) { throw PhotoUploadFailure("Fotoğraf yüklenirken bağlantı kesildi veya hizmete ulaşılamadı. Biraz sonra tekrar deneyin.") }
}

internal fun assistantPhotoReceiptId(body: AssistantAttachmentReceipt?): String {
    if (body == null || body.size !in 1..(8 * 1024 * 1024) ||
        body.media_type !in setOf("image/jpeg", "image/png", "image/webp", "image/gif"))
        throw PhotoUploadFailure("Fotoğraf yüklemesi doğrulanamadı.")
    val id = runCatching { UUID.fromString(body.attachment_id).toString() }.getOrNull()
    if (id == null || !id.equals(body.attachment_id, ignoreCase = true))
        throw PhotoUploadFailure("Fotoğraf yüklemesi doğrulanamadı.")
    return id
}
