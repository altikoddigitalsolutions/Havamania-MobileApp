package com.havamania

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Cancels account work on session changes and captures its owner before dispatch. */
internal class AccountTaskScope(private val parent: CoroutineScope, private val currentUid: () -> String) {
    @Volatile private var generation = 0L
    private var job = SupervisorJob(parent.coroutineContext[Job])

    @Synchronized fun reset() {
        generation++
        job.cancel()
        job = SupervisorJob(parent.coroutineContext[Job])
    }

    @Synchronized fun close() { job.cancel() }

    @Synchronized fun launch(context: CoroutineContext = EmptyCoroutineContext,
               block: suspend CoroutineScope.(String) -> Unit): Job {
        val uid = currentUid()
        val epoch = generation
        return CoroutineScope(parent.coroutineContext + job).launch(context) {
            if (epoch == generation && uid == currentUid()) block(uid)
        }
    }
}
