# 24 Kasım 2026 — Çıkış sonrası geciken Firebase oturumu

Firebase Task beklemesini iptal etmek SDK girişini iptal etmez. Eski istek çıkıştan sonra SDK oturumunu tekrar oluşturabilir; yeni girişle yarışabilir.

## Değişiklik

- SDK örneğine bağlı AuthSessionFence, ViewModel kapansa da tamamlanmamış SDK görevini takip eder. Eski görev temizlenmeden yeni giriş başlatılmaz. Temizlik tamamlanmadan kilit açılmaz; yinelenen callback sonraki hesabı kapatamaz.
- Üretimde noBackup içindeki atomik auth-session-state.json dosyası pending/signed_out/accepted ve kabul edilmiş UID bilgisini tutar. Parola/token yazılmaz. Bozuk veya yazılamayan kayıt oturumu yetkilendirmez.
- AuthViewModel giriş, kayıt ve kayıt sırasındaki SDK ad güncellemesini takip eder. Açık çıkış, kayıt toparlanmasının eski oturumu yeniden kabul etmesini engeller.
- Hesap silme yeniden girişi ve reauthentication aynı sıralamayı kullanır; beklenen UID doğrulanır. Canlı silme yapılmadı.

## Doğrulama

673 JVM testi hatasız geçti (7 yeni oturum koruması testi dahil). Derlemede Task<Void> tür çıkarımı açık türle düzeltildi. SM-T500 Android 12 üzerinde yerel demo Auth/Firestore ile 4 cihaz testi geçti: geciken giriş/çıkış/sonraki B hesabı, kayıt yeniden deneme, gerçek kişiselleştirme ve farklı UID/UID'siz toparlanma. XML: 4 test, 0 failure/error/skip. Play paketi ve canlı hesaplar kullanılmadı. Emülatörler ve ADB reverse bağlantıları kapatıldı.

## Açık kontroller

SDK görevi hiç tamamlanmazsa yeni giriş güvenli biçimde engellenir; kalıcı beklemenin kullanıcı deneyimi ayrıca incelenmeli. Hesap silmenin tam manager/backend/cihaz temizliği kabulü açık; SDK reauthentication yarışı ve geciken kayıt oluşturmanın iptal/ViewModel değişimi kabulü aşağıdaki bölümlerde tamamlandı. Kalıcı oturumun süreç yeniden başlatma kabulü aşağıdaki bölümde tamamlandı. Önceki birleşik Activity zaman aşımının kök nedeni hâlâ ayrıştırılmadı. Tam ürün ve mağaza kabulü tamamlandı sayılmaz.

## Profil güncellemesinde oturum koruması

ProfileViewModel içindeki bağımsız SDK updateProfile çağrısı aynı korumaya bağlandı. Kabul edilmiş oturumun nesli, giriş isteğinin neslinden ayrı izlenir: normal profil kaydı kullanıcıyı bekleme veya giriş ekranına düşürmez. Çıkış bu nesli geçersiz kılar; bekleyen görev temizlenmeden başka bir giriş başlatılamaz. SDK UID kontrolü yanlış kullanıcı adına görev başlatılmasını engeller. Üretim yolu aynı atomik oturum dosyasını, izole testler bellekteki kaydı kullanır.

Son kaynakla 676 JVM testi, 0 failure/error/skip ile geçti. Üç yeni JVM testi normal/eşzamanlı profil görevlerini, yanlış UID'yi, açık ve dış SDK çıkışını, yeni girişin bekletilmesini ve yinelenen callback'in B hesabını kapatamamasını doğrular.

SM-T500 Android 12 ve demo Auth/Firestore ile son cihaz koşusu: 5 test, 0 failure/error/skip. Yeni test gerçek SDK ile normal ad güncellemesini yapar; sonra iptal edilen coroutine beklemesinin ardından A kullanıcısının SDK güncellemesini çıkış sonrası tamamlatır. Eski oturumun kapalı kaldığı ve B girişinin başarılı olduğu doğrulanır. Diğer dört kayıt/giriş/kişiselleştirme testi de geçti. Bu test ortak üretim SDK yardımcısı ve AuthViewModel düzeyindedir; profil düzenleme formunun Firestore/yerel tercih adımlarının tümüyle birleşik yarışı ayrıca açık.

Sıradaki: profil formunun birleşik yazıları ve Activity recreate kararlılığı. Profil düzenleme formunda kaydetme sürerken çıkış, görünür hata/yeniden deneme ve süreç kapanması kabulü de listede kalır.

## Kalıcı oturumun süreç yeniden başlatma kabulü

`npm run test:android:session:restart` SM-T500 Android 12 üzerinde yerel demo SDK ve UUID'ye ait atomik oturum dosyasıyla geçti. Tek mantıksal senaryo dört instrumentation aşamasında (prepare, accepted, pending, signedout) çalıştı; dört aşama da OK (1 test) döndürdü. Aşamalar arasında QA paketi force-stop edildi ve farklı PID test içinde doğrulandı.

- Kabul edilmiş A oturumu SDK önbelleği ve atomik dosyadan aynı UID ile geri geldi; B UID'si bu kayıtla yetkilendirilmedi.
- Giriş niyeti diske yazılı, SDK isteği henüz gönderilmemiş sınırdan yeniden açılışta A otomatik yetkilendirilmedi. Açık kimlik bilgileriyle B girişi çalıştı.
- Çıkış diske yazıldıktan ve ViewModel kapandıktan sonra fixture eski A oturumunu SDK önbelleğine kasıtlı olarak yazdı. Sonraki süreç bu kaydı reddetti ve temizledi; açık B girişi çalıştı.

Bu adımda yeni üretim hatası bulunmadı; önceki oturum korumasının kalıcılığına regresyon testleri eklendi. Test force-stop kullanır; güç kesintisi, launcher ekranının tümü veya gerçek uzak ağ gecikmesini kapsamaz. Profil formunun tüm yazı adımlarının birleşik yarışı ve önceki Activity zaman aşımının kök nedeni açık kalır. Önceki son birim testi tabanı 676/676'dır; bu adımda üretim Kotlin kaynağı değişmedi.

`npm run test:android:registration:restart` de yeni kalıcı oturum dosyasıyla tekrar geçti: prepare ve resume ayrı PID ile OK (1 test). Yarım kayıt işareti ve oturum pending durumu birlikte korundu; profil tamamlanınca kabul edilmiş UID, çıkışta signed_out dosyası doğrulandı. Toplam iki mantıksal senaryo, altı başarılı instrumentation aşaması; altı bağımsız senaryo olarak sayılmamalı. Emülatörler ve sahip olunan ADB reverse bağlantıları kapatıldı.

## İptal sonrasında geciken kayıt yanıtı doğrulandı

SM-T500 Android 12 üzerinde `test:android:registration` son koşusu XML sonucu: 7 test, 0 failure/error/skip. İki yeni test, bekleyen SDK kayıt görevini açık kayıt iptali ve ViewModel kapanması/yeniden oluşturulması sonrasında gerçek demo Auth create sonucuyla tamamlatır. Üretim AuthViewModel, aynı SDK örneğine bağlı oturum koruması ve UUID'ye ait gerçek atomik günlükler kullanılır.

Eski görev bitmeden B girişinin SDK'ya gönderilmediği; geç A sonucunun otomatik oturum açmadığı; iptal edilen adın SDK'ya veya users belgesine yazılmadığı doğrulandı. A hesabıyla açık kimlik bilgileri üzerinden yeniden giriş yapılınca UID'siz eski taslak temizlendi ve profile aktarılmadı. Sonraki B girişi çalıştı; B profilinin ad/e-posta/biyografisi aynı kaldı. Her senaryoda tek create çağrısı ve sıfır eski profil yazımı doğrulandı.

Bu adımda yeni üretim hatası bulunmadı; mevcut korumaya iki regresyon testi eklendi. Üretim Kotlin kaynağı değişmedi; önceki birim testi tabanı 676/676 olarak kaldı. Firebase SDK uzaktaki Auth hesabını oluşturmuş olabilir; uygulamadaki iptal bu hesabı geri almaz veya otomatik silmez. Test yalnız uygulama yetkilendirmesinin ve sonraki profil yazılarının durdurulmasını doğrular. ProfileViewModel oluşturulmadığından sonradan açık girişte eksik profilin tam ürün akışıyla hazırlanması ayrıca açık. Fiziksel ağ kesintisi ve gerçek uzak servis kabulü değildir.

Sıradaki: hesap silme yeniden doğrulaması sürerken çıkış ve geç SDK sonucu; ardından profil formunun tüm Firestore/yerel tercih yazılarının birleşik yarışı. Önceki Activity zaman aşımının kesin nedeni ve tam mağaza kabulü açık kalır. Demo emülatörler ve sahip olunan ADB reverse bağlantıları kapanmıştır.

## Hesap silme hazırlığında hesap değişimi düzeltildi

Önceki AccountDeletionManager.start, servis kullanılabilirliği isteğinden sonra auth.currentUser okuyordu. A hesabında düğmeye basıldıktan sonra bu bekleme sırasında B hesabına geçilmesi, silme hazırlığının B ile yürütülmesine neden olabilirdi. Silinecek FirebaseUser ve e-posta artık çağrı başında yakalanır. Hazırlık, servis kontrolünden sonra hem UID'yi hem kabul edilmiş oturum neslini doğrular; çıkıştan sonra aynı UID ile yeniden giriş de eski niyeti geçerli kılmaz.

Yeni prepareDeletionOwner ve awaitDeletionOwnerToken yordamları reauthentication/token beklemelerini korur. Token SDK görevleri ortak görev takibine alındı; çıkış sonrası geç tamamlanma yeni oturumla yarışamaz. Journal oluşturulmasından önce geçersiz kalan hazırlık durur. Daha önce kabul edilmiş silme günlüğünün yeniden denenmesi korunur; bu değişiklik kabul edilmiş silmeyi geri alma işlemi değildir.

Beş yeni JVM testi normal sıra, kullanılamayan servis, A–B değişimi, aynı UID ile yeni oturum ve token beklemesinde eski oturumun geçersizleşmesini sınar. Son kaynakla 681 JVM testi, 0 failure/error/skip ile geçti.

Yeni demo SDK testi normal yeniden doğrulamayı, yanlış beklenen UID'nin SDK çağrısından önce reddini ve açık çıkıştan sonra gelen gerçek reauthenticate sonucunun başarısız sayılıp sonraki B girişini engellememesini doğrular. Geç yanıt token/günlük adımına devam etmez. Bu fixture silme API'sine veya AccountDeletionManager singleton'ına erişmez; tam HTTP→günlük→backend→cihaz temizliği uçtan uca kabulü açık kalır.

İlk cihaz paketi 8 testte 1 hata aldı: yeni silme testi ve diğer altı SDK testi geçti, kişiselleştirme Activity testi ilk recreate adımında STOPPED→RESUMED beklerken zaman aşımına uğradı. Kontrol anında tablet açık/kilitsizdi; zaman aşımının kesin nedeni ayrıştırılmadı. Yeni testteki ilk derleme tipi uyumsuzluğu Task<Void>/Result<Unit> ile düzeltildi; son Kotlin derlemesi geçti.

Son tekrar koşusu XML sonucu: 8 test, 0 failure/error/skip. Aynı kaynak ve kilitsiz SM-T500 üzerinde mevcut yedi kayıt/profil/kişiselleştirme testi ile yeni silme yeniden doğrulama testi birlikte geçti. Önceki Activity zaman aşımı bu tekrarla çözülmüş sayılmaz. Sıradaki: Activity recreate kararlılığının nedenini ayırmak ve profil formundaki Firestore/SDK/yerel tercih yazılarını birleşik test etmek. Emülatörler ve sahip olunan ADB reverse bağlantıları kapatıldı; canlı silme yapılmadı.

## Activity yenilemesinde teşhis ve arka plan kabulü

Kişiselleştirme SDK Activity testine ön plan recreate öncesinde Compose idle eşlemesi ve hata ayrıntıları eklendi. Debug fixture create/start/resume/pause/stop/destroy ve pencere odağını monoton zaman/instance kimliğiyle kaydeder. Ön plan yenilemesi başarısız olursa scenario/lifecycle durumu, ekran etkileşimi ve kilit durumu da hataya eklenir. Kullanıcı adı, parola veya token yaşam döngüsü izine yazılmaz.

Ön plandaki üç recreate kontrolü korundu. İlgi seçimi sonrasında Activity CREATED durumuna alınarak gerçek recreate ve RESUMED dönüşü eklendi; Meteoroloji seçiminin ve aynı Auth/Profile ViewModel örneklerinin korunması kontrol edilir. Ardından sunucuya kayıt, UID'ye bağlı yerel tercih ve geri yığını kontrolleri devam eder. `test:android:entry-recreation` komutu aynı senaryoyu üç ayrı Gradle cihaz koşusunda çalıştırır; ilk başarısızlıkta durur, test içinde hata yutma veya otomatik yeniden deneme yapmaz.

Bu değişiklikler debug/test kapsamındadır; üretim Kotlin akışı değiştirilmedi. Önceki birim testi tabanı 681/681 olarak kalır. Compose eşlemesi iyileştirilmiş olsa da önceki STOPPED→RESUMED zaman aşımının kök nedeni bu değişiklikle kanıtlanmış değildir. Geçen tekrarlar sorunun kesin çözüldüğü anlamına gelmez; tekrar oluşursa yeni yaşam döngüsü iziyle incelenmelidir. Fixture giriş/hava gövdeleri işaretçi olduğu için tam WeatherPremiumActivity/launcher kabulü açık kalır.

Sonuç: SM-T500 Android 12 üzerinde üç tekrarın her biri 1/1, 0 failure/error/skip ile geçti. Toplam 12 açık recreate çağrısı (9 ön plan, 3 arka plan) ve her koşudaki sunucu/yerel tercih doğrulamaları başarılı. Bu üç farklı senaryo değil, genişletilmiş tek senaryonun üç tekrarıdır. Son XML yalnız üçüncü koşuyu içerir; üç BUILD SUCCESSFUL sonucu runner çıktısında doğrulandı. Demo emülatörler ve sahip olunan ADB reverse bağlantıları kapandı. Sıradaki: profil düzenleme formunun Firestore→SDK→yerel tercih yazılarının birleşik kabulü; aralıklı Activity kök nedeni izlemeye açık kalır.

Profil formunun açık sunucu→SDK→yerel kayıt yolu, hata/ret/tekrar deneme ve hesap değişimi kabulü yeni raporda tamamlandı: 24_KASIM_PROFIL_FORMU_KAYIT_BUTUNLUGU.md. Süreç kapanması/çevrimdışı uzlaştırma ve aralıklı Activity kök nedeni açık kalır.
