# Görsel bütünlük — iç içe dikdörtgen kabul matrisi

24 Kasım 2026 kullanıcı bildirimiyle yeniden açık ve öncelikli. Bu dosya inceleme planıdır; aşağıdaki ekranlar görsel olarak tamamlandı anlamına gelmez. Önceki ortak kart düzeltmeleri bu kapsamı kapatmaz.

## Ekran ve durum envanteri

| Grup | İncelenecek durumlar | Durum |
|---|---|---|
| Hava | Şehirsiz, konum izni, şehirli, şehir değişimi, yükleme/hata/çevrimdışı; hero, tüm ölçümler, saatlik/günlük, güneş zamanları | Açık, ilk görsel tur |
| Şehir yönetimi/arama | Boş/dolu liste, sonuç yok/çok sonuç, seçim/silme, Türkiye önceliği, klavye | Açık |
| Takvim/seyahat | Seyahatsiz/dolu, tarih seçimi, oluşturma/düzenleme/silme, otomatik rehber | Açık |
| Güzergâh ve rehber | Riskler, duraklar, öneri kartları, genişletme, kaynak düğmesi, yenileme/hata | Açık |
| Asistan | İzin yükleme/hata/kabul/geri çekme, boş sohbet, yanıt/hata/tekrar, uzun sohbet ve klavye | Açık |
| Asistan geçmişi | Boş/dolu, detay, silme ve onay | Açık |
| Profil | Yükleme/hata/normal, istatistik/bilgi, profil düzenleme/kayıt hatası | Açık |
| Ayarlar | Tema/dil/üslup, çıkış, hesap silme/parola, silme toparlanması | Açık |
| Bildirimler | Boş/dolu/okunmuş/okunmamış, filtreler, akıllı uyarılar/izin reddi | Açık |
| Giriş/kayıt | Karşılama, giriş, kayıt, parola sıfırlama, kişiselleştirme, hesap yükleme | Açık |
| Premium | Paket/özellik/eylem ve kullanılamayan servis durumları | Açık |
| Ortak gezinme | Üst/alt menü, sekme seçimi, geri, modal, açılır menü, geçişler | Açık |

## Her grup için kabul

Açık/koyu ve mevsimsel paletlerde gereksiz üst üste zemin/çerçeve olup olmadığı görsel olarak kontrol edilecek. Ayrı eylem veya bilgi grubunu anlatan sınırlar korunacak; yalnız iç içe kart görünümü üreten gereksiz katman kaldırılacak. Değişiklik sonrası kontrast, dokunma hedefi, seçili/pasif/yükleme/hata görünümü ve etkileşim yeniden kontrol edilecek.

Telefon/tablet, yatay/dikey, büyük yazı ve klavye durumları kapsamda. Yazı tipi/ağırlığı, Türkçe büyük-küçük harf/yazım, boşluk/hizalama, köşe yarıçapı ve ikon ölçeği birlikte değerlendirilecek. Bileşen testi tek başına tüm ekran kabulü sayılmayacak. Önce/sonra ekran kanıtı, yapılan somut değişiklik, test sonucu ve kalan durumlar bu matrise eklenecek.

Öncelik: token iptali cihaz kabulünden sonra Hava → ortak kart/menü/buton tüketicileri → Takvim/rehber → Asistan → Profil/ayarlar → diğer ekranlar. Erişilemeyen/hesap gerektiren durumlar açık bırakılacak.

24 Kasım ilerleme: Hava detay bileşeninde tek yüzey, açık/koyu ve büyük yazı kısmi kabulü tamamlandı (3 cihaz testi). Hero, günlük/saatlik, menüler ve diğer durumlar açık. [Ayrıntı](24_KASIM_HAVA_DETAY_GORSEL_SADELESTIRME.md).


25 Kasım ilerleme: Günlük/saatlik dar bileşende koyu normal/açık yüzde 200 yazı, seçim ve yağış metni taşması kısmi kabulü geçti. Geniş günlük satır, tam liste genişletme, uzun saatlik seçim/kaydırma, menüler ve diğer tema durumları açık. [Rapor](25_KASIM_TAHMIN_YERLESIM_VE_GUN_SINIRI.md).


25 Kasım devam: Geniş günlük satır; 10 günlük liste açma/daraltma ve Compose durum geri yükleme; 48 saatlik listede yinelenen saat ayrımı geçti. 690 JVM + 7 cihaz testi, aynı iki senaryo görsel kanıtla tekrar geçti. Üst/alt menü ve ortak buton tüketicileri sırada; tam WeatherView ve diğer tema/ekranlar açık. [Rapor](25_KASIM_TAM_TAHMIN_LISTELERI.md).


25 Kasım ortak gezinme: Üst/alt menü bileşenlerinde 692 JVM + 4 cihaz testi, açık büyük yazı/koyu normal ve uzun başlık görsel kontrolü geçti. Dikdörtgen parlama kaldırıldı. Tüm ekran tüketicileri kapanmadı; profil/ayarlar özel menü ve iç yüzeyleri sırada. [Rapor](25_KASIM_ORTAK_MENU_BASLIK.md).


25 Kasım profil/ayar devam: İlgi kategorisi/uzun etiket, kategori açıklığı, ulaşım-doğa önceliği; dar ayar değeri, tema/üslup tek seçim eylemi 6 cihaz testinde geçti. 692 JVM; son etiket geometrisi UI'da yeniden doğrulandı. Üç görüntü incelendi. Profil üst kimlik/istatistik/Hakkımda ve tam ekran tüketicileri açık. [Rapor](25_KASIM_PROFIL_AYAR_GORSEL.md).


25 Kasım profil kimliği/istatistik/Hakkımda: 692 JVM ve sekiz cihaz senaryosu geçti; son boş açıklama renk düzeltmesi aynı kartta tekrar doğrulandı. Kimlik/hızlı işlem/kart/kayıt görüntüleri incelendi. Tam profil gerçek hesap ve tüm tema/cihaz durumları açık. Kişilik/rozet içerik eşleştirmesi ayrıca sıraya alındı. [Rapor](25_KASIM_PROFIL_KIMLIK_ISTATISTIK_HAKKIMDA.md).


25 Kasım profil içerik kabulü: Kişilik/rozet yanlış eşleşmeleri 13 yeni JVM regresyonu ve iki yeni cihaz güncelleme senaryosuyla ele alındı; toplam705 JVM +10 cihaz geçti. Yapılandırılmış tercihler gerçek karta bağlandı; tam doğal dil ve uzak kişiselleştirme kabulü değil. [Rapor](25_KASIM_PROFIL_ETIKET_DOGRULUGU.md).

25 Kasım rehber kişiselleştirmesi: İzole gerçek rehber bileşeninde tercih açık/kapalı ve A/B sahiplik sınırı; DataStore yeniden açılış kabulü 2/2 cihaz senaryosunda geçti. Son kaynakla 719 JVM ve 95 backend testi geçti. Tam ekran/navigasyon ve bütün temalar kabulü değildir. Kalan iç içe dikdörtgen ve görsel matris işleri açık. [Ayrıntılı rapor](25_KASIM_ASISTAN_REHBER_KISISELLESTIRME.md).
25 Kasım rehber yenileme: gerçek RichTravelGuideView bileşeninde kısmi/kayıtlı veri uyarısının başarılı yeni snapshot ile kalkması ve valiz önerisinin değişmesi 1 yeni cihaz senaryosuyla doğrulandı; önceki 2 kişiselleştirme senaryosu da geçti. 733 JVM. Tam ekran/tema/yatay/TalkBack kabulü açık. [Rapor](25_KASIM_REHBER_ANALIZ_YENILEME.md).