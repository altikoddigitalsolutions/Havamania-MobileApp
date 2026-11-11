package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/** One recovery only for an early empty result, within the caller's single overall deadline. */
internal suspend fun <T : Any> recoverEarlyEmptyPosition(
    request: suspend () -> T?, canRetry: () -> Boolean, elapsedMillis: () -> Long,
    waitBeforeRetry: suspend () -> Unit = { delay(1500) }, onRetry: () -> Unit = {}
): T? {
    val started = elapsedMillis()
    val first = request()
    currentCoroutineContext().ensureActive()
    if (first != null || elapsedMillis() - started !in 0L..2000L || !canRetry()) return first
    waitBeforeRetry()
    currentCoroutineContext().ensureActive()
    if (!canRetry()) return null
    onRetry()
    val second = request()
    currentCoroutineContext().ensureActive()
    return second
}
