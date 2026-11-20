package com.havamania.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal data class ThemeButtonPalette(val stops: List<Color>, val content: Color)

internal fun themeGradientSamples(stops: List<Color>): List<Color> {
    require(stops.isNotEmpty())
    if (stops.size == 1) return stops
    return stops.zipWithNext().flatMap { (first, last) -> (0..20).map { lerp(first, last, it / 20f) } }
}

/** One foreground for the complete gradient; tone the gradient only when neither foreground is readable. */
internal fun themeButtonPalette(stops: List<Color>, preferred: Color): ThemeButtonPalette {
    val samples = themeGradientSamples(stops)
    fun minimum(text: Color, colors: List<Color>) = colors.minOf { themeColorContrast(it, text) }
    if (minimum(preferred, samples) >= 4.5f) return ThemeButtonPalette(stops, preferred)
    val best = listOf(Color.Black, Color.White).maxBy { minimum(it, samples) }
    if (minimum(best, samples) >= 4.5f) return ThemeButtonPalette(stops, best)
    val text = if (themeColorContrast(Color.Black, preferred) >= themeColorContrast(Color.White, preferred)) Color.White else Color.Black
    val target = if (text == Color.White) Color.Black else Color.White
    for (step in 1..20) {
        val adjusted = stops.map { lerp(it, target, step / 20f) }
        if (minimum(text, themeGradientSamples(adjusted)) >= 4.5f) return ThemeButtonPalette(adjusted, text)
    }
    return ThemeButtonPalette(List(stops.size) { target }, text)
}