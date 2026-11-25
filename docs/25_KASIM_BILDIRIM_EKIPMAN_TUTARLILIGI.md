# 25 Kasım 2026 — Bildirim ekipman önerileri

## Bulgular ve değişiklik

TravelNotificationWorker bildirim verisini üretirken rehberden ayrı kurallar kullanıyordu: yalnız yağış yüzdesi/yüksek sıcaklıkla karar veriliyor; bilinen yağmur kodu, kar/don ve kuvvetli rüzgar atlanabiliyor; geçersiz sıcaklık/yüzde/rüzgar alanları bildirim verisine geçiyordu. UV bilinmezken yüksek sıcaklıktan güneş kremi çıkarılıyor, düşük sıcaklık ile yüksek UV birlikteyken koruma önerisi kaçırılıyordu.

- Veri üretimi test edilebilir buildTravelNotificationData yordamına alındı; Worker aynı yordamı çağırır.
- Ortak travelWeatherPreparation risk kümesi rehber valiz metni ile bildirim ekipmanını besler. Mevcut rehber eşikleri korunur: yağış olasılığı >50 veya bilinen yağış kodu; min<=0/max<12 veya kar/don kodu; sıcak max>28; rüzgar>30; UV>5; sıcaklık farkı>10. Bildirimin önceki ayrı >40 yağış/<15 soğuk kuralları ortak rehberle eşitlendi. Bunlar uygulama hazırlık kurallarıdır, resmî meteorolojik alarm eşikleri değildir.
- Yağış için yağmurluk/su geçirmeyen ayakkabı, soğuk/kar için sıcak katman/kaymayan ayakkabı, rüzgar için rüzgarlık, sıcaklık için hafif kıyafet/su, yüksek UV için güneş kremi/şapka, sıcaklık farkı için hırka önerilir. Birden fazla risk birlikte korunur; yinelenen ekipman çıkarılır. Fırtınada genel şemsiye dayatılmaz.
- Ortak doğrulama sonlu/sınır içi sıcaklık-rüzgar-UV ve 0..100 yağış gerektirir; ters min/max bilinmeyendir. Bildirim veri alanları doğrulanmış değerleri kullanır. Bilinmeyen rüzgar null kalır. Sıcaklık tek başına UV değeri yerine geçmez.
- Bildirim ilgi/sağlık tercihi okumaz veya yeni kişisel bağlam eklemez; öneri hava kanıtına dayanır.

## Test kanıtı

Yedi yeni JVM senaryosunun altısı ilk kaynakta hatayı yeniden üretti: kayıtsız hava, sonlu olmayan/sınır dışı veri, yüzde olmadan yağmur kodu, ılıman maksimumla kar/don, yalnız kuvvetli rüzgar, sıcaklık/UV bağımsızlığı ve birleşik riskler. Sıcaklık testinin büyük/küçük harfe bağlı yanlış geçmemesi için kontrol harf duyarsız güçlendirildi. Önceki rehber/valiz testleri de tam birim koşumuna dahildir.

Son kaynakla 761 JVM testi geçti; hata/istisna/atlama yok. assembleDebug ile QA APK derlemesi başarılı. Bu tur cihaz testi yapılmadı.

## Sınırlar ve sıradaki açık iş

Bu düzeltme ekipman/veri hazırlığıdır. Gerçek bildirim tepsisi, kanal/izin, zamanlama/teslim/Activity geçişi veya tüm bildirim detay UI kabulü bu tur yapılmadı. UI değişmedi; fiziksel cihaz testi tekrarlanmadı. Eski kaydedilmiş bildirimler yeniden yazılmaz.

Yeni doğrulanmış kaynak bulgusu: Worker.generateNotificationText bazı durumlarda kısmi alanlardan null derece yazabiliyor; seyahat dönemi snapshot'ını devam eden seyahatte Bugün hava diye adlandırabiliyor. Metnin eksik veri ve dönem/gün ayrımını test edip düzeltmek sırada. Önceki görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler, aralıklı Activity/profil kök nedenleri ve yayın kapıları açık. Push yapılmadı.