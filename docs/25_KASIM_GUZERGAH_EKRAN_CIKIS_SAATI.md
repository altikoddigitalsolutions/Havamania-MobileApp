# 25 Kasım 2026 — Güzergâh ekranında çıkış saati

## Değişiklik

TravelRouteViewModel hareket anını cihaz saatinden, ekran kilidi ve zaman çizelgesi ise ayrıca cihaz günü/saatinden üretiyordu. Ekran artık açık RouteDepartureUiState ile saat dilimi yükleme, çözülmüş hareket anı ve hata durumlarını ayırır.

- Rota için gerçekten kullanılan başlangıç noktası (seçili çıkış veya izinli cihaz konumu) saat dilimi çözümüne verilir. Eski seçili koordinat, kullanılan gerçek konumun yerine geçmez. Aynı dar servis yordamı, 10 saniye zaman aşımı ve DST-aware hareket saati kullanılır.
- Rota yenilenince eski hareket anı/saat durumu sıfırlanır. Geçerli kalkış saati varsa başlangıçtan itibaren yükleme durumu kullanılır; rota hazırlanırken erken hata/tekrar eylemi sunulmaz. İptal edilen eski isteğin sonucu ensureActive kontrolünden geçemez. Saat çözümü yokken analiz düğmesi açılmaz; hata durumunda yeniden çöz eylemi vardır. Hava örnekleme aynı çözülmüş hareket milisaniyesine dayanır.
- ViewModel ve UI ortak routeClockEligible politikasını kullanır: bilinmeyen saat kapalı; tam 48 geçen saat; önceden var olan devam eden seyahat erişimi korunur; geçmiş durum çıkış konum gününe göre hesaplanır. Eski TravelStatusResolver API'sı da açık ZoneId ister ve aynı DST-aware hesaba yönlenir; varsayılan cihaz bölgesi yoktur.
- Ekran görünür/RESUMED durumdayken saat en fazla 30 saniyede yenilenir; arka planda periyodik güncelleme durur, dönüşte güncel saat okunur.
- Başlangıç, ara nokta, varış, tekrarlanan geçiş seçimi ve ayrıntı saatleri açık çıkış saat dilimiyle biçimlenir; gün geçişi kaybolmasın diye tarih de yazılır. Zaman çizelgesi hangi saat dilimini kullandığını açıklar. Bu etiketler ara/varış konumunun yerel saati iddiası değildir. Bilinmeyen saat cihaz saatine çevrilmez. Dar zaman sütunu büyük yazıya göre genişler.

## Doğrulama

Sekiz yeni JVM senaryosu bilinmeyen kilit, 48 saat sınırı, devam eden/geçmiş seyahat, cihaz saat diliminden bağımsız metin, eksik zaman, DST hatası, gerçek başlangıç koordinatı ve provider hatasını kapsar. Yeni iki Compose cihaz testi yükleme/hata/yeniden deneme/analize açılma ve 320 dp/koyu/yüzde160 yazıda tarih-saat etiketini kontrol eder. Önceki beş rota retry/konum/seçim/ayrıntı testi regresyon olarak çalıştırılır; eski fixture açık saat bilgisi sağlayacak şekilde güncellendi.

793 JVM testi ve SM-T500 üzerinde 7 izole cihaz senaryosu hatasız geçti. Son ViewModel başlangıç yükleme korumasından sonra 793 JVM ve QA APK derlemesi yeniden geçti; bu son tek durum başlatma değişikliği için cihaz paketi tekrarlanmadı. Cihaz koşumu iki yeni ekran/çizelge ve beş önceki rota bileşeni testidir; tam ViewModel entegrasyonu değildir.

## Sınırlar ve sıra

Bu kabul saf saat mantığı ve izole gerçek bileşenlerdir; tam ViewModel/Firebase/ağ/konum izni/MapLibre/Activity entegrasyonu, canlı timezone servisi ve tüm telefon/tablet/yatay/TalkBack kapsamı değildir. Yerel snapshot kayıtları değişmedi. UI'daki ongoing erişim ile arka plan bildirimindeki yalnız hareket öncesi 48 saat penceresi eski davranışlarıdır; birbirine eşitlenmedi.

Ara/varış konumlarının kendi yerel saatini göstermek için o noktaların saat dilimi ayrıca gerekir; şimdiki gösterim açıkça çıkış saatidir. routeWeatherDisplayName hâlâ gün/gece açıklamasını cihaz saatiyle türetebiliyor; bu ayrı içerik doğruluğu işi sırada. Ayrıca DAO metaveri güncellemelerinde rota/saat çözümünün gereksiz tekrarını önlemek ve tam ViewModel iptal/hesap değişimi kabulü açık. Önceki görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servisler ve yayın kapıları açık. Push yapılmadı.