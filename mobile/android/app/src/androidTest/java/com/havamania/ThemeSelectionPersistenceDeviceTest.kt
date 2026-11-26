package com.havamania

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.havamania.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ThemeSelectionPersistenceDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun awaitSelection(theme: AppTheme) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Seçim: ${theme.name}").fetchSemanticsNodes().size == 1 }
    }
    @Test fun autoPersistsAcrossActualActivityRecreationWithoutChangingOtherOwner() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.havamania", context.packageName)
        assertNull(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)
        val ownerA = "qa-theme-a-${UUID.randomUUID()}"
        val ownerB = "qa-theme-b-${UUID.randomUUID()}"
        val owners = MutableStateFlow(ownerA)
        var scenario: ActivityScenario<ThemeSelectionPersistenceTestActivity>? = null
        try {
            runBlocking {
                ThemeManager.saveTheme(context, AppTheme.SUMMER, ownerA)
                ThemeManager.saveTheme(context, AppTheme.LIGHT, ownerB)
            }
            ThemeSelectionPersistenceFixture.owner = owners
            val opened = ActivityScenario.launch(ThemeSelectionPersistenceTestActivity::class.java)
            scenario = opened
            awaitSelection(AppTheme.SUMMER)
            compose.onNodeWithText("Temayı seç (QA)").performClick()
            compose.onNodeWithText(AppTheme.AUTO.title).performScrollTo().assertIsDisplayed().performClick()
            awaitSelection(AppTheme.AUTO)
            opened.recreate()
            awaitSelection(AppTheme.AUTO)
            compose.onNodeWithText("Temayı seç (QA)").performClick()
            compose.onNodeWithText(AppTheme.AUTO.title).performScrollTo().assertIsSelected()
            opened.recreate()
            compose.onNodeWithText(AppTheme.AUTO.title).performScrollTo().assertIsSelected()
            compose.onNodeWithText(AppTheme.DARK.title).performScrollTo().assertIsDisplayed().performClick()
            awaitSelection(AppTheme.DARK)
            owners.value = ownerB
            awaitSelection(AppTheme.LIGHT)
            owners.value = ownerA
            awaitSelection(AppTheme.DARK)
            compose.onNodeWithText("Temayı seç (QA)").performClick()
            for (theme in AppTheme.entries) {
                compose.onNodeWithText(theme.title).performScrollTo().assertIsDisplayed()
            }
            compose.onNodeWithText("Kapat").performClick()
            runBlocking {
                assertEquals(AppTheme.DARK, ThemeManager.getTheme(context, ownerA).first())
                assertEquals(AppTheme.LIGHT, ThemeManager.getTheme(context, ownerB).first())
            }
        } finally {
            scenario?.close()
            ThemeSelectionPersistenceFixture.owner = null
            runBlocking {
                context.dataStore.edit {
                    it.remove(stringPreferencesKey("havamania:$ownerA:app_theme"))
                    it.remove(stringPreferencesKey("havamania:$ownerB:app_theme"))
                }
            }
        }
    }
}
