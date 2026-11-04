# Güncel yayın sırası — 4 Kasım 2026

Ana kontrol listesinde 59 geniş kabul maddesinin 22’si tamamlandı işaretli, 37’si açık. Bu, doğrulanmış bug sayısı veya tek oturumda bitecek adım sayısı değildir. Son yerel alt düzeltmeler canlı/çok cihazlı/yayın kontrollerini kendiliğinden kapatmaz. Ayrıntılı kaynak: YAYIN_ONCELIKLERI.md ve YAYIN_DURUM_RAPORU.md.

| Öncelik | Çalışma paketi | Açık kapanış koşulu |
|---|---|---|
| 1 | Asistan izinleri ve platform bağlantısı | 4 Kasım tablet geri bildirimiyle üç düğmenin çalıştığı doğrulandı. İzin metninin yüklenmesini engelleyen adres erişim hatası açık. Cihazın `api.havamania.com` DNS çözümlemesi, HTTPS erişimi ve ardından canlı izin metni, kayıt/geri çekme ve asistan istek sözleşmesi doğrulanacak. |
| 2 | Bildirim dayanıklılığı | Aktif teslim sırasında liste temizleme yarışı yerelde doğrulandı. Dokunma sonrası okundu ve sayaç davranışı yerelde doğrulandı. Gerçek tepsi, izin, kanal ve Activity yaşam döngüsü açık. |
| 3 | Tüm ekran ve durumların görsel denetimi | Boş/dolu/yükleme/hata/offline, telefon/tablet, açık/koyu/mevsimsel tema, yatay/klavye, büyük yazı/TalkBack; yazım, tipografi, geçiş ve premium bütünlük. |
| 4 | Hesap, Firebase ve senkronizasyon | A/B izolasyonu, silme/kurtarma, profil/şehir/seyahat/AI/tercih senkronizasyonu, çok cihaz çatışmaları ve avatar/Storage/Blaze. |
| 5 | Hava, güzergâh ve kişiselleştirme | 10 gün/48 saat, saat dilimi, eksik/eski veri ve kritik riskler; tercihlerle karşılaştırmalı gerçek kişiselleştirme. |
| 6 | Mimari, performans, temizlik ve güvenlik | Native/RN/FastAPI/network/depolar, async/iptal, bellek/recomposition, ölü/mükerrer kod, sırlar/loglar/kurallar, PostgreSQL dağıtım provası, teknik borç. |
| 7 | Üretim ve mağaza hazırlıkları | Servis lisans/kota/kapasite/gözlemleme, KVKK/gizlilik/koşullar/silme URL, Play Console kuruluşu ve mağaza beyanları. |
| 8 | Release ve son kabul | Gerçek imzalı R8 AAB, 16 KB cihaz/emülatör, release dayanıklılığı ve Google Play iç test/pre-launch. |

Sekiz satır bir çalışma sırasıdır; her paket alt test ve dış ortam bağımlılıkları içerir. Kullanıcı tercihi yalnız yerel testtir. Canlı hesap/izin değişikliği, ücretli plan veya mağaza işlemi yapılmış sayılmaz.

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
