# 26 Kasım — Hava API hız sınırı

## Doğrulanan sorun
FastAPI bağımlılığı max_requests ve window_seconds alanlarını HTTP sorgu parametresi olarak yayımlıyordu. Yerel regresyonda max_requests=1000000 veya window_seconds=-1 ile 61. istek de 200 döndü; beklenen hava kotası uygulanmadı. OpenAPI kontrolü de bu iki alanı doğruladı. Düzeltme öncesi üç regresyon başarısız oldu.

## Düzeltme
Bağımlılık yalnızca Request alır; sunucu politikası istemciden gelmez. IP başına tüm hava uçlarında ortak kayan pencere: 60 istek/60 saniye. Fazla istekte 429 ve kalan süreye göre yukarı yuvarlanan Retry-After döner. Pencere tam sınırda yenilenir, zaman ölçümü monotonic kullanır.

Senkron FastAPI bağımlılığı worker thread'lerde çalıştığı için sayaç temizleme/kontrol/ekleme işlemi kilitle atomik yapıldı. Bellek 10.000 istemci anahtarı ve istemci başına en fazla 60 zaman kaydıyla sınırlı. Kapasitede süresi dolan anahtarlar temizlenir; canlı bir istemcinin kotası yeni IP lehine silinmez. Kapasite tamamen doluysa yeni istemci 429 alır. Genel middleware hız sınırı ayrı olarak korunur.

OpenAPI JSON ve uç nokta referansından istemci politika parametreleri kaldırıldı. Bilinmeyen eski sorgu parametreleri FastAPI tarafından yok sayılır; limiti değiştiremez. Test fixture'ı her backend testi için hava sayacını da sıfırlar; testlerin kotası birbirine taşınmaz.

## Testler
Yeni dosyada yedi kontrol var: OpenAPI sorgu alanları, iki parametre aşımı, tam pencere sınırı/Retry-After, aynı IP'de 100 eşzamanlı istek, kapasite ve süre dolunca toparlanma, farklı hava uçları arasında ortak kota. Mevcut genel limiter testleriyle ilgili koşu 11/11 geçti.

Tam backend paketi: 267 geçti, 13 atlandı, hata yok. Atlananlar PostgreSQL migration ortamı isteyen testler; bu koşuda PG_MIGRATION_TEST_URL yok. Dört Firebase SDK deprecation uyarısı mevcut, test hatası değil. Yerel testlerde gerçek HTTP engellenir; hava verisi stub kullanır. Android kaynağı bu adımda değişmedi; tablet veya Android testlerini yeniden çalıştırmayı gerektiren değişiklik yok. git diff --check temiz.

## Sınırlar ve sıra
Sayaç süreç içindedir; yeniden başlatmada sıfırlanır, worker/sunucular arasında paylaşılmaz. Production için güvenilir proxy/istemci IP aktarımı ve ortak sayaç (örneğin Redis veya ağ geçidi) doğrulanmalı. Aynı IP'yi paylaşan kullanıcılar ortak kota kullanır. Canlı sunucuda yük testi veya dağıtım yapılmadı.

Sıradaki: üretim dağıtımında proxy IP güveni ve çok worker hız sınırı yapılandırmasının incelenmesi. PostgreSQL migration kabulü, canlı FCM teslimatı, Firebase/senkronizasyon/güvenlik, tüm tema/ekran matrisi ve Play yayın kapıları açık kalır. Push yapılmadı.
