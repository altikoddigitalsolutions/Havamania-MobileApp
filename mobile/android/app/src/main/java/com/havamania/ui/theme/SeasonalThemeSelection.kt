package com.havamania.ui.theme

import java.time.LocalDate

internal fun resolveThemeSelection(selection: AppTheme, date: LocalDate): AppTheme =
    if (selection == AppTheme.AUTO) ThemeManager.getSeasonalTheme(date.monthValue) else selection
