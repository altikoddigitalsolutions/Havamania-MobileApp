package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PreferenceWriteEpochTest {
    @Test fun `deletion invalidates queued writes even after busy flags clear`() {
        val fence = PreferenceWriteEpoch()
        val old = fence.capture()
        assertTrue(fence.permits(old, "A", "A", false))
        fence.invalidate()
        assertFalse(fence.permits(old, "A", "A", false))
        val duringDeletion = fence.capture()
        assertFalse(fence.permits(duringDeletion, "A", "A", true))
        fence.invalidate() // Completion acknowledgement.
        assertFalse(fence.permits(duringDeletion, "A", "A", false))
    }
    @Test fun `different and signed out owners cannot write deleted account keys`() {
        val fence = PreferenceWriteEpoch(); val token = fence.capture()
        assertFalse(fence.permits(token, "A", "B", false))
        assertFalse(fence.permits(token, "A", null, false))
        assertTrue(fence.permits(token, "legacy", null, false))
        assertFalse(fence.permits(token, "legacy", "B", false))
    }
    @Test fun `queued edit checks fence at transaction execution rather than launch`() = runBlocking {
        val fence = PreferenceWriteEpoch(); val token = fence.capture()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        var transformed = false
        val work = async {
            entered.complete(Unit); release.await()
            if (fence.permits(token, "A", "A", false)) transformed = true
        }
        entered.await(); fence.invalidate(); release.complete(Unit); work.await()
        assertFalse(transformed)
    }
    @Test fun `fresh current session writes remain allowed after invalidation`() {
        val fence = PreferenceWriteEpoch()
        fence.invalidate()
        val current = fence.capture()
        assertTrue(fence.permits(current, "B", "B", false))
        assertFalse(fence.permits(current, "B", "B", true))
    }
}
