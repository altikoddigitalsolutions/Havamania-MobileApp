# Havamania Profil Fotoğrafı Yükleme - Root Cause Analysis (RCA) Planı

Bu plan, profil fotoğrafı yükleme sürecindeki hataları adım adım izleyerek gerçek kök nedeni (root cause) saptamayı ve kalıcı olarak düzeltmeyi hedefler.

## User Review Required

> [!IMPORTANT]
> **Detaylı Loglama:** `ProfileViewModel` ve `ProfileView` içerisindeki tüm kritik adımlara (URI alımı, resim işleme, Storage yükleme, Firestore güncelleme) numaralandırılmış loglar eklenecektir.
> **Hata Yakalama:** `StorageException` tüm detaylarıyla (ErrorCode, HTTP Code, Cause) loglanacaktır.

## Proposed Changes

### 1. ViewModel Tracing ve Hata Analizi
- **[MODIFY] [ProfileViewModel.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/ProfileViewModel.kt)**:
    - `uploadProfileImage` fonksiyonu, yükleme başlamadan önce `bucket`, `path`, `UID` ve `MIME type` bilgilerini loglayacak.
    - `putBytes` çağrısı öncesi ve sonrası açıkça belirtilecek.
    - `downloadUrl` alımı ve Firestore güncellemesi ayrı ayrı loglanacak.
    - `fetchProfile` fonksiyonunda Firestore'dan dönen verinin ham hali (`doc.data`) yazdırılacak.

### 2. UI Tracing
- **[MODIFY] [ProfileView.kt](file:///C:/Havamania-MobileApp/mobile/android/app/src/main/java/com/havamania/ProfileView.kt)**:
    - `photoPickerLauncher` sonucunda dönen URI loglanacak.
    - `PremiumProfileHeader` bileşenine gelen `imageUri` her render sırasında (timestamp ile birlikte) loglanacak.

### 3. Bucket ve Konfigürasyon Doğrulaması
- `google-services.json` içindeki bucket (`havamania-be3df.firebasestorage.app`) ile kodda kullanılan adresin uyumu tekrar kontrol edilecek.

## Araştırma Soruları (Loglardan Beklenen Yanıtlar)
1. **URI Geçerli mi?** (`Selected URI = ...`)
2. **Resim İşleme Başarılı mı?** (Step 2.2 OK)
3. **Storage Hatası Nedir?** (HTTP Code ve ErrorCode)
4. **Firestore Güncelleniyor mu?** (Step 6.1 OK)
5. **UI Neden Null Görüyor?** (Firestore'dan gelen veri UI state'ine aktarılamıyor mu?)

## Verification Plan

### Manual Verification
1. **Tracing:** Logcat'te `PHOTO_DEBUG` etiketiyle akış izlenecek.
2. **Root Cause Tespiti:** Hatanın durduğu adım saptanacak (Örn: Step 4'e ulaşılamadı).
3. **Fix Uygulama:** Saptanan nedene göre (Yetki, Path, Model uyuşmazlığı) düzeltme yapılacak.
