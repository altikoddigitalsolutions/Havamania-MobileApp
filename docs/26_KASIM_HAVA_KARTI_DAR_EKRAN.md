# 26 Kasım — Hava kartı dar ekran yerleşimi

## Düzeltilenler
- Uzun konum satırı bildirim düğmesinin alanını tüketiyordu. Konum alanı kalan genişliği kullanıyor; ad en fazla iki satır/ellipsis, bildirim kontrolü ayrı sabit alan ve konum hedefi en az 48 dp.
- Sabit 340 dp kart, büyük yazıya göre genişletiliyor. Konum alt açıklamasının gereksiz düşük opaklığı kaldırıldı.
- Nem/rüzgâr/UV için ikinci çerçeve kaldırıldı. FlowRow metrikleri gerektiğinde alt satıra geçiriyor; dar alanda sabit 64 dp kutuya sıkıştırılmıyor.
- Üstte tekrarlanan dekoratif hava ikonu kaldırıldı; hava başlığı ve dekoratif efekt korunuyor. Gece UV değeri açıkça UV -- şeklinde gösteriliyor.
- Güneş/ay dekorasyonu üst kontrollerden ve alt metriklerden ayrılan alanda çiziliyor. İlk görsel kontrolde görülen kesik güneş diski giderildi; disk güvenli alanda küçük bir dekor olarak konumlandırıldı. Bu konum astronomik gök cismi konumu değildir.

## Test ve görsel kabul
Son üretim kaynağıyla 931 JVM testi geçti; hata/atlama yok. Debug APK derlendi. SM-T500 Android 12 üzerinde beş cihaz testi geçti: iki yeni dar ekran testi, iki faz/canlı efekt regresyonu ve gerçek pencere sistem ölçeği testi. Kullanıcı hesabı/Firebase verisine işlem yapılmadı.

Yeni testler 320 dp genişlikte uzun AFYONKARAHİSAR MERKEZ adı, -18°/-24°, %100 nem, 123 km/sa ve UV 11 kullanıyor. Açık tema %200 ve koyu tema normal yazı boyutunda konum/bildirim tıklamaları, düğmelerin birbirine taşmaması, 48 dp konum hedefi ve metriklerin yatay sınırları doğrulandı. Tam konum metni semantikte kalırken büyük yazıda görsel olarak iki satır sonunda kısaltılabiliyor.

Son koşunun iki PNG'si .artifacts/hero-layout-20261126 altında gözle incelendi: kesik disk/üst kontrol örtüşmesi ve metrik yatay taşması görülmedi; büyük yazıda metrikler üç satıra geçiyor. Güncel örneklerin hava paleti gündüz olduğundan koyu uygulama teması da gündüz paletini korur. Bu, gece ve tüm hava/tema birleşimlerinin tamamının görsel onayı değildir. Yerel PNG'ler push kapsamına dahil edilmez.

## Kalan sıra
Geniş/yatay ekran, tüm hava koşulları, daha büyük erişilebilirlik ölçekleri ve tüm menülerde iç içe çerçeve matrisi açık. Sonraki iş API dokümanında doğrulanan FCM kayıt 201/200 yanıt uyuşmazlığıdır; ardından hava hız sınırı parametreleri. Önceki Firebase/senkronizasyon/güvenlik/canlı servis ve Play yayın kapıları korunur. Push yapılmadı.
