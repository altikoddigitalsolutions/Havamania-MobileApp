package com.havamania

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
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
import java.time.LocalDateTime
import java.time.LocalTime

private const val TAG = "HourlyForecast"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HourlyForecastRow(
    modifier: Modifier = Modifier,
    items: List<HourlyWeather>,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    onItemSelect: (Int) -> Unit = {},
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    HourlyForecastContent(modifier, items, sunriseTime, sunsetTime, onItemSelect, currentTheme)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HourlyForecastContent(
    modifier: Modifier = Modifier,
    items: List<HourlyWeather>,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    onItemSelect: (Int) -> Unit = {},
    currentTheme: AppTheme
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val themeColors = HavamaniaTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "SAATLİK TAHMİN",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Black
            ),
            color = themeColors.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(start = 20.dp, end = 48.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> item.fullTime.ifBlank { "$index:${item.time}" } }
            ) { index, item ->
                HourlyForecastItem(
                    data = item,
                    sunriseTime = sunriseTime,
                    sunsetTime = sunsetTime,
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
    data: HourlyWeather,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    currentTheme: AppTheme,
    onClick: () -> Unit
) {
    val isSelected = data.isSelected
    val themeColors = HavamaniaTheme.colors

    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = interactionSource.collectIsPressedAsState()
    val isPressed = isPressedState.value

    val phase = remember(data.fullTime, data.time, sunriseTime, sunsetTime) {
        val sunrise = sunriseTime?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } }
        val sunset = sunsetTime?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } }

        val ldt = try {
            if (!data.fullTime.isNullOrEmpty()) {
                LocalDateTime.parse(data.fullTime)
            } else {
                // Fallback: data.time formatı "HH:mm" veya "23 Haz" olabilir
                val timeObj = if (data.time.contains(":")) {
                    LocalTime.of(data.time.split(":")[0].toInt(), 0)
                } else {
                    LocalTime.of(0, 0) // "23 Haz" gibi durumlarda gece yarısı kabul et
                }
                LocalDateTime.now().with(timeObj)
            }
        } catch (e: Exception) {
            LocalDateTime.now().with(LocalTime.of(12, 0))
        }

        WeatherMapper.getDayPhase(ldt, sunrise, sunset)
    }

    val condition = remember(data.weatherCode, data.isDay) {
        WeatherMapper.mapWeatherCodeToCondition(data.weatherCode, data.isDay)
    }

    val spec = WeatherStyleResolver.resolveSpec(condition, phase, currentTheme)

    val itemAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.88f,
        animationSpec = tween(500),
        label = "itemAlpha"
    )

    // Shared theme palette keeps selected text readable across the full gradient.
    val palette = com.havamania.ui.theme.themeButtonPalette(themeColors.gradientSecondary, themeColors.onAccent)
    val selectedGradient = Brush.verticalGradient(palette.stops)
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 2f)
    val unselectedBackground = if (currentTheme == AppTheme.LIGHT) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .width((88 * fontScale).dp)
            .heightIn(min = 160.dp)
            .graphicsLayer {
                val pressScale = if (isPressed) 0.96f else 1f
                scaleX = pressScale
                scaleY = pressScale
                alpha = itemAlpha
            }
            .clip(RoundedCornerShape(28.dp))
            .background(if (isSelected) selectedGradient else Brush.linearGradient(listOf(unselectedBackground, unselectedBackground)))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.35f) else if (currentTheme == AppTheme.LIGHT) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(28.dp)
            )
            .selectable(
                selected = isSelected,
                role = androidx.compose.ui.semantics.Role.Tab,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = data.time,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (isSelected) palette.content else themeColors.textPrimary
            )

            Icon(
                imageVector = spec.mainIcon,
                contentDescription = null,
                modifier = Modifier.size(if (isSelected) 32.dp else 28.dp),
                tint = if (isSelected) palette.content else themeColors.textPrimary
            )

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = data.temp,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = if (isSelected) palette.content else themeColors.textPrimary
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!data.precipProb.isNullOrEmpty() && data.precipProb != "0%" && data.precipProb != "0") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.WaterDrop,
                                null,
                                tint = if (isSelected) palette.content else themeColors.accent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = data.precipProb,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                color = if (isSelected) palette.content else themeColors.accent
                            )
                        }
                    } else {
                        Spacer(Modifier.height(22.dp))
                    }
                }
            }
        }
    }
}
