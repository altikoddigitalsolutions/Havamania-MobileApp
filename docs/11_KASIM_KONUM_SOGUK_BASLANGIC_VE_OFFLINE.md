# B5-2 devamı — Soğuk konum başlangıcı ve çevrimdışı hava

## Bulgu ve düzeltme

GPS kapatılıp açıldıktan sonraki ilk istek cihazda yaklaşık 99 ms içinde boş konum döndürdü. Aşama kaydı `POSITION_STARTED, POSITION_EMPTY` idi: bu koşuda ters adres sorgusu başlamamıştı; sorun yalnız geocoder veya 15 saniyelik süre aşımı olarak açıklanamaz.

İlk konum sonucu 2 saniye içinde boş dönerse 1,5 saniye beklenip yalnız bir kez yeniden istenir. Sağlayıcı hatasında, izin/konum hizmeti kaybında veya iptalde yeniden istenmez. İki istek aynı toplam 31 saniyelik sınırı paylaşır. SDK isteği 30 saniye süre ve en fazla 10 saniyelik konum yaşı ile yapılandırılır. Üst coroutine iptali korunur; geç sonuç şehir yayımlamaz. Tanılama varsayılan olarak sessizdir; test kaydı yalnız aşama ve süre içerir, koordinat veya şehir içermez.

SDK'nin tek istek için önbellekteki veya yeni konumu kullanabildiği ve boş sonuç döndürebildiği [FusedLocationProviderClient belgesinde](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient) açıklanır. Süre ve konum yaşı ayrı [CurrentLocationRequest ayarlarıdır](https://developers.google.com/android/reference/com/google/android/gms/location/CurrentLocationRequest). Seçilen sınırlı kurtarma davranışı uygulamaya ait karardır; tüm cihazlarda GPS başarısı garantisi değildir.

## Hava ekranı ve veri doğruluğu

- Gerçek HomeScreen, WeatherViewModel, ayrı DataStore ve Room ile çevrimdışı geri dönüş sınandı. Ağ ve konum sağlayıcıları kontrollüdür; radyo fiziksel olarak kapatılmadı.
- Eski hava önbelleğinin eksik şehir/koordinat bilgisi istenen önbellek anahtarının şehri ve koordinatlarıyla tamamlanır. Böylece varsayılan İstanbul etiketi veya eksik koordinat taşınmaz.
- Eksik nem, rüzgâr ve UV için sabit `%65`, `12 km/sa` ve `0` gösterilmez. Gerçek sıfır korunur; eksik veri açıklanır.
- Rüzgâr hızı için önce sayısal hız kullanılır. Eski metin verisinde yalnız tam hız etiketi kabul edilir; “Rüzgâr soğutma” ve “Rüzgâr hamlesi” hız yerine seçilmez.

## Doğrulama

- **477 JVM testi, 0 hata.** Yeni 7 sınırlı konum kurtarma ve 4 hava ölçümü testi dahil.
- SM-T500 / Android 12: normal yaklaşık konum ve GPS yeniden açılışından sonraki ilk şehir çözümü **2/2 geçti**. Son koşuda normal çözüm 511 ms, GPS yeniden açılışı 119 ms sürdü; ikisi de doğrudan konum aldı. Yeni kurtarma dalı bu başarılı cihaz koşusunda tetiklenmedi; erken boş sonuçtan kurtarma kontrollü JVM testleriyle doğrulandı. Önceki boş sonuç bulgusu korunur.
- Gerçek çevrimdışı HomeScreen testi ile 7 geç ağ/önbellek regresyonu **8/8 geçti**. Ölçüm düzeltmesinin sonrasında HomeScreen testi tekrar **1/1 geçti**; bu tekrar ayrı benzersiz test sayılmaz.
- Son Gradle çalışması `testDebugUnitTest connectedDebugAndroidTest` başarılı. `git diff --check` hata vermedi.

## İzolasyon ve açık kalanlar

QA paketi `com.havamania` kullanıldı; Play paketi `com.havamania.mobile` değiştirilmedi. Testler ayrı yerel veri dosyaları ve oturumsuz QA uygulaması kullanır. Canlı Firebase hesap verisine yazılmadı. GPS testi önceki konum hizmeti durumunu finally içinde geri yükler.

Geniş B5-2 hâlâ açık: gerçek girişten başlayarak tam uygulama soğuk açılışı, fiziksel internet kesintisi, gerçek hesap A–B geçişi, süreç ölümü, gerçek kalıcı izin reddi ve farklı telefon/üretici doğrulaması. Tek başarılı cihaz koşusu aralıklı sağlayıcı sorunlarının tümünü kapatmaz. Release AAB üretilmedi.

Sıradaki B5-3 veri/UI incelemesine eklendi: boş günlük listede “0 günlük tahmin” başlığı yerine anlamlı boş durum, “Bursa'de” gibi yer adı ekleri, eski/eksik önbellekte hava açıklaması–kod tutarlılığı ve ayrıntı eşleyicide eksik ölçümlerin sıfıra çevrilmesi. Bu gözlemler seyrek eski önbellek test verisinde görüldü; canlı API'de aynı koşulun oluştuğu henüz doğrulanmadı.
