package com.havamania

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
    timeOfDay: TimeOfDay,
    intensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM,
    weatherCode: Int = 0,
    isAnimationEnabled: Boolean = true,
    parallaxOffset: Float = 0f
) {
    if (intensity == WeatherEffectIntensity.OFF) return

    val theme = HavamaniaTheme.colors
    val baseOpacity = if (theme.isDark) 0.7f else 0.4f

    // Reduce opacity in LOW intensity
    val effectiveOpacity = if (intensity == WeatherEffectIntensity.LOW) baseOpacity * 0.6f else baseOpacity

    Box(modifier = Modifier.fillMaxSize()) {
        when (condition) {
            is WeatherCondition.Clear, is WeatherCondition.NightClear -> {
                if (timeOfDay == TimeOfDay.NIGHT) {
                    StarFieldEffect(
                        modifier = Modifier.fillMaxSize(),
                        isAnimationEnabled = isAnimationEnabled,
                        intensity = intensity,
                        parallaxOffset = parallaxOffset
                    )
                } else {
                    SunGlowEffect(effectiveOpacity)
                }
            }
            is WeatherCondition.MostlySunny -> {
                SunGlowEffect(effectiveOpacity * 0.9f)
                CloudHazeEffect(effectiveOpacity * 0.3f, intensity = intensity)
            }
            is WeatherCondition.Cloudy -> {
                CloudHazeEffect(effectiveOpacity, intensity = intensity)
            }
            is WeatherCondition.PartlyCloudy -> {
                CloudHazeEffect(effectiveOpacity * 0.6f, intensity = intensity)
                if (timeOfDay != TimeOfDay.NIGHT) {
                    SunGlowEffect(effectiveOpacity * 0.7f)
                }
            }
            is WeatherCondition.Rain -> {
                val rainIntensity = when (weatherCode) {
                    51, 53, 55 -> RainIntensity.DRIZZLE
                    80, 81, 82 -> RainIntensity.RAIN
                    else -> RainIntensity.RAIN
                }
                RainEffect(effectiveOpacity, intensity = rainIntensity, visualIntensity = intensity, isAnimationEnabled = isAnimationEnabled)
            }
            is WeatherCondition.Thunderstorm -> {
                RainEffect(effectiveOpacity * 1.2f, intensity = RainIntensity.HEAVY, visualIntensity = intensity, isAnimationEnabled = isAnimationEnabled)
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
    val starCount = if (intensity == WeatherEffectIntensity.LOW) 10 else 25

    // Generate star positions once and remember them
    val stars = remember(starCount) {
        List(starCount) {
            Star(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = 0.5f + Random.nextFloat() * 1.0f, // Smaller stars
                baseAlpha = 0.15f + Random.nextFloat() * 0.4f, // Lower opacity
                twinkleDelay = Random.nextInt(3000, 7000)
            )
        }
    }

    if (isAnimationEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "stars")
        val twinkle by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle"
        )

        Canvas(modifier = modifier) {
            stars.forEachIndexed { index, star ->
                val alpha = (star.baseAlpha * if (index % 4 == 0) twinkle else 1f).coerceIn(0.1f, 0.8f)
                val x = (star.xRatio * size.width + parallaxOffset * (1f + index % 3 * 0.5f)) % size.width

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = star.radius.dp.toPx(),
                    center = Offset(x, star.yRatio * size.height)
                )
            }
        }
    }
else {
        Canvas(modifier = modifier) {
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.baseAlpha),
                    radius = star.radius.dp.toPx(),
                    center = Offset(star.xRatio * size.width, star.yRatio * size.height)
                )
            }
        }
    }
}

// ── RAIN EFFECT ──────────────────────────────────────────────────────────────
data class RainDrop(
    val xRatio: Float,
    val yRatio: Float,
    val speed: Float,
    val length: Float,
    val alpha: Float
)

enum class RainIntensity { DRIZZLE, RAIN, HEAVY }

@Composable
fun RainEffect(
    opacity: Float,
    intensity: RainIntensity = RainIntensity.RAIN,
    visualIntensity: WeatherEffectIntensity = WeatherEffectIntensity.MEDIUM,
    isAnimationEnabled: Boolean = true
) {
    val dropCount = when (intensity) {
        RainIntensity.DRIZZLE -> 15
        RainIntensity.RAIN -> 30
        RainIntensity.HEAVY -> 50
    }.let { if (visualIntensity == WeatherEffectIntensity.LOW) it / 2 else it }

    // Generate rain drops once and remember them
    val drops = remember(dropCount) {
        List(dropCount) {
            RainDrop(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                speed = 0.4f + Random.nextFloat() * 0.8f, // Slower for premium feel
                length = 8f + Random.nextFloat() * 12f,  // More particle-like
                alpha = 0.08f + Random.nextFloat() * 0.15f
            )
        }
    }

    if (isAnimationEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "rain")
        val animationOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3500, easing = LinearEasing), // Slower, more elegant
                repeatMode = RepeatMode.Restart
            ),
            label = "offset"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drops.forEach { drop ->
                val x = size.width * drop.xRatio
                val y = (size.height * drop.yRatio + animationOffset * size.height * drop.speed) % size.height

                // Draw as soft particles/lines
                drawCircle(
                    color = Color.White.copy(alpha = drop.alpha * opacity),
                    radius = 0.8.dp.toPx(),
                    center = Offset(x, y)
                )

                drawLine(
                    color = Color.White.copy(alpha = drop.alpha * opacity * 0.5f),
                    start = Offset(x, y),
                    end = Offset(x + 2f, y + drop.length),
                    strokeWidth = 1f
                )
            }
        }
    }
else {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drops.forEach { drop ->
                val x = size.width * drop.xRatio
                val y = size.height * drop.yRatio
                drawLine(
                    color = Color.White.copy(alpha = drop.alpha * opacity),
                    start = Offset(x, y),
                    end = Offset(x + 4f, y + drop.length),
                    strokeWidth = 1.2f
                )
            }
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
