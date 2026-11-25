# 25 Kasım — Tam tahmin listeleri ve geniş görünüm

## Günlük liste

Liste genişletme eylemi iç zemin/çerçeveli özel Box yerine ortak tema metin rengi kullanan standart Material TextButton oldu. En az 48 dp yükseklik, çok satıra esneyen metin ve açık/kapalı stateDescription var. Liste açıklığı rememberSaveable ile ekran durumu geri yüklemesinde korunur.

Yeni testler: 560 dp günlük satırda gün ve sıcaklık aynı satırda; 320 dp/yüzde 200 yazıda 10 günlük panelde 7→10 açma, durum geri yükleme, sekizinci tarihi seçme ve 10→7 kapama. Bu bileşen ve Compose kaydet/geri yükleme kabulüdür; tam Activity süreç ölümü değildir.

## Saatlik liste

Üretim HourlyForecastRow ile aynı içeriği çizen HourlyForecastContent ayrıldı; içerik testi sahte ThemeViewModel veya canlı hesap gerektirmez. LazyRow anahtarı yalnız saat yerine fullTime olur; tam zaman yoksa indeks/saat yedeği kullanılır. 48 saatlik iki günde 01:00 tekrarları ayrı kimlikle seçilir. Seçili kayda tekrar dokunmak ikinci callback üretmez; diğer günün aynı saati seçilmemiş kalır. Bölüm başlığının fazladan alfa azaltımı kaldırıldı, textSecondary kullanılır.

Bu değişiklik aynı fullTime'ın iki kez gönderilmesine ayrı bir çözüm değildir; mevcut veri normalleştirme katmanı benzersizliği sağlar. Tam tarih yoksa indeks yedeği kullanılması veri yeniden sıralanmasında kimliğin kalıcı olduğu anlamına gelmez.

## Sonuç ve sınırlar

Son test sonuçları aşağıya eklenecek. Mevcut iki dar tahmin testi ve iki hava detay testi regresyon paketinde korunur. Testler ayrı QA paketini kullanır; canlı servis/kullanıcı verisi veya Play kurulumu değiştirilmez. USB ekran açık tutma ayarı yalnız koşum süresince uygulanıp finally ile geri yüklenir.

Tam WeatherView içinde tarih/saat değişimi, tüm mevsimsel paletler, fiziksel yatay telefon, ekran okuyucu turu, menü/buton tüketicileri ve diğer ekranların iç içe dikdörtgen incelemesi açık. Ana tasarım maddesi kapanmadı. Push yok.
## Kaynak doğrulaması

Son üretim kaynağıyla 690 JVM testi ve 7 cihaz testi sıfır hata/başarısızlık/atlama ile geçti. Yedi cihaz testi: önceki iki dar tahmin kabulü, iki hava detay regresyonu ve yeni geniş satır, günlük liste geri yükleme, iki günlük saatlik liste senaryosu. Bu sayı yedi ayrı hata giderildi anlamına gelmez. Ardından yalnız geniş satır ve günlük panel testlerine görüntü kaydı eklenerek iki senaryo görsel kanıt için tekrar çalıştırılıyor; üretim kaynağı değişmedi.
## Görsel sonuç

Görüntü eklenen iki senaryo da 2/2 hatasız tekrar geçti; bunlar önceki yedinin içindeki aynı senaryolardır. Geniş 560 dp koyu satır ve büyük yazılı açık panel alt bölümü gözle incelendi: sıcaklık alanları okunuyor, standart genişlet/daralt düğmesi ekstra iç çerçeve olmadan yer alıyor. Görüntü panelin alt bölümünü gösterir; bütün 10 günlük listeyi tek karede göstermesi beklenmez. Yerel kanıt `.artifacts/forecast-20261125/wide-row.png` ve `panel-expanded-large.png`; Git'e dahil olduğu varsayılmaz. Ekran açık tutma ayarı her koşum sonunda finally ile geri yüklendi.

Bu adımın kapsamı tamamlandı. Sıradaki görsel inceleme üst/alt menü ve ortak buton tüketicileri; tam ürün akışı ve diğer tema/ekran durumları açık kalır. Push yapılmadı.