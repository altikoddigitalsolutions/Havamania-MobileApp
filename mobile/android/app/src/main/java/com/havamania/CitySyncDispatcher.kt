package com.havamania

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val CITY_SYNC_TIMEOUT_MILLIS = 60000L
internal const val CITY_SYNC_UNCERTAIN_SECONDS = 2250L + CITY_SYNC_TIMEOUT_MILLIS / 1000

/** Shared across ViewModels, bounded memory; collisions only serialize unrelated writes. */
internal object CitySyncDispatcher {
    private val locks = Array(64) { Mutex() }

    suspend fun sendLatest(
        key: String, owner: String, currentOwner: () -> String,
        read: suspend () -> CitySyncChange?,
        send: suspend (CitySyncChange) -> Unit,
        acknowledge: suspend (CitySyncChange) -> Unit,
        recordFailure: suspend (CitySyncChange, Exception) -> Unit = { _, _ -> },
        now: () -> Long = { System.currentTimeMillis() / 1000 },
        reserve: suspend (CitySyncChange, Long) -> CitySyncChange? = { change, _ -> change },
        timeoutMillis: Long = CITY_SYNC_TIMEOUT_MILLIS,
        isAllowed: () -> Boolean = { true },
        clock: () -> CitySyncClock? = { null },
        refreshClock: suspend (CitySyncChange) -> CitySyncChange? = { it }
    ) {
        suspend fun guardedPublish(block: suspend () -> Unit) {
            currentCoroutineContext().ensureActive()
            if (isAllowed()) publishAccountResult(owner, currentOwner, block)
        }
        locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            guardedPublish {
                // Read after acquiring the lock, never retain a stale queued payload.
                val initial = read() ?: return@guardedPublish
                if (initial.acknowledged) return@guardedPublish
                val change = refreshClock(initial) ?: return@guardedPublish
                if (change.acknowledged) return@guardedPublish
                val remaining = clock()?.let { change.retrySecondsLeft(it) }
                    ?: (change.retryNotBefore.coerceAtLeast(0) - now().coerceAtLeast(0)).coerceAtLeast(0)
                if (change.blockedReason != null || remaining > 0) {
                    val retryAt = now().coerceIn(0, Long.MAX_VALUE - remaining) + remaining
                    throw CitySyncWaitingException(change.blockedReason, retryAt)
                }
                guardedPublish {
                    val reserved = reserve(change, now()) ?: return@guardedPublish
                    try {
                        guardedPublish {
                            val completed = withTimeoutOrNull(timeoutMillis) { send(reserved); true }
                            if (completed != true) throw CitySyncTimeoutException()
                        }
                    }
                    catch (error: CancellationException) { throw error }
                    catch (error: Exception) {
                        // A timeout has an uncertain remote outcome; keep the durable preflight fence.
                        if (error !is CitySyncTimeoutException) {
                            guardedPublish { recordFailure(reserved, error) }
                        }
                        throw error
                    }
                    guardedPublish { acknowledge(reserved) }
                }
            }
        }
    }
}
