package com.havamania

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationCity
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
import com.havamania.ui.theme.HavamaniaTheme

/**
 * Genel Empty State Bileşeni
 */
@Composable
fun WeatherEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
    accentColor: Color = Color(0xFF38BDF8)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İkon Alanı
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = accentColor.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Başlık
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Açıklama
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp
            ),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Aksiyon Butonu (Opsiyonel)
        if (buttonText != null) {
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor.copy(alpha = 0.15f),
                    contentColor = accentColor
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    accentColor.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = buttonText.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                )
            }
        }
    }
}

/**
 * Şehir eklenmemiş durumu için özel görünüm
 */
@Composable
fun NoCitiesEmptyState(onAddClick: () -> Unit) {
    WeatherEmptyState(
        icon = Icons.Rounded.LocationCity,
        title = "Henüz Şehir Yok",
        description = "Takip ettiğiniz bir şehir bulunmuyor. Hava durumunu görmek için ilk şehrinizi ekleyin.",
        buttonText = "Şehir Ekle",
        onButtonClick = onAddClick,
        accentColor = Color(0xFF38BDF8) // Sky Blue
    )
}

/**
 * AI Geçmişi boş durumu için özel görünüm
 */
@Composable
fun NoAiHistoryEmptyState(onAskClick: () -> Unit) {
    WeatherEmptyState(
        icon = Icons.Rounded.AutoAwesome,
        title = "Asistan Geçmişi Boş",
        description = "Hava durumu asistanıyla henüz bir sohbetiniz yok. AI ile hava durumu hakkında konuşmaya başlayın.",
        buttonText = "Soru Sor",
        onButtonClick = onAskClick,
        accentColor = Color(0xFFA78BFA) // Violet/Purple
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun PreviewNoCities() {
    HavamaniaTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0E14))) {
            NoCitiesEmptyState {}
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E14)
@Composable
fun PreviewNoAiHistory() {
    HavamaniaTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0E14))) {
            NoAiHistoryEmptyState {}
        }
    }
}
