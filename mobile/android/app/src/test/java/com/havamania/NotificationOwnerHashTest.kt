package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NotificationOwnerHashTest {
    @Test fun markerUsesStableDigestRatherThanRawUid() {
        assertEquals("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb", accountOwnerHash("a"))
        assertEquals(accountOwnerHash("owner-a"), accountOwnerHash("owner-a"))
        assertNotEquals(accountOwnerHash("owner-a"), accountOwnerHash("owner-b"))
    }
}
