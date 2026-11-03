package com.havamania

import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CoroutineScope
import java.io.IOException

internal class AvatarMutationController(
    scope: CoroutineScope, private val currentOwner: () -> String?,
    private val writesAllowed: () -> Boolean, onBusy: (Boolean) -> Unit,
    private val onSuccess: () -> Unit, private val onError: (String) -> Unit,
    timeoutMillis: Long = 60000
) {
    private val runner = AssistantTaskRunner(scope, { currentOwner() ?: "legacy" }, onBusy, timeoutMillis)
    fun reset() = runner.reset()
    fun start(operation: suspend (String) -> Unit): Boolean {
        if (currentOwner() == null || !writesAllowed()) return false
        return runner.start(operation = { owner ->
            runAccountProfileWrite(owner, currentOwner, writesAllowed, { operation(owner) })
        }, onSuccess = { if (writesAllowed()) onSuccess() },
            onError = { if (writesAllowed()) onError(avatarErrorMessage(it)) })
    }
}

internal class AvatarImagePreparationException : Exception("Avatar preparation failed")

internal fun avatarErrorMessage(error: Exception): String = when {
    error is AvatarSourceTooLargeException -> "Fotoğraf çok büyük. En fazla 20 MB boyutunda bir fotoğraf seçin."
    error is AvatarImagePreparationException -> "Fotoğraf işlenemedi. Lütfen başka bir fotoğraf seçin."
    error is StorageException && error.errorCode == StorageException.ERROR_NOT_AUTHORIZED ->
        "Fotoğraf işlemi için erişim izni alınamadı. Oturumunuzu kontrol edin."
    error is StorageException && (error.errorCode == StorageException.ERROR_QUOTA_EXCEEDED || error.httpResultCode == 402) ->
        "Fotoğraf hizmeti şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."
    error is IOException -> "Fotoğraf işlemi tamamlanamadı. Bağlantınızı kontrol edip tekrar deneyin."
    error is SecurityException -> "Seçilen fotoğrafa erişilemiyor. Fotoğrafı yeniden seçin."
    else -> "Fotoğraf işlemi tamamlanamadı. Lütfen tekrar deneyin."
}
