# Mimari ve ürün kalitesi denetim planı

22 Ekim 2026. Android / Google Play öncelikli. Ana takip: [yayın öncelikleri](YAYIN_ONCELIKLERI.md). Bu belge kullanıcının 30 maddesini eşler; yeni tamamlanmış denetim veya yeni doğrulanmış bug raporu değildir. Daha önce geçen testler kendi kapsamlarında geçerlidir; bütün satırlar uçtan uca doğrulama açısından açıktır.

## Kapsam ve kapanış ölçütleri

| No | Alan | İnceleme / test kapsamı |
|---|---|---|
| 1 | Bug / crash | Null ve bozuk veri, süreç ölümü, eksik izin, yarış koşulu; yeniden üretme ve regresyon testi. |
| 2 | Firebase Auth / Firestore / Storage | Şema, sahiplik, token, dinleyici, dosya yükleme/silme ve hata akışları. |
| 3 | A–B veri izolasyonu | Bekleyen istek, cache, kuyruk, bildirim ve UI eski kullanıcıyı yeni hesaba taşımamalı. |
| 4 | Seyahat ve güzergâh | Mesafe/ETA/saat dilimi, alternatif ve uzun rota, eksik geometri ve risk örnekleme. |
| 5 | Hava: 10 gün / 48 saat | Tek ürün kuralı; tam sınır/öncesi/sonrası, eksik sağlayıcı verisi ve eski cache. |
| 6 | Asistan / kişiselleştirme | Tercih kaydından öneriye kadar veri akışı; aşağıdaki karşılaştırmalı kabul testleri. |
| 7 | Bildirimler | Kimlik, tekrar, sahiplik, zamanlama, izin, deep link ve kapalı uygulama davranışı. |
| 8 | Room / DataStore / cache | Migration, atomik güncelleme, TTL, UID ayrımı, kuyruk ve silme uzlaştırması. |
| 9 | Kotlin / Compose | Durum sahipliği, lifecycle-aware collection, side effect, kararlı liste anahtarları. |
| 10 | React Native | Gerçek giriş noktaları, bağımlılıklar, TypeScript/Jest ve eksik native köprüler. |
| 11 | Backend / FastAPI | Şema/migration, yetki, işlem bütünlüğü, async I/O, hata sözleşmesi ve kapasite. |
| 12 | API / network | Timeout, iptal, HTTP hata eşlemesi, retry/idempotency, kota ve bağlantı kurtarma. |
| 13 | Performans | Açılış, ekran geçişi, liste, sorgu ve ağ ölçümleri; aynı cihazda önce/sonra p50/p95. |
| 14 | Bellek / recomposition | Tekrarlı navigasyonda bellek eğilimi, Activity/collector sızıntısı ve gereksiz yeniden çizim ölçümü. |
| 15 | Coroutine / async | Structured concurrency, iptalin yutulmaması, eski sonuç, çift iş ve yarış testleri. |
| 16 | Güvenlik | Ana listedeki güvenlik kontrolü; bulgu önem derecesi ve tekrar testi. |
| 17 | Sabit IP / endpoint / key | Kaynak/build/config taraması; ortam ayrımı, sırların loglanmaması; herkese açık yapılandırma ile sır ayrımı. |
| 18 | Hata yönetimi | Yükleme, gerçek boş, hata ve tekrar deneme ayrımı; başarı onayı gelmeden başarı gösterilmemesi. |
| 19 | Offline | Soğuk/sıcak başlangıç, bekleyen yazı/silme, yeniden bağlantı ve stale veri göstergesi. |
| 20 | Telefon / tablet | Dar/geniş, yatay, klavye ve büyük yazıda tüm ekran durumları. |
| 21 | Light / dark | Açık/koyu/mevsimsel tema, sistem çubukları ve ortak renk tokenları. |
| 22 | Erişilebilirlik | TalkBack adı/durum/sıra, dokunma hedefi, kontrast, büyük yazı ve animasyon kapalı kullanım. |
| 23 | UI / kozmetik / yazım | Font, ağırlık, hiyerarşi, boşluk, ikon, Türkçe yazım ve harf dönüşümleri; ortak bileşenlerle bütünlük. |
| 24 | Kullanılmayan / eski kod | Manifest/DI/reflection ve platform girişleri kontrolünden sonra kanıtlı temizlik. |
| 25 | RN / Native paralelliği | Hangi paket hangi kodu çalıştırıyor; yinelenen iş kuralı/model/CI ve farklı davranışlar haritası. |
| 26 | Teknik borç | Etki ve risk sırasıyla küçük değişiklikler; her borç için kapanış kanıtı. |
| 27 | Üretim öncesi | Servis kapasitesi, güvenlik, yasal sayfalar, mağaza, imza/R8/16 KB ve iç test kapıları. |
| 28 | İncelenmemiş kapsam | Her satır için kaynak inceleme/test/cihaz kanıtı ayrı tutulur; kanıtsız kapsam açık kalır. |
| 29 | Gerçek kişiselleştirme | Tercih → öneri/sıralama/sohbet etkisi; neden önerildi açıklaması ve kullanıcının kapatabilmesi. |
| 30 | Takip / Git | Bu kapsam ana listeye bağlı; doküman gönderimi testlerin tamamlandığı anlamına gelmez. |

## Kişiselleştirme kabul senaryoları

1. Sabit hava, konum, rota ve aynı soruda örneğin yürüyüş ile fotoğrafçılık tercihleri öneri içeriği veya sıralamasında anlamlı, açıklanabilir fark üretmeli; yalnızca rozet veya hitap değişmesi yeterli değil.
2. Tercih güncellendiğinde sonraki öneri güncel tercihi kullanmalı. Kaydetme hatası başarı gibi gösterilmemeli; cache ve sohbet bağlamı eski tercihi taşımamalı.
3. Kişiselleştirme kapalıyken veya tercihler boşken nötr öneri verilmeli; kullanıcıya ait olmayan bilgi çıkarılmamalı. A→B geçişinde A bağlamı B yanıtında bulunmamalı.
4. Yerel ve backend asistan yolları aynı tercih sözleşmesini kullanmalı. Bir yol kişiselleştirmeyi desteklemiyorsa destekliyormuş gibi sunulmamalı.
5. Risk uyarıları tercih nedeniyle gizlenmemeli; veri yokken hava/aktivite uygunluğu uydurulmamalı. Kullanıcı açıklaması sistem talimatlarını veya erişim sınırlarını değiştirememeli.
6. Önce sabit veri ve sahte servisle bağlam oluşturma/sıralama testleri; üretken yanıtlarda salt metin eşitliği yerine ilgi uygunluğu, olgusal dayanak, kullanıcı izolasyonu ve kapatma davranışı ölçütleri kullanılmalı. Gerçek servis testi ayrı aşamadır.

Ürün önerisi: önerilerde kısa “Neden önerildi?” açıklaması, tercihleri düzenlemeye doğrudan erişim ve kullanıcı isterse “ilgilenmiyorum” geri bildirimi değerlendirilecek. Bunlar uygulanmış özellik değildir.

## Premium görünümün somut ölçütleri

Ortak tipografi ve tasarım tokenları; tutarlı başlık/düğme dili, Türkçe İ/i/ı/I, noktalama ve birimler; kullanıcı isimlerini zorla dönüştürmeme. Yükleme/boş/hata/dolu ekranlar aynı görsel kaliteye sahip olmalı. Geçişler içerik sıçraması veya yanıltıcı geçici değer üretmemeli. Telefon/tablet, tema ve büyük yazı görsel kayıtlarıyla doğrulanmalı; yalnızca daha fazla animasyon eklemek kalite kanıtı sayılmaz.

## Çalışma ve kanıt sırası

Profil sayaçları → Firebase/senkronizasyon ve mimari veri akışı haritası → güvenlik/izolasyon → kişiselleştirme ve hava/rota kuralları → ekran durumları/akıcılık/kozmetik → ölçümlü performans ve teknik borç → üretim/mağaza doğrulaması. Kritik crash, veri kaybı veya güvenlik bulgusu öne alınır.

Her bulgu: dosya/akış, yeniden üretme, etki/öncelik, çözüm, uygun otomatik test ve gerekiyorsa cihazda önce/sonra kanıt. Kaynak incelemesi, birim testi, emülatör, cihaz ve canlı doğrulama ayrı durumlar olarak kaydedilir. Şu an yalnızca yerel test yetkisi geçerlidir; canlı dağıtım/ücretli plan/gerçek silme bu planla yapılmaz.
