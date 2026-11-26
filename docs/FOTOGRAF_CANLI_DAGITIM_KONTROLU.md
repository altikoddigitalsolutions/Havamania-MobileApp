# Fotoğraf yükleme — canlı dağıtım kontrolü

27 Kasım 2026. Öncelik 1 / V26-2. Yerel kod testleri başarılı; canlı fotoğraf kabulü henüz açık.

## Doğrulanan bulgu

https://api.havamania.com üzerinde kimliksiz ve dosyasız kontroller:

| İstek | Canlı sonuç | Güncel kaynakta beklenen |
| --- | --- | --- |
| GET /health | 200 | 200 |
| GET /v1/chatbot/consent | 401 | 401 |
| POST /v1/chatbot/attachments | 404 (Not Found) | 401 |

Önce çalışan backend imajı/kodu ve reverse proxy yolu kontrol edilmeli. Yerel Docker Desktop'ı başlatmak uzak API'yi güncellemez. Bu bulgu platform /api/bridge/upload ucunun veya köprü anahtarının ayrıca hazır olduğunu kanıtlamaz.

## Sunucu ekibinin uygulayacağı kontrol

Komutlar backend Python ortamında çalışır. Bunlar dağıtım komutu değildir; veri silmez, dosya/token/anahtar/soru göndermez.

```sh
python -m app.photo_deployment_check --base-url https://api.havamania.com
```

Beklenen çıkış kodu 0 ve routing_ok=true. Mevcut 404 durumunda 1 döner. Redirect otomatik izlenmez. Bu modül sunucuda bulunmuyorsa kontrol modülünü içeren sürüm henüz kurulmamıştır; yalnız bu durumdan fotoğraf yolunun bulunmadığı sonucu çıkarılmaz.

Konteynerdeki servisle dış adresi ayırmak için aynı güncel backend ortamında:

```sh
python -m app.photo_deployment_check --base-url http://127.0.0.1:8000
```

İç kontrol 401, dış kontrol 404 ise proxy/gateway yolu incelenir. İkisi de fotoğraf için 404 ise çalışan uygulamada route/sürüm/prefix kontrol edilir. /v1/chatbot/attachments route'u güncel kaynakta app/api/v1/routes/chatbot.py içinde; app/api/v1/router.py üzerinden kayıtlıdır. Üretim Dockerfile backend/pyproject.toml ile kurar; python-multipart ve httpx çalışma zamanı bağımlılıkları bulunmalı.

## Dağıtım ve son kabul

Sunucu yöneticisi çalışan servis adı, mevcut imaj/commit, compose dosyası ve proxy yapılandırmasını belirlemeli. Bu bilgiler doğrulanmadan rastgele compose up, migration, veritabanı silme veya servis değiştirme komutu uygulanmamalı. Dağıtım mevcut sunucunun yöntemiyle yapılır; bu belge sunucuya erişim sağlamaz.

Routing kontrolünün geçmesi yükleme kabulü değildir. Sonrasında izin verilen hesapta güncel rıza ile gerçek JPEG/PNG yüklemesi 201, dönen attachment_id ile ask 200 ve model/panelde görüntü kanıtı gerekir. Tekrar deneme aynı request_id/fotoğraf kimliklerini korumalı. Mevcut hesapla test kullanıcı tarafından onaylandı; henüz fotoğraf/model isteği gönderilmedi.

Platform kaynaklı güvenli tanılar: 503 bridge_auth_failed (platform 401/403), 503 upload_endpoint_unavailable (platform 404/405/501), 503 not_configured (yayın/bağlantı hazır değil). Gerçek mobil/backend 401 ise oturum doğrulaması ayrıdır. Ham hata metni veya anahtar paylaşılmaz.
