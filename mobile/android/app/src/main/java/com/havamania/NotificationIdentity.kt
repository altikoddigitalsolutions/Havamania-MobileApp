package com.havamania

/** Length framing keeps different owner/event boundaries and Unicode input unambiguous. */
internal fun notificationEventRecordId(owner: String, kind: String, event: String): String {
    require(owner.isNotBlank() && kind.isNotBlank() && event.isNotBlank())
    val framed = listOf(owner, kind, event).joinToString("") { "${it.length}:$it" }
    return "event_" + accountOwnerHash(framed)
}

/** Android identifies a notification by (tag, id), not the database id's 32-bit hash. */
internal fun notificationSystemTag(item: NotificationItem): String {
    val key = item.deduplicationKey?.takeIf { it.isNotBlank() }
    return notificationEventRecordId(item.userId, if (key == null) "record" else "event", key ?: item.id)
}
