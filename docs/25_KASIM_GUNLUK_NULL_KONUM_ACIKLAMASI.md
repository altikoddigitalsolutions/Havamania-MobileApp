# 25 Kasım — Günlük eksik ölçümler ve hava açıklamalarının saat kaynağı

## Günlük tahminlerde null ölçümler
DailyDto boş nesneyi ve nullable dizi öğelerini kabul ediyor. Tarih, hava kodu ve sıcaklık aralığı satır bazında doğrulanıyor; geçersiz satır atlanırken komşu günlerin özgün indeksleri korunuyor. Bir eksik günlük ölçüm geçerli anlık hava verisini düşürmüyor. Gerçek sıfır sıcaklık geçerli. Seyahat tahmin özeti eksik tarih aralığını tam kapsam olarak kabul etmiyor; gereken ölçümler ayrıca doğrulanıyor.

## Ana hava açıklamalarında konum zamanı
Mapper anlık açıklamayı sağlayıcının is_day alanından, saatlik açıklamayı ilgili günün doğrulanmış güneş aralığından üretiyor. Faz bilinmiyorsa açık hava açıklaması nötr kalıyor; cihazın anlık saatinden güneşli/gece sonucu çıkarılmıyor. Rota ve genel mapper ortak açıklama yordamını kullanıyor. Gün adları katı LocalDate ayrıştırması ve ortak Türkçe biçimleyiciyle üretiliyor.

## Doğrulama
Yeni 10 regresyonun 8'i eski kodda hatayı üretti. Düzeltmeden sonra testDebugUnitTest ve assembleDebug başarılı: 838 test, 0 hata, 0 atlama. Yeni günlük bileşen testi doğru komşu tarih/sıcaklık ve seçim callback'ini kapsıyor. Fiziksel test koşusu başlayamadı: ADB hiçbir cihaz/emülatör bulamadı. Cihazdaki başarı olarak sayılmadı.

## Açık kontroller
Yeni günlük bileşen testi ve saatlik/rota fazı regresyonları tablet bağlandığında çalıştırılmalı. Eksik/geçersiz saat diliminin UTC varsayımı, eski UI faz yardımcıları ve ay fazının cihaz saati kullanımı ayrıca incelenecek. Bilinmeyen current fazının Boolean modelde temsili, eksik gün/saat kapsamının kullanıcıya gösterimi açık. Tam Activity, canlı servis, tüm tema/ekran ve yayın kabulü tamamlanmış sayılmaz. Git push yapılmadı.
