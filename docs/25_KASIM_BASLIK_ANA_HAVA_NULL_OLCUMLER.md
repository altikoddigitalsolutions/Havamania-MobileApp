# 25 Kasım 2026 — Bildirim başlığı ve ana hava saatlik null verisi

## Güzergâh bildirim başlığı

Worker önceki ve yeni özet metnini karşılaştırıp her metin farkını hava değişikliği olarak adlandırıyordu. Oysa ifade, veri tamlığı veya değerlendirme kuralı değişmiş olabilir. Başlık üretimi routeForecastNotificationTitle yordamına alındı: yeni metin boşsa veri bekleniyor, önceki analiz yok/boşsa ilk tahmin hazır, diğer durumda tahmin güncellendi denir. Gerçek meteorolojik değişiklik iddiası çıkarılmaz. Worker aynı yordamı doğrudan kullanır; tekrarlı başlık kodu kaldırıldı.

## Ana hava ekranındaki saatlik null ölçümler

Genel HourlyDto zorunlu/null kabul etmeyen diziler nedeniyle tek null ölçümde bütün OpenMeteoResponse çözümlemesini reddediyordu. Boş hourly nesnesi de geçerli current verisini kullanılamaz hale getirebiliyordu.

Saatlik diziler artık null öğe kabul eder; zaman/sıcaklık/kod yoksa boş diziye karşılık gelir. WeatherMapper aynı ham indeks üzerinden zaman, bilinen WMO kodu ve sonlu -100..65 sıcaklığı doğrular. Gerekli alanı geçersiz satır atlanır; kalan saatler ve anlık/günlük veriler korunur. Yağış null veya yüzde dışında ise bilinmeyendir. Gerçek sıfır geçerlidir. Dizi öğeleri birbirinden bağımsız filtrelenmediği için saat/sıcaklık/yağış eşleşmesi kaymaz. Genel saatlik taşıma modeli önbellek/yanıt uyumluluğunu korur; veritabanı şeması değişmez. Rota için önceki adımda ayrılmış RouteHourlyDto etkilenmez.

## Test kanıtı

İlk yedi testin tamamı eski kaynakta başarısız oldu. Üç başlık senaryosu: ifade farkı, boş önceki metin, boş yeni metin. Beş saatlik senaryo: JSON null sıcaklıkta current ve komşu saat, boş hourly nesnesi, null zaman/kodda indeks eşleşmesi, geçersiz kod/aşırı sıcaklık ve gerçek sıfırlar.

Yeni cihaz testi gerçek HourlyForecastContent içinde null komşudan sonra 14:00/18 derece satırının görünmesini, eksik 13:00 veya sahte sıfır eklenmemesini ve dokunmanın doğru gösterim indeksine gitmesini kontrol eder. Mevcut current17 derece de korunur.

Son kaynakla 828 JVM testi ve SM-T500 üzerinde 1 izole saatlik tahmin cihaz testi geçti; hata/istisna/atlama yok. QA APK ve test APK derlemesi başarılı.

## Açık kabul ve sıra

Bu kabul JSON/mapper, saf başlık mantığı ve izole saatlik bileşendir. Gerçek bildirim tepsisi ve tam hava ekranı giriş/Room/ağ/Activity akışı, canlı servis, telefon/tablet tüm tema/yatay/TalkBack kabulü değildir. Eski kaydedilmiş başlıklar değiştirilmez.

Atlanan saat satırı mevcut UI modelinde bir boşluk olarak kalır; bu çalışma eksik saatler için yeni yer tutucu veya kapsama uyarısı tasarlamaz. Tamamen bozuk JSON türlerinde hata yolu korunur. Sıradaki: DailyDto ölçüm dizilerindeki null değerler ve ana WeatherMapper'ın gündüz/gece/saat dilimi varsayımları. Önceki rota yerel saat etiketleri, tam hesap değişimi/iptal, görsel matris/iç içe dikdörtgenler, geçmiş dialog küçük ekran/büyük yazı, canlı servis ve yayın kapıları açık. Push yapılmadı.