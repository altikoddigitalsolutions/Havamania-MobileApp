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
    val overlayAlpha: Float,
    val textColor: Color,
    val accentColor: Color,
    val mainIcon: ImageVector,
    val cloudDensity: Int,
    val effectType: VisualEffectType,
    val sunMoonPosition: Offset, // Normalized 0..1
    val isDark: Boolean,
    val cloudColor: Color = Color.White,
    val phase: DayPhase = DayPhase.DAY,
    val sunOpacity: Float = 1.0f
)

object WeatherStyleResolver {
    @Composable
    fun resolveSpec(condition: WeatherCondition, phase: DayPhase, theme: AppTheme, windSpeed: Float = 0f): WeatherCardVisualSpec {
        // 1. Base Colors by Phase & Condition - Refactored for natural realism
        val baseColors = when (phase) {
            DayPhase.DAWN -> when (condition) {
                is WeatherCondition.Clear, is WeatherCondition.MostlySunny -> listOf(Color(0xFF2C3E50), Color(0xFF4A90E2), Color(0xFFFFD194)) // Natural Dawn
                else -> listOf(Color(0xFF30336B), Color(0xFF535C68), Color(0xFF95AFC0))
            }
            DayPhase.MORNING -> when (condition) {
                is WeatherCondition.Overcast, is WeatherCondition.Cloudy -> listOf(Color(0xFFD8E5F2), Color(0xFFECEFF1), Color(0xFFF1F5F8))
                is WeatherCondition.Clear, is WeatherCondition.MostlySunny -> listOf(Color(0xFF54A0FF), Color(0xFF81D4FA), Color(0xFFB3E5FC)) // Pastel Morning
                else -> listOf(Color(0xFF8BA9C5), Color(0xFFA5BFD3), Color(0xFFC0D5E1))
            }
            DayPhase.DAY -> when (condition) {
                is WeatherCondition.Clear, is WeatherCondition.MostlySunny -> listOf(Color(0xFF1E90FF), Color(0xFF70A1FF), Color(0xFFD1E8FF))
                is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> listOf(Color(0xFF2C3E50), Color(0xFF4B6584), Color(0xFF778CA3))
                is WeatherCondition.Overcast -> listOf(Color(0xFF78909C), Color(0xFFB0BEC5), Color(0xFFCFD8DC))
                is WeatherCondition.Cloudy, is WeatherCondition.PartlyCloudy -> listOf(Color(0xFF485E74), Color(0xFF7F8C8D), Color(0xFFBDC3C7))
                else -> listOf(Color(0xFF3498DB), Color(0xFF5DADE2), Color(0xFF85C1E9))
            }
            DayPhase.GOLDEN_HOUR -> when (condition) {
                is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> listOf(Color(0xFF1A1A2E), Color(0xFFD35400), Color(0xFFE67E22))
                else -> listOf(Color(0xFF2C3E50), Color(0xFFE67E22), Color(0xFFF1C40F)) // Amber Sunset
            }
            DayPhase.DUSK -> listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81))
            DayPhase.EVENING -> listOf(Color(0xFF0A1230), Color(0xFF162B5E), Color(0xFF1F3F8A))
            DayPhase.NIGHT -> listOf(Color(0xFF0A1230), Color(0xFF162B5E), Color(0xFF1F3F8A)) // Luxurious Night
            else -> listOf(Color(0xFF1E90FF), Color(0xFF70A1FF), Color(0xFFD1E8FF))
        }

        // 2. Effect Mapping - Realistic Priorities
        val effectType = when {
            windSpeed > 30f && condition !is WeatherCondition.Rain && condition !is WeatherCondition.Thunderstorm -> VisualEffectType.WIND
            condition is WeatherCondition.Rain -> VisualEffectType.RAIN
            condition is WeatherCondition.Thunderstorm -> VisualEffectType.THUNDER
            condition is WeatherCondition.Snow -> VisualEffectType.SNOW
            condition is WeatherCondition.Fog -> VisualEffectType.FOG
            phase in listOf(DayPhase.NIGHT, DayPhase.DUSK, DayPhase.EVENING, DayPhase.DAWN) -> {
                if (condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || condition is WeatherCondition.PartlyCloudy) VisualEffectType.MOON
                else VisualEffectType.NONE
            }
            else -> {
                if (condition is WeatherCondition.Clear || condition is WeatherCondition.MostlySunny || condition is WeatherCondition.PartlyCloudy) VisualEffectType.SUN
                else VisualEffectType.NONE
            }
        }

        // 3. Density & Focus - Refined for atmosphere
        val cloudDensity = when (condition) {
            is WeatherCondition.Clear -> 0
            is WeatherCondition.MostlySunny -> 1
            is WeatherCondition.PartlyCloudy -> 3
            is WeatherCondition.Cloudy -> 6
            is WeatherCondition.Overcast -> 12
            is WeatherCondition.Rain, is WeatherCondition.Thunderstorm -> 8
            is WeatherCondition.Fog -> 4
            else -> 2
        }

        // 4. Sun/Moon Logic - Refactored for Premium feel
        val sunOpacity = when {
            condition is WeatherCondition.Overcast -> 0.0f
            condition is WeatherCondition.Cloudy -> 0.15f
            condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm -> 0.05f
            condition is WeatherCondition.PartlyCloudy -> 0.45f
            phase == DayPhase.DAWN -> 0.6f
            else -> 1.0f
        }

        val sunMoonPos = when (phase) {
            DayPhase.GOLDEN_HOUR -> Offset(0.85f, 0.65f) // Closer to horizon
            DayPhase.DUSK -> Offset(0.85f, 0.70f)
            DayPhase.EVENING -> Offset(0.85f, 0.18f)
            DayPhase.DAWN -> Offset(0.15f, 0.75f) // Closer to horizon
            DayPhase.NIGHT -> Offset(0.85f, 0.18f) // Higher
            DayPhase.MORNING -> Offset(0.35f, 0.25f)
            DayPhase.DAY -> Offset(0.82f, 0.15f)
            else -> Offset(0.82f, 0.15f)
        }

        val isDark = phase in listOf(DayPhase.NIGHT, DayPhase.DUSK, DayPhase.EVENING, DayPhase.DAWN) || condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm || condition is WeatherCondition.Overcast
        val textColor = if (isDark) Color.White else Color(0xFF0F172A)

        val accentColor = when (phase) {
            DayPhase.GOLDEN_HOUR -> Color(0xFFFFD166)
            DayPhase.NIGHT, DayPhase.EVENING -> Color(0xFF74B9FF)
            DayPhase.DUSK -> Color(0xFF81ECEC)
            DayPhase.DAWN -> Color(0xFFBDEBFF)
            else -> if (isDark) Color(0xFF81ECEC) else Color(0xFF0984E3)
        }

        val cloudColor = when (phase) {
            DayPhase.GOLDEN_HOUR -> Color(0xFFFFEAA7).copy(alpha = 0.3f)
            DayPhase.NIGHT, DayPhase.EVENING -> Color(0xFF2C3E50).copy(alpha = 0.25f)
            DayPhase.DUSK -> Color(0xFF1E1B4B).copy(alpha = 0.25f)
            DayPhase.MORNING -> Color.White.copy(alpha = 0.4f)
            else -> if (isDark) Color(0xFFBDC3C7).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
        }

        val isNightPhase = phase in listOf(DayPhase.NIGHT, DayPhase.DUSK, DayPhase.EVENING, DayPhase.DAWN)

        val mainIcon = when (condition) {
            is WeatherCondition.Rain -> Icons.Rounded.WaterDrop
            is WeatherCondition.Snow -> Icons.Rounded.AcUnit
            is WeatherCondition.Thunderstorm -> Icons.Rounded.Thunderstorm
            is WeatherCondition.Fog -> Icons.Rounded.FilterDrama
            is WeatherCondition.Overcast, is WeatherCondition.Cloudy -> Icons.Rounded.Cloud
            is WeatherCondition.PartlyCloudy -> if (isNightPhase) Icons.Rounded.CloudQueue else Icons.Rounded.WbCloudy
            else -> if (isNightPhase) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
        }

        return applyPolish(
            WeatherCardVisualSpec(
                gradientColors = baseColors,
                overlayAlpha = when(phase) {
                    DayPhase.GOLDEN_HOUR -> 0.08f
                    DayPhase.NIGHT, DayPhase.EVENING -> 0.15f
                    DayPhase.DUSK -> 0.12f
                    DayPhase.DAWN -> 0.1f
                    else -> 0.02f
                },
                textColor = textColor,
                accentColor = accentColor,
                mainIcon = mainIcon,
                cloudDensity = cloudDensity,
                effectType = effectType,
                sunMoonPosition = sunMoonPos,
                isDark = isDark,
                cloudColor = cloudColor,
                phase = phase,
                sunOpacity = sunOpacity
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

    val isActualDay = remember(phase) {
        phase == DayPhase.MORNING || phase == DayPhase.DAY || phase == DayPhase.GOLDEN_HOUR
    }

    val condition = remember(weatherCode, isActualDay) {
        WeatherMapper.mapWeatherCodeToCondition(weatherCode, isActualDay)
    }
    val windSpeedValue = remember(windSpeed) { try { windSpeed.filter { it.isDigit() || it == '.' }.toFloat() } catch(e: Exception) { 0f } }
    val spec = WeatherStyleResolver.resolveSpec(condition, phase, currentTheme, windSpeedValue)

    val context = LocalContext.current
    val isReducedMotion = remember {
        try { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f } catch (e: Exception) { false }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.96f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(600)) // Cinematic crossfade

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
            .border(1.5.dp, Color.White.copy(0.12f), RoundedCornerShape(32.dp))
    ) {
        // Background crossfade for cinematic transitions
        Crossfade(targetState = spec, animationSpec = tween(500), label = "hero_bg") { targetSpec ->
            LiveBackgroundLayer(spec = targetSpec)
        }

        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp))) {
            WeatherEffectLayer(spec = spec, isAnimationEnabled = !isReducedMotion)
        }

        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = spec.overlayAlpha), Color.Transparent, Color.Black.copy(alpha = spec.overlayAlpha * 0.5f))
            )
        ))

        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
            PremiumWeatherContent(
                cityName = cityName, districtName = districtName, conditionLabel = conditionLabel, temperature = temperature,
                feelsLike = feelsLike, humidity = humidity, windSpeed = windSpeed, uvIndex = uvIndex,
                spec = spec, unreadCount = unreadCount,
                onCityClick = onCityClick, onNotificationsClick = onNotificationsClick
            )
        }
    }
}

@Composable
fun LiveBackgroundLayer(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val move by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_move"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

        // Base gradient with depth: Cinematic vertical sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = spec.gradientColors,
                startY = 0f,
                endY = size.height
            )
        )

        // Atmosphere depth and volumetric lighting base
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color.White.copy(0.04f),
                1.0f to Color.Transparent,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                radius = size.width * 1.5f
            ),
            blendMode = BlendMode.Overlay
        )

        // Atmospheric scattering based on condition & phase
        val scatteringColor = when (spec.phase) {
            DayPhase.GOLDEN_HOUR -> Color(0xFFD35400)
            DayPhase.NIGHT -> Color(0xFF162B5E)
            DayPhase.DUSK -> Color(0xFF312E81)
            DayPhase.DAWN -> Color(0xFFF4A8C4)
            DayPhase.MORNING -> Color(0xFFD1E8FF)
            DayPhase.DAY -> if (spec.effectType == VisualEffectType.SUN) Color(0xFF54A0FF).copy(alpha = 0.6f) else Color.White
            else -> Color.White
        }

        drawCircle(
            brush = Brush.radialGradient(
                0.0f to scatteringColor.copy(0.08f),
                1.0f to Color.Transparent,
                center = sunCenter,
                radius = size.width * 1.2f
            ),
            center = sunCenter,
            radius = size.width * 1.2f,
            blendMode = BlendMode.Screen
        )
    }
}

@Composable
fun WeatherEffectLayer(spec: WeatherCardVisualSpec, isAnimationEnabled: Boolean) {
    if (!isAnimationEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        if (spec.phase == DayPhase.NIGHT && spec.effectType != VisualEffectType.RAIN && spec.effectType != VisualEffectType.THUNDER) {
            StarFieldEffect()
        }

        when (spec.effectType) {
            VisualEffectType.SUN -> PremiumSunEffect(spec)
            VisualEffectType.MOON -> PremiumMoonEffect(spec)
            VisualEffectType.RAIN -> RainEffect()
            VisualEffectType.THUNDER -> { RainEffect(); LightningEffect() }
            VisualEffectType.SNOW -> SnowParticleEffect()
            VisualEffectType.FOG -> FogHazeEffect()
            VisualEffectType.WIND -> PremiumWindEffect()
            else -> {}
        }

        if (spec.cloudDensity > 0) {
            CloudDriftEffect(count = spec.cloudDensity, color = spec.cloudColor)
        }
    }
}

@Composable
fun PremiumWindEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "wind")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "wind_drift"
    )
    val lines = remember { List(12) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        lines.forEach { line ->
            val x = ((line.x + drift) % 1f) * size.width
            val y = line.y * size.height
            val length = 80.dp.toPx()

            drawLine(
                Brush.horizontalGradient(listOf(Color.White.copy(0f), Color.White.copy(0.2f), Color.White.copy(0f))),
                Offset(x, y),
                Offset(x + length, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun PremiumSunEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun_fx")
    val energy by infiniteTransition.animateFloat(1f, 1.02f, infiniteRepeatable(tween(10000, easing = SineEaseInOut), RepeatMode.Reverse), label = "sun_energy")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(180000, easing = LinearEasing)), label = "sun_rot")

    val phase = spec.phase
    val sunOpacity = spec.sunOpacity

    val sunScale = when(phase) {
        DayPhase.DAWN -> 0.7f
        else -> 0.85f
    }

    val coreColors = when (phase) {
        DayPhase.GOLDEN_HOUR -> listOf(Color(0xFFF1C40F), Color(0xFFE67E22), Color(0xFFD35400))
        DayPhase.DAWN -> listOf(Color(0xFFFFD8B5), Color(0xFFF4A8C4), Color(0xFF8E6AE8))
        else -> listOf(Color(0xFFFFF9C4), Color(0xFFFDE047), Color(0xFFF1C40F))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 40.dp.toPx() * energy * sunScale

        // 1. Soft Atmospheric Aura
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[1].copy(alpha = 0.15f * sunOpacity),
                1.0f to Color.Transparent,
                center = center,
                radius = 240.dp.toPx()
            ),
            center = center,
            radius = 240.dp.toPx(),
            blendMode = BlendMode.Screen,
            alpha = sunOpacity
        )

        // 2. Volumetric Rays (Low opacity)
        drawContext.canvas.save()
        drawContext.canvas.rotate(rotation, center.x, center.y)
        repeat(8) { i ->
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(coreColors[0].copy(alpha = 0.05f * sunOpacity), Color.Transparent)
                ),
                topLeft = Offset(center.x - 1.dp.toPx(), center.y - 180.dp.toPx()),
                size = Size(2.dp.toPx(), 140.dp.toPx()),
                alpha = 0.3f * sunOpacity
            )
            drawContext.canvas.rotate(45f, center.x, center.y)
        }
        drawContext.canvas.restore()

        // 3. Sun Core
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to coreColors[0],
                0.5f to coreColors[1].copy(alpha = 0.8f),
                1.0f to Color.Transparent,
                center = center,
                radius = r
            ),
            center = center,
            radius = r,
            alpha = 0.9f * sunOpacity
        )
    }
}

@Composable
fun PremiumMoonEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val glow by infiniteTransition.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(8000, easing = SineEaseInOut), RepeatMode.Reverse), label = "glow")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 24.dp.toPx() // 70% of previous 36dp

        // 1. Nebula Ambient Halo
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF74B9FF).copy(0.12f * glow),
                1.0f to Color.Transparent,
                center = center,
                radius = 160.dp.toPx()
            ),
            center = center,
            radius = 160.dp.toPx(),
            blendMode = BlendMode.Screen
        )

        val path = Path().apply {
            addOval(Rect(center.x - r, center.y - r, center.x + r, center.y + r))
        }
        val clipPath = Path().apply {
            // Thinner crescent
            addOval(Rect(center.x - r * 1.8f, center.y - r * 1.4f, center.x + r * 0.2f, center.y + r * 0.8f))
        }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(path, Color(0xFFF1F2F6))
        drawContext.canvas.restore()
    }
}

val SineEaseInOut = Easing { f -> ((1 - Math.cos(f * Math.PI)) / 2).toFloat() }

fun DrawScope.drawCloud(center: Offset, scale: Float, color: Color) {
    val w = 150.dp.toPx() * scale
    val h = 80.dp.toPx() * scale
    val path = Path().apply {
        moveTo(center.x - w * 0.4f, center.y + h * 0.25f)
        cubicTo(center.x - w * 0.65f, center.y + h * 0.1f, center.x - w * 0.6f, center.y - h * 0.35f, center.x - w * 0.25f, center.y - h * 0.25f)
        cubicTo(center.x - w * 0.1f, center.y - h * 0.75f, center.x + w * 0.3f, center.y - h * 0.7f, center.x + w * 0.35f, center.y - h * 0.2f)
        cubicTo(center.x + w * 0.65f, center.y - h * 0.1f, center.x + w * 0.7f, center.y + h * 0.35f, center.x + w * 0.4f, center.y + h * 0.3f)
        close()
    }
    drawPath(path, color)
}

@Composable
fun CloudDriftEffect(count: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )

    // Layered clouds with lower opacity for Mostly Sunny/Cloudy depth
    val clouds = remember(count) {
        List(count) { i ->
            CloudState(
                x = (i.toFloat() / count) + (Random.nextFloat() * 0.15f),
                y = (i % 3) * 0.15f + 0.05f + (Random.nextFloat() * 0.1f),
                scale = 1.0f + Random.nextFloat() * 1.5f,
                speed = 0.15f + Random.nextFloat() * 0.3f,
                opacity = if (i % 2 == 0) 0.15f else 0.10f // Opaklık %10-20 aralığında
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val cw = 220.dp.toPx() * cloud.scale
            val totalSpan = size.width + cw * 2
            var xPos = (cloud.x + drift * cloud.speed) % 1f
            val x = xPos * totalSpan - cw

            drawCloud(Offset(x, size.height * cloud.y), cloud.scale, color.copy(cloud.opacity))
        }
    }
}

private data class CloudState(val x: Float, val y: Float, val scale: Float, val speed: Float, val opacity: Float)

@Composable
fun RainEffect() {
    val ry by rememberInfiniteTransition(label = "rain").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "rain_fall"
    )
    val p = remember { List(100) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            val x = dr.x * size.width
            val y = (dr.y * size.height + ry) % size.height

            // Meteorological rain: sharper, subtle and premium
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(0f), Color.White.copy(0.18f), Color.White.copy(0f))
                ),
                start = Offset(x, y),
                end = Offset(x - 1.5f, y + 25f),
                strokeWidth = 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun LightningEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")
    val a by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 10000
                0f at 0
                0f at 7000
                0.25f at 7050
                0f at 7100
                0.4f at 7150
                0f at 7300
            }
        ),
        label = "lightning_alpha"
    )

    // Indigo-Purple dramatic but chic atmospheric tint
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4F46E5).copy(alpha = a * 0.15f)))
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(a * 0.35f)))
}

@Composable
fun SnowParticleEffect() {
    val sy by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(8000, easing = LinearEasing)))
    val p = remember { List(50) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            val x = dr.x * size.width + (Math.sin(sy.toDouble() / 150 + dr.x * 20) * 40).toFloat()
            val y = (dr.y * size.height + sy) % size.height

            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color.White.copy(0.7f),
                    1.0f to Color.Transparent,
                    center = Offset(x, y),
                    radius = 2.5.dp.toPx()
                ),
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun StarFieldEffect() {
    val tw by rememberInfiniteTransition().animateFloat(0.3f, 1f, infiniteRepeatable(tween(3500), RepeatMode.Reverse))
    val stars = remember { List(50) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, s ->
            val starAlpha = 0.4f * (if (i % 3 == 0) tw else 1.2f - tw)
            drawCircle(
                Color.White.copy(starAlpha.coerceIn(0.1f, 0.6f)),
                (0.8f + Random.nextFloat() * 0.4f).dp.toPx(),
                Offset(s.x * size.width, s.y * size.height)
            )
        }
    }
}

@Composable
fun FogHazeEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val d by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "fog_drift"
    )
    val op by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "fog_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Soft rolling fog layers - Silvery and premium
        repeat(3) { i ->
            val xBase = -300f + d * (i + 1) * 0.2f
            val yPos = size.height * (0.15f + i * 0.25f)
            val h = (100.dp + (60.dp * i)).toPx()

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFFCBD5E1).copy(alpha = op * (3 - i) / 3f),
                        Color.Transparent
                    )
                ),
                Offset(xBase, yPos),
                Size(size.width + 600f, h),
                blendMode = BlendMode.Screen
            )
        }
    }
}

@Composable
fun PremiumWeatherContent(
    cityName: String, districtName: String?, conditionLabel: String, temperature: String, feelsLike: String, humidity: String, windSpeed: String, uvIndex: String,
    spec: WeatherCardVisualSpec, unreadCount: Int, onCityClick: () -> Unit, onNotificationsClick: () -> Unit
) {
    val textColor = spec.textColor
    val secondaryColor = textColor.copy(alpha = 0.7f)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .background(if (spec.isDark) Color.White.copy(0.12f) else Color.Black.copy(0.06f))
                .clickable { onCityClick() }
                .padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = spec.accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text((districtName ?: cityName).uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = textColor))
                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = secondaryColor, modifier = Modifier.size(16.dp))
                    }
                    Text("Konumu değiştir", style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryColor.copy(0.5f)))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumNotificationButton(unreadCount, spec.isDark, textColor, onNotificationsClick)
                Spacer(Modifier.width(12.dp))
                Icon(spec.mainIcon, null, tint = spec.accentColor, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.weight(0.5f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(temperature, style = MaterialTheme.typography.displayLarge.copy(fontSize = 110.sp, fontWeight = FontWeight.W200, letterSpacing = (-4).sp, color = textColor))
            Text(conditionLabel.uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = textColor.copy(0.9f)))
            Text("HİSSEDİLEN $feelsLike", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = secondaryColor))
        }
        Spacer(Modifier.weight(1f))
        Surface(modifier = Modifier.fillMaxWidth().height(64.dp),
            color = if (spec.isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.5.dp, if (spec.isDark) Color.White.copy(0.15f) else Color.Black.copy(0.08f))) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                listOf(Icons.Rounded.WaterDrop to humidity, Icons.Rounded.Air to windSpeed, Icons.Rounded.WbSunny to "UV $uvIndex").forEach { (icon, valStr) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = textColor.copy(0.6f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(valStr, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumNotificationButton(unreadCount: Int, isDark: Boolean, tint: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp).zIndex(120f).clip(CircleShape).background(Brush.verticalGradient(if (isDark) listOf(Color.White.copy(0.25f), Color.White.copy(0.1f)) else listOf(Color.Black.copy(0.12f), Color.Black.copy(0.05f)))).border(1.dp, if (isDark) Color.White.copy(0.3f) else Color.Black.copy(0.15f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Notifications, null, tint = tint, modifier = Modifier.size(22.dp))
        if (unreadCount > 0) Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(Color(0xFF3B82F6)).border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape))
    }
}
