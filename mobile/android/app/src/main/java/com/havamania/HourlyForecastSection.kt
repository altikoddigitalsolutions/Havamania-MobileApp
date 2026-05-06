package com.havamania

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.HavamaniaTheme
import com.havamania.ui.theme.AppTheme

private const val TAG = "HourlyForecast"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HourlyForecastRow(
    modifier: Modifier = Modifier,
    items: List<HourlyForecastData>,
    onItemSelect: (Int) -> Unit = {},
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val themeColors = HavamaniaTheme.colors
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SAATLİK TAHMİN",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Black
            ),
            color = themeColors.textPrimary.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.time }
            ) { index, item ->
                HourlyForecastItem(
                    data = item,
                    currentTheme = currentTheme,
                    onClick = {
                        if (!item.isSelected) {
                            onItemSelect(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HourlyForecastItem(
    data: HourlyForecastData,
    currentTheme: AppTheme,
    onClick: () -> Unit
) {
    val isSelected = data.isSelected
    val themeColors = HavamaniaTheme.colors

    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    val timeOfDay = remember(data.time) {
        WeatherMapper.resolveTimeOfDay(try {
            data.time.split(":")[0].toInt()
        } catch (e: Exception) {
            if (data.isDay) 12 else 0
        })
    }

    val condition = remember(data.weatherCode, data.isDay) {
        WeatherMapper.mapWeatherCodeToCondition(data.weatherCode, data.isDay)
    }

    val style = WeatherStyleResolver.resolve(condition, timeOfDay, currentTheme)

    val itemAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = tween(500),
        label = "itemAlpha"
    )

    // Premium Blue/Cyan Gradient for Selected State
    val selectedGradient = remember {
        Brush.verticalGradient(listOf(Color(0xFF32BDF2), Color(0xFF1298D6)))
    }
    val unselectedBackground = if (currentTheme == AppTheme.LIGHT) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .width(88.dp)
            .height(160.dp)
            .graphicsLayer {
                val pressScale = if (isPressed) 0.96f else 1f
                scaleX = scale * pressScale
                scaleY = scale * pressScale
                alpha = itemAlpha
            }
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) selectedGradient else Brush.linearGradient(listOf(unselectedBackground, unselectedBackground)))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.35f) else if (currentTheme == AppTheme.LIGHT) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Subtle glow for selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                            radius = 180f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = data.time,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isSelected) Color.White else themeColors.textPrimary
            )

            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(if (isSelected) 32.dp else 28.dp),
                tint = if (isSelected) Color.White else themeColors.textPrimary
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = data.temp,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = if (isSelected) Color.White else themeColors.textPrimary
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier.height(22.dp).width(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!data.precipProb.isNullOrEmpty() && data.precipProb != "0%" && data.precipProb != "0") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.22f) else themeColors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.WaterDrop,
                                null,
                                tint = if (isSelected) Color.White else themeColors.accent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = data.precipProb,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = if (isSelected) Color.White else themeColors.accent
                            )
                        }
                    } else {
                        Spacer(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
