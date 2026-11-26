# 26 Kasım — Hava kartında nötr gündüz/gece fazı

## Doğrulanan sorun
Hero hesaplayıcı eksik güneş saatini 06:00–18:00 varsayıyor; isDay kart parametresi görünüm hesabına aktarılmıyordu. Gün batımında gece başlığı/ay ikonu ile güneş efekti bir araya gelebiliyordu. İlk altı regresyonun dördü eski kodda başarısızdı; geçerli gündüz ve fırtına kontrolleri korunuyordu.

## Faz kaynağı ve nötr görünüm
Öncelik sağlayıcının doğrulanmış is_day=0/1 alanında. Bu yoksa yalnızca geçerli, sıralı güneş aralığı ve verilen yerel saat faz çıkarabiliyor. Saat/güneş aralığı eksik ya da geçersizse phaseKnown=false nötr stil seçiliyor: güneş/ay ve yıldızlı gökyüzü yok, statik gri-mavi arka plan, nötr hava başlığı. Bilinen yağış/kar/fırtına bilgisi faz bilinmediğinde de korunuyor. Donan yağış ve kar tanesi kodları ilgili yağış/kar görsel sınıfına dahil edildi.

Ana kart için 06:00/18:00 ve öğlen saati uydurma kaldırıldı. WeatherData.verifiedIsDay nullable alanı gerçek sağlayıcı bilgisini saklıyor; eski Boolean isDay uyumluluk için duruyor, doğrulanmış faz yerine kullanılmıyor. Eski JSON'da yeni alanın varsayılanı null. Günlük tahmin anlık fazı ödünç almıyor; eski anlık kayıt sağlayıcı fazını güncel diye kullanmıyor. Seçili saat kendi fazını koruyor. Başlık ortak describeWeatherPhase yordamından geliyor. Güneş/ay yörüngesi görsel/dekoratiftir; astronomik konum doğruluğu iddiası yok.

## UI bağlantısı
isDay remember anahtarına ve görünüm çözümleyiciye aktarılıyor. Faz değişince başlık ve görünüm birlikte güncelleniyor. Kart erişilebilirlik stateDescription alanında bilinmeyen/gündüz/gece görünümünü bildiriyor. Nötr arka plan sonsuz arka plan animasyonu oluşturmuyor. HeroThemeSpec.phaseKnown=false durumunda dayPart görsel enum uyumluluğu için yer tutucudur; faz doğruluğu olarak okunmamalı.

## Testler ve görsel kontrol
WeatherHeroPhaseTest toplam 10 test: eksik/ters/tek güneş sınırı, gün batımı, gerçek güneş aralığı, fırtına; sağlayıcı gece önceliği, saatsiz sağlayıcı gündüzü, mapper 0/1/boş/geçersiz fazı ve eski/günlük/legacy Boolean ayrımı.

Son uygulama kaynağıyla 930 JVM testi geçti; hata/atlama yok. testDebugUnitTest ve assembleDebug başarılı. SM-T500 Android 12 üzerinde WeatherHeroPhaseDeviceTest geçti: bilinmeyen→gece→gündüz→bilinmeyen ve açık/koyu tema geçişi gerçek kartta doğrulandı. Canlı efekt tercihi testte enjekte edilerek ThemeViewModel/Firebase bağımlılığı olmadan çalıştırıldı. Testin görüntü saklama yeri düzeltildikten sonra cihaz testi tekrar geçti; üretim kaynağı değişmedi. USB açık kalma ayarı önceki değerine döndürüldü.

Dört QA PNG yerel .artifacts/hero-phase-20261126 klasörüne alınıp gözle incelendi. Nötr açık/koyu, gece ve gündüz görüntülerinde bu 360 dp örnekte yazı/ölçüm taşması gözlenmedi; nötr görünümde güneş/ay yok. Görseller yerel QA çıktısıdır, push'a dahil edilmez. git diff --check temiz.

## Sıradaki ve sınırlar
Görsel incelemede gündüz paletindeki küçük sarı ikonların kontrastı iyileştirme konusu olarak kaydedildi. Bilinen fazda LiveBackgroundLayer canlı efekt/sistem hareket azaltma tercihinden bağımsız animasyon açabiliyor; sıradaki iş bu tercihleri arka plan dahil tutarlı uygulamak. Büyük yazı/yatay/tablet-telefon ve tüm temalar için tam görsel kabul henüz değil. Eski faz yardımcılarının tüm tüketicileri, yerel valiz kişiselleştirme, eski bildirim payload'ları, çağrısız öneri, canlı Firebase/Activity/servisler, iç içe dikdörtgen matrisi ve yayın kapıları açık. Push yapılmadı.
