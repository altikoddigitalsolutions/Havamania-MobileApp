# Havamania — Yayına hazırlık durum raporu

Güncelleme: 22 Ekim 2026. Hedef: önce Android / Google Play.

Canlı ekran bulguları, düzeltmeler ve açık varyantlar: [Canlı görsel denetim](CANLI_GORSEL_DENETIM.md).

## Son geniş denetim

[Kapsamlı kalite raporu](KAPSAMLI_KALITE_DENETIMI_2026-10-22.md): 20 kategori, 5 kodla doğrulanmış bug, 1 UI riski ve 1 iyileştirme. Son koşu: 125 JVM + 16 cihaz + 64 backend + 4 PostgreSQL + 4 Firebase kural testi başarılı; 187 lint uyarısı açık. Ayrıntılı sonuçlar bu dosyanın sonunda.

## Kısa durum

Ana kontrol listesinde **59 geniş madde: 22 tamamlandı, 37 açık**. Canlı görsel ekran denetimi durum varyantlarıyla genişletildi; tüm kod tabanının erişilebilirlik/tekrar denetimi ayrı açık madde eklendi. Bu sayı hata adedi değildir: tek bir açık madde çok sayıda cihaz/servis senaryosu içerir. Aşağıdaki alt düzeltmeler geniş matrisi tamamen kapatmadığı için sayı otomatik artırılmaz. Uygulama henüz yayına hazır olarak onaylanmadı.

Çalışma kapsamı: yerel testler. Canlı hesap silme, üretim dağıtımı ve mağazaya gönderim yapılmadı. Son bildirim ve canlı görsel denetim düzeltmeleri bu tur Git gönderimine dahil edildi.

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

## Doğrulama durumu

- Önceki tamamlanmış Android koşusu: **111 birim testi**, debug derleme ve lint başarılı; **0 lint hatası, 194 uyarı, 1 ipucu**.
- Önceki son Samsung SM-T500 / Android 12 koşusu: **12 cihaz testi geçti**. 2 SQLite migration, 2 Firebase model, 8 izole Compose testi. Bu tur tablet testi çalıştırılmadı.
- Önceki backend koşusu: **64 test**, Ruff temiz. Ayrı PostgreSQL koşusu **4 test**, Firebase kural emülatörü **4 test**. Bu tur yeniden çalıştırılmadılar.
- Önceki cihaz testlerindeki geçici Activity başlatma hataları ve test temasındaki ekran boyutu eksikliği ayrıntılı denetim günlüğünde kayıtlı; başarılı tekrarlar ilk hataları silmez.
- Kullanıcının 14:56:30–32 boş tablet ekranı görüntüleri izole auth testiyle eşleşiyor. Bu, normal uygulamanın açılışının ayrıca doğrulandığı anlamına gelmez. Yeni tablet testi öncesi haber verilecek.

## Sırada ne var?

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
