package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real settings dialog state and delete composable, isolated from account/backend calls. */
class AccountSettingsDialogsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun content(owner: State<String?>, confirmed: (String) -> Unit) = @Composable {
        val colors = ThemeFactory.createColors(AppTheme.DARK)
        CompositionLocalProvider(LocalHavamaniaColors provides colors,
            LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
            ProvideResponsiveLayout {
                MaterialTheme {
                    val state = rememberSettingsDialogs(owner.value)
                    TextButton(onClick = { state.delete.value = true }) { Text("Silme penceresini aç") }
                    if (state.delete.value) AccountDeleteDialog(
                        onDismiss = { state.delete.value = false }, onConfirm = confirmed)
                }
            }
        }
    }
    private fun fill() {
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("test-only-password")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("SİL")
        compose.onNodeWithText("HESABI SİL").assertIsEnabled()
    }
    private fun assertFresh() {
        compose.onNodeWithText("Hesabımı Sil").assertDoesNotExist()
        compose.onNodeWithText("Silme penceresini aç").performClick()
        compose.onAllNodes(hasSetTextAction())[0].assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onAllNodes(hasSetTextAction())[1].assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("HESABI SİL").assertIsNotEnabled()
    }
    @Test fun changingOwnerClosesConfirmationAndDropsPassword() {
        val owner = mutableStateOf<String?>("a")
        compose.setContent(content(owner) { fail("Unexpected account action") })
        compose.onNodeWithText("Silme penceresini aç").performClick()
        fill()
        compose.runOnIdle { owner.value = "b" }
        assertFresh()
    }
    @Test fun signingOutClosesConfirmationAndDropsPassword() {
        val owner = mutableStateOf<String?>("a")
        compose.setContent(content(owner) { fail("Unexpected account action") })
        compose.onNodeWithText("Silme penceresini aç").performClick()
        fill()
        compose.runOnIdle { owner.value = null }
        assertFresh()
    }
    @Test fun recreationDoesNotRestoreSensitiveConfirmationOrPassword() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent(content(mutableStateOf("a")) { fail("Unexpected account action") })
        compose.onNodeWithText("Silme penceresini aç").performClick()
        fill()
        restoration.emulateSavedInstanceStateRestore()
        assertFresh()
    }
}