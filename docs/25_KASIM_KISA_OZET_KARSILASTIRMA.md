# 25 Kasım 2026 — Kısa özet ve karşılaştırma doğruluğu

## Düzeltilen davranış

RecommendationEngine içindeki eski kısa özet fallback'i sıcaklık ve yağış yüzdesini doğrulamıyordu. Bu yardımcı mevcut kart/rehber tüketicilerinde artık kullanılmıyor; uyumluluk amacıyla korunuyor. Boş olmayan kayıtlı uyarı metni korunur; sayısal fallback artık sonlu/sınır içi sıcaklık ve 0..100 yağış olasılığı gerektirir. Geçersiz veri bilinmeyen olarak belirtilir; gerçek sıfır korunur.

TravelAiHelper.generateComparisonText analiz kaydında ve bildirim verisi hazırlanırken kullanılıyor. Skor değerleri doğrulanmadan çıkarılıyordu; bazı üsluplarda skor artışı genel hava iyileşmesi/termal stabilite, yağış düşüşü açık gökyüzü/bulut azalması gibi desteklenmeyen sonuçlara çevriliyordu.

- Skor ve yağış yalnız iki tarafta da 0..100 ise karşılaştırılır; çıkarma bu doğrulamadan sonra yapılır. Mevcut 5 skor/10 yağış değişim eşiği korunur.
- Metin değişen ölçümü ve önceki/yeni değerini açıklar; skor yüzde veya meteorolojik güvenlik hükmü olarak sunulmaz.
- Bir alan karşılaştırılabilir olsa da diğer eksik/geçersiz alan açıkça belirtilir. Yeterli veri yokken hava değişmedi iddiası yoktur.
- Yeni kayıttaki bilinen tehlike uyarısı karşılaştırmaya eklenir; skor artışı kar/sis/fırtınayı bastırmaz.
- Her iki kaydın pozitif zaman damgası varsa, yeni kabul edilen kayıt daha eski olduğunda değişim karşılaştırılmaz. Zaman damgası olmayan eski kayıtlar için zaman sırası kanıtlanmış sayılmaz; mevcut uyumluluk davranışı korunur.

## Test kanıtı

Altı yeni JVM senaryosunun dördü ilk kaynakta hatayı yeniden üretti. Tüm üsluplarda skor taşması/sınır dışı skor, fırtınayla skor artışı, yağıştan bulut çıkarımı, eski gelen snapshot, sayısal fallback ve gerçek sıfır/kayıtlı tehlike metni kontrol edildi. Sınır dışı skor testi taşma dışındaki -1→110 aralığıyla da güçlendirildi.

Son kaynakla 754 JVM testi geçti; hata/istisna/atlama yok. assembleDebug ile QA APK derlemesi başarılı. Bu tur cihaz testi çalıştırılmadı.

## Sınırlar ve sonraki sıra

Bu saf Kotlin metin üretimi düzeltmesidir; sistem bildirim tepsisi, teslim/izin/kanal ve tam Activity akışı bu tur tekrar test edilmedi. Yeni fiziksel cihaz testi gerektiren UI değişikliği yapılmadı. Zaman damgası aynı konumu/tarih aralığını kanıtlamaz; veri kaynağı ve tam canlı kabul ayrı kalır. Saklanmış eski özet/karşılaştırma metinleri yeniden yazılmaz.

Sıradaki bulgu: TravelNotificationWorker.buildNotificationData ekipman önerilerinde ayrı sıcaklık/yağış varsayımları ve yalnız sıcaklıktan güneş koruması çıkarımı bulunuyor. Bildirim hazırlığını ortak güvenli hava kurallarıyla karşılaştırıp test etmek sırada. Diğer görsel matris, geçmiş dialog büyük yazı/küçük ekran, iç içe dikdörtgenler, canlı servisler, aralıklı Activity/profil kök nedenleri ve yayın kapıları açık. Push yapılmadı.