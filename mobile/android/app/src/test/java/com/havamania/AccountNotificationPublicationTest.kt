package com.havamania

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Collections
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountNotificationPublicationTest {
    @Test fun preferenceDisabledAfterPreparationBlocksFinalPost() = runBlocking {
        var enabled = true
        val epoch = NotificationSessionEpoch.capture()
        var posts = 0
        NotificationPreferenceGate.withLock { enabled = false }
        assertFalse(postNotificationIfCurrent("a", epoch, { "a" }, { false },
            enabled = { enabled }) { posts++ })
        assertEquals(0, posts)
        NotificationPreferenceGate.withLock { enabled = true }
        assertTrue(postNotificationIfCurrent("a", epoch, { "a" }, { false },
            enabled = { enabled }) { posts++ })
        assertEquals(1, posts)
    }

    @Test fun postWaitsForPreferenceSaveAndReadsCommittedValue() = runBlocking {
        var enabled = true
        var posts = 0
        var reads = 0
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val save = launch {
            NotificationPreferenceGate.withLock {
                entered.complete(Unit); release.await(); enabled = false
            }
        }
        entered.await()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false },
                enabled = { reads++; enabled }) { posts++ }
        }
        assertFalse(result.isCompleted); assertEquals(0, reads)
        release.complete(Unit); save.join()
        assertFalse(result.await()); assertEquals(1, reads); assertEquals(0, posts)
    }

    @Test fun cancellationDuringPreferenceReadDoesNotPostOrKeepGateLocked() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Boolean>()
        var posts = 0
        val work = launch {
            postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false },
                enabled = { entered.complete(Unit); never.await() }) { posts++ }
        }
        entered.await(); work.cancelAndJoin()
        assertEquals(0, posts)
        assertTrue(withTimeout(2000) {
            postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false },
                enabled = { true }) { posts++ }
        })
        assertEquals(1, posts)
    }

    @Test fun failedPreferenceReadFailsClosedAndReleasesGate() = runBlocking {
        var posts = 0
        val failure = runCatching {
            postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false },
                enabled = { throw java.io.IOException("unreadable preferences") }) { posts++ }
        }
        assertTrue(failure.exceptionOrNull() is java.io.IOException)
        assertEquals(0, posts)
        withTimeout(2000) { NotificationPreferenceGate.withLock { /* A failed read must release the gate. */ } }
    }

    @Test fun wrongOwnerOldEpochOrDeletionCannotPost() {
        val epoch = NotificationSessionEpoch.capture()
        assertFalse(NotificationSessionEpoch.publish("a", epoch, { "b" }, { false }) { fail("No wrong-owner post") })
        assertFalse(NotificationSessionEpoch.publish("a", epoch, { "a" }, { true }) { fail("No deletion post") })
        NotificationSessionEpoch.changed()
        assertFalse(NotificationSessionEpoch.publish("a", epoch, { "a" }, { false }) { fail("No stale-session post") })
    }
    @Test fun currentSessionPostsExactlyOnce() = runBlocking {
        var posts = 0
        assertTrue(postNotificationIfCurrent("a", NotificationSessionEpoch.capture(), { "a" }, { false }) { posts++ })
        assertEquals(1, posts)
    }
    @Test fun accountResetClearsAfterAlreadyAdmittedPostAndBlocksLatePost() {
        val epoch = NotificationSessionEpoch.capture()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val resetStarted = CountDownLatch(1)
        val trace = Collections.synchronizedList(mutableListOf<String>())
        val poster = Thread {
            NotificationSessionEpoch.publish("a", epoch, { "a" }, { false }) {
                entered.countDown(); release.await(3, TimeUnit.SECONDS); trace.add("post")
            }
        }
        val resetter = Thread { resetStarted.countDown(); NotificationSessionEpoch.changed { trace.add("clear") } }
        try {
            poster.start(); assertTrue(entered.await(2, TimeUnit.SECONDS))
            resetter.start(); assertTrue(resetStarted.await(2, TimeUnit.SECONDS))
            assertTrue(trace.isEmpty())
            release.countDown(); poster.join(3000); resetter.join(3000)
            assertFalse(poster.isAlive); assertFalse(resetter.isAlive)
            assertEquals(listOf("post", "clear"), trace.toList())
            assertFalse(NotificationSessionEpoch.publish("a", epoch, { "a" }, { false }) { fail("No late post") })
        } finally { release.countDown(); poster.join(3000); if (resetter.state != Thread.State.NEW) resetter.join(3000) }
    }
    @Test fun cancellationWhileWaitingForGateDoesNotPost() = runBlocking {
        val epoch = NotificationSessionEpoch.capture()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = Thread {
            NotificationSessionEpoch.publish("a", epoch, { "a" }, { false }) {
                entered.countDown(); release.await(3, TimeUnit.SECONDS)
            }
        }
        var posts = 0
        val started = CompletableDeferred<Unit>()
        val workerThread = java.util.concurrent.atomic.AtomicReference<Thread>()
        try {
            holder.start(); assertTrue(entered.await(2, TimeUnit.SECONDS))
            val work = launch(Dispatchers.Default) {
                workerThread.set(Thread.currentThread())
                started.complete(Unit)
                postNotificationIfCurrent("a", epoch, { "a" }, { false }) { posts++ }
            }
            started.await()
            withTimeout(2000) { while (workerThread.get().state != Thread.State.BLOCKED) yield() }
            work.cancel(); release.countDown(); work.join()
            assertEquals(0, posts)
        } finally { release.countDown(); holder.join(3000) }
    }
}
