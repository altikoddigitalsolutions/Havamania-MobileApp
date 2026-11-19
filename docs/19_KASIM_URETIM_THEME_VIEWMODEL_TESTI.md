# 19 Kasım — Üretim ThemeViewModel ve gerçek yerel tercihler

## Değişiklik

ThemeViewModel'in public Application constructor'ı korundu. Internal constructor ayrı Auth/Firestore/UserProfileRepository, hava önbelleğine hesap değişimi bildirimi ve senkronizasyon kapısı alabiliyor. Normal uygulama aynı Firebase ve WeatherRepository davranışını kullanır; testte gerçek hava önbelleğine dokunmadan hesap bildirimleri gözlenir. Üretim oturum ve hesap silme kontrolleri gevşetilmedi.

## Doğrulanan senaryo

Gerçek üretim ThemeViewModel, ayrı demo FirebaseApp/Auth/Firestore, gerçek QA DataStore ve yalnız rastgele demo UID anahtarlarıyla çalıştırıldı. A'nın yerel adı/Ankara şehri ve sunucu premium onayı yüklendi. Çıkışta eski ad, şehir ve üyelik temizlendi. Ayrı B hesabında B'nin yerel adı/Bursa şehri yüklendi. Tek DataStore işleminde A ve B adları değiştirildiğinde B'nin yeni adı yayımlandı, A değişikliği son B durumunu bozmadı. B'ye sunucudan üyelik verme/geri alma gözlendi. ViewModelStore.clear sonrasında yerel tercih güncellemesi ViewModel'i değiştirmedi.

Test sonunda sadece oluşturulan iki demo kullanıcının havamania:<uid>: anahtarları temizlendi; diğer kullanıcı anahtarları silinmedi. Test ayrı bir DataStore dosyası kullanmaz; QA uygulamasının gerçek DataStore'unda test kullanıcılarının ad/şehir alanları hazırlanır. Bunu üretim DataStore'una genel temizlik yapılmış gibi yorumlamayın.

## Sonuç

631 JVM testi yeniden geçti. Bağlı Samsung SM-T500 Android 12 üzerinde özel Firebase koşusu XML 3 test / 0 atlanan / 0 hata: yeni ThemeViewModel testi, önceki sunucu üyelik testi ve geciken A–B profil olayı testi birlikte geçti. Derleme ve git diff --check başarılı; geçici ADB bağlantıları kaldırıldı. Yeni bir üretim hatası bulunmadı.

## Sınırlar ve sıradaki iş

Senkronizasyon kapısı bu testte kapalı; bekleyen şehir gönderme kuyruğu/hesap silme kapısı bu senaryoda doğrulanmadı. Şehir DataStore'a önceden hazırlanır; bu test sunucudan varsayılan şehir değişikliğinin diske yazıldığını kanıtlamaz. ProfileViewModel snapshot yazıcısı varsayılan Firebase oturumunu da kontrol ettiği için demo kimlikle bu koruma kaldırılmadı. WeatherRepository bildirimi testte gözlenir, gerçek hava önbelleği değiştirilmez.

Tam SettingsScreen/ProfileScreen + üretim ViewModel'ler + Activity.recreate + gerçek snapshot kalıcılığı kabulü hâlâ açık. Sonraki yerel iş: profil snapshot kalıcılığını aynı demo oturumuyla, varsayılan üretim oturumunu bozmadan sınayabilecek test sınırı hazırlamak ve ardından ViewModel'leri Activity fixture'a bağlamak. Canlı servisler ve mağaza kabulü açık kalır.