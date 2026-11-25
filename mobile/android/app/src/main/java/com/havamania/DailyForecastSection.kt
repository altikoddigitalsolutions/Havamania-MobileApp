package com.havamania

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import kotlinx.coroutines.delay

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DailyForecastStyle(
    val accentColor: Color,
    val barGradient: List<Color>,
    val iconColor: Color
)

object DailyForecastStyleMapper {
    @Composable
    fun getStyle(code: Int): DailyForecastStyle {
        val colors = HavamaniaTheme.colors
        return when (code) {
            0, 1 -> DailyForecastStyle(
                accentColor = colors.weatherClear,
                barGradient = listOf(colors.weatherClear.copy(alpha = 0.6f), colors.weatherClear),
                iconColor = colors.weatherClear
            )
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> DailyForecastStyle(
                accentColor = colors.weatherRain,
                barGradient = listOf(colors.weatherRain.copy(alpha = 0.6f), colors.weatherRain),
                iconColor = colors.weatherRain
            )
            71, 73, 75, 77, 85, 86 -> DailyForecastStyle(
                accentColor = colors.weatherSnow,
                barGradient = listOf(colors.weatherSnow.copy(alpha = 0.6f), colors.weatherSnow),
                iconColor = colors.weatherSnow
            )
            95, 96, 99 -> DailyForecastStyle(
                accentColor = colors.weatherStorm,
                barGradient = listOf(colors.weatherStorm.copy(alpha = 0.6f), colors.weatherStorm),
                iconColor = colors.weatherStorm
            )
            else -> DailyForecastStyle(
                accentColor = colors.textSecondary,
                barGradient = listOf(colors.textSecondary.copy(alpha = 0.6f), colors.textSecondary),
                iconColor = colors.textSecondary
            )
        }
    }
}

@Composable
fun DailyForecastSection(
    modifier: Modifier = Modifier,
    forecasts: List<DailyForecast>,
    selectedDate: String = "",
    onDayClick: (DailyForecast) -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(1000)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(1000)),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        DailyForecastPanel(forecasts = forecasts, selectedDate = selectedDate, onDayClick = onDayClick)
    }
}

@Composable
fun DailyForecastPanel(
    forecasts: List<DailyForecast>,
    selectedDate: String = "",
    onDayClick: (DailyForecast) -> Unit
) {
    var expandedForecast by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val visibleForecast = if (expandedForecast) forecasts else forecasts.take(7)

    val themeColors = HavamaniaTheme.colors
    // ...
    val themeStyles = HavamaniaTheme.styles
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clip(RoundedCornerShape(32.dp)) // More rounded
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.surface.copy(alpha = 0.9f),
                        themeColors.surface.copy(alpha = 0.7f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = themeColors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (forecasts.isEmpty()) "GÜNLÜK TAHMİN" else "${forecasts.size} GÜNLÜK TAHMİN",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = themeColors.textSecondary.copy(alpha = 0.8f)
                )
            }

            // Forecast List
            if (forecasts.isEmpty()) {
                Text(
                    text = "Günlük tahmin verisi şu anda bulunmuyor. Bağlantı sağlandığında yeniden deneyebilirsiniz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textSecondary
                )
            }
            Column {
                visibleForecast.forEachIndexed { index, forecast ->
                    key(forecast.date) {
                        val isSelected = forecast.date == selectedDate
                        DailyForecastRow(forecast, isSelected = isSelected, onClick = { onDayClick(forecast) })
                        if (index < visibleForecast.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                thickness = 0.5.dp,
                                color = themeColors.border.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }

            if (forecasts.size > 7) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ForecastExpandButton(
                        expanded = expandedForecast,
                        onClick = { expandedForecast = !expandedForecast }
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastExpandButton(expanded: Boolean, onClick: () -> Unit) {
    val colors = HavamaniaTheme.colors
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            stateDescription = if (expanded) "Tüm günler gösteriliyor" else "İlk 7 gün gösteriliyor"
        },
        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(if (expanded) "7 güne düşür" else "Tüm günleri göster",
            modifier = Modifier.weight(1f, fill = false), fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DailyForecastRow(data: DailyForecast, isSelected: Boolean = false, onClick: () -> Unit) {
    val colors = HavamaniaTheme.colors
    val style = DailyForecastStyleMapper.getStyle(data.weatherCode)
    val dateLabel = if (data.isToday) "Bugün" else runCatching {
        LocalDate.parse(data.date).format(DateTimeFormatter.ofPattern("dd/MM", Locale("tr", "TR")))
    }.getOrDefault(data.date)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        val selection = Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
            .selectable(selected = isSelected, role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
        @Composable fun Day(modifier: Modifier = Modifier) {
            Column(modifier) {
                Text(if (data.isToday) "Bugün" else data.day.split(" ").last(),
                    style = MaterialTheme.typography.titleMedium, color = colors.textPrimary,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold)
                if (!data.isToday) Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
        @Composable fun Symbol() {
            Icon(WeatherMapper.getIconFromName(data.iconName), null,
                tint = if (isSelected) colors.accent else style.iconColor,
                modifier = Modifier.size(28.dp))
        }
        @Composable fun Temperatures(modifier: Modifier = Modifier) {
            Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${data.minTemp}°", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                TemperatureRangeBar(Modifier.weight(1f), data.minTemp, data.maxTemp, style, isSelected)
                Text("${data.maxTemp}°", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
        if (stacked) Column(selection, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Day(Modifier.weight(1f)); Symbol() }
            Temperatures(Modifier.fillMaxWidth())
        } else Row(selection, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Day(Modifier.weight(1f)); Symbol(); Temperatures(Modifier.weight(1.5f))
        }
    }
}

@Composable
fun TemperatureRangeBar(
    modifier: Modifier = Modifier,
    min: Int,
    max: Int,
    style: DailyForecastStyle,
    isSelected: Boolean = false
) {
    val themeColors = HavamaniaTheme.colors
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        startAnim = true
    }

    val progress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "barAnim"
    )

    Box(
        modifier = modifier
            .height(8.dp) // Thicker bar
            .clip(CircleShape)
            .background(themeColors.textPrimary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.CenterStart
    ) {
        val rangeWidth = 0.6f * progress
        val startOffset = 0.2f

        // Glow for the bar
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rangeWidth)
                    .fillMaxHeight()
                    .offset(x = (startOffset * 100).dp)
                    .blur(6.dp)
                    .background(style.accentColor.copy(alpha = 0.3f), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(rangeWidth)
                .fillMaxHeight()
                .offset(x = (startOffset * 100).dp)
                .background(
                    brush = Brush.horizontalGradient(style.barGradient),
                    shape = CircleShape
                )
        )
    }
}
