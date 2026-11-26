package com.havamania

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherHeroContrastTest {
    @Test fun daytimeFunctionalIconsContrastWithEveryPaletteStop() {
        for (code in listOf(0, 1, 2, 45, 71)) {
            val spec = WeatherHeroStyleManager.resolveWeatherHeroTheme(code, "", null, 41.0, 29.0, null, null, true)
            val foreground = heroForegroundIconColor(spec).luminance()
            for (background in spec.gradientColors) {
                val luminance = background.luminance()
                val ratio = (maxOf(foreground, luminance) + 0.05f) / (minOf(foreground, luminance) + 0.05f)
                assertTrue("code=$code ratio=$ratio", ratio >= 3f)
            }
        }
    }
}
