package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal enum class PreferenceWriteResult { SAVED, OBSOLETE, FAILED }
internal suspend fun writeOwnedPreference(owner: String, isCurrent: () -> Boolean,
    write: suspend (String) -> Unit): PreferenceWriteResult {
    currentCoroutineContext().ensureActive()
    if (!isCurrent()) return PreferenceWriteResult.OBSOLETE
    try { write(owner) } catch (error: CancellationException) { throw error }
    catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        return if (isCurrent()) PreferenceWriteResult.FAILED else PreferenceWriteResult.OBSOLETE
    }
    currentCoroutineContext().ensureActive()
    return if (isCurrent()) PreferenceWriteResult.SAVED else PreferenceWriteResult.OBSOLETE
}
