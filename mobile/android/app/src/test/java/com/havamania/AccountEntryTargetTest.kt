package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AccountEntryTargetTest {
    @Test fun absentOrPreviousOwnersProfileCannotOpenAccountContent() {
        assertEquals(Routes.ACCOUNT_LOADING, accountEntryTarget("b", null))
        for (completed in listOf(false, true)) {
            assertEquals(Routes.ACCOUNT_LOADING, accountEntryTarget("b", UserProfile(uid = "a", onboardingCompleted = completed)))
        }
    }
    @Test fun newAccountMustCompletePersonalizationBeforeWeather() {
        val profile = UserProfile(uid = "a")
        assertEquals(Routes.PERSONALIZATION, accountEntryTarget("a", profile))
        assertEquals(Routes.WEATHER_ROOT, accountEntryTarget("a", profile.copy(onboardingCompleted = true)))
    }
    @Test fun lateProfileAfterLogoutCannotReopenAccount() {
        assertEquals(Routes.AUTH_WELCOME, accountEntryTarget(null, UserProfile(uid = "a", onboardingCompleted = true)))
        assertEquals(Routes.AUTH_WELCOME, accountEntryTarget(null, null))
    }
}