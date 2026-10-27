package com.havamania

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class TravelEditorRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<TravelEditorTestActivity>()

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
