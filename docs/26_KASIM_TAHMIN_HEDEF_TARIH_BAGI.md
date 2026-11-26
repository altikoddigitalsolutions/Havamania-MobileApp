# 26 Kasım — Tahminin hedef ve tarih bağı

## Yeni kayıt bilgisi
ForecastSnapshot'a nullable binding alanı eklendi. TripForecastBinding plan/kullanıcı kimliği, il/ilçe, istenen koordinatlar, planın başlangıç/bitişi ve kapsanan başlangıç/bitişi saklıyor. buildTripForecastSnapshot alanı üretirken tam günlük kapsama doğrulamasını koruyor. TravelAnalysisEngine bu yordamı geocoding sonrası kullanılan koordinatlar ve gerçek planla çağırıyor. Bu, talep edilen hedef bağını kaydeder; canlı sağlayıcı sonucunun coğrafi doğruluğu ayrıca entegrasyon kontrolüdür.

## Valiz koruması
tripForecastMatchesPlan hedef, plan/kullanıcı ve tarihler uyuşmadığında ölçüm/ekipman yanıtını engelliyor. Devam eden planda geçmiş günleri içeren tam kayıt kabul edilir; kalan günleri atlayan başlangıç veya eksik bitiş kabul edilmez. Eski binding'siz snapshot silinmiyor ve yüklenebiliyor; valizde eşleşmesi doğrulanmış sayılmıyor, analiz yenileme uyarısı veriliyor. Önceki analiz-zamanı uyarıları ayrıca korunuyor.

## Saklama ve uyumluluk
Yeni alan nullable, metadata alanları varsayılan değerli. Room'un mevcut JSON dönüştürücüsü yeni alanı koruyor; eski JSON binding olmadan okunuyor. TEXT sütunları değişmediği için Room sürümü/migration değiştirilmedi. Eski okuyucuların ignoreUnknownKeys davranışı kaynakta mevcut; eski APK üzerinde ayrıca çalışma testi yapılmadı. Firestore kurallarının trips bölümü alt alan şeması sınırlamıyor; canlı Firestore SDK/map turu bu adımda çalıştırılmadı.

## Testler
Eski snapshot'ın valizde ölçüm üretmesi regresyonu önce başarısız oldu. TripForecastBindingTest toplam 8 test: üretici metadata/tam kapsam, hedef/ilçe/koordinat, plan tarihleri, plan ve kullanıcı değişimi, eksik/bozuk kapsam, devam eden planın kalan günleri, yeni JSON turu ve legacy okuma. Önceki valiz veri/zaman testlerine açık, eşleşen metadata fixture'ları eklendi; sayısal ve zaman kontrolleri korundu.

Son kaynakla 885 JVM testi geçti; sıfır hata/atlama. testDebugUnitTest ve assembleDebug başarılı. SM-T500 Android 12 üzerinde 1 izole Room testi geçti: UUID adlı ayrı veritabanına yazma, kapatıp yeniden açma, metadata eşitliği, geçerli plan eşleşmesi ve A–B kullanıcı erişimi/eşleşme reddi doğrulandı. Test veritabanı temizlendi, kişisel veritabanı kullanılmadı. git diff --check temiz.

## Sıradaki ve açık kapsam
Yeni eşleşme kontrolü henüz rehber/özet/bildirim dahil tüm snapshot tüketicilerine uygulanmadı; sıradaki adım bu tüketicilerin matrisi. Yerel valiz kişiselleştirme ve aynı tarihte birden çok plan seçimi açık. Room toEntity/toDomain plan tarihlerini systemDefault üzerinden epoch'a çeviriyor; cihaz saat dilimi değişiminde olası tarih kayması ayrıca test edilmeli ve gerekiyorsa düzeltilmeli. Bu adım tüm uygulamada zaman/senkronizasyon kabulü değildir. Nötr hero dekorasyonu, sayı ayrıştırıcıları, gerçek Activity ve tüm ekran/tema/iç içe dikdörtgen matrisi, canlı servis ve mağaza kapıları açık. Push yapılmadı.
