package com.havamania

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.havamania.ui.theme.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AssistantTripSuggestionsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun multiTripRequiresExplicitSelectionAndTracksDeletionAndOwnerChange() {
        val day = LocalDate.of(2027, 1, 10)
        val a = TravelPlan(id = "first", userId = "A", city = "Balıkesir", district = "Ayvalık", startDate = day, endDate = day)
        val b = a.copy(id = "second", startDate = day.plusDays(1), endDate = day.plusDays(2))
        val plans = mutableStateOf(listOf(a, b))
        val owner = mutableStateOf("A")
        val enabled = mutableStateOf(true)
        val selected = mutableListOf<String>()
        compose.setContent {
            val c = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f),
                LocalHavamaniaColors provides c,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(c)) {
                Box(Modifier.width(320.dp)) {
                    AssistantTripSuggestions(plans.value, owner.value, c, enabled.value) { selected.add(it); true }
                }
            }
        }
        compose.onNodeWithText("Seyahat seç").performClick()
        compose.runOnIdle { assertTrue(selected.isEmpty()) }
        compose.onNodeWithText("11 Ocak 2027 – 12 Ocak 2027").performScrollTo().performClick()
        compose.onNodeWithText("Hangi seyahat?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("second"), selected) }
        compose.onNodeWithText("Seyahat seç").performClick()
        compose.runOnIdle { plans.value = listOf(a) }
        compose.onNodeWithText("11 Ocak 2027 – 12 Ocak 2027").assertDoesNotExist()
        compose.onNodeWithText("Vazgeç").performClick()
        compose.onNodeWithText("Havasını sor").assertIsDisplayed()
        compose.runOnIdle { plans.value = listOf(a, b); enabled.value = false }
        compose.onNodeWithText("Seyahat seç").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText("Seyahat seç").performClick()
        compose.runOnIdle { owner.value = "B"; plans.value = emptyList() }
        compose.onNodeWithText("Hangi seyahat?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("second"), selected) }
    }
}
