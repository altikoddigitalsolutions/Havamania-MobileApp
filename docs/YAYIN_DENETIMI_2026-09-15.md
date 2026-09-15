# Havamania — Android / Google Play yayın denetimi

Tarih: 15 Eylül 2026. Hedef: önce Android / Google Play.

**Güncel takip:** Bu rapordaki ilk turdan sonra hesap silme, veritabanı geçişleri, koordinat önbelleği ve saat dilimi düzeltmeleri eklendi. Son test sonuçları ve kalan öncelikler [yayın öncelikleri](YAYIN_ONCELIKLERI.md) dosyasındadır.

## Karar

**Henüz üretime yayın onayı verilmedi.** Kodda önemli hatalar giderildi; aşağıdaki canlı servis, imzalama, veri silme ve cihaz kontrolleri tamamlanmalıdır. Birim testlerin geçmesi uygulamanın tüm cihazlarda hatasız olduğu anlamına gelmez.

Depodaki Android uygulaması Kotlin / Jetpack Compose kullanıyor. Launcher `WeatherPremiumActivity`; React Native kaynakları Android'in çalışan uygulamasıyla aynı uygulama katmanı değil. README ve CLAUDE.md mimari anlatımı güncel Android yapısını tam yansıtmıyor. iOS/React Native yayını bu denetimin hedefi değil.

## Giderilen sorunlar

| Öncelik | Bulgu ve etkisi | Değişiklik / kanıt |
| --- | --- | --- |
| P1 | Debug derleme release anahtarı yokken yapılandırmada çöküyordu. | İmzalama kontrolü release görevlerine taşındı. Debug APK üretildi. Release anahtarı zorunluluğu korundu. |
| P1 | API 35 hedefi güncel yeni uygulama gönderim şartını karşılamıyor. | compile/target SDK 36; AGP 8.10.1, Gradle 8.11.1; makineye özel JDK yolu kaldırıldı. |
| P1 | MapLibre 11.5.2 yerel kütüphaneleri 4 KB hizalıydı. | 11.8.0'a güncellendi; son APK içindeki altı ARM64/x86_64 kütüphanenin tümünde 16 KB ELF hizalaması doğrulandı. CI'a kontrol eklendi. |
| P1 | Eksik uzunluktaki saatlik/günlük diziler `IndexOutOfBoundsException` oluşturuyordu. | Eksik zorunlu alanı olan satırlar atlanıyor, opsiyonel yağış verisi güvenli okunuyor. Regresyon testi eklendi. |
| P1 | Gün doğumu bulunup gün batımı bulunmayan veri çöküyordu. | İki alan bağımsız okunuyor. Regresyon testi eklendi. |
| P1 | Güzergâh toplu hava isteği koordinatları tekrarlanan parametreler olarak gönderiyordu. | Open-Meteo biçimine uygun virgülle ayrılmış koordinatlar kullanılıyor; tek nokta nesne yanıtı ayrı işleniyor. Üç sağlayıcı testi eklendi. |
| P1 | Tahmin aralığının dışındaki seyahate son mevcut tahmin gösterilebiliyordu. | Hedef saate en fazla 60 dakika uzak tahmin kabul ediliyor; aralık dışı veri gösterilmiyor. |
| P1 | Firebase oturumu, backend'in kendi JWT doğrulamasıyla uyumsuzdu. | Firebase Admin ile imza/proje/iptal doğrulaması ve proje+UID üzerinden kararlı kullanıcı eşlemesi eklendi. E-posta üzerinden otomatik hesap bağlanmıyor. Üç test eklendi. |
| P1 | Profil yüklemesinde yakalanmayan ağ hatası ve tekrarlanan kolektörler vardı. | Tek profil gözlem işi tutuluyor; hatada kullanıcıya tekrar denenebilir durum gösteriliyor; çıkışta eski gözlem temizleniyor. |
| P1 | Hesap değişiminde önceki kullanıcıya ait profil, seyahat veya kişiselleştirme geç sonuçlarla görünebiliyordu. | İlgili işler iptal ediliyor, kullanıcı kimliği yeniden kontrol ediliyor, kişisel durum sıfırlanıyor. Firestore seyahat sahibinin kimliği belge yolundan alınıyor. |
| P1 | Storage kuralı silme işleminde olmayan `request.resource` alanını kontrol ediyordu. | Silme yetkisi sahiplik kontrolüyle ayrı tanımlandı; okuma kullanıcıya sınırlandı. `firebase.json` Storage kurallarını da içeriyor. Canlıya uygulanmadı. |
| P2 | Şehir aramalarında eski yanıt yeni sonucu ezebiliyordu. | 300 ms bekleme ve önceki arama işini iptal etme eklendi. |
| P2 | Yenileme önce çevrimdışı hava önbelleğini siliyordu. | Önbellek korunarak zorunlu yenileme yapılıyor; yenileme göstergesi `finally` ile kapanıyor. |
| P2 | Coroutine iptali sıradan hata gibi yutuluyordu. | İncelenen hava, rota ve profil akışlarında iptal yukarı aktarılıyor. GPS isteği de iptal belirtecine bağlandı. |
| P2 | Ağ tekrar denemesinde önceki kapatılmış yanıt geri dönebiliyordu. | Yanıt referansı kapatıldıktan sonra sıfırlanıyor; iptal edilmiş çağrı tekrar denenmiyor. |
| P2 | Harita stili yeniden çizimlerde tekrar yükleniyor ve ekran kapanırken yerel kaynaklar bırakılmıyordu. | Stil yükleme MapView başına bir kez yapılıyor; ekran dispose aşamasında MapView durdurulup kapatılıyor. |
| P2 | Büyük yazı ve uzun metin ana butonda kırpılabiliyordu. | Minimum 48 dp yükseklik ve metne göre büyüme; tek satır kırpması kaldırıldı. Cihazda görsel kontrol gerektirir. |
| P2 | Panoramik fotoğraf gereğinden büyük çözülüyor; aşırı dar fotoğraf 0 piksel boyut üretebiliyordu. | Büyük boyuta göre örnekleme, minimum 1 piksel ve arka planda JPEG sıkıştırma eklendi. |
| P2 | Son kullanıcıya Firebase/Blaze altyapı hatası gösteriliyordu. | Tekrar denemeyi anlatan kullanıcı mesajı kullanılıyor. |
| P2 | Saatlik ve günlük sıcaklıklar ana karttan farklı yuvarlanıyordu. | Aynı `roundToInt` davranışı kullanılıyor. |
| P2 | Backend istek sınırlandırıcısının anahtar limiti hiçbir zaman çalışmıyordu. | Yeni anahtar eklenmeden önce kapasite denetleniyor. Kapasite ve 429 testi eklendi. |
| P2 | Backend hava önbelleği sınırsız büyüyebiliyordu. | En fazla 1024 kayıt, kilitli erişim, en az kullanılanı çıkarma; iki test eklendi. |
| P2 | Backend Firebase Admin bağımlılığı eksikti; test toplama aşaması duruyordu. | İki bağımlılık listesine Firebase Admin eklendi; bcrypt sürüm kısıtı eşitlendi. CI test ortamı açıkça belirtildi. |
| P1 | Backend avatar uç noktası boyutsuz dosya kopyalıyor ve SVG/istemci uzantısı kabul ediyordu. | 5 MB üst sınır, JPEG/PNG/WebP türü ve imza kontrolü, sunucunun belirlediği uzantı ve senkron endpoint iş parçacığı kullanılıyor. Üç test eklendi. |

## Yayını durduran / dış yapılandırma gerektiren maddeler

1. **Release imzası:** `keystore.properties` ve gerçek yükleme anahtarı bu klonda yok. İmzalı ve küçültülmüş release AAB üretilip çalıştırılmadı. Debug APK yayın paketi değildir. Mevcut Play kaydı varsa uygulama kimliği, anahtar ve versionCode eşleşmesi doğrulanmalı.
2. **Firebase ve backend:** Android `https://api.havamania.com/` kullanıyor. Bu alan adının depodaki backend'i çalıştırdığı doğrulanmadı. `FIREBASE_PROJECT_ID` Android Firebase projesiyle aynı olmalı; sunucuda Application Default Credentials ve token iptal kontrolü için yetki bulunmalı. Gerçek tokenlarla canlı uçtan uca asistan testi yapılmadı. Firebase takma adları/değerleri tahmin edilerek yapılandırılmadı.
3. **Hesap silme:** Sunucu tarafından yönetilen, devam anahtarlı ve cihazdaki temizlikten sonra tamamlanan akış eklendi. Yerel testleri yapıldı; canlı özellik kapalıdır. Dağıtım sırası ve kalan kontroller [hesap silme raporunda](HESAP_SILME_VE_SUNUCU_GECISLERI.md). Gerçek test hesabıyla uçtan uca doğrulama ayrıca gereklidir.
4. **Kurallar:** Firestore/Storage sahiplik ve silme engeli için dört yerel emülatör testi geçti. Kurallar Firebase'e yayınlanmadı; canlı projeyle fark kontrolü gerekli. Download token içeren fotoğraf URL'leri bağlantıyı bilenlere erişim sağlayabilir; gizlilik modeli buna göre belirlenmeli.
5. **Ticari servisler:** Open-Meteo ücretsiz uç noktası ve OSRM demo sunucusu kullanılıyor. Ticari kullanım sözleşmesi/kota ve üretim rota altyapısı kararı gerekli; ödeme veya servis satın alımı yapılmadı.
6. **Yasal sayfalar:** Kodda `/privacy`, `/terms`, `/kvkk` bağlantıları var. Web aracıyla privacy/terms içerikleri doğrulanamadı; bu sonuç tek başına sayfaların kapalı olduğunu kanıtlamaz. Sayfalar tarayıcıdan açılıp veri toplama, Firebase, konum, fotoğraf, AI sağlayıcısı ve saklama süreleriyle karşılaştırılmalı.
7. **Google Play beyanları:** Data Safety, içerik derecelendirme, uygulama erişimi/test hesabı, hesap silme web bağlantısı, izin açıklamaları ve mağaza görselleri Play Console'da kontrol edilmedi.
8. **Premium:** Android'de yaklaşan özellik sunumu bulunuyor; gerçek satın alma yok. Backend makbuz ve webhook uç noktaları üretimde kapalı, üretim dışı ortamlarda demo davranışı var. Gerçek mağaza doğrulaması olmadan bu uç noktalar üretimde açılmamalı.

## Diğer açık teknik bulgular

- Backend istek başına DDL kaldırıldı; token tekilliği Alembic 0012 geçişine taşındı. Eksik profil kolonları 0013 ile eklendi. Gerçek Alembic zinciri SQLite üzerinde doğrulandı; üretim PostgreSQL yükseltme/eşzamanlılık testi halen gereklidir.
- Backend avatarında uygulama düzeyinde sınır ve dosya imzası kontrolü var. Ters proxy/gateway üzerinde toplam istek boyutu sınırı, antivirüs/decode doğrulaması ve eski avatar temizliği ayrıca yapılandırılmalı.
- Rate limit süreç içidir; birden çok worker/replica için ortak sayaç ve güvenilir proxy yapılandırması gerekir. Kapasite sınırı bu mimari ihtiyacı çözmez.
- Hava önbelleği artık koordinatları içerir ve 100 kayıtla sınırlıdır. Seçili saat konumun saat dilimine göre hesaplanır, rota istekleri UTC kullanır. Tüm astronomi/tarih gösterimlerinin farklı saat dilimi ve yaz saati senaryolarında görsel testi halen gereklidir.
- Güzergâh riskleri sınırlı ara nokta sayısına indirgeniyor. Şiddetli hava noktalarının seçimin dışında kalmadığı, eksik verinin güvenli koşul gibi sunulmadığı senaryo testleri gerekli.
- Java/Kotlin deprecation uyarıları mevcut. Bunlar tek başına çökme kanıtı değildir; özellikle MapLibre annotation API ve eski geocoder API geçişi planlanmalı.
- React Native testinde bulunmayan `SignUpScreen` import'u var; eski mobil CI işi ayrı bir bakım sorunu. Android yayını native Android çıktısıyla yapılmalı.
- Backend Ruff genel denetimi temizlendi: import düzeni, kullanılmayan değişkenler, exception/log kullanımı ve test saat dilimi düzeltildi. FastAPI'nin deklaratif `Depends`/`Query`/`File` çağrıları lint yapılandırmasında tanındı; tüm backend Ruff denetimi geçiyor.

## Cihaz / tasarım test matrisi

Her madde hem açık hem koyu tema ile, mümkün olduğunda 320/360 dp telefon ve tablet genişliğinde denenmeli:

- Soğuk açılış; çevrimdışı açılış; kayıt/giriş/şifre sıfırlama; profil yok veya ağ hatalıyken açılış.
- Hesap A → çıkış → hesap B; profil, seyahat, bildirim ve AI geçmişinin karışmaması.
- Konum reddi; yaklaşık konum; GPS kapalı; izin sonradan iptal; varsayılan şehir olmayan kullanıcı.
- Hızlı şehir arama/değiştirme, yenileme sırasında ekran değiştirme, uçak modu, eski önbellek.
- Seyahat oluşturma/düzenleme/silme, geçmiş ve ileri tarih, tahmin sınırı dışı tarih, saat dilimi değişimi.
- Harita aç/kapat, geri dön, ekran döndür, arka plana al, yeniden aç; rota bulunamaması.
- Fotoğraf ekleme/kaldırma, büyük/panoramik dosya, yarım kalan yükleme, hesap silme.
- %100/%150/%200 yazı boyutu; klavye açık formlar; TalkBack etiketleri; dokunma alanları; açık temada kontrast.
- Android 13+ bildirim izni, WorkManager kısıtları, pil tasarrufu ve bildirimden doğru ekrana yönlendirme.
- İmzalı release sürümünde R8, Firebase, ağ güvenliği, soğuk açılış ve en az bir 16 KB Android ortamı.

## Tekrar çalıştırılabilir kontroller

```powershell
# JDK 17 ile mobile/android içinde
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --no-daemon --max-workers=2

# Repo kökünde
python tools/check_apk_alignment.py mobile/android/app/build/outputs/apk/debug/app-debug.apk

# backend içinde, proje sanal ortamı etkinleştirildikten sonra
$env:APP_ENV='test'
python -m pytest -q
python -m ruff check app tests
```

ELF kontrolü yalnızca kütüphane segment hizalamasını doğrular; ayrıca `zipalign -c -P 16 -v 4` ve gerçek 16 KB cihaz/emülatör testi gerekir.

## Resmî kaynaklar

- [Google Play hedef API gereksinimleri](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [AGP 8.10 / API 36 / Gradle 8.11.1 uyumluluğu](https://developer.android.com/build/releases/agp-8-10-0-release-notes)
- [Android 16 KB uyumluluk ve doğrulama](https://developer.android.com/guide/practices/page-sizes)
- [Open-Meteo parametreleri ve ticari API açıklaması](https://open-meteo.com/en/docs)

## İlk tur doğrulama sonuçları (d06ef8b)

- Backend: **49 test geçti**; dört bağımlılık deprecation uyarısı var. Son kod üzerinde Ruff geçti.
- Son API 36 Android turu: **51 test geçti**, sıfır hata; debug APK üretildi. Android lint geçti: **0 hata, 204 uyarı, 1 ipucu**. Uyarıların 153'ü mevcut Android Log kullanımı; 21'i kullanılmayan kaynaklar. Bunlar kapatılarak gizlenmedi.
- Son APK: ZIP hizalaması geçti; ARM64/x86_64 altı yerel kütüphanenin tamamı 16384 bayt ELF LOAD hizalamasına sahip. Bu statik sonuç, gerçek 16 KB ortamında çalışma testinin yerini tutmaz.
- Cihaz: bağlı SM-T500 üzerine son debug APK `adb install -r` ile mevcut veriler korunarak yüklendi. `am start -W` başarılı; cihaz hedef SDK'yı 36 olarak raporladı. Süreç kontrol anında çalışıyordu; incelenen son uygulama loglarında FATAL EXCEPTION/ANR bulunmadı. GoogleApiManager, Google Play Services broker SecurityException logları üretiyor; bunun giriş/konum işlevlerine etkisi henüz doğrulanmadı. İlk UI denemesinde kilit ekranı görüldü; kullanıcı kilidi açmadığı için uygulama ekranları doğrulanmış sayılmıyor.
- Üretim dağıtımı, Play Console yüklemesi, gerçek satın alma veya gerçek hesap silme yapılmadı.

### İlk tur test APK'sı (d06ef8b)

Dosya: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`

Boyut: 83.954.171 bayt. SHA-256: `4A64521B244300BBB816F6023FEA749A6223C4348B8D1A6517750993594385A2`.

Bu çıktı debug imzalıdır. Mağaza için release AAB ve gerçek cihaz senaryoları ayrıca tamamlanmalıdır.
