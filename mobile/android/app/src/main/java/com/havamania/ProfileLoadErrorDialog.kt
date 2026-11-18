package com.havamania

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.havamania.ui.theme.HavamaniaDialog

@Composable
internal fun ProfileLoadErrorDialog(message: String?, owner: String?, onRetry: () -> Unit, onDismiss: () -> Unit = {}) {
    if (message == null || owner == null) return
    var dismissed by rememberSaveable(owner, message) { mutableStateOf(false) }
    if (dismissed) return
    HavamaniaDialog(onDismissRequest = { dismissed = true; onDismiss() }, title = "Profil yüklenemedi",
        text = message, confirmText = "Tekrar dene", onConfirm = onRetry, dismissText = "Vazgeç")
}
