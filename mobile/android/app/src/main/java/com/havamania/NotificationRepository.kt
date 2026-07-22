package com.havamania

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NotificationRepository(private val dao: NotificationDao) {
    private val TAG = "NotificationRepo"

    fun getAllNotificationsFlow(uid: String): Flow<List<NotificationItem>> = dao.getAllNotifications(uid).map { list ->
        if (list.isEmpty() && uid == "legacy") {
            Log.d(TAG, "Legacy user list is empty. Returning DefaultNotifications.")
            DefaultNotifications.create(uid)
        } else {
            list
        }
    }

    fun getUnreadCount(uid: String): Flow<Int> = dao.getUnreadCount(uid)

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

    suspend fun markAllAsRead(uid: String) {
        try {
            dao.markAllAsRead(uid)
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

    suspend fun deleteAll(uid: String) {
        try {
            dao.deleteAll(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all", e)
        }
    }

    suspend fun deleteByCategory(uid: String, category: NotificationCategory) {
        try {
            dao.deleteByCategory(uid, category.name)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete by category: $category", e)
        }
    }

    suspend fun seedInitialDataIfNeeded(context: android.content.Context, uid: String) {
        try {
            // New logic: If user is authenticated, they should start clean unless they migrated.
            // For now, let's keep seeding only for "legacy" or if explicitly asked.
            if (uid != "legacy") {
                Log.d(TAG, "🚀 SEED SKIP: Authenticated user $uid starts clean.")
                return
            }

            val hasSeeded = com.havamania.ui.theme.ThemeManager.getHasSeededNotifications(context, uid).first()
            val count = dao.getTotalCountFirst(uid)
            Log.d(TAG, "🔍 SEED CHECK: Total notifications in DB for $uid: $count, hasSeeded: $hasSeeded")

            if (count == 0 && !hasSeeded) {
                Log.d("Notifications", "🚀 SEEDING: Database is empty. Generating default test notifications...")
                val demoList = DefaultNotifications.create(uid)
                Log.d("Notifications", "seed created size=${demoList.size}")

                demoList.forEach {
                    dao.insert(it)
                    Log.v(TAG, "✅ SEEDED: ${it.id} (${it.category})")
                }
                com.havamania.ui.theme.ThemeManager.saveHasSeededNotifications(context, true, uid)

                val checkCount = dao.getTotalCountFirst(uid)
                Log.d(TAG, "📊 SEED DONE: New count: $checkCount")
            } else {
                Log.d(TAG, "⏩ SEED SKIP: Database already has items or was already seeded.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SEED ERROR: Critical failure during seeding", e)
        }
    }

    suspend fun refreshDemoNotifications(uid: String) {
        try {
            Log.d(TAG, "MANUAL REFRESH of demo notifications requested")
            dao.deleteAll(uid)
            val demoList = DefaultNotifications.create(uid)
            demoList.forEach { dao.insert(it) }
            Log.d(TAG, "Manual seed notifications inserted: ${demoList.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh demo notifications", e)
        }
    }
}
