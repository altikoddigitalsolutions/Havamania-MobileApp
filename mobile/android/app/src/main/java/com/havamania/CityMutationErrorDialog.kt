package com.havamania

import androidx.compose.runtime.Composable
import com.havamania.ui.theme.HavamaniaDialog

@Composable
internal fun CityMutationErrorDialog(visible: Boolean, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null, details: String? = null) {
    if (!visible) return
    HavamaniaDialog(
        onDismissRequest = onDismiss,
        title = "Konum işlemi tamamlanamadı",
        text = details ?: "Değişiklik cihazınıza kaydedilmiş olabilir; bulut eşitlemesi tamamlanmamış olabilir. Bağlantınızı ve kayıtlı konumlarınızı kontrol edin.",
        confirmText = if (onRetry == null) "Tamam" else "Eşitlemeyi tekrar dene",
        onConfirm = onRetry ?: onDismiss,
        dismissText = if (onRetry == null) null else "Vazgeç"
    )
}
