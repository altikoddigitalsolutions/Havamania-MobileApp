# 23 Kasım Kayıt hazırlığı ve erken yönlendirme düzeltmesi

AuthViewModel artık yönlendirme için ham Firebase kullanıcısını doğrudan vermiyor. currentUser yalnız Authenticated durumundaki, SDK UID'siyle eşleşen ve kayıt hazırlığı tamamlanmış kullanıcıyı döndürüyor. İşlem veya yarım kayıt hazırlığı sürerken Auth listener erken Authenticated yayımlayamıyor. Mevcut kayıtlı oturumla soğuk açılışın başlangıç durumu korunuyor.

## Yapılan değişiklikler

RegistrationPreparation hesap oluşturma → ad güncelleme → profil yazma sırasını yönetiyor. Profil yazımı başarıyla tamamlanmadan hazırlık kilidi kalkmıyor. Aynı ViewModel oturumunda profil yazımı başarısız olduğunda aynı e-posta/UID ile tekrar deneme mevcut SDK hesabını kullanıyor; hesabı yeniden oluşturmuyor. SDK hesabı oluşmuş fakat create await sonucu kaybolmuşsa aynı e-posta oturumu da tekrar kullanılabiliyor. Parola hazırlık nesnesinde saklanmıyor.

Hesap oluşmadan normal oluşturma hatası gelirse hazırlık serbest bırakılıyor; kullanıcı adresi düzeltebiliyor. Hesap oluştuktan sonra farklı adresle devam edilmesi veya UID değişimi reddediliyor. Her askıya alınan aşamadan sonra iptal ve güncel hesap kontrolü var. Eski hazırlık temizlendiyse geç sonuç hazır olarak yayımlanamıyor.

Profil yazımı transaction içinde SetOptions.merge kullanıyor. Mevcut alanların tümünü silen set kaldırıldı; createdAt varsa korunuyor, yoksa ekleniyor. Transaction SDK sahibini doğruluyor. Profil ViewModel'inin eşzamanlı oluşturmuş olabileceği kayıtla birlikte bu kaynak değişikliği premium, tercihler ve diğer alanların silinmesini önlemeyi amaçlıyor; gerçek SDK/kurallar altında birleşik kabulü henüz yapılmadı.

Kayıt ekranının üst geri düğmesi, sistem geri eylemi ve girişe geçişi yarım kayıt varsa oturumu kapatıyor ve yerel işlem kimliğini geçersiz kılıyor. Hesap silinmiyor. Bu ekran eylemlerinin gerçek cihazdaki zamanlamaları ayrıca doğrulanmalı.

## Yerel test sonucu

Son kaynaklarla Kotlin derlemesi ve testDebugUnitTest geçti: 653 test, 0 hata, 0 atlanan. Yeni RegistrationPreparationTest dokuz senaryo içeriyor:

- Profil yanıtı gecikirken kilidin korunması ve işlem sırası.
- Profil hatasından sonra aynı hesabı tekrar kullanma.
- Yarım kayıtta farklı adresin reddi.
- Ad güncellemesi sırasında hesap değişirse eski profile yazmama.
- Hazırlık temizlendikten sonra hazır sonucu yayımlamama.
- İptal sonrası aynı oturumda tekrar deneme.
- Hesap oluşmuş fakat oluşturma sonucu alınamamışken toparlanma.
- İlgisiz mevcut oturumda yeni kayıt başlatmama.
- Hesap oluşmadan hata sonrası adres düzeltme.

Testler üretimde kullanılan hazırlık sınıfını yerel coroutine/callback'lerle çalıştırıyor; AuthViewModel/Firebase SDK/Activity için uçtan uca test değildir. Bu tur cihaz testi veya canlı hesap işlemi yapılmadı. Git diff --check temiz. Kişisel gradle.properties değiştirilmedi; commit/push yapılmadı.

## Açık kalan kabul ve sonraki sıra

1. Demo Firebase Auth/Firestore ile gerçek AuthViewModel, profil transaction'ı ve Activity yönlendirmesini birleştir. Geciken profil yanıtında kayıt ekranının korunmasını, retry'da tek hesabı ve mevcut profil alanlarının kalmasını doğrula. Yeni oluşturulan navController'ın kişiselleştirme/hedef rota seçimi de bu kabulün parçası.
2. RegistrationPreparation yalnız ViewModel ömründe tutuluyor. Süreç tamamen kapanınca yarım hazırlık işareti korunmuyor; SDK oturumu yeniden açıldığında erken hazır sayılmaması için kalıcı takip/toparlanma açık.
3. Coroutine iptali SDK Task'ını veya başlamış Firestore yazısını geri almaz. Çıkıştan sonra geç SDK oturum değişimi ve sunucu yazımı ayrıca sınanmalı.
4. Gerçek geri hareketi, Activity yeniden oluşturma ve tam kayıt/giriş formu cihaz regresyonu açık.