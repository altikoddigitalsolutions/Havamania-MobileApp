# 11 Kasım: konum, otomatik rehber, tasarım, asistan ve güncelleme

Kalan işler GUNCEL_YAYIN_SIRASI.md dosyasında B5-1–B5-6 kimlikleriyle açık kutular olarak izlenir. Otomatik rehberin yerel ana akışı 7 cihaz testiyle doğrulandı; sonuç ve kalan sınırlar 11_KASIM_OTOMATIK_REHBER_TESTI.md içindedir. Sonraki bağımsız yerel iş B5-2 konum izin dayanıklılığıdır. Gerçek konum çözümü ve geçen yerel izin testleri tamamlanmış kanıt olarak korunur; bunlardan geniş uçtan uca veya canlı kabul sonucu çıkarılmaz.

## 1. Mevcut konumdan hava şehri

Bulgu: LocationPermissionFlow tanımlıydı fakat hiçbir ekranda çağrılmıyordu. Ortak hasLocationPermission yalnızca hassas konumu kabul ediyordu.

Düzeltme: Şehirsiz hava ekranı konum izni akışını bir ekran oturumunda bir kez sunuyor. Hassas veya yaklaşık konum izni yeterli. İzin verilirse otomatik moda geçilip güncel şehir çözülüyor. Şehirsiz ekranda ayrıca “Mevcut konumumu kullan” ile yeniden deneme var. Reddetme manuel şehir seçimini engellemiyor. Mevcut manuel şehir otomatik olarak değiştirilmez.

Mevcut 8 WeatherStartupLocationTest; başarılı çözüm, yavaş servis, iptal, manuel seçimin geç yanıttan korunması ve servis başarısızlığını kapsar. Gerçek tablet için CurrentLocationDeviceTest eklendi. Test kendi com.havamania QA paketini öne açıp yalnızca o pakete yaklaşık konum izni verir. Play uygulamasının izinlerine dokunmaz.

## 2. Seyahat sonrası otomatik rehber

Kayıt yerel Room veritabanına başarıyla yazıldıktan sonra, ağ senkronizasyonunu beklemeden rehber açılır. Tarih kaydedilen seyahate, filtre uygun listeye alınır. Hesap değişmişse callback çalışmaz. Hatalı/çift kayıt doğrulamasından geçmeyen seyahat için rehber açılmaz. Genişletme kimliği tarih/filtre değişimiyle sıfırlanmaz. Rehber başlangıcı görünür alana getirilir.

Rehberin içeriğine dokunmanın yanlışlıkla kapatmaya yol açması da düzeltildi; yalnızca başlık açar/kapatır. Tam takvimde gerçek kullanıcı verisiyle kayıt sonrası uçtan uca kabul ayrıca doğrulanmalıdır.

## 3. İç içe kart görünümü

Ortak HavamaniaCard ve HavamaniaGlassCard bileşenleri içinde kullanılan ikinci kart tekrar zemin, çerçeve ve gölge çizmez. Bu ortak bileşenler hava, takvim/rota, profil düzenleme, kişiselleştirme, şehirler ve asistan geçmişinde kullanılır. Özel Surface bileşenlerinin tümü bu mekanizmaya dahil değildir.

Görseldeki hava ölçümleri düz zeminli ve çerçevesiz hale getirildi. Sabit 130 dp yükseklik minimum yüksekliğe çevrildi; açıklamadaki tek satır kesmesi kaldırıldı ve metin kontrastı artırıldı. Büyük yazı testi eklendi. Tüm ekranların bütün durumlarıyla canlı görsel kabulü tamamlanmış sayılmaz.

## 4. Asistan izinleri

Mevcut ViewModel hesap başlangıcında izinleri yükler. Geçerli kabul varsa tekrar onay istemez; kabul eksikse veya sağlık izni yeniden incelenmeliyse pencere açılır. Başarısız yükleme, tekrar deneme, geri çekme, bekleyen geri çekme ve kapatma senaryolarının kontrolcü testleri ile gerçek dokunma kullanan cihaz testleri çalıştırılır. İzin hatası onay yerine geçmez.

11 Kasım ağ kontrolü: api.havamania.com çözümlendi, /v1/chatbot/consent kimliksiz GET isteğine HTTP 401 verdi. Bu, uç noktanın erişilebilir olduğunu gösterir; geçerli hesapla onay veya geri çekmenin canlı doğrulaması değildir. Kullanıcının önceki yerel test sınırı korunur; gerçek hesabın izinleri bu turda değiştirilmez.

## 5. Rehber güncellemeleri

Firestore üzerinden sürümlü editoryal rehber güncellemesi, IO üzerinde kalıcı önbellek, süre dolumu, konum/sürüm/kaynak doğrulama ve açık ekranda periyodik yenileme eklendi. Görünen metinler yeni içerik geldiğinde yeniden hesaplanır. İnternetsiz kullanım paket içindeki veya son geçerli içeriği kullanır.

İstemcilerin içerik yayımlaması engellendi. Yerel Firebase emülatöründe 5/5 güvenlik testi geçti. Yayın aracı süresi dolmuş içerik ve güvensiz URL'leri reddetme, geçerli payload ve sabit konum kimliği yönünden yazma yapmadan doğrulandı.

Canlıda etkinleşmesi için kurallar dağıtılmalı ve yetkili editoryal içerik yayımlanmalıdır. Bu turda canlı Firebase'e kural veya içerik yazılmadı. [Yayın şeması ve araç kullanımı](REHBER_YAYINLAMA.md).

## Doğrulama kaydı

İlk Android derlemesi ve 457 birim testi başarılı. İlk cihaz turunda 8 testten 7'si geçti; konum testi kurulumda sıfırlanan izin nedeniyle durdu. İzin test içinde verilecek şekilde düzeltildi; son cihaz sonuçları aşağıya eklenir.

İkinci tur: Samsung SM-T500 üzerinde 14/14 cihaz testi başarılı. Gerçek yaklaşık konum izniyle şehir çözümü, hata durumunda tekrar deneme/vazgeçme/geri çekme, onay etkileşimleri, açık/koyu tema, büyük yazı ve kaynak düğmeleri kapsandı. Kullanıcının Play kurulumu değiştirilmedi. Rüzgâr kartının minimum yüksekliği ve ölü basma animasyonu temizliği sonrasında hedefli hava testi tekrar çalıştırılıyor.

Son hava kartı değişiklikleri sonrasında 457 birim testi ve hedefli büyük yazı cihaz testi yeniden başarılı oldu. Konum düğmesi şehirsiz ekranın ana eylemleri içine yerleştirilip sistem çubukları için boşluk korundu. Normal uygulama kimliğiyle son debug derlemesi ayrıca kontrol ediliyor.

Son doğrulama: Konum düğmesinin önizleme çağrısı uyumu düzeltildi. Normal com.havamania.mobile kimliğiyle testDebugUnitTest ve assembleDebug başarılı; 457 birim testi hatasız. Son kaynaklar debug APK'ya dahil. Bu turda Play kurulumu güncellenmedi ve imzalı release AAB yeniden üretilmedi.

B5-1 yerel devam sonucu: 11_KASIM_OTOMATIK_REHBER_TESTI.md. Beş yeni alt düzeltme, 7/7 takvim cihaz testi ve 457 JVM testi başarılı; gerçek hesap değişimi ve fiziksel telefon/süreç kabulü açık tutuldu.

B5-2 devamı: 11_KASIM_KONUM_DAYANIKLILIK_TESTI.md içinde yeni izin/GPS akışı, 24 saatlik otomatik şehir kaydı ve kanıtlar var. 466 JVM testi geçti; toplu cihaz turu 9/10, GPS sonrası şehir çözümü ayrı tekrar 1/1. İlk başarısız deneme kapatılmadı; gerçek hava ekranında çevrimdışı soğuk açılış ve diğer geniş kabul koşulları açıktır.
