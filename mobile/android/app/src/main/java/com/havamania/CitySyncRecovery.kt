package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

internal data class CitySyncRecoveryWork(val change: CitySyncChange, val defaultCity: Boolean = false)

/** One sequential account-owned loop; it neither clears blocks nor bypasses retry clocks. */
internal suspend fun runCitySyncRecovery(
    owner: String, currentOwner: () -> String, isAllowed: () -> Boolean,
    read: suspend () -> List<CitySyncRecoveryWork>, clock: () -> CitySyncClock,
    send: suspend (CitySyncRecoveryWork) -> Unit,
    onFailure: (Exception) -> Unit,
    pause: suspend (Long) -> Unit = { delay(it) }
) {
    if (owner == "legacy") return
    suspend fun report(error: Exception) {
        currentCoroutineContext().ensureActive()
        if (owner == currentOwner() && isAllowed()) onFailure(error)
    }
    while (owner == currentOwner()) {
        currentCoroutineContext().ensureActive()
        var nextSeconds = 30L
        if (isAllowed()) {
            val items = try { read() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                report(error)
                // A corrupt/unknown queue must not cause an automatic retry/error loop.
                if (citySyncFailure(error) != CitySyncFailure.TEMPORARY) return
                emptyList()
            }
            for (item in items) {
                currentCoroutineContext().ensureActive()
                if (owner != currentOwner()) return
                if (!isAllowed()) break
                if (item.change.acknowledged || item.change.blockedReason != null) continue
                val remaining = item.change.retrySecondsLeft(clock())
                if (remaining > 0) { nextSeconds = minOf(nextSeconds, remaining); continue }
                try { send(item) }
                catch (error: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    if (owner != currentOwner()) return
                    // An individual attempt may cancel after a transient gate closes.
                    // The account job is still active: pause and reread rather than killing recovery.
                    break
                }
                catch (error: Exception) { report(error) }
            }
        }
        currentCoroutineContext().ensureActive()
        if (owner != currentOwner()) return
        pause(nextSeconds * 1_000)
    }
}
