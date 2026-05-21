package com.havamania

object DefaultNotifications {
    fun create(): List<NotificationItem> {
        val now = System.currentTimeMillis()
        return listOf(
            // SEYAHAT
            NotificationItem(
                id = "def_travel_1",
                title = "İzmir Seyahati Hazırlığı",
                message = "Hafta sonu İzmir seyahatin için UV seviyesi yüksek görünüyor. Güneş koruyucu ve şapka almanı öneririm.",
                category = NotificationCategory.TRAVEL,
                isRead = false,
                actionType = NotificationActionType.TRAVEL_CALENDAR,
                createdAt = now - (1000 * 60 * 15),
                deepLinkTarget = "havamania://app/calendar",
                actionLabel = "Planı Gör"
            ),
            NotificationItem(
                id = "def_travel_2",
                title = "Rota Önerisi: Batman",
                message = "Hasankeyf ve çevresindeki tarihi dokuyu keşfetmek için önümüzdeki 3 gün hava oldukça elverişli.",
                category = NotificationCategory.TRAVEL,
                isRead = true,
                actionType = NotificationActionType.TRAVEL_CALENDAR,
                createdAt = now - (1000 * 60 * 60 * 3),
                deepLinkTarget = "havamania://app/calendar",
                actionLabel = "İncele"
            ),
            // YAĞMUR
            NotificationItem(
                id = "def_rain_1",
                title = "Yağış Uyarısı: İstanbul",
                message = "Son 6 saatte yağış ihtimali %20'den %65'e yükseldi. Akşam saatlerinde şemsiyeni yanına almalısın.",
                category = NotificationCategory.RAIN,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 45),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Tahmini Gör"
            ),
            NotificationItem(
                id = "def_rain_2",
                title = "Haftalık Yağış Raporu",
                message = "Bu hafta geneli için 2 gün hafif yağış, kalan günler ise açık hava etkinliklerine uygun görünüyor.",
                category = NotificationCategory.RAIN,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 60 * 8),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Detaylar"
            ),
            // UV
            NotificationItem(
                id = "def_uv_1",
                title = "Yüksek UV İndeksi",
                message = "Bugün 12:00 - 15:00 saatleri arasında UV indeksi 8 seviyesine ulaşacak. Koruyucu kullanmayı unutma.",
                category = NotificationCategory.UV,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 120),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "UV Detay"
            ),
            NotificationItem(
                id = "def_uv_2",
                title = "Güneşli Gün Önerisi",
                message = "Cuma günü planlanan açık hava aktiviteleri için hava koşulları ve UV seviyesi oldukça uygun görünüyor.",
                category = NotificationCategory.UV,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 60 * 12),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "İncele"
            ),
            // UYARI
            NotificationItem(
                id = "def_warn_1",
                title = "Ani Sıcaklık Düşüşü",
                message = "İstanbul'da 18:00 sonrası sıcaklık hızlı düşecek. Dışarıdaysan hafif ceket alman konforunu artıracaktır.",
                category = NotificationCategory.WARNING,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 5),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Sıcaklık Grafiği"
            ),
            NotificationItem(
                id = "def_warn_2",
                title = "Rüzgar Hamlesi Uyarısı",
                message = "Kıyı kesimlerinde rüzgar hızı anlık 45 km/s hıza ulaşabilir. Deniz ulaşımında gecikmeler yaşanabilir.",
                category = NotificationCategory.WARNING,
                isRead = true,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 60 * 2),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Rüzgar Detay"
            ),
            // ÖZET
            NotificationItem(
                id = "def_sum_1",
                title = "Günün Hava Özeti",
                message = "Sabah sisli, öğleden sonra parçalı bulutlu bir gökyüzü bekleniyor. Akşam ise tamamen açık bir hava hakim olacak.",
                category = NotificationCategory.SUMMARY,
                isRead = false,
                actionType = NotificationActionType.WEEKLY_SUMMARY,
                createdAt = now - (1000 * 60 * 300),
                deepLinkTarget = "havamania://app/ai",
                actionLabel = "Özeti Aç"
            ),
            NotificationItem(
                id = "def_sum_2",
                title = "Haftalık Seyahat Analizi",
                message = "Planladığın 3 rota için en uygun hava koşulları Çarşamba günü gerçekleşecek gibi görünüyor.",
                category = NotificationCategory.SUMMARY,
                isRead = true,
                actionType = NotificationActionType.WEEKLY_SUMMARY,
                createdAt = now - (1000 * 60 * 60 * 24),
                deepLinkTarget = "havamania://app/ai",
                actionLabel = "Analize Git"
            ),
            // GÜNCELLEME
            NotificationItem(
                id = "def_upd_1",
                title = "Yeni Özellik: Rüzgar Çizgileri",
                message = "Hava kartlarına rüzgar şiddetine göre değişen premium animasyonlar eklendi. Hemen keşfet!",
                category = NotificationCategory.UPDATE,
                isRead = false,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - (1000 * 60 * 60 * 1),
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "Yenilikler"
            ),
            NotificationItem(
                id = "def_upd_2",
                title = "Performans İyileştirmesi",
                message = "Atmosferik efektler ve geçiş animasyonları daha düşük pil tüketimi için optimize edildi.",
                category = NotificationCategory.UPDATE,
                isRead = true,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - (1000 * 60 * 60 * 48),
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "İncele"
            ),
            // GENEL
            NotificationItem(
                id = "def_gen_1",
                title = "Havamania Premium",
                message = "Tüm hava durumlarına özel astronomik fazlarla zenginleştirilmiş seyahat deneyimine hoş geldin.",
                category = NotificationCategory.GENERAL,
                isRead = false,
                actionType = NotificationActionType.WEATHER_HOME,
                createdAt = now - (1000 * 60 * 60 * 24 * 3),
                deepLinkTarget = "havamania://app/weather",
                actionLabel = "Kullanmaya Başla"
            ),
            NotificationItem(
                id = "def_gen_2",
                title = "İlgi Alanlarını Güncelle",
                message = "Sana daha iyi öneriler sunabilmemiz için profilindeki ilgi alanlarını (kamp, spor, fotoğrafçılık vb.) güncel tutabilirsin.",
                category = NotificationCategory.GENERAL,
                isRead = true,
                actionType = NotificationActionType.APP_UPDATE,
                createdAt = now - (1000 * 60 * 60 * 24 * 7),
                deepLinkTarget = "havamania://app/profile",
                actionLabel = "Profilime Git"
            )
        )
    }
}
