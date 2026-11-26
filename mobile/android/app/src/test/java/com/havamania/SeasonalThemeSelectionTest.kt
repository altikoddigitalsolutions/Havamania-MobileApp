package com.havamania

import com.havamania.ui.theme.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.junit.Assert.*
import org.junit.Test

class SeasonalThemeSelectionTest {
    @Test fun autoResolvesEveryMonth() {
        val expected = listOf(AppTheme.WINTER, AppTheme.WINTER, AppTheme.SPRING, AppTheme.SPRING,
            AppTheme.SPRING, AppTheme.SUMMER, AppTheme.SUMMER, AppTheme.SUMMER,
            AppTheme.AUTUMN, AppTheme.AUTUMN, AppTheme.AUTUMN, AppTheme.WINTER)
        expected.forEachIndexed { index, theme ->
            assertEquals(theme, resolveThemeSelection(AppTheme.AUTO, LocalDate.of(2026, index + 1, 1)))
        }
    }
    @Test fun manualSelectionsNeverFollowSeason() {
        AppTheme.entries.filter { it != AppTheme.AUTO }.forEach { selection ->
            (1..12).forEach { month ->
                assertEquals(selection, resolveThemeSelection(selection, LocalDate.of(2026, month, 1)))
            }
        }
    }
    @Test fun refreshedDateChangesAutoAtSeasonBoundary() = runBlocking {
        var date = LocalDate.of(2026, 11, 30)
        val dates = calendarDateChanges(today = { date }, awaitNextCheck = { date = date.plusDays(1) }).take(2).toList()
        assertEquals(listOf(AppTheme.AUTUMN, AppTheme.WINTER), dates.map { resolveThemeSelection(AppTheme.AUTO, it) })
    }
    @Test fun restartedDateObserverReadsCurrentDateImmediately() = runBlocking {
        var date = LocalDate.of(2026, 12, 31)
        val first = calendarDateChanges(today = { date }).take(1).toList().single()
        date = LocalDate.of(2027, 3, 1)
        val resumed = calendarDateChanges(today = { date }).take(1).toList().single()
        assertEquals(AppTheme.WINTER, resolveThemeSelection(AppTheme.AUTO, first))
        assertEquals(AppTheme.SPRING, resolveThemeSelection(AppTheme.AUTO, resumed))
    }
}
