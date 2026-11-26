# Asistan platform köprüsü — 30 Ekim 2026

Yerel kod ve sahte servislerle doğrulanan sözleşme. Canlı platform davranışı, gerçek anahtar ve gerçek hesapla doğrulama yapılmadı.

## Sunucu yapılandırması

- CHATBOT_BASE_URL: platformun HTTPS `/api/bridge` temel adresi. Üretimde HTTP, loopback, URL içi kimlik bilgisi, query ve fragment reddedilir. Yönlendirmeler takip edilmez.
- CHATBOT_API_KEY: yalnızca backend secret store/ortam değişkeni. Üretimde eksikse başlangıç reddedilir. SecretStr, repr ve validation hata girdisi gizleme kullanılır; Android’e eklenmez. Anahtar yalnız Authorization Bearer başlığında gönderilir.
- CHATBOT_TIMEOUT_SECONDS: varsayılan okuma zaman aşımı 50 saniye; bağlantı 3 saniye. Yalnız ConnectError bir kez tekrarlanır; okuma/5xx/409 otomatik tekrarlanmaz. Bunlar uçtan uca kesin süre garantisi değildir. Android HTTP/iş süresi 75 saniye ile sınırlı.
- Onaylanmış rıza metni/sürümü ve önceki 0014/0015 migration’ları ayrıca gerekir. Anahtar olması hukuki veya mağaza hazırlığının tamamlandığı anlamına gelmez.

## Mobil → backend → platform

Mobil soru metni değiştirilmez. conversation_id UUID, mode (Native şu an genel), ton, rıza bilgileri ve context ayrı alanlardır. X-App-Version en fazla 40 karakter; client metadata’sına eklenir. user_id istemciden alınmaz; doğrulanmış backend kullanıcısının kimliğidir.

Tonlar: SAMIMI→samimi, RESMI→resmi, DENGELI→dengeli, KISA_NET→kisa, DETAYLI_UZMAN→uzman. Ton talimatı soru metnine yapıştırılmaz.

Context yalnız city ≤100, weather ≤2000, trips ≤2000 kabul eder; istemci context.profile gönderemez. Backend profile alanlarını izin listesinden ve kullanıcı kaydından kurar, her alanı 500 karakterle sınırlar. Son context UTF-8 olarak en fazla 8 KB; tüm köprü gövdesi 32 KB sınırı için kontrol edilir. Soru 4000 karaktere kadar; backend ve Android üst sınırı eşlendi.

Kişiselleştirme kapalıysa seyahat/profil/sağlık aktarımı çıkarılır; konum ve doğrulanabilir hava bağlamı işlevsel veri olarak kalır. Native ilgi alanlarını UID’ye bağlı depodan okur. selected_interests: null/eski istemcide eksik alan backend profilindeki tercihe döner; [] açık temizlemedir ve eski ilgi alanını geri getirmez. Liste en fazla 20×50 karakter; platform interest alanı 500 karakterle sınırlanır. Diğer profil tercihlerinin Firebase–SQL senkronizasyonu ayrıca doğrulanmalı.

Android bağlamı mevcut snapshot’tan hazırlanır; yeni hava isteği açmaz. Eski, gelecek zaman damgalı veya farklı konuma ait snapshot güncelmiş gibi gönderilmez. Edinme zamanı ve konum saat dilimi bulunur; bu zaman meteoroloji sağlayıcısının ölçüm/issue zamanı değildir. Günlük tahminlerde tarih ve min/max doğrulanır; en fazla 10 tarih paylaşılır ve paylaşılmayan tarih için tahmin üretilmemesi belirtilir. Kesin koordinat, seyahat notu ve serbest profil hakkında metni bu mobil bağlama eklenmez.

## Yanıt ve hata

Platform cevabı answer + geçerli conversation_id/message_id gerektirir. Beklenen sohbetten farklı kimlik veya bozuk/boş yanıt 502 olur. Agro/Fly API modu kullanılırsa disclaimer eksik cevap reddedilir; Native mod seçimi henüz yoktur. Cevap message_id ve disclaimer Android sohbet geçmişinde saklanır; uyarı bot mesajı altında gösterilir.

Yeni sohbet kaydında aktif konuşma UUID’si korunur. Kaydetme denetleyicisi önceki iptal edilmiş sohbetin kimliğini yeni açık kimliğe tercih etmez. Bu sürümden önce yalnız cihaz/Firestore’da duran eski konuşmaların platforma otomatik aktarımı veya eski yanlış kimliklerin eşlenmesi yapılmadı.

429 günlük/geçici sınır ve geçerli sayısal Retry-After korunur; başarısız köprü isteğinin yerel hakkı iade edilir. 409 conversation_conflict/mode_disabled otomatik yeniden gönderilmez. 401/403 anahtar hatası kullanıcıya 503 olur, güvenli olay kaydı yazılır. Upstream mesajı/ham gövdesi gösterilmez. Platform 428 dönerse henüz canlı metin adaptörü olmadığı için consent_sync_required/503 döner; kullanıcı eski yerel metni tekrar tekrar onaylamaya yönlendirilmez.

## Açık yayın bağımlılıkları

1. Platform GET /consent adaptörü ve güncel metinlerin sunucu rıza kaydıyla eşlenmesi (şu an yapılandırma metni kullanılıyor).
2. Hesap silme işine platform delete-user adımı, kalıcı yeniden deneme ve uçtan uca silinebilir test hesabı.
3. Gerçek anahtar/rotasyon, DNS/TLS, ters vekil ve toplam zaman aşımı, kapasite, canlı Firebase/backend/platform doğrulaması.
4. Platform idempotency/sonuç sorgulama sözleşmesi; kayıtlı başarısız request_id ile yeniden işlem stratejisi ve bilinmeyen sonuçların geri alınması. Mevcut koruma backend yerelidir; haricî serviste exactly-once garantisi değildir.
5. Hukuki metin/aktarım dayanağı, saklama ve mağaza beyanları; bütün UI/TalkBack/telefon/yatay matrisi.

Son kanıt: backend 112 geçti, 6 PostgreSQL testi bu koşuda atlandı; Ruff temiz. Android 203 JVM testi ve debug derleme başarılı. Bu tur cihaz, genel lint ve PostgreSQL yeniden çalıştırılmadı; önceki rıza cihaz testleri bu yeni canlı köprü akışını doğrulamaz.

30 Ekim: ASSISTANT_CONSENT_SOURCE=platform ile GET /consent kaynağı desteklenir. 15 saniye süreç içi önbellek; onay yazarken zorunlu yeniden yükleme; platform 428 yanıtında invalidasyon; hatada eski/local metne geri dönüş yok. Varsayılan configured korunur. Canlı metin ve bağlantı doğrulaması tamamlanmadan üretimde hazır kabul edilmez. Yerel hedefli paket: 52 test geçti.

Hesap silme: POST /delete-user yalnızca server UUID ile çağrılır. Makbuz doğrulanmadan SQL/Firebase kimliği silinmez; hata durumunda mevcut kalıcı pending iş tekrar denenir. HTTP 200 negatif olmayan tamsayı deleted_conversations/deleted_photos zorunludur. Canlı idempotency ve eşzamanlı ask/silme doğrulanmadı; endpoint değişikliği bekleyen işler bitene kadar yapılmamalı.

0016 sonrası: silme işi platform_base_url ve backend_user_id tutar. Hedef değişikliğinde işlem durur; eski işlerin NULL platform adresi doğrulanmadan otomatik tamamlanmaz. Ask ve prepare_deletion PostgreSQL kullanıcı satırını kilitleyerek gönderim/silme kabulünü sıralar; kabulden sonra account_deletion_pending (409) döner, model çağrılmaz. İşlemdeki eski ask geri çağrılamaz; silme onun bitmesini bekler. SQLite testleri gerçek satır kilidi kanıtı değildir; ayrı PostgreSQL 16 testi geçti. Android özel hata metni henüz açık.


## 26 Kasım 2026 — Fotoğraf eki (platform sözleşmesi v4)

Android sistem Photo Picker ile en fazla dört fotoğraf seçer; depolama izni istemez. Seçilen dosya IO iş parçacığında sınırlandırılmış olarak okunur (kaynak en fazla 20 MB, çözünürlük en fazla 50 MP). Desteklenen sistem decoder'ıyla JPEG/PNG/WebP ve GIF ilk kare JPEG'e çevrilir; HEIC/HEIF cihaz decoder desteğine bağlıdır, destek yoksa güvenli seçim hatası gösterilir. Uzun kenar en fazla 2560; EXIF yönü uygulanır, yeni JPEG kaynak EXIF/GPS içermez. Çıktı en fazla 8 MB. Önizleme Coil disk/bellek önbelleğine yazılmaz; uygulama yalnız oturum belleğinde fotoğraf baytlarını tutar.

Mobil `POST /v1/chatbot/attachments` çağrısında Bearer oturum token'ı ve yalnız multipart `file` gönderir. Backend kendi doğrulanmış kullanıcı kimliğini ve güncel kabul edilmiş asistan sürümünü ekleyerek `/api/bridge/upload` ucuna aktarır. İstemci user_id/consent_version belirleyemez. 201 yanıtı UUID attachment_id, desteklenen media_type ve size içerir. Yükleme mesaj kotasını harcamaz. Fotoğraf-only soruda metin otomatik “Bu fotoğrafı yorumlar mısın?” olur. Selamlaşma + fotoğraf da izin kontrollü uzak asistan akışına gider.

Backend UploadFile kullanmaz; böylece büyük multipart dosyalar geçici diske spool edilmez. Gövde 8 MB + 64 KB, dosya 8 MB ve gövde okuma 30 saniye ile sınırlanır; tek file alanı ve JPEG/PNG/WebP/GIF imzası kontrol edilir. Tam dosya çözümleme, 50 MP ve sahiplik/son kullanma kabulü platformda yapılır. Aynı süreçte en fazla dört aktif yükleme, kullanıcı başına on yükleme/dakika sınırı vardır. Çok worker/sunucu için ortak gateway/Redis sınırı ayrıca gerekir. Fotoğraf baytları backend veritabanı/disk/loglarına kaydedilmez. Platform yüklemesinde 30 saniye HTTP timeout, bağlantıda 5 saniye ve redirect kapalıdır. Belirsiz yükleme sonucunda otomatik tekrar yok; Android HTTP otomatik bağlantı tekrarı da kapalıdır.

`/ask` isteğinin attachment_ids alanı isteğe bağlı UUID listesidir; dört farklı kimlik kabul edilir, yinelenen/büyük harfli kimlikler normalize edilir, sıralama önemsizdir. Yerel tekrar hash'i fotoğraf kümesini içerir, fotoğrafsız eski hash'ler korunur. Aynı request_id ve farklı fotoğraf kümesi 422 verir. Android kısmi yüklemede her başarılı kimliği hemen kaydeder; tekrar yalnız eksik fotoğrafları yükler. /ask tekrarında tamamlanmış yükleme yinelenmez. Sohbet geçmişinde yalnız kimlik/sayı saklanır, fotoğraf baytı veya content URI saklanmaz. Süreç kaybından sonra eksik dosyalarla sessizce eksik soru gönderilmez; yeniden fotoğraf seçilmesi istenir.

Hesap değişimi, yeni sohbet, geçmişe geçiş ve rızayı geri çekme bekleyen fotoğraf belleğini temizler. Seçici açıkken hesap değişirse eski seçici sonucu yok sayılır. photos_disabled fotoğraf ekleme düğmesini oturum için kapatır; user_deleted ve kimlik çatışmaları yeniden denenmez. 429 Retry-After beklemesi korunur. Platform 428 cevabındaki mevcut consent_sync_required eşlemesi ve rıza yenileme politikası korunmuştur. Platform /delete-user zaten bağlı ve sahipsiz fotoğrafları da temizlediği için ikinci bir silme ucu eklenmedi.

Canlı kabul açık: platform /upload dağıtımı, hedef backend deployment, ters proxy multipart boyut/zaman sınırları, gerçek rıza/hesapla fotoğrafın panelde ve modelde görünmesi, A–B sahiplik reddi ve hesap silmede fotoğraf temizliği. Bu adımda canlı servise fotoğraf gönderilmedi. Fiziksel Photo Picker seçimi, HEIC cihaz matrisi, süreç ölümü ve tam gerçek asistan ekranı uçtan uca kabulü ayrıca izlenmelidir. Yeni AAB üretilmedi.

Yerel doğrulama: backend ilk tam koşu 278 geçti, ayrı PostgreSQL ortamı isteyen 13 test atlandı. Son fotoğraf paketi 16/16, ilgili asistan/izin/köprü regresyonu önceki alt koşuda 60/60; Ruff başarılı. Testlerde gerçek HTTP adaptörleri engellidir. İlk Android koşusu 948 JVM + dört SM-T500 fotoğraf testi hatasız geçti. Son kaynak/400 hata eşlemesi için JVM derlemesi ve 948 test başarılı; son kontrast düzeltmesi için dört SM-T500 testi tekrar hatasız/atlamasız geçti. İzole sonbahar yüzde200 yazı ve koyu tema görselleri gözle incelendi; mesaj etiketi tema textPrimary kullanıyor. Görseller .artifacts/assistant-photo-20261126 altında yereldir, depoya eklenmedi. USB ekran ayarı eski 0 değerine döndürüldü. Gerçek Photo Picker ve tam asistan akışı, platform dağıtımı, gizlilik/mağaza fotoğraf beyanları ve canlı kabul hâlâ açıktır.


26 Kasım multipart dayanıklılık düzeltmesi: email MIME ağacı ayrıştırması kaldırıldı. İç içe message/rfc822 isteklerinin RecursionError oluşturduğu yerel regresyonla gösterildi. python-multipart (test edilen 0.0.22 alt sınırı iki bağımlılık dosyasında aynı) tek parça callback ayrıştırıcısı, başlıklarda 8 KB/sekiz adet sınırı, ikinci parça ve mükerrer başlık reddi, kesin kapanış kontrolü kullanılır. Ham fotoğraf baytları dönüştürülmez. Ayrıştırma worker thread üzerinde yapılır; disk spool yoktur. Son tam backend paketi 290 geçti/13 PostgreSQL testi atlandı; Ruff başarılı. Dağıtım henüz yapılmadı.


26 Kasım hesap/izin yarışı: fotoğraf okunurken silinmiş ORM kullanıcı nesnesinin yenilenmesi ObjectDeletedError üretiyordu. Kullanıcı kimliği beklemeden önce sabitlenir; okuma sonrası User varlığı ve silme durumu bu sabit kimlikle with_for_update sorgusunda kontrol edilir, ardından taze rıza durumu okunur. Silinmiş hesap 409, okuma sırasında geri çekilmiş rıza 428 ile upstream öncesinde durur. İzin reddinden sonra yükleme kapasitesinin tamamı serbesttir. İlgili 38 test ve tam backend 292/13 atlama; Ruff başarılı. PostgreSQL gerçek iki-oturum ve canlı kabul bu SQLite regresyonlarıyla kapanmadı.


26 Kasım gerçek seçici kabulü: AiChatScreen ve izole debug ekranı rememberAssistantPhotoPicker kullanır. Açılış istisnaları güvenli hataya çevrilir, açık seçiciye ikinci launch engellenir ve başlangıç token'i korunur. Son SM-T500 koşusunda gerçek seçim/iptal, hazırlama/önizleme/kaldırma ve güvenli açılış testleri 5/5 geçti. Yalnız fixture fotoğrafı okunup silindi; gerçek hesap/platform kullanılmadı. Teslim edilmiş 1.3/v6 AAB bu son Android değişikliğini içermez. Çoklu/HEIC/cloud/recreate/tam asistan kabulü açık; ayrıntı güncel yayın sırasının üst kaydında.

## Fotoğraf yüklemede güvenli dağıtım tanısı — 27 Kasım

Mobil /v1/chatbot/attachments için platform /upload yanıtı 401/403 ise backend 503 bridge_auth_failed; 404/405/501 ise 503 upload_endpoint_unavailable; 503 not_configured ise aynı kod döner. Bu kodlar backend–platform bağlantı/yayın tanısıdır; Firebase oturumunu 401 gibi yeniletmez. Ham platform mesajı dışarı aktarılmaz. Android kodları ayrı güvenli metinlerle gösterir. Bu sözleşme yerelde doğrulandı; canlı deploy/kabul ayrıca açık.

## Canlı dağıtım kontrolü — 27 Kasım

api.havamania.com health 200 ve consent 401 iken anonim POST /v1/chatbot/attachments standart 404 verdi. Aynı istek güncel yerel kaynakta 401 verir. Dağıtım imajı/kod sürümü ve reverse proxy yönlendirmesi kontrol edilmeden fotoğraf akışı canlı kabul edilmiş sayılmaz. Yalnız yerel Docker Desktop'ı başlatmak uzak API dağıtımını güncellemez.

27 Kasım dağıtım tanısı: python -m app.photo_deployment_check --base-url https://api.havamania.com dosyasız/kimliksiz yönlendirme kontrolü sağlar; canlıda attachments 404 nedeniyle exit 1 doğrulandı. Konteyner içi karşılaştırma ve son kabul için FOTOGRAF_CANLI_DAGITIM_KONTROLU.md belgesine bakın. Bu modül mevcut canlı sunucuya kurulmadı ve fotoğrafın modele ulaştığını kanıtlamaz.
