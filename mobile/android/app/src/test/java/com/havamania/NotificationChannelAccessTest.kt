package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NotificationChannelAccessTest {
    @Test fun preChannelDevicesUseExistingAppPermissionPolicy() {
        assertTrue(notificationChannelPolicyAllowsPosting(25, null, true))
    }

    @Test fun missingDisabledOrInvalidImportanceCannotConsumeDelivery() {
        for (sdk in listOf(26, 27, 28, 33, 35)) {
            for (importance in listOf(null, 0, -1000)) {
                assertFalse(notificationChannelPolicyAllowsPosting(sdk, importance, false))
            }
        }
    }

    @Test fun silentAndLowImportanceAreStillValidVisibleChannels() {
        for (importance in 1..5) {
            assertTrue(notificationChannelPolicyAllowsPosting(26, importance, false))
            assertTrue(notificationChannelPolicyAllowsPosting(35, importance, false))
        }
    }

    @Test fun groupBlockIsEnforcedOnlyWhereSupported() {
        assertTrue(notificationChannelPolicyAllowsPosting(27, 3, true))
        assertFalse(notificationChannelPolicyAllowsPosting(28, 3, true))
        assertFalse(notificationChannelPolicyAllowsPosting(35, 4, true))
        assertTrue(notificationChannelPolicyAllowsPosting(35, 3, false))
    }
}
