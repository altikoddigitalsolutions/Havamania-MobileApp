# 18 Kasım — Android Firebase bekleyen yazı ve hesap değişimi

## Yapılan çalışma ve sonuç

FirebaseCityContractDeviceTest'e üç kabul testi eklendi. Son npm run test:android koşusu SM-T500 Android 12 üzerinde 6/6 geçti; 0 hata/atlama. Önceki üç Android SDK kabul testi aynı koşuda tekrar geçti. Bu tur üretim Kotlin kodunda değişiklik gerekmedi; test kapsamı genişletildi. Gerçek kullanıcı/Firebase yerine ayrı demo FirebaseApp, yerel Auth/Firestore emülatörleri ve her test için rastgele yerel kullanıcılar kullanıldı.

## Yeni kabul senaryoları

1. SDK disableNetwork sırasında profil yazısı yerel cache'te pending olarak görünür; yazının Task'i tamamlanmamıştır ve sunucu onayı sayılmaz. enableNetwork sonrası Task tamamlanır; Source.SERVER görüntüsü cache/pending değildir ve konum kimliği üretim çözümleyicisinden doğru döner. Daha sonra şehir belgesinin kimliği değiştirilir; önceki kimlikle transaction silme reddedilir ve yeni kayıt korunur.
2. Üretim CitySyncDispatcher rezervasyon geri çağrısı içinde gerçek emülatör Auth hesabı A'dan B'ye değiştirilir. Gönderim ve onay sayacı sıfır kalır. B'nin aynı şehir yolunda kayıt oluşmaz. A'ya tekrar girişle eski kaydın korunması sunucudan doğrulanır.
3. A'nın gerçek SDK transaction silmesi tamamlandıktan sonra, send geri dönmeden emülatör Auth hesabı B'ye geçirilir. Yerel onay verilmez; testin pending değişkeni korunur, B'de kayıt oluşmaz. A'ya yeniden girişte uzak silmenin zaten gerçekleştiği doğrulanır. Korunan işlem A için yeniden gönderilir; olmayan kaydın idempotent silmesi başarıyla tamamlanıp yalnız o zaman onaylanır. Tamamlanmış uzak işlemin hesap değişiminde geri alınabileceği iddia edilmez.

Hesap geçişi testlerinde yerel kuyruk test değişkenidir; bu koşu DataStore/Room kalıcılığı veya süreç ölümü testi değildir. Gerçek emülatör oturumu/SDK + üretim dispatcher korumaları birlikte çalıştırılır; uygulamanın tüm Activity/ViewModel hesap geçişi akışı çalıştırılmaz.

## İlk deneme ve kapsam düzeltmesi

İlk koşuda hesap geçişi testleri geçti; ağ testi 'disableNetwork sonrasında transaction mutlaka başarısız olur' beklentisinde kaldı. Bu SDK/emülatör koşulunda transaction tamamlandı. Bu davranış gizlenmedi ve fiziksel bağlantı kesintisi geçmiş sayılmadı. Test, SDK'nın cache/pending yazı ve sunucu onayı davranışını doğrulayacak şekilde düzeltildi; üretim kodu değiştirilmedi ve testin adı kapsamına göre güncellendi. Gerçek taşıma bağlantısını keserek transaction zaman aşımı/sonradan commit riski sınanması açık iş olarak kaldı.

SDK disableNetwork API'sinin cache okuma ve yeniden ağ açılana kadar bekleyen yazı sözleşmesi: https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestore#disableNetwork() . Testte bu yerel ağ anahtarı, fiziksel internet veya USB bağlantı kesintisiyle eşdeğer kabul edilmiyor.

## Sırada ve sınırlar

Fiziksel/taşıma seviyesinde bağlantı kesintisi, transaction timeout ve geç commit; süreç ölümü ile kalıcı DataStore kuyruğunun SDK sonucuyla birlikte yeniden başlaması; zorlanmış transaction çatışma/retry ve hesap silme işareti yarışı hâlâ açık. Eski istemcinin yalnız ad yazıp eski kimlik haritasını bırakması için uyumluluk politikası da açık. Önceki 608 JVM, 13 UI/DataStore ve 24 JS kural testi bu tur tekrar çalıştırılmadı; üretim kaynakları/kurallar değişmedi. Canlı kurallar dağıtılmadı; yayın kapıları açık. Push/release yapılmadı.


## Devam — Gerçek DataStore kuyruğu ile SDK birlikte kabulü

Yeni durableDeletionReopensAfterCommitAndAccountSwitchWithoutPrematureRetry testi gerçek izole DataStore preferences_pb dosyası, CitySyncStore, CitySyncDispatcher, reserveAttempt/acknowledge/mergeRemoteCities ve gerçek Android Firestore transaction'ını birlikte kullanır. Yerel kuyruk bu senaryoda artık test değişkeni değildir.

A hesabında iki şehir SDK ile yazılıp gerçek sunucu görüntüsünden yerel depoya alınır. Bir şehir için silme isteği diske kaydedilir; gönderim öncesi belirsiz sonuç bekleme işareti reserveAttempt ile kalıcı tutulur. Uzak silme commit olur, onaydan önce gerçek emülatör Auth oturumu B'ye geçer. İşlem onaysız kalır; aynı revision ve retryNotBefore diskte bulunur. B'nin bağımsız yerel tercihi de oluşturulur.

DataStore scope kapatılıp aynı dosya yeni scope/factory ile açılır. Revision, bekleme sınırı, silinmiş yerel şehir durumu ve B tercihi korunur. B oturumunda A'nın dispatcher'ı gönderim yapmaz. A'ya girişte sunucudaki silmenin gerçekleştiği görülür; bekleme sınırı dolmadan CitySyncWaitingException alınır, yeni SDK gönderimi olmaz. Enjekte edilen test saati bekleme sınırına taşınınca olmayan kaydı silme idempotent biçimde başarılı olur. Onay işareti yalnız bu aşamada yazılır; gerçek SDK sunucu görüntüsü birleştirilince kuyruk temizlenir. İkinci disk yeniden açılışında temiz kuyruk, kalan şehir ve B tercihi doğrulanır.

Sonuç: npm run test:android SM-T500 Android 12 üzerinde 7/7 geçti; 0 hata/atlama. Önceki altı SDK kabul testi aynı koşuda tekrar geçti. Üretim kaynaklarında yeni bir hata bu senaryoda görülmedi; bu tur test kapsamı genişletildi. Önceki 608 JVM / 13 UI-DataStore / 24 JS kural testi tekrar koşulmadı. Emülatörler kapandı; koşucunun eklediği reverse portları kaldırıldı.

Kapsam sınırı: aynı Android test sürecinde DataStore dosyası kapatılıp açıldı; OS process kill, uygulama force-stop, gerçek reboot veya Room migration testi yapılmadı. Enjekte edilen wall clock ilerletildi; gerçek bekleme/reboot clock düzeltmesi bu SDK entegrasyonunda sınanmadı. Gerçek taşıma kesintisi/zaman aşımı, transaction retry çatışması ve hesap silme işareti yarışı hâlâ açık. Tam Activity/ViewModel açılışının bu kuyruğu nasıl sürdürdüğü ayrıca doğrulanmalı. Canlı Firebase/kurallar değiştirilmedi; push/release yapılmadı.


## Devam — Gerçek transaction çatışması ve SDK yeniden denemesi

actualConflictRetryRereadsIdentityBeforeDeleting testi eklendi. İlk SDK transaction okuması eşzamanlama bariyerinde tutulur. Aynı şehir ID'sine farklı koordinat/ülke içeren kayıt normal SDK yazısıyla değiştirilir; bu yazının sunucu onayı gelmeden eski transaction devam ettirilmez. İlk deneme eski görüntüyle yalnız bir silme aşamalar; SDK commit çatışması sonrası callback yeniden çalışır. Yeni okumada üretim stageCityDocumentMutation kimlik çakışmasını reddeder.

Test yalnız sonuç kaydına bakmaz: callback deneme sayısının en az iki olması, ilk denemede tam bir silme aşamalanması, yeni kimlik çakışmasının SDK exception cause zincirinde korunması ve kör manuel retry sunulmaması da kontrol edilir. Source.SERVER ile değişmiş konumun hâlâ aynen durduğu doğrulanır. Bu bir callback retry simülasyonu değil; gerçek Android SDK + yerel Firestore emülatörü üzerinde çakışma ile tetiklenen yeniden denemedir.

Son npm run test:android başarılı. XML sonucu tests=9, failures=0, errors=0, skipped=1: sekiz SDK senaryosu geçti. Ayrı prepare/resume koşucusu gerektiren processRestart yöntemi normal koşuda planlı atlandı; bu tur özel süreç koşusu tekrar çalıştırılmadı. Gradle konsolundaki 10/9 sayaç satırı test sayısı olarak kullanılmadı; XML ve yöntem sonucu esas alındı. Üretim helper bu yarışta doğru davrandı; bu tur üretim kodu/kurallar değiştirilmedi. Emülatörler durdu; geçici reverse eşlemeleri temizlendi.

Açık maddeler: fiziksel taşıma kesintisi/zaman aşımı ve geç commit, hesap silme işaretinin transaction ile yarışı, tam Activity/ViewModel yeniden açılışı, gerçek reboot/saat değişimi ve legacy ad-only uyumluluğu. Transaction çatışması sonrası yeniden kimlik okuma kabulü bu fixture kapsamı için tamamlandı; tüm eşzamanlılık senaryoları kapanmış sayılmaz. Önceki JVM/UI/JS kural testleri tekrar koşulmadı. Canlı dağıtım, release ve push yapılmadı.
