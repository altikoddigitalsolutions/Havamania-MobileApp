package com.havamania

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountProfileWriteTest {
    @Test fun validWriteRunsAllStagesBeforeSuccess() = runBlocking {
        val stages = mutableListOf<Int>()
        runAccountProfileWrite("a", { "a" }, { true }, { stages.add(1) }, { stages.add(2) })
        assertEquals(listOf(1, 2), stages)
    }
    @Test fun deletionAfterRemoteWriteStopsNextStageAndSuccess() = runBlocking {
        var allowed = true
        try {
            runAccountProfileWrite("a", { "a" }, { allowed }, { allowed = false }, { fail("No next write") })
            fail("No success")
        } catch (_: CancellationException) { }
    }
    @Test fun accountChangeAfterFinalStageStillCannotReportSuccess() = runBlocking {
        var uid = "a"
        try {
            runAccountProfileWrite("a", { uid }, { true }, { uid = "b" })
            fail("No stale success")
        } catch (_: CancellationException) { }
    }
    @Test fun cancelledNonCancellableStageCannotContinue() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val work = launch {
            runAccountProfileWrite("a", { "a" }, { true }, {
                entered.complete(Unit); withContext(NonCancellable) { release.await() }
            }, { fail("No cancelled next stage") })
        }
        entered.await(); work.cancel(); release.complete(Unit); work.join()
        assertTrue(work.isCancelled)
    }
}
