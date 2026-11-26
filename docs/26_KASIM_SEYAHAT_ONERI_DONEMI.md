# 26 Kasım — Seyahat önerisinde hedef günü ve dönem bilgisi

## Yerel seyahat yanıtı
Yaklaşan plan seçimi cihazın gününden ayrıldı. Her aday planın snapshot saat dilimi doğrulanıp o hedefin yerel günü hesaplanıyor. Arşivlenmiş planlar hariç tutuluyor. Açık bir şehir sorulmadığında, saat dilimi bilinmeyen aday varsa seyahat yokmuş gibi konuşmak veya keyfî plan seçmek yerine analiz yenileme uyarısı veriliyor. Şehir açıkça sorulmuşsa genel analiz yönlendirmesi korunuyor; mevcut şehir havasından yolculuk koşulu çıkarılmıyor.

## Eski seyahat önerisi yordamı
Üretim kaynaklarında generateTravelRecommendation çağrısı bulunmadı; yalnızca test çağrıları var. Bu değişiklik tam ekran/kart kabulü olarak sayılmıyor. Yordamın kendisi artık enjekte edilen Instant ve hedef snapshot saat dilimiyle çalışıyor. Bozuk tarih aralığı ve bilinmeyen bölge açıkça belirtiliyor. Tamamlanmış seyahat yaklaşan diye anlatılmıyor. Devam eden planın gün sayısı hedef tarihine göre hesaplanıyor; dönem özeti bugünkü/güncel ölçüm diye sunulmuyor. Sayısal doğrulama ve dönem anlatımı ortak travelWeatherAdvice üzerinden geliyor; yinelenen eski maksimum-sıcaklık metni kaldırıldı. previousSnapshot parametresi eski çağrı uyumluluğu için duruyor, yeni bir karşılaştırma iddiası yok.

## Testler
TravelRecommendationClockTest yedi yeni regresyon: bilinmeyen hedef bölgesi, hedef yerel günü/gün sayısı, bütün tonlarda dönem ifadesi, tamamlanmış plan, aşırı sıcaklık, aday planların kendi bölgesine göre seçimi ve bilinmeyen planı yok saymama. Yedisi de eski davranışta başarısızdı. Eski maksimum testi sabit now ve açık bölgeyle çalışacak şekilde güncellendi; eksik sıcaklık ve gerçek sıfır kontrolleri korundu.

Son kaynak: 870 JVM testi, sıfır hata/atlama. testDebugUnitTest ve assembleDebug başarılı; git diff --check temiz. Yeni fiziksel/uzak servis testi yapılmadı.

## Açık ve sıradaki
Valiz akışında aday seçimi hâlâ mevcut hava şehrinin gününü kullanıyor; hedefin yerel günüyle birleştirme gerekiyor. Snapshot güncelliği ve tarih aralığı/hedef eşleşmesi ayrıca doğrulanmalı. Aynı başlangıç tarihli birden çok uygun planın seçim politikası ve çağrısız eski öneri yordamının kaldırılması ayrıca değerlendirilecek. Diğer sayı ayrıştırıcıları, nötr hero dekorasyonu, gerçek Activity akışı, tüm tema/ekran ve iç içe dikdörtgen görsel matrisi, canlı servisler ve yayın kapıları açık. Push yapılmadı.
