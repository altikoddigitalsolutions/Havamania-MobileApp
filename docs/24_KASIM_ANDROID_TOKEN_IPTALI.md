# 24 Kasım — Android refresh-token iptali kabulü

## Kapsam

FirebaseAuthSessionValidationDeviceTest paketi gerçek refresh-token iptali senaryosuyla genişletildi. Yerel demo Auth fixture'ı validSince değerini token iat değerinden ileriye taşır; zaman sınırı emülatör saatinde kontrol edilir. Sabit loopback demo proje, nonce ve UID denetimleri korunur. Üretim anahtarı kullanılmaz.

Senaryo gerçek AuthViewModel, ProfileViewModel, ortak foreground helper, debug profil Activity ve DataStore kullanır. İlk doğrulama/profil çakışması; bekleyen çevrimdışı profil yazısı; ağ kapısı kapalıyken önbellek oturumunun korunması; tekrar ön plana dönüşte zorunlu token yenileme ve güvenli çıkış; kalıcı signed_out; eski Saved/geri çağrısı oluşmaması ve açık girişle aynı kullanıcı profilinin toparlanması kontrol edilir. Var olan disable/geçici hata/eski sonuç testleri pakette korunur.

## Çalıştırma geçmişi

Mevcut üç senaryo önce 3/3 geçti. İlk genişletilmiş dört testte üçü geçti; token iptali testi, iptal zamanı token üretim zamanından ileri olmadığı için fixture önkoşulunda durdu. Tablet saatine dayalı bekleme kaldırılıp emülatör saatinde iat sınırı bekletildi. Bu ilk hata uygulama oturum davranışı için sonuç değildir. Son tekrarın sonucu aşağıya eklenecek.

## Sınırlar

Kontrollü ağ kapısı gerçek uçak modu değildir. Tam WeatherPremiumActivity launcher, canlı Firebase tokenları, tüm cihaz ağı ve önceki aralıklı Activity kararlılığı bu testle kapanmaz. Bu tur üretim Kotlin değiştirilmedi; önceki 688 JVM testi yeniden çalıştırılmadı. Push veya canlı dağıtım yok.

## Görsel takip

Kullanıcının iç içe dikdörtgen sorunu yeniden açık ve öncelikli sıraya eklendi. Tüm ekran/menü/buton ve temaların kabul matrisi [GORSEL_BUTUNLUK_KABUL_MATRISI.md](GORSEL_BUTUNLUK_KABUL_MATRISI.md) dosyasında. Önceki kart sadeleştirmeleri tüm ürün görsel kabulü sayılmıyor; sıradaki görsel tur hava ekranından başlayacak.
## Son doğrulama

Son kaynakla SM-T500 Android 12 üzerinde 4/4 test geçti; XML sonucu sıfır hata/başarısızlık/atlama. Gerçek refresh-token iptali sonrası foreground doğrulama güvenli çıkış ve kalıcı signed_out üretti. Onaylı yerel profil adı korundu, eski Saved/geri olayı oluşmadı. Açık yeniden girişle aynı hesabın pending profili sunucuda tamamlandı ve SDK/yerel ad uzlaştırıldı. Disable, geçici ağ hatası ve eski hesap sonucunun yeni hesaba etkisizliği regresyonları da geçti. Yeni üretim hatası bulunmadı; önceki oturum düzeltmeleri bu senaryoyu karşıladı.