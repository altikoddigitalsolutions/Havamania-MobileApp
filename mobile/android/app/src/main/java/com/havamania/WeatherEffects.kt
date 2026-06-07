package com.havamania

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.HavamaniaTheme
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Havamania AAA Premium Weather Effects Engine
 */

@Composable
fun WeatherEffectLayer(
    spec: WeatherCardVisualSpec,
    isAnimationEnabled: Boolean = true
) {
    if (!isAnimationEnabled) return

    Box(modifier = Modifier.fillMaxSize()) {
        // NIGHT AMBIENCE (Stars)
        if (spec.phase in listOf(DayPhase.NIGHT, DayPhase.TWILIGHT, DayPhase.BLUE_HOUR, DayPhase.DUSK, DayPhase.EVENING)) {
            StarFieldEffect(spec.cloudDensity)
        }

        // WEATHER PARTICLES & PHENOMENA
        when (spec.effectType) {
            VisualEffectType.SUN -> if (spec.cloudDensity < 8) PremiumSunEffect(spec)
            VisualEffectType.MOON -> PremiumMoonEffect(spec)
            VisualEffectType.RAIN -> RainEffect(spec.rainIntensity)
            VisualEffectType.THUNDER -> {
                RainEffect(spec.rainIntensity * 1.5f)
                ThunderEffect()
            }
            VisualEffectType.SNOW -> SnowParticleEffect()
            VisualEffectType.FOG -> FogEffect()
            else -> {}
        }

        // CLOUD SYSTEM
        if (spec.cloudDensity > 0) {
            CloudDriftEffect(count = spec.cloudDensity, isNight = spec.isDark)
        }
    }
}

@Composable
fun PremiumSunEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(8000, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)

        // 1. Large Atmospheric Diffusion
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to spec.accentColor.copy(0.05f),
                1.0f to Color.Transparent,
                center = center,
                radius = 260.dp.toPx()
            ),
            center = center,
            radius = 260.dp.toPx(),
            blendMode = BlendMode.Screen
        )

        // 2. Soft Halo
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to spec.accentColor.copy(0.12f),
                1.0f to Color.Transparent,
                center = center,
                radius = 110.dp.toPx() * pulse
            ),
            center = center,
            radius = 110.dp.toPx() * pulse
        )

        // 3. Sun Core
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFEFCE8),
                0.5f to spec.accentColor.copy(0.8f),
                1.0f to Color.Transparent,
                center = center,
                radius = 22.dp.toPx()
            ),
            center = center,
            radius = 22.dp.toPx(),
            alpha = 0.85f * spec.sunOpacity
        )
    }
}

@Composable
fun PremiumMoonEffect(spec: WeatherCardVisualSpec) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val pulse by infiniteTransition.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(12000, easing = SineEaseInOut), RepeatMode.Reverse))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * spec.sunMoonPosition.x, size.height * spec.sunMoonPosition.y)
        val r = 22.dp.toPx()

        // Moonlight Diffusion
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF94A3B8).copy(0.04f),
                1.0f to Color.Transparent,
                center = center,
                radius = 150.dp.toPx() * pulse
            ),
            center = center,
            radius = 150.dp.toPx() * pulse,
            blendMode = BlendMode.Screen
        )

        // Moon Shape
        val path = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center.x - r, center.y - r, center.x + r, center.y + r)) }
        val clipPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center.x - r * 1.5f, center.y - r * 1.2f, center.x + r * 0.4f, center.y + r * 0.8f)) }

        drawContext.canvas.save()
        drawContext.canvas.clipPath(clipPath, ClipOp.Difference)
        drawPath(path, brush = Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8))), alpha = 0.85f)
        drawContext.canvas.restore()
    }
}

@Composable
fun ThunderEffect() {
    var flashAlpha by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(5000, 15000))
            flashAlpha = 0.25f
            delay(50)
            flashAlpha = 0f
            delay(100)
            flashAlpha = 0.4f
            delay(80)
            flashAlpha = 0f
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFBBDEFB).copy(alpha = flashAlpha)))
}

@Composable
fun RainEffect(intensity: Float) {
    val ry by rememberInfiniteTransition().animateFloat(0f, 2000f, infiniteRepeatable(tween((1200 / intensity).toInt(), easing = LinearEasing)))
    val drops = remember { List(60) { Offset(Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drops.forEach { d ->
            val x = d.x * size.width
            val y = (d.y * size.height + ry) % size.height
            drawLine(
                brush = Brush.verticalGradient(listOf(Color.White.copy(0f), Color.White.copy(0.25f), Color.White.copy(0f))),
                start = Offset(x, y), end = Offset(x - 2f, y + 45f), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SnowParticleEffect() {
    val sy by rememberInfiniteTransition().animateFloat(0f, 1000f, infiniteRepeatable(tween(12000, easing = LinearEasing)))
    val p = remember { List(40) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        p.forEach { dr ->
            val x = dr.x * size.width + (Math.sin(sy.toDouble() / 200 + dr.x * 20) * 30).toFloat()
            val y = (dr.y * size.height + sy) % size.height
            drawCircle(Color.White.copy(0.5f), radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun StarFieldEffect(cloudDensity: Int) {
    val starCount = (45 - (cloudDensity * 5)).coerceAtLeast(5)
    val tw by rememberInfiniteTransition().animateFloat(0.3f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse))
    val stars = remember(starCount) { List(starCount) { StarData(Offset(Random.nextFloat(), Random.nextFloat() * 0.8f), 0.5f + Random.nextFloat() * 0.8f, 0.1f + Random.nextFloat() * 0.3f) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { i, s ->
            val alpha = s.alphaBase * (if (i % 4 == 0) tw else 1f)
            drawCircle(Color.White.copy(alpha.coerceIn(0f, 0.4f)), s.size.dp.toPx(), Offset(s.pos.x * size.width, s.pos.y * size.height))
        }
    }
}

@Composable
fun FogEffect() {
    val drift by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(30000, easing = LinearEasing)))
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(3) { i ->
            val y = size.height * (0.2f + i * 0.25f)
            val h = 100.dp.toPx()
            val xOff = ((drift + i * 0.3f) % 1f) * size.width
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFCBD5E1).copy(0.2f), Color.Transparent), startX = xOff - 400f, endX = xOff + 400f),
                topLeft = Offset(0f, y), size = Size(size.width, h)
            )
        }
    }
}

@Composable
fun CloudDriftEffect(count: Int, isNight: Boolean) {
    val cloudColor = if (isNight) Color(0xFF334155) else Color(0xFFE2E8F0)
    val drift by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(180000, easing = LinearEasing)))
    val clouds = remember(count) {
        List(count) { i ->
            val layer = i % 3
            CloudState(
                x = (i.toFloat() / count) + (Random.nextFloat() * 0.4f),
                y = when(layer) { 0 -> 0.05f; 1 -> 0.2f; else -> 0.45f } + Random.nextFloat() * 0.15f,
                scale = when(layer) { 0 -> 2.5f; 1 -> 1.5f; else -> 1.0f },
                opacity = when(layer) { 0 -> 0.08f; 1 -> 0.14f; else -> 0.22f },
                duration = when(layer) { 0 -> 200000; 1 -> 150000; else -> 100000 }
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val infiniteTransition = rememberInfiniteTransition()
            val float by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(40000, easing = SineEaseInOut), RepeatMode.Reverse))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = 260.dp.toPx() * cloud.scale
                val totalSpan = size.width + cw * 2
                val x = ((cloud.x + drift) % 1f) * totalSpan - cw
                val yOsc = (float - 0.5f) * 20.dp.toPx()
                drawCloudMass(Offset(x, size.height * cloud.y + yOsc), cloud.scale, cloudColor, cloud.opacity)
            }
        }
    }
}

fun DrawScope.drawCloudMass(center: Offset, scale: Float, color: Color, opacity: Float) {
    val baseRadius = 85.dp.toPx() * scale
    val puffs = listOf(Offset(0f, 0f) to 1.0f, Offset(-baseRadius * 0.4f, baseRadius * 0.1f) to 0.8f, Offset(baseRadius * 0.4f, -baseRadius * 0.05f) to 0.85f, Offset(-baseRadius * 0.2f, -baseRadius * 0.2f) to 0.7f)
    puffs.forEach { (offset, pScale) ->
        val r = baseRadius * pScale
        drawCircle(brush = Brush.radialGradient(0.0f to color.copy(opacity), 0.65f to color.copy(opacity * 0.5f), 1.0f to Color.Transparent, center = center + offset, radius = r), center = center + offset, radius = r)
    }
}

private data class CloudState(val x: Float, val y: Float, val scale: Float, val opacity: Float, val duration: Int)
private data class StarData(val pos: Offset, val size: Float, val alphaBase: Float)
val SineEaseInOut = Easing { f -> ((1 - Math.cos(f * Math.PI)) / 2).toFloat() }
