package com.havamania

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
    forecasts: List<DailyForecastData>
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
        DailyForecastPanel(forecasts = forecasts)
    }
}

@Composable
fun DailyForecastPanel(forecasts: List<DailyForecastData>) {
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
            .clip(RoundedCornerShape(themeStyles.cardCornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.gradientPrimary[0].copy(alpha = 0.95f),
                        themeColors.surfaceGlass.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = themeStyles.cardBorderWidth,
                brush = Brush.verticalGradient(
                    listOf(themeColors.border, Color.Transparent)
                ),
                shape = RoundedCornerShape(themeStyles.cardCornerRadius)
            )
    ) {
        // Subtle background glow
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
                .blur(themeStyles.glassBlur * 5)
                .background(themeColors.accent.copy(alpha = 0.05f), CircleShape)
        )

        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(themeColors.accent.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = themeColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "GÜNLÜK TAHMİN",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    ),
                    color = themeColors.textPrimary.copy(alpha = 0.5f)
                )
            }

            // Forecast List
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visibleForecast.forEachIndexed { index, forecast ->
                    key(forecast.day) {
                        DailyForecastRow(forecast)
                        if (index < visibleForecast.size - 1) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(themeColors.border.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }

            if (forecasts.size > 7) {
                Spacer(Modifier.height(20.dp))
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
    val isPressed by interactionSource.collectIsPressedAsState()

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
fun DailyForecastRow(data: DailyForecastData) {
    val themeColors = HavamaniaTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val style = DailyForecastStyleMapper.getStyle(data.weatherCode)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "pressScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // Taller row
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (data.isToday) themeColors.accent.copy(alpha = 0.08f) else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Label
        Text(
            text = if (data.isToday) "Bugün" else data.day.split(" ").last(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (data.isToday) FontWeight.Black else FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = if (data.isToday) themeColors.textPrimary else themeColors.textPrimary.copy(alpha = 0.8f),
            modifier = Modifier.width(85.dp)
        )

        // Weather Icon
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = WeatherMapper.getIconFromName(data.iconName),
                contentDescription = null,
                tint = if (data.isToday) style.accentColor else style.iconColor.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp) // Larger icon
            )
        }

        // Min Temp
        Text(
            text = "${data.minTemp}°",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = themeColors.textPrimary.copy(alpha = 0.4f),
            modifier = Modifier.width(40.dp)
        )

        // Range Bar
        TemperatureRangeBar(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            min = data.minTemp,
            max = data.maxTemp,
            style = style,
            isToday = data.isToday
        )

        // Max Temp
        Text(
            text = "${data.maxTemp}°",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
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
    isToday: Boolean = false
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
        if (isToday) {
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
