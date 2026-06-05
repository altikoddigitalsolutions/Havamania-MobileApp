package com.havamania

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.havamania.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

private const val TAG = "WeatherHeroCard"

enum class VisualEffectType { NONE, SUN, MOON, RAIN, SNOW, THUNDER, FOG, WIND }

data class WeatherCardVisualSpec(
    val gradientColors: List<Color>,
    val textColor: Color,
    val accentColor: Color,
    val mainIcon: ImageVector,
    val cloudDensity: Int,
    val effectType: VisualEffectType,
    val sunMoonPosition: Offset,
    val isDark: Boolean,
    val cloudColor: Color = Color.White,
    val phase: DayPhase = DayPhase.DAY,
    val sunOpacity: Float = 1.0f,
    val rainIntensity: Float = 1.0f,
    val atmosphereProgress: Float = 0.5f // 0 to 1 based on sun altitude
)

object WeatherStyleResolver {
    @Composable
    fun resolveSpec(
        weatherCode: Int,
        isDay: Boolean,
        phase: DayPhase,
        altitude: Float,
        tempValue: Float,
        theme: AppTheme
    ): WeatherCardVisualSpec {
        val condition = remember(weatherCode, isDay) { WeatherMapper.mapWeatherCodeToCondition(weatherCode, isDay) }

        // Normalize altitude for atmosphere progress (0 at night, 1 at peak day)
        val progress = ((altitude + 18f) / (90f + 18f)).coerceIn(0f, 1f)

        // 1. Unified Dynamic Gradients - Natural and Atmospheric
        val baseColors = when (phase) {
            DayPhase.DAWN -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy) {
                    listOf(Color(0xFF2D3748), Color(0xFF4A5568), Color(0xFF94A3B8)) // Muted Lavender Gray
                } else {
                    listOf(Color(0xFF1E1B4B), Color(0xFF7C2D12), Color(0xFFFB923C), Color(0xFFFEF3C7))
                }
            }
            DayPhase.GOLDEN_HOUR -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy) {
                    listOf(Color(0xFF1F2937), Color(0xFF374151), Color(0xFF9CA3AF))
                } else {
                    listOf(Color(0xFF0F172A), Color(0xFF7C2D12), Color(0xFFF97316), Color(0xFFFDE68A))
                }
            }
            DayPhase.MORNING, DayPhase.DAY -> {
                when {
                    condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm ->
                        listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B)) // Slate Atmosphere
                    condition is WeatherCondition.Snow ->
                        listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFFF8FAFC))
                    condition is WeatherCondition.Fog ->
                        listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFCBD5E1))
                    condition is WeatherCondition.PartlyCloudy ->
                        listOf(Color(0xFF0369A1), Color(0xFF0EA5E9), Color(0xFFBAE6FD))
                    condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy ->
                        listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF94A3B8)) // Balanced Gray-Blue
                    else -> {
                        // Natural Blue Sky: Deep at top, soft at horizon
                        if (tempValue > 28f) listOf(Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFFFDE68A)) // Warm clear
                        else listOf(Color(0xFF0369A1), Color(0xFF0EA5E9), Color(0xFFBAE6FD)) // Crisp clear
                    }
                }
            }
            DayPhase.DUSK, DayPhase.BLUE_HOUR, DayPhase.TWILIGHT, DayPhase.NIGHT -> {
                if (condition is WeatherCondition.Overcast || condition is WeatherCondition.Cloudy || condition is WeatherCondition.PartlyCloudy || condition is WeatherCondition.Fog || condition is WeatherCondition.Rain) {
                    listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
                } else {
                    listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E1B4B)) // Midnight
                }
            }
            else -> listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFFBAE6FD))
        }

        val situationalColors = baseColors // Already handled condition nuances above

        // 2. Effect System
        val effectType = when {
            condition is WeatherCondition.Thunderstorm -> VisualEffectType.THUNDER
            condition is WeatherCondition.Rain -> VisualEffectType.RAIN
            condition is WeatherCondition.Snow -> VisualEffectType.SNOW
            condition is WeatherCondition.Fog -> VisualEffectType.FOG
            altitude <= -2f -> {
                if (condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || condition is WeatherCondition.PartlyCloudy || condition is WeatherCondition.NightClear) VisualEffectType.MOON
                else VisualEffectType.NONE
            }
            else -> {
                if (condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || condition is WeatherCondition.PartlyCloudy) VisualEffectType.SUN
                else VisualEffectType.NONE
            }
        }

        // 3. Density Controls
        val cloudDensity = when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> 0
            is WeatherCondition.MostlySunny -> 1
            is WeatherCondition.PartlyCloudy -> 2
            is WeatherCondition.Cloudy -> 4
            is WeatherCondition.Overcast -> 6
            else -> 3
        }

        val rainIntensity = when (condition) {
            is WeatherCondition.Thunderstorm -> 1.5f
            is WeatherCondition.Rain -> 0.8f
            else -> 0.4f
        }

        // 4. Sun/Moon Position calculated from real altitude
        // Map 0..90 altitude to Y coordinate 0.8..0.15
        val sunY = 0.8f - ((altitude.coerceIn(0f, 90f) / 90f) * 0.65f)
        val sunMoonPos = Offset(0.85f, sunY)

        val isDark = altitude < 0f || condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm || condition is WeatherCondition.Overcast
        val textColor = if (isDark) Color.White else Color(0xFF0F172A)

        val accentColor = when {
            phase == DayPhase.GOLDEN_HOUR -> Color(0xFFFFD166)
            altitude < 0f -> Color(0xFF74B9FF)
            else -> if (isDark) Color(0xFF81ECEC) else Color(0xFF0984E3)
        }

        val iconName = WeatherMapper.getWeatherIconName(weatherCode, altitude)
        val mainIcon = WeatherMapper.getIconFromName(iconName)

        return applyPolish(
            WeatherCardVisualSpec(
                gradientColors = situationalColors,
                textColor = textColor,
                accentColor = accentColor,
                mainIcon = mainIcon,
                cloudDensity = cloudDensity,
                effectType = effectType,
                sunMoonPosition = sunMoonPos,
                isDark = isDark,
                phase = phase,
                sunOpacity = (altitude / 10f).coerceIn(0.1f, 1.0f),
                rainIntensity = rainIntensity,
                atmosphereProgress = progress
            ), theme
        )
    }

    private fun applyPolish(spec: WeatherCardVisualSpec, theme: AppTheme): WeatherCardVisualSpec {
        val (polishColor, accentTweak) = when (theme) {
            AppTheme.SPRING_DAY, AppTheme.SPRING_NIGHT -> Color(0xFFD1FAE5) to Color(0xFF10B981)
            AppTheme.SUMMER_DAY, AppTheme.SUMMER_NIGHT -> Color(0xFFFFF7ED) to Color(0xFFF59E0B)
            AppTheme.AUTUMN_DAY, AppTheme.AUTUMN_NIGHT -> Color(0xFFFEF3C7) to Color(0xFFD97706)
            AppTheme.WINTER_DAY, AppTheme.WINTER_NIGHT -> Color(0xFFEFF6FF) to Color(0xFF3B82F6)
            else -> null to null
        }
        return if (polishColor != null) {
            spec.copy(
                gradientColors = spec.gradientColors.map { it.lerp(polishColor, 0.08f) },
                accentColor = if (accentTweak != null) spec.accentColor.lerp(accentTweak, 0.15f) else spec.accentColor
            )
        } else spec
    }

    private fun Color.lerp(other: Color, fraction: Float): Color = Color(
        red + (other.red - red) * fraction,
        green + (other.green - green) * fraction,
        blue + (other.blue - blue) * fraction,
        alpha + (other.alpha - alpha) * fraction
    )
}

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
    onNotificationsClick: () -> Unit = {},
    unreadCount: Int = 0,
    modifier: Modifier = Modifier,
    districtName: String? = null,
    time: LocalTime = LocalTime.now(),
    fullTime: String? = null,
    sunriseTime: String? = null,
    sunsetTime: String? = null,
    parallaxOffset: Float = 0f,
    solarAltitude: Float = 45f,
    themeViewModel: com.havamania.ui.theme.ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val currentTheme by themeViewModel.currentTheme.collectAsState()

    val phase = remember(time, fullTime, sunriseTime, sunsetTime) {
        val sunrise = try { LocalTime.parse(sunriseTime) } catch (e: Exception) { LocalTime.of(6, 30) }
        val sunset = try { LocalTime.parse(sunsetTime) } catch (e: Exception) { LocalTime.of(19, 30) }
        val dateTime = if (fullTime != null) {
            try { LocalDateTime.parse(fullTime) } catch (e: Exception) { LocalDateTime.now().with(time) }
        } else {
            LocalDateTime.now().with(time)
        }
        WeatherMapper.getDayPhase(dateTime, sunrise, sunset)
    }

    val tempVal = remember(temperature) { temperature.filter { it.isDigit() || it == '-' }.toFloatOrNull() ?: 20f }
    val spec = WeatherStyleResolver.resolveSpec(weatherCode, isDay, phase, solarAltitude, tempVal, currentTheme)

    val context = LocalContext.current
    val isReducedMotion = remember {
        try { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.98f, animationSpec = tween(1000))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(1200))

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
            .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(32.dp))
    ) {
        Crossfade(targetState = spec, animationSpec = tween(1500), label = "hero_bg") { targetSpec ->
            LiveBackgroundLayer(spec = targetSpec)
        }

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(spec = spec, isAnimationEnabled = !isReducedMotion)
        }

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = conditionLabel, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                spec = spec, unreadCount = unreadCount, weatherIcon = spec.mainIcon,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(spec: WeatherCardVisualSpec) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sunCenter = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

            // 1. Base Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to spec.gradientColors.first(),
                    0.6f to spec.gradientColors.getOrElse(1) { spec.gradientColors.first() },
                    1.0f to spec.gradientColors.last()
                )
            )

            // 2. Horizon Glow & Atmosphere Scattering
            val horizonGlow = when (spec.phase) {
                DayPhase.DAWN -> Color(0xFFFF8C94).copy(0.15f)
                DayPhase.GOLDEN_HOUR -> Color(0xFFFB923C).copy(0.18f)
                DayPhase.MORNING, DayPhase.DAY -> Color(0xFFBAE6FD).copy(0.12f)
                DayPhase.BLUE_HOUR -> Color(0xFF3B82F6).copy(0.08f)
                DayPhase.NIGHT -> Color(0xFF1E293B).copy(0.06f)
                else -> Color.White.copy(0.05f)
            }

            drawRect(
                brush = Brush.verticalGradient(
                    0.7f to Color.Transparent,
                    1.0f to horizonGlow
                )
            )

            // 3. Directional Ambient Lighting
            val ambientColor = when (spec.phase) {
                DayPhase.DAWN -> Color(0xFFFBCFE8).copy(0.08f)
                DayPhase.GOLDEN_HOUR -> Color(0xFFFDE68A).copy(0.1f)
                DayPhase.NIGHT -> Color(0xFF94A3B8).copy(0.03f)
                else -> Color.White.copy(0.04f)
            }

            drawCircle(
                brush = Brush.radialGradient(0.0f to ambientColor, 1.0f to Color.Transparent, center = sunCenter, radius = size.width * 1.2f),
                center = sunCenter, radius = size.width * 1.2f, blendMode = BlendMode.Screen
            )

            // 4. Subtle Haze Layer for Heavy Weather
            val hazeOpacity = when {
                spec.effectType == VisualEffectType.FOG -> 0.15f
                spec.effectType == VisualEffectType.RAIN -> 0.08f
                spec.cloudDensity > 10 -> 0.05f
                else -> 0f
            }
            if (hazeOpacity > 0f) {
                drawRect(color = Color.White.copy(alpha = hazeOpacity), blendMode = BlendMode.Overlay)
            }

            // 5. Cinematic Vignette
            drawRect(
                brush = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.65f to Color.Transparent,
                    1.0f to Color.Black.copy(0.15f),
                    center = center,
                    radius = size.width * 1.4f
                )
            )
        }
    }
}

@Composable
fun WeatherEffectLayer(spec: WeatherCardVisualSpec, isAnimationEnabled: Boolean) {
    if (!isAnimationEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        if (spec.phase in listOf(DayPhase.NIGHT, DayPhase.TWILIGHT, DayPhase.BLUE_HOUR)) {
            StarFieldEffect()
        }

        when (spec.effectType) {
            VisualEffectType.SUN -> PremiumSunEffect(spec)
            VisualEffectType.MOON -> PremiumMoonEffect(spec)
            VisualEffectType.RAIN -> RainEffect(spec.rainIntensity)
            VisualEffectType.THUNDER -> { RainEffect(2.2f); LightningEffect() }
            VisualEffectType.SNOW -> SnowParticleEffect()
            VisualEffectType.FOG -> FogHazeEffect()
            else -> {}
        }

        if (spec.cloudDensity > 0) {
            CloudDriftEffect(count = spec.cloudDensity, color = Color.White)
        }
    }
}

@Composable
fun PremiumSunEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val bloomPulse by infiniteTransition.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(8000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_bloom")

    val coreColor = when (spec.phase) {
        DayPhase.GOLDEN_HOUR -> Color(0xFFFBBF24)
        DayPhase.DAWN -> Color(0xFFFFD1D1)
        else -> Color(0xFFFFF9C4)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

        // 1. Atmosphere Bloom (Subtle)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColor.copy(0.08f * spec.sunOpacity * bloomPulse),
                0.6f to coreColor.copy(0.02f * spec.sunOpacity),
                1.0f to Color.Transparent,
                center = center,
                radius = 200.dp.toPx()
            ),
            center = center,
            radius = 200.dp.toPx(),
            blendMode = BlendMode.Screen
        )

        // 2. Secondary Glow
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColor.copy(0.3f * spec.sunOpacity),
                1.0f to Color.Transparent,
                center = center,
                radius = 60.dp.toPx()
            ),
            center = center,
            radius = 60.dp.toPx()
        )

        // 3. Sun Core (Soft source)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.White,
                0.5f to coreColor.copy(0.9f),
                1.0f to coreColor.copy(0f),
                center = center,
                radius = 18.dp.toPx()
            ),
            center = center,
            radius = 18.dp.toPx(),
            alpha = spec.sunOpacity * 0.95f
        )
    }
}

@Composable
fun PremiumMoonEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val glowPulse by infiniteTransition.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse), label = "moon_glow")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 20.dp.toPx()

        // 1. Moon Haze/Bloom
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF94A3B8).copy(0.18f * glowPulse),
                1.0f to Color.Transparent,
                center = center,
                radius = 140.dp.toPx()
            ),
            center = center,
            radius = 140.dp.toPx(),
            blendMode = BlendMode.Screen
        )

        // 2. Moon Shape (Crescent logic)
        val path = Path().apply { addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r)) }
        val clipPath = Path().apply { addOval(Rect(center.x - r * 1.5f, center.y - r * 1.2f, center.x + r * 0.4f, center.y + r * 0.8f)) }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(
            path,
            brush = Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFF94A3B8))),
            alpha = 0.95f
        )
        drawContext.canvas.restore()
    }
}

val SineEaseInOut = Easing { f -> ((1 - Math.cos(f * Math.PI)) / 2).toFloat() }

fun DrawScope.drawCloud(center: Offset, scale: Float, color: Color, opacity: Float) {
    val w = 160.dp.toPx() * scale
    val radius = w * 0.5f

    // Natural cloud mass using overlapping radial gradients instead of a single path
    val positions = listOf(
        Offset(0f, 0f),
        Offset(-w * 0.25f, 0f),
        Offset(w * 0.25f, 0f),
        Offset(0f, -w * 0.15f)
    )

    positions.forEach { relPos ->
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to color.copy(alpha = opacity),
                0.7f to color.copy(alpha = opacity * 0.5f),
                1.0f to Color.Transparent,
                center = center + relPos,
                radius = radius
            ),
            center = center + relPos,
            radius = radius
        )
    }
}

@Composable
private fun IndividualCloud(cloud: CloudState, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_${cloud.x}_${cloud.y}")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cloud.duration, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000 + (cloud.y * 5000).toInt(), easing = SineEaseInOut), RepeatMode.Reverse),
        label = "float"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = 260.dp.toPx() * cloud.scale
        val totalSpan = size.width + cw * 2
        val x = ((cloud.x + drift) % 1f) * totalSpan - cw
        val yOscillation = (floatAnim - 0.5f) * 20.dp.toPx()
        drawCloud(Offset(x, size.height * cloud.y + yOscillation), cloud.scale, color, cloud.opacity)
    }
}

@Composable
fun CloudDriftEffect(count: Int, color: Color) {
    val clouds = remember(count) {
        List(count) { i ->
            val isFar = i % 3 == 0
            val isMid = i % 3 == 1
            CloudState(
                x = (i.toFloat() / count) + (Random.nextFloat() * 0.2f),
                y = if (isFar) (Random.nextFloat() * 0.2f) else (Random.nextFloat() * 0.4f + 0.5f), // Bias away from center
                scale = if (isFar) 2.8f else if (isMid) 1.6f else 0.8f,
                speed = 1f,
                opacity = if (isFar) 0.1f else if (isMid) 0.18f else 0.28f,
                duration = if (isFar) 140000 else if (isMid) 110000 else 80000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            IndividualCloud(cloud = cloud, color = color)
        }
    }
}

private data class CloudState(val x: Float, val y: Float, val scale: Float, val speed: Float, val opacity: Float, val duration: Int)

@Composable
fun RainEffect(intensity: Float = 1.0f) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")

    // Multiple layers for depth - Thinner and more natural
    val layers = listOf(
        RainLayer(count = 20, speed = 1.3f, alpha = 0.08f, length = 35f, width = 1f),  // Background
        RainLayer(count = 35, speed = 1.0f, alpha = 0.25f, length = 25f, width = 0.8f) // Foreground
    )

    layers.forEach { layer ->
        val ry by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2000f,
            animationSpec = infiniteRepeatable(tween((1400 / (intensity * layer.speed)).toInt(), easing = LinearEasing)),
            label = "rain_fall"
        )

        val particles = remember(intensity, layer.count) {
            List(layer.count) { Offset(Random.nextFloat(), Random.nextFloat()) }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val x = p.x * size.width
                val y = (p.y * size.height + ry) % size.height

                drawLine(
                    color = Color.White.copy(alpha = layer.alpha),
                    start = Offset(x, y),
                    end = Offset(x, y + layer.length),
                    strokeWidth = layer.width.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private data class RainLayer(val count: Int, val speed: Float, val alpha: Float, val length: Float, val width: Float)

@Composable
fun LightningEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")
    val a by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 15000
                0f at 0
                0.2f at 10000
                0f at 10050
                0.3f at 10100
                0f at 10250
            }
        ),
        label = "lightning_alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(a * 0.2f)))
}

@Composable
fun SnowParticleEffect() {
    val sy by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(15000, easing = LinearEasing)))
    val p = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            val x = dr.x * size.width + (Math.sin(sy.toDouble() / 300 + dr.x * 20) * 35).toFloat()
            val y = (dr.y * size.height + sy) % size.height

            drawCircle(
                Color.White.copy(0.45f),
                radius = 1.6.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun StarFieldEffect() {
    val infiniteTransition = rememberInfiniteTransition()
    val twinkling by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(5000), RepeatMode.Reverse))

    val stars = remember {
        List(30) {
            StarData(
                pos = Offset(Random.nextFloat(), Random.nextFloat()),
                size = 0.5f + Random.nextFloat() * 1.2f,
                alphaBase = 0.05f + Random.nextFloat() * 0.3f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, star ->
            val alpha = star.alphaBase * (if (i % 4 == 0) twinkling else 1f)
            if (alpha > 0.05f) {
                drawCircle(
                    Color.White.copy(alpha = alpha.coerceIn(0f, 0.4f)),
                    star.size.dp.toPx(),
                    Offset(star.pos.x * size.width, star.pos.y * size.height)
                )
            }
        }
    }
}

private data class StarData(val pos: Offset, val size: Float, val alphaBase: Float)

@Composable
fun FogHazeEffect() {
    val op by rememberInfiniteTransition().animateFloat(
        initialValue = 0.08f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(30000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "fog_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Vertical atmospheric haze - Muted blue-grey
        drawRect(
            Brush.verticalGradient(
                0.4f to Color.Transparent,
                0.9f to Color(0xFF94A3B8).copy(alpha = op),
                1.0f to Color(0xFF64748B).copy(alpha = op * 1.5f)
            ),
            blendMode = BlendMode.Screen
        )
    }
}

@Composable
fun PremiumWeatherContent(
    cityName: String, districtName: String?, conditionLabel: String, temperature: String, feelsLike: String, humidity: String, windSpeed: String, uvIndex: String,
    spec: WeatherCardVisualSpec, unreadCount: Int, weatherIcon: ImageVector, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = spec.textColor
    val secondaryColor = textColor.copy(alpha = 0.7f)
    Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                .background(if (spec.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f))
                .clickable { onCityClick() }
                .padding(10.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = spec.accentColor.copy(0.9f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(8.dp))
                Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = textColor))
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, spec.isDark, textColor, onNotificationsClick)
                Spacer(Modifier.width(14.dp))
                Icon(weatherIcon, null, tint = spec.accentColor, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.weight(0.6f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 105.sp, fontWeight = FontWeight.W100, letterSpacing = (-4).sp, color = textColor))
            Text(conditionLabel.uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.5.sp, color = textColor.copy(0.9f)))
            Text("HİSSEDİLEN $feelsLike", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = secondaryColor))
        }
        Spacer(Modifier.weight(1f))
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp),
            color = if (spec.isDark) Color.White.copy(0.08f) else Color.Black.copy(0.04f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.6.dp, if (spec.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f))) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val uvIcon = if (spec.phase in listOf(DayPhase.NIGHT, DayPhase.TWILIGHT, DayPhase.BLUE_HOUR)) Icons.Rounded.Brightness3 else Icons.Rounded.WbSunny
                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, uvIcon to "UV $uvIndex").forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = textColor.copy(0.5f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(valStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumNotificationButton(unreadCount: Int, isDark: Boolean, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Notifications, null, tint = tint.copy(0.85f), modifier = Modifier.size(20.dp))
        if (unreadCount > 0) Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(9.dp).clip(CircleShape).background(Color(0xFF3B82F6)).border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape))
    }
}
