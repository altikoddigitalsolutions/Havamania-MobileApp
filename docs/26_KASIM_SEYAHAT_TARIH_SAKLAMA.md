# 26 Kasım — Seyahat günlerinin saat diliminden bağımsız saklanması

## Doğrulanan hata
startDate/endDate cihaz bölgesinde gece yarısı epoch olarak yazılıp sonradan başka cihaz bölgesinde okunuyordu. İstanbul–Los Angeles, tarih çizgisinin iki tarafı ve DST geçişi dahil dört ilk regresyon da eski kodda gün kaymasını gösterdi. Worker içindeki gölgelenen dönüşüm kopyaları da aynı yanlış varsayımı içeriyordu; kaldırıldı, ortak model dönüşümleri kullanılıyor.

## Yeni biçim ve geçiş
TravelPlanEntity nullable startDateIso/endDateIso alanlarını saklıyor. Yeni yazılar takvim gününü ISO olarak, eski epoch alanlarını UTC gece yarısı olarak üretiyor. Okuma açık ISO gününü esas alıyor. Room 23→24 iki nullable TEXT sütunu ekliyor, kayıtları ve eski epoch değerlerini silmeden ISO günlerini dolduruyor. Kimlik/hedefi uyuşan snapshot binding tarihi varsa eski kaydın özgün günü oradan kurtarılıyor. Aksi halde geçişte mevcut cihaz bölgesinde görünen yorum bir kez sabitleniyor. updatedAt, notlar ve kuyruk sürümleri değiştirilmeden korunuyor. Sorgu sıralaması ISO gününe; mükerrer plan kontrolü takvim gününe bağlandı.

## Legacy yeniden yükleme
Yerel ve uzak kaydı içeri alma yolları tarihleri sabitliyor. Ek cihaz testi, aynı eski kaydın yeni bölgede tekrar alınmasının ilk düzeltmede 26→25 kaydırdığını gösterdi. Aynı plan/kullanıcı ve değişmemiş eski epoch çifti yeniden geldiğinde mevcut sabit gün korunuyor. Geçerli açık ISO/binding günleri öncelikli. Gerçek epoch düzenlemesi veya başka kullanıcıdaki kayıt önceki günleri ödünç almıyor.

## Testler
TripStoredDateTest toplam 9 test: ilk dört kırmızı regresyon, legacy sabitleme, açık ISO önceliği, aynı kaydın tekrarı, gerçek düzenleme ve farklı kullanıcı. Son kaynakla 908 JVM testi geçti; sıfır hata/atlama. testDebugUnitTest ve assembleDebug başarılı.

SM-T500 Android 12 üzerinde son kaynakla 10 test geçti: TripStoredDateDeviceTest (3), gerçek v11→24 zinciri, TripDeletionPersistenceTest, v22→24 hesap çiti ve TripForecastBindingDeviceTest. Bunlar yeni/legacy kayıt yeniden açılışı, tarih çizgisi, gerçek v23 tablo yapısından geçiş, bekleyen gönderme/silme kayıtları ve hesap ayrımını kapsıyor. Eski tablo fixture'larında yeni sütunlar gerçekten kaldırılarak eski şema oluşturuldu. UUID veya mevcut izole test veritabanları kullanıldı; kişisel veritabanı temizlenmedi. git diff --check temiz.

## Açık sınırlar
Orijinal yazan cihazın bölgesi ve güvenilir binding/ISO bilgisi yoksa eski epoch'tan özgün takvim günü kesin çıkarılamaz. Bu tür kayıtların geçişte mevcut yorumu korunur; daha önce gerçekleşmiş kayma otomatik olarak kanıtlanıp düzeltilemez. Eski APK'nın yeniden düzenlediği ISO'suz kayıtların ilk kez indirilmesinde de bu sınır geçerlidir. Firestore yeniden indirmesi yerel DAO senaryosuyla simüle edildi; canlı çapraz sürüm Firebase testi yapılmadı. Gerçek takvim/Activity akışında cihaz bölgesi değişimi ayrı görsel kabul konusudur. Worker zamanlama saati burada değiştirilmedi.

## Sıradaki
Öneri ve kısa hava özeti yardımcılarında sıcaklık metnini yalnızca rakamlara süzen/varsayılan değer kullanan yollar incelenecek. Yerel valiz kişiselleştirme, nötr hero dekorasyonu, eski bildirim payload'ları ve çağrısız öneri yordamı; tüm ekran/tema/iç içe dikdörtgen matrisi, canlı servis ve yayın kapıları açık. Push yapılmadı.
