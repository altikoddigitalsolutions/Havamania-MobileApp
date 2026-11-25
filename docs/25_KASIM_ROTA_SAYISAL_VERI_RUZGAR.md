# 25 Kasım 2026 — Rota sayısal verisi ve rüzgar riski

## Sayısal doğruluk ve eksik veri

Rota sağlayıcısı sıcaklık/yüzde/rüzgar alanlarını doğrulamadan UI'ya taşıyor ve tekil/toplu dönüşümü iki ayrı blokta yapıyordu. Yeni RouteForecastResponse/RouteHourlyDto rota taşımacılığını genel hava ekranı modelinden ayırır; ölçüm dizilerinde tekil null değer kabul edilir. Bir saatin sıcaklığı null diye aynı saatteki fırtına/rüzgar veya başka noktanın geçerli örneği düşmez. Saat dilimi eksik yanıt UTC varsayılmaz; zamanla eşleşme yapılamayan veri kullanılmaz.

Ortak routeWeatherSample dönüştürücüsü tekil ve toplu yolda kullanılır. Uygulama sınırları: sonlu sıcaklık -100..65, hissedilen -120..85, yüzde/nem 0..100, sonlu rüzgar/hamle 0..500. Bilinmeyen alan null kalır. Sıcaklık domain'de nullable'dır; UI Bilgi yok gösterir, sıfır/NaN/sonsuz sayıya dönüştürmez. Gerçek sıfır korunur. Bilinmeyen WMO kodu domain'de -1 ile temsil edilir; rota metni/simgesi bunu güneşli olarak sunmaz. Tüm risk verisi kullanılamıyorsa nokta null döner.

Kod, sıcaklık, yağış olasılığı, rüzgar veya hamle eksikse hasIncompleteData işaretlenir. Bilinen tehlikeler yine değerlendirilir; eksik veri tamamen uygun/yeşil sayılmaz. Hissedilen, nem ve gündüz/gece eksikliği ayrı bilgi kaybıdır, bu basit sürüş riski modelinin tamlık koşulu değildir. ViewModel kısmi veri uyarısını gösterir; güzergâh bildirim özeti de eksikliği açıklar. Risk açıklamasındaki tek satır sınırı kaldırıldı; birleşik uyarılar gizlenmez, DANGER metni hata rengi kullanır.

## Rüzgar ve rüzgar hamlesi

Saatlik istekler wind_gusts_10m alanını ve açık wind_speed_unit=kmh birimini ister. Hız ve hamle aynı örneklenen saatten gelir. Mevcut uygulama rüzgar sınıflarıyla uyumlu ihtiyat bantları: >=39 km/sa CAUTION, >=62 km/sa DANGER; eşikler hız ve hamleye ayrı uygulanır. Kar, donan yağış, fırtına ve rüzgar nedenleri birlikte korunur; en yüksek risk seviyesi seçilir. Donma sıcaklığı metni doğrudan yol buzlanması gerçekleşti iddiası yerine buzlanma açısından dikkat ifadesi kullanır.

[Open-Meteo](https://open-meteo.com/en/docs) hamle alanını önceki saatin maksimumu olarak tanımlar. [Kanada Meteoroloji Beaufort tablosu](https://www.canada.ca/en/environment-climate-change/services/general-marine-weather-information/understanding-forecasts/beaufort-wind-scale-table.html) 39–49 ve 62–74 km/sa bantlarını verir; [Met Éireann](https://www.met.ie/forecasts/marine-inland-lakes/beaufort-scale) Beaufort'un ortalama rüzgar ölçeği olduğunu açıklar. Uygulamanın hamleye de uyguladığı ihtiyat puanı resmî Beaufort hamle sınıfı, yol kapanışı veya araç bazlı sürüş güvenliği garantisi değildir. Model yol yüzeyi/araç/yerel resmî uyarıları içermez. Bu kaynak sözleşmeleri kontrol edildi, canlı hava çağrısı yapılmadı.

## Test kanıtı

İlk yedi regresyonun tamamı eski kodda hatayı yeniden üretti: geçersiz sıcaklık/opsiyonel veri, bilinmeyen kod, eksik rüzgar, açık gökyüzünde kuvvetli rüzgar, birleşik kar+rüzgar ve tekil/toplu doğrulama. Hamle, eşik sınırları, gerçek sıfır, tümü kullanılamaz örnek, kısmi veri özeti, eksik saat dilimi ve gerçek JSON null dizileri de eklenmiştir.

İlk tablet koşumunda yeni metin ölçümü başarısız oldu; birleşik tıklanabilir satır yerine gerçek uyarı Text düğümü ölçülecek şekilde düzeltildi ve metin/çok satır/taşma assert'leri sıkılaştırıldı. Son kaynakla 820 JVM testi ve SM-T500 üzerinde 9 izole cihaz senaryosu geçti; hata/istisna/atlama yok. Yeni kapsam 13 sayısal/rüzgar +2 wire JSON JVM senaryosudur. Gerçek Text düğümünde çok satır ve hasVisualOverflow=false kontrolü geçti; null sıcaklık/tehlike görünümü ve yeni veriyle güncelleme doğrulandı. QA APK ve test APK derlemesi başarılı.

## Sınırlar ve sıra

Veritabanı/Firestore şeması değişmedi; rota verisi çalışma zamanı modelidir. Testler fake API ve gerçek Kotlin serialization kullanır; canlı Retrofit/servis, bütün ViewModel/MapLibre/ağ ve gerçek bildirim tepsisi kabulü değildir. Tamamen bozuk JSON türleri veya HTTP hatasında sağlayıcı veri yok yoluna düşer; bu iş her bozuk payload'dan kısmi veri kurtarma iddiası değildir.

Sıradaki: güzergâh bildirim başlığında yalnız özet metni değişti diye hava değişti iddiası; genel hava ekranının ortak HourlyDto modelindeki null ölçüm dizileri ayrıca incelenmeli. Ara/varış yerel saati, tam hesap değişimi/iptal, görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler ve yayın kapıları açık. Push yapılmadı.