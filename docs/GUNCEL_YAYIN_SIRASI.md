# Güncel yayın sırası — 11 Kasım 2026

4 Kasım tarihli ana kontrol listesi sayımı 59 geniş kabul maddesinin 22’sini tamamlandı, 37’sini açık gösteriyordu. Bu tarihsel sayım güncel bug veya kalan adım sayısı değildir; aşağıdaki yeni alt kabuller de ayrıca izlenir. Son yerel alt düzeltmeler canlı/çok cihazlı/yayın kontrollerini kendiliğinden kapatmaz. Ayrıntılı kaynak: YAYIN_ONCELIKLERI.md ve YAYIN_DURUM_RAPORU.md.

## Beş kullanıcı bildirimi için açık kapanış işleri

Bu alt liste mevcut sekiz çalışma paketine bağlıdır; aynı sorunlar yeni bug olarak tekrar sayılmaz. Kodun eklenmesi, cihaz kabulü ve canlı yayın ayrı durumlardır. Yerel iş sırası aşağıdadır; canlı işler mevcut yalnızca yerel test sınırının dışına çıkılmadan tamamlandı işaretlenmez.

- [ ] **B5-1 — Otomatik rehberin tam takvim akışı (paket 3).** Bir sonraki yerel iş: izole test verisiyle telefonda ve tablette seyahat oluştur; başarıdan sonra ek dokunuş olmadan doğru seyahatin rehberinin açılıp görünür alana kaydığını doğrula. Yeni kayıt, düzenleme, farklı tarih/filtre, çift veya geçersiz kayıt, çevrimdışı kayıt, geri hareketi ve hesap değişimi kontrol edilsin. Başarısız kayıtta veya eski hesap cevabında rehber açılmamalı. 11 Kasım yerel devamında gerçek takvim/form/ViewModel/Room ile 7/7 cihaz testi ve 457 JVM testi geçti; 390 dp telefon düzeni, tablet, arşiv filtresi, düzenleme, çift/geçersiz kayıt ve eski odak yarışları doğrulandı. Fiziksel telefon, gerçek Firebase A→B oturum değişimi ve süreç yeniden oluşturma kabulü açık. Ayrıntı: 11_KASIM_OTOMATIK_REHBER_TESTI.md. Sonraki bağımsız yerel iş B5-2.
- [ ] **B5-2 — Konumdan hava ekranına uçtan uca dayanıklılık (paket 5).** Gerçek tablette yaklaşık konumla şehir çözümü geçti; bu sonuç korunur. İzin penceresinden hava ekranına ilk kurulum akışı, hassas/yaklaşık izin, reddetme/kalıcı ret, GPS kapalı, konum bulunamaması, çevrimdışı, ayarlardan izin verme ve hesap değişimi ayrı doğrulansın. Otomatik şehir ile tahmin aynı konuma ait olmalı; izin yokken manuel şehir seçimi erişilebilir kalmalı. 11 Kasım devamında ret/ayarlar/GPS açıklamaları, tek arama, ilerleme, manuel geri dönüş ve UID bazlı süreli otomatik şehir önbelleği eklendi. 466 JVM testi hatasız; son toplu cihaz turu 9/10, yaklaşık şehir çözümü tekil tekrar 1/1 geçti. GPS yeniden açıldıktan hemen sonraki ilk konum sonucunun bir kez alınamaması açık bulgu olarak korunur. Tam HomeScreen–hava servisi çevrimdışı soğuk açılış ve gerçek oturum/telefon/süreç kabulü açıktır. Ayrıntı: 11_KASIM_KONUM_DAYANIKLILIK_TESTI.md. Bir sonraki yerel iş bu ilk konum/çevrimdışı tam ekran kabulüdür.
- [ ] **B5-3 — Tüm ekranlarda görsel kapanış (paket 3).** Ortak kartlar ve hava ölçümleri düzeltildi; özel Surface/kart kullanan ekranlar dahil ekran matrisi tamamlanmalı. Şehir ve seyahat yok/var, yükleme/hata/çevrimdışı durumları; telefon/tablet, açık/koyu/mevsimsel tema, yatay, klavye ve büyük yazı kontrol edilsin. İç içe gereksiz çerçeve, taşma, düşük kontrast ve yanlış dokunma alanı kalmadığı ekran bazında kaydedilmeli. Genel görsel kabul açık.
- [ ] **B5-4 — Asistan izinlerinin canlı hesap kabulü (paket 1; canlı ortam bağımlılığı).** Yerel kontrolcü/dokunma testleri geçti. Bilgisayardan DNS ve kimliksiz HTTP erişimi doğrulandı (401); bu tablet veya kimlikli işlem kabulü değildir. Yetkili test ortamı/hesabında uygulamaya girişten izin metni alma, onay kaydı, yeniden açılışta kabulü koruma, geri çekme, bağlantı kesintisinde bekleyen geri çekme ve gönderimin engellenmesi uçtan uca doğrulanmalı. Gerçek kullanıcı hesabının izinleri yerel test kapsamında değiştirilmez.
- [ ] **B5-5 — Güncellenebilir rehberin canlıya alınması (paket 4/7; yayın bağımlılığı).** Firestore kuralları doğru projeye dağıtılmalı ve doğrulanmış sürümlü içerik yetkili yayın aracıyla yayımlanmalı. Daha yeni sürüm açık rehbere yansımalı; eski/yanlış konumlu/geçersiz/süresi dolmuş içerik reddedilmeli. Yeniden açılışta önbellek, çevrimdışı kullanım ve süre dolumu cihazda doğrulanmalı. Editoryal sorumlu ve güncelleme takvimi belirlenmeli; otomatik veri çekme tek başına yeni içerik üretmez. Altyapı hazır, canlı yayın yapılmadı. Yayın yönergesi: REHBER_YAYINLAMA.md.
- [ ] **B5-6 — Yayınlanan uygulamada son kabul (paket 8).** Bu düzeltmeleri içeren yeni imzalı sürümle Google Play iç testinde beş akış tekrar doğrulanmalı. Mevcut Play kurulumu ve önceki AAB bu yerel kaynak değişikliklerini içeriyor sayılmaz.

Kanıt ve sınırlar: 11_KASIM_BES_KONTROL.md. Türkiye il/ilçe içerik kapsamı, ilgi alanlarına göre öneri seçimi ve canlı etkinlik uygunluğu mevcut rehber kuyruğunda ayrıca açıktır; bu altı kapanış işine dahil edilerek kaybolmaz.

## Ana çalışma paketleri

| Öncelik | Çalışma paketi | Açık kapanış koşulu |
|---|---|---|
| 1 | Asistan izinleri ve platform bağlantısı | 11 Kasım yerel düğme testleri geçti; bilgisayardan DNS ve kimliksiz izin uç noktası erişimi doğrulandı (401). Tablette kimlikli canlı izin metni, kayıt/geri çekme ve asistan istek sözleşmesi kabulü açık (B5-4). |
| 2 | Bildirim dayanıklılığı | Aktif teslim sırasında liste temizleme yarışı yerelde doğrulandı. Dokunma sonrası okundu ve sayaç davranışı yerelde doğrulandı. Gerçek tepsi, izin, kanal ve Activity yaşam döngüsü açık. |
| 3 | Tüm ekran ve durumların görsel denetimi | İlk açılışta Hava ekranının takılması ve Takvim’e geçişte iki ekran görünümü öncelikli alt maddeler. Ayrıca boş/dolu/yükleme/hata ve çevrimdışı durumlar, telefon/tablet, açık/koyu/mevsimsel tema, yatay kullanım, klavye, büyük yazı, TalkBack, yazım, tipografi ve geçişler. |
| 4 | Hesap, Firebase ve senkronizasyon | A/B izolasyonu, silme/kurtarma, profil/şehir/seyahat/AI/tercih senkronizasyonu, çok cihaz çatışmaları ve avatar/Storage/Blaze. |
| 5 | Hava, güzergâh ve kişiselleştirme | 10 gün/48 saat, saat dilimi, eksik/eski veri ve kritik riskler; tercihlerle karşılaştırmalı gerçek kişiselleştirme. |
| 6 | Mimari, performans, temizlik ve güvenlik | Native/RN/FastAPI/network/depolar, async/iptal, bellek/recomposition, ölü/mükerrer kod, sırlar/loglar/kurallar, PostgreSQL dağıtım provası, teknik borç. |
| 7 | Üretim ve mağaza hazırlıkları | Servis lisans/kota/kapasite/gözlemleme, KVKK/gizlilik/koşullar/silme URL, Play Console kuruluşu ve mağaza beyanları. |
| 8 | Release ve son kabul | Gerçek imzalı R8 AAB, 16 KB cihaz/emülatör, release dayanıklılığı ve Google Play iç test/pre-launch. |

Sekiz satır bir çalışma sırasıdır; her paket alt test ve dış ortam bağımlılıkları içerir. Kullanıcı tercihi yalnız yerel testtir. Canlı hesap/izin değişikliği, ücretli plan veya mağaza işlemi yapılmış sayılmaz.

**6 Kasım tablet bildirimi · İlk açılışta Hava ekranı (açık).** Uygulama ilk açıldığında Hava ekranı beklenen içeriğe geçmiyor. Başka sekmeye geçip Hava’ya dönünce içerik geliyor. Soğuk açılışta ekran ve veri yükleme yaşam döngüsü, kayıtlı şehir olan ve olmayan durumlar ile bağlantılı ve çevrimdışı koşullar ayrı test edilecek. Kabul ölçütü, ilk açılışta sekme değiştirmeye gerek kalmadan uygun tahmin, şehir seçimi veya yeniden denenebilir hata durumunun gösterilmesi.

**6 Kasım tablet bildirimi · Takvim sekmesine geçiş (açık).** Takvim düğmesine bir kez dokunulduğunda önce bir ekran, ardından üstüne ikinci bir ekran geliyormuş gibi görünüyor. İlk geçiş ve başka sekmeden geri dönüş, kayıtlı seyahat olan ve olmayan durumlarda test edilecek. Gezinme yığını, geçiş animasyonu ve ilk veri yüklemesinde ekranın nasıl değiştiği ayrı incelenecek. Kabul ölçütü, tek dokunuşla Takvim’in açılması ve veri yüklenirken ekran katmanlarının üst üste görünmemesi veya içeriğin sıçramaması.

**6 Kasım uygulama genelinde gezinme ve ekran geçişleri (açık, öncelik 3).** Hava, Takvim, Asistan ve Profil sekmeleri dahil, ekran açan düğmeler, kartlar, menüler, bildirimler, geri hareketi, diyaloglar ve alt paneller tek tek incelenecek. Tek dokunuş, hızlı ardışık dokunuş, ilk açılış, geri dönüş ve boş/dolu/yükleme/hata durumları telefon ve tablette ayrı doğrulanacak. Gezinme yığınında çift hedef oluşması, geçişte iki ekranın üst üste görünmesi ve veri geldikten sonra içeriğin sıçraması ayrı kaydedilip düzeltilecek.

Bu tur doğrulaması: 403 JVM testi (0 hata), debug APK ve SM-T500 Android 12 üzerinde 9/9 izole Compose testi başarılı (3 yeni bağlı controller+gerçek touch + 6 mevcut dialog testi). Üç yeni JVM testi busy kapatma/late result, single-flight reopen, geri çekme intent'inin korunması ve stale save cevabının pencereyi yeniden açmamasını doğrular. Yeni cihaz testleri hata ekranındaki Tekrar dene, Uzak asistan izinlerini geri çek ve Vazgeç/Kapat düğmelerini performTouchInput ile çalıştırır; gateway sahte, gerçek hesap/servis değişmedi. Gerçek uygulamadaki eşzamanlı tüm düğme tepkisizliğinin ve backend erişim hatasının kök nedeni henüz doğrulanmadığından geniş sorun açık.

4 Kasım retry/revoke devamı: hızlı başarısız isteğin çalışmadı gibi görünmesine karşı işlem bazlı Deneme N geri bildirimi eklendi. Gerçek servis ve üretim UI sorunu öncelik 1 olarak açıktır. Doğrulama: 404 Android JVM testi (0 hata), debug APK, SM-T500 Android 12 üzerinde 10/10 Compose testi (4 gerçek dokunma etkileşimi + 6 dialog regresyonu) ve 22 yerel backend izin/platform testi başarılı (2 mevcut bağımlılık uyarısı). Yeni test, hızlı aynı hata sonrasında Deneme 2 ve Deneme 3 görünürlüğünü ve gerçek retry sayısını doğrular; JVM testi retry/revoke işlem sayısı ve hesap sıfırlamasını kapsar. İlk cihaz koşusunda 6 test Compose ağacına erişemedi (4 geçti); aynı paket tekrarında 10/10 geçti. Backend testlerinde gerçek HTTP engellendi; canlı erişim ve gerçek hesap izin değişikliği yapılmadı.

4 Kasım izin incelemesi: Oturum başlangıcındaki izin yüklemesi sürerken kullanıcı izin penceresini açarsa, onaylı belge geldiğinde pencere artık açık kalır. Yinelenen arka plan yüklemesi pencereyi görünür yapmaz. Tek istek ve açık pencere davranışı yeni JVM testiyle doğrulandı. Toplam 405 JVM testi başarılı, debug APK üretildi. Gerçek tablet ve canlı izin hizmeti doğrulaması açık.

4 Kasım eşzamanlı izin işlemleri: Yükleme veya kaydetme sürerken aynı hesabın geri çekme isteği beklemeye alınırsa geç gelen onay gösterilmez. Geri çekme yeniden denenebilir ve ikinci hesap bu durumdan etkilenmez. Yeni yerel regresyon testi dahil 406 Android JVM testi geçti ve debug APK üretildi. Tablet ve canlı hizmet doğrulaması açık.

4 Kasım eşzamanlı geri çekme: Asistan ve Ayarlar ekranlarından aynı hesap için başlatılan geri çekme işlemleri sırayla yürütülüyor. İlk isteğin başarısı, sonradan başarısız olan isteğin bekleyen kaydını silemiyor. Yeni yerel regresyon testi dahil 407 Android JVM testi geçti, debug APK üretildi. Tablet ve canlı hizmet doğrulaması açık.

4 Kasım oturum geçişi: İzin yükleme, kaydetme ve geri çekme sonuçları ekrana uygulanmadan önce aktif hesap yeniden kontrol ediliyor. A hesabının geç yanıtı, Firebase oturum dinleyicisi sıfırlama yapmadan önce B hesabına geçilmişse atılıyor. Yeni yerel regresyon testi dahil 408 Android JVM testi geçti ve debug APK üretildi. Tablet ve canlı hizmet doğrulaması açık.

4 Kasım izin penceresi hesap ayrımı: Hesap değiştikten sonra eski pencerenin kaydetme, geri çekme ve yeniden deneme işlemleri engelleniyor. Yeni hesap kendi izin metnini ayrı yükleyebiliyor. Yeni yerel regresyon testi dahil 409 Android JVM testi geçti ve debug APK üretildi. Tablet ve canlı hizmet doğrulaması açık.

4 Kasım izin hizmeti hata mesajları: DNS, TLS, zaman aşımı ve HTTP 5xx hataları kullanıcıya ayrı nedenlerle gösteriliyor. Teknik sunucu adresleri hata metnine taşınmıyor. Yeni yerel regresyon testi dahil 410 Android JVM testi geçti ve debug APK üretildi. Tablet ve canlı hizmet doğrulaması açık.

4 Kasım 12:49 tablet geri bildirimi: Tekrar dene, Uzak asistan izinlerini geri çek ve Vazgeç çalışıyor. Deneme 3 ekranındaki adres erişim hatası ayrı takip ediliyor. Android hedefi https://api.havamania.com/v1/chatbot/consent ve depodaki GET/POST yolları eşleşiyor. Yerel backend izin testleri 13/13 geçti (2 bağımlılık uyarısı). Canlı DNS ve HTTPS yerel test kapsamı dışında, kök neden doğrulanmadı.

4 Kasım bildirim yönlendirme: Başka hesaba ait, engellenmiş veya biçimi geçersiz bildirim Intent’leri açılışta bekleyen geçerli yönlendirmeyi değiştiremiyor. 410 Android JVM testi ve SM-T500 Android 12 üzerinde 3/3 izole bildirim testi geçti. Debug uygulama ve test APK’ları derlendi. Gerçek bildirim tepsisinden soğuk ve sıcak açılış ayrıca doğrulanacak.

4 Kasım bildirim açılış sırası: Bildirime dokunma işlemi gezinme yığınında gerçek bir ekran oluşana kadar bekliyor. Giriş ve kişiselleştirme ekranları yönlendirmeye kapalı. Yeni regresyon dahil 411 Android birim testi geçti ve debug APK üretildi. Bildirim tepsisinden soğuk ve sıcak açılışın gerçek cihaz doğrulaması açık.

4 Kasım bildirim ve ekran yeniden oluşturma: Bekleyen bildirim yönlendirmesi Activity kapsamlı ViewModel içinde korunuyor. İşlenmiş istek ikinci yeniden oluşturmada tekrar açılmıyor. SM-T500 Android 12 üzerinde izole ActivityScenario yeniden oluşturma testi dahil 4/4 bildirim cihaz testi geçti. Gerçek bildirim tepsisi ve süreç yeniden başlatma doğrulaması açık.

4 Kasım eski bildirim açılış komutu: Activity geri yüklenirken Android’in eski açılış Intent’i yeniden sıraya alınmıyor. Aynı süreçte bekleyen istek ViewModel’de korunuyor; yeni dokunuşlar normal akıştan geçiyor. SM-T500 Android 12 üzerinde 4/4 izole bildirim testi geçti. Gerçek bildirim tepsisi ve süreç ölümü sonrası bekleyen yönlendirme açık.

4 Kasım bildirim ve hesap silme: Bekleyen bildirim, hesap silme başlayınca iptal ediliyor. Ekran açılmadan hemen önce güncel hesap, oturum ve silme durumu tekrar kontrol ediliyor. Okundu işlemi sırasında başlayan silme için yeni regresyon dahil 412 Android birim testi geçti ve debug APK üretildi. Gerçek bildirim tepsisi doğrulaması açık.

4 Kasım seyahat bildirimi hedefi: Analiz bildirimleri hedef seyahat kimliğini doğrudan kaydediyor. Birincil hedefi eksik veya geçersiz olan seyahat detay kayıtları geçerli ilişkili seyahat kimliğini kullanıyor. Eski bağlantı biçimi destekleniyor. 413 Android birim testi geçti ve debug APK üretildi. Gerçek bildirim tepsisi doğrulaması açık.

4 Kasım seyahat yönlendirme kodlaması: Takvim ve bildirimler seyahat kimliğini ortak rota oluşturucularına ham değer olarak iletiyor. Özel karakterler rota oluşturulurken bir kez kodlanıyor. 414 Android birim testi geçti ve debug APK üretildi. Gerçek bildirim tepsisi doğrulaması açık.

4 Kasım seyahat gezinmesi cihaz testi: SM-T500 Android 12 üzerinde izole Compose NavHost testi geçti. Özel karakter ve yüzde dizisi içeren seyahat kimliği güzergâh ve takvim hedeflerine özgün değeriyle ulaşıyor. Gerçek bildirim tepsisinden uçtan uca açılış açık.

4 Kasım Android bildirim teslimi: SM-T500 Android 12 üzerinde geçici yerel bildirimin sistemdeki PendingIntent'i çalıştırıldı. İlk bildirim izole test Activity'sini açtı, ikincisi aynı Activity örneğine yeni istek olarak ulaştı. 1/1 test geçti. Tepsiye dokunma otomasyonu iki koşuda test satırına erişemedi; gerçek Havamania ekranına tepsiden uçtan uca geçiş açık. Geçici bildirimler ve test kanalı temizlendi.

4 Kasım Asistan hava özeti: Başlık ve sıcaklık metni sadeleştirildi, Türkçe küçük harf dönüşümü düzeltildi ve metinler Android kaynaklarına taşındı. 414 Android birim testi ve debug APK derlemesi başarılı. Yeni metnin cihazda görsel kontrolü açık.

4 Kasım Asistan hava özeti cihaz kontrolü: Eski veri Son kaydedilen hava özeti başlığıyla ayrılıyor. SM-T500 Android 12 üzerinde yüzde 200 yazı ölçeğiyle açık temada güncel ve koyu temada eski özet için 2/2 izole Compose testi geçti. İlk koşuda kilitli ekran nedeniyle iki test arayüze erişemedi; kilit açıldıktan sonra aynı testler geçti. 414 birim testi ve debug APK derlemesi başarılı.

4 Kasım Asistan hızlı işlem kartları: Dar alan veya büyük yazıda kartlar alt alta, yeterli alanda yan yana. Sabit 9 sp ve iki satır kesmesi kaldırıldı. SM-T500 Android 12 üzerinde 320 dp ve yüzde 200 yazıyla üç kartın görünürlüğü ve soru tetiklemesi doğrulandı. İki hava özeti regresyonuyla birlikte 3/3 cihaz testi geçti. Tam ekran görsel denetimi açık.

4 Kasım Asistan hava kartı: Hava durumunu yorumla kartı sabit Ankara sorusu yerine mevcut hava verisindeki şehri kullanabilen Bugün hava nasıl? sorusunu gönderiyor. SM-T500 Android 12 üzerinde kart etkileşimi ve iki hava özeti regresyonu dahil 3/3 izole Compose testi geçti. Canlı hava yanıtı bu testin kapsamı dışında.

4 Kasım Asistan seyahatli ve seyahatsiz durum: Seyahat havasını değerlendir kartı hızlı seyahat sorusuyla aynı koşula bağlı ve aktif seyahat varsa gösteriliyor. SM-T500 Android 12 üzerinde 320 dp ve yüzde 200 yazı ölçeğinde kartın plansızken gizlenmesi, plan geldiğinde görünmesi ve doğru soruyu göndermesi doğrulandı. 3/3 izole Compose testi geçti.

5 Kasım Asistan mesaj sınırı: 4.000 karakteri aşan taslak korunuyor, alan uyarı gösteriyor ve gönderim devre dışı kalıyor. Android ve backend emoji içeren metinlerde Unicode kod noktalarını sayıyor. 415 Android birim testi geçti, debug APK üretildi. Mesaj alanının klavye açıkken cihaz doğrulaması açık.

5 Kasım Asistan mesaj alanı cihaz testi: SM-T500 Android 12 üzerinde 4.001 karakterlik taslak korunuyor, uyarı görünür ve gönderme kapalı. Metin kısaltılınca uyarı kalkıyor ve yalnız kısa metin gönderim callback'ine ulaşıyor. 4/4 izole Compose testi geçti. Tam ekranda sistem klavyesiyle yerleşim ve canlı gönderim açık.

5 Kasım Asistan ve sistem klavyesi: Launcher Activity için adjustResize eklendi. Asistan ekranında klavye açıkken alt sekme çubuğu gizleniyor ve kapanınca geri geliyor. 415 Android birim testi ve debug APK derlemesi başarılı. Tam ekranda gerçek cihaz klavyesiyle yerleşim doğrulaması açık.

5 Kasım Asistan klavye cihaz testi: SM-T500 Android 12 üzerinde izole kenardan kenara Scaffold ile gerçek sistem klavyesi açıldı. Gönder düğmesinin yukarı taşındığı ve dokunmayla mesajı yerel callback'e ilettiği doğrulandı. 5/5 Compose testi geçti. Tam Asistan ekranında alt sekme çubuğunun gizlenmesi ve yatay yerleşim doğrulaması açık.

5 Kasım Asistan alt boşluğu: Sekme çubuğu açıkken ayrılmış sistem gezinme boşluğu Asistan mesaj alanında ikinci kez uygulanmıyor. Klavye açılınca sekme çubuğu yer ayırmadığından mesaj alanı klavye boşluğunu kendi uyguluyor. 415 Android birim testi ve debug APK derlemesi başarılı. Tam ekran yatay cihaz doğrulaması açık.

5 Kasım Asistan yatay klavye: SM-T500 Android 12 üzerinde izole test Activity'si yataya çevrildi. Gerçek sistem klavyesi açılınca Gönder düğmesi yukarı taşındı ve dokunmayla kısa mesaj yerel callback'e ulaştı. 6/6 izole cihaz testi geçti. Tam Asistan ekranında alt sekme çubuğunun gizlenmesi ve canlı gönderim doğrulaması açık.

5 Kasım Asistan mesaj taslağı: Taslak ekran bileşeni yerine Asistan ViewModel'inde bellekte tutuluyor. Activity yeniden oluşturulurken aynı ViewModel taslağı koruyor, oturum değişiminde ve sohbet sıfırlanırken taslak temizleniyor. 415 birim testi geçti, debug ve cihaz test APK'ları derlendi. Tam Asistan ekranında döndürme ve hesap değişimi cihaz doğrulaması açık.

5 Kasım Asistan taslağı ve izin bekleme: Gönder düğmesi taslağı hemen silmiyor. ViewModel, izin gerekiyorsa metni koruyor ve yalnız istek başladığında hâlâ aynı olan taslağı temizliyor. 415 Android birim testi, debug APK ve SM-T500 üzerinde 6/6 izole Compose testi başarılı. Cihaz testi kabul edilmeyen gönderimde taslağın kaldığını doğruluyor. Gerçek izin hizmetiyle uçtan uca akış açık.

5 Kasım Asistan bekleyen taslak düzenlemesi: Kullanıcı izin bekleyen taslağı değiştirirse veya silerse eski gönderim iptal ediliyor. İzin onayı eski metni göndermiyor; güncel taslak yeni bir gönderim bekliyor. 415 Android birim testi ve debug APK derlemesi başarılı. Bu senaryonun izin hizmetiyle uçtan uca doğrulaması açık.

5 Kasım Asistan eşzamanlı gönderim: Yanıt beklenirken hızlı işlem kartları, hazır sorular ve seyahat kartındaki SOR düğmesi devre dışı. İkinci istek izin akışına alınmıyor, daha önce izin bekleyen istek devam eden iş bitene kadar bekliyor. 415 Android birim testi ve SM-T500 üzerinde 6/6 izole Compose testi geçti.

5 Kasım hava önerisinden Asistan'a geçiş: Asistan meşgulse öneri bekliyor, gönderim akışı isteği kabul edince temizleniyor. Bekleyen öneri hesap değişiminde sıfırlanıyor. 415 Android birim testi ve debug APK derlemesi başarılı. Hava ekranından Asistan'a tam cihaz akışı açık.

5 Kasım Asistan öneri ve izin sırası: Hava ekranından gelen öneri, izin penceresi açıkken veya daha önceki mesaj izin beklerken sırada kalıyor. Önceki mesaj izin sonrası işleniyor ve öneri gönderim boşalınca yeniden değerlendiriliyor. 415 Android birim testi ve debug APK derlemesi başarılı. İzin hizmeti ve öneriyle birleşik cihaz akışı açık.

5 Kasım Asistan konuşma kaydırması: Yeni yanıt geldiğinde konuşmanın sonuna yakın olan kullanıcı için otomatik kaydırma sürüyor. Eski mesajları okurken gelen yanıt konumu değiştirmiyor. Yeni kullanıcı mesajı ve yazıyor göstergesi uygun durumda görünür alana alınıyor. 415 Android birim testi ve debug APK derlemesi başarılı. Uzun konuşma kaydırması için cihaz doğrulaması açık.

5 Kasım uzun Asistan konuşması cihaz testi: SM-T500 Android 12 üzerinde 30 mesajlı izole listede uygulamanın kaydırma davranışı doğrulandı. Eski mesaj okunurken gelen yanıt konumu korudu, liste sonundayken yeni yanıt görünür alana taşındı. 415 Android birim testi ve 7/7 cihaz testi geçti.

5 Kasım Asistan son mesaja dönüş: Uzun konuşmada son öğe görünüm dışında kaldığında Son mesaja git düğmesi gösteriliyor. Düğme son öğeye götürüyor ve hedef görünür olunca gizleniyor. SM-T500 Android 12 üzerinde 7/7 izole cihaz testi ve 415 Android birim testi geçti.

5 Kasım Asistan kaydırma geri yükleme: Önceki mesaj sayısı liste konumuyla birlikte saklanıyor, böylece ekran durumu geri yüklenirken eski mesajı okuyan kullanıcı sona atılmıyor. SM-T500 Android 12 üzerinde Compose kayıtlı durum geri yükleme senaryosu dahil 7/7 cihaz testi ve 415 birim testi geçti.

5 Kasım Asistan geçmiş sohbet hatası: Boş hata ekranındaki Tekrar dene artık aynı kayıtlı sohbeti yeniden yüklemeyi başlatıyor. Hata ekranı karşılama içeriğiyle üst üste gelmiyor ve mesaj alanı bu durumda gizleniyor. 415 Android birim testi ve debug APK derlemesi başarılı. Kayıt yükleme hatasının cihazda uçtan uca doğrulaması açık.

5 Kasım kayıtlı Asistan sohbeti yükleme durumu: İlk açılış ve yeniden deneme sırasında Sohbet yükleniyor göstergesi var. Karşılama kartları ve mesaj alanı yükleme sırasında görünmüyor. 415 Android birim testi ve debug APK derlemesi başarılı. Yavaş yükleme ve yeniden deneme geçişlerinin cihaz doğrulaması açık.

5 Kasım açık kayıtlı sohbet ve ekran yeniden oluşturma: ViewModel aynı sohbeti daha önce yüklediyse yeniden oluşturulan ekranın yükleme çağrısını atlıyor. Devam eden yanıt ve bellekteki mesajlar tekrar yüklemeyle kesilmiyor. Başarısız ilk yükleme yeniden denenebilir. 415 Android birim testi ve debug APK derlemesi başarılı. Tam cihaz akışı açık.

5 Kasım kayıtlı sohbet ilk yüklemesi: Aynı kayıt hâlâ okunuyorsa ekran yeniden oluşturulduğunda mevcut işlem korunuyor. İşlem bitince, başarısız olunca veya iptal edilince yükleme işareti temizleniyor. 415 Android birim testi geçti ve debug APK üretildi. Yavaş yerel yükleme sırasında tam ekran döndürme doğrulaması açık.

5 Kasım kayıtlı sohbet ve izin geri çekme: Geri çekme, cihazdaki kayıtlı sohbet okumasını yarıda kesmiyor. Devam eden asistan yanıtı iptal edilirse bekleme durumu temizleniyor. 415 Android birim testi geçti ve debug APK üretildi. Birleşik cihaz senaryosu açık.

5 Kasım kayıtlı Asistan sohbetini bitirme: Kaydet ve bitir başarılı olduğunda eski sohbet kimliğini taşıyan ekran yeni sohbet ekranıyla değiştiriliyor. Böylece yeni sohbet eski kaydı yüklemeye çalışmıyor. 415 Android birim testi geçti ve debug APK üretildi. Kaydetme sonrası tam cihaz gezinmesi açık.

5 Kasım sohbeti bitirme gezinmesi cihaz testi: SM-T500 Android 12 üzerinde izole NavHost ile eski sohbet kaydının yeni sohbetle değiştirildiği, kimliğin taşınmadığı ve Geri ile geçmişe dönüldüğü doğrulandı. Geçersiz kalan eski ekran callback'i mevcut ekranı değiştirmiyor. 2/2 gezinme testi ve 415 Android birim testi geçti. Gerçek sohbet kaydıyla uçtan uca doğrulama açık.

5 Kasım sohbet kaydederken ekran yeniden oluşturma: Kaydetme artık Compose ekranı ayrılırken iptal edilmiyor, ViewModel ve hesap yaşam döngüsüne bağlı kalıyor. Kaydetme sürerken bekleme penceresi yeniden gösteriliyor. 415 Android birim testi geçti, debug APK üretildi. Gerçek kayıt sırasında ekran döndürme cihaz doğrulaması açık.

5 Kasım yanıt sürerken sohbet kaydı: Yanıt beklenirken Kaydet düğmesi ve onay işlemi devre dışı. Kaydetme başlarken ViewModel'in en güncel mesaj listesi kullanılıyor. 415 Android birim testi geçti, debug APK üretildi. Gecikmeli yanıt ve kayıt geçişi cihaz doğrulaması açık.

5 Kasım sohbet kaydı ve otomatik gönderim: Kaydetme sürerken ve başarı sonucu işlenirken hava önerisi veya izin onayı otomatik mesaj başlatamıyor. Kayıt durumu değişince bekleyen işlem yeniden değerlendiriliyor. 415 Android birim testi geçti, debug APK üretildi. Eşzamanlı kayıt ve öneri senaryosunun cihaz doğrulaması açık.

5 Kasım Asistan geçmiş listesi: Yerel kayıtlar okunmadan boş geçmiş gösterilmiyor. Yerel okuma hatası, tekrar deneme düğmesiyle ayrı gösteriliyor. Başarılı boş sonuç boş geçmiş ekranına gidiyor. 415 Android birim testi ve debug APK derlemesi başarılı. Yükleme ve hata durumları cihaz doğrulaması açık.

5 Kasım Asistan geçmiş listesi cihaz testi: SM-T500 Android 12 üzerinde izole Compose testi yükleme, hata, Tekrar dene ile yüklemeye dönüş ve başarılı boş sonucu doğruladı. 1/1 cihaz testi ve 415 Android birim testi geçti. Gerçek Room okuma hatası bu arayüz testinde oluşturulmadı.

5 Kasım Asistan geçmişi yerel ve sunucu boş durumu: Cihazda kayıt yokken sunucu boşluğu doğrulanmadıysa cihaz kapsamlı mesaj gösteriliyor. Hesap geçmişinin boş olduğu mesaj yalnızca aynı hesaba ait sunucu doğrulamasıyla gösteriliyor. 415 Android birim testi ve SM-T500 üzerinde 1/1 izole cihaz testi geçti. Canlı senkronizasyon açık.

5 Kasım Asistan geçmişi senkronizasyon hatası: Dinleyici hata verirse cihazdaki kayıtlar görünür kalıyor, eski sunucu boşluk doğrulaması kaldırılıyor ve kullanıcı dinleyiciyi yeniden başlatabiliyor. 415 Android birim testi ve debug APK derlemesi başarılı. Canlı Firebase hata ve yeniden bağlanma doğrulaması açık.

5 Kasım Asistan geçmişi uyarı paneli: Senkronizasyon, yükleme ve silme uyarıları listeye bindirilmek yerine ölçülen bir alt alanda gösteriliyor. Uzun içerik panel içinde kaydırılıyor, sabit 100 dp liste boşluğu kaldırıldı. 415 Android birim testi ve debug APK derlemesi başarılı. Büyük yazı ve yatay kullanımda panel ile liste cihaz doğrulaması açık.

5 Kasım geçmiş uyarı paneli cihaz senaryosu: 320 dp yükseklik ve yüzde 200 yazı ölçeğinde son sohbet satırı ile paneldeki yeniden deneme düğmesinin erişilebilirliğini kontrol eden test eklendi. 415 birim testi geçti ve cihaz paketi derlendi. Tablet koşusunda ekran kilitliydi ve iki test arayüz ağacına erişemedi; cihaz doğrulaması açık.

5 Kasım geçmiş paneli yeniden test: Tablet kilidi açıldıktan sonra SM-T500 Android 12 üzerinde 2/2 cihaz testi geçti. 320 dp yükseklik ve yüzde 200 yazıyla son sohbet satırı görünür kaldı, uyarı panelindeki yeniden deneme düğmesine kaydırılıp dokunulabildi ve düğme liste alanıyla çakışmadı.

5 Kasım Asistan geçmişi silme sırasında kartlar: Kartlar görünür kalıyor, açma ve silme işlemleri devre dışı. Dokunma anında da güncel silme durumu kontrol ediliyor. SM-T500 üzerinde 3/3 cihaz testi ve 415 birim testi başarılı. İlk koşuda yeni testin ekran boyutu sağlayıcısı eksikti, test kurulumu düzeltildikten sonra üçü de geçti. Gerçek silme işlemi bu arayüz testinde çalıştırılmadı.

6 Kasım Asistan geçmişi detay silme kontrolü: Silme kaydı, detay okunmadan önce ve yanıt geldikten sonra kontrol ediliyor. Önceden silme işaretli kayıt için okuma yapılmıyor; okuma sırasında silme başlarsa gelen detay gösterilmiyor. 416 Android birim testi ve debug APK derlemesi başarılı. Canlı Firestore cihaz senaryosu açık.

6 Kasım kayıtlı sohbet açılışında silme kontrolü: Asistan sohbet ekranı da detay ekranıyla aynı silme kontrolünü kullanıyor. Silme işaretli kayıt açılmıyor ve okuma sırasında silme başlarsa dönen kayıt uygulanmıyor. 416 Android birim testi ve debug APK derlemesi başarılı. Doğrudan açılışın cihaz doğrulaması açık.

6 Kasım geçmiş listesi silme görünürlüğü: Yerel silme isteği kaydedilince liste yeniden okunuyor; silme işaretli sohbetler görünür listeden ve yükleme kuyruğundan dışlanıyor. Yerel satır sunucu onayı için korunuyor. 416 Android birim testi ve SM-T500 üzerinde 4/4 izole Room cihaz testi geçti. Canlı Firestore silme onayı bu testin dışında.

6 Kasım geçmiş listesi eşzamanlı okumaları: Yeni liste okuması önceki okumayı iptal ediyor. İptal edilen veya önceki silme dönemine ait sonuç listeye ya da hata durumuna uygulanmıyor. 416 Android birim testi ve debug APK derlemesi başarılı. Gecikmeli eşzamanlı okumanın cihaz doğrulaması açık.

6 Kasım Asistan geçmişi senkronizasyon uygulaması: Firestore sonucu Room'a işlenemezse yeniden deneme uyarısı gösteriliyor. Kapatılmış dinleyicinin gecikmiş sonucu yeni durumu veya hesap boşluk doğrulamasını değiştiremiyor. 416 Android birim testi ve debug APK derlemesi başarılı. Firebase ile yerel yazma hatasının birleşik cihaz senaryosu açık.

6 Kasım Asistan geçmişi bozuk sunucu kaydı: Sunucu görüntüsündeki bir sohbet çözümlenemezse liste kısmen uygulanmıyor, yerel geçmiş korunuyor ve senkronizasyon uyarısı gösteriliyor. 416 Android birim testi geçti ve debug APK üretildi. Bozuk belgeyle Firestore entegrasyon testi açık.

6 Kasım toplu geçmiş silme: Tüm geçmiş silme onayı aynı hesabın bekleyen tekil silme isteklerini de tamamlıyor. Tekil silme işaretleri eski verinin dönmesini engellemek için korunuyor. Diğer hesabın kuyruğu etkilenmiyor. 416 Android birim testi ve SM-T500 üzerinde 4/4 izole Room testi geçti. Canlı Firestore toplu silme doğrulaması açık.

6 Kasım sohbet kimliği ve hesap ayrımı: Yerel Room kimliği başka hesaba aitse kaydetme açıkça başarısız oluyor ve uzak yükleme başlamıyor. Mevcut kayıt korunuyor. 416 Android birim testi ve SM-T500 üzerinde 5/5 izole Room testi geçti. Şema aynı sohbet kimliğinin iki hesapta eşzamanlı saklanmasını henüz desteklemiyor.

6 Kasım Asistan geçmişi yükleme ve hesap değişimi: Firestore yazısı başlatılmadan hemen önce işlem iptali ve kayıt sahibinin güncel hesapla eşleşmesi kontrol ediliyor. Onaydan sonraki sahiplik kontrolü de devam ediyor. 416 Android birim testi ve debug APK derlemesi başarılı. Canlı hesap geçişi senaryosu açık.

6 Kasım geçmiş silme ve hesap geçişi: Tekil veya toplu Firestore silmesi öncesinde, toplu işlemde her grup öncesinde ve yerel onay işlemi öncesinde aktif hesap ve iptal durumu kontrol ediliyor. 416 Android birim testi ve debug APK derlemesi başarılı. Canlı Firestore'da gruplar arasında hesap değişimi doğrulaması açık.

6 Kasım geçmiş silme eski yeniden deneme: Önceden kuyruğa yazılan silme başka ekranda tamamlandıysa eski Tekrar dene işlemi yeni silme isteği oluşturmuyor. Kuyruğa ilk yazma başarısızsa yeniden denenebiliyor. 416 Android birim testi ve debug APK derlemesi başarılı. İki ekranlı uçtan uca senaryo açık.

6 Kasım eski toplu silme yeniden deneme cihaz testi: SM-T500 Android 12 üzerinde izole Room veritabanında toplu silme tamamlandı, yeni sohbet eklendi ve eski istek yeniden denendi. İkinci silme başlamadı ve yeni sohbet görünür kaldı. 6/6 cihaz testi ve 416 Android birim testi geçti. Canlı Firestore çağrısı bu testin kapsamında değil.

6 Kasım uzun sohbet detayı: Geçmiş detay ekranı tüm mesajları açılışta oluşturmak yerine LazyColumn kullanıyor. Başlık Sohbet detayı olarak düzeltildi ve Türkçe tarih biçimi korundu. 416 Android birim testi ve debug APK derlemesi başarılı. Uzun kayıtla cihaz kaydırma ve performans ölçümü açık.

6 Kasım uzun sohbet detayı cihaz testi: SM-T500 Android 12 üzerinde 200 sentetik mesaj ve yüzde 200 yazıyla başlık ve şehir görünür, son mesaj açılışta görünüm dışında, kaydırınca erişilebilir ve başa dönüş çalışıyor. 4/4 izole cihaz testi ve 416 birim testi geçti. Kare süresi ve bellek ölçümü açık.

6 Kasım Asistan geçmişi özet sorgusu: Geçmiş listesi ve Profil istatistikleri Room'dan yalnızca görünür sohbetlerin özet alanlarını alıyor. Mesaj dizisi liste açılışında okunmuyor. 200 mesajlı kayıtla özet ve detay ayrı doğrulandı. 416 Android birim testi ve SM-T500 üzerinde 7/7 izole Room testi geçti. Bellek ve süre ölçümü açık.

6 Kasım geçmiş senkronizasyonu yerel karşılaştırma: Room, sunucu listesinde eksik olabilecek onaylı kayıtların yalnızca kimliklerini okuyor. Bekleyen, güncellenmiş veya eski şemadan gelen onaysız kayıtların korunması devam ediyor. 416 Android birim testi ve SM-T500 üzerinde 6/6 izole veritabanı testi geçti. Canlı Firestore ve performans ölçümü açık.

6 Kasım toplu geçmiş silme yerel performansı: Room, sohbet içeriklerini açmadan silme işaretlerini tek SQL komutuyla oluşturuyor. Hesap ayrımı, tamamlanan işaretlerin korunması ve bekleyen tekil isteklerin kapanması doğrulandı. 416 Android birim testi ve SM-T500 üzerinde 7/7 izole Room testi geçti. Büyük geçmişle süre ölçümü açık.

6 Kasım sunucu geçmişi toplu silme işaretleri: Her Firestore silme grubu için yerel işaretler tek Room işlemiyle yazılıyor. Önceden bekleyen tekil silme durumu korunuyor ve yalnız sunucuda bulunan sohbetin işareti de doğru hesaba yazılıyor. 416 Android birim testi ve SM-T500 üzerinde 7/7 izole Room testi geçti. Canlı Firestore ve süre ölçümü açık.

6 Kasım sohbet sürüm sorgusu: Room, kayıt çakışması ve senkronizasyon sürüm kararlarında yalnızca hesap, güncelleme zamanı ve bekleyen yükleme alanlarını okuyor. Mesaj dizisi bu karşılaştırmada açılmıyor. 416 Android birim testi ve SM-T500 üzerinde 13/13 izole veritabanı testi geçti. Süre ve bellek ölçümü açık.

6 Kasım sohbet kaydı tarih ve sürüm okuması: Yeniden kaydetme eski mesaj dizisini açmadan oluşturulma tarihi ve güncelleme sürümünü okuyor. Yerel kayıt öncesi aktif hesap tekrar kontrol ediliyor. 200 mesajlı kayıtla tarih ve sürüm sorgusu doğrulandı. 416 birim testi ve SM-T500 üzerinde 7/7 izole Room testi geçti. Uçtan uca kayıt süresi ölçümü açık.

6 Kasım sohbet sürümü ve yerel kayıt atomikliği: Oluşturulma tarihinin ve sürümün okunması ile yeni kaydın yazılması aynı Room işlemi içinde. Firestore yüklemesi yerel işlem tamamlandıktan sonra başlıyor. 416 Android birim testi ve debug APK derlemesi başarılı. Eşzamanlı kayıt ve senkronizasyonun cihaz doğrulaması açık.

6 Kasım eşzamanlı yerel sohbet kaydı cihaz testi: Sürüm hesaplama ve yazma tek Room DAO işlemi olarak test edildi. Aynı başlangıç sürümüyle eşzamanlı iki kayıt 201 ve 202 sürümlerini aldı, oluşturulma tarihi korundu ve son sürüm saklandı. 416 birim testi ve SM-T500 üzerinde 8/8 izole Room testi geçti. Cihazlar arası canlı Firestore yarışı açık.

6 Kasım Asistan geçmişi yükleme kuyruğu: Room kuyruk sorgusu yalnızca sıralı sohbet kimliklerini döndürüyor. Her sohbetin mesaj dizisi yükleme sırası geldiğinde bir kez okunuyor. Sıralama, silme işaretleri ve hesap ayrımı doğrulandı. 416 Android birim testi ve SM-T500 üzerinde 14/14 izole veritabanı testi geçti. Canlı yükleme ve bellek ölçümü açık.

6 Kasım sohbet sürüm üst sınırı: Sürüm Long.MAX_VALUE değerindeyse güncelleme reddediliyor ve mevcut kayıt korunuyor. Sayısal taşma daha eski bir sürümün yazılmasına neden olmuyor. 416 Android birim testi ve SM-T500 üzerinde 9/9 izole Room testi geçti.

6 Kasım Asistan hata ayıklama günlükleri: Hesap kimliği, şehir adı, mesaj uzunluğu ve seyahat okumasının ham hata ayrıntıları kaldırıldı. Debug istek kimliği ve hata aşaması korunuyor. 416 Android birim testi ve debug APK derlemesi başarılı. Daha geniş uygulama günlük denetimi açık.

6 Kasım Android günlük denetimi: Bildirim, tema, seyahat, hava ve ağ günlüklerindeki hesap kimlikleri, şehir adı ve seçili ham hata ayrıntıları kaldırıldı. Olay ve yeniden deneme bilgileri korunuyor. 416 Android birim testi ve debug APK derlemesi başarılı. Çalışma zamanı logcat ve servis günlükleri doğrulaması açık.

6 Kasım konum taşıyan ağ günlükleri: Open-Meteo istek ve yanıt dökümü ile OSRM rota adresi günlüğü kaldırıldı. Konum sorgu parametreleri ve rota yolundaki koordinatlar bu istemcilerin HTTP günlüklerine yazılmıyor. 416 Android birim testi ve debug APK derlemesi başarılı. Çalışma zamanı Logcat doğrulaması açık.

6 Kasım hava isteği yeniden deneme: Ara 5xx yanıtları bekleme öncesinde kapatılıyor ve iptal edilen istek yeni deneme başlatmıyor. 429 doğrudan dönüyor. Canlı ağ kullanmayan sentetik OkHttp testleri dahil 418 Android birim testi ve debug APK derlemesi başarılı.

6 Kasım hava isteği TLS ve protokol hataları: TLS el sıkışması, uç kimliği doğrulaması ve protokol hataları otomatik yeniden denenmiyor. Sentetik OkHttp testi üç hatanın ilk denemede değişmeden iletildiğini doğruladı. 419 Android birim testi ve debug APK derlemesi başarılı.

6 Kasım hava ekranı yenileme: Son şehir seçimi hesapla eşleşmek zorunda. Hesap değişiminde veya şehir kaldırıldığında seçim sıfırlanıyor ve bekleyen istek iptal ediliyor. Elle yenileme 20 saniye ile sınırlı. Hata durumunda mevcut hava verisi korunuyor, veri yoksa yenileme hatası gösteriliyor. 419 Android birim testi ve debug APK derlemesi başarılı. Hesap/şehir değişimi sırasında yenileme ve bağlantı kesintisi senaryoları gerçek cihaz doğrulamasına açık.

6 Kasım hava ekranı ilk yükleme ve yenileme: İki işlem aynı istek ve hata akışını kullanıyor. Önbellekten gösterilen tahmin ağ zaman aşımında korunuyor. İptal edilmiş veya artık etkin hesaba/şehre ait olmayan istek hata ekranı açmıyor. Ham ağ istisnası günlükten kaldırıldı. 419 Android birim testi ve debug APK derlemesi başarılı. SM-T500 üzerinde 6 izole geç yanıt testi geçti. Ekranda şehir değiştirirken ve bağlantı kesilirken yenilemenin uçtan uca doğrulaması açık.

6 Kasım hava ekranı bağlantı ve şehir geçişi: Çevrimdışıyken Tekrar dene devre dışı, bağlantı dönünce etkin, istek sürerken ilerleme göstergeli. Hava ekranındaki eski ve yeni içerikleri bir saniye üst üste tutan geçiş kaldırıldı. 419 Android birim testi, debug APK ve SM-T500 üzerinde 7/7 hedefli test başarılı. Cihaz testleri kontrollü bağlantı durumu ve geç yanıtı kapsıyor. Canlı bağlantıyı keserek şehir seçimi ve yenilemeyi birlikte doğrulama açık.

6 Kasım çevrimdışı şehir seçimi: Hava hatası ekranı şehir seçiciyi açabiliyor. Arama yapılmadığında cihazda kayıtlı şehirler listeleniyor ve internet gerektirmeden seçilebiliyor. Aynı şehir adı satırda iki kez yazılmıyor. 419 Android birim testi, debug APK ve SM-T500 üzerinde 2/2 hedefli Compose testi başarılı. Test çevrimdışı durumu simüle ediyor. Gerçek bağlantı kesintisinde önbellekteki tahminin gösterilmesi uçtan uca doğrulamaya açık.

6 Kasım önbellekteki hava verisi: Bağlantı yokken çevrimdışı veri uyarısı, bağlantı varken eski önbellek uyarısı gösteriliyor. Son güncelleme tarihi ve saati seçili şehrin saat dilimine göre yazılıyor. SM-T500 üzerinde 7/7 hedefli test geçti. Yeni izole Room testi, başarısız ağ yenilemesinde seçili şehrin önbelleğinin korunduğunu doğruluyor. 419 birim testi ve debug APK derlemesi başarılı. Gerçek ağ kesintisiyle tam ekran doğrulaması açık.

6 Kasım hava kartı çökmesi ve önbellek uyarısı: Gün doğumu veya batımı eksikse hava kartı görsel hesaplarında varsayılan saatleri kullanıyor. Elle yenileme başarısız olduğunda 15 dakikadan yeni tahmin de önbellek olarak işaretleniyor. İstanbul saat diliminde gün değişimi ve yeni veri geldiğinde uyarının kalkması Compose ekranında doğrulandı. SM-T500 üzerinde 10/10 hedefli test, 419 birim testi ve debug APK derlemesi başarılı. Gerçek ağ kesintisiyle uçtan uca doğrulama açık.

6 Kasım Android bağlantı izlemesi: Hava ve seyahat ekranları etkin varsayılan ağın doğrulanmış internet erişimini takip ediyor. Ağ doğrulaması değiştiğinde durum yeniden okunuyor. Android 6 için genel ağ bildirimi uyumluluğu korunuyor. 419 Android birim testi, debug APK ve SM-T500 üzerinde 11/11 hedefli test başarılı. Ağ izleyicisinin ilk durumu tabletin Android bağlantı bilgisiyle karşılaştırıldı. Wi-Fi kesilip geri açılırken tam ekran akışı ve Android 6 cihaz doğrulaması açık.

6 Kasım gerçek Wi-Fi kesintisi: İlk SM-T500 denemesinde Android Wi-Fi bağlantısı kesilmesine rağmen izleyici çevrimdışı olayını kaçırdı. Varsayılan ağın kayıp ve yetenek değişiklikleri artık geri çağrının taşıdığı durumla işleniyor. Wi-Fi kapatılıp geri açıldığında aynı gözlemci çevrimiçi, çevrimdışı ve tekrar çevrimiçi durumlarını sırayla doğruladı. Test Wi-Fi ayarını geri yükledi ve yalnızca testWifiToggle parametresiyle çalışıyor. Hava ekranı Compose testleri dahil SM-T500 üzerinde 5/5 hedefli test ve debug APK derlemesi başarılı. Gerçek bağlantı olayını ana hava ekranı ve seyahat akışına kadar tek senaryoda izleme ile Android 6 cihaz doğrulaması açık.

6 Kasım gerçek Wi-Fi kesintisinde hava ekranı: Yerel test tahmini ve gerçek ağ izleyicisiyle SM-T500 üzerinde Wi-Fi kesildiğinde çevrimdışı uyarısı göründü, şehir kartı görünür kaldı ve Wi-Fi dönünce uyarı kalktı. Şehir başlığı Türkçe büyük harf kurallarını kullanıyor ve İZMİR olarak doğrulandı. İlk test süreci istisna kaydı olmadan kesildi, tekrar koşusunda 5/5 hedefli test geçti ve Wi-Fi yeniden bağlandı. Seyahat akışının gerçek kesinti testi ve Android 6 doğrulaması açık.

6 Kasım seyahat eşitleme paneli: Bağlantı kesilince devam eden bir deneme olsa bile panel Bağlantı bekleniyor durumunu gösteriyor. Gerçek Wi-Fi kesintisinde yerel test verisiyle bekleyen yükleme ve silme sayaçlarının korunduğu ve bağlantı dönünce işlem bitmişse yeniden denemenin açıldığı doğrulandı. SM-T500 üzerinde 5/5 hedefli test geçti, Wi-Fi geri yüklendi. Gerçek hesabın kayıt ve silme kuyruğunun uzak uçla eşitlenmesi ve Android 6 cihaz testi açık.

6 Kasım seyahat kuyruğunun bağlantı dönüşü: Bağlantı geldiğinde çalışan kayıt veya silme denemesi varsa ilgili kuyruk deneme bitince yeniden taranıyor. Kuyruklar birbirinden bağımsız. Bağlantının tekrar kesilmesi veya oturumun değişmesi bekleyen yeniden denemeyi iptal ediyor. 421 Android birim testi ve debug APK derlemesi başarılı. Gerçek hesabın Firebase kuyruğuyla uçtan uca eşitlemesi açık.

6 Kasım seyahat yükleme kuyruğunda kayıt bazlı hata: Bir yükleme hata verir veya zaman aşımına uğrarsa yerel kayıt beklemede kalıyor ve sonraki kayıt deneniyor. Oturum iptali kuyruğu durduruyor. 423 Android birim testi ve debug APK derlemesi başarılı. Firebase üzerinden gerçek hesapla uçtan uca eşitleme açık.

6 Kasım seyahat silme kuyruğu: Aynı silme isteği tekrar geldiğinde tamamlanmış işaret korunuyor ve uzak silme yeniden çağrılmıyor. Bekleyen istek yeniden denenebiliyor. 423 Android birim testi, debug APK ve SM-T500 üzerinde 5 izole Room testi başarılı. Canlı Firebase üzerinde silme ve eşitleme doğrulaması açık.

6 Kasım alt sekme gezinmesi: Açık ana sekmeye tekrar dokunmak yeni gezinme işlemi başlatmıyor. Belirli bir seyahat veya sohbet argümanı taşıyan girişlerin mevcut davranışı korunuyor. 423 Android birim testi ve debug APK derlemesi başarılı. İlk Takvim geçişindeki katmanlaşma ve tüm ekran geçişlerinin cihaz denetimi açık.

6 Kasım ilk hava yüklemesi: Otomatik konum modunda kayıtlı şehir için tahmin isteği GPS ve adres çözümünü beklemiyor. Konum bulunamazsa kayıtlı şehir korunuyor, farklı konum bulunursa tahmin o konuma geçiyor. 425 Android birim testi ve debug APK derlemesi başarılı. Videodaki ilk açılış belirtisinin bu gecikmeden kaynaklandığı ve tablette sekme değiştirmeden düzeldiği henüz doğrulanmadı.

6 Kasım ilk hava açılışı tablet doğrulaması: Güncel debug APK veriler korunarak SM-T500 Android 12 cihazına yüklendi. Uygulama zorla durdurulup sekmelere dokunmadan başlatıldığında bağlantı açıkken tahmin ve saatlik içerik görüntülendi. Wi-Fi kapalı soğuk açılışta ilk kontrolde tahmin henüz görünmedi, sonraki kontrolde önbellekteki tahmin ve çevrimdışı uyarısı göründü. Wi-Fi ayarı geri açıldı. Mevcut hesap ve kayıtlı veri koşullarında bildirilen takılma yeniden oluşmadı. Şehir veya önbellek bulunmayan durum, diğer cihazlar ve Takvim geçişi açık.

6 Kasım Takvim ilk geçişi: Seyahat listesi yüklenirken gerçek takvimin yanında ikinci takvim ve özet iskeleti çiziliyordu. Liste iskeleti listeyle sınırlandı, yaklaşan seyahat kartı takvimin altına alındı. 425 Android birim testi ve debug APK başarılı. Güncel APK ile SM-T500 üzerinde ilk Takvim dokunuşu ve Hava’dan Takvim’e dönüşte tek Seyahat Planlayıcı başlığı doğrulandı. Geçişin ilk karelerinin görsel doğrulaması ile boş ve dolu veri karşılaştırması açık.

6 Kasım Takvim yerleşim sıçraması: Seyahat yükleme, hata ve eşitleme panelleri takvim şeridinin üstünden seyahat listesinin başına taşındı. Böylece panel görünüp kaybolduğunda takvim şeridini doğrudan itmez. 425 Android birim testi, debug APK ve SM-T500 üzerinde 3 hedefli durum paneli erişilebilirlik testi başarılı. Tam ekranın geçiş kareleri ve kayıtlı seyahat bulunan/bulunmayan durumların görsel karşılaştırması açık.

6 Kasım ana sekmelerde üst üste çizim: Hava, Takvim, Asistan ve Profil arasındaki ortak NavHost geçişinde eski ve yeni ekranı aynı anda çizen saydamlık animasyonu kaldırıldı. Alt ekranlarda mevcut geçiş kullanılıyor. SM-T500 cihazındaki izole NavHost testi Hava–Takvim gidiş ve dönüşünde ilk 64 ms içinde önceki içeriğin kaldırıldığını doğruladı. 425 Android birim testi, 3 hedefli cihaz testi ve debug APK başarılı. Gerçek tam ekran geçiş kaydı ve diğer gezinme yüzeylerinin görsel denetimi açık.

6 Kasım hızlı çift dokunuş: Hava, Takvim, Asistan ve geçmişi, Profil ve Ayarlar içindeki ilgili alt ekran geçişleri yalnız kaynak gezinme girdisi hâlâ etkinse çalışıyor. Arkada kalan ekrandan gelen ikinci olay ayrıntıyı yeniden açmıyor. SM-T500 üzerinde rota testi geç gelen ikinci isteği, seyahat kimliğinin korunmasını ve tek geri dönüşü doğruladı. 425 Android birim testi, 3 hedefli cihaz testi ve debug APK başarılı. Diğer etkileşimler ve gerçek ekranlarda hızlı dokunuş turu açık.

6 Kasım çift Geri dokunuşu: Ekran içi Geri işlemi yalnız o gezinme girdisi hâlâ etkinse çalışıyor. Kapanan ekrandan gelen ikinci olay bir önceki ekranı atlamıyor. SM-T500 üzerinde iki ardışık geri çağrısında kaynak ekranın korunduğu doğrulandı. 425 Android birim testi, 3 hedefli gezinme cihaz testi ve debug APK başarılı. Sistem geri hareketi ve gerçek ekranlardaki hızlı dokunuş denetimi açık.

6 Kasım Takvim Geri davranışı: Tablet düzeninde açık seyahat rehberi, sistem Geri veya başlık Geri düğmesiyle önce kapanıyor. Rehber oturum değişiminde sıfırlanıyor. SM-T500 üzerinde izole düzenleyici testi gerçek Android Geri tuşunun pencereyi kapattığını, alttaki ekranı koruduğunu ve düzenleyicinin yeniden açılabildiğini doğruladı. 425 Android birim testi, 3 cihaz testi ve debug APK başarılı. Tablet rehberinin gerçek Takvim ekranında doğrulaması ve telefon düzenindeki satır içi rehber açık.

9 Kasım Takvim rehberi: Telefon ve tablet aynı rehber seçimini kullanıyor. Sistem ve başlık Geri düğmeleri açık rehberi önce kapatıyor. Tarih, filtre veya hesap değiştiğinde açık seçim sıfırlanıyor. Tablet düğmesi açma ve kapatma durumunu ayrı gösteriyor. 425 Android birim testi ve debug APK başarılı. Telefon ve tablette gerçek Geri hareketi doğrulaması açık.

9 Kasım seyahat rehberi Geri testi: Takvim’in kullandığı ortak Geri kontrolü SM-T500 üzerinde izole Compose ekranında test edildi. Android Geri tuşu ve ekran düğmesi açık rehberi kapatırken Takvim açık kaldı. Rehber kapalıyken sonraki sistem Geri normal gezinmeye döndü. 425 birim testi, 4/4 cihaz testi ve debug APK başarılı. Gerçek Takvim ekranında telefon ve tablet yerleşimlerinin uçtan uca doğrulaması açık.

9 Kasım bildirim listesi geçişi: Ayrıntı ve Hava yedek hedefi yalnız bildirim listesi hâlâ etkinse açılıyor. İzole NavHost testi aynı hedef türüne ait geç ikinci isteğin ilk rota kimliğini değiştirmediğini doğruladı. 425 birim testi, SM-T500 üzerinde 4 gezinme testi ve debug APK başarılı. Gerçek bildirim listesinden hızlı dokunuş ve sistem tepsisi doğrulaması açık.

9 Kasım bildirim okundu sırası: Liste etkinse ve açma işlemi kabul edildiyse bildirim okundu işaretleniyor. Ayrıntısız bildirimler etkin listede okunabiliyor. Arkada kalan listeden gelen olay okundu durumunu değiştirmiyor. 425 Android birim testi ve debug APK başarılı. Gerçek bildirim listesinde ardışık dokunuş ve sayaç doğrulaması açık.

9 Kasım bildirim sayacı: Kabul edilen açma ve okundu güncellemesi ortak akışta. SM-T500 izole Room testinde ilk bildirim okundu, reddedilen ikinci bildirim okunmamış kaldı ve sayaç 2’den 1’e indi. 425 birim testi, 3 cihaz testi ve debug APK başarılı. Gerçek listede dokunuş ve sistem tepsisi doğrulaması açık.

9 Kasım sistem bildirim tepsisi: SM-T500 Android 12 üzerinde tepsi açıldı ancak geçici bildirimin satırı iki ayrı otomasyon denemesinde bulunup tıklanamadı. Mevcut PendingIntent teslim testi korundu. Gerçek tepsi dokunuşuyla ilk ve tekrar açılış ile uygulama içi hedefin doğrulanması açık.

9 Kasım bildirim okundu hesap kapsamı: Kabul edilen açma işlemi bildirim kimliğini ve sahibini birlikte taşıyor. Hesap değişirse eski bildirimin okundu işlemi yeni hesaba uygulanmıyor. 425 birim testi, SM-T500 üzerinde 3 izole bildirim testi ve debug APK başarılı. Gerçek hesap geçişi ve tepsi dokunuşu açık. Sunucu dağıtımı kullanıcı isteğiyle sonraya bırakıldı.

9 Kasım tekil bildirim işlemleri: Silme ve okundu durumunu değiştirme, dokunulan bildirimin sahibiyle birlikte kuyruğa alınıyor. Kilit beklemesinden sonra etkin hesap yeniden denetleniyor. 425 birim testi, SM-T500 üzerinde 3 mevcut izole bildirim testi ve debug APK başarılı. Gerçek A/B hesap değişimi ve toplu seçim akışı açık.

9 Kasım toplu bildirim işlemleri: Seçilenleri silme ve okundu işaretleme, ekrandaki hesap ve seçim anlık görüntüsüyle çalışıyor. Tümünü sil onayı hesap değişiminde kapanıyor ve eski onay yeni hesaba uygulanmıyor. 425 birim testi, SM-T500 üzerinde 3 mevcut izole bildirim testi ve debug APK başarılı. Gerçek A/B hesap geçişinde toplu işlem ve sayaç doğrulaması açık.

9 Kasım bildirim seçimi: Liste güncellendiğinde artık görünmeyen bildirimler seçimden çıkarılıyor. Tümünü seç, görünür kimlikleri seçili kimliklerle karşılaştırıyor. Son seçim kalkınca seçim modu kapanıyor. Yeni regresyon dahil 426 Android birim testi ve debug APK başarılı. Fiziksel cihazda filtre ve toplu seçim akışı açık.

9 Kasım bildirim filtresi: Etkin filtre tekrar seçildiğinde mevcut seçim korunuyor. Seçim boşken liste yenilemesi ek kimlik taraması yapmıyor. 426 Android birim testi ve debug APK başarılı. Filtre ve toplu seçim davranışının gerçek cihaz doğrulaması açık.

9 Kasım bildirim seçim düğmesi: Tüm görünür satırlar seçildiğinde simge ve ekran okuyucu etiketi Seçimi kaldır olarak değişiyor. Eski hesap görünümünden gelen toplu seçim dokunuşu yeni hesaba uygulanmıyor. 426 Android birim testi ve debug APK başarılı. TalkBack ve gerçek cihazda toplu seçim doğrulaması açık.

9 Kasım bildirim seçim düğmesi cihaz testi: Ortak düğme bileşeni için Tümünü seç ve Seçimi kaldır erişilebilirlik etiketi ile seçim değişimini kontrol eden Compose testi eklendi. 426 Android birim testi ve debug APK başarılı. SM-T500 ekranı kapalı ve kilitli olduğu için yeni cihaz testi arayüze erişemedi. Cihaz doğrulaması açık.

9 Kasım bildirim seçim düğmesi tekrar testi: Tablet kilidi açıldıktan sonra SM-T500 Android 12 üzerinde izole Compose testi 1/1 geçti. Tümünü seç ve Seçimi kaldır etiketleri ile seçimin iki yönde değişmesi doğrulandı. Tam bildirim listesi ve TalkBack turu açık.

9 Kasım bildirim satırı erişilebilirliği: Seçim modunda satır hem seçili hem okundu durumunu erişilebilirlik ağacına veriyor. Seçme ve seçimi kaldırma eylemleri etiketlendi. 426 birim testi, SM-T500 üzerinde 1 hedefli Compose testi ve debug APK başarılı. Tam listeyle sesli TalkBack turu açık.

9 Kasım bildirim filtreleri: Etkin filtre seçili semantiğini ve temanın vurgu üstü metin rengini kullanıyor. SM-T500 Compose testi Tümü ve Okunmamış arasında seçili durumunun doğru taşındığını doğruladı. 426 Android birim testi, 1 hedefli cihaz testi ve debug APK başarılı. Sesli TalkBack turu açık.

9 Kasım bildirim merkezi boş durumları: Gerçek boş kutu ve sonuçsuz filtre ayrı metin gösteriyor. Filtreyi temizleme düğmesi yalnızca sonuçsuz filtrede var. Liste ve toplu seçim aynı filtreli veriyi kullanıyor. 426 birim testi, SM-T500 üzerinde 1 hedefli Compose testi ve debug APK başarılı. Tam liste ve TalkBack doğrulaması açık.

9 Kasım bildirim yükleme hatası: Room okuma hatası boş kutu yerine yeniden denenebilir hata ekranı gösteriyor. Yeniden deneme etkin hesapla sınırlı. 426 Android birim testi ve debug APK başarılı. Hata enjeksiyonuyla cihaz doğrulaması açık.

9 Kasım bildirim okuma regresyonu: Akış hatası, başarılı boş liste ve iptal ayrı işleniyor. Yapay okuma hatası sonrası yeni okumada bildirim alındığı ve iptalin hata olarak sunulmadığı doğrulandı. 428 Android birim testi ve debug APK başarılı. Hata ekranından gerçek dokunuşla yeniden deneme cihazda açık.

9 Kasım bildirim hata ekranı: SM-T500 üzerinde 1 hedefli Compose testi geçti. Tekrar dene bir kez çağrıldı ve simüle edilen başarılı okumadan sonra hata görünümü boş kutu görünümüne geçti. Debug APK derlendi. Gerçek Room hatasının tam bildirim ekranından uçtan uca doğrulaması açık.

9 Kasım bildirim Room yeniden okuma: SM-T500 üzerinde yapay SQLite okuma hatasının hata sonucu olarak geldiği ve sonraki okumada yalnız doğru hesabın kayıtlı bildiriminin Room veritabanından döndüğü doğrulandı. 428 birim testi ve 4 cihaz testi başarılı. Tam bildirim ekranından birleşik hata/yeniden deneme akışı açık.

9 Kasım bildirim hata ve yeniden deneme birleşik testi: SM-T500 üzerinde ilk okuma SQLite hatasıyla simüle edildi. Tekrar dene düğmesine tek dokunuş sonrasında kayıt izole Room veritabanından alındı ve gerçek bildirim satırı bileşeninde göründü. 1/1 hedefli cihaz testi başarılı. Tam Activity ve gerçek hesapla doğrulama açık.

9 Kasım tam Bildirim Merkezi cihaz kontrolü: SM-T500 üzerinde uygulama paketi bulunmadığı için güncel debug APK kuruldu ve uygulama açıldı. Giriş ekranı görüldü. Gerçek Bildirim Merkezi ve filtre doğrulaması test hesabıyla oturum açılmasını bekliyor.

9 Kasım gerçek Bildirim Merkezi: Test hesabıyla oturum açıldıktan sonra tahmin başarıyla yüklenmediğinde Hava ekranında Bildirimler düğmesinin eksik olduğu görüldü. Yükleme, boş şehir ve hata durumlarına bildirim düğmesi eklendi. SM-T500 üzerinde gerçek uygulamada düğme merkezi açtı. Tümü ve Okunmamış filtrelerinin Android erişilebilirlik ağacında işaretli duruma geçtiği doğrulandı. 428 birim testi ve debug APK başarılı. Dolu liste, sesli TalkBack ve sistem tepsisi açık.

9 Kasım Bildirim Merkezi sıcak bağlantısı: havamania://app/notifications açık Activity’ye ulaştığında etkin oturum ve gezinme hazırsa merkezi açıyor. Merkez zaten açıksa ikinci kayıt eklenmiyor. SM-T500 üzerinde iki bağlantı teslimi ve tek Geri ile doğrulandı. 428 Android birim testi ve debug APK başarılı. Sistem tepsisindeki sahipli bildirimle uçtan uca test açık.

9 Kasım sistem bildirimi teslim testi: İlk doğrudan cihaz koşusunda ikinci bildirim notify dönüşünden hemen sonra etkin listede görünmedi. Test artık Android yayınının etkin listeye yansımasını en çok beş saniye bekliyor. Test APK derlendi. Gerçek tepsi satırına dokunma ve uygulama içi hedef doğrulaması açık.
Güncellenen teslim testi SM-T500 üzerinde üç ardışık koşuda 3/3 geçti.

9 Kasım tepsi dokunuşu tekrar denemesi: Android etkin listede bildirimin yayımlandığı doğrulandı. SM-T500 ekranı açık ve kilitsiz olmasına rağmen otomasyon bildirim satırını panelin erişilebilirlik ağacında bulamadı. Başarısız deneme kodu kaldırıldı, etkin listeyi bekleyen mevcut teslim testi korundu. Fiziksel tepsi dokunuşu açık.

9 Kasım bekleyen Bildirim Merkezi bağlantısı: İstek hesap kimliği ve bildirim oturumu sürümüyle sınırlanıyor. A hesabına geri dönülse bile önceki oturumun bekleyen bağlantısı geçersiz. Hesap silme engeli de gezinmeden önce yeniden kontrol ediliyor. 429 birim testi ve debug APK başarılı. Gerçek A/B hesap geçişi cihazda açık.

9 Kasım Bildirim Merkezi soğuk bağlantısı: İlk ve sıcak açılış aynı oturum kontrolünden geçiyor. Bekleyen bağlantı Activity yeniden oluşturulurken ViewModel içinde tutuluyor ve eski Intent tekrar işlenmiyor. SM-T500 üzerinde gerçek soğuk başlangıçta Bildirim Merkezi açıldı, tek Geri ana ekrana döndü. 429 birim testi ve debug APK başarılı. Sistem tepsisi dokunuşu açık.

9 Kasım bildirim bağlantısı Activity testi: SM-T500 üzerinde bekleyen doğrudan Bildirim Merkezi bağlantısı yeniden oluşturulan Activity’nin ViewModel’inde korundu. Tüketilen doğrudan bağlantı ve sahipli bildirim isteği ikinci yeniden oluşturmada geri gelmedi. Test APK derlendi ve 4/4 bildirim cihaz testi geçti. Tam uygulamada süreç ölümü ve fiziksel tepsi dokunuşu açık.

9 Kasım Bildirim Merkezi süreç ölümü: SM-T500 üzerinde bağlantıyla merkez açıldı, uygulama arka plana alındı ve süreç sonlandırıldı. Başlatıcıyla geri dönüldüğünde farklı PID ve geri yüklenen Bildirim Merkezi doğrulandı. Tek sistem Geri ana ekrana döndü. Gerçek test hesabıyla yapıldı. Sistem tepsisi dokunuşu açık.

9 Kasım hava yüklemesi: Tahmin akışı veri vermeden biterse etkin isteğin yükleme ekranı yeniden denenebilir hataya geçiyor. Eldeki başarılı tahmin korunuyor. 429 Android birim testi ve debug APK başarılı. Boş akışın hedefli cihaz doğrulaması açık.

9 Kasım Hava ilk açılış ayar hatası: Yerel şehir ayarı okunamazsa ekran yüklemede kalmak yerine hata gösteriyor. İnternet yokken de ayar yeniden okunabiliyor. Asistan tonu okuma hatası Hava başlangıç işlerini iptal etmiyor. 429 Android birim testi ve debug APK başarılı. Yerel ayar hata enjeksiyonu ve tablet doğrulaması açık.

9 Kasım Hava yerel hata arayüzü: Şehir ayarı hatası kendi başlığını gösteriyor ve internet kapalıyken yeniden denemeye izin veriyor. SM-T500 üzerinde 4/4 Hava hata testi, 429 birim testi ve debug APK başarılı. Gerçek DataStore hatasının tam açılış akışında doğrulaması açık.

9 Kasım Hava manuel şehir seçimi: Elle seçim bekleyen otomatik konum işini iptal ediyor ve konum tercihini manuel kaydediyor. Yeni birim testi iptalden sonra ulaşan konumun elle seçilmiş şehri değiştirmediğini doğruladı. 430 birim testi ve debug APK başarılı. Gerçek GPS ve şehir seçimi cihaz doğrulaması açık.

9 Kasım manuel şehir kaydı: Şehir, konum modu ve bekleyen şehir eşitleme kaydı tek DataStore yazımında saklanıyor. SM-T500 izole DataStore testi depoyu yeniden açınca seçili şehir ve Manuel modun korunduğunu, diğer hesabın Auto modunun değişmediğini doğruladı. 430 birim testi, 1 cihaz testi ve debug APK başarılı. Gerçek Hava ekranında GPS ile birlikte doğrulama açık.

9 Kasım Hava konum tercihi okuması: Şehir ve konum modu aynı DataStore anlık görüntüsünden okunuyor. İzole SM-T500 testinde ilk değer Otomatik ve şehir yok iken sonraki değer doğrudan Manuel ve Ankara oldu. 430 birim testi, 1 cihaz testi ve debug APK başarılı. Gerçek Hava ekranında GPS ile birlikte doğrulama açık.

9 Kasım Hava otomatik konum beklemesi: Kayıtlı şehir yoksa beş saniyelik şehir seçme geçişi doğrudan etkin konum aramasına bağlı. İkinci DataStore okuması kaldırıldı. Erken konum sonucu veya elle seçim bekleyen geçişi iptal ediyor. 430 Android birim testi ve debug APK başarılı. Yavaş GPS ile gerçek cihaz doğrulaması açık.

9 Kasım otomatik konum hatası: Konum sağlayıcısı hata verirse kayıtlı şehir korunuyor; kayıtlı şehir yoksa şehir seçme görünümü açılıyor. İptal ayrı işleniyor. Yapay sağlayıcı hatası regresyonu dahil 431 Android birim testi ve debug APK başarılı. Gerçek GPS hata senaryosu açık.

9 Kasım Hava soğuk açılış tekrar kontrolü: SM-T500 üzerinde güncel APK ve açık test hesabıyla, sekme değiştirmeden ilk kontrolde tahmin görünüyordu. Şehir seçici açıldı ve sistem Geri ile kapandı. Şehir tercihi değiştirilmedi. Bu hesapta kayıtlı tahmin bulunduğundan şehir bulunmayan soğuk açılış ve gerçek yavaş GPS senaryosu açık.

9 Kasım yavaş konum testi: Konum aramasına bağlı şehir seçme beklemesi aynı çözümleme akışına taşındı. Kontrollü zamanlama testi kayıtlı şehir yokken konum yanıtı beklenmeden şehir seçme seçeneğinin sunulduğunu, sonra gelen konumun yüklenebildiğini doğruladı. 432 Android birim testi ve debug APK başarılı. Gerçek yavaş GPS cihaz senaryosu açık.

9 Kasım konum zamanlayıcı regresyonları: Konum erken tamamlandığında veya arama iptal edildiğinde bekleyen şehir seçme seçeneğinin sonradan açılmadığı iki kontrollü birim testiyle doğrulandı. 434 Android birim testi ve debug APK doğrulaması başarılı. Gerçek GPS cihaz kontrolü açık.

9 Kasım otomatik konum adları: Boş adres alanları atlanıyor ve il adı Türkçe harf karşılaştırmasıyla ilçe olarak tekrarlanmıyor. Yer adı bulunamazsa Mevcut konum gösteriliyor. Yeni regresyonla 435 birim testi ve debug APK başarılı. Gerçek cihazın adres sağlayıcısıyla doğrulama açık.

9 Kasım seyahat rotası: Kayıtlı başlangıç yoksa GPS koordinatı adres çözümlemesine bağlı olmadan kullanılıyor. İptal edilen hesaplama rota sonucunu güncellemiyor ve başlangıç koordinatları debug günlüğüne yazılmıyor. 435 Android birim testi ve debug APK başarılı. Adres sağlayıcısı yanıt vermezken rota başlangıcının gerçek cihazda doğrulanması açık.

9 Kasım rota hava analizi: Rota yenilenince analiz durumu sıfırlanıyor ve önceki rotadan geciken analiz sonuçları yeni rotaya yazılmıyor. 435 Android birim testi ve debug APK derlemesi başarılı. Adres sağlayıcısı yanıt vermezken GPS ile rota başlatma ve analiz sırasında rota değiştirme senaryoları gerçek cihazda açık.

9 Kasım güzergâh hava verisi hatası: Tüm tahminler eksikse analiz tamamlanmış sayılmıyor. Mevcut rota korunuyor ve kullanıcı hava analizini aynı ekranda tekrar başlatabiliyor. 435 Android birim testi ve debug APK derlemesi başarılı. Veri kesintisi ve yeniden deneme akışının telefon ve tablette görsel kontrolü açık.

9 Kasım kısmi güzergâh tahmini: Tahmin alınabilen noktalar gösteriliyor. Herhangi bir örnek noktası eksikse telefon ve tablet yerleşimlerinde o kesimlerde riskin değerlendirilemediği belirtiliyor. 435 Android birim testi ve debug APK başarılı. Kısmi yanıtın gerçek cihazda görsel doğrulaması açık.

9 Kasım güzergâh tahmin saatleri: ETA, servisten gelen ilk ve son geçerli saat örneği arasındaysa eşleştiriliyor. Sınır dışı saatlerde en yakın örnek kullanılmıyor. Yeni regresyonla 436 Android birim testi ve debug APK derlemesi başarılı.

9 Kasım güzergâh tahmin boşluğu: Geçiş saati iki örnek arasındaysa tahmin ancak bu örnekler en fazla bir saat aralıklı olduğunda gösteriliyor. Eksik saat komşu tahminle doldurulmuyor. Yeni regresyon dahil 437 Android birim testi ve debug APK derlemesi başarılı.

9 Kasım rota örnekleme: Harita ve hava analizi ortak örnekleme kodunu kullanıyor. Parça mesafeleri her örnekleme çağrısında bir kez hesaplanıyor. Tekrarlanan koordinatların ara noktaları ve tahmini geçiş saatlerini değiştirmediği regresyonla doğrulandı. 438 Android birim testi ve debug APK derlemesi başarılı.

9 Kasım OSRM rota doğrulaması: Bozuk ara koordinatlar, geçersiz geometri ve negatif veya sonlu olmayan süre/mesafe hata olarak ele alınıyor. Geçerli koordinat sırası ve bozuk ara nokta regresyonları dahil 440 Android birim testi ve debug APK derlemesi başarılı. Tablet bağlantısı görüldü. GPS başlangıcı ve rota değişimi için cihaz doğrulaması açık.

9 Kasım rota ekranı: Rota hazır olmadan hava analizi düğmesi gösterilmiyor. Telefon ve tablet yerleşimlerinde rota hatası ve yeniden yükleme eylemi eklendi. Android birim testleri ve debug APK başarılı. SM-T500 üzerinde seyahat kimliğinin rota ve takvim hedeflerine doğru iletildiğini doğrulayan izole test geçti. Rota hatasının görsel testi, GPS başlangıcı ve analiz sırasında rota değişimi cihazda açık.

9 Kasım rota yeniden yükleme cihaz testi: SM-T500 üzerinde izole Compose testinde hata mesajı, yeniden yükleme tıklaması, yükleme boyunca analiz seçeneğinin gizlenmesi ve rota hazır olunca geri gelmesi doğrulandı. Android birim testleri ve debug APK derlemesi başarılı. GPS başlangıcı ve aktif analiz sırasında rota değişimi açık.

9 Kasım rota başlangıcı cihaz testi: SM-T500 üzerinde izole konum sağlayıcısıyla koordinatın adres sorgusu yapılmadan rota başlangıcına aktarıldığı ve konum yoksa boş sonuç döndüğü doğrulandı. Rota yeniden yükleme testi de geçti. Android birim testleri ve debug APK başarılı. Canlı GPS ve OSRM uçtan uca denemesi ile analiz sırasında rota değiştirme cihaz doğrulaması açık.

9 Kasım güzergâh analizi iptali: Rota değiştiğinde eski analiz iptal ediliyor. Eski işlem yeni analizin yükleme durumuna dokunamıyor ve uç nokta tahminleri sonuçla birlikte yayımlanıyor. Android birim testleri ve debug APK derlemesi başarılı. Kontrollü gecikmeyle analiz sırasında rota değiştirme regresyonu ve canlı cihaz doğrulaması açık.

9 Kasım rota analizi regresyonu: İptal edilen analizin tamamlanması kontrollü olarak geciktirildi. Eski analiz sonlandığında yeni analizin yükleme göstergesi açık kaldı ve yalnızca yeni analiz tamamlanınca kapandı. 441 Android birim testi ve debug APK derlemesi başarılı. Gerçek cihazda uçtan uca rota değiştirme kontrolü açık.

9 Kasım rota haritası: Yükleme veya hata durumunda önceki rota çizgisi, işaretçileri ve etiketleri temizleniyor. Ara nokta seçimi güncel rota ve listeye bağlı. İşaretçi dinleyicisi ekran değişiminde kaldırılıyor ve seçim başına kamera tek kez hareket ediyor. Android birim testleri ve debug APK derlemesi başarılı. Harita geçişi ve işaretçi etkileşiminin cihaz doğrulaması açık.

9 Kasım harita işaretçisi: Ara nokta seçimi koordinat araması yerine MapLibre işaretçi kimliğine bağlandı. Harita yeniden çizildiğinde eski eşleştirmeler temizleniyor. Android birim testleri ve debug APK başarılı. Çakışan işaretçilerin fiziksel dokunma davranışı cihazda açık.

9 Kasım güzergâh listesi: Kalkış ve varış satırları hava verisi mevcutsa ilgili ayrıntıyı açıyor. Samsung SM-T500 üzerinde aynı koordinattaki iki geçişin ve iki uç noktanın ayrı ayrı doğru seçildiği doğrulandı. Üç izole cihaz testi, Android birim testleri ve debug APK derlemesi başarılı. Haritada fiziksel işaretçi dokunma doğrulaması açık.

9 Kasım güzergâh listesi eksik tahmin: Analiz tamamlanmadan ve tamamlandığında veri eksik olan satırlar ayrı metinlerle gösteriliyor. SM-T500 cihaz testi bu geçişi doğruladı. Üç izole cihaz testi, Android birim testleri ve debug APK derlemesi başarılı. Haritadaki fiziksel dokunma doğrulaması açık.

9 Kasım güzergâh hava açıklaması: Liste ve ayrıntı kartındaki gündüz/gece metni telefonun anlık saatinden değil tahmini geçiş saatinden üretiliyor. Mevcut seyahat saat dilimi ve gündüz/gece eşikleri kullanılıyor. 442 Android birim testi, üç SM-T500 cihaz testi ve debug APK derlemesi başarılı. Fiziksel harita dokunma doğrulaması açık.

9 Kasım rota kamera hareketi: Tahmin verisi işaretçileri güncellediğinde kamera tekrar tüm rotaya uzaklaşmıyor. İlk otomatik hareket beklerken ara nokta seçilirse rota geneline dönüş atlanıyor. Android birim testleri ve debug APK derlemesi başarılı. Kamera ve işaretçi etkileşiminin cihaz doğrulaması açık. Firebase Admin anahtarı kurulumu kullanıcı isteğiyle ertelendi.

9 Kasım güzergâh ayrıntı kartı: Android geri tuşu açık kartı kapatıyor, kapatma düğmesi erişilebilirlik etiketi taşıyor ve risk metni ayrı satıra alındı. Tahmin eksikse durum açıklanıyor. Android birim testleri ve debug APK başarılı. Kartın büyük yazı ve gerçek harita etkileşimiyle cihaz doğrulaması açık.

9 Kasım güzergâh ayrıntısı büyük yazı: Kart dikey kaydırılabiliyor, uzun başlık üç satırla sınırlı. SM-T500 üzerinde 320 × 250 dp test alanında yüzde 200 yazıyla risk uyarısına kaydırma ve kartı kapatma doğrulandı. Dört cihaz testi, birim testleri ve debug APK başarılı. Canlı harita ve sistem geri tuşu uçtan uca doğrulaması açık.

9 Kasım güzergâh ayrıntısı geri tuşu: Geri işlemi doğrudan kartta ele alınıyor. SM-T500 üzerinde izole test ilk sistem geri basışının kartı kapattığını, ikincisinin üst ekrana iletildiğini doğruladı. İlk koşuda Compose arayüzü bulunamadı, tekrar koşusunda 5/5 cihaz testi geçti. Birim testleri ve debug APK başarılı. Canlı harita işaretçisi uçtan uca kontrolü açık.

9 Kasım MapLibre işaretçi cihaz testi: SM-T500 üzerinde gerçek MapLibre görünümüne yerel stil ile dokunuldu. Aynı koordinatta işaretçi yeniden çizildiğinde güncel geçiş seçildi. Üretim ekranı ve test aynı işaretçi seçim kodunu kullanıyor. Android birim testleri ve debug APK başarılı. Eşzamanlı üst üste işaretçilerin seçimi ve canlı servislerle uçtan uca rota kontrolü açık.

9 Kasım çakışan rota işaretçileri: Aynı koordinattaki kayıtlar tek dokunuşla rota sırasına göre seçime sunuluyor. Yerel MapLibre stiliyle SM-T500 cihaz testi eşzamanlı çakışan iki işaretçinin adaylarını doğruladı. Android birim testleri ve debug APK başarılı. Seçim penceresinin görsel doğrulaması ve canlı servislerle uçtan uca kontrol açık.

9 Kasım çakışan işaretçi seçim penceresi: SM-T500 üzerinde yerel MapLibre stilinde dokunuşla gerçek seçim penceresi açıldı. İkinci geçişe basıldığında pencerenin kapandığı ve ikinci kaydın seçildiği doğrulandı. Android birim testleri ve debug APK başarılı. Canlı servislerle uçtan uca rota kontrolü açık.

9 Kasım giriş sonrası Hava ekranı ve şehir araması: Videoda ilk yükleme görünümü ve sekme dönüşünde açılan şehir ekleme ekranı incelendi. Gezinme denetleyicisi ve NavHost aynı hesap anahtarıyla yenileniyor. Aynı UID için yinelenen auth bildirimi hava/arama durumunu sıfırlamıyor. Oturum değişirse açık sorgu yeniden çalışıyor. Android birim testleri ve debug APK başarılı. Gerçek girişten hemen sonra sekme değiştirmeden Hava ekranının açılması ve şehir sonuçlarının görünmesi cihazda doğrulanacak.

9 Kasım seyahat bildirimi güzergâh riski: Bildirim ekranla aynı yoğun rota örneklemesini kullanıyor. Kısmi tahmin güvenli rota olarak sunulmuyor ve mevcut tehlike uyarıları korunuyor. İki yeni regresyon dahil 444 Android birim testi ve debug APK başarılı. Arka plan bildiriminin gerçek cihaz kontrolü açık. Giriş sonrası Hava ve şehir araması cihaz doğrulaması kullanıcı isteğiyle ertelendi.

9 Kasım ortak güzergâh tahmin hesabı: Ekran ve seyahat bildirimi, rota geometrisinin kalkış ve varış koordinatlarını ve aynı ara geçiş saatlerini kullanıyor. Kısa rota regresyonu dahil 445 Android birim testi ve debug APK başarılı. Arka plan bildiriminin cihaz doğrulaması açık.

9 Kasım değişen güzergâh bildirimi: Tahmin değişse bile bildirim gövdesi güncel risk ve eksik veri özetini gösteriyor. Android birim testleri ve debug APK başarılı. Tüm tahminlerin geçici olarak alınamadığı durumda mevcut günlük döngü bekleniyor. Sınırlı yeniden deneme ve cihaz doğrulaması açık.

9 Kasım güzergâh bildirimi yeniden deneme: Rota veya hava tahmini geçici olarak alınamadığında WorkManager en fazla iki ek deneme istiyor. Rota bulunamazsa, seyahat uygun değilse veya günlük bildirim zaten gönderildiyse bu yol tetiklenmiyor. 446 Android birim testi ve debug APK başarılı. Gerçek cihazda zamanlama ve teslimat kontrolü açık.

9 Kasım ilk güzergâh bildirimi: Hazır bildirimi artık tahmin sonucu alındığında gönderiliyor ve risk özeti içeriyor. İlk bildirim ve sonraki güncellemeler aynı günlük tekrar korumasını kullanıyor. Tahmin tamamen yoksa sınırlı yeniden deneme uygulanıyor. 446 Android birim testi ve debug APK başarılı. WorkManager zamanlaması ve cihaz bildirimi açık.

9 Kasım güzergâh bildirimi teslimat kurtarma: Yarım kalan rezervasyon günlük teslimat sayılmıyor. SM-T500 üzerindeki izole Room testinde önceki süreçten kalan kayıt kurtarıldı ve tek tamamlanmış bildirim oluştu. İlk koşuda testin süreç kimliği eşleşmedi, test düzeltildikten sonra 3/3 cihaz testi geçti. Birim testleri ve debug APK başarılı. WorkManager zamanlaması ve gerçek sistem bildirimi açık.

10 Kasım güzergâh bildirimi Android teslimatı: SM-T500 üzerinde izole güzergâh risk bildirimi Android'e gönderildi. Bildirim metni ve test Activity'sine ilk açılışta ve Activity açıkken iletilen bildirim kimlikleri doğrulandı. Test 1/1 geçti. WorkManager zamanlaması ve gerçek güzergâh ekranına yönlendirme açık.

10 Kasım güzergâh bildirimi yönlendirmesi: Yeni bildirimler ROUTE_WEATHER eylemi ve seyahat kimliğini taşıyor. Eski bağlantılar destekleniyor. Özel karakterli kimlik, yedek kimlik ve geçersiz hedef test edildi. Android birim testleri ve debug APK başarılı. Gerçek güzergâh ekranına cihaz yönlendirmesi ve WorkManager zamanlaması açık.

10 Kasım güzergâh bildirimi gezinme testi: SM-T500 üzerinde izole Android NavHost, ROUTE_WEATHER bildiriminden üretilen hedefi açtı ve özel karakterli seyahat kimliğini değiştirmeden aldı. Birim testleri, debug APK ve hedefli cihaz testi başarılı. Sistem bildiriminden üretim ekranına tek akışta geçiş ve WorkManager zamanlaması açık.

10 Kasım güzergâh bildirimi hesap ayrımı: Samsung tablette Android'in teslim ettiği Intent, üretim çözümleyicisi ve izole Room kaydıyla sınandı. Doğru hesapta güzergâh hedefi ve okundu durumu doğrulandı; farklı hesapta istek reddedildi. 1/1 cihaz testi geçti. Gerçek uygulama ekranı ve WorkManager zamanlaması açık.

10 Kasım seyahat bildirimi ilk zamanlama: Cihazın yerel 09.00 hedefi tek zaman örneğinden hesaplanıyor. Yaz saati değişen günlerde gecikme gerçek geçen süreye göre belirleniyor. İki yeni zamanlama testi dahil Android birim testleri ve debug APK başarılı. WorkManager'ın 24 saatlik tekrarının cihazda gözlemi açık.

11 Kasım seyahat rehberi içerik kapsamı: Kullanıcı hedefi Türkiye il ve ilçeleri. Görünen kartlar CityPersonalityProvider, analiz metni TravelAiHelper üzerinden ayrı içerik kullanıyordu. İki yol LocalTravelGuides kataloğunda birleştirildi. İlçe plan kaydından taşınıyor. İlk içerik geçişi 12 il ve Çeşme/Alaçatı/Urla/Ayvalık içindir. Eksik konumlar genel gezi/yemek metniyle doldurulmuyor. Tüm il ve ilçe kapsamı, güncel etkinlik/restoran entegrasyonu, kişisel tercihlere göre seçim ve görsel cihaz kontrolü açık. Kaynaklar ve kapsam docs/SEYAHAT_REHBERI_KAPSAMI.md içinde.
11 Kasım rehber doğrulaması: İlk derleme Windows çıktı klasörü kilidiyle durdu. Gradle süreçleri kapatılıp tek süreçte tekrar derlendi. Android birim testleri ve debug APK başarılı. İlçe ayrımı, Türkçe/ASCII ad eşleşmesi, kapsam dışı yerler ve görünen rehberin ortak katalog kullanımı test edildi.

11 Kasım deneyim rehberi: Mevcut 12 il ve 4 ilçe/yerleşim için 33 somut deneyim eklendi; sanat, fotoğraf, el sanatları, yerel üretim ve aileyle keşif dahil. Seyahat türüyle eşleşen en fazla iki kart seçiliyor; analiz metni aynı seçimi kullanıyor. Kaynak düğmeleri eklendi. 455 birim testi hatasız geçti. Tüm Türkiye kapsamı, profil ilgi alanlarıyla seçim ve canlı etkinlik uygunluğu açık. Büyük yazı/açık-koyu tema cihaz doğrulaması ayrıca yürütülüyor.
11 Kasım deneyim kartı cihaz doğrulaması: SM-T500 üzerinde ayrı com.havamania test kimliğiyle 2/2 test geçti. Açık/koyu temada %200 yazı boyutu, dar kartta kaydırma, kaynak düğmesine erişim ve doğru URL teslimi doğrulandı. Kaynak tarayıcısındaki sayfa içeriği ve tam seyahat ekranı görsel kontrolü bu testin kapsamında değildir. Play com.havamania.mobile kurulumu değiştirilmedi.

11 Kasım beşli kontrol: Şehirsiz hava ekranına gerçek konum izni akışı ve yaklaşık konum desteği eklendi. Yerel seyahat kaydı başarılı olunca rehber otomatik açılır ve başlangıca kaydırılır. Rehberde tüm alanın kapanma hedefi olması düzeltildi. Ortak normal/cam kartlarda iç içe zemin/çerçeveler kaldırıldı; hava ölçümleri sadeleştirildi ve büyük yazıya esnetildi. Sürümlü Firestore rehber yenileme ve güvenli yayın aracı eklendi. 457 birim, 14 cihaz ve 5 yerel Firebase güvenlik testi geçti. Gerçek tablette yaklaşık konumla şehir çözümü doğrulandı. Canlı asistan izin uç noktası kimliksiz isteğe 401 verdi; gerçek hesapla mutasyon yapılmadı. Kuralların/içeriğin canlı yayını, tam takvim kayıt-sonrası uçtan uca kabul ve tüm özel ekranların görsel kabulü açık. Ayrıntılar docs/11_KASIM_BES_KONTROL.md ve docs/REHBER_YAYINLAMA.md içinde.

11 Kasım B5-1 devamı: Eski bildirim odağının yeni kayıt tarihini geri alması, reddedilen Room yazısının başarı sayılması, telefon düzeninde rehberin görünmemesi, rehber metninin güzergâhı açması ve görünmeyen takvim hata bildirimi düzeltildi. Son tur 7/7 gerçek takvim cihaz testi ve 457 JVM testi başarılı. İlgili 5 Room regresyonu da geçti. Kullanıcı verisi ve Play kurulumu değişmedi; USB ekran ayarı önceki 0 değerine geri yüklendi. Geniş B5-1 fiziksel telefon/gerçek oturum/süreç koşulları nedeniyle açık; sıradaki bağımsız yerel iş B5-2. Ayrıntı: 11_KASIM_OTOMATIK_REHBER_TESTI.md.

11 Kasım B5-2 devamı: İzin reddi ve GPS ayarlarına güvenli yönlendirme, ayarlardan dönüş, çift arama önleme, ilerleme/failure metni, manuel seçim ve 24 saatlik hesaba bağlı son otomatik şehir kaydı eklendi. Manuel varsayılan ve Firebase kuyruğu değişmiyor. 466 JVM testi geçti. Son toplu cihaz turu 9/10; GPS yeniden açılışı sonrası tek şehir çözümü sonuç alamadı, ayrı tekrar 1/1 geçti. Bu bulgu kapatılmadı; geniş B5-2 açık. Rapor: 11_KASIM_KONUM_DAYANIKLILIK_TESTI.md.

11 Kasım B5-2 soğuk başlangıç devamı: GPS yeniden açılışında ilk sağlayıcının 99 ms içinde boş sonuç verdiği ayrıştırıldı. İzin/hizmet kontrolleri ve tek toplam süre sınırı içinde yalnız erken boş sonuca bir kurtarma denemesi eklendi. Gerçek HomeScreen çevrimdışı geri dönüşü, eski önbellek şehir/koordinat onarımı ve eksik nem/rüzgâr/UV gösterimi düzeltildi; rüzgâr soğutmanın hız yerine seçilmesi kaldırıldı. 477 JVM testi, 2/2 gerçek konum testi, 8/8 HomeScreen/önbellek regresyonu geçti; son ölçüm değişikliği ardından HomeScreen 1/1 tekrar geçti. Geniş B5-2 kabulü açık. B5-3 sırasına boş günlük tahmin, yer adı ekleri ve eksik veri eşleyici kontrolleri eklendi. Ayrıntı: 11_KASIM_KONUM_SOGUK_BASLANGIC_VE_OFFLINE.md.

12 Kasım yerel B5-3 devamı: Tablet kullanıcı yanında olmadığı için cihaz kabulü bekletiliyor. Eksik ayrıntı ölçümlerinin varsayılan gözlemlere dönüşmesi ve NaN hissedilen sıcaklığın yuvarlamada çökmesi düzeltildi; gerçek sıfırlar korunur. Boş günlük tahmine açıklama eklendi; şehir eki ve iki yazım hatası düzeltildi. 482 JVM testi ve debug derlemesi başarılı. Görsel kabul açık. Sıradaki yerel iş: eksik veride uygunluk/öneri yorumları, günlük-saatlik sayısal doğrulama, hava kodu ve yön tutarlılığı. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım yerel uygunluk devamı: Eksik ölçümlerin tam uygunluk/100 puan sayılması engellendi; bilinmeyen değerlendirme nötr kart ve veri eksik açıklamasıyla gösteriliyor, bilinen riskler korunuyor. Ana ekran önerileri eksik/eski veride 20 derece varsaymıyor; soğuktan taze kar sonucu çıkarmıyor. Altı yeni test dahil 488 JVM testi ve debug derlemesi başarılı. Cihaz kabulü açık. Sırada asistanın ayrı yanıt yollarındaki varsayılanlar, günlük/saatlik sayısal doğrulama, hava kodu/yön tutarlılığı ve eski skor önbelleği var. Ayrıntılar: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım yerel asistan devamı: Kıyafet/aktivite/yağış yanıtlarında eksik veri koruması, gerçek hissedilen sıcaklık ve anlık sıcaklık seçimi düzeltildi. Yanlış şehir/gelecek tarihe bugünkü ölçümler taşınmıyor. Yağış kesinlik ifadeleri kaldırıldı; yüksek UV/nem ideal aktivite sayılmıyor. Hafta sonu eksik/eski yağışta gün seçmiyor. Yedi yeni regresyon dahil 495 JVM testi ve debug derlemesi başarılı. Cihaz ve canlı asistan doğrulaması açık. Sırada seyahat yanıtı, hafta sonu tarih/gün seçimi, ölü genel yanıt yolu ve günlük/saatlik sayılar var. Ayrıntı: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım hafta sonu/seyahat devamı: Tarihle aynı hafta sonu eşleştirme, mükerrer kayıt reddi, pazar günü geçmiş cumartesiyi önermeme, iki riskli/eşit yağışlı günde kazanan dayatmama eklendi. Analizsiz seyahat hava uygun iddiası ve çağrılmayan eski genel yanıt kodu kaldırıldı. Altı yeni test dahil 501 JVM testi ve debug derlemesi başarılı. Sıradaki yerel iş günlük/saatlik sayısal doğrulama, eksik kod/yön, seyahat özet kartı varsayımları ve eski skor önbelleği. Tablet kontrolleri açık. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım günlük/saatlik doğrulama: Sonlu olmayan sıcaklıklar, ters günlük aralıklar ve bozuk tarih/saatler eleniyor; geçersiz yağış yüzdeleri bilinmiyor kalıyor. Eksik yönün kuzey sayılması ve sıcaklık yokken rüzgâr soğutma hesaplanması kaldırıldı. Seyahat kartında 20 derece yedeği kaldırıldı. Beş yeni test dahil 506 JVM testi ve debug derlemesi başarılı. Sırada eski skor önbelleği, ana kart günlük tarih eşleştirmesi, eksik kod, günlük UV ve mükerrer tahmin zamanı kontrolleri var. Tablet kabulü açık. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım tarih/önbellek devamı: Ana kart ve uygunluk hesabı şehrin yerel bugünün tek kaydını kullanıyor; eksik/mükerrer günde başka günün ölçümleri taşınmıyor. Günlük UV yedeği doğrulanıyor. Sürümsüz eski önbellek skoru ölçümler korunarak değerlendirme güncellenmeli durumuna alınıyor. Dört yeni test dahil temiz derlemede 510 JVM testi, 0 hata; debug APK başarılı. Sırada önbellekte gün değişimi/süre aşımı, eksik hava kodu ve yinelenen zamanlar var. Cihaz kabulü açık. Ayrıntı: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım gün devri/veri yaşı devamı: Önbellek yerel gün değişimini tazelik koşuluna kattı; önceki günün günlük ölçümleri yeni güne taşınmıyor, eski veri uygunluk skoru nötrleşiyor. Eksik/tanınmayan mevcut hava kodunun metni Veri yok oldu. Dört yeni test dahil 514 JVM testi ve debug derlemesi başarılı. Sırada açık ekranda gün devri, bilinmeyen kod simge/tema/uygunluk ve mükerrer tahmin zamanları var. Room/ekran ve tablet kabulü açık. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım görünür ekran devamı: STARTED sırasında 30 saniyelik veri yaşı/gün devri kontrolü ve yeni eskime geçişinde tek yenileme eklendi. Bilinmeyen kod uygunluk/günlük öneride eksik sayılır; ana kart nötr simge, sıfır bulut yoğunluğu ve atmosfer efektsiz görünüm kullanır. Üç yeni test dahil 517 JVM testi ve debug derlemesi başarılı. Yaşam döngüsü/ağ isteği ve görsel cihaz kabulü açık. Sırada çevrimiçi geri dönüş kurtarma, mükerrer tahmin zamanları ve geç risk/öneri sonuçları var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım bağlantı kurtarma devamı: Görünür ekranda çevrimiçi geçiş ve STARTED dönüşü eski veri/hata için kontrollü yenileme istiyor; aktif istek ve UID kontrolleri var. Geciken akıllı uyarıya seçim/veri/hesap kontrolü eklendi; şehir değişiminde önceki uyarılar temizleniyor. Yerel günlük öneri kuyruğu kaldırıldı. Üç yeni karar testi dahil 520 JVM testi ve debug derlemesi başarılı. Ağ/lifecycle/geç sonuç cihaz kabulü açık. Sırada mükerrer ve sırasız tahmin zamanları ile ek iptal testleri var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım tahmin sırası devamı: Yeni saatlik/günlük eşleme ve eski liste seçim yardımcıları kronolojik sıralıyor; aynı parse edilmiş zamanın tüm mükerrer adayları belirsiz kabul edilip eleniyor. Üç yeni regresyon dahil 523 JVM testi ve debug derlemesi başarılı. Sırada geç uyarı/öneri iptal testleri, ham önbellek tüketicileri ve mükerrer güneş saatleri var. Cihaz/görsel kabul açık. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım gecikmiş iş devamı: Uyarı ayar okumasına ortak geçerlilik/iptal koruması bağlandı; isteğe bağlı okuma hatası ana hava yüklemesine taşınmıyor. Beş kontrollü gecikme testi dahil 528 JVM testi ve debug derlemesi başarılı. ViewModel/Firebase/ekran cihaz kabulü açık. Sırada ham önbellek tüketicilerinin mükerrer zaman ve güneş saati eşleştirmesi var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım tüketici/güneş eşleştirme devamı: Eski Room JSON listeleri okunurken sıra/mükerrer zaman normalizasyonu uygulanıyor. Tarihli asistan ve seçili güneş kaydı tek eşleşme arıyor; saatlik gündüz/gece hesabı mükerrer veya ters güneş aralığı kullanmıyor. Üç yeni test dahil 531 JVM testi ve debug derlemesi başarılı. Sırada akıllı yağmur uyarısının gerçek gelecek iki saat hesabı ve günlük ters güneş saatleri var. Cihaz kabulü açık. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım iki saatlik yağış devamı: Akıllı uyarı gerçek Instant aralığı ve şehir saat dilimiyle saat dilimlerini seçiyor; eski/mükerrer/belirsiz saatler uyarı üretmiyor. Yerel gün tekrar anahtarı ve yağmur kapalı ayarı sınandı. Altı yeni test dahil 537 JVM testi ve debug derlemesi başarılı. Bildirim/cihaz kabulü açık. Sırada bekleyen uyarıların zamanla temizlenmesi, diğer türlerin veri yaşı ve ters güneş aralığı var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım uyarı ömrü devamı: Görünür ekran kontrolü aynı kullanıcı/veri için mevcut ayarlarla uyarıları yeniden hesaplıyor; eski veri ve geçmiş yağış dilimi uyarısı korunmuyor. Ondalıklı sıcaklığın rakamları birleşmiyor; geçersiz ölçümler uyarı üretmiyor. Dört yeni test dahil 541 JVM testi ve debug derlemesi başarılı. Motor doğrulandı; ViewModel/cihaz döngüsü açık. Sırada canlı uyarı tercihi değişikliği ve günlük ters güneş aralığı var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım canlı uyarı tercihi devamı: Hesaba bağlı ayar Flow gözlemcisi değişiklikleri aynı hava kaydına uyguluyor; hava yenilemesi beklenmiyor. Eşit değerler tekrarlanmıyor, eski hesap olayları reddediliyor; hata uyarıları temizliyor ve iptal korunuyor. Beş yeni test dahil 546 JVM testi ve debug derlemesi başarılı. Gerçek ayar ekranı/cihaz kabulü açık. Sırada günlük ters güneş aralığı ve geçici ayar okuma hatasından toparlanma var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım güneş/ayar kurtarma devamı: Ana/günlük/eski önbellek güneş alanlarında ters/eşit aralıklar temizleniyor; tek geçerli ölçüm korunuyor. Ayar okuması geçici IO hatasında 1/2 saniyeyle en fazla iki ek kez deneniyor; iptal/UID kontrolü var ve aynı değer kurtarmada tekrar uygulanıyor. Beş yeni test dahil 551 JVM testi ve debug derlemesi başarılı. Cihaz/DataStore gerçek hata kabulü açık. Sırada tercih yazımının UID ayrımı, hata bildirimi ve kalıcılık incelemesi var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.

12 Kasım tercih yazımı devamı: SmartAlertViewModel UID/nesil koruması, hesap değişiminde temizleme, kaydetme/yükleme durumu ve görünür hata eklendi. Düğmeler hazır değilken/yazma sürerken kapalı, değerler disk gözlemcisinden geliyor. Dört yeni test dahil 555 JVM testi ve debug derlemesi başarılı. DataStore yeniden açılış ve gerçek hesap/ekran kabulü açık. Sırada profil tercihleri ve hesap silme sırasında geç yazılar var. Rapor: 12_KASIM_YEREL_HAVA_VERI_VE_METIN.md.
