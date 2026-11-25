# 26 Kasım — Mapper saat dilimi doğrulaması

## Çözülen sorun
Eksik timezone alanı UTC olarak dolduruluyordu. Geçersiz timezone da mapper içinde UTC'ye düşüyordu. Bu nedenle konum günü bilinmeden Bugün ve güncel saat seçimi üretilebiliyordu. API DTO eksik saat dilimini artık boş olarak koruyor; mapper yalnızca geçerli ZoneId ile gün/saat işaretliyor. Tarihli günlük ve saatlik tahminler silinmiyor. Bugüne ait yağış/UV gibi türetilen günlük değerler bilinmeyen günle eşleştirilmiyor.

## Testler
İki yeni regresyon eski kodda başarısızdı. Son kaynakta 840 JVM testi geçti; hata/atlama yok. assembleDebug başarılı. Günlük UV, gerçek sıfır yağış ve tam veri uygunluk testlerinin konum saat dilimleri açıkça belirtilerek fixture varsayımları kaldırıldı; beklentiler gevşetilmedi. ADB cihaz listesi boş; fiziksel doğrulama yapılmadı.

## Kalan işler
Bu düzeltme mapper kapsamındadır. ForecastTime.zone/localNow eski UTC dönüşü; önbellek, ViewModel, WeatherView, yerel asistan ve öneri çağıranları henüz dönüştürülmedi. Eski UTC olarak kaydedilmiş verinin asıl saat dilimi sonradan çıkarılamaz. Sonraki adım bu tüketicileri bilinmeyen zaman durumuna taşımak ve eksik tahmin kapsamını göstermek. Önceki günlük/saatlik bileşen cihaz testleri bağlantı bekliyor. Tam ekran/tema, canlı servis ve yayın kapıları açık. Push yapılmadı.
