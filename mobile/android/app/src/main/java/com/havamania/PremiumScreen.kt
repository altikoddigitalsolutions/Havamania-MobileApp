package com.havamania

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "HAVAMANIA PREMIUM",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(themeColors.accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.WorkspacePremium,
                    null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Sınırsız Atmosferik Deneyim",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "Tüm premium özellikleri açın ve hava durumunu bir adım önde takip edin.",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Features
            PremiumFeatureItem(
                icon = Icons.Rounded.Route,
                title = "Gelişmiş Seyahat Planlayıcı",
                desc = "15 günlük pencerede rotanızdaki tüm hava değişimlerini anlık takip edin."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.CompareArrows,
                title = "Tahmin Karşılaştırması",
                desc = "Dünkü tahminle bugünkü arasındaki farkları net bir şekilde görün."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.NotificationsActive,
                title = "Akıllı Bildirimler",
                desc = "Yağış, UV ve seyahat riskleri için kişiselleştirilmiş uyarılar alın."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.Backpack,
                title = "AI Destekli Valiz Önerisi",
                desc = "Hava durumuna göre yanınıza almanız gereken her şeyi sizin için listeleriz."
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Purchase Card
            HavamaniaGlassCard(alpha = 0.9f) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Yıllık Plan",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = themeColors.accent
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "₺199,99 / yıl",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = themeColors.textPrimary
                        )
                        Surface(
                            color = themeColors.success.copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Text(
                                "%50 İNDİRİM",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = themeColors.success
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    HavamaniaPrimaryButton(
                        text = "ABONELİĞİ BAŞLAT",
                        onClick = onPurchaseSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "İstediğiniz zaman ayarlardan iptal edebilirsiniz.",
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.textMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PremiumFeatureItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(themeColors.accent.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.textSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
