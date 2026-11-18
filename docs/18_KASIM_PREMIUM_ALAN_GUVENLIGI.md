# 18 Kasım — Premium profil alanı güvenliği

## Sorun ve değişiklik

Android UserProfile içindeki isPremium Boolean özelliğinin SDK alan adı çıkarımına bırakılması, eski premium alanıyla tutarsızlık yaratabiliyordu. Kullanıcı profil kuralları iki alanın da istemciden değiştirilmesini engellemiyordu. Backend ücretli API yetkisini SQL aboneliğinden belirliyor; Firestore değeri yalnızca arayüz aynasıdır.

- UserProfile üyelik özelliği @get:Exclude ile profil yazımlarından çıkarıldı. Normal profil kaydı üyelik alanını göndermez.
- Repository ham belge üzerinden isPremium ve eski premium alanlarını açıkça okuyor. Eksik alan ücretsiz; yalnız Boolean değerler kabul ediliyor. İki alan çelişirse veya tür yanlışsa güvenli profil hata durumu gösteriliyor; uzak belge otomatik değiştirilmez.
- Önbellek veya bekleyen yerel yazı yeni üyelik onayı sayılmaz. Aynı dinleyicinin son sunucu onayı korunur; yeni hesap/dinleyici veya hata durumunda sıfırlanır.
- Yerel Firestore kuralları istemcinin üyelik vermesini, mevcut aynayı değiştirmesini/kaldırmasını engelliyor. Ücretsiz eski istemciler için eksik false alanı başlangıcı ve diğer profil düzenlemeleri destekleniyor.

## Doğrulama

- 631 JVM testi: hata yok; dokuz yeni alan/önbellek/çelişki testi dahil.
- 34 yerel Firestore/Storage kural testi: tamamı geçti; yedi yeni premium koruma senaryosu dahil.
- Bağlı Samsung SM-T500 Android 12 üzerinde 13 Android Firebase SDK senaryosu geçti. XML 16 test / 3 özel mod atlandı / 0 hata. Yeni test gerçek SDK serializer alan dışlama, iki alias için reddedilen yerel grant, bekleyen yazıda premium görünmemesi ve normal profil düzenlemesini kapsıyor. Yalnız demo Auth/Firestore ve ayrı FirebaseApp kullanıldı.

## Açık yayın koşulları

Canlı Firebase kuralları dağıtılmadı, canlı kullanıcı veya abonelik değiştirilmedi. Tarihsel üyelik alanları önceki kurallarla istemciden yazılabildiğinden, canlı geçiş öncesinde yetkili SQL abonelik kaynağıyla denetim ve düzeltme gerekir; mevcut true değeri tek başına satın alma kanıtı değildir. Çelişen alanlar yetkili süreçle düzeltilmeli.

Eski sürümler sunucudaki true alanını false ile geri yazarsa profil kaydı reddedilebilir; eski sürüm uyumluluğu dağıtım öncesi değerlendirilmelidir. Backend üretim receipt/webhook doğrulaması bu değişiklikte etkinleştirilmedi. Gerçek ücretli üyelik uçtan uca kabulü ve sunucu ayna güncelleme süreci açıktır. Devam koşusunda yetkili demo fixture tarafından yazılan true/false, eski alan, çelişki ve yanlış türün Android dinleyicisine geçişi gerçek SDK ile doğrulandı. Bu yalnız yerel emülatör kabulüdür; canlı abonelik/ödeme kabulü açık. Ayrıntı: 18_KASIM_PREMIUM_SUNUCU_SENKRONIZASYONU.md.