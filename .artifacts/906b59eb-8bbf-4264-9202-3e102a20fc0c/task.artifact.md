# Production-Ready Yayına Hazırlık Görev Listesi

- `[x]` Güvenlik ve Manifest Düzenlemeleri
    - `[x]` `AndroidManifest.xml`: Backup ve cleartext traffic ayarlarını production seviyesine çek.
    - `[x]` İzin denetimi ve query kısıtlamaları.
- `[x]` Yasal Uyumluluk (KVKK/GDPR)
    - `[x]` `AuthScreens.kt`: Kayıt ekranına yasal onay kutusu ve linkleri ekle.
- `[x]` String Resource ve Yerelleştirme
    - `[x]` Kod içindeki kritik hardcoded metinleri `strings.xml`'e taşı.
    - `[x]` Yazım denetimi ve tutarlılık kontrolü.
- `[x]` Kullanıcı Dostu Hata Yönetimi
    - `[x]` `AuthViewModel.kt`: Teknik Firebase hatalarını anlaşılır Türkçe mesajlara çevir.
    - `[x]` `WeatherViewModel.kt`: Network ve API hatalarını UX standartlarına getir.
- `[x]` Erişilebilirlik ve UX Cila
    - `[x]` Kritik ikon ve butonlara `contentDescription` ekle.
    - `[x]` Dokunma alanlarını (touch targets) doğrula.
- `[x]` Final Release Derlemesi ve Raporlama
