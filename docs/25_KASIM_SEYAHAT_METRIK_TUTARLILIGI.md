# 25 Kasım 2026 — Seyahat metriklerinin kaynak tutarlılığı

## Düzeltilen davranış

RichTravelGuideView metrikleri analyses.lastOrNull(), hava/valiz uyarıları ise lastForecastSnapshot kaydından okuyordu. Bu kayıtlar ayrıştığında eski sıcaklık/yağış/skor yeni tahminle karışıyordu. Double.NaN.toInt() sıfır sıcaklık gibi görünebiliyor; yüzde/skor sınırları da kontrol edilmiyordu. Bilinmeyen yağış yeşil risk rengi kullanıyordu.

TripDisplayMetrics.kt ortak gösterim kaynağıdır. Snapshot varsa ortalama sıcaklık, yağış olasılığı ve konfor skoru yalnız o kayıttan gelir. Eksik snapshot alanına eski analizden değer taşınmaz. Snapshot yoksa son kayıtlı analizin sayısal değerleri doğrulanarak kullanılır. Sıcaklık sonlu ve -100..65, yağış/skor 0..100 aralığında olmalıdır. Gerçek sıfır kaybolmaz; bilinmeyen değer sıfırla değiştirilmez. Bilinmeyen yağış rengi nötrdür.

Rehber ve seyahat kartı kısa özetlerinde de aynı metrik kaynağı kullanılır; eski serbest metin yeni snapshot üstüne taşınmaz. Snapshot tehlike kodunun uyarısı korunur. Snapshot olmayan eski kaydın serbest özeti kaybolmaz; Kayıtlı analiz ön ekiyle gösterilir. Snapshot ortalama sıcaklık alanı yoksa min/max uçlarından dönem ortalaması türetilmez. Yenileme durum/zaman etiketleri önceki turdaki gibi kalır. Skor bilinmiyorsa kartta sıfır skor rozeti üretilmez.

## Doğrulama

Sekiz yeni JVM senaryosu: tek snapshot kaynağı, kısmi veriyle eski alanları karıştırmama, NaN/sonsuz/sınır dışı değer, snapshot olmayan eski analiz, gerçek sıfırlar, tehlike bilgisinin eski olumlu özeti bastırması yeni kayıtta metriklerin birlikte değişmesi ve snapshot olmayan eski kaydın açık tehlike metninin korunması. Mevcut gerçek rehber tablet senaryosuna eski 35 derece metninin görünmemesi, yeni 24 derece ve 85/100 skorunun görünmesi eklendi.

Son kaynakla 741 JVM testi ve SM-T500 üzerinde 3 izole cihaz senaryosu geçti; hata/istisna/atlama yok. QA APK ve test APK derlemesi başarılı. Tablet regresyonu eski 35°C değerinin kaldırılmasını, yeni 24°C ve 85/100 skorunu gerçek rehber bileşeninde doğruladı.

## Sınırlar ve kalan işler

Bu gösterim katmanı düzeltmesidir; kalıcı veri yapısı/migration ve geçmiş kayıtlar değişmez. Eski şemada varsayılan sıfır ile gerçekten ölçülen sıfırı ayıracak metadata yoktur; doğrulanabilir aralık içindeki sıfır geçerli kabul edilir. Rehberin snapshot/analysis eşleşmesini zaman farkı varsayımıyla uydurmak yerine kaynaklar karıştırılmaz. Canlı senkronizasyon/yenileme sürecinin tam kabulü ayrı kalır.

Sıradaki yerel inceleme: geçmiş seyahat özeti ve diğer analiz tüketicilerindeki NaN/eksik sayısal veri; snapshot olmadan çalışan eski kısa özetler. Tam ekran/tema/telefon/tablet/yatay/TalkBack, iç içe dikdörtgen görsel matrisi, aralıklı Activity/profil kök nedenleri ve canlı/release/mağaza kapıları açık. Push yapılmadı.