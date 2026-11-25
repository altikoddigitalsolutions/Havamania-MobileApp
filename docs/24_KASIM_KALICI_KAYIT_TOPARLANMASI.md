# 24 Kasım Kalıcı kayıt hazırlığı ve süreç sonrası toparlanma

Yarım kayıt hazırlığının yalnız ViewModel belleğinde kalması giderildi. Üretim AuthViewModel'i, kayıt başlamadan önce uygulamanın noBackupFilesDir alanındaki registration-pending.json dosyasına atomik rezervasyon yazıyor. Kayıt sürüm, ad, e-posta ve biliniyorsa UID içeriyor; parola veya token içermiyor.

## Son davranış

İlk rezervasyon diske yazılmadan SDK hesap oluşturma başlamıyor. SDK hesabı oluşunca UID, profil mutasyonundan önce kayda bağlanıyor. Profil hazırlığı tamamlanınca kayıt atomik olarak null içeriğine çevriliyor; temizleme başarısızsa başarı yayımlanmıyor. Disk hatası ve okunamayan/gelecek sürümlü kayıt oturumu hazır kabul ettirmiyor. Çıkışta yerel kayıt temizlenemese bile SDK oturumu ve yerel iş iptal ediliyor; temizleme hatası hata durumuna yansıyor.

Yeni ViewModel açılışında kalıcı UID ve e-posta, mevcut SDK oturumuyla eşleşiyorsa aynı hazırlık otomatik tamamlanıyor. Parola kullanarak yeniden hesap oluşturulmuyor. Eşleşmeyen veya UID'si bağlanmamış kayıt otomatik sahiplenilmiyor. Açık girişte UID eşleşmiyorsa ya da belirsizse eski taslak temizleniyor; eski alanlar yeni oturuma uygulanmıyor. Gerekirse kullanıcı kayıt ekranında bilgilerini yeniden girer. Bozuk kaydın temizlenmesi de açık giriş/çıkış gibi kontrollü yoldan yapılır.

Otomatik toparlanma, sonradan tamamlanmış sunucu profilini eski taslakla ezmiyor. RegistrationProfilePatch yalnız eksik/geçersiz ad/e-posta veya eksik oluşturulma alanlarını tamamlıyor; mevcut dolu alanlar, tercihler, üyelik ve createdAt korunuyor. Değişiklik yoksa updatedAt da değişmiyor. Otomatik toparlanma SDK görünen adını eski taslakla değiştirmiyor. Normal, açık kayıt işlemi girilen adı güncellemeye devam ediyor.

## Doğrulama

Son kaynaklarla 666 JVM testi, 0 hata ve 0 atlanan sonuç verdi. Önceki 656 teste eklenen yedi kalıcılık senaryosu; rezervasyon/UID yazım sırası, yeniden açılan hazırlık, disk/temizleme hatası, bozuk/gelecek kayıt ve başka UID'nin reddini kapsıyor. Üç profil patch senaryosu daha yeni alanların korunmasını, eksik alanların tamamlanmasını ve normal kayıt güncellemesini doğruluyor.

Samsung SM-T500 Android 12 üzerinde son kayıt SDK koşusu 3 test, 0 hata, 0 atlanan verdi: önceki kayıt retry/merge; gerçek kişiselleştirme Activity/form/DataStore; farklı UID ve UID'siz kalıcı kayıt. Son senaryo aynı UID'ye ait eski taslağın mevcut sunucu adını değiştirmediğini de doğruladı. B hesabının A belgesini okuması kurallarca reddedildi; test bu reddi bekler.

Gerçek süreç testi ayrı iki aşama olarak geçti. Prepare aşamasında gerçek SDK hesabı ve atomik bağlı UID kaydı oluşturuldu, profil yazımı bekletildi. Runner yalnız com.havamania QA paketini force-stop yaptı. Resume aşaması farklı PID'yi, aynı SDK UID'sini ve kalıcı dosyayı doğruladı. Yeni AuthViewModel başlangıçta Loading/currentUser=null kaldı; hazırlığı tamamlayınca doğru profil sunucuda oluştu ve yerel işaret temizlendi. Her aşama OK (1 test) verdi; bunlar iki ayrı özellik değil tek süreç toparlanma akışının iki aşamasıdır.

Komutlar tools/firebase-rules-tests içinde npm run test:android:registration ve npm run test:android:registration:restart. Emülatörler demo-havamania-audit/localhost ile sınırlı; test dosyaları UUID'ye bağlı ayrı dosyalardır. Sonunda emülatörler ve geçici ADB reverse bağlantıları kapandı. Play paketine veya canlı hesaplara işlem yapılmadı. Git diff --check temiz.

## İzlenen sonuçlar ve açık işler

Bir ilk SDK kontrolünde B'nin A belgesini okuması PERMISSION_DENIED verdi; güvenlik kuralı doğruydu, test beklentisi düzeltildi. Birleşik Activity bir ara koşuda kişiselleştirmeye geçiş beklemesinde zaman aşımı aldı. Son tekrarlar geçti; kesin neden ayrıştırılmadı. Tanı bilgisi oturum/profil/rota durumunu içerecek şekilde genişletildi; tekrarlı arayüz kararlılığı açık kabul maddesi olarak kalıyor.

Sıradaki öncelik, kullanıcı çıkış yaptıktan veya kaydı iptal ettikten sonra geciken SDK Task'ın oturumu yeniden açmasını engellemek ve doğrulamak. Coroutine iptali SDK isteğini geri almaz. Ayrıca ağ kapalı açılış/toparlanma açıklaması, her olası dosya yazımı anında kesinti, yavaş depolama ölçümü, tam launcher/WeatherPremiumActivity soğuk açılışı ve kaydetme sırasında recreate/geri/skip/deep link senaryoları açık. Bu SDK süreç testi tam ürün arayüzü veya donanım güç kesintisi kabulü değildir. Dosya kayıtları küçük ve eşzamanlıdır; yavaş depolama üzerindeki kullanıcı arayüzü etkisi ayrıca ölçülmeli.

Yeni kalıcılık bu sürümde başlatılan kayıtlar için geçerlidir; önceki sürümde hiç diske yazılmamış taslağı geri getirmez. Kişisel gradle.properties korundu; commit/push yapılmadı.