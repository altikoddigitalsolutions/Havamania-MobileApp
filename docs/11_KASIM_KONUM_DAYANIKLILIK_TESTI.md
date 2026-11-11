# B5-2 — Konum izinleri ve çevrimdışı geri dönüş

## Düzeltilenler

- Ret sonrası sessiz kalma kaldırıldı. Konumu elle seçme ve uygulama ayarlarından izin verme yolları var. Hassas veya yaklaşık konum yeterli.
- Cihaz konumu kapalıysa ayrı açıklama ve konum ayarlarına bağlantı gösterilir. Ayarlardan dönünce gerçek izin/hizmet durumu yeniden kontrol edilir. Normal her geri dönüş yeni konum isteği başlatmaz.
- Otomatik moda geçiş ile doğrudan yenilemenin aynı konum aramasını iki kez başlatması önlendi. Hâlihazırda otomatik arama yapan ekran yeniden otomatik arama başlatmaz; açık kullanıcı isteğiyle tekrar deneme yapılabilir.
- Konum aranırken ilerleme metni var. Yeniden deneme düğmesi arama sırasında devre dışı; manuel şehir seçimi kullanılabilir kalır.
- Şehir çözülemediğinde açıklama gösterilir. İptal edilmiş arama hata bildirimi veya eski şehir yayımlamaz. Başka hesap/manuel seçime ait akış kapatıldığında geç izin sonuçları yok sayılır.
- Konum hizmeti kontrolü güvenli hale getirildi; izinsizken sağlayıcıya başvurulmaz, hizmet durumunun okunamaması crash oluşturmaz.
- Kullanılmayan eski LocationPermissionFlow kaldırıldı; tek akış kullanılıyor. Başarılı hava ekranındaki şehir seçicisinden de mevcut konum yeniden istenebilir.

## Son otomatik şehir kaydı

Otomatik bulunan şehir manuel varsayılanı değiştirmeden `havamania:<uid>:last_automatic_weather_city` anahtarında saklanır. Kayıt yalnız otomatik modda, güncel hesap için ve hesap silme beklemiyorken yazılır. Firebase şehir senkronizasyon kuyruğu oluşturmaz.

Kayıt 24 saat içinde geri dönüş için kullanılabilir. Süresi dolmuş, geleceğe tarihli veya bozuk kayıt okunurken temizlenir. Manuel modda kullanılmaz. Hesap silmenin mevcut UID öneki temizliği bu anahtarı da kapsar. Bu 24 saat, konum kaydının kullanım süresidir; hava tahminlerinin tazelik kurallarını değiştirmez.

Konum yeniden doğrulanamazsa kayıtlı şehir korunur. Hava ekranında kayıtlı şehrin gösterildiği açıklanır; eski koordinatlar doğrulanmış güncel konum gibi sunulmaz. Önbellek yazılması hava konumu tercihi gözlemcisini yeniden tetikleyip sonsuz konum sorgusu oluşturmaz.

## Doğrulama

**466 JVM testi hatasız geçti.** Bu turda 7 izin/ayar kontrolcüsü testi ve konum bulunamaması/iptal bildirimi için 2 test eklendi.

Samsung SM-T500 / Android 12 üzerinde son toplu koşu **9/10 başarılı, 0 atlanan**:

1. Gerçek Android izin reddi, uygulama ayarlarını açma, sistem geri hareketi ve manuel seçime devam.
2. İzin açıklamasından manuel seçim.
3. Ret sonrası ayarlar/dönüş kontrolü ve tek arama başlatılması (izole gateway).
4. GPS kapalı açıklaması, ayarlar iptali ve manuel seçim (izole gateway).
5. Konum bulunamaması metni, ilerleme ve manuel seçimin erişilebilirliği.
6. Gerçek DataStore dosyasında yeniden açılış, A–B ayrımı, manuel varsayılanın korunması ve gözlemci döngüsü oluşmaması.
7. Manuel mod/eski hesap/silme engeli/geçersiz koordinatla önbellek yazılmaması.
8. Süre aşımı, bozuk/gelecek tarihli kayıt temizliği ve UID bazlı silme.
9. Gerçek cihaz konum hizmetini kapatma, konum ayarlarını açma ve açık dönünce bir kez arama başlatma. Test eski cihaz konum ayarını finally içinde geri yükler.

10. **Açık bulgu:** GPS hizmeti kapatılıp açıldıktan hemen sonraki yaklaşık konumla şehir çözümü bir kez 15 saniyede sonuç alamadı (test süresi yaklaşık 16 saniye). Daha sonra aynı test tek başına yeniden çalıştırıldığında 1/1 geçti. Bu tekrar, ilk başarısızlığı kapatmaz; hizmet yeniden açılışındaki ilk fix/geocoder hazır olma koşulu ayrıca incelenecek. Çökme görülmedi; yazılımın veri yok durumunda kayıtlı şehir ve manuel seçim sunması yerel testlerle doğrulandı.

## İzolasyon ve kalan kabul

- QA kimliği com.havamania; Play'deki com.havamania.mobile kurulumu ve izinleri değiştirilmedi. Önbellek testleri ayrı preferences dosyaları kullanır. Canlı kullanıcı hesabına veya Firebase'e yazılmadı.
- Test sırasında değiştirilen cihaz konum hizmeti önceki açık durumuna, USB ekran ayarı önceki 0 değerine geri döndürüldü.
- Gerçek sistem izin reddi ve GPS ayarları sınandı. Ağ kesintisi/konum bulunamaması callback testleri ve disk geri dönüşü izole testlerdir; fiziksel internet kesintisiyle tam HomeScreen–hava servisi–önbellek kabulü yerine geçmez.
- Açık kalanlar: GPS yeniden açılışının ilk konum sonucu, gerçek hava ekranında çevrimdışı soğuk açılış, sistemin kalıcı ret nedeniyle izin penceresini hiç göstermediği gerçek cihaz durumu, fiziksel telefon/üretici çeşitliliği, gerçek Firebase hesap değişimi ve süreç ölümü/izin penceresi sırasında yeniden oluşturma. Kalıcı ret için uygulamanın ret/ayarlar mantığı birim testlerinde kapsandı; gerçek cihaz günlükleri bu turda iki normal sistem penceresi reddi ve sıfır penceresiz ret gösterdi.
- Yeni release AAB üretilmedi. Bu değişiklikler Play'e yayımlandı sayılmaz.

## Sonraki doğrulama
İlk boş sağlayıcı sonucu ayrıştırıldı ve sınırlı kurtarma eklendi. GPS yeniden açılışı 2/2 konum koşusunda geçti; gerçek HomeScreen kontrollü çevrimdışı testinde açıldı. Önceki açık maddelerin güncel kapsamı ve 477 JVM / cihaz kanıtları için [devam raporuna](11_KASIM_KONUM_SOGUK_BASLANGIC_VE_OFFLINE.md) bakın. Fiziksel ağ kesintisi ve tam giriş/launcher akışı hâlâ açık.
