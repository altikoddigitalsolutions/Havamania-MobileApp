# 20 Kasım Giriş işlemlerinde çift gönderim ve iptal koruması

AuthViewModel giriş, kayıt ve parola sıfırlama işlemlerini ortak AuthOperationRunner üzerinden yürütüyor. İşlem rezervasyonu coroutine çalıştırılmadan önce alınıyor; aynı ViewModel üzerinde başka bir işlem sürerken yeni servis çağrısı başlatılmıyor.

## Düzeltilen davranışlar

- Loading durumu ilk çağrı içinde senkron yayımlanıyor. Kuyruk henüz çalışmasa bile ikinci giriş, kayıt veya parola sıfırlama isteği reddediliyor.
- CancellationException kullanıcıya giriş hatası olarak dönüştürülmeden yeniden fırlatılıyor. Geçerli işlemin iptali bekleme durumunu bırakıyor; normal hata sonrası yeniden deneme mümkün.
- Çıkış ve ViewModel kapanışı önce işlem kimliğini geçersiz kılıyor, ardından coroutine'i iptal ediyor. İptale direnerek geç dönen işin başarı/hata callback'i yeni oturumun veya yeni işlemin durumunu değiştiremiyor. Eski işin finally bloğu yeni işlemin kilidini açamıyor.
- İşlem sürerken clearError yükleme durumunu silemiyor. Başarılı giriş/kayıt sonunda SDK'nın güncel kullanıcısı açıkça yayımlanıyor.
- ViewModel sınırında ad/e-posta kenar boşlukları temizleniyor; parola değiştirilmiyor. Kayıtta SDK null kullanıcı döndürürse sessiz başarı kabul edilmiyor.

## Yerel doğrulama

Son kaynakla testDebugUnitTest çalıştı: 644 test, 0 hata, 0 atlanan. Yeni AuthOperationRunnerTest altı senaryo içeriyor: dispatch öncesi çift gönderim; hata ve yeniden deneme; iptalin hata olmaması; eski geciken hatanın yeni kilidi açmaması; eski geciken başarının yayımlanmaması; kapanmış/iptal scope'un yeni işe izin vermemesi. Kuyruk denetlenebilir yerel CoroutineDispatcher kullanıyor; gerçek servis ve parola kullanılmıyor.

Üretim Kotlin derlemesi geçti. Bu tur cihaz testleri tekrarlanmadı; önceki form testlerinin sonuçları yeni SDK/ViewModel uçtan uca kabulü sayılmıyor. Git diff --check temiz.

## Açık kalan işler ve sonraki sıra

1. Parola sıfırlama ekranı şu anda isteği başlatır başlatmaz successSent=true yapıyor. Başarı ekranı yalnız servis onayından sonra gösterilmeli; hata, tekrar deneme ve IME gönderimi test edilmeli. Bu doğrulanmış kaynak hatası sıradaki düzeltmedir.
2. Firebase Auth listener'ı kayıt sırasında kullanıcı oluştuğunda Authenticated yayımlayabiliyor; profil yazımının tamamlanmasını beklemiyor. Activity de currentUser üzerinden yönlendiği için yalnız listener'ı susturmak yeterli değildir. AuthViewModel/Activity/profil hazırlığı birlikte demo SDK testinde ele alınmalı.
3. Coroutine iptali Firebase SDK Task'ını geri almaz. Bu koruma yerel işlem callback'lerini sınırlar; çıkıştan sonra geciken SDK oturum değişimi ve Auth listener hesabı ayrıca test edilmeli.
4. Form Activity yenileme, gerçek telefon/yatay görünüm ve canlı Firebase giriş/kayıt kabulü açık.

Canlı Firebase, hesaplar, üretim servisleri ve kişisel Gradle ayarları değiştirilmedi. Bu tur commit/push yapılmadı.