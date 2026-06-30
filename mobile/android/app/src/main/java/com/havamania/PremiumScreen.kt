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
                title = "PREMIUM",
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

            // Premium Badge & Glow Effect
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(themeColors.accent.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
                Surface(
                    color = themeColors.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp),
                    border = BorderStroke(2.dp, themeColors.accent.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Hava Durumunda Sınırları Kaldır",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "AI destekli seyahat analizleri, kişiselleştirilmiş uyarılar ve reklamsız bir atmosfer.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = themeColors.textSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Features Grid
            PremiumFeatureItem(
                icon = Icons.Rounded.Route,
                title = "Akıllı Seyahat Analizi",
                desc = "15 günlük seyahat periyodunda valiz önerisinden rota güvenliğine her şey."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.AutoAwesome,
                title = "Hiper-Kişiselleştirme",
                desc = "Sağlık hassasiyetlerine ve ilgi alanlarına göre özelleşmiş AI asistanı."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.Compare,
                title = "Trend Karşılaştırması",
                desc = "Önceki tahminlerle güncel veriler arasındaki değişimleri anlık görün."
            )
            PremiumFeatureItem(
                icon = Icons.Rounded.NotificationsActive,
                title = "Kritik Hava Uyarıları",
                desc = "UV, fırtına ve ani yağışlarda telefonunuza özel, anlaşılır bildirimler."
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Price Card (Prominent & Modern)
            Surface(
                color = themeColors.accent.copy(alpha = 0.05f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Yıllık Abonelik",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = themeColors.textPrimary
                            )
                            Text(
                                "Tüm özellikler açık",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₺199,99",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = themeColors.textPrimary
                            )
                            Text(
                                "yıllık",
                                style = MaterialTheme.typography.labelSmall,
                                color = themeColors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HavamaniaPrimaryButton(
                        text = "PREMIUM'A GEÇ",
                        onClick = onPurchaseSuccess,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Aboneliğinizi istediğiniz zaman Store üzerinden iptal edebilirsiniz.",
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
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = themeColors.accent.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = themeColors.textPrimary
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = themeColors.textSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
