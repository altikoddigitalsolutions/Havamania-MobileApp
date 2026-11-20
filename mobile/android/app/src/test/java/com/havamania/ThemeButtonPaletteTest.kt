package com.havamania

import androidx.compose.ui.graphics.Color
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class ThemeButtonPaletteTest {
    @Test fun everyRealThemeGradientHasOneReadableForegroundAcrossItsSamples() {
        for (theme in AppTheme.values().filter { it != AppTheme.AUTO }) {
            val c = ThemeFactory.createColors(theme)
            val palette = themeButtonPalette(c.buttonGradient ?: listOf(c.accent), c.onAccent)
            for (sample in themeGradientSamples(palette.stops))
                assertTrue("$theme unreadable gradient", themeColorContrast(sample, palette.content) >= 4.5f)
        }
    }
    @Test fun readablePreferredTextKeepsTheOriginalGradient() {
        val stops = listOf(Color(0xFF020617), Color(0xFF0F172A))
        val palette = themeButtonPalette(stops, Color.White)
        assertEquals(stops, palette.stops)
        assertEquals(Color.White, palette.content)
    }
    @Test fun winterGradientIsTonedWhenNeitherOriginalForegroundMeetsTheTarget() {
        val colors = ThemeFactory.createColors(AppTheme.WINTER)
        val palette = themeButtonPalette(colors.buttonGradient!!, colors.onAccent)
        assertNotEquals(colors.buttonGradient, palette.stops)
        assertEquals(Color.White, palette.content)
    }
}