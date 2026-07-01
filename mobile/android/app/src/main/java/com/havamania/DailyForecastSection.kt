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
        val themeColors = HavamaniaTheme.colors
        return when (code) {
            0, 1 -> DailyForecastStyle(
                accentColor = Color(0xFFFBBF24), // Keep Amber for sun as it's semantic
                barGradient = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                iconColor = Color(0xFFFBBF24)
            )
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> DailyForecastStyle(
                accentColor = themeColors.accent,
                barGradient = listOf(themeColors.accent.copy(alpha = 0.7f), themeColors.accent),
                iconColor = themeColors.accent
            )
            71, 73, 75, 77, 85, 86 -> DailyForecastStyle(
                accentColor = if (themeColors.isDark) Color(0xFF93C5FD) else Color(0xFF3B82F6),
                barGradient = if (themeColors.isDark)
                    listOf(Color(0xFFBFDBFE), Color(0xFF3B82F6))
                    else listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                iconColor = if (themeColors.isDark) Color(0xFFBFDBFE) else Color(0xFF3B82F6)
            )
            else -> DailyForecastStyle(
                accentColor = themeColors.textSecondary,
                barGradient = listOf(themeColors.textSecondary.copy(alpha = 0.6f), themeColors.textSecondary),
                iconColor = themeColors.textSecondary
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
    var expandedForecast by remember { mutableStateOf(false) }
    val visibleForecast = if (expandedForecast) forecasts.take(10) else forecasts.take(7)

    val themeColors = HavamaniaTheme.colors
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
                    text = "10 GÜNLÜK TAHMİN",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = themeColors.textSecondary.copy(alpha = 0.8f)
                )
            }

            // Forecast List
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
fun ForecastExpandButton(
    expanded: Boolean,
    onClick: () -> Unit
) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(CircleShape)
            .background(themeColors.textPrimary.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(themeColors.border.copy(alpha = 0.4f), Color.Transparent)
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (expanded) "7 güne düşür" else "10 günü göster",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textPrimary.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = themeColors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@Composable
fun DailyForecastRow(data: DailyForecast, isSelected: Boolean = false, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value
    val style = DailyForecastStyleMapper.getStyle(data.weatherCode)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "pressScale"
    )

    SideEffect {
        Log.d(
            "DailyForecastStyle",
            "label=${if (data.isToday) "Bugün" else data.day} date=${data.date} selected=$isSelected today=${data.isToday}"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp) // Even taller for more air
            .scale(scale)
            .clip(RoundedCornerShape(20.dp)) // More rounded
            .background(
                if (isSelected) themeColors.accent.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Label
        Column(modifier = Modifier.width(90.dp)) {
            Text(
                text = if (data.isToday) "Bugün" else data.day.split(" ").last(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = if (isSelected) themeColors.textPrimary else themeColors.textPrimary.copy(alpha = 0.9f)
            )
            Text(
                text = if (data.isToday) "Şimdi" else {
                    try {
                        val dateObj = LocalDate.parse(data.date)
                        dateObj.format(DateTimeFormatter.ofPattern("dd/MM", Locale("tr", "TR")))
                    } catch (e: Exception) {
                        data.date.split("-").let { if (it.size >= 3) "${it[2]}/${it[1]}" else data.date }
                    }
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = themeColors.textSecondary.copy(alpha = 0.5f)
            )
        }

        // Weather Icon
        Box(
            modifier = Modifier.width(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = WeatherMapper.getIconFromName(data.iconName),
                contentDescription = null,
                tint = if (isSelected) themeColors.accent else style.iconColor.copy(alpha = 0.8f),
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Min Temp
        Text(
            text = "${data.minTemp}°",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = themeColors.textPrimary.copy(alpha = 0.4f),
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )

        // Range Bar
        TemperatureRangeBar(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            min = data.minTemp,
            max = data.maxTemp,
            style = style,
            isSelected = isSelected
        )

        // Max Temp
        Text(
            text = "${data.maxTemp}°",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 17.sp
            ),
            color = themeColors.textPrimary,
            modifier = Modifier.width(40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
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
