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
    LegalScreenTemplate(title = "GİZLİLİK POLİTİKASI", onBack = onBack) {
        LegalText("Son Güncelleme: Ocak 2024\n\nHavamania olarak kişisel verilerinizin güvenliği önceliğimizdir. Bu metin, 6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) ve GDPR uyumluluğu kapsamında hazırlanmıştır.")

        LegalSection("1. İşlenen Veriler")
        LegalText("Hizmet sunabilmek adına şu veriler işlenmektedir:\n• Hassas Konum Verisi (Anlık hava durumu sunmak için)\n• Kullanıcı Tercihleri (İlgi alanları, profil bilgileri)\n• Cihaz Bilgileri (Performans optimizasyonu için)")

        LegalSection("2. Veri İşleme Amacı")
        LegalText("Konum verileriniz yalnızca size en yakın istasyon verisini sunmak ve seyahat rotanızdaki hava durumunu analiz etmek amacıyla kullanılır. Bu veriler kesinlikle anonimleştirilmeden üçüncü taraflara satılmaz.")

        LegalSection("3. Kullanıcı Hakları")
        LegalText("Verilerinizin silinmesini, kopyalanmasını veya düzeltilmesini her zaman Ayarlar > Verileri Sıfırla bölümünden talep edebilirsiniz.")

        LegalSection("4. İletişim")
        LegalText("Gizlilik ile ilgili sorularınız için: support@havamania.app")
    }
}

@Composable
fun TermsOfUseScreen(onBack: () -> Unit) {
    LegalScreenTemplate(title = "KULLANIM ŞARTLARI", onBack = onBack) {
        LegalText("Havamania uygulamasını kullanarak aşağıdaki şartları peşinen kabul etmiş sayılırsınız.")

        LegalSection("1. Veri Kaynakları")
        LegalText("Hava durumu tahminleri Open-Meteo ve diğer global veri sağlayıcılardan alınmaktadır. Verilerin %100 doğruluk payı garanti edilmez; meteorolojik koşullar anlık değişebilir.")

        LegalSection("2. Kullanım Amacı")
        LegalText("Uygulama bilgilendirme amaçlıdır. Özellikle ekstrem hava koşullarında (fırtına, sel vb.) resmi kurumların uyarıları dikkate alınmalıdır.")

        LegalSection("3. Premium Abonelik")
        LegalText("Premium özellikler yıllık veya aylık abonelik esasına dayanır. İptal işlemleri Store politikaları gereği kullanıcı tarafından yönetilir.")

        LegalSection("4. Sorumluluk Reddi")
        LegalText("Hava durumu tahminlerine dayanarak yapılan planlarda (seyahat, etkinlik vb.) oluşabilecek aksaklıklardan Havamania sorumlu tutulamaz.")
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
