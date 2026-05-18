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

    suspend fun refreshDemoNotifications() {
        try {
            dao.deleteAll()
            val demoList = listOf(
                NotificationItem(
                    title = "Batman Seyahat Analizi Hazır",
                    message = "Batman seyahatin için güncel hava analizi hazır. Yağmur ihtimali yüksek görünüyor.",
                    category = NotificationCategory.TRAVEL,
                    actionLabel = "Seyahati Aç"
                ),
                NotificationItem(
                    title = "Yağmur Başlıyor",
                    message = "Bulunduğun konumda 15 dakika içinde hafif yağış başlayacak.",
                    category = NotificationCategory.RAIN,
                    actionLabel = "Saatlik Tahmini Aç"
                ),
                NotificationItem(
                    title = "Yüksek UV Endeksi",
                    message = "Bugün UV seviyesi 7. Öğle saatlerinde güneş kremi ve şapka önerilir.",
                    category = NotificationCategory.UV,
                    actionLabel = "UV Detayları"
                ),
                NotificationItem(
                    title = "Kritik Hava Uyarısı",
                    message = "Önümüzdeki 3 saat içinde kuvvetli rüzgar bekleniyor. Dışarıdaki eşyalarını kontrol et.",
                    category = NotificationCategory.WARNING,
                    actionLabel = "Uyarıyı Aç"
                ),
                NotificationItem(
                    title = "Uygulama Güncellendi",
                    message = "Bildirim Merkezi, seyahat analizleri ve premium hava kartları iyileştirildi.",
                    category = NotificationCategory.UPDATE,
                    actionLabel = "Yenilikleri Gör"
                )
            )
            demoList.forEach { dao.insert(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh demo notifications", e)
        }
    }
}
