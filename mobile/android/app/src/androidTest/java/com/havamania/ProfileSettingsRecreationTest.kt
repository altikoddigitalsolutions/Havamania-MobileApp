package com.havamania

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileSettingsRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<ProfileSettingsTestActivity>()

    @Test fun actualRecreationKeepsIdentityAndPremiumButDropsPasswordAndConfirmation() {
        compose.onNodeWithText("Havamania Premium Üyesi").assertIsDisplayed()
        compose.onNodeWithText("Şehir: Ankara").assertIsDisplayed()
        compose.onNodeWithText("Profil A").assertExists()
        compose.onNodeWithText("Silme penceresini aç").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("fixture-password")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("SİL")
        compose.onNodeWithText("HESABI SİL").assertIsEnabled()
        val before = compose.activity.model
        compose.activityRule.scenario.recreate()
        compose.runOnIdle { assertSame(before, compose.activity.model) }
        compose.onNodeWithText("Hesabımı Sil").assertDoesNotExist()
        compose.onNodeWithText("Havamania Premium Üyesi").assertIsDisplayed()
        compose.onNodeWithText("Şehir: Ankara").assertIsDisplayed()
        compose.onNodeWithText("Profil A").assertExists()
        compose.onNodeWithText("Silme penceresini aç").performClick()
        compose.onAllNodes(hasSetTextAction())[0].assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onAllNodes(hasSetTextAction())[1].assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("HESABI SİL").assertIsNotEnabled()
    }

    @Test fun accountSwitchThenRecreationDoesNotBringBackOldHeaderCityOrPremium() {
        compose.onNodeWithText("Silme penceresini aç").performClick()
        compose.runOnIdle {
            compose.activity.model.profile = UserProfile(uid = "fixture-b", name = "Profil B", defaultCity = "Bursa")
        }
        compose.onNodeWithText("Hesabımı Sil").assertDoesNotExist()
        compose.onNodeWithText("Havamania Premium Üyesi").assertDoesNotExist()
        compose.onNodeWithText("Şehir: Ankara").assertDoesNotExist()
        compose.onNodeWithText("Profil A").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Havamania Premium").assertIsDisplayed()
        compose.onNodeWithText("Şehir: Bursa").assertIsDisplayed()
        compose.onNodeWithText("Profil B").assertExists()
        compose.onNodeWithText("Profil A").assertDoesNotExist()
        compose.onNodeWithText("Havamania Premium Üyesi").assertDoesNotExist()
        compose.onNodeWithText("Hesabımı Sil").assertDoesNotExist()
    }
}