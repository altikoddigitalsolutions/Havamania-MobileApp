# Android / Google Play yeniden denetimi — 22 Ekim 2026

## Kapsam ve karar

GitHub `origin/main` çekildi; başlangıç kodu `5d1e52a8b01f90b1e6d5e70b20f2d54eb3c78186` ile aynıydı. Hedef Kotlin/Compose Android uygulaması. Bu tur yerel kod, test, manifest, derleme/imzalama ayarları, Firestore modelleri, arka plan işleri, profil ve ortak arayüz bileşenleri incelendi. Bütün ekranların görsel veya tüm canlı akışların uçtan uca doğrulandığı iddia edilmiyor.

**Yayın henüz hazır değil.** Yerel düzeltmelerin yanında aşağıdaki dış koşullar kapanmalı.

## Bu tur bulunan ve düzeltilenler

| Öncelik | Sorun | Düzeltme |
| --- | --- | --- |
| P1 | Profil düzenleme ekranı sunucu yanıtını beklemeden kapanıyor; kayıt hatası görünmüyor. | Ayrı kayıt durum makinesi, çift tıklama engeli, 20 saniyelik onay sınırı, görünür hata/tekrar deneme ve yalnızca onay sonrası geri dönüş. |
| P1 | Profil kayıt işlemi bekleme sonrasında `auth.currentUser` üzerinden farklı Firebase kullanıcısının adını değiştirebilir. | Başlangıçtaki kullanıcı nesnesi ve UID tutuluyor; hesaba bağlı işler oturum değişiminde iptal ediliyor. |
| P1 | AI geçmişi, mesaj ve eylem modellerinde Firestore için boş kurucu yok; `mapNotNull` hatalı okumayı sessizce atlıyor. | Varsayılan değerlerle boş kurucular sağlandı. Mesajların mevcut Firestore `user`/`fallback` alan adları getter/setter anotasyonlarıyla korunuyor; JSON alan adları değişmedi. Eylem enum'u R8 için korunuyor. Room şeması değişmedi. |
| P1 | Uzun süren seyahat analizinden sonra eski hesabın sistem bildirimi gösterilebilir; iptal hata gibi yutuluyor. | Analizden sonra ve sistem bildirimi öncesinde oturum/silme kontrolü; coroutine iptalini yeniden iletme. Zaten gösterilmiş bildirimlerin tüm yaşam döngüsü bu değişiklikle doğrulanmış sayılmaz. |
| P2 | Her profil snapshot'ı kullanıcının yazdığı taslağı eziyor; rotasyonda taslak kayboluyor. | Hesaba bağlı `rememberSaveable`, yalnızca ilk profil yüklemesinde taslak doldurma. Gerçek süreç ölümü ve rotasyon akışı ayrıca cihaz matrisinde açık. |
| P2 | Profil formunda klavye ve sabit bio yüksekliği büyük yazıda sorun yaratıyor. | Klavye boşluğu ve minimum yükseklik kullanıldı. |
| P2 | Ortak düğmenin yüklenirken erişilebilir adı kayboluyor; dolu metin alanında etiket görünmüyor. | Yükleme durumunda düğme adı/durum semantiği; kalıcı alan etiketi. |
| P2 | Premium ekranı satış yokken ödeme tahsil edileceğini söylüyor. | Bu sürümde satış ve ücret tahsilatı olmadığı açıklandı; sabit düğme yüksekliği kaldırıldı. |

Kayıt zaman aşımı, sunucuda başlamış yazıyı geri almaz. Kullanıcıya "kayıt onayı alınamadı" denir; aynı alanlarla tekrar deneme güvenlidir. Firebase ve DataStore arasında dağıtık işlem/atomiklik garantisi yoktur.

## Yayın engelleri ve sıra

1. **Canlı hesap silme ve backend/Firebase eşleşmesi:** yerel backend ayarında özellik kapalı; staging uçtan uca doğrulama ve üretim dağıtım planı gerekli. Gerçek hesap silinmedi.
2. **Gerçek release AAB:** bu klonda `keystore.properties` yok. Anahtar, Play uygulama kimliği/versionCode ve R8 küçültülmüş paket cihaz testi gerekli. Debug paket bu doğrulamanın yerine geçmez.
3. **Yasal sayfalar ve hesap silme web bağlantısı:** `/privacy`, `/terms`, `/kvkk` web aracıyla doğrulanamadı; bu bulgu tek başına sayfaların kapalı olduğunu kanıtlamaz. Play Console beyanları, saklama süreleri ve silme web akışı tamamlanmalı.
4. **Üretim sağlayıcıları:** Open-Meteo genel API, OSRM ve MapLibre demo uç noktaları kodda duruyor. Ticari kullanım, kota, erişilebilirlik ve üretim sağlayıcısı kararları gerekli.
5. **Cihaz/tasarım matrisi:** telefon/tablet, açık/koyu tema, %200 yazı, TalkBack, izin reddi/geri çekme, arka plan, süreç ölümü, Android 16 ve gerçek 16 KB cihaz. İzole bileşen testleri tüm uygulama testi değildir.
6. **Rota ve zaman dilimi:** uç tarih aralıkları, şiddetli hava örneklemesi, astronomi/tarih gösterimleri; önceki açık maddeler devam ediyor.
7. **Üretim performansı:** soğuk açılış, kaydırma kare süreleri, bellek ve pil için release ölçümü yok. Debug hızını üretim performansı olarak raporlamıyoruz.
8. **CI / eski React Native:** `mobile/tests/auth.smoke.test.tsx` halen olmayan `SignUpScreen` import ediyor. Android native CI ayrı; eski React Native işi bağımsız bakım gerektiriyor. Bu tur iOS/React Native işlevleri değiştirilmedi.

## Güncel resmi koşullar

- 31 Ağustos 2026 itibarıyla yeni Android uygulaması/güncellemesi API 36 hedeflemeli. Projede compile/target SDK 36: [Android Developers](https://developer.android.com/google/play/requirements/target-sdk).
- Hesap oluşturulan uygulamalarda uygulama içi silme yolu ve Data Safety formunda çalışan silme web bağlantısı gerekir: [Google Play hesap silme gereklilikleri](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en).

## Doğrulama

Son koşu sonuçları aşağıya eklenir. Canlı hesap veya servis verisi değiştirilmedi. Firebase model testleri yalnızca SDK dönüştürücüsünü çağırır; belge get/set veya dinleyici çalıştırmaz. Arayüz testleri üretim Activity/ViewModel'lerini açmadan ortak bileşenleri ayrı test Activity'sinde çalıştırır.

## Devam turu: PostgreSQL ve rota riskleri

Gerçek PostgreSQL 16.12 testinde eski UNIQUE kısıtının silinmesiyle indeksi de kaldırıldığı için migration'ın ikinci DROP INDEX komutunda durduğu bulundu. Kısıt kaldırıldıktan sonra indeks listesi yeniden okunuyor. Dört senaryo geçti: boş kurulum/tekrar, eski kısıttan veri koruyarak yükseltme, çakışan token'da güvenli durma/yeniden deneme, bağımsız eski UNIQUE indeksinden yükseltme. Test kümesi mevcut PostgreSQL hizmetine bağlanmadan, rastgele localhost portunda ve ayrı veri dizininde oluşturulup kapatıldı. CI işi eklendi; uzak CI sonucu henüz yok.

Rota görünümündeki `take(7)` ve coğrafi yakınlık elemesi, geç veya birbirine yakın tehlikeleri saklayabiliyordu. Tehlike/eksik veri örnekleri artık görünüm bütçesinden bağımsız korunuyor; zaman çizelgesinde rota mesafesi sırası kullanılıyor. Aynı koordinattan farklı zamanda tekrar geçen rota noktaları korunuyor. Normal noktalar, başka koordinata hava etiketi taşımak yerine gerçek örneklenmiş noktalardan seçiliyor. Geometrik uzunlukla rota sağlayıcısının mesafesi farklı olduğunda örnekleme geometrik uzunluk üzerinde yapılıyor, ETA için sağlayıcının mesafe ölçeğine çevriliyor. Altı regresyon testi geçti. Bu, örneklenmeyen yol kesimlerinde risk olmadığı anlamına gelmez.

### Kesin son sonuçlar

- 85 Android birim testi, 7 SM-T500 cihaz testi geçti.
- `assembleDebug`, `lintDebug`: başarılı; lint 0 hata, 199 uyarı, 1 ipucu.
- Backend 64 test ve Ruff geçti; ayrı PostgreSQL koşusunda 4 test geçti.
- Firestore/Storage yerel emülatör koşusu: 4 test geçti.
- Altı ARM64/x86_64 kitaplığın 16 KB ELF hizalaması ve zipalign geçti.
- Son APK SHA-256: `07A8B9850529677EB193335B046D6AA59F52BB91AB9A1080CABCC3520F0D0E65`.
- Önce kilitli ekran nedeniyle başarısız olan üç Compose testi, ekran açıldıktan sonra iki ayrı koşuda geçti.

Sekiz başlığın kapanış koşulları ve unutulmaması gereken bütün açık maddeler [ana kontrol listesinde](YAYIN_ONCELIKLERI.md) tutuluyor. Üretim eşleşmesi, canlı hesaplar, tam cihaz/görsel matris ve imzalı release hâlâ açık.

## Devam turu: tahmin konumunun saat dilimi

WeatherViewModel ve WeatherView, doğru dönüştürülmüş tahmini cihazın tarihi/saatiyle yeniden filtreliyordu. Ortak ForecastTime yardımcı sınıfıyla gün ve saat seçimi tahmin konumuna bağlandı. Gece yarısında ekranda tarih yazan saatlerin ISO fullTime alanı kullanılıyor; süresi geçmiş önbellekte sabahın ilk saati güncel hava olarak seçilmiyor. Günlük tahminin ilk satırı koşulsuz bugün sayılmıyor; geçersiz tarihler ve yaz saati başlangıcındaki var olmayan saatler eleniyor.

Yedi yeni regresyon testiyle Android birim testleri 85 oldu (0 hata). Debug derleme ve lint yeniden geçti. Cihaz, backend, PostgreSQL ve Firebase kural testlerinin yukarıdaki sonuçları önceki koşulardandır; bu saat dilimi turunda tekrar çalıştırılmadı. Toplam 164 farklı test için başarılı sonuç kaydı var; aynı APK üzerinde tüm testlerin yeniden çalıştırıldığı anlamına gelmez. Yeni APK için hizalama kontrolü ayrıca yenilenmelidir.

Açık kapsam: seçilen gelecek güne ait güneş doğuş/batış saatlerinin bütün görünümlerde kullanılması, yaz saati bitişindeki tekrarlanan saatin belirsizliği ve tam cihaz/konum saat dilimi matrisi. Bu nedenle 4. başlığın geniş tarih/astronomi maddesi kapatılmadı. Ana listede 45 madde: 22 tamamlandı, 23 açık.
## Devam turu: güneş saatlerinin seçilen güne bağlanması

Günlük tahmin modeline varsayılanı null olan sunriseTime/sunsetTime alanları eklendi; eski JSON önbellekleri okunabilir. Ana kart, saatlik görünüm ve güneş zamanları paneli artık seçilen tarihin saatlerini kullanıyor. Eksik veya başka tarihe ait güneş verisi başka günden tamamlanmıyor. Saatlik dönüşümde bir günün eksik güneş verisi diğer günlerin geçerli verilerini iptal etmiyor. Güncel hava modelinin güneş saatleri de ilk satırdan değil, konumun bugünkü tarihinden seçiliyor.

Tam gün batımı anında hava metni gece olarak sınıflanıyor; saatlik sınıflamayla sınır aynı. Doğuş/batış ortalaması cihaz saat diliminden bağımsız LocalTime ile hesaplanıyor. Bu değer astronomik güneş öğlesi olmadığı için kullanıcı etiketi Gündüz ortası yapıldı.

Kalan kapsam: AI günlük yanıtının tarih/gece-gündüz metni, kutup günü/gecesinde sağlayıcının is_day verisinin kullanımı, yaz saati bitişindeki tekrarlanan saat ve gerçek cihazda gün seçimi/görsel kontrol. Ana listedeki geniş astronomi maddesi bu nedenle açık kalıyor.
Doğrulama: 90 birim testi (0 başarısız), debug APK derlemesi ve yeni APK üzerinde altı yerel kütüphanenin ELF/zipalign kontrolü geçti. SHA-256: `2CA9F522446B4CD1ABF6D622C04E60D124EE70D9973F592B62D93AB1C7BB4CDD`. Yeni testler: gün bazında güneş saatleri, bozuk/yanlış tarih, eski JSON uyumu, eksik/kutup verisinde orta saat üretmeme ve gün batımı sınırı. Cihaz, backend ve Firebase testleri bu tur yeniden koşulmadı.
Son Gradle koşusu: testDebugUnitTest, assembleDebug ve lintDebug birlikte BUILD SUCCESSFUL (3 dakika 10 saniye).
## Devam turu: AI günlük tahmin tarihi

Yerel asistanın tarihli hava yanıtı ayrı ve test edilebilir AiWeatherReply bileşenine taşındı. Bugün/yarın, veri alındıktan sonra tahmin konumunun saat diliminde çözülüyor. Günlük hava kodu tüm günü özetlediği için yanıt cihazın anlık gece/gündüz durumundan bağımsız. Donan yağış ve dolu kodları korunuyor, bilinmeyen kod güneşli diye gösterilmiyor. Eski önbellek yanıtında uyarı var; geçersiz yağış yüzdesi gösterilmiyor. İstenen gün yoksa güncel hava yerine o tarihin verisinin bulunmadığı söyleniyor; sabit 10 gün garantisi kaldırıldı.

Tarih ayrıştırıcısında yarından sonra +2 gün oldu; açıkça yazılan yıl korunuyor ve artık gün o yıl için doğrulanıyor. Varsayılan tarih parametresi diğer çağrı noktalarının uyumluluğunu koruyor.

Ek takip bulguları (mevcut 4/5. başlıklar kapsamında): geçersiz veya desteklenmeyen tarih ifadelerinin kullanıcıdan netleştirilmesi; RecommendationEngine alternatif yanıt yolundaki tarih/veri kapsamı; AiChatViewModel yerel selamlama/hava coroutine işlerinin hesap değişiminde iptali ve hata sonrası yükleme temizliği. Bu tur canlı AI/backend çağrısı yapılmadı.
AI tarih turu doğrulaması: 97 birim testi, 0 başarısız; assembleDebug başarılı. APK SHA-256: 5E11D3F1F1018D962F0C8C03BF9B13195E94106B11D3EFFE0646A5F0A5EE4823. Cihaz/backend/Firebase testleri bu tur tekrarlanmadı.
AI tarih turunun lintDebug kontrolü de geçti; tüm Gradle koşusu BUILD SUCCESSFUL (3 dakika 26 saniye).
## Devam turu: asistan isteklerinin hesap ve sohbet yaşam döngüsü

Selamlama, yerel hava yanıtı ve sunucu yanıtı AssistantTaskRunner üzerinden aynı tek-istek akışına alındı. İstek başlamadan yükleme kilidi ayrılıyor; hızlı ikinci gönderim yeni kullanıcı mesajı eklemiyor. Hesap değişimi, sohbet sıfırlama ve geçmiş sohbet açma istek neslini geçersiz kılıp işi iptal ediyor. İptali dikkate almayan bir bağımlılık geç dönse bile hem iptal durumu hem başlangıç UID/istek nesli denetlenmeden yanıt veya hata yayımlanmıyor. Eski isteğin finally bloğu yeni isteğin yükleme durumunu temizleyemiyor.

Yerel hava akışı da hata/yeniden deneme ve 60 saniyelik toplam zaman aşımı kapsamında. Sohbet geçmişi sorgusu başlangıç UID'sini kullanıyor; sonuç aynı korumadan geçiyor. Gönderilmeyen bağlamı hazırlamak için yapılan gereksiz hava sorguları uzak yanıt yolundan kaldırıldı; sunucuya yine yalnızca özgün soru gidiyor.

Bu değişiklik tüm ViewModel arka plan işlerinin hesabı izole ettiği anlamına gelmez: otomatik hava yükleme/seyahat gözlemleme ve gerçek cihaz A→B matrisi ayrı açık kontrollerdir. Canlı hesap veya AI sunucusuyla işlem yapılmadı.
Asistan yaşam döngüsü turu: 102 birim testi, 0 başarısız; debug derleme başarılı. APK SHA-256: 229EBD2786862B44771477FC4F72A446938393B10153115D6DDB4522DCF8F147. Önceki cihaz/backend testleri bu tur tekrarlanmadı.
Son Gradle doğrulaması BUILD SUCCESSFUL (3 dakika 19 saniye); lintDebug geçti.
Güncel lint sonucu: 0 hata, 196 uyarı, 1 ipucu.
## Devam turu: otomatik veri işleri ve UI taraması

AiChatViewModel otomatik hava yükleme, hava gözlemleme ve seyahat okuma işleri AccountTaskScope'a taşındı. Auth değişimi tüm bu işleri iptal ediyor. Askıdan dönüşte iptal ve başlangıç UID kontrolü yapılıyor; eski işin iptali yeni hesabın ekranına hata olarak yazılmıyor. Seyahat sorgusu yakalanan kullanıcıya ait, yayın Main üzerinde. Otomatik hava sonucu hem veri hem UI durumuna yazılıyor. Zaman aşımı kullanıcıya ayrı hata olarak bildiriliyor. Paylaşılan repository hava durumunun bütün üreticilerinin hesap sahipliği ve gerçek cihaz A→B testi yine açık.

UI alt envanteri: [ekran bazlı rapor](UI_DENETIMI_2026-10-22.md). Düzeltmeler: auth alan etiketleri/şifre düğmesi, asistan eylem adları ve IME boşluğu, öneri kartı minimum yüksekliği, geçmiş silme hedefi, ayar anahtar adları, büyük yazıda güneş paneli. Kişiselleştirme kaydı, profil alt penceresi ve geçmiş detay eylemleri yeni açık alt maddeler olarak kaydedildi.

102 birim testi geçti; debug APK üretildi. İlk cihaz koşusunda 3/10 test Activity hiyerarşisi bulunamadığı için başarısız; ekran açık ve kilitsizdi. Kod değişmeden tekrar koşuda 10/10 geçti. Gerçek üretim Activity'si veya canlı servis bu testlerin kapsamına dahil değil.
Bu turun ayrı lintDebug koşusu BUILD SUCCESSFUL (16 saniye). APK SHA-256: BD0CC2ECB2E76AC8FD9BD8D492DD9732FD2A275F97BEA9A613C19DEB5B866CE2.
## Devam turu: kişiselleştirme kayıt onayı ve taslak

PersonalizationSaveController, hesap/istek nesli korumalı istek yöneticisini kullanır. Sunucu ve DataStore yazıları tamamlandıktan sonra Saved yayımlanır; hata profil snapshot durumunu bozmaz. Çift gönderim engeli, 20 saniye onay sınırı ve tekrar deneme eklendi. Hesap değişiminde/eski ekran ayrılırken sonuç geçersiz kılınır. Ekran yalnızca Saved durumunda otomatik kapanır; kayıt sırasında geri/atla ve seçim değişiklikleri engellenir.

Taslak ilk eşleşen kullanıcı profilinden bir kez doldurulur; sonraki snapshot'lar düzenlenen seçimleri ezmez. İlgi alanı/seyehat tarzı kümeleri ve hassasiyetler saveable durumdadır; süreçten geri yüklenen taslağın kullanıcı kimliği eşleşmezse gösterilmeden temizlenir. Hassasiyet satırında uzun metin esnek genişlik kullanır ve anahtarın erişilebilir adı vardır. Başlık Türkçe karakterleri düzeltildi.

106 birim testi geçti (4 yeni: onayı bekleme/çift gönderim, hata/yeniden deneme, hesap değişimi/geç yanıt, zaman aşımı). Debug APK üretildi; SHA-256: 1EA7D8BCCD6EBC754386D587D97BAC0E4CE6132C7BEA33442E8BA9EBA18D6BF1. Canlı Firestore ve cihaz rotasyon/süreç ölümü testleri bu tur yapılmadı. İlk derlemede nullable profil kontrolü hatası yakalanıp düzeltildi. Sunucuda başlamış yazı iptal nedeniyle geri alınmış sayılmaz; hata mesajı bu yüzden kayıt onayının alınamadığını belirtir.
Son kişiselleştirme koşusu: testDebugUnitTest, assembleDebug, lintDebug birlikte BUILD SUCCESSFUL (3 dakika 46 saniye).
## Devam turu: Hakkımda penceresi ve seyahat ekleme

Hakkımda alanı 4–8 satır ve minimum yükseklikle, kaydet düğmesi minimum 56 dp ile büyüyebiliyor; kaydırılan içerik IME boşluğu kullanıyor. Dolu alan etiketli, taslak saveable. Bu alanda da sunucu onayı beklenmeden kapanma bulundu ve giderildi: ayrı kayıt durumu, 20 saniyelik onay sınırı, görünür hata/yeniden deneme ve hesap değişimi/ekran ayrılmasında geç sonucu reddetme. Firebase ve yerel aboutMe yazıları tamamlanmadan Saved yayımlanmıyor. Seyahat ekleme düğmesi 48 dp ve erişilebilir adı var.

106 birim testi ve debug derlemesi geçti. İlk cihaz koşusunda izole TestTheme'in LocalWindowSize sağlamaması nedeniyle yeni Hakkımda testi çöktü. Üretim teması bu sağlayıcıyı içeriyor; test teması ProvideResponsiveLayout ile düzeltildi. Cihaz doğrulaması tekrar çalıştırılıyor. APK SHA-256: 8B2E5F209F43ADBB24157220BF1D3C4BA7D5DCBC07B944D6F6A0031DB4957378.
Hakkımda turu cihaz tekrar sonucu: SM-T500 / Android 12 üzerinde 12/12 geçti. Yeni iki test saved-instance-state geri yükleme ve büyük yazıyla kaydırarak kaydet eylemine erişimdir. Test temasına ProvideResponsiveLayout eklenmesi ilk çöküşü giderdi.
Hakkımda turu son doğrulaması: lintDebug ve connectedDebugAndroidTest birlikte BUILD SUCCESSFUL (1 dakika 42 saniye).

## AI geçmiş detayı ve tablette görülen boş ekran — devam turu

Geçmiş detayı artık başlangıçtaki boş listeyi kayıt yok saymıyor. Yerel kullanıcıya ait kayıt okunuyor; bulunamazsa giriş yapmış kullanıcı için sunucudan belge doğrulanıyor. Yükleniyor, bulunamadı ve hata durumları ayrıldı; hata için yeniden deneme eklendi. 20 saniye sınırı, hesap değişimi ve geç gelen eski istek sonucunu engelleme var. Salt okunur geçmişte işlevsiz asistan eylem düğmeleri gizlendi. Geçmişin genel silme/senkronizasyon davranışı bu düzeltmeyle tamamen doğrulanmış sayılmaz.

Doğrulama: 111 JVM testi, 0 başarısızlık; assembleDebug ve lintDebug başarılı. Lint: 0 hata, 194 uyarı, 1 bilgi. Yeni 5 test bekleme, yokluk, hata/yeniden deneme, eski istek ve hesap değişimini kapsar. Canlı Firebase bağlantısı ve bu ekranın tam cihaz matrisi test edilmedi. Bu tur cihaz arayüz testleri yeniden çalıştırılmadı; önceki son cihaz koşusu 12/12 idi.

Kullanıcının 14:56:30 ve 14:56:32 ekran görüntüleri kayıtlı authFieldsKeepLabelsAndPasswordToggleAnnouncesAction testine denk geliyor: test 14:56:30.307, androidx.activity.ComponentActivity başlangıcı 14:56:30.452. Görüntüler bu izole test Activity geçişiyle uyumlu; normal uygulama başlangıcının başarısız olduğunu göstermiyor. Normal kullanımda sürekli boş ekran kabul edilmez. Bu inceleme normal launcher açılışının ayrıca doğrulandığı anlamına gelmez. Bundan sonraki tablet testlerinden önce kullanıcı bilgilendirilecek.

Ana 45 maddelik listede 22 tamamlanan / 23 açık durumu korunuyor; geniş UI ve cihaz matrisi maddeleri kapanmadı. Sonraki UI incelemesi: şehir aramasındaki eski sonuçlar ve bildirim eylemlerinin erişilebilirliği.

## Şehir araması / bildirim erişilebilirliği devam turu

Şehir ekranı artık kısa ve boş sorguyu da ViewModel'e iletiyor. Her yeni sorguda eski öneriler temizleniyor, sorgu trim ediliyor ve iptal edilmiş işin sonuç yayınlaması ensureActive ile engelleniyor. Ülke metni sabit Türkiye yerine veriden geliyor. Arama etiketi, varsayılan/sil eylem adları ve uzun öneri metni için ağırlık eklendi. Bildirim kartına kaydırma gerektirmeyen erişilebilir sil/okundu/seç eylemleri ve okunma/seçim durumu eklendi. Tam ekran klavye/büyük yazı ve gerçek TalkBack doğrulaması açık; tablet testleri bu tur çalıştırılmadı.

Güncel kullanıcı özeti: [Yayın durum raporu](YAYIN_DURUM_RAPORU.md). Ana 45 madde: 22 tamamlanan, 23 açık. Alt düzeltmeler tüm cihaz matrisinin tamamlandığı anlamına gelmez.

## AI geçmişi silme — devam turu

Tek kayıt silmede Firestore delete görevi artık await ile bekleniyor; sunucu onayından önce yerel kayıt kaldırılmıyor. Toplu silme listeyi SERVER kaynağından okuyor. Her iki işlemde 30 saniye onay sınırı, ortak meşgul durumu, çift işlem engeli, görünür hata ve yeniden deneme var. Hesap değişimi işlem/hata/yeniden deneme durumunu temizliyor. Eski snapshot işleri epoch ve yerel yazma kilidiyle silme işlemiyle sıralanıyor. Onay diyalogları hesaba bağlı; tümünü sil düğmesi adlandırıldı.

Sınırlar: await iptali Firestore'a gönderilmiş yazıyı geri almaz; zaman aşımında sunucuda silme sonradan tamamlanabilir. Bu nedenle hata metni “tamamlandığı doğrulanamadı” der. Çok partili toplu silme kısmen tamamlanabilir, yeniden deneme kalan belgeleri tekrar okur. Başka cihazdan silinen kayıtları yerel listeden çıkarma, eşzamanlı kayıt/silme, süreç ölümü sonrası kalıcı işlem takibi ve kayıt ekleme hatasının görünürlüğü hâlâ açık. Gerçek Firebase testi yapılmadı; bu bölüm tüm senkronizasyonun kapandığı anlamına gelmez.

Yeni üç birim testi sunucu onayını bekleme, hata sonrası yerel kopyayı koruyarak yeniden deneme ve iptal sonrası yerel silmeyi engellemeyi kapsar. Bu tur tablet testi çalıştırılmadı. Ana liste 22/45 tamamlanan, 23 açık geniş madde olarak korunuyor.

Silme turu doğrulaması: 114 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı.

## Geçmiş senkronizasyonu — devam turu

Aktif dinleyicide sunucudan gelen REMOVED olayları artık yerel kayda uygulanıyor. Önbellek snapshot'ı veya bekleyen yazısı olan snapshot silme için kullanılmıyor. Silinen belgenin updatedAt alanı eksikse güvenli tarafta kalıp yerel veri korunuyor. Daha yeni yerel düzenlemeler korunuyor; silme SQL koşulu ile kullanıcı ve sürüm karşılaştırılarak yapılıyor. Sunucudan ekleme/güncelleme birleştirmesi de Room transaction içinde daha yeni yerel veriyi ve başka kullanıcı sahipliğini koruyor. Dinleyici işleri ana coroutine kuyruğundan yerel kilide giriyor.

Aynı AiHistoryViewModel içindeki kayıt/silme işleri ayrı mutationMutex ile sıralanıyor; kayıtta Firestore görevi 30 saniyeye kadar bekleniyor. Yerel kayıt hemen listeye yükleniyor. Silme sırasında liste okuması dinleyiciyi erken başlatmıyor; eski işlemin finally bloğu yeni hesap döneminin dinleyicisini değiştirmiyor.

Kalan sınırlar: uygulama kapalıyken gerçekleşen silmelerin ilk açılışta uzlaştırılması, kalıcı pending-write/tombstone kayıtları, farklı ViewModel/cihazlar arası eşzamanlı düzenlemeler, saatleri farklı cihazlarda sürüm çatışması ve kaydet/bitir ekranında kayıt hatasının görünürlüğü. Silme olayı pending/cache nedeniyle atlandıysa sonraki tam sunucu uzlaştırması gerekir; henüz bu iş kapatılmadı. Firestore zaman aşımı yazıyı geri almaz. Kayıt ve silmenin aynı ViewModel'de sıralanması küresel bir kilit değildir.

Dört yeni birim testi sunucuca onaylı silme, daha yeni yerel düzenleme, önbellek ve bekleyen yazı korumalarını kapsar. Bunlar gerçek Firestore/Room entegrasyonu yerine geçmez. Bu tur canlı bağlantı veya tablet testi yapılmadı. Ana liste 22/45 tamamlanan, 23 açık geniş madde olarak korunuyor.

Son kodla doğrulama: 118 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı.

## Sohbeti kaydet ve bitir — devam turu

Kaydet ve bitir artık kayıt tamamlanmadan sohbeti temizlemiyor. Ayrı Idle/Saving/Saved/Failed durumu, 30 saniye onay sınırı, çift gönderim engeli ve diyalog içinde hata/yeniden deneme eklendi. Giriş yapılmış hesapta Firestore set onayı bekleniyor; misafir için yerel kayıt tamamlanması yeterli. Hata sohbeti silmez. Aynı ViewModel ömründeki yeniden denemeler aynı belge kimliğini kullanır; başarıdan sonra yeni sohbet için kimlik sıfırlanır. Hesap değişiminde eski onay reddedilir; ekran ayrılınca bekleyen onay iptal edilir. Bu iptal, sunucuda başlamış yazıyı geri almaz.

İlk açılışta kaçırılan silmeleri uzlaştırma bu tur kapatılmadı: yerel kaydın sunucuda onaylandığını belirten kalıcı işaret olmadan eksik sunucu kaydı ile henüz yüklenmemiş yerel sohbet ayırt edilemiyor. Sıradaki iş kalıcı senkronizasyon durumu ve Room migration testleri; ardından tam sunucu snapshot'ı ile güvenli uzlaştırma. Süreç ölümü sonrası aynı kayıt kimliğiyle yeniden deneme de bu kalıcı durum çalışmasına dahil. Yeni dört test onay bekleme/çift gönderim, aynı kimlikle yeniden deneme, hesap değişimi ve ekran ayrılmasını kapsar. Canlı Firebase ve tablet testi yapılmadı.

Kayıt onayı turu doğrulaması: 122 JVM testi (0 başarısızlık), assembleDebug ve lintDebug başarılı.

## Kalıcı geçmiş doğrulaması ve ilk açılış uzlaştırması

Room sürümü 18 oldu. 17→18 geçişi ai_history tablosuna nullable syncedUpdatedAt sütunu ekliyor; eski kayıtları sunucuda onaylanmış varsaymıyor. Alan Firestore'a gönderilmiyor. Yerel kayıt/düzenleme yeni nesnede null olarak başlar. Yalnızca cache olmayan ve pending write içermeyen tam sunucu snapshot'ı ilgili sürümü doğrular.

Tam sunucu koleksiyonu Room transaction içinde birleştirilir; sunucuda bulunmayan kayıt yalnızca syncedUpdatedAt ile updatedAt aynıysa silinir. Sahiplik korunur, daha yeni yerel sürüm ezilmez. Bozuk belge ayrıştırılamasa bile ham belge kimliği var sayılır; yanlış yokluk kararı verilmez. Snapshot hatası veya çevrimdışı cache temizleme başlatmaz. Aktif oturumdaki özel REMOVED mantığının yerini bu tam-liste uzlaştırması aldı; metadata değişiklikleri dinlenmeye devam eder.

Yeni izole SQLite testi veritabanını kapatıp yeniden açarak sunucu onayının kalıcılığını, uzaktaki silmenin uygulanmasını, pending/eski sürümden taşınan kayıtların korunmasını, yeni yerel düzenlemeyi, başka kullanıcı sahipliğini ve bozuk belge kimliğini doğrular. 11→18 migration zinciri testinde de eski AI kaydının null işaretle korunduğu denetlenir.

Sınırlar: eski sürümden gelen ve sunucuda hiç yeniden doğrulanmamış kayıtlar bilinçli olarak korunur; geçmişte silinmiş bütün kayıtların otomatik temizlendiği iddia edilmez. İşaret, tek başına kalıcı yeniden yükleme kuyruğu veya silme tombstone'u değildir. Süreç ölümü sonrası kaydetme isteğinin aynı kimlikle sürdürülmesi, başarısız kayıtları yeniden yükleme kuyruğu ve farklı cihaz saatlerinde çatışma çözümü açık. Canlı Firebase erişimi yapılmadı.

Kalıcı uzlaştırma turu doğrulaması: 122 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 5 cihaz testi (3 veritabanı, 2 Firebase model; 0 başarısızlık/0 hata), assembleDebug ve lintDebug başarılı. UI testleri bu koşuda çalıştırılmadı; canlı ağ çağrısı yapılmadı.

## Kalıcı bekleyen sohbet yüklemeleri — devam turu

Room 18→19 geçişi pendingUpload sütununu ekliyor; varsayılan 0 olduğu için eski/bilinmeyen kayıtlar otomatik sunucuya gönderilmiyor. Yeni yerel kayıt, uzak yazıdan önce aynı satır/kimlik ve pending işaretiyle saklanıyor. Sunucu onayı yalnızca aynı kullanıcı, kimlik ve updatedAt sürümünü işaretler; eski onay daha yeni düzenlemeyi kuyruktan çıkarmaz. Yerel sürüm aynı milisaniyedeki kayıtlarda da monoton artırılır. Pending kayıt tam snapshot uzlaştırmasıyla silinmez/ezilmez. Yerel takip alanları Firestore'a gönderilmez.

AiHistoryViewModel oluşturulup hesap bağlandığında bekleyen kayıtlar kendi mevcut belge kimlikleriyle yeniden denenir. Tek koşu 60 saniye ile sınırlıdır; hata kalan kayıtları veritabanında bırakır. Geçmiş ekranında bekleyen sayı/hata ve Yeniden gönder eylemi bulunur. Kaydet ve bitir mevcut bir sohbeti düzenliyorsa conversationId geçirilir. Kayıt içeriği Room'a yazıldıktan sonra uygulama kapanırsa sonraki kuyruk gönderimi yeni belge üretmez. Room yazısı öncesindeki kaydedilmemiş taslağın süreç ölümüyle kurtarılması garanti edilmez.

Silme sırası geldiğinde ilgili pending işaretleri önce kapatılır; başarısız silmenin ardından otomatik yükleme ile kayıt diriltilmez. Sunucuda zaten başlamış Firestore yazıları iptal garantisi taşımaz. Ayrı ViewModel/cihazlar arası yarışlar ve kalıcı silme tombstone'u hâlâ açık. Kayıt kuyruğu WorkManager arka plan işi değildir; uygulama kapalıyken gönderim veya ağ geri geldiği anda kendiliğinden çalışma vaat edilmez. Yeniden hesap bağlanması/ekranın ViewModel oluşturması ve manuel yeniden gönderme tetikler.

Yeni gerçek SQLite testi veritabanını yeniden açıp aynı kayıt kimliğinin kaldığını; kullanıcı izolasyonunu, eski onayın yeni sürümü kuyruktan çıkaramamasını, doğru onayın kuyruğu kapatmasını ve silme niyetinin yüklemeyi durdurmasını kontrol eder. 11→19 migration zinciri eski kayıtların pending=false olarak korunduğunu denetler. Ana durum 22/45 tamamlanan, 23 açık geniş madde.

Kuyruk turu doğrulaması: 122 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 6 cihaz testi (4 veritabanı, 2 Firebase model; 0 hata), assembleDebug ve lintDebug başarılı. Compose UI ve canlı Firebase testleri bu tur çalıştırılmadı.

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

## Kalıcı AI geçmişi silme devam kaydı — 19→20

Silme talebi kullanıcı/sohbet kimliğiyle yeni ai_history_deletions tablosuna yazılır; aynı transaction yükleme işaretini kapatır. Boş sohbet kimliği tüm geçmiş isteğini temsil eder. Uzak silme onayı gelmedikçe talep bekler. Yeniden ViewModel/hesap bağlanmasında bekleyen silmeler, yüklemelerden önce yeniden denenir. Her iki kuyruk aynı süreçte tüm AiHistoryViewModel örneklerinin paylaştığı coroutine kilidini kullanır.

Onaylanan tekli silme işareti tamamlandı olarak korunur; geç snapshot ve kayıt girişleri o kimliği yeniden oluşturamaz. Toplu silmede bilinen yerel/uzak kimlikler için aynı koruma tutulur; genel engel tamamlanınca kaldırılır, yeni kimlikli sohbetler kaydedilebilir. Hesap silme cihaz temizliği bu işaretleri de temizler. Aynı silmenin tekrar tamamlanması güvenlidir. Room 19→20 migration yeni tabloyu oluşturur; eski sohbetler değiştirilmez.

Yeni SQLite testi veritabanını kapatıp açar; bekleyen silme kimliği, kullanıcı izolasyonu, yükleme engeli, eski snapshot ve kayıt engeli, idempotent tamamlama ve tamamlanmış silmeden sonra geç snapshot korumasını denetler. Toplu silme işaretinin diğer kullanıcıyı etkilemediği de kontrol edilir.

Sınırlar: veritabanına niyet yazılmadan süreç ölürse talep kaydedilmiş sayılmaz. Başka cihazda eşzamanlı oluşturulan yeni kayıtların küresel silme garantisi yoktur. Tamamlanmış kimlik işaretleri hesap verileri silinene kadar saklanır; zamanla büyüme için güvenli temizleme politikası ayrıca değerlendirilmeli. WorkManager arka plan yürütmesi ve gerçek Firebase kesinti/çok cihaz testi açık. Bu tur canlı kullanıcı verisi kullanılmadı. Ana 22/45 geniş madde durumu korunur; bütün senkronizasyon matrisi kapanmaz.

Kalıcı silme turu doğrulaması: 125 JVM testi (0 başarısızlık), SM-T500/Android 12 üzerinde seçili 7 cihaz testi (5 SQLite, 2 Firebase model; 0 başarısızlık/0 hata) geçti. Debug derleme ve lint başarılı; 0 lint hatası, 187 uyarı, 2 ipucu. UI, backend ve canlı Firebase bu tur yeniden test edilmedi. Sonraki iş bildirim tekli silme/okundu işlemlerinde hata ve hesap değişimi koruması.
