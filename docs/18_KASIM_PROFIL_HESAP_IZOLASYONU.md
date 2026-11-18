# 18 Kasım — Profil A–B hesap izolasyonu

## Yapılan çalışma

Üretimde kullanılan UserProfileRepository için gerçek Android Firebase SDK ile A→B→A hesap geçişi senaryosu eklendi. Yalnız demo Auth/Firestore emülatörü, ayrı FirebaseApp ve QA uygulaması kullanıldı. Canlı hesap veya ödeme sistemi değiştirilmedi.

Repository dinleyicisine testte kontrollü Executor verilebiliyor. Varsayılan yine ana iş parçacığı; üretimdeki callback iş parçacığı değişmedi. Mevcut UID ve listenerVersion korumaları korunuyor. Firestore sağlayıcısı son constructor parametresi bırakıldı; mevcut trailing-lambda test enjeksiyonları aynı şekilde ayrı demo FirebaseApp kullanıyor.

## Senaryo

- A hesabında sunucudan premium onayı, Ankara şehri ve profil adı okunur.
- Gerçek SDK profil güncelleme olayı Executor kuyruğunda bekletilir.
- Gerçek Auth çıkışı ve ayrı B oturumu açılır; B profili Bursa ve ücretsizdir.
- B seçildiğinde eski profil ve sunucu şehri anında temizlenir. Gecikmiş SDK olayları B seçildikten sonra çalıştırılır. Geçiş boyunca yayımlanan dolu profil değerlerinin tümünün B'ye ait ve ücretsiz olduğu kontrol edilir.
- Eski A kaydı sunucudan bozulur; B'nin yeni adı ve üyelik bilgisi etkilenmez. B'nin sunucudan A belgesini okuma girişimi PERMISSION_DENIED alır.
- Dinleyici durdurulunca B üyelik aynası güncellense de repository Idle/null kalır.
- A'nın gerçek emülatör giriş bilgileriyle tekrar giriş yapılır. Bozuk A güvenli hata verir; sunucu düzeltince yalnız A'nın adı/şehri ve ücretsiz üyeliği yüklenir.

SDK kaldırılmış dinleyici olaylarını kendi içinde de bastırabilir. Bu kabul gerçek SDK + repository birlikte davranışını ölçer; SDK'nın bastırdığı callback'in repository içine zorla girdiğini iddia etmez. Bu, tam login/UI/theme akışının görsel kabulü değildir; o katman ve gerçek çok cihazlı/canlı kabul açık.

## Sonuç

631 JVM testi yeniden çalıştırıldı, sıfır hata. İlk hesap geçişi senaryosu ve önceki sunucu üyelik senaryosu birlikte 2/2 geçti. Geçiş boyunca tüm profil yayınlarını izleyen güçlendirilmiş son koşu da 2/2 geçti: XML 2 test / 0 atlanan / 0 hata. Profil izolasyonunda yeni bir üretim hatası bulunmadı; mevcut UID/dinleyici koruması gerçek SDK olayıyla doğrulandı.

## Sonraki yerel iş

Tam profil/ayarlar tüketicisinin oturum değişimi ve yeniden oluşturulma sırasında üyelik, ad ve şehir tutarlılığı. Canlı premium aynası denetimi, kuralların dağıtımı, gerçek satın alma/iptal kabulü ve çok cihaz testi açık kalır.

Son normal SDK regresyonu: XML 18 test / 5 özel mod atlandı / 0 hata; 13 aktif senaryo geçti. İki profil özel modu yukarıdaki ayrı koşuda 2/2 geçti. Geçici ADB yönlendirmeleri kaldırıldı. git diff --check geçti. Kurallar değişmediğinden 34 kural testi bu tur yeniden çalıştırılmadı.
