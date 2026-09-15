package com.havamania

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccountDeletionRecoveryScreen(
    state: AccountDeletionState,
    onRetry: (String, String) -> Unit,
    onDone: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    BackHandler { /* An accepted deletion cannot be cancelled by back navigation. */ }
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (state.complete) "Hesap silindi" else "Hesap silme", style = MaterialTheme.typography.headlineSmall)
            Text(state.message ?: "Silme isteğiniz işleniyor. Uygulamayı kapatsanız da tekrar açtığınızda devam edebilirsiniz.")
            if (state.busy) CircularProgressIndicator()
            if (state.needsAuthentication) {
                OutlinedTextField(email, { email = it }, label = { Text("E-posta") },
                    singleLine = true, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Şifre") },
                    visualTransformation = PasswordVisualTransformation(), singleLine = true,
                    enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            }
            Button(
                enabled = !state.busy && (!state.needsAuthentication || password.isNotBlank()),
                onClick = {
                    if (state.complete) onDone() else {
                        onRetry(email, password)
                        password = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (state.complete) "Giriş ekranına dön" else "Tekrar kontrol et") }
        }
    }
}
