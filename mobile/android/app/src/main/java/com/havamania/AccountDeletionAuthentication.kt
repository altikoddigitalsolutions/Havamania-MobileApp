package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Only preparation is cancellable by session changes; an already accepted deletion keeps its journal. */
internal suspend fun <T : Any> prepareDeletionOwner(
    owner: String, currentOwner: () -> String?, fence: AuthSessionFence,
    available: suspend () -> Boolean, reauthenticate: suspend () -> Unit, token: suspend () -> T
): T? {
    val lease = fence.ownerLease(owner)
    val availability = available()
    currentCoroutineContext().ensureActive()
    check(currentOwner() == owner) { "Deletion owner changed" }
    fence.verifyOwnerLease(owner, lease)
    if (!availability) return null
    reauthenticate()
    return awaitDeletionOwnerToken(owner, currentOwner, fence, token)
}

internal suspend fun <T> awaitDeletionOwnerToken(
    owner: String, currentOwner: () -> String?, fence: AuthSessionFence, token: suspend () -> T
): T {
    currentCoroutineContext().ensureActive()
    check(currentOwner() == owner) { "Deletion owner changed" }
    val lease = fence.ownerLease(owner)
    val value = token()
    currentCoroutineContext().ensureActive()
    check(currentOwner() == owner) { "Deletion owner changed" }
    fence.verifyOwnerLease(owner, lease)
    return value
}
