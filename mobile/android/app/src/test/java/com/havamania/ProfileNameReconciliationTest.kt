package com.havamania

import org.junit.Assert.*
import org.junit.Test

class ProfileNameReconciliationTest {
    private val profile = UserProfile(uid = "a", name = "Server name")
    @Test fun onlyConfirmedCurrentOwnerMayRepairDifferentName() {
        assertTrue(needsProfileNameReconciliation(profile, "a", "Old", true, true, false))
        assertFalse(needsProfileNameReconciliation(profile, "b", "Old", true, true, false))
        assertFalse(needsProfileNameReconciliation(profile, null, "Old", true, true, false))
    }
    @Test fun cachedOrPendingSnapshotCannotChangeSdkName() {
        assertFalse(needsProfileNameReconciliation(profile, "a", "Old", false, true, false))
    }
    @Test fun activeSaveOrUnsettledSdkSessionPreventsCompetingRepair() {
        assertFalse(needsProfileNameReconciliation(profile, "a", "Old", true, true, true))
        assertFalse(needsProfileNameReconciliation(profile, "a", "Old", true, false, false))
    }
    @Test fun matchingBlankOrInvalidNameDoesNotStartSdkWrite() {
        assertFalse(needsProfileNameReconciliation(profile, "a", profile.name, true, true, false))
        assertFalse(needsProfileNameReconciliation(profile.copy(name = " "), "a", "Old", true, true, false))
        assertFalse(needsProfileNameReconciliation(profile.copy(name = "x".repeat(51)), "a", "Old", true, true, false))
    }
}
