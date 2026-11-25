# 25 Kasım 2026 — Güzergâh hava açıklaması ve hesaplama tekrarları

## Gündüz/gece açıklamalarının doğruluğu

Eski routeWeatherDisplayName cihaz saat dilimi ve sabit 06/20 saatlerinden güneşli/gece çıkarıyordu. Her noktanın kendi saatlik is_day verisi olmadığı halde aynı ETA farklı cihazlarda farklı açıklama alabiliyordu; ETA eksikse bilinen fırtına açıklaması da kayboluyordu.

Tekil ve toplu rota saatlik istekleri artık is_day alanını ister. Saatlik rota veri modeli optional dizi taşır; RouteWeatherProvider yalnız örneklenen aynı idx için 0/1 değerini Boolean olarak WaypointWeather'a aktarır. Eksik veya farklı sayısal değer bilinmeyen kalır. Eski yanıtlarla uyumludur; rota domain verisi kalıcı şemaya eklenmedi.

Gündüz/gece metni artık cihaz/çıkış/varış saati tahminiyle üretilmez. Bilgi yoksa Açık/Az bulutlu gibi nötr metin kullanılır; bilinen kar/fırtına/sis koşulu korunur. Açık gece ay simgesi alır; açık ama gündüz/gece bilinmeyen veri güneş simgesiyle sunulmaz. Tekrarlı zaman tabanlı eski yardımcı kaldırıldı. [Open-Meteo resmî dokümanı](https://open-meteo.com/en/docs) ve [resmî OpenAPI şeması](https://github.com/open-meteo/open-meteo/blob/main/openapi/forecast.yml) saatlik is_day alanını destekliyor; 1 gündüz, 0 gece bilgisidir. Sözleşme 25 Kasım kontrol edildi; canlı uygulama HTTP kabulü ayrı kalır.

## Gereksiz rota hesaplamalarının önlenmesi

DAO her seyahat metaverisi güncellemesinde calculateRoute çağırıyor; mevcut rota, analiz ve çıkış saati sıfırlanıyor, ağ/saat çözümü tekrarlanıyordu. RouteCalculationGate gerçek girdileri karşılaştırır: kullanıcı/seyahat kimliği, başlangıç/varış koordinatı, yer etiketleri, başlangıç/bitiş günü ve hareket saati. Bunlar değişince iptal/sıfırlama ve yeni hesap devam eder. Not, puan, güncelleme zamanı, bildirim takibi veya varış hava snapshot'ı değişince mevcut rota/analiz korunur; _trip yine güncellenir.

Yeni loadTrip/Rotayı yenile ve kaydın silinmesi kapıyı sıfırlar; hata sonrası aynı girdilerle açık yeniden deneme engellenmez. Seçilmemiş başlangıçta cihaz konumunun yeniden alınması manuel yenilemeye bağlıdır; metaveri olayıyla başlangıç sessizce değiştirilmez. Canlı ağ çağrı sayısı ve profiler ölçümü bu tur yapılmadı; optimizasyon kaynak bağlantısı ve girdi kapısıyla doğrulanır.

## Test kanıtı

İlk dokuz yeni JVM regresyonunun altısı eski davranışta başarısız oldu. Son kapsam altı kapı testi ve altı gün/gece/provider testidir. Tekil/toplu is_day isteği, nokta-idx ayrımı, eksik/geçersiz faz, açık gece simgesi, bilinen tehlike, saat diliminden bağımsızlık, yinelenen/metaveri girdisi, gerçek rota değişimi, kullanıcı/seyahat ayrımı ve açık retry kapsanır. Eski sağlayıcı testi artık desteklenmeyen cihaz saatinden gece varsayımı yerine sağlayıcı kanıtını doğrular.

Son kaynakla 805 JVM testi ve SM-T500 üzerinde 8 izole cihaz senaryosu geçti; hata/istisna/atlama yok. QA APK ve test APK derlemesi başarılı. Yeni cihaz senaryosu gerçek TimelineWaypointItem bileşeninde bilinmeyen → gece → gündüz değişiminde metin ve simgenin birlikte yenilenmesini kontrol eder; önceki yedi rota ekran/etkileşim senaryosu da koşulur.

## Açık kabul ve sıra

Kapsam yerel sağlayıcı/metin/girdi mantığı ve izole Compose bileşenleridir; gerçek ViewModel+Room+MapLibre+ağ/konum/hesap değişimi ve canlı provider kabulü ayrı kalır. Not değişiminin gerçek HTTP sayacı/profiler etkisi ölçülmedi. Zaman çizelgesi önceki turdaki açık çıkış saati temelini kullanmaya devam eder; ara/varışın kendi yerel saatleri ayrı açık iştir.

Sıradaki inceleme: RouteWeatherProvider geçersiz sıcaklık/yüzde/rüzgar alanlarını nasıl geçiriyor ve sürüş riski değerlendirmesinde kuvvetli rüzgar atlanıyor mu. Genel görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler, aralıklı Activity/profil kök nedenleri ve yayın kapıları açık. Push yapılmadı.