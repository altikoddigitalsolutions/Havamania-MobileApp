package com.havamania

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
    GENERAL("Genel")
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
    val actionLabel: String? = null
) {
    fun getSafeId(): String = id.ifBlank { UUID.randomUUID().toString() }
    fun getSafeTitle(): String = title.ifBlank { "Bildirim" }
    fun getSafeMessage(): String = message.ifBlank { "İçerik mevcut değil." }
}
