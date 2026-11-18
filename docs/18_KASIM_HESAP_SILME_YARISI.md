# 18 Kasım — Hesap silme işareti ve şehir transaction yarışı

## Yeni kabul senaryoları

Yerel Firebase kural testlerine üç kontrollü yarış eklendi. Yetkili owner JS SDK transaction'ı şehir belgesini önce başarıyla okur; callback bir Promise bariyerinde bekler. Bundan sonra yalnız rules-unit-testing withSecurityRulesDisabled bağlamı üzerinden demo emülatörüne account_deletions/owner işareti yazılır. Son senaryoda aynı yerel admin bağlamı şehir belgesini de temizler. İşaretin yazısı tamamlanmadan eski transaction devam ettirilmez.

- delete: Önceden okunan şehir için silme commit'i permission-denied alır; mevcut şehir bozulmaz.
- create: Önce yokluğu okunan şehir için oluşturma commit'i permission-denied alır; belge oluşturulmaz.
- recreate-after-cleanup: Okuma sonrasında işaret konup şehir admin tarafından temizlenir; eski istemcinin yazısı permission-denied alır ve silinmiş belge geri gelmez.

Testler hatanın gerçekten permission-denied olduğunu, eski yetkili bağlamın ilk okumadan sonra bir mutasyon aşamaladığını, işaretin korunmasını ve sonrasında okumanın da reddedilmesini doğrular. Son uzak belge durumu yalnız emülatör admin bağlamından okunur; üretim kimlik bilgisi veya servis anahtarı kullanılmaz. Yarış zamanlaması sabit sleep yerine bariyerle belirlenir; okuma bariyeri ve test için zaman sınırları vardır.

## Sonuç ve kapsam

npm test: 27/27 başarılı, 0 hata/atlama/iptal. Önceki 24 Firestore/Storage kural testi ve üç yeni yarış testi aynı koşuda geçti. Emulator projesi demo-havamania-audit, adresler localhost Firestore/Storage; koşu sonunda emülatörler kapandı. Üretim kuralları ve Android Kotlin kodu değişmedi; bu yarışlarda yeni bir üretim düzeltmesi gerektiren hata bulunmadı.

Bu kanıt JS SDK + yerel güvenlik kuralları içindir. Android AccountDeletionManager, gerçek backend hesap silme sıralaması, Auth kullanıcısının silinmesi/token iptali, avatar/seyahat/AI geçmişi temizliği veya cihazdaki kalıcı kuyruk temizliği birlikte çalıştırılmadı. Aynı yarışın Android SDK ve yerel silme kapılarıyla uçtan uca birleşimi açık. Canlıya dağıtılmış kuralların bu depodaki kurallarla aynı olduğu varsayılmadı; canlı kurallar dağıtılmadı.

Sırada: fiziksel taşıma kesintisi/transaction zaman aşımı ve geç commit kabulü; tam Android silme akışı ve kuyruk/SDK yarışı; Activity/ViewModel otomatik kurtarma, reboot/saat değişimi ve legacy ad-only uyumluluğu. Önceki Android SDK, süreç restart, JVM/UI testleri bu tur tekrar koşulmadı. Yayın kapıları açık; push/release yapılmadı.
