# Canlı görsel ekran denetimi

## Tamamlanma ölçütü

Her ekran gerçek uygulamada açılır; tema, cihaz, yön, yazı ölçeği ve klavye durumu kaydedilir. Büyük/küçük harf, yazım, terim birliği, yazı tipi/ağırlığı, renk/kontrast, ikon, boşluk, hizalama, buton konumu/davranışı, kırpılma ve kaydırma incelenir. Her bulgu hata veya iyileştirme olarak ayrılır; önem seviyesi, görüntü kanıtı ve düzeltme sonrası tekrar kontrolü tutulur. Kod taraması ve izole bileşen testi tek başına canlı ekran onayı sayılmaz.

## Ekran matrisi

| Ekran | Canlı görsel durum |
|---|---|
| Karşılama | Bekliyor |
| Giriş | Bekliyor |
| Kayıt | Bekliyor |
| Şifre sıfırlama | Bekliyor |
| Ana hava durumu / saatlik / günlük detaylar | İlk görünüm incelendi; detaylar ve varyantlar açık |
| Asistan | İlk görünüm incelendi; klavye ve sohbet akışı açık |
| Asistan geçmişi ve detay | Liste incelendi; hatalı detay yönlendirmesi bulundu ve düzeltildi; tekrar kontrol aşağıda |
| Profil ve profil düzenleme | İlk görünümler incelendi; profil kontrast/başlık düzeltmesi cihazda doğrulandı |
| Hakkımda alt penceresi | İlk görünüm incelendi; klavye ve kaydetme varyantları açık |
| Kişiselleştirme | İlk görünüm incelendi; diğer kategoriler ve kayıt varyantları açık |
| Şehir yönetimi | Kayıtlı liste incelendi; arama/klavye varyantları açık |
| Seyahat takvimi / oluşturma / detay | Boş ve kayıtlı takvim incelendi; form/detay varyantları açık |
| Rota ve hava riskleri | Konumsuz durum incelendi; harita üstü açıklamaya opak zemin eklendi |
| Bildirim merkezi / filtre / seçim | Bekliyor |
| Ayarlar / tema / hesap silme | Ayarlar ilk görünümü incelendi; tema ve silme varyantları açık |
| Akıllı uyarılar | İlk görünüm incelendi; bildirim teslimi doğrulanmadı |
| Premium | Ayarlardan açılan bilgi diyaloğu incelendi |
| Hesap silme kurtarma | Bekliyor; gerçek silme yapılmayacak |

Kişisel veri içerebilen ekran görüntüleri .local-tests/visual-audit altında yerelde tutulur, Git'e gönderilmez. Görülmeyen ekranlar tamamlandı işaretlenmez. Ana kontrol listesine ayrı madde eklendi: 46 madde, 22 tamamlanan / 24 açık. Önceki 45 maddelik sayılar tarihsel kayıttır.

## Bu turun teknik düzeltmesi

Bildirim tekli/toplu seçili silme, okundu/okunmadı ve tümünü okundu yap işlemleri kullanıcı kimliğini işlem başında yakalayan AccountTaskScope'a taşındı. Hesap değişimi bekleyen işleri iptal eder. DAO sorguları kullanıcı kimliğiyle sınırlandı. Repository hataları yutmaz; Snackbar hata bildirimi verir. Seçim yalnızca başarılı işlemden sonra temizlenir. Aynı ekrandaki işlemler mutex ile sıralanır. Veritabanı şeması değişmedi.

Sınırlar: cihazda gerçek A→B giriş, disk hata enjeksiyonu ve tüm Snackbar navigasyon senaryoları ayrıca doğrulanmalı. Demo yenileme/kategori silme gibi diğer yardımcı akışlar bu düzeltmenin tamamlandığı anlamına gelmez.

## 22 Ekim 2026 canlı inceleme bulguları

Cihaz: Samsung SM-T500 / Android 12; 1200×2000, 240 dpi, yazı ölçeği 1,1; dikey, otomatik sonbahar teması. Bu kayıt tüm tema/cihaz/klavye matrisinin tamamlandığı anlamına gelmez. Mevcut oturum kapatılmadı, kişisel veri silinmedi; bulut üzerinde test kaydı oluşturulmadı.

| Bulgu | Düzeltme / kanıt |
|---|---|
| Alt menü ve açıklamaların sonbahar zemininde düşük okunurluğu | Alt menü ve ortak boş durum secondary rengini kullanıyor; sonbahar muted rengi açıldı. after-profile.png / after-home.png üzerinde etiket ve açıklamalar tekrar görüldü. |
| PROFIL / PROFILİ başlıklarında Türkçe İ hatası | PROFİL / PROFİLİ olarak düzeltildi; profil başlığı cihazda doğrulandı. |
| AI Geçmişi / Asistan Geçmişi terim ayrılığı | Profil kısayolu Asistan Geçmişi oldu; cihazda doğrulandı. |
| Kayıtlı şehir varken varsayılan konum yok durumunda “Henüz Şehir Yok” | Konum seçmeye yönlendiren doğru içerik; derlendi. Oturumdaki konum sonradan değiştiğinden boş durum tekrar görüntülenmedi. |
| Harita üzerinde açıklama harita etiketlerine karışıyor | Konum/rota hata metinlerine opak tema yüzeyi eklendi. |
| Geçmiş kaydına dokununca kayıt yerine boş asistan açılıyor | İki navigasyon grafiği de kayıt kimliğini kodlayarak mevcut analiz detayına yönlendiriyor. |

Kanıt dosyaları yerel .local-tests/visual-audit klasöründedir. history.png adıyla kaydedilen görüntü rota ekranıdır; geçmiş kanıtı history-verified.png dosyasıdır. history-detail.png düzeltme öncesi yanlış açılan asistanı gösterir.

Açık takip: bütün ekranların kaydırma/klavye/büyük yazı/yatay ve açık-koyu tema varyantları; giriş ekranları; bildirim seçimi/hata durumu; kişiselleştirme ve Hakkımda; rota dolu durum; detaylı saatlik/günlük paneller. Harita dışındaki accent metinleri ve saydam metinlerin kontrastı da tam matrisle ölçülmeli. Görsel değerlendirme tek başına WCAG uygunluk onayı değildir.

## Teknik doğrulama

Bildirim değişikliği: 125 JVM testi ve tablette 8 seçili veritabanı/model testi geçti. Lint: 0 hata, 187 uyarı, 2 öneri. Son UI/navigasyon düzeltmeleri ayrıca derlenip cihazda hedefli kontrol ediliyor. Backend ve Firebase testleri bu tur yeniden çalıştırılmadı (bu alanlarda kod değişmedi).

Ek bulgu: analiz detayının ilk yüklemesi Firebase ilk oturum bildirimi tarafından iptal ediliyordu; kullanıcı kimliği değişmediğinde detay yükleyicisi artık sıfırlanmıyor. Hesap değişiminde eski detay yine temizleniyor. preferences.png ve about-me.png ilk görünümleri incelendi. Kişiselleştirmedeki Outdoor/Extreme gibi İngilizce etiketler ve sen/siz üslup karışımı ayrıca dil bütünlüğü takibine alındı; kayıtlı ilgi alanı anahtarlarıyla uyumluluk korunarak ele alınmalı. notifications.png profil ekranını gösteriyor; bildirim merkezi kanıtı sayılmaz.

Son cihaz doğrulaması: after-history-detail.png üzerinde seçilen kaydın başlığı, tarihi ve kullanıcı/asistan mesajları görüntülendi; yanlış yönlendirme ve sonsuz yükleme düzeltmeleri doğrulandı. Harita zemin düzeltmesi derlendi; son sürümle görsel tekrar kontrolü açık.
Son kod üzerinde birim testleri tekrar çalıştırıldı: 125/125 geçti. Güncel lint raporu: 0 hata, 180 uyarı, 2 öneri (tur başındaki 187 uyarı tarihsel sonuçtur).
