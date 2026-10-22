# Ekran durumları, profil videosu ve kod denetimi — 22 Ekim 2026

## Sıra ve kapsam

Ana liste: **47 geniş madde; 22 tamamlandı, 25 açık**. Bu sayı bug sayısı değildir. Mevcut görsel madde genişletildi; kod erişilebilirliği/tekrar denetimi ayrıca açık madde olarak eklendi. Tüm ekranlar ya da tüm kod satırları tamamlandı sayılmıyor.

1. Bu tur: videodaki profil geçişi, profil veri bütünlüğü, kanıtlanan kod tekrarları.
2. Sonraki: aşağıdaki boş/dolu/yükleme/hata matrisi; ilk öncelik şehir, seyahat ve bildirimler. Profil sayaçlarının ilk yüklemede geçici sıfır göstermesi de ayrı doğrulanmalı.
3. Sonra: genel dil/tema/klavye/büyük yazı/yatay ekran matrisi; sen/siz, Outdoor/Extreme, tipografi ve kontrast.
4. Kod denetimi: Android yaşam döngüsü ve arka plan işleri; backend eksik entegrasyonlar; React Native test/derleme ve gerçek satın alma bağlantısı; ardından ana yayın listesinin dış ortama bağlı maddeleri.

## Her ekran için zorunlu durum matrisi

| Alan | Ayrı incelenecek durumlar |
|---|---|
| Hava/şehir | Hiç kayıt yok; kayıt var ama varsayılan yok; tek/çok şehir; varsayılan değişimi; yükleniyor; güncel/önbellek/eski veri; ağ hatası; yeniden dene; GPS kapalı/izin reddi; konum değişimi; saatlik/günlük panel; aramada boş sonuç/uzun ad |
| Seyahat | Hiç seyahat yok; seçili günde yok ama başka günde var; tek/çok; geçmiş/bugün/gelecek; form boş/dolu/hatalı; kayıt sürüyor/başarılı/başarısız; düzenle/sil/iptal; tarih/saat dilimi; çevrimdışı yeniden açılış |
| Rota | Başlangıç konumu yok; izin yok; hesaplanıyor; rota yok; servis hatası; kısmi hava verisi; uzun rota; riskli/risksiz güzergâh; yenileme |
| Profil | İlk yükleme; önbellekli açılış; aynı sekmeye tekrar dokunma; asistan–profil geçişi; boş/dolu bio ve Hakkımda; tercihsiz/tercihli; fotosuz/fotolu/yükleme hatası; sayaçlar; hesap değişimi; çevrimdışı; düzenleme taslağı |
| Asistan/geçmiş | Boş sohbet; ilk yanıt; mesaj sürüyor; ağ/zaman aşımı/iptal; geçmiş boş/dolu; detay yükleniyor/kayıp/hata; bekleyen kaydetme/silme; tekrar dene; hesap değişimi; uzun yanıt |
| Bildirim | Boş/dolu; filtre sonucu boş; okunmuş/okunmamış; tekli/çoklu seçim; silme/okundu hatası; tümünü sil; deep link; izin yok; hesap değişimi |
| Giriş/kayıt | Oturumsuz; boş/hatalı form; klavye; işlem sürüyor; ağ/servis hatası; başarı; parola sıfırlama; oturum süresi dolması |
| Ayarlar/tercihler | İlk okuma; seçim değişikliği; açık/koyu/otomatik/mevsimsel tema; aç/kapat; kayıt hatası; taslak iptali; Premium kapalı/gerçek yetki; yasal sayfa erişimi |
| Hesap silme | Onay, yeniden kimlik doğrulama, her adımda kesinti, kısmi tamamlanma, yeniden başlatma/devam, cihaz verisi temizliği. Gerçek hesap silme yapılmaz; ayrı test hesabı/ortam gerekir. |

Her satırın uygulanabilir varyantları: telefon/tablet, dikey/yatay, normal/%200 yazı, klavye açık/kapalı, TalkBack, geri hareketi, arka plana al/geri dön, süreç ölümü ve farklı hesap. Bir ilk görünüm ekran görüntüsü bu matrisin tamamlandığı anlamına gelmez. Şehir/seyahat kayıtları kullanıcı hesabında deneme amacıyla değiştirilmez; izole veri ya da ayrı test hesabı gerekir.

## Video ve neden analizi

Gönderilen 7,62 saniyelik video ses işlenmeden incelendi. 0,5 saniye aralıklı kareler .local-tests/profile-video/contact.jpg dosyasında yerelde tutuluyor. Yaklaşık 3. ve 6,5. saniyelerde asistandan profile dönüş görülüyor. Video bir crash kanıtı değil; tekrar kurulan görünüm ve sonradan gelen içerik hissini gösteriyor.

Kodla doğrulanan nedenler:

- Alt sekmelerin bazılarında saveState=true iken restoreState=false kullanılıyordu; profile/asistana dönüşte yeni ekran durumları oluşturuluyordu.
- Profil başlığı Firestore profilinden, kişilik rozeti ve ilgi bilgileri ayrı ThemeViewModel/DataStore akışlarından okunuyordu. İlk boş tercihler gerçek tercihler gelene kadar varsayılan rozet üretebiliyordu.
- Profil ekranı kendi ProfileViewModel örneğini oluştururken Activity zaten aynı profili izliyordu.
- Aynı profil iki ayrı senkronizasyon yolundan, çok sayıda ayrı DataStore yazımıyla güncelleniyordu. Yollardan biri boş metinleri atlıyordu; bu da uzaktan temizlenen alanların eski yerel değerini koruyabiliyordu.

## Yapılan düzeltmeler

- Sekme geri yükleme etkin; profil ve ana hava ekranı Activity’nin mevcut ProfileViewModel örneğini kullanıyor.
- Profil adı/bio/fotoğraf/tercihler/Hakkımda/rozet aynı kullanıcıya ait tek profil snapshot’ından okunuyor. Profil hazır değilse sahte varsayılan profil yerine yükleniyor durumu var; farklı UID’nin profili gösterilmiyor.
- Aynı hesaba ait eldeki repository profili yeniden gözleme sırasında korunuyor.
- Profil alanları tek DataStore transaction ile yazılıyor; boşaltılmış alanlar ve eksik tercih listesi de uygulanıyor. Yazma collector içinde sıralı; ayrı her-alan işler kaldırıldı.
- Kullanılmayan syncWithFirebase ve syncDataStoreFromProfile yolları birleştirildi; Premium yetkisi repository gözleminden güncelleniyor.
- Manifestte bulunmayan, uygulama/test kodundan referans almayan eski WeatherActivity ve yinelenmiş navigasyon grafiği kaldırıldı. Etkin giriş WeatherPremiumActivity.
- Hiç atanmayan userDocListener alanı, ikinci başlangıç loadSettings çağrısı, kullanılmayan backgroundGradient/activeWeatherData ve header onEditClick parametresi kaldırıldı.
- Rozet büyük harf dönüşümü Türkçe locale kullanıyor.
- Eski React Native smoke testindeki olmayan SignUpScreen import’u/rota adı mevcut RegisterScreen ile eşleştirildi. Jest koşusu henüz doğrulanmadı.

## Genel tarama sınırları ve açık bulgular

Android/React Native/backend kaynak dizinlerinde 254 dosyalık envanter, çağrı referansları, manifest, placeholder/TODO ve yükleme/geçiş örüntüleri tarandı; bu satır satır tam güvence değildir. Backend Ruff kontrolü temiz. React Native TypeScript komutu node_modules olmadığı için başlayamadı; bağımlılık kurulumu ve Jest/tsc açık.

- React Native iapService gerçek satın alma yerine demo makbuzu döndürüyor. Android Kotlin girişinden ayrı; gerçek IAP entegrasyonu olmadan satış onayı verilmemeli.
- Backend ExternalAlertProvider hâlâ dış uyarı entegrasyonu için iskelet. Soyut sağlayıcıların NotImplementedError metodları tek başına ölü kod değildir.
- Hava ekranının bütün WeatherUiState üzerinden 1 saniyelik Crossfade’i ve yapay giriş gecikmeleri ayrıca cihazda ölçülmeli; tüm animasyonları körlemesine kaldırmadık.
- Profil rota/analiz sayaçlarının ilk veri okuması, hata/yenileme, hesap geçişi ve süreç ölümü ayrı uçtan uca kontroller gerektirir.
- Build/manifest/Firebase dönüştürücü/reflection/DI/dış platform girişleri doğrulanmadan “az referanslı” dosyalar silinmez. React Native/iOS kodu sırf Android manifestinde yok diye kaldırılmaz.

## Doğrulama

ProfileSnapshotTest, izole UID’ler kullanır: profil alanları ara karışık snapshot yayımlamadan birlikte değişir; boş profil önceki fotoğraf/bio/tercihleri temizler; başka UID korunur. Test kendi anahtarlarını finally içinde temizler. Üretim Firebase’e test yazımı yapmaz.

Son test ve cihaz geçiş kaydı sonucu aşağıya eklenecek.

### Son yerel sonuç

- 125 JVM testi geçti; son debug APK derlendi.
- SM-T500 / Android 12 üzerinde ProfileSnapshotTest: 1 test, 0 hata; kendi UID anahtarlarıyla gerçek DataStore atomik güncellemesi ve hesap ayrımı kontrol edildi.
- İlk lint koşusunda bildirilen girinti hatası güncel kaynakla tekrar koşuda yok: lintDebug başarılı, 0 hata / 176 uyarı / 2 öneri.
- Backend Ruff temiz. Backend çalışma zamanı testleri bu tur tekrarlanmadı; backend kodu değişmedi.
- React Native RegisterScreen import düzeltmesi kaynakla doğrulandı; node_modules bulunmadığından Jest/TypeScript çalıştırılamadı.
- Yeni APK tablete kuruldu. Cihaz açıldıktan sonra oturumsuz karşılama ekranı görüldü; profil geçiş kaydı için kullanıcıdan tablette giriş istendi. Test sonrası gerçek uygulamayı tekrar kurmak ve oturumu kontrol etmek gerekir; test ekranı ürün ekranı doğrulaması sayılmaz.

### Oturum açıldıktan sonra canlı doğrulama — 22 Ekim 2026

Kullanıcı tablette giriş yaptı. Güncel debug sürümünde şehir seçilmemiş başlangıç ekranından profile girildi, ardından iki kez Asistan → Profil geçişi yapıldı. 15 saniyelik kayıt sessiz olarak incelendi (4 kare/sn örnekleme ve tam çözünürlüklü seçili kareler).

- İlk örneklenen profil karesinde (yaklaşık 1,00 sn) ad, biyografi ve YOL SAVAŞÇISI rozeti birlikte görünür. Sekmeye dönüşlerde (yaklaşık 6,25 ve 10,50 sn) farklı/geçici rozet veya ikinci profil yerleşimi gözlenmedi.
- Ayrı bir açık sorun doğrulandı: ilk profil karesinde rota ve kayıtlı analiz sayaçları 0, veri geldikten sonra 27 ve 22. Bu gerçek boş durum değildir; yükleniyor durumunu sayıdan ayırmak gerekiyor. Sekmeye dönüşlerde yüklü değerler korunuyor. Profildeki bütün gecikmeli içerik sorunlarının bittiği iddia edilmez.
- Bu doğrulama giriş yapılmış mevcut hesap ve tablet üzerinde sekme dönüşünü kapsar; süreç ölümü, hesap değiştirme, çevrimdışı başlangıç ve tüm ekran durumları henüz tamamlanmadı.
- Yerel kanıtlar: .local-tests/profile-video/after.mp4, after-contact.jpg, profile-first.png, profile-return.png. Kişisel veri içerebildiği için Git'e eklenmez.

Sıradaki somut iş: profil sayaçlarının yükleme/hata/gerçek sıfır ayrımı; ardından şehir ve seyahat ekranlarının boş/dolu durumları. Genel takip: 47 ana madde, 22 tamamlanan, 25 açık. Bu turdaki alt düzeltmeler bütün görsel denetim maddesini kapatmaz.


Güncel kapsam eki: Firebase yaşam döngüsü, Firebase–cihaz ve uçtan uca senkronizasyon ana listeye üç açık madde olarak eklendi. Güncel takip 50/22/28 (toplam/tamamlanan/açık); yukarıdaki 47/22/25 önceki durumdur. Ayrıntılar YAYIN_ONCELIKLERI.md üçüncü başlığında.
