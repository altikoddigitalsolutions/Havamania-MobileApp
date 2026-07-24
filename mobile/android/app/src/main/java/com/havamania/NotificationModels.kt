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

enum class NotificationActionType {
    WEATHER_HOME,
    TRAVEL_CALENDAR,
    TRAVEL_DETAIL,
    HOURLY_FORECAST,
    DAILY_FORECAST,
    UV_DETAIL,
    WEATHER_ALERT,
    WEEKLY_SUMMARY,
    APP_UPDATE,
    NONE
}

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "legacy",
    val title: String = "",
    val message: String = "",
    val category: NotificationCategory = NotificationCategory.GENERAL,
    val createdAt: Long = System.currentTimeMillis(),
    val eventAt: Long? = null,
    val isRead: Boolean = false,
    val actionType: NotificationActionType = NotificationActionType.NONE,
    val targetId: String? = null,
    val deepLinkTarget: String? = null,
    val relatedTripId: String? = null,
    val actionLabel: String? = null,
    val deduplicationKey: String? = null,
    val severity: String = "NORMAL", // NORMAL, HIGH
    val travelData: TravelNotificationData? = null
) {
    fun getSafeId(): String = id.ifBlank { UUID.randomUUID().toString() }
    fun getSafeTitle(): String = title.ifBlank { "Bildirim" }
    fun getSafeMessage(): String = message.ifBlank { "İçerik mevcut değil." }
}

enum class NotificationFilter {
    ALL, UNREAD, TRAVEL, RAIN, UV, WARNING, SUMMARY, UPDATE, GENERAL, SYSTEM
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
