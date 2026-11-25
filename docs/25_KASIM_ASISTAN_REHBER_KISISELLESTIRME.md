# 25 Kasım 2026 — Asistan ve seyahat rehberi kişiselleştirmesi

## Düzeltilen davranışlar

- Seyahat analizi, kalıcı kişiselleştirme kapalı olsa da açık varsayan profil oluşturuyordu. Asistan, analiz ve rehber artık aynı kullanıcıya ait tek DataStore görüntüsünden izin ve ilgi listesini birlikte okuyor. Boş/eski kullanıcı kimliği kapalı; rehberin ilk yükleme durumu tercihsiz. Rehber profil kimliği seyahat sahibiyle eşleşmezse tercihler uygulanmıyor.
- Fotoğraf hazırlığı Türkçe bir alt dize aradığı için kayıtlı bulut_fotografciligi/gun_batimi kimliklerini kaçırıyordu. Bilinen kimlikler ortak hazırlık kuralına bağlandı; trekking için koşu yerine yürüyüş ekipmanı öneriliyor. Kişiselleştirme kapatılınca kişisel ekipman önerisi kaldırılıyor. Genel sıcaklık farkı uyarısı izin kapalıyken de korunuyor.
- Rehber deneyimleri yalnızca seyahat türüne göre seçiliyordu. Mevcut doğrulanmış içerik içinde bir yer seyahat amacına ayrılıyor, kalan seçim bilinen ilgi alanlarına göre sıralanıyor. Paketli ve yayınlanmış rehber aynı seçiciyi kullanıyor. Analiz ve görünür rehber aynı tercihlerle aynı deneyimleri seçiyor. Yeni mekân, etkinlik veya ilçe içeriği uydurulmuyor.
- Migren, polen, UV/basınç hassasiyeti kimlikleri genel ilgi listesi üzerinden bağımsız sağlık iznini aşabiliyordu. Android gönderim/yeniden deneme filtresi ve backend model bağlamı bu bilinen seçimleri sağlık iznine bağlıyor. Eski virgüllü ilgi alanındaki bilinen karşılıklar da süzülüyor. Kişiselleştirme kapalıyken seçili ilgi/seyahat/sağlık bağlamı gönderilmiyor; şehir ve hava bağlamı korunuyor.

## Doğrulama

Son kaynakla 719 Android JVM testi ve SM-T500 üzerinde 2 izole cihaz testi geçti; hata/atlama yok. İlk son derleme Context/DataStore aşırı yükü nedeniyle Application tipini çıkaramadı; açık Application tipiyle düzeltildi ve tüm Android kontrolleri yeniden geçti. Backend yerel koşumunda 95 test geçti: test_personalization_context.py, test_platform_v3.py, test_chatbot_bridge.py, test_assistant_consent.py. Köprüye gerçekten seri hale getirilen profil bağlamı MockTransport ile kontrol edildi; canlı model çağrılmadı.

Yeni regresyonlar: kapalı tercihin analiz ve rehberde etkisizliği, fotoğraf/kamp/yürüyüş kimlikleri, seyahat amacının korunması, eksik ilçede içerik üretmeme, yayınlanmış/paketli seçici eşitliği, sağlık/unknown kimliklerinin rehber yönlendirmemesi, genel hava uyarısının korunması; ayrı sağlık izni, geri çekme ve sabit istek kimliği. Cihaz senaryoları kendi izole DataStore dosyasını yeniden açar, A/B ayrımını kontrol eder ve gerçek rehber bileşeninde tercih değişikliğini gözler.

## Kabul sınırları ve açık işler

- Bu kabul yerel servis testleri ve izole Compose/DataStore senaryolarıdır. Tam canlı giriş/sekme geçişi, süreç öldürme, Firebase/backend dağıtımı ve tüm cihaz/tema erişilebilirlik matrisi tamamlanmış sayılmaz.
- Önceden saklanmış analiz metinleri geriye dönük silinmez veya yeniden hesaplanmaz. Yeni analiz üretimi ve güncel rehber seçimi/hazırlık önerisi yeni tercihi kullanır.
- Sağlık süzgeci bilinen yapılandırılmış seçimleri ve eski kesin karşılıkları kapsar. Serbest soru/profil metninden evrensel sağlık çıkarımı veya anlamsal sansür yapılmaz.
- Sıradaki yerel öncelik: TravelAiHelper eksik tahmin alanlarını 20 derece/sıfır yağış/sıfır rüzgâr/açık gibi varsayabiliyor; tahmin yokken olumlu hava ifadesi de üretebiliyor. Eksik veri güvenliğinin regresyonları ve düzeltmesi açık.
- Önceki aralıklı Activity yeniden oluşturma/ilk çevrimdışı profil bekleme kök nedenleri, kalan görsel matris, canlı bağlantılar, imzalı release ve mağaza/yasal kapılar açık. Bu rapor genel yayın onayı değildir. Push yapılmadı.