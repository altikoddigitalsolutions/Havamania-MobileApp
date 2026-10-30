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
