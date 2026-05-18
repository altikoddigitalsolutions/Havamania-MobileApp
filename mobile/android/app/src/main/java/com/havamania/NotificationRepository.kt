package com.havamania

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.UUID

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCountFlow: StateFlow<Int> = _unreadCount.asStateFlow()

    private var deletedNotificationsBackup: List<AppNotification>? = null

    init {
        _notifications.value = createDefaultNotifications()
        updateUnreadCount()
    }

    fun markAsRead(id: String) {
        try {
            _notifications.update { currentList ->
                currentList.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
            }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking as read", e)
        }
    }

    fun markAsUnread(id: String) {
        try {
            _notifications.update { currentList ->
                currentList.map {
                    if (it.id == id) it.copy(isRead = false) else it
                }
            }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking as unread", e)
        }
    }

    fun toggleReadStatus(id: String) {
        try {
            _notifications.update { currentList ->
                currentList.map {
                    if (it.id == id) it.copy(isRead = !it.isRead) else it
                }
            }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error toggling read status", e)
        }
    }

    fun markMultipleAsRead(ids: Set<String>) {
        try {
            _notifications.update { currentList ->
                currentList.map {
                    if (ids.contains(it.id)) it.copy(isRead = true) else it
                }
            }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking multiple as read", e)
        }
    }

    fun deleteNotification(id: String) {
        try {
            val list = _notifications.value
            deletedNotificationsBackup = list.filter { it.id == id }
            _notifications.update { it.filter { item -> item.id != id } }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error deleting notification", e)
        }
    }

    fun deleteMultiple(ids: Set<String>) {
        try {
            val list = _notifications.value
            deletedNotificationsBackup = list.filter { ids.contains(it.id) }
            _notifications.update { it.filter { item -> !ids.contains(item.id) } }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error deleting multiple notifications", e)
        }
    }

    fun undoDelete(): Boolean {
        return try {
            val backup = deletedNotificationsBackup
            if (backup != null) {
                _notifications.update { (backup + it).distinctBy { item -> item.id } }
                updateUnreadCount()
                deletedNotificationsBackup = null
                true
            } else false
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error undoing delete", e)
            false
        }
    }

    fun markAllAsRead() {
        try {
            _notifications.update { currentList ->
                currentList.map { it.copy(isRead = true) }
            }
            updateUnreadCount()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error marking all as read", e)
        }
    }

    fun clearAll() {
        deletedNotificationsBackup = _notifications.value
        _notifications.value = emptyList()
        updateUnreadCount()
    }

    fun refreshDemoNotifications() {
        _notifications.value = createDefaultNotifications()
        updateUnreadCount()
    }

    private fun updateUnreadCount() {
        _unreadCount.value = _notifications.value.count { !it.isRead }
    }

    private fun createDefaultNotifications(): List<AppNotification> {
        return listOf(
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Batman Seyahat Analizi Hazır",
                message = "Batman seyahatin için güncel hava analizi hazır. Yağmur ihtimali yüksek görünüyor.",
                type = NotificationType.TRAVEL,
                timeText = "09:31",
                isRead = false,
                actionText = "Seyahati Aç"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Londra Seyahati: Yağmur Uyarısı",
                message = "Hafta sonu Londra seyahatin için yağmur ihtimali %80. Şemsiyeni bavula eklemeyi unutma.",
                type = NotificationType.TRAVEL,
                timeText = "12:45",
                isRead = false,
                actionText = "Analizi Aç"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Yağmur Başlıyor",
                message = "Bulunduğun konumda 15 dakika içinde hafif yağış başlayacak.",
                type = NotificationType.RAIN,
                timeText = "10:15",
                isRead = false,
                actionText = "Saatlik Tahmini Aç"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Gece Yağışı Bekleniyor",
                message = "Saat 22:00 sonrası yağmur ihtimali artıyor. Dışarı çıkacaksan hazırlıklı ol.",
                type = NotificationType.RAIN,
                timeText = "Dün",
                isRead = true,
                actionText = "Yağmur Detayı"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Yüksek UV Endeksi",
                message = "Bugün UV seviyesi 7. Öğle saatlerinde güneş kremi ve şapka önerilir.",
                type = NotificationType.UV,
                timeText = "Bugün",
                isRead = false,
                actionText = "UV Detayları"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Kritik Hava Uyarısı",
                message = "Önümüzdeki 3 saat içinde kuvvetli rüzgar bekleniyor. Dışarıdaki eşyalarını kontrol et.",
                type = NotificationType.WARNING,
                timeText = "08:20",
                isRead = false,
                actionText = "Uyarıyı Aç"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Haftalık Özet Hazır",
                message = "Bu hafta hava trendleri ve sana özel aktivite önerileri hazır.",
                type = NotificationType.SUMMARY,
                timeText = "Pazartesi",
                isRead = true,
                actionText = "Özeti Gör"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Havamania’dan İpucu",
                message = "Saatlik tahmin kartlarına dokunarak detaylı sıcaklık, yağış ve rüzgar bilgilerini görebilirsin.",
                type = NotificationType.GENERAL,
                timeText = "3 gün önce",
                isRead = true,
                actionText = "Detayları Gör"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Uygulama Güncellendi",
                message = "Bildirim Merkezi, seyahat analizleri ve premium hava kartları iyileştirildi.",
                type = NotificationType.UPDATE,
                timeText = "Yeni",
                isRead = false,
                actionText = "Yenilikleri Gör"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Ani Sıcaklık Düşüşü",
                message = "Akşam saatlerinde sıcaklık belirgin şekilde düşebilir. Yanına ince bir ceket al.",
                type = NotificationType.WARNING,
                timeText = "19:00",
                isRead = false,
                actionText = "Detayları Gör"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Güneşli Saatler",
                message = "Bugün 12:00–15:00 arası güneş etkisi yüksek. Açık hava planlarını buna göre yap.",
                type = NotificationType.UV,
                timeText = "11:30",
                isRead = false,
                actionText = "Güneş Detayı"
            ),
            AppNotification(
                id = UUID.randomUUID().toString(),
                title = "Günlük Plan Önerisi",
                message = "Bugünkü hava yürüyüş ve kısa açık hava aktiviteleri için uygun görünüyor.",
                type = NotificationType.SUMMARY,
                timeText = "07:45",
                isRead = true,
                actionText = "Planı Aç"
            )
        )
    }
}
