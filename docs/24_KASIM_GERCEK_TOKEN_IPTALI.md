# 24 Kasım — Gerçek yerel Firebase token iptali ve backend kabulü

## Doğrulanan davranış

Yeni npm run test:backend:auth-session komutu yalnız 127.0.0.1:9096 Auth emülatörünü demo-havamania-audit projesinde açar. Gerçek Firebase Admin SDK create_user, revoke_refresh_tokens, update_user ve delete_user çağrıları kullanılır. verify_id_token mock değildir: üretim firebase_user ve get_current_user zinciri gerçek SDK doğrulamasından geçer. Korumalı /v1/profile FastAPI TestClient üzerinden, ayrı bellek SQLite veritabanında çağrılır.

Üç senaryo geçti:

1. Gerçek token önce 200 aldı. iat saniyesi geçildikten sonra revoke_refresh_tokens çağrıldı ve SDK'dan okunan tokens_valid_after_timestamp değerinin token zamanından yeni olduğu doğrulandı. Aynı eski token 401 aldı. Parolayla açık yeniden giriş 200 ve aynı backend user_id döndürdü; eski token hâlâ 401 aldı.
2. Devre dışı bırakılan hesapta önce kabul edilen token 401 aldı. Hesap etkinleştirilip açık giriş yapıldığında aynı backend user_id ile 200 alındı.
3. Firebase'den silinen hesapta önce kabul edilen token 401 aldı. Önceki adımda eklenen UserNotFoundError yönetimi gerçek SDK ile doğrulandı.

## İzolasyon ve çalıştırma

Çalıştırma: tools/firebase-rules-tests dizininde npm run test:backend:auth-session. Windows sanal ortamı backend/venv/Scripts/python.exe kullanılır. İlk çalıştırmada cmd göreli eğik çizgili executable yolunu tanımadı; Node spawn çalıştırıcısı eklendi ve son çalıştırma sıfır çıkışla tamamlandı.

Sabit demo proje ve emülatör adresi zorunludur. HTTP adapter yalnız bu loopback adresine izin verir; harici istek engellenir. Servis anahtarı veya üretim kimliği yüklenmez. Test yalnız kendi UUID e-postalı kullanıcılarını oluşturur ve sonunda siler. Bellek veritabanı, uygulama bağımlılık override'ı ve named Firebase uygulaması kapatılır; emülatör komut sonunda kapanır. Token ve parola çıktıya yazılmaz.

## Kapsam sınırı ve sıradaki

Bu üç senaryo Firebase Auth emülatörü ve gerçek Admin SDK/backend entegrasyon kabulüdür. Emülatör tokenları üretim imzalı değildir; canlı sertifika/ağ/servis hesabı ve üretim yetki kontrollerinin kabulü değildir. Android uygulamasının refresh token iptali sonrasında ön plana dönüşü ve yerel durum temizliği bu komutla test edilmedi; sıradaki adım olarak açık tutuldu. Tablet gerekmedi. Önceki Activity kararlılığı, canlı dağıtım ve mağaza kapıları açık.

Bu turda üretim kodu değiştirilmedi; test çalıştırıcısı ve rapor eklendi. Önceki 31 backend testi ve 688 Android birim testi bu tur yeniden çalıştırılmadı. Git push veya sunucu dağıtımı yapılmadı.