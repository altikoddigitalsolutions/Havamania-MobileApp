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
                title = "HAVAMANİA PREMIUM",
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
                                colors = listOf(themeColors.accent.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                )
                Surface(
                    color = themeColors.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp),
                    border = BorderStroke(2.dp, themeColors.accent.copy(alpha = 0.4f))
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
                "Atmosferik Gücü Serbest Bırak",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                "Gelişmiş AI analizleri ve kişiselleştirilmiş meteorolojik rehberlik ile standartların ötesine geçin.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = themeColors.textSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Features List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumFeatureItem(
                    icon = Icons.Rounded.Route,
                    title = "Gelişmiş Seyahat Analizi",
                    desc = "15 günlük seyahat periyodunda valiz önerisinden rota güvenliğine kadar her detayı AI ile planlayın."
                )
                PremiumFeatureItem(
                    icon = Icons.Rounded.Compare,
                    title = "Tahmin Değişim Karşılaştırması",
                    desc = "Hava tahminleri değiştikçe eski verilerle anlık kıyaslama yapın, sürprizlere yer bırakmayın."
                )
                PremiumFeatureItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Akıllı Bildirimler",
                    desc = "Yağış, UV ve kritik rüzgar değişikliklerini sadece sizin için önemli olan anlarda bildirelim."
                )
                PremiumFeatureItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Kişiselleştirilmiş AI",
                    desc = "Sağlık hassasiyetlerinize, hobilerinize ve günlük rutininize göre şekillenen özel meteorolojik rehberlik."
                )
                PremiumFeatureItem(
                    icon = Icons.Rounded.History,
                    title = "AI Sohbet Geçmişi",
                    desc = "Asistanınızla yaptığınız tüm seyahat planlarını ve hava durumu analizlerini arşivleyin."
                )
                PremiumFeatureItem(
                    icon = Icons.Rounded.AutoFixHigh,
                    title = "Atmosferik Görseller",
                    desc = "Hava durumuna göre dinamikleşen, ultra yüksek çözünürlüklü premium efektler ve temalar."
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Price Card
            Surface(
                color = themeColors.accent.copy(alpha = 0.08f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, themeColors.accent.copy(alpha = 0.3f)),
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
                                "Yıllık Premium",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = themeColors.textPrimary
                            )
                            Text(
                                "Tüm özellikler sınırsız",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₺299,99",
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
                        text = "PREMIUM'U AKTİF ET",
                        onClick = onPurchaseSuccess,
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Abonelik ödemeleri Store hesabınızdan tahsil edilir ve dilediğiniz zaman iptal edilebilir.",
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
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
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = themeColors.accent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = themeColors.accent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = themeColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = themeColors.textSecondary.copy(alpha = 0.75f)
                )
            }
        }
    }
}
