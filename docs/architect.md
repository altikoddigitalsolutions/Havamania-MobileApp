# Havamania Teknik Mimari (Architect)

## 1) Mimari Hedefler
- Hizli ve guvenilir hava verisi sunumu.
- Veri saglayici degisikliklerine dayanikli entegrasyon.
- Freemium modelini backend tarafinda kesin olarak uygulatma.
- Mobilde sade ama guclu UX.

## 2) Yuksek Seviye Mimari

```text
[React Native App]
   |  HTTPS (JWT)
   v
[FastAPI Backend]
   |-- Auth Service
   |-- Weather Service (Provider Adapter)
   |-- Alerts Service
   |-- Profile Service
   |-- Subscription Service
   |-- Chatbot Bridge Service
   |
   +--> [PostgreSQL]
   +--> [Redis Cache]
   +--> [Push Provider (FCM/APNs)]
   +--> [Weather Providers: Open-Meteo -> Paid APIs]
   +--> [Existing Chatbot Backend]
```

## 3) Bilesenler

### 3.1 React Native Uygulamasi
- Durum yonetimi: `@tanstack/react-query` + hafif global store (Zustand/Redux Toolkit).
- Navigasyon: React Navigation (Bottom Tabs + Stack).
- Ekranlar: Home, Map, Alerts, Profile/Settings, Auth.
- Ozellik kapisi: subscription claim'lerine gore UI gating.

### 3.2 FastAPI Backend
- Katmanlar:
- `api`: router/endpoint katmani
- `application`: use-case katmani
- `domain`: is kurallari
- `infrastructure`: DB, dis API, cache, queue, push

### 3.3 PostgreSQL
- Kullanici, tercih, konum, abonelik, chatbot log/meta, uyari kayitlari.
- Alembic ile migration yonetimi.

### 3.4 Redis (onerilen)
- Hava verisi cache (kisa TTL).
- Rate limiting (chatbot ve hava endpointleri).
- Idempotency anahtarlari.

## 4) Veri Saglayici Stratejisi (Open-Meteo -> Ucretli API)
- `WeatherProvider` arayuzu tanimlanir:
- `get_current(lat, lon)`
- `get_hourly(lat, lon, hours)`
- `get_daily(lat, lon, days)`
- Ilk implementasyon: `OpenMeteoProvider`
- Sonraki implementasyonlar: `PaidProviderX`, `PaidProviderY`
- Runtime secim:
- feature flag ile global gecis
- premium kullanicida farkli provider secimi (opsiyonel)

## 5) Chatbot Entegrasyonu
- Mevcut chatbot servisine `Chatbot Bridge` katmani ile baglanilir.
- Bridge gorevleri:
- kimlik dogrulama token gecisi
- prompt/cevap format donusumu
- timeout/retry
- kota kontrolu (free vs premium)

## 6) Abonelik ve Freemium Mimari
- Mobil satin alim: App Store / Google Play.
- Oneri: RevenueCat benzeri katman veya dogrudan store receipt validation.
- Backend kaynakli dogrulama zorunlu:
- `subscription_status` server'da tutulur.
- Mobil istemci sadece gorunum amacli "aktif/pasif" bilgisini kullanir.
- Yetkilendirme:
- JWT claim + backend kontrolu bir arada.
- Kritik endpointlerde (gelismis harita, premium chatbot) server-side kontrol zorunlu.

## 7) Veritabani Taslagi

### 7.1 Tablolar
- `users`
- `id (uuid, pk)`
- `email (unique)`
- `password_hash`
- `full_name`
- `created_at`, `updated_at`

- `profiles`
- `user_id (pk/fk users.id)`
- `primary_location_id (fk locations.id, nullable)`
- `temperature_unit` (C/F)
- `wind_unit` (kmh/mph)
- `theme` (light/dark/system)

- `locations`
- `id (uuid, pk)`
- `user_id (fk users.id)`
- `label`
- `lat`, `lon`
- `is_primary`
- `is_tracking_enabled`

- `notification_preferences`
- `user_id (pk/fk users.id)`
- `severe_alert_enabled`
- `daily_summary_enabled`
- `rain_alert_enabled`
- `updated_at`

- `subscriptions`
- `id (uuid, pk)`
- `user_id (fk users.id, unique)`
- `plan_code`
- `status` (active, grace, canceled, expired)
- `expires_at`
- `store` (ios, android)
- `original_transaction_id`
- `updated_at`

- `chatbot_usage_daily`
- `id (uuid, pk)`
- `user_id (fk users.id)`
- `date`
- `message_count`
- `token_count`
- `unique(user_id, date)`

- `alerts`
- `id (uuid, pk)`
- `location_id (fk locations.id)`
- `severity` (advisory, active, critical)
- `title`
- `description`
- `starts_at`, `ends_at`
- `created_at`

- `weather_snapshots`
- `id (uuid, pk)`
- `location_key` (lat/lon hash)
- `provider`
- `payload_jsonb`
- `fetched_at`

## 8) API Taslagi (v1)

### 8.1 Auth
- `POST /v1/auth/signup`
- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `POST /v1/auth/logout`

### 8.2 Weather
- `GET /v1/weather/current?lat=&lon=`
- `GET /v1/weather/hourly?lat=&lon=&hours=24`
- `GET /v1/weather/daily?lat=&lon=&days=7`
- `GET /v1/weather/map-layers?lat=&lon=&layer=temperature`

### 8.3 Profile & Preferences
- `GET /v1/profile`
- `PATCH /v1/profile`
- `GET /v1/profile/locations`
- `POST /v1/profile/locations`
- `PATCH /v1/profile/locations/{location_id}`
- `DELETE /v1/profile/locations/{location_id}`
- `GET /v1/profile/notifications`
- `PATCH /v1/profile/notifications`

### 8.4 Alerts
- `GET /v1/alerts?location_id=&severity=`
- `GET /v1/alerts/{alert_id}`

### 8.5 Chatbot
- `POST /v1/chatbot/ask`
- `GET /v1/chatbot/usage`

### 8.6 Subscription
- `GET /v1/subscription/status`
- `POST /v1/subscription/validate-receipt`
- `POST /v1/subscription/webhook/store`

## 9) Guvenlik
- JWT access + refresh token modeli.
- Sifreler `argon2` veya `bcrypt` ile hashlenmeli.
- Rate limit:
- auth endpointleri
- chatbot endpointleri
- weather endpointleri
- Secrets:
- `.env` + secret manager (production)
- API key rotating stratejisi.

## 10) Performans ve Olceklenebilirlik
- Hava endpointlerinde agresif cache:
- current: 1-3 dk TTL
- hourly/daily: 10-30 dk TTL
- DB indexleri:
- `alerts(location_id, severity, starts_at)`
- `locations(user_id, is_primary)`
- `subscriptions(user_id, status)`
- Uygulama buyudukce:
- read replica
- queue tabanli background jobs (Celery/RQ/Arq)

## 11) Gozlemlenebilirlik
- Structured logging (request_id ile).
- Metrics:
- endpoint latency (p50, p95, p99)
- provider error rate
- chatbot timeout rate
- subscription validation failure rate
- Error tracking: Sentry.

## 12) Dagitim
- Backend: Docker + CI/CD (staging/prod).
- Mobil:
- iOS TestFlight
- Android Internal Testing
- Ortamlar:
- `dev`, `staging`, `prod`
- Ayrik API key ve veritabani.

## 13) Onerilen Monorepo Yapisi

```text
havamania/
  backend/
    app/
      api/
      application/
      domain/
      infrastructure/
    alembic/
    tests/
  mobile/
    src/
      screens/
      components/
      services/
      store/
      hooks/
    tests/
  docs/
    prd.md
    architect.md
    task.md
```

## 14) Kritik Teknik Kararlar
- K1: Hava saglayici adapter pattern ile degisebilir olmali.
- K2: Premium yetkilendirme istemciye birakilmamali, backend zorunlu dogrulamali.
- K3: Chatbot entegrasyonu direkt degil bridge katmani ile yapilmali.
- K4: Konum ve bildirim tercihleri profile bagli normalize tabloda tutulmali.
