package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun publishAccountResult(
    owner: String,
    currentOwner: () -> String,
    publish: suspend () -> Unit
) {
    // Cancellation also distinguishes logging out and back into the same UID.
    currentCoroutineContext().ensureActive()
    if (owner == currentOwner()) publish()
}
