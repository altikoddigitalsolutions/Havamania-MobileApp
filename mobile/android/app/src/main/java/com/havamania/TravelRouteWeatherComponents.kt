package com.havamania

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JourneyHero(
    origin: String?,
    destination: String,
    date: String,
    departureTime: String?,
    modifier: Modifier = Modifier
) {
    val colors = HavamaniaTheme.colors
    val typography = HavamaniaTheme.typography

    HavamaniaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = colors.surface,
        borderColor = colors.border.copy(alpha = 0.1f),
        padding = 20.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = origin ?: "Mevcut Konum",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.Bold),
                        color = if (origin == null) colors.accent else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("BAŞLANGIÇ", style = typography.caption.copy(fontSize = 9.sp), color = colors.textMuted)
                }

                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.accent.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 12.dp).size(18.dp)
                )

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = destination,
                        style = typography.cardTitle.copy(fontWeight = FontWeight.Black),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    Text("VARILACAK YER", style = typography.caption.copy(fontSize = 9.sp), color = colors.textMuted, textAlign = TextAlign.End)
                }
            }

            Spacer(Modifier.height(20.dp))
            Divider(color = colors.border.copy(alpha = 0.05f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBlock(Icons.Rounded.CalendarToday, date)
                InfoBlock(Icons.Rounded.Schedule, departureTime ?: "Belirtilmedi")
            }
        }
    }
}

@Composable
private fun InfoBlock(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = HavamaniaTheme.colors.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = HavamaniaTheme.colors.textPrimary
        )
    }
}

@Composable
fun RouteMetricRow(
    distance: String,
    duration: String,
    arrival: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricItem(Modifier.weight(1f), distance, "Mesafe", Icons.Rounded.Navigation)
        MetricItem(Modifier.weight(1f), duration, "Sürüş", Icons.Rounded.Schedule)
        MetricItem(Modifier.weight(1f), arrival ?: "--:--", "Varış", Icons.Rounded.Flag)
    }
}

@Composable
private fun MetricItem(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    val colors = HavamaniaTheme.colors
    Surface(
        modifier = modifier,
        color = colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = HavamaniaTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = colors.textPrimary)
            Text(label.uppercase(), style = HavamaniaTheme.typography.caption.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold), color = colors.textMuted)
        }
    }
}

@Composable
fun TimelineWaypointItem(
    title: String,
    time: String,
    weather: WaypointWeather?,
    isLast: Boolean = false,
    isFirst: Boolean = false,
    onClick: () -> Unit
) {
    val colors = HavamaniaTheme.colors
    val typography = HavamaniaTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(enabled = weather != null) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Time
        Text(
            text = time,
            style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.accent,
            modifier = Modifier.width(48.dp).padding(top = 16.dp)
        )

        // Middle: Dot and Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(colors.accent.copy(alpha = 0.2f))
                )
            }

            val dotColor = when (weather?.risk) {
                RouteRisk.OK -> colors.success
                RouteRisk.CAUTION -> colors.warning
                RouteRisk.DANGER -> colors.error
                else -> colors.accent
            }

            Surface(
                color = dotColor,
                shape = CircleShape,
                modifier = Modifier.size(if (isFirst || isLast) 12.dp else 8.dp)
            ) {
                if (isFirst || isLast) {
                    Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(Color.White))
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // Right side: Info
        Surface(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            color = if (isFirst || isLast) colors.surface.copy(alpha = 0.5f) else Color.Transparent,
            shape = RoundedCornerShape(16.dp),
            border = if (isFirst || isLast) BorderStroke(1.dp, colors.border.copy(alpha = 0.1f)) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typography.bodyLarge.copy(
                            fontWeight = if (isFirst || isLast) FontWeight.Black else FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (weather != null) {
                        Text(
                            text = WeatherUtils.getWeatherDisplayName(weather.weatherCode, LocalDateTime.now(), null, null),
                            style = typography.caption.copy(fontSize = 11.sp),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (weather.risk != RouteRisk.OK && !weather.riskReason.isNullOrBlank()) {
                            Text(
                                text = "⚠️ ${weather.riskReason}",
                                style = typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = colors.warning,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text("Veri bekleniyor...", style = typography.caption.copy(fontSize = 11.sp), color = colors.textMuted)
                    }
                }

                if (weather != null) {
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${weather.temperatureC.toInt()}°",
                            style = typography.cardTitle.copy(fontWeight = FontWeight.Black, fontSize = 20.sp),
                            color = colors.textPrimary
                        )
                        Text(
                            text = WeatherUtils.getWeatherEmoji(weather.weatherCode),
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}
