package com.havamania

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class NotificationCategory(val label: String) {
    TRAVEL("Seyahat"),
    RAIN("Yağmur"),
    UV("UV"),
    SUMMARY("Özet"),
    WARNING("Uyarı"),
    SYSTEM("Sistem"),
    GENERAL("Genel"),
    UPDATE("Güncelleme")
}

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val deepLinkTarget: String? = null,
    val relatedTripId: String? = null,
    val actionLabel: String? = null,
    val travelData: TravelNotificationData? = null
) {
    fun getSafeId(): String = id.ifBlank { UUID.randomUUID().toString() }
    fun getSafeTitle(): String = title.ifBlank { "Bildirim" }
    fun getSafeMessage(): String = message.ifBlank { "İçerik mevcut değil." }
}

enum class NotificationFilter {
    ALL, UNREAD, TRAVEL, RAIN, UV, WARNING, SUMMARY, UPDATE, GENERAL
}

fun NotificationCategory.getIcon(): ImageVector {
    return when (this) {
        NotificationCategory.TRAVEL -> Icons.Rounded.Route
        NotificationCategory.RAIN -> Icons.Rounded.WaterDrop
        NotificationCategory.UV -> Icons.Rounded.WbSunny
        NotificationCategory.WARNING -> Icons.Rounded.Warning
        NotificationCategory.SUMMARY -> Icons.AutoMirrored.Rounded.Article
        NotificationCategory.UPDATE -> Icons.Rounded.SystemUpdate
        NotificationCategory.SYSTEM -> Icons.Rounded.Settings
        NotificationCategory.GENERAL -> Icons.Rounded.Info
    }
}
