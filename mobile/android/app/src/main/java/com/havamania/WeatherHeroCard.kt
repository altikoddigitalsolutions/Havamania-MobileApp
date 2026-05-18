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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalTime
import kotlin.random.Random

private const val TAG = "WeatherHeroCard"
private const val SunnyDebugMode = true // Premium Sun Disk Debug Mode

enum class SunVariant {
    CLEAR_DAY,
    MOSTLY_SUNNY,
    PARTLY_CLOUDY,
    EVENING
}

// ── Models & Styles ──────────────────────────────────────────────────────────

data class WeatherCardStyle(
    val gradientColors: List<Color>,
    val lightOverlay: Color,
    val accentColor: Color,
    val icon: ImageVector,
    val isDark: Boolean,
    val styleName: String
)

// ── Style Resolver ──────────────────────────────────────────────────────────

object WeatherStyleResolver {
    @Composable
    fun resolve(
        condition: WeatherCondition,
        timeOfDay: TimeOfDay,
        theme: AppTheme
    ): WeatherCardStyle {
        val style = when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> resolveSunnyStyle(timeOfDay)
            is WeatherCondition.MostlySunny -> resolveMostlySunnyStyle(timeOfDay)
            is WeatherCondition.PartlyCloudy -> resolvePartlyCloudyStyle(timeOfDay)
            is WeatherCondition.Cloudy -> resolveCloudyStyle(timeOfDay)
            is WeatherCondition.Rain -> resolveRainStyle(timeOfDay)
            is WeatherCondition.Thunderstorm -> resolveThunderStyle(timeOfDay)
            is WeatherCondition.Snow -> resolveSnowStyle(timeOfDay)
            is WeatherCondition.Fog -> resolveFogStyle(timeOfDay)
        }
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
                gradientColors = listOf(Color(0xFFB0C4D6), Color(0xFF91A6B8), Color(0xFF708596)),
                lightOverlay = Color.White.copy(alpha = 0.15f),
                accentColor = Color(0xFF2C3E50),
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
                gradientColors = listOf(Color(0xFF56CCF2), Color(0xFF7DDDF5), Color(0xFFBDEBFF)),
                lightOverlay = Color.White.copy(alpha = 0.25f),
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

    private fun resolveMostlySunnyStyle(time: TimeOfDay): WeatherCardStyle {
        return when (time) {
            TimeOfDay.DAY -> resolveSunnyStyle(TimeOfDay.DAY).copy(styleName = "MOSTLY_SUNNY_DAY")
            TimeOfDay.MORNING -> resolveSunnyStyle(TimeOfDay.MORNING).copy(styleName = "MOSTLY_SUNNY_MORNING")
            TimeOfDay.EVENING -> resolveSunnyStyle(TimeOfDay.EVENING).copy(styleName = "MOSTLY_SUNNY_EVENING")
            TimeOfDay.NIGHT -> resolveSunnyStyle(TimeOfDay.NIGHT).copy(styleName = "MOSTLY_SUNNY_NIGHT")
        }
    }

    private fun resolvePartlyCloudyStyle(time: TimeOfDay): WeatherCardStyle {
        val base = resolveSunnyStyle(time)
        return base.copy(
            gradientColors = if (time == TimeOfDay.DAY) listOf(Color(0xFF4FA8E5), Color(0xFF8BCDF5), Color(0xFFC7E9FF)) else base.gradientColors,
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
    onNotificationClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
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

    val isSunnyScene = SunnyDebugMode ||
            condition is WeatherCondition.Clear ||
            condition is WeatherCondition.MostlySunny ||
            (condition is WeatherCondition.PartlyCloudy && timeOfDay != TimeOfDay.NIGHT)

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
        val sunVariant = if (isSunnyScene && timeOfDay != TimeOfDay.NIGHT) {
            when {
                timeOfDay == TimeOfDay.EVENING -> SunVariant.EVENING
                condition is WeatherCondition.MostlySunny -> SunVariant.MOSTLY_SUNNY
                condition is WeatherCondition.PartlyCloudy -> SunVariant.PARTLY_CLOUDY
                else -> SunVariant.CLEAR_DAY
            }
        } else null

        // Layer 1: Background (Integrated with Lighting)
        LiveBackgroundLayer(colors = style.gradientColors, isReducedMotion = isReducedMotion, sunVariant = sunVariant)

        // Layer 2: Atmospheric Effect (CLIPPED INSIDE)
        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(
                condition = condition,
                timeOfDay = timeOfDay,
                weatherCode = weatherCode,
                isAnimationEnabled = !isReducedMotion,
                parallaxOffset = parallaxOffset
            )
        }

        // Layer 3: Overlay
        Box(modifier = Modifier.fillMaxSize().background(style.lightOverlay))

        // Layer 4: Content
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
            isSunnyScene = isSunnyScene,
            onCityClick = onCityClick
        )

        PremiumNotificationButton(
            unreadCount = unreadNotificationCount,
            isDarkCard = style.isDark,
            onClick = onNotificationClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
        )

        // Border
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

@Composable
fun LiveBackgroundLayer(colors: List<Color>, isReducedMotion: Boolean, sunVariant: SunVariant? = null) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_bg")
    val move by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Reverse),
        label = "move"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * 0.82f, size.height * (if (sunVariant == SunVariant.EVENING) 0.32f else 0.23f))

        // 1. BASE DYNAMIC ATMOSPHERE
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(move, -move),
                end = Offset(size.width - move, size.height + move)
            )
        )

        // 2. MULTI-STAGE ATMOSPHERIC SCATTERING
        if (sunVariant != null) {
            val scatteringBase = when(sunVariant) {
                SunVariant.EVENING -> Color(0xFFFF7A3D)
                else -> Color(0xFFFFD166)
            }

            // Layer A: Wide Sky Wash (Environmental base)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to scatteringBase.copy(alpha = 0.12f),
                    0.5f to scatteringBase.copy(alpha = 0.04f),
                    1.0f to Color.Transparent,
                    center = sunCenter,
                    radius = size.width * 1.6f
                ),
                center = sunCenter,
                radius = size.width * 1.6f
            )

            // Layer B: Warm Haze Concentration (Near the source)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to scatteringBase.copy(alpha = 0.08f),
                    1.0f to Color.Transparent,
                    center = sunCenter,
                    radius = size.width * 0.8f
                ),
                center = sunCenter,
                radius = size.width * 0.8f
            )
        }
    }
}

// ── Weather Effects ──────────────────────────────────────────────────────────

@Composable
fun WeatherEffectLayer(
    condition: WeatherCondition,
    timeOfDay: TimeOfDay,
    weatherCode: Int,
    isAnimationEnabled: Boolean,
    parallaxOffset: Float
) {
    if (!isAnimationEnabled) return

    val isSunnyScene = SunnyDebugMode ||
            condition is WeatherCondition.Clear ||
            condition is WeatherCondition.MostlySunny ||
            (condition is WeatherCondition.PartlyCloudy && timeOfDay != TimeOfDay.NIGHT)

    Box(modifier = Modifier.fillMaxSize()) {
        val isSunnyScene = SunnyDebugMode ||
                condition is WeatherCondition.Clear ||
                condition is WeatherCondition.MostlySunny ||
                (condition is WeatherCondition.PartlyCloudy && timeOfDay != TimeOfDay.NIGHT)

        if (isSunnyScene && timeOfDay != TimeOfDay.NIGHT) {
            val sunVariant = when {
                timeOfDay == TimeOfDay.EVENING -> SunVariant.EVENING
                condition is WeatherCondition.MostlySunny -> SunVariant.MOSTLY_SUNNY
                condition is WeatherCondition.PartlyCloudy -> SunVariant.PARTLY_CLOUDY
                else -> SunVariant.CLEAR_DAY
            }

            PremiumSunEffect(variant = sunVariant)

            if (condition is WeatherCondition.MostlySunny) {
                CloudDriftEffect(count = 2, opacity = 0.20f)
            } else if (condition is WeatherCondition.PartlyCloudy) {
                CloudDriftEffect(count = 3, opacity = 0.32f)
            }
        } else {
            when (condition) {
                is WeatherCondition.Clear, is WeatherCondition.NightClear -> {
                    if (timeOfDay == TimeOfDay.NIGHT) StarFieldEffect()
                }
                is WeatherCondition.MostlySunny -> {
                    if (timeOfDay == TimeOfDay.NIGHT) StarFieldEffect()
                }
                is WeatherCondition.PartlyCloudy -> {
                    if (timeOfDay == TimeOfDay.NIGHT) {
                        StarFieldEffect()
                        CloudDriftEffect(count = 2, opacity = 0.25f, color = Color(0xFF94A3B8))
                    }
                }
                is WeatherCondition.Cloudy -> {
                    val cloudColor = if (timeOfDay == TimeOfDay.NIGHT) Color(0xFF64748B) else Color(0xFFD1D5DB)
                    if (timeOfDay == TimeOfDay.NIGHT) CloudDriftEffect(count = 4, opacity = 0.35f, color = cloudColor)
                    else CloudDriftEffect(count = 6, opacity = 0.42f, color = cloudColor)
                }
                is WeatherCondition.Rain -> RainEffect()
                is WeatherCondition.Thunderstorm -> {
                    RainEffect()
                    LightningEffect()
                }
                is WeatherCondition.Snow -> SnowParticleEffect()
                is WeatherCondition.Fog -> FogHazeEffect()
            }
        }
    }
}

@Composable
fun PremiumSunEffect(variant: SunVariant) {
    val infiniteTransition = rememberInfiniteTransition(label = "premium_sun")

    // Luxury motion: barely perceptible
    val drift by infiniteTransition.animateFloat(
        initialValue = 0.999f, targetValue = 1.001f,
        animationSpec = infiniteRepeatable(tween(30000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "drift"
    )

    val energy by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.012f,
        animationSpec = infiniteRepeatable(tween(20000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "energy"
    )

    // Cinematic configuration
    val sunRadiusBase = if (variant == SunVariant.EVENING) 46.dp else 36.dp
    val centerXMult = 0.82f
    val centerYMult = if (variant == SunVariant.EVENING) 0.32f else 0.23f

    val coreColors = when (variant) {
        SunVariant.EVENING -> listOf(Color(0xFFFFE5B4), Color(0xFFFF9933), Color(0xFFFF6600))
        SunVariant.MOSTLY_SUNNY -> listOf(Color(0xFFFFF9E0), Color(0xFFFFF5D0), Color(0xFFFFD88A))
        else -> listOf(Color(0xFFFFFDF0), Color(0xFFFFF3C4), Color(0xFFFFD166))
    }

    val atmosphereColor = when (variant) {
        SunVariant.EVENING -> Color(0xFFFF5500)
        else -> Color(0xFFFFB703)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * centerXMult, size.height * centerYMult)
        val r = sunRadiusBase.toPx() * energy

        // 1. DISSOLVED MULTI-LAYER GLOW SYSTEM
        // Level 4: Distant Bloom (Sky integration)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to atmosphereColor.copy(alpha = 0.04f * drift),
                1.0f to Color.Transparent,
                center = center,
                radius = 350.dp.toPx()
            ),
            center = center,
            radius = 350.dp.toPx()
        )

        // Level 3: Soft Diffusion (The airy glow)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to atmosphereColor.copy(alpha = 0.08f),
                1.0f to Color.Transparent,
                center = center,
                radius = 200.dp.toPx() * drift
            ),
            center = center,
            radius = 200.dp.toPx()
        )

        // Level 2: Warm Halo (Transition layer)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[1].copy(alpha = 0.15f),
                1.0f to Color.Transparent,
                center = center,
                radius = r * 3.5f
            ),
            center = center,
            radius = r * 3.5f
        )

        // Level 1: Source Bloom (Blends the core edge)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[1].copy(alpha = 0.25f),
                1.0f to Color.Transparent,
                center = center,
                radius = r * 1.8f
            ),
            center = center,
            radius = r * 1.8f
        )

        // 2. THE DISSOLVED CORE (No hard edges)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[0],
                0.3f to coreColors[1].copy(alpha = 0.95f),
                0.7f to coreColors[2].copy(alpha = 0.60f),
                1.0f to Color.Transparent, // Edge dissolve
                center = center,
                radius = r * 1.1f
            ),
            center = center,
            radius = r * 1.1f,
            alpha = if (variant == SunVariant.PARTLY_CLOUDY) 0.5f else 0.8f
        )

        // 3. INTERNAL LUMINANCE VARIATION (Subtle energy depth)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.White.copy(alpha = 0.12f * drift),
                1.0f to Color.Transparent,
                center = Offset(center.x - r * 0.2f, center.y - r * 0.2f),
                radius = r * 0.6f
            ),
            center = Offset(center.x - r * 0.2f, center.y - r * 0.2f),
            radius = r * 0.6f
        )

        // 4. ATMOSPHERIC INTERACTION
        if (variant == SunVariant.MOSTLY_SUNNY || variant == SunVariant.PARTLY_CLOUDY) {
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = r * 2.2f,
                center = Offset(center.x + (drift - 1f) * 30f, center.y + 15.dp.toPx())
            )
        }
    }
}

val SineEaseInOut = Easing { f ->
    ((1 - Math.cos(f * Math.PI)) / 2).toFloat()
}

// ── CLOUD DRAWING HELPER (Natural Path Silhouette) ──────────────────────────

fun DrawScope.drawCloud(center: Offset, scale: Float, color: Color) {
    val w = 150.dp.toPx() * scale
    val h = 80.dp.toPx() * scale

    // Multi-layered cloud for cinematic mass
    val cloudColor = color.copy(alpha = color.alpha * 0.8f)

    val path = Path().apply {
        moveTo(center.x - w * 0.4f, center.y + h * 0.25f)
        cubicTo(center.x - w * 0.65f, center.y + h * 0.1f, center.x - w * 0.6f, center.y - h * 0.35f, center.x - w * 0.25f, center.y - h * 0.25f)
        cubicTo(center.x - w * 0.1f, center.y - h * 0.75f, center.x + w * 0.3f, center.y - h * 0.7f, center.x + w * 0.35f, center.y - h * 0.2f)
        cubicTo(center.x + w * 0.65f, center.y - h * 0.1f, center.x + w * 0.7f, center.y + h * 0.35f, center.x + w * 0.4f, center.y + h * 0.3f)
        lineTo(center.x - w * 0.4f, center.y + h * 0.25f)
        close()
    }

    // Base Mass
    drawPath(path = path, color = cloudColor, style = Fill)

    // Subtle internal shading for volume
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
            center = center,
            radius = w * 0.5f
        ),
        style = Fill
    )
}

@Composable
fun CloudDriftEffect(count: Int = 3, opacity: Float = 0.3f, color: Color = Color.White) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by infiniteTransition.animateFloat(
        initialValue = -80f, targetValue = 80f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    // Remember cloud properties with fixed initial distribution
    val clouds = remember(count) {
        List(count) { i ->
            Triple(
                Offset(Random.nextFloat(), Random.nextFloat() * 0.35f + 0.05f),
                0.8f + Random.nextFloat() * 0.7f,
                0.5f + Random.nextFloat() * 1.0f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val finalColor = color.copy(alpha = opacity)
        clouds.forEach { (pos, scale, speed) ->
            val cloudW = 160.dp.toPx() * scale
            val x = (size.width * pos.x + drift * speed * 1.5f) % (size.width + cloudW * 2) - cloudW
            val y = size.height * pos.y
            drawCloud(Offset(x, y), scale, finalColor)
        }
    }
}

@Composable
fun RainEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val rainY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "rainY"
    )
    val particles = remember { List(35) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val x = p.x * size.width
            val y = (p.y * size.height + rainY) % size.height
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(x, y),
                end = Offset(x - 4f, y + 18f),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun LightningEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                0f at 0
                0f at 5000
                0.25f at 5050
                0f at 5100
                0.4f at 5150
                0f at 5300
            }
        ),
        label = "alpha"
    )
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha)))
}

@Composable
fun SnowParticleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val snowY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
        label = "snowY"
    )
    val particles = remember { List(45) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val x = p.x * size.width + (Math.sin(snowY.toDouble() / 80 + p.x * 15) * 25).toFloat()
            val y = (p.y * size.height + snowY) % size.height
            drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun StarFieldEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse),
        label = "twinkle"
    )
    val stars = remember { List(60) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, s ->
            val t = if (i % 2 == 0) twinkle else 1.3f - twinkle
            drawCircle(Color.White.copy(alpha = 0.5f * t), radius = 1.2.dp.toPx(), center = Offset(s.x * size.width, s.y * size.height))
        }
    }
}

@Composable
fun FogHazeEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(3) { i ->
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent)
                ),
                topLeft = Offset(-100f + drift * (i + 1) * 0.25f, size.height * (0.65f + i * 0.08f)),
                size = Size(size.width + 200f, 35.dp.toPx())
            )
        }
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
    isSunnyScene: Boolean,
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
        // [ ÜST ] Şehir + Durum (KONUM BUTONU)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (style.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                    .clickable { onCityClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    null,
                    tint = style.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (districtName ?: cityName).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = textColor
                            )
                        )
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint = secondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        "Konumu değiştir",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // [ ORTA ] Sıcaklık
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = temperature,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 110.sp,
                    fontWeight = FontWeight.W100,
                    letterSpacing = (-6).sp,
                    color = textColor
                )
            )
            Text(
                conditionLabel,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Hissedilen $feelsLike",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryColor
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // [ ALT BAR ]
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
            .height(64.dp),
        color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { detail ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(detail.icon, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        detail.value,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = textColor
                    )
                }
            }
        }
    }
}

data class WeatherDetail(val icon: ImageVector, val value: String)
