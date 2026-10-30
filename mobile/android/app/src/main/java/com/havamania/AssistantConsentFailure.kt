package com.havamania

internal enum class ConsentFailure { AUTH, FORBIDDEN, UNAVAILABLE, RATE_LIMIT, NETWORK, TIMEOUT, INVALID, UNKNOWN }
internal class AssistantConsentServiceException(val failure: ConsentFailure) : Exception()
internal class AssistantWithdrawalPendingException : Exception()
internal enum class ConsentRetry { LOAD, REVOKE }

internal fun consentHttpFailure(status: Int) = AssistantConsentServiceException(when (status) {
    401 -> ConsentFailure.AUTH
    403 -> ConsentFailure.FORBIDDEN
    404, 405, 500, 502, 503 -> ConsentFailure.UNAVAILABLE
    429 -> ConsentFailure.RATE_LIMIT
    408, 504 -> ConsentFailure.TIMEOUT
    else -> ConsentFailure.UNKNOWN
})

internal fun consentFailureMessage(error: Exception): String = when {
    error is AssistantWithdrawalPendingException -> "Önceki izin geri çekme işlemi henüz tamamlanmadı."
    error is AssistantConsentServiceException -> when (error.failure) {
        ConsentFailure.AUTH -> "Oturum doğrulanamadı. Yeniden giriş yapmanız gerekebilir."
        ConsentFailure.FORBIDDEN -> "Bu hesap için izin işlemine erişim sağlanamadı."
        ConsentFailure.UNAVAILABLE -> "Asistan izin hizmeti şu anda kullanılamıyor. Daha sonra yeniden deneyin."
        ConsentFailure.RATE_LIMIT -> "Çok sık istek gönderildi. Bir süre bekleyip yeniden deneyin."
        ConsentFailure.TIMEOUT -> "İzin hizmeti zamanında yanıt vermedi. Yeniden deneyebilirsiniz."
        ConsentFailure.INVALID -> "İzin hizmetinden geçerli yanıt alınamadı."
        ConsentFailure.NETWORK -> "İzin hizmetine bağlantı kurulamadı. İnternet bağlantınızı kontrol edin."
        ConsentFailure.UNKNOWN -> "İzin işlemi tamamlanamadı. Daha sonra yeniden deneyin."
    }
    error is java.net.SocketTimeoutException -> "İzin hizmeti zamanında yanıt vermedi. Yeniden deneyebilirsiniz."
    error is kotlinx.serialization.SerializationException -> "İzin hizmetinden geçerli yanıt alınamadı."
    error is com.google.firebase.auth.FirebaseAuthException -> "Oturum doğrulanamadı. Yeniden giriş yapmanız gerekebilir."
    error is com.google.firebase.FirebaseNetworkException -> "İzin hizmetine bağlantı kurulamadı. İnternet bağlantınızı kontrol edin."
    error is java.io.IOException -> "İzin hizmetine bağlantı kurulamadı. İnternet bağlantınızı kontrol edin."
    else -> "İzin işlemi doğrulanamadı. Daha sonra yeniden deneyin."
}
