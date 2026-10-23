package com.havamania

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun LocationSearchFeedback(status: LocationSearchStatus, onRetry: () -> Unit) {
    val message = when (status) {
        LocationSearchStatus.LOADING -> "Konumlar aranıyor…"
        LocationSearchStatus.EMPTY -> "Konum bulunamadı. Farklı bir ad deneyin."
        LocationSearchStatus.ERROR -> "Konumlar alınamadı. Bağlantınızı kontrol edip yeniden deneyin."
        else -> null
    }
    message?.let { Text(it, color = HavamaniaTheme.colors.textPrimary) }
    if (status == LocationSearchStatus.ERROR) {
        TextButton(onClick = onRetry) { Text("Yeniden dene", color = HavamaniaTheme.colors.accent) }
    }
}
