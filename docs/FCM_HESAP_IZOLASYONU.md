# FCM hesap izolasyonu — açık yayın engeli

30 Ekim 2026. Aşağıdaki istemci protokolü tasarımdır; uygulanmış veya canlıda doğrulanmış değildir.

## Mevcut durum

Aktif Kotlin Android modülünde firebase-messaging bağımlılığı, FirebaseMessagingService/onMessageReceived/onNewToken veya backend /devices/push-token çağrısı bulunmadı. Yerel hava/seyahat WorkManager bildirimleri FCM değildir. Backend FCM sağlayıcısı notification payload kullanıyor; uygulama arka plandayken işletim sistemi bunu uygulamanın UID kontrolü olmadan gösterebilir. Hesap değişiminde tray temizliği, daha sonra ulaşan eski bildirimi kesin engellemez.

Backend token devri için atomik upsert ve sahiplik koşullu DELETE eklendi. Bunlar DB yarışlarını düzeltir; eski hesabın gecikmiş REGISTER isteğinin geçerli yeni kaydı geri almasını tek başına önlemez. Yerel session-generation ve kayıt sıralaması protokolü gereklidir. FCM tokenı bir yetkilendirme anahtarı veya kullanıcı kimliği sayılmamalıdır.

## Uygulama sırası

1. Android FCM bağımlılığı ve dışa kapalı service ekle. onNewToken ve auth değişimini tek session-generation sahibi üzerinden işle; eski coroutine/401 retry yeni hesabın token bağını güncelleyemesin. Oturum yokken tokenı kullanıcıya bağlama. Çıkış/devir çevrimdışı ise kalıcı, sürümlü kayıt kuyruğunu doğrula.
2. Firebase UID ile backend UUID eşlemesini yalnızca mevcut hesabın doğrulanmış backend yanıtından al. UI'dan gelen rastgele UID/UUID'yi güvenme. Her callback'te o anki UID, eşlemenin sahibi, oturum generation'ı ve pending silme durumunu karşılaştır.
3. Sunucu ve istemci capability/version sözleşmesi birlikte eklenmeden payload geçişini yayınlama. Data-only mesajda sabit event_id, owner_backend_user_id, expires_at ve sınırlandırılmış hedef türü olsun. Hesap veya süre uyuşmazsa içerik diske/bildirim alanına yazılmadan düşürülsün. Bildirim izni/ana tercih kontrol edilsin. Data-only teslimin cihaz güç/arka plan kısıtlarında gecikebileceği ayrıca test edilsin.
4. Room'da kullanıcı/event_id unique takip ve hesap/event temelli sistem notification tag ekle. Aynı olayın server retry'si görünür bildirimi çoğaltmasın. onMessageReceived + tıklama + süreç yeniden açılışında aynı sahiplik kontrolleri uygulansın. Intent extras tek başına veri erişim yetkisi değildir.
5. Yerel sahte payload testleri: A–B geçişinde geç gelen A mesajı, çıkış, silme, boş/bozuk/büyük payload, aynı event tekrarı, süresi geçmiş mesaj, permission reddi. Cihaz testleri: soğuk/sıcak açılış, arka plan, process death, çevrimdışı token devri. Gerçek FCM teslimi ancak ayrıca yetkilendirilmiş test ortamında doğrulansın.

Bu protokol ve cihaz/servis kontrolleri bitmeden FCM hesap izolasyonu kapalı sayılmaz. Backend gönderim kaydının reserved/unknown durumlarında otomatik tekrar politikası, istemci event dedup doğrulandıktan sonra yeniden değerlendirilmeli; mevcut konservatif kayıtlar körlemesine sıfırlanmamalıdır.

## 30 Ekim — Alıcı aşaması uygulandı, entegrasyon tamamlanmadı

Kotlin modülüne firebase-messaging, dışa kapalı HavamaniaMessagingService ve protokol-1 data mesaj doğrulaması eklendi. Bu, yukarıdaki başlangıç durumunu kısmen değiştirir: artık alıcı vardır; onNewToken/auth üzerinden backend kayıt kuyruğu ve sunucu data-only/capability geçişi hâlâ yoktur. Otomatik FCM başlatma manifestte false tutulur; token alınması/kaydı bu aşamada açılmaz. Mevcut backend notification payload'ı değiştirilmedi.

Alıcı protokolü: protocol=1, owner_backend_user_id, UUID event_id, expires_at (Unix saniye), title, body, target=weather. Kullanıcı/backend UUID eşleşmesi, sunucunun mevcut Firebase project/UID UUIDv5 formülüyle hesaplanır; istemcinin yerel Firebase projesi farklıysa mesaj reddedilir. Yalnızca mevcut hesap, geçerli süre (en fazla 24 saat), sınırlı UTF-8 boyutu ve tanımlı hedef kabul edilir. Payload'dan keyfi rota veya kullanıcı UID alınmaz.

Room'a fcm_UID_EVENT birincil anahtarıyla INSERT IGNORE yapılır; aynı olayın tekrarı tekrar gösterime ilerlemez. Veri kaydı sırasında hesap değişirse eski kullanıcı kaydı silinip işlem durur. Auth epoch A→B→A geçişini de tanır. Sistem bildirimi etiketinin tam hesap/olay kimliği ve sahipli PendingIntent kullanılır. Ana tercih ve Android izni kontrol edilir; callback kısa, yalnızca yerel işlemlerle 3 saniye sınırında çalışır. Ağ çağrısı ve bağımsız coroutine yoktur.

Sınırlar: DB kaydı sonrası gösterim öncesi çökme/timeout bildirim kaybına yol açabilir; istemci primary-key dedup bir teslim garantisi değildir. Silme/hesap değişimi ile Room yazımının gerçek eşzamanlılığı ve süreç ölümünde davranış cihazda ayrıca doğrulanmalıdır. Global sunucu notification payload'ının OS tarafından otomatik gösterilmesini callback'te reddetmek çözmez; sunucu geçişi ve eski istemci rollout sözleşmesi hâlâ zorunludur.

Kullanılan birincil referanslar: [Firebase alıcı ve callback sınırları](https://firebase.google.com/docs/cloud-messaging/android/receive-messages), [Android istemci kurulumu](https://firebase.google.com/docs/cloud-messaging/android/get-started). Mevcut BOM ile legacy onMessageReceived API kullanıldı; yeni AndroidConfigV2 istemci/sunucu uyumu ayrıca değerlendirilmelidir. Üretim etkinleştirmesi yapılmadı.

## 30 Ekim — Yerel token kuyruğu ve sunucu zarfı

Uygulananlar:
- FcmTokenJournal kurulum UUID'si, artan revision, hedef UID/token ve pending durumunu tutar. AtomicFile, noBackupFilesDir ve yazım sonrası doğrulama kullanılır. Bozuk kayıt sessizce yeni kimliğe sıfırlanmaz. Varsayılan süreçte tek yazıcı FcmTokenQueue vardır. onNewToken ve auth değişimi yalnızca yerel kuyruğu günceller; çıkış/silme UID ve tokenı temizlerken kurulum/revision tombstone'unu korur. Token/kurulum kimliği toString/log'a konmaz.
- Sync çekirdeği kayıt sırasında UID/epoch değişimini ve eski acknowledgement'ı reddeder; ağ/kalıcı yazım hatasında pending korunur. Gateway şu anda testlerde sahtedir; Retrofit/WorkManager gönderim adaptörü henüz bağlanmadı. JSON fake-storage yeniden açılış testi gerçek Android güç kaybı/AtomicFile cihaz testi yerine geçmez.
- Backend sağlayıcısı artık yalnızca owner_backend_user_id/event_id/expiry taşıyan protokol-1 data mesajı üretir; notification fallback kaldırıldı. Kritik öncelik ve mesaj süresine bağlı TTL var. Başlık/gövde Türkçe ve emoji dahil Kotlin UTF-16 sınırlarına göre kısaltılır. Eksik/bozuk kimlikte gönderim yapılmaz.
- FCM_OWNER_PROTOCOL_ENABLED=false varsayılandır. Kapalıyken Firebase default SDK başlatılmaz ve kalıcı gönderim claim'i tüketilmez. Sadece kimlik doğrulamanın named Firebase app'i bulunması default FCM app sayılmaz; default init yarışı ayrıca ele alınır. SDK hataları credential/token ayrıntılarını log'a taşımaz.

Hâlâ açık / etkinleştirmeden önce zorunlu:
1. Backend token kayıt endpoint'ine kurulum/revision CAS + tombstone/capability sözleşmesi eklemek; gecikmiş A REGISTER'ın B'yi geri almasını sunucuda engellemek. Mevcut legacy endpoint henüz revision doğrulamıyor; yeni kuyruk kesinlikle ona bağlanmamalı.
2. Gerçek gateway'nin owner/backend UUID + kurulum/revision makbuzunu doğrulaması; 401 yenileme ve offline WorkManager retry akışını son kuyruk kaydıyla yürütmesi.
3. Yeni token alma/auto-init ve FCM_OWNER_PROTOCOL_ENABLED ancak istemci capability/sürüm rollout'u doğrulandıktan sonra açılmalı. Mevcut kayıtlı tokenların hepsinin protokol-1 alıcısı olduğu varsayılmaz.
4. API 23 dahil AtomicFile, Room, process death, cihaz ve gerçek staging teslim testi; gizlilik/mağaza beyanlarına FCM veri akışını dahil etme.

Bu aşamada gerçek token talebi, backend'e token POST veya FCM mesajı gönderilmedi. Sunucu data sözleşmesi ve istemci kuyruğu test edildi; uçtan uca FCM hâlâ yayına hazır değildir.

## 30 Ekim — Sunucuda revision/CAS ve capability sözleşmesi uygulandı

Bu bölüm önceki açık server-revision maddesini günceller. Migration `0018_push_token_bindings` ile token hash başına kurulum, revision, son kullanıcı ve bound/unbound durumu tutulur. PushToken.owner_protocol eski satırlarda 0'dır. Yeni Kotlin alıcı desteği olmayan satırlara owner-bound data mesajı gönderilmez ve dispatch claim tüketilmez.

Yeni kayıt isteği (POST /v1/devices/push-token) mevcut platform/token alanlarına üçünü birlikte ekler:

```json
{"platform":"android","token":"<device-token>","owner_protocol":1,"installation_id":"<canonical-uuid>","revision":1}
```

Yanıt authenticated backend user_id yanında installation_id, revision ve owner_protocol döndürür. İstemci bunların tamamını yerel snapshot ile karşılaştırmadan acknowledgement yapmamalıdır. revision pozitif, strict integer ve en fazla 9007199254740991; protocol yalnızca 1 ve platform Android olmalıdır. Kurulum kimliği bir yetkilendirme anahtarı değildir; UID her zaman authenticated kullanıcıdan gelir.

Aynı kurulum + daha yüksek revision sahipliği değiştirebilir. Aynı revision yalnızca aynı kullanıcı ve aynı bound durumu için idempotenttir. Eski/eşit-ama-farklı kullanıcı/durum, farklı kurulum veya eksik metadata ile downgrade 409 token_binding_conflict döndürür. PostgreSQL'de ortak transaction advisory lock ve atomik conditional upsert kullanılır; legacy DML üzerinde ayrıca binding yokluğu koşulu vardır.

DELETE aynı metadata ve daha yeni revision ile son kullanıcıya bağlı unbound tombstone bırakır. Sadece mevcut token sahibi unbind edebilir. İlk POST ulaşmadan DELETE gelirse de tombstone oluşturulur; gecikmiş eski POST daha sonra tokenı yeniden oluşturamaz. Legacy istekler korunan tokenı kaydedemez/silemez. Hesap silmede binding kayıtları da CASCADE ile temizlenir; silinen hesabın eski yetkilendirmesi auth gate tarafından reddedilir.

Kapsam/sınırlar: revision token hash başınadır; farklı/rotated tokenların aynı kuruluma ait eski kayıtlarını toplu emekliye ayırma ve geçersiz FCM token temizliği hâlâ açık. Farklı kurulumla aynı token için otomatik takeover yapılmaz; yeniden kurulum/kurtarma akışı doğrulanmalıdır. İstemci offline logout sonrası eski auth olmadan sunucuda unbind yapamaz; yerel alıcı çıkışta içerik göstermez fakat server/SDK token iptali ve retry politikası ayrıca tamamlanmalıdır.

Sıradaki: gerçek Retrofit gateway ve WorkManager adaptörü, 401 refresh/409 conflict/429 backoff ve makbuz doğrulaması. Bunlar bitene kadar FCM auto-init ve FCM_OWNER_PROTOCOL_ENABLED kapalı kalır. Üretimde migration uygulanmadı ve gerçek token kaydı/gönderim yapılmadı.

## 30 Ekim — Android Retrofit gönderim ve makbuz kontrolü

BackendFcmService ile POST v1/devices/push-token sözleşmesi eklendi; mevcut Retrofit servis/factory bu arayüzü de sağlar. İstek platform=android, owner_protocol=1, token, installation_id ve revision gönderir. registerFcmTokenSnapshot fonksiyonu HTTP öncesi/sonrası oturum kontrolü yapar; yalnızca 200 ve birebir eşleşen kullanıcı, platform, token, kurulum, revision, protokol makbuzunu kabul eder. Boş kayıt kimliği reddedilir. Authorization ve token alanları nesne toString çıktısına yazılmaz. Coroutine iptali yutulmaz.

Beklenen backend kullanıcı kimliği çağıran tarafından güvenilir mevcut oturumdan sağlanmalıdır. Fonksiyon kendisi yerel kuyruğu acknowledge etmez; syncPendingFcmToken'ın son UID/epoch/snapshot kontrolü korunmalıdır. Bu katman henüz üretim kuyruğuna veya WorkManager'a bağlanmadı. 401 refresh, 409 conflict ve 429 backoff kararları worker aşamasında ayrıca bağlanmalıdır; bu fonksiyon 200 dışı sonuçları başarı saymaz. Ağ hataları çağırana iletilir; kalıcı kuyruk çekirdeği bunları retry olarak işler.

FCM auto-init ve FCM_OWNER_PROTOCOL_ENABLED hâlâ kapalı. Gerçek token alma/kayıt/gönderim yapılmadı; canlı FCM ve cihaz hesap geçişi matrisi hâlâ açık.

Doğrulama: 4 yeni test dahil 240 Android JVM testi geçti (0 hata/başarısızlık); debug APK derlendi. Yeni testler eşleşen makbuz, 7 uyumsuz makbuz varyantı, HTTP öncesi/sırasında hesap değişimi ve iptali kapsar. HTTP bu testlerde sahtedir; cihaz/gerçek servis testi yapılmadı. Son değişiklikler push edilmedi.

## 30 Ekim — WorkManager ve HTTP hata politikası (etkinleştirme kapalı)

FcmTokenSyncWorker, uygulamanın tek journal nesnesini kullanarak kayıt gönderimini syncPendingFcmToken çekirdeğine bağlar. UID/epoch/silme ve snapshot kontrolü auth öncesinde/sonrasında ve makbuzdan sonra korunur. Beklenen backend UUID mevcut Firebase project/UID için sunucunun UUIDv5 sözleşmesinden türetilir; proje/kimlik sözleşmesinin canlı doğrulaması hâlâ gereklidir. İş verisine token veya auth bilgisi yazılmaz.

- 401: aynı snapshot ile yalnızca bir force-refresh ve yeniden gönderim; hesap değişirse durur. İkinci 401, 403, 409 ve diğer kalıcı 4xx durumları pending kaydı acknowledge etmeden mevcut iş denemesini bitirir.
- 429: saniye türünde Retry-After 1..86400 kabul edilir, eksik/geçersiz değerde 60 saniye. Sonraki gönderimin alt zaman sınırı journal'a kalıcı yazılır; yeniden açılış bu sınırı kaldırmaz. WorkManager exponential backoff nedeniyle daha geç çalışabilir.
- Ağ/408/5xx: bağlantı koşullu WorkManager exponential retry (30 saniyeden başlar). İptal yutulmaz. Hatalı makbuz veya decode başarısı sayılmaz.
- Tekil iş zincirinde APPEND_OR_REPLACE kullanılır; çalışan işin sonuna gelen yeni revision KEEP yüzünden kaybolmaz. Kalıcı HTTP hatasında worker Result.success döndürür ancak kayıt başarılı sayılmaz/acknowledge edilmez; Result.failure ile daha önce eklenmiş yeni hesap işlerinin iptal edilmesi önlenir.
- Journal defer kaydı revision artırmaz; yeni UID/token revision'u eski bekleme süresini temizler. Eski snapshot yeni kaydın bekleme süresini veya ack durumunu değiştiremez.

BuildConfig.FCM_TOKEN_SYNC_ENABLED=false varsayılanı hem planlama hem worker girişini kapatır. Manifest auto-init ve backend server flag de kapalı kalır. Hiçbir gerçek token isteği, kayıt veya FCM gönderimi yapılmadı.

Açık yayın işleri: gerçek Worker/process-death/offline cihaz testi, sunucu kimlik eşlemesinin uçtan uca doğrulanması, ilk token alma ve rotation/reinstall kurtarma, çıkışta eski server kaydının unbind/iptali, conflict durumunun kullanıcı/operatör kurtarma akışı ve gerçek staging FCM teslimi. Bu çalışma bu engelleri kapatmaz.

Son doğrulama: 6 yeni gateway/journal testi dahil 246 Android JVM testi geçti, debug APK derlendi; üretilen BuildConfig'te FCM_TOKEN_SYNC_ENABLED=false doğrulandı. Testlerde sahte HTTP/kimlik sağlayıcısı ve bellek diski kullanıldı. Gerçek CoroutineWorker/WorkManager süreç ölümü, auth ve AtomicFile cihaz testi henüz yapılmadı. Mevcut SDK/deprecation derleme uyarıları sürüyor; diff kontrolü temiz. Son değişiklikler push edilmedi.

## 2 Kasım 2026 — gerçek cihazda AtomicFile dayanıklılığı

Kalıcı journal okuması 4096 UTF-8 baytla sınırlandı; sınırsız readText kaldırıldı. Yazma boyutu atomik işlem başlamadan kontrol edilir, büyük girdi eski dosyayı değiştirmez. Geçersiz UTF-8 artık yerine karakter koyularak kabul edilmez. Bozuk dosya yeni kurulum kimliğiyle sessizce sıfırlanmaz. Pending kayıt için revision>0 ve tamamlanmış kayıt için retryNotBefore=0 tutarlılığı doğrulanır.

Depolama sınıfına izole test dosyası kabul eden constructor eklendi; uygulamanın üretim yolu noBackupFilesDir/fcm-token-queue.json olarak kalır. Testler target cache altında rastgele klasör kullanıp yalnızca kendi dosyalarını temizler; üretim journal'ını veya hesap verisini açmaz.

Doğrulama:
- 247 Android JVM testi geçti (1 yeni metadata tutarlılık testi dahil), 0 failure/error.
- Debug APK ve androidTest APK derlendi.
- SM-T500 / Android 12 üzerinde yalnızca FcmJournalStorageDeviceTest çalıştırıldı: 5/5 başarılı. Gerçek dosyadan yeniden açma + kalıcı bekleme + eski ack reddi, bitirilmemiş atomik yazmadan kurtarma, legacy .bak kurtarma, büyük dosya/girdi sınırı ve geçersiz UTF-8/JSON'da kimliği koruma kapsandı.
- Yarım yazma, stream finishWrite çağrılmadan kapatılarak simüle edildi; gerçek uygulama süreç öldürme veya elektrik/güç kesintisi testi yapılmadı. WorkManager'ın yeniden başlatılması ve gerçek auth/HTTP/FCM entegrasyonu bu testlerin kapsamında değildir.
- FCM senkronizasyon/auto-init bayrakları açılmadı. Test kodu gerçek token kaydı veya Firebase/HTTP çağrısı yapmaz. Backend kaynakları bu adımda değişmedi. Diff kontrolü temiz.

Sırada: gerçek Worker/process-death/offline cihaz matrisi, ilk token bootstrap/rotation, logout server unbind ve kimlik/gerçek staging teslim doğrulaması. Son GitHub commit'i 07708d1; bu yeni değişiklikler henüz push edilmedi.

## 2 Kasım 2026 — Worker karar katmanı ve cihaz testi

Bekleme süresinin oturum sahipliğinden önce kontrol edilmesi düzeltildi. Başka hesaba ait veya silme bekleyen kayıt, gelecekteki retry deadline yüzünden tekrar planlanmaz; mevcut iş acknowledge olmadan biter. runFcmSyncAttempt ile karar katmanı ayrıldı; aynı üretim fonksiyonu hem Worker hem sahte gateway'li testlerde kullanılır. Worker'ın yakaladığı Firebase kullanıcı/epoch çifti korunur; yeni hesabın kimliği eski FirebaseUser nesnesiyle eşleştirilmez. HTTP istemcisi ihtiyaç oluşana kadar oluşturulmaz.

6 yeni JVM testi: stale owner/silme ve gelecekteki deadline, auth/HTTP öncesi bekleme, yanıt zamanına göre kalıcı deadline, eski yanıtın yeni UID/revision'u değiştirememesi, kalıcı hata ile ack ayrımı, ağ hatası/iptal. Toplam Android JVM 253/253 geçti; uygulama ve test APK'ları derlendi.

SM-T500 / Android 12 cihazında FcmWorkerLocalDeviceTest ve önceki AtomicFile paketi birlikte 8/8 geçti (3 yeni + 5 önceki test). Yeni cihaz testleri:
- Gerçek WorkManager'a yalnızca testin UUID'sine ait iş verildi; FCM_TOKEN_SYNC_ENABLED=false iken Worker SUCCEEDED durumuna ulaştı. Üretim kuyruğu gönderilmedi; test sonunda yalnızca kendi iş kimliği iptal edildi.
- Gerçek geçici dosyadan yeniden oluşturulan karar katmanı 429 süresinden önce sahte gönderimi çağırmadı, tam sınırda eşleşen sonuçla acknowledge etti.
- Sahte UID/epoch değişiminde eski APPLIED yanıt yeni hesabın disk kaydını tamamlayamadı.

Sınırlar: gerçek Firebase hesabı değiştirilmedi; HTTP ve ağ hatası testleri sahtedir. Cihazın Wi-Fi'si kapatılmadı, gerçek süreç öldürme/power-loss testi yapılmadı. Gerçek WorkManager testi kapalı flag dalını doğrular; açık flag ile auth/HTTP entegrasyonunu doğrulamaz. Retry deadline yazımında disk doluluğu/commit belirsizliği ve yeniden deneme politikası için hata enjeksiyonu kontrolü de sonraki dayanıklılık matrisinde açıktır. Üretim bayrakları kapalı kalır, push yapılmadı.

## 2 Kasım 2026 — 429 sonrası disk yazımı ve belirsiz commit

Disk yazım hatası enjeksiyonu önceki kodda erken yeniden gönderimi yeniden üretti: 429 sonrası defer kaydı yazılamayınca eski bellek/disk snapshot'ı hemen gönderilebilir kalıyordu. İlk regresyon testi düzeltme öncesinde başarısız oldu.

runFcmSyncAttempt artık HTTP/auth gönderiminden önce aynı kurulum/revision için kalıcı reserveAttempt kaydı oluşturur. Bu yazım başarılı olmadan gönderim başlamaz. Daha sonraki deadline veya ack yazımı başarısız/sonucu belirsiz olsa da önceki koruma hem bellekte hem yeniden açılan journal'da kalır. Yeni hesap/token revision'u eski korumayı devralmaz. Başarılı makbuz pending ve süreyi temizler.

Politika güncellemesi: deneme 180 saniye ile sınırlıdır; belirsiz sonuç koruması en yüksek kabul edilen Retry-After (86400 saniye) + deneme penceresi olarak 86580 saniyedir (24 saat 3 dakika). Bu ek pencere, bekleme başlığının yanıt geldiği anda başlamasını hesaba katar. Bilinen 429 kendi doğrulanmış süresini, bilinen 408/5xx ve HTTP öncesi kimlik edinme başarısızlığı 30 saniyeyi kaydeder. HTTP yanıtı alınamaması veya sonraki disk yazımının belirsizliği korumayı muhafaza eder. Yerel timeout retry olur; üst coroutine/Worker iptali yutulmaz. Timeout, sunucudaki işlemin durduğunu garanti etmez.

Bu konservatif politika belirsiz bir ağ sonucunda FCM kaydını 24 saat 3 dakikaya kadar geciktirebilir; yeniden kayıt kullanılabilirliği ve kurtarma yolu canlı etkinleştirmeden önce değerlendirilmelidir. Önceki 'tüm ağ hataları kısa backoff ile yeniden gönderilir' açıklaması bu bölümle güncellenir. FCM bayrakları kapalıdır. Manuel cihaz saati değişimleri, gerçek süreç öldürme/güç kesintisi ve fiziksel disk doluluğu matrisi bu enjeksiyon testleriyle kapanmaz.

Yeni JVM testleri yazım öncesi/sonrası hata, preflight başarısızlığında sıfır HTTP, belirsiz ack, kayıp yanıt sonrası aynı kimlikle süre sonunda devam, bilinen kısa backoff, geç gelen en yüksek Retry-After ve timeout senaryolarını kapsar. Yeni cihaz testi gerçek geçici AtomicFile üzerinde yazmadan önce veya commit'ten sonra IOException enjekte eder; üretim dosyası ve hesap verisi kullanılmaz. Son test sonuçları aşağıya eklenecektir.

Nihai doğrulama: 7 yeni JVM testi dahil Android 260/260, 0 failure/error. SM-T500 Android 12 seçili cihaz paketi 9/9 (önceki 8 + gerçek geçici dosyada 1 hata-enjeksiyon testi). Debug/test APK derlendi; üretilen FCM_TOKEN_SYNC_ENABLED=false doğrulandı. Son ortak birim+cihaz Gradle koşusu başarılı, diff kontrolü temiz. Gerçek disk doldurma, süreç öldürme ve canlı servis çağrısı yapılmadı. Son değişiklikler push edilmedi.

## 2 Kasım 2026 — kalıcı Firebase oturum hatası ve kurtarma

FirebaseAuthInvalidUserException artık genel geçici hata gibi 30 saniyelik tekrar yoluna girmez. fcmAuthorization adaptörü bunu özel, sabit mesajlı FcmAuthRequiredException'a çevirir; SDK mesajı/cause taşınmaz. Gateway AUTH_REQUIRED sonucu üretir. HTTP 401 sonrası refresh aşamasında da aynı kontrol geçerlidir. HTTP 401'in tek başına kullanıcı silindiği varsayılmaz; ikinci 401 hâlâ STOP'tur (backend yapılandırma sorunu olabilir).

Journal'da geriye uyumlu authRequired=false alanı eklendi. Kalıcı auth hatası pending kaydı başarı/ack olarak kapatmadan authRequired=true durumunu diske yazar. Worker/çekirdek/gateway aynı snapshot için yeniden auth veya HTTP başlatmaz; ack ve reserve/defer girişleri de engellidir. Yeni UID/token revision'u veya çıkış-giriş yeni kaydı durumu temizler. Sadece aynı UID/token ile uygulamayı açmak engeli kaldırmaz. Eski revision'un geç hata cevabı yeni hesabı işaretleyemez. Bu durumun yazımı başarısız olursa önceki kalıcı gönderim koruması uygulanmaya devam eder.

Geçici auth ağ hataları RETRY/30 saniye davranışını korur. Kullanıcının gerçek oturumu kapatılmaz veya hesabı silinmez. Kalıcı engelin ayarlarda kullanıcıya sunumu ve yeniden giriş/kurtarma UX'i, token bootstrap ve canlı FCM rollout'u ile birlikte hâlâ açık yayın işleridir.

Doğrulama: 4 yeni JVM testi dahil Android 264/264, debug ve test APK derlemesi başarılı. SM-T500 Android 12 üzerinde seçili paket 11/11 geçti (önceki 9 + 2 yeni). Gerçek SDK FirebaseAuthInvalidUserException sınıfı dört yerel hata koduyla oluşturularak adaptörün mesaj/cause temizliği doğrulandı; FirebaseAuth servisine bağlanılmadı. Diğer yeni cihaz testi ayrı geçici dosyada auth engelinin yeniden açılışta korunmasını ve yeni revision'da temizlenmesini doğruladı. Gerçek kullanıcı silme/devre dışı bırakma veya oturum açma yapılmadı. FCM flag açılmadı; diff kontrolü temiz. Son yerel değişiklikler push edilmedi.
