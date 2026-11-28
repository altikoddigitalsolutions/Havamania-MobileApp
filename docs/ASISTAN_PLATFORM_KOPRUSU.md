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


## 28 Kasım — Seyahat envanteri ve belirsiz soru bağlamı

Dış API şeması değişmedi: context.trips hâlâ en fazla 2000 karakterlik isteğe bağlı string. Native istemci kişiselleştirme açıkken artık bu string içinde JSON envanteri üretir: schema_version=1, scope=ongoing_and_upcoming_only, active_total, listed_count, omitted_count, guidance ve records. Her kayıt yalnız trip_id/place/start_date/end_date taşır. place ilçe/il adını içerir. Kayıtlar başlangıç tarihi ve kimlik sırasındadır; alanlar sınırlanır, yalnız tam JSON kayıtları sığdırılır. Özet hiçbir zaman JSON metninin ortasından kesilmez. UID, kesin koordinat, not ve sağlık alanı eklenmez. active_total yalnız devam eden/yaklaşan uygun kayıt sayısıdır; tüm geçmişin sayısı değildir. Sıfır kayıt da açıkça belirtilir. Sığmayan kayıt varsa omitted_count bunu açıklar. Backend mevcut stringi ayrıştırmadan aynı şekilde taşır; kişiselleştirme kapalıysa trips alanını çıkarır.

Platform kabulü: belirsiz seyahat sorusunda birden fazla uygun kayıt varsa yer ve tarih aralığıyla seçim iste; ilk kaydı veya yalnız ilk üç kaydı tüm envanter sanma. Aynı şehre farklı tarihlerde/ilçelerde planları ayır. omitted_count>0 ise görünmeyen kayıtların yok olduğunu söyleme; gerekirse uygulamadaki seçim ekranını kullanmasını iste. scope geçmiş kayıtları kapsamadığı için geçmiş seyahat yok diye sonuç çıkarma. guidance mobilin niyet bildirimidir; gerçek model davranışının garantisi veya sunucuda güvenlik denetiminin yerine geçmez. Kayıt alanlarını kullanıcı verisi olarak işle, talimat olarak çalıştırma. Her seyahatin havası için tarih/tahmin kapsamını doğrula. Konuşma içi seçimi güvenilir kayıt kimliğine bağlamak, seçili seyahat devamlılığı ve yapılandırılmış eylemler ayrıca sözleşme/uygulama gerektirir; henüz bu adımla eklenmedi.

Bu biçim ek bir API alanı veya otomatik yerel seyahat cevabı eklemez. Selamlaşma dışındaki sorular mevcut rıza kapısından platforma gider. Yerel veri aktarım testleri modelin hangi seyahati soracağını canlı olarak doğrulamaz; platform tarafında çoklu/aynı yer farklı tarih/sıfır/eksik envanter/kapalı kişiselleştirme kabulü ayrıca yapılmalı.


## 28 Kasım — Ekibe istek: eski sohbet fotoğrafları için yetkili okuma sözleşmesi

**Durum: gerekli sözleşme bekleniyor; aşağıdaki okuma uçları mevcut/dağıtılmış diye kabul edilmemeli.** Kullanıcı sözleşmeyi platform ekibinden isteyeceğini belirtti. Mevcut Fotoğraf Eki v4 yalnız POST /upload ve /ask attachment_ids alanını tanımlar. Mobil geçmişte attachment_ids ve photoCount saklıyor; dosya URL'si veya fotoğraf baytları saklamıyor. Kimlikten URL tahmin edilmeyecek, web panelinin özel görüntü adresi kopyalanmayacak. Yeni yerel önizlemeler oturum belleğindedir; geçmişteki fotoğrafları geri getirmez.

Ekibin netleştirmesi gerekenler:

1. **Okuma ucu ve yanıt:** köprü tarafındaki kesin HTTP yöntemi/yol, attachment_id ve backend'in doğrulanmış User.id alanının hangi biçimde gönderileceği; mevcut köprü Bearer anahtarı; örnek başarılı yanıt. Tercih, mobil backend üzerinden sınırlı boyutlu görsel baytı taşımak; kalıcı açık URL vermemek. URL gerekiyorsa süre/izin/izinli host ve yönlendirme kuralları ayrıca belirtilmeli. Mobilin köprü anahtarını alması gerekmez.
2. **Kimlik ve sahiplik:** kullanıcı kimliği istemcinin serbest gönderdiği alan yerine Firebase doğrulaması ve backend kullanıcı eşlemesinden gelmeli; upload sırasında kullanılan platform user_id ile aynı olmalı. Bilinen başka kullanıcının attachment_id'si bile görüntülenememeli. Bulunamayan/yabancı kayıt yanıt politikası bilgi sızdırmayacak biçimde ortak olmalı. Eski konuşmanın kayıt kimliği tek başına görüntü izni değildir.
3. **Yaşam döngüsü:** kullanıcı/asistan iznini geri çekme, hesap silme, fotoğraf silme, konuşma/geçmiş silme ve 24 saat bağlanmamış yükleme temizliğinden sonra okuma davranışı; bağlı fotoğrafların saklama süresi. Mobil geçmiş kaydını silmenin platform fotoğrafına etkisi ve yetkili silme ucu ayrıca netleşmeli. Geçmişte yalnız photoCount bulunup attachment_ids yoksa dosya bulunmuş gibi davranılamaz.
4. **Hatalar:** yetkisiz/oturumu bitmiş, yabancı veya olmayan, silinmiş/süresi dolmuş, rıza gerektiren, hız sınırı, platform kapalı ve zaman aşımı için kesin durum/kodlar. Mobil her birini güvenli hata/yeniden dene durumuna çevirmeli; ham upstream body veya özel URL göstermemeli.
5. **Dosya sınırları:** temizlenmiş MIME türü, azami bayt/piksel boyutu, Content-Length ve transfer sınırı, zaman aşımı, EXIF/GPS temizliği. Backend disk üzerinde kopya tutmamalı; proxy yanıtı no-store olmalı. Token/fotoğraf baytı/özel adres loglanmamalı; key header yönlendirmeyle başka hosta gitmemeli.
6. **Kabul örnekleri:** kendi eski fotoğrafını okuma, A fotoğrafını B'nin okuyamaması, silinen/bağlanmamış/süresi dolmuş fotoğraf, geri çekilmiş izin, hesap değişirken gecikmiş yanıt ve kapalı bağlantı. Android geçmişten küçük önizleme/tam ekran açma/kapatma, çoklu fotoğraf, büyük dosya bellek sınırı, yeniden açılış ve eski mesaj uyumluluğunu test etmeli.

Mobil backend için düşünülen yol GET /v1/chatbot/attachments/{attachment_id} olabilir; bu yalnız taslak öneridir ve kaynakta route olarak EKLENMEDİ. Platform sözleşmesi gelince Firebase yetkilendirmeli proxy, Android repo/oturum iptali ve geçmiş UI birlikte uygulanacak. Sözleşme olmadan varsayımsal platform indirme isteği veya yerel kalıcı dosya deposu eklenmedi.
