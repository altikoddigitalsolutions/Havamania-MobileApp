package com.havamania

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val localNotificationDeliveryMutex = Mutex()

internal suspend fun <T> withNotificationDeliveryLock(block: suspend () -> T): T =
    localNotificationDeliveryMutex.withLock { block() }

internal enum class LocalNotificationDelivery { POSTED, ALREADY_RECORDED, SKIPPED }

/** Reservations persist until native post is acknowledged or the owned attempt is released. */
internal suspend fun deliverLocalNotification(
    item: NotificationItem,
    reserve: suspend (NotificationItem) -> Long,
    delete: suspend (String, List<String>) -> Unit,
    complete: suspend (NotificationItem) -> Unit = {},
    recovery: NotificationDeliveryRecovery? = null,
    publish: suspend (NotificationItem) -> Boolean
): LocalNotificationDelivery = localNotificationDeliveryMutex.withLock {
    currentCoroutineContext().ensureActive()
    recovery?.retryForOwner(item.userId)
    val reservation = reserve(item)
    if (reservation == 0L) return@withLock LocalNotificationDelivery.ALREADY_RECORDED
    if (reservation < 0L) return@withLock LocalNotificationDelivery.SKIPPED
    var posted = false
    var primaryFailure: Throwable? = null
    try {
        currentCoroutineContext().ensureActive()
        posted = publish(item)
        if (posted) withContext(NonCancellable) {
            if (recovery == null) complete(item)
            else recovery.runOrRemember(item.userId, item.id) { complete(item) }
        }
        return@withLock if (posted) LocalNotificationDelivery.POSTED else LocalNotificationDelivery.SKIPPED
    } catch (error: Throwable) {
        primaryFailure = error
        throw error
    } finally {
        if (!posted) cleanupNotificationPreservingFailure(primaryFailure) {
            withContext(NonCancellable) {
                if (recovery == null) delete(item.userId, listOf(item.id))
                else recovery.runOrRemember(item.userId, item.id) { delete(item.userId, listOf(item.id)) }
            }
        }
    }
}
