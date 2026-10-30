package com.havamania

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Same UUIDv5 identity as backend Firebase authentication; project is local app config. */
internal fun firebaseBackendUserId(project: String, uid: String): String {
    val namespace = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
    val bytes = ByteBuffer.allocate(16).putLong(namespace.mostSignificantBits)
        .putLong(namespace.leastSignificantBits).array()
    val digest = MessageDigest.getInstance("SHA-1").apply {
        update(bytes)
        update("https://securetoken.google.com/$project/$uid".toByteArray(Charsets.UTF_8))
    }.digest().copyOf(16)
    digest[6] = ((digest[6].toInt() and 0x0f) or 0x50).toByte()
    digest[8] = ((digest[8].toInt() and 0x3f) or 0x80).toByte()
    val buffer = ByteBuffer.wrap(digest)
    return UUID(buffer.long, buffer.long).toString()
}

internal data class FcmEnvelope(val eventId: String, val title: String, val body: String, val expiresAt: Long)

internal fun validatedFcmEnvelope(data: Map<String, String>, project: String?, uid: String?,
    deletionPending: Boolean, nowSeconds: Long): FcmEnvelope? {
    if (project.isNullOrBlank() || !canOpenOwnedNotification(uid, uid, deletionPending)) return null
    if (data["protocol"] != "1" || data["owner_backend_user_id"] != firebaseBackendUserId(project, uid!!)) return null
    if (data.entries.sumOf { it.key.toByteArray(Charsets.UTF_8).size + it.value.toByteArray(Charsets.UTF_8).size } > 4096) return null
    val event = data["event_id"] ?: return null
    if (!event.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) return null
    val title = data["title"]?.takeIf { it.isNotBlank() && it.length <= 200 && it.none(Char::isISOControl) } ?: return null
    val body = data["body"]?.takeIf { it.isNotBlank() && it.length <= 1000 && '\u0000' !in it } ?: return null
    val expires = data["expires_at"]?.toLongOrNull() ?: return null
    if (expires <= nowSeconds || expires > nowSeconds + 86400) return null
    if (data["target"] != "weather") return null
    return FcmEnvelope(event, title, body, expires)
}

internal object NotificationSessionEpoch {
    private val epoch = java.util.concurrent.atomic.AtomicLong()
    fun capture(): Long = epoch.get()
    fun changed() { epoch.incrementAndGet() }
}

internal suspend fun persistFcmRecord(uid: String, envelope: FcmEnvelope, ownsSession: () -> Boolean,
    insert: suspend (NotificationItem) -> Long, delete: suspend (String, List<String>) -> Unit): NotificationItem? {
    currentCoroutineContext().ensureActive()
    if (!ownsSession()) return null
    val item = NotificationItem(id = "fcm_${uid}_${envelope.eventId}", userId = uid,
        title = envelope.title, message = envelope.body, category = NotificationCategory.WARNING,
        severity = "HIGH", deduplicationKey = "fcm_${envelope.eventId}",
        actionType = NotificationActionType.WEATHER_HOME)
    if (insert(item) == -1L) return null
    currentCoroutineContext().ensureActive()
    if (!ownsSession()) {
        delete(uid, listOf(item.id))
        return null
    }
    return item
}
