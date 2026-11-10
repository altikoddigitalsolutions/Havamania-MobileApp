package com.havamania

/** Equal text does not mean equal events: different trips/targets must remain reachable. */
internal fun notificationDisplayOrder(items: List<NotificationItem>): List<NotificationItem> =
    items.sortedByDescending { it.createdAt }
