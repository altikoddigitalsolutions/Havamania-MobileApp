# Yerel Firebase kural testleri

Node.js 20/22 ve JDK 17 ile:

```text
npm ci
npm test
```

İlk çalıştırma emülatör dosyalarını indirir. Testler yalnızca `demo-havamania-audit` projesini, Firestore için `127.0.0.1:8086` ve Storage için `127.0.0.1:9196` adreslerini kullanır. Demo proje başka servisleri kullanmaya çalışırsa Firebase CLI bunu reddeder; test dosyası da emülatör ortam değişkenleri yoksa çalışmaz. Gerçek Firebase hesabı veya giriş gerekmez.

`prepare.mjs`, her çalıştırmadan önce uygulamanın gerçek kural dosyalarını `.generated` dizinine kopyalar. Bu dizin, indirilen bağımlılıklar ve emülatör logları Git'e eklenmez. Emülatörler test bittiğinde kapanır.

Kapsam: hesap sahibinin erişimi, başka kullanıcının engellenmesi, iç içe Firestore belgeleri, silme işaretinin eski oturumla yeniden yazmayı engellemesi, fotoğraf sahipliği/türü/boyutu ve silme yetkisi.

## Android kayıt hazırlığı SDK testi

JDK 17, Android SDK/ANDROID_HOME ve USB hata ayıklaması açık tek yetkili cihazla bu dizinde çalıştırın:

```text
npm run test:android:registration
```

Komut yalnız Auth/Firestore demo emülatörlerini 127.0.0.1:9096 ve 127.0.0.1:8086 üzerinde açar. Ayrı named FirebaseApp ve com.havamania QA paketi kullanılır; com.havamania.mobile Play paketine işlem yapılmaz. Üretim AuthViewModel'i ile geciken profil yazımı, kontrollü hata sonrası aynı UID ile yeniden deneme ve gerçek transaction'da profil alanlarının korunması sınanır. İlk hata transaction öncesine enjekte edilir; fiziksel ağ kesintisi veya tam Activity navigasyonu testi değildir. Test varsayılan çalıştırmada atlanır; emulator opt-in argümanını bu komut verir. Eklenen ADB reverse bağlantıları ve emülatörler sonunda kapatılır.
Kayıt komutu artık FirebaseAuthRegistrationDeviceTest ile FirebaseAccountEntryActivityDeviceTest sınıflarını birlikte çalıştırır. İkinci senaryo gerçek Auth/Profile ViewModel'lerini, üretim kişiselleştirme formunu, ortak yönlendirmeyi ve UID/engel/yazı nesli korumalı QA DataStore eşlemesini bir debug Activity'de birleştirir. Yeni profil oluşturma, bekleyen kayıt sırasında recreate, ilgi seçiminin korunması, sunucu kaydı ve yerel tercihlerin geri okunması sınanır. Test sonunda yalnız demo UID anahtarları temizlenir. Giriş ve hava gövdeleri işaretçidir; tam WeatherPremiumActivity/üretim servisleri veya süreç ölümü kabulü değildir.
## Kalıcı kayıt ve gerçek süreç yeniden başlatma

```text
npm run test:android:registration:restart
```

Bu özel mod QA APK'sını kurar, prepare testinden sonra yalnız com.havamania sürecini force-stop eder ve aynı nonce ile resume testini başlatır. Farklı PID, aynı SDK UID, kalıcı atomik dosya, profil tamamlama ve işaret temizliği doğrulanır. Play paketi com.havamania.mobile durdurulmaz. İki aşama tek mantıksal süreç toparlanma testidir; donanım güç kesintisi değildir.

Normal test:android:registration komutu artık üçüncü olarak farklı UID/UID'siz kayıt ve mevcut profilin eski taslakla ezilmemesi kontrolünü de çalıştırır. Test sonunda yalnız UUID'ye ait kayıt/metadata dosyaları temizlenir. Üretim dosyası parola veya token içermez; sadece UID'ye bağlanan kayıt niyetidir. UID bağlanmamış kayıt otomatik alan aktarımı yapmaz.
24 Kasım: aynı kayıt komutu artık 4 testi çalıştırır. Yeni lateSignInAfterLogoutCannotRestoreOrOverwriteNextOwner senaryosu Task sonucunu bekletip çıkıştan sonra gerçek demo SDK girişini tamamlar; eski hesabın temizlendiğini, beklerken ikinci SDK girişinin başlatılmadığını ve sonra B hesabının kabul edildiğini doğrular. Bu fiziksel ağ kesintisi veya geç kayıt/silme reauthentication testi değildir.

Profil SDK koruması eklendikten sonra kayıt komutu 5 test çalıştırır. cancelledProfileUpdateCannotReviveOwnerOrOverwriteNextLogin normal SDK ad güncellemesini, iptal/çıkış sonrası geciken güncellemeyi ve sonraki B girişini gerçek yerel SDK ile doğrular. Tam profil formu/Firestore/yerel tercih birleşik testi değildir.

## Kalıcı oturumun süreç yeniden başlatma kabulü

```text
npm run test:android:session:restart
```

QA paketinde tek mantıksal senaryoyu dört bağımsız instrumentation aşamasında çalıştırır: prepare, accepted, pending, signedout. Her yeni aşamadan önce yalnız com.havamania force-stop edilir; test farklı PID ve aynı named demo SDK'nın diskteki kullanıcı kaydını doğrular. Üretim AuthViewModel ve FileRegistrationPendingStore ile kabul edilmiş UID'nin korunması, bekleyen girişin otomatik yetkilendirmemesi, açık çıkış kaydına karşı eski SDK önbelleğinin reddi ve açık kimlik bilgileriyle B hesabına giriş kontrol edilir.

pending sınırı, giriş niyeti diske yazıldıktan ve yeni SDK isteği henüz gönderilmeden önce hazırlanır. signedout sınırında fixture, ViewModel dinleyicisi kapandıktan sonra demo A oturumunu kasıtlı olarak SDK önbelleğine yazar; yeniden açılışın bu eski kaydı yetkilendirmemesi beklenir. Bu kurulum, işletim sistemi güç kesintisi veya gerçek ağda geciken cevap testi değildir. UUID'ye ait günlük/metadata dosyaları final aşamada temizlenir. Ara aşama başarısızlığı önceki hazırlık dosyalarını bırakabilir; tüm noBackup dizini silinmemelidir.

Mevcut `test:android:registration:restart` testi de artık aynı kayıt sırasında ayrı atomik oturum dosyasını kullanır; bekleyen durum, başarılı UID kabulü ve çıkışın diske yazılması doğrulanır. Canlı Firebase ve com.havamania.mobile paketi kullanılmaz.

## Geciken hesap oluşturma yanıtı

`npm run test:android:registration` artık iki ek geciken kayıt testiyle 7 senaryo çalıştırır. Birinde `cancelRegistration`, diğerinde ViewModelStore.clear ve aynı SDK üzerinde yeni AuthViewModel kullanılır. Test, SDK görevini TaskCompletionSource ile bekletir ve iptal sonrasında gerçek demo createUserWithEmailAndPassword sonucunu teslim eder. Bekleyen eski görev sırasında yeni girişin SDK'ya gönderilmemesi, eski taslağın ad/profil yazmaması, açık kimlik bilgileriyle A hesabının tekrar erişilebilir olması ve B profilinin değişmemesi kontrol edilir.

Bu bir fiziksel ağ kesintisi veya uzak Auth hesabını geri alma testi değildir. Firebase SDK oluşturmayı tamamladıysa Auth hesabı mevcut olabilir; iptal yalnız uygulama oturumunu ve sonraki profil adımlarını durdurur. Test otomatik uzak hesap silme yapmaz. Üretim ProfileViewModel bu testte oluşturulmaz; dolayısıyla sonradan açık girişte eksik profilin normal yükleme yoluyla hazırlanması ayrı kabul konusudur. Her senaryo named demo app ve yalnız UUID'ye ait yerel günlükleri kullanır.

## Hesap silme yeniden doğrulamasında oturum yarışı

Kayıt komutu artık `deletionReauthenticationRejectsWrongOwnerAndLateResultAfterLogout` ile 8 senaryo çalıştırır. Test yalnız named demo SDK'da normal reauthenticate, yanlış beklenen UID'nin SDK çağrısından önce reddi, çıkış sırasında bekleyen görevin geç tamamlanması ve sonraki B girişini doğrular. Geç görevden sonra token/günlük adımına devam edilmediği bir sayaçla kontrol edilir. Silme API'si, AccountDeletionManager singleton'ı veya canlı hesap silme kullanılmaz.

`AccountDeletionAuthenticationTest` başlangıçtaki hesap/oturumun servis kullanılabilirliği ve token beklemesi boyunca korunmasını ayrıca sınar. Manager üretimde bu ortak hazırlık yordamını kullanır. Tam manager HTTP→atomik silme günlüğü→backend kabulü→cihaz temizliği uçtan uca testi bundan ayrı tutulur.

## Kişiselleştirme Activity yenileme kararlılığı

```text
npm run test:android:entry-recreation
```

Yalnız FirebaseAccountEntryActivityDeviceTest sınıfını aynı yerel demo emülatör oturumunda üç kez çalıştırır. Bir başarısızlık sonraki tekrarları durdurur; test içinde hata yutma veya tekrar deneme yoktur. Her koşu yeni named demo app/UID oluşturur ve yalnız kendi tercihlerini temizler. Ön planda üç recreate (kayıt beklerken, seçimden sonra, kayıt tamamlanınca) korunur; ayrıca CREATED durumunda gerçek recreate ve RESUMED dönüşünde seçim/aynı ViewModel kontrolü vardır.

Ön plan recreate öncesinde Compose idle eşlemesi yapılır. Debug Activity yalnız yaşam döngüsü, pencere odağı, instance kimliği ve monoton zaman kaydeder; kullanıcı adı/token/parola bu izlere yazılmaz. Ön plan recreate hatası scenario durumu, ekran etkileşimi, kilit durumu ve yaşam döngüsü geçmişiyle raporlanır. Bu ek kayıt veya geçen tekrarlar önceki aralıklı STOPPED→RESUMED hatasının kesin nedenini kanıtlamaz. Fixture gerçek SDK/ViewModel/form kullanır; giriş ve hava ekranları işaretçidir, tam launcher testi değildir.

## Profil formu birleşik kayıt testi

`npm run test:android:profile-edit` yalnız FirebaseProfileEditDeviceTest çalıştırır. Üretim EditProfileScreen/AuthViewModel/ProfileViewModel, demo SDK ve QA DataStore kullanılır. Sunucu onayı sonrası SDK beklemesi ve recreate, yerel I/O hatası, koruma reddi, yeniden deneme ve SDK öncesi hesap değişimi doğrulanır. Snapshot yazıcısı bu açık profil kayıt yolunu ayırmak için no-op'tur. Kontrollü I/O hatası fiziksel depolama arızası değildir; tamamlanmış uzak yazı geri alınmaz. Debug Activity release'e girmez. Sadece test UID'lerinin yerel tercihleri temizlenir; Play paketi kullanılmaz.

## Sunucuda tamamlanmış profilin süreç toparlanması

`npm run test:android:profile:restart` UUID'ye ait named demo SDK ve gerçek Firestore disk önbelleğiyle prepare/resume aşamalarını farklı PID'lerde çalıştırır. Sunucuda onaylı profil SDK öncesinde bekletilir; QA force-stop edilir. Resume önce Firestore ağı kapalıyken önbelleğin SDK adına yetki vermediğini, sonra serverConfirmed olayının SDK/local eşitlemeyi başlattığını doğrular. Daha yeni sunucu adı ve createdAt korunur. Yalnız kendi UID tercihi, metadata dosyası ve named Firestore cache temizlenir. SDK/Auth ağı bu testte kapatılmaz; onaylanmamış offline yazı veya tam launcher testi değildir.

## Onaylanmamış çevrimdışı profil yazısı

`npm run test:android:profile:offline` iki gerçek SDK/form senaryosu çalıştırır: zaman aşımı sonrası geç kabul ve kuralla ret sonrası aynı taslağı tekrar kaydetme. 9397 loopback fixture, nonce ile yalnız demo account_deletions işaretini block/unblock eder ve sonunda kendi işaretlerini temizler. Gerçek silme endpoint'i veya üretim kurallarında değişiklik yoktur. Pending snapshot'ın onaylı QA DataStore adını kirletmemesi, SDK adına yansımaması, erken Saved/geri çağrısı vermemesi kontrol edilir. Firestore network kapatılır; Auth network ve bütün cihaz ağı kapatılmaz. Süreç ölümü testi değildir.

## Onay bekleyen profil kuyruğunda süreç ve hesap değişimi

`npm run test:android:profile:pending-restart` tek mantıksal senaryoyu prepare/resume/switchback aşamalarında, üç farklı QA sürecinde çalıştırır. A yazısı Firestore ağı kapalıyken gerçek disk mutation kuyruğuna alınır. İlk force-stop sonrası A'nın pending kaydı diskte doğrulanır; B'ye geçilip yalnız B olarak bağlanılır. İkinci force-stop sonrası A'ya dönülür, aynı pending kayıt yeniden bulunur ve A oturumunda onaylanır. B'nin sunucu/SDK/tercih adları, biyografisi ve createdAt değeri korunur; A'nın eski form isteği Saved üretmez.

Auth/Profile ViewModel, aynı UID'ye bağlı atomik oturum dosyası, gerçek SQLite Firestore önbelleği ve QA DataStore kullanılır. Activity/launcher/form taslağının görsel kabulü değildir. Firestore ağı SDK üzerinden kapatılır; cihazın bütün ağı ve Auth ağı kapatılmaz. Yalnız com.havamania force-stop edilir; com.havamania.mobile Play paketi kullanılmaz. Son aşama yalnız kendi named SDK cache'ini, iki test UID'sinin tercihlerini ve UUID'ye ait metadata/oturum dosyalarını temizler. Bir ara aşama başarısızsa önceki test hazırlık dosyaları kalabilir; tüm noBackup dizini silinmemelidir.

## Süreç sonrası pending profil reddi

`npm run test:android:profile:pending-reject` mevcut loopback block/unblock fixture'ını `--restart-rejection` ile kullanır. prepare/resume/verify aşamaları üç PID'de disk kuyruğu, üretim kural reddi, rollback ve sonraki süreçte tekrar gönderilmeme kontrolünü yapar. Marker yalnız demo projenin erişim kuralını etkiler; Auth disable, refresh-token revocation veya gerçek kullanıcı silme işlemi değildir. Değişmeyen sunucu ad/biyografi/createdAt, SDK adı, UID tercihleri ve Saved üretilmemesi doğrulanır. Ara aşama başarısızsa önceki fixture dosyaları kalabilir; tüm uygulama verisi temizlenmemelidir. Diğer restart modlarının argümanları korunur.

## Auth sunucu oturumu ve ön plana dönüş

`npm run test:android:auth-session` üç senaryo çalıştırır: gerçek Auth emülatöründe disable/enable ve Activity foreground kontrolü; kontrollü geçici ağ hatasında cached owner korunması; eski kalıcı hata yanıtının B hesabını etkilememesi. --auth-session fixture modu yalnız loopback/demo projede sabit Auth disable/enable ve ad/biyografi okuma işlemlerini nonce ile sunar. Firebase CLI'nin kurulu emülatör implementasyonundaki privileged accounts:update endpoint'i kullanılır; credential/servis hesabı veya canlı Admin bağlantısı yüklenmez. Cleanup devre dışı bıraktığı test kullanıcılarını yeniden etkinleştirir. Refresh-token revocation veya fiziksel ağ kesintisi testi değildir.

## Backend gerçek Auth token iptali

`npm run test:backend:auth-session` yalnız yerel demo Auth emülatöründe gerçek Admin SDK ile revoke/disable/delete ve `/v1/profile` erişimini doğrular. Backend'in `venv/Scripts/python.exe` ortamı ve backend bağımlılıkları gerekir. Tablet veya servis hesabı gerekmez. Dış HTTP engellenir; yalnız testin oluşturduğu UUID kullanıcıları temizlenir. Üretim imzalı token veya Android uçtan uca kabulü değildir. Ayrıntı: `docs/24_KASIM_GERCEK_TOKEN_IPTALI.md`.