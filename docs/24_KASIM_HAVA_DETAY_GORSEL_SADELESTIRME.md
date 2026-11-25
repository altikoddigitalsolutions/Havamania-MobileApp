# 24 Kasım — Hava detayları yüzey ve dar ekran düzeni

## Düzeltilenler

Hava detaylarında ayrı görsel yüzeyler yerine tek bölüm yüzeyi kullanıldı. Rüzgâr yönü ve güneş saatleri ortak kart derinliği içinde çerçevesiz/zeminsiz çiziliyor; ölçümlerin kendi saydam yüzeyi korundu. Uygunluk skoru ayrı anlam taşıyan durum kartı olarak kaldı. Güneş saatleri ince ayraçla ayrılıyor.

Ölçümler 360 dp altı içerik genişliğinde veya fontScale 1.3 üzerindeyse tek sütuna geçiyor. Geniş ve normal yazılı görünümde iki sütun sürüyor. Türkçe başlık dönüşümü tr-TR kullanıyor: Hissedilen artık HİSSEDİLEN olur. Gün doğumu / Gündüz ortası / Gün batımı yazımı tutarlı hale getirildi. Görsel kontrolde koyu temada soluk kalan rüzgâr/güneş/uygunluk başlık ve açıklamalarının ek alfa azaltımı kaldırıldı; ortak textSecondary rengi kullanılıyor.

## Doğrulama ve sınır

Yerel bileşen fixture'ı gerçek WeatherDetailsPanel'i koyu 420 dp/normal yazı ve açık 320 dp/yüzde 200 yazıda açar. Büyük yazıda başlıkların dikey sırası, tüm örnek ölçüm/rüzgâr/güneş alanlarına kaydırılarak erişim ve Türkçe başlık sınanır. Mevcut dar güneş saatleri testi de pakete dahil. Canlı hava verisi veya gerçek tam WeatherView navigasyonu kullanılmaz.

İlk derlemede yeni test sınıfı pakete yetişmedi ve sınıf bulunamadı; doğru dizinde oluşturulup tekrar paketlendiğinde 3/3 cihaz testi geçti. 688 JVM testi sıfır hata/atlamayla geçti. Bu JVM koşumu son renk alfa düzeltmesinden önceydi; son renk düzeltmesiyle üç UI testi yeniden çalıştırılıyor. Koyu/açık ilk görüntüler görüldü; son görüntü kontrolü aşağıya eklenecek.

Bu tüm hava ekranı veya ürün görsel kabulü değildir. Hero, saatlik/günlük tahmin, alt/üst menü, tüm mevsimsel paletler, fiziksel telefon/yatay görünüm ve diğer ekranlar açık. Önceki sürümden karşılaştırmalı ekran görüntüsü bu tur alınmadı; eski katmanlar kaynak farkından belirlendi. Testler yalnız ayrı QA com.havamania paketini kullanır; Play paketi değiştirilmez. Push yapılmadı.

## Sıradaki

Görsel bütünlük matrisinde hava detayları kısmen doğrulandı olarak işaretlenecek. Günlük tahmin/seçili satır ve saatlik tahmin yüzeyleri, ortak menü/buton tüketicileri incelenecek. Tüm ekran/tema kontrolü tamamlanana kadar ana iç içe dikdörtgen maddesi açık kalır.
## Son kaynak sonucu

Son renk düzeltmesiyle 3/3 SM-T500 Android 12 cihaz testi sıfır hata/atlamayla tekrar geçti. Son iki görüntü ayrıca gözle incelendi: koyu normal yazıda tek yüzey ve ortak başlık tonları; açık yüzde 200 yazıda tek sütun ve satıra taşan başlıklar. İlk ekran görüntüsü büyük yazıda bölümün tamamını göstermez; alt alanlara erişim kaydırma testinde doğrulandı. Görseller yerel `.artifacts/weather-details-20261124/dark.png` ve `light-large-text.png` dosyalarında; bu yerel kanıt dosyalarının Git'e dahil olduğu varsayılmaz. Önceki 688 JVM sonucu son yerleşim/metin değişikliklerini kapsar; son alfa değişikliği için yeniden UI doğrulaması yapıldı.