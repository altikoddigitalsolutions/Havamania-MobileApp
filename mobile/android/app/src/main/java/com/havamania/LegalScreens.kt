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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Gizlilik Politikası", onBack = onBack) {
        LegalText("Havamania, gizliliğinize önem verir. Bu politika, verilerinizin nasıl işlendiğini açıklar.")
        LegalSection("Toplanan Veriler")
        LegalText("Hava durumu tahmini sunmak için konum verileriniz, asistan hizmeti için ise paylaştığınız tercihler işlenmektedir.")
        LegalSection("KVKK ve GDPR")
        LegalText("Verileriniz yerel mevzuatlara uygun olarak saklanmakta ve üçüncü taraflarla paylaşılmamaktadır.")
    }
}

@Composable
fun TermsOfUseScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "Kullanım Şartları", onBack = onBack) {
        LegalText("Havamania uygulamasını kullanarak aşağıdaki şartları kabul etmiş sayılırsınız.")
        LegalSection("Hizmet Kapsamı")
        LegalText("Hava durumu verileri üçüncü taraf sağlayıcılardan alınmaktadır ve doğruluk garantisi verilmemektedir.")
        LegalSection("Sorumluluk")
        LegalText("Uygulama verilerine dayanarak alınan kararlardan kullanıcı sorumludur.")
    }
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
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = HavamaniaTheme.colors.accent,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun LegalText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = HavamaniaTheme.colors.textPrimary.copy(alpha = 0.8f),
        lineHeight = 22.sp
    )
}
