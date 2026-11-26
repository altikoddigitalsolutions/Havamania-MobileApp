# 26 Kasım — Hava kartı hareket ve ikon kontrastı

## Düzeltmeler
Bilinen gündüz/gece fazında arka planın sonsuz animasyonu canlı efekt tercihini dikkate almıyordu. LiveBackgroundLayer artık aynı animasyon iznini alıyor; kapalı durumda sonsuz transition oluşturmuyor. Statik atmosfer korunuyor. Efektler ve arka plan ortak izin kullanıyor.

Sistem animasyon ölçeği tek seferlik Settings okuması yerine Compose MotionDurationScale üzerinden izleniyor. Ölçek sıfırken giriş animasyonları oluşturulmuyor ve paralaks sıfırlanıyor. Gerçek Android penceresinde sistem ölçeği 1→0→1 değişimi ve sıfır ölçekte Activity yeniden oluşturma kabulü HeroSystemMotionDeviceTest ile geçti. Test Compose test saatinden bağımsız çalışır; önceki sistem ayarı finally içinde geri yüklenir.

Parlak gündüz paletlerinde konum, hava ve metrik ikonları dekoratif sarı yerine okunabilir ön plan rengini kullanıyor. Gece/akşam ve nötr faz davranışı korunuyor. Dekoratif güneşin rengi değiştirilmedi.

## Doğrulama
931 JVM testi hatasız, atlama yok; testDebugUnitTest ve assembleDebug başarılı. Kontrast regresyonu beş gündüz hava sınıfında tüm gradyan durakları için en az 3:1 kontrol ediyor; bütün ekran/tema ve kompozit yüzeyler için kapsamlı erişilebilirlik onayı değildir.

SM-T500 Android 12 üzerinde iki WeatherHeroPhaseDeviceTest geçti; hata/atlama yok. Canlı efekt açıkken kapatıldıktan sonra 10 saniye ilerletilen arka plan görüntüsü piksel bazında aynı kaldı. Nötr/gece/gündüz ve açık/koyu geçiş regresyonu da geçti. Bu adımda yeni ekran görüntülerinin gözle görsel kabulü yapılmadı. Büyük yazı/yatay kullanım, tüm tema matrisi açık. Sistem ölçeği değişiminin gözlenmesi geçti; giriş/paralaksın tüm geometrik sınırları ayrıca ölçülmedi.

## Sıradaki
Sistem hareket tercihi canlı değişimi ve gündüz görsel kabulü; API raporunda bulunan FCM 201/200 uyuşmazlığı ve hava hız sınırı parametreleri. Önceki Firebase, güvenlik, senkronizasyon, yayın ve iç içe dikdörtgen matrisi açık kalır. Push yapılmadı.

## Gerçek pencere ve görsel kabul güncellemesi
SM-T500 Android 12 üzerinde HeroSystemMotionDeviceTest ve iki WeatherHeroPhaseDeviceTest toplam üç test hatasız geçti; atlama yok. Debug fixture yalnızca debug manifestinde bulunur, Firebase ya da kullanıcı hesabı kullanmaz. Önceki sistem ayarı test sonunda geri yüklendi; test öncesi ve sonrası animator_duration_scale değeri null. Üretim kaynağı bu ek doğrulamada değiştirilmedi; önceki 931 JVM sonucu geçerlidir.

Kontrast düzeltmesinden sonra oluşan gündüz/gece PNG'leri .artifacts/hero-motion-20261126 altında incelendi. Gündüz konum/hava/metrik ikonları koyu ve okunabilir; bu 360 dp örnekte metrik taşması görülmedi. Güneş dekorasyonu üst konum alanına yaklaşıyor; dar ekran/dekorasyon yerleşimi sıraya eklendi. Görüntüler tam ekran/tema matrisi onayı değildir. Yerel QA çıktıları push kapsamına alınmaz.
