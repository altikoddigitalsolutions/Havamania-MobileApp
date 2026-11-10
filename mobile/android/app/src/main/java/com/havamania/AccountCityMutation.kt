package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes list edits and fences each phase against a superseded session. */
internal suspend fun mutateAccountCities(
    lock: Mutex, owner: String, currentOwner: () -> String,
    local: suspend () -> Boolean, remote: suspend () -> Unit,
    onFailure: suspend (Exception) -> Unit
) {
    try {
        var changed = false
        lock.withLock {
            publishAccountResult(owner, currentOwner) {
                changed = local()
            }
        }
        // An offline Firestore acknowledgement must not block further local edits.
        if (changed) publishAccountResult(owner, currentOwner) { remote() }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        publishAccountResult(owner, currentOwner) { onFailure(error) }
    }
}
