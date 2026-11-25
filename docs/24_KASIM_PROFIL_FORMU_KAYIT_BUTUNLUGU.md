# 24 Kasım 2026 Profil formunda kayıt bütünlüğü

Profil düzenleme formu, üretim Auth/Profile ViewModel'leri, gerçek yerel Firebase Auth/Firestore ve QA DataStore ile birlikte test edildi. Canlı servis veya kullanıcı hesabı kullanılmadı.

## Düzeltilenler

EditProfileScreen, kaydı verilen ProfileViewModel ile yaparken görüntülenen profili global UserProfileRepository'den okuyordu. Form artık aynı ViewModel'in ProfileState.Success durumunu kullanır. Kayıt düğmesi profil mevcut hesabın UID'siyle eşleşmeden etkinleşmez; profil yükleme hatasında eski hazır durumla kayıt başlatılmaz. Form taslağı hesap değişiminde hâlâ owner anahtarıyla ayrılır.

Yerel profil ad/biyografi yazıcısı güvenlik kontrolü yazıyı atlasa da normal dönebiliyordu. saveProfileBasics artık DataStore düzenlemesinin gerçekten uygulandığını doğrular; uygulanmayan yazı üst kayıt akışında başarı sayılmaz. Üretim sarmalayıcısı ve test yolu aynı UID/engel/yazı nesli kontrolü ve alan eşlemesini kullanır.

ProfileViewModel'e internal profil yazıcısı ve SDK öncesi bekletme noktası eklendi; üretim varsayılanları korunur. İzole debug Activity gerçek EditProfileScreen kullanır, varsayılan Firebase fabrikasına veya silme ekranına erişmez ve release paketine dahil olmaz.

## Tablet doğrulaması

Komut: `npm run test:android:profile-edit`.

SM-T500 Android 12 üzerinde tek birleşik senaryo XML sonucu: 1 test, 0 failure/error/skip.

- Form başlangıçtaki A profilini ve e-postasını doğru SDK/ViewModel kaynağından gösterdi.
- Gerçek Firestore yazısı sunucuda doğrulandıktan sonra SDK adımı bekletildi. Kayıt Saving kaldı ve geri/başarı çağrısı yapılmadı.
- Gerçek Activity recreate sırasında aynı modeller ve düzenlenen metin korundu.
- SDK ad güncellemesinden sonra enjekte edilen yerel I/O hatasında form kapanmadı, hata gösterildi ve metin korundu.
- Yerel yazma korumasının reddi de başarı/geri çağrısı üretmedi; yerel alanlar yazılmadı.
- Engel kaldırılıp tekrar denendiğinde sunucu, SDK displayName ve UID'ye bağlı yerel ad/biyografi doğrulandı; başarı bir kez bildirildi.
- Sonraki A kaydı sunucu aşamasını geçtiğinde hesap B'ye değiştirildi. A'nın kalan SDK/yerel yazıları ve eski başarı bildirimi durdu; B formu kendi profilini gösterdi. B SDK adı ve yerel tercihleri A taslağını almadı.

İlk koşu yeni JUnit metodunun Unit dönüş türü eksik olduğu için initializationError aldı; test imzası düzeltildikten sonra senaryo geçti. Bu uygulama çökmesi değildir.

## Kapsam ve sıradaki kontroller

Test, açık profil kaydı yolundaki üç yazıyı ayrı doğrulamak için otomatik profil snapshot yazıcısını devre dışı bırakır; snapshot senkronizasyonu önceki ayrı kabullerde izlenir. Fixture geri hareketini sayaçla kaydeder; tam ürün navigasyon yığını kabulü değildir. I/O hatası kontrollü enjeksiyondur, fiziksel depolama arızası değildir.

Sunucu yazısı tamamlandıktan sonra iptal edilen A kaydının uzak değişikliği geri alınmaz. Üç servis tek atomik işlem değildir; yalnız SDK/yerel devamın ve yanlış başarı/hesap sızıntısının engellendiği doğrulandı. Sunucuda henüz onaylanmamış çevrimdışı yazıların kabul/ret ve süreç toparlanması ile tam launcher akışı açık kalır. Sunucuda tamamlanmış profilin süreç sonrası uzlaştırması aşağıdaki bölümde ayrıca izlenir. Önceki aralıklı Activity STOPPED→RESUMED hatasının kesin nedeni ayrıca açıktır.

Son kaynakla Android derlemesi ve 681 birim testi de geçti: 0 failure/error/skip. Demo emülatörler ve sahip olunan ADB reverse bağlantıları kapatıldı.

## Sunucuda tamamlanmış profilin yeniden açılışta uzlaştırılması

Önceki akış profil snapshot'ını yerel tercihlere taşıyor, ancak SDK displayName eski kaldığında yeniden açılışta bunu düzeltmiyordu. ProfileReadState.Ready artık snapshot'ın sunucudan geldiğini ve bekleyen yazı içermediğini serverConfirmed ile ayırır. ProfileViewModel, aynı kabul edilmiş hesapta sunucunun onayladığı geçerli ad farklıysa SDK adını uzlaştırır. Firestore'a eski taslak tekrar gönderilmez.

Önbellek veya bekleyen yerel yazı SDK adını değiştiremez. Aktif profil kaydı, başka hesap, devam eden SDK görevi, boş veya 50 karakter sınırını aşan ad otomatik SDK yazısını başlatmaz. Uzlaştırma sırasında form yeni kaydı başlatmaz; önceki SDK görevi hâlâ sürüyorsa kullanıcıya yeniden deneme durumu verilir. Eski hesabın iptal edilen işlemi yeni hesabın bekleme durumunu kapatamaz. Uzlaştırma Saved üretmez ve ekranı otomatik kapatmaz.

İşlem 20 saniyeyle sınırlıdır. SDK başarısızlığı sunucu profilini gizlemez; yenileme veya yeni sunucu olayı tekrar deneyebilir. Otomatik periyodik arka plan kuyruğu eklenmedi. Yerel profil snapshot'ları aynı hesap için çevrimdışı görüntülemeyi sürdürebilir; SDK adına yetki veren kaynak değildir.

Yeni `test:android:profile:restart` senaryosu, gerçek disk Firestore önbelleğiyle iki instrumentation süreci kullanır. prepare: sunucuda yeni ad/biyografi onaylanır, SDK öncesinde kayıt bekletilir; modeller kapatılıp yerel ad kasıtlı eski değere alınır. QA force-stop sonrasında resume: farklı PID ve eski SDK adı doğrulanır, Firestore ağı kapalıyken diskteki yeni profil okunur ve SDK adının değişmediği kontrol edilir. Bağlantı açılınca sunucu onayıyla SDK ve yerel profil eşitlenir. Daha yeni sunucu adı da korunur; createdAt alanı değişmez.

Bu test, SDK/Auth ağının tamamen kesilmesi veya sunucuda onaylanmamış bir yazının yeniden gönderilmesi testi değildir. Donanım güç kesintisi, tam ürün Activity taslağı ve çok cihazlı eşzamanlı çakışma kabulü ayrıca açık kalır.

Son kaynakla süreç senaryosunun prepare ve resume aşamalarının her biri OK (1 test) döndürdü; farklı PID, disk önbelleği, çevrimdışı SDK değişmeme, sunucu onayıyla eşitleme ve daha yeni sunucu değerinin korunması geçti. Ardından birleşik profil formu regresyonu da 1 test, 0 failure/error/skip ile geçti. Form testi B hesabının SDK adının artık kendi onaylı B profiliyle eşleşmesini doğrular; A değerini almaması kontrolü korunur. Bu iki mantıksal senaryodur; süreç aşamaları bağımsız bug veya senaryo sayılmamalıdır.

Son kaynakla 685 JVM testi de 0 failure/error/skip ile geçti (dört yeni uzlaştırma kuralı testi dahil). Emülatörler ve sahip olunan ADB reverse bağlantıları kapatıldı. Sıradaki: henüz sunucuda onaylanmamış çevrimdışı profil yazısının kabul/ret, yeniden bağlanma ve süreç kapanması davranışı. Önceki aralıklı Activity kök nedeni ve tam mağaza kabulü açık kalır.

## Çevrimdışı profil yazısının kabul ve ret kontrolü

Firestore'un gecikme telafisiyle sunduğu henüz onaylanmamış profil snapshot'ı, önceki ProfileViewModel yolunda cihaz tercihlerine yazılabiliyordu. Sunucu reddinde bu geçici taslak kalıcı ad/biyografi gibi kullanılabilirdi. Ready artık hasPendingWrites bilgisini taşır; profil gözlemcisi pending snapshot'ı yerel profil tercihlerine aktarmaz. Formun taslağı ve Firestore'un kendi çevrimdışı kuyruğu korunur. Sunucu onayı gelince normal snapshot/SDK uzlaştırması devam eder.

`test:android:profile:offline` gerçek EditProfileScreen, Auth/Profile ViewModel, demo SDK, QA DataStore ve uygulamanın gerçek Firestore kurallarıyla iki senaryo çalıştırır. Kabul senaryosu çevrimdışı yazının 20 saniyede zaman aşımına uğramasını, onaylı yerel ad/SDK adının korunmasını, geç sunucu kabulünün otomatik Saved/geri hareketi üretmemesini ve açık tekrar denemeyi kontrol eder. Ret senaryosu, test yardımcı servisinin yerel demo projesine koyduğu geçici account_deletions işaretiyle yazıyı kurala reddettirir; onaylı tercihler/SDK adı ve form taslağı korunur. İşaret kaldırılıp profil yenilendikten sonra aynı taslak yeniden kaydedilir.

Yardımcı servis yalnız 127.0.0.1:9397 üzerinde rastgele nonce ile sabit block/unblock işlemlerini kabul eder; key/credential yüklemez veya serbest belge yazma arayüzü sunmaz. Test sonunda kendi geçici işaretlerini temizler. Gerçek hesap silme API'si kullanılmaz ve üretim kuralları değiştirilmez.

Firestore ağı SDK üzerinden kapatılır; Auth ağı ve cihazın tüm bağlantısı kesilmez. Sunucuda henüz onaylanmamış yazı sırasında gerçek süreç ölümü/yeniden açılış ve çok cihaz çatışması bundan ayrı kabul konularıdır. Önceki aralıklı Activity kök nedeni açık kalır.

Son çevrimdışı koşu XML sonucu: 2 test, 0 failure/error/skip. Geç kabul senaryosu 20 saniyelik gerçek kayıt zaman aşımını bekledi; bu sırada onaylı yerel ad ve SDK adı değişmedi. Sunucu kabulünden sonra snapshot/SDK eşitlendi ancak eski form isteği Saved veya geri çağrısı üretmedi. Açık tekrar deneme başarıyı bir kez bildirdi. Ret senaryosunda gerçek kurallar yazıyı reddetti; yerel ad/biyografi ve SDK adı korundu. Engel kaldırılıp profil yenilendikten sonra aynı form taslağı sunucu/SDK/yerel tercihlere kaydedildi ve createdAt korundu.

İlk derleme eksik üretilmiş annotationProcessors.json girdisi nedeniyle testlere ulaşamadı. İlgili javaPreCompileDebugAndroidTest görevi yeniden çalıştırılarak dosya Gradle tarafından üretildi; mevcut QA init dosyası kullanıldı. İlk cihaz paketinde ret testi geçti; kabul testi çevrimdışı adıma geçmeden başlangıç profilini beklerken zaman aşımına uğradı. Beklemeler Compose saatiyle eşlendi, onaylı snapshot yazısının bitiş sayacı ve hata bağlamı eklendi; son tekrar iki testi birlikte geçti. İlk başlangıç beklemesinin kesin nedeni ayrıca kanıtlanmış değildir; geçen tekrar tek başına genel Activity/başlangıç kararlılığını kapatmaz.

Son kaynakla onaylı profil süreç toparlanma regresyonunun prepare/resume aşamaları da geçti; runner iki aşamada OK (1 test) kontrolünü tamamladı. Ardından 685 JVM testi 0 failure/error/skip ile geçti. Çevrimdışı kabul/ret iki ayrı senaryo, süreç toparlanması tek senaryonun iki aşamasıdır. Yardımcı servis, demo emülatörler, geçici kural işaretleri ve sahip olunan ADB reverse bağlantıları temizlendi. Sıradaki: sunucu onayı bekleyen kayıt sırasında gerçek süreç kapanması ve A–B hesap değişimi. Önceki Activity/başlangıç kararlılığı kök nedenleri ve tam mağaza kabulü açık kalır.

## Onay bekleyen profil kuyruğu süreç ve hesap değişiminde doğrulandı

`npm run test:android:profile:pending-restart` SM-T500 Android 12 üzerinde prepare/resume/switchback aşamalarının her birinde OK (1 test) döndürdü. Tek mantıksal senaryo üç farklı PID'de çalıştı; üç ayrı bug veya bağımsız senaryo değildir.

prepare aşamasında A profili Firestore ağı kapalıyken gerçek disk mutation kuyruğuna alındı. Snapshot ve Source.CACHE hasPendingWrites bilgisi doğrulandı; SDK adı ve onaylı DataStore adı eski onaylı değerde kaldı. Uygulama süreci durduruldu.

resume aşamasında A'nın aynı bekleyen kaydı diskten geri geldi. Çevrimdışıyken açık girişle B'ye geçildi ve yalnız B oturumunda Firestore bağlantısı açıldı. B'nin sunucu ad/biyografisi, SDK adı ve UID'ye bağlı tercihleri korundu; A'nın eski kaydı B'ye taşınmadı. İkinci süreç durdurması yapıldı.

switchback aşamasında B SDK oturumu ve atomik kabul kaydı geri geldi. A'ya çevrimdışı dönülünce aynı A pending kaydı hâlâ bulundu; bağlantı açılınca yalnız A altında onaylandı. SDK ve yerel ad/biyografi eşitlendi; eski form isteği Saved üretmedi. A createdAt=123 ve B createdAt=456 korundu. B'ye tekrar giriş ve sunucu okuması, B verilerinin değişmediğini doğruladı.

Bu adımda üretim Kotlin kaynağı değiştirilmedi ve yeni üretim hatası bulunmadı. Yeni model/SDK regresyon testi ve çalıştırıcı modu eklendi. Önceki birim testi tabanı 685/685'tir; değişmeyen üretim kaynakları için bu tur yeniden çalıştırılmadı. Auth/Profile ViewModel, gerçek disk Firestore cache, QA DataStore ve UID'ye bağlı atomik oturum dosyası birlikte kullanıldı.

Activity/launcher/form taslağının görsel kabulü, donanım güç kesintisi, tüm cihaz/Auth ağının kesilmesi veya çok cihazlı eşzamanlı çakışma kabulü değildir. Yalnız QA paketi force-stop edildi. Kendi named SDK cache'i, iki test UID'sinin tercihleri, metadata/oturum dosyaları, demo emülatörler ve sahip olunan ADB reverse bağlantıları temizlendi.

Sıradaki: onay bekleyen yazıyla yeniden açılış sonrasında sunucu reddi/oturum geçersizliği. Önceki aralıklı Activity/başlangıç kök nedenleri ve tam mağaza kabulü açık kalır.

## Süreç sonrasında reddedilen bekleyen yazı tekrar gönderilmedi

`npm run test:android:profile:pending-reject` SM-T500 Android 12 üzerinde prepare/resume/verify aşamalarının her birinde OK (1 test) döndürdü. Tek mantıksal senaryo üç farklı PID'de çalıştı.

prepare aşamasında profil değişikliği Firestore ağı kapalıyken gerçek SQLite mutation kuyruğuna yazıldı. Süreç durduruldu. resume aşamasında diskten pending taslak geri geldi; yerel onaylı ad/biyografi ve SDK adı değişmemişti. Yalnız demo projeye geçici account_deletions işareti konuldu ve bağlantı açıldı. Üretim kuralları yazıyı ve profil erişimini reddetti. SDK pending kuyruğu tamamlandı; cache dokümanında hasPendingWrites=false ve eski onaylı ad doğrulandı. Yanlış Saved üretilmedi.

Erişim işareti kaldırılıp profil yenilendiğinde sunucu ad/biyografisi ve createdAt=123 değişmemişti. İkinci süreç durdurmasından sonraki verify aşamasında kuyruk hâlâ boştu; bağlantı yeniden açılınca reddedilen taslak tekrar gönderilmedi. SDK ve yerel onaylı ad korundu.

Bu adımda yeni üretim hatası bulunmadı; üretim Kotlin ve Firebase kural kaynakları değiştirilmedi. Yeni model/SDK regresyonu ve çalıştırıcı modu eklendi. Önceki birim testi tabanı 685/685'tir; değişmeyen üretim kaynakları için bu tur yeniden çalıştırılmadı. Kendi named cache'i, UID tercihleri, UUID metadata/oturum dosyaları, geçici demo erişim işareti, emülatörler ve sahip olunan ADB reverse bağlantıları temizlendi.

Bu bir Firestore erişim iptali testidir. Firebase Auth hesabı devre dışı bırakılmadı, gerçek refresh token revocation veya kullanıcı silme API'si çağrılmadı. Tam Activity/launcher, form taslağının süreçler arasında görsel geri yüklenmesi, donanım güç kesintisi ve bütün cihaz ağı kesintisi kabulü değildir.

Sıradaki: sunucu taraflı Firebase Auth oturum geçersizliği ve yeniden giriş/toparlanma. Önceki Activity/başlangıç kararlılığı kök nedenleri ve mağaza kabulü açık kalır.


Güncelleme: Auth disable/enable ve ön plana dönüş adımı ayrıca tamamlandı; güncel sonuç ve kalan token iptali kapsamı için [Auth ön plan raporu](24_KASIM_AUTH_ON_PLAN_DOGRULAMA.md).
