# 18 Kasım — Bekleme sonrası otomatik konum kurtarma

## Bulunan üretim eksikliği

ThemeViewModel giriş/açılışta kuyruk için bir defalık deneme yapıyordu. Reservation veya backoff henüz dolmamışsa CitySyncWaitingException oluşuyor; süre dolduğunda tekrar denemeyi başlatan başka zamanlayıcı bulunmuyordu. Önceki testler süreyi geçirip gönderimi kendileri çağırdığı için bu otomatik başlatma eksikliğini kapatmıyordu. Bekleme penceresinde kör manuel retry de kapalıydı.

## Düzeltme

CitySyncRecovery.kt ile hesap sahibine bağlı tek sıralı döngü eklendi. Auth oturumu değiştiğinde eski accountTasks scope iptal edilir; yeni döngü başlar. ViewModel temizlenirken döngü iptal edilir. Mevcut ilk açılış denemesi anında kalır; kurtarma döngüsü 30 saniye sonra IO dispatcher üzerinde devralır. Her tur en fazla 30 saniye, daha yakın süre varsa kalan süre kadar bekler. Yerel kuyruk kontrolü yapılır; boş/onaylı/bloke kuyruk için ağ isteği yapılmaz. WorkManager/kalıcı arka plan servisi veya alarm eklenmedi.

Hem şehir hem varsayılan konum kuyruğu okunur; retry clock gerekliyse mevcut kalıcı düzeltme yöntemi kullanılır. Karar elapsed/boot korumalı saatle verilir; duvar saati ileri alınması süreyi atlatmaz. Gönderim mevcut request gate, dispatcher, reserveAttempt ve revision onay yollarını kullanır. Onaylanmış/bloke işlemler gönderilmez, bloklar otomatik kaldırılmaz. Hesap silme kapısı kapalıysa kuyruk okunup yazılmaz; kapı bir gönderim sırasında kapanırsa etkin worker güvenle beklemeye geçebilir. Gerçek coroutine iptali veya hesap değişimi eski worker'ı durdurur.

Bir öğenin hatası diğer hazır öğeleri engellemez. Geçici okuma sorunu kontrollü aralıkla yeniden kontrol edilir; bozuk/tanınmayan kuyruk okuması otomatik hata döngüsüne sokulmaz. Mevcut hata politikası ve bloklar korunur. Bekleme açıklaması, uygulama çalışırken sürenin ardından yeniden deneneceğini belirtir.

## Doğrulama

Tam JVM koşusu: 615 test, 0 hata/atlama. Yedi yeni test elapsed saate rağmen wall jump, silme kapısı ve bloke/onaylı öğeler, okuma sırasında hesap değişimi, bir hatadan sonra başka hazır öğe, gerçek coroutine iptali, gönderim sırasında kapı kapanıp açılması ve guest/tanınmayan kuyruk güvenli duruşunu kapsar. Önceki bekleme metni testi yeni davranışa göre güncellendi.

Gerçek DataStore + Android SDK emülatör kabulü eklendi: şehir silme reservation'ı diske yazılır, store kapatılıp tekrar açılır. Üretim kurtarma döngüsü enjekte edilen bekleme/saat ile çalıştırılır. Her bekleme sırasında gönderim sayısı sıfırdır; elapsed süre tam dolunca yalnız bir SDK transaction gönderilir. Sunucu silmesi, onay ve snapshot birleştirmesi ile kuyruk temizliği doğrulanır. Bekleme süreleri sanal ilerletilir; test gerçek 38 dakikayı beklemez.

Normal SDK XML tests=12, failures=0, errors=0, skipped=3: dokuz normal senaryo geçti. ProcessRestart/TCP/ReplyLoss özel modları normal koşuda planlı atlandı; bu tur özel koşuları tekrar çalıştırılmadı. Konsoldaki 15/12 sayacı toplam olarak kullanılmadı. Derleme ve diff kontrolü geçti; emülatörler kapandı, reverse eşlemeleri kalmadı. Canlı Firebase kullanılmadı.

## Açık sınırlar ve sıra

ThemeViewModel bağlantısı üretim koduna eklendi ve derlendi; tüm gerçek Activity/login/sekme açılışı emülatörle uçtan uca çalıştırılmadı. SDK testi aynı üretim kurtarma yordamını çağırır ama bütün ViewModel'i oluşturmaz. Uygulama kapalıyken worker çalışması vaat edilmez; tekrar açılışta Auth yolu yeniden başlatır. Gerçek reboot/Doze ve cihazın gerçek bekleme süresi ayrıca kabul gerektirir. UI büyük yazı/görsel metin testi bu tur tekrar koşulmadı.

Sırada tam açılış/kurtarma ve Android hesap silme birleşik kabulü, yeni otomatik kurtarmanın özel süreç/ağ kaybı modlarıyla birlikte sınanması, legacy ad-only uyumluluğu ve önceki yayın kapıları var. Firebase kuralları/servisleri dağıtılmadı; push/release yapılmadı.
## Devam — Otomatik kurtarma ile gerçek ağ kaybının birlikte kabulü

Önceki özel TCP/yanıt kaybı testleri bekleme sonrasındaki son gönderimi kendileri çağırıyordu. İkisi de üretim runCitySyncRecovery döngüsünü kullanacak şekilde genişletildi. Fixture store ve dispatcher artık aynı enjekte edilen elapsed/boot saatini kullanır; reservation diskte bu saatle tutulur. Ortak recoverCityQueueAutomatically yardımcısı yalnız sanal beklemeyi ilerletir; son retry doğrudan testten çağrılmaz.

Her beklemede gönderim sayısının sıfır olması, aralığın 1–30 saniye olması, toplam sanal beklemenin retrySecondsLeft ile tam eşleşmesi ve süreden sonra yalnız tek otomatik send çağrılması zorunludur. O gönderim mevcut dispatcher, reserveAttempt ve gerçek Android SDK transaction yolunu kullanır. Onay ve sunucu görüntüsüyle birleştirme sonucunda kalıcı kuyruk boşalmalıdır.

Yanıt kaybı birleşimi: ayrı SDK gözlemcisi uzak commit'in tamamlandığını doğrular; kayıp yanıt nedeniyle onaysız kuyruk korunur. Üretim kurtarma döngüsü süreden sonra idempotent onayı başlatır. İlk gerçek silmeden sonra ikinci bir delete aşamalanmaz.

Tam TCP kesintisi birleşimi: ilk SDK görevi timeout olur ve eski işlem delete aşamalamaz. Dönüşte kuyruk/reservation korunur; üretim döngüsü bekleme sonrasında yeni etkin istekle gerçek silmeyi tamamlar. Toplam yalnız bir delete aşamalanır.

Doğrulama: npm run test:android:reply-loss ve npm run test:android:transport ayrı koşularda 1/1 geçti; ikisinde 0 hata/atlama. İlk koşunun yöntem adı ve XML sonucu ikinci koşudan önce doğrulandı; son TCP XML'i de ayrıca doğrulandı. Emülatörler/proxy kapandı; test dinleyicileri ve reverse eşlemeleri kalmadı. Üretim kodu bu tur değiştirilmedi. Önceki 615 JVM ve dokuz normal SDK senaryosu bu tur tekrar çalıştırılmadı; sadece değiştirilen iki özel ağ modu koşuldu.

Sınırlar: sanal saat ve bekleme bağımlılığı kullanıldı, gerçek 38 dakikalık bekleme geçirilmedi. Core kurtarma yordamı gerçek SDK ve DataStore ile birlikte çalıştırıldı; tam Activity/ThemeViewModel oluşturulmadı. Özel süreç yeniden başlatması ile bu yeni otomatik döngünün birleşimi, tam ekran açılışı, Android hesap silme birleşimi, reboot/Doze, üretim servis kabulü ve legacy ad-only uyumluluğu hâlâ açık. Push/release/canlı dağıtım yapılmadı.
## Devam — Tek gönderim iptali ve kuyruk revizyon yarışı

Kod incelemesinde yeni bir durma yolu bulundu: CitySyncRecovery send geri çağrısından CancellationException aldığında hesap coroutine'i hâlâ aktif ve UID aynı olsa da, silme kapısı o anda açık görünüyorsa hatayı tekrar fırlatıp bütün worker'ı bitiriyordu. Tek isteğin iptali veya kısa süre kapanıp açılan kapı tüm otomatik kurtarmayı durdurmamalı.

Düzeltme: catch içinde gerçek parent Job iptali ensureActive ile yine yayılır; owner değişmişse worker yine durur. Hesap görevi aktif ve owner aynıysa yalnız o tur kesilir, kontrollü beklemeden sonra kuyruk taze okunur. Blok, reservation, saat ve onay mantığı değiştirilmedi; bağımsız istek iptali ağ hatası gibi kullanıcıya yayımlanmaz. Yeni JVM testi, kapı açıkken tek gönderimin kendini iptal etmesinden sonra aynı aktif hesap worker'ının sonraki turda ilerlediğini doğrular. Önceki gerçek coroutine iptali/gate kapanması testleri de tekrar geçti.

Yeni Android SDK testi recoverySnapshotCannotSendSupersededRevisionOfAnotherCity: kurtarma listesi iki şehrin ilk görüntüsünü alır. İlk SDK gönderimi tamamlandıktan sonra, ikinci şehir DataStore'da silinip aynı kimlik/koordinat ve güncellenmiş metadata ile yeniden kaydedilir. Kurtarma listesi hâlâ eski ikinci revision'ı taşır. Mevcut üretim dispatcher okuması güncel kuyruğu yeniden okuyarak yeni revision ve payload'ı gönderir; eski revision hiç SDK gönderimine girmez. Gerçek Source.SERVER görüntüsü yeni metadata'yı doğrular; sunucu snapshot birleştirmesiyle her iki onaylı kuyruk kaydı temizlenir. Burada eski snapshot'ın kullanılmaması mevcut dispatcher korumasıyla doğrulandı; bu koruma değiştirilmedi.

Tam JVM XML sonucu 616 test, 0 hata/atlama. Normal SDK XML tests=13, failures=0, errors=0, skipped=3: on normal senaryo başarılı; özel ProcessRestart/TCP/ReplyLoss modları normal koşuda planlı atlandı. Üç özel mod bu tur ayrıca çalıştırılmadı. Konsoldaki 16/13 sayacı toplam alınmadı; XML ve yeni yöntem sonucu esas alındı. Debug derleme başarılı; emülatörler ve reverse eşlemeleri kapandı.

Tam Activity/ViewModel kullanıcı akışı, yeni iptal davranışının tüm gerçek hesap silme/Doze/reboot koşulları ve canlı hizmet/mağaza kabulü açık kalır. SDK testi üretim recovery/dispatcher/store yardımcılarını birlikte kullanır; tam ViewModel'i oluşturmaz. Canlı Firebase kuralları değiştirilmedi veya dağıtılmadı. Push/release yapılmadı.