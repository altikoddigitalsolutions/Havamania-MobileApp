package com.havamania

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Main-thread confined. Reserves before dispatch and invalidates late callbacks before cancellation. */
internal class AuthOperationRunner(
    private val scope: CoroutineScope,
    private val onStart: () -> Unit,
    private val onFailure: (Exception) -> Unit,
    private val onCancelled: () -> Unit
) {
    private var ticket: Any? = null
    private var job: Job? = null
    private var closed = false
    val busy: Boolean get() = ticket != null

    fun start(onSuccess: () -> Unit, work: suspend () -> Unit): Boolean {
        if (closed || busy || scope.coroutineContext[Job]?.isActive == false) return false
        val current = Any()
        ticket = current
        onStart()
        val pending = scope.launch(start = CoroutineStart.LAZY) {
            try {
                work()
                coroutineContext.ensureActive()
                if (ticket === current) onSuccess()
            } catch (cancelled: CancellationException) {
                if (ticket === current) onCancelled()
                throw cancelled
            } catch (failure: Exception) {
                if (ticket === current) onFailure(failure)
            } finally {
                if (ticket === current) {
                    ticket = null
                    job = null
                }
            }
        }
        job = pending
        pending.start()
        return true
    }

    fun cancel() {
        val previous = job
        ticket = null
        job = null
        previous?.cancel()
    }

    fun close() {
        closed = true
        cancel()
    }
}