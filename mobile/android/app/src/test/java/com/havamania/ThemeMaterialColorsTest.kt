package com.havamania

import com.havamania.ui.theme.*

import org.junit.Assert.*
import org.junit.Test

class ThemeMaterialColorsTest {
    private val themes = AppTheme.values().filter { it != AppTheme.AUTO }
    @Test fun explicitThemesUseTheirOwnPaletteAndSurfaceRoles() {
        for (theme in themes) {
            val colors = ThemeFactory.createColors(theme)
            val scheme = ThemeManager.getColorScheme(theme)
            assertEquals(themeActionColor(colors.accent, colors.background), scheme.primary)
            assertEquals(colors.background, scheme.background)
            assertEquals(colors.surface, scheme.surface)
            assertEquals(colors.textPrimary, scheme.onSecondaryContainer)
            assertEquals(scheme.secondaryContainer, scheme.primaryContainer)
            assertEquals(scheme.secondaryContainer, scheme.tertiaryContainer)
        }
    }
    @Test fun allFilledAndTonalButtonPairsMeetTheProjectContrastTarget() {
        for (theme in themes) {
            val s = ThemeManager.getColorScheme(theme)
            for ((fill, text) in listOf(s.primary to s.onPrimary, s.secondary to s.onSecondary,
                s.tertiary to s.onTertiary, s.error to s.onError,
                s.primaryContainer to s.onPrimaryContainer, s.secondaryContainer to s.onSecondaryContainer,
                s.inverseSurface to s.inverseOnSurface, s.inverseSurface to s.inversePrimary)) {
                assertTrue("$theme unreadable fill/text pair", themeColorContrast(fill, text) >= 4.5f)
            }
        }
    }
    @Test fun textActionsMeetTheProjectContrastTargetOnEachPageBackground() {
        for (theme in themes) {
            val s = ThemeManager.getColorScheme(theme)
            for (action in listOf(s.primary, s.secondary, s.tertiary, s.error))
                assertTrue("$theme text action", themeColorContrast(action, s.background) >= 4.5f)
        }
    }
    @Test fun alreadyReadableAccentIsNotChanged() {
        for (theme in themes) {
            val colors = ThemeFactory.createColors(theme)
            if (themeColorContrast(colors.accent, colors.background) >= 4.5f)
                assertEquals(colors.accent, ThemeManager.getColorScheme(theme).primary)
        }
    }
}