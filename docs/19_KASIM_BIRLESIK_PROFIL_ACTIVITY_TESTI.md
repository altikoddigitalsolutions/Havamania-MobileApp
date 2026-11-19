# 19 Kasım — Üretim profil ve ayarlar birleşik Activity kabulü

## Test düzeni

ProfileSettingsIntegrationActivity yalnız debug manifestinde, exported=false olarak bulunur. Testin verdiği ViewModel fabrikası zorunludur; varsayılan Firebase'e geri dönüş yoktur. Gerçek ProfileViewModel ve ThemeViewModel aynı ayrı demo Auth/Firestore/UserProfileRepository kullanır. Profil snapshotWriter'ı üretimdeki korumalı ThemeManager snapshot eşlemesine bağlıdır; aktif kullanıcı demo Auth'tan okunur. Gerçek QA DataStore'da yalnız rastgele demo UID alanları yazılır ve finally bloğunda temizlenir.

Aktarılan zincir: demo Firestore → UserProfileRepository → üretim ProfileViewModel → korumalı snapshot/DataStore → üretim ThemeViewModel → gerçek profil başlığı ve premium kartı.

## Senaryo

- A profilinin uzaktaki adı snapshot üzerinden DataStore'a ve ayar etiketine ulaşır; sunucu premium aynası kartta görünür.
- Gerçek silme bileşenine test parolası/onayı girilir; gerçek ActivityScenario.recreate sonrası iki üretim ViewModel aynı kalır, pencere kapanır, A bilgileri görünür.
- A'nın uzaktaki adı değiştirilir; hem yerel akış hem disk okuması değişikliği doğrular.
- Gerçek demo Auth çıkışı ve B girişi yapılır; B uzaktaki adı snapshot ile yerel akışa gelir. B ücretsizdir.
- İkinci gerçek Activity yeniden oluşturma sonrası yalnız B adı/yerel şehir/ücretsiz kart görünür; eski A başlığı ve premium kartı dönmez. A'nın diskteki kendi kaydı korunur.

Şehirler testin başında her demo UID için yerel olarak hazırlanır; bu koşu uzaktan şehir değişikliğinin kalıcılığını doğrulamaz. Hesap silme işlemi, Storage ve hava servisi çağrılmaz. Senkronizasyon gönderme kapısı testte kapalıdır; snapshot silme engeli testte false verilir. Önceki ayrı disk testindeki oturum/nesil/engel kabulleri korunur.

## Açık sınırlar

Bu, gerçek üretim ViewModel'leriyle birleşik veri ve Activity kabulüdür; tüm SettingsScreen/ProfileScreen navigasyonunu kullanmaz, bu ekranların gerçek temel bileşenlerini debug test ekranında birleştirir. Süreç ölümü, tüm ekran eylemleri, varsayılan üretim Firebase wrapper'ı ve gerçek AccountDeletionManager/WeatherRepository ile birleşim tamamlandı sayılmaz. Canlı servis ve Google Play kabulü de ayrıdır.
## Sonuç ve tanı

Samsung SM-T500 Android 12 üzerinde son özel koşu XML: 4 test / 0 atlanan / 0 hata. Önceki üç özel Firebase senaryosu ve yeni birleşik Activity testi birlikte geçti. Android test derlemesi başarılı. Önceki 631 JVM ve 15 normal SDK kabulü korunuyor; bu tur üretim davranış kaynağı değiştirilmediği için bu paketler yeniden koşulmadı. Yeni dosyalar debug/test kapsamındadır.

İlk koşu kilitli tablette Compose hiyerarşisine erişemedi. Kilit açıldıktan sonra coroutine StateFlow.first beklemesiyle birleşik UI koşusu zaman aşımına uğradı; aşama tanısı A akışı ve ilk recreate sonrasında hesap geçişine işaret etti. Compose saati ilerleyen waitUntil tabanlı durum kontrolüne geçildi. Test finally temizliği NonCancellable ve süre sınırıyla çalışacak şekilde düzenlendi; böylece asıl hata iptal edilmiş temizliğin hatasıyla maskelenmiyor. Son koşuda süre uzatılmadan tüm senaryo geçti. Tanı düzenlemesi sırasında test dosyasındaki geçici derleme hatası düzeltildi; son kaynaklar derlendi.

Bu tur yeni bir üretim veri sızıntısı bulunmadı; birleşik testin bekleme/temizlik altyapısı düzeltildi. Test factory oturum sonunda kaldırıldı, ayrı repository/Activity kapatıldı ve yalnız demo UID tercihleri temizlendi. Canlı kullanıcı veya satın alma sistemi değiştirilmedi.

Sıradaki bağımsız yerel kontrol: aynı birleşik veri akışında çevrimdışı yeniden açılış ve bozuk kayıt/hata sonrası toparlanma. Tam ürün navigasyonu ve süreç ölümü ayrı açık kapılardır.