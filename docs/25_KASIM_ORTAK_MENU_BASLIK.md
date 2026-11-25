# 25 Kasım — Ortak menü ve başlık kontrolleri

## Alt menü

Seçili sekme hesabı bottomNavigationOwner ile rota bölümlerine göre merkezileştirildi. Sorgulu kök rotalar, güzergâh ekranı ve asistan geçmiş detayları ilgili sekmeye bağlandı. Benzer başlayan bilinmeyen rotalar yanlış sekme seçmez. Etkin kök sekmeye tekrar dokunmak aynı gezinme callback'ini tekrar üretmez; alt ekrandan kök sekmeye dönüş hâlâ çalışır.

Büyük yazıda alt menü en az 112 dp, etiketler en çok iki satır kullanır. Standart yazıda 80 dp minimum korunur. Bu değişiklik üç katı yazı veya çok dar fiziksel cihaz kabulü değildir.

## Üst başlık ve kapatma eylemi

Ortak üst başlıkta tek satır sınırlaması kaldırıldı. Uzun başlık metinle büyür; geri ve eylem alanları korunur. Hata kartındaki 24 dp kapatma düğmesi 48 dp yapıldı. Testler uzun başlık, geri/bilgi callback'leri ve hata kapatma alanını doğrular.

## Doğrulama

İki rota birim testi ve NavigationChromeDeviceTest içinde dört cihaz senaryosu eklendi: açık/büyük yazılı alt menü; koyu alt menü ve yinelenen kök dokunması; uzun başlık ve iki eylem; 48 dp hata kapatma. Testler gerçek ortak bileşenlerle izole Compose fixture kullanır. Tam NavHost/back stack veya bütün ekran tüketicileri kabulü değildir.

Sonuç ve görsel inceleme aşağıya eklenecek. Tam WeatherPremiumActivity, tüm mevsimsel temalar, fiziksel yatay telefon, TalkBack sesli turu ve ekran bazlı özel butonlar açık. Genel iç içe dikdörtgen matrisi kapanmadı. Push yapılmadı.
## İlk doğrulama ve ek düzeltme

İki rota regresyonu dahil 692 JVM testi geçti (0 hata/atlama). İlk dört cihaz testinde hata kapatma hedefi geçti; iki alt menü senaryosunda etiket TextLayoutResult taşma bildirdi, uzun başlık senaryosunda geri düğmesi 40 dp ölçüldü. Alt etiketler fillMaxWidth + ortalı metne geçirildi; geri düğmesine açık 48 dp boyut verildi. Taşma ve boyut assert'leri korunarak dört senaryo yeniden çalıştırılıyor. Görsel kayıt ayrıca alınacak.
## Test geçtikten sonra bulunan görsel tutarsızlık

İkinci dört testlik paket 4/4 geçti. Görüntü incelemesinde ise yüzde 200 yazıda Takvim/Asistan kelimelerinin satır ortasından bölündüğü, üst başlığın eylemler arasında daralıp uzadığı görüldü. Seçili simgedeki blur parlaması da dikdörtgen alan gibi görünüyordu. Bu yüzden yalnız test geçişiyle kabul verilmedi.

Alt etiket genişliği gerçek bar genişliğinin dörtte biri eksi küçük boşluk olacak şekilde ayarlandı; fazladan harf aralığı kaldırıldı. Simge arkasındaki bulanık parlama kaldırıldı. Büyük yazıda üst kontroller birinci satırda, tam genişlik başlık ikinci satırda çiziliyor; geri düğmesinin gereksiz ayrı zemin/çerçevesi kaldırıldı. Test artık etiketlerin tek satır olmasını ve başlığın kontrollerin altında olmasını da assert ediyor. Son paket sonucu aşağıda.
## Son kaynak kabulü

Son üretim kaynağıyla 692 JVM ve 4/4 SM-T500 cihaz testi geçti; hata/başarısızlık/atlama yok. Son üç görüntü ayrıca gözle incelendi: açık yüzde 200 yazıda dört alt etiket tek satır, koyu normal yazıda seçili durum net, dikdörtgen parlama yok; uzun başlık kontrol satırının altında tam genişlikte okunuyor. Geri ve hata kapatma düğmesi 48 dp hedefleri doğrulandı. Rota callback testi alt ekrandan köke dönüşü ve aynı köke tekrar dokunmanın callback üretmemesini doğrular; gerçek NavController yığını bu fixture'da kullanılmaz.

Yerel kanıt `.artifacts/navigation-20261125/bottom-light-large.png`, `bottom-dark.png`, `top-large.png` dosyalarında. Git'e dahil olduğu varsayılmaz. USB ekran açık tutma ayarı finally ile önceki değerine geri yüklendi. Kullanıcının gradle.properties değişikliğine dokunulmadı. Push yok.

Sıradaki: profil/ayarlar ve diğer ekranlardaki özel menü/buton tüketicileri, ham Card/Surface kullanan iç yüzeyler. Ortak bileşen düzeltmesi tüm tüketicilerin görsel kabulü sayılmaz. Normal/dar/yatay tüm tam ekranlar, üç katı yazı, mevsimsel paletlerin tamamı ve TalkBack açık kalır.