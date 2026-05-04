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
import com.havamania.ui.theme.WeatherDataColors
import kotlin.math.roundToInt

@Composable
fun WeatherDetailsPanel(
    data: WeatherData,
    modifier: Modifier = Modifier
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

        // 2. Ana Detay Grid (2 Sütun)
        DetailMetricGrid(data = data)

        // 3. Güneş Zamanları
        SunTimesCard(
            sunrise = data.sunriseTime,
            solarNoon = data.solarNoon,
            sunset = data.sunsetTime
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DetailMetricGrid(data: WeatherData) {
    val themeColors = HavamaniaTheme.colors

    // Değer yardımcı fonksiyonu
    fun formatValue(value: Any?, unit: String = "", fallback: String = "Veri yok"): String {
        return when (value) {
            null -> fallback
            is Double -> "${value.roundToInt()}$unit"
            is Float -> "${value.roundToInt()}$unit"
            is Int -> "$value$unit"
            is Long -> "$value$unit"
            is String -> if (value.isEmpty() || value == "null") fallback else "$value$unit"
            else -> value.toString().ifEmpty { fallback }
        }
    }

    // Rüzgar Soğutma Hesaplama (Wind Chill)
    val feelsLikeVal = data.feelsLike?.toDoubleOrNull() ?: data.temperature.replace("°", "").toDoubleOrNull() ?: 0.0
    val windSpeedVal = data.windSpeed ?: 0.0
    val windChillValue = if (data.windChill != null) {
        formatValue(data.windChill, "°")
    } else if (feelsLikeVal <= 10.0 && windSpeedVal > 4.8) {
        // Basit wind chill yaklaşımı veya doğrudan hissedilen
        "${feelsLikeVal.roundToInt()}°"
    } else {
        "Düşük"
    }

    val items = listOf(
        WeatherDetailData("Hissedilen", data.feelsLike, "Rüzgar etkisi dahil", "Thermostat", "#FB7185"),
        WeatherDetailData("Rüzgar Soğutma", windChillValue, if (windChillValue == "Düşük") "Rüzgar etkisi düşük" else "Rüzgar etkisi dahil", "Snow", "#38BDF8"),
        WeatherDetailData("Yağış Olasılığı", formatValue(data.precipitationProbability, "%"), "Yağış ihtimali", "Rain", "#60A5FA"),
        WeatherDetailData("Yağış Miktarı", formatValue(data.precipitationAmount, " mm"), "Son 1 saat", "Rain", "#0EA5E9"),
        WeatherDetailData("Rüzgar", formatValue(data.windSpeed, " km/s"), "Anlık rüzgar hızı", "Air", "#34D399"),
        WeatherDetailData("Rüzgar Hamlesi", formatValue(data.windGust, " km/s"), "Maksimum rüzgar hızı", "Air", "#10B981"),
        WeatherDetailData("UV İndeksi", formatValue(data.uvIndex), getUVLabel(data.uvIndex), "Sun", "#FBBF24"),
        WeatherDetailData("Bulutluluk", formatValue(data.cloudCover, "%"), "Gökyüzü kapalılığı", "Cloudy", "#64748B"),
        WeatherDetailData("Görüş", formatValue(data.visibilityKm, " km"), getVisibilityLabel(data.visibilityKm), "Visibility", "#10B981"),
        WeatherDetailData("Basınç", formatValue(data.pressure, " hPa"), "Yüzey basıncı", "Compress", "#A78BFA"),
        WeatherDetailData("Nem", formatValue(data.humidity, "%"), "Bağıl nem oranı", "Rain", "#06B6D4")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // İlk 4 öğe
        items.take(4).chunked(2).forEach { rowItems ->
            MetricRow(rowItems)
        }

        // Rüzgar Yönü Kartı
        WindDirectionCard(
            degrees = data.windDirectionDegrees,
            label = data.windDirectionLabel
        )

        // Kalan öğeler
        items.drop(4).chunked(2).forEach { rowItems ->
            MetricRow(rowItems)
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
private fun MetricRow(rowItems: List<WeatherDetailData>) {
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
        if (rowItems.size < 2) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun WeatherDetailCard(
    data: WeatherDetailData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "scale"
    )

    val accentColor = try {
        Color(android.graphics.Color.parseColor(data.accentColorHex))
    } catch (e: Exception) {
        themeColors.accent
    }

    Card(
        modifier = modifier
            .height(130.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = themeColors.border.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                    text = data.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = themeColors.textSecondary.copy(alpha = 0.5f)
                )
            }

            Text(
                text = data.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = themeColors.textPrimary,
                maxLines = 1
            )

            Text(
                text = data.description,
                style = MaterialTheme.typography.labelSmall,
                color = themeColors.textSecondary.copy(alpha = 0.6f),
                maxLines = 1
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

    Card(
        modifier = modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.CompassCalibration, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(
                        "RÜZGAR YÖNÜ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = themeColors.textSecondary.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = if (degrees != null && label != null) "$degrees° $label" else "Veri yok",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = themeColors.textPrimary
                )
                Text(
                    text = "Anlık rüzgar yönü",
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.textSecondary.copy(alpha = 0.6f)
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.WbSunny, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                Text(
                    "GÜNEŞ ZAMANLARI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textSecondary.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SunTimeItem("Gün Doğumu", sunrise, themeColors)
                Box(Modifier.width(1.dp).height(30.dp).background(themeColors.border.copy(alpha = 0.1f)))
                SunTimeItem("Öğle", solarNoon, themeColors)
                Box(Modifier.width(1.dp).height(30.dp).background(themeColors.border.copy(alpha = 0.1f)))
                SunTimeItem("Gün Batımı", sunset, themeColors)
            }
        }
    }
}

@Composable
private fun SunTimeItem(label: String, time: String?, themeColors: com.havamania.ui.theme.HavamaniaColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time ?: "Veri yok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.textPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = themeColors.textSecondary.copy(alpha = 0.6f))
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
        score > 80 -> Color(0xFF10B981)
        score > 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    if (text.isEmpty() || text == "Veri yok") return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.surface.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, themeColors.border.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(scoreColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (score > 60) Icons.Rounded.AutoAwesome else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = "HAVAMANIA DURUM SKORU",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = themeColors.textSecondary.copy(alpha = 0.5f)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = themeColors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = themeColors.textSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun WeatherDetailsGrid(
    modifier: Modifier = Modifier,
    details: List<WeatherDetailData>,
    onItemClick: (WeatherDetailData) -> Unit = {}
) {
    // Legacy support - although WeatherDetailsPanel is preferred
}
