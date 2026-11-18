# 18 Kasım — Sunucudan premium profil senkronizasyonu

## Kapsam

Gerçek Android Firebase SDK ve üretimde kullanılan UserProfileRepository ile sunucu üyelik aynasının dinleyiciye geçişi incelendi. Ayrı FirebaseApp, demo-havamania-audit projesi, yerel Auth/Firestore emülatörleri ve QA paketi kullanılıyor. Gerçek ödeme, SQL abonelik akışı veya canlı Firebase bu testin kapsamında değil.

Yeni çalıştırma komutu: tools/firebase-rules-tests içinde npm run test:android:profile. Yerel yardımcı yalnızca 127.0.0.1:9396 üzerinde, koşuya özel nonce ile ve beş sabit üyelik senaryosu için yazıyor. Arbitrary belge/alan yazımı, servis anahtarı veya üretim bağlantısı yok. ADB yönlendirmelerinin yalnız koşunun eklediği kısmı kaldırılıyor.

## Kabul senaryosu

1. Ücretsiz profil sunucudan doğrulanır.
2. Yetkili yerel fixture isPremium=true yazar; gerçek profil dinleyicisi true alır.
3. İstemci normal profil DTO merge yapar; sunucunun true değeri korunur. İstemcinin false yazması reddedilir.
4. Eski premium=true alanına geçilir; dinleyici yenilenerek ham alanın doğru okunduğu doğrulanır.
5. İki alan çelişirse profil ve doğrulanmış şehir temizlenir, güvenli hata durumu oluşur.
6. Geçerli false kayıtla düzelme, tekrar true ve sunucudan false ile üyeliğin geri alınması doğrulanır.
7. Yanlış tür güvenli profil hatasına dönüşür; sunucu düzeltince aynı dinleyici toparlanır ve normal profil adı korunur.

## Açık işler

Canlı üyelik bayraklarının yetkili abonelik kaynağıyla denetimi ve güncellenmesi, eski Android sürüm yazım uyumluluğu ve gerçek mağaza satın alma/iptal/webhook akışı açık. Test yalnız repository dinleyicisini kapsar; ücretli hesabın tam ayarlar ekranında görsel kabulü ve gerçek ödeme yetkilendirmesi ayrıca doğrulanmalı. Canlı kurallar dağıtılmadı.
## Sonuç

Özel sunucu senaryosu bağlı Samsung SM-T500 Android 12 üzerinde geçti: XML 1 test / 0 atlanan / 0 hata. Test APK derlemesi başarılı. İlk koşularda fixture sunucuda eski alanı doğru yazmasına rağmen hemen yapılan tekil SDK okuması eski belgeyi döndürdü. Test, bekleyen istemci işlemlerinin bitmesini ve beklenen ham alanların sunucudan dinleyiciye ulaşmasını koşullu olarak bekleyecek şekilde düzeltildi; sabit uyku veya hata yutma kullanılmadı. Uygulama üretim kaynaklarında bu tur ek değişiklik gerekmedi; önceki düzeltmenin sunucu tarafıyla kabulü tamamlandı.

Önceki 631 JVM ve 34 kural testi sonucu korunuyor; bu tur üretim kaynağı/kurallar değişmediğinden yeniden çalıştırılmadı. Bu sonuçlar tam UI veya gerçek ödeme kabulü değildir.

Normal SDK regresyon koşusu da geçti: XML 17 test / 4 özel mod atlandı / 0 hata; 13 aktif senaryo başarılı. Özel premium senaryosu yukarıdaki ayrı koşuda 1/1 geçti. Geçici ADB yönlendirmeleri koşu sonunda temizlendi. git diff --check ve Node sözdizimi kontrolü geçti.
