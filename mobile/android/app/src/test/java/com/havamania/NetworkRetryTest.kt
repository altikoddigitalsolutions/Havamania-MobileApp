package com.havamania

import org.junit.Assert.*
import org.junit.Test

class NetworkRetryTest {

    @Test
    fun `calculateBackoffMillis increases with attempt and includes jitter`() {
        val delay0 = NetworkModule.calculateBackoffMillis(0)
        val delay1 = NetworkModule.calculateBackoffMillis(1)

        // Attempt 0 base is 250 + jitter (0..150) -> range [250, 400]
        // Attempt 1 base is 500 + jitter (0..150) -> range [500, 650]
        assertTrue(delay0 in 250..450)
        assertTrue(delay1 in 500..700)
    }
}
