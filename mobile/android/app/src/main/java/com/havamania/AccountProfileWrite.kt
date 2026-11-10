package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Fence every suspension boundary, including the final success boundary. */
internal suspend fun runAccountProfileWrite(
    owner: String, currentOwner: () -> String?, writesAllowed: () -> Boolean,
    vararg steps: suspend () -> Unit
) {
    suspend fun check() {
        currentCoroutineContext().ensureActive()
        if (owner != currentOwner() || !writesAllowed()) throw CancellationException("Profile write superseded")
    }
    for (step in steps) { check(); step() }
    check()
}
