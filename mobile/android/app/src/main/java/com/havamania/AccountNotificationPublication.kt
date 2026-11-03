package com.havamania

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Only local preference I/O and the final synchronous post belong in this gate. */
internal object NotificationPreferenceGate {
    private val mutex = Mutex()
    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}

internal suspend fun postNotificationIfCurrent(
    owner: String, epoch: Long, currentOwner: () -> String?, blocked: () -> Boolean,
    enabled: suspend () -> Boolean = { true },
    post: () -> Unit
): Boolean {
    val context = currentCoroutineContext()
    context.ensureActive()
    val job = context[Job]
    return NotificationPreferenceGate.withLock {
        if (!enabled()) return@withLock false
        context.ensureActive()
        NotificationSessionEpoch.publish(owner, epoch, currentOwner,
            { job?.isActive == false || blocked() }, post)
    }
}
