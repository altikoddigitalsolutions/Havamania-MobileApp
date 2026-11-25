# 25 Kasım 2026 — Günlük seyahat bildiriminde konum günü

## Bulgu ve düzeltme

Worker seyahat uygunluğunu konum günüyle, günlük bildirim ID/tekrar anahtarını ve lastDailyNotificationDate alanını cihaz günüyle hesaplıyordu. eventAt cihaz saat dilimindeki seyahat başlangıcıydı. Cihaz ve konumun günleri ayrıştığında aynı konum günündeki tekrar veya konumda yeni günün atlanması mümkündü.

TravelDailyIdentity tek konum günü kimliğidir. Geçerli snapshot saat dilimi ve açık Instant girdisinden gün, notificationId, deduplicationKey ve seyahat başlangıcı eventAt üretilir. Worker erken gönderildi kontrolünde bilinen konum gününü kullanır; yeni analizden sonra kimliği yeniden üretir. Mesaj gün sayısı, ID, tekrar anahtarı ve başarılı günlük takip işareti aynı kimliğe bağlıdır. Analiz güncellemesi gün sınırını aşarsa eski cihaz günü kullanılmaz.

Saat dilimi olmayan/geçersiz kayda sessiz cihaz saati varsayımı uygulanmaz. Erken kontrolde kimlik bilinmiyorsa analiz denenir; güncel analiz de geçerli saat dilimi getirmediyse günlük bildirim üretilmez. Owner/trip ayrımı mevcut ID biçiminde korunur. Veritabanı şeması ve eski kayıtlar değişmez.

## Doğrulama

Yedi yeni JVM testinin beşi ilk hesapta hatayı yeniden üretti: aynı anda Tokyo/Los Angeles günleri, cihaz saat dilimi değişimi, konum gece yarısında yeni kimlik, UTC gece yarısında aynı konum günü, konum gün başlangıcı eventAt, bilinmeyen saat dilimi ve kullanıcı/seyahat ayrımı.

Yeni cihaz testi kendi UUID adlı Room dosyasında 20 eşzamanlı rezervasyonu, tamamlanmış teslimin kapatıp yeniden açılan veritabanında aynı konum gününde tekrarlanmamasını, yeni konum gününün yeni kaydını ve B kullanıcısının ayrımını kontrol eder. Sistem bildirimi yayımlamaz ve kullanıcı hesaplarına dokunmaz; test sonunda kendi dosyasını siler.

Son kaynakla 774 JVM testi ve SM-T500 üzerinde 1 izole kalıcı Room cihaz testi geçti; hata/istisna/atlama yok. QA APK ve test APK derlemesi başarılı.

## Kabul sınırları ve sıradaki işler

Room rezervasyon kabulü gerçek sistem tepsisinde tam olarak bir kez teslim garantisi değildir. İşletim sistemi yayını ile veritabanı tamamlaması arasında süreç kapanması ve mevcut teslim kurtarma politikası ayrı kabul kapısıdır. Bu tur tam Worker ağ/Firebase/Activity akışı çalıştırılmadı. Eski lastDailyNotificationDate yalnız tarih taşır; hangi saat dilimiyle yazıldığı bilinmez. Eski kayıtlara tahminî tarih dönüşümü veya toplu sıfırlama uygulanmadı; sürüm geçişinde bu belirsizlik açık kalır.

Sıradaki: güzergâh bildirimi günlük anahtarı ve hareket saatinin saat dilimi. routeForecastNeedsRetry/performRouteWeatherAnalysis hâlâ cihaz saat dilimi kullanıyor; çıkış konumu ile varış konumu aynı varsayılmamalı. Ayrı kaynak/şema ve test incelemesi gerekli. Diğer görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler, aralıklı Activity/profil kök nedenleri ve release/mağaza kapıları açık. Push yapılmadı.