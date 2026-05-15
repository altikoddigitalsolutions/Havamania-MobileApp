package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class NotificationType {
    TRAVEL, RAIN, UV, SUMMARY, WARNING, UPDATE, GENERAL, OTHER
}

enum class NotificationFilter {
    ALL, UNREAD, TRAVEL, RAIN, UV, WARNING, SUMMARY, UPDATE, GENERAL
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val timeText: String,
    val isRead: Boolean = false,
    val actionText: String? = null
)

fun NotificationType.getIcon(): ImageVector {
    return when (this) {
        NotificationType.TRAVEL -> Icons.Rounded.Route
        NotificationType.RAIN -> Icons.Rounded.WaterDrop
        NotificationType.UV -> Icons.Rounded.WbSunny
        NotificationType.WARNING -> Icons.Rounded.Warning
        NotificationType.SUMMARY -> Icons.AutoMirrored.Rounded.Article
        NotificationType.UPDATE -> Icons.Rounded.SystemUpdate
        NotificationType.GENERAL -> Icons.Rounded.Info
        NotificationType.OTHER -> Icons.Rounded.Notifications
    }
}
