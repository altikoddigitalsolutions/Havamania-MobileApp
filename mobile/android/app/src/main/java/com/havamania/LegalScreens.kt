package com.havamania

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*

@Composable
fun KVKKScreen(onBack: () -> Unit) {
    LegalWebViewScreen(
        title = "KVKK AYDINLATMA METNİ",
        url = LegalUrls.KVKK,
        onBack = onBack
    )
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalWebViewScreen(
        title = "GİZLİLİK POLİTİKASI",
        url = LegalUrls.PRIVACY_POLICY,
        onBack = onBack
    )
}

@Composable
fun TermsOfUseScreen(onBack: () -> Unit) {
    LegalWebViewScreen(
        title = "KULLANIM KOŞULLARI",
        url = LegalUrls.TERMS_OF_USE,
        onBack = onBack
    )
}

@Composable
fun LegalScreenTemplate(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    HavamaniaScreen(
        topBar = { HavamaniaTopBar(title = title, onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            content = content
        )
    }
}

@Composable
fun LegalSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = HavamaniaTheme.colors.accent,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
    )
}

@Composable
fun LegalText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
        color = HavamaniaTheme.colors.textPrimary.copy(alpha = 0.85f)
    )
}
