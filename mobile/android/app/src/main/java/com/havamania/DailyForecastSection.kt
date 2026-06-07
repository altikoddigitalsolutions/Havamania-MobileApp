package com.havamania

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
                accentColor = Color(0xFFFBBF24),
                barGradient = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                iconColor = Color(0xFFFBBF24)
            )
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> DailyForecastStyle(
                accentColor = Color(0xFF38BDF8),
                barGradient = listOf(Color(0xFFBAE6FD), Color(0xFF0EA5E9)),
                iconColor = Color(0xFF38BDF8)
            )
            71, 73, 75, 77, 85, 86 -> DailyForecastStyle(
                accentColor = Color(0xFFBFDBFE),
                barGradient = listOf(Color(0xFFDBEAFE), Color(0xFF3B82F6)),
                iconColor = Color(0xFFBFDBFE)
            )
            else -> DailyForecastStyle(
                accentColor = themeColors.textPrimary.copy(alpha = 0.6f),
                barGradient = listOf(themeColors.textPrimary.copy(alpha = 0.2f), themeColors.textPrimary.copy(alpha = 0.4f)),
                iconColor = themeColors.textPrimary.copy(alpha = 0.5f)
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
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.gradientPrimary[0].copy(alpha = 0.95f),
                        themeColors.surfaceGlass.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(themeColors.border, Color.Transparent)),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = themeColors.textPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "GÜNLÜK TAHMİN",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = themeColors.textPrimary.copy(alpha = 0.4f)
                )
            }

            // Forecast List
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                visibleForecast.forEachIndexed { index, forecast ->
                    key(forecast.date) {
                        DailyForecastRow(forecast, isSelected = forecast.date == selectedDate, onClick = { onDayClick(forecast) })
                        if (index < visibleForecast.size - 1) {
                            Spacer(Modifier.fillMaxWidth().height(0.5.dp).background(themeColors.border.copy(alpha = 0.2f)))
                        }
                    }
                }
            }

            if (forecasts.size > 7) {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ForecastExpandButton(expanded = expandedForecast, onClick = { expandedForecast = !expandedForecast })
                }
            }
        }
    }
}

@Composable
fun ForecastExpandButton(expanded: Boolean, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(themeColors.textPrimary.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (expanded) "Daha az göster" else "10 günü göster",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = themeColors.textPrimary.copy(alpha = 0.6f)
            )
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = themeColors.textPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DailyForecastRow(data: DailyForecast, isSelected: Boolean = false, onClick: () -> Unit) {
    val themeColors = HavamaniaTheme.colors
    val style = DailyForecastStyleMapper.getStyle(data.weatherCode)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) themeColors.accent.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (data.isToday) "Bugün" else data.day.split(" ").last(),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold, fontSize = 15.sp),
            color = themeColors.textPrimary,
            modifier = Modifier.width(80.dp)
        )

        Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = WeatherMapper.getIconFromName(data.iconName),
                contentDescription = null,
                tint = if (isSelected) style.accentColor else style.iconColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = "${data.minTemp}°",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = themeColors.textPrimary.copy(alpha = 0.4f),
            modifier = Modifier.width(35.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        TemperatureRangeBar(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), min = data.minTemp, max = data.maxTemp, style = style)

        Text(
            text = "${data.maxTemp}°",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
            color = themeColors.textPrimary,
            modifier = Modifier.width(35.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun TemperatureRangeBar(modifier: Modifier = Modifier, min: Int, max: Int, style: DailyForecastStyle) {
    val themeColors = HavamaniaTheme.colors
    Box(
        modifier = modifier.height(6.dp).clip(CircleShape).background(themeColors.textPrimary.copy(alpha = 0.05f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight()
                .offset(x = 20.dp)
                .background(brush = Brush.horizontalGradient(style.barGradient), shape = CircleShape)
        )
    }
}
