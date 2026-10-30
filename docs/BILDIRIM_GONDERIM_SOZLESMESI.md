# Backend bildirim gönderim takibi

30 Ekim 2026. Yalnızca yerel olarak doğrulandı; canlı migration/FCM doğrulaması yapılmadı.

Migration: `0017_push_deliveries`. Aynı Alert kimliği, backend kullanıcı kimliği ve cihaz tokenının SHA-256 özeti için tek kayıt tutulur. Ham token ve bildirim içeriği bu tabloda saklanmaz. Token silinip aynı değerle yeniden kaydedilse de takip kaydı korunur. Hesap veya Alert silinince takip kaydı cascade ile temizlenir.

| Durum | Anlamı | Otomatik tekrar |
|---|---|---|
| reserved | Dış çağrıdan önce diske yazılan hak; çökmede sonuç bilinmeyebilir | Yok |
| sent | Sağlayıcı çağrısı başarı bildirdi; cihaz teslim kanıtı değildir | Yok |
| unknown | Sağlayıcı başarısız/belirsiz döndü veya exception oluştu | Yok |
| cancelled | Son kontrolde silme, token sahipliği, süre veya tercih engeli görüldü | Yok |

Gönderim kendi SQL oturumunda çalışır. Çağıranın ilgisiz değişikliklerini commit etmez; çağıranın rollback'i kaydı silmez. Girdi Alert ve hesap/konum/token verileri önceden commit edilmiş olmalıdır. Gönderim hakkı unique constraint ve atomik insert ile alınır; kayıt diske yazılmadan sağlayıcı çağrılmaz. Sonrasında kullanıcı silme durumu, token sahipliği, uyarı geçerliliği ve tercihler yeniden okunur. Kullanıcı ve token satırları sağlayıcı çağrısı boyunca kilitlidir.

Bu sözleşme aynı uyarı/cihaz için uygulama seviyesinde tek sağlayıcı çağrısı denemesi sağlar. FCM/SDK iç tekrarlarını veya cihazda tam bir kez görünmeyi garanti etmez. Claim sonrası çağrıdan önce çökme olursa bildirim hiç ulaşmayabilir. FCM yanıtı kaybolursa gönderilmiş de olabilir. Bu nedenle reserved/unknown kayıtlar körlemesine tekrar açılmamalıdır. Kritik uyarılarda hem güvenilir yeniden deneme hem mükerrer görünmeme için sabit olay kimliği ve cihaz tarafında dedup protokolü ayrıca gereklidir.

Farklı Alert kimlikleriyle yeniden üretilen aynı meteorolojik olay bu kapsamda birleşmez. Gerçek harici sağlayıcının olay kimliği/ingestion dedup sözleşmesi henüz yoktur. Eski gönderimlerin geçmişi geriye dönük bilinmediği için migration öncesi bildirimler için dedup garantisi verilmez. Üretim kuyruğu devreye alınırken eski işler ayrıca gözden geçirilmelidir.

Doğrulama: süreç/oturum yeniden açılışı, belirsiz hata ve process crash, token tekrar kaydı, çok cihaz, silmenin claim sonrası başlaması, caller rollback/connection-bound session, hesap cascade temizliği. İzole PostgreSQL testinde 16 paralel kuyruk tek sağlayıcı çağrısı yaptı.
