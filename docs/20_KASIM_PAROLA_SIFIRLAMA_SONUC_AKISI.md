# 20 Kasım Parola sıfırlama sonuç ve yeniden deneme düzeltmesi

Parola sıfırlama ekranının istek başlatılır başlatılmaz başarı göstermesi giderildi. AuthViewModel yalnız sendPasswordResetEmail tamamlandıktan sonra PasswordResetSent yayımlıyor; ekran başarı içeriğini bu durumdan üretiyor. Yerel successSent=true ataması kaldırıldı.

## Son davranış

İstek sürerken form ve düğme devre dışı kalır; başarı mesajı gösterilmez. Hata gelirse form açılır ve girilen e-posta korunur. Hata kapatılabilir ve tekrar gönderilebilir. E-postanın kenar boşlukları gönderimde temizlenir; yalnız boşluk içeren değer gönderilmez. Klavyedeki Bitti düğmesi ile ekrandaki gönderim aynı kontrolü kullanır; gönderimde odak ve klavye kapanır.

E-posta alanı rememberSaveable kullanır; başarı durumu ViewModel'de tutulur. Geri eylemi tamamlanmış durum/hata bilgisini temizleyerek dışarı çıkar. İstek devam ederken clearError mevcut işlem kilidini kaldırmaz. Tam Activity yeniden oluşturma ve sistem geri hareketinin tüm zamanlamaları bu koşuda ayrıca sınanmadı.

## Test sonucu

Son kaynakla 644 JVM testi yeniden çalıştı: sıfır hata. Samsung SM-T500 Android 12 üzerindeki ayrı QA uygulamasında son AuthFormsUiTest koşusu 6 test, 0 hata, 0 atlanan verdi. Dört mevcut giriş/kayıt/karşılama senaryosu ve iki yeni parola sıfırlama senaryosu çalıştı.

Yeni senaryolar 320×640 dp kısıtlı görünümde yüzde 200 yazıyla bekleme → hata → hata kapatma → yeniden deneme → onaylı başarı → geri callback akışını; normal yazı boyutunda boş adresin IME ile reddini ve gerçek sistem klavyesinin gönderimde kapanmasını kapsıyor. Üretim formu yerel callback'lerle sınandı; gerçek Firebase sıfırlama e-postası gönderilmedi. Bu canlı SDK/e-posta teslimi kabulü değildir.

İlk yeni cihaz testi koşusunda devre dışı alan düzenleme eylemi üzerinden arandığından iki test başarısız oldu. Seçici E-posta etiketinden alanı doğrulayacak şekilde düzeltildi; son koşuda altı testin tamamı yeniden çalışıp geçti. Git diff --check temiz.

## Sıradaki sıra

1. Kayıtta Auth listener, profil belgesinin hazırlanması ve Activity yönlendirmesini birlikte incele. Kullanıcı SDK'da oluştuğunda profil yazımı henüz bitmeden Authenticated/currentUser üzerinden geçiş yapılabilmesi açık.
2. Çıkış sonrası geciken SDK Task/oturum değişimi kabulü; yerel coroutine iptali uzak SDK işlemini geri almaz.
3. Formların Activity yenileme, gerçek telefon/yatay görünüm ve canlı Firebase kabulü.

Bu tur canlı hesap, Firebase ayarı veya Play paketi değiştirilmedi; commit/push yapılmadı.