package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Apply a delayed local result only while its exact account/selection/data is still current. */
internal suspend fun <T> resolveLatestWeatherWork(
    isCurrent: () -> Boolean,
    load: suspend () -> T,
    publish: (T) -> Unit
): Boolean {
    currentCoroutineContext().ensureActive()
    if (!isCurrent()) return false
    val value = try { load() } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        return false // Optional local enrichment must not fail the primary weather load.
    }
    currentCoroutineContext().ensureActive()
    if (!isCurrent()) return false
    publish(value)
    return true
}
