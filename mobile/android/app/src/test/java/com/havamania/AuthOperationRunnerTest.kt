package com.havamania

import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AuthOperationRunnerTest {
    private class Queue : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.add(block) }
        fun drain() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }
    private class Harness {
        val queue = Queue()
        val scope = CoroutineScope(SupervisorJob() + queue)
        var starts = 0; var failures = 0; var cancelled = 0; var successes = 0
        val runner = AuthOperationRunner(scope, { starts++ }, { failures++ }, { cancelled++ })
        fun start(work: suspend () -> Unit) = runner.start({ successes++ }, work)
    }

    @Test fun reservesBeforeDispatchAndRejectsCrossOperationDuplicate() {
        val h = Harness(); var calls = 0
        assertTrue(h.start { calls++ })
        assertTrue(h.runner.busy)
        assertEquals(1, h.starts)
        assertFalse(h.start { calls += 100 })
        assertEquals(0, calls)
        h.queue.drain()
        assertEquals(1, calls); assertEquals(1, h.successes)
        assertFalse(h.runner.busy)
        assertTrue(h.start { calls++ }); h.queue.drain()
        assertEquals(2, calls)
        h.scope.cancel()
    }

    @Test fun failedRequestReleasesReservationForRetry() {
        val h = Harness()
        h.start { throw java.io.IOException("local-offline") }; h.queue.drain()
        assertEquals(1, h.failures); assertEquals(0, h.successes)
        assertFalse(h.runner.busy)
        h.start {}; h.queue.drain()
        assertEquals(1, h.successes)
        h.scope.cancel()
    }

    @Test fun cancellationIsNotPresentedAsFailureAndReleasesReservation() {
        val h = Harness()
        h.start { throw CancellationException("test cancellation") }; h.queue.drain()
        assertEquals(0, h.failures); assertEquals(0, h.successes); assertEquals(1, h.cancelled)
        assertFalse(h.runner.busy)
        h.start {}; h.queue.drain(); assertEquals(1, h.successes)
        h.scope.cancel()
    }

    @Test fun signOutInvalidatesLateNonCancellableFailureWithoutUnlockingNewRequest() {
        val h = Harness(); val oldReply = CompletableDeferred<Unit>(); val newReply = CompletableDeferred<Unit>()
        h.start { withContext(NonCancellable) { oldReply.await(); throw java.io.IOException("late") } }
        h.queue.drain(); h.runner.cancel()
        h.start { newReply.await() }; h.queue.drain()
        oldReply.complete(Unit); h.queue.drain()
        assertTrue(h.runner.busy); assertEquals(0, h.failures); assertEquals(0, h.cancelled)
        newReply.complete(Unit); h.queue.drain()
        assertEquals(1, h.successes); assertFalse(h.runner.busy)
        h.scope.cancel()
    }

    @Test fun signOutSuppressesLateNonCancellableSuccess() {
        val h = Harness(); val reply = CompletableDeferred<Unit>()
        h.start { withContext(NonCancellable) { reply.await() } }; h.queue.drain()
        h.runner.cancel(); reply.complete(Unit); h.queue.drain()
        assertEquals(0, h.successes); assertEquals(0, h.failures); assertFalse(h.runner.busy)
        h.scope.cancel()
    }

    @Test fun closedOrCancelledScopeCannotStartAnotherRequest() {
        val h = Harness(); var calls = 0
        h.start { calls++ }; h.runner.close(); h.queue.drain()
        assertEquals(0, calls); assertEquals(0, h.cancelled)
        assertFalse(h.start { calls++ }); assertFalse(h.runner.busy)
        h.scope.cancel()
        val other = Harness(); other.scope.cancel()
        assertFalse(other.start { calls++ }); assertEquals(0, other.starts)
    }
}