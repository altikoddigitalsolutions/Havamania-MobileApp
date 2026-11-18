# 18 Kasım — Yeni Android sürecinde şehir kuyruğu kurtarma

## Tamamlanan kabul

npm run test:android:restart eklendi. Yalnız yerel demo Auth/Firestore emülatörleri, QA com.havamania paketi ve rastgele senaryo kimliğiyle çalışır. Koşucu debug/test APK'larını üretir, verileri silmeden install -r ile yükler, aynı test yöntemini prepare ve resume olarak iki ayrı instrumentation çağrısında çalıştırır. Arada yalnız com.havamania için am force-stop yapar ve pidof ile çalışan QA süreci olmadığını denetler. Play com.havamania.mobile paketi durdurulmaz; pm clear/uninstall yoktur.

Prepare aşaması gerçek CitySyncStore/DataStore dosyasına silme ve gönderim öncesi rezervasyon kaydını yazıp gerçek SDK transaction ile uzak silmeyi tamamlar. Yerel onay verilmez. UID, revision, retry sınırı ve ilk PID yalnız testin private metadata dosyasına yazılıp fsync edilir. DataStore ve senaryoya özel named FirebaseApp bilerek açık bırakılır; iki aşama arasında varsayılan uygulamanın Firebase oturumuna müdahale edilmez.

Resume aşaması yeni PID'yi ve aynı emülatör Auth UID'sini doğrular. Kuyruğun revision/retry sınırı, silme durumu ve yerel kalan şehir diskten geri okunur. Sunucuda silmenin zaten gerçekleştiği görülür. Bekleme sınırından önce dispatcher gerçek gönderim yapmaz. Enjekte edilen test saati sınıra getirilince olmayan kaydın transaction silmesi idempotent başarılı olur; onay yazılır ve gerçek SDK sunucu görüntüsüyle kuyruk temizlenir. Başarıda yalnız bu senaryonun dosyaları ve named emülatör oturumu temizlenir.

## Kanıt

İki aşamalı koşu başarılı: prepare OK (1 test), resume OK (1 test), script çıkışı 0. Resume'daki assertNotEquals eski/yeni PID'yi denetledi; aynı UID/revision/retry kayıtları doğrulandı. Bu, aynı süreçte yalnız DataStore kapatıp açma testinden farklıdır: iki ayrı Android instrumentation süreci ve arada QA force-stop uygulanmıştır. Yazının tam ortasında SIGKILL veya güç kaybı simüle edilmemiştir.

Kurulum/temizlik kodu da değiştiği için npm run test:android yeniden çalıştırıldı. XML sonucu: tests=8, failures=0, errors=0, skipped=1. Önceki 7 SDK senaryosu geçti; süreç yöntemi yalnız kendi senaryo argümanlarıyla çalıştığından normal koşuda planlı atlandı. Gradle konsolundaki 'Finished 9 tests' satırı toplam olarak kullanılmadı; yöntem bazlı XML 8 test/1 atlama gösteriyor. Özel testin başarısı normal koşunun atlamasına değil ayrı prepare/resume çıktısına dayanır. Koşu sonunda emülatörler kapandı, reverse --list boştu.

## Kapsam ve kalanlar

Bu test üretim CitySyncStore/dispatcher yardımcıları ve SDK ile çalışır, ancak tüm Activity/ViewModel açılış akışını başlatmaz. Kullanıcı ekranının otomatik kuyruğu sürdürmesi bu testle tamamlanmış sayılmaz. Gerçek reboot, boot/monotonic saat değişimi, force-stop sırasında yarım DataStore yazısı veya SDK commit'i, fiziksel taşıma kesintisi ve transaction çatışma/retry açık. Bekleme sınırı diskten geri okunur ama süreyi geçirmek için test saati enjekte edilir.

Üretim Kotlin ve Firebase kuralları bu tur değiştirilmedi; test ve koşucu geliştirildi. Önceki 608 JVM / 13 UI-DataStore / 24 JS kural testi tekrar koşulmadı. Canlı Firebase/kurallar dağıtımı yok; eski istemci ad-only uyumluluğu ve mağaza kabul kapıları hâlâ açık. Push/release yapılmadı.

## Devam — Yeni süreçte otomatik kurtarma birleşimi

İki aşamalı processRestartMaintainsDeletionFence testi üretim runCitySyncRecovery döngüsüne bağlandı. Önceki testte son gönderim elle çağrılıyordu; artık ortak kurtarma yardımcısı yalnız sanal beklemeyi ilerletir, son send'i üretim döngüsü başlatır.

Prepare gerçek CitySyncClocks.capture ile cihazın wall/elapsed/boot saatini alır, DataStore reservation'ını bu saatle kaydeder ve uzak silmeyi onaydan önce tamamlar. Named emülatör oturumu ve test dosyası iki süreç arasında korunur. Resume farklı PID ve aynı UID, revision, retry sınırı ve onaysız silme durumunu doğrular. Saatin boot/elapsed koruması gerekliyse mevcut store.refreshRetryClock yolu uygulanır. Erken gönderim engellenir; kalan süre sanal olarak ilerletildiğinde döngü yalnız bir idempotent SDK gönderimi yapıp onay/snapshot ile kuyruğu temizler.

npm run test:android:restart: prepare OK (1 test), resume OK (1 test), çıkış 0. İki aşama da başarılı. Üretim kodu bu tur değişmedi; test kapsamı genişletildi. Önceki 615 JVM, dokuz normal SDK, ağ modları ve JS kural testleri bu tur tekrar koşulmadı. Gerçek reboot yapılmadı; gerçek saat/boot ilk durumu alındı ama bekleme süresi test bağımlılığıyla ilerletildi. Tüm Activity/ViewModel oluşturulmadı; yalnız yeni süreç + SDK/DataStore + üretim kurtarma yordamı birlikte doğrulandı. Süreç kapanması sırasında yarım fiziksel disk yazısı ve tam açılış/Doze kabulü ayrı açık kalır. Canlı dağıtım/push/release yapılmadı.