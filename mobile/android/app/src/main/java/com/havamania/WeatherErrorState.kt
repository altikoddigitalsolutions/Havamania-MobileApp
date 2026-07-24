package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*

import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.ErrorOutline

/**
 * Modern ve Premium Hata Durumu Ekranı
 */
@Composable
fun HavamaniaErrorState(
    title: String,
    description: String,
    icon: ImageVector = Icons.Rounded.ErrorOutline,
    onRetry: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val errorColor = themeColors.accent // Use theme accent or a soft red

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    errorColor.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = errorColor.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = themeColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = themeColors.textSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            HavamaniaPrimaryButton(
                text = "TEKRAR DENE",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.7f),
                icon = Icons.Rounded.Refresh
            )
        }
    }
}

// Predefined error states
@Composable
fun OfflineErrorState(onRetry: () -> Unit) {
    HavamaniaErrorState(
        title = "Bağlantı Sorunu",
        description = "Şu an internete ulaşılamıyor. Kısa süreli bir kesinti olabilir, tekrar deneyebilirsin.",
        icon = Icons.Rounded.SignalWifiOff,
        onRetry = onRetry
    )
}

@Composable
fun LocationPermissionErrorState(onRetry: () -> Unit) {
    HavamaniaErrorState(
        title = "Konum Gerekli",
        description = "Canlı konumunuzu gösterip size özel analizler yapabilmemiz için konum iznine ihtiyacımız var.",
        icon = Icons.Rounded.LocationOff,
        onRetry = onRetry
    )
}

@Composable
fun CityNotFoundErrorState(onRetry: () -> Unit) {
    HavamaniaErrorState(
        title = "Şehir Bulunamadı",
        description = "Aradığın şehrin hava verilerine şu an ulaşamıyoruz. Yazımı kontrol edip tekrar deneyebilirsin.",
        icon = Icons.Rounded.SearchOff,
        onRetry = onRetry
    )
}

@Composable
fun ApiErrorState(onRetry: () -> Unit) {
    HavamaniaErrorState(
        title = "Sunucu Meşgul",
        description = "Meteoroloji servislerinden veri alırken bir gecikme yaşandı. Tekrar denemeye ne dersin?",
        icon = Icons.Rounded.ErrorOutline,
        onRetry = onRetry
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherErrorState() {
    HavamaniaTheme {
        HavamaniaErrorState(
            title = "Hata Oluştu",
            description = "Beklenmedik bir sorun yaşandı.",
            onRetry = {}
        )
    }
}
