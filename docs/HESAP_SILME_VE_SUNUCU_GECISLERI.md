# Hesap silme ve sunucu geçişleri

## Durum

Bu değişiklikler yerel doğrulama içindir. Canlı Firebase, üretim veritabanı veya gerçek kullanıcı hesabı üzerinde işlem yapılmadı. `ACCOUNT_DELETION_ENABLED` varsayılan olarak kapalıdır. Android önce hizmetin hazır olup olmadığını kontrol eder; hazır değilse kullanıcı hesabını değiştirmez.

## Hesap silme protokolü

1. Android hizmetin kullanılabilirliğini kontrol eder, kullanıcıdan aldığı şifreyle Firebase oturumunu yeniden doğrular.
2. Cihaz 256 bit rastgele bir devam anahtarı üretir. UID ve bu anahtar, yedeklemeye alınmayan uygulama özel dizininde atomik olarak kaydedilir. Şifre ve Firebase token'ı kaydedilmez.
3. `POST /v1/account/deletion` ilk istekte son beş dakika içinde doğrulanmış Firebase kimliği ister. Sunucu anahtarın yalnızca SHA-256 özetini, UID/proje/bucket ve iş durumunu saklar. E-posta eşleşmesiyle başka bir backend hesabı silinmez.
4. Sunucu Firestore'a sadece Admin SDK'nin yazabildiği bir silme işareti koyar, Firebase hesabını devre dışı bırakır ve oturumlarını iptal eder. Güncel kurallar eski token ile profil/fotoğraf oluşturulmasını engeller.
5. `profile-images/{uid}/` altındaki Storage nesneleri, Firestore `users/{uid}` belgesi ve tüm alt koleksiyonları, backend avatar dosyaları ve bağlı SQL kayıtları temizlenir. Başarısız toplu Firestore silmeleri başarı sayılmaz.
6. Firebase Auth kaydı en son silinir. Başarıyla tamamlanan işten UID/proje/bucket çıkarılır. Yarım kalan iş aynı devam anahtarıyla, Firebase kimliği silinmiş olsa bile yeniden denenebilir.
7. Android sunucu tamamlanmasını cihazda kaydeder; oturumu kapatır, ekran işlerini durdurur, Firestore yerel önbelleğini, kullanıcıya ait Room kayıtlarını ve DataStore ayarlarını, hava/görsel önbelleklerini ve görünen bildirimleri temizler. Yerel temizlik yarıda kalırsa sonraki açılışta internete gerek olmadan tekrar denenir.

`200 + complete` dışındaki cevaplar silme başarısı olarak gösterilmez. `202 + pending` kabul edilmiş ancak bitmemiş işi; `401` yeni kimlik doğrulaması gerektiğini belirtir. Devam anahtarı bu silme işi için yetki taşır; destek/log/analitik sistemlerine gönderilmemelidir.

## Daha sonra yapılacak dağıtım sırası

1. Üretimden ayrı test ortamında veritabanı yedeğini ve geri dönüş yöntemini doğrulayın.
2. Backend dizininde `python -m alembic upgrade head` çalıştırın. Geçişler çevrimiçi şema incelemesi kullanır; `--sql` çıktısı ile uygulanmaz. Yeni head: `0013_profile_fields`.
   - `0011`: kalıcı hesap silme işleri.
   - `0012`: cihaz token'ı tekilliği. Yinelenen token varsa veri silmeden hata verir; çakışma önce çözülmelidir. Kullanıcı/platform tekilliği kaldırılır, aynı kullanıcının birden çok cihazı korunur.
   - `0013`: daha önce yalnızca ORM'de bulunan avatar/kişiselleştirme alanları. İstek başına DDL kaldırılmıştır; dağıtım sırasında migration zorunludur.
3. Firebase Admin kimlik bilgilerini sunucunun güvenli ortamına tanımlayın. `FIREBASE_PROJECT_ID` ve `FIREBASE_STORAGE_BUCKET` Android'in kullandığı projeye ait olmalıdır. Bekleyen işler varken bu eşleştirmeyi değiştirmeyin.
4. Repodaki Firestore ve Storage kurallarını doğru test projesine dağıtın. Storage'ın Firestore silme işaretini okuyabilmesi için ürünler arası erişim yetkisini etkinleştirin. Eski kurallarla silme özelliğini açmayın.
5. `python -m app.services.resume_account_deletions` komutunu sunucudaki zamanlayıcıda örneğin her beş dakikada bir çalıştırın. Komut yeni silme talebi başlatmaz; yalnızca önceden yetkilendirilmiş bekleyen işleri yürütür. Çıkış kodu 1 ise kalan hataları araştırın. Bu zamanlayıcı burada kurulmadı.
6. Backend avatar dizinini kalıcı ve yazılabilir bir volume üzerinde tutun. Birden çok backend örneğinde aynı dosya alanını kullanın; aksi halde diğer örnekteki avatarı bu süreç silemez.
7. Hizmetleri doğruladıktan sonra `ACCOUNT_DELETION_ENABLED=true` yapın. Silinebilir test hesabıyla kesintili ağ, yeniden açılış, eski token ile tekrar yazma, Storage hatası ve SQL ilişkilerini test edin. Sonra Android sürümünü dağıtın.

## Saklanan sınırlı kayıtlar ve kapsam

- Firestore `account_deletions/{uid}` işareti eski oturumların veri oluşturmasını engellemek için kalır. Tamamlanmış SQL işinde yalnızca rastgele anahtarın özeti ve tamamlanma durumu kalır. Bunlar profil/analiz içeriği değildir; gizlilik metninde saklama kapsamı doğru açıklanmalıdır.
- Storage soft-delete/nesne sürümleri, veritabanı yedekleri, erişim logları, Crashlytics ve harici AI sağlayıcısı kayıtlarının saklama süreleri altyapı/sağlayıcı ayarlarına bağlıdır. Bu kod geçmiş yedekleri yok etmez.
- Mevcut kodun ayrı backend e-posta hesabını Firebase hesabına güvenli şekilde bağlayan bir akışı yoktur. UID ile eşleşmeyen hesabı otomatik silmek yerine güvenli hesap bağlama/destek süreci gerekir.
- Diğer cihazların yerel önbellekleri uzaktan fiziksel olarak temizlenmez. Kurallar sunucu erişimini keser; kullanıcılar arası görünürlük cihazdaki oturum izolasyonuna dayanır.
- Android geçmişinde doğrulanabilen hava veritabanı 9→10→11→13→14→15→16→17 ve bildirim 3→4→5 yolları desteklenir. Daha eski/bilinmeyen şemalar ve bozuk disk dosyaları için ayrıca kurtarma stratejisi gerekir; kullanıcı verisi sessizce silinmez.

## Yerel kontroller

```text
# backend
python -m pytest -q
python -m ruff check app tests

# mobile/android; bağlı test cihazıyla
gradlew.bat testDebugUnitTest assembleDebug lintDebug connectedDebugAndroidTest --no-daemon --max-workers=2
```

Android geçiş testleri `migration_test_*` adında ayrı veritabanlarını kullanır. Canlı Firebase silme, gerçek hesap oturumu veya mağaza yayını bu kontrollerin parçası değildir.

## Teknik kaynaklar

- [Storage kurallarında Firestore koşulları](https://firebase.google.com/docs/storage/security/rules-conditions)
- [Android Firestore terminate / clearPersistence](https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/FirebaseFirestore)
