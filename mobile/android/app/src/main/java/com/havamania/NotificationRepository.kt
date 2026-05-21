package com.havamania

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepository(private val dao: NotificationDao) {
    private val TAG = "NotificationRepo"

    val allNotifications: Flow<List<NotificationItem>> = dao.getAllNotifications().map { list ->
        if (list.isEmpty()) {
            Log.d(TAG, "Flow emission: List is empty. Returning DefaultNotifications.")
            DefaultNotifications.create()
        } else {
            list
        }
    }
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    suspend fun insert(notification: NotificationItem) {
        try {
            dao.insert(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert notification", e)
        }
    }

    suspend fun markAsRead(id: String) {
        try {
            dao.markAsRead(listOf(id))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as read: $id", e)
        }
    }

    suspend fun markAsRead(ids: List<String>) {
        try {
            if (ids.isNotEmpty()) dao.markAsRead(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as read: $ids", e)
        }
    }

    suspend fun toggleReadStatus(id: String, currentRead: Boolean) {
        try {
            if (currentRead) dao.markAsUnread(listOf(id))
            else dao.markAsRead(listOf(id))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle read status: $id", e)
        }
    }

    suspend fun markAsUnread(ids: List<String>) {
        try {
            if (ids.isNotEmpty()) dao.markAsUnread(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as unread: $ids", e)
        }
    }

    suspend fun markAllAsRead() {
        try {
            dao.markAllAsRead()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark all as read", e)
        }
    }

    suspend fun delete(id: String) {
        try {
            dao.delete(listOf(id))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete: $id", e)
        }
    }

    suspend fun delete(ids: List<String>) {
        try {
            if (ids.isNotEmpty()) dao.delete(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete: $ids", e)
        }
    }

    suspend fun deleteAll() {
        try {
            dao.deleteAll()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all", e)
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        try {
            val count = dao.getTotalCountFirst()
            Log.d(TAG, "🔍 SEED CHECK: Total notifications in DB: $count")

            if (count == 0) {
                Log.d("Notifications", "🚀 SEEDING: Database is empty. Generating 14 test notifications...")
                val demoList = createSeedNotifications()
                Log.d("Notifications", "seed created size=${demoList.size}")

                demoList.forEach {
                    dao.insert(it)
                    Log.v(TAG, "✅ SEEDED: ${it.id} (${it.category})")
                }

                val checkCount = dao.getTotalCountFirst()
                Log.d(TAG, "📊 SEED DONE: New count: $checkCount")
            } else {
                Log.d(TAG, "⏩ SEED SKIP: Database already has items.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SEED ERROR: Critical failure during seeding", e)
        }
    }

    private fun createSeedNotifications(): List<NotificationItem> {
        val demoList = mutableListOf<NotificationItem>()
        val now = System.currentTimeMillis()

        NotificationCategory.entries.forEach { category ->
            val label = category.label
            val (target, action) = when(category) {
                NotificationCategory.TRAVEL -> "havamania://app/calendar" to NotificationActionType.TRAVEL_CALENDAR
                NotificationCategory.RAIN, NotificationCategory.WARNING -> "havamania://app/weather" to NotificationActionType.WEATHER_HOME
                NotificationCategory.UV -> "havamania://app/weather" to NotificationActionType.WEATHER_HOME
                NotificationCategory.SUMMARY -> "havamania://app/ai" to NotificationActionType.WEEKLY_SUMMARY
                NotificationCategory.UPDATE -> "havamania://app/profile" to NotificationActionType.APP_UPDATE
                NotificationCategory.SYSTEM -> "havamania://app/settings" to NotificationActionType.NONE
                else -> "havamania://app/weather" to NotificationActionType.WEATHER_HOME
            }

            // 1. Yeni (Okunmamış) bildirim
            demoList.add(NotificationItem(
                id = "seed_${category.name}_new",
                title = "$label Analizi Hazır",
                message = "Havamania AI sistemleri $label kategorisinde senin için yeni bir veri seti derledi. Detaylara dokunarak ulaşabilirsin.",
                category = category,
                isRead = false,
                actionType = action,
                createdAt = now - (1000 * 60 * 10), // 10 dk önce
                deepLinkTarget = target,
                actionLabel = "İncele"
            ))

            // 2. Eski (Okunmuş) bildirim
            demoList.add(NotificationItem(
                id = "seed_${category.name}_old",
                title = "Geçmiş $label Raporu",
                message = "Dünkü $label verileri arşivlendi. Haftalık özetin içinde bu verilerin etkisini görebilirsin.",
                category = category,
                isRead = true,
                actionType = action,
                createdAt = now - (1000 * 60 * 60 * 5), // 5 saat önce
                deepLinkTarget = target,
                actionLabel = "Özeti Aç"
            ))
        }
        return demoList
    }

    suspend fun refreshDemoNotifications() {
        try {
            Log.d(TAG, "MANUAL REFRESH of demo notifications requested")
            dao.deleteAll()
            val demoList = createSeedNotifications()
            demoList.forEach { dao.insert(it) }
            Log.d(TAG, "Manual seed notifications inserted: ${demoList.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh demo notifications", e)
        }
    }
}
