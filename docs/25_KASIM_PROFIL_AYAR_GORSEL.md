# 25 Kasım — Profil ilgi alanları ve ayar seçimleri

## Profil

Kategori simgesinin ayrı yuvarlatılmış zemini ve seçili sayı rozeti sadeleştirildi; sayı başlık altında metin olarak gösterilir. Kategori açıklamasındaki tek satır/ellipsis kısıtı kaldırıldı. Başlık en az 48 dp ve açık/kapalı durum açıklamasıyla düğme semantiği kullanır; açıklık rememberSaveable ile korunur.

İlgi öğeleri seçilmediğinde saydam/çerçevesiz, seçildiğinde okunabilir ortak tema paletiyle çizilir. Min 48 dp, uzun etikete göre büyüyen metin ve Checkbox toggle semantiği eklendi; iç öğelerdeki gereksiz kutu görünümü azaldı. Ulaşım ve Outdoor öncelik filtresinin gerçek kategori adlarıyla uyuşmaması giderildi. Motosiklet yazımı düzeltildi; kayıtlı ilgi kimliği motorsiklet değiştirilmedi.

## Ayarlar

Ayar değerleri dar içerik veya büyük yazıda başlığın altına alınır; etkin/pasif davranış korunur. Tema ve üslup seçimlerinde tek selectable RadioButton satırı kullanılır; iç radyo pasiftir, böylece aynı seçenek için iki ayrı eylem sunulmaz. Tema listesi yükseklik sınırı içinde kaydırılır. Görünüm seç / Asistan üslubu başlıkları tutarlı cümle yazımına geçirildi.

## Kabul yöntemi

ProfileSettingsVisualDeviceTest altı yerel gerçek Compose bileşen senaryosu içerir: dar/büyük ayar değeri ve pasif eylem; açık/büyük ve koyu/normal ilgi kategorisinde uzun etiket, toggle ve save/restore; öncelikli grupların daha fazla açılmadan erişimi; tema seçimi; üslup seçimi. Canlı hesap, Firebase veya kullanıcı profil kaydı değiştirilmez. QA paketinde ekran açık tutma ayarı koşum sonunda finally ile geri yüklenir.

Sonuç ve görüntü kontrolü aşağıya eklenecek. Bu tam ProfileScreen/SettingsScreen Activity ve sunucu senkronizasyonu kabulü değildir. Tüm mevsimsel temalar, yatay fiziksel telefon, sesli TalkBack, tüm ayar satırları ve profil üst kimlik/istatistik/Hakkımda alanları açık. Genel tüm ekran iç içe dikdörtgen maddesi kapanmadı. Push yok.
## İlk doğrulama

692 JVM testi hatasız geçti. İlk altı cihaz senaryosunda beşi geçti; koyu/normal uzun ilgi etiketi TextLayoutResult taşması bildirdi. Metin kalan genişliği dolduracak şekilde güncellendi; öğe en fazla 280 dp içinde metne göre büyür. Taşma assert'i korunarak aynı altı senaryo son kaynakla yeniden çalıştırılıyor. Son genişlik değişikliği için UI doğrulaması yapılıyor; önceki JVM sonucu bu son geometri değişikliğinden öncedir.
## Son cihaz kabulü ve görsel kontrol

Son kaynakla 6/6 SM-T500 Android 12 cihaz testi geçti; XML'de başarısızlık/hata/atlama yok. Doğrudan ilgi Text düğümünde taşma kalmadı. Tema/üslup seçimleri tek callback, pasif ayar eylemi sıfır callback üretti. Öncelikli gruplar daha fazla açılmadan erişildi; kategori açıklığı ve seçili öğe Compose durum geri yüklemesinde korundu.

Son üç görüntü gözle incelendi: açık yüzde 200 ayar satırında değer başlığın altında; koyu normal ve açık yüzde 200 ilgi kategorisinde açıklama tam, seçili etiket okunur ve ayrı kategori simge zemini yok. Seçili ilgi öğesinin dolu zemini etkileşim durumunu belirtmek için korunur; seçilmemiş öğelerde iç çerçeve yoktur. Yerel kanıt `.artifacts/profile-settings-20261125/settings-large.png`, `interest-dark.png`, `interest-light-large.png`; Git'e dahil olduğu varsayılmaz.

692 JVM koşumu geçti; son etiket genişliği değişikliği için altı UI testi tekrar yapıldı. Tüm ProfileScreen/SettingsScreen canlı hesap kabulü ve bütün palet/yatay/telefon/TalkBack durumları tamamlanmadı. Sıradaki profil üst kimliği, istatistik ve Hakkımda alanları. Ekran açık tutma ayarı geri yüklendi; kişisel gradle.properties değiştirilmedi. Push yapılmadı.