package com.havamania

object DefaultNotifications {
    fun create(uid: String = "legacy"): List<NotificationItem> {
        val now = System.currentTimeMillis()
        val oneHour = 1000L * 60 * 60
        val oneDay = oneHour * 24

        return listOf(
            // SEYAHAT
            NotificationItem(
                id = "def_travel_1",
                userId = uid,
                title = "Seyahat Güncellemesi",
                message = "İzmir seyahatin için UV seviyesi yüksek görünüyor. Güneş koruyucu ve şapka almanı öneririm.",
                category = NotificationCategory.TRAVEL,
                isRead = false,
                actionType = NotificationActionType.TRAVEL_CALENDAR,
                createdAt = now - (1000 * 60 * 15),
                eventAt = now + (oneDay * 2) + (oneHour * 3), // 2 gün sonra
                deepLinkTarget = "havamania://app/calendar",
                actionLabel = "Planı Gör"
            ),
            NotificationItem(
                id = "def_travel_2",
                userId = uid,
                title = "Seyahat Güncellemesi",
                message = "Hasankeyf ve çevresindeki tarihi dokuyu keşfetmek için önümüzdeki günler oldukça elverişli.",
                category = NotificationCategory.TRAVEL,
                isRead = true,
                actionType = NotificationActionType.TRAVEL_CALENDAR,
                createdAt = now - (oneHour * 3),
                eventAt = now + (oneDay * 5), // 5 gün sonra
                deepLinkTarget = "havamania://app/calendar",
                actionLabel = "İncele"
            ),
            // YAĞMUR
            NotificationItem(
                id = "def_rain_1",
                userId = uid,
                title = "Yağış Bilgilendirmesi",
                message = "Yağış ihtimali %20'den %65'e yükseldi. Şemsiyeni yanına almalısın.",
                category = NotificationCategory.RAIN,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 45),
                eventAt = now + (oneHour * 4), // Bugün ilerleyen saatler
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Tahmini Gör"
            ),
            NotificationItem(
                id = "def_rain_2",
                userId = uid,
                title = "Yağış Bilgilendirmesi",
                message = "Hafif yağış geçişleri ve açık hava etkinliklerine uygun günler sizi bekliyor.",
                category = NotificationCategory.RAIN,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (oneHour * 8),
                eventAt = now + (oneDay * 3),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Detaylar"
            ),
            // UV
            NotificationItem(
                id = "def_uv_1",
                userId = uid,
                title = "UV Uyarısı",
                message = "UV indeksi 8 seviyesine ulaşacak. Koruyucu kullanmayı unutma.",
                category = NotificationCategory.UV,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (oneHour * 2),
                eventAt = now + (oneHour * 1), // Bugün
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "UV Detay"
            ),
            NotificationItem(
                id = "def_uv_2",
                userId = uid,
                title = "UV Uyarısı",
                message = "Açık hava aktiviteleri için hava koşulları ve UV seviyesi oldukça uygun görünüyor.",
                category = NotificationCategory.UV,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (oneHour * 12),
                eventAt = now + (oneDay * 1) + (oneHour * 2), // Yarın
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "İncele"
            ),
            // UYARI
            NotificationItem(
                id = "def_warn_1",
                userId = uid,
                title = "Hava Durumu Uyarısı",
                message = "Sıcaklık hızlı düşecek. Dışarıdaysan hafif ceket alman konforunu artıracaktır.",
                category = NotificationCategory.WARNING,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 5),
                eventAt = now + (oneHour * 3), // Bugün
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Sıcaklık Grafiği"
            ),
            NotificationItem(
                id = "def_warn_2",
                userId = uid,
                title = "Hava Durumu Uyarısı",
                message = "Kıyı kesimlerinde rüzgar hızı anlık 45 km/sa hıza ulaşabilir. Dikkatli olunmalıdır.",
                category = NotificationCategory.WARNING,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (oneHour * 2),
                eventAt = now + (oneHour * 5), // Bugün
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Rüzgar Detay"
            ),
            // ÖZET
            NotificationItem(
                id = "def_sum_1",
                userId = uid,
                title = "Hava Durumu Özeti",
                message = "Sabah sisli, öğleden sonra parçalı bulutlu bir gökyüzü bekleniyor.",
                category = NotificationCategory.SUMMARY,
                isRead = false,
                actionType = NotificationActionType.WEEKLY_SUMMARY,
                createdAt = now - (oneHour * 5),
                eventAt = now, // Bugün
                deepLinkTarget = "havamania://app/ai",
                actionLabel = "Özeti Aç"
            ),
            NotificationItem(
                id = "def_sum_2",
                userId = uid,
                title = "Hava Durumu Özeti",
                message = "Planladığın rotalar için en uygun hava koşulları yaklaşıyor.",
                category = NotificationCategory.SUMMARY,
                isRead = true,
                actionType = NotificationActionType.WEEKLY_SUMMARY,
                createdAt = now - oneDay,
                eventAt = now + (oneDay * 2),
                deepLinkTarget = "havamania://app/ai",
                actionLabel = "Analize Git"
            ),
            // GÜNCELLEME
            NotificationItem(
                id = "def_upd_1",
                userId = uid,
                title = "Uygulama Güncellemesi",
                message = "Hava kartlarına rüzgar şiddetine göre değişen premium animasyonlar eklendi. Hemen keşfet!",
                category = NotificationCategory.UPDATE,
                isRead = false,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - oneHour,
                eventAt = now - oneHour,
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "Yenilikler"
            ),
            NotificationItem(
                id = "def_upd_2",
                userId = uid,
                title = "Uygulama Güncellemesi",
                message = "Atmosferik efektler ve geçiş animasyonları daha düşük pil tüketimi için optimize edildi.",
                category = NotificationCategory.UPDATE,
                isRead = true,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - (oneDay * 2),
                eventAt = now - (oneDay * 2),
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "İncele"
            ),
            // GENEL
            NotificationItem(
                id = "def_gen_1",
                userId = uid,
                title = "Havamania'ya Hoş Geldin",
                message = "Tüm hava durumlarına özel astronomik fazlarla zenginleştirilmiş seyahat deneyimine hoş geldin.",
                category = NotificationCategory.GENERAL,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (oneDay * 3),
                eventAt = now - (oneDay * 3),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Kullanmaya Başla"
            ),
            NotificationItem(
                id = "def_gen_2",
                userId = uid,
                title = "Havamania",
                message = "Sana daha iyi öneriler sunabilmemiz için profilindeki ilgi alanlarını (kamp, spor, fotoğrafçılık vb.) güncel tutabilirsin.",
                category = NotificationCategory.GENERAL,
                isRead = true,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - (oneDay * 7),
                eventAt = now - (oneDay * 7),
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "Profilime Git"
            )
        )
    }
}
