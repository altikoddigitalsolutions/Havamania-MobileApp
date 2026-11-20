# 20 Kasım Ortak Material tema ve düğme renkleri

Ortak Material renk şeması uygulamanın mevsimsel paletinden farklıydı. Sonbahar/kış aynı turkuaz, ilkbahar/yaz aynı mavi Material eylemlerini kullanıyordu. Bazı onPrimary ve tonal container rolleri belirlenmediği için varsayılan mor tonları da görünebiliyordu.

## Düzeltme

ThemeManager.getColorScheme artık ThemeFactory'nin aynı paletinden üretiliyor. Primary, secondary, tertiary, hata, dolgu üzerindeki metin, container, yüzey ve ters yüzey eylem rolleri ThemeMaterialColors içinde merkezi eşleniyor. AUTO yine ThemeFactory üzerinden mevsimsel temayı çözüyor.

Metin eylemlerinin sayfa zemininden ve dolgu metninin düğme zemininden ayrılması için projede 4.5 oranı hedeflendi. Mevcut vurgu bu hedefi sağlıyorsa değiştirilmez; sağlamıyorsa aynı renk ailesinde koyulaştırılır/açılır. Dolgu üzerindeki tercih edilen yazı yeterliyse korunur, değilse okunabilir siyah/beyaz alternatif seçilir. Bu test hedefi bütün uygulama için erişilebilirlik sertifikası anlamına gelmez.

Profil yükleme eylemlerindeki ekran içi renk düzeltmesi kaldırıldı; standart Button/TextButton artık ortak şemayı devralıyor. Varsayılan Material kontrollerini kullanan diğer ekranlar da aynı merkezi kaynağı kullanır. Özel gradyan/renk tanımlayan bileşenler bundan otomatik olarak düzelmiş sayılmaz.

## Doğrulama

Son kaynakla 635 JVM testi ve 7 UI testi sıfır hatayla geçti. Dört yeni birim testi altı sabit temanın palet/yüzey eşlemesini, dolgu ve tonal metin çiftlerini, ters yüzeyi, metin eylemlerini ve yeterli vurgunun korunmasını denetliyor.

İki yeni UI testi standart Button, FilledTonalButton ve TextButton'ın gerçek ortak renk şemasını koyu→açık ve ilkbahar→sonbahar değişimlerinde devraldığını, altı düğme tıklamasının çalıştığını doğruladı. Üç profil responsive ve iki dil seçimi testi de aynı son koşuda geçti. Açık dar ekran ve koyu kısa görünüm PNG'leri yeniden incelendi; standart düğme metnindeki varsayılan mor ton kaldırıldı. Kısa görüntü kaydırma sonundaki eylem alanıdır.

Son XML toplamı 635 JVM, 7 UI, sıfır hata/atlama. Android debug derlemesi ve git diff --check başarılı. Bu tur Firebase/backend/veri saklama davranışı değiştirilmedi; canlı sistem veya kişisel Gradle ayarı değiştirilmedi.

## Kalanlar

Tüm ürün ekranları tek tek görsel olarak incelenmiş değildir. Gerçek 800 ms tema animasyonunun tüm özel bileşenlerle eşleşmesi ve fiziksel telefon/TalkBack kabulü açık. Sıradaki yerel iş özel HavamaniaPrimaryButton/HavamaniaSecondaryButton gradyan metni, devre dışı durumları ve büyük yazıda dar ekran kırpılmasıdır. Bu bileşenlerde açıkça verilen renkler ve tek satır sınırları ayrıca ölçülmeli.