# Asistan platform yönlendirmesi — 26 Kasım

Tam selamlaşma listesi dışındaki mesajlar artık izin verilmişse /v1/chatbot/ask akışına girer. Hava/tarih/şehir algılaması yerel cevap üretmez. Reddedilen soruda alt dize selamlaşma eşleşmesiyle başarı yanıtı üretme kaldırıldı. İstek ve konuşma kimliği, hata/iptal ve izin kapısı korundu.

Mevcut uygulama hava verisi context.weather_data olarak paylaşılır; eksik/eski veri güncel gibi sunulmaz. Bağlam açıklamaları gerektiğinde platform hava araçlarıyla doğrulamaya izin verir. İzin penceresinin açıklaması yeni davranışa uyarlandı. Yalnızca selamlaşma için yerel yanıt üretilir; karşılaştırma tüm metne, Türkçe harf dönüşümü ve baş/son boşluk temizliğiyle uygulanır.

Sürüm 1.2 / versionCode 5, paket com.havamania.mobile, minSdk 24. Bu kaynak düzeltmesine altı yönlendirme/sözleşme regresyonu eklendi. Test ve imzalı paket üretim sonuçları teslimat raporunda ayrıca kaydedilecektir. Canlı platform konuşma kaydı ve araç çalıştırma kabulü bu kaynak değişikliğiyle doğrulanmış sayılmaz.
