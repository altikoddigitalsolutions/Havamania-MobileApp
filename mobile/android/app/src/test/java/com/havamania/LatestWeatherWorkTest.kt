package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LatestWeatherWorkTest {
    @Test fun `new weather result wins over delayed old settings`() = runBlocking {
        val old = Any(); val fresh = Any()
        var current = old
        val ready = CompletableDeferred<String>()
        val started = CompletableDeferred<Unit>()
        var published = ""
        val pending = async {
            resolveLatestWeatherWork({ current === old }, { started.complete(Unit); ready.await() }, { published = it })
        }
        started.await()
        current = fresh
        assertTrue(resolveLatestWeatherWork({ current === fresh }, { "fresh" }, { published = it }))
        ready.complete("old")
        assertFalse(pending.await())
        assertEquals("fresh", published)
    }
    @Test fun `account switch and return cannot revive old selection`() = runBlocking {
        val oldSelection = Any()
        var selection = oldSelection
        var uid = "A"
        val ready = CompletableDeferred<Unit>(); val started = CompletableDeferred<Unit>()
        var published = false
        val pending = async {
            resolveLatestWeatherWork({ uid == "A" && selection === oldSelection },
                { started.complete(Unit); ready.await() }, { published = true })
        }
        started.await()
        uid = "B"; selection = Any()
        uid = "A"; selection = Any()
        ready.complete(Unit)
        assertFalse(pending.await())
        assertFalse(published)
    }
    @Test fun `cancellation rejects even noncooperative delayed completion`() = runBlocking {
        val ready = CompletableDeferred<Unit>(); val started = CompletableDeferred<Unit>()
        var published = false
        val pending = launch {
            resolveLatestWeatherWork({ true }, {
                withContext(NonCancellable) { started.complete(Unit); ready.await() }
            }, { published = true })
        }
        started.await()
        pending.cancel()
        ready.complete(Unit)
        pending.join()
        assertTrue(pending.isCancelled)
        assertFalse(published)
    }
    @Test fun `optional settings failure leaves primary result and prior enrichment intact`() = runBlocking {
        var published = "existing"
        assertFalse(resolveLatestWeatherWork<String>({ true }, { throw java.io.IOException("local settings") }, { published = it }))
        assertEquals("existing", published)
    }
    @Test fun `obsolete work is not loaded at all`() = runBlocking {
        var loaded = false
        assertFalse(resolveLatestWeatherWork({ false }, { loaded = true }, { fail("obsolete publication") }))
        assertFalse(loaded)
    }
}
