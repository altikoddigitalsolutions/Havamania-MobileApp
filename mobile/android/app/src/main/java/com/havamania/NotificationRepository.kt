package com.havamania

import android.util.Log
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    private val TAG = "NotificationRepo"

    val allNotifications: Flow<List<NotificationItem>> = dao.getAllNotifications()
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    suspend fun insert(notification: NotificationItem) {
        try {
            dao.insert(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert notification", e)
        }
    }

    suspend fun markAsRead(ids: List<String>) {
        try {
            if (ids.isNotEmpty()) dao.markAsRead(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as read: $ids", e)
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
}
