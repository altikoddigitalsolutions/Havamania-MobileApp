package com.havamania

import org.junit.Assert.*
import org.junit.Test

class AvatarImageSizingTest {
    @Test fun largeAndExtremeDimensionsUseBoundedSampling() {
        assertEquals(4, avatarSampleSize(4096, 2048))
        assertEquals(1, avatarSampleSize(512, 512))
        assertTrue(Int.MAX_VALUE.toLong() <= 1024L * avatarSampleSize(Int.MAX_VALUE, 1))
    }
    @Test fun outputPreservesAspectAndNeverUpscales() {
        assertEquals(512 to 256, avatarOutputSize(4096, 2048))
        assertEquals(256 to 512, avatarOutputSize(2048, 4096))
        assertEquals(2 to 3, avatarOutputSize(2, 3))
        assertEquals(512 to 1, avatarOutputSize(Int.MAX_VALUE, 1))
    }
    @Test fun invalidBoundsAreRejected() {
        for ((w, h) in listOf(0 to 10, -1 to 10, 10 to 0)) {
            try { avatarSampleSize(w, h); fail("Expected invalid bounds") } catch (_: IllegalArgumentException) { }
            try { avatarOutputSize(w, h); fail("Expected invalid bounds") } catch (_: IllegalArgumentException) { }
        }
    }
}
