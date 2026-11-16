# 16 Kasım — Kayıtlı konumların güvenli silinmesi

## Bulgu ve düzeltme

Görünür liste aynı kaydı tekilleştirirken silme koşulu ham satır sayısını kullanıyordu. Aynı konumdan iki satır kaldığında son görünen konum yanlışlıkla silinebilir durumdaydı.

canDeleteRegisteredCity ortak kuralı hem şehir yönetimi ekranına hem CitySyncStore.recordCityChange içindeki DataStore işlemine bağlandı. Son benzersiz konum, varsayılan kayıt ve listede olmayan/uyuşmayan aday silinmez. Ekran düğmesinin kapalı olması tek başına güvenlik sayılmaz; disk işlemi kuralı tekrar uygular.

Eski id=0 kayıtlarında farklı konumların aynı kuyruk kimliğini taşıması ayrıca risklidir: mergeCityChanges kimlik bazlı çalıştığından başka bir konumun değişikliği bile bu kayıtları tekleştirebilirdi. Böyle bir listede yerel ekleme/silme işlemi reddedilir, veriler ve kuyruk korunur; arayüz nedenini açıklar. Kimlikleri otomatik değiştirme veya kayıp kayıtları tahmin ederek birleştirme yapılmadı.

## Test kapsamı

Dört yeni JVM testi: mükerrer son kayıt, varsayılan/olmayan aday, geçerli diğer konum, çakışan eski kuyruk kimlikleri.

Yeni izole cihaz testi gerçek PreferenceDataStore dosyasına mükerrer kayıt ve çakışan kimlikler yazar. Silme/ekleme reddedildiğinde kayıtların ve kuyruğun değişmediğini sınar. Şehir yönetimine son görünen kaydın silme düğmesinin kapalı olduğunu sınayan Compose testi eklendi. Önceki atomik çoklu işlem ve ülke satırı testleri de aynı koşuya dahil.

Canlı hesap, Firebase verisi ve Play paketi com.havamania.mobile değiştirilmez; testler com.havamania QA paketinde ayrı dosyalarda çalışır. Kullanıcının mevcut gradle.properties değişikliği korunur.

## Derleme ortamı

İlk koşu eksik NotificationDao_Impl.java KSP çıktısı nedeniyle derlenemedi. Ardından clean, Windows tarafından kilitli tutulan R.jar dosyasını silemedi. Kaynak veya kullanıcı ayarı silinmeden, app buildDirectory geçici QA init betiğiyle ayrı Temp klasörüne yönlendirildi. Sonuç aşağıya kaydedilecek.

## Açık kalanlar

Eski çakışan kimliklerin kalıcı onarımı ve uzak kayıtlarla doğrulanmış geçiş planı açık. Varsayılan konum senkronizasyonu, sunucudan liste birleştirme ve diğer mutasyon yolları aynı onarım incelemesine dahil edilmeli. Gerçek hesapta silme/eşitleme ve önceki profil/uyarı ayarı ekranı kabulü tamamlandı sayılmaz. Bu tur release veya push yapılmadı.
## Sonuç

Yalıtılmış derlemede testDebugUnitTest ve connectedDebugAndroidTest başarılı: 579 JVM testi, 0 hata; SM-T500 / Android 12 üzerinde 7/7 cihaz testi, 0 atlanan. Cihaz koşusu: yeni izole DataStore silme testi (1), mevcut atomik çoklu işlem testi (1), ülke satırı/şehir yönetimi Compose testleri (5).

Gerçek kullanıcının hesabı silinmedi, Firebase veya backend verisi değiştirilmedi. Çakışan eski kimliklere bu tur otomatik onarım uygulanmadı; güvenli ret ve kullanıcıya açıklama sağlandı. Sonraki adım doğrulanmış eski kayıt onarımı ile diğer tercih yazma/ayar ekranı kabulüdür.

## Devam — Güvenli eski kayıt onarımı

Şehir listesi gözlemi başlarken DataStore işlemi içinde yalnız bütün alanları aynı olan tekrarlar temizlenir. Sıra korunur; varsayılan konum, eşitleme kuyruğu ve başka UID'nin verileri değiştirilmez. İkinci onarım yeni değişiklik üretmez. İsteğe bağlı bakım yazısı başarısızsa okunabilir kayıtların gözlemi devam eder; iptal yutulmaz.

Pozitif servis kimliklerinde de ad/ülke/koordinatı farklı kayıtlar aynı kimliği taşıyorsa çakışma tespit edilir. Bu durumda kayıtlar otomatik seçilip atılmaz. Ekran ham listeyi bu açıdan kontrol eder; tekilleştirme çakışmayı gizleyip silmeyi etkinleştirmez. Yazı engeli veya bozuk JSON varsa onarım veriyi değiştirmez.

Sonuç: 583 JVM testi, 0 hata; SM-T500 üzerinde 9/9 cihaz testi. Dört yeni onarım birim testi ve iki yeni DataStore testi eklendi. Mevcut silme, atomik işlem ve şehir yönetimi testleri de geçti. Yeni cihaz testinde onarım sonrası aynı dosya kapatılıp yeniden açıldı; temizlenmiş liste kalıcıydı. Varsayılan, kuyruk ve diğer UID anahtarları korundu. Gerçek hesap/Firebase yerine ayrı test dosyaları kullanıldı.

Yalıtılmış geçici buildDirectory ile testDebugUnitTest ve connectedDebugAndroidTest başarılı. Kullanıcının gradle.properties dosyası ve Play kurulumu korundu; yeni release/push yapılmadı.

Açık kalanlar: aynı kimlikli farklı konumların yetkili servis kayıtlarıyla doğrulanmış onarımı, uzak liste birleştirme ve varsayılan eşitleme yolları; bozuk kayıt için kullanıcıya özel kurtarma akışı; önceki gerçek oturum ve ayar ekranı kabulü. Bu tur otomatik kimlik yeniden ataması veya gerçek bulut verisi düzeltmesi yapılmadı.

## Devam — Uzak liste birleştirmesinde kimlik çakışması

mergeCityChanges artık aynı kimlikteki farklı sunucu kayıtlarını associateBy ile sessizce tek kayda indirmez. Yerel liste, gelen liste veya kuyruk kendi içinde çakışıyorsa DataStore birleştirmesi hata ile geri döner. Bekleyen işlem aynı kimlikli uzak kaydı hedeflediğinde koordinat/ülke kimliği de doğrulanır; pozitif kimlikte yalnız ad güncellemesi normal akışta korunur. Sıfır/negatif kimliklerde ortak kayıt eşleşmesi gerekir.

Kayıt ve kuyruk budama aynı edit işlemindedir; çakışmada ikisi de değiştirilmez. Gelen çelişkiyi otomatik onarmak veya eski yerel çelişkiyi yeni snapshot ile silmek yerine kayıtlar korunur. Var olan hata penceresi kimlik çakışması için özel açıklama kullanır.

Son tur: 587 JVM testi, 0 hata; SM-T500 üzerinde 7/7 izole cihaz testi. Dört yeni saf doğrulama testi ve iki yeni DataStore testi eklendi. Yeni cihaz testleri gelen/yerel çakışmada tüm preference haritasının korunduğunu, diğer UID'nin etkilenmediğini ve yanlış koordinata ait snapshot'ın bekleyen silmeyi doğrulayamadığını sınar. Önceki yeniden açılış, atomik işlem, onarım ve silme regresyonları da geçti. testDebugUnitTest ve connectedDebugAndroidTest başarılı.

Bu kontrol yerel snapshot birleştirmesini korur; uzak isteği gönderen dispatch/preflight yolunun kimlik doğrulaması ayrıca açıktır ve sıradaki önceliktir. Gerçek Firebase/backend mutasyonu yapılmadı; canlı silme güvenliği tamamlandı sayılmaz. Varsayılan konumun yalnız isimle eşleştirilmesi de sonraki incelemeye dahildir. Kullanıcı gradle.properties değişikliği korundu; release/push yapılmadı.

## Devam — Uzak konum gönderiminde transaction kontrolü

ThemeViewModel şehir dokümanına doğrudan set/delete göndermek yerine Firestore transaction içinde hedef belgeyi okur. Aynı şehir kimliği ve koordinat/ülke kontrolü yazı/silme hazırlanırken uygulanır. Hedef yoksa ekleme oluşturulabilir; zaten silinmiş belgeyi yeniden silmek gerekmez. Birden fazla kuyruk revizyonu aynı şehir kimliğini taşıyorsa ilkini seçerek gönderme yapılmaz.

Transaction callback'i her denemede okur ve captured coroutine Job, UID ve hesap silme engelini tekrar kontrol eder. Firestore transaction'larının eşzamanlı değişimde yeniden çalışabilmesi [resmî Firebase belgesinde](https://firebase.google.com/docs/firestore/manage-data/transactions) açıklanır. Okuma ve yazı aynı transaction'dadır; callback UI durumunu değiştirmez.

SDK tarafından sarılan kimlik hatasının cause zinciri korunuyorsa özel açıklama ve UNKNOWN gönderim engeli kullanılır. Dispatcher bu hatada işlemi onaylamaz. Zaman aşımı belirsizliğindeki mevcut kalıcı rezervasyon koruması sürer. İptalden önce commit aşamasına girmiş bir uzak işlemin geri alındığı iddia edilmez.

Sonuç: 594 JVM testi, 0 hata; SM-T500 üzerinde 14/14 cihaz testi. Yedi yeni yerel test; yanlış hedef, geçerli yazı/eksik silme, okuma sırasında oturum kapısı değişimi, SDK retry simülasyonu, mükerrer kuyruk, sarılmış hata ve dispatcher onayı kontrollerini kapsar. Bir yeni hata açıklaması büyük yazı cihaz testi eklendi; önceki 13 regresyonla koştu. testDebugUnitTest ve connectedDebugAndroidTest başarılı.

Bu tur Firestore transaction adaptörü derlendi, ancak gerçek Firestore veya emülatör transaction çalıştırılmadı. Retry/kimlik aşamaları üretim yardımcılarıyla kontrollü JVM senaryolarıdır. Gerçek SDK hata sarmalama, güvenlik kuralları, ağ kesintisi ve transaction commit davranışı ayrı kabul gerektirir. Canlı hesap/Firebase verisi değiştirilmedi; release/push yapılmadı.

Sırada varsayılan şehrin yalnız ada göre eşleştirilmesi ve ilgili kuyruğun doğrulanması var. Gerçek transaction/emülatör ve önceki mağaza/üretim kabul maddeleri açık.

## Devam — Varsayılan şehirde ad belirsizliği

ThemeViewModel'in profil gözlemcisi ve gönderim sonrası onay yolu artık ilk ad eşleşmesini seçmez. İsim normalize edilerek tek kayıt aranır; tamamen aynı tekrarlar sayılmaz. Aynı adlı farklı kayıtlar varsa yerel tercih korunur ve özel belirsizlik açıklaması gösterilir. Çözümlenen kayıt ile sunucudan gelen adın uyuşması DataStore işleminde de denetlenir.

Onaylanmış varsayılan tercih koruması, yalnız adı aynı diye kaldırılmaz; yerel listeden tekil çözümlenen kaydın kimliği/koordinatı bekleyen tercih ile de eşleşmelidir. Eski revizyon onayı yeni tercihi değiştirmez. Kimliği çözülemeyen, kayıtlı şehirler dışında seçilmiş bir konumun acknowledged koruması tutulabilir; bu yeni gönderim döngüsü oluşturmaz, dispatcher zaten acknowledged işlemi atlar.

Sonuç: 598 JVM testi, 0 hata; SM-T500 üzerinde 13/13 cihaz testi. Dört yeni birim testi, iki yeni izole DataStore testi ve bir büyük yazı açıklama testi eklendi. Aynı adla farklı konum, yanlış onay, eski revizyon, diğer UID'nin korunması ve uyuşmayan çözümde disk haritasının değişmemesi doğrulandı. Önceki varsayılan yeniden açılış ve geç sonuç regresyonları da geçti. testDebugUnitTest ve connectedDebugAndroidTest başarılı.

Önemli sınır: serverDefaultCity şu anda yalnız uid/name taşır. Yerel listeden kimlik çözülmesi, sunucunun o kimliği gerçekten taşıdığı anlamına gelmez. Platformlar arasında aynı adlı fakat listede bulunmayan yerleri kesin ayırmak için varsayılan şehir kimliği/konumu içeren uyumlu bir senkronizasyon sözleşmesi gerekir. Bu tur Firestore şeması, kurallar veya canlı profil verisi değiştirilmedi. Belirsiz onayların korunması ve yeniden deneme/yönlendirme deneyimi bu sözleşmeyle ayrıca ele alınacak.

Sıradaki öncelik: varsayılan konum için isim dışındaki kimlik bilgisinin geriye uyumlu taşınmasını hazırlamak ve yerel/emülatör doğrulamasını planlamak. Önceki gerçek Firestore transaction ve üretim/mağaza kabul maddeleri açık.


## Devam — Varsayılan konum kimliği sözleşmesi

Mevcut defaultCity adı korunurken aynı profil yazısında defaultCityLocation haritası da gönderiliyor. Harita version=1, id (Firestore Long), name, latitude, longitude, country, country_code, admin1/admin2/admin3 alanlarını taşır. country_code ve yönetim alanları null olabilir. Koordinatlar sonlu ve enlem -90..90, boylam -180..180 olmalıdır. İsim boş olamaz. Profil merge yazısı diğer alanları korumayı amaçlar; gerçek SDK/emülatör kabulü henüz yapılmadı.

UserProfileRepository yalnız sunucudan onaylanmış görüntüde bu haritayı yayınlar. Yeni harita varsa isimden ilk kayıt seçilmez; haritanın adı, sürümü, koordinatları ve kayıtlı listede aynı kimliğe sahip konumlarla tutarlılığı doğrulanır. Geçersiz harita ad eşleşmesine düşmez; işlem korunarak hata verilir. Kayıtlı listede bulunmayan manuel konum da haritayla çözümlenebilir. Harita yoksa önceki belirsizlik kontrollü ad çözümü kullanılır.

Doğrulama: testDebugUnitTest + assembleDebug başarılı. 604 JVM testi, 0 hata/atlama; altı yeni sözleşme testi. SM-T500 Android 12 üzerinde DefaultCityIdentityDeviceTest ve DefaultCitySyncStoreDeviceTest toplam 4/4 geçti. Yeni izole DataStore testi manuel konumun kimlikli onayını, bekleyen işaretin kalkmasını ve B hesabının değişmemesini doğruladı. Kullanıcının canlı Firebase verisine dokunulmadı; release/push yapılmadı.

Açık sınırlar: Firestore kuralları dosyasında aktif sahibin profil alanlarını yazmasına izin var; kurallar bu yeni alanın biçimini ayrıca doğrulamıyor. Emülatör denemesi firebase CLI/dependency kurulumu olmadığı için başlamadı; geçmedi sayılmalı. Gerçek SDK serileştirme, profil merge, A–B izolasyonu ve account_deletions işareti altında yeni alanın reddi ayrıca emülatörde doğrulanacak. Yeni alanın cihazda kodlanıp çözülmesi canlı sunucu kanıtı değildir.

Eski istemci yalnız defaultCity yazarsa önceki companion haritası kalabilir. İsim değişmişse yeni okuyucu çelişkiyi reddeder. Aynı adlı farklı bir yere eski istemciden geçilmesi yalnız adla tespit edilemez; eski istemciler için aynı adlı geçişte tam garanti yok. Legacy adı tek başına çözen geriye uyumlu yol da sunucunun kimlik kanıtı sayılmaz. Tüm yazarların kimlikli sözleşmeye geçmesi veya sunucuda uyumluluk/temizleme politikası gereklidir. Otomatik tahminle harita silinmez.

Sırada: yerel Firebase test bağımlılıklarını hazırlayıp yeni alanın emülatör kabul testlerini çalıştırmak; ardından eski istemci uyumluluğunu ve hata ekranındaki yeniden deneme/yönlendirme davranışını tamamlamak. Önceki canlı senkronizasyon, gerçek transaction ve mağaza kabul kapıları açık.


## Devam — Firebase emülatör kabulü

Yerel test bağımlılıkları package-lock üzerinden npm ci ile kuruldu; kilit dosyası ve üretim bağımlılıkları değiştirilmedi. Firebase CLI ile yalnız demo-havamania-audit projesinin localhost Firestore (8086) / Storage (9196) emülatörleri çalıştırıldı. Test koruması bu iki adres dışında çalışmayı reddeder. Canlı Firebase'e bağlanılmadı; kurallar dağıtılmadı.

npm test: 9/9 geçti, 0 hata/atlama. Dört yeni test yeni varsayılan konum haritasının JS Firestore SDK ile yazılıp okunmasını, merge ile displayName/interests alanlarının korunmasını, A–B/misafir erişiminin reddini, silme işaretiyle mevcut profilde yazının ve olmayan profilin yeniden oluşturulmasının reddini kapsıyor. Önceki rehber, profil/altkoleksiyon ve Storage testleri de geçti. PERMISSION_DENIED günlükleri beklenen negatif senaryolardır. Emülatörler başarı sonrası durdu.

Sonuç: önceki turdaki 'Firebase CLI eksik; emülatör başlamadı' engeli bu tur aşıldı. Bu kanıt JS SDK + yerel kurallar içindir; Android SDK üzerinden emülatöre bağlı uçtan uca senkronizasyon, Android transaction yarışları ve canlıya dağıtılmış kuralların kabulü yerine geçmez. Önceki 604 JVM / 4 cihaz sonucu bu tur yeniden çalıştırılmadı; Android kaynakları bu tur değiştirilmedi.

Yeni uyumluluk testi, eski istemcinin yalnız defaultCity alanını merge etmesinin önceki defaultCityLocation haritasını koruduğunu gerçekten doğruladı. Bu test sorunun giderildiği anlamına gelmez. Farklı ad çelişkisini Android reddediyor; aynı adlı farklı konum geçişi eski istemciden kimlik olmadan ayırt edilemez. Tüm yazarların kimlikli sözleşmeye geçmesi veya sunucu tarafında uyumluluk politikası açık. Kurallar yeni haritanın şeklini henüz ayrıca doğrulamıyor.

Sıradaki öncelikler: eski istemci/kimlik uyumluluğu ve varsayılan konum hata ekranında doğru kurtarma yönlendirmesi; sonra Android SDK ile gerçek emülatör senkronizasyon/transaction testleri. Test araçlarının npm engine/deprecation uyarıları ayrı araç bakımı maddesi olarak izlenecek; bu tur sürüm yükseltilmedi. Canlı servisler, gerçek cihaz uçtan uca kabulü ve Play yayın kapıları açık. Push/release yapılmadı.


## Devam — Konum eşitleme hatasında güvenli kurtarma

Hava ve Şehirleri Yönet ekranları artık üretimde aynı yeniden deneme politikasını kullanıyor. Kimlik çakışması, varsayılan ad belirsizliği, bekleme penceresi, oturum/sunucu reddi veya tanınmayan hata için kör yeniden gönderim düğmesi gösterilmiyor. Geçici bağlantı/zaman aşımı hatalarında düğme korunuyor. ViewModel eylemi de politika kapısıyla korunuyor; yalnız arayüzde gizlemekle yetinilmiyor. Belirsiz varsayılan ad açıklaması yerel tercihin korunduğunu ve doğru konumun yeniden varsayılan seçilmesi gerektiğini söylüyor; otomatik navigasyon eklenmedi.

Ek düzeltme: transport yeniden denemesi önceden tüm blockedReason değerlerini serbest bırakabiliyordu. Artık manuel bağlantı denemesi ilgisiz kimlik/ret bloklarını açmıyor. Yeniden giriş yolu yalnız AUTHENTICATION bloklarını açabiliyor. Hata kapanırken ve hesap değişirken yeniden deneme izni temizleniyor. Kimlik sorunu seçimle düzelmiyorsa destek/doğrulama gerekli; uygulama otomatik kayıt silmiyor veya kimlik tahmin etmiyor.

Doğrulama: son kaynakla testDebugUnitTest, assembleDebug ve assembleDebugAndroidTest başarılı; 608 JVM testi, 0 hata. Dört yeni politika testi geçici hata, sarılmış kimlik/ad hatası, bekleme/korumalı hata ve kullanıcı açıklamasını kapsıyor. İlk cihaz koşusunda tablet kilitliydi: 10 UI testi arayüze erişemedi, 3 izole veri testi geçti. Kullanıcı kilidi açtıktan sonra son APK ile aynı 13 test tekrar çalıştı ve 13/13 geçti. Bunlar CityMutationErrorUiTest (10 açık/koyu, 200% yazı ve eylem testi) ve DefaultCityIdentityDeviceTest (3 izole DataStore testi). İki yeni UI testi üretim politika yardımcısını kullanarak yanlış retry eyleminin pencere açıkken bulunmadığını kontrol ediyor. Gerçek Firebase başarısız isteği veya tam Activity/ViewModel ağ akışı bu fixture testlerinde çalıştırılmadı.

Sırada: Android SDK ile yerel Firebase emülatör senkronizasyon/transaction kabulü; eski istemcinin yalnız adı değiştirip eski kimlik haritasını bırakması için uyumluluk politikası; yeni harita biçiminin sunucu kurallarında doğrulanması. Önceki 9 Firebase emülatör testi bu tur tekrar koşulmadı, kurallar değişmedi. Canlı servis/mağaza kapıları açık; push/release yapılmadı.
