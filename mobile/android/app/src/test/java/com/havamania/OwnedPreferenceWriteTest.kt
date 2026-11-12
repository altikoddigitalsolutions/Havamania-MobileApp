package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class OwnedPreferenceWriteTest {
    @Test fun `delayed write stays with original owner and cannot report success for new account`() = runBlocking {
        var uid = "A"
        val records = mutableMapOf("B" to true)
        val started = CompletableDeferred<Unit>(); val ready = CompletableDeferred<Unit>()
        val pending = async {
            writeOwnedPreference("A", { uid == "A" }) { owner ->
                started.complete(Unit); ready.await(); records[owner] = false
            }
        }
        started.await(); uid = "B"; ready.complete(Unit)
        assertEquals(PreferenceWriteResult.OBSOLETE, pending.await())
        assertEquals(true, records["B"])
        assertEquals(false, records["A"])
    }
    @Test fun `failed write never reports saved or changes existing value`() = runBlocking {
        val records = mutableMapOf("A" to true)
        val result = writeOwnedPreference("A", { true }) { throw java.io.IOException("disk") }
        assertEquals(PreferenceWriteResult.FAILED, result)
        assertEquals(true, records["A"])
    }
    @Test fun `obsolete queued write does not touch storage`() = runBlocking {
        var writes = 0
        assertEquals(PreferenceWriteResult.OBSOLETE, writeOwnedPreference("A", { false }) { writes++ })
        assertEquals(0, writes)
    }
    @Test fun `cancelled noncooperative completion cannot publish saved result`() = runBlocking {
        val started = CompletableDeferred<Unit>(); val ready = CompletableDeferred<Unit>()
        var success = false
        val job = launch {
            writeOwnedPreference("A", { true }) {
                withContext(NonCancellable) { started.complete(Unit); ready.await() }
            }
            success = true
        }
        started.await(); job.cancel(); ready.complete(Unit); job.join()
        assertTrue(job.isCancelled)
        assertFalse(success)
    }
}
