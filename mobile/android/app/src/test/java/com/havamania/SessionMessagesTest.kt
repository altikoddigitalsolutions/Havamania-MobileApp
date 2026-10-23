package com.havamania

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class SessionMessagesTest {
    @Test fun changingSessionCancelsVisibleMessageAndAcceptsNewMessages() = runBlocking {
        val session = MutableStateFlow(0L)
        val messages = MutableSharedFlow<String>()
        val oldShown = CompletableDeferred<Unit>()
        val oldRemoved = CompletableDeferred<Unit>()
        val newShown = CompletableDeferred<Unit>()
        val collector = launch {
            collectSessionMessages(session, messages) { message ->
                if (message == "old") {
                    oldShown.complete(Unit)
                    try { awaitCancellation() } finally { oldRemoved.complete(Unit) }
                } else { newShown.complete(Unit) }
            }
        }
        try {
        withTimeout(5_000) {
            messages.subscriptionCount.first { it > 0 }
            messages.emit("old")
            oldShown.await()
            session.value++
            oldRemoved.await()
            // Wait for the replacement collector before sending a new message.
            yield()
            messages.subscriptionCount.first { it > 0 }
            messages.emit("new")
            newShown.await()
        }
        } finally { collector.cancelAndJoin() }
        assertTrue(oldRemoved.isCompleted)
        assertTrue(newShown.isCompleted)
    }
}
