package com.havamania

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun postNotificationIfCurrent(
    owner: String, epoch: Long, currentOwner: () -> String?, blocked: () -> Boolean,
    post: () -> Unit
): Boolean {
    val context = currentCoroutineContext()
    context.ensureActive()
    val job = context[Job]
    return NotificationSessionEpoch.publish(owner, epoch, currentOwner,
        { job?.isActive == false || blocked() }, post)
}
