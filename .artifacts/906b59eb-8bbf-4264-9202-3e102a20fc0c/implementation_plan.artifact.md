# Havamania Production-Ready Yayına Hazırlık Planı

Bu plan, Havamania uygulamasının Google Play Store yayını öncesi tüm teknik ve yasal eksiklerini tamamlamayı, güvenliği artırmayı ve üretim kalitesine (production-ready) ulaşmayı hedefler.

## Kullanıcı İncelemesi Gerekenler

> [!IMPORTANT]
> **Yasal Onaylar:** Kayıt ekranına "Kullanım Koşullarını ve Gizlilik Politikasını okudum, kabul ediyorum" onay kutusu eklenecektir. Bu onay verilmeden hesap oluşturulamayacaktır.
> **Release Build:** Uygulama `release` modunda derlenecek, ProGuard/R8 etkinleştirilecek ve debug logları tamamen temizlenecektir.

## Önerilen Değişiklikler

### 1. Android Manifest ve Güvenlik
- **[MODIFY] [AndroidManifest.xml](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/AndroidManifest.xml)**:
    - `android:allowBackup="false"` (Veri güvenliği için).
    - `android:usesCleartextTraffic="false"` (Güvenli ağ iletişimi için).
    - Gereksiz izinlerin (`READ_EXTERNAL_STORAGE` gibi eğer kullanılmıyorsa) denetimi ve temizliği.

### 2. Yasal Uyumluluk (KVKK/GDPR)
- **[MODIFY] [AuthScreens.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AuthScreens.kt)**:
    - Kayıt ekranına (`RegisterScreen`) interaktif onay kutusu (Checkbox) ve yasal metin linkleri eklenecek.
- **[MODIFY] [LegalScreens.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/LegalScreens.kt)**:
    - Metinler son kez gözden geçirilecek ve tutarlılık sağlanacak.

### 3. String Kaynakları ve Yerelleştirme
- **[MODIFY] [strings.xml](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/res/values/strings.xml)**:
    - Kod içindeki tüm hardcoded Türkçe metinler bu dosyaya taşınacak.
    - Yazım ve noktalama hataları düzeltilecek.

### 4. Hata Mesajları ve UX
- **[MODIFY] [AuthViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/AuthViewModel.kt)** & **[WeatherViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/WeatherViewModel.kt)**:
    - "FirebaseException", "HttpException" gibi teknik terimler temizlenip "Bağlantı sorunu yaşandı" gibi kullanıcı dostu mesajlara dönüştürülecek.

### 5. Erişilebilirlik (Accessibility)
- Tüm butonlara ve ikonlara `contentDescription` eklenecek.
- Dokunma alanları (Touch targets) kontrol edilecek.

## Doğrulama Planı

### Smoke Test Senaryoları
1. **Yasal Onay Testi:** Onay kutusu işaretlenmeden kayıt butonunun çalışmadığı doğrulanacak.
2. **Hata Mesajı Testi:** Yanlış şifre girildiğinde teknik kod yerine Türkçe hata mesajı alındığı görülecek.
3. **Release Build Testi:** Projenin release modunda hatasız derlendiği ve açıldığı teyit edilecek.
4. **Offline Durum:** İnternet kapalıyken uygulamanın pürüzsüz bir "çevrimdışı" deneyimi sunduğu doğrulanacak.
