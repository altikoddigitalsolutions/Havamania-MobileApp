package com.havamania

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
        backgroundColor = colors.surface.copy(alpha = 0.4f),
        padding = 24.dp
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = origin ?: "Mevcut Konum",
                        style = typography.cardTitle.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = if (origin == null) colors.accent else colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Visible
                    )
                    Text("KALKIŞ", style = typography.caption, color = colors.accent)
                }

                Icon(
                    imageVector = Icons.Rounded.East,
                    contentDescription = null,
                    tint = colors.accent.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = destination,
                        style = typography.cardTitle.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Visible
                    )
                    Text("VARIŞ", style = typography.caption, color = colors.accent)
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoBlock(Icons.Rounded.Event, date)
                Box(Modifier.width(1.dp).height(20.dp).background(colors.divider))
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricItem(Modifier.weight(1f), distance, "Mesafe", Icons.Rounded.Straighten)
        MetricItem(Modifier.weight(1f), duration, "Sürüş", Icons.Rounded.Timer)
        MetricItem(Modifier.weight(1f), arrival ?: "--:--", "Varış", Icons.Rounded.Flag)
    }
}

@Composable
private fun MetricItem(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    val colors = HavamaniaTheme.colors
    Surface(
        modifier = modifier,
        color = colors.surface.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, null, tint = colors.accent.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = HavamaniaTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = colors.textPrimary)
            Text(label.uppercase(), style = HavamaniaTheme.typography.caption.copy(fontSize = 8.sp), color = colors.textMuted)
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
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Time
        Text(
            text = time,
            style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary,
            modifier = Modifier.width(50.dp)
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
        HavamaniaCard(
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            backgroundColor = colors.surface.copy(alpha = 0.3f),
            padding = 12.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (weather != null) {
                        Text(
                            text = WeatherUtils.getWeatherDisplayName(weather.weatherCode, LocalDateTime.now(), null, null),
                            style = typography.caption.copy(fontSize = 10.sp),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text("Veri bekleniyor...", style = typography.caption.copy(fontSize = 10.sp), color = colors.textMuted)
                    }
                }

                if (weather != null) {
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${weather.temperatureC.toInt()}°",
                            style = typography.bodyLarge.copy(fontWeight = FontWeight.Black, fontSize = 16.sp),
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = WeatherUtils.getWeatherEmoji(weather.weatherCode),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}
