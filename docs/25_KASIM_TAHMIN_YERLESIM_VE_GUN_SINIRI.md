# 25 Kasım — Günlük/saatlik tahmin yerleşimi ve gün sınırı

## Tahmin bileşenleri

Günlük satırdaki 72 dp sabit yükseklik ve dar sabit metin sütunları kaldırıldı. Dar içerik veya büyük yazıda gün/simge ile sıcaklık aralığı iki satıra ayrılır; geniş görünümde tek satır korunur. Satır en az 72 dp, metin büyüdükçe daha yüksek olabilir. En düşük sıcaklık artık ayrıca soldurulmuyor. Bugünün günlük toplamına Şimdi etiketi verilmesi kaldırıldı; günlük tahmin anlık ölçüm değildir.

Saatlik kart en az 160 dp yüksekliğinde, metinle büyüyen içerik ve yazı ölçeğine uyarlanan genişlik kullanır. Yağış yüzdesindeki 44×22 dp sabit iç kutu ve küçük dikdörtgen zemini kaldırıldı. %100 gibi değer tek satırda daha geniş alana sığar. Seçili dolgu/metin ortak tema gradyan paletinden gelir. Bu kontrastı değiştiren ek beyaz parlama kaldırıldı. Seçili kartın dışa taşan büyümesi ve sabit 1 değerine animasyon kaldırıldı; dokunma geri bildirimi sürer. Günlük/saatlik eylemler seçili durumunu selectable/Tab semantiğiyle bildirir.

## Testte bulunan ek hata

İlk tam JVM koşumunda 688 testten dördü gece yarısında başarısız oldu. Asistanın fallback yanıtında bugün cihaz LocalDate.now ile ayrıştırılıyor, sonra hava konumunun gününe karşılaştırılıyordu. Cihaz günü ile UTC hava günü ayrışınca geçerli bugünkü gözlemler başka gün gibi reddediliyordu. Artık tek Instant ve hava konumunun günü kullanılır; tarih ayrıştırıcısına aynı gün verilir. Sabit saatli Los Angeles ve İstanbul gün devri regresyonları eklendi; yarın sorusu güncel gözlemle yanıtlanmaz.

## Doğrulama kapsamı

Yeni ForecastLayoutDeviceTest koyu/normal yazı ve açık/yüzde 200 yazıda dar 320 dp görünüm kullanır; sıcaklık alanları, %100 metninde TextLayoutResult taşma kontrolü, seçim semantiği ve birer dokunma callback'i doğrulanır. Önceki hava detayları iki testlik regresyonu pakete dahil. QA com.havamania kullanılır; Play paketi ve canlı kullanıcı verileri değiştirilmez.

Son test sonucu ve görüntü incelemesi aşağıya eklenecek. Bu tam WeatherView, bütün mevsimsel temalar, yatay fiziksel telefon, ekran okuyucu sesli turu veya günlük genişlet/daralt listesinin tam kabulü değildir. İç içe dikdörtgen ana maddesi ve görsel matris açık kalır. Push yok.
## Ara doğrulama ve test düzeltmesi

Gün sınırı düzeltmesi sonrası XML toplamı 690 JVM testi, 0 başarısızlık/hata/atlama. İlk cihaz koşumunda yağış taşması assert'i başarısız oldu. Ayrıntılı ölçüm, birleşik erişilebilirlik düğümündeki GetTextLayoutResult eyleminin %100 yerine 10:00 metnini döndürdüğünü gösterdi; bu sonuç yağış taşması kanıtı değildir. Ölçüm useUnmergedTree=true ile doğrudan yağış Text düğümüne bağlandı ve dönen metnin %100 olduğu da assert edildi. Açık/koyu seçilmemiş görüntüler incelendi; iç yağış dikdörtgeni yok, değerler okunuyor. Son dört testlik paket sonucu aşağıda belirtilecek.
## Genişlik düzeltmesinin devamı

Doğru %100 metin düğümüyle tekrar, küçük genişlik taşmasının devam ettiğini gösterdi. Alt sıcaklık/yağış sütunu ve yağış Row/Box'ı fillMaxWidth ile kartın içerik alanına yayıldı; yüzde metni weight(1f) ile simgeden sonra kalan genişliği alıyor. Assert kaldırılmadı veya toleransla gevşetilmedi. Bu yerleşim düzeltmesi sonrası hedefli dört test yeniden çalıştırıldı; sonuç aşağıda.

Son genişlik değişikliğinin ilk cihaz tekrarında tablet ekranı kapandı/kilitlendi; dört test Compose hiyerarşisine erişemedi. dumpsys showing=true/SCREEN_STATE_OFF bunu doğruladı. Kullanıcı kilidi kaldırdı; son kaynakla 690 JVM testi tekrar geçti. Cihaz tekrarında USB ekran açık tutma ayarı geçici uygulanıp finally ile önceki değerine döndürülecek.

## Son doğrulama sonucu

Son üretim kaynağıyla 690 JVM testi ve son cihaz XML'inde 4/4 test sıfır başarısızlık/hata/atlama ile geçti. Doğrudan %100 TextLayoutResult ölçümünde artık görsel taşma yok. Günlük/saatlik seçili semantiği ve birer dokunma callback'i geçti; önceki hava detayları regresyonları da geçti. Son seçili koyu/normal ve açık/yüzde 200 görüntüler gözle incelendi: değerler okunuyor, yağış iç dikdörtgeni yok, seçili dolgu/metin tutarlı. Yerel görsel kanıt `.artifacts/forecast-20261125/dark-selected.png` ve `light-large-selected.png` dosyalarında. Bunların Git'e dahil olduğu varsayılmaz. Ekran açık tutma ayarı finally ile önceki değere döndürüldü.

Bu iki yeni tahmin senaryosu 320 dp içerik kullanır; günlük satırın geniş tek satır dalı, tam günlük panel genişlet/daralt eylemi, uzun saatlik liste seçim/kaydırma ve mevsimsel paletlerin tamamı açık kalır. Sıradaki görsel kabul bunlar ve ortak menü/buton tüketicileridir. Ana tüm ekran/iç içe dikdörtgen maddesi kapanmadı.