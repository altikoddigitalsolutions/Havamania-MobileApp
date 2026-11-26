# 26 Kasım — FCM kayıt yanıtı uyumu

## Sorun ve düzeltme
Backend POST /v1/devices/push-token işlemi upsert sonucunda 201 döndürüyor. Android'de hem registerFcmTokenSnapshot hem sendFcmTokenWithAuth yalnızca 200 kabul ediyordu; geçerli kayıt yerelde başarısız sayılabiliyordu. İki yol da 200 ve 201 yanıtlarını kabul ediyor. Diğer 2xx kodları otomatik başarı sayılmıyor.

Başarı için makbuz hâlâ tam eşleşmeli: kullanıcı, token, Android platformu, kurulum kimliği, revision, owner_protocol=1 ve dolu kayıt kimliği. Yanıt öncesi/sonrası güncel oturum kontrolü, iptal yayılımı, 401 için tek yenileme, 429 ve sunucu hatası yeniden deneme politikası korunuyor. Backend, protokol bayrakları ve canlı yapılandırma değiştirilmedi.

## Doğrulama
Önce regresyon testleri mevcut uygulamada çalıştırıldı: 12 ilgili testten üçü başarısız oldu (201 başarı, gateway 201, 401 sonrası 201). Düzeltmeden sonra son kaynakla 933 JVM testi hatasız geçti; atlama yok, debug APK başarılı.

Yeni kapsam: doğru 201 kabulü, 202 ve boş 204 reddi, 201 yanlış kullanıcı/eski revision reddi ve yanıt sırasında hesap değişiminde SUPERSEDED. Mevcut 200 ve makbuz alan kontrolleri de çalıştı. Backend test_device_token_transfer ve test_push toplam sekiz yerel testi geçti; gerçek HTTP test fixture'ıyla engelleniyor. Test veritabanı kullanıldı, canlı kullanıcı token'ı veya Firebase verisi işlenmedi.

## Sınırlar ve sıra
Bu adım ağ yanıtı yorumlamasını düzeltir; üretim FCM sahibi protokolünün etkinliği, gerçek token kaydı, push teslimatı, izin/kanal, hesap değişimi ve tepsi etkileşimi uçtan uca doğrulandı anlamına gelmez. Canlı FCM testi açık.

Sıradaki: hava API hız sınırının max_requests/window_seconds parametreleriyle istemci tarafından değiştirilebilmesi. Önceki görsel matris, Firebase/senkronizasyon/güvenlik ve Play yayın kontrolleri korunur. Push yapılmadı.
