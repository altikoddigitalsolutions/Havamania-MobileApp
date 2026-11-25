# 25 Kasım 2026 — Bildirim metninde eksik veri ve dönem ayrımı

## Bulgular ve düzeltme

Worker metni conditionSummary varlığını sıcaklık alanlarının da varlığı gibi kabul ediyor ve null derece üretebiliyordu. Seyahat dönemini kapsayan snapshot devam eden seyahatte Bugün hava diye sunuluyor; veri olmasa da yarın/ilerideki seyahatte analiz hazır deniyordu. Kayıtlı comparisonText varlığı değişim kanıtı sayılıyordu. Gün sayımı ayrıca cihaz tarihini okuyordu.

buildTravelNotificationText saf metin üretim yordamıdır; Worker doğrudan bu yordamı çağırır. Metinde:

- Sıcaklık alanları ortak doğrulama sonrasında ayrı işlenir. İki alan varsa ikisi, biri varsa o alan ve diğerinin eksikliği gösterilir. İkisi de yoksa bilinmeyen ifade edilir. null/NaN derece yazılmaz.
- Seyahat dönemi tahmini açıkça adlandırılır; dönem verisi bugün ölçülmüş hava olarak sunulmaz. Sıcaklık mevcutken durum açıklaması eksik diye sayı kaybedilmez.
- Veri eksikken hazır denmez; açıklama/karşılaştırma metni bulunması tek başına hava değişikliği sayılmaz. Yarın/bugün ifadeleri seyahat başlangıcını anlatır, hava ölçümünü değil.
- Güncel analiz sonrası gün hesabı, kalan gün ve devam ediyor durumu updatedPlan konum tarihiyle yeniden hesaplanır; aynı tarih metin üretimine açık argüman olarak gider. Önceki kayıt saat dilimiyle yeni tahmin metni karıştırılmaz.
- Tarih ve Türkçe metin açık tr-TR locale kullanır. İki gereksiz özel yönlendirme yordamı kaldırıldı; Worker ortak veri/metin yordamlarını doğrudan kullanır.

## Test kanıtı

Altı yeni JVM testinin beşi ilk kaynakta hatayı yeniden üretti: kısmi sıcaklıkta null, dönem/gün ayrımı, eksik veriyle hazır iddiası, açıklama yokken geçerli sıcaklık, verilen konum tarihiyle gün sayımı ve comparisonText varlığının yanlış değişim iddiası. Tarih sabit test argümanıyla kontrol edilir.

Son kaynakla 767 JVM testi geçti; hata/istisna/atlama yok. assembleDebug ile QA APK derlemesi başarılı. Bu tur cihaz testi yapılmadı.

## Kapsam ve kalan işler

Bu metin/bağlantı mantığı yerel kabulüdür. Fiziksel bildirim tepsisi, kesilme/uzun metin, teslim/kanal/izin, Activity ve tüm dil/tema kabulü bu tur yapılmadı; cihaz testi tekrarlanmadı. Daha önce saklanmış bildirimler yeniden yazılmaz. Tam Worker ağ/veritabanı akışı entegrasyon kabulü değildir.

Sıradaki açık inceleme: günlük bildirim anahtarı/dateStr ve eventAt hâlâ cihaz saat dilimiyle oluşturuluyor; konum tarihine göre gün sınırı ve tekil teslim davranışı birlikte test edilmeli. Kalan görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler, aralıklı Activity/profil kök nedenleri ve yayın kapıları açık. Push yapılmadı.