package com.havamania

import androidx.compose.ui.test.performScrollTo

import androidx.compose.foundation.verticalScroll

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AssistantWeatherSummaryDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun incomingReplyPreservesOlderReadingPositionAndFollowsAtTheEnd() {
        val messages = mutableStateOf(List(30) { AltikodChatMessage(text = "Mesaj $it", isUser = false) })
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val state = rememberLazyListState()
            FollowAssistantConversation(state, messages.value, isLoading = false)
            Box {
                LazyColumn(Modifier.height(240.dp).testTag("conversation"), state = state) {
                    items(messages.value, key = { it.id }) { Text(it.text, Modifier.height(48.dp)) }
                }
                JumpToLatestAssistantMessage(state, messages.value.lastIndex)
            }
        }
        val conversation = compose.onNodeWithTag("conversation")
        val jump = compose.onNodeWithText("Son mesaja git")
        compose.onNodeWithText("Mesaj 29").assertIsDisplayed()
        jump.assertDoesNotExist()
        conversation.performScrollToIndex(2)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Mesaj 2").assertIsDisplayed()
        compose.runOnIdle {
            messages.value = messages.value + AltikodChatMessage(text = "İlk yeni yanıt", isUser = false)
        }
        compose.onNodeWithText("Mesaj 2").assertIsDisplayed()
        jump.assertIsDisplayed().performClick()
        compose.onNodeWithText("İlk yeni yanıt").assertIsDisplayed()
        jump.assertDoesNotExist()
        compose.runOnIdle {
            messages.value = messages.value + AltikodChatMessage(text = "İkinci yeni yanıt", isUser = false)
        }
        compose.onNodeWithText("İkinci yeni yanıt").assertIsDisplayed()
    }

    @Test fun consentOpenedWithSystemKeyboardKeepsDraftAndSendLockedUntilDismissed() {
        val sent = mutableListOf<String>()
        val visible = mutableStateOf(false)
        val draft = mutableStateOf("Taslak korunmalı")
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)) {
                androidx.compose.material3.MaterialTheme {
                    Scaffold(bottomBar = {
                        ChatInput(draft.value, { draft.value = it }, { sent.add(it) }, false, colors, HavamaniaStyles(),
                            photoControlsEnabled = !visible.value)
                    }) { padding ->
                        Box(Modifier.then(Modifier.padding(padding))) {
                            androidx.compose.material3.TextButton(onClick = { visible.value = true }) {
                                Text("İzinleri aç (yerel test)")
                            }
                        }
                    }
                    AssistantConsentDialog(AssistantConsentState(visible = visible.value,
                        error = "Yerel bağlantı testi"), {}, {}, {}, { visible.value = false })
                }
            }
        }
        val input = compose.onNode(hasSetTextAction())
        input.performTouchInput { click() }
        val decor = compose.activity.window.decorView
        compose.waitUntil(8_000) {
            compose.runOnUiThread { ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == true }
        }
        compose.onNodeWithText("İzinleri aç (yerel test)").performTouchInput { click() }
        compose.onNodeWithTag("assistant_consent_dialog").assertIsDisplayed()
        compose.onNodeWithText("Vazgeç").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithTag("assistant_consent_dialog").assertDoesNotExist()
        input.assertTextContains("Taslak korunmalı")
        compose.runOnIdle { assertTrue(sent.isEmpty()) }
        compose.onNodeWithContentDescription("Mesaj gönder").assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf("Taslak korunmalı"), sent) }
    }

    @Test fun systemKeyboardMovesTheSendButtonUpAndKeepsItUsable() {
        verifySystemKeyboard()
    }

    @Test fun landscapeSystemKeyboardKeepsTheSendButtonUsable() {
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(timeoutMillis = 8_000) {
            compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
        verifySystemKeyboard()
    }

    private fun verifySystemKeyboard() {
        val sent = mutableListOf<String>()
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        compose.setContent {
            val draft = androidx.compose.runtime.remember { mutableStateOf("") }
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                Scaffold(bottomBar = {
                    ChatInput(draft.value, { draft.value = it }, { sent.add(it); draft.value = "" },
                        isSending = false, c = colors, s = HavamaniaStyles())
                }) {}
            }
        }
        val decor = compose.activity.window.decorView
        val send = compose.onNodeWithContentDescription("Mesaj gönder")
        val closedBottom = send.getUnclippedBoundsInRoot().bottom
        val input = compose.onNode(hasSetTextAction())
        input.performTouchInput { click() }
        compose.waitUntil(timeoutMillis = 8_000) {
            compose.runOnUiThread {
                ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
        }
        input.performTextReplacement("Bugün hava nasıl?")
        send.assertIsDisplayed()
        assertTrue(send.getUnclippedBoundsInRoot().bottom < closedBottom)
        send.performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf("Bugün hava nasıl?"), sent) }
    }

    @Test fun oversizedDraftRemainsEditableAndOnlyTheShortenedMessageIsSent() {
        val sent = mutableListOf<String>()
        var accept = false
        compose.setContent {
            val draft = androidx.compose.runtime.remember { mutableStateOf("") }
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                ChatInput(draft.value, { draft.value = it }, {
                    if (accept) {
                        sent.add(it)
                        draft.value = ""
                    }
                },
                    isSending = false, c = colors, s = HavamaniaStyles())
            }
        }
        val draft = "a".repeat(ASSISTANT_QUESTION_LIMIT + 1)
        val input = compose.onNode(hasSetTextAction())
        val send = compose.onNodeWithContentDescription("Mesaj gönder")
        input.performClick().performTextReplacement(draft)
        input.assertTextContains(draft)
        compose.onNodeWithText("Mesaj en fazla 4000 karakter olabilir.").assertIsDisplayed()
        send.assertIsNotEnabled()
        compose.runOnIdle { assertEquals(emptyList<String>(), sent) }

        input.performTextReplacement("Bugün hava nasıl?")
        compose.onNodeWithText("Mesaj en fazla 4000 karakter olabilir.").assertDoesNotExist()
        send.performClick()
        input.assertTextContains("Bugün hava nasıl?")
        compose.runOnIdle {
            assertEquals(emptyList<String>(), sent)
            accept = true
        }
        send.performClick()
        compose.runOnIdle { assertEquals(listOf("Bugün hava nasıl?"), sent) }
        send.assertIsNotEnabled()
    }

    @Test fun narrowLayoutKeepsDistinctSuggestionsClickableAtLargeFontScale() {
        val prompts = mutableListOf<String>()
        val enabled = mutableStateOf(false)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                Box(Modifier.width(320.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        AssistantStarterSuggestions(colors, enabled.value) { prompts.add(it) }
                    }
                }
            }
        }
        compose.onNodeWithText(assistantStarterPrompts.first()).assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        assistantStarterPrompts.forEach { prompt ->
            compose.onNodeWithText(prompt).performScrollTo().assertIsDisplayed().performClick()
        }
        compose.runOnIdle { assertEquals(assistantStarterPrompts, prompts) }
    }

    @Test fun lightThemeShowsCurrentSummaryAtLargeFontScale() {
        show(AppTheme.LIGHT, stale = false)
        compose.onNodeWithText("Bugünün hava özeti").assertIsDisplayed()
        assertSummaryVisible()
    }

    @Test fun darkThemeLabelsCachedSummaryAtLargeFontScale() {
        show(AppTheme.DARK, stale = true)
        compose.onNodeWithText("Son kaydedilen hava özeti").assertIsDisplayed()
        assertSummaryVisible()
    }

    private fun show(theme: AppTheme, stale: Boolean) {
        compose.setContent {
            val colors = ThemeFactory.createColors(theme)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)
            ) {
                TodaySummarySection(WeatherData(cityName = "Balıkesir", condition = "SAĞANAK YAĞIŞ",
                    temperature = "18°", isStale = stale), colors)
            }
        }
    }

    private fun assertSummaryVisible() {
        compose.onNodeWithText("Balıkesir için hava durumu sağanak yağış. Sıcaklık 18°.")
            .assertIsDisplayed()
    }
}
