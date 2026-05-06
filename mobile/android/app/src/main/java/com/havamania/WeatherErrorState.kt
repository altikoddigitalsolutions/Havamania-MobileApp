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

/**
 * Modern ve Premium Hata Durumu Ekranı
 * İnternet yok veya veri çekilemediğinde kullanılır.
 */
@Composable
fun WeatherErrorState(
    title: String = "Bağlantı Hatası",
    description: String = "Hava durumu verilerine şu an ulaşılamıyor. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.",
    icon: ImageVector = Icons.Rounded.WifiOff,
    onRetry: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    val errorColor = Color(0xFFEF4444)

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
            // İkon Bölümü (Arka plan parıltılı)
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

            // Metin İçeriği
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
                color = themeColors.textSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Tekrar Dene Butonu (Premium Tasarım)
            HavamaniaPrimaryButton(
                text = "TEKRAR DENE",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.7f),
                icon = Icons.Rounded.Refresh
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherErrorState() {
    HavamaniaTheme {
        WeatherErrorState(onRetry = {})
    }
}
