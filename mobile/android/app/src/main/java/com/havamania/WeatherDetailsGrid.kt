package com.havamania

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.HavamaniaCard
import com.havamania.ui.theme.WeatherDataColors
import kotlin.math.roundToInt

@Composable
fun WeatherDetailsPanel(
    data: WeatherData,
    modifier: Modifier = Modifier,
    sunriseTime: String? = data.sunriseTime,
    sunsetTime: String? = data.sunsetTime,
    solarNoon: String? = data.solarNoon
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Havamania Durum Skoru
        WeatherSuitabilityCard(
            score = data.weatherSuitabilityScore,
            text = data.weatherSuitabilityText,
            description = data.weatherSuitabilityDesc
        )

        // Each metric has its own surface; no outer card nesting them together.
        DetailMetricGrid(data = data)
        SunTimesCard(sunrise = sunriseTime, solarNoon = solarNoon, sunset = sunsetTime)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailMetricGrid(data: WeatherData) {
    val items = data.details

    if (items.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val columns = if (maxWidth < 360.dp || fontScale > 1.3f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // İlk 4 öğe (Hissedilen, Rüzgar Soğutma, Yağış Olasılığı, Yağış Miktarı)
            items.take(4).chunked(columns).forEach { rowItems ->
                MetricRow(rowItems, columns)
            }

            // Rüzgar Yönü Kartı
            WindDirectionCard(
                degrees = data.windDirectionDegrees,
                label = data.windDirectionLabel
            )

            // Kalan öğeler
            items.drop(4).chunked(columns).forEach { rowItems ->
                MetricRow(rowItems, columns)
            }
        }
    }
}

private fun getUVLabel(uv: Int?): String {
    if (uv == null) return "Veri yok"
    return when {
        uv <= 2 -> "Düşük risk"
        uv <= 5 -> "Orta risk"
        uv <= 7 -> "Yüksek risk"
        else -> "Çok yüksek risk"
    }
}

private fun getVisibilityLabel(km: Double?): String {
    if (km == null) return "Veri yok"
    return when {
        km >= 10 -> "Mükemmel"
        km >= 5 -> "İyi"
        km >= 2 -> "Orta"
        else -> "Kısıtlı"
    }
}

@Composable
private fun MetricRow(rowItems: List<WeatherDetailData>, columns: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rowItems.forEach { item ->
            WeatherDetailCard(
                data = item,
                modifier = Modifier.weight(1f)
            )
        }
        if (rowItems.size < columns) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun WeatherDetailCard(
    data: WeatherDetailData,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors

    val accentColor = remember(data.accentColorHex, themeColors.accent) {
        try {
            Color(android.graphics.Color.parseColor(data.accentColorHex))
        } catch (e: Exception) {
            themeColors.accent
        }
    }

    HavamaniaCard(
        modifier = modifier,
        backgroundColor = themeColors.surface,
        borderColor = themeColors.border.copy(alpha = 0.35f),
        elevation = 0.dp,
        padding = 16.dp,
        onClick = null
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = WeatherMapper.getIconFromName(data.iconName),
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = data.title.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
                    style = HavamaniaTheme.typography.sectionTitle,
                    color = themeColors.textSecondary
                )
            }

            Text(
                text = data.value,
                style = HavamaniaTheme.typography.cardTitle.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = themeColors.textPrimary,
                maxLines = 3
            )

            Text(
                text = data.description,
                style = HavamaniaTheme.typography.bodySmall,
                color = themeColors.textSecondary
            )
        }
    }
}

@Composable
fun WindDirectionCard(
    degrees: Int?,
    label: String?,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors

    HavamaniaCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 110.dp),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Compass Visual
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(themeColors.textPrimary.copy(alpha = 0.05f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(32.dp).rotate(degrees?.toFloat() ?: 0f)
                )
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CompassCalibration, null, tint = themeColors.textMuted, modifier = Modifier.size(14.dp))
                    Text(
                        "RÜZGÂR YÖNÜ",
                        style = HavamaniaTheme.typography.sectionTitle,
                        color = themeColors.textSecondary
                    )
                }
                Text(
                    text = if (degrees != null && label != null) "$degrees° $label" else "Veri yok",
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.ExtraBold),
                    color = themeColors.textPrimary
                )
                Text(
                    text = "Anlık rüzgâr yönü",
                    style = HavamaniaTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
            }
        }
    }
}

@Composable
fun SunTimesCard(
    sunrise: String?,
    solarNoon: String?,
    sunset: String?,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors

    HavamaniaCard(
        modifier = modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.WbSunny, null, tint = themeColors.weatherClear, modifier = Modifier.size(16.dp))
                Text(
                    "GÜNEŞ ZAMANLARI",
                    style = HavamaniaTheme.typography.sectionTitle,
                    color = themeColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                if (maxWidth < 300.dp || fontScale > 1.3f) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        SunTimeItem("Gün doğumu", sunrise, themeColors)
                        SunTimeItem("Gündüz ortası", solarNoon, themeColors)
                        SunTimeItem("Gün batımı", sunset, themeColors)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SunTimeItem("Gün doğumu", sunrise, themeColors)
                        SunTimeItem("Gündüz ortası", solarNoon, themeColors)
                        SunTimeItem("Gün batımı", sunset, themeColors)
                    }
                }
            }
        }
    }
}

@Composable
private fun SunTimeItem(label: String, time: String?, themeColors: com.havamania.ui.theme.HavamaniaColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time ?: "Veri yok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = themeColors.textSecondary)
    }
}

@Composable
fun WeatherSuitabilityCard(
    score: Int,
    text: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val themeColors = HavamaniaTheme.colors
    val scoreColor = when {
        score < 0 -> themeColors.textSecondary
        score > 80 -> Color(0xFF10B981)
        score > 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    if (text.isEmpty() || text == "Veri yok") return

    HavamaniaCard(
        modifier = modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(scoreColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (score < 0) Icons.Rounded.Info else if (score > 60) Icons.Rounded.AutoAwesome else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = "HAVAMANIA DURUM SKORU",
                    style = HavamaniaTheme.typography.sectionTitle,
                    color = themeColors.textSecondary
                )
                Text(
                    text = text,
                    style = HavamaniaTheme.typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textPrimary
                )
                Text(
                    text = description,
                    style = HavamaniaTheme.typography.bodySmall,
                    color = themeColors.textSecondary
                )
            }
        }
    }
}

