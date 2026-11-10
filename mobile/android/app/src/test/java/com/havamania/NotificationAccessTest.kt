package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NotificationAccessTest {
    @Test fun systemLevelDenialBlocksPostingOnAllSupportedAndroidVersions() {
        for (sdk in listOf(23, 26, 31, 32, 33, 35)) {
            assertFalse(notificationPostingAllowed(sdk, true, false))
            assertFalse(notificationPostingAllowed(sdk, false, false))
        }
    }
    @Test fun runtimePermissionIsRequiredStartingWithAndroid13() {
        assertTrue(notificationPostingAllowed(32, false, true))
        assertFalse(notificationPostingAllowed(33, false, true))
        assertTrue(notificationPostingAllowed(33, true, true))
        assertTrue(notificationPostingAllowed(35, true, true))
    }
}
