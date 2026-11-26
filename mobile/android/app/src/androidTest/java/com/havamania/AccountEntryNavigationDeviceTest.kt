package com.havamania

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccountEntryNavigationDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun awaitRoute(scenario: ActivityScenario<AccountEntryTestActivity>, expected: String) {
        compose.waitUntil(10_000) { compose.mainClock.advanceTimeByFrame(); var route: String? = null; scenario.onActivity { route = it.route }; route == expected }
    }
    @Test fun delayedProfileAndActualRecreationNeverSkipNewAccountPersonalization() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Loading)
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, { owner.value = null }, {
            profile.value = ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true))
        })
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.ACCOUNT_LOADING)
            opened.recreate(); awaitRoute(opened, Routes.ACCOUNT_LOADING)
            profile.value = ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = false))
            awaitRoute(opened, Routes.PERSONALIZATION)
            compose.onNodeWithText("Hava hedefi").assertDoesNotExist()
            opened.onActivity { assertFalse(it.visited.contains(Routes.WEATHER_ROOT)) }
            opened.recreate(); awaitRoute(opened, Routes.PERSONALIZATION)
            compose.onNodeWithText("Kişiselleştirmeyi tamamla").performClick()
            awaitRoute(opened, Routes.WEATHER_ROOT)
            opened.onActivity { assertNull(it.nav.previousBackStackEntry) }
            opened.recreate(); awaitRoute(opened, Routes.WEATHER_ROOT)
            compose.onNodeWithText("Hava hedefi").assertIsDisplayed()
            opened.onActivity { assertNull(it.nav.previousBackStackEntry) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }

    @Test fun errorRetryAndSystemBackLeaveSafeWelcomeEvenIfOldProfileArrives() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Error("Yerel test bağlantı hatası"))
        var retries = 0
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, { retries++ }, { owner.value = null }, {})
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.ACCOUNT_LOADING)
            compose.onNodeWithText("Profil yüklenemedi").assertIsDisplayed()
            compose.onNodeWithText("Tekrar dene").performClick()
            compose.runOnIdle { assertEquals(1, retries) }
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitRoute(opened, Routes.AUTH_WELCOME)
            profile.value = ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true))
            opened.recreate(); awaitRoute(opened, Routes.AUTH_WELCOME)
            compose.onNodeWithText("Giriş başlangıcı").assertIsDisplayed()
            compose.onNodeWithText("Hava hedefi").assertDoesNotExist()
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }

    @Test fun accountSwitchCannotUsePreviousOwnersReadyProfile() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true)))
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, { owner.value = null }, {})
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.WEATHER_ROOT)
            owner.value = "b"
            awaitRoute(opened, Routes.ACCOUNT_LOADING)
            opened.recreate(); awaitRoute(opened, Routes.ACCOUNT_LOADING)
            opened.onActivity { assertFalse(it.visited.contains(Routes.WEATHER_ROOT)) }
            profile.value = ProfileState.Success(UserProfile(uid = "b", onboardingCompleted = false))
            awaitRoute(opened, Routes.PERSONALIZATION)
            opened.onActivity { assertFalse(it.visited.contains(Routes.WEATHER_ROOT)) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }
    @Test fun tabStacksReturnToWeatherInsteadOfTransientProfileGate() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true)))
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, { owner.value = null }, {})
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.WEATHER_ROOT)
            for (route in listOf(Routes.CALENDAR_ROOT, Routes.PROFILE_ROOT, Routes.CALENDAR_ROOT)) {
                opened.onActivity { it.nav.navigate(route) { accountTabOptions(route) } }
                awaitRoute(opened, route)
                opened.onActivity { assertEquals(Routes.WEATHER_ROOT, it.nav.previousBackStackEntry?.destination?.route) }
            }
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            awaitRoute(opened, Routes.WEATHER_ROOT)
            opened.onActivity { assertNull(it.nav.previousBackStackEntry) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }

    @Test fun firstPersonalizationToolbarBackHasSafeExit() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Success(UserProfile(uid = "a")))
        var exits = 0
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, { exits++; owner.value = null }, {})
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.PERSONALIZATION)
            compose.onNodeWithText("Kişiselleştirmeden geri dön").performClick()
            awaitRoute(opened, Routes.AUTH_WELCOME)
            compose.runOnIdle { assertEquals(1, exits) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }
    @Test fun recommendationAssistantCanReturnToWeatherThroughActualBottomBar() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true)))
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, {}, {}, tabControls = true)
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.WEATHER_ROOT)
            compose.onNodeWithText("Soru sor (yerel test)").performClick()
            awaitRoute(opened, Routes.AI_ROOT + "?conversationId={conversationId}")
            compose.onNode(isSelectable() and hasText("Hava")).performClick()
            awaitRoute(opened, Routes.WEATHER_ROOT)
            compose.onNodeWithText("Hava hedefi").assertIsDisplayed()
            opened.onActivity { assertNull(it.nav.previousBackStackEntry) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }

    @Test fun recommendationTabSwitchKeepsDraftAcrossRecreationAndOtherTabs() {
        val owner = MutableStateFlow<String?>("a")
        val profile = MutableStateFlow<ProfileState>(ProfileState.Success(UserProfile(uid = "a", onboardingCompleted = true)))
        AccountEntryTestSession.inputs = AccountEntryTestSession.Inputs(owner, profile, {}, {}, {}, tabControls = true)
        var scenario: ActivityScenario<AccountEntryTestActivity>? = null
        val ai = Routes.AI_ROOT + "?conversationId={conversationId}"
        try {
            val opened = ActivityScenario.launch(AccountEntryTestActivity::class.java); scenario = opened
            awaitRoute(opened, Routes.WEATHER_ROOT)
            compose.onNodeWithText("Soru sor (yerel test)").performClick()
            awaitRoute(opened, ai)
            compose.onNodeWithText("Taslağı hazırla").performClick()
            opened.recreate()
            awaitRoute(opened, ai)
            compose.onNodeWithText("Taslak korunmalı").assertIsDisplayed()
            repeat(2) {
                for ((label, route) in listOf("Hava" to Routes.WEATHER_ROOT, "Takvim" to Routes.CALENDAR_ROOT,
                    "Profil" to Routes.PROFILE_ROOT, "Hava" to Routes.WEATHER_ROOT, "Asistan" to ai)) {
                    compose.onNode(isSelectable() and hasText(label)).performClick()
                    awaitRoute(opened, route)
                    compose.onNode(isSelectable() and hasText(label)).assertIsSelected()
                }
                compose.onNodeWithText("Taslak korunmalı").assertIsDisplayed()
            }
            compose.onNode(isSelectable() and hasText("Hava")).performClick()
            awaitRoute(opened, Routes.WEATHER_ROOT)
            opened.onActivity { assertNull(it.nav.previousBackStackEntry) }
        } finally { scenario?.close(); AccountEntryTestSession.inputs = null }
    }

}