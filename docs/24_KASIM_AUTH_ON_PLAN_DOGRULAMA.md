# 24 Kasım 2026 Ön planda Firebase Auth oturum doğrulaması

Uygulama, SDK'da saklanan kullanıcıyı başlangıçta kabul ediyordu; sunucuda devre dışı bırakılma ön plana dönüşte ayrıca doğrulanmıyordu. Bu adım başlangıç/ön plan kontrolünü, geçici ağ hatasının ayrılmasını ve eski doğrulama yanıtlarının yeni hesabı etkilememesini ele alır.

## Değişiklik

WeatherPremiumActivity ortak AuthSessionForegroundCheck kullanır. Android ağının doğrulanmış olduğu durumda ilk aktif ekranda ve ON_RESUME olayında AuthViewModel.refreshSession çalışır. Ağ doğrulanmamışsa yeni kontrol başlatılmaz; çevrimdışı önbellek oturumu yalnız bu nedenle kapatılmaz.

Kontrol, aynı kullanıcı/oturum için reload ve zorunlu token yenilemesini ortak SDK görev takibiyle yürütür. İşlem 20 saniyeyle sınırlıdır. USER_DISABLED, USER_NOT_FOUND, USER_TOKEN_EXPIRED ve INVALID_USER_TOKEN kalıcı hata kodları çıkış niyetini atomik oturum dosyasına yazar, SDK oturumunu kapatır ve yeniden giriş hata durumu üretir. Ağ/servis hatası veya zaman aşımı tek başına çıkış yaptırmaz. Kullanıcı çıkışı, yeni kimlik doğrulama ve ViewModel kapanması eski kontrolün sonuç yayınlamasını geçersiz kılar.

Hata eşlemesi artık FirebaseAuthException.errorCode bilgisini de kullanır; yalnız İngilizce hata mesajındaki parçalara bağlı değildir. Devre dışı hesabın açık giriş denemesi doğru Türkçe mesajı alır.

Oturum doğrulaması sırasında profil SDK eşitlemesi başlayamadıysa başarılı doğrulama/kimlik doğrulama sonrasındaki oturum hazır olayı, en son sunucu onaylı profili yeniden uzlaştırır. Ek profil sorgusu veya Saved/geri hareketi üretmez. Aynı anda iki profil uzlaştırması başlatılmaz; mevcut profil SDK görevinin bitişi beklendiğinde eski oturum nesli tekrar kontrol edilir.

## Yerel kabul yöntemi

`npm run test:android:auth-session` üç senaryo çalıştırır. Devre dışı hesap senaryosu gerçek Auth emülatörünün yönetim endpoint'inde yalnız test UID'sini disable/enable yapar; key veya servis hesabı yüklenmez. Fixture yalnız loopback, sabit demo proje ve nonce kullanır; profil okuması yalnız ad/biyografi alanlarını döndürür.

Üretim foreground helper'ı debug Activity'de kullanılır. İlk gerçek reload kontrollü bekletilirken sunucu onaylı profil hazır hale getirilir; SDK adı eski kalır. Doğrulama tamamlanınca uzlaştırmanın atlanmaması kontrol edilir. Ardından profil yazısı çevrimdışı kuyruğa alınır, Activity durdurulur ve Auth hesabı sunucuda devre dışı bırakılır. Kontrollü ağ kapısı kapalıyken ön plana dönüş SDK isteği/çıkış üretmez; kapı açıldığında tekrar ön plana dönüş, kalıcı hata sonrası çıkışı ve atomik signed_out kaydını doğrular. Bekleyen profil sunucuda ilerlemez ve eski Saved üretilmez. Devre dışı kimlik bilgileri oturum açamaz. Hesap yeniden etkinleştirilince açık girişle kendi pending profili normal kurallarla tamamlanabilir.

Diğer iki senaryo kontrollü FirebaseNetworkException ile mevcut oturumun korunmasını ve çıkış sonrasında geç gelen kalıcı hata sonucunun B girişini engellememesini sınar. Bunlar fiziksel TCP kesintisi değildir; B girişinde gerçek demo SDK kullanılır.

## Sınırlar

Bu bir anlık sunucu itmesi veya periyodik arka plan oturum denetimi değildir. Çevrimdışıyken sunucu kararı öğrenilemez; önceden kabul edilmiş önbellek oturumu korunur. Ağ kapısı testte kontrollüdür; fiziksel Android bağlantı geçişleri ayrıca açık kalır. SDK beklemesi iptal edilse de alttaki SDK görevi sonlanana kadar ortak güvenli görev takibi bazı yeni işlemleri bekletebilir.

Gerçek refresh-token revocation/validSince, backend check_revoked kabulü, canlı Firebase ve tam WeatherPremiumActivity launcher akışı bu testle tamamlandı sayılmaz. Önceki aralıklı Activity/başlangıç kararlılığı kök nedenleri ve diğer mağaza kapıları açık kalır.

## Son kaynakla doğrulama sonucu

24 Kasım 2026: Auth ön plan paketi 3/3, mevcut kayıt/kişiselleştirme SDK paketi 8/8 ve JVM XML toplamı 688/688 geçti; hata, başarısızlık ve atlanan test yok. Zincir son kaynakla sırasıyla çalıştı ve sıfır çıkış koduyla tamamlandı. İlk Auth denemesinde fixture profil okumasının sonucu dış kapsama aktarılmadığı için eksik ad alanı hatası oluştu; fixture düzeltildi. Bu uygulama hatası değildi. Sonrasında oturum hazır olayı ve kontrollü ilk reload çakışma kontrolü de eklenerek paketler yeniden doğrulandı.

Bu sonuç önceki aralıklı Activity/başlangıç sorununun kök nedenini kapatmaz. Sıradaki yerel adım: gerçek refresh-token iptali/validSince ve backend tarafında iptal edilmiş token reddi. Canlı ortama işlem yapılmadı; Git push yapılmadı.