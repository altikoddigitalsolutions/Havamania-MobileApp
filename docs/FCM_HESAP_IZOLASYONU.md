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
