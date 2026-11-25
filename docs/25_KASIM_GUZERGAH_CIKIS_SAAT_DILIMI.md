# 25 Kasım 2026 — Güzergâh bildiriminde çıkış saat dilimi

## Bulgu ve düzeltme

Güzergâh bildirimi hareket saatini cihazın saat dilimine çeviriyor, günlük tekrar anahtarını cihaz günüyle üretiyor ve tahmin noktalarının ETA hesabında tekrar cihaz saatine dönüyordu. Çıkış ve varış farklı saat dilimlerindeyken 48 saat penceresi ve tahmin saati kayabiliyordu. Kayıtlı hareket saati LocalDateTime'dır; kendi saat dilimi/offset metadata'sı yoktur.

- Bildirim akışı çıkış koordinatını doğrular ve mevcut Open-Meteo servisinden timezone=auto, bir günlük dar saatlik istekle konum saat dilimini çözer. Yeni LocationTimezoneResponse eksik saat dilimini UTC varsaymaz. Varış snapshot'ının saat dilimi kullanılmaz.
- Koordinat bazlı çözüm Worker örneği içinde tekrar kullanılır. Çözüm 10 saniyelik coroutine zaman aşımına bağlıdır; servis/eksik yanıt mevcut sınırlı retry politikasına döner. Kullanıcı iptali yutulmaz; hesap sahipliği çözümden sonra yeniden denetlenir. Canlı servis erişimi/kota testi bu tur yapılmadı.
- RouteNotificationClock tek çıkış saatini, çıkış konum gününü, günlük anahtarı ve ID'yi üretir. Aynı departure Instant hem 48 saat kontrolüne hem rota tahmin noktalarına verilir; ikinci kez cihaz saati dönüşümü yoktur. Bildirim eventAt hareket anıdır.
- Yaz saati boşluğunda olmayan yerel saat veya çift offsetli belirsiz saat sessizce düzeltilmez/seçilmez; bildirim o kayıt için üretilmez. Offset seçimini açıklayan düzenleme UI'sı ayrı açık iştir.
- Veritabanı şeması/seyahat kayıtları değişmedi. Çıkış seçilmemişse Worker mevcut konumu varsayarak arka planda izin/konum istemez; önceki ineligible davranışı korunur.

## Test kanıtı

Yedi yeni saat testinin tamamı ilk cihaz saati hesabında başarısız oldu: New York çıkış/Tokyo varış, cihaz saat dilimi değişimi, DST boşluğu/çakışması, bilinmeyen saat dilimi, çıkışta gece yarısı ve tam 48 saat sınırı. Dört ek resolver testi doğru çıkış koordinatını, geçersiz koordinatta servis çağrılmamasını, eksik JSON saat diliminin bilinmeyen kalmasını ve iptal yayılımını kapsar. Servis fake fonksiyonla test edilir, canlı HTTP/model/Firebase çağrısı yoktur.

Son kaynakla 785 JVM testi geçti; hata/istisna/atlama yok. assembleDebug ile QA APK derlemesi başarılı. Bu tur cihaz veya canlı servis testi yapılmadı.

## Açık kabul ve sıra

Bu kabul güzergâh bildirim akışının yerel saat mantığıdır; canlı origin timezone servisi, bütün Worker ağ/Room/tepsi teslimi ve fiziksel UI testi değildir. Ek istek servis kapasitesi/dağıtım kabulünde ölçülmelidir. Resolver yalnız saat dilimi döndürür; yanlış sağlayıcı yanıtını koordinat doğrulamasıyla teyit eden canlı sözleşme testi açık kalır.

**Sıradaki zorunlu devam:** TravelRouteViewModel, TravelRouteWeatherScreen ve TravelStatusResolver.isRouteWeatherEligible hâlâ cihaz saat dilimi varsayımı içeriyor. Ekrandaki analiz anı, 48 saat kilidi, çıkış/ara/varış etiketleri ve DST/eksik saat dilimi hata durumu aynı açık çıkış saati sözleşmesine bağlanmalı. Bildirim düzeltmesi tüm güzergâh UI'sını tamamlamaz. Varış/ara nokta etiketleri ayrıca kendi konum saat dilimi gerektirir. Önceki görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servis, aralıklı Activity/profil kök nedenleri ve yayın kapıları açık. Push yapılmadı.