package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class TravelHistoryDeviceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun historyDialogRejectsInvalidTemperatureAndUpdatesWithPlan() {
        val day = LocalDate.of(2026, 10, 1)
        var plan by mutableStateOf(TravelPlan(city = "Ankara", startDate = day, endDate = day,
            analyses = listOf(TravelWeatherAnalysis(averageTemperature = Double.NaN, rainRiskPercent = -1))))
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                ProvideResponsiveLayout { MaterialTheme {
                    PastTravelDetailDialog(plan, onDismiss = {}, onSaveNote = { _, _ -> })
                } }
            }
        }
        compose.onNodeWithText("geçerli sıcaklık bilgisi bulunmuyor", substring = true).assertIsDisplayed()
        compose.onNodeWithText("0°C", substring = true).assertDoesNotExist()
        compose.runOnIdle { plan = plan.copy(analyses = listOf(TravelWeatherAnalysis(averageTemperature = 14.0, rainRiskPercent = 80))) }
        compose.onNodeWithText("geçerli sıcaklık bilgisi bulunmuyor", substring = true).assertDoesNotExist()
        compose.onNodeWithText("14°C", substring = true).assertIsDisplayed()
        compose.onNodeWithText("%80", substring = true).assertIsDisplayed()
    }
}