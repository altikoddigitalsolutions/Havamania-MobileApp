# Android yayın kontrol listesi — 30 Ekim 2026

Bu belge sekiz ana başlığın tek takip listesidir. Bir alt testin geçmesi bütün başlığı kapatmaz. Kullanıcı tercihi: yerel doğrulama; canlı dağıtım, gerçek hesap silme veya satın alma yapılmaz. Ayrıntılı son denetim: [22 Ekim raporu](YAYIN_DENETIMI_2026-10-22.md).

## 30 Ekim son devam — Platform ask sözleşmesi

Sunucu anahtarı/HTTPS koruması, konuşma kimliği, ton, sınırlı şehir/hava/seyahat bağlamı ve ilgi alanı opt-out/temizleme davranışı eklendi. Sohbet kaydı aktif UUID’yi koruyor; cevap metadata/uyarısı saklanıyor. Son yerel kanıt: **112 backend testi**, 6 atlanan PostgreSQL testi; **203 Android JVM testi**, debug derleme ve Ruff başarılı. Bu tur cihaz/PG yeniden koşulmadı. Ayrıntılı güncel sözleşme: ASISTAN_PLATFORM_KOPRUSU.md.

Sıradaki yerel işler: platform rıza metni adaptörü ve hesap silme işine platform verisi silme/yeniden deneme. Gerçek anahtar, canlı ortam, hukuki onay, profil Firebase–SQL senkronizasyonu ve mağaza/release bağımlılıkları açık. Yayın öncesi backend yapılandırması/0014–0015 ve onaylanmış metinler birlikte doğrulanmalı. Bu alt düzeltmeler geniş maddeleri otomatik kapatmaz.

## 30 Ekim son devam — Android asistan izinleri

Asistan ve Ayarlar’a sunucu rıza metni, ayrı isteğe bağlı sağlık kutusu, geri çekme ve 428/409 yeniden onay akışı bağlandı. Hesap değişiminde bellek/bekleyen soru temizleniyor; onay sunucudan doğrulanıyor. Android **196 JVM testi**, debug/test paketi derlemesi ve tablette **2 izole ekran testi** geçti (koyu %200 yazı, açık tema/sağlık olmadan kabul). Backend rıza alt paketi **11 test** ve Ruff geçti. Önceki 95 backend / 6 PostgreSQL sonucu önceki turun kanıtıdır; bu tur genel backend/PG yeniden koşulmadı.

Geniş maddeler kapanmadı. Sonraki yerel iş: platform köprüsü sunucu anahtarı ve güncel istek/yanıt sözleşmesi; konuşma kimliği, ton ve şehir/hava/seyahat bağlamı. Onaylanmış hukuki metin, gerçek Firebase/backend/platform testi ve mağaza/release bağımlılıkları hâlâ açık. Canlı dağıtım veya gerçek hesapta onay değişikliği yapılmadı. Ayrıntılar ASISTAN_RIZA_SOZLESMESI.md ve YAYIN_DURUM_RAPORU.md içinde.

## 27 Ekim güncel durum

Ana listedeki işaretler sayıldı: **59 geniş madde, 22 tamamlanan, 37 açık**. Aşağıdaki yerel düzeltmeler geniş canlı/cihaz matrislerini otomatik kapatmaz. Eski tarihlerdeki test sayıları tarihsel kayıttır.

| Alan | Güncel kanıt ve açık sınır |
|---|---|
| Seyahat senkronizasyonu | UID/sürüm bazlı kalıcı gönderme/silme takibi, yeniden deneme ve görünür durumlar eklendi. Kapalı uygulamada iş yürütme, çok cihaz çatışması ve canlı entegrasyon açık. |
| Yerel yükleme | Tek Room akışı, iptal/UID koruması, IO dönüşümü ve hata/yeniden deneme eklendi. Gerçek bozuk veritabanı/oturum entegrasyonu açık. |
| Seyahat formu | Koordinat doğrulaması, dar/büyük yazı tarih yerleşimi, kaydedilebilir taslak ve üst düzenleme bağlamı eklendi. Gerçek Activity recreate ve ayrı debug sürecinin ölümü test edildi; üretim navigasyonu/Auth/Room yeniden açılış matrisi açık. |
| Arama ve UI | Boş/yükleme/hata ayrımı, eski sonuç temizliği ve yeniden deneme eklendi; seçili bileşenlerde klavye/%200 yazı testleri var. Bütün ekranlar, temalar, yatay ve TalkBack matrisi açık. |
| Hesap silme / güvenlik / Firebase | Yerel kanıtlar önceki kayıtlarda; gerçek silinebilir hesap, canlı kurallar/backend bağlantısı ve güvenlik dağıtımı açık. |
| Backend migration | Önceki yerel PostgreSQL doğrulaması geçerli kayıt; üretim sürümü/yedek/staging provası açık. Bu devamda yeniden koşulmadı. |
| Hava / rota / kişiselleştirme | Önceki alt düzeltmeler korunuyor. 10 gün/48 saat, saat dilimleri, risk/eksik veri ve karşılaştırmalı kişiselleştirme matrisi açık. |
| Üretim / mağaza / release | Avatar/Blaze, yasal metinler, Play Console, servis kapasitesi, imzalı R8 AAB, gerçek 16 KB ve Play iç test açık. |

Son tamamlanmış Android JVM koşusu: **183 test, 0 hata**; debug derleme başarılı. Cihaz sonuçları tarih/senaryo bazında [yayın durum raporunda](YAYIN_DURUM_RAPORU.md); farklı turları toplayarak tam uygulama testi sonucu çıkarılmaz. Önceki backend/kurallar/PostgreSQL/lint sayıları bu devamda yeniden doğrulanmış sayılmaz. 27 Ekim Git gönderim kapsamı: `4c919c0` sonrasındaki form/taslak, süreç yeniden açılışı, yerel yükleme ve 48 saat düzeltmeleri ile testleri. Tarihsel tur sonlarındaki push notları o turun durumudur.

Mevcut yerel sıra: yükleme hata panelinin cihaz doğrulaması → hava verisinin 10 gün/48 saat sınırları ve eksik veri davranışı → kalan dayanıklılık/UI matrisleri. Canlı ortam ve mağaza bağımlılıkları açık tutulur. Ayrıntılı tarihsel kayıt aşağıdadır.

## 1. Hesap silme ve veri temizliği — AÇIK

- [x] Yeniden denenebilir sunucu devam kaydı ve cihaz kurtarma akışı.
- [x] Hesap/Firestore alt koleksiyonları/Storage/backend/cihaz temizliğine yönelik yerel testler.
- [ ] Staging üzerinde avatar, seyahat, AI geçmişi ve backend kayıtları olan test hesabıyla uçtan uca silme.
- [ ] Her silme aşamasında bağlantı/süreç kesintisi, yeniden giriş ve yeniden deneme matrisi.
- [ ] Firebase kurallarının ve backend silme özelliğinin kontrollü üretim dağıtımı.

## 2. Backend migration — YEREL POSTGRESQL DOĞRULANDI

- [x] İstek sırasında şema değiştirme kaldırıldı; Alembic 0011–0013 kullanılıyor.
- [x] PostgreSQL 16.12 üzerinde sıfırdan kurulum ve tekrar `upgrade head`.
- [x] Eski UNIQUE kısıtı ve bağımsız UNIQUE indeksi olan iki yükseltme yolu; verilerin korunması ve aynı kullanıcıya birden çok cihaz.
- [x] Çakışan token'da veri silmeden durma, düzeltildikten sonra yeniden yükseltme; FK cascade davranışı.
- [x] PostgreSQL'in kısıtla birlikte kaldırdığı indeksi tekrar silme hatası düzeltildi.
- [x] CI'a ayrı PostgreSQL migration işi eklendi (uzak CI henüz çalıştırılmadı).
- [ ] Üretim PostgreSQL sürümü/uzantılarıyla eşleşme, yedek, staging veri kopyası ve operasyonel dağıtım provası.

## 3. Canlı Firebase–backend ve kurallar — AÇIK

- [x] Yerel Firestore/Storage emülatöründe sahiplik ve silme sonrası erişim engeli: 4 test.
- [x] Gerçek Firebase SDK dönüştürücüsüyle AI geçmişi ve eski belge okuma: 2 cihaz testi, ağ çağrısız.
- [ ] Gerçek test hesabıyla giriş, asistan, fotoğraf yükleme/silme.
- [ ] Proje, bucket, servis hesabı, token doğrulama ve API alan adı eşleşmesi.
- [ ] Canlı kurallarda kullanıcılar arası erişim ve iptal edilmiş token kontrolü.

### Firebase ve senkronizasyon için ek zorunlu kontroller

- [ ] Firebase yaşam döngüsü: Auth oturumu/token yenileme, Firestore/Storage dinleyici hata ve yeniden bağlanma, tek aktif dinleyici, çıkış/hesap değişiminde iptal; eski hesabın verisinin yeni ekrana taşınmaması.
- [ ] Firebase–cihaz eşitlemesi: profil/avatar/tercihler, şehirler/varsayılan şehir, seyahatler ve AI geçmişi için kaynak sahipliği; Room/DataStore/Firestore tutarlılığı, ilk açılış uzlaştırması, çevrimdışı yazma ve yeniden bağlanma, süreç ölümü sonrası kalıcı gönderme/silme kuyruğu, idempotent yeniden deneme ve silinen verinin geri gelmemesi.
- [ ] Uçtan uca genel senkronizasyon: iki cihazda eşzamanlı düzenleme/silme çatışmaları, zaman/sürüm sıralaması, Firebase–backend eşleşmesi ve kısmi başarısızlıkların kurtarılması; yükleniyor/bekleyen/senkronize/hata/gerçek boş durumlarının doğru gösterilmesi. Önce yerel/emülatör testleri; staging ve çok cihazlı canlı doğrulama ayrıca açık kalır.

- [ ] Profil fotoğrafı / Firebase Blaze hazırlığı: mevcut özellik bayrağı ve Storage akışı, proje/bucket yapılandırması, güncel plan gereksinimleri ve maliyet kontrolü; fotoğraf seçme/izin reddi, boyut ve içerik doğrulama, sıkıştırma, yükleme ilerlemesi/iptal/yeniden deneme, eski avatarın silinmesi, önbellek yenileme ve hesaplar arası erişim. Ücretli plana geçiş ayrı operasyon adımıdır; bu madde tamamlandı sayılmaz.
- [ ] Kapsamlı güvenlik denetimi: depoya/APK'ya/loglara sızan sırlar ve kişisel veri, bağımlılık açıkları, Android exported bileşenler/deep link/izinler/yedekleme/ağ güvenliği, backend kimlik ve nesne sahipliği doğrulaması, girdi/dosya kontrolleri, kötüye kullanım sınırları ve Firestore/Storage kuralları. Bulgular önem derecesi ve tekrar testiyle izlenir.

- [ ] Gerçek kişiselleştirme: ilgi alanı, tercih ve kullanıcı açıklamasının öneri seçimi/sıralaması ile asistan bağlamına etkisi; tercih değişikliği, kapatma, veri yokluğu ve A→B geçişi. Aynı hava/rota/soru için farklı tercihlerle karşılaştırmalı davranış testleri; güvenlik uyarıları tercihle bastırılmaz. Kabul ölçütleri: [mimari ve kalite matrisi](MIMARI_VE_URUN_KALITESI.md).

## 4. Hava verisinin doğruluğu — KISMEN TAMAM

- [x] Koordinatlı önbellek, 100 kayıt sınırı, bozuk disk ve çevrimdışı geri dönüş testleri.
- [x] UTC rota sorguları ve tahmin konumuna göre saat seçiminin temel testleri.
- [x] Görünümdeki 7 nokta sınırının geç tehlikeleri gizlemesi düzeltildi; eksik verili noktalar korunuyor.
- [x] Geometrik uzunluk/sağlayıcı mesafe farkında tüm rota boyunca örnekleme ve orantılı ETA testi.
- [ ] Tüm tarih/astronomi görünümleri, yaz saati geçişleri, cihaz/konum saat dilimi farkları.
- [ ] Örnekler arasındaki hava riskleri, rüzgar/veri kalitesi ve uzun rota senaryoları; örnekleme tam güvenlik garantisi değildir.

- [ ] 10 gün / 48 saat kuralları: ürün kuralının tüm ekran/API/asistan/rota yollarında tek tanımı; sınır anları, konum saat dilimi, sağlayıcıdan eksik gün/saat, eski önbellek ve ufuk dışı taleplerde açık durum. Veri yokken tahmin üretmiş gibi gösterilmez.

## 5. Gerçek cihaz dayanıklılığı — KISMEN TAMAM

- [x] Hesaba bağlı iş iptali, geç sonuç korumaları ve konum ölçümü zaman aşımı.
- [x] Eski hesap için arka plan seyahat bildirimi yayımlamadan önce sahiplik/silme kontrolü.
- [x] SM-T500/Android 12: 2 veritabanı + 2 Firebase model + 3 ortak arayüz testi.
- [ ] İnternet kes/kurtar, izin reddi/geri çekme, GPS kapalı/açık, arka plan ve süreç ölümü.
- [ ] Gerçek A→B geçişi, bekleyen bildirim/deep link ve uzun süreli bellek/pil testi.
- [ ] Konumdan adres çözümlemenin toplam zaman aşımı (eski Geocoder API).

- [ ] Kod erişilebilirliği ve tekrar denetimi: Android, backend, React Native, test/CI; ölü kod, çift dinleyici/iş, sahte başarı ve eksik entegrasyonların doğrulanarak temizlenmesi.

- [ ] Veri, kaynak ve proje temizliği: kullanılmayan dosya/asset/bağımlılıklar, geçici fotoğraf ve önbellekler, eski işler/dinleyiciler, test verileri ve hassas logların saklama/temizleme davranışı. Kod tekrar denetimiyle birlikte ele alınır; kullanıcı verisi veya platform girişleri kullanım kanıtı olmadan silinmez.

- [ ] Mimari ve kod organizasyonu: UI–ViewModel–repository–veri kaynağı sorumlulukları, bağımlılık yönleri, durum sahipliği, DI/yaşam döngüsü, coroutine iptali ve hata sözleşmeleri; Kotlin/Compose, RN ve FastAPI girişlerinin/iş kurallarının haritalanması, paralel RN/Native ayrışmalarının test edilmesi.
- [ ] Teknik borç planı: doğrulanmış borçları etki/risk/öncelik, dosya ve kapanış testiyle kaydet; lint/bağımlılık/CI açıkları ve büyük sınıfları davranış koruyan küçük değişikliklerle azalt. Sırf mimari görünüm için toplu yeniden yazım yapılmaz.

## 6. Tasarım bütünlüğü ve erişilebilirlik — KISMEN TAMAM

- [ ] Tüm ekranların canlı görsel incelemesi: ekran görüntüsü/tema/ölçek kaydı, tipografi, büyük-küçük harf, yazım, yerleşim, buton ve tasarım bütünlüğü; premium görsel dil için ortak font ailesi/ağırlık/ölçek, boşluk/ikon/renk/köşe sistemi ve tutarlı Türkçe terminoloji; İ/i/ı/I dönüşümleri ve kullanıcı metninin korunması; bulgu ve tekrar doğrulama matrisi. Her ekranın boş/dolu, yükleniyor/hata, çevrimdışı, izin ve hesap değişimi durumları ayrı denetlenir; şehir/seyahat eklemeden önce ve sonra dahil. Ayrıntılar: [durum matrisi](EKRAN_DURUMLARI_VE_KOD_DENETIMI.md).

- [x] Profil kaydı onay/hata durumu, çift tıklama engeli, taslağın snapshot ile ezilmesini önleme.
- [x] Profil klavye boşluğu, büyüyen bio alanı ve hesap bazlı kaydedilebilir taslak.
- [x] Ortak düğmede yükleme adı/durum semantiği; dolu alan etiketi; %200 yazıda büyüyen düğme cihaz testleri.
- [x] Premium satış/ücret metni gerçek davranışla tutarlı.
- [ ] Bütün ekranlar: açık/koyu/mevsimsel tema, telefon/tablet, yatay, %200 yazı, TalkBack ve kontrast.
- [ ] Profil taslağı için gerçek rotasyon/süreç ölümü ve kayıt sırasında geri navigasyon uçtan uca testi.

- [ ] Buton, sahne ve ekran geçişlerinde akıcılık: hızlı/çift tıklama, ileri/geri ve sekme dönüşü, modal/klavye açılışı, animasyon tutarlılığı, boş ekran/parlama/yerleşim sıçraması, gereksiz yeniden oluşturma, ana iş parçacığı yükü ve kare süreleri. Telefon/tablet ve düşük kaynaklı cihazda ölçüm; animasyonlar kapalıyken ve erişilebilirlik açıkken kullanılabilirlik; önce/sonra kayıt ve ölçümle doğrulama.

## 7. Üretim servisleri ve mağaza — AÇIK

- [ ] Hava/rota/harita üretim sağlayıcısı, ticari lisans/kota ve hata durumları.
- [ ] KVKK / Gizlilik Politikası / Kullanım Koşulları ve yasal sayfalar: gerçek veri envanteri, amaç/saklama/silme, üçüncü taraflar ve aktarım, kullanıcı başvuru/iletişim yolu; gerekli aydınlatma ve rıza akışlarının değerlendirilmesi, çalışan hesap silme web yolu, uygulama içi ve mağaza bağlantıları, sürüm/tarih ve Data Safety tutarlılığı. Güncel mevzuat ve işletme bilgileriyle doğrulama; mevcut metinler incelenmeden uygun sayılmaz.
- [ ] Play Console hesap kuruluşu ve yükleme hazırlığı: hesap türü/kuruluş bilgileri, güncel kimlik doğrulama ve test gereksinimleri, yetkiler, uygulama kaydı, paket kimliği/imzalama, mağaza taslağı ve imzalı AAB iç test yükleme akışı. Hesap açma, ödeme, yükleme ve yayın birbirinden ayrı izlenir; 8. başlığın paket/Play testleriyle birlikte kapanır.
- [ ] Data Safety, içerik derecelendirmesi, test erişimi ve mağaza görselleri.
- [ ] Çoklu sunucu rate limit, PostgreSQL kapasitesi, gözlemleme ve yedekten dönüş.
- [ ] Eski React Native CI: bulunmayan SignUpScreen import’u RegisterScreen olarak düzeltildi; bağımlılık kurulumu ve Jest/TypeScript doğrulaması açık (native Android’den ayrı).

## 8. İmzalı release AAB — AÇIK

- [x] compile/target SDK 36, debug APK'nın altı yerel kütüphanesinde 16 KB ELF ve zipalign kontrolü.
- [ ] Gerçek yükleme anahtarı/keystore, uygulama kimliği ve versionCode doğrulaması.
- [ ] R8 küçültülmüş imzalı AAB üretimi ve release cihaz testi.
- [ ] Gerçek 16 KB Android cihaz/emülatör testi.
- [ ] Play iç test ve pre-launch raporu; gerekli hesap türü test şartları.

## Önceki doğrulama — 22 Ekim

- Android: **102 birim testi geçti; önceki cihaz koşusunda 7 test geçti**; debug derleme ve lint başarılı (**0 hata, 199 uyarı, 1 ipucu**).
- Backend: **64 test geçti**, Ruff temiz. Standart koşuda PostgreSQL testleri atlanır; aşağıdaki ayrı koşuda gerçekten çalıştırıldı.
- PostgreSQL 16.12: **4 test geçti**; yalnızca oluşturduğumuz izole localhost kümesi kullanıldı ve kapatıldı.
- Firebase kuralları: **4 yerel emülatör testi geçti**.
- Toplam **181 farklı test için başarılı sonuç kaydı**; bu sayı bütün uygulamanın veya canlı ortamın doğrulandığı anlamına gelmez.
- APK SHA-256: `229EBD2786862B44771477FC4F72A446938393B10153115D6DDB4522DCF8F147`.
- PostgreSQL tekrar koşusu: `.venv/Scripts/python.exe tools/run_postgres_audit.py --pg-bin "C:/Program Files/PostgreSQL/16/bin"`.

## Geçmiş sonuçlar

## Önceki tur doğrulama (07b268f)

- Backend: **64 test geçti**, Ruff temiz. Gerçek HTTP taşıyıcıları testlerde engellendi; abonelik testi dış hava servisine bağımlı olmaktan çıkarıldı.
- Android: **58 birim testi geçti**; `assembleDebug` ve `lintDebug` başarılı. Lint: **0 hata, 200 uyarı, 1 ipucu**.
- SM-T500 / Android 12: **2 test geçti**. Bildirim 4→5 ve hava veritabanı 11→17 geçişlerinde eski kayıtlar korundu; farklı kullanıcı kayıtları temizlenmedi. Hava önbelleğinin 100 kayıt sınırı da gerçek SQLite üzerinde doğrulandı.
- Firestore/Storage emülatörleri: **4 test geçti**. Yalnızca yerel demo proje kullanıldı.
- Toplam: **128 başarılı test**. Bu sayı tüm uygulama akışlarının veya canlı ortamın doğrulandığı anlamına gelmez.

## Cihaz dayanıklılığı turu

- Android: **66 birim testi geçti** (önceki 58 + yeni 8). `assembleDebug` ve `lintDebug` başarılı.
- Son APK ile SM-T500 / Android 12 üzerinde **2 izole cihaz testi geçti**.
- Backend ve Firebase kural testleri bu tur yeniden çalıştırılmadı; bu bileşenlerde değişiklik yok.
- Detaylar ve açık cihaz matrisi: [cihaz dayanıklılığı raporu](CIHAZ_DAYANIKLILIGI.md).

## Önceki debug APK (5d1e52a)

Yol: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`

SHA-256: `5268567EB63FDDFAB99410543E9A071C6E2382C72418AB531EFB6D26ABAD61CB`

ARM64/x86_64 altı kütüphanenin ELF LOAD hizalaması 16384 bayt. Gerçek 16 KB cihaz testi henüz yapılmadı. Bu paket Google Play yükleme paketi değildir.

Hesap silme mimarisi, kalan veri saklama kapsamı ve dağıtım sırası: [teknik rapor](HESAP_SILME_VE_SUNUCU_GECISLERI.md).

## Saat dilimi devam turu

Takip: **45 madde; 22 tamamlandı, 23 açık**. Dördüncü başlık üzerinde çalışılıyor; ilk üç başlıktaki canlı ortam gerektiren maddeler açık kalıyor. Tahmin ekranı ve ViewModel konum saat dilimine geçirildi; gece yarısı, eski önbellek, yanlış bugün işareti ve yaz saati başlangıcı için 7 yeni test geçti. 85 birim testi, debug derleme ve lint bu tur yeniden çalıştırıldı; diğer test sayıları önceki başarılı koşulara aittir. Astronomi ve yaz saati bitişi kapsamı hâlâ açık.
## Güneş saatleri devam turu

Seçilen güne ait doğuş/batış verileri ana kart, saatlik tahmin ve güneş zamanları paneline bağlandı. Eksik/yanlış tarihli veriler başka günden alınmıyor; eski önbellek uyumlu. Gün batımı sınırı düzeltildi. Yeni 5 testle **90 Android birim testi geçti**; debug APK üretildi ve altı yerel kütüphanenin 16 KB ELF/zipalign kontrolleri yenilendi. Cihaz ve backend testleri bu tur tekrarlanmadı. Güncel APK SHA-256: `2CA9F522446B4CD1ABF6D622C04E60D124EE70D9973F592B62D93AB1C7BB4CDD`.

Geniş astronomi maddesi açık: AI günlük yanıtındaki zaman seçimi, kutup günü/gecesi, yaz saati bitişi ve cihaz doğrulaması gerekiyor. Takip sayısı **22 tamamlanan / 23 açık** olarak devam ediyor.
Bu turun lintDebug kontrolü de başarıyla tamamlandı.
## AI günlük tahmin devam turu

AI tarihli hava yanıtı konumun tarihini kullanıyor; günlük özet cihazın gece/gündüz saatinden bağımsız. Yarından sonra ve açık yıl ayrıştırması düzeltildi. Eksik gün için başka günün tahmini kullanılmıyor; eski önbellek belirtiliyor. **97 Android birim testi (7 yeni) geçti**, debug APK üretildi. Önceki düzeltmeler aynı birim test koşusuna dahil; canlı ortam ve cihaz testleri bu tur yeniden koşulmadı.

Açık alt kontroller: kutup günü/gecesi, yaz saati bitişi, geçersiz tarih sorusunu netleştirme, alternatif AI yanıt yolu ve yerel AI işlerinin hesap değişimi/hata yaşam döngüsü. Ayrıntılar 22 Ekim denetim raporunda. 45 geniş takip maddesinde 22 tamamlanan / 23 açık durumu sürüyor.
AI tarih turunda testDebugUnitTest, assembleDebug ve lintDebug birlikte başarıyla tamamlandı.

## Asistan istek yaşam döngüsü devam turu

Hesap değişimi/sohbet sıfırlama/geçmiş sohbet açma sırasında bekleyen yanıt geçersiz kılınıyor. Yerel ve uzak yanıtlar aynı çift gönderim, hata ve zaman aşımı yönetimini kullanıyor. İptali dikkate almayan geç sonuçlar ve eski isteğin yeni yükleme durumunu temizlemesi için koruma eklendi. **102 birim testi (5 yeni) geçti**; debug APK üretildi. Gerçek cihaz hesap değişimi matrisi açık; önceki cihaz/backend sonuçları bu tur tekrarlanmadı.

Sıradaki bağlantılı kontrol: otomatik hava yükleme ve seyahat gözlemleme işlerinde iptal/hesap sahipliği; ardından 4. başlığın kutup günü/gecesi ve yaz saati bitişi kapsamı. Geniş takip listesi 22 tamamlanan / 23 açık madde.
Asistan yaşam döngüsü turunda testDebugUnitTest, assembleDebug ve lintDebug birlikte başarıyla tamamlandı.
Güncel lint: 0 hata, 196 uyarı, 1 ipucu; üstteki önceki 199 uyarılık sonuç bu turda 196 oldu.
## Arka plan işleri ve UI devam turu

Asistanın otomatik veri işleri hesaba bağlı iptal ve yayın kontrollerine alındı. Giriş/asistan/ayarlar/geçmiş/güneş panelinde erişilebilirlik ve esnek yerleşim düzeltmeleri yapıldı. **102 birim testi ve son koşuda 10 cihaz testi geçti.** İlk cihaz koşusundaki 3 geçici Activity hiyerarşisi hatası ayrıntılı raporda kayıtlı. UI tarama bulguları ve sonraki düzeltmeler [ekran bazlı alt listede](UI_DENETIMI_2026-10-22.md); bütün ekranlar görsel olarak onaylanmış değil. Ana listedeki geniş 5/6. başlıklar açık kalıyor.
## Kişiselleştirme devam turu

Kişiselleştirme ekranının kayıt sonucunu beklemeden kapanması ve snapshot ile taslağı ezmesi düzeltildi. Hesap bazlı saklanabilir taslak, onay/hata/yeniden deneme, zaman aşımı ve çift gönderim koruması eklendi; uzun hassasiyet metni ile anahtar yerleşimi iyileştirildi. **106 birim testi geçti**, debug APK üretildi. Önceki 10 cihaz testi bu tur tekrar çalıştırılmadı; gerçek rotasyon/süreç ölümü matrisi açık.

Sonraki UI işi: profil AboutMeSheet klavye/büyük yazı ve seyahat ekleme düğmesi, ardından geçmiş detayındaki işlevsiz eylemler. Ayrıntılı durum UI_DENETIMI_2026-10-22.md dosyasında. Geniş 6. başlık henüz kapanmadı.
Kişiselleştirme turunda lintDebug de başarıyla tamamlandı.
## Hakkımda / seyahat ekleme devam turu

Hakkımda penceresinde klavye boşluğu, esnek alan/düğme, kaydedilebilir taslak, kayıt onayı/hata/yeniden deneme ve eski hesap sonucunu engelleme tamamlandı. Seyahat ekleme eylemi adlandırıldı, 48 dp oldu. **106 birim testi ve son tablet koşusunda 12 cihaz testi geçti.** Yeni iki cihaz testi izole Hakkımda içeriğinde saved-state taslak geri yükleme ve %200 yazı/küçük alanda kaydet eylemine erişimi doğrular. İlk koşu test temasının ekran boyutu sağlamaması nedeniyle çöktü; test kurulumu düzeltildikten sonra 12/12 geçti. Gerçek Firebase ve tam uygulama süreç ölümü testi değildir.

Sıradaki UI düzeltmesi: AI geçmiş detayındaki işlevsiz eylemler ve yükleniyor/kayıt bulunamadı ayrımı.
Hakkımda turunda lintDebug de başarıyla tamamlandı.

## Geçmiş detayı devam turu
Yükleme/yokluk/hata ayrımı, yeniden deneme, eski hesap/istek sonucu koruması ve işlevsiz geçmiş eylemlerinin gizlenmesi tamamlandı. 111 JVM testi, debug derleme ve lint geçti (0 hata, 194 uyarı). Bu tur cihaz testleri çalıştırılmadı. Ekran görüntülerinin önceki izole test saatleriyle eşleşmesi ayrıntılı rapora kaydedildi. Tam cihaz matrisi ve canlı doğrulamalar açık; 22/45 tamamlandı, 23 geniş madde açık. Sonraki UI incelemesi şehir araması ve bildirim eylemleri.

## Şehir araması / bildirim erişilebilirliği devam turu

Şehir ekranı artık kısa ve boş sorguyu da ViewModel'e iletiyor. Her yeni sorguda eski öneriler temizleniyor, sorgu trim ediliyor ve iptal edilmiş işin sonuç yayınlaması ensureActive ile engelleniyor. Ülke metni sabit Türkiye yerine veriden geliyor. Arama etiketi, varsayılan/sil eylem adları ve uzun öneri metni için ağırlık eklendi. Bildirim kartına kaydırma gerektirmeyen erişilebilir sil/okundu/seç eylemleri ve okunma/seçim durumu eklendi. Tam ekran klavye/büyük yazı ve gerçek TalkBack doğrulaması açık; tablet testleri bu tur çalıştırılmadı.

Güncel kullanıcı özeti: [Yayın durum raporu](YAYIN_DURUM_RAPORU.md). Ana 45 madde: 22 tamamlanan, 23 açık. Alt düzeltmeler tüm cihaz matrisinin tamamlandığı anlamına gelmez.

## AI geçmişi silme devam turu
Tek/toplu silmede onay bekleme, görünür hata/yeniden deneme, 30 saniye sınırı ve hesap değişimi koruması eklendi. Üç yeni birim testi eklendi. Genel senkronizasyon ve canlı doğrulama açık; ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında. 22/45 tamamlandı, 23 geniş madde açık.

## Geçmiş senkronizasyonu devam turu
Aktif dinleyicide uzaktan silme, yeni yerel sürümün korunması ve aynı ViewModel kayıt/silme sıralaması eklendi. Kalıcı ilk-açılış uzlaştırması ve cihazlar arası çatışmalar açık. Ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında; 22/45 tamamlandı, 23 geniş madde açık.

## Sohbet kayıt onayı devam turu
Kaydet ve bitir onay bekliyor; hata sohbeti koruyor, aynı ViewModel'deki yeniden deneme aynı kayıt kimliğini kullanıyor. Dört birim testi eklendi. İlk açılış uzlaştırması için kalıcı senkronizasyon durumu/migration işi açık. Ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında. Ana durum 22/45 tamamlanan, 23 açık geniş madde.

## Kalıcı geçmiş doğrulaması turu
Room 17→18 ve syncedUpdatedAt ile tam sunucu snapshot'ına dayalı güvenli uzlaştırma eklendi. Bilinmeyen/pending/eski kayıtlar korunuyor. Kalıcı yeniden yükleme kuyruğu, süreç ölümü sonrası kayıt kimliği ve çok cihazlı çatışmalar açık. Ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında. Ana liste 22/45 tamamlanan, 23 açık geniş madde.

## Bekleyen kayıt kuyruğu turu
Room 18→19 ve pendingUpload; yeniden açılışta aynı kimlikle gönderme, kullanıcı/sürüm bazlı onay ve görünür manuel yeniden gönderme eklendi. Kapalı uygulamada arka plan gönderimi, kalıcı silme tombstone'u ve çok cihazlı çatışmalar açık. Ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında. 22/45 tamamlanan, 23 açık geniş madde.

## Geniş kalite denetimi
Kullanıcının 20 kategorili raporu KAPSAMLI_KALITE_DENETIMI_2026-10-22.md dosyasında takip ediliyor. Bildirim kimliği/boş liste/silme sonucu, ortak metin biçimi, şehir kaydırması ve lifecycle saat iyileştirmesi uygulandı. Kalıcı silme devam kaydı ve tüm cihaz/release/canlı matrisleri açık. 22/45 tamamlanan, 23 açık geniş madde korunuyor.

## Kalıcı silme devam kaydı
Room 19→20, kullanıcı/kimlik bazlı bekleyen ve tamamlanmış silme işaretleri; yeniden açılışta yüklemeden önce silme; aynı süreçte paylaşılan işlem kilidi eklendi. Canlı kesinti/çok cihaz ve arka plan yürütmesi açık. Ayrıntılar YAYIN_DURUM_RAPORU.md dosyasında. 22/45 tamamlanan, 23 açık geniş madde.

27 Ekim yeni Git gönderim kapsamı: `7599ac9` sonrasındaki eksik hava verisi, risk/sıcaklık özeti, saat dilimi/zaman etiketi, konum değişimi temizliği ve sürüm koşullu analiz yazıları ile testleri. Son JVM koşusu 183 test başarılı; tarihsel “Push yapılmadı” notları ilgili turun durumudur. Saat dilimi bilinmeyen ilk açılış ve takvim filtreleri açık kalır.

## 30 Ekim 2026 — Asistan entegrasyonu devamı

Asistan köprüsünde hata cevabının başarılı sayılması ve hak tüketimi düzeltildi; ConnectError dışındaki otomatik tekrarlar kaldırıldı. Yerel backend: 77 geçti, 4 atlandı; Ruff geçti. Bu alt düzeltme ana listedeki geniş maddeleri kapatmaz. Platform sözleşmesi, sunucu rızası/veri minimizasyonu, hesap silme bağlantısı, atomik kota/istek tekilleştirme ve Android bağlam/sohbet/ton akışı açık. Detaylar YAYIN_DURUM_RAPORU.md içindeki 30 Ekim kaydında. Canlı test yapılmadı.

30 Ekim devam: paralel kota için atomik koşullu SQL rezervasyonu ve hata iadesi eklendi; normal/premium ortak akış. Backend 79 geçti, 4 atlandı; 16 paralel yerel SQLite istek/3 hak testi geçti. Kalıcı request_id, süreç çökmesi kurtarması, tekrar gönderim tekilleştirme ve PostgreSQL eşzamanlılık doğrulaması açık; ana madde kapanmadı.

30 Ekim kalıcı istek devamı: 0014 migration ile UID/request_id unique kayıt, atomik kota+istek yazımı ve süresi geçen rezervasyonun tek sefer iadesi eklendi. Backend 82 geçti, 5 atlandı; ayrı yerel PostgreSQL denetimi 5 geçti. Mobil request_id/tekrar akışı, platform idempotency ve sonuç kurtarma, rıza/veri minimizasyonu açık. 0014 üretime uygulanmadı; sıradaki Android sabit mesaj kimliği ve 409 davranışı.

30 Ekim Android devamı: kullanıcı mesajı UUID'si request_id olarak gönderiliyor; tekrar aynı kimlikle, kullanıcı balonu çoğaltılmadan yapılır. 409 duplicate/expired ayrı açıklama ve terminal davranış; hata balonunda yeniden deneme düğmesi eklendi, salt okunur geçmiş dışlandı. Dört yeni testle ilk JVM koşusu 187/0 hata ve debug derleme başarılı. Canlı/tablet görsel kontrol, platform sonuç kurtarma ve kaydedilmemiş sohbet süreç ölümü açık. Sıradaki kota 429 ve Android hata ayrımı; yayın öncesi backend+0014 gerekir.

30 Ekim hata sözleşmesi devamı: yerel günlük kota 429 daily_limit; genel hız sınırı 429 rate_limited ve Retry-After. Android kota/hız sınırı/servis/zaman aşımı ayrımı ve bekleme sırasında kapalı tekrar düğmesi eklendi. Backend 84 geçti, 5 atlandı; Ruff temiz. Platformun kendi 428/429 aktarımı, UTC/İstanbul geçişi, canlı/tablet matrisi açık. Sıradaki sunucu rıza kaydı ve sağlık verisi minimizasyonu.

30 Ekim rıza devamı: 0015 ile sunucuda rıza+audit, GET/POST consent, sürüm/hash doğrulaması ve onaysız profil sağlık alanının çıkarılması eklendi. Metin yoksa 503, güncel onay yoksa 428; otomatik onay yok. Backend 95 geçti, 6 atlandı; ayrı PostgreSQL 6 geçti. Yeni sözleşme ASISTAN_RIZA_SOZLESMESI.md. Android rıza/onay ekranı henüz bağlanmadığı için bu backend aşaması tek başına yayımlanmamalı. Sıradaki Android ayrı onay/geri çekme ve 428/409 akışı; canlı/hukuki/platform bağımlılıkları açık.
