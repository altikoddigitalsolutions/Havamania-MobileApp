# Havamania — Yayına hazırlık durum raporu

Güncelleme: 27 Ekim 2026. Hedef: önce Android / Google Play.

Canlı ekran bulguları, düzeltmeler ve açık varyantlar: [Canlı görsel denetim](CANLI_GORSEL_DENETIM.md).

## Son yerel doğrulama özeti — 27 Ekim

- Android JVM: **183 test geçti**, debug APK derlendi. Sonraki cihaz koşuları bu dosyanın sonunda senaryolarıyla kayıtlıdır.
- Gerçek Activity recreate: 2 izole test; ayrı debug sürecinin ölümü/yeniden açılışı: 1 betik senaryosu geçti. Üretim navigasyonu/Auth/Room uçtan uca süreç ölümü açık.
- Önceki backend, PostgreSQL, Firebase kuralları ve lint sonuçları bu devamda yeniden koşulmadı.
- Ana liste: **59 geniş madde, 22 tamamlanan, 37 açık**. Bu sayılar doğrulanmış hata adedi değildir. Yayın onayı, imzalı release ve canlı matrisler açık.

## Önceki geniş denetim — 22 Ekim

[Kapsamlı kalite raporu](KAPSAMLI_KALITE_DENETIMI_2026-10-22.md): 20 kategori, 5 kodla doğrulanmış bug, 1 UI riski ve 1 iyileştirme. O tarihteki koşu: 125 JVM + 16 cihaz + 64 backend + 4 PostgreSQL + 4 Firebase kural testi başarılı; 187 lint uyarısı açık. Ayrıntılı sonuçlar bu dosyanın sonunda.

## Kısa durum

Ana kontrol listesinde **59 geniş madde: 22 tamamlandı, 37 açık**. Canlı görsel ekran denetimi durum varyantlarıyla genişletildi; tüm kod tabanının erişilebilirlik/tekrar denetimi ayrı açık madde eklendi. Bu sayı hata adedi değildir: tek bir açık madde çok sayıda cihaz/servis senaryosu içerir. Aşağıdaki alt düzeltmeler geniş matrisi tamamen kapatmadığı için sayı otomatik artırılmaz. Uygulama henüz yayına hazır olarak onaylanmadı.

Çalışma kapsamı: yerel testler. Canlı hesap silme, üretim dağıtımı ve mağazaya gönderim yapılmadı. 27 Ekim Git gönderim kapsamına `4c919c0` sonrasındaki form/taslak, süreç ve yerel yükleme, koordinat ve 48 saat düzeltmeleri ile testleri alındı. Tarihsel turlardaki “Push yapılmadı” notları ilgili turun durumunu anlatır.

## Çözülen problemler

| Alan | Problem ve yapılan düzeltme | Kanıt / sınır |
|---|---|---|
| Hesap silme | Yarım kalan silme için devam kaydı ve cihaz kurtarma akışı; hesap, avatar, seyahat, AI ve backend temizliği | Yerel testler var; gerçek hesapla uçtan uca işlem açık |
| Backend şeması | İstek sırasında şema değişikliği kaldırıldı; ayrı Alembic migration. PostgreSQL'in otomatik sildiği indeksin ikinci kez silinmesi düzeltildi | PostgreSQL 16.12 üzerinde 4 test; üretim provası açık |
| Kullanıcı verileri | Hesaba bağlı iş iptali, eski sonuç korumaları, bildirimden önce sahiplik/silme kontrolü | Yerel testler; gerçek A→B geçiş matrisi açık |
| Firebase modelleri | AI geçmişi ve eski belgelerin SDK ile okunması düzeltildi | 2 ağ çağrısız cihaz testi; canlı kuralları doğrulamaz |
| Hava önbelleği | Koordinata göre ayrım, 100 kayıt sınırı, bozuk disk/çevrimdışı geri dönüş | Yerel testler |
| Saat ve tahmin | Konuma göre saat/tarih seçimi, güneş zamanı alanları, AI tarihli yanıtları | Birim testleri; tüm DST ve kutup senaryoları açık |
| Rota riskleri | 7 nokta sınırında kritik/eksik verinin kaybolması; mesafe farkında tüm rotanın örneklenmesi ve ETA | Birim testleri; örnekler arasındaki riskleri garanti etmez |
| Profil kaydı | Onay beklemeden kapanma, çift gönderim, snapshot'ın taslağı ezmesi | Onay/hata/yeniden deneme, kullanıcıya bağlı taslak; canlı yazı açık |
| Kişiselleştirme | Erken kapanma ve seçimlerin kaybolması | Kayıt onayı, saklanabilir taslak, zaman aşımı; gerçek süreç ölümü açık |
| Hakkımda | Klavye/büyük yazıda erişim ve kayıt hatasında metin kaybı | Esnek alan, kaydırma, IME boşluğu; izole cihaz testleri |
| Ortak UI | İsimsiz eylemler, dolu alan etiketi, yükleme düğmesi semantiği, güneş paneli dar yerleşimi | İzole Compose testleri; tam ekran/TalkBack matrisi açık |
| Premium | Satış/ücret metninin gerçek davranışla tutarsızlığı | Metin ve esnek yerleşim düzeltildi |
| AI geçmiş detayı | İlk boş listeyi kayıt yok sanma, hata/yükleme ayrımının olmaması, işlevsiz eylemler | Ayrı durumlar, yeniden deneme, eski sonuç koruması; 5 yeni birim testi |
| Şehir araması — bu tur | Kısa/boş sorguda eski öneriler, yeni sorgu beklerken önceki sonuçların kalması | Her sorguda temizleme, trim, iptal sonrası sonuç yayınlama kontrolü |
| Şehir kartları — bu tur | Yabancı konuma Türkiye yazılması, isimsiz varsayılan/sil eylemleri | Gerçek ülke alanı ve erişilebilir eylem adları |
| Bildirimler — bu tur | Silme/okundu eylemine yalnızca kaydırarak erişim | Ekran okuyucu için özel sil/okundu/seç eylemleri ve durum semantiği; cihazda TalkBack doğrulaması açık |

## Önceki doğrulama durumu

- Önceki tamamlanmış Android koşusu: **111 birim testi**, debug derleme ve lint başarılı; **0 lint hatası, 194 uyarı, 1 ipucu**.
- Önceki son Samsung SM-T500 / Android 12 koşusu: **12 cihaz testi geçti**. 2 SQLite migration, 2 Firebase model, 8 izole Compose testi. Bu tur tablet testi çalıştırılmadı.
- Önceki backend koşusu: **64 test**, Ruff temiz. Ayrı PostgreSQL koşusu **4 test**, Firebase kural emülatörü **4 test**. Bu tur yeniden çalıştırılmadılar.
- Önceki cihaz testlerindeki geçici Activity başlatma hataları ve test temasındaki ekran boyutu eksikliği ayrıntılı denetim günlüğünde kayıtlı; başarılı tekrarlar ilk hataları silmez.
- Kullanıcının 14:56:30–32 boş tablet ekranı görüntüleri izole auth testiyle eşleşiyor. Bu, normal uygulamanın açılışının ayrıca doğrulandığı anlamına gelmez. Yeni tablet testi öncesi haber verilecek.

## Önceki turdaki iş sırası — tarihsel kayıt

| Öncelik | İş | Tamamlanma ölçütü / bağımlılık |
|---|---|---|
| 1 — yerel | Şehir ekranında klavye, dar/yatay ekran, büyük yazı; bildirim eylemleri | Tam ekran yerleşim ve TalkBack cihaz doğrulaması. Bu tur yalnızca kod/derleme kontrolü |
| 2 — yerel | AI geçmişi silme/senkronizasyon hata yolları | Başarısız silme görünür olmalı, yeniden denenmeli; eski kayıtlar geri gelmemeli |
| 3 — yerel | Hava zamanı ve rota sınır durumları | DST geri geçişi, kutup gündüz/gecesi, geçersiz AI tarihi, rüzgar/eksik veri testleri |
| 4 — yerel/cihaz | Dayanıklılık | Ağ ve izin değişimi, hesap A→B, arka plan, süreç ölümü, bildirim/deep link; eski Geocoder toplam zaman aşımı |
| 5 — test ortamı gerekli | Hesap silme ve Firebase–backend uçtan uca | Silinebilir test hesabı, staging, avatar/seyahat/AI verileri; kesinti sonrası devam ve başka kullanıcıya erişim engeli |
| 6 — servis/işletme bilgileri gerekli | Üretim ve mağaza | Sağlayıcı lisans/kota, gizlilik/KVKK/koşullar, hesap silme web adresi, Data Safety ve mağaza görselleri; yedekleme/izleme |
| 7 — imza ve Play erişimi gerekli | Release onayı | Gerçek anahtarla R8 AAB, release cihaz testi, gerçek 16 KB ortamı, Play iç test/pre-launch |

Ek açık işler: üretim PostgreSQL sürüm/uzantı eşleşmesi ve yedekten dönüş provası; uzak CI doğrulaması; eski React Native CI import sorunu; tüm temalarda kontrast ve tam UI matrisi. Bunlar unutulmuş veya tamamlanmış kabul edilmiyor.

## Ayrıntılı kayıtlar

- [Ana 45 maddelik kontrol listesi](YAYIN_ONCELIKLERI.md)
- [Teknik bulgular ve test geçmişi](YAYIN_DENETIMI_2026-10-22.md)
- [Ekran bazlı UI envanteri](UI_DENETIMI_2026-10-22.md)
- [Cihaz dayanıklılığı](CIHAZ_DAYANIKLILIGI.md)

Bu dosya güncel özet; ayrıntılı raporlardaki eski test sayıları ait oldukları turun geçmiş kaydıdır.

## Bu turun tamamlanan doğrulaması
Şehir ve bildirim düzeltmeleri sonrasında 111 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı. Yeni cihaz/TalkBack testi çalıştırılmadı. Önceki test sonuçlarıyla bu turun kapsamı karıştırılmamalı.

## AI geçmişi silme — devam turu

Tek kayıt silmede Firestore delete görevi artık await ile bekleniyor; sunucu onayından önce yerel kayıt kaldırılmıyor. Toplu silme listeyi SERVER kaynağından okuyor. Her iki işlemde 30 saniye onay sınırı, ortak meşgul durumu, çift işlem engeli, görünür hata ve yeniden deneme var. Hesap değişimi işlem/hata/yeniden deneme durumunu temizliyor. Eski snapshot işleri epoch ve yerel yazma kilidiyle silme işlemiyle sıralanıyor. Onay diyalogları hesaba bağlı; tümünü sil düğmesi adlandırıldı.

Sınırlar: await iptali Firestore'a gönderilmiş yazıyı geri almaz; zaman aşımında sunucuda silme sonradan tamamlanabilir. Bu nedenle hata metni “tamamlandığı doğrulanamadı” der. Çok partili toplu silme kısmen tamamlanabilir, yeniden deneme kalan belgeleri tekrar okur. Başka cihazdan silinen kayıtları yerel listeden çıkarma, eşzamanlı kayıt/silme, süreç ölümü sonrası kalıcı işlem takibi ve kayıt ekleme hatasının görünürlüğü hâlâ açık. Gerçek Firebase testi yapılmadı; bu bölüm tüm senkronizasyonun kapandığı anlamına gelmez.

Yeni üç birim testi sunucu onayını bekleme, hata sonrası yerel kopyayı koruyarak yeniden deneme ve iptal sonrası yerel silmeyi engellemeyi kapsar. Bu tur tablet testi çalıştırılmadı. Ana liste 22/45 tamamlanan, 23 açık geniş madde olarak korunuyor.

Silme turu sonucu: 114 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Tablet ve canlı Firebase testleri bu tur çalıştırılmadı.

## Geçmiş senkronizasyonu — devam turu

Aktif dinleyicide sunucudan gelen REMOVED olayları artık yerel kayda uygulanıyor. Önbellek snapshot'ı veya bekleyen yazısı olan snapshot silme için kullanılmıyor. Silinen belgenin updatedAt alanı eksikse güvenli tarafta kalıp yerel veri korunuyor. Daha yeni yerel düzenlemeler korunuyor; silme SQL koşulu ile kullanıcı ve sürüm karşılaştırılarak yapılıyor. Sunucudan ekleme/güncelleme birleştirmesi de Room transaction içinde daha yeni yerel veriyi ve başka kullanıcı sahipliğini koruyor. Dinleyici işleri ana coroutine kuyruğundan yerel kilide giriyor.

Aynı AiHistoryViewModel içindeki kayıt/silme işleri ayrı mutationMutex ile sıralanıyor; kayıtta Firestore görevi 30 saniyeye kadar bekleniyor. Yerel kayıt hemen listeye yükleniyor. Silme sırasında liste okuması dinleyiciyi erken başlatmıyor; eski işlemin finally bloğu yeni hesap döneminin dinleyicisini değiştirmiyor.

Kalan sınırlar: uygulama kapalıyken gerçekleşen silmelerin ilk açılışta uzlaştırılması, kalıcı pending-write/tombstone kayıtları, farklı ViewModel/cihazlar arası eşzamanlı düzenlemeler, saatleri farklı cihazlarda sürüm çatışması ve kaydet/bitir ekranında kayıt hatasının görünürlüğü. Silme olayı pending/cache nedeniyle atlandıysa sonraki tam sunucu uzlaştırması gerekir; henüz bu iş kapatılmadı. Firestore zaman aşımı yazıyı geri almaz. Kayıt ve silmenin aynı ViewModel'de sıralanması küresel bir kilit değildir.

Dört yeni birim testi sunucuca onaylı silme, daha yeni yerel düzenleme, önbellek ve bekleyen yazı korumalarını kapsar. Bunlar gerçek Firestore/Room entegrasyonu yerine geçmez. Bu tur canlı bağlantı veya tablet testi yapılmadı. Ana liste 22/45 tamamlanan, 23 açık geniş madde olarak korunuyor.

Senkronizasyon turu son doğrulaması: 118 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı. Son güvenli belge okuma değişikliğinden sonra yeniden çalıştırıldı. Tablet/canlı Firebase testleri yapılmadı.

## Sohbeti kaydet ve bitir — devam turu

Kaydet ve bitir artık kayıt tamamlanmadan sohbeti temizlemiyor. Ayrı Idle/Saving/Saved/Failed durumu, 30 saniye onay sınırı, çift gönderim engeli ve diyalog içinde hata/yeniden deneme eklendi. Giriş yapılmış hesapta Firestore set onayı bekleniyor; misafir için yerel kayıt tamamlanması yeterli. Hata sohbeti silmez. Aynı ViewModel ömründeki yeniden denemeler aynı belge kimliğini kullanır; başarıdan sonra yeni sohbet için kimlik sıfırlanır. Hesap değişiminde eski onay reddedilir; ekran ayrılınca bekleyen onay iptal edilir. Bu iptal, sunucuda başlamış yazıyı geri almaz.

İlk açılışta kaçırılan silmeleri uzlaştırma bu tur kapatılmadı: yerel kaydın sunucuda onaylandığını belirten kalıcı işaret olmadan eksik sunucu kaydı ile henüz yüklenmemiş yerel sohbet ayırt edilemiyor. Sıradaki iş kalıcı senkronizasyon durumu ve Room migration testleri; ardından tam sunucu snapshot'ı ile güvenli uzlaştırma. Süreç ölümü sonrası aynı kayıt kimliğiyle yeniden deneme de bu kalıcı durum çalışmasına dahil. Yeni dört test onay bekleme/çift gönderim, aynı kimlikle yeniden deneme, hesap değişimi ve ekran ayrılmasını kapsar. Canlı Firebase ve tablet testi yapılmadı.

Kayıt onayı turu sonucu: 122 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı. Tablet/canlı Firebase testi yapılmadı.

## Kalıcı geçmiş doğrulaması ve ilk açılış uzlaştırması

Room sürümü 18 oldu. 17→18 geçişi ai_history tablosuna nullable syncedUpdatedAt sütunu ekliyor; eski kayıtları sunucuda onaylanmış varsaymıyor. Alan Firestore'a gönderilmiyor. Yerel kayıt/düzenleme yeni nesnede null olarak başlar. Yalnızca cache olmayan ve pending write içermeyen tam sunucu snapshot'ı ilgili sürümü doğrular.

Tam sunucu koleksiyonu Room transaction içinde birleştirilir; sunucuda bulunmayan kayıt yalnızca syncedUpdatedAt ile updatedAt aynıysa silinir. Sahiplik korunur, daha yeni yerel sürüm ezilmez. Bozuk belge ayrıştırılamasa bile ham belge kimliği var sayılır; yanlış yokluk kararı verilmez. Snapshot hatası veya çevrimdışı cache temizleme başlatmaz. Aktif oturumdaki özel REMOVED mantığının yerini bu tam-liste uzlaştırması aldı; metadata değişiklikleri dinlenmeye devam eder.

Yeni izole SQLite testi veritabanını kapatıp yeniden açarak sunucu onayının kalıcılığını, uzaktaki silmenin uygulanmasını, pending/eski sürümden taşınan kayıtların korunmasını, yeni yerel düzenlemeyi, başka kullanıcı sahipliğini ve bozuk belge kimliğini doğrular. 11→18 migration zinciri testinde de eski AI kaydının null işaretle korunduğu denetlenir.

Sınırlar: eski sürümden gelen ve sunucuda hiç yeniden doğrulanmamış kayıtlar bilinçli olarak korunur; geçmişte silinmiş bütün kayıtların otomatik temizlendiği iddia edilmez. İşaret, tek başına kalıcı yeniden yükleme kuyruğu veya silme tombstone'u değildir. Süreç ölümü sonrası kaydetme isteğinin aynı kimlikle sürdürülmesi, başarısız kayıtları yeniden yükleme kuyruğu ve farklı cihaz saatlerinde çatışma çözümü açık. Canlı Firebase erişimi yapılmadı.

Kalıcı uzlaştırma turu doğrulaması: 122 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 5 cihaz testi (3 veritabanı, 2 Firebase model; 0 başarısızlık/0 hata), assembleDebug ve lintDebug başarılı. UI testleri bu koşuda çalıştırılmadı; canlı ağ çağrısı yapılmadı.

## Kalıcı bekleyen sohbet yüklemeleri — devam turu

Room 18→19 geçişi pendingUpload sütununu ekliyor; varsayılan 0 olduğu için eski/bilinmeyen kayıtlar otomatik sunucuya gönderilmiyor. Yeni yerel kayıt, uzak yazıdan önce aynı satır/kimlik ve pending işaretiyle saklanıyor. Sunucu onayı yalnızca aynı kullanıcı, kimlik ve updatedAt sürümünü işaretler; eski onay daha yeni düzenlemeyi kuyruktan çıkarmaz. Yerel sürüm aynı milisaniyedeki kayıtlarda da monoton artırılır. Pending kayıt tam snapshot uzlaştırmasıyla silinmez/ezilmez. Yerel takip alanları Firestore'a gönderilmez.

AiHistoryViewModel oluşturulup hesap bağlandığında bekleyen kayıtlar kendi mevcut belge kimlikleriyle yeniden denenir. Tek koşu 60 saniye ile sınırlıdır; hata kalan kayıtları veritabanında bırakır. Geçmiş ekranında bekleyen sayı/hata ve Yeniden gönder eylemi bulunur. Kaydet ve bitir mevcut bir sohbeti düzenliyorsa conversationId geçirilir. Kayıt içeriği Room'a yazıldıktan sonra uygulama kapanırsa sonraki kuyruk gönderimi yeni belge üretmez. Room yazısı öncesindeki kaydedilmemiş taslağın süreç ölümüyle kurtarılması garanti edilmez.

Silme sırası geldiğinde ilgili pending işaretleri önce kapatılır; başarısız silmenin ardından otomatik yükleme ile kayıt diriltilmez. Sunucuda zaten başlamış Firestore yazıları iptal garantisi taşımaz. Ayrı ViewModel/cihazlar arası yarışlar ve kalıcı silme tombstone'u hâlâ açık. Kayıt kuyruğu WorkManager arka plan işi değildir; uygulama kapalıyken gönderim veya ağ geri geldiği anda kendiliğinden çalışma vaat edilmez. Yeniden hesap bağlanması/ekranın ViewModel oluşturması ve manuel yeniden gönderme tetikler.

Yeni gerçek SQLite testi veritabanını yeniden açıp aynı kayıt kimliğinin kaldığını; kullanıcı izolasyonunu, eski onayın yeni sürümü kuyruktan çıkaramamasını, doğru onayın kuyruğu kapatmasını ve silme niyetinin yüklemeyi durdurmasını kontrol eder. 11→19 migration zinciri eski kayıtların pending=false olarak korunduğunu denetler. Ana durum 22/45 tamamlanan, 23 açık geniş madde.

Kuyruk turu doğrulaması: 122 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 6 cihaz testi (4 veritabanı, 2 Firebase model; 0 hata), assembleDebug ve lintDebug başarılı. Compose UI ve canlı Firebase testleri bu tur çalıştırılmadı.

## Kullanıcının kapsamlı kalite raporuna göre geniş denetim

20 kategorilik kapsam matrisi, severity/tekrar adımları/etki/kanıt ve açık testler ayrı dosyada: [Kapsamlı kalite denetimi](KAPSAMLI_KALITE_DENETIMI_2026-10-22.md). Bu tur 5 kodla doğrulanmış bug, 1 doğrulama gereken UI riski ve 1 Improvement kaydedildi; bunlar uygulamanın toplam bug sayısı değildir.

Düzeltmeler: farklı hedefli aynı metin bildirimleri gizlenmiyor; boş misafir listesinde iki katmandaki sahte bildirim geri dönüşü kaldırıldı; tümünü sil onayı kapanıyor ve sonuç bekleniyor; ortak bileşenlerin zorunlu büyük harf dönüşümü kaldırıldı; şehir arama/öneri/kayıtlı liste tek LazyColumn ve IME boşluğu altında; bildirimlerde kart başına timer yerine tek STARTED-lifecycle saati kullanılıyor. İlk kurulum örnek bildirimi seed akışı ayrı kalıyor. Pil/frame kazancı ölçülmüş gibi sunulmuyor.

İlk cihaz koşusunda 6 veritabanı/model testi geçti, 10 Compose testi No compose hierarchies found ile başarısızdı. ADB ekranı SCREEN_STATE_OFF ve showing=true gösterdi. Kullanıcı kilidi açtıktan sonra son kodla tekrar test başlatıldı. İlk başarısız koşu kaydı korunuyor. Tam uygulama görsel matrisi, gerçek TalkBack, release performans profili, canlı hesaplar ve kalıcı silme devam kaydı hâlâ açık.

## Geniş denetim — tamamlanan doğrulama

- Android: 125 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Lint: 0 hata, 187 uyarı, 2 ipucu.
- Samsung SM-T500 / Android 12: kilit açıldıktan sonraki son koşuda 16/16 test geçti (4 SQLite, 2 Firebase model, 10 izole Compose). Şehir yönetiminde %200 yazı/320×420 dp alanda kaydırıp varsayılan eylemine basma ve ortak butonda İzinleri yönet metni doğrulandı. Gerçek klavye/TalkBack/full-Activity rotasyon bu testler değildir.
- Backend: 64 test geçti, Ruff temiz. Genel koşuda atlanan 4 PostgreSQL testi daha sonra ayrı izole PostgreSQL 16 kümesinde geçti. Test ortamı üretime bağlanmadı.
- Firebase Firestore/Storage: demo-havamania-audit yerel emülatörlerinde 4 kural testi geçti; emülatörler kapatıldı. Logdaki PERMISSION_DENIED satırları beklenen negatif erişim testleriyle ilişkili, test başarısızlığı değil.
- İlk cihaz koşusunda kilitli ekrandan kaynaklanan 10 başarısızlık raporda tutuluyor. Son başarılı koşu önceki başarısızlığı gizlemez.
- Backend/dependency deprecation uyarıları var; bu tur bağımlılık güncellemesi yapılmadı.

Öncelikli devam: kalıcı AI silme devam kaydı ve çoklu işlem yarışları; bildirim tekli işlemlerinde hata/sahiplik; tam UI matrisinde telefon/yatay/klavye; ardından ölçümlü açılış/frame/RAM-pil profili. Önceki sekiz yayın başlığı korunuyor. Sonuç tüm ürünün yayın onayı değildir.

## Kalıcı AI geçmişi silme devam kaydı — 19→20

Silme talebi kullanıcı/sohbet kimliğiyle yeni ai_history_deletions tablosuna yazılır; aynı transaction yükleme işaretini kapatır. Boş sohbet kimliği tüm geçmiş isteğini temsil eder. Uzak silme onayı gelmedikçe talep bekler. Yeniden ViewModel/hesap bağlanmasında bekleyen silmeler, yüklemelerden önce yeniden denenir. Her iki kuyruk aynı süreçte tüm AiHistoryViewModel örneklerinin paylaştığı coroutine kilidini kullanır.

Onaylanan tekli silme işareti tamamlandı olarak korunur; geç snapshot ve kayıt girişleri o kimliği yeniden oluşturamaz. Toplu silmede bilinen yerel/uzak kimlikler için aynı koruma tutulur; genel engel tamamlanınca kaldırılır, yeni kimlikli sohbetler kaydedilebilir. Hesap silme cihaz temizliği bu işaretleri de temizler. Aynı silmenin tekrar tamamlanması güvenlidir. Room 19→20 migration yeni tabloyu oluşturur; eski sohbetler değiştirilmez.

Yeni SQLite testi veritabanını kapatıp açar; bekleyen silme kimliği, kullanıcı izolasyonu, yükleme engeli, eski snapshot ve kayıt engeli, idempotent tamamlama ve tamamlanmış silmeden sonra geç snapshot korumasını denetler. Toplu silme işaretinin diğer kullanıcıyı etkilemediği de kontrol edilir.

Sınırlar: veritabanına niyet yazılmadan süreç ölürse talep kaydedilmiş sayılmaz. Başka cihazda eşzamanlı oluşturulan yeni kayıtların küresel silme garantisi yoktur. Tamamlanmış kimlik işaretleri hesap verileri silinene kadar saklanır; zamanla büyüme için güvenli temizleme politikası ayrıca değerlendirilmeli. WorkManager arka plan yürütmesi ve gerçek Firebase kesinti/çok cihaz testi açık. Bu tur canlı kullanıcı verisi kullanılmadı. Ana 22/45 geniş madde durumu korunur; bütün senkronizasyon matrisi kapanmaz.

Kalıcı silme turu doğrulaması: 125 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 7 cihaz testi (5 SQLite, 2 Firebase model; 0 başarısızlık/0 hata) geçti. Debug derleme ve lint başarılı; 0 lint hatası, 187 uyarı, 2 ipucu. UI, backend ve canlı Firebase bu tur yeniden test edilmedi. Sonraki iş bildirim tekli silme/okundu işlemlerinde hata ve hesap değişimi koruması.

## Profil geçişi ve kod tekrarları devam turu

[Video analizi, durum matrisi ve kod denetimi](EKRAN_DURUMLARI_VE_KOD_DENETIMI.md). Bu turun değişiklikleri yereldedir; bu tur için Git gönderimi henüz yapılmadı. Önceki Git gönderimi bilgisi önceki tur içindir.

Profil geçişinin cihaz doğrulaması tamamlandı: ilk görünümde isim/biyografi/rozet birlikte, iki Asistan → Profil dönüşünde kararlı. İlk yüklemede rota/analiz sayaçlarının geçici 0 göstermesi ayrıca doğrulandı ve sıradaki düzeltme olarak açık tutuldu. Kanıt ve kapsam: EKRAN_DURUMLARI_VE_KOD_DENETIMI.md. Değişiklikler henüz yerel; push yapılmadı.


## Firebase ve senkronizasyon kapsamı — 22 Ekim 2026

Kullanıcı isteğiyle ana listenin üçüncü başlığına üç ayrı açık madde eklendi: Firebase yaşam döngüsü, Firebase–cihaz eşitlemesi ve cihazlar/Firebase/backend arasındaki uçtan uca senkronizasyon. Güncel toplam 50 geniş madde: 22 tamamlanan, 28 açık. Önceki 47/22/25 sayıları tarihsel durumdur; yeni kapsam çözülmüş sayılmaz.

Öncelik: profil sayaçlarında yükleniyor/gerçek sıfır ayrımı; ardından senkronizasyon kaynakları ve kuyruk/dinleyici denetimiyle birlikte şehir/seyahat boş-dolu ekranları. Canlı dağıtım veya üretim yazımı bu kapsam eklemesiyle yapılmaz. Profil geçiş düzeltmeleri, izole test, kod temizliği ve bu takip güncellemesi aynı Git gönderimi için hazırlanmıştır. Bu tur yalnızca belgeler değişti; önceki başarılı kod testleri yeniden çalıştırılmadı.


## Temizlik, güvenlik ve yayın hazırlığı kapsam eki — 22 Ekim 2026

Beş ayrı açık kontrol eklendi: profil fotoğrafı/Blaze hazırlığı, kapsamlı güvenlik, veri/kaynak/proje temizliği, geçiş akıcılığı ve Play Console hesap kuruluşu/yükleme hazırlığı. Mevcut KVKK/gizlilik/koşullar maddesi ayrıntılandırıldı; aynı iş için ikinci bir madde sayılmadı. Güncel ana liste: 55 madde, 22 tamamlanan, 33 açık. Bunlar doğrulanmış yeni hata sayısı değil, zorunlu inceleme kapsamıdır.

Öncelik sırası: profil sayaçları ve Firebase senkronizasyonu; güvenlik ve veri temizliği (kritik bulgu çıkarsa öne alınır); şehir/seyahat dahil ekran durumları ve geçiş performansı; avatar/Blaze hazırlığı; yasal metinler ve mağaza beyanları; Play Console kuruluş ve imzalı AAB iç test/yayın adımları. Güvenlik, hukuki içerik ve mağaza kontrolleri tamamlanmadan yayın onayı verilmez.

Bu tur takip belgeleri güncellendi; yeni güvenlik taraması, Blaze geçişi, hesap kuruluşu, yasal uygunluk doğrulaması veya mağazaya yükleme yapılmadı. İlgili aşamalarda güncel resmi kaynaklar ve gerçek yapılandırma incelenecek. Önceki test sonuçları yeni kapsamın geçtiği anlamına gelmez.


## Mimari ve ürün kalitesi kapsam eki

Kullanıcının 30 maddesi [mimari ve kalite matrisinde](MIMARI_VE_URUN_KALITESI.md) eşlendi. Mevcut konular mükerrer sayılmadı; mimari/kod organizasyonu, teknik borç, gerçek kişiselleştirme ve 10 gün/48 saat kuralları dört ayrı açık madde olarak eklendi. Görsel denetimin tipografi/Türkçe/kozmetik kabul ölçütleri genişletildi. Güncel sayı 59 geniş madde: 22 tamamlanan, 37 açık. Yeni hata sayısı değildir.

Bu tur yalnızca takip belgeleri güncellendi ve Git gönderimine hazırlandı; yeni mimari, kişiselleştirme veya performans testi çalıştırılmadı. Önceki turdan kalan temizlik/güvenlik/Blaze/Play Console/yasal sayfa/geçiş kapsamı da aynı gönderime dahildir.

## 23 Ekim — Seyahat eşitlemesinde veri kaybı koruması

Profil sayaçlarının incelenmesi sırasında TravelViewModel içinde sunucuda görünmeyen, 60 saniyeden eski yerel seyahatlerin silindiği doğrulandı. Yerel kayıt yaşı yükleme onayı değildir. Bu otomatik silme kaldırıldı; çevrimdışı/bekleyen kaydın yalnızca uzak listede bulunmadığı için silinmesi önlendi.

Sınır: başka cihazdan silinen seyahatlerin yerelde temizlenmesi, kalıcı senkronizasyon onayı ve silme işaretleri eklenene kadar garanti edilmez. Uzaktan gelen kayıtların yerel değişiklikleri ezmesi ve kalıcı gönderme kuyruğu ayrıca açık. Profil sayaçlarında yükleme/gerçek sıfır ayrımı henüz tamamlanmadı. Ana takip 59/22/37 olarak korunur; bu bir alt düzeltmedir.
Doğrulama: testDebugUnitTest ve assembleDebug başarılı. Cihaz/çok cihazlı Firebase testi ve lint bu tur çalıştırılmadı. Değişiklikler yerel; push yapılmadı.

## 23 Ekim — Profil sayaçları

Boş yerel liste artık doğrulanmış sıfır kabul edilmiyor. Sunucudan boş ve bekleyen yazısı olmayan snapshot gelene kadar boş sayaç — olarak gösteriliyor; dolu yerel liste sayısı korunuyor. Boşluk onayı UID ile tutulup hesap değişiminde temizleniyor. Seyahat dinleyicisi metadata değişimlerini de alıyor. İki birim testi eklendi.

Sınır: — yükleniyor/çevrimdışı/hata durumlarını henüz ayrı açıklamıyor; dolu yerel sayının sunucuyla tamamen eşit olduğu garantisi yok. Cihazda tekrar görsel kontrol, tam hata/yeniden deneme ve A→B uçtan uca matrisi açık. Genel 59/22/37 takip durumu değişmedi. Push yapılmadı.
Sonuç: testDebugUnitTest ve assembleDebug başarılı; git diff --check temiz. Bu tur cihaz testi ve lint çalıştırılmadı.
23 Ekim devam: seyahat silme yerel kaydı bulut onayından önce kaldırıyordu. Sunucu onayı ardından yerel silme ve 30 saniye bekleme sınırı eklendi; hata/zaman aşımında tekrar deneme mesajı var. İki birim testi eklendi. Firestore bekleyen yazıyı timeout sonrası sunucuya gönderebilir; timeout iptal garantisi değildir. Kalıcı silme işareti, dinleyici yarışı ve çok cihazlı silme uzlaştırması açık. Push yapılmadı.
Seyahat silme turu doğrulaması: testDebugUnitTest ve assembleDebug başarılı. İlk Kotlin daemon başlatma uyarısından sonra derleme tamamlandı. Cihaz, canlı Firebase ve lint bu tur çalıştırılmadı.
23 Ekim kalıcı seyahat silme: Room 20→21 için trip_deletions tablosu ve migration eklendi. UID/kayıt bazlı bekleyen istek önce diske yazılıyor; uygulama ViewModel açılışında aynı kullanıcı için yeniden deneniyor. Tamamlanmış işaret korunuyor; insertTravelPlan transaction bu işaretli kaydı geri eklemiyor. Hesap silmede işaretler de temizleniyor. Arka planda kapalı uygulama yürütmesi, gönderme kuyruğu, çok cihazlı çatışmalar ve eski yazıların uzak kaydı yeniden oluşturması açık. Yeni izole cihaz testi yeniden açılış/UID ayrımı/işaret temizliğini kapsıyor; 20→21 eski şemadan migration cihaz testi ayrıca açık.
23 Ekim devam doğrulaması: yarım kalan TripDeletionPersistenceTest SM-T500 üzerinde doğrudan instrumentation ile tekrar çalıştırıldı: OK (1 test), 0 hata. Yeni izole Room veritabanını kapatıp açma, bekleyen silmenin kalıcılığı, A/B ayrımı, tamamlanmış işaret ve UID bazlı temizlik doğrulandı. Debug ve test APK kurulumu başarılı; önceki testDebugUnitTest/assembleDebug/assembleDebugAndroidTest koşusu başarılı. Bu test eski v20 şemasından migration veya gerçek Firebase kesintisi testi değildir. 20→21 migration testi, eski snapshot ile yeniden ekleme regresyonu ve çok cihazlı uçtan uca kontroller sırada. Bu tur üretim kodu değiştirilmedi; önceki yerel değişiklikler henüz push edilmedi.
23 Ekim migration ve regresyon doğrulaması: TripDeletionPersistenceTest tablette 3/3 geçti. Oluşturulmuş v20 şema örneği (v21 tablosu çıkarılıp sürüm 20 yapılarak) 20→21 migration ile Room şema doğrulamasından geçti, mevcut seyahat korundu ve kuyruk yazılabildi. DAO üzerinden eski kayıt uygulanarak bekleyen/tamamlanan silmenin geri alınmadığı doğrulandı; yeniden açılış ve UID ayrımı da geçti. Önceki açık migration maddesinin bu yerel kapsamı doğrulandı; gerçek eski sürüm dosyası, canlı Firebase, eşzamanlı uzak yazılar ve çok cihaz testi açık. Bu tur üretim kodunda yeni hata bulunmadı/değişiklik yapılmadı; iki regresyon testi eklendi. assembleDebugAndroidTest ve git diff --check başarılı. Push yapılmadı.
23 Ekim yazma/silme sıralaması: TravelViewModel içindeki altı doğrudan Firebase set yolu uploadTrip üzerinden geçirildi. Aynı süreçte ViewModel örnekleri ortak TripMutationGate kullanıyor; yazmadan önce UID ve kalıcı silme işareti kontrol ediliyor. Silme de aynı kapıdan geçiyor. Yazma beklemesi 30 saniye ile sınırlı. İki eşzamanlılık testi eklendi. Sınır: kilit süreç içidir; Firebase SDK iptalden sonra gönderimi tamamlayabilir. Kalıcı upload kuyruğu, sunucu sürüm/çatışma kontrolü, süreç ölümü ve çok cihazlı yarışlar açık; tüm senkronizasyon tamamlandı sayılmaz.
Yazma/silme sıralaması turu: testDebugUnitTest ve assembleDebug başarılı, git diff --check temiz. Cihaz/Firebase testleri ve lint bu tur yeniden çalıştırılmadı. Değişiklikler yerel; push yapılmadı.
23 Ekim kalıcı seyahat yükleme kuyruğu: Room 21→22 trip_uploads tablosu, UID/kayıt/sürüm bazlı gönderim kaydı ve atomik saveLocalTrip eklendi. Altı yerel yazma yolu bu kaydı oluşturuyor; yeniden ViewModel açılışında bekleyenler deneniyor. Eski gönderim onayı yeni sürümü silmez, bekleyen yerel değişiklik uzak snapshot ile ezilmez; tamamlanan silme gönderim kaydını da temizler. Hesap silme temizliği güncellendi. Eski kurulumdaki geçmiş başarısız yazılar geriye dönük otomatik kuyruğa alınmıyor. Uygulama kapalı arka plan yürütmesi, bağlantı geri gelince otomatik tetikleme ve görünür manuel yeniden deneme, çok cihazlı çatışmalar açık.
Kalıcı yükleme turu: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Migration testi oluşturulmuş v20 şemasından 20→21→22 zincirine güncellendi. Canlı Firebase ve çok cihazlı test değildir; eski başarısız kayıtları kurtarma ve görünür senkronizasyon durumu sonraki işlerdir.
Son cihaz sonucu: SM-T500 üzerinde TripDeletionPersistenceTest 4/4 geçti (0 hata). Yeniden açılış, UID ayrımı, eski snapshot/silme, oluşturulmuş şemadan migration ve sürüm bazlı upload onayı doğrulandı. Push yapılmadı.
23 Ekim görünür yükleme kuyruğu: seyahat ekranına UID bazlı bekleyen yükleme sayısı ve çevrimiçiyken Yeniden dene eylemi eklendi. Silme işaretli kayıtlar sayıdan hariç. Bağlantı geri geldiğinde ViewModel yaşarken yüklemeler otomatik deneniyor; aktif retry işi varsa ikinci iş başlatılmıyor. Hesap değişiminde sayı sıfırlanıp ilgili hesabın Room akışına bağlanıyor. Bu gösterge yüklemeleri kapsıyor; silme durumu, ayrıntılı hata/yükleniyor gösterimi, kapalı uygulamada yürütme ve gerçek ağ kes/kurtar cihaz testi açık.
Görünür kuyruk turu doğrulaması: testDebugUnitTest ve assembleDebug başarılı, git diff --check temiz. Mevcut birim testleri çalıştırıldı; yeni bağlantı/UI davranışı cihazda henüz doğrulanmadı. Lint bu tur çalıştırılmadı. Push yapılmadı.
23 Ekim bekleyen silme görünümü: UID bazlı Room sayacı tamamlanmamış silmeleri gösteriyor. Seyahat ekranında yüklemelerden ayrı metin, iki kuyruğu çalıştıran ortak Yeniden dene eylemi var. Bağlantı geri geldiğinde silmeler de deneniyor; retry işi silmeleri sırayla bekliyor ve aynı retry çalışırken yeni iş başlatılmıyor. İzole cihaz testine A/B ve tamamlanan işaretin sayaçtan çıkması kontrolleri eklendi. Ayrıntılı kalıcı hata durumu ve çalışıyor düğme görünümü henüz açık; düğme çevrimdışıyken kapalı. Push yapılmadı.
Bekleyen silme görünümü sonucu: birim testleri, debug/test APK derlemeleri ve tablette 4 izole veritabanı testi geçti. Yeni sayaç kontrolleri bu cihaz koşusuna dahil. Tam ekran UI, gerçek bağlantı kesintisi ve canlı Firebase testi yapılmadı. git diff --check temiz; push yapılmadı.
23 Ekim Git gönderimi: önceki turlardaki push yapılmadı notları o anki durumu belirtir. Profil sayaçları, Room 20→22 kalıcı seyahat yükleme/silme kuyrukları, süreç içi yazma/silme sıralaması, bekleyen işlem görünümü, yeniden deneme ve ilgili testler bu gönderime dahil edildi. Son doğrulama: birim testleri ve debug/test APK derlemeleri başarılı, tablette 4 izole veritabanı testi geçti. Açık canlı/UI kontrolleri aynı şekilde devam ediyor.
23 Ekim eşitleme durumları: manuel/otomatik kuyruk denemesi sırasında Eşitleniyor metni ve pasif düğme eklendi. Yükleme/silme için ayrı StateFlow hata metinleri, yeniden denemede temizleme ve DAO okuma hatasında görünür hata eklendi. Hesap değişiminde durum sıfırlanıyor; retry finally blokları oturum epoch kontrolü yapıyor. UI ve gerçek ağ kesintisi cihaz testi açık. Doğrudan tek kayıt yazma yollarının tamamı henüz bu retry durum modeline dahil değil. Push yapılmadı.
Eşitleme durumları turu sonucu: mevcut birim testleri ve assembleDebug başarılı; git diff --check temiz. Yeni UI/oturum durum davranışı için özel otomatik test ve cihaz testi bu tur yapılmadı; lint tekrarlanmadı. Push yapılmadı.
23 Ekim özel eşitleme durum testleri: başlangıç/hata/bitiş için SyncAttemptState oturum bileti kontrolü eklendi. Önceki koruma yalnız retry finally/hata için vardı; gecikmiş başlangıç ve doğrudan silme hata yayını da aynı oturum kontrolüne bağlandı. İki yeni JVM testi eski oturumun yeni durumu başlatamamasını/bozamamasını ve hata metninin bitişten sonra yeniden denemeye kadar korunmasını kapsıyor. Doğrudan kayıt işlemlerinin tamamı, UI görsel ve canlı A→B testi hâlâ açık.
Özel eşitleme durum testi sonucu: SyncAttemptStateTest dahil testDebugUnitTest ve assembleDebug başarılı. Eski oturum başlangıç/hata/bitiş koruması ve hatanın yeniden denemeye kadar korunması için iki yeni test geçti. Cihaz ve canlı hesap değişimi testi bu tur yapılmadı; lint tekrarlanmadı. Push yapılmadı.
23 Ekim doğrudan seyahat yazma durumu: uploadTrip ortak eşitleme durumuna bağlandı; kayıt/düzenleme/arşiv/analiz yazmaları da durum ve hata yayımlıyor. SyncAttemptState aktif işlem sayısı kullanıyor; iç içe retry/yazma veya eşzamanlı yazmalarda ilk biten iş göstergeyi erken kapatmıyor. Aktif grup içinde yeni işlem önceki hatayı silmiyor. İki yeni özel test eklendi: örtüşen işlerin göstergesi/hata korunması ve oturum sıfırlama sonrası eski bitişler. UI cihaz doğrulaması ve gerçek ağ kesintisi açık; push yapılmadı.
Doğrudan yazma durumu doğrulaması: testDebugUnitTest ve assembleDebug başarılı. SyncAttemptStateTest toplam 4 test (bu tur 2 yeni) geçti; hata ve örtüşen iş/oturum davranışı doğrulandı. UI ve canlı servis testi bu tur yapılmadı, lint çalıştırılmadı. Push yapılmadı.
23 Ekim canlı tablet kontrolü: son debug APK veri silmeden kuruldu; WeatherPremiumActivity açıldı. İlk anlık kare boş açılış yüzeyi, sonraki kare normal Konum Seçin ekranı; bu kayıttan açılış süresi ölçülmedi. Takvimde 23 Ekim boş gün görünümü ve 21 Ekim seçimi görüntülendi. 21 Ekim altında kayıt işareti olmasına rağmen sağ panel Bu tarih için planlanmış seyahat yok gösterdi; filtre/işaret veri kapsamı tutarlılığı doğrulanacak yeni görsel bulgu olarak açık tutuldu. Mevcut hesapta bekleyen eşitleme görünmedi; yükleniyor/hata bileşenini test etmek için gerçek kayda yazı yapılmadı, bu UI durumları doğrulanmadı. Kanıtlar ignored .local-tests/travel-review.png ve travel-populated-review.png. Bu tur kod değişmedi; push yapılmadı.
23 Ekim görsel bulgu düzeltmesi: önceki 21 Ekim kayıt işareti yorumu yanlıştı; işaret 22 Ekim altındaydı. Tablette 22 Ekim seçilince Batman seyahati açıldı (.local-tests/calendar-22-check.png). Filtre tutarsızlığı doğrulanmadı; önceki kayıt geri çekildi. Ayrı doğrulanmış hata: highlight null iken city.contains(boş metin) bütün kartları vurguluyordu. Boş/blank sorgu artık vurgulamıyor; açık hedef ID veya dolu eşleşen sorgu gerekiyor. İki regresyon testi eklendi.
23 Ekim kart vurgusu doğrulaması: testDebugUnitTest ve assembleDebug başarılı; TripHighlightTest 2 test, 0 hata. Güncel APK mevcut veriler korunarak tablete kuruldu. Takvimde boş 23 Ekim ve kayıtlı 22 Ekim görüntülendi; Batman kartındaki gereksiz turuncu odak çerçevesi kalktı (.local-tests/highlight-check.png). Tüm ekran/tema/büyük yazı incelemesi tamamlanmış sayılmıyor. Sırada senkronizasyon bekleme/hata panelinin izole UI testleri ve takvim Türkçe büyük harf (EKIM → EKİM) tutarlılığı var. Bu tur push yapılmadı.

## 23 Ekim — Senkronizasyon paneli ve Türkçe takvim
- Takvim ay/gün başlıklarında büyük harf dönüşümü tr-TR ile yapılıyor: EKIM yerine EKİM.
- Bekleyen gönderme/silme, işlem sürüyor ve hata durumları TripSyncStatusPanel bileşenine ayrıldı. Panel metin ve düğme renkleri uygulama temasını kullanıyor; değişiklikler erişilebilirlik canlı bölgesi olarak işaretlendi.
- 137 JVM testi: 0 hata. testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı.
- Samsung tablette 3 yeni izole Compose testi geçti: çevrimdışı ve işlem sürerken yeniden denemenin kapalı olması; %200 yazıda hata ve yeniden deneme erişimi, hata kaldırılınca panelin kaybolması; EKİM başlığı.
- Bu testler Firebase çağrısı yapmaz; hata/success geçişi test durumuyla sağlanır. Gerçek ağ kesintisi, gerçek TalkBack, tüm temalar/telefonlar ve canlı Firebase doğrulanmış sayılmaz.
- Sıradaki: takvim şeridinin gün değişimi ve tarih aralığı dışındaki seyahatlere yönlendirilmesi; ardından senkronizasyonun gerçek kesinti/hesap değişimi matrisi (yerel/izole testlerle).
- Bu tur push yapılmadı; önceki çalışma değişiklikleri korundu.

## 23 Ekim — Takvim tarih penceresi ve gün güncellemesi
- Takvim şeridi ekranın ViewModel tarihini alıyor; uygulamaya dönüşte loadPlans ile güncellenen bugün artık ilk açılış değerinde kalmıyor.
- Aralık dışı seçili tarih için pencere o tarihe taşınıyor; yıl farkından bağımsız 121 günlük sınırlı liste korunuyor. Seçim değiştiğinde ilgili gün görünür konuma kaydırılıyor.
- Günlere tam Türkçe tarih, seçili durum, bugün ve seyahat var bilgileri için erişilebilirlik semantiği eklendi.
- testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Tablette iki izole Compose testi geçti: Türkçe ay başlığı ve 320 dp alanda 2026 → 2030 → 2020 seçimi/gün bilgisinin yenilenmesi.
- Sınır: testte bugün girdisi değiştirilir; cihaz saati değiştirilmedi. Ekran gece boyunca ön planda kalırsa otomatik gün yenileme henüz eklenmedi; sıradaki iş bu. Gerçek TalkBack ve tam bildirimden seyahate yönlendirme uçtan uca testi açık.
- Bu tur push yapılmadı.

## 23 Ekim — Ön planda otomatik gün yenileme
- Seyahat ekranı RESUMED durumundayken sistem tarihini 60 saniyede bir kontrol eder; tarih değişimi en geç bir sonraki kontrolde bugün işaretine ve tarih tabanlı hesaplamalara iletilir.
- Ekran duraklatıldığında/sahneden çıkıldığında coroutine iptal edilir. Dönüşte ilk okuma hemen yapılır. Kontrol, seyahat veritabanını veya ağı yeniden yüklemez; aynı tarih tekrar yayınlanmaz.
- Gece yarısı/geri saat değişimi ve iptal-yeniden başlatma için kontrollü saat/tetikleyici kullanan iki JVM regresyon testi eklendi.
- Kapsam sınırı: gerçek cihaz saati değiştirilmedi; gerçek gece yarısı/lifecycle uçtan uca testi ayrıca açık. Seçilen geçmiş tarih kullanıcı tercihi olarak korunur.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı; iki yeni tarih akışı testi geçti. Bu tur cihaz testi veya push yapılmadı. Sıradaki: yerel senkronizasyon kesintisi ve hesap değişimi senaryolarının eksik kapsamı.

## 23 Ekim — Tekil silme durumu ve oturum izolasyonu
- Tekil seyahat silme de SyncAttemptState işlem sayacına alındı. İşlem boyunca bekleme durumu açık; başarı/hata/iptalde finally ile kapanır. Kuyruk yeniden denemesiyle iç içe işlemlerde mevcut sayaç korunur.
- Silme hatasının ikinci, oturum etiketi taşımayan geçici mesajı kaldırıldı. Hata, mevcut oturum nesliyle korunan panelde kalır; hesap değişiminde sıfırlanır.
- Üç yerel coroutine regresyon testi: iptal sonrası bekleme durumunun kapanması ve yeni oturumda işlem; eski oturumdaki gecikmiş hata/tamamlanmanın yeni oturumu etkilememesi; bağlantı hatasının korunması ve yeniden denemede temizlenmesi.
- Kapsam: durum yönetimi testidir; gerçek Firebase kesintisi, tüm ViewModel uçtan uca akışı veya çok cihazlı çatışma testi değildir. Diğer seyahat işlemlerindeki geçici mesajların oturum izolasyonu sıradaki kontroldür.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı; SyncAttemptStateTest toplam 7 test, 0 hata. Bu tur cihaz testi veya push yapılmadı.

## 23 Ekim — Seyahat mesajları ve analiz iptali
- Seyahat ViewModel mesajları yayınlanmadan önce coroutine iptali ve güncel UID kontrolünden geçiyor; A-B geçişinde ve iptal edilmiş A oturumuna geri dönüşte eski yayın engelleniyor.
- Kaydetme/arşivleme/geri alma/not işlemlerindeki mükerrer bulut hata mesajları kaldırıldı. Yerel kayıt başarılıyken işlem tamamen başarısızmış gibi mesaj gösterilmiyor; bulut hatası kalıcı senkronizasyon panelinde kalıyor.
- Analiz sonucu ve hata işlenmeden önce iptal kontrolü eklendi. Eski analiz finally bloğu yalnızca kendi Job kaydını kaldırır; aynı kimlikli yeni işi silemez.
- Üç regresyon testi: farklı UID için yayın yok, güncel UID için yayın var, iptal yutulsa ve aynı UID geri gelse bile eski sonuç yayınlanmıyor.
- Açık kapsam: daha önce ekrana teslim edilmiş snackbar mesajlarının hesap değişiminde kapatılması ve şehir önerisi sonuçlarının iptal sonrası korunması ayrıca incelenecek. Gerçek Firebase/cihaz uçtan uca hesap değişimi bu tur çalıştırılmadı.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı; AccountResultTest 3 test, 0 hata. Bu tur cihaz testi veya push yapılmadı.

## 23 Ekim — Oturum değişiminde mesaj ve öneri temizliği
- Seyahat ekranının mesaj koleksiyonu oturum sürümüne bağlandı. Oturum değişince çalışan showSnackbar çağrısı iptal edilir; sonraki oturumun mesajları yeni koleksiyonda alınır. Aynı UID ile yeniden giriş de yeni sürümdür.
- Varış ve başlangıç şehri aramalarında yeni sorgu başlar başlamaz eski öneriler temizlenir. Ağdan dönen sonuçlar AccountResult iptal/UID kontrolünden sonra yayınlanır. Hesap değişimi her iki listeyi de temizler.
- Yerel coroutine testi eski mesaj gösteriminin iptalini ve yeni mesajın alınmasını kontrol eder. Önceki AccountResult testleri geç sonuçlar için kullanılan ortak iptal/UID kontrolünü kapsar.
- Sınır: gerçek Snackbar/cihaz ve Firebase hesap değişimi bu tur test edilmedi; gerçek arama servisi çağrılmadı. Sıradaki: şehir aramalarında hata/boş sonuç/yükleniyor durumlarının ayrımı ve UI geri bildirimi.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı. SessionMessagesTest 1 test, 0 hata. Bu tur push yapılmadı.

## 23 Ekim — Seyahat konum araması durumları
- Repository arama hatasını boş sonuç gibi gösteriyordu. Seyahat formu için hatayı ileten searchCityOrThrow yolu eklendi; diğer mevcut çağırıcıların davranışı bu tur değiştirilmedi.
- Başlangıç/varış araması için IDLE/LOADING/RESULTS/EMPTY/ERROR ayrımı; 15 saniye bekleme sınırı, hata metni ve yeniden deneme eylemi eklendi. İptal normal hata olarak yayınlanmaz.
- Form açılış/kapanışında aramalar ve eski durumlar temizlenir. Yeni sorgu ve hesap değişimi korumaları korunur.
- Üç yerel test: boş yanıt ile ağ hatası ayrımı, sonuçların korunması, iptalin hata sonucuna çevrilmemesi.
- Açık kapsam: gerçek arama servisi ve cihazda klavye/büyük yazı ile yeni geri bildirimlerin görsel kontrolü. Diğer şehir yönetimi aramalarının hata/boş sonuç ayrımı ayrıca incelenecek.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı; LocationSearchStatusTest 3 test, 0 hata. Bu tur cihaz testi veya push yapılmadı.

## 23 Ekim — Şehir yönetimi arama durumları ve cihaz testleri
- WeatherViewModel şehir araması da hatayı ileten repository yoluna geçti. Aranıyor/sonuç/boş/hata ayrımı ve 15 saniye sınırı ortak arama işlevinden geliyor. İptal kontrolü korunuyor; hesap değişiminde durum sıfırlanıyor.
- Şehir yönetiminde ortak geri bildirim ve yeniden deneme eylemi gösteriliyor; ekrandan çıkışta arama iptal edilip sonuçlar temizleniyor.
- testDebugUnitTest, assembleDebug, assembleDebugAndroidTest başarılı. Samsung tablette iki izole Compose testi geçti: durum geçişleri/yeniden deneme; 320 dp x 300 dp görünüm ve %200 yazıda yeniden denemeye kaydırarak erişim.
- Gerçek servis veya kullanıcı verisi değiştirilmedi. Küçük görünüm testi gerçek klavye testi değildir. Seyahat formunun gerçek klavyeyle görsel kontrolü ve tüm tema matrisi açık.
- Sıradaki: ana hava ekranındaki konum arama penceresine aynı durum geri bildiriminin bağlanması; gerçek klavye ile tam form kontrolü.
- Bu tur push yapılmadı.

## 23 Ekim — Ana hava ekranı konum penceresi
- Kısa/boş sorgular ViewModel'e iletiliyor; eski önerilerin ekranda kalması engellendi. Pencere kapanınca arama temizleniyor.
- Yükleniyor/boş/hata ve yeniden deneme ortak geri bildirimi bağlandı. Başlık, etiketli arama alanı ve sonuçlar tek LazyColumn içinde; IME boşluğu ve büyük yazı için kaydırma sağlandı. Tekrarlı sonuç kimlikleri eleniyor; seçili konum vurgusu korunuyor.
- JVM testleri ve debug/test APK derlemesi başarılı. İlk izole cihaz testi Compose IME inset kontrolünde 5 saniye zaman aşımına uğradı; klavye erişimi henüz doğrulanmış sayılmadı. Test açık klavye isteği ve Android rootWindowInsets görünürlüğüyle yeniden hazırlanıyor.
- Son doğrulama: test kurulumunda açık klavye isteği ve rootWindowInsets IME görünürlük kontrolü sonrası cihaz testi geçti (1/1). %200 yazı, 320 dp genişlik, gerçek klavye görünürlüğü ve yeniden denemeye kaydırıp basma doğrulandı. İlk başarısız koşu gizlenmedi. Tam ModalBottomSheet ve seyahat formu görsel matrisi ayrıca açık. Sıradaki: seyahat ekleme formunun gerçek klavye ve büyük yazıda alan/düğme erişimi. Push yapılmadı.

## 27 Ekim — Seyahat formu büyük yazı ve konum tutarlılığı
- Form gerçek ViewModel bağlantısından ayrılarak izole TravelPlanForm olarak test edilebilir hale getirildi; üretimde aynı ViewModel verileri/callbackleri bağlanıyor. Form köküne IME boşluğu eklendi.
- Hedef konumu değiştirme açıldığında eski seçim temizleniyor; öneriden yeni seçim yapılmadan eski koordinatlarla kayıt engelleniyor. Dolu kalkış alanı da öneri seçimi gerektiriyor ve açıklayıcı hata gösteriyor.
- Düzenlemede kalkış metni silindiğinde originCity ile birlikte originLatitude/originLongitude temizleniyor. Kullanılmayan iki yerel alan kaldırıldı; konum düzenleme ikonuna erişilebilir açıklama eklendi.
- testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Samsung tablette üç izole form testi geçti: %200 yazıda metin girip/sildikten sonra kaydet düğmesine erişim; hedef kilidi açılınca kaydın engellenmesi; mevcut kalkış temizlenince koordinatların da null olması.
- Testler yalnızca onSave çıktısını yakalar; gerçek Firebase veya yerel seyahat kaydı oluşturmaz. Bu tur testinde IME görünürlüğü ayrıca assert edilmedi; önceki konum arama testinin gerçek klavye sonucu tam seyahat formuna genellenmez. Telefon/yatay/tema ve tam görsel matris açık.
- Sıradaki: form taslağının ekran yeniden oluşturulmasında korunması ve tarih alanlarının dar ekran/büyük yazı yerleşimi. Push yapılmadı.

## 27 Ekim — Form içeriği saved-state ve tarih yerleşimi
- Form içindeki seçimler, metinler, kilit durumu, seyahat türü, başlangıç/bitiş ve saat rememberSaveable kullanıyor. Seçili konum DTO'su JSON, tarihler epochDay olarak kaydediliyor. Aktif oturum değişince form key ile yeniden oluşturuluyor.
- Tarih alanları 400 dp altı genişlikte veya fontScale > 1.3 olduğunda alt alta; geniş normal yazıda yan yana gösteriliyor.
- İlk derleme eksik serialization extension importu nedeniyle başarısız oldu; import düzeltildi ve kontroller tekrar başlatıldı.
- Önemli açık kapsam: bu adım form içeriğinin saved-state geri yüklemesidir. Üst TravelPlannerScreen showAddDialog/planToEdit/prefillPlan hâlâ remember kullanıyor; gerçek Activity yeniden oluşumunda formun açık kalması ve düzenleme bağlamının korunması sonraki adımda tamamlanmalı. Süreç ölümü sonrası uçtan uca taslak tamamlandı denemez.
- Sonuç: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Tablette 2/2 izole test geçti: selected konum/koordinatlarının StateRestorationTester sonrası korunması ve 280 dp genişlik/%200 yazıda tarihlerin dikey yerleşimi. Gerçek Activity/süreç yeniden oluşturma testi değildir. Sıradaki üst ekran form açılış/düzenleme bağlamı korumasıdır. Push yapılmadı.

## 27 Ekim — Üst ekran seyahat düzenleme bağlamı
- TravelPlannerScreen form açıklığı, düzenlenen kayıt kimliği ve küçük başlangıç parametrelerini UID'ye bağlı saved-state bağlamında saklıyor. Analiz/geçmiş yükleri Bundle'a kopyalanmıyor; düzenlenen kayıt kimlik ve sahip UID ile güncel listeden çözülüyor.
- Düzenlenen seyahat henüz yüklenmediyse yeni kayıt formuna düşmek yerine yüklenme/yeniden deneme/kapat penceresi gösteriliyor.
- Başlangıç parametrelerinin işlendiği bilgisi ve ön doldurma kayıt kimliği korunuyor; yeniden oluşturma aynı parametreyle formu yeniden başlatmıyor. Form kapanınca bağlam temizleniyor.
- UID değişince veya mevcut çalışma sırasında oturum nesli değişince form kapanıyor. İlk ViewModel auth başlatması, geri yüklenen aynı kullanıcı taslağını sıfırlamamalı; saved-state anahtarı bu nedenle geçici oturum sayacı yerine UID kullanıyor.
- İki izole cihaz testi eklendi: üst bağlam + iç form taslağı geri yükleme ve kullanıcı değişiminde kapanma; ön doldurma kimliği/işlenmiş başlangıç bilgisi korunması.
- Açık kapsam: gerçek Activity recreate ve süreç ölümü/Room yükleme yarışı uçtan uca cihaz testi. Ek inceleme adayı: başlangıç parametresinden açılan ön doldurma akışında konumun gerçek koordinatla doğrulanması.
- Doğrulama: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Samsung tablette iki yeni izole test 2/2 geçti. Bunlar StateRestorationTester ile Compose saved-state testidir; gerçek Activity recreate/süreç ölümü testi değildir. Sıradaki ön doldurma akışının gerçek koordinat doğrulaması ve yükleme gecikmesinde düzenleyici entegrasyonudur. Push yapılmadı.

## 27 Ekim — Ön doldurulan konum doğrulaması
- Ön doldurma yeni seyahat akışı olarak açıkça işaretleniyor; şehir adı geldi diye hedef kilitlenmiyor ve (0,0) ile düzenleme/kayıt yapılamıyor. İlk arama şehir adıyla başlıyor; öneriden seçim gerekli. Ön doldurma kimliği/tarih/tür korunuyor.
- UI ve TravelViewModel.savePlan için ortak koordinat doğrulaması: sonlu değerler, enlem/boylam sınırları, iki ekseni birlikte sıfır olmayan konum. Tek eksende sıfır geçerli. Dolu kalkış adı için de çözümlenmiş koordinatlar gerekli.
- Kalkış kaldırılırken originDistrict de temizleniyor; yeni seçimde ilçe bilgisi kayda aktarılıyor.
- Üç JVM regresyon testi eklendi: eksik/geçersiz/sıfır çift koordinat, geçerli tek sıfır ekseni ve opsiyonel kalkış doğrulaması. Bir cihaz testi ön doldurmayı seçimsiz kaydetmenin engellenmesini, seçim sonrası gerçek koordinatların ve kayıt kimliğinin korunmasını kapsıyor.
- Bu akışta (0,0) uygulamanın eski çözümlenmemiş konum işaretidir. Mevcut hatalı kayıtlar otomatik değiştirilmedi; düzenlemede yeni konum seçimi gerektirir. Gerçek geocoding/Firebase çağrısı test edilmedi.
- Koordinat turu doğrulaması: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Ön doldurulan formun gerçek koordinat seçimi cihaz testi 1/1 geçti. Test form çıktısını yakalar; Firebase'e kayıt yapmaz.

## 27 Ekim — Düzenleyicide geciken/eksik veri ve UID kontrolü
- Üretimdeki düzenleyici yükleme/eksik kayıt davranışı TravelEditorHost bileşenine taşındı. Düzenleme kimliği varsa kayıt gelmeden yeni seyahat formu açılmaz. Kayıt çözümlemesi hem kimlik hem UID ile yapılır; farklı sahip bağlamı gösterilmez.
- Kaydet callback'i aktif Firebase UID'sini tekrar kontrol eder; ekrandaki eski formun hesabı değişmişse kayıt başlatılmaz.
- İki izole UI testi eklendi: yükleme → başka kullanıcının aynı kimlikli kaydı → doğru kaydın gelmesi → kaydın kaybolması ve farklı sahibin form bağlamının gizlenmesi.
- Gerçek Room/Firebase gecikmesi ve süreç ölümü uçtan uca senaryoları bu testlerin kapsamı dışındadır.
- Sonuç: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı; tablette geciken/eksik veri ve sahip izolasyonu testleri 2/2 geçti. Koordinat turuyla birlikte bu devamda 3 yeni cihaz senaryosu geçti. Sıradaki gerçek Activity yeniden oluşturma entegrasyonu; Firebase/Room gecikmesi ve süreç ölümü testi açık. Push yapılmadı.

## 27 Ekim — Gerçek Activity yeniden oluşturma testi
- Debug kaynağına dışa kapalı TravelEditorTestActivity eklendi; gerçek form/düzenleyici ve sabit yerel veri kullanır. ViewModel, repository veya hesap işlemi çağırmaz. Release kaynaklarına dahil değildir.
- ActivityScenario.recreate kullanan iki cihaz testi eklendi: açık düzenleyici ve yazılmış taslak; seçilen kalkış konumunun koordinatlarıyla korunması. Bunlar StateRestorationTester yerine Android Activity'yi yeniden oluşturur.
- Süreç öldürme/yeniden başlatma, gerçek uygulama navigasyonu ve Firebase/Room yeniden yükleme bu fixture testinin kapsamı değildir.
- Sonuç: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Samsung tablette TravelEditorRecreationTest 2/2 geçti: gerçek Activity recreate sonrası açık form/yazılı taslak ve seçilmiş kalkış koordinatları korundu. Bu tur yeni üretim hatası doğrulanmadı; mevcut düzeltmelerin daha güçlü doğrulaması eklendi. Sıradaki süreç ölümü/yeniden açılışın izole doğrulanması ve tam uygulama yükleme entegrasyonu. Push yapılmadı.

## 27 Ekim — İzole süreç ölümü testi hazırlığı
- Ayrı :editor_test süreci ve ayrı görev kimliği kullanan debug-only TravelEditorProcessDeathActivity eklendi. Giriş Android DUMP izni ile korunuyor; normal uygulamalar açamaz. Mevcut Activity recreate test ekranı varsayılan süreçte kalıyor.
- tools/test_trip_editor_process_death.py yerel test betiği: formu açar, taslak yazar, arka plana alır, yalnızca PID'si doğrulanan test sürecini run-as ile kapatır ve tutulan test görevini yeniden açar. Veri temizleme veya pakete force-stop uygulanmaz.
- Başarı ölçütleri: yeni PID, savedInstanceState restored=true kaydı ve aynı taslağın EditText içinde görünmesi. Bu test gerçek üretim Activity/Firebase/Room entegrasyonu yerine gerçek formun izole süreçteki saved-state davranışını ölçer.
- İlk gerçek süreç koşusu açma düğmesini bulamadan durdu. Crash kaydı ikinci süreçte MainApplication → TravelNotificationWorker.schedule → WorkManager başlatılmamış hatasını doğruladı. MainApplication otomatik başlangıç işleri varsayılan süreçle sınırlandı (API 28+ getProcessName, eski API ActivityManager PID çözümü); yeniden derleme/test başlatıldı. Ana süreçte aynı crash gözlemlendi denmiyor.
- Sonuç: testDebugUnitTest ve assembleDebug başarılı. Gerçek izole süreç testi geçti: old_pid=6979, new_pid=7139, draft_restored=true, saved_instance_state=true. Testte üretim verisi kaydedilmedi/temizlenmedi.
- Ara koşular: WorkManager ikincil süreç açılış hatası düzeltildikten sonra sinyal numarasıyla toybox kill komutları tablette Unknown signal hatası verdi. Varsayılan sonlandırma komutuyla süreç tamamen sonlandırıldı ve PID yokluğu doğrulandı; ardından yeni PID ve saved-state geri yüklemesi birlikte doğrulandı. Başarısız hazırlık koşuları tamamlanmış test sayılmadı.
- Kapsam sınırı: aynı gerçek formun ayrı debug sürecinde Android saved-state geri yüklenmesi doğrulandı. Üretim navigasyonu/Firebase Auth yeniden başlatma, Room kayıt yüklemesi ve API 23–27 süreç adı fallback'i bu koşuda test edilmedi.
- Sıradaki: tam uygulama oturum başlatma/yerel veri yükleme entegrasyonu ve son yayın kontrol listesinin güncellenmesi. Push yapılmadı.

## 27 Ekim — Yerel seyahat yükleme akışının birleştirilmesi
- Aynı listeyi güncelleyen tek seferlik DAO sorgusu kaldırıldı. Açılış/ekrana dönüş/yeniden deneme aynı Room dinleyicisini yeniler; önceki dinleyici iptal edilir.
- DAO erişimi ve domain dönüşümü flow içinde, IO dispatcher üzerinde çalışır. Veri/hata yayınları UI dispatcher'da ortak iptal/UID kontrolüyle yapılır. Okuma/dönüşüm hatası yakalanır, son başarılı liste korunur, isLoading kapanır ve yeniden yükle eylemi gösterilir.
- Boş seedPlans listesiyle çalışan ve çağrısı yalnızca init'te olan eski seyahat seed rutini kaldırıldı; kullanıcı kayıtlarını değiştiren işlem yapılmadı.
- Dört yerel coroutine testi: okuma hatası/son veriyi koruma/yeniden deneme; eski hesap verisi ve hatasının yayınlanmaması; iptalin kullanıcı hatası olmaması; bozuk tarih dönüşümünde kısmi liste yayınlanmaması.
- Kapsam: Room adaptörünü kullanan ortak gözlem protokolünün yerel testidir; gerçek bozuk kullanıcı veritabanı oluşturulmadı, canlı Auth/Firestore/Room uçtan uca arızası yapılmadı. Yerel yükleme hatası UI erişimi ve ana yayın listesiyle kalan kapsam uzlaştırması sırada.
- Doğrulama: testDebugUnitTest ve assembleDebug başarılı. AccountDataObserverTest 4/4 geçti. Bu tur cihaz testi veya canlı hata senaryosu çalıştırılmadı. Push yapılmadı.

## 27 Ekim — Yerel yükleme paneli ve yayın listesi uzlaştırması
- Yerel yükleme paneli erişilebilir canlı bölgeyle ayrıldı. Hata → yeniden deneme/yükleniyor → hata veya başarı geçişi gösteriliyor; işlem sürerken düğme kapalı, başarıda panel kalkıyor.
- testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. Samsung tablette 1 izole Compose testi geçti: 280 dp/%200 yazı, koyu→açık tema, kaydırarak yeniden deneme, işlem sırasında kapalı eylem, başarıda panelin kaybolması. Gerçek DB arızası/TalkBack/kontrast ölçümü değildir.
- Ana kontrol listesi checkbox sayımı: 59 toplam, 22 tamamlanan, 37 açık; geniş maddeler otomatik kapatılmadı. Her iki belgenin başı 27 Ekim kanıtıyla güncellendi; eski test sayıları ve önceki iş sırası tarihsel kayıt olarak işaretlendi.
- Sonraki yerel sıra: hava verisi 10 gün/48 saat sınırları ve eksik veri davranışı; dayanıklılık/UI matrisleri devam eder. Canlı Firebase/backend, hesap silme, avatar/Blaze, yasal sayfalar, Play Console ve imzalı release konuları açık. Push yapılmadı.

## 27 Ekim — Rota hava analizi 48 saat sınırı
- Tam saate yuvarlama 48 saat 59 dakika öncesini kabul ediyordu; bildirim kontrolü kalkıştan sonraki ilk saati de 0 saat gibi görebiliyordu. Ortak Instant/Duration kontrolü tam [0,48 saat] aralığını kullanıyor.
- Rota ekranı ve ViewModel analiz başlangıcı aynı uygunluk kontrolüne bağlandı; bildirim kontrolü ortak kalkış öncesi pencereyi kullanıyor. Devam eden seyahatlere erişim mevcut ürün davranışı olarak korunuyor; kalkış sonrası yeni rota-hazır bildirimi uygun sayılmıyor.
- Saat dilimi dönüşümü kalkış için cihazın mevcut ZoneId'siyle, süre karşılaştırması Instant üzerinden yapılıyor; yaz saati geçişinde 48 yerel saat ile geçen süre karışmıyor.
- Dört regresyon testi: tam sınır/+1 nanosaniye/+59 dakika; şimdi/geçmiş kalkış; Berlin sonbahar DST 49 saat örneği; yaklaşan/devam eden/geçmiş/saatsiz seyahat uygunluğu.
- Açık kapsam: 10 gün kuralı, eksik günlük veri/kod varsayımları, konum saat dilimi ve cihaz saat dilimi ayrımı; rota ekranının hiç etkileşim olmadan sınır anında otomatik yenilenmesi. Bu tur gerçek API/bildirim veya DST cihaz saati değiştirilmedi.
- Doğrulama: ilk derlemede bildirim gün etiketi için Instant.format kullanımı tür hatası verdi; aynı andan LocalDateTime gün etiketi üretilerek düzeltildi. Son testDebugUnitTest ve assembleDebug başarılı, RouteWeatherWindowTest 4/4 geçti. Gerçek bildirim/API veya cihaz DST testi yapılmadı. 10 gün/eksik hava kodunun açık hava varsayımı sonraki düzeltmedir. Push yapılmadı.

## 27 Ekim — Eksik günlük veri ve 10 günlük hazırlık sınırı
- Seyahat snapshot üretimi ayrıldı: istenen kalan günlerin tamamı ve her gün için geçerli hava kodu/sıcaklık/yağış olasılığı/rüzgâr gerekir. Eksik kod 0 (açık hava) yapılmaz; NaN/Infinity, ters sıcaklık, yanlış olasılık, bozuk tarih, yinelenen tarih ve eksik günler reddedilir. Devam eden seyahatte geçmiş günler yerine bugünden itibaren kalan tarihler kullanılır.
- Yetersiz veride yeni AI/konfor analizi üretilmez. Worker analiz hatasında veya kısmi/eski veri durumunda o seyahat için “güncel analiz hazır” bildirimi göndermez. Eski kayıtlar ve geçmiş snapshotlar otomatik değiştirilmez.
- Analiz/geocoding catch bloklarında dış coroutine iptali yeniden yükseltilir; yerel timeout hata olarak işlenebilir.
- Hazırlık uygunluğu ortak tarih kontrolüne taşındı; 10. gün dahil, 11. gün hariç, geçmiş ve ters tarih aralığı uygun değil. Engine, ViewModel ve günlük bildirim yolu bunu kullanıyor. Bu kural sağlayıcıdan istenen gün sayısıyla aynı kavram değildir: mevcut ana hava isteği 10, seyahat isteği 16 gün; bu tur sağlayıcı kapasitesi/ürün ufuk kararı değiştirilmedi.
- Beş regresyon testi eklendi: eksik/bilinmeyen hava kodu, tam/eksik/yinelenen tarih kapsamı, geçersiz sıcaklık ve eksik risk alanları, yalnızca istenen günlerin özeti, 10/11 gün sınırı.
- Açık kapsam: seyahat özetinde baskın hava kodunun nadir kritik riski gölgelemesi, konum/cihaz saat dilimi ayrımı, eski snapshotın ekrandaki tazelik işareti, sağlayıcı/ana ekran/asistan tahmin ufku ve gerçek API/bildirim entegrasyonu.
- Sonuç: testDebugUnitTest ve assembleDebug başarılı; TripForecastSnapshotTest 5/5 geçti. Gerçek API/bildirim veya cihaz hava senaryosu bu tur çalıştırılmadı. Sıradaki seyahat özetindeki nadir kritik hava riskinin çoğunluk koduyla gizlenmesi. Push yapılmadı.

## 27 Ekim — Nadir kritik hava kodlarının korunması
- Seyahat özeti kodları sıklıktan önce risk grubuna göre sıralar; nadir fırtına, donan yağış, kar ve şiddetli sağanak güneşli çoğunluğun arkasında kalmaz. Sis/yağmur/çiseleme de açık hava çoğunluğunda kaybolmaz. Aynı grupta sıklık ve kodla deterministik sıralama yapılır.
- Birden fazla risk etiketi tek conditionSummary içinde korunur; benzer kodların aynı etiketi tekrar etmesi önlenir. Bu etiketler cihazın gündüz/gecesinden bağımsızdır.
- Asistan hava özetinde riskler tüm tonlarda açıkça aktarılır; kar/donan yağış için “harika/elverişli hava” gibi çelişkili olumlu metin engellenir. Kişiselleştirme risk özetini bastırmaz.
- Beş yeni test geçti: tek fırtına günü; eşzamanlı kar/donan yağış/fırtına/sis; açık çoğunluğunda dikkat koşulları; tüm tonlarda kar/buzlanma; sağlayıcı sırasından bağımsızlık.

## 27 Ekim — Tahmin metninde veri dayanağı
- Sıcaklığın yüksek olması artık açık/güneşli veya yüksek UV kanıtı gibi anlatılmıyor. UV sayısı yalnızca geçerli UV verisi varsa aktarılıyor; kişiselleştirme kapalıyken de veri bildirimi korunuyor.
- Geçmiş özetinde analiz kayıt sayısı yağışlı/güneşli gün adedi yapılmıyor (mevcut bilinmiyor işareti -1). Sıcaklık güneşli hava kanıtı sayılmıyor; eksik yağış olasılığı “düşük” diye sunulmuyor.
- İki metin hatası düzeltildi: görünür yemek önerisindeki “and ardından” ve sıcak hava metnindeki “güneşer”; kullanılmayan tercih değişkeni kaldırıldı.
- Açık kapsam: günlük sıcaklık uçları, rüzgâr/UV uçları ve sayısal konfor skorunun riskle birlikte görsel sunumu; konum saat dilimi ve eski snapshot tazeliği. Gerçek API/bildirim/cihaz görsel senaryosu bu tur çalıştırılmadı.
- Sonuç: testDebugUnitTest ve assembleDebug başarılı; TripWeatherCodeSummaryTest 5/5, TravelAdviceEvidenceTest 3/3 geçti. Yeni risk/metin senaryoları JVM testleridir. Sıradaki günlük sıcaklık uçları ve konfor skorunun risk göstergesiyle karışmaması. Push yapılmadı.

## 27 Ekim — Sıcaklık uçları ve konfor/risk sunumu
- Yeni snapshotlar minTemp/maxTemp içinde dönem boyunca görülen en düşük/en yüksek tahmini tutar; günlük min/max orta değerlerinin dönem ortalaması yeni nullable averageTemperature alanına alınır. Konfor hesabı ve analiz ortalaması bu alanı kullanır; eski snapshotlar önceki hesapla okunur. Room JSON dönüştürücüsü yeni alanı destekler; tablo migration'ı gerekmez.
- Eski snapshotlarda min/max ortalanmış olabileceğinden averageTemperature alanı olmayan kaydı gerçek dönem uçları diye etiketlemiyoruz. Yeni kayıtta <=0°C veya >30°C gibi mevcut ürün eşikleri metinde tam aralıkla korunur; ortalama olumluysa bile aralık gizlenmez.
- Kart ve rehber yüksek skordan açık/güneşli/elverişli hava üretmez; snapshot risk metni veya kayıtlı analiz özeti gösterilir. Etiket KONFOR SKORU, değer n/100; risk varsa göstergenin yeşil olmasını engelleyen uyarı rengi kullanılır. Gösterge %200 yazıda büyür, metin sütunu genişlikle sınırlanır.
- Üç JVM testi: tek soğuk/sıcak günün korunması ve önceki konfor ortalaması; eski/yeni JSON uyumu; yüksek skordan uydurma hava sonucu çıkarılmaması. Bir cihaz testi 320 dp/%200 yazıda 96/100 puanla fırtına uyarısının birlikte görünmesini kontrol eder.
- Açık kapsam: gerçek sağlayıcı/Firestore round-trip, tüm tema/cihaz/kontrast/TalkBack matrisi, konum saat dilimi ve eski snapshot tazeliği; konfor skoru güvenlik onayı olarak sunulmaz. Tarih aralığı dışındaki günlerin etkisi önceki snapshot testinde ayrıdır.
- Sonuç: testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. TripTemperatureExtremesTest 3/3; tablette yüksek konfor/fırtına kartı 1/1 geçti. Sonraki yerel inceleme konum saat dilimi ve snapshot tazeliği. Tüm hava doğruluğu matrisi kapanmadı. Push yapılmadı.

## 27 Ekim — Snapshot zamanı ve sağlayıcı saat dilimi
- ForecastSnapshot.generatedAt varsayılanı 0 oldu; timestamp taşımayan eski JSON kayıtları artık okunurken bugünün zamanını almaz. Yeni snapshot üretimi gerçek analiz anını açıkça yazar; sağlayıcının geçerli ZoneId'si nullable timezone alanında korunur.
- API yanıtından sonra kalan seyahat günleri ve 10 gün kontrolü sağlayıcının konum tarihine göre yeniden hesaplanır. Geçersiz saat dilimi yeni snapshot için hata olur; sessiz UTC tahmini yapılmaz. Bu ek kontrol pencere dışındaki bir tarihi hazır analize dönüştürmez.
- Kart ve rehber analiz tarih/saatini açık ZoneId ile gösterir. Önceki konum günündeki analiz, bilinmeyen zaman/saat dilimi ve cihaz saatinin ilerisindeki timestamp ayrı metin taşır. Saat dilimi bilinmiyorsa zaman açık UTC etiketiyle sunulur; yerel gün hakkında hüküm verilmez.
- Bu etiket analiz üretim zamanıdır; sağlayıcının model yayın zamanı/gerçek veri yaşı değildir. Otomatik son kullanım/TTL kararı ve etkisiz açık ekranda zaman etiketinin periyodik yenilenmesi açık.
- Önemli kalan kapsam: Engine API öncesi, ViewModel otomatik analiz ve kart kilidi ilk uygunluk kontrolleri hâlâ cihaz tarihini kullanıyor. Sağlayıcı konumuna göre tam ilk-açılış pencere düzeltmesi tamamlanmadı. Rota kalkış saat dilimi varış hava saat dilimiyle karıştırılmamalı; ayrıca ele alınacak.
- Dört yerel test: Tokyo/Los Angeles aynı an farklı gün; konum tarihine göre kalan gün ve metadata; konum gece yarısında analiz etiketi; eski/bilinmeyen/gelecek timestamp. Gerçek API, cihaz saati veya tüm görsel matris bu tur çalıştırılmadı.
- Sonuç: testDebugUnitTest ve assembleDebug başarılı. TripForecastTimeTest 4/4 geçti. Bu tur cihaz/API testi yapılmadı. Sıradaki API öncesi uygunluk/kart kilidi saat dilimi ve konum değişiminde eski snapshot bağlamının temizlenmesi. Push yapılmadı.

## 27 Ekim — Konum/tarih değişimi ve geç analiz sürümü
- Konum (ad/ilçe/koordinat), dönem veya seyahat türü değişince eski üretilmiş hava/AI metinleri, konfor skoru, snapshot/saat dilimi ve analiz geçmişi temizlenir; kullanıcının notu/puanı, kayıt kimliği ve sahibi korunur. Başlangıç/kalkış saati değişimi günlük varış tahminini korur, rota özetini temizler.
- Aynı temizleme form çıktısında ve savePlan'ın mevcut yerel kayda karşı kontrolünde uygulanır. Form kaydında mevcut analiz işi iptal edilir; kaydetmeden sonra ikinci bir manuel analiz başlatmak yerine Room akışının otomatik analizi kullanılır.
- DAO'ya işlem içinde UID/sürüm/silme işareti kontrolüyle analiz kaydı eklendi. Geç gelen eski analiz düzenlenmiş kaydı geri yazamaz; başarılı kayıt yeni revizyonu döndürür. Worker günlük/rota analizi de bu kontrolü kullanır. Bildirim sonrası takip alanı sürüm koşullu SQL ile güncellenir; tüm eski nesne tekrar yazılmaz.
- Snapshot'ta geçerli konum saat dilimi varsa API öncesi Engine, ViewModel otomatik uygunluk, kart kilidi ve Worker uygunluğu bu konumun gününü kullanır. Saat dilimi henüz bilinmeyen ilk kayıt/legacy akışlarında cihaz günü fallback'i hâlâ açık kapsamdır. Takvim filtreleri/rota kalkış saat dilimi ve canlı bildirim yayımlama yarışı ayrıca doğrulanmalıdır.
- Üç JVM testi: konum/dönem/tür değişiminde temizleme ve kullanıcı içeriği; kalkış/başlangıçta yalnızca rota temizliği; değişmeyen kayıt ve bilinen konum tarihi. Cihaz testi ayrı in-memory SQLite'ta eski analiz, UID, yeni analiz, koşullu bildirim takibi ve silme işareti sınırlarını kapsar.
- Ek kontrol: Worker rota özeti yazısı da sürüm koşuluna taşındı; son revizyon bildirim takip güncellemesine aktarılır. Analiz iş haritasındaki form kaydı kaynaklı iptal Main.immediate üzerinde yapılır. Bu son kaynaklar için derleme/test paketi yeniden doğrulanıyor. Canlı sistem bildirimini yayınlama ile eşzamanlı düzenleme arasındaki uçtan uca yarış tamamen kapandı sayılmaz.
- Sonuç: son kaynaklarla testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı. TripForecastInvalidationTest 3/3; Samsung üzerinde TripAnalysisRevisionTest 1/1 geçti (ayrı in-memory SQLite; ekran/hesap verisi kullanılmadı). Sıradaki bilinmeyen saat diliminde ilk uygunluk ve takvim filtrelerinin konum gününe uyumu. Canlı bildirim/API/Firestore veya tüm UI matrisi test edilmedi. Push yapılmadı.

27 Ekim yeni Git gönderim kapsamı: `7599ac9` sonrasındaki eksik hava verisi, risk/sıcaklık özeti, saat dilimi/zaman etiketi, konum değişimi temizliği ve sürüm koşullu analiz yazıları ile testleri. Son JVM koşusu 183 test başarılı; tarihsel “Push yapılmadı” notları ilgili turun durumudur. Saat dilimi bilinmeyen ilk açılış ve takvim filtreleri açık kalır.

## 30 Ekim 2026 — Asistan köprüsünde başarısız istek ve hak tüketimi

- Köprü hata metnini başarılı cevap olarak döndürmüyor. Zaman aşımı 504, erişim/servis hataları 503, geçersiz JSON veya boş/yanlış türde cevap 502 oluyor. İç servis hata gövdesi kullanıcıya aktarılmıyor.
- Hem normal hem premium ask ucunda BridgeError sonrası sayaç artırılmıyor. Başarılı cevap sayımı korunuyor.
- Otomatik tekrar yalnızca ConnectError için bir kez; okuma zaman aşımı, taşıma hatası ve HTTP hatasında tekrar yok. Bu, kullanıcı eliyle yeniden gönderilen isteklerin tekilleştirildiği anlamına gelmez.
- Yerel doğrulama: backend pytest 77 geçti, 4 atlandı; Ruff ve git diff --check geçti. Dört atlanan test ve bağımlılıkların dört kullanım dışı API uyarısı kapanmış sayılmadı. Gerçek ağ çağrıları test fixture tarafından engelleniyor. İlk genel koşu sandbox geçici klasör izninde durdu; izinli koşu tamamlandı.
- Açık sınır: bu bir platform entegrasyonu değildir. Yeni API anahtarı/sözleşmesi, 428/429/Retry-After eşleme, sunucuda rıza kaydı ve sağlık verisi filtresi, sohbet/istek kimliği, bağlam/ton, platform verisi silme, atomik kota rezervasyonu/iadesi ve İstanbul gün sınırı henüz açık. Mevcut yerel günlük limit yanıtı da hâlâ 200; Android hata modeliyle birlikte düzeltilecek.
- Sıradaki: atomik kota ve istek tekilleştirme tasarımı; ardından yeni platform sözleşmesi/rıza ve Android akışı. Canlı servis, Firebase ve mağaza kontrolleri bu koşuyla doğrulanmadı.

## 30 Ekim 2026 — Paralel asistan kota rezervasyonu

- Sayaç oku/artır yarışının yerine koşullu SQL UPDATE (message_count < limit) ile çağrıdan önce atomik hak ayırma eklendi. Kullanıcı/gün satırının eşzamanlı ilk oluşturulmasındaki unique yarışında mevcut satır okunuyor. Model yanıtı beklenirken yazma kilidi tutulmuyor.
- BridgeError ve beklenmeyen normal istisnada ayrılan hak, rezervasyonun özgün gün satırından iade ediliyor. Başarılı cevap ikinci kez sayılmıyor. Normal/premium uçları aynı yardımcı akışı kullanıyor.
- Yeni yerel SQLite testi: ayrı Session kullanan 8 iş parçacığında 16 istek/3 hak; tam üç rezervasyon kabul edildi, iade edilen son hak yeniden kullanılabildi. Route testi: başarısız ilk istek son hakkı tüketmiyor, dolu kotada köprü çağrılmıyor.
- Tüm backend: 79 geçti, 4 atlandı, 4 bağımlılık uyarısı; Ruff ve diff boşluk kontrolü geçti. Yeni şema değişikliği yok. Bu tur PostgreSQL eşzamanlılık testi yapılmadı; canlı servis çağrılmadı.
- Açık sınırlar: sayaç devam eden istekleri de içeriyor. Süreç zorla kapanırsa ayrılan hak için kalıcı istek bazlı kurtarma henüz yok; iade metodu yalnızca mevcut route'un tek hata yolunda çağrılmalı, tekrar çağrıya dayanıklı bir istek kaydı değildir. Kullanıcı tekrar gönderiminde çift işlem koruması, kalıcı request_id/sonuç kaydı, PostgreSQL paralel doğrulama, UTC/İstanbul gün sınırı ve limit için 429/Android hata modeli açık. Bunlar tamamlanmadan kota/tekilleştirme geniş maddesi kapatılmadı.

## 30 Ekim 2026 — Kalıcı asistan istek kaydı ve rezervasyon kurtarma

- 0014_chatbot_requests migration ve ORM modeli eklendi. Kullanıcı/request_id unique; kullanıcı ve kullanım satırı silinince CASCADE. Mesaj/yanıt/sağlık içeriği bu tabloda saklanmıyor.
- İstek kaydı ile kota artışı aynı transaction içinde. Aynı kullanıcı/UUID tekrarında 409 duplicate_request ile model çağrısı engelleniyor; farklı soruyla aynı UUID de tekrar kabul edilmiyor. Kayıtlı başarılı cevap yeniden sunulmuyor. Önceki istemcilerle uyum için request_id isteğe bağlı; eksikse sunucu yeni UUID üretir.
- pending→complete/failed geçişi koşullu UPDATE ile tek kez yapılır; iade aynı transaction içinde özgün kullanım satırından yapılır. Süresi geçen (300 sn) pending kayıtlar kullanıcının sonraki ask/usage erişiminde kurtarılır. Ayrı Session/servis ile yeniden başlama simülasyonu, iki kez kurtarma/iade ve kurtarılmış kayda geç gelen tamamlamanın reddi test edildi. Gerçek işletim sistemi süreç öldürme testi değildir; otomatik periyodik kurtarma işi henüz yok.
- Normal backend: 82 geçti, 5 PostgreSQL testi URL olmadığı için atlandı. Ayrı izole PostgreSQL16 denetimi: 5 geçti (migration, tekrar upgrade ve paralel kota/aynı request_id yarışı dahil). 16 istek/3 hak, aynı UUID ile 8 paralel istek/tek kabul ve A–B ayrımı doğrulandı. Ruff/diff kontrolü geçti. İlk PG koşusundaki eski 0013 beklentisi 0014 olarak güncellendi; son koşu temiz.
- Yayın önkoşulu: backend sürümü açılmadan alembic upgrade head (0014) çalıştırılmalı. Migration uygulama isteklerinde çalışmaz. Canlıya uygulanmadı.
- Açık sınırlar: Android request_id üretme/saklama/tekrar kullanımını henüz göndermiyor; bu nedenle mobil elle tekrar akışı uçtan uca korunmuş sayılmaz. Platform idempotency sözleşmesi ve yanıt kurtarma yok; dış sağlayıcıda tamamlanıp backend'de kaydedilemeyen cevabın geri getirilmesi çözülmedi. Yeni kimlikle bilinçli tekrar ayrı işlem olur. İstek kayıtlarının saklama/temizlik politikası, UTC/İstanbul günü, hata/rıza sözleşmesi ve canlı entegrasyon açık.
- Sıradaki: Android mesaj bazlı sabit UUID ve doğru yeniden deneme/409 kullanıcı durumu; ardından platform sözleşmesi ve rıza/veri minimizasyonu.

## 30 Ekim 2026 — Android asistan mesaj kimliği ve yeniden deneme

- Yeni kullanıcı mesajının UUID kimliği request_id olarak backend'e gönderiliyor. 401 token yenilemesinde aynı istek gövdesi kullanılıyor; kullanıcı yeniden denemesinde de aynı mesaj ID'si korunuyor. Aynı metinle yazılan yeni mesaj ayrı UUID alıyor.
- Yeniden deneme kimliği mevcut kullanıcı mesajı ve metinle eşleşmeli; eksik/eski kimlik, yanlış metin ve mevcut sohbet/hesapta bulunmayan mesaj gönderilmiyor. Yeniden denemede kullanıcı balonu çoğalmıyor; aynı isteğin önceki hata balonu kaldırılıyor.
- Hata kaydında retryRequestId saklanıyor (JSON/Room sohbet geçmişi modeli). Eski kayıtlar null varsayılanla okunuyor; kimliksiz eski hata için güvenli olmayan yeniden gönderim yapılmıyor. Canlı, kaydedilmemiş sohbetin süreç ölümü sonrası otomatik geri gelmesi bu değişikliğin kapsamında değil.
- Hata balonunda eksik olan Tekrar dene düğmesi eklendi. Salt okunur geçmişte düğme gösterilmiyor. duplicate_request ve request_expired kodları güvenli JSON ayrıştırmayla ayrı durum; terminal açıklama gösteriliyor, tekrar düğmesiyle döngü oluşmuyor. Bilinmeyen/bozuk hata gövdesi genel HTTP hatasına düşüyor.
- Dört yeni JVM testi: aynı UUID ile tekrar/yeni mesaj ayrımı ve istek serileştirme; yanlış/eksik mesaj kimliği; eski/yeni geçmiş uyumu; 409 ayrımı ve bozuk JSON. İlk tamamlanmış koşu 187 JVM testi, 0 hata/atlama ve assembleDebug başarılı. Son geçmiş düğmesi değişikliği için kaynak doğrulaması ayrıca tekrar koşuldu; sonuç sonraki kayıtla teyit edilecek.
- Açık sınırlar: Bu kalıcı sonuç geri getirme değildir. Backend aynı kimlikle alınmış/başarısız/tamamlanmış isteği yeniden çalıştırmaz; sonucu alınamayan istek için açıklama gösterilir, kullanıcı yeni mesaj göndermeyi seçebilir. Platform tarafı idempotency ve sonuç sorgulama hâlâ açık. Canlı uçtan uca ve tablet görsel testi yapılmadı. Mobil sürümden önce backend ve 0014 migration yayına alınmalı; eski backend ek request_id alanını yok sayacağından bu koruma eski sunucuda garanti edilmez.
- Sıradaki: backend günlük limitini 200 cevap yerine 429 durumuna geçirmek, Android kota/hız sınırı/hizmet hatalarını ayırmak; ardından rıza/veri minimizasyonu ve platform sözleşmesi.
30 Ekim Android son kaynak teyidi: salt okunur geçmiş düğmesi değişikliği dahil testDebugUnitTest + assembleDebug tekrar başarılı; XML toplamı 187 test, 0 hata, 0 atlama. Mevcut Gradle depo yapılandırması ve kullanım dışı API uyarıları var; lint/cihaz matrisi bu tur yeniden çalıştırılmadı.

## 30 Ekim 2026 — Günlük kota / geçici hız sınırı / servis hatası ayrımı

- Normal/premium ask ucunda yerel kota dolunca 200 answer yerine 429 daily_limit dönüyor. Kullanım sayısı, reset_at (UTC) ve sıfır kalan hak hata gövdesinde; Retry-After mevcut UTC kota gününün bir sonraki gece yarısına göre hesaplanıyor. Kota kontrolünde model çağrılmıyor.
- Genel IP hız sınırında 429 rate_limited, güvenli Türkçe mesaj ve kalan pencereyi belirten sayısal Retry-After eklendi. Eski detail alanı korundu. Bu sınırlayıcı hâlâ süreç yerelidir; dağıtık/podlar arası kota çözümü değildir.
- Android 429 daily_limit için terminal kota açıklaması; diğer 429 için geçici hız sınırı; 502/503 için hizmet hatası ve 504 için zaman aşımı ayrımı yapıyor. Hata gövdesindeki servis içeriği aynen gösterilmiyor. Günlük kota mesajında cihaz gününe göre yanıltıcı yarın ifadesi yok.
- Sayısal Retry-After 1..86400 saniye aralığında okunur; geçersiz/negatif/aşırı büyük değerler yok sayılır. Hız sınırında bitiş zamanı hata mesajı modeline kaydedilir, düğme bekleme süresince devre dışı ve Lütfen bekleyin yazılıdır; ViewModel de erken yeniden denemeyi reddeder. HTTP-date biçiminde Retry-After desteği ve cihaz saat değişimi matrisi açık.
- Yerel backend 84 geçti, 5 PostgreSQL testi bu koşuda atlandı, 4 bağımlılık uyarısı; Ruff ve diff kontrolü geçti. Normal/premium sıfır kota ve dolu son hak testlerinde model çağrısı yok, answer alanı yok, doğru hata kodu ve bekleme başlığı doğrulandı. Bu tur yeni migration yok, PG denetimi yeniden koşulmadı.
- Android için iki yeni JVM testi kota/hız sınırı ayrımı, hatalı bekleme değerleri ve 502/503/504 eşlemesini kapsar. Derleme/testin son sonucu sonraki teyit kaydında.
- Açık: platform köprüsünün 428/429/error ve Retry-After aktarma sözleşmesi hâlâ uygulanmadı; bu tur yerel backend kotası ve genel hız sınırlayıcıyı kapsar. Canlı servis/tablet görsel/sayaç bekleme cihaz testi yapılmadı. UTC→İstanbul kota geçişi, üretim çok süreçli hız sınırı, rıza/veri minimizasyonu ve platform sonuç geri getirme açık. Sonraki adım rıza kaydı ve sağlık verisi gönderim sınırları; ardından platform bağlantı sözleşmesi.
30 Ekim kota Android son teyit: testDebugUnitTest ve assembleDebug başarılı; XML toplamı 189 test, 0 hata, 0 atlama. Var olan Gradle depo/deprecated API uyarıları sürüyor. Lint ve cihaz görsel testleri bu tur çalıştırılmadı.

## 30 Ekim 2026 — Sunucuda rıza kaydı ve profil sağlık alanı filtresi

- 0015_assistant_consent: kullanıcıya bağlı güncel kayıt ve ayrı audit olay tablosu; hesabın SQL silinmesinde iki tablo da CASCADE. Sunucu zamanı, onay booleans, metin sürümleri ve SHA-256 özetleri tutulur; profil sağlık/sohbet içeriği audit'e yazılmaz.
- Auth gerektiren GET/POST chatbot/consent eklendi; kişisel onay cevabı no-store. Onay metni/sürümü yapılandırılmadığında uzak ask 503 ile kapalı; metin hazır ama güncel kullanıcı onayı yoksa 428. Varsayılan/onaysız kabul yok. Normal/premium ask aynı korumayı kullanır.
- Sağlık onayı ayrı ve isteğe bağlı: sunucu kaydı + sürüm/özet + o isteğin gerçek boolean sağlık bayrağı/sürümü birlikte doğrulanmadan profile.health_sensitivities köprüye gönderilmez. Sürüm numarası veya istemcinin tek başına true göndermesi yeterli değil. Soru/diğer serbest metinlerdeki hassas içerik tespiti bu filtreye dahil değil.
- Metin gösterildikten sonra değişirse eski hash ile kabul 409; önceki onay güncel metne otomatik taşınmaz. Genel geri çekme metin yapılandırması eksikken de yapılabilir. Kota ayrılırken geri çekme olursa son kontrolde model çağrısı engellenir ve hak iade edilir; halihazırda dış servise gitmiş çağrı geri alınamaz.
- İlk 20 hedef test geçti. Son geniş koşuda ortak TestClient IP sayacı önceki testlerden taşındığı için ilgisiz testler 429 aldı; fixture yalnızca test başlangıcında limiter durumunu sıfırlayacak şekilde izole edildi. Üretim hız limiti gevşetilmedi. Son genel backend: 95 geçti, 6 atlandı, 4 bağımlılık uyarısı. İzole PostgreSQL16 denetimi: 6 geçti. Ruff ve diff kontrolü temiz.
- Ayrıntılı sözleşme: docs/ASISTAN_RIZA_SOZLESMESI.md. Yeni backend devreye alınmadan 0015 uygulanmalı; Android rıza ekranı ve onaylanmış yapılandırma henüz yok, eski mobil istemci otomatik onaylı sayılmaz. Bu aşama tek başına yayına hazır entegrasyon değil. Canlı servis/Firebase ve Android/cihaz kontrolleri bu tur çalıştırılmadı; önceki Android 189 test sonucu tarihsel kayıttır.
- Sıradaki: Android rıza metni/isteğe bağlı sağlık kutusu, sunucu kabulü ve 428/409 yeniden onay akışı; Ayarlar'dan geri çekme. Ardından platform canlı metin/anahtar/hesap silme bağlantısı ve hukuki metin doğrulaması. Genel yayın maddesi kapanmadı.

## 30 Ekim 2026 — Android asistan izin ekranı, Ayarlar ve yeniden onay

- Sunucu DTO/GET/POST, gösterilen sürüm+hash ile kabul, ayrı sağlık onayı ve genel geri çekme eklendi. Kabul sonrası GET tekrar okunuyor; yerel bayrak veya başarısız POST onay sayılmıyor. 401 yalnızca bir token yenilemesiyle aynı işlem; 409 yeni metni getirip tekrar açık karar istiyor. Her istek öncesi/sonrası UID/iptal kontrolleri var.
- Asistan ve Ayarlar’da izin yönetimi girişi. Kaydırılabilir metin/aksiyon yüzeyi, ayrı varsayılan kapalı sağlık kutusu, HTTPS bağlantı düğmeleri ve hata/yeniden dene durumları eklendi. Metin metadata yenilendiğinde sağlık onayı eskimişse backend health_review_required işaretiyle yeniden gösteriliyor; sessiz sağlık onayı yok. Metadata arka planda push ile güncellenmiyor.
- Uzak soru onay öncesi gönderilmiyor; bekleyen soru sadece aynı hesap/sohbet doğrulanarak devam ediyor. 428 sonrasında aynı mesaj UUID’siyle yeniden onay; vazgeçme, geri çekme ve hesap/sohbet değişiminde bekleyen gönderim iptal. Yerel selamlama/hava cevapları uzak aktarım iznine bağlanmadı. Genel onayı güncel kullanıcıya kontrol sırasında gereksiz tekrar kabul zorlanmıyor.
- Hesapla bağlı görev kapsamı eski UID sonuçlarını bastırıyor. İşlem zaman aşımı meşgul durumunu temizliyor; istemci onay kaydını DataStore’da varsaymıyor. Sağlık alanının son aktarım kararı sunucuda kalıyor; başka ekrandaki geri çekme eski istemci durumunu yetkili hale getirmez.
- 7 yeni JVM testi: varsayılan ret/isteğe bağlı sağlık, çift tık, A→B geç sonuç, değişen metin, kayıt hatası/bozuk DTO, serileştirme, sağlık yeniden inceleme ve zaman aşımı. Son koşu toplam 196 JVM test, 0 hata/atlama; assembleDebug ve assembleDebugAndroidTest başarılı. Son Ayarlar ayırıcı değişikliği cihaz testi derlemesinde ayrıca derlendi.
- SM-T500 Android12’de yalnızca AssistantConsentDialogTest: 2 geçti. Koyu temada %200 yazı, uzun metin, işaretsiz kutuyu seçme, onay ve Vazgeç erişimi; açık temada sağlık seçmeden kabul ve geri çekme düğmesine erişim. Testler saf diyalog/yerel callback, gerçek rıza kaydı veya asistan servisi çağrısı yapmaz. Gerçek kullanıcı uçtan uca ve tüm telefon/yatay/TalkBack matrisi kapanmadı.
- Backend health_review_required alanı için 11 rıza testi yeniden geçti; Ruff/diff temiz. Önceki genel 95 backend / 6 PostgreSQL bu tur yeniden koşulmadı. Var olan Gradle depo ve deprecated API uyarıları sürüyor. Yeni migration yok; yayın öncesi 0014+0015 ve onaylanmış metin yapılandırması şart.
- Sözleşme belgesi güncellendi. Açık: platform canlı metin/anahtar bağlantısı, gerçek Firebase/backend onay–geri çekme, konuşma kimliği/ton/bağlam, platform veri silme ve sonuç kurtarma. Onay metni bu çalışmada hukuki açıdan onaylanmış sayılmadı. Sonraki yerel iş platform köprüsü sözleşmesi ve anahtar koruması; canlı işler kullanıcı tercihi gereği yapılmıyor. Git gönderimi yapılmadı.

## 30 Ekim 2026 — Platform ask köprüsü, konuşma ve bağlam

- Sunucu SecretStr anahtarı/üretimde zorunluluk, HTTPS/URL kontrolü, redirect kapatma ve hassas validation girdisini gizleme eklendi. Anahtar yalnız Authorization başlığında; istemciye veya JSON bağlamına girmez. Varsayılan köprü okuma 50 sn; Native HTTP/iş süresi 75 sn.
- conversation_id, mode/ton, doğrulanmış sunucu rızası, izinli sağlık bayrakları ve client/app_version iletiliyor. Platform yanıtı UUID’lerle doğrulanıyor; farklı sohbet kimliği, boş/bozuk yanıt veya Agro/Fly güvenlik uyarısı eksikliği reddediliyor. 409 çatışma otomatik yeniden gönderilmiyor. 429/Retry-After korunur; hata metni sabit güvenli mesajdır. Canlı rıza provider’ı henüz yokken upstream 428 eski metinle yeniden onay döngüsü yerine 503 consent_sync_required olur.
- Native mevcut snapshot’tan şehir/hava/tarih/saat dilimi bağlamı kuruyor; yeni ağ sorgusu açmaz. Eski/farklı konum verisi çıkarılır. İlk 10 doğrulanabilir günlük tarih, sınırlı aktif seyahat bilgisi; notlar ve koordinatlar hariç. Eski kullanılmayan bağlam/ton talimatı yardımcıları kaldırıldı. Soru orijinal haliyle, bağlam ayrı gönderilir; 2000/4000 backend–Android farkı 4000’de eşlendi.
- Kişiselleştirme kapalıyken profil/seyahat/sağlık çıkartılır. İlgi alanları UID’ye bağlı Native tercihten alınır; açık boş liste eski backend ilgi alanını geri getirmez. Sağlık alanı ek rıza kontrolünden geçmeye devam eder. İstemcinin context.profile enjekte etmesi reddedilir. Bağlam 8 KB, köprü gövdesi 32 KB sınırı; profil alanları 500 karakter.
- Yeni sohbetin geçmiş kaydına aktif konuşma UUID’si verildi. HistorySaveController eski iptal edilmiş girişin ID’sini yeni açık sohbet kimliğine tercih etmiyor; bunun için regresyon testi eklendi. Eski yerel konuşmaların platform geçmişi otomatik oluşturulmadı. platformMessageId/disclaimer JSON geçmişinde saklanır; uyarı bot balonu altında görünür.
- Son backend koşusu 112 geçti, 6 atlandı, 4 bağımlılık uyarısı; Ruff/diff temiz. Son Android koşusu testDebugUnitTest + assembleDebug başarılı; XML toplamı 203 test, 0 hata/atlama. Yeni testler anahtar/redaksiyon, hata başlıkları, kullanıcı/sohbet/ton/versiyon, opt-out/temizlenen tercihler, UTF-8 boyutu, snapshot sınırları ve geçmiş kimliği/metadata içerir. Bu tur cihaz/PG/lint yeniden koşulmadı. Gerçek anahtar veya canlı servis kullanılmadı.
- Yeni yayın sözleşmesi: docs/ASISTAN_PLATFORM_KOPRUSU.md. Açık ve sıradaki: platform güncel rıza metni adaptörü ve hesap silmeye delete-user/yeniden deneme adımı. Gerçek ortam, provider kişiselleştirme davranışı, profil Firebase–SQL senkronizasyonu, zaman aşımı/kota gün sınırı ve mağaza/release matrisleri açık; bu yerel ask düzeltmesi geniş maddeyi kapatmaz. Git gönderilmedi.

## 30 Ekim 2026 — Platform izin metni senkronizasyonu
Platform GET /consent adaptörü eklendi (ASSISTANT_CONSENT_SOURCE=platform). Varsayılan configured; canlı ayarlar değiştirilmedi. Anahtar yalnızca sunucu Authorization başlığında taşınır. Genel/isteğe bağlı sağlık sürümleri ve metinleri doğrulanır, tam UTF-8 metninden SHA-256 hesaplanır. Onay kaydında önbellek atlanır; aynı sürümde değişen metin eski onayı geçersiz kılar.
Süreç başına 15 saniye önbellek, eşzamanlı çağrı kilidi, 256 KiB yanıt sınırı, yönlendirme yasağı ve kısa ağ zaman aşımı var. Süresi geçmiş metne veya yerel kaynağa sessiz geri dönüş yoktur. Platform 428 önbelleği iptal eder; güvenli 503 sonrası sonraki istek güncel metni yükler. Geri çekme ağdan bağımsızdır.
Yeni 9 yerel test önbellek, hash değişimi, bozuk yanıt ve erişim hatasında eski verinin reddini kapsıyor. Canlı doğrulama, hukuki içerik, çok süreçli anlık invalidasyon açık. Sırada: platform delete-user çağrısını kalıcı hesap silme akışına ekleme.
Doğrulama: platform consent + mevcut consent/chatbot/bridge hedefli paketi 52 geçti; Ruff ve git diff --check geçti. Android kodu değişmedi; cihaz/üretim/PG testi bu adımda çalıştırılmadı.

## 30 Ekim 2026 — Hesap silmede platform asistan verileri
- Kalıcı hesap silme işine authenticated POST /delete-user eklendi. Platforma Firebase UID yerine ask sözleşmesindeki backend UUID gönderilir; kullanıcı isteğiyle başka kimlik verilemez.
- Firebase yazımları bloke edildikten sonra, Storage/Firestore/SQL/Firebase kimliği kaldırılmadan önce platform silme onayı beklenir. Onay yoksa iş pending kalır, SQL ve kimlik korunur. Yeniden başlayan worker aynı iş/kimlikle tekrar dener; tamamlanmış iş yeniden çağrı yapmaz.
- Sadece HTTP 200 ve iki negatif olmayan tamsayı içeren deleted_conversations/deleted_photos makbuzu başarı sayılır. Zaman aşımı, 401/409/429/5xx, yönlendirme ve bozuk makbuzda tamamlandı denmez. Yanıt gövdesi ve transport hatasının hassas ayrıntıları loga taşınmaz. Anahtar yoksa iş bekler; sessizce platform temizliğini atlamaz.
- Hedefli 47 test geçti: servis hata/makbuz denetimleri, yeni SQL oturumunda yeniden deneme, A–B ayrımı, sonraki Firebase adımı hata verince idempotent yeniden silme. Ruff ve diff kontrolü geçti.
- Canlı silme çalıştırılmadı. Üretimde bridge anahtarı/endpoint ve periyodik resume worker hazırlanmalı. Mevcut işler proje/bucket hedefini sabitler; platform URL hedefinin iş bazında sabitlenmesi hâlâ açık. Bekleyen işler varken platform hedefi değiştirilmemeli. Platformun eşzamanlı ask/silme engeli ve idempotent silme sözleşmesi canlıda doğrulanmalı.
Son doğrulama: tüm yerel backend paketi 136 geçti, PostgreSQL bağlantısı gerektiren 6 test atlandı; 4 mevcut deprecation uyarısı var. Bu adım Android/cihaz kodunu değiştirmedi.

## 30 Ekim 2026 — Silme hedefi ve eşzamanlı asistan isteği
- Migration 0016_deletion_target: kalıcı silme işinde backend_user_id ve platform_base_url tutulur; kullanıcı/durum indeksi eklendi. URL veya türetilmiş kullanıcı kimliği uyuşmazsa hiçbir dış servis çağrılmaz. Tamamlanınca bu alanlar da temizlenir; anahtar diske eklenmez.
- Yeni işler platform adresini hazırlandıkları anda sabitler. Bekleyen eski işlerde kullanıcı UUID'si mevcut project/UID'den migration sırasında hesaplanır; eski platform adresi bilinmediğinden NULL bırakılır ve iş durdurulur. Üretime geçmeden önce bu işlerin gerçek eski hedefi yetkili operatörce doğrulanıp iş kaydına yazılmalıdır; ortamın güncel adresi otomatik varsayılmaz. Bu migration canlıya uygulanmadı.
- Normal/premium ortak ask akışında bekleyen silme kontrolü kota öncesinde ve rezervasyon commit'i sonrasında yapılır. İkinci kontrolde silme başlamışsa model çağrılmaz, kota iade edilir. A'nın silme kaydı B'yi engellemez.
- PostgreSQL kullanıcı satırı kilidi, son ask kontrolü ile model yanıtının tamamlanması arasında tutulur. Silme işinin kabulü aynı kilidi alır: önce başlamış gönderim biter, sonra silme kabul edilir; kabulden sonraki gönderimler engellenir. Bunun bedeli aynı kullanıcının eşzamanlı isteklerinin beklemesidir; upstream zaman aşımı mevcut sınırlarla uygulanır. Bu koruma asistan gönderim yoluna yöneliktir, tüm backend yazma uçları için genel dondurma iddiası değildir.
- Doğrulama: ilgili dört test dosyası 41 geçti; son kimlik kontrolünden sonra hesap silme paketi 17 geçti (önceki paketle örtüşür, toplam olarak toplanmamalı). İzole yerel PostgreSQL 16 üzerinde 8 test geçti: gerçek migration zinciri, eski iş backfill'i, eşzamanlı ask/silme kilidi dahil. Ruff ve diff kontrolü temiz. İzole PostgreSQL kapatıldı; mevcut servis/veriye dokunulmadı.
- Sırada: Android'de account_deletion_pending hatasının anlaşılır, yeniden deneme önermeyen sunumu ve hesap silme sırasında diğer backend yazma yollarının incelenmesi. Canlı Firebase/platform davranışı, üretim migration ve periyodik worker kurulumu açık.

## 30 Ekim 2026 — Silme sırasında ortak erişim ve Android hata sunumu
- Android account_deletion_pending kodunu hem doğrudan error hem FastAPI detail.error biçiminde tanır. Asistan bu durumu terminal hata olarak gösterir: hesap silmenin sürdüğünü açıklar, retry kimliği/aksiyonu sunmaz. Derleme geçti; bu adımda tablette görsel test yapılmadı.
- Ortak require_active_account denetimi backend erişim tokenı, Firebase kimlik eşleme, giriş/token üretimi ve yenileme yollarına bağlandı. Bekleyen silmede 409 + no-store döner. Doğrulanmış Firebase tokenı bile bekleyen hesabın SQL kimliğini yeniden oluşturamaz. SQL kullanıcısı artık yoksa normal yazma helper'ları 401 ile durur.
- Profil/bildirim tercihi oluşturma yardımcıları ve abonelik yardımcıları commit sonrası kullanıcı kilidini tekrar alıp silmeyi denetler. Avatar dosyası yazımı bu denetimden sonraya alındı. Yeni konum yazımı da kilitle korunur. Kullanıcıya bağlı diğer profil/cihaz yolları ortak auth kilidi ile işlem başında korunur. Silme continuation endpoint'i auth'a bağlı olmadığı için mevcut secret ile devam edebilir.
- Testler: profil/konum/bildirim/avatar/cihaz/abonelik, login/refresh engeli; diske avatar veya yeni token yazılmaması; A–B ayrımı ve Firebase yeniden oluşturma engeli. Tüm backend paketi 144 geçti / 8 opt-in PostgreSQL testi atlandı. Son küçük kimlik-kontrolü düzenlemesi sonrası ilgili erişim/Firebase/auth paketi ayrıca 9 geçti (örtüşür). Ayrı izole PostgreSQL 16 audit'inde 8 test geçti, ortak denetimin gerçek satır kilidi ve silme sıralaması dahil; geçici sunucu kapatıldı.
- Android JVM 204 test geçti (0 hata/atlama); assembleDebug başarılı. Ruff ve diff kontrolü temiz. Mevcut Python/Gradle deprecation uyarıları sürüyor. Canlı Firebase/platform/hesap silme ve cihaz görsel doğrulaması yapılmadı.
- Açık takip: arka plan bildirim/iş üreticilerinin silme işaretiyle davranışı; Android uygulamasının asistan dışındaki ekranlarında silme durumunun ortak sunumu; üretim migration, eski silme hedeflerinin doğrulanması ve periyodik worker kurulumu. Tüm üretim ve mağaza maddeleri önceki kuyrukta korunur.

## 30 Ekim 2026 — Arka plan bildirimleri ve silme izolasyonu
- Backend PushService kuyruğa alınan Alert/Location kimliklerini veritabanından yeniden okur. Bekleyen silme veya artık bulunmayan kullanıcıda sağlayıcı çağrılmaz. Kritik olmayan, gelecekte başlayan, süresi dolmuş ve silinmiş uyarılar teslim edilmez; kullanıcı bildirim tercihi yeniden okunur. Bildirim başlığı Uyarısı biçiminde düzeltildi.
- Gönderim mevcut require_active_account kullanıcı satır kilidi altında kalır; çağıran işlem transaction'ı kapatmalıdır. Silme kabulünden önce sağlayıcıya gönderilmiş bildirim geri çağrılamaz. FCM'nin kendisindeki teslim zamanı için yerel garanti verilmez.
- Harici backend uyarı ingestion'ı silinmiş konum/bekleyen silme için sağlayıcı çağırmaz, koordinatları kayıtlı konumdan alır. Daha önce gerçek sağlayıcı kurulmadan Demo Wind Notice üretiyordu; artık açık NotImplementedError ile durur. Bu yol için gerçek üretim uyarı sağlayıcısı ve zamanlayıcı hâlâ hazırlanmalıdır; mevcut kodda ingestion için aktif scheduler çağrısı bulunmadı.
- Eski WeatherNotificationWorker legacy hesaba düşmez; oturum yoksa, silme bekliyorsa veya ana bildirim tercihi kapalıysa çıkar. Ağ dönüşü ve kayıt/gösterim aşamalarında UID/silme kontrolü ve coroutine iptal kontrolü eklendi. CancellationException worker ve NotificationRepository.insert içinde artık yutulmaz. Hesap silme yerel temizliği eski WeatherDailyUpdateWork işini de iptal eder; mevcut seyahat worker'ında UID/silme kontrolleri zaten var. Weather worker için güncel kaynakta schedule çağrısı bulunmasa da geçmiş kurulumdan kalan işler korunmalıydı.
- Testler: arka plan/push/alerts/erişim hedefli backend paketi 17 geçti (7 yeni arka plan senaryosu dahil); tüm backend paketi bu adımda yeniden çalıştırılmadı. Android JVM 205 test, sıfır hata/atlama; yeni iptal yayılımı testi geçti. assembleDebug, Ruff ve diff kontrolü başarılı. Cihazda çalışan worker, gerçek FCM ve canlı silme testi yapılmadı; bu yerel kontroller üretim teslim garantisi değildir.
- Sıradaki yerel inceleme: backend tekrar gönderim/idempotency ve eski kuyruk tekrarlarının aynı bildirimi çoğaltması; Android uygulama genelinde silme durumunun sunumu ve bildirim tıklama/hesap değişimi senaryoları. Gerçek sağlayıcı, worker dağıtımı, migration ve tüm mağaza maddeleri açık kuyrukta korunuyor.

## 30 Ekim 2026 — Kalıcı bildirim gönderim hakları
- Migration 0017_push_deliveries ve model eklendi. Alert/kullanıcı/token hash unique kaydı gönderimden önce bağımsız SQL oturumunda commit edilir. Aynı kuyruk yeniden açıldığında, token silinip yeniden eklendiğinde veya paralel süreçlerde aynı hedef ikinci kez sağlayıcıya gönderilmez. Hesap/Alert cascade temizliğine dahil; ham token ve içerik yeni tabloya yazılmaz.
- Claim commit sonrası silme, token sahipliği, tercih ve uyarı geçerliliği yeniden denetlenir. Son gönderim sırasında kullanıcı/token kilidi tutulur. Çağıranın ilgisiz işi commit edilmez; caller/Connection transaction rollback'i kaydı silemez.
- Sınır: reserved/unknown sonuçlar otomatik yeniden denenmez. Claim ile gerçek çağrı arasında çökme bildirimin ulaşmamasına yol açabilir; FCM cevabı kaybolursa teslim bilinmez. Bu, tek uygulama sağlayıcı çağrısı denemesidir; FCM/SDK/cihaz için exactly-once garantisi değildir. Kritik uyarıların güvenilir yeniden denemesi ve farklı Alert ID ile aynı olayın çoğalması için olay kimliği + istemci dedup hâlâ açık.
- İlk ilgili paket 34 geçti. Ek transaction/token/silme yarışı testleri aynı arka plan dosyasına eklendi; sonuçlar aşağıda. Ayrı izole PostgreSQL 16 audit 9 geçti: 16 paralel kuyrukta tek sağlayıcı çağrısı, migration şema uyumu ve cascade dahil. İzole sunucu kapatıldı. Android kodu değişmedi; cihaz/gerçek FCM/üretim migration yapılmadı.
- Ayrıntılı sözleşme: docs/BILDIRIM_GONDERIM_SOZLESMESI.md. Sonraki adım: Android'de bildirim tıklama ve hesap değişimi izolasyonu; backend olay kimliği ve güvenilir yeniden deneme protokolü üretim öncesi açık tutulur.
Son ek test sonucu: arka plan dosyasında 16 test geçti (önceki 34 paketle örtüşür; toplanmaz). Connection-bound caller rollback senaryosu da doğrulandı. Ruff ve git diff --check temiz.

## 30 Ekim 2026 — Android bildirim tıklaması ve hesap izolasyonu
- Yeni seyahat PendingIntent'leri oluşturan UID ve bildirim ID'sini taşır; action kimliği de hesap/kayıt bazında ayrıdır. Böylece extras'ın PendingIntent eşlemesinde dikkate alınmaması nedeniyle farklı hesap bildirimlerinin birbirini güncellemesi önlenir.
- WeatherPremiumActivity bildirim intent'ini otomatik NavHost tüketiminden ayırır; soğuk açılış ve onNewIntent için UID/silme kontrolü yapar, ekran hazır olduğunda tekrar kontrol ederek bir kez işler. Oturum yoksa veya hesap değişmişse istek atılır; sonradan başka hesapla giriş yapıldığında taşınmaz. Auth/kişiselleştirme ekranı üstünde bildirim yönlendirmesi zorlanmaz.
- Navigasyon controller'ı hesap UID'sine göre anahtarlanır. MainApplication auth listener'ı hesap değişimi/çıkışında görünür sistem bildirimlerini temizler; Activity açık olmasına bağlı değildir.
- Bildirim merkezi tıklamasında güncel UID, pending silme ve kaydın güncel kullanıcı listesindeki varlığı yeniden kontrol edilir. Ham deepLinkTarget doğrudan navigate edilmez; NONE fallback yalnızca izinli dört sabit uygulama kökünü kabul eder. TRAVEL_DETAIL kimliği URL-encoded; kontrol karakterli/aşırı uzun ID takvime güvenli dönüş yapar.
- Saf JVM testleri A–B, çıkış, legacy, silme durumu, seyahat ID query enjeksiyonu ve bilinmeyen iç/dış rota hedeflerini kapsar. Gerçek Activity/PendingIntent/Compose yeniden oluşturma, açık/kapalı uygulama, process death, notification tray ve TalkBack testleri bu adımda cihazda yapılmadı. Eski kurulumdan UID taşımayan intent'lerin kaynağı geriye dönük bilinemez; rota Room sorgusu UID'ye bağlı kalır.
- Ek sınır: Firebase tarafından önceden kabul edilmiş notification-payload bildiriminin hesap değiştikten sonra işletim sistemi tarafından gösterilmesi bu yerel tray temizliğiyle kesin engellenmez. FCM data/owner protokolü ve sunucudaki token devri uçtan uca doğrulama gerektirir. Bunlar kapatılmış sayılmaz.
Son doğrulama: son kaynaklarla testDebugUnitTest ve assembleDebug başarılı; XML sonuçları 208 test, 0 failure/error/skipped. Bildirim navigasyonu için 3 yeni JVM testi var. Sistem bildirim URI'lerinde seyahat kimliği encoding de uygulandı. Backend bu adımda değişmedi; yeniden backend/PG testi çalıştırılmadı. Gerçek Activity/PendingIntent, NavHost durum geri yükleme ve eski ViewModel temizlenmesi cihaz matrisi açık.

## 30 Ekim 2026 — Token devri yarışları ve eksik FCM istemcisi
- /devices/push-token kayıt yolu token unique anahtarında atomik upsert kullanır. Aynı token için ilk kayıt yarışı 500'e dönüşmez; satır kimliği tekrar kayıtta sabittir ve sahiplik authenticated current_user üzerinden belirlenir.
- DELETE artık eski ORM okumasına göre nesne silmez; kullanıcı/token koşulunu doğrudan SQL DELETE üzerinde değerlendirir. A'nın gecikmiş çıkışı, B'ye devredilmiş tokenı silemez. Gönderimdeki token satır kilidiyle kayıt devri de aynı satır üzerinde sıralanır.
- 22 hedefli backend testi geçti. İzole PostgreSQL 16 audit 10 geçti: 16 paralel A/B token upsert tek satır/ID üretir; A'nın eski ORM cache'iyle unregister B'nin kaydını korur. Geçici sunucu kapatıldı. Ruff/diff temiz; Android/üretim/gerçek FCM testi yapılmadı; şema değişmedi.
- Önemli tespit: aktif Kotlin modülünde firebase-messaging, FCM service ve backend token-kayıt çağrısı yok. WorkManager yerel bildirimleri bu açığı kapatmaz. Backend notification payload'ı eski hesaba ait gecikmiş içeriği OS üzerinden gösterebilir. Bu nedenle FCM hesap izolasyonu ve uçtan uca teslim yayın engeli olarak açık kaldı.
- Eski A oturumunun geç kalmış REGISTER isteğinin B kaydını geri alması salt upsert ile çözülmüş değildir. İstemci session-generation/kayıt sıralama ve server capability/data-only owner/event protokolü gerekir. Hazırlanan docs/FCM_HESAP_IZOLASYONU.md uygulanacak tasarımı, test matrisini ve sınırları ayırır; tamamlanmış entegrasyon değildir.
- Sıradaki: Kotlin FCM alıcı/token-kayıt entegrasyonunu ve hesap/olay kimliği taşıyan data mesaj sözleşmesini yerel testlerle uygulamak. Gerçek bildirim gönderimi mevcut yalnızca-yerel-test sınırının dışında kalır; staging ve ayrıca yetkilendirme gerektirir.

## 30 Ekim 2026 — Kotlin FCM alıcısı, ilk aşama
- firebase-messaging bağımlılığı ve exported=false service eklendi. Otomatik FCM başlatma manifestte kapalı; gerçek token alma/kaydetme veya mesaj gönderimi bu adımda yapılmaz.
- Protokol-1 data zarfı mevcut Firebase oturumunun backend UUIDv5 kimliğiyle karşılaştırılır. Süre, UUID, 4 KiB boyut, metin sınırları ve tek izinli weather hedefi doğrulanır. Eski/başka hesap, legacy/çıkış ve pending silme reddedilir.
- Hesap epoch'u A–B–A geçişini tanır. Room INSERT IGNORE ve hesap/olay primary key'i tekrar gösterimi engeller. Kayıt sırasında hesap değişirse eski kayıt kaldırılır. Gösterimde ana tercih, Android izni, süre ve oturum tekrar kontrol edilir; PendingIntent UID/ID ve sistem notification tag'i hesap/olay bazlıdır.
- Callback yalnızca yerel kısa işler yapar (3 saniye); token/backend ağ çağrısı veya detached coroutine yoktur. Yeni saf JVM testleri zarf/kimlik/süre/bozuk veri, hesap değişimi, tekrar olay ve coroutine iptalini kapsar. Gerçek Room concurrency/Service/cihaz testi açık.
- FCM_HESAP_IZOLASYONU.md güncellendi. Token kayıt kuyruğu, sunucunun data-only/capability geçişi, eski notification-payload engeli ve gerçek teslim henüz tamamlanmadı; özellik üretimde hazır sayılmaz. Sırada oturum sürümüne bağlı kalıcı token kayıt kuyruğu ve backend protokol eşleştirmesi var.
Doğrulama: testDebugUnitTest + assembleDebug başarılı; XML'de 216 test / 0 failure/error/skipped. 8 yeni FCM zarf/persist testinin tamamı geçti. Birleşik manifestte exported=false ve firebase_messaging_auto_init_enabled=false doğrulandı. Gerçek FCM/Room cihaz concurrency ve token kayıt entegrasyonu açık; bu sonuç üretim etkinleştirmesi değildir.

## 30 Ekim 2026 — Kalıcı token kuyruğu ve owner-bound FCM sunucu sözleşmesi
- Yerel sürümlü token journal + AtomicFile/noBackup adaptörü eklendi. onNewToken/auth/hesap silme kuyruğa bağlandı; yalnızca yerel disk işlemi yapar. Token dönüşü, hesap değişimi, eski acknowledgement ve offline hata testleri eklendi; token logları redakte. Gerçek HTTP gateway ve WorkManager kaydı hâlâ bağlı değil.
- Backend provider notification fallback yerine protokol-1 owner/event/expiry data mesajı üretir. UTF-16/emoji sınırları, boyut ve kimlik doğrulaması, kritik öncelik ve TTL var. FCM_OWNER_PROTOCOL_ENABLED=false varsayılanı SDK başlatma/gönderimi kapatır; kapalıyken dispatch claim tüketilmez. Üretim ayarı değiştirilmedi.
- Ek Firebase hatası düzeltildi: named identity app default messaging app yerine kabul edilmiyor; default init yarışı kontrol ediliyor. Credential/token ayrıntıları hata loglarına taşınmıyor.
- Backend full suite 168 geçti / 10 opt-in PostgreSQL atlandı; son TTL assertion'larıyla provider paketi ayrıca 9 geçti (örtüşür). Ruff temiz. Yeni SQL şeması/migration eklenmedi, PG bu adımda tekrar çalıştırılmadı.
- Açık yayın engeli: server token kayıt revision/CAS tombstone ve capability sözleşmesi yok; eski REGISTER yarışı hâlâ açık. Legacy endpoint'e yeni kuyruk bağlanmadı. Sıradaki adım bu server sözleşmesi, ardından gerçek gateway/WorkManager retry ve cihaz matrisi. Auto-init/FCM rollout bu işler doğrulanmadan açılmamalı.

## 30 Ekim 2026 — Backend token revision ve capability koruması
- Önceki kuyruğun son AtomicFile doğrulaması da testDebugUnitTest/assembleDebug derlemesinden geçti. 222 JVM testi başarılı; Android'de bu son server adımı için yeni kaynak değişmedi.
- 0018_push_token_bindings migration, user CASCADE'li token-hash/kurulum/revision/bound takibi ve PushToken owner_protocol default 0 eklendi. Eski cihazlar protokol-1 desteği var sayılmaz; gönderimden önce capability kontrol edilir.
- POST/DELETE yeni metadata üçlüsünü strict doğrular. Aynı kurulum daha yüksek revision kabul eder; aynı sürüm yalnızca aynı kullanıcı/durum için idempotenttir. Eski REGISTER, başka kullanıcının geç UNREGISTER'ı, farklı kurulum ve legacy downgrade 409 döner. SQL conditional upsert + PostgreSQL transaction advisory lock birlikte kullanılır.
- DELETE son sürümü unbound tombstone olarak korur; ilk POST'tan önce logout gelmesi de korunur. Account deletion CASCADE binding metadata'sını temizler. DML legacy guard ayrıca kayıt/silme anında değerlendirilir.
- İlgili 26 yerel test ilk pakette geçti; son offline-unbind senaryosu full pakete dahil edildi. İzole PostgreSQL 16 audit 12 geçti: 16 paralel revision'da en yüksek revision/owner korunması, tombstone replay engeli, legacy migration capability 0 ve önceki migration/silme/gönderim yarışları dahil. Geçici sunucu kapatıldı. Son full backend: 173 geçti / 12 PostgreSQL testi normal pakette atlandı; aynı 12 test ayrı izole PostgreSQL audit ile geçti. Ruff ve diff kontrolü temiz.
- docs/FCM_HESAP_IZOLASYONU.md sözleşme güncellendi. Gerçek gateway/WorkManager, makbuz doğrulama, offline logout/FCM token iptali, farklı token rotasyonu temizliği ve cihaz/reinstall matrisi açık. Native auto-init ve server FCM flag kapalı; canlı migration/POST/FCM yok.

## 30 Ekim 2026 — Kullanıcı bildirimi: asistan izin ekranında tekrar hata

Öncelik: P1, uzak asistanın yayın öncesi doğrulamasına eklendi. Token/FCM işleri bu maddeyi kapatmaz.

Kullanıcı adımları: asistan ekranında sorun görülüyor; Tekrar dene aynı ilk duruma dönüyor; Uzak asistan izinlerini geri çek farklı bir ikinci duruma götürüyor. Screenshot_20261030-145617.jpg ve Screenshot_20261030-145625.jpg belirtilen Desktop yollarında bulunamadı; görseller ve hata yazıları doğrulanamadı. İkinci durumun başarı mı hata mı olduğu varsayılmadı.

Kod incelemesi: AssistantConsentController.load tüm yükleme hatalarını aynı bağlantı mesajına çeviriyor. Repository 401 için bir kez token yeniliyor, 409'u stale sayıyor; diğer HTTP hatalarını ayırmıyor. Bu nedenle ağ kesintisi, yayınlanmamış endpoint (404), sunucu/izin metni yapılandırması (503) ve yetkilendirme sorunu kullanıcı açısından aynı görünebilir. Canlı neden doğrulanmadı; prod servise istek gönderilmedi. İzin metni alınmadan onay/uzak gönderimin engellenmesi korunmalıdır; hatayı geçmek için sahte metin/onay oluşturulmamalı.

Sırası geldiğinde test/düzeltme:
- Görseldeki gerçek hata metni ve cihaz zamanı ile adımları eşleştir; yeni görsel veya yazılı hata gerekir. Mevcut dosyalar görülemedi.
- Yerel gateway testleri: 401+refresh, 403, 404, 409, 429, 503, timeout/offline ve tekrar deneme. busy durumu çözülmeli; başarısız istek sonsuz döngü yaratmamalı.
- Hata sunumunu güvenli fakat ayırt edici yap: servis/metin hazır değilken kullanıcıyı sürekli internetini kontrol etmeye yönlendirme. Ham sunucu/credential ayrıntılarını gösterme.
- İzin geri çekme: başarılı POST sonrası açık sonuç; başarısız POST'ta başarı gösterilmemesi; sunucuda metin eksik olsa bile geri çekmenin çalışması; tekrar açılışta server state false; A–B ve çift tıklama testleri.
- Android/service sürüm eşleşmesi ve GET/POST /v1/chatbot/consent sözleşmesi. Canlı ortam kontrolü mevcut yalnızca-yerel-test sınırının dışında; ayrıca yetkilendirilmiş test ortamında yapılmalı.

Durum: bildirildi ve kodda hata ayrımının eksikliği doğrulandı; görsel/gerçek neden ve düzeltme tamamlanmadı. Bu adımda uygulama kodu değiştirilmedi.

### Aynı bildirimin görselleri doğrulandı — 30 Ekim 14:56
Önceki dosya-yolu erişim sorunu sonrası görseller sohbet içinde görüldü. İlk görsel: Asistan izin metni alınamadı. Bağlantınızı kontrol edip tekrar deneyin. İkinci görsel: İzin geri çekilemedi. Lütfen tekrar deneyin. İkinci ekran başarı değildir; sunucuda izin geri çekildiği doğrulanmamıştır. İki işlem de hata veriyor; gerçek HTTP/network nedeni hâlâ bilinmiyor ve prod çağrısı yapılmadı.
Ek doğrulanmış UI sorunu: kısa hata içeriği için tabletin neredeyse tamamını kaplayan boş pencere. Kaynakta AssistantConsentDialog Surface fillMaxHeight(0.92f) kullanıyor. Düzeltme sırasına içerik kadar büyüyen fakat uzun metin/büyük yazı/yatay kullanımda ekran sınırını aşmayan, kaydırılabilir diyalog eklendi; uzun metin erişilebilirliği korunmalı.
Ek akış kontrolü: aynı dialogdaki Tekrar dene callback'i izin ekranını yeniden yüklemeye bağlı; geri çekme hatası sonrası kullanıcının geri çekmeyi yeniden denediğini sanmaması için işlem bazlı retry ve açıklayıcı düğme/test ayrımı ele alınmalı. Yalnızca metin yüklemesini yeniden denemek sunucudaki onayı geri çekmiş sayılmaz.
Durum: P1 mevcut asistan izin problemi görsel kanıtla güncellendi. Hata ayrımı, yükleme/revoke retry ve tablet dialog düzeni tek takip maddesi altında; yeni mükerrer problem sayısı oluşturulmadı. Bu adımda kod değiştirilmedi veya test çalıştırılmadı; tanı ve sıra kaydı güncellendi.

## 30 Ekim 2026 — P1 asistan izin ekranı: istemci düzeltmeleri

Kullanıcı görsellerindeki iki hata ve boş tablet penceresi için:
- HTTP 401/403/404/405/429/5xx, timeout, ağ ve geçersiz yanıt güvenli hata türlerine ayrıldı. Servis kullanılamıyorsa her durumda internet bağlantısı suçlanmıyor; ham response/credential ayrıntısı gösterilmiyor. 401 en fazla bir token yenilemesiyle denenir.
- Tekrar dene artık işlem bazlı: metin yükleme hatasında metni yükler, geri çekme hatasında geri çekmeyi tekrarlar. İkincisi açıkça İzni geri çekmeyi tekrar dene diye adlandırılır. Ekranı yeniden açmak bekleyen geri çekmeyi otomatik POST etmez. Save stale-text akışında yeni metin için yeniden açık karar gerekir.
- Geri çekme başarısızlığında eski accepted=true yerel olarak geri yüklenmez. UID bazlı, diske commit edilen pending withdrawal flag'i, yeni controller/uygulama açılışında önceki onayın otomatik kullanılmasını engeller. Başka hesabı etkilemez; HTTP'den önce yazılır; sadece doğrulanmış false/false POST yanıtı sonrası temizlenir. Disk hatasında POST yapılmaz. Hesap silme yerel temizliğine bu UID bayrağı dahil edildi.
- AiAssistantRepository ilk gönderim ve 401 retry öncesinde yerel pending flag'i denetler. Asistan ekranında geri çekme, o ViewModel'in mevcut isteğini ve bekleyen prompt'u da iptal eder. Daha önce sunucuya iletilmiş verinin bu istemci kontrolüyle geri alınması garanti edilmez.
- Başarılı geri çekme açık mesaj ve Kapat gösterir; başarısız işlemde başarı mesajı yoktur. Bekleyen durumda Kapat yalnızca ekranı kapatır, gizlilik niyetini iptal etmez. Başarı ekranında doğrudan yeniden Onayla düğmesi yerine İzinleri yeniden yönet vardır.
- Dialog zorunlu %92 yükseklik yerine içeriğe göre ölçülür; maksimum yükseklik/kaydırma korunur. Büyük yazı/uzun metinde kontroller erişilebilir kalır.

Doğrulama: 227 JVM testi ilk koşuda geçti. SM-T500 Android 12 üzerinde yalnızca sahte verili AssistantConsentDialogTest 4/4 geçti: kısa revoke hata penceresi geometri/düğmesi, açık başarı sonucu, koyu/büyük yazı ve açık/sağlık opsiyonel akış. Gerçek izin API'si, üretim ViewModel'i veya hesap izni bu fixture'larda çağrılmaz. Son reopen/no-auto-POST kontrolü dahil final birim/APK derlemesi başarılı; XML sonuçları 227 test, 0 failure/error/skipped.

Kapanmayan P1 kısmı: cihazdaki gerçek servis hatasının HTTP/network nedeni henüz bilinmiyor. Yeni canlı istek gönderilmeden eski log buffer'ında yalnızca consent URL'si arandı, kayıt bulunmadı. Backend endpoint sürümü/auth/metin ayarları yetkilendirilmiş test ortamında doğrulanmalı; bu arayüz düzeltmesiyle asistan servisinin canlıda çalıştığı iddia edilmez. P1'in istemci kısmı düzeltiliyor, uçtan uca yayın doğrulaması açık. FCM gateway/WorkManager kuyruğu önceki sırasıyla korunur.
Son paket durumu: final 227 JVM testi başarılı, debug APK derlendi ve adb install -r Success ile SM-T500'e güncellendi. Uygulama verisini temizleyen komut kullanılmadı; gerçek izin yükleme/geri çekme isteği bu adımda başlatılmadı. Tablet UI fixture sonuçları 4/4; backend kaynakları bu P1 adımında değiştirilmedi.

## 30 Ekim — platform v3 / yapılandırılmış hava ve tekrar koruması

Kesin weather_data şeması uygulandı. Yeni migration 0019_chatbot_replay; aynı kimlikle başarılı yanıtı yeniden kullanma, kayıp yanıtı aynı platform gövdesiyle sorgulama, İstanbul gününe göre atomik kota ve yeni hata durumları eklendi. Android debug derlemesi ve 236 JVM testi başarılı. İzole PostgreSQL 13/13; hedefli backend 77/77. Önceki sağlık gizliliği ek kontrolleri tablette 8/8 geçti. Gerçek platform/model çağrısı yapılmadı.

Ayrıntılı sözleşme, kişisel veri içerebilen 24 saatlik tekrar kaydı ve fiziksel temizleme görevi, doğrulamalar ve canlıya geçiş engelleri: [ASISTAN_PLATFORM_V3.md](ASISTAN_PLATFORM_V3.md). Canlı anahtar/dağıtım, gerçek izin endpoint sorunu ve uçtan uca A–B testi açık. Firebase/FCM ve mağaza hazırlıkları önceki sırada korunuyor. Bu değişiklikler henüz Git'e gönderilmedi.
Son tam doğrulama: backend 188 geçti / 13 PostgreSQL testi standart koşuda atlandı; ayrı PostgreSQL koşusu 13/13 başarılı. Ruff başarılı; 6 bağımlılık deprecation uyarısı kaldı.

30 Ekim ek adım: asistan yollarının başarı/hata/kimlik ve doğrulama cevaplarına merkezi no-store koruması eklendi. Yeni platform hata kodları, hata sonrası kota iadesi ve request_in_progress sonrası aynı kimlikle başarılı yeniden deneme doğrulandı. İlgili backend regresyon paketi 65/65; Ruff ve diff kontrolü başarılı. Ayrıntı ASISTAN_PLATFORM_V3.md dosyasında. Canlı platform izin/sohbet doğrulaması, anahtarın sunucuya tanımlanması, migration ve düzenli tekrar kaydı temizleme görevi açık; Git'e push yapılmadı.

30 Ekim — izin sürümü / tekrar kaydı: yeni metin kabulünden sonra eski sürümlü gönderim gövdesinin veya önbellek cevabının kullanılabilmesi engellendi. Asistan ve sağlık için çözümlenen izin sürümleri tekrar parmak izine dahil. Tamamlanan ve zaman aşımında kalan istekler için 2 regresyon senaryosu eklendi; ilgili backend paketi 33/33 başarılı, Ruff/diff kontrolü temiz. Eski kimlik yerine yeni mesaj gerekir; yeni kimlik güncel sürümle doğrulandı. Canlı çağrı ve push yapılmadı. Ayrıntı ASISTAN_PLATFORM_V3.md içinde.

30 Ekim — bozuk tekrar kayıtları: eksik gönderim gövdesinin yeniden oluşturulması kaldırıldı; yanlış kullanıcı/kimlik/soru/bağlam ve eksik-geçersiz kayıtlı yanıt güvenli request_expired sonucu verir. Ek model çağrısı veya kota tüketimi yok. 8 yeni senaryo dahil ilgili paket 41/41 geçti, Ruff/diff kontrolü temiz. Ayrıntı ASISTAN_PLATFORM_V3.md. Canlı bağlantı ve dağıtım açık; Git'e push yapılmadı.

30 Ekim push öncesi son tam backend paketi: 203 geçti, opt-in PostgreSQL 13 normal koşuda atlandı (önceki ayrı PostgreSQL koşusu 13/13 başarılı). Altı bağımlılık deprecation uyarısı mevcut. Bu doğrulama canlı bağlantı onayı değildir.

30 Ekim — temizlik devamı: tam süre sınırında tekrar kaydı temizliğinin <= kuralı düzeltildi. Başarılı/yarım kalan kayıtların süre öncesi/sınırı/sonrası ve owner filtresi altı senaryoyla doğrulandı; kimlik işareti korunur. İlgili paket 47/47 ve Ruff başarılı. Önceki birikmiş değişiklikler 796a02c ile origin/main'e gönderildi. Düzenli sunucu temizleme görevi ve canlı bağlantı testi açık.

30 Ekim — hava payload boyutu: JSON biçimlendirme boşluklarının geçerli snapshot'ı reddetmesi düzeltildi. Ölçüm boşluksuz UTF-8 JSON ile; 4095/4096/4097 bayt ve null/bilinmeyen alan testleri eklendi. Yeni 4 test dahil ilgili paket 40/40 geçti; Ruff başarılı. Ayrıntı ASISTAN_PLATFORM_V3.md. Son push a3ff1a2; bu yeni düzeltme henüz push edilmedi. Canlı platform doğrulaması açık.

30 Ekim — hava zaman doğrulaması: +03:99 gibi geçersiz saat farkının Python tarafından sessizce başka farka dönüştürülmesi engellendi. Geçerli negatif/çeyrek saat ve Z farkları korunur. 10 yeni senaryo dahil ilgili paket 50/50 geçti; Ruff/diff kontrolü temiz. Başlatma komutlarında değişiklik yok. Son push a3ff1a2; hava boyutu ve saat farkı düzeltmeleri yerelde, canlı bağlantı açık.

30 Ekim — güvenli doğrulama cevabı: NaN/Infinity girdisinin 422 oluşturulurken sunucu hatası üretmesi ve hatalı alan değerlerinin yanıtta yansıtılması 4 başarısız testle doğrulandı, asistan yollarına özel handler ile giderildi. Alan yolu/türü ve sabit mesaj korunur, input/ctx çıkarılır, no-store vardır. Platform çağrısı ve kota tüketimi yok. 4 yeni test dahil ilgili paket 65/65, Ruff/diff temiz. Ayrıntı ASISTAN_PLATFORM_V3.md. Son yerel değişiklikler push edilmedi; canlı doğrulama açık.

30 Ekim — köprü hata gizliliği: ask transport/timeout/yanıt doğrulama exception chain'i standart traceback'e özel servis ayrıntılarını taşımayacak şekilde bastırıldı. 5 yeni senaryo dahil 39/39 köprü testi geçti; kodlar, timeout sayacı ve tekrar sayıları korundu. Ruff/diff kontrolü temiz. Sentry/altyapı canlı saklama ayarları açık; detay ASISTAN_PLATFORM_V3.md. Son yerel düzeltmeler push edilmedi.

30 Ekim — Sentry hata raporu: istek gövdesi/varsayılan PII/yerel değişken toplama kapatıldı; hata raporlarına allowlist filtresi eklendi. Özel request/user/context/log/exception değerleri yerine hata türü ve stack konumu kalır. 5 yeni test dahil ilgili paket 44/44; gerçek kurulu SDK envelope testi bellek içi alıcı ile geçti, ağ çağrısı yok. Ruff/diff temiz. Canlı Sentry/proxy saklama ve diğer telemetri kanalları açık; ayrıntı ASISTAN_PLATFORM_V3.md. Son değişiklikler henüz push edilmedi.

30 Ekim — metrik performansı: rastgele HTTP method etiketlerinin sınırsız zaman serisi üretmesi OTHER etiketiyle engellendi. Beklenmeyen 500 hataları artık gecikmesiyle bir kez ölçülüyor; hata gizlenmiyor. Mevcut rota şablonu/404 fallback koruması test edildi. 3 yeni test dahil ilgili metrik/Sentry paketi 10/10, Ruff başarılı. Canlı erişim logları ve dağıtım açık; son değişiklikler henüz push edilmedi.

30 Ekim — takip/500 logu: X-Request-ID tek ve standart UUID olarak doğrulanıyor, güvensiz değer yeni UUID ile değişiyor. 500 yanıtında kimlik başlığı ve no-store korunuyor. Genel hata logu ham mesaj/zincir/URL yerine tür, takip kimliği, rota şablonu ve kod konumu tutuyor. 7 yeni test dahil ilgili paket 15/15; ardından tüm backend 247 geçti / 13 opt-in PostgreSQL atlandı / 6 bağımlılık uyarısı. Ruff/diff temiz. Son PG audit bu adımda tekrarlanmadı; canlı altyapı logları, FCM gateway/WorkManager ve canlı asistan doğrulaması açık. Son yerel değişiklikler push edilmedi.

30 Ekim — FCM gönderim adaptörü: Retrofit kayıt sözleşmesi ve kullanıcı/token/kurulum/revision/protokol makbuz doğrulaması eklendi. HTTP öncesi/sonrası oturum kontrolü ve cancellation korunur. Gateway henüz WorkManager/kuyruk üretim akışına bağlanmadı; 401 refresh/409 conflict/429 backoff ve güvenilir backend kimlik bağlantısı sonraki adım. 4 yeni test dahil Android 240 JVM testi başarılı, debug APK derlendi. Gerçek token/FCM çağrısı yok; auto-init/server flag kapalı. Detay FCM_HESAP_IZOLASYONU.md. Son değişiklikler push edilmedi.

30 Ekim — FCM WorkManager: kapalı build flag arkasında bağlantı koşullu tekil iş zinciri eklendi; auth/token olayları planlama ve logout/silme iptaline bağlı. 401 tek force-refresh aynı snapshot, 409/kalıcı hata ack olmadan denemeyi bitirme, 429 kalıcı not-before, ağ/5xx exponential backoff. Yeni hesap için eklenmiş işin eski işin failure sonucu ile iptal edilmemesi gözetildi. 6 yeni test dahil Android 246/246 JVM, debug APK başarılı; üretilmiş flag false. Cihaz Worker/process death, ilk token bootstrap, server unbind ve gerçek staging FCM açık. Detay FCM_HESAP_IZOLASYONU.md; son değişiklikler push edilmedi.
