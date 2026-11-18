package com.havamania

import org.junit.Assert.*
import org.junit.Test

class ProfilePremiumMirrorTest {
    @Test fun absentMirrorIsFree() { assertFalse(readProfilePremiumMirror(emptyMap())) }
    @Test fun canonicalAndLegacyBooleanNamesAreExplicitlySupported() {
        for (key in listOf("isPremium", "premium")) {
            assertTrue(readProfilePremiumMirror(mapOf(key to true)))
            assertFalse(readProfilePremiumMirror(mapOf(key to false)))
        }
    }
    @Test fun agreeingAliasesRemainReadable() {
        assertTrue(readProfilePremiumMirror(mapOf("isPremium" to true, "premium" to true)))
        assertFalse(readProfilePremiumMirror(mapOf("isPremium" to false, "premium" to false)))
    }
    @Test fun conflictingAliasesNeverChooseTheMorePrivilegedValue() {
        for (value in listOf(true, false)) {
            try { readProfilePremiumMirror(mapOf("isPremium" to value, "premium" to !value)); fail("Conflict accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun invalidTypesIncludingNullAreNotCoerced() {
        for (key in listOf("isPremium", "premium")) for (value in listOf(null, "true", 1, emptyList<String>())) {
            try { readProfilePremiumMirror(mapOf(key to value)); fail("Invalid type accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun pendingLocalGrantDoesNotBecomeServerConfirmation() {
        assertFalse(profilePremiumForSnapshot(mapOf("isPremium" to true), false, false, true))
    }
    @Test fun cacheCannotGrantAndCannotOverrideExistingConfirmation() {
        assertFalse(profilePremiumForSnapshot(mapOf("premium" to true), false, true, false))
        assertTrue(profilePremiumForSnapshot(mapOf("isPremium" to false), true, true, false))
    }
    @Test fun serverRevocationOverridesEarlierConfirmation() {
        assertFalse(profilePremiumForSnapshot(mapOf("isPremium" to false), true, false, false))
    }
    @Test fun malformedServerMirrorBecomesSafeProfileFailure() {
        val state = decodeObservedProfile("a") {
            UserProfile(uid = "a").copy(isPremium = profilePremiumForSnapshot(
                mapOf("isPremium" to "private-value"), false, false, false))
        }
        assertEquals(ProfileReadState.Failed("a", PROFILE_DATA_ERROR), state)
    }
}
