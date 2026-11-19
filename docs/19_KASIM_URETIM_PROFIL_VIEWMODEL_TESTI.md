# 19 Kasım — Üretim profil ViewModel test sınırı

## Yapılan değişiklik

ProfileViewModel'in Android fabrikasının kullandığı public Application constructor'ı korundu. Internal constructor ile Auth, Firestore, UserProfileRepository, profil yazma kapısı, snapshot yazıcısı ve tembel Storage sağlayıcısı verilebiliyor. Normal uygulama önceki Firebase singleton ve ThemeManager davranışını kullanıyor. Storage artık ilk fotoğraf işlemi gerektiğinde alınır; yalnız profil okuma için oluşturulmaz.

Test ayrı demo FirebaseApp/Auth/Firestore ile gerçek üretim ViewModel'i oluşturur. Yerel snapshot yazıcısı yalnız bellekte gözlem toplar; gerçek kullanıcının DataStore'una yazmaz. Hesap silme kapısı testte açık kabul edilir; AccountDeletionManager bu senaryonun kapsamı değildir. Storage sağlayıcısı erişimde hata verir; avatar yolu test edilmez.

## Senaryo

- A profilinin ViewModel Success ve aktif UID durumuna yansıması.
- Gerçek demo Auth çıkışında Idle ve null UID.
- Ayrı B oturumunda Bursa ve B adının yüklenmesi; eksik hesabın profil oluşturma yolu çalışabilir.
- B adının yanlış tür yapılmasıyla güvenli hata, uzak kayıt düzeltildiğinde aynı ViewModel'de toparlanma.
- ViewModelStore.clear sonrasında repository yeni sunucu verisini alırken temizlenmiş ViewModel'in ve snapshot yazıcısının artık güncellenmemesi.
- Testin finally bloğunda ViewModelStore ve ayrı repository dinleyicisi temizlenir.

## Kapsam sınırı ve sıradaki iş

Bu gerçek ProfileViewModel ve SDK kabulüdür; tam ProfileScreen/SettingsScreen navigasyonu veya üretim ThemeViewModel/DataStore kabulü değildir. Önceki gün bileşen fixture ile gerçek Activity.recreate testi ayrı geçti. Bu kanıtlar birleştirilip tam ekran uçtan uca kabul yapılmış sayılmaz. Sonraki yerel iş: ThemeViewModel yerel tercihlerinde ve profil snapshot'ında aynı test bağlamını güvenli biçimde kurup tam ekran Activity testine bağlamak. Canlı Firebase, ödeme, kurallar dağıtımı ve süreç ölümü hâlâ açık.

## Sonuç

631 JVM testi yeniden geçti. Samsung SM-T500 Android 12 demo Firebase SDK koşusu XML: 19 toplam / 5 özel mod bilerek atlandı / 0 hata; 14 aktif senaryo geçti. Yeni üretim ViewModel senaryosu bu 14 içindedir. Android derlemesi ve git diff --check başarılı. Bu kontrolde yeni bir üretim hatası bulunmadı; test erişimi ve tembel Storage oluşturma eklendi. Geçici emülatör ADB bağlantıları koşu tarafından temizlendi.
