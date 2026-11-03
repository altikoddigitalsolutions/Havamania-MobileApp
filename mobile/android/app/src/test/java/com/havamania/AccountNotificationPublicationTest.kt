package com.havamania

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Collections
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AccountNotificationPublicationTest {
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
