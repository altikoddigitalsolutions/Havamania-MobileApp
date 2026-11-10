package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun TripLoadStatusPanel(error: String?, loading: Boolean, onRetry: () -> Unit) {
    if (error == null && !loading) return
    val colors = HavamaniaTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(if (loading) "Seyahatler yükleniyor…" else error.orEmpty(), color = colors.textPrimary)
        TextButton(
            onClick = onRetry, enabled = !loading,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.accent,
                disabledContentColor = colors.textPrimary.copy(alpha = 0.6f))
        ) { Text(if (loading) "Yükleniyor…" else "Seyahatleri yeniden yükle") }
    }
}
