# 24 Kasım — Backend Firebase kimlik iptali hata yönetimi

## Giderilen sorun

Firebase Admin verify_id_token çağrısı zaten check_revoked=True kullanıyordu. Ancak iptal kontrolünde kullanıcı Firebase'den silinmişse ortaya çıkan UserNotFoundError yakalanmıyordu. Korumalı endpoint bu durumda kimlik reddi yerine sunucu hatası üretebilirdi. Bu hata artık diğer geçersiz kimlik durumlarıyla birlikte 401 ve genel Invalid Firebase credentials cevabına dönüştürülüyor. Mevcut backend kullanıcı kaydı silinmiyor; başarısız kimlikten yeni kullanıcı oluşturulmuyor.

## Doğrulama

- Kimlik ve giriş paketi: 12 test geçti.
- Hesap erişimi ve hesap silme regresyonları: 19 test geçti.
- Yeni beş kabul durumu: önce kabul edilen token için revoked/disabled/not-found/expired sonuçlarıyla sonraki profil erişiminin reddi; ilk erişimde bulunamayan Firebase kimliğinden backend hesabı oluşturulmaması.
- Test gerçek FastAPI bağımlılık zincirini, token doğrulama sarmalayıcısını ve izole SQLite veritabanını kullanır. Firebase Admin verify_id_token sınırındaki sonuçlar kontrollüdür; her çağrıda check_revoked=True ve proje uygulaması parametresi doğrulanır. SDK özel hata metni cevaba aktarılmaz.
- İlk test denemesinde ExpiredIdTokenError test nesnesinin zorunlu cause argümanı eksikti; test düzeltildikten sonra 12/12 geçti. Bu uygulama hatası değildi.

## Açık kapsam ve sıradaki

Bu çalışma gerçek revoke_refresh_tokens/validSince emülatör entegrasyonu, Android token yenilemesi veya canlı Firebase kabulü değildir. Bunlar sırada korunuyor. Backend dağıtımı yapılmadı; çalışan sunucu ancak bu sürüm dağıtıldıktan sonra düzeltmeyi alır. Android kaynakları bu adımda değiştirilmedi ve önceki 688 JVM testi yeniden çalıştırılmadı. Önceki Activity kararlılığı ve mağaza kapıları açık. Push yapılmadı.