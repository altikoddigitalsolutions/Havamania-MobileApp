package com.havamania

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.havamania.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProfileUnavailableUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show(owner: State<String?>, state: ProfileState, attempt: State<Int>, retry: () -> Unit, back: () -> Unit) {
        compose.setContent {
            val colors = ThemeFactory.createColors(AppTheme.DARK)
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalHavamaniaColors provides colors,
                LocalHavamaniaTypography provides ThemeFactory.createTypography(colors),
                LocalDensity provides Density(density, 2f)) {
                ProvideResponsiveLayout { MaterialTheme {
                    ProfileUnavailableContent(owner.value, state, attempt.value, retry, back)
                } }
            }
        }
    }
    @Test fun slowLoadOffersRetryAndRetryRestartsItsOwnWait() {
        compose.mainClock.autoAdvance = false
        val attempt = mutableStateOf(0)
        var retries = 0
        show(mutableStateOf("a"), ProfileState.Loading, attempt, { retries++; attempt.value++ }, {})
        compose.onNodeWithText("Profil yükleniyor…").assertExists()
        compose.onNodeWithText("Tekrar dene").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(8_100)
        compose.onNodeWithText("Profil henüz yüklenemedi").assertExists()
        compose.onNodeWithText("Tekrar dene").performScrollTo().performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Profil yükleniyor…").assertExists()
        compose.runOnIdle { assertEquals(1, retries) }
    }
    @Test fun errorImmediatelyShowsMessageAndIndependentExit() {
        var retries = 0
        var backs = 0
        show(mutableStateOf("a"), ProfileState.Error(PROFILE_CONNECTION_ERROR), mutableStateOf(0),
            { retries++ }, { backs++ })
        compose.onNodeWithText(PROFILE_CONNECTION_ERROR).assertExists()
        compose.onNodeWithText("Profil yükleniyor…").assertDoesNotExist()
        compose.onNodeWithText("Tekrar dene").performScrollTo().performClick()
        compose.onNodeWithText("Geri dön").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(1, backs) }
    }
    @Test fun ownerChangeDropsOldSlowStateAndSignOutDoesNotOfferRetry() {
        compose.mainClock.autoAdvance = false
        val owner = mutableStateOf<String?>("a")
        show(owner, ProfileState.Loading, mutableStateOf(0), { fail("Unexpected retry") }, {})
        compose.mainClock.advanceTimeBy(8_100)
        compose.onNodeWithText("Profil henüz yüklenemedi").assertExists()
        compose.runOnIdle { owner.value = "b" }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Profil yükleniyor…").assertExists()
        compose.runOnIdle { owner.value = null }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Oturum açık değil").assertExists()
        compose.onNodeWithText("Tekrar dene").assertDoesNotExist()
        compose.onNodeWithText("Geri dön").assertExists()
    }
}