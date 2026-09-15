# Android cihaz dayanıklılığı — 15 Eylül 2026

Bu tur yalnızca yerel doğrulama kapsamındadır. Gerçek hesap değiştirme veya canlı servise veri yazan kullanıcı akışları çalıştırılmadı.

## Düzeltilen sorunlar

- AI geçmişinin Firestore dinleyicisi hesap değişiminde eski kullanıcıda kalıyordu. Dinleyici artık yeniden kuruluyor; geciken okuma ve snapshot sonuçları kullanıcı/sürüm kontrolünden geçiyor. Yerel kayıttaki sahiplik ve belge kimliği Firestore yolundan alınıyor.
- Seyahat, tema, AI geçmişi ve hava önerilerinin işleri hesap değişiminde iptal ediliyor. Her işlem başladığı kullanıcı kimliğini tutuyor; bekleme sonrasında yeni kullanıcının bulut yoluna geçmiyor. Başka hesaba ait seyahat ve profil nesneleri reddediliyor.
- Tema ayarları her yüklemede süresiz yeni dinleyiciler açıyordu. Ayar ve profil gözlem işleri artık önceki işi iptal ederek kuruluyor. Hesap değişiminde kişisel ekran verileri ve premium görünümü sıfırlanıyor.
- İptal edilen eski bildirim gözlemcisi yeni hesabın listesini temizleyebiliyordu. İptal hatası yeniden iletiliyor ve liste güncellemeleri sahiplik kontrolü yapıyor.
- Konum ölçümü 15 saniye ile sınırlandı. Bekleme iptalinde Google Play Services isteği de iptal ediliyor. İzin geri çekilmesindeki SecurityException boş sonuç döndürüyor; yaklaşık konum izni dengeli doğruluk kullanıyor.
- Yeni konum isteği, hesap değişimi veya manuel konuma geçiş eski konum işini iptal ediyor. Geç gelen konum sonucu farklı hesabın hava ekranını değiştiremiyor.
- Hava uyarısı hesaplaması, başlatan hava isteğinin alt işi olarak çalışıyor. Eski istek iptal edildiğinde uyarı işi de duruyor.

## Yeni yerel regresyon senaryoları

AccountTaskScopeTest: geciken yanıt sırasında A→B, sıraya alınmış iş başlamadan kullanıcı değişimi, bekleme boyunca aynı hedef kullanıcı, hızlı A→B→A ve ViewModel üst kapsamının kapanması.

WeatherCacheTest: ağ yokken eski önbelleği koruma ve işaretleme, önbelleksiz durumda hata, iptali çevrimdışı geri dönüş olarak yutmama. Sahte API/DAO kullanılır; gerçek ağ çağrısı yapılmaz.

## Hâlâ doğrulanması gerekenler

1. Açık cihaz ekranında yaklaşık/kesin izin, izin reddi, izin geri çekme, GPS kapalı ve yeniden açma.
2. Ekran döndürme, arka plana alma, işletim sisteminin süreci öldürmesi ve form durumunun geri yüklenmesi. ViewModel kapsam testi bu kullanıcı akışlarının yerine geçmez.
3. Uzun kullanımda bellek, pil ve kaydırma akıcılığı ölçümleri.
4. Staging hesaplarıyla gerçek Firebase A→B geçişi ve bağlantı kesilip geri geldiğinde senkronizasyon. Başlatılmış uzak sunucu yazısının coroutine iptaliyle geri alınacağı varsayılmıyor; yazı hedefi başlangıç kullanıcısına sabitleniyor.
5. Ters coğrafi kodlama hâlâ eski eşzamanlı Geocoder API'sini IO iş parçacığında kullanıyor. 15 saniyelik sınır konum ölçümünü kapsar; adres çözümlemesinin toplam süresi için garanti değildir.

Cihaz dayanıklılığı başlığı bütünüyle kapanmış değildir. Sonraki bağımsız iş tasarım/erişilebilirlik incelemesidir; yukarıdaki cihaz matrisi yayın öncesinde tamamlanmalıdır.

## Son doğrulama sonucu

- `testDebugUnitTest`: 66 test, 0 başarısızlık.
- `connectedDebugAndroidTest`: SM-T500 / Android 12 üzerinde 2 izole veritabanı testi geçti.
- `assembleDebug` ve `lintDebug`: başarılı.
- APK: altı ARM64/x86_64 kütüphanesinde 16 KB ELF hizalaması ve `zipalign -c -P 16 4` geçti.
- APK SHA-256: `5268567EB63FDDFAB99410543E9A071C6E2382C72418AB531EFB6D26ABAD61CB`.
- Backend ve yerel Firebase emülatör testleri bu tur yeniden çalıştırılmadı.
