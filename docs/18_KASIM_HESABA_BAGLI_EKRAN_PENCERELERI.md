# 18 Kasım — Hesaba bağlı profil ve ayarlar pencereleri

## Bulgu

Profil ve ayarlardaki geçici pencere durumlarının bazıları kullanıcı kimliğine bağlı değildi. Aynı composition açıkken oturum değiştiğinde eski hesapta açılmış silme/çıkış onayı, tema/dil/üslup seçimi veya profil istatistik penceresi kalabiliyordu. Silme penceresinin kendi remember parola/onay metni de pencere composition'da kaldığı sürece korunabilirdi. Bu, hesap silme backend'inin hatalı hesap sildiğinin kanıtı değildir; ekran durumunun yanlış hesaba taşınma riskidir.

## Düzeltme

- ThemeViewModel etkin UID'yi gözlemlenebilir StateFlow olarak yayımlıyor. Hesap değişiminde önce yerel hesap durumu temizleniyor.
- SettingsScreen gerçek etkin UID ile rememberSettingsDialogs kullanıyor. Silme, çıkış, tema, dil, asistan üslubu ve bilgi pencereleri hesap değişiminde sıfırlanıyor.
- Geçici onaylar rememberSaveable kullanılmadan tutuluyor. Composition yeniden kurulduğunda açık tehlikeli işlem ve parola geri yüklenmiyor.
- Silme/çıkış onayı, pencerenin bağlı olduğu UID hâlâ mevcut Auth kullanıcısıysa çalışıyor. Eski composition'dan gelen onay yeni hesaba uygulanmıyor.
- Profil bilgi başlığı, bilgi penceresi ve istatistik detayı remember(owner) ile hesap değişiminde kapanıyor. Hakkımda penceresi zaten aynı şekilde bağlıydı.

## Yerel test kapsamı

AccountSettingsDialogsUiTest, üretimdeki rememberSettingsDialogs ve gerçek AccountDeleteDialog ile sahte parola giriyor; A→B ve çıkışta pencerenin kapandığını, yeniden açıldığında parola/onay alanlarının boş ve silme düğmesinin devre dışı olduğunu kontrol ediyor. Compose StateRestorationTester ile aynı hesabın composition durumu yeniden kurulunca tehlikeli onay/parolanın geri yüklenmediği ayrıca sınanıyor. Test hiçbir hesap silme servisini çağırmıyor.

Bu bir tam Activity.recreate veya process-death testi değildir. Tüm SettingsScreen/ProfileScreen, gerçek AuthViewModel/ThemeViewModel/DataStore, ad/şehir/üyelik ve navigasyonun uçtan uca birleşimi hâlâ açık. Önceki gerçek Firebase SDK A–B ve sunucu üyelik kabulleri ayrı kanıtlardır; bu UI fixture onları kapsadığını iddia etmez.

İlk üç UI koşusu, boş alan karşılaştırmasında TextField etiketinin de metne katılması nedeniyle başarısız oldu. Kontrol doğrudan EditableText değerine düzeltildi; uygulama hatası testten gizlenmedi.

## Sonuç

Son kaynaklarla 631 JVM testi ve bağlı Samsung SM-T500 Android 12 üzerinde 8 UI testi geçti (3 yeni hesap/parola testi + 3 profil hata + 2 dil seçimi; sıfır atlama/hata). Son Android derlemesi başarılı; git diff --check geçti. Canlı Firebase veya gerçek hesap silme çağrısı yapılmadı.
