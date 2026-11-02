package com.havamania

import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

internal enum class CitySyncFailure { TEMPORARY, AUTHENTICATION, REJECTED, UNKNOWN }
internal class CitySyncTimeoutException : IOException("City synchronization response timed out")

internal fun citySyncFailure(error: Exception): CitySyncFailure = when (error) {
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.CANCELLED,
        FirebaseFirestoreException.Code.INTERNAL -> CitySyncFailure.TEMPORARY
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> CitySyncFailure.AUTHENTICATION
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.INVALID_ARGUMENT,
        FirebaseFirestoreException.Code.FAILED_PRECONDITION,
        FirebaseFirestoreException.Code.NOT_FOUND,
        FirebaseFirestoreException.Code.ALREADY_EXISTS,
        FirebaseFirestoreException.Code.OUT_OF_RANGE,
        FirebaseFirestoreException.Code.UNIMPLEMENTED -> CitySyncFailure.REJECTED
        else -> CitySyncFailure.UNKNOWN
    }
    is IOException -> CitySyncFailure.TEMPORARY
    else -> CitySyncFailure.UNKNOWN
}

internal fun citySyncAfterFailure(
    change: CitySyncChange, failure: CitySyncFailure, now: Long,
    jitter: (Int) -> Int = { kotlin.random.Random.nextInt(it + 1) }
): CitySyncChange {
    if (failure != CitySyncFailure.TEMPORARY) {
        return change.copy(blockedReason = failure.name, retryNotBefore = 0)
    }
    val attempts = change.failedAttempts.coerceIn(0, 30) + 1
    val base = (30L shl (attempts - 1).coerceAtMost(6)).coerceAtMost(1800)
    val delay = base + jitter((base / 4).toInt()).coerceIn(0, (base / 4).toInt())
    return change.copy(failedAttempts = attempts, blockedReason = null,
        retryNotBefore = now.coerceIn(0, Long.MAX_VALUE - delay) + delay)
}

internal class CitySyncWaitingException(val blocked: String?, val retryAt: Long) :
    Exception("City synchronization is waiting for recovery")

internal fun citySyncErrorText(error: Exception, now: Long = System.currentTimeMillis() / 1000): String {
    val category = if (error is CitySyncWaitingException) error.blocked else citySyncFailure(error).name
    return when (category) {
        "AUTHENTICATION" -> "Konumlar eşitlenemedi. Oturumunuzu kontrol edip yeniden giriş yapın. Yerel kayıtlarınız korunuyor."
        "REJECTED" -> "Sunucu konum işlemini kabul etmedi. Erişim veya yapılandırma sorunu giderildikten sonra eşitlemeyi tekrar deneyin."
        "UNKNOWN" -> "Konum eşitlemesi güvenli biçimde durduruldu. Sorun sürerse destek ile iletişime geçin; kayıtlarınız silinmedi."
        else -> if (error is CitySyncWaitingException) {
            val seconds = (error.retryAt.coerceAtLeast(0) - now.coerceAtLeast(0)).coerceAtLeast(0)
            val minutes = seconds / 60 + if (seconds % 60 > 0) 1 else 0
            "Yeniden denemeden önce yaklaşık ${minutes.coerceAtLeast(1)} dakika bekleyin. Süre dolunca eşitlemeyi tekrar deneyin; bu pencere otomatik gönderim başlatmaz."
        } else "Konum eşitlemesi tamamlanamadı. Bağlantınızı kontrol edip kısa bir süre sonra tekrar deneyin. Yerel kayıtlarınızı da kontrol edin."
    }
}
