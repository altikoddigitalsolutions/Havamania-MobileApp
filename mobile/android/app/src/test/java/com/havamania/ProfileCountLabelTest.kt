package com.havamania

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileCountLabelTest {
    @Test fun emptyCacheIsNotAConfirmedZero() {
        assertEquals("—", profileCountLabel(0, false))
        assertEquals("0", profileCountLabel(0, true))
    }
    @Test fun cachedRecordsRemainVisibleWithoutServerConfirmation() {
        assertEquals("27", profileCountLabel(27, false))
    }
}
