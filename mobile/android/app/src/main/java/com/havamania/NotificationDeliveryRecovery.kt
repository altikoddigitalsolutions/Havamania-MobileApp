package com.havamania

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Current-process retry of idempotent local ACK/DELETE only; never retry the OS post here. */
internal class NotificationDeliveryRecovery {
    private data class Key(val owner: String, val id: String)
    private val gate = Mutex()
    private val pending = linkedMapOf<Key, suspend () -> Unit>()

    suspend fun runOrRemember(owner: String, id: String, operation: suspend () -> Unit) = gate.withLock {
        val key = Key(owner, id)
        pending[key] = operation
        operation()
        pending.remove(key)
        Unit
    }

    suspend fun retryForOwner(owner: String) = gate.withLock {
        // Remove only after success. Exceptions/cancellation keep the operation for the next trigger.
        for (key in pending.keys.filter { it.owner == owner }) {
            pending.getValue(key).invoke()
            pending.remove(key)
        }
    }

    suspend fun discardOwner(owner: String) = gate.withLock {
        pending.keys.removeAll { it.owner == owner }
    }

    companion object {
        val shared = NotificationDeliveryRecovery()
    }
}

/** Cleanup failures must not replace the original cancellation or post error. */
internal suspend fun cleanupNotificationPreservingFailure(original: Throwable?, cleanup: suspend () -> Unit) {
    try { cleanup() }
    catch (failure: Throwable) {
        if (original == null) throw failure
        if (original !== failure) original.addSuppressed(failure)
    }
}
