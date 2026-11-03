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


## Atomik profil oluşturma ve geç hata izolasyonu — 2 Kasım

Önceki paket b69f579 ile main'e gönderildi. Bu bölüm sonraki, henüz push edilmemiş değişikliği açıklar.

Profilin yokluğunu ayrı get ve ardından set ile doğrulayan yol kaldırıldı. Artık Firestore runTransaction içinde belge okunur ve yalnız mevcut değilse başlangıç profili stage edilir. SDK çatışmada transaction'ı yeniden çağırdığında varlık kontrolü tekrar yapılır; başka istemcinin oluşturduğu özelleştirilmiş profil varsayılan bilgilerle ezilmez. Timestamp/başlangıç nesnesi transaction dışında sabitlenir; callback içinde UI veya DataStore yan etkisi yoktur.

createMissingAccountProfile aktif coroutine Job, UID ve silme busy/pending durumunu callback'in yeniden denemelerinde de kontrol eden canlı bir predicate sağlar. Transaction read öncesinde ve write stage öncesinde kontrol yapılır. İptal edilmiş eski oturum A→B→A dönse bile eski Job aktif olmadığından yazı hazırlayamaz. Başlamış server commit geri alınmış sayılmaz; sunucu silme kuralları halen gereklidir.

Profil yükleme catch bloğundaki hata yayını UID/iptal kontrolünden ve silme izin kontrolünden geçer. Böylece geç hata yeni hesaba veya silme kurtarma ekranına yayınlanmaz. Önceki snapshot DataStore silme guard'ı korunur.

Açık doğrulamalar: gerçek Firestore/emulator transaction çatışması ve güvenlik kuralları, gerçek offline/retry/silme matrisi. Yerel testteki atomik adaptör Mutex ile simüle edilir; bu sonuç canlı SDK transaction doğrulaması değildir. Sonraki kapsam tekil profil/tercih yazımları, avatar ve kişiselleştirme yollarının silme/hesap geçişi kontrolleridir. Son değişiklikler push edilmedi.

Doğrulama: 331 Android JVM testi geçti (0 failure/error); debug APK derlendi. AccountProfileSync test paketi 5 eski testten 8 transaction/guard davranış testine güncellendi: silme/UID değişiminde staging olmaması, yalnız eksik belgede staging, iptal sonrası SDK retry callback'inin geçersizleşmesi, başka istemci commit'inden sonra mevcut profilin korunması, Mutex tabanlı sahte atomik adaptörde 20 istemciden tek create ve okuma hatasında sıfır write. Diğer UID/hata yayını ortak AccountResult testleriyle birlikte yeniden geçti. Bu koşuda cihaz veya gerçek Firebase/emulator testi çalıştırılmadı.


## Tekil profil ve kişiselleştirme yazımları — 2 Kasım

runAccountProfileWrite her aşamadan önce ve son aşamadan sonra coroutine/UID/silme kontrolü yapar. Ad-biyografi Firestore → yakalanmış Firebase Auth kullanıcı nesnesi → yerel önbellek zinciri, kişiselleştirme ve hakkımda kayıtları bu yola bağlandı. Bir uzak aşamadan sonra hesap değişir veya silme başlarsa sonraki Auth/DataStore aşaması çalışmaz ve işlem başarıyla bitmiş sayılmaz. Başlamış uzak yazım geri alınmış sayılmaz.

Ad/biyografi yerel kaydı tek edit içinde; ilgi alanı/onboarding yerel kaydı da tek edit içinde tutulur. Tekil ad, bio, avatar URI, ilgi alanı ve hakkımda DataStore setter'ları silme guard'ını edit transform'unda kullanır. İlgi alanı toggle yolunda yakalanan UID ile AccountTaskScope sahibinin eşleşmesi kontrol edilir; uzak yazı sonrası iptal ve geç hata yayını korunur.

ProfileSaveController'a reset generation kontrolü eklendi. Önceki işlem sonradan başarılı/hatalı dönse de yeni oturumun kayıt state'ini değiştiremez. Final başarı ve hata yayını öncesi iptal kontrol edilir; yerel timeout parent iptaliyle karıştırılmaz. Aynı generation iptalinde Saving temizlenir, eski generation iptali yenisinin durumunu temizlemez.

Açık işler: avatar uzak upload/silme ve hata/log gizliliği, ilgi alanı toggle'ın stale UI listesiyle tüm alanı yazması (atomik alan/transaction), kişiselleştirme/hakkımda iptalinde özel UI durum matrisi, diğer tercih/depo setter'larının tam silme taraması ve canlı Firebase/Auth senaryoları. Önceki işlem kısmen sunucuya yazılmış olabilir; bu adım dağıtık atomik transaction değildir ve kalıcı profil retry kuyruğu sağlamaz. Son değişiklikler push edilmedi.

Doğrulama: 4 yeni AccountProfileWrite ve 2 yeni ProfileSaveController testi dahil 337 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Testler aşama sırası, uzak dönüşten sonra silmede sonraki yazının durması, son aşamada UID değişince başarı olmaması, NonCancellable dönüş sonrası iptal, reset sonrası eski başarı/hatanın yeni Saved'i ezmemesi ve iptalde Saving'in temizlenmesini kapsar. SM-T500 Android 12 üzerinde mevcut ProfileSnapshotDeletionDeviceTest 1/1 yeniden geçti; ayrı DataStore ve sahte silme bayrağı kullanıldı. Yeni Auth/Firestore zinciri gerçek Firebase veya tam ViewModel cihaz testi değildir; kullanıcı verisi değiştirilmedi.


## Avatar işlemleri: çakışma, silme ve hata gizliliği — 3 Kasım

Önceki profil paketi e8a4745 ile main'e gönderildi; bu avatar değişiklikleri henüz push edilmedi.

Yükleme ve silme aynı AvatarMutationController/AssistantTaskRunner üzerinden yürür. Aynı ViewModel'de yalnız bir avatar işlemi çalışır; ikinci dokunma ayrı işlem başlatmaz. Auth değişimi ve ViewModel kapanışı runner'ı resetler; eski işin başarı/hata/finally sonucu yeni işlemin busy durumunu temizleyemez. Yükleme/silme aşamaları runAccountProfileWrite ile UID/iptal/silme kontrolünden geçer. Başarı sürümü yalnız son yerel kayıt ve final guard sonrasında güncellenir.

Yüklemede işleme → Storage put → URL alma → Firestore URL → yerel URI sırası açık aşamalara ayrıldı. Upload await iptalinde Storage upload task'ına cancel çağrısı yapılır; uzakta tamamlanmış bir yazı geri alınmış sayılmaz. JPEG compress sonucu kontrol edilir ve bu akışın sonuç bitmap'i finally içinde recycle edilir. Ana thread'deki mükerrer URI açma kontrolü kaldırıldı; görsel hazırlama Default dispatcher'da kalır. Eski URI/bucket/path/download URL izleme logları ve görsel işleme exception stack logu kaldırıldı.

Silme yalnız Storage ERROR_OBJECT_NOT_FOUND sonucunu idempotent başarı sayar. Yetki/kota/bağlantı gibi diğer hatalar yutulup Firestore URL'sinin temizlenmesine devam edilmez. Genel avatar hata mesajı raw localizedMessage içermez; yetki/hizmet/bağlantı/URI erişimi/bozuk görsel için sabit güvenli metinler kullanılır.

Açık sınırlar: gerçek Firebase Storage upload/delete/402-kota/izin testi, iki ViewModel/cihaz arasında aynı sabit avatar dosyası çatışması, başlamış SDK işleminin uzak tamamlanması, Storage başarılı ama Firestore/yerel yazı başarısızsa orphan/eksik fotoğraf kurtarma kuyruğu, EXIF ayna yönleri ve büyük/bozuk görsel cihaz matrisi. Mevcut ara bitmap yaşam döngüsü ve tam görüntü performans benchmark'ı bu adımda tamamlanmadı. Hiçbir gerçek fotoğraf veya hesap üzerinde işlem yapılmadı.

Doğrulama: 5 yeni AvatarMutationController testi dahil 342 Android JVM testi geçti (0 failure/error); debug APK derlendi. Testler eşzamanlı yükleme/silmenin reddini, reset sonrası eski işlemin yeni busy/success durumunu etkilememesini, silme sırasında başarı/hata bastırılmasını, oturumsuz/silinen kullanıcıda başlamamayı ve gizli URL/token içeren exception'ın sabit mesaja dönüşmesini kapsar. Sahte uzak işlem callback'leri kullanıldı. Bu koşuda cihaz, gerçek bitmap/URI, Firebase Storage veya gerçek fotoğraf yükleme/silme testi çalıştırılmadı.


## Profil ilgi alanlarının atomik güncellenmesi — 3 Kasım

Profil chip'inin toggleInterest(id, ekranListesi) yoluyla eski ekran listesini tüm alan olarak göndermesi kaldırıldı. UI artık açık selected=true/false niyeti iletir. Firestore transaction mevcut selectedInterests listesini okur; yalnız hedef kimlik eklenir/çıkarılır. SDK retry'sinde son liste yeniden okunur, farklı ilgi alanı değişiklikleri eski UI kopyasıyla ezilmez. Aynı seçme/kaldırma niyeti idempotenttir; tekrar seçme mükerrer eleman üretmez. Henüz UI'a yansımamış aynı chip'e tekrar dokunma, görünür duruma göre aynı niyet sayılır; iki kör tersine çevirme olarak oynatılmaz.

Job/UID/silme guard'ı transaction okumasından önce ve write stage öncesinde korunur. Mevcut olmayan profil için update başarısız olur; profil bu yoldan otomatik oluşturulmaz. Bozuk türde uzak liste sessizce filtrelenip ezilmez. Kimlik boş/100 karakter üstü/virgüllü ise reddedilir; virgülle ayrılan yerel cache korunur. Ham hata logu kaldırıldı, UID/silme kontrollü sabit kullanıcı hatası korunur.

Açık sınırlar: canlı/emulator transaction çatışması ve offline davranış, chip bazlı pending/optimistic UI, tam kişiselleştirme formunun bütün nesneyi yazmasıyla eşzamanlılık, ID katalog whitelist'i, diğer cihazlar ve avatar görsel hazırlama matrisi. Firestore transaction offline kalıcı kuyruk olarak sunulmaz; ağ hatasında mevcut hata yolu kullanılır. Bu ekler avatar değişiklikleriyle birlikte henüz push edilmedi.

Doğrulama: 4 yeni InterestSelection testi dahil 346 Android JVM testi geçti (0 failure/error); debug APK derlendi. Yeni testler retry'de son listeyi birleştirme, tekrar edilen seçme/kaldırma niyetinin idempotentliği, hedef dışı tercihlerin korunması ve yerel cache'i bozabilecek ID'nin reddini kapsar. Saf liste politikası ve mevcut ortak guard testleri çalıştı; bu koşuda cihaz/UI, gerçek Firebase veya emulator transaction testi yapılmadı.


## Avatar görsel hazırlama ve EXIF — 3 Kasım

ProfileViewModel içindeki bitmap/stream yönetimi AvatarImageProcessor'a taşındı. Üretim prepareAvatarJpeg, Default dispatcher'da EXIF → sınırlar → örneklenmiş decode → yön → ölçek → JPEG aşamalarını yapar. Her InputStream use ile kapatılır. Çalışan bitmap'ler finally içinde recycle edilir; dönüş/yön/ölçek ayrı bitmap oluşturursa önceki sahip olunan bitmap serbest bırakılır. Dispatcher sınırından bitmap yerine yalnız JPEG ByteArray döner; iptal sırasında arada kalan sonuç bitmap'i sorumluluğu dışarı taşınmaz.

Önceki yalnız 90/180/270 dönüşü yerine sekiz EXIF yönü (yatay/dikey ayna, transpose/transverse dahil) uygulanır. Decode örnekleme hedefi en uzun kenarda 1024 piksel; çıkış en uzun kenarda 512 piksel ve oranı korunur. Küçük görsel büyütülmez, aşırı dar kenar en az 1 piksel tutulur. Örnekleme hesabı Long aritmetik kullanır. Geçersiz görüntü sınırları reddedilir; sıkıştırma başarısızlığı başarıya çevrilmez. Aşamalar arasında iptal kontrolü vardır; native codec'in zorla anında durduğu iddia edilmez.

Açık sınırlar: farklı cihaz/codec ve gerçek galeriden URI izin/mime matrisi, çok büyük/zararlı metadata ve fiziksel düşük bellek testi, HEIC/animasyon/şeffaf PNG politikası, gerçek fotoğraf yükleme ve performans benchmark'ı. Decode sample boyutu codec'e verilen istektir; tüm codec'ler için ölçülmüş sabit bellek tavanı iddia edilmez. Diğer avatar uzak senkronizasyon ve yayın engelleri korunur. Henüz push edilmedi.

Doğrulama: 3 yeni AvatarImageSizing testi dahil 349 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12 üzerinde AvatarImageProcessorDeviceTest 4/4 geçti: sekiz yönün tümünde piksel dizilimi, 4096×2048 sentetik JPEG'in 512×256 hazırlanması ve üç stream'in kapanması, gerçek JPEG EXIF=6 etiketinin okunup 100×200→200×100 dönüşmesi, bozuk verinin reddi/akış kapanması. JVM testleri boyut sınırı, oran/küçük görseli büyütmeme, aşırı dar/Int.MAX_VALUE hesabı ve geçersiz sınırları kapsar. Gerçek kullanıcı fotoğrafı, gerçek galeriden izin, fiziksel düşük bellek veya Firebase upload testi yapılmadı.


## Avatar metadata gizliliği ve başarısız stream kapanışı — 3 Kasım

Hazırlanan JPEG kaynak dosyayı kopyalamaz; decode edilmiş pikseller yeniden sıkıştırılır. Bu adımda sentetik kaynak JPEG'e GPS enlem/boylam, cihaz üretici/model, sanatçı, açıklama, kullanıcı yorumu ve çekim zamanı eklenerek çıktıda seçili özel EXIF alanlarının taşınmaması cihaz testiyle doğrulanır. Tüm olası format/metadata/codec için evrensel temizleme garantisi iddia edilmez.

Decode sonucu artık stream use bloğu kapanmadan owned working bitmap değişkenine atanır. Stream.close decode sonrasında IOException üretirse bitmap finally temizliğinin dışında kalmaz. Decode sonrasında yön dönüşünden önce ayrıca iptal kontrolü yapılır. Başarısız kapanış/iptal tamamlanmış JPEG gibi dönmez.

Yeni fixture'lar üçüncü açılışta SecurityException (izin iptali benzetimi), decode stream kapanış hatası ve decode sonrası iptali kapsar. Gerçek Android galeri izni değiştirilmez; InputStream sağlayıcısı sahte erişim hatası üretir. Gerçek kullanıcı konumu/fotoğrafı kullanılmaz, Firebase'e gönderim yapılmaz.

Açık işler: gerçek galeri URI/üretici/codec izin matrisi, XMP/IPTC/ICC ve farklı dosya türleri, kaynak URI içeriğinin üç okuma arasında değişmesi, fiziksel düşük bellek, Storage çok cihaz/orphan kurtarma ve diğer yayın engelleri. Henüz push edilmedi.

Doğrulama: mevcut 349 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12'de AvatarImagePrivacyDeviceTest + önceki AvatarImageProcessorDeviceTest 8/8 geçti (4 yeni gizlilik/hata, 4 önceki görsel testi). Yeni testte kaynak özel EXIF alanlarının gerçekten bulunduğu kontrol edildikten sonra çıktıda hepsinin yokluğu ve PRIVATE_ işaretinin bulunmaması doğrulandı. Sahte izin kaybında sıfır JPEG, tüm açılmış stream'lerin kapanması, decode kapanış hatasında kısmi sonuç dönmemesi ve decode sonrası iptalde JPEG dönmemesi doğrulandı. Gerçek galeri izni değişmedi, gerçek fotoğraf veya Firebase kullanılmadı.


## Avatar kaynağının tek açılışta sabitlenmesi — 3 Kasım

EXIF/bounds/decode için aynı content URI'nin üç ayrı açılışı kaldırıldı. snapshotAvatarSource sağlayıcıyı bir kez açıp en fazla 20.000.000 baytlık kaynak görüntüsünü bellekte özel parçalarda tutar. İşleyicinin üç okuma geçişi artık bu aynı baytlardan yapılır; sağlayıcının sonraki açılışta farklı fotoğraf döndürmesi yön/boyut/decode uyumsuzluğu oluşturmaz. Sağlayıcının tek okuma sürerken kendi içeriğini atomik tuttuğu garantisi verilmez; yakalanan baytlar sonraki aşamalarda değişmez.

Kaynak büyükse daha decode başlamadan AvatarSourceTooLargeException ve sabit 20 MB mesajı üretilir. Kaynak diske kopyalanmaz; GPS/yorum içeren orijinal için yeni kalıcı cache dosyası oluşturulmaz. Parçalı saklama ikinci tam boyutlu ByteArray kopyasını önler, ancak kaynağı bellekte tutmanın maliyeti vardır. Bu tüm codec'ler/native allocation için sabit toplam heap garantisi değildir; düşük bellek testi açık.

Okuma/close/izin hatası veya iptal kaynak görüntüsü tamamlanmış sayılmaz. Kaynak kopyalama boyunca iptal kontrolleri ve stream use kapanışı korunur; 0-byte bulk read durumunda tek bayt okuyarak ilerleme sağlanır. Bloklanan ve iptale uymayan content-provider okumasının zorla kesildiği iddia edilmez.

Önceki görsel/gizlilik fixture'ları tek sağlayıcı açılışına güncellendi: izin kaybı kaynak okuması sırasında, close hatası kaynak kapanışında, iptal kaynak alımından sonra sınanır. Sonraki provider açılışı farklı içerik verecek fixture'da yalnız ilk açılış kullanılmalıdır. Gerçek galeri/codec/URI matrisi, sağlayıcı bloklaması, fiziksel düşük bellek, diğer metadata türleri ve Storage canlı testleri açık. Henüz push edilmedi.

Doğrulama: 4 yeni AvatarSourceSnapshot testi dahil 353 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni JVM testleri tek sağlayıcı açılışıyla çok parçalı 150.000 baytın üç geçişte aynı kalmasını, enjekte küçük sınırda taşmanın reddi/kapanışı, tam sınır ve 0-byte okuma, güvenli boyut mesajını kapsar. SM-T500 Android 12'de güncellenmiş AvatarImagePrivacyDeviceTest + AvatarImageProcessorDeviceTest 9/9 geçti: tek açılışta değişen sağlayıcı senaryosu, metadata, izin/close/iptal, EXIF ve büyük JPEG doğrulamaları. Gerçek galeri izni değiştirilmedi, gerçek fotoğraf/Firebase kullanılmadı.


## Şeffaflık, format ve parça sınırı okuması — 3 Kasım

JPEG alpha taşımaz; alpha içeren bitmap artık ölçeklemeden sonra açıkça beyaz zemine Canvas ile birleştirilir. Tam şeffaf pikseller için rastlantısal siyah arka plan yerine tanımlı bir JPEG politikası vardır; yarı şeffaf pikseller de beyazla compositing yapar. Bu akışta yeni opak bitmap'in sahipliği finally temizliğine devredilir, önceki ölçeklenmiş bitmap serbest bırakılır. Çıkış JPEG/75 ve mevcut boyut politikası korunur; küçük görseller büyütülmez.

Parçalı kaynak, Enumeration/SequenceInputStream yerine tek konum imleçli bağımsız InputStream okuyucuları sunar. available tüm kalan kaynağı gösterir; bulk read/skip parça sınırını aşabilir, mark/reset aynı baytlara döner. Tam boyutlu ek kaynak kopyası oluşturulmaz. Böylece codec/EXIF okuyucusunun uzunluk ve geri okuma davranışı için belirgin akış sözleşmesi sağlanır. Okuyucular birbirinin konumunu değiştirmez; aralık hatası konumu ilerletmez.

Yeni Android fixture'ları şeffaf/yarı şeffaf PNG'nin görsel sonucunu, PNG/WebP'den JPEG üretimini ve EXIF APP1 bloğu 64 KB sonrasına itilmiş JPEG'i kapsar. Renk karşılaştırması JPEG kayıplı sıkıştırmasına uygun toleransla yapılır. Gerçek galeri/HEIC/animasyon/üretici codec matrisi, geniş renk gamutları, düşük bellek ve canlı Storage hâlâ açık. Bu adım fotoğraf yükleme veya Firebase testi değildir. Henüz push edilmedi.

Doğrulama: 3 yeni kaynak akışı testi dahil 356 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. Yeni JVM testleri parça sınırında bulk read/mark/reset/skip/EOF, tam available uzunluğu, bağımsız okuyucular, unsigned tek bayt ve aralık hatasında konumun korunmasını kapsar. SM-T500 Android 12'de AvatarFormatDeviceTest + önceki gizlilik/işleme paketi 13/13 geçti (4 yeni format testi, 9 önceki test). Yeni testler tam/kısmi alpha compositing rengini, PNG/WebP→JPEG MIME/boyutunu ve 64 KB sonrası EXIF=6 etiketinin doğru yönlendirmesini doğrular. Yalnız sentetik görsel ve ayrı test dosyaları kullanıldı; gerçek galeri, fiziksel düşük bellek veya Firebase testi yapılmadı.


## Bildirim Room deposu: hesap silme sonrası yeniden oluşma — 3 Kasım

NotificationDatabase sürümü 5→6 oldu; notification_account_deletions tablosu ve migration eklendi. AccountDeletionManager artık bildirimler için deleteAccountData kullanır: aynı Room transaction'ında hesap engeli kaydedilir ve o UID'nin mesajları silinir. Geç insert/insertIncoming/update yolları engeli kontrol eder; uygulama/veritabanı yeniden açıldıktan sonra da geç sonuçlar mesajı yeniden oluşturamaz. insertIncoming ret durumunda -1 döndürür; mevcut FCM persistence yolu bunu başarılı kayıt saymaz.

Engelde ham UID veya bildirim içeriği yerine SHA-256 UID özeti tutulur. Bu minimal kalıcı güvenlik metadatasıdır; anonimlik veya bütün yerel verinin fiziksel olarak sıfırlanması iddia edilmez. İşaret mevcut Room verisi/app verileri silinene kadar korunur; bu yerel tutma davranışının gizlilik dokümanında değerlendirilmesi açık kalır. Migration geçmişte tamamlanmış silmelerin UID'sini geriye dönük icat etmez.

REPLACE insert ve entity update aynı bildirim kimliğinde başka kullanıcının kaydını ezemez; owner kontrolü aynı transaction içindedir. Normal kullanıcı inbox deleteAll işlemi hesap silme işareti koymaz, gelecekteki bildirimlere izin verir. Raw DAO metotları Room altyapısı içindir; üretim çağrıları korumalı metotları kullanır. Eski 4→5 migration testi yeni 5→6 zincirini de alacak şekilde güncellendi.

Açık sınırlar: sistem bildirimini OS'a gönderme aşamasında geç işlemin durdurulması ayrıca doğrulanmalı; DB'ye ret ile tüm OS bildirim kanalları kapsanmış sayılmaz. Weather Room seyahat/AI ve kuyruk yazımları, canlı FCM/hesap silme ve çok cihaz matrisi sıradadır. Bu adım gerçek kullanıcı veritabanını temizlemez; testler ayrı adlarla oluşturulan veritabanlarındadır. Henüz push edilmedi.

Doğrulama: 1 yeni NotificationOwnerHash testi dahil 357 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12 üzerinde NotificationDeletionDeviceTest (3 yeni) ve mevcut version4 bildirim migration testi toplam 4/4 geçti. Yeni testler ayrı dosyada silme sonrası yeniden açılışta insert/incoming/update reddi, B verisinin korunması, çapraz UID REPLACE/update engeli, normal kutu temizliği sonrası yeni mesaja izin ve 5→6 veriyi koruyan migration'ı kapsar. Mevcut test 4→5→6 zincirini doğruladı. Gerçek kullanıcı DB'si, gerçek hesap silme, OS bildirim gönderimi veya canlı FCM testi yapılmadı.


## Seyahat/AI Room deposu: hesap silme sonrası yeniden oluşma — 3 Kasım

WeatherDatabase sürümü 22→23 oldu; weather_account_deletions tablosu ve migration eklendi. deleteAccountData aynı transaction içinde UID'nin SHA-256 özet engelini yazar; seyahatleri, AI geçmişini ve history/trip deletion/upload kuyruklarını temizler. AccountDeletionManager'ın ayrı ayrı temizleme çağrıları bu metoda taşındı. Bildirim ve Weather depoları farklı veritabanlarıdır; bütün depolar arasında tek dağıtık transaction iddia edilmez. Mevcut silme günlüğü yarım kalan yerel temizliği yeniden yürütür.

Gelen plan insert, yerel seyahat/analiz kaydı, yerel/gelen AI geçmişi ve tam uzlaştırma yollarında hesap engeli kontrol edilir. Geç history deletion, trip deletion ve upload enqueue çağrıları da korumalı transaction'lara alındı. Silme tamamlanması callback'leri hesabın kuyruk işaretlerini tekrar oluşturmaz. AI yerel insert engelde hata döndürür; sessizce kayıt başarılı denmez. Raw insert metotları Room altyapısı için kalır; üretim çağrıları korumalı yolları kullanır.

Bildirimdeki SHA-256 yardımcı fonksiyonu AccountOwnerHash'a taşındı ve iki depo tarafından paylaşıldı. Eski hash'ler değişmez. Minimal özet işareti app verileri kaldırılana kadar yerel güvenlik metadatası olarak kalır; anonimlik/fiziksel tam sıfırlama iddia edilmez. Geçmişte tamamlanmış silmelerin kimliği geriye dönük tahmin edilmez. 20 ve 11 sürümü test fixture/migration zincirleri de 23'e kadar güncellendi.

Açık sınırlar: paylaşılan weather_cache geç ağ sonucu tarafından yeniden doldurulabilir; UID'ye bağlı depo engeli bunu kapsamaz. Başlamış uzak upload/silme, OS bildirim yayını, raw API'lerin gelecekte yanlış kullanılması, kullanıcıya doğru no-op/engel UI bildirimi ve canlı silme matrisi ayrı doğrulama ister. Gerçek kullanıcı DB'sine veya servise müdahale edilmedi. Henüz push edilmedi.

Doğrulama: mevcut 357 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12'de 8 seçili Room testi geçti: 3 yeni WeatherAccountDeletionDeviceTest, 4 mevcut TripDeletionPersistenceTest ve mevcut sürüm 11 migration testi. Yeni testler yeniden açılışta A plan/AI/kuyruk yazımlarının reddi ve B verisinin korunması, 100 yerel yazımla hesap temizliği yarışı ve 22→23 veriyi koruyan migration'ı kapsar. Mevcut testler 20→23 ve 11→23 migration zincirlerini de doğruladı. Gerçek kullanıcı DB'si, gerçek hesap silme veya canlı Firebase/backend testi yapılmadı.


## Hesap temizliği sonrası geç hava yanıtı — 3 Kasım

WeatherRepository üretim singleton'ına pending/busy silme kontrolü bağlandı. getWeatherData cache okuma/yayın ve ağ sonucu/hata aşamalarında izin kontrol eder. Silme başladığında eski başarı veya hata yeni hava state'ine taşınmaz. Hesap temizliği clearForAccountDeletion kullanır: aynı repository kilidinde epoch artırılır, görünür veri sıfırlanır ve weather_cache temizlenir. O anda cache yazımı başlamışsa temizleme onu bekler; sonra yazılmış kayıt da silinir.

Akış epoch'u getWeatherData çağrısında yakalanır (collection başlangıcında değil). Böylece temizlemeden önce oluşturulup daha sonra collect edilen cold flow eski koordinatlarla yeni istek başlatamaz. Silme kurtarması bitip erişim yeniden açılsa da eski epoch yanıtı geçerli sayılmaz. Yeni çağrı yeni epoch ile normal çalışır. Cache/state yayınları kilitle serileştirilir; downstream emit sırasında kilit tutulup temizleme UI tüketicisine bağımlı hale getirilmez. Önceden tüketiciye ulaşmış verinin geri çağrıldığı iddia edilmez.

Normal ayarlar cache temizleme ve clearCurrentWeather davranışı bu silme işlemiyle karıştırılmaz; kullanıcı cache temizlerken görünen hava verisini aniden boşaltma eklenmedi. Üretimde tek repository örneği kullanılır; farklı keyfi repository örnekleri arasında bu bellek epoch'u paylaşılmaz. Süreç ölünce eski callback'ler de ölür; yeniden açılışta pending silme günlüğü yeni erişimi engeller.

Açık sınırlar: silme dışı A/B/A hesap geçişinde ortak hava state'i epoch'u, bağımsız fetchWeatherSnapshot çağıranlarının tam yaşam döngüsü matrisi, gerçek ağ/SDK iptali ve OS bildirim yayını. Bu adım gerçek Firebase/hesap silme veya gerçek hava servis isteğiyle test edilmedi. Henüz push edilmedi.

Doğrulama: mevcut 357 Android JVM testi geçti (0 failure/error); debug/test APK'ları derlendi. SM-T500 Android 12 üzerinde WeatherLateResponseDeviceTest (4 yeni) + mevcut WeatherCacheClearStorageTest toplam 5/5 geçti. Yeni testler temizlik bitip izin yeniden açılsa bile geç başarının cache/state/collector'a dönmemesi ve yeni isteğin çalışması, başlamış cache yazımını temizliğin bekleyip silmesi, eski cold flow'un sonradan ağ başlatamaması, pending silmede yeni çağrı/late hata engelini kapsar. Gerçek ayrı in-memory Room ve sahte WeatherApiService kullanıldı; gerçek hava servisine/Firebase'e istek veya gerçek hesap silme yapılmadı.


## OS bildirim yayımlama: oturum/temizlik yarışı — 3 Kasım

NotificationSessionEpoch artık kısa eşzamanlı OS publish ve epoch-change+cancelAll işlemlerini aynı süreç içi kapıda sıralar. MainApplication hesabın değiştiğini gözlediğinde ve AccountDeletionManager son yerel temizliği yaptığında epoch artırma ile görünür bildirim iptali aynı kapıdan geçer. Daha önce kapıya kabul edilmiş post önce biter, ardından temizleme uygulanır; temizlik bitince eski epoch tekrar post edemez. Başlamış native çağrının atomik biçimde geri alındığı iddia edilmez.

Hava ve seyahat worker'ları çalışma başında epoch yakalar; UID aynı görünse bile gözlenen A→B→A geçişi eski işi geçersiz kılar. Son postNotificationIfCurrent kapısı canlı Job iptalini de kilit alındıktan sonra kontrol eder. Böylece kapıyı beklerken iptal edilen worker native notify çağırmaz. Final aşamada permission ve pending/busy tekrar kontrol edilir. İki worker ayrıca notification DB'deki kalıcı hesap silme işaretine bakar. FCM data-only alıcısı final TTL/izin/oturum kontrolüyle aynı yayın kapısına bağlandı.

Üç üretim notify çağrısı ortak kapının içindedir. Kapı içinde ağ/Room işi yapılmaz; yalnız kısa koşul/OS çağrısı bulunur. Seyahat güzergâh analizinde CancellationException yutan catch de düzeltildi. İşlem kipleri ve uygulama içi bildirim kaydı/OS gösterimi birbirine eşit sayılmaz.

Açık sınırlar: gerçek OS tepsi/kanal/API33/OEM ve hızlı Firebase auth callback matrisi, ilk process açılışındaki eski tepsi temizliği, çok süreç, app bildirim tercihi hazırlık sırasında kapanırsa davranış ve kanal/dedup kurtarma. FCM notification payload'ının SDK tarafından arka planda otomatik gösterimi bu data-only kapısına dahil değildir; sunucu data-only sözleşmesi/rollout doğrulanmalı. Gerçek FCM etkinleştirilmedi, cihaz tepsisinde gerçek bildirim gönderilmedi/temizlenmedi. Henüz push edilmedi.

Doğrulama: 4 yeni AccountNotificationPublication testi dahil 361 Android JVM testi geçti (0 failure/error); debug APK derlendi. Testler yanlış UID/eski epoch/silme engeli, geçerli oturumda tek post, gerçek iki thread ile post tamamlanmadan cleanup callback'inin girememesi ve cleanup sonrası eski epoch'un reddi, monitor beklerken iptal edilen coroutine'in post etmemesini kapsar. OS post/clear callback'leri sahtedir; cihaz bildirimi, gerçek worker/FCM/OEM veya Auth SDK geçişi testi yapılmadı.
