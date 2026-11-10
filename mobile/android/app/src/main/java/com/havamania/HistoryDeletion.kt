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

/** Caller holds the history mutation lock while checking and finishing the queued request. */
internal suspend fun WeatherDao.finishQueuedHistoryDeletion(
    uid: String, recordId: String, finish: suspend () -> Unit
) {
    if (getHistoryDeletions(uid).any { it.recordId == recordId }) finish()
}
