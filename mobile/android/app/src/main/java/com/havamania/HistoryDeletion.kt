package com.havamania

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Retain the local copy until the remote deletion is acknowledged. Retry is idempotent. */
internal suspend fun deleteHistoryWithAcknowledgement(
    remoteDelete: suspend () -> Unit,
    localDelete: suspend () -> Unit
) {
    remoteDelete()
    coroutineContext.ensureActive()
    localDelete()
}

/** Serializes history mutations across ViewModel instances in this process. */
internal object HistoryOperationLock { val mutex = kotlinx.coroutines.sync.Mutex() }
