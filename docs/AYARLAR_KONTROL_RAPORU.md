# Android ayarlar kontrolü — 2 Kasım 2026

## Önbellek düğmesindeki veri kaybı riski

Önceki bağlantı `SettingsScreen → TravelViewModel.clearAllPlans` idi. Bu yöntem hava önbelleğiyle birlikte oturum sahibinin yerel seyahatlerini siliyor ve listeyi boşaltıyordu. Ayrıca işlem bitmeden başarı mesajı gösteriliyordu.

Yeni bağlantı `SettingsCacheViewModel → WeatherRepository.clearAllCachedWeather → WeatherDao.clearAllWeatherCache`. Yalnızca weather_cache tablosu temizlenir. Seyahat, AI geçmişi, upload/deletion kuyrukları veya hesap tercihleri bu işleme dahil değildir. Görünür hava verisi bir sonraki yenilemeye kadar korunur; düğme hemen ağdan yenileme yapıldığı izlenimi vermez.

İşlem sürerken düğme devre dışıdır; Mutex aynı anda ikinci temizlemeyi engeller. Başarı ancak depolama işlemi tamamlanınca gösterilir. Hata sabit mesaj ve tekrar deneme sunar; iptal başarıya çevrilmez. ViewModel durumu ekran yeniden oluşturulmasında korunur. Artık çağrılmayan toplu silme yöntemi kaldırıldı.

Ayarlar parametrelerinden kullanılmayan TravelViewModel, AiHistoryViewModel, NotificationViewModel ve ProfileViewModel varsayılanları kaldırıldı. Böylece bu ekran için gereksiz kurulum yolları açılmaz. Bu adımda açılış süresi/FPS benchmark'ı yapılmadı.

Doğrulama: 267 JVM testi (3 yeni), SM-T500 Android 12'de ayrı in-memory Room ve saf arayüz paketi 3/3 geçti. İki sahte kullanıcının seyahat/AI geçmişi ve bekleyen kuyrukları korunurken hava kayıtları silindi. Açık/koyu tema ve 2x yazı ile çalışma, hata/tekrar ve başarı durumları test edildi. Gerçek hesap/veritabanı temizliği yapılmadı; daha önce silinmiş kayıtlar için kurtarma yapılmadı.

## Gerçekte uygulanmayan dil seçimi

Native ayarlarda English seçeneği tercih kaydediyordu; arayüz çevirisi veya uygulama locale değişimi uygulanmıyordu. Asistan bağlantısı da Türkçe kullanıyor. Bu nedenle uygulama İngilizceye geçmiş gibi gösteriliyordu.

AppLanguageSupport yalnızca tamamlanmış native dil desteğini seçilebilir yapar. Şu anda Türkçe kullanılabilir; English açıkça 'Henüz kullanılamıyor' olarak devre dışıdır. Satır ve RadioButton tek erişilebilir seçim hedefidir; desteklenmeyen seçime dokunmak tercih yazmaz. Dil penceresi büyük yazıda kaydırılabilir.

Önceden EN/bozuk dil tercihi kaydedilmişse getter gerçek desteklenen dile (TR) döner; eski kayıt topluca değiştirilmez. Geçerli TR değerleri normalize edilir; yeni desteklenmeyen tercihler kaydedilmez. Bu çalışma tam İngilizce çeviri eklemez; RN/iOS tarafının dil sistemi değişmedi.

Son test sonuçları ve açık işler yayın durum raporunda kaydedilir.

## Açık işler

- Tam native çok dillilik: kaynak metinleri, dil değişimi ve asistan dil sözleşmesi birlikte tamamlanmalı.
- FCM authRequired durumunu ayarlarda sunma ve güvenli yeniden giriş/kurtarma arayüzü.
- Android 13+ gerçek izin reddi/kalıcı ret ve eski/OEM cihazlarda sistem ayarına geçiş matrisi; kanal bazlı bildirim durumu.
- Telefon/yatay/klavye ve tüm ayarlar ekranı için geniş görsel/erişilebilirlik matrisi.
- Canlı servisler ve mağaza hazırlıkları genel yayın sırasında korunur.

Son kaynak değişiklikleri henüz GitHub'a gönderilmedi.

## Son doğrulama

Dil düzeltmesi sonrasında 269 Android JVM testi geçti (0 failure/error). Debug ve androidTest APK'ları derlendi. SM-T500 / Android 12'de SettingsLanguageUiTest + WeatherCacheClearUiTest seçili paketi 4/4 geçti: eski EN seçimi, desteklenmeyen seçime dokunmada sıfır callback, desteklenen seçimin tek callback'i, açık/koyu 2x yazı, cache çalışma/hata/başarı durumları. Önceki ayrı Room veri koruma testi de başarılıdır; son UI koşusunda Room testi yeniden çalıştırılmadı.

Kullanılan arayüzler saf fixture'lardır; gerçek kullanıcı tercihleri veya seyahatleri üzerinde işlem yapılmadı. Diff kontrolü temiz; mevcut SDK deprecation uyarıları sürüyor. İngilizce yerelleştirme tamamlanmış değildir; yalnızca yanıltıcı seçim engellendi. Son değişiklikler henüz push edilmedi.

## Bildirim tercihi ve Android izni — 2 Kasım

Aktif PermissionHelper Android 13 öncesinde her zaman true dönüyordu. Artık AndroidX NotificationManagerCompat uygulama seviyesindeki sistem izni de kontrol edilir; API 33+ için runtime POST_NOTIFICATIONS ayrıca gereklidir. Aktif FCM ve hava/seyahat bildirim üreticileri ortak kontrolü kullanır. Çağrısı olmayan mükerrer PermissionManager nesnesi ve istek composable'ları kaldırıldı; bunun aktif bir tekrar-izin-dialogu hatasını düzelttiği iddia edilmez.

Ayarlar switch'i kullanıcının uygulama tercihini korur; sistem izni kapalıysa ayrı açıklama gösterilir. Tercih kapalıyken otomatik izin isteği veya ayarlar açılışı yapılmaz. Tercih açıkken kullanıcı Android ayarlarını kendisi açabilir; API 26+ bildirim ayarları, desteklenmediğinde uygulama ayrıntıları sayfası kullanılır. ON_RESUME'da durum yeniden okunur; observer ekran terk edilince kaldırılır. Kanal bazındaki kapatmalar uygulama seviyesi izinle eş tutulmaz; sistem ayarları bağlantısı bu kanalları yönetmek için de erişilebilirdir.

Doğrulama: 2 yeni izin politikası testi dahil 271 Android JVM geçti. SM-T500 Android 12 üzerinde NotificationSettingsUiTest 4/4 geçti: tercihle sistem ret durumunu ayırma, yalnızca kullanıcı dokunuşunda ayar açma callback'i, tercihi koruyarak uyarıyı güncelleme, gerçek LifecycleRegistry ON_RESUME ile yeniden okuma. Açık/koyu ve 2x yazı kullanıldı. Uygulama/test APK'ları derlendi, diff kontrolü temiz.

Testlerde izin değerleri ve ayar açma callback'i sahtedir. Gerçek OS izni değiştirilmedi veya sistem ayarları Activity'si başlatılmadı. Android 13+ fiziksel izin reddi, OEM/kanal bazlı davranış ve uçtan uca gerçek FCM hâlâ açık. Kaynak değişiklikleri henüz push edilmedi.

## Temel tercih okuma/yazma dayanıklılığı — 2 Kasım

ThemeViewModel içindeki 20 tercih akışında okuma hataları kullanıcıya güvenli bir yükleme hatası olarak iletilir. 14 temel ayar yazımı ortak, oturum sahibini yakalayan yardımcıdan geçer. Disk hatasında başarı yayınlanmaz; iptal edilen işlem iptal olarak kalır. Hesap değişiminde geç gelen başarı/hata yeni hesabın arayüzüne uygulanmaz. Eski hesap için başarısız yazı otomatik yeniden oynatılmaz. Yükleme hatasında kullanıcı açıkça yeniden yükleyebilir; kaydetme hatasında ayarı yeniden seçmesi istenir.

Doğrulama: 6 yeni test dahil 277 Android JVM testi geçti; debug ve androidTest APK'ları derlendi. SM-T500 / Android 12 üzerinde SettingsIssueUiTest 2/2 geçti (0 failure/error): açık/koyu tema, 2x yazı, yükleme için tek yeniden yükleme callback'i ve kaydetme hatasında eski yazının tekrarlanmaması. Cihaz arayüzleri izole fixture'lardır; gerçek kullanıcı tercihleri veya Firebase verisi değiştirilmedi. Diff kontrolünde boşluk hatası yok.

Kapsam sınırı: şehir/profil/bulut yazımları ve toplu sıfırlama bu ortak yazma yoluna henüz taşınmadı. Yeni ThemeViewModel kurulurken paylaşılan hava durumunun temizlenmesinin görsel geçişlere etkisi ayrıca incelenecek; mevcut bulgu kanıtlanmış ekran titremesi nedeni sayılmıyor. FCM yeniden giriş/kurtarma UI ve canlı servis doğrulamaları açık. Son değişiklikler henüz push edilmedi.


## Şehir değişikliklerinin oturum ve hata kontrolü — 2 Kasım

ThemeViewModel içindeki ekleme/silme/varsayılan seçme işlemleri aynı ViewModel örneğinde Mutex ile sıraya alındı. Yerel liste okunurken başka bir kullanıcı işleminin aynı eski listeyi okuyarak eklemeyi ezmesi önlenir. Her aşamada yakalanan UID ve coroutine iptali kontrol edilir; yerel disk işlemi ardından oturum değişmişse sonraki Firestore isteği başlatılmaz. Yerel sonuçlar da aynı oturum kontrolüyle yayınlanır. Zaten gönderilmiş uzak istek geri alınmış sayılmaz.

Şehir yönetimi ve hava ekranında konum işlem hatası görünür hale getirildi. Önceki ekleme/silme hata SharedFlow'u bu ekranlarda toplanmıyordu; varsayılan konum hatası yalnızca debug loguna yazılıyordu. Mesaj, yerel kaydın gerçekleşmiş ancak bulut eşitlemesinin tamamlanmamış olabileceğini açıklar; otomatik yazma/tekrar gönderme yapmaz. Varsayılan şehri silme engeli ViewModel'de de uygulanır; arayüzle aynı kuraldır.

Açık sınırlar: kalıcı şehir gönderme/silme kuyruğu ve yeniden deneme, Firestore snapshot'larının bekleyen yerel değişikliklerle uzlaştırılması, farklı ViewModel/cihaz örnekleri arasındaki eşzamanlılık, profil gözlemcileri ve toplu sıfırlama akışları ayrıca ele alınmalı. Bu Mutex tek başına uçtan uca senkronizasyon çözümü değildir. Gerçek Firebase hesabı veya servis üzerinde test yapılmadı. Son değişiklikler push edilmedi.

Doğrulama sonucu: 5 yeni şehir işlem testi dahil 282 Android JVM testi geçti (0 failure/error). Testler art arda liste düzenlemesini, yerel hata sonrası uzak isteğin durmasını, uzak hata sonrası yerel kaydın korunmasını, hesap değişiminde uzak yazının engellenmesini ve A→B→A iptalini kapsıyor. SM-T500 Android 12'de CityMutationErrorUiTest 2/2 geçti: açık/koyu tema, 2x yazı ve kapatma düğmesi. Bunlar izole arayüz fixture'larıdır; gerçek ViewModel/Firebase eşitlemesi doğrulanmış sayılmaz. Debug ve test APK'ları derlendi; git diff --check boşluk hatası bildirmedi.


## Firestore şehir dinleyicisinin eski oturuma ait sonuçları — 2 Kasım

Aynı UID'ye yeniden girişte yalnız UID kontrolü yeterli değildi: kaldırılan dinleyicinin callback'i yeni AccountTaskScope içinde iş başlatabiliyordu. Her dinleyici kurulmasına monoton bir sürüm eklendi; kaldırma/yeniden kurma ve ViewModel kapanması eski sürümü geçersiz kılar. Callback girişi, disk kaydı öncesi ve görünür durum yayını öncesi UID/sürüm/iptal kontrol edilir. Disk işlemi sürerken oturum değişirse eski sonuç yeni ekrana yayınlanmaz. Başlamış diskteki kayıt veya uzak istek geri alınmış sayılmaz; kayıt hedefi yakalanmış eski UID olarak kalır.

Bu adım kalıcı şehir senkronizasyon kuyruğunu tamamlamaz. Aynı dinleyicide eski/yeni snapshot sıralaması ve bekleyen yerel ekleme/silme uzlaştırması, farklı ViewModel/cihaz eşzamanlılığı, offline yeniden deneme, profil gözlemcisi ve reset yolları açık tutulur. Gerçek Firebase veya kullanıcı verisine erişilmedi; yeni arayüz değişikliği yoktur. Son değişiklikler push edilmedi.

Doğrulama: 6 yeni CitySnapshotApplication testi dahil 288 Android JVM testi geçti (0 failure/error); debug APK derlendi. Yeni testler sıralı kayıt/yayın, farklı UID, aynı UID eski dinleyici sürümü, disk sırasında UID/sürüm değişimi ve iptal edilen NonCancellable depolama sonucunu kapsar. Bu koşuda cihaz/UI testi çalıştırılmadı; canlı Firestore callback davranışı doğrulanmış sayılmaz.


## Kalıcı şehir ekleme/silme kuyruğu — 2 Kasım

CitySyncStore, kullanıcıya bağlı registered_cities ve city_sync_changes kayıtlarını tek DataStore edit içinde yazar. Her şehir için son istenen durum, UUID işlem sürümü ve uzak yazım onayı saklanır. Ekleme/silme sırasında diske kayıt tamamlanmadan uzak istek başlatılmaz. Kuyruk hatası sessizce sıfırlanmaz. Hesap silmenin mevcut havamania:UID: önek temizliği bu kayıtları da kapsar.

Tema ViewModel'i oturum açılışında aynı UID'nin kayıtlarını yeniden gönderir. Firestore işlemleri aynı belgeye set/delete olduğundan bu tekrar istemci açısından idempotenttir; backend/model isteği değildir. Önceki işin onayı yalnız kendi UUID sürümünü işaretler. Kuyruk girdisi ancak uzak yazı onayından sonra, istenen değerle eşleşen sunucu snapshot'ında kaldırılır. Önbellekten veya bekleyen yerel Firestore yazısından gelen snapshot'lar uzlaştırma için kullanılmaz; metadata değişiklikleri de dinlenir. Bekleyen ekleme/silme sunucu listesi üzerinde uygulanır. Böylece bekleyen ekleme eski listede kaybolmaz ve silme işaretli şehir hemen geri eklenmez.

Şehir mutasyon kilidi yalnız yerel düzenleme süresince tutulur; uzak onay beklerken diğer yerel işlemler engellenmez. Tekrar gönderme döngüsü her kayıttan önce mevcut kuyruğu ve hesabı kontrol eder. Başlamış uzak işlem geri alınmış sayılmaz.

Açık kalanlar: varsayılan şehir değişikliği için kalıcı kuyruk, aynı oturumda snapshot işlerinin tam sıralaması, farklı ViewModel/cihaz çatışma protokolü, başarısız girişimler için kontrollü backoff/kalıcı hata sınıflaması, uygulama kapalıyken WorkManager yürütmesi ve gerçek offline/Firebase testi. İlk girişimde hata sonraki kuyruk kayıtlarının gönderimini durdurur; bunlar diskte korunur ve sonraki ViewModel oturum açılışında yeniden denenir. Yerel işlem kayıtları şehir/ad/koordinat içerir ve hesabın mevcut uygulama-özel DataStore alanında saklanır. Son değişiklikler push edilmedi.

Doğrulama: 292 Android JVM testi geçti (0 failure/error; 3 yeni kuyruk birleştirme ve 1 uzak onayın yerel düzenlemeyi kilitlememesi testi). Debug/test APK'ları derlendi. SM-T500 Android 12 üzerinde CitySyncStoreDeviceTest 1/1 geçti: ayrı Preferences DataStore dosyasında yeniden açılış, A/B ayrımı, eski sürüm onayının yeni silmeyi onaylamaması, henüz gönderilmemiş silmenin boş snapshot ile düşmemesi, bekleyen ekleme/silmenin korunması, uzak onay+eşleşmede temizleme ve UID önek temizliği. Bu test gerçek süreç ölümü, gerçek Firebase veya fiziksel bağlantı kesintisi testi değildir. Diff kontrolünde boşluk hatası yok.


## Şehir snapshot sıralaması ve bellek sınırı — 2 Kasım

Her sunucu snapshot'ı için bağımsız IO işi başlatılması kaldırıldı. Her Firestore şehir dinleyicisinin CitySnapshotQueue üzerinden tek tüketicisi vardır. Bir snapshot'ın kaydı/yayını bitmeden sonraki snapshot'ın kaydı başlamaz; böylece aynı dinleyicide yavaş eski kayıt daha yeni kaydın ardından tamamlanarak onu ezmez. İşlenmekte olan snapshot dışında yalnız son bekleyen snapshot tutulur (conflated channel); sınırsız iş/snapshot birikimi önlenir. Ara snapshot'ların atlanması bekleyen şehir işlemlerini onaylamaz; kuyruk yalnız işlenen onaylı sunucu durumuyla uzlaştırılır.

Oturum/dinleyici değişiminde önceki tüketici iptal edilir; tamamlanma kancası bekleyen kanalı kapatır. UID/dinleyici sürümü ve kayıt sonrası yayın kontrolleri korunur. Dinleyici hatası ve snapshot parse/depolama hatası sabit konum hata penceresine aktarılır; iptal hata mesajına çevrilmez ve ham hata kullanıcıya gösterilmez.

Kapsam aynı dinleyicinin snapshot işleme sırasıdır. Farklı ViewModel/cihaz çatışmaları, snapshot işlemiyle eşzamanlı yerel düzenlemenin görünür durum yayın sırası, varsayılan konumun kalıcı kuyruğu, kontrollü yeniden deneme/WorkManager ve canlı Firebase/offline testleri açık kalır. Bu adımda gerçek kullanıcı veya servis üzerinde işlem yapılmadı. Son değişiklikler push edilmedi.

Doğrulama: 3 yeni CitySnapshotQueue testi dahil 295 Android JVM testi geçti (0 failure/error); debug APK derlendi. Yavaş ilk kayıt sürerken sonraki kayıtların başlamaması, bekleyen ara değerlerin yerine son değerin tutulması (1000 değerlik burst dahil) ve iptal sonrası bekleyen snapshot'ın uygulanmaması test edildi. Bu koşuda cihaz veya gerçek Firestore testi çalıştırılmadı. Önceki izole DataStore cihaz testi sonucu korunur; burada yeniden çalıştırılmış sayılmaz.


## Varsayılan konumun kalıcı gönderim kaydı — 2 Kasım

CitySyncStore varsayılan konumu ve UUID sürümlü bekleyen değişikliği tek DataStore edit ile kaydeder. Oturum açılışında aynı UID'nin bekleyen varsayılanı yeniden gönderilir; eski gönderimin onayı yeni seçimin sürümünü onaylayamaz. Eski profil verisi bekleyen seçim varken yerel seçimi geri almaz. Yerel legacy kullanıcı için bulut kaydı oluşturulmaz. UID önekli kayıtlar mevcut hesap silme temizliğinin kapsamındadır.

UserProfileRepository varsayılan konum için yalnız sunucudan gelen ve bekleyen Firestore yazısı olmayan profil snapshot'ını ayrı akışta sunar (metadata değişiklikleri dahil). ThemeViewModel bu akışı şehir listesiyle birlikte izler; profil şehirlerden önce gelirse liste yüklenince isim eşleştirmesi yeniden yapılır. Kuyruk kaydı ancak kendi uzak yazım onayı ve eşleşen sunucu adı ile kaldırılır. Bekleyen kayıt bozuksa sessizce sıfırlanmaz. Kullanıcı değişimi/iptal kontrolleri korunur. Profil dinleyicisine sürüm kontrolü de eklendi; kaldırılmış dinleyicinin callback'i yeni aboneliğe veri yayınlayamaz.

Açık sınırlar: uzak profil sözleşmesi konumu yalnız adla tutuyor; aynı adlı şehir/ilçe ayrımı için sunucuda kalıcı konum kimliği gerekir. Çok cihaz/çok ViewModel yazım sırası ve çatışma protokolü, yerel/sunucu yayın yarışı, backoff/kalıcı hata ayrımı, kapalı uygulamada WorkManager ve canlı offline/Firebase doğrulaması açık. Yeniden deneme ViewModel/oturum açılışına bağlıdır; arka plan hizmeti tamamlanmış sayılmaz. Gerçek Firebase veya kullanıcı verisi üzerinde test yapılmadı. Son değişiklikler push edilmedi.

Doğrulama: mevcut 295 Android JVM testi tekrar geçti (0 failure/error); debug ve test APK'ları derlendi. SM-T500 Android 12'de DefaultCitySyncStoreDeviceTest + CitySyncStoreDeviceTest 2/2 geçti. Yeni varsayılan testi ayrı dosyada yeniden açılış, A/B ayrımı, eski UUID onayının yeni seçimi onaylamaması, yalnız snapshot'ın kaydı tamamlamaması, uzak onay+eşleşmeyle temizleme, şehir sonradan çözümlendiğinde uygulama ve legacy için kuyruk oluşturmamayı kapsar. Gerçek ViewModel/Firebase akışı, süreç ölümü veya internet kesintisi test edilmedi.


## Şehir/varsayılan konum yayın yarışı — 2 Kasım

Aynı veri için hem DataStore akışı hem de yerel/snapshot kaydetme metodunun dönüşü ViewModel state'ine yazıyordu. Diskte eski yazı tamamlandıktan sonra çağıranın dönüşü gecikirse, daha yeni kayıt akıştan ekrana uygulanmışken eski dönüş tekrar eski değeri yayınlayabiliyordu.

Şehir listesi ve varsayılan konumun kalıcı değerleri artık yalnız UID bağlı DataStore gözlem akışından yayınlanır. Ekleme/silme/varsayılan seçimi, sunucu şehir birleştirmesi ve sunucu varsayılan birleştirmesi diski günceller; dönen eski değeri ayrıca ekrana yazmaz. Oturum temizliğinin boş state yayını korunur. Üretim getter'ları CitySyncStore.observeCities/observeDefaultCity üzerinden aynı okuma yoluna taşındı; legacy varsayılan ve mevcut bozuk JSON fallback davranışı değişmedi.

Çağrı taramasında kullanılmayan ThemeViewModel.resetCities/resetAllData kaldırıldı. Özellikle resetCities diske İstanbul yazarken görünür listeyi boşaltıyordu; yeni kuyrukla da uzlaştırılmıyordu. ThemeManager'ın diğer saklama/temizleme API'leri bu adımda topluca kaldırılmadı. Hesap silme temizliği değişmedi.

Kapsam görünür değerin eski metod dönüşüyle geri alınmasıdır. Yerel kararların geciken UI listesinden okunması, çok ViewModel/cihaz gönderim sırası, sunucuda konum kimliği, backoff/WorkManager ve gerçek Firebase/offline testleri açık kalır. Kaynak değişiklikleri push edilmedi.

Doğrulama: mevcut 295 Android JVM testi geçti (0 failure/error); debug ve test APK'ları derlendi. SM-T500 Android 12 üzerinde 4/4 seçili depolama testi geçti: 2 yeni CityPreferencePublicationDeviceTest ve önceki CitySyncStoreDeviceTest/DefaultCitySyncStoreDeviceTest. Yeni testler gerçek ayrı DataStore dosyası ve üretim observeAccountData/CitySyncStore okuma akışı kullanır; eski yazı diske tamamlandıktan sonra metodun dönüşü bekletilir, arada yeni değer kaydedilir, eski dönüş serbest bırakılınca görünür değer ve kuyruk yeni kalır. Tam ViewModel/Compose/Firebase testi veya fiziksel süreç ölümü değildir.


## Atomik şehir ekleme/silme kararları — 2 Kasım

ThemeViewModel'in gecikebilen registeredCities/defaultCity state'inden karar verme kaldırıldı. CitySyncStore.recordCityChange listeyi, varsayılanı ve kuyruğu aynı DataStore edit içinde okur; kuralı doğrular ve yalnız kabul edilen değişikliği liste/kuyruğa atomik yazar. Aynı kimlik yeniden eklenemez; son kayıtlı konum, güncel varsayılan veya bulunmayan konum silinemez. Varsayılan/legacy İstanbul davranışı diskte henüz anahtar yokken de korunur. Bozuk kalıcı JSON yeni işlemle sessizce ezilmez; hata mevcut güvenli hata yoluna gider.

Metot Boolean sonuç döndürür: reddedilen/no-op değişiklikte uzak gönderim başlatılmaz, mevcut kuyruğun UUID/onay bilgisi değişmez. Aynı adlı ama farklı kimlikli yerler artık yalnız ad/admin1 eşitliği nedeniyle mükerrer sayılmaz; kayıt kimliği kullanılır. UI'daki düğme kontrolleri yardımcıdır, veri bütünlüğü artık onlara bağlı değildir.

Önceki depolama testleri yeni silme kuralına uygun gerçekçi en az iki konumla güncellendi. Geciken yayın testinde aynı kimliği yeniden eklemek yerine farklı kimlikli yeni konum eklenir; akıştan yayımlanan en güncel liste kontrol edilir. Sunucudan gerçek veri kaybı/silme kurtarma veya çok cihaz çatışması çözüldü denmez.

Açık işler: çok ViewModel/cihaz uzak gönderim sırası, sunucu konum kimliği, kontrollü retry/backoff ve kalıcı hata ayrımı, gerçek offline/Firebase doğrulaması, profil/reset temizliği ve FCM kurtarma UI. Son değişiklikler push edilmedi.

Doğrulama: mevcut 295 Android JVM testi geçti (0 failure/error); debug ve test APK'ları derlendi. SM-T500 Android 12'de 5 seçili izole depolama testi geçti: yeni CityMutationAtomicDeviceTest ve önceki yayın/şehir/varsayılan testleri. Yeni test 20 eşzamanlı aynı-kimlik eklemede tek kabulü, iki eşzamanlı silmede en az bir konumun korunmasını, varsayılan/son/bulunmayan konumu silmenin kuyruğu değiştirmemesini, A/B ayrımını ve legacy varsayılan korumasını doğrular. Gerçek Firebase/Compose ViewModel akışı, fiziksel offline veya çok cihaz testi değildir.


## Şehir/varsayılan uzak gönderim sırası — 2 Kasım

CitySyncDispatcher süreç genelinde sabit 64 Mutex kullanır. UID ve kaynak anahtarına göre aynı şehir/varsayılan için gönderimler sıralanır; kilit tablosu hesap/konum sayısıyla sınırsız büyümez. Hash çakışması ilgisiz iki gönderimi de sıraya sokabilir; yerel DataStore düzenleme kilidi bundan ayrıdır.

Kuyruk içeriği gönderim kilidi alındıktan sonra diskten okunur. Bekleme sırasında değişen eski payload saklanıp gönderilmez; o kaynağın son UUID sürümü kullanılır. Oturum kontrolü okumadan önce, okuma sonrası gönderimden önce ve uzak dönüşte onaydan önce yapılır. İptal/hata onaya çevrilmez. Önceden uzak yazımı onaylanmış ama sunucu snapshot eşleşmesini bekleyen değer tekrar gönderilmez; onay/snapshot temizliği ayrı kalır.

Açılış kuyruğu, kullanıcı ekleme/silmesi ve varsayılan seçimi aynı göndericiye bağlandı. Aynı süreçteki farklı ViewModel örnekleri kilidi paylaşır. Yerel düzenleme bulut onayını beklemez. Başlamış Firestore isteği geri alınmış sayılmaz; sunucuda sürüm/çatışma protokolü ve çok cihaz garantisi yoktur.

Açık işler: kontrollü retry/backoff ve hata sınıflaması, uzun offline beklemede bekleyen görevlerin birleştirilmesi/iptal politikası, WorkManager, sunucu konum kimliği, hesap silmeyle eşzamanlı gönderim matrisi ve canlı Firebase/offline doğrulaması. Bu adım otomatik ağ retry veya çok cihaz sıralaması sağlamaz. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni CitySyncDispatcher testi dahil 299 Android JVM testi geçti (0 failure/error); debug APK derlendi. Testler yavaş ilk gönderim sırasında yeni isteklerin sıralanmasını ve son sürümün okunmasını, onaylı değerin tekrar gönderilmemesini, okuma sırasında UID değişiminde sıfır gönderimi, hata sonrası onay vermeden kilidin açılmasını ve NonCancellable dönüşten sonra iptalin onayı engellemesini kapsar. Sahte gönderim/okuma callback'leri kullanıldı; bu koşuda cihaz veya gerçek Firebase testi çalıştırılmadı.


## Offline bekleyen gönderim taleplerini birleştirme ve açık retry — 2 Kasım

Her ThemeViewModel oturumu için CitySyncRequestGate eklendi. Aynı UID/kaynak için bir eylem çalışır; yeni talepler ayrı bekleyen ağ görevleri oluşturmak yerine tek dirty sinyalinde birleşir. Başarılı dönüşten sonra sinyal varsa son kalıcı değer ortak CitySyncDispatcher üzerinden yeniden kontrol edilir. Zaten onaylı değer tekrar ağa gitmez. Kaynak tamamlanınca kayıt kaldırılır; yeni oturum yeni gate kullanır. Farklı kaynaklar gate seviyesinde birbirini beklemez; ortak dispatcher'ın sabit hash kilidinde tesadüfi çakışma yine mümkündür.

Hata ve iptal otomatik döngüye çevrilmez. Dirty sinyal hata/iptalde tekrar oynatılmaz; asıl istenen durum diskte korunur. Başarısızlıktan sonra açık kullanıcı retry'si veya sonraki ViewModel/oturum açılışıyla yeniden denenebilir. Bu bellek içi birleştirmedir; kalıcı kuyruğun yerine geçmez ve toplam farklı konum sayısı için sabit görev limiti iddia edilmez.

Şehir yönetimi/hava konum hata penceresine Eşitlemeyi tekrar dene ve Vazgeç eklendi. Retry son kalıcı şehir/varsayılan kuyruklarını okur; eski hata payload'ını veya diske hiç kaydedilmemiş seçimi otomatik yeniden yazmaz. Düğmeye dokunmadan ağ retry başlatılmaz. Ekran kapanışı/pencere gösterimi otomatik retry sayılmaz.

Açık işler: hata koduna göre kalıcı/geçici ayrımı, kalıcı backoff/jitter, WorkManager ve gerçek internet kesintisi testi. Başlamış Firestore isteğinin geri alındığı veya SDK offline kuyruğunun temizlendiği iddia edilmez. Gerçek servis/kullanıcı verisi kullanılmadı. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni CitySyncRequestGate testi dahil 303 Android JVM testi geçti (0 failure/error); debug APK ve test paketi derlendi. Yeni testler 1000 yinelenen sinyalin tek aktif+tek takip eyleminde birleşmesini, hata sonrası otomatik döngü olmamasını/açık retry'yi, iptalde dirty işin oynatılmamasını ve farklı kaynakların gate seviyesinde ilerleyebilmesini kapsar. SM-T500 Android 12 üzerinde CityMutationErrorUiTest 4/4 geçti: açık/koyu 2x yazı, eski kapatma davranışı ve açık retry düğmesine dokunmadan sıfır/dokununca bir callback. Saf fixture ve sahte ağ callback'leri kullanıldı; gerçek Firebase, gerçek internet kesintisi veya WorkManager retry testi değildir.


## Kalıcı retry aralığı ve hata sınıflaması — 2 Kasım

CitySyncChange'a geriye dönük varsayılanlarla failedAttempts, retryNotBefore ve blockedReason eklendi. Geçici gönderim hataları için 30 saniyeden başlayan üstel bekleme (taban en çok 1800 saniye, en çok %25 rastgele ekleme; toplam en çok 2250 saniye) diskte saklanır. Yeni UUID'li kullanıcı değişikliği yeni deneme bütçesi başlatır. Bu kaynak/işlem sürümü politikasıdır; servis genel kota sınırlayıcısı değildir.

Firestore unavailable/deadline/resource exhausted/aborted/cancelled/internal ve IOException geçici; unauthenticated ayrı kimlik doğrulama engeli; permission denied/geçersiz argüman/önkoşul/unsupported ve benzeri retler kalıcı engel sayılır. Diğer hatalar UNKNOWN engeliyle otomatik göndermeye kapatılır. Ham exception mesajı veya stack kayıt içine yazılmaz. Coroutine iptali sınıflanmaz/onaylanmaz. Yalnız uzak send hatası bu politikaya girer; yerel acknowledge/disk hatası yanlışlıkla servis reddi sayılmaz.

Hata kaydı UID/UUID bazlı CAS ile yazılır: eski başarısızlığın yeni seçimi engellemesi veya onaylanmış işi geriye döndürmesi önlenir. Bekleme/engel süresinde dispatcher ağ çağrısı başlatmaz. Oturum hazırlığı yalnız AUTHENTICATION engelini kaldırır; açık kullanıcı retry'si ret/unknown engelini yeniden denemeye açabilir. Açık retry geçici bekleme deadline'ını atlamaz. Başarısız hazırlık kullanıcıya mevcut güvenli hata penceresiyle bildirilir.

Açık işler: bekleme kalan süresi/ret sebebi için daha açıklayıcı kurtarma arayüzü, bir kuyruk öğesinin bekleme/ret durumunda diğer öğeleri durdurmaması, disk yazım hatasında retry politikasının garantisi, saat geri/ileri değişimi matrisi, WorkManager ile zamanı gelince yürütme, gerçek offline/Firebase testleri. Süre dolunca kendiliğinden worker başlatılmıyor; sonraki açık retry/oturum tetiklemesi gerekir. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni retry politikası testi dahil 307 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12 üzerinde CitySyncRetryDeviceTest ve önceki varsayılan/şehir kalıcılık testleri 3/3 geçti. Yeni JVM testleri sınırlı jitter/backoff, güvenli hata kategorisi, eski JSON uyumu/taşmasız deadline ve bekleme/engelde sıfır ağ çağrısını kapsar. Yeni cihaz testi gerçek ayrı DataStore dosyasında yeniden açılış, eski UUID hata kaydının yeni seçime uygulanmaması, auth-only kurtarmanın kalıcı reddi kaldırmaması, açık kurtarma ve gerçek SDK exception nesneleriyle unavailable/permission/unauthenticated ayrımını doğrular. Firebase servisine veya gerçek kullanıcıya istek gönderilmedi; fiziksel offline/WorkManager testi değildir.


## Kuyrukta öğe bazlı devam ve kurtarma metinleri — 2 Kasım

Açılış/manuel şehir kuyruğu runCitySyncBatch ile öğe bazında işlenir. Bekleme süresi/engel veya hata dönen bir öğe sonraki öğelerin denenmesini durdurmaz. Her öğenin gönderimi ve hata yayını UID/iptal kontrolünden geçer; coroutine iptali sıradan öğe hatası sayılmaz. Hesap değişince sonraki öğeler gönderilmez ve eski hata yeni ekrana taşınmaz. Diskten kuyruk listesinin bütünü okunamıyorsa işlem mevcut güvenli hata yolunda durur; bozuk kuyruğun atlandığı iddia edilmez.

CitySyncWaitingException artık yalnız güvenli engel kategorisi/deadline taşır. Hata penceresi yaklaşık kalan dakikayı (yukarı yuvarlanmış), açık retry gereğini ve pencerenin otomatik gönderim başlatmadığını açıklar. Kimlik doğrulama, sunucu reddi ve bilinmeyen engel için ayrı sabit kurtarma metinleri kullanılır. Ham exception/servis içeriği UI'a aktarılmaz. Süre statik hata anında hesaplanır; canlı geri sayım değildir. Genel başlık ve kullanıcı tetiklemeli retry/çıkış korunur.

Açık sınırlar: hiç yanıt dönmeyen ilk ağ isteği hâlâ kendi bekleme süresince sıradaki öğeleri bekletebilir; bu adım sınırsız paralel istek veya timeout eklemez. Çok hata varsa pencerede en son bildirilen neden gösterilir, toplu durum listesi yoktur. WorkManager, disk hatasında retry garantisi, saat değişimi, çok cihaz protokolü ve canlı Firebase/offline doğrulaması açık. Son değişiklikler push edilmedi.

Doğrulama: 5 yeni CitySyncBatch testi dahil 312 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni testler hata/beklemede sonraki öğeye devamı, UID değişiminde eski hata ve gönderimin bastırılmasını, iptalin propagasyonunu, dakika yuvarlaması ve güvenli kurtarma metinlerini kapsar. SM-T500 Android 12 üzerinde CityMutationErrorUiTest 6/6 geçti; yeni bekleme/ret açıklamaları ile önceki açık/koyu 2x yazı retry/kapatma fixture'ları doğrulandı. UI gerçek ViewModel/Firebase veya gerçek offline testi değildir; üretim servislerine istek gönderilmedi.


## Yanıtsız istek ve retry disk hatası güvenliği — 2 Kasım

Üretim şehir/varsayılan göndericisi ağdan önce CitySyncStore.reserveAttempt ile mevcut UID/UUID ve tam snapshot'ı atomik kontrol ederek retryNotBefore kaydeder. Geçersizleşmiş, onaylı, engelli veya hâlâ bekleyen kayıt için ağ başlamaz. Ön kayıt yazımı hata verirse ağ çağrılmaz; disk commit olmuş fakat dönüşü hata vermiş olsa da kalıcı bekleme işareti korunur.

SDK await gönderimi withTimeoutOrNull ile 60 saniyeyle sınırlandırılır. Yerel timeout başarı/onay sayılmaz; batch sonraki öğeye geçebilir. Parent/oturum iptali ayrı kalır. Kotlin coroutine iptaline uymayan bir işin zorla sonlandırıldığı iddia edilmez. Firestore SDK'ya verilmiş uzak yazı yerel timeout ile geri alınmaz; sonradan sunucuda tamamlanabilir.

Belirsiz sonuç için önceden yazılan 2310 saniye (en uzun istemci retry aralığı 2250 + 60 saniye istek penceresi) korunur. Bilinen servis hatasında daha özel kategori/deadline yazılabilir; bu sonraki disk yazımı veya onay yazımı başarısız olursa önceki durable işaret kalır. Bu tercih erişilebilirlik maliyeti taşır: aynı sürümün yeniden gönderimi yaklaşık 39 dakikaya kadar bekleyebilir. Yeni kullanıcı değişikliği yeni UUID olarak kalır; eski hatanın yeni sürüme uygulanmaması korunur. Bu süre sunucudan Retry-After garantisi veya çok cihaz çatışma protokolü değildir.

Açık işler: saat değişimi/yeniden başlatma matrisi, uzun bekleme için daha belirgin durum sunumu, gerçek SDK geç yanıt/offline testi, hesap silmeyle eşzamanlı istek ve WorkManager. FCM ve diğer açık yayın engelleri korunur. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni CitySyncAttemptSafety testi dahil 316 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni JVM testleri preflight hata/supersession durumunda sıfır ağ, kısa enjekte timeout'ta onaysız fence ve sonraki öğeye geçiş, hata kaydı yazımı başarısızlığında fence korunmasını kapsar. SM-T500 Android 12 üzerinde CitySyncPreflightDeviceTest + CitySyncRetryDeviceTest 2/2 geçti. Yeni cihaz testi ayrı gerçek DataStore dosyasında commit sonrası IOException enjeksiyonu, sıfır ağ, yeniden açılışta deadline/onaysız kaydın korunması, erken denemenin ve B UID'sinin rezervasyonunun reddini doğrular. Fiziksel disk doluluğu, gerçek süreç ölümü/Firebase veya gerçek internet kesintisi testi değildir.


## Hesap silmeyle eşzamanlı şehir yazımları — 2 Kasım

Üretim şehir/varsayılan dispatcher'ına AccountDeletionManager pending/busy kontrolü bağlandı. Başlangıç okuması, rezervasyon sonrası uzak gönderim, uzak dönüşte onay ve hata kaydı öncesinde dinamik kontrol yapılır. Silme meşgul/bekleyen durumdaysa yeni ağ işlemi veya yerel onay/hata yazımı başlatılmaz. Mevcut FCM yaklaşımı gibi süreç genelindeki silme kurtarma durumu fail-closed kullanılır; bozuk silme günlüğü pending olduğunda da yazım açılmaz.

CitySyncStore bütün edit transform'larında aynı dinamik writesAllowed kontrolünü yapar. Üretim ThemeManager adaptörü bu kontrolü sağlar. Böylece temizleme sonrasında gelen eski snapshot, varsayılan, acknowledge, hata, retry serbest bırakma veya rezervasyon yazımı silinen UID anahtarlarını yeniden oluşturamaz. Kontrol edit öncesinde yalnız çağıran tarafta değil, serialize edilen DataStore transform'unun içindedir. Okuma/gözlem akışları engellenmez; hesap silme temizliğinin doğrudan UID-önek silmesi bu guard tarafından engellenmez.

Bu istemci düzeltmesi başlamış Firestore isteğini geri alamaz. Sunucu tarafında silme/tombstone ve güvenlik kuralı ile geç gelen uzak yazımın reddi, Firebase/backend uçtan uca silme ve çok cihaz testi yayın engeli olarak açık kalır. Diğer profil/seyahat depolarının bütün yazım yollarının bu şehir guard'ıyla kapandığı iddia edilmez. Saat değişimi/WorkManager ve diğer yayın maddeleri de açık. Gerçek hesap silme veya servis isteği yapılmadı. Son değişiklikler push edilmedi.

Doğrulama: 3 yeni CitySyncDeletionGuard testi dahil 319 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni testler silme öncesinde sıfır read/send, rezervasyon sırasında silmede sıfır send ve uzak başarı/hata dönerken silmede sıfır onay/hata yazımını kapsar. SM-T500 Android 12'de CitySyncDeletionStorageDeviceTest + önceki preflight testi 2/2 geçti. Yeni cihaz testi gerçek ayrı DataStore dosyasında sahte silme bayrağı sonrası UID-önek temizliği, tüm geç yazım yollarının anahtarları yeniden oluşturmaması ve B verisinin korunmasını doğrular. Gerçek AccountDeletionManager/backend/Firebase silmesi veya gerçek kullanıcı işlemi yapılmadı.


## Saat değişimine dayanıklı şehir retry süresi — 2 Kasım

Bekleme kayıtlarına retryDuration/retryElapsedStart/retryBoot eklendi; eski JSON için varsayılan alanlar korunur. Üretim adaptörü Android elapsedRealtime ve boot_count okur. Aynı açılış boyunca bekleme elapsed süreyle ölçülür; duvar saatinin ileri/geri değiştirilmesi erken gönderim veya gereksiz uzun bekleme oluşturmaz. Duvar saati yalnız gösterilecek yaklaşık kalan deadline için kullanılır.

Başlatma kimliği değişirse, elapsed gerilerse veya eski kayıtta güvenilir süre metadatası yoksa UUID CAS ile kalıcı yeniden başlangıç yapılır. Geçerli kayıt için önceki sınırlı bekleme süresi yeniden başlatılır; eski/geçersiz metadata için 2310 saniyelik üst sınır kullanılır. Aynı açılışta her denemede süre başa alınmaz. Açılış sayacı okunamıyorsa süreç kimliği fallback'i kullanılır; bu cihazlarda uygulama yeniden açılışı ek ihtiyat beklemesine neden olabilir. Bu kullanılabilirlik maliyetidir; kayıt silinmez veya onaylanmış sayılmaz.

Dispatcher bekleme kararında ve store rezervasyonu içinde aynı saat modeli kullanılır. Ön rezervasyon ve bilinen hata yazımı elapsed/boot bilgisini diske taşır. Onaylanmış kayıt tekrar saat hazırlığı/ağ işlemine sokulmaz. Sistem saatini veya cihaz ayarlarını değiştiren kod eklenmedi.

Açık işler: gerçek cihaz reboot/saat değişimi ve üretici davranışı matrisi, açılış sayacı fallback'i için UX, WorkManager ile zamanlanmış yürütme, gerçek SDK offline/geç yanıt ve sunucu çok cihaz protokolü. Bu adım Firebase veya cihazın fiziksel saatini değiştirerek doğrulanmış sayılmaz. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni CitySyncClock testi dahil 323 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni JVM testleri aynı açılışta ileri/geri duvar saatinde kalan sürenin değişmemesi, elapsed bitince geri saatte de gönderim, ileri saatte erken ağın engellenmesi ve eski/reboot/gerileyen elapsed metadata kontrolünü kapsar. SM-T500 Android 12 üzerinde CitySyncClockDeviceTest + önceki preflight testi 2/2 geçti. Yeni cihaz testi Android saat adaptörünü salt okunur kontrol eder; ayrı DataStore dosyasında sahte saat/boot ile kayıt yeniden açılışı, tek seferlik kalıcı yeniden başlangıç ve süre dolunca rezervasyonu doğrular. Fiziksel reboot veya gerçek sistem saati değiştirilmedi; Firebase'e istek gönderilmedi.


## Profil önbelleği ve otomatik oluşturma: silme koruması — 2 Kasım

Profil gözlemcisi silme busy/pending durumunda yeni profil önbelleği/otomatik oluşturma işi başlatmaz. Otomatik oluşturma, eski iç içe accountTasks.launch yolundan çıkarıldı; createMissingAccountProfile mevcut UID, izin ve coroutine iptalini sunucu okumasından önce ve sonra kontrol eder. Böylece silme sırasında veya hesap değişiminden sonra dönen yokluk sonucu yeni profil set isteği başlatamaz. Eksik profil kontrolü Source.SERVER kullanır; offline/cache eksikliği sunucuda profil yokmuş gibi değerlendirilmez. Okuma hatası oluşturma izni değildir.

ThemeManager.saveProfileSnapshot içindeki atomik çoklu alan yazımı saveProfilePreferences üzerinden pending/busy kontrolünü DataStore edit transform'u içinde yapar. Silme temizliği sonrasında geç gelen snapshot ad/bio/hakkımda/ilgi alanı/avatar/ton tercihlerini geri yazamaz. Mevcut temizleme doğrudan UID anahtarlarını kaldırmaya devam eder. Bu adım bireysel saveUserName/saveUserBio vb. bütün tercihler veya avatar upload akışlarının tamamını kapsamaz.

İncelemede açık bırakılanlar: ayrı sunucu get ve set arasında çok istemci profil oluşturma yarışı (transaction/create-if-absent gerekir), profil geç hata yayınlarının UID kontrolü, tekil profil/tercih yazımları, avatar upload/silme, kişiselleştirme, seyahat/AI/notification depolarının tam silme matrisi. Başlamış uzak set isteği istemci guard'ıyla geri alınmaz; sunucu silme işareti/güvenlik kuralı doğrulaması açık. Gerçek Firebase veya hesap silme çalıştırılmadı. Son değişiklikler push edilmedi.

Doğrulama: 5 yeni AccountProfileSync testi dahil 328 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Testler silme öncesinde sıfır read/create, okuma sırasında silme/UID değişiminde sıfır create, yalnız doğrulanmış yoklukta oluşturma ve iptal sonrası create olmamasını kapsar. SM-T500 Android 12'de ProfileSnapshotDeletionDeviceTest + önceki şehir silme guard testi 2/2 geçti. Yeni test gerçek ayrı DataStore dosyasında UID-önek temizliği sonrası snapshot transform'unun çalışmamasını ve B verisinin korunmasını doğrular. Gerçek ThemeManager alan eşlemesi/Firebase profil oluşturma, gerçek hesap silme veya çok istemci transaction testi değildir.
