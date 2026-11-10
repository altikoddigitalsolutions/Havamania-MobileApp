package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Listener identity matters even after signing back into the same account. */
internal suspend fun applyCitySnapshot(
    owner: String, listenerVersion: Long,
    currentOwner: () -> String, currentVersion: () -> Long,
    save: suspend () -> Unit, publish: () -> Unit
) {
    suspend fun isCurrent(): Boolean {
        currentCoroutineContext().ensureActive()
        return owner == currentOwner() && listenerVersion == currentVersion()
    }
    if (!isCurrent()) return
    save()
    if (isCurrent()) publish()
}
