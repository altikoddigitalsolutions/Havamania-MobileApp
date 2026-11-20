# 20 Kasım — Çevrimdışı profil ve hata sonrası toparlanma

## Doğrulananlar

- Ayrı demo FirebaseApp/Firestore istemcisi kalıcı disk önbelleğiyle kapatılıp aynı ad ve seçeneklerle yeniden açıldı. Firestore ağı yalnız bu istemcide kapatıldı; cihaz Wi-Fi/izin/kilit ayarı değiştirilmedi.
- Profil aynı UID için disk önbelleğinden okundu. Yeni dinleyici için önbellekteki premium=true sunucu onayı sayılmadı; üyelik gösterimi false ve doğrulanmış sunucu şehri null kaldı.
- Firestore çevrimdışıyken yerel yetkili fixture uzak premium alanını yanlış tür yaptı. Bağlantı açıldığında güvenli profil hatası oluştu ve eski profil temizlendi. Uzak alan düzeltildiğinde aynı dinleyici geçerli profili yeniden yayımladı.
- Firestore tekrar çevrimdışı bırakılıp gerçek demo Auth ile B hesabına geçildi. B'nin önbelleği yoktu; A profili/şehri gösterilmedi. Durum Loading kaldı ve sunucuya bağlanmadan Missing sayılmadı. Bağlantı geri gelince B için sunucudan doğrulanan Missing alındı.
- Birleşik gerçek ProfileViewModel → korumalı DataStore snapshot → ThemeViewModel → debug Activity testine çevrimdışı Activity.recreate eklendi. Aynı oturumda daha önce doğrulanmış ad ve premium gösterimi korundu.
- Ağ geri açıldıktan sonra profil adı yanlış tür yapıldı: üretim ProfileViewModel Error verdi, premium gösterimi temizlendi, eski profil başlığı kayboldu. Ad düzeltildiğinde gerçek snapshot yazımı, yerel ad ve üyelik gösterimi toparlandı. Sonraki B geçişi ve ikinci recreate kontrolü de geçti.

## Sonuç

Samsung SM-T500 Android 12 üzerinde son özel koşu XML: 5 test / 0 atlanan / 0 hata. Yeni kalıcı önbellek senaryosu, genişletilmiş birleşik Activity ve önceki üç özel Firebase senaryosu birlikte geçti. Android test derlemesi ve git diff --check başarılı. Üretim kaynakları değişmediğinden önceki 631 JVM ve 15 normal SDK sonucu bu tur yeniden koşulmadı. Bu kapsamda yeni üretim hatası bulunmadı.

Kalıcı Firestore önbelleği yalnız rastgele named demo app'e aittir; test sonunda terminate/clearPersistence ile temizlendi. Birleşik testte yalnız demo UID DataStore anahtarları temizlendi. Geçici ADB yönlendirmeleri kaldırıldı; canlı kullanıcı veya sunucu değiştirilmedi.

## Açık kabul ve sıradaki

SDK istemcisinin aynı süreçte yeniden oluşturulması Android süreç ölümü değildir. Çevrimdışı Activity testi mevcut ViewModel'leri korur; tam uygulama soğuk açılışı/navigasyonu kabulü tamamlanmadı. Auth emülatörü erişilebilir kalırken yalnız Firestore SDK ağı kesildi; tüm cihazın uçak modu kabulü değildir. Tam ProfileScreen/SettingsScreen ve gerçek servis kapıları hâlâ açık.

Önbelleksiz B hesabının çevrimdışı durumu Loading olarak doğrulandı. Bu güvenli veri davranışı, kullanıcıya sonsuz bekleme yerine yeterli açıklama ve geri dönüş gösterildiğini kanıtlamaz. Sıradaki yerel iş: ilk kez girilen/önbelleksiz hesapta çevrimdışı yükleme, görünür hata/yeniden deneme ve manuel geri dönüşün tam kullanıcı arayüzü kabulü.