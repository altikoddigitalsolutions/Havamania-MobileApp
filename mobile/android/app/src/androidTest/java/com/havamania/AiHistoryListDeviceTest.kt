package com.havamania

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.havamania.ui.theme.AppTheme
import com.havamania.ui.theme.LocalHavamaniaColors
import com.havamania.ui.theme.LocalHavamaniaTypography
import com.havamania.ui.theme.ThemeFactory
import com.havamania.ui.theme.HavamaniaScreen
import com.havamania.ui.theme.ProvideResponsiveLayout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AiHistoryListDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longHistoryDetailKeepsHeaderAndLastMessageReachable() {
        val record = AiHistoryEntity(title = "Uzun sohbet", cityName = "İzmir",
            messages = List(200) { AltikodChatMessage(text = "Örnek mesaj $it", isUser = it % 2 == 0) })
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(LocalDensity.current.density, 2f)
            ) {
                ProvideResponsiveLayout {
                    AiHistoryDetailMessages(record, Modifier.testTag("history-detail"))
                }
            }
        }
        compose.onNodeWithText("Uzun sohbet").assertIsDisplayed()
        compose.onNodeWithText("İzmir").assertIsDisplayed()
        val last = compose.onNodeWithText("Örnek mesaj 199")
        last.assertDoesNotExist()
        val list = compose.onNodeWithTag("history-detail")
        list.performScrollToIndex(record.messages.size)
        last.assertIsDisplayed()
        list.performScrollToIndex(0)
        compose.onNodeWithText("Uzun sohbet").assertIsDisplayed()
    }

    @Test fun historyCardActionsAreUnavailableDuringDeletionAndReturnAfterward() {
        val enabled = mutableStateOf(false)
        var opened = false
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                ProvideResponsiveLayout {
                    AiHistoryCard(AiHistorySummary("example", "Örnek sohbet", "", null, 0L, false),
                        enabled.value, { opened = true }, {})
                }
            }
        }
        val open = compose.onNode(hasText("Örnek sohbet") and hasClickAction())
        compose.onNodeWithText("Örnek sohbet").assertIsDisplayed()
        open.assertDoesNotExist()
        compose.onNodeWithContentDescription("Sohbeti sil").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        open.performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun longWarningsLeaveTheLastHistoryRowAndRetryReachableAtLargeFontScale() {
        var retried = false
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            val config = Configuration(LocalConfiguration.current).apply { screenHeightDp = 320 }
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, 2f)
            ) {
                Box(Modifier.size(440.dp, 320.dp)) {
                    HavamaniaScreen(bottomBar = {
                        AiHistoryNoticePanel {
                            Text("Senkronizasyon bekleniyor. ".repeat(12))
                            TextButton(onClick = { retried = true }) { Text("Senkronizasyonu tekrar dene") }
                        }
                    }) { padding ->
                        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("history-list")) {
                            items(20) { Text("Sohbet $it") }
                        }
                    }
                }
            }
        }
        val list = compose.onNodeWithTag("history-list")
        list.performScrollToIndex(19)
        compose.onNodeWithText("Sohbet 19").assertIsDisplayed()
        val retry = compose.onNodeWithText("Senkronizasyonu tekrar dene")
        retry.performScrollTo().assertIsDisplayed().performClick()
        assertTrue(retry.getUnclippedBoundsInRoot().top >= list.getUnclippedBoundsInRoot().bottom)
        compose.runOnIdle { assertTrue(retried) }
    }

    @Test fun historySeparatesLoadingFailureAndEmptyAfterRetry() {
        val status = mutableStateOf(AiHistoryListStatus.LOADING)
        val confirmedEmpty = mutableStateOf(false)
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.LIGHT)
            CompositionLocalProvider(
                LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors)
            ) {
                AiHistoryWithoutItems(status.value, confirmedEmpty.value) { status.value = AiHistoryListStatus.LOADING }
            }
        }
        val loading = compose.onNodeWithText("Sohbet geçmişi yükleniyor…")
        val empty = compose.onNodeWithText("Henüz bir sohbet geçmişin yok.")
        val error = compose.onNodeWithText("Geçmiş yüklenemedi")
        loading.assertIsDisplayed()
        empty.assertDoesNotExist()
        compose.runOnIdle { status.value = AiHistoryListStatus.FAILED }
        error.assertIsDisplayed()
        empty.assertDoesNotExist()
        compose.onNodeWithText("Tekrar dene", ignoreCase = true).performClick()
        loading.assertIsDisplayed()
        error.assertDoesNotExist()
        compose.runOnIdle { status.value = AiHistoryListStatus.READY }
        compose.onNodeWithText("Bu cihazda sohbet kaydı yok.").assertIsDisplayed()
        empty.assertDoesNotExist()
        compose.runOnIdle { confirmedEmpty.value = true }
        empty.assertIsDisplayed()
        loading.assertDoesNotExist()
    }
}
