# 20 Kasım Önbelleksiz profil için birleşik arayüz kabulü

Yeni ProfileUnavailableContent üretim ProfileViewModel ve ThemeViewModel kullanan debug Activity'ye bağlandı. Gerçek demo Firebase ve gerçek QA DataStore ile önbelleksiz çevrimdışı hesap akışı artık görünür arayüz üzerinden de doğrulanıyor.

## Kabul

A hesabından çıkıldıktan sonra yalnız ilgili demo Firestore istemcisinin ağı kapatıldı ve ayrı B hesabına giriş yapıldı. B'de önbellek yokken Profil yükleniyor açıklaması göründü; A başlığı dönmedi. Compose saati sekiz saniye ilerletildiğinde gecikme açıklaması ve Tekrar dene sunuldu. Düğme gerçek ProfileViewModel.refresh çağrısını yaptı ve yükleme durumunu yeniden başlattı.

Yükleme sırasında gerçek ActivityScenario.recreate çalıştırıldı. Hâlâ B için yükleme gösterildi, eski profil görünmedi. Geri dön düğmesi test Activity'sinin callback sayacını bir kez artırdı. Bu gerçek ürün navigasyonu değil callback kabulüdür; ürün callback bağlantısı önceki kaynak incelemesinde doğrulanmıştı.

FireStore ağı kapalıyken bekleyen SDK yazısı oluşmadığı waitForPendingWrites ile doğrulandı. Bağlantı açılıp B profili alındığında üretim snapshot yazımı, yerel ad/şehir ve ücretsiz kart görünümü toparlandı; sonraki Activity yeniden oluşturması da geçti. Şehir test UID'sine yerelden hazırlanır, sunucudan şehir kalıcılığı bu koşunun kanıtı değildir.

## Sonuç

Son koşu Samsung SM-T500 Android 12 üzerinde 5 test, sıfır atlama ve sıfır hata ile geçti. Yeni kabul ayrı test sayısı eklemez; mevcut birleşik senaryo genişletildi. Önceki dört özel senaryo da aynı koşuda geçti. Son APK'nın çevrimdışı yüklemede recreate adımını içerdiği ayrıca doğrulandı. İlk paketten sonra eklenen son adım için test yeniden derlenip çalıştırıldı.

Bu tur debug/test kaynakları değişti; üretimdeki yükleme düzeltmesi önceki 631 JVM ve 6 UI kabulünü koruyor, o paketler bu tur yeniden koşulmadı. Yeni üretim hatası bulunmadı. Sadece demo UID tercihleri ve ayrı SDK bağlantıları kullanıldı; cihaz Wi-Fi ayarı veya canlı kullanıcı değiştirilmedi.

## Kalanlar

Debug Activity gerçek üretim ViewModel ve UI bileşenlerini kullanır, ancak tam ProfileScreen/SettingsScreen navigasyonunu çalıştırmaz. Geri dön için gerçek rota değişimi, süreç ölümü ve tüm cihaz uçak modu açık kalır. Sıradaki yerel UI kontrolü: açık tema, dar telefon düzeni ve büyük yazıda yükleme/hata eylemlerinin görünürlüğü ve erişilebilirliği.