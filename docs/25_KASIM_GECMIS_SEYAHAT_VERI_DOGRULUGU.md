# 25 Kasım 2026 — Geçmiş seyahat özetlerinde veri doğruluğu

## Bulgular ve düzeltme

Geçmiş özet üretimi açıklaması boş sayısal kayıtları yok sayıyor, son liste elemanını en yeni kayıt kabul ediyor ve Double.NaN.toInt() gibi dönüşümlerle geçersiz sıcaklığı sayı olarak sunuyordu. Farklı tarihlerde yapılan tahminlerin dönem ortalamaları gerçek en düşük/en yüksek sıcaklık gibi birleştiriliyordu. Kayıt yokken skor sıfır kabul ediliyordu.

- En büyük kayıt zaman damgası seçilir; eşit zamanlarda listedeki son kayıt kararlı biçimde seçilir. Farklı revizyonlar arasında alan tamamlanmaz. Açıklama metni boş olsa da geçerli sayısal analiz kullanılır.
- Sıcaklık sonlu ve -100..65, yağış olasılığı/skor 0..100 aralığında olmalıdır. Geçersiz/eksik değer bilinmeyendir; gerçek sıfır korunur. TravelHistorySummary.comfortScore nullable yapıldı; bu türe ait tüketim yalnız türetilmiş özet/debug bilgisidir, kalıcı analiz şeması değişmez.
- Tahmin revizyonlarının ortalamaları günlük gözlem sayılmaz: minTemp/maxTemp bilinmeyen kalır; yağışlı/güneşli/bulutlu gün sayısı üretilmez.
- Özet ve hazırlık metinleri kayıtlı planlama tahminini anlatır; gerçekleşen hava ölçümü iddiası içermez. Bozuk tarih aralığı negatif gün üretmez.

## Test kanıtı

Yedi yeni JVM regresyonunun tamamı eski kodda başarısız oldu: geçersiz sıcaklık, bozuk yüzde/skor, boş açıklamalı geçerli analiz, ortalamalardan uç değer üretimi, liste sırası, kayıtsız skor, yeni kısmi kayda eski sıcaklık taşınması. Yeni izole tablet testi gerçek PastTravelDetailDialog içinde geçersiz sıcaklık uyarısını ve değişen planla 14 derece/%80 metnine geçişi kontrol eder.

Son kaynakla 748 JVM testi ve SM-T500 üzerinde 1 izole geçmiş özet cihaz testi geçti; hata/istisna/atlama yok. QA APK ve test APK derlemesi başarılı.

## Sınırlar ve sıra

Kalıcı kayıtlar yeniden yazılmaz; bu gösterim/özet düzeltmesidir. Eski şemadaki varsayılan sıfır ile gerçek sıfırı ayıracak metadata yoktur; geçerli sıfır kabul edilir. Gerçek günlük gözlem ve sıcaklık uçları için ayrı veri gerekir. Cihaz testi izole dialog kabulüdür; tüm takvim/navigasyon/Activity/tema/TalkBack kabulü değildir.

Sıradaki yerel iş: RecommendationEngine eski kısa özet fallback'i ve diğer sayısal karşılaştırma tüketicileri. Geçmiş özet dialogunun büyük yazı/küçük ekran kaydırma ve görsel sadeleştirmesi de açık görsel matrise bağlıdır. Kalan iç içe dikdörtgenler, canlı servisler, aralıklı Activity/profil kök nedenleri, release ve mağaza kapıları açık. Push yapılmadı.