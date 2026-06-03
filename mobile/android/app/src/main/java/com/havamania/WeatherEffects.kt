package com.havamania

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Havamania Canlı Hava Sistemi - Core Engine
 */

enum class WeatherEffectIntensity { OFF, LOW, MEDIUM }

@Composable
fun WeatherEffectLayer(
    condition: WeatherCondition,
    phase: DayPhase,
    intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM,
    weatherCode: Int = 0,
    isAnimationEnabled: Boolean = true,
    parallaxOffset: Float = 0f
) {
    if (intensity == WeatherEffectIntensity.OFF) return

    val theme = HavamaniaTheme.colors
    val baseOpacity = if (theme.isDark) 0.65f else 0.45f

    // Reduce opacity in LOW intensity
    val effectiveOpacity = if (intensity == WeatherEffectIntensity.LOW) baseOpacity * 0.5f else baseOpacity

    Box(modifier = Modifier.fillMaxSize()) {
        // Atmospheric Transitions Layer
        AnimatedVisibility(
            visible = condition is WeatherCondition.Rain || condition is WeatherCondition.Thunderstorm,
            enter = fadeIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(1000))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
        }

        when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> {
                if (phase == DayPhase.NIGHT || phase == DayPhase.DUSK || phase == DayPhase.EVENING) {
                    StarFieldEffect(
                        modifier = Modifier.fillMaxSize(),
                        isAnimationEnabled = isAnimationEnabled,
                        intensity = intensity,
                        parallaxOffset = parallaxOffset
                    )
                }
            }
            is WeatherCondition.MostlySunny -> {
                CloudHazeEffect(effectiveOpacity * 0.25f, intensity = intensity)
            }
            is WeatherCondition.Cloudy -> {
                CloudHazeEffect(effectiveOpacity * 0.8f, intensity = intensity)
            }
            is WeatherCondition.Overcast -> {
                CloudHazeEffect(effectiveOpacity * 1.1f, intensity = intensity)
            }
            is WeatherCondition.PartlyCloudy -> {
                CloudHazeEffect(effectiveOpacity * 0.45f, intensity = intensity)
            }
            is WeatherCondition.Rain -> {
                LayeredRainEffect(effectiveOpacity, intensity = intensity, isAnimationEnabled = isAnimationEnabled)
            }
            is WeatherCondition.Thunderstorm -> {
                LayeredRainEffect(effectiveOpacity * 1.3f, intensity = intensity, isAnimationEnabled = isAnimationEnabled)
                if (intensity != WeatherEffectIntensity.LOW) {
                    ThunderEffect(effectiveOpacity, isAnimationEnabled = isAnimationEnabled)
                }
            }
            is WeatherCondition.Snow -> {
                SnowEffect(effectiveOpacity, intensity = intensity)
            }
            is WeatherCondition.Fog -> {
                FogEffect(effectiveOpacity)
            }
            else -> {}
        }
    }
}

// ── STAR FIELD EFFECT ────────────────────────────────────────────────────────
data class Star(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinkleDelay: Int
)

@Composable
fun StarFieldEffect(
    modifier: Modifier = Modifier,
    isAnimationEnabled: Boolean = true,
    intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM,
    parallaxOffset: Float = 0f
) {
    val starCount = if (intensity == WeatherEffectIntensity.LOW) 20 else 45

    // Generate star positions once and remember them
    val stars = remember(starCount) {
        List(starCount) {
            Star(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = 0.4f + Random.nextFloat() * 0.8f, // Smaller stars
                baseAlpha = 0.1f + Random.nextFloat() * 0.3f, // Lower opacity
                twinkleDelay = Random.nextInt(2000, 8000)
            )
        }
    }

    if (isAnimationEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "stars")
        val twinkle by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle"
        )

        Canvas(modifier = modifier) {
            // Nebula/Galaxy Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF74B9FF).copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                    radius = size.width * 0.6f
                ),
                blendMode = BlendMode.Screen
            )

            stars.forEachIndexed { index, star ->
                val alpha = (star.baseAlpha * if (index % 3 == 0) twinkle else 1f).coerceIn(0.05f, 0.7f)
                val x = (star.xRatio * size.width + parallaxOffset * (0.8f + index % 5 * 0.2f)) % size.width

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.radius.dp.toPx(),
                    center = Offset(x, star.yRatio * size.height)
                )
            }
        }
    }
}

// ── LAYERED RAIN EFFECT ───────────────────────────────────────────────────────
@Composable
fun LayeredRainEffect(
    opacity: Float,
    intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM,
    isAnimationEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain_layers")

    val t1 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(2500, easing = LinearEasing)), "l1")
    val t2 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing)), "l2")
    val t3 by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), "l3")

    // Layer 1: Fine drops
    val l1Drops = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    // Layer 2: Medium drops
    val l2Drops = remember { List(25) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    // Layer 3: Blurred distant drops
    val l3Drops = remember { List(15) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Layer 3: Distant
        l3Drops.forEach { d ->
            val y = (d.y * size.height + t3 * size.height) % size.height
            drawLine(
                Color.White.copy(0.08f * opacity),
                Offset(d.x * size.width, y),
                Offset(d.x * size.width + 1f, y + 25f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }

        // Layer 2: Medium
        l2Drops.forEach { d ->
            val y = (d.y * size.height + t2 * size.height) % size.height
            drawLine(
                Color.White.copy(0.12f * opacity),
                Offset(d.x * size.width, y),
                Offset(d.x * size.width + 2f, y + 20f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round
            )
        }

        // Layer 1: Fine
        l1Drops.forEach { d ->
            val y = (d.y * size.height + t1 * size.height) % size.height
            drawLine(
                Color.White.copy(0.18f * opacity),
                Offset(d.x * size.width, y),
                Offset(d.x * size.width + 3f, y + 15f),
                strokeWidth = 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ── SNOW EFFECT ──────────────────────────────────────────────────────────────
@Composable
fun SnowEffect(opacity: Float, intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val particleCount = if (intensity == WeatherEffectIntensity.LOW) 20 else 40
    val particles = remember(particleCount) {
        List(particleCount) {
            SnowParticle(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                size = 2f + Random.nextFloat() * 3f,
                drift = (Random.nextFloat() - 0.5f) * 100f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = ((progress + p.yOffset) % 1f) * size.height
            val driftOffset = Math.sin(progress * Math.PI * 4 + p.yOffset * 10).toFloat() * p.drift
            val x = (p.x * size.width + driftOffset) % size.width

            drawCircle(
                color = Color.White.copy(alpha = 0.8f * opacity),
                radius = p.size.dp.toPx() / 2,
                center = Offset(x, y)
            )
        }
    }
}

private data class SnowParticle(val x: Float, val yOffset: Float, val size: Float, val drift: Float)

// ── SUN GLOW EFFECT ──────────────────────────────────────────────────────────
@Composable
fun SunGlowEffect(opacity: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * 0.85f, size.height * 0.15f)
        val radius = size.minDimension * 0.45f * pulse

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f * opacity), Color.Transparent),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
    }
}

// ── FOG EFFECT ───────────────────────────────────────────────────────────────
@Composable
fun FogEffect(opacity: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "fog")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(opacity * 0.4f)
            .blur(30.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val xOffset = drift * size.width
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0f)),
                    startX = -size.width + xOffset,
                    endX = xOffset
                )
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0f)),
                    startX = xOffset,
                    endX = size.width + xOffset
                )
            )
        }
    }
}

// ── CLOUD HAZE EFFECT ───────────────────────────────────────────────────────
@Composable
fun CloudHazeEffect(opacity: Float, intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM) {
    val infiniteTransition = rememberInfiniteTransition(label = "haze")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing), // Very slow drift
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    val wave by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val xOffset = drift * size.width

        // Subtle top-down density haze
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = opacity * 0.1f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.8f
            )
        )

        val count = if (intensity == WeatherEffectIntensity.LOW) 2 else 5
        repeat(count) { i ->
            val xBase = (i * size.width * 0.3f + xOffset + (wave * (i+1) * 0.5f)) % size.width
            val yBase = size.height * (0.15f + i * 0.12f)
            val radius = (160.dp + (50.dp * i)).toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = opacity * (0.15f - i * 0.02f)),
                        Color.Transparent
                    ),
                    center = Offset(xBase, yBase),
                    radius = radius
                ),
                center = Offset(xBase, yBase),
                radius = radius
            )
        }
    }
}

// ── THUNDER EFFECT ───────────────────────────────────────────────────────────
@Composable
fun ThunderEffect(opacity: Float, isAnimationEnabled: Boolean = true) {
    if (!isAnimationEnabled) return

    var flashAlpha by remember { mutableStateOf(0f) }
    var flashCenter by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        while (true) {
            // Rarer interval for premium feel
            delay(Random.nextLong(8000, 20000))

            val intensity = Random.nextFloat() * 0.25f + 0.1f
            flashCenter = Offset(Random.nextFloat(), 0.2f) // Top area flash

            // Subtle double flash pattern
            flashAlpha = intensity
            delay(60)
            flashAlpha = 0f
            delay(100)
            flashAlpha = intensity * 1.4f
            delay(80)
            flashAlpha = 0f
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (flashAlpha > 0f) {
            // 1. Global subtle tint (not pure white)
            drawRect(
                color = Color(0xFFBBDEFB).copy(alpha = flashAlpha * opacity * 0.3f),
                blendMode = BlendMode.Screen
            )

            // 2. Localized atmospheric burst
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = flashAlpha * opacity * 0.8f),
                        Color.Transparent
                    ),
                    center = Offset(flashCenter.x * size.width, flashCenter.y * size.height),
                    radius = size.width * 0.8f
                ),
                blendMode = BlendMode.Overlay
            )
        }
    }
}
