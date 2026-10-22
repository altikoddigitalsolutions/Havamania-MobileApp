# Kapsamlı kalite denetimi — 22 Ekim 2026

## Kapsam ve durum

Kullanıcının Yapıştırılan metin.txt raporundaki 20 kategori esas alındı. Bu belge ilk genişletilmiş denetim turudur; tüm uygulamanın testinin tamamlandığı veya yayına hazır olduğu anlamına gelmez. Gerçek servis yazıları, gerçek hesap silme ve yayın yapılmaz. Android önceliklidir; iOS/VoiceOver/Dynamic Island bu tur kapsam dışıdır.

**Bu tur kayıtları:** 5 kodla doğrulanan bug (0 Critical, 1 High, 3 Medium, 1 Low), 1 doğrulama gereken UI riski, 1 Improvement. Bunlar geçmiş tüm hataların toplamı değildir. 45 maddelik yayın listesi bug sayısı değildir ve 22/45 durumu korunur. Kodla doğrulama ile cihazda yeniden üretme ayrı belirtilmiştir.

## Yirmi kategori kapsam matrisi

| Kategori | Mevcut kanıt / yapılması gereken |
|---|---|
| Fonksiyonel | Bildirim kimliği/silme ve geçmiş kayıtları incelendi; her butonun tam akışı açık. |
| Edge case/stres | 10.000 bildirim, aynı metin/farklı hedef, %200 yazı, geçmiş testleri; uzun süre stres ve eşzamanlı gerçek cihazlar açık. |
| UI/görsel | Şehir yerleşimi ve ortak bileşenler değişti; tüm ekran tema/ölçek görsel matrisi açık. |
| UX | Silme onayı ve sonuç geri bildirimi düzeltildi; tam görev bazlı kullanılabilirlik açık. |
| Metin/içerik | Ortak otomatik büyük harf kaldırıldı; tüm sabit metinlerin dil/terminoloji/localization envanteri açık. |
| Form/input | Önceki auth/profil testleri yeniden koşulacak; emoji, autofill, paste ve tüm sınırlar açık. |
| Navigasyon | Bildirim hedeflerinin metinle gizlenmesi düzeltildi; deep link/geri/stack matrisi açık. |
| Login/hesap | Önceki yerel sahiplik testleri mevcut; canlı token/çok cihaz/giriş/silme açık. |
| Network/API | GET-only retry ve 30sn call timeout kodda görüldü; bütün HTTP hata enjeksiyonları bu tur koşulmadı. |
| Veri bütünlüğü | Room geçiş/kalıcı kuyruk/uzlaştırma testleri; kalıcı silme işareti ve çoklu ViewModel yarışları açık. |
| Performans | Kart timerları tek saate indirildi, şehir listesi lazy oldu; açılış p50/p95, frame, RAM/CPU/pil ölçümü açık. |
| Güvenlik/gizlilik | Manifest allowBackup=false ve önceki yerel kurallar; canlı IDOR/token iptal ve tam hassas-log taraması açık. |
| Erişilebilirlik | Ortak etiket/büyük yazı testleri; gerçek TalkBack, kontrast ve fokus matrisi açık. |
| Cihaz/ekran | SM-T500 Android12 izole testleri; telefon ve farklı OS/rotasyon matrisi açık. |
| İzinler | Önceki kod korumaları; sistemden izin geri çekme/kalıcı ret cihaz matrisi açık. |
| Bildirimler | Liste, boş durum, silme sonucu, timer düzeltildi; gerçek push/kapalı uygulama/saat senaryoları açık. |
| Tarih/saat | Dakika sınırı ve mevcut hava testleri; DST geri dönüşü/kutup/cihaz saati açık. |
| Kurulum/güncelleme | 11→19 Room zinciri ve yeniden açılış testleri; imzalı sürüm yükseltme/yarım kurulum açık. |
| Beklenmeyen davranış | Mevcut çift gönderim/iptal testleri; gerçek ağ kes/arka plan/çok cihaz kombinasyonları açık. |
| Tasarım iyileştirmeleri | Cümle biçimi korunması, tek kaydırma, durum geri bildirimi; tüm marka tipografi/ikon/renk matrisi açık. |

## Sorun kartları

### QA-001 — Aynı metinli farklı bildirimler gizleniyor

- **ID:** QA-001
- **Başlık:** Aynı metinli farklı bildirimler gizleniyor
- **Kategori:** Functional / Data Integrity
- **Severity:** HIGH
- **Bulunduğu ekran/özellik:** Bildirim merkezi
- **Sorunun açıklaması:** Başlık+mesaj ile gruplama farklı kimlik/hedefleri tek satıra indiriyor.
- **Tekrarlama adımları:** Aynı metinle iki farklı seyahat bildirimi oluştur; listeyi aç; her iki hedefe erişimi kontrol et.
- **Beklenen sonuç:** İki ayrı bildirim görünür.
- **Gerçekleşen sonuç:** Yalnızca en yeni kayıt gösteriliyor (kod yolu).
- **Kullanıcıya etkisi:** Bir seyahat uyarısına erişilemiyor.
- **Tekrarlanabilirlik:** Always: aynı gruplama anahtarında
- **Muhtemel sebep:** Metin, olay kimliği yerine kullanılıyor.
- **Önerilen/uygulanan çözüm:** Metin bazlı gruplama kaldırıldı; kayıt kimlikleri korunuyor.
- **İlgili component/dosya:** NotificationCenterScreen.kt, NotificationPresentation.kt
- **Etkileyebileceği diğer alanlar:** Filtreler, toplu seçim, okunmamış sayısı
- **Düzeltme sonrası test:** Aynı metin/farklı hedef ve 10.000 kayıt testi
- **Durum:** Düzeltildi; birim testi eklendi

### QA-002 — Boş misafir listesi örnek bildirimlerle yeniden doluyor

- **ID:** QA-002
- **Başlık:** Boş misafir listesi örnek bildirimlerle yeniden doluyor
- **Kategori:** Functional / Data Integrity
- **Severity:** MEDIUM
- **Bulunduğu ekran/özellik:** Bildirim merkezi / misafir
- **Sorunun açıklaması:** Repository ve ViewModel boş listeyi DefaultNotifications ile değiştiriyor.
- **Tekrarlama adımları:** Misafir bildirimlerini sil; veritabanı boş kalsın; liste akışını yeniden oku.
- **Beklenen sonuç:** Boş durum görünür.
- **Gerçekleşen sonuç:** Silinmiş gibi görünen bildirimlerin yerine örnek kayıtlar gösterilir (iki kod yolu).
- **Kullanıcıya etkisi:** Silme güvenilir görünmez, sayaç ve liste ayrışır.
- **Tekrarlanabilirlik:** Always: boş legacy listesinde
- **Muhtemel sebep:** İki katmanda sahte boş-liste geri dönüşü.
- **Önerilen/uygulanan çözüm:** Her iki otomatik geri dönüş kaldırıldı; ilk kurulum seed akışı ayrı kaldı.
- **İlgili component/dosya:** NotificationRepository.kt, NotificationViewModel.kt
- **Etkileyebileceği diğer alanlar:** İlk kurulum, misafir, toplu silme
- **Düzeltme sonrası test:** Boş legacy repository akışının gerçek SQLite testi; tam ilk kurulum akışı açık
- **Durum:** Düzeltildi; SQLite testi genişletildi

### QA-003 — Bildirim silme tamamlanmadan başarı bildiriliyor

- **ID:** QA-003
- **Başlık:** Bildirim silme tamamlanmadan başarı bildiriliyor
- **Kategori:** Functional / UX
- **Severity:** MEDIUM
- **Bulunduğu ekran/özellik:** Bildirim merkezi / tümünü sil
- **Sorunun açıklaması:** Ekran işlemi başlatınca başarı gösteriyor; repository hatayı yutuyor.
- **Tekrarlama adımları:** Silme sırasında DAO hatası üret; onayla; mesajı izle.
- **Beklenen sonuç:** Başarı yalnızca onaydan sonra, hatada yeniden deneme açıklaması.
- **Gerçekleşen sonuç:** Başarı işlemin sonucundan bağımsız gösteriliyor (kod kanıtı; arıza enjeksiyonu cihazda koşulmadı).
- **Kullanıcıya etkisi:** Kullanıcı silinmeyen veriyi silinmiş sanabilir.
- **Tekrarlanabilirlik:** Always: eski başarı mesajı; hata koşulu doğrulaması açık
- **Muhtemel sebep:** Asenkron sonuç ekranla paylaşılmıyor.
- **Önerilen/uygulanan çözüm:** Repository hatayı iletiyor; hesap bağlı runner sonuca göre mesaj veriyor.
- **İlgili component/dosya:** NotificationRepository.kt, NotificationViewModel.kt, NotificationCenterScreen.kt
- **Etkileyebileceği diğer alanlar:** Hesap değişimi, çift dokunma, seçim modu
- **Düzeltme sonrası test:** Runner regresyonları; DAO hata enjeksiyonlu tam UI testi açık
- **Durum:** Düzeltildi; uçtan uca hata testi açık

### QA-004 — Toplu bildirim silme onay penceresi kapanmıyor

- **ID:** QA-004
- **Başlık:** Toplu bildirim silme onay penceresi kapanmıyor
- **Kategori:** UI / UX
- **Severity:** MEDIUM
- **Bulunduğu ekran/özellik:** Bildirim merkezi / onay
- **Sorunun açıklaması:** OnConfirm showDeleteAllDialog değişkenini kapatmıyor.
- **Tekrarlama adımları:** Tümünü sil penceresini aç; onayla; pencere durumunu incele.
- **Beklenen sonuç:** Pencere kapanır, sonuç mesajı görünür.
- **Gerçekleşen sonuç:** Açıklık durumu true kalıyor (kod kanıtı).
- **Kullanıcıya etkisi:** Yinelenen onaya ve kafa karışıklığına neden olur.
- **Tekrarlanabilirlik:** Always
- **Muhtemel sebep:** Onay durum geçişi eksik.
- **Önerilen/uygulanan çözüm:** Onayda pencere kapatılıyor; ortak silme runner çift işlemi engelliyor.
- **İlgili component/dosya:** NotificationCenterScreen.kt
- **Etkileyebileceği diğer alanlar:** Geri tuşu, seçim modu
- **Düzeltme sonrası test:** Tam ekran etkileşim testi açık
- **Durum:** Düzeltildi

### QA-005 — Ortak bileşenler metnin yazımını zorla değiştiriyor

- **ID:** QA-005
- **Başlık:** Ortak bileşenler metnin yazımını zorla değiştiriyor
- **Kategori:** Content
- **Severity:** LOW
- **Bulunduğu ekran/özellik:** Ortak buton, etiket, başlık, diyalog
- **Sorunun açıklaması:** Locale bağımsız uppercase Türkçe i/ı harflerini ve verilen metin biçimini değiştiriyor.
- **Tekrarlama adımları:** Ortak düğmeye İzinleri yönet metnini ver; görünen yazıyla karşılaştır.
- **Beklenen sonuç:** Belirlenmiş metin aynen korunur.
- **Gerçekleşen sonuç:** Zorunlu büyük harf dönüşümü yapılır (kod kanıtı).
- **Kullanıcıya etkisi:** Dil ve marka tutarlılığı bozulur.
- **Tekrarlanabilirlik:** Always: küçük harfli metinde
- **Muhtemel sebep:** Sunum bileşeni içerik politikasını değiştiriyor.
- **Önerilen/uygulanan çözüm:** Ortak zorunlu dönüşümler kaldırıldı; çağıranın metni korunur.
- **İlgili component/dosya:** ui/theme/HavamaniaCommonComponents.kt
- **Etkileyebileceği diğer alanlar:** Tüm ortak bileşen çağıran ekranlar
- **Düzeltme sonrası test:** Türkçe cümle biçimi Compose testi; tüm ekran metin envanteri açık
- **Durum:** Düzeltildi; metin envanteri tamamlanmadı

### QA-006 — Şehir arama sonuçları kısa ekranda kayıtlı listeyi sıkıştırabilir

- **ID:** QA-006
- **Başlık:** Şehir arama sonuçları kısa ekranda kayıtlı listeyi sıkıştırabilir
- **Kategori:** UI / Accessibility
- **Severity:** MEDIUM
- **Bulunduğu ekran/özellik:** Şehirler
- **Sorunun açıklaması:** Arama ve beş öneri kaydırılmayan Column içinde; yalnızca kayıtlı liste kayıyor.
- **Tekrarlama adımları:** 320×420 dp ve %200 yazı; beş uzun öneri; kayıtlı konum eylemine ulaş.
- **Beklenen sonuç:** Tüm içeriğe kaydırarak erişilir.
- **Gerçekleşen sonuç:** Ölçümde alanın tükenmesi riski; eski düzen cihazda yeniden üretilmedi.
- **Kullanıcıya etkisi:** Konum yönetimi dar ekranda zorlaşabilir.
- **Tekrarlanabilirlik:** Doğrulama gerekli
- **Muhtemel sebep:** Birden çok dikey bölümün yalnızca biri kaydırılabilir.
- **Önerilen/uygulanan çözüm:** Tek LazyColumn ve IME boşluğu; öneri/liste anahtarları ayrıldı.
- **İlgili component/dosya:** CitiesManagementScreen.kt
- **Etkileyebileceği diğer alanlar:** Klavye, boş liste, uzun ülke/şehir, tablet
- **Düzeltme sonrası test:** Yeni düzen için büyük yazı/kısa ekran Compose testi; gerçek klavye/yatay açık
- **Durum:** Potansiyel risk; önleyici düzeltme yapıldı, bug toplamına dahil değil

### QA-007 — Her bildirim kartının ayrı dakika döngüsü var

- **ID:** QA-007
- **Başlık:** Her bildirim kartının ayrı dakika döngüsü var
- **Kategori:** Performance
- **Severity:** IMPROVEMENT
- **Bulunduğu ekran/özellik:** Bildirim merkezi
- **Sorunun açıklaması:** Her görünür kartta ayrı LaunchedEffect timer çalışıyor; lifecycle STARTED sınırı yok.
- **Tekrarlama adımları:** Birden çok kart göster; arka plana geç; zamanlayıcı sahipliğini incele.
- **Beklenen sonuç:** Ortak ekran saati, görünür yaşam döngüsünde güncelleme.
- **Gerçekleşen sonuç:** Kart başına döngü (kod kanıtı); pil/frame etkisi ölçülmedi.
- **Kullanıcıya etkisi:** Gereksiz coroutine ve güncellemeler azaltılabilir.
- **Tekrarlanabilirlik:** Always: oluşturulmuş kart başına
- **Muhtemel sebep:** Göreli zaman hesabı kartlara dağıtılmış.
- **Önerilen/uygulanan çözüm:** Ekranda tek STARTED kapsamlı saat ve yaşam döngüsüne bağlı flow toplama.
- **İlgili component/dosya:** NotificationCenterScreen.kt, NotificationDateFormatter.kt
- **Etkileyebileceği diğer alanlar:** Dakika sınırı, arka plan/ön plan, cihaz saati
- **Düzeltme sonrası test:** Ortak saatle dakika sınırı birim testi; CPU/pil/frame benchmark açık
- **Durum:** Uygulandı; hız/pil kazancı sayısal olarak iddia edilmiyor

## En riskli 10 açık alan — doğrulanmış bug sıralaması değildir

1. Hesap silmenin gerçek servislerde kesinti sonrası devamı ve üretim kuralları.
2. AI geçmişinde kalıcı silme devam kaydı ve birden çok ViewModel/cihazın yarışları.
3. Canlı Firebase/backend token, bucket ve kullanıcılar arası erişim doğrulaması.
4. Bildirim repositorysinin tekli silme/okundu gibi diğer işlemlerinde hata yutma ve hesap geçişinde sahiplik kontrolü: potansiyel risk, hata enjeksiyonu gerekli.
5. Hava sağlayıcı kapasitesi/lisansı ve rota örnekleri arasındaki riskler.
6. Tam timezone/DST/kutup günü ve yanlış cihaz saati matrisi.
7. Gerçek cihazda ağ/izin kesintisi ve süreç ölümü sonrası kurtarma.
8. Bütün ekranlarda tema/klavye/yatay/%200 yazı ve TalkBack.
9. İmzalı R8 release, gerçek 16 KB cihaz ve Play iç test/pre-launch.
10. Gizlilik/KVKK/hesap silme adresi ve mağaza beyanları.

## Yayın kararı ve ölçüm sınırı

Yayın onayı verilmedi. Veri silme/güvenlik/canlı servis ve imzalı release kanıtları çıkış öncesi gerekir. Kozmetik metin/boşluk düzenlemeleri ve ölçülmüş düşük etkili iyileştirmeler daha sonra yapılabilir; erişilemeyen ana eylemler ertelenmemeli.

Bu tur yeni arayüz doğrulaması iki izole içerik/bileşen senaryosudur (şehir yönetimi ve ortak buton); tam uçtan uca ekran sayısı 0. Önceki denetim envanterindeki 18 satır, 18 ekranın görsel olarak tamamen geçtiği anlamına gelmez. Fonksiyonel doğrulama alanları bildirim sunumu/SQLite ve mevcut kayıt akışlarıdır; özellik yüzdesi hesaplanmadı.

Performans kazancı için milisaniye/yüzde uydurulmaz. 10.000 kayıt testi veri koruma/sıralama testidir, scroll benchmark değildir. Açılış, frame süreleri, uzun süre RAM/CPU/pil ve ağ hacmi ölçümü henüz yapılmadı.

## İlgili belgeler

- [Güncel yayın durum raporu](YAYIN_DURUM_RAPORU.md)
- [Ana yayın kontrol listesi](YAYIN_ONCELIKLERI.md)
- [Ekran bazlı UI envanteri](UI_DENETIMI_2026-10-22.md)
- [Ayrıntılı test geçmişi](YAYIN_DENETIMI_2026-10-22.md)

## Geniş denetim — tamamlanan doğrulama

- Android: 125 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Lint: 0 hata, 187 uyarı, 2 ipucu.
- Samsung SM-T500 / Android 12: kilit açıldıktan sonraki son koşuda 16/16 test geçti (4 SQLite, 2 Firebase model, 10 izole Compose). Şehir yönetiminde %200 yazı/320×420 dp alanda kaydırıp varsayılan eylemine basma ve ortak butonda İzinleri yönet metni doğrulandı. Gerçek klavye/TalkBack/full-Activity rotasyon bu testler değildir.
- Backend: 64 test geçti, Ruff temiz. Genel koşuda atlanan 4 PostgreSQL testi daha sonra ayrı izole PostgreSQL 16 kümesinde geçti. Test ortamı üretime bağlanmadı.
- Firebase Firestore/Storage: demo-havamania-audit yerel emülatörlerinde 4 kural testi geçti; emülatörler kapatıldı. Logdaki PERMISSION_DENIED satırları beklenen negatif erişim testleriyle ilişkili, test başarısızlığı değil.
- İlk cihaz koşusunda kilitli ekrandan kaynaklanan 10 başarısızlık raporda tutuluyor. Son başarılı koşu önceki başarısızlığı gizlemez.
- Backend/dependency deprecation uyarıları var; bu tur bağımlılık güncellemesi yapılmadı.

Öncelikli devam: kalıcı AI silme devam kaydı ve çoklu işlem yarışları; bildirim tekli işlemlerinde hata/sahiplik; tam UI matrisinde telefon/yatay/klavye; ardından ölçümlü açılış/frame/RAM-pil profili. Önceki sekiz yayın başlığı korunuyor. Sonuç tüm ürünün yayın onayı değildir.

Kalıcı silme turu doğrulaması: 125 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 7 cihaz testi (5 SQLite, 2 Firebase model; 0 başarısızlık/0 hata) geçti. Debug derleme ve lint başarılı; 0 lint hatası, 187 uyarı, 2 ipucu. UI, backend ve canlı Firebase bu tur yeniden test edilmedi. Sonraki iş bildirim tekli silme/okundu işlemlerinde hata ve hesap değişimi koruması.
