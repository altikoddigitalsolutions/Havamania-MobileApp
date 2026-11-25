# 26 Kasım — Önbellekte bilinmeyen konum günü

## Düzeltilenler
cachedWeatherSameLocalDay artık geçerli ZoneId gerektiriyor. Boş/geçersiz saat dilimi aynı gün kabul edilmiyor. Önbellek güncel sayılmıyor; günlük yüksek/düşük, yağış/UV, güneş bilgileri ve uygunluk puanı eski kayıt üzerinden bugüne taşınmıyor. Kayıtlı temel ölçümler eski veri işaretiyle korunuyor.

ForecastTime.dailyFromToday konum günü bilinmediğinde geçerli tarihli kayıtları koruyor fakat Bugün işareti üretmiyor. hoursForDate bilinmeyen saat diliminde güncel saat listesi üretmiyor. Önbellek normalizasyonu eski Bugün ve seçili saat bayraklarını bu durumda temizliyor. Açıkça belirtilen UTC geçerli saat dilimi olmaya devam ediyor.

## Doğrulama
Dört yeni saf testin üçü eski kodda hatayı üretti; açık UTC kontrolü zaten geçiyordu. Repository seviyesinde ek test bilinmeyen saat dilimindeki önbelleğin yeniden ağ isteği gerektirdiğini doğruluyor. Koordinat ayrımı testine gerçek konum saat dilimi eklenerek testin amacı korundu. Eski invalid-timezone testinin UTC saat sunma beklentisi, bilinmeyen zamanda seçim yapmama sözleşmesine güncellendi.

Son kaynak: 845 JVM testi, sıfır hata/atlama. testDebugUnitTest ve assembleDebug başarılı; git diff --check temiz (satır sonu uyarıları dışında). Fiziksel test yapılmadı; önceki tablet kontrolleri açık.

## Sıradaki
ViewModel başlangıç seçimi/yeniden doğrulama, WeatherView, yerel asistan ve öneri akışında kalan ForecastTime.localNow/zone UTC dönüşleri incelenecek. Bu adım tüm uygulamanın saat dilimi dönüşümünü bitirmiyor. Eski UTC olarak kaydedilen fakat kaynağı bilinmeyen kayıtların gerçek bölgesi sonradan anlaşılamaz. Eksik saat/gün kapsamının görünümü, iç içe dikdörtgen görsel matrisi, canlı servisler ve mağaza kabulü açık. Push yapılmadı.
