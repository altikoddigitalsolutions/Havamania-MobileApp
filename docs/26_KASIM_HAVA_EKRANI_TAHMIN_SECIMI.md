# 26 Kasım — Hava ekranı tahmin seçimi

## Başlangıç ve yeniden doğrulama
Başlangıç tahmin seçimi ortak saf yordamda konum saat dilimiyle hesaplanıyor. Boş/geçersiz saat diliminde gün/saat otomatik seçilmiyor. StateFlow ve ekran parametresi nullable: seçim olmaması sahte bir tarihle temsil edilmiyor. Bilinen bölgede bugünün satırı, yoksa sıradaki geçerli gün seçiliyor; saatlik seçim aynı konum saatine göre filtreleniyor. Günlük veri olmadan geçerli saatlik veri de kullanılabiliyor. ViewModel yeniden doğrulamasındaki UTC dönüşü kaldırıldı.

## Açıklama ve güncelleme etiketi
Saatlik açıklama seçili saatin doğrulanmış isDay değerinden; günlük açıklama nötr kod açıklamasından geliyor. Günlük tahmin mevcut gece/gündüze göre yeniden adlandırılmıyor. Anlık görünüm mapper açıklamasını kullanıyor. Çevrimdışı/eski kayıt güncelleme saati yalnızca geçerli bölgede biçimleniyor; bilinmeyen bölgede Saat dilimi doğrulanamadı gösteriliyor.

## Doğrulama
851 JVM testi, sıfır hata/atlama. Altı yeni test bilinmeyen bölge, İstanbul günü ve farklı cihaz bölgesi, günlük olmadan saat seçimi, nötr günlük açıklama, saatlik faz ve güncelleme etiketi durumlarını kapsıyor. testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı.

SM-T500 Android 12: 7 cihaz testi geçti. MainDailyNullDeviceTest ve MainHourlyNullDeviceTest doğru komşu tarih/saat, sıcaklık, sahte sıfır olmaması ve seçim callbacklerini; RouteWeatherPhaseDeviceTest fazı; WeatherLoadErrorDeviceTest dört senaryoda yeniden deneme/şehir arama/çevrimdışı uyarı ve güncelleme etiketini kapsadı. Sonuncusuna geçerli bölgeden bilinmeyen bölgeye geçişte eski saat etiketinin kaybolması kontrolü eklendi. Önceki günlük bileşen cihaz kontrolü böylece tamamlandı. Test sırasında USB açık kalma ayarı önceki değerine döndürüldü.

## Açık işler
Bu testler izole Compose bileşenleridir; gerçek giriş sonrası tam WeatherViewModel/Activity yaşam döngüsünün kabulü değildir. Asistan ve öneri UTC dönüşleri sırada. Hero dekorasyonu hala LocalTime ve varsayılan güneş aralığı gerektiriyor; saat bilinmediğinde görsel konum için nötr öğlen kullanılması yalnızca dekoratif, metin/tahmin kaynağı değil. Nötr güneş/ay tasarımı, eski Boolean current faz modeli ve eksik kapsam görünümü ayrıca ele alınmalı. Tüm ekran/tema ve iç içe dikdörtgen matrisi, canlı servis ve yayın kontrolleri açık. Push yapılmadı.
