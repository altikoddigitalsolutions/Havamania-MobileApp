# 17 Kasım — Varsayılan konum için Firebase kural doğrulaması

## Tamamlanan düzeltme

Profilde defaultCityLocation oluşturulurken veya bu alan değişirken sürüm, Long/int kimlik, ad, enlem/boylam aralığı ve isteğe bağlı metin alanları doğrulanıyor. Ad defaultCity ile aynı olmalı; bilinmeyen alanlar reddediliyor. Eksik koordinat sıfır kabul edilmiyor. null veya map dışı companion yeni yazıda kabul edilmiyor; alan deleteField ile kaldırılabilir.

Önemli kural düzeltmesi: users altındaki eski recursive wildcard sıfır yol parçasıyla ana profil belgesini de eşleyebiliyordu. Böyle bir genel allow yeni profil doğrulamasını OR ile geçersiz bırakırdı. Wildcard artık en az bir alt koleksiyon/belge çifti gerektiriyor. Profil create/update doğrulaması atlanamıyor; mevcut alt koleksiyon sahiplik kontrolleri korunuyor.

Geriye uyumluluk: companion yoksa eski profil yazısı destekleniyor. Companion değişmeden kalan başka profil alanları düzenlenebilir; mevcut bozuk legacy harita kullanıcıyı tüm profil işlemlerinden kilitlemez. Geçerli haritayla düzeltme ve haritayı kaldırma test edildi. Bu politika eski istemcinin yalnız defaultCity adını değiştirmesini de hâlâ kabul eder; eski companion kalması bu tur çözülmedi. Yeni harita değişiyorsa tüm yeni biçim doğrulanır. Bu kurallar şehir kimliğinin gerçek coğrafi kaynağını veya kullanıcının gerçekten orada olduğunu kanıtlamaz.

## Doğrulama

npm test, localhost demo-havamania-audit Firestore/Storage emülatörleri: 24/24 geçti; 0 hata, atlama veya iptal. Önceki 9 teste 15 test eklendi. On iki geçersiz veri senaryosu create ve update üzerinde reddi ve mevcut profilin aynen kalmasını kontrol ediyor: sürüm, kesirli kimlik, boş/çelişkili ad, koordinat aralığı, NaN/Infinity, yanlış koordinat/metaveri türü ve bilinmeyen alan. Üç ek test eksik/map dışı harita, legacy haritanın başka profil düzenlemesini engellememesi/düzeltme/silme ve manuel sıfır kimlikli konumu kapsıyor.

Önceki hesap A–B/misafir izolasyonu, hesap silme işareti, alt koleksiyon, rehber ve Storage regresyonları da geçti. Negatif senaryolardaki PERMISSION_DENIED beklenen sonuçtur. Emülatörler koşu sonunda kapandı. Android kodu değişmedi; önceki 608 birim / 13 cihaz testi bu tur yeniden çalıştırılmadı.

## Açık işler ve dağıtım sınırı

Bu tur yalnız depodaki kurallar ve yerel testler değişti. Canlı Firebase kuralları dağıtılmadı; bu korumanın canlıda etkin olduğu iddia edilemez. Canlı dağıtımdan önce mevcut üretim kurallarıyla fark kontrolü ve diğer profil yazarlarının kabul testi gerekiyor. Eski companion alanı adı değişince otomatik temizlenmediği için eski istemci uyumluluk politikası açık. Android SDK ile emülatöre bağlı gerçek senkronizasyon/transaction testleri de sırada. Gerçek servis ve Play yayın kabul kapıları açık. Push/release yapılmadı.
