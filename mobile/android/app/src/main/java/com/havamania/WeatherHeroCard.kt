package com.havamania

import android.provider.Settings
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalTime
import kotlin.random.Random

private const val TAG = "WeatherHeroCard"

// ── Models & Styles ──────────────────────────────────────────────────────────

data class WeatherCardStyle(
    val gradientColors: List<Color>,
    val lightOverlay: Color,
    val accentColor: Color,
    val icon: ImageVector,
    val isDark: Boolean,
    val styleName: String
)

// ── Style Resolver (The Core System) ──────────────────────────────────────────

object WeatherStyleResolver {
    @Composable
    fun resolve(
        condition: WeatherCondition,
        timeOfDay: TimeOfDay,
        theme: AppTheme
    ): WeatherCardStyle {
        val style = when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> resolveSunnyStyle(timeOfDay)
            is WeatherCondition.PartlyCloudy -> resolvePartlyCloudyStyle(timeOfDay)
            is WeatherCondition.Cloudy -> resolveCloudyStyle(timeOfDay)
            is WeatherCondition.Rain -> resolveRainStyle(timeOfDay)
            is WeatherCondition.Thunderstorm -> resolveThunderStyle(timeOfDay)
            is WeatherCondition.Snow -> resolveSnowStyle(timeOfDay)
            is WeatherCondition.Fog -> resolveFogStyle(timeOfDay)
        }

        // Final polish with theme atmosphere (Seasonal hints)
        return applyThemePolish(style, theme)
    }

    private fun resolveCloudyStyle(time: TimeOfDay): WeatherCardStyle {
        return when (time) {
            TimeOfDay.MORNING -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFFD8E6F2), Color(0xFFAFC1D2), Color(0xFF7F95AA)),
                lightOverlay = Color.White.copy(alpha = 0.3f),
                accentColor = Color(0xFF475569),
                icon = Icons.Rounded.Cloud,
                isDark = false,
                styleName = "CLOUDY_MORNING"
            )
            TimeOfDay.DAY -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFFC4D5E4), Color(0xFF9BAFC3), Color(0xFF6F849A)),
                lightOverlay = Color.White.copy(alpha = 0.2f),
                accentColor = Color(0xFF334155),
                icon = Icons.Rounded.Cloud,
                isDark = false,
                styleName = "CLOUDY_DAY"
            )
            TimeOfDay.EVENING -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFFB7AFC8), Color(0xFF8F8FA8), Color(0xFF5F7185)),
                lightOverlay = Color(0xFFFFCCBC).copy(alpha = 0.1f),
                accentColor = Color(0xFFF1F5F9),
                icon = Icons.Rounded.Cloud,
                isDark = true,
                styleName = "CLOUDY_EVENING"
            )
            TimeOfDay.NIGHT -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFF1B2432), Color(0xFF263447), Color(0xFF33485F)),
                lightOverlay = Color.Transparent,
                accentColor = Color(0xFFA7B5C5),
                icon = Icons.Rounded.Cloud,
                isDark = true,
                styleName = "CLOUDY_NIGHT"
            )
        }
    }

    private fun resolveSunnyStyle(time: TimeOfDay): WeatherCardStyle {
        return when (time) {
            TimeOfDay.MORNING -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFFFFADAD), Color(0xFFFFD1D1), Color(0xFFD0E1FF)),
                lightOverlay = Color(0xFFFFF9C4).copy(alpha = 0.4f),
                accentColor = Color(0xFFD84315),
                icon = Icons.Rounded.WbSunny,
                isDark = false,
                styleName = "SUNNY_MORNING"
            )
            TimeOfDay.DAY -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7DD3FC)),
                lightOverlay = Color.White.copy(alpha = 0.3f),
                accentColor = Color(0xFF0369A1),
                icon = Icons.Rounded.WbSunny,
                isDark = false,
                styleName = "SUNNY_DAY"
            )
            TimeOfDay.EVENING -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFFF97316), Color(0xFFFB923C), Color(0xFFFCD34D)),
                lightOverlay = Color(0xFFF472B6).copy(alpha = 0.2f),
                accentColor = Color.White,
                icon = Icons.Rounded.WbSunny,
                isDark = true,
                styleName = "SUNNY_EVENING"
            )
            TimeOfDay.NIGHT -> WeatherCardStyle(
                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
                lightOverlay = Color.Transparent,
                accentColor = Color(0xFFFDE68A),
                icon = Icons.Rounded.NightsStay,
                isDark = true,
                styleName = "SUNNY_NIGHT"
            )
        }
    }

    private fun resolvePartlyCloudyStyle(time: TimeOfDay): WeatherCardStyle {
        val base = resolveSunnyStyle(time)
        return base.copy(
            icon = if (time == TimeOfDay.NIGHT) Icons.Rounded.CloudQueue else Icons.Rounded.WbCloudy,
            styleName = "PARTLY_CLOUDY_$time"
        )
    }

    private fun resolveRainStyle(time: TimeOfDay): WeatherCardStyle {
        val isNight = time == TimeOfDay.NIGHT || time == TimeOfDay.EVENING
        return WeatherCardStyle(
            gradientColors = if (isNight) listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
                             else listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569)),
            lightOverlay = Color.White.copy(alpha = 0.05f),
            accentColor = Color(0xFF60A5FA),
            icon = Icons.Rounded.WaterDrop,
            isDark = true,
            styleName = "RAIN_$time"
        )
    }

    private fun resolveThunderStyle(time: TimeOfDay): WeatherCardStyle {
        return WeatherCardStyle(
            gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF1E293B)),
            lightOverlay = Color(0xFFC084FC).copy(alpha = 0.1f),
            accentColor = Color(0xFFC084FC),
            icon = Icons.Rounded.Thunderstorm,
            isDark = true,
            styleName = "THUNDER_$time"
        )
    }

    private fun resolveSnowStyle(time: TimeOfDay): WeatherCardStyle {
        val isNight = time == TimeOfDay.NIGHT || time == TimeOfDay.EVENING
        return WeatherCardStyle(
            gradientColors = if (isNight) listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF33485F))
                             else listOf(Color(0xFFE0F2FE), Color(0xFFF1F5F9), Color(0xFFFFFFFF)),
            lightOverlay = Color.White.copy(alpha = 0.4f),
            accentColor = if (isNight) Color(0xFF94A3B8) else Color(0xFF0EA5E9),
            icon = Icons.Rounded.AcUnit,
            isDark = isNight,
            styleName = "SNOW_$time"
        )
    }

    private fun resolveFogStyle(time: TimeOfDay): WeatherCardStyle {
        return WeatherCardStyle(
            gradientColors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFE2E8F0)),
            lightOverlay = Color.White.copy(alpha = 0.2f),
            accentColor = Color(0xFF475569),
            icon = Icons.Rounded.FilterDrama,
            isDark = false,
            styleName = "FOG_$time"
        )
    }

    private fun applyThemePolish(style: WeatherCardStyle, theme: AppTheme): WeatherCardStyle {
        // Subtle seasonal color mixing
        val (polishColor, accentTweak) = when (theme) {
            AppTheme.SPRING_DAY, AppTheme.SPRING_NIGHT -> Color(0xFFD1FAE5) to Color(0xFF10B981)
            AppTheme.SUMMER_DAY, AppTheme.SUMMER_NIGHT -> Color(0xFFFFF7ED) to Color(0xFFF59E0B)
            AppTheme.AUTUMN_DAY, AppTheme.AUTUMN_NIGHT -> Color(0xFFFEF3C7) to Color(0xFFD97706)
            AppTheme.WINTER_DAY, AppTheme.WINTER_NIGHT -> Color(0xFFEFF6FF) to Color(0xFF3B82F6)
            else -> null to null
        }

        return if (polishColor != null) {
            style.copy(
                gradientColors = style.gradientColors.map { it.lerp(polishColor, 0.08f) },
                accentColor = if (accentTweak != null) style.accentColor.lerp(accentTweak, 0.15f) else style.accentColor
            )
        } else style
    }

    private fun Color.lerp(other: Color, fraction: Float): Color {
        return Color(
            red = red + (other.red - red) * fraction,
            green = green + (other.green - green) * fraction,
            blue = blue + (other.blue - blue) * fraction,
            alpha = alpha + (other.alpha - alpha) * fraction
        )
    }
}

// ── Main Component ───────────────────────────────────────────────────────────

@Composable
fun WeatherHeroCard(
    cityName: String,
    temperature: String,
    conditionLabel: String,
    weatherCode: Int,
    isDay: Boolean,
    high: String,
    low: String,
    feelsLike: String,
    humidity: String,
    windSpeed: String,
    uvIndex: String,
    onCityClick: () -> Unit,
    modifier: Modifier = Modifier,
    districtName: String? = null,
    time: LocalTime = LocalTime.now(),
    parallaxOffset: Float = 0f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()
    val timeOfDay = remember(time) { WeatherMapper.resolveTimeOfDay(time.hour) }
    val condition = remember(weatherCode, isDay) { WeatherMapper.mapWeatherCodeToCondition(weatherCode, isDay) }

    val style = WeatherStyleResolver.resolve(condition, timeOfDay, currentTheme)

    // Debug Logs
    LaunchedEffect(condition, time.hour, currentTheme, style) {
        Log.d(TAG, "condition=$condition, hour=${time.hour}, timeOfDay=$timeOfDay, theme=$currentTheme, finalStyle=${style.styleName}")
    }

    val context = LocalContext.current
    val isReducedMotion = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f
        } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer {
                this.alpha = if (isReducedMotion) 1f else alpha
                this.scaleX = if (isReducedMotion) 1f else scale
                this.scaleY = if (isReducedMotion) 1f else scale
            }
            .clip(RoundedCornerShape(32.dp))
    ) {
        // Layer 1: Live Gradient Background
        LiveBackgroundLayer(colors = style.gradientColors, isReducedMotion = isReducedMotion)

        // Layer 2: Atmospheric Effect
        WeatherEffectLayer(
            condition = condition,
            timeOfDay = timeOfDay,
            weatherCode = weatherCode,
            isAnimationEnabled = !isReducedMotion,
            parallaxOffset = parallaxOffset
        )

        // Layer 3: Light Overlay
        Box(modifier = Modifier.fillMaxSize().background(style.lightOverlay))

        // Layer 4: Premium Content
        PremiumWeatherContent(
            cityName = cityName,
            districtName = districtName,
            conditionLabel = conditionLabel,
            temperature = temperature,
            feelsLike = feelsLike,
            humidity = humidity,
            windSpeed = windSpeed,
            uvIndex = uvIndex,
            style = style,
            onCityClick = onCityClick
        )

        // Subtle Inner Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 0.5.dp,
                    color = if (style.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(32.dp)
                )
        )
    }
}

// ── Background ─────────────────────────────────────────────────────────────

@Composable
fun LiveBackgroundLayer(colors: List<Color>, isReducedMotion: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_bg")
    val move by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse),
        label = "move"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val brush = Brush.linearGradient(
            colors = colors,
            start = Offset(move, -move),
            end = Offset(size.width - move, size.height + move)
        )
        drawRect(brush)
    }
}

// ── Content ──────────────────────────────────────────────────────────────────

@Composable
fun PremiumWeatherContent(
    cityName: String,
    districtName: String?,
    conditionLabel: String,
    temperature: String,
    feelsLike: String,
    humidity: String,
    windSpeed: String,
    uvIndex: String,
    style: WeatherCardStyle,
    onCityClick: () -> Unit
) {
    val textColor = if (style.isDark) Color.White else Color(0xFF0F172A)
    val secondaryColor = textColor.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // [ ÜST ] Şehir + Durum
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onCityClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (districtName ?: cityName).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = secondaryColor
                    )
                )
                if (districtName != null) {
                    Text(
                        text = cityName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    conditionLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }
            Icon(
                style.icon,
                null,
                tint = style.accentColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // [ ORTA ] Büyük Sıcaklık
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = temperature,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 100.sp,
                    fontWeight = FontWeight.W200,
                    letterSpacing = (-4).sp,
                    color = textColor
                )
            )
            Text(
                text = "Hissedilen $feelsLike",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryColor
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // [ ALT BAR ] Glass Effect
        GlassBottomBar(
            items = listOf(
                WeatherDetail(Icons.Rounded.WaterDrop, humidity),
                WeatherDetail(Icons.Rounded.Air, windSpeed),
                WeatherDetail(Icons.Rounded.WbSunny, "UV $uvIndex")
            ),
            textColor = textColor,
            isDark = style.isDark
        )
    }
}

@Composable
fun GlassBottomBar(
    items: List<WeatherDetail>,
    textColor: Color,
    isDark: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { detail ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(detail.icon, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        detail.value,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}

data class WeatherDetail(val icon: ImageVector, val value: String)
