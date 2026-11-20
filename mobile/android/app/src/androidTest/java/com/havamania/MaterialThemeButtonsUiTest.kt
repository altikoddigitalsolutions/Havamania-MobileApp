package com.havamania

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Default Material controls inherit the real shared scheme, without per-screen color overrides. */
class MaterialThemeButtonsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun verify(initial: AppTheme, next: AppTheme) {
        val selected = mutableStateOf(initial)
        val observed = mutableMapOf<String, Color>()
        var clicks = 0
        compose.setContent {
            val scheme = ThemeManager.getColorScheme(selected.value)
            MaterialTheme(colorScheme = scheme) {
                Column {
                    Button(onClick = { clicks++ }) {
                        val color = LocalContentColor.current
                        SideEffect { observed["primary"] = color }
                        Text("Ana eylem")
                    }
                    FilledTonalButton(onClick = { clicks++ }) {
                        val color = LocalContentColor.current
                        SideEffect { observed["tonal"] = color }
                        Text("İkincil eylem")
                    }
                    TextButton(onClick = { clicks++ }) {
                        val color = LocalContentColor.current
                        SideEffect { observed["text"] = color }
                        Text("Metin eylemi")
                    }
                }
            }
        }
        for (theme in listOf(initial, next)) {
            compose.runOnIdle { selected.value = theme }
            compose.waitForIdle()
            compose.runOnIdle {
                val scheme = ThemeManager.getColorScheme(theme)
                assertEquals(scheme.onPrimary, observed["primary"])
                assertEquals(scheme.onSecondaryContainer, observed["tonal"])
                assertEquals(scheme.primary, observed["text"])
            }
            compose.onNodeWithText("Ana eylem").assertIsDisplayed().performClick()
            compose.onNodeWithText("İkincil eylem").assertIsDisplayed().performClick()
            compose.onNodeWithText("Metin eylemi").assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(6, clicks) }
    }
    @Test fun darkToLightKeepsDefaultControlForegroundsAndActionsConsistent() = verify(AppTheme.DARK, AppTheme.LIGHT)
    @Test fun springToAutumnUsesSeasonalMaterialActionColors() = verify(AppTheme.SPRING, AppTheme.AUTUMN)
}