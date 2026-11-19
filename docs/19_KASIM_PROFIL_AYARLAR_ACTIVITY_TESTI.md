# 19 Kasım — Gerçek Activity yeniden oluşturma kabulü

## Kapsam

ProfileSettingsTestActivity yalnız debug kaynaklarında, dışarıya kapalı bir test Activity'sidir. Gerçek PremiumProfileHeader, PremiumSettingsCard, rememberSettingsDialogs ve AccountDeleteDialog bileşenlerini kullanır. Ayrı fixture ViewModel yalnız test profilini taşır; Firebase/Auth/Room/DataStore/backend oluşturmaz. Release manifestine eklenmedi. PremiumSettingsCard testte erişilebilmesi için private yerine internal yapıldı; ürün davranışı değişmedi.

## Senaryolar

1. A profili/premium kartı/şehir görünürken silme penceresine sahte parola ve SİL girilir. Gerçek ActivityScenario.recreate çağrılır. Fixture ViewModel aynı kalmalı, ad/şehir/üyelik korunmalı; tehlikeli onay kapanmalı ve tekrar açıldığında parola/onay boş, silme düğmesi devre dışı olmalı.
2. Silme penceresi açık A profilinden ücretsiz B profiline geçilir. Pencere kapanmalı; A'nın adı/şehri/premium kartı kaybolmalı. Gerçek Activity yeniden oluşturulduğunda yalnız B'nin adı/şehri/ücretsiz kartı görünmeli.

Bu kabul önceki Compose StateRestorationTester kontrolünden farklı olarak Android Activity yeniden oluşturulmasını gerçekten çalıştırır. Ancak tüm ProfileScreen/SettingsScreen ve üretim ProfileViewModel/ThemeViewModel/AuthViewModel birleşimini test etmez. Fixture ViewModel ile UI bileşen kabulüdür; gerçek hesap geçişi önceki SDK koşularında ayrı doğrulanmıştır. Süreç ölümü, canlı hesap, tam navigasyon ve servis bağımlılıklarıyla Activity yeniden oluşturulması açık kalır.

## Sonraki iş

Tam ekran testini canlı hesaba erişmeden kurabilmek için üretim ViewModel servis bağımlılıklarının güvenli enjeksiyon sınırını incele. Gereksiz kapsamlı mimari değişiklik yapmadan Auth, repository ve yerel kullanıcı tercihlerini aynı demo/test bağlamına bağlayacak yolu belirle. Bu kapı kapanmadan tam ekran yeniden oluşturma kabulünü tamamlandı işaretleme.

## Sonuç

631 JVM testi ve bağlı Samsung SM-T500 Android 12 üzerinde 2 gerçek Activity yeniden oluşturma testi geçti; sıfır atlama/hata. Yeni üretim hatası bulunmadı. Debug derlemesi başarılı. Canlı servis veya hesap işlemi yapılmadı.
