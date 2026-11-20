# 20 Kasım Giriş ve kayıt klavye ve yerleşim kabulü

Giriş ve kayıt formlarında klavye eylemleri, odak rengi ve büyük yazıda eylemlere erişim düzeltildi. Gerçek form içerikleri servis çağrısından ayrıldı; public ekran ve ViewModel bağlantıları korunuyor.

## Yapılan düzeltmeler

- Giriş, kayıt ve parola sıfırlama içerikleri klavye boşluğunu hesaba katıyor. Girişte e-posta → parola, kayıtta ad → e-posta → parola → parola tekrarı odak zinciri eklendi.
- Klavyeden gönderim ve düğme aynı koşulları kullanıyor. Kayıtta şifre eşleşmesi ve koşulların onayı klavyeden atlanamıyor. Ad/e-posta kenar boşlukları temizleniyor; parola aynen korunuyor.
- İşlem sırasında giriş/kayıt alanları ve hesap geçişi bağlantıları devre dışı. Odak çerçevesi artık gerçek odak değişimini izliyor.
- Karşılama ekranı kaydırılabilir; yasal bağlantılar ve hesap geçişi satırları dar alanda satıra yayılıyor. Kompakt marka başlığı ve Türkçe büyük harf dönüşümü düzenlendi; Email etiketleri E-posta oldu.
- Kullanılmayan LegalDivider kaldırıldı. Kayıt onayı tek checkbox semantiği üzerinden yönetiliyor.

## Doğrulama

Samsung SM-T500 Android 12 üzerinde ayrı QA paketiyle son koşu 7 test, 0 hata, 0 atlanan sonuç verdi. AuthFormsUiTest dört senaryo; CustomButtonsUiTest üç regresyon senaryosu içeriyor.

Girişte gerçek sistem klavyesi dokunmayla açıldı, IME görünürlüğü sistem WindowInsets üzerinden doğrulandı; Sonraki parola alanına geçti, Bitti gönderdi, yüklemede alanlar kilitlendi ve klavye kapandı. Kayıtta yanlış parola ve onaysız gönderim reddedildi; onaydan sonra tek callback doğru alanlarla alındı. 320×640 dp kısıtlı görünüm ve yüzde 200 yazıda hata kapatma, hesap geçişi bağlantıları, kayıt düğmesi ve karşılama yasal etiketlerine kaydırarak erişildi. Bu fiziksel telefon veya tüm metinlerde kırpılma kabulü değildir.

Birim test XML toplamı 638 test, 0 hata, 0 atlanan. Son testDebugUnitTest görevi UP-TO-DATE olduğundan mevcut başarılı sonuçlar yeniden kullanıldı. Git diff --check temiz.

İlk test koşusunda IME görünürlüğü beklemesi ve testin onay satırındaki yasal bağlantıya denk gelen dokunuşu başarısız oldu. Test Activity'si edge-to-edge/adjustResize ile ayarlandı; IME kontrolü sistemden okundu ve onay eylemi doğrudan checkbox semantiğiyle tetiklendi. Son koşu tüm yedi testi yeniden çalıştırıp geçti.

## Sıradaki kontroller

1. AuthViewModel için yinelenen gönderim, coroutine iptali ve kayıt profilinin tamamlanma sırasını demo/yerel testlerle incele. Loading ataması launch içinde; Exception yakalama iptali de kapsıyor. Bunlar bu tur düzeltilmiş sayılmıyor.
2. Formların Activity yeniden oluşturma ve gerçek yatay telefon davranışı; parola saklanmadan güvenli toparlanma.
3. Parola sıfırlama IME gönderimi ve hata/success akışının ayrı testi. Bu tur yalnız klavye boşluğu ve etiket ortak düzeltmesi uygulandı.
4. Canlı Firebase giriş/kayıt, tam ürün navigasyonu, TalkBack, yasal sayfa içerikleri ve mağaza kabulü ayrı yayın kontrolleri olarak açık.

Testler üretim LoginContent/RegisterContent bileşenlerini yerel callback'lerle kullanır; AuthViewModel veya canlı hesap işlemleri için uçtan uca kabul değildir. Üretim Firebase yapılandırması, Play paketi ve kişisel Gradle ayarları bu adımda değiştirilmedi. Commit/push yapılmadı.