package com.havamania

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class TravelEditorRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<TravelEditorTestActivity>()

    @Test fun systemBackDismissesEditorAndKeepsUnderlyingScreen() {
        val openEditor = compose.onNodeWithText("Test düzenleyicisini aç")
        openEditor.performClick()
        compose.onNode(hasSetTextAction()).assertExists()

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)

        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        openEditor.assertIsDisplayed().performClick()
        compose.onNode(hasSetTextAction()).assertExists()
    }

    @Test fun actualActivityRecreationPreservesOpenEditorAndTypedDraft() {
        compose.onNodeWithText("Test düzenleyicisini aç").performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Korunan taslak")
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction()).performScrollTo().assertTextContains("Korunan taslak")
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().assertIsNotEnabled()
    }

    @Test fun selectedOriginCoordinatesSurviveActualActivityRecreation() {
        compose.onNodeWithText("Test düzenleyicisini aç").performClick()
        compose.onNode(hasText("Bursa") and hasClickAction()).performScrollTo().performClick()
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Bursa").assertExists()
        compose.onNodeWithText("DEĞİŞİKLİKLERİ KAYDET").performScrollTo().performClick()
        compose.onNodeWithText("Test çıktısı: local-trip, Bursa, 40.2, 29.1").assertIsDisplayed()
    }
}
