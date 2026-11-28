package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun AssistantTripSuggestions(plans: List<TravelPlan>, owner: String, c: HavamaniaColors,
    enabled: Boolean, onAsk: (String) -> Boolean) {
    var choosing by rememberSaveable(owner) { mutableStateOf(false) }
    var failed by remember(owner) { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr")) }
    fun ask(id: String) {
        if (onAsk(id)) { choosing = false; failed = false } else failed = true
    }
    if (plans.size == 1) {
        ActiveTripContextCard(plans.single(), c, enabled) { ask(plans.single().id) }
    } else if (plans.size > 1) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = c.surface,
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${plans.size} seyahatin var", color = c.textPrimary, style = HavamaniaTheme.typography.bodyMedium)
                Text("Hangi seyahatin havasını öğrenmek istersin?", color = c.textSecondary)
                TextButton(onClick = { failed = false; choosing = true }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Seyahat seç") }
            }
        }
    }
    if (failed) Text("Soru gönderilemedi. Seyahat listesi değişmiş olabilir; yeniden seçebilirsin.",
        color = c.textSecondary, modifier = Modifier.padding(horizontal = 16.dp))
    if (choosing && plans.isNotEmpty()) {
        AlertDialog(onDismissRequest = { choosing = false; failed = false },
            title = { Text("Hangi seyahat?") },
            text = {
                Column {
                    if (failed) Text("Soru gönderilemedi. Güncel listeden yeniden seçebilirsin.")
                    LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(plans, key = { it.id }) { trip ->
                            OutlinedButton(onClick = { ask(trip.id) }, enabled = enabled,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(trip.displayName)
                                    Text(if (trip.startDate == trip.endDate) trip.startDate.format(formatter)
                                        else "${trip.startDate.format(formatter)} – ${trip.endDate.format(formatter)}")
                                    trip.originDisplayName?.let { Text("Çıkış: $it") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false; failed = false }) { Text("Vazgeç") } })
    }
    LaunchedEffect(plans.isEmpty()) { if (plans.isEmpty()) { choosing = false; failed = false } }
}
