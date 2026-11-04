# Güncel yayın sırası — 4 Kasım 2026

Ana kontrol listesinde 59 geniş kabul maddesinin 22’si tamamlandı işaretli, 37’si açık. Bu, doğrulanmış bug sayısı veya tek oturumda bitecek adım sayısı değildir. Son yerel alt düzeltmeler canlı/çok cihazlı/yayın kontrollerini kendiliğinden kapatmaz. Ayrıntılı kaynak: YAYIN_ONCELIKLERI.md ve YAYIN_DURUM_RAPORU.md.

| Öncelik | Çalışma paketi | Açık kapanış koşulu |
|---|---|---|
| 1 | Asistan izinleri ve platform bağlantısı | 3 Kasım ekran raporu açık: retry/revoke/cancel tepkisizliği öncelikli. Bekleme sırasında kapatma ve geç sonucun pencereyi yeniden açması düzeltildi; gerçek uygulama/servis nedeni henüz doğrulanmadı. Canlı izin metni, kayıt/geri çekme ve asistan istek sözleşmesi ayrıca doğrulanacak. |
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
