package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun TripSyncStatusPanel(
    pendingUploads: Int,
    pendingDeletions: Int,
    retrying: Boolean,
    isOnline: Boolean,
    uploadError: String?,
    deletionError: String?,
    onRetry: () -> Unit
) {
    if (pendingUploads <= 0 && pendingDeletions <= 0 && !retrying &&
        uploadError == null && deletionError == null) return

    val colors = HavamaniaTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pendingUploads > 0) Text(
            "$pendingUploads seyahat değişikliği eşitlenmeyi bekliyor.",
            color = colors.textPrimary
        )
        if (pendingDeletions > 0) Text(
            "$pendingDeletions seyahat silme işlemi tamamlanmayı bekliyor.",
            color = colors.textPrimary
        )
        uploadError?.let { Text(it, color = colors.textPrimary) }
        deletionError?.let { Text(it, color = colors.textPrimary) }
        TextButton(
            onClick = onRetry,
            enabled = isOnline && !retrying,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.accent,
                disabledContentColor = colors.textPrimary.copy(alpha = 0.6f)
            )
        ) {
            Text(if (retrying) "Eşitleniyor…" else if (isOnline) "Yeniden dene" else "Bağlantı bekleniyor")
        }
    }
}
