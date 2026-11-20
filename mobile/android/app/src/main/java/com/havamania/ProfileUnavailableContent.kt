package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.delay

/** A slow/missing profile must leave a useful exit, without treating cache misses as server absence. */
@Composable
internal fun ProfileUnavailableContent(owner: String?, state: ProfileState, attempt: Int,
    onRetry: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val waiting = owner != null && state !is ProfileState.Error
    var slow by remember(owner, attempt, waiting) { mutableStateOf(false) }
    LaunchedEffect(owner, attempt, waiting) {
        if (waiting) { delay(8_000); slow = true }
    }
    val colors = HavamaniaTheme.colors
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (waiting && !slow) {
            CircularProgressIndicator(color = colors.accent,
                modifier = Modifier.semantics { contentDescription = "Profil bilgileri yükleniyor" })
            Spacer(Modifier.height(20.dp))
        }
        Text(when {
            owner == null -> "Oturum açık değil"
            state is ProfileState.Error -> "Profil yüklenemedi"
            slow -> "Profil henüz yüklenemedi"
            else -> "Profil yükleniyor…"
        }, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(12.dp))
        Text(when {
            owner == null -> "Profilini görmek için hesabına giriş yapabilirsin."
            state is ProfileState.Error -> state.message
            slow -> "Bağlantını kontrol edip yeniden deneyebilirsin. Bağlantı kurulduğunda profilin otomatik olarak güncellenir."
            else -> "Hesabına ait bilgiler alınıyor."
        }, color = colors.textSecondary, textAlign = TextAlign.Center)
        if (owner != null && (!waiting || slow)) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry) { Text("Tekrar dene") }
        }
        TextButton(onClick = onBack) { Text("Geri dön") }
    }
}