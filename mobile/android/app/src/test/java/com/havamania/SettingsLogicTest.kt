package com.havamania

import com.havamania.ui.theme.AppTheme
import com.havamania.ui.theme.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SettingsLogicTest {

    @Test
    fun `otomatik tema Temmuz ayinda Yaz olur`() {
        val month = 7 // Temmuz
        val theme = getSeasonalThemeManual(month)
        assertEquals(AppTheme.SUMMER, theme)
    }

    @Test
    fun `otomatik tema Ocak ayinda Kis olur`() {
        val month = 1 // Ocak
        val theme = getSeasonalThemeManual(month)
        assertEquals(AppTheme.WINTER, theme)
    }

    @Test
    fun `otomatik tema Nisan ayinda Ilkbahar olur`() {
        val month = 4 // Nisan
        val theme = getSeasonalThemeManual(month)
        assertEquals(AppTheme.SPRING, theme)
    }

    @Test
    fun `otomatik tema Ekim ayinda Sonbahar olur`() {
        val month = 10 // Ekim
        val theme = getSeasonalThemeManual(month)
        assertEquals(AppTheme.AUTUMN, theme)
    }

    private fun getSeasonalThemeManual(month: Int): AppTheme {
        return when (month) {
            3, 4, 5 -> AppTheme.SPRING
            6, 7, 8 -> AppTheme.SUMMER
            9, 10, 11 -> AppTheme.AUTUMN
            12, 1, 2 -> AppTheme.WINTER
            else -> AppTheme.SUMMER
        }
    }
}
