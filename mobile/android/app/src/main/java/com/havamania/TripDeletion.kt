package com.havamania

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext

internal suspend fun deleteTripWithAcknowledgement(
    remoteDelete: suspend () -> Unit,
    localDelete: suspend () -> Unit
) {
    withTimeout(30_000) { remoteDelete() }
    coroutineContext.ensureActive()
    localDelete()
}


/** Shared by all travel ViewModels in this process. */
internal object TripMutationGate {
    private val mutex = kotlinx.coroutines.sync.Mutex()
    suspend fun <T> run(operation: suspend () -> T): T {
        mutex.lock()
        try { return operation() } finally { mutex.unlock() }
    }
}
