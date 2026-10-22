package com.havamania

import kotlinx.coroutines.*

/** Called on the main thread. A reset invalidates results even if a dependency ignores cancellation. */
internal class AssistantTaskRunner(
    private val scope: CoroutineScope,
    private val currentUid: () -> String,
    private val onBusy: (Boolean) -> Unit,
    private val timeoutMillis: Long = 60_000
) {
    private var generation = 0L
    private var job: Job? = null
    private var busy = false

    fun reset() {
        generation++
        job?.cancel()
        job = null
        busy = false
        onBusy(false)
    }

    fun <T> start(
        onStart: () -> Unit = {},
        operation: suspend (String) -> T,
        onSuccess: (T) -> Unit,
        onError: (Exception) -> Unit
    ): Boolean {
        if (busy) return false
        val owner = currentUid()
        val epoch = generation
        fun ownsRequest() = epoch == generation && owner == currentUid()
        busy = true // Reserve before dispatch, including greeting and local weather requests.
        onBusy(true)
        onStart()
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (!ownsRequest()) return@launch
                val result = withTimeout(timeoutMillis) { operation(owner) }
                ensureActive()
                if (ownsRequest()) onSuccess(result)
            } catch (e: TimeoutCancellationException) {
                if (ownsRequest()) onError(Exception("Yanıt zaman aşımına uğradı. Tekrar deneyebilirsiniz."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ensureActive()
                if (ownsRequest()) onError(e)
            } finally {
                // An old request must not clear the loading state of its replacement.
                if (epoch == generation) {
                    busy = false
                    onBusy(false)
                }
            }
        }
        job?.start()
        return true
    }
}
