# Asistan platform yönlendirmesi — 26 Kasım

Tam selamlaşma listesi dışındaki mesajlar artık izin verilmişse /v1/chatbot/ask akışına girer. Hava/tarih/şehir algılaması yerel cevap üretmez. Reddedilen soruda alt dize selamlaşma eşleşmesiyle başarı yanıtı üretme kaldırıldı. İstek ve konuşma kimliği, hata/iptal ve izin kapısı korundu.

Mevcut uygulama hava verisi context.weather_data olarak paylaşılır; eksik/eski veri güncel gibi sunulmaz. Bağlam açıklamaları gerektiğinde platform hava araçlarıyla doğrulamaya izin verir. İzin penceresinin açıklaması yeni davranışa uyarlandı. Yalnızca selamlaşma için yerel yanıt üretilir; karşılaştırma tüm metne, Türkçe harf dönüşümü ve baş/son boşluk temizliğiyle uygulanır.

Sürüm 1.2 / versionCode 5, paket com.havamania.mobile, minSdk 24. Bu kaynak düzeltmesine altı yönlendirme/sözleşme regresyonu eklendi. Test ve imzalı paket üretim sonuçları teslimat raporunda ayrıca kaydedilecektir. Canlı platform konuşma kaydı ve araç çalıştırma kabulü bu kaynak değişikliğiyle doğrulanmış sayılmaz.

## Tamamlanan doğrulama ve AAB

Temiz kaynak commit: `3dec7be016215912c84e98fbf164f1963e82aaf5`. 939 JVM testi geçti; hata/atlama yok. Release Kotlin, R8, kritik lint ve bundleRelease başarılı. AAB içinden paket com.havamania.mobile, versionName 1.2, versionCode 5, minSdk 24, targetSdk 36 doğrulandı. ZIP bütünlüğü ve JAR imzası kontrol edildi.

Yükleme sertifikası SHA1: `58:9C:CB:03:CE:26:B9:47:F4:61:BB:C5:A3:95:A8:C5:B5:44:89:BC`.

AAB SHA256: `d3bea2bc437e676478a1cdeb1636f5f2912135a26183d6a2187b06f16860ac30`. Boyut: 27155079 bayt. Dosya: `.artifacts/assistant-release-v5/Havamania-1.2-v5-3dec7be.aab`.

Derleme sürerken çalışma klasöründe değişen bağımsız AiHistoryScreen/EditProfileScreen düzenlemeleri korunarak commit kaynak arşiviyle derlendi. Yerel release ayarı, eşleşen Downloads/keystore.properties ve release.keystore kullanır; gizli değerler rapora yazılmaz.

Canlı platformda konuşma kaydı, hava araçları, Google Play iç test ve 16 KB cihaz kabulü açık. Mağazaya yükleme yapılmadı.
