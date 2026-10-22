package com.havamania

import org.junit.Assert.*
import org.junit.Test

class HistorySyncPolicyTest {
    @Test fun confirmedRemovalDeletesUnchangedMirror() {
        assertTrue(shouldApplyHistoryRemoval(100, 100, false, false))
        assertTrue(shouldApplyHistoryRemoval(90, 100, false, false))
    }
    @Test fun newerLocalEditSurvivesOldRemoval() {
        assertFalse(shouldApplyHistoryRemoval(101, 100, false, false))
    }
    @Test fun offlineCacheCannotDeleteLocalCopy() {
        assertFalse(shouldApplyHistoryRemoval(100, 100, true, false))
    }
    @Test fun unacknowledgedWriteCannotDeleteLocalCopy() {
        assertFalse(shouldApplyHistoryRemoval(100, 100, false, true))
    }
}
