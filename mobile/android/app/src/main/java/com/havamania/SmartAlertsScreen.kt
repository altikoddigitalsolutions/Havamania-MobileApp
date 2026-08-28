package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.havamania.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlertsScreen(
    onBack: () -> Unit,
    viewModel: SmartAlertViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val themeColors = HavamaniaTheme.colors

    HavamaniaScreen(
        topBar = {
            HavamaniaTopBar(
                title = "AKILLI UYARILAR",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Kişiselleştirilmiş hava durumu uyarılarını yönetin. Belirlediğiniz koşullar oluştuğunda sizi bilgilendirelim.",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            AlertGroupLabel("KRİTİK HAVA DURUMLARI")
            HavamaniaGlassCard {
                AlertToggleRow(
                    title = "Yağmur Uyarısı",
                    desc = "Yağış başlamadan 2 saat önce haber ver.",
                    icon = Icons.Rounded.WaterDrop,
                    checked = config.rainEnabled,
                    onCheckedChange = { viewModel.toggleAlert("rain", it) }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Fırtına ve Yıldırım",
                    desc = "Gök gürültülü sağanak beklendiğinde uyar.",
                    icon = Icons.Rounded.Thunderstorm,
                    checked = config.stormEnabled,
                    onCheckedChange = { viewModel.toggleAlert("storm", it) }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Kuvvetli Rüzgar",
                    desc = "Rüzgar hızı 40 km/sa üzerine çıktığında bildir.",
                    icon = Icons.Rounded.Air,
                    checked = config.windEnabled,
                    onCheckedChange = { viewModel.toggleAlert("wind", it) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AlertGroupLabel("SICAKLIK VE GÖRÜŞ")
            HavamaniaGlassCard {
                AlertToggleRow(
                    title = "Aşırı Sıcaklık",
                    desc = "Sıcaklık 35°C üzerine çıktığında uyar.",
                    icon = Icons.Rounded.WbSunny,
                    checked = config.heatEnabled,
                    onCheckedChange = { viewModel.toggleAlert("heat", it) }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Don Riski",
                    desc = "Gece sıcaklığı 0°C altına düştüğünde bildir.",
                    icon = Icons.Rounded.AcUnit,
                    checked = config.frostEnabled,
                    onCheckedChange = { viewModel.toggleAlert("frost", it) }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Sis ve Pus",
                    desc = "Görüş mesafesi düştüğünde haber ver.",
                    icon = Icons.Rounded.Visibility,
                    checked = config.fogEnabled,
                    onCheckedChange = { viewModel.toggleAlert("fog", it) }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AlertGroupLabel("SAĞLIK VE ÇEVRE")
            HavamaniaGlassCard {
                AlertToggleRow(
                    title = "UV İndeksi",
                    desc = "Güneş radyasyonu riskli seviyeye ulaştığında bildir.",
                    icon = Icons.Rounded.LightMode,
                    checked = config.uvEnabled,
                    onCheckedChange = { viewModel.toggleAlert("uv", it) }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Hava Kalitesi",
                    desc = "Hava kalitesi düşük olduğunda bildir. (Yakında)",
                    icon = Icons.Rounded.Masks,
                    checked = false,
                    enabled = false,
                    onCheckedChange = { }
                )
                AlertDivider()
                AlertToggleRow(
                    title = "Polen Seviyesi",
                    desc = "Yüksek polen tespiti yapıldığında uyar. (Yakında)",
                    icon = Icons.Rounded.Grass,
                    checked = false,
                    enabled = false,
                    onCheckedChange = { }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AlertToggleRow(
    title: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(themeColors.accent.copy(alpha = if (enabled) 0.1f else 0.05f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) themeColors.accent else themeColors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) themeColors.textPrimary else themeColors.textMuted
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.textSecondary.copy(alpha = if (enabled) 1f else 0.6f)
            )
        }

        HavamaniaToggle(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun AlertGroupLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Black),
        color = HavamaniaTheme.colors.accent.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
    )
}

@Composable
fun AlertDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = HavamaniaTheme.colors.textPrimary.copy(alpha = 0.05f))
}
