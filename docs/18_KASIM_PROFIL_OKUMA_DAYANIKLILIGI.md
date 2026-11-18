# 18 Kasım — Profil okuma güvenliği ve hata penceresi

## Bulunan sorunlar ve düzeltmeler

UserProfileRepository Firestore callback'inde toObject dönüşümünü yakalamıyordu; yanlış alan türü RuntimeException üretip callback dışına taşabilirdi. Hata ve bulunamayan profil aynı null sonucu veriyordu. ProfileViewModel null durumda ensureRemoteProfile yoluna gidiyordu; ilk/cache okuması, veri hatası ve gerçek yokluk ayrılmıyordu. Ayrıca hata sonrası serverDefaultCity eski doğrulanmış değeri tutabiliyordu. Aynı UID için dinleyici nesnesi durduğu sürece refresh tekrar dinleyici kurmuyordu.

ProfileReadState ile Idle/Loading/Ready/Missing/Failed ayrıldı. Dönüşüm hatası yakalanır, ham veri/exception mesajı gösterilmez. Başka UID taşıyan profil reddedilir. UID alanı olmayan eski/yarım belge yalnız doğru belge yolu UID'siyle bellekte tamamlanır; uzak belge yazılmaz. Gerçek coroutine iptali yutulmaz. Hata/yoklukta profil ve eski authoritative default city temizlenir. Eksik cache görüntüsü Loading, yalnız sunucudan onaylı yokluk Missing olur.

ProfileViewModel yalnız Missing için mevcut create-if-missing transaction yolunu çalıştırır. Loading veya Failed durumunda profil oluşturulmaz. Veriler uygunsa Success ve profil snapshot kaydı kullanılır; hata anlaşılır ProfileState.Error'a çevrilir. Açık refresh forceRefresh ile aynı UID'nin eski listener'ını kaldırıp yeni version altında tekrar kurar. Repository'ye gecikmeli Firestore sağlayıcısı enjekte edilebilir; normal singleton varsayılan SDK'yı kullanmaya devam eder, testler ayrı demo SDK kullanır. ThemeViewModel geçerli aynı hesap profili kalmadığında eski premium görünüm bayrağını korumaz.

Profil hata penceresinde eski Tamam/dışarı dokunma davranışı refresh çağırıyordu. Artık Tekrar dene ve Vazgeç ayrıdır; kapatma ağ isteği başlatmaz. İlgisiz sabit internet uyarısı kaldırıldı; veri hatası, erişim reddi ve bağlantı hatası kendi güvenli açıklamasını kullanır. Kapatmada hâlâ doğrulanmış aynı hesap profili varsa yalnız bu önbellek görünümü geri alınır; başka hesap profili kullanılamaz. Yerel dismiss durumu owner/message ile ayrılır.

## Doğrulama

Son tam JVM XML: 622 test, 0 hata/atlama. Altı yeni test geçerli profil, eksik UID'nin yalnız bellekte tamamlanması, yanlış UID reddi, dönüşüm hatasında güvenli açıklama, null decode'un Missing sayılmaması ve gerçek cancellation yayılımını kapsar.

Normal Android SDK emülatör koşusu XML: tests=15, failures=0, errors=0, skipped=3. On iki normal senaryo geçti; ProcessRestart/TCP/ReplyLoss özel modları planlı atlandı, bu tur tekrar koşulmadı. İki yeni SDK testi gerçek callback'e name alanında yanlış tür gönderir: bozuk profil çökme yerine Failed verir, eski profil/default city temizlenir ve uzak bozuk değer değiştirilmez. Alan düzeltildiğinde aynı listener Ready'ye döner. Başka UID erişimi güvenli erişim hatası verir. UID'siz kısmi profil doğru path sahibine okunur; uzak UID alanı eklenmez. Sağlayıcı sayacı aynı UID için normal çağrıda listener çoğalmamasını, forceRefresh'te yeni listener kurulmasını doğrular. Yanlış UID alanı reddedilir ve uzakta otomatik düzeltilmez; stopObserving Idle'a döner.

İlk SDK denemesinde isPremium adlı alanla beklenen dönüşüm hatası oluşmadığı için bir test 30 saniye bekleme sınırına takıldı. Testin alan eşleme varsayımı düzeltilip kesin DTO metin alanı name için yanlış tür kullanıldı; son tüm SDK koşusu geçti. isPremium/premium Boolean alan eşlemesi ve istemci yazma yetkisi ayrı inceleme maddesi olarak korunur; bu tur canlı şema veya premium alan sözleşmesi tahminle değiştirilmedi. Firestore Kotlin alan adlandırma referansı: [PropertyName](https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/PropertyName).

Üç izole Compose profil hata testi SM-T500 üzerinde 3/3 geçti: açık tema/200% yazıda kapatma retry çağırmaz; koyu tema/200% yazıda retry ve vazgeç ayrıdır; aynı kompozisyonda owner değişince eski dismiss yeni hata penceresini gizlemez. Firebase hesabı kullanılmayan component fixture testleridir; gerçek tüm ProfileScreen/Activity, süreç sonrası saved-state veya tam login senaryosu değildir.

Debug derleme ve diff kontrolü geçti. Son UI/XML sonuçları SDK sonucunu dosyada değiştirdiği için SDK 15/0/3 özeti UI koşusundan önce ayrıca doğrulandı; konsolun şişmiş test sayısı yerine XML esas alındı. Emülatörler kapandı; geçici reverse eşlemeleri kaldırıldı.

## Kalan sıra

isPremium/premium alan sözleşmesi ve güvenlik yetkileri öncelikli ayrı inceleme; tam Activity/login/profil hata kurtarma kabulü, UI saved-state/account birleşimi, eksik cache ile uzun offline davranış, Android hesap silme uçtan uca ve önceki yayın kapıları açık. Normal SDK testleri production repository/store yardımcılarını gerçek emülatörde kullanır, tüm ProfileViewModel'i oluşturmaz. Canlı kurallar/servisler değişmedi; push/release/canlı dağıtım yapılmadı.