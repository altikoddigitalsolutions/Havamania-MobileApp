# Android UI denetimi — 22 Ekim 2026

## Kapsam ve kanıt sınırı

Kotlin/Compose ekran dosyalarında yerleşim, kaydırma, sabit ölçüler, etiketler ve kayıt akışları tarandı. Aşağıdaki tablo bir kod incelemesidir; bütün ekranların cihazda baştan sona görsel olarak doğrulandığı anlamına gelmez. Açık/koyu/mevsimsel tema, telefon/tablet, yatay, %200 yazı, TalkBack ve klavye matrisi tamamlanmadan tasarım başlığı kapanmaz. Dekoratif ikonlarda null açıklama tek başına hata sayılmadı.

## Bu tur düzeltilenler

- Giriş/kayıt/şifre sıfırlama ortak alanlarında placeholder yerine kalıcı etiket; şifre göster/gizle eylem adı.
- Asistan kaydet/geçmiş ikonlarına ad; mesaj gönder düğmesine ad ve yükleme durumu; giriş alanına kalıcı etiket, en çok 5 görünür satır ve IME boşluğu.
- Asistan öneri kartlarında 100 dp sabit yükseklik yerine minimum yükseklik.
- Sohbet geçmişinde silme eylemine ad ve 48 dp alan.
- Ayarlar ve akıllı uyarı anahtarlarına ilgili ayarın erişilebilir adı.
- Güneş zamanları dar alanda veya fontScale > 1.3 olduğunda dikey diziliyor; üç sütun taşması önleniyor.

## Ekran bazında takip

| Ekran / dosya | Kod bulgusu ve kalan doğrulama |
|---|---|
| AuthScreens | Ortak alan etiketleri ve şifre düğmesi düzeltildi. Giriş/kayıt/şifre sıfırlama kaydırılabilir; klavye açık yatay ve büyük yazı doğrulaması açık. |
| WeatherView / WeatherDetailsGrid | Konum saati ve güneş günü düzeltmeleri korundu; güneş paneli uyarlanabilir oldu. Hero kartındaki sabit yükseklik, uzun şehir adı ve diğer metriklerin %200 yazı kontrolü açık. |
| AiChatScreen / AiChatComponents | İsimsiz eylemler ve sabit kart yüksekliği düzeltildi. Klavye boşluğu ve mesaj satır sınırı eklendi. Konuşma taslağının rotasyon/hesap değişimi davranışı ve uzun yanıt kaydırması açık. |
| AiHistoryScreen | Silme hedefi ve adı düzeltildi. Uzun başlık/özet kısaltmaları, silme onayı ve filtre boş durumlarının cihaz testi açık. |
| AiHistoryDetailScreen | İçerik kaydırılıyor. Kayıt yüklenirken “Kayıt bulunamadı” gösterilmesi ve geçmişteki işlevsiz yeniden deneme/eylem düğmeleri ayrıca düzeltilmeli. |
| EditProfileScreen | Önceki kayıt onayı, taslak ve klavye düzeltmeleri korunuyor. Gerçek rotasyon/süreç ölümü/kayıt sırasında geri navigasyon açık. |
| ProfileView | Ana içerik kaydırılıyor, ilgi alanları FlowRow kullanıyor. Hakkımda içeriğine IME boşluğu, esnek metin/düğme yüksekliği ve kaydedilebilir taslak eklendi. Onay gelmeden kapanma kaldırıldı; hata/yeniden deneme görünür. Tüm profil ekranının görsel matrisi açık. |
| PersonalizationScreen | Kayıt onayı/hata durumu ve tek seferlik taslak doldurma eklendi. Hesap bazlı saveable taslak ve geri yükleme sahiplik kontrolü var. PreferenceSwitch metni esnek genişlikte, anahtar adlandırılmış. Gerçek cihazda rotasyon/süreç ölümü ve kayıt sırasında geri davranışı açık. |
| SettingsView | Anahtar adı düzeltildi; içerik kaydırılıyor. Tema seçici radyo/row çift eylemleri, silme formu klavye ve büyük yazı kontrolü açık. |
| SmartAlertsScreen | Anahtar adı düzeltildi; açıklamalar esnek sütunda. Devre dışı seçeneklerin kontrastı ve büyük yazı matrisi açık. |
| CitiesManagementScreen | Liste ve arama düzeni tarandı. Küçük yatay ekranda üst arama kartı/listenin alanı, klavye ve arama temizlenince eski sonuçların kalması kontrol edilmeli. |
| TravelPlannerScreen | Kaydırma/tablet dalı mevcut. Ekle ikonunun adı ve 48 dp dokunma alanı düzeltildi; takvim ve filtreler büyük yazı/yatayda denenmeli. |
| TravelRouteWeatherScreen | Telefon/tablet haritası sabit 350/500 dp; kaydırma mevcut. Yatayda harita/alt bilgi dengesi, risk kartları ve uzun yer adları cihazda incelenmeli. |
| NotificationCenterScreen | Üst eylemler adlandırılmış; liste/filtreler kaydırılıyor. 108 dp kaydırma eylemleri ve %200 yazı, çoklu seçim ve TalkBack alternatifleri açık. |
| PremiumScreen | Önceki satış metni ve düğme yüksekliği düzeltmeleri korunuyor. Büyük dekoratif alanın yatay telefonda akışı ve karşılaştırma metinleri açık. |
| AccountDeletionRecoveryScreen | Kaydırma, safeDrawing ve IME boşluğu var. E-posta klavyesi, alan doğrulaması ve durumun TalkBack ile duyurulması ayrıca incelenmeli. |
| SplashScreen | Açılış görseli/animasyonu var. Küçük yatay ekran ve azaltılmış hareket davranışı cihazda doğrulanmalı. |
| Ortak HavamaniaTopBar | Başlık tek satır/ellipsis; üç eylemli başlıklarda kullanılabilir metin genişliği az. Büyük yazı matrisi açık. |

## Sıradaki UI düzeltme önceliği

1. Kişiselleştirme düzeltmesinin cihazda rotasyon/süreç ölümü ve hata/yeniden deneme doğrulaması.
2. Profil alt penceresi ve seyahat ekle düğmesi düzeltmelerinin tam ekran/gerçek klavye matrisi; bileşen testleri ayrı kayıtlı.
3. Geçmiş detayında yükleme/yok durumu ve işlevsiz eylemler.
4. Telefon/tablet tema-yatay-yazı ölçeği cihaz matrisi, ardından kontrast/TalkBack ölçümü.

Ana yayın takip listesi: [YAYIN_ONCELIKLERI.md](YAYIN_ONCELIKLERI.md). Bu rapor açık UI bulgularının kaybolmaması için alt envanterdir; yeni bulgular üst listedeki 6. başlık altında tutulur.
## Cihaz doğrulaması

SM-T500 / Android 12 üzerinde ikinci koşuda 10 test geçti: 2 SQLite migration, 2 Firestore model, 6 izole Compose testi. Yeni üç Compose testi dolu auth etiketleri/şifre eylemi, yüklenen gönderme düğmesi ve 260 dp genişlikte %200 yazıyla güneş zamanlarının dikey yerleşimini kontrol ediyor. İlk koşuda 3 Compose testi “No compose hierarchies found” hatasıyla başarısızdı; cihaz ekranı açık/kilitsizdi. Kod değiştirmeden tekrar koşuda tamamı geçti. Bu aralıklı test Activity başlangıcı sorununun nedeni kesinleşmedi; ilk başarısızlık saklanmıyor. Testler bütün uygulamanın görsel onayı değildir.
Ayrı lintDebug koşusu başarıyla tamamlandı. 102 birim testi, 10 cihaz testi; debug APK SHA-256: BD0CC2ECB2E76AC8FD9BD8D492DD9732FD2A275F97BEA9A613C19DEB5B866CE2.

## Kişiselleştirme devam turu

Sunucu ve yerel tercih yazıları tamamlanmadan ekran başarıyla kapanmıyor. Ayrı kayıt durumu, çift gönderim engeli, 20 saniye onay sınırı, hata mesajı ve yeniden deneme eklendi. Hata profil yükleme durumunu bozmaz; seçimler korunur. Snapshot yalnızca ilk yüklemede taslağı doldurur. İlgi alanları, seyahat tarzları ve hassasiyetler rememberSaveable ile saklanır; geri yüklenen taslağın kullanıcı kimliği eşleşmiyorsa çizilmeden temizlenir. Kayıt sırasında değişiklik, geri ve atla engellenir; ekran ayrılırsa bekleyen kayıt onayı iptal edilir. Bu, sunucuda başlamış yazıyı geri alma garantisi değildir. Gerçek rotasyon/süreç ölümü matrisi henüz koşulmadı.

## Hakkımda ve seyahat düğmesi devam turu

Metin alanı 180 dp minimum ve 4–8 görünür satırla, kaydet düğmesi 56 dp minimumla büyüyebiliyor. Dikey kaydırma ve IME boşluğu birlikte kullanılıyor. Dolu alanın Hakkımda etiketi var; taslak rememberSaveable ile saklanıyor. Kayıt onayı bekleniyor; hata metni canlı bölgeyle duyuruluyor ve metin korunuyor. Kayıt sırasında düzenleme/kapatma engelleniyor; 20 saniye onay sınırı ve hesap/ekran değişiminde eski sonucun reddi var. Sunucuda başlamış yazının geri alındığı iddia edilmiyor. Seyahat ekle düğmesi 48 dp ve erişilebilir adı Seyahat ekle.

Yeni testlerin kapsamı izole içerikte saved-instance-state geri yükleme ve 320×420 dp/%200 yazıda düğmeye kaydırarak erişimdir. Tam uygulama süreç ölümü, gerçek klavye ve Firebase yazısı bu testlerin yerine geçmez.
Sonuç: 106 birim testi ve 12 cihaz testi geçti. İlk cihaz koşusunda eksik LocalWindowSize yüzünden test kurulumu çöktü; ProvideResponsiveLayout sağlandıktan sonra tam koşu geçti.

## AI geçmiş detayı ve tablette görülen boş ekran — devam turu

Geçmiş detayı artık başlangıçtaki boş listeyi kayıt yok saymıyor. Yerel kullanıcıya ait kayıt okunuyor; bulunamazsa giriş yapmış kullanıcı için sunucudan belge doğrulanıyor. Yükleniyor, bulunamadı ve hata durumları ayrıldı; hata için yeniden deneme eklendi. 20 saniye sınırı, hesap değişimi ve geç gelen eski istek sonucunu engelleme var. Salt okunur geçmişte işlevsiz asistan eylem düğmeleri gizlendi. Geçmişin genel silme/senkronizasyon davranışı bu düzeltmeyle tamamen doğrulanmış sayılmaz.

Doğrulama: 111 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Lint: 0 hata, 194 uyarı, 1 bilgi. Yeni 5 test bekleme, yokluk, hata/yeniden deneme, eski istek ve hesap değişimini kapsar. Canlı Firebase bağlantısı ve bu ekranın tam cihaz matrisi test edilmedi. Bu tur cihaz arayüz testleri yeniden çalıştırılmadı; önceki son cihaz koşusu 12/12 idi.

Kullanıcının 14:56:30 ve 14:56:32 ekran görüntüleri kayıtlı authFieldsKeepLabelsAndPasswordToggleAnnouncesAction testine denk geliyor: test 14:56:30.307, androidx.activity.ComponentActivity başlangıcı 14:56:30.452. Görüntüler bu izole test Activity geçişiyle uyumlu; normal uygulama başlangıcının başarısız olduğunu göstermiyor. Normal kullanımda sürekli boş ekran kabul edilmez. Bu inceleme normal launcher açılışının ayrıca doğrulandığı anlamına gelmez. Bundan sonraki tablet testlerinden önce kullanıcı bilgilendirilecek.

Ana 45 maddelik listede 22 tamamlanan / 23 açık durumu korunuyor; geniş UI ve cihaz matrisi maddeleri kapanmadı. Sonraki UI incelemesi: şehir aramasındaki eski sonuçlar ve bildirim eylemlerinin erişilebilirliği.

## Şehir araması / bildirim erişilebilirliği devam turu

Şehir ekranı artık kısa ve boş sorguyu da ViewModel'e iletiyor. Her yeni sorguda eski öneriler temizleniyor, sorgu trim ediliyor ve iptal edilmiş işin sonuç yayınlaması ensureActive ile engelleniyor. Ülke metni sabit Türkiye yerine veriden geliyor. Arama etiketi, varsayılan/sil eylem adları ve uzun öneri metni için ağırlık eklendi. Bildirim kartına kaydırma gerektirmeyen erişilebilir sil/okundu/seç eylemleri ve okunma/seçim durumu eklendi. Tam ekran klavye/büyük yazı ve gerçek TalkBack doğrulaması açık; tablet testleri bu tur çalıştırılmadı.

Güncel kullanıcı özeti: [Yayın durum raporu](YAYIN_DURUM_RAPORU.md). Ana 45 madde: 22 tamamlanan, 23 açık. Alt düzeltmeler tüm cihaz matrisinin tamamlandığı anlamına gelmez.

## Kullanıcının kapsamlı kalite raporuna göre geniş denetim

20 kategorilik kapsam matrisi, severity/tekrar adımları/etki/kanıt ve açık testler ayrı dosyada: [Kapsamlı kalite denetimi](KAPSAMLI_KALITE_DENETIMI_2026-10-22.md). Bu tur 5 kodla doğrulanmış bug, 1 doğrulama gereken UI riski ve 1 Improvement kaydedildi; bunlar uygulamanın toplam bug sayısı değildir.

Düzeltmeler: farklı hedefli aynı metin bildirimleri gizlenmiyor; boş misafir listesinde iki katmandaki sahte bildirim geri dönüşü kaldırıldı; tümünü sil onayı kapanıyor ve sonuç bekleniyor; ortak bileşenlerin zorunlu büyük harf dönüşümü kaldırıldı; şehir arama/öneri/kayıtlı liste tek LazyColumn ve IME boşluğu altında; bildirimlerde kart başına timer yerine tek STARTED-lifecycle saati kullanılıyor. İlk kurulum örnek bildirimi seed akışı ayrı kalıyor. Pil/frame kazancı ölçülmüş gibi sunulmuyor.

İlk cihaz koşusunda 6 veritabanı/model testi geçti, 10 Compose testi No compose hierarchies found ile başarısızdı. ADB ekranı SCREEN_STATE_OFF ve showing=true gösterdi. Kullanıcı kilidi açtıktan sonra son kodla tekrar test başlatıldı. İlk başarısız koşu kaydı korunuyor. Tam uygulama görsel matrisi, gerçek TalkBack, release performans profili, canlı hesaplar ve kalıcı silme devam kaydı hâlâ açık.

## Geniş denetim — tamamlanan doğrulama

- Android: 125 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Lint: 0 hata, 187 uyarı, 2 ipucu.
- Samsung SM-T500 / Android 12: kilit açıldıktan sonraki son koşuda 16/16 test geçti (4 SQLite, 2 Firebase model, 10 izole Compose). Şehir yönetiminde %200 yazı/320×420 dp alanda kaydırıp varsayılan eylemine basma ve ortak butonda İzinleri yönet metni doğrulandı. Gerçek klavye/TalkBack/full-Activity rotasyon bu testler değildir.
- Backend: 64 test geçti, Ruff temiz. Genel koşuda atlanan 4 PostgreSQL testi daha sonra ayrı izole PostgreSQL 16 kümesinde geçti. Test ortamı üretime bağlanmadı.
- Firebase Firestore/Storage: demo-havamania-audit yerel emülatörlerinde 4 kural testi geçti; emülatörler kapatıldı. Logdaki PERMISSION_DENIED satırları beklenen negatif erişim testleriyle ilişkili, test başarısızlığı değil.
- İlk cihaz koşusunda kilitli ekrandan kaynaklanan 10 başarısızlık raporda tutuluyor. Son başarılı koşu önceki başarısızlığı gizlemez.
- Backend/dependency deprecation uyarıları var; bu tur bağımlılık güncellemesi yapılmadı.

Öncelikli devam: kalıcı AI silme devam kaydı ve çoklu işlem yarışları; bildirim tekli işlemlerinde hata/sahiplik; tam UI matrisinde telefon/yatay/klavye; ardından ölçümlü açılış/frame/RAM-pil profili. Önceki sekiz yayın başlığı korunuyor. Sonuç tüm ürünün yayın onayı değildir.
