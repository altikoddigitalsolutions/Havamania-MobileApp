# Havamania mevcut API dokümantasyonu

26 Kasım 2026. Depo koduna göre hazırlanmıştır. Adreslerin canlı erişimi, dağıtım sürümü, Firebase kuralları ve üretim ortam değişkenleri bu çalışma kapsamında doğrulanmadı. Anahtarlar ve parolalar bu dokümana dahil edilmedi.

## Dokümanlar

- [Uç noktalar ve tüm veri modelleri](API_ENDPOINT_REFERANSI.md): 26 yol altında 32 backend işlemi ve 38 şema.
- [OpenAPI JSON](API_OPENAPI.json): Swagger/Postman gibi araçlara aktarılabilir sözleşme. Sunucu adresini hedef ortama göre ayrıca seçin.

## Bağlantı mimarisi

| Bileşen | Adres / servis | Kullanım |
|---|---|---|
| Native Android backend | `https://api.havamania.com/` | Asistan, izinler, hesap silme ve cihaz bildirim kaydı |
| Backend platform köprüsü | `https://havamania.com/api/bridge` | Sunucudan sunucuya asistan bağlantısı; varsayılan yapılandırma |
| Open-Meteo | `https://api.open-meteo.com/v1/forecast` | Android hava ve güzergâh tahmini |
| Open-Meteo Geocoding | `https://geocoding-api.open-meteo.com/v1/search` | Şehir araması; Türkiye önceliği istemcide uygulanır |
| BigDataCloud | `https://api.bigdatacloud.net/data/reverse-geocode-client` | Android Geocoder sonrasında ters konum çözümleme alternatifi |
| OSRM | `https://router.project-osrm.org/route/v1/driving/{coordinates}` | Sürüş güzergâhı; koordinat sırası **boylam,enlem**, noktalar `;` ile ayrılır |
| OpenFreeMap | `https://tiles.openfreemap.org/styles/liberty` | Harita stili |
| MapLibre demo | `https://demotiles.maplibre.org/style.json` | Alternatif/demo harita stili |
| Firebase | Auth, Firestore, Storage, FCM SDK'ları | Giriş, senkronizasyon, avatar ve bildirimler; proje ayarlarına bağlı |

React Native tarafı ayrı yapılandırmadır: geliştirmede Android `http://192.168.1.50:8000`, iOS localhost; üretim adresi kodda `https://api.havamania.app` yer tutucusudur. Native Android ile aynı dağıtım olduğu varsayılmamalı. RN Axios zaman aşımı 15 saniyedir.

Eski `AltikodChatService` içinde `https://chatbot.altikodtech.com.tr/` tanımı bulunuyor; incelenen aktif asistan akışı BackendChatbotFactory kullanıyor. Seyahat rehberi mevcut kodda yerel seçilmiş içerik ve kurallı öneriler kullanıyor; Google Places veya canlı restoran API'si olarak değerlendirilmemeli.

## Backend erişimi ve kimlik doğrulama

API öneki varsayılan `/v1`. Yerel Docker geliştirme yapılandırması backend'i host 8000 portuna açar; PostgreSQL host 5433/container 5432 kullanır. Bu, üretimin bu bilgisayarda çalıştığının kanıtı değildir.

Korumalı isteklerde `Authorization: Bearer <erişim_belirteci>` kullanılır. Backend kendi erişim JWT'sini ve doğrulanmış Firebase ID token'ını destekler; bunların kullanıcı eşleştirme ve hesap durumu kontrolleri vardır. Aynı e-postalı bağımsız hesapta otomatik birleştirme yerine hesap bağlama hatası oluşabilir. Native giriş Firebase SDK akışıdır; backend giriş uçları ayrı sözleşmedir.

`POST /v1/auth/login` JSON e-posta/parola alır; form isteği varsaymayın. Refresh uçları `refresh_token` taşır. Parola gibi verileri loglamayın. Alanların kesin sınırları uç nokta referansındadır.

`X-Request-ID` izleme kimliğidir; geçerli UUID değilse sunucu üretir ve yanıtta döndürür. Asistan gövdesindeki `request_id` ise tekrar koruması içindir; birbirlerinin yerine geçmezler.

## Backend kapsamı

| Alan | Uçlar |
|---|---|
| Sağlık | `GET /health` |
| Kimlik | `POST /v1/auth/signup`, `/login`, `/refresh`, `/logout` |
| Profil | `GET/PATCH /v1/profile`; `GET/POST /v1/profile/locations`; `PATCH/DELETE /v1/profile/locations/{location_id}` |
| Profil tercihleri | `GET/PATCH /v1/profile/notifications`; `POST /v1/profile/avatar` |
| Hava | `GET /v1/weather/current`, `/hourly`, `/daily`, `/map-layers` |
| Uyarılar | `GET /v1/alerts`, `GET /v1/alerts/{alert_id}` |
| Asistan | `POST /v1/chatbot/ask`, `/ask/premium`; `GET /v1/chatbot/usage`; `GET/POST /v1/chatbot/consent` |
| Hesap silme | `GET /v1/account/deletion/availability`; `POST /v1/account/deletion` |
| Abonelik | `GET /v1/subscription/status`; `POST /v1/subscription/validate-receipt`, `/webhook/store` |
| Bildirim cihazı | `POST/DELETE /v1/devices/push-token` |

OpenAPI dışında ayrıca `GET /v1/metrics` bulunur; kullanıcı JWT'si yerine ayrı `METRICS_SECRET` ile erişilir. Otomatik `/docs`, `/redoc`, `/openapi.json` ve `/static` yolları uygulamada tanımlıdır; canlı proxy erişimi ayrıca kontrol edilmelidir. `/health` yalnızca uygulama yanıtını gösterir, veritabanı veya dış servis hazırlık kontrolü değildir.

## Asistan isteği ve tekrar deneme

Soru sınırı 4000 karakterdir. Her yeni soruya UUID `request_id` üretin; aynı sorunun yeniden denemesinde kimliği ve içeriği koruyun. Konuşmayı sürdürmek için dönen `conversation_id` kullanılmalıdır. Modlar `genel`, `agro`, `fly`; tonlar `samimi`, `resmi`, `dengeli`, `kisa`, `uzman`.

Aşağıdaki örnekteki izin sürümü temsilidir; gerçek sürüm izin hizmetinden alınmalıdır:

```json
{"question":"Yarın Bursa'da hava nasıl?","request_id":"64358c06-83d7-4b4f-a06b-ab780588e83d","mode":"genel","tone":"dengeli","consent_version":1,"personalization_enabled":false,"health_consent":false,"context":{"city":"Bursa","weather_data":{"observed_at":"2026-11-26T12:00:00Z","timezone":"Europe/Istanbul","location":{"name":"Bursa"},"current":{"temp_c":18.5,"condition":"Bulutlu","humidity_pct":50.0,"wind_kmh":12.0},"forecast":[{"date":"2026-11-27","min_c":10.0,"max_c":20.0,"precip_prob_pct":30.0}],"stale":false,"source":"Open-Meteo"}}}
```

Başarılı yanıtta `answer`, isteğe bağlı `conversation_id`, `message_id`, `disclaimer` ve kullanım alanları `used_messages_today`, `remaining_messages_today`, `is_premium` bulunur. Hatalar cevap balonu olarak kaydedilmemeli; hata durumu ve uygun yeniden deneme sunulmalıdır.

| Durum | İstemci davranışı |
|---|---|
| 409 `request_in_progress` | Yeni kimlikle ikinci soru oluşturmayın; aynı isteği kontrollü yeniden deneyin |
| 409 konuşma/hesap silme çatışması | Kodu ayırt ederek konuşma veya hesap durumunu çözün |
| 422 tekrar kimliği/içerik uyuşmazlığı | Aynı kimlikle farklı içerik göndermeyin |
| 428 `consent_required` | Güncel izin metnini alıp kullanıcıya gösterin |
| 429 kota veya hız sınırı | Hata kodunu ayırın; varsa bekleme bilgisini uygulayın |
| 502 geçersiz platform yanıtı | Başarı gibi kaydetmeyin |
| 503 hizmet/yapılandırma/izin eşitleme sorunu | Kullanıcıya geçici hizmet hatası gösterin; köprü anahtarı hatasını kullanıcı giriş hatası saymayın |
| 504 zaman aşımı | Aynı kimlikle kullanıcı denetiminde yeniden deneyin |

Hata gövdeleri tüm uçlarda tek biçimde değildir: `detail`, doğrulama listesi veya `error`/`message` kullanılabilir. OpenAPI'deki yanıt listesi çalışma zamanındaki bütün özel hata kodlarını kapsamaz.

### Hava bağlamı ve kişiselleştirme

`context.weather_data`: offset içeren `observed_at`, geçerli IANA `timezone`, isteğe bağlı `location.name`, `current`, `forecast`, `stale`, `source`. Tahmin en fazla 10 gün; tarihler benzersiz, sıralı ve ardışık olmalı; minimum sıcaklık maksimumu geçmemeli. Serileştirilmiş hava nesnesi en fazla 4096 bayt. Kesin alan sınırları `AssistantWeatherData` ve ilişkili şemalardadır. Koordinat alanı yoktur; yer adı kullanılır.

Mobil `personalization_enabled`, köprüde `personalization` alanına dönüştürülür. False olduğunda profil/seyahat/sağlık bağlamı filtrelenir. Sağlık verisi ayrıca ayrı izin gerektirir. Android'in analizdeki 10 gün/48 saat iş kuralları, sağlayıcı isteklerinin `forecast_days` değerleriyle aynı kavram değildir.

### Köprü ve kota

Backend `CHATBOT_BASE_URL` altında `/ask`, platform izin kaynağı seçildiğinde `/consent`, hesap temizliğinde `/delete-user` kullanır. `CHATBOT_API_KEY` yalnızca sunucuda tutulur; mobil uygulamaya konmaz. Köprü zaman aşımı varsayılan 50 saniye; bağlantı hatasında sınırlı tek tekrar vardır, zaman aşımında otomatik tekrar yoktur. Native backend istemcisi connect/read/call süreleri 10/65/75 saniyedir.

Gün hesabı Europe/Istanbul kullanır. Varsayılan ücretsiz/premium günlük limit 10/100; ortam ayarları değiştirebilir. Rezervasyon, başarıda kesinleştirme ve başarısızlıkta iade uygulanır; kullanım sayısı bekleyen rezervasyonları geçici olarak içerebilir.

## Diğer önemli sözleşmeler

- İzin GET/POST: asistan ve sağlık izinleri ayrı; güncel sürüm/hash ile onay verilir. Geri çekme hatası başarı kabul edilmemeli.
- Hesap silme: availability ucu kullanılabilirliği bildirir. Yeni talep yakın zamanda doğrulanmış Firebase token'ı ve 64 karakter küçük harf hex `continuation_key` kullanır. Var olan iş aynı gizli anahtarla devam ettirilebilir. 200 tamamlandı, 202 bekliyor. Anahtarı loglamayın; bu işlem gerçek verileri siler.
- Backend avatar: multipart `file`, JPEG/PNG/WebP, en fazla 5 MiB; içerik imzası kontrol edilir. Backend `/static/avatars/…` çıktısı, Native Firebase Storage akışıyla aynı değildir.
- Hava proxy: current/hourly/daily lat/lon alır; hourly 1–72 saat, daily 1–14 gün. Native Android ana hava akışı doğrudan Open-Meteo kullanır.
- Bildirim kaydı: POST 201, DELETE 204. DELETE JSON gövdesi içerir; token sahipliği/protokol alanlarının kesin tanımı şemalardadır.
- Native Open-Meteo istemcisi connect/read/write/call 15/20/20/30 saniye; uygun GET ağ ve 5xx hatalarında en fazla toplam üç deneme uygular.

## Yayın öncesi koddan görülen açık noktalar

1. **FCM durum kodu uyumu — 26 Kasım düzeltmesi:** backend kayıt POST'u 201 döndürür. Native gönderim yolları artık 200/201 yanıtında tam sahiplik makbuzu eşleşirse başarı kabul eder; 202/boş yanıt başarı sayılmaz. Canlı FCM teslimatı ayrıca doğrulanmalıdır. [Düzeltme raporu](26_KASIM_FCM_KAYIT_YANITI.md).
2. **Hava hız sınırı — 26 Kasım düzeltmesi:** İstemci `max_requests`/`window_seconds` ile politikayı değiştiremez; alanlar OpenAPI sorgu sözleşmesinden kaldırıldı. Hava uçları istemci IP'si başına süreç içinde ortak 60 istek/60 saniye uygular; 429 yanıtında `Retry-After` döner. Çok süreçli dağıtım ve güvenilir proxy IP yapılandırması ayrıca doğrulanmalıdır.
3. **Abonelik:** makbuz doğrulama ve mağaza webhook'u production ortamında 403 ile kapalı; uçların varlığı satın alma doğrulamasının hazır olduğu anlamına gelmez.
4. **Harita katmanı:** map-layers mevcut uygulaması gerçek hava katmanı yerine özet/yer tutucu veri döndürüyor.
5. **Adres bütünlüğü:** RN `.app`, Native `.com` ve eski Altikod tanımları ayrıştırılıp üretim yapılandırması netleştirilmeli.
6. Canlı DNS/TLS, Firebase güvenlik kuralları, hizmet kotaları, platform anahtarı, hesap silme ve üretim senaryoları ayrıca doğrulanmalı.

## Kaynaklar ve doğrulama kapsamı

Backend: `app/main.py`, `app/api`, `app/schemas`, `app/core/config.py`, kimlik bağımlılıkları ve köprü istemcisi. Native: `NetworkModule`, `WeatherApiService`, `BackendChatbotFactory`, `BackendFcmService`, `ReverseGeocoder`, OSRM ve harita sağlayıcıları. RN: `mobile/src/services/apiClient.ts`, `openMeteoApi.ts`.

OpenAPI çevrimdışı uygulama tanımından üretildi; canlı servise istek gönderilmedi. Bu belgeler üretim ortamındaki dağıtımın birebir aynı sürüm olduğunu kanıtlamaz.
