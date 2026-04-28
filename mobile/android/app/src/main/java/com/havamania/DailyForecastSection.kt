package com.havamania

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaDesign

@Composable
fun DailyForecastSection(
    modifier: Modifier = Modifier,
    forecasts: List<DailyForecastData>
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(HavamaniaDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = BorderStroke(HavamaniaDesign.CardBorderWidth, colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = null,
                    tint = colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "10 GÜNLÜK TAHMİN",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            forecasts.forEach { forecast ->
                DailyForecastItem(forecast)
                if (forecast != forecasts.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = colorScheme.outline.copy(alpha = 0.05f)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyForecastItem(data: DailyForecastData) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = { })
            .background(if (data.isToday) colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (data.isToday) "Bugün" else data.day,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (data.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = colorScheme.onSurface.copy(alpha = if (data.isToday) 1f else 0.8f),
            modifier = Modifier.width(135.dp),
            maxLines = 1
        )

        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = WeatherMapper.getIconFromName(data.iconName),
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "${data.minTemp}°",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp)
        )

        TemperatureRangeBarCustom(
            modifier = Modifier
                .weight(2.2f)
                .padding(horizontal = 8.dp)
        )

        Text(
            text = "${data.maxTemp}°",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = colorScheme.onSurface,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
fun TemperatureRangeBarCustom(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(CircleShape)
            .background(colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight()
                .offset(x = 12.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.6f),
                            colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}
