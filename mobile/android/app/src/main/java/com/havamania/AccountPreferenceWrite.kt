package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal enum class SettingsIssue { LOAD, SAVE }

/** Write only the captured owner; never publish a late result to another session. */
internal suspend fun persistAccountPreference(
    owner: String, currentOwner: () -> String,
    write: suspend (String) -> Unit, onApplied: () -> Unit, onFailure: () -> Unit
) {
    currentCoroutineContext().ensureActive()
    if (owner != currentOwner()) return
    try {
        write(owner)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        publishAccountResult(owner, currentOwner) { onFailure() }
        return
    }
    publishAccountResult(owner, currentOwner) { onApplied() }
}
