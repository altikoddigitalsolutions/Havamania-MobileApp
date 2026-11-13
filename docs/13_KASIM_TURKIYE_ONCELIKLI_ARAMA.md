# 13 Kasım — Türkiye öncelikli konum araması ve tablet dönüşü

## Uygulanan istek

Konum aramasında önce Türkiye sonuçları, ardından dünya sonuçları gösterilir. Her ülke grubunda yazılan metinle başlayan adlar belirsiz eşleşmelerden önce gelir. Türkçe harf/büyük-küçük harf normalizasyonu kullanılır.

Yalnız mevcut beş global sonucu sıralamak yeterli değildi: Türkiye sonucu listeye girmeyebiliyordu. Ortak repository araması Türkiye filtreli ve global sorguyu paralel gönderir (her biri en fazla 10 sonuç). Kimliği aynı kayıtlar birleştirilir. Global kapsam kaldırılmadı. Hava, seyahat varış/kalkış ve repository aramasını kullanan diğer yollar aynı sıralamayı kullanır.

Bir sorgu hata verirse diğer kullanılabilir sonuçlar korunur. Kullanılabilir sonuç yokken sorgulardan biri hata verdiyse boş liste yerine hata korunur. İptal iki alt sorguya yayılır. Bir alt sorgu çok yavaşsa sonuçların gösterim süresi mevcut üst arama zaman aşımına bağlıdır; kısmi sonucu erken gösterme bu turda eklenmedi.

Ülke filtresinin adı ve sonuç sınırı [Open-Meteo belgesinden](https://open-meteo.com/en/docs/geocoding-api) doğrulandı. Salt okunur canlı bal/TR sorgusu Balıkesir, Bala ve Balya yanında Batman da döndürdü. Bu nedenle önek eşleşmesi ayrı olarak öne alındı.

## Doğrulama

- 567 JVM testi, 0 hata. Yeni arama için 7 regresyon ve ters güncel sıcaklık aralığı için 1 regresyon eklendi.
- SM-T500 / Android 12: 2/2 yeni arama arayüz testi. Gerçek WeatherCitySearchContent üzerinde açık/koyu temada Balıkesir Bali'den yukarıda; dokunma doğru kaydı döndürdü. Ağ yerine kontrollü sonuçlar kullanıldı; gerçek hesapla uçtan uca arama bu testin kapsamında değil.
- Aynı tablette bekleyen izole DataStore regresyonları 3/3 geçti: ProfileSnapshotDeletionDeviceTest ve CityPreferencePublicationDeviceTest. Gerçek profil hesabı/Firebase mutasyonu yapılmadı. Bu testler yeni epoch korumasının tüm gerçek hesap silme akışını kapsamaz.
- Gradle testDebugUnitTest ve iki connectedDebugAndroidTest koşusu başarılı; debug QA uygulaması/test APK üretildi. Release AAB üretilmedi.
- İlk tam test turunda tarihe bağlı eski bir test başarısızdı. Teste sabit now verildi. Aynı incelemede ana kartın ters minimum–maksimum aralığını gösterebildiği bulundu; bu durumda iki alan da bilinmiyor gösterilecek şekilde düzeltildi.

## Çalışma alanı ve izolasyon

Eski Desktop/a klasörü mevcut değildi; son gönderilen a07bd6f commit'ini içeren C:/Havamania-MobileApp kopyasında devam edildi. Mevcut mobile/android/gradle.properties kullanıcı değişikliği korundu; değiştirilmedi. Git yapılandırması değiştirilmedi.

Tablet testleri ayrı com.havamania QA paketinde yürütüldü. Play com.havamania.mobile kurulumu değiştirilmedi. Tablet kilidi/konum izni/USB ekran ayarları değiştirilmedi. Bu tur push yapılmadı.

## Sıradaki kabul

- Gerçek uygulama oturumunda hava ve seyahat kalkış/varış aramasına bal/iz/ank gibi sorgular girerek uçtan uca doğrulama.
- Aynı adlı farklı ülkelerin satırlarında ülke bilgisinin görünürlüğü.
- Önceki profil/uyarı ayarı ekranlarının gerçek DataStore ve UI kabulü; tema/diğer doğrudan tercih yazıları ve sessiz profil ret bildirimi.
- Önceki canlı Firebase/backend, mağaza ve imzalı release kabul maddeleri açık kalır.
## Devam — Ülke satırları ve kimliksiz konumlar

Hava aramasında il/ülke açıklaması görünür hale getirildi. Seyahat kalkış/varış arama bileşeni de aynı açıklamayı kullanır; eksik il adı boş virgül üretmez, aynı il adı tekrarlanmaz. Ülke adı yoksa ülke kodu; ikisi de yoksa açık bilinmiyor metni gösterilir.

Kimliği olmayan farklı konumlar yalnız id=0 nedeniyle tek satıra düşmez. Servis kimliği varsa o, yoksa ülke kodu/ad/koordinat birleşimi liste anahtarı olur. Ortak arama sonuç birleştirmesi de aynı anahtarı kullanır. Diğer ekranlardaki seçili/kayıtlı şehir karşılaştırmalarının yalnız id=0 ile çalışıp çalışmadığı ayrıca incelenecek.

Son tur: 571 JVM testi, 0 hata; SM-T500 üzerinde 4/4 cihaz testi. İkisi önceki Türkiye önceliği açık/koyu tema regresyonu, ikisi yeni aynı adlı Fransa/ABD konumlarının hava ve seyahat bileşeninde ayrı görünmesi ve doğru kaydın seçilmesi. Bu testler kontrollü sonuçlarla gerçek Compose bileşenlerini kullanır; gerçek oturumda yazı yazarak ağdan sonuç getirme ve seyahat kaydetme kabulü yerine geçmez.

Gradle testDebugUnitTest ve connectedDebugAndroidTest başarılı. Play kurulumu, kullanıcı hesabı ve Firebase verileri değiştirilmedi. Kullanıcının gradle.properties değişikliği korundu; push yapılmadı. Sırada seçili/kayıtlı konum kimliği eşleştirmesi ve önceki profil/uyarı ayarı ekranı kabulü var.

## Devam — Seçili ve varsayılan konum eşleştirmeleri

Hava seçicisinin şehir/ilçe adına bakarak farklı ülkelerdeki aynı adlı konumları seçili işaretlemesi kaldırıldı. WeatherViewModel güncel UID'ye ait istenen konumun koordinatını ve şehir/ilçe bilgisini kullanır; API'nin tahmin ızgarası koordinatı veya yalnız görünen isim kullanılmaz. Geçersiz koordinat seçili sayılmaz.

Şehir yönetiminde öneri ve kayıtlı satırlar ortak kayıt anahtarını kullanır. id=0 olan farklı konumlar aynı LazyColumn anahtarına düşmez ve hepsi varsayılan sayılmaz. Öneriler tekilleştirildikten sonra ilk beş alınır. İl/ülke açıklaması bu ekranda da ortak biçime alındı.

Son tur: 575 JVM testi, 0 hata; SM-T500 üzerinde 6/6 cihaz testi. Dört önceki arama/ülke satırı regresyonu ve iki yeni şehir yönetimi testi geçti. Yeni testler id=0 aynı adlı önerilerin ayrı kalmasını, tek varsayılan işaretini ve doğru ülkeye ait geri çağrıyı doğrular. Ağ, gerçek kayıt işlemi ve kullanıcı oturumu yerine kontrollü Compose girdileri kullanıldı.

Bulut kayıt kimlikleri, silme ve senkronizasyon kuyruğunun biçimi değiştirilmedi. İd'siz eski kayıtların gerçekten saklanması/silinmesi ve sunucuyla senkronizasyonu ayrıca denetlenecek; UI eşleştirmesi bu kalıcılık kabulünü kapatmaz. Görünen liste tekilleştirilirken kullanılan şehir sayısının silme düğmesi kurallarıyla tutarlılığı da sırada.

Play kurulumu, Firebase hesabı ve mevcut gradle.properties değişikliği korundu. Yeni release veya push yapılmadı. Önceki ayar ekranı, gerçek oturum ve mağaza kabul maddeleri açık.
