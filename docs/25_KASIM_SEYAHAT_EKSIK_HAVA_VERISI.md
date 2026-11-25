# 25 Kasım 2026 — Seyahat önerilerinde eksik hava verisi

## Sorun ve düzeltme

TravelAiHelper boş alanlara yağış/rüzgar için sıfır, sıcaklık için 20/15 derece, durum için açık hava atıyordu. Tahmin hiç yoksa elverişli/güzel dönem veya mevsim normalleri gibi veriyle desteklenmeyen yorumlar üretebiliyordu. Valiz önerisi eksik veriyle serin akşam/ceket çıkarımı yapıyordu. Karşılaştırma da eksik yağış olasılığını sıfıra çevirerek artış/azalış bildiriyordu.

- Hava ve valiz önerisinin ortak doğrulaması TravelWeatherAdvice.kt içine alındı. Eksik veri bilinmeyen olarak kalır. Sonlu olmayan, fiziksel sınırlar dışındaki sıcaklık/rüzgar/UV, yüzde dışındaki yağış ve ters min/max aralığı önerilerde kullanılmaz.
- Mevcut sıcaklık, yağış olasılığı ve rüzgar değerleri ayrı ayrı gösterilir; eksik alanlar açıkça belirtilir. Hava uygunluğu hakkında genel olumlu hüküm verilmez. Tahmin yokken servisin neden yanıt vermediği veya mevsim koşulları uydurulmaz.
- Bilinen tehlikeli hava kodları ve kritik sıcaklık aralığı uyarıları korunur. Diğer alanlar eksik olsa da fırtına/sis/don/kar bilgisi kaybolmaz. UV uyarısı yalnız geçerli UV verisiyle çıkar.
- Valiz için eldeki yağış, soğuk, rüzgar, sıcaklık farkı ve yüksek UV verileri birlikte değerlendirilir; tek koşul diğer hazırlıkları bastırmaz. Eksik veriyle mevsimlik kıyafet dayatılmaz. İzinli kişisel ekipman önerisi korunur.
- Yağış değişimi yalnız eski ve yeni olasılık geçerliyse hesaplanır. Eksik karşılaştırmada havanın değişmediği iddia edilmez. Belirgin değişim yok metni yalnız karşılaştırılan yağış ve skoru anlatır.

## Test kanıtı

Sekiz yeni JVM regresyonu eklendi. İlk kaynakta beşi başarısız olarak sorunları yeniden üretti; bilinen fırtına ve yağış hazırlığı gibi çalışan davranışlar kontrol olarak korundu. Tüm üsluplarda tahmin yok/boş/geçersiz/NaN/sonsuz değer, kısmi sıcaklık, eksik alanlarla fırtına, valiz ve çift yönlü eksik yağış karşılaştırması kapsandı.

Son kaynakla 727 JVM testi geçti: hata, istisna ve atlama yok. assembleDebug ile QA APK derlemesi de başarılı. Üretim imzalı release/AAB kabulü değildir.

## Sınırlar ve sıra

Bu çalışma yerel Kotlin öneri üretimi ve karşılaştırma mantığıdır; canlı hava servisi, tam seyahat ekranı, tüm temalar ve tablet görsel kabulü bu tur yeniden çalıştırılmadı. Önceden kaydedilmiş eski öneriler kendiliğinden yeniden yazılmaz. Tahmin kodları, hava verisinin güncelliği/tarih kapsamı ve kaynağın doğruluğu ayrı kabul kapılarıdır. Genel yayın tamamlandı sayılmaz.

Sıradaki inceleme: rehber/takvim tüketicilerinde eski veya kısmi analiz etiketleri ve önerilerin güncellenmesi; ardından açık görsel matris (iç içe dikdörtgenler dahil). Önceki canlı Firebase/backend, aralıklı Activity/profil kök nedenleri, release ve mağaza kapıları açık kalır. Push yapılmadı.