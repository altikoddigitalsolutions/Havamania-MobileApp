package com.havamania

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.havamania.ui.theme.HavamaniaTheme

@Composable
internal fun AssistantConsentDialog(state: AssistantConsentState, onSave: (Boolean) -> Unit,
    onRevoke: () -> Unit, onRetry: () -> Unit, onDismiss: () -> Unit) {
    if (!state.visible) return
    val colors = HavamaniaTheme.colors
    val doc = state.document
    var health by remember(doc?.text_hash, doc?.health?.text_hash, doc?.accepted) {
        mutableStateOf(doc?.accepted == true && doc.health_accepted)
    }
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = colors.surface, shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight(0.92f).padding(12.dp)) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Asistan izinleri", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Column(Modifier.padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bu izinler uzak yapay zekâ asistanı içindir. Cihazda hazırlanan hava yanıtları bu servise gönderilmez.",
                        color = colors.textSecondary)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    doc?.let {
                        ConsentText(it.text)
                        it.health?.let { separate ->
                            HorizontalDivider()
                            Text("Sağlık bilgileri — isteğe bağlı", style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary)
                            ConsentText(separate.text)
                            Row(Modifier.fillMaxWidth().toggleable(value = health, enabled = !state.busy,
                                role = Role.Checkbox, onValueChange = { checked -> health = checked }).padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = health, onCheckedChange = null, enabled = !state.busy)
                                Text("Profil sağlık hassasiyetlerimin asistana gönderilmesine onay veriyorum.",
                                    color = colors.textPrimary, modifier = Modifier.weight(1f))
                            }
                            Text("Bu kutuyu işaretlemeden de uzak asistanı kullanabilirsiniz.", color = colors.textSecondary)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (doc != null) Button(onClick = { onSave(health) }, enabled = !state.busy && doc.isValid(),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (doc.accepted) "Seçimleri kaydet" else "Onayla ve devam et")
                    } else TextButton(onClick = onRetry, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Tekrar dene")
                    }
                    TextButton(onClick = onRevoke, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Uzak asistan izinlerini geri çek")
                    }
                    TextButton(onClick = onDismiss, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("Vazgeç")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentText(text: String) {
    val colors = HavamaniaTheme.colors
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    Text(text, color = colors.textPrimary)
    val links = remember(text) { Regex("https://[^\\s<>()]+")
        .findAll(text).map { it.value.trimEnd('.', ',') }.distinct().toList() }
    links.forEach { link ->
        TextButton(onClick = {
            runCatching { uri.openUri(link) }.onFailure {
                Toast.makeText(context, "Bağlantı açılamadı.", Toast.LENGTH_SHORT).show()
            }
        }) { Text(link) }
    }
}
