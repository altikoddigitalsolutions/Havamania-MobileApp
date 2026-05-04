package com.havamania

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme

/**
 * Küçük Bilgi Kartı (Ay Fazı, Gün Doğumu vb.)
 * Sabit renkler kaldırıldı, temaya bağlandı.
 */
@Composable
fun DetailSmallCard(
    label: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
    val themeStyles = HavamaniaTheme.styles
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // Small radius
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surfaceGlass.copy(alpha = 0.5f)
        ),
        border = BorderStroke(themeStyles.cardBorderWidth, themeColors.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textSecondary.copy(alpha = 0.6f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = themeColors.textPrimary
                )
            )
        }
    }
}

/**
 * Bölüm Başlığı
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = HavamaniaTheme.colors.accent.copy(alpha = 0.8f),
        modifier = modifier.padding(bottom = 10.dp)
    )
}
