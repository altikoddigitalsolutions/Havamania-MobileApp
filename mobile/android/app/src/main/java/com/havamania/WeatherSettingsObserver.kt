package com.havamania

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay

internal suspend fun <T> observeCurrentWeatherSettings(
    settings: Flow<T>, isCurrent: () -> Boolean,
    onValue: (T) -> Unit, onUnavailable: () -> Unit,
    waitBeforeRetry: suspend (Long) -> Unit = { delay(it) }
) {
    settings.distinctUntilChanged().retryWhen { error, attempt ->
        if (error is CancellationException) throw error
        currentCoroutineContext().ensureActive()
        if (error !is java.io.IOException || attempt >= 2 || !isCurrent()) return@retryWhen false
        onUnavailable()
        waitBeforeRetry((attempt + 1) * 1000)
        currentCoroutineContext().ensureActive()
        isCurrent()
    }.catch { error ->
        if (error is CancellationException) throw error
        currentCoroutineContext().ensureActive()
        if (isCurrent()) onUnavailable()
    }.collect { value ->
        resolveLatestWeatherWork(isCurrent, { value }, onValue)
    }
}
