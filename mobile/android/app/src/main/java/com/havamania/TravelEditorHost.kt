package com.havamania

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun TravelEditorHost(
    context: TravelEditorContext,
    plans: List<TravelPlan>,
    owner: String,
    loading: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable (TravelPlan?, Boolean) -> Unit
) {
    if (!context.open || context.owner != owner) return
    val plan = if (context.editId != null) {
        plans.firstOrNull { it.id == context.editId && it.userId == owner }
    } else context.prefill()

    if (context.editId != null && plan == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Seyahat düzenleme") },
            text = { Text(if (loading) "Seyahat yükleniyor…" else "Seyahat bulunamadı veya henüz yüklenemedi.") },
            confirmButton = {
                TextButton(onClick = onRetry, enabled = !loading) { Text("Yeniden dene") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
        )
    } else {
        content(plan, context.editId == null && plan != null)
    }
}
