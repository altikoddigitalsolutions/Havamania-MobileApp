# 26 Kasım — Valiz hedef günü ve analiz zamanı

## Hedef plan seçimi
Valiz ve yerel seyahat yanıtı ortak selectTravelAdvicePlan yordamını kullanıyor. Her adayın hedef snapshot saat dilimiyle yerel günü hesaplanıyor; bitmiş/arşivlenmiş planlar seçilmiyor. Hedef bölgesi bilinmeyen bir aday varsa otomatik seçim yerine eksik bölge bilgisi uyarısı var. Valiz için mevcut şehirde hava verisi veya geçerli saat dilimi gerekmiyor. Başlangıç zaman penceresi de seçilen hedef gününü kullanıyor.

## Analiz zamanı
Valiz yanıtında tripForecastTimeLabel gösteriliyor. Bilinmeyen/pozitif olmayan üretim zamanı, gelecek üretim zamanı veya hedefin önceki yerel gününe ait analiz ayrıca güncelliği doğrulanamadı uyarısı alıyor. Bilinen ölçümler kayıtlı tahmine dayalı hazırlık olarak korunuyor; yeni veya canlı ölçüm olduğu iddia edilmiyor. Aynı yerel gün kuralı bir saatlik TTL veya canlı güncellik garantisi değildir.

## Testler
PackingDestinationClockTest: yedi regresyonun tamamı eski kodda başarısızdı. Hedef gününe göre seçim, mevcut şehirde bölge/hava verisi gerekmemesi, bilinmeyen/eski/gelecek analiz zamanı ve geçerli hedef yerel saatinin gösterimi doğrulandı. Önceki sayısal valiz testleri bilinen hedef bölgesi ve sabit üretim zamanı kullanıyor; eksik snapshot kontrolü korunuyor.

Son kaynak: 877 JVM testi geçti, hata/atlama yok. testDebugUnitTest ve assembleDebug başarılı; git diff --check temiz. Yeni fiziksel veya canlı servis testi yapılmadı.

## Sıradaki ve sınırlar
ForecastSnapshot içinde kapsanan başlangıç/bitiş günleri ve hedef kimliği bulunmuyor. Bu nedenle bu adım snapshot'ın plan tarih aralığı ve hedefiyle tam eşleştiğini kanıtlamaz. Öncelik bu metadata ve legacy/uyuşmazlık kontrolleri; plan düzenlemede mevcut invalidateChangedTripForecast temizliği korunur. Aynı başlangıç tarihli birden çok planın seçim politikası ve yerel valiz kişiselleştirme aktarımı açık. Sayı ayrıştırıcıları, nötr hero dekorasyonu, gerçek Activity akışı, bütün tema/ekran ve iç içe dikdörtgen görsel matrisi, canlı servis ve mağaza kapıları açık. Push yapılmadı.
