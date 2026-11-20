# 20 Kasım Özel düğmelerde gradyan ve büyük yazı

Özel HavamaniaSecondaryButton sabit yükseklik, tek satır ve Clip kullanıyordu. Büyük yazıdaki uzun etiket kesilebiliyordu. Birincil düğmenin yükleme göstergesi her temada beyazdı; açık zemin üzerinde seçilmesi zorlaşıyordu. İç kutuların koşulsuz tam genişliği fillMaxWidth=false seçimini de etkisiz bırakabiliyordu.

## Düzeltmeler

İkincil düğme sabit yükseklik yerine en az istenen yükseklik ve 48 dp kullanır; metin satır sayısı sınırlandırılmaz, içerikle büyür. İki düğmenin iç kutusu da fillMaxWidth seçimini izler. Yüzde 200 yazıda ikonlu uzun etiketler birden çok satıra sığar.

Birincil düğmenin gradyanı ThemeButtonPalette ile örneklenir. Tercih edilen yazı yeterliyse korunur; değilse gradyan boyunca uygun tek siyah/beyaz metin seçilir. İkisi de proje hedefini sağlamıyorsa gradyan aynı ailede tonlanır. Mevcut kış gradyanında bu son adım gerekir. Metin ve dekoratif ikon aynı ön planı kullanır. Palet ve Brush hesapları tema/durum anahtarlarıyla remember içinde tutulur; performans hızlanması için benchmark iddiası yoktur.

Yükleme göstergeleri tema textPrimary rengini kullanır. İkincil düğmeye de Button rolü, özgün etiket ve İşlem sürüyor semantiği eklendi. enabled=false veya isLoading=true durumunda iki düğme de fiziksel tıklamayı reddeder. Pasif metin tema textSecondary rengini kullanır.

## Doğrulama

638 JVM testi ve bağlı Samsung SM-T500 Android 12 üzerinde 9 UI testi geçti; sıfır hata/atlama. Üç yeni birim testi altı sabit temanın gradyan örneklerini, geçerli ön planın korunmasını ve kış gradyanı tonlamasını doğrular. Kullanılan 4.5 oranı bu projenin test hedefidir; bütün uygulama için erişilebilirlik sertifikası değildir.

Üç yeni UI senaryosu 280 dp görünümde yüzde 200 yazı ve ikonlarla uzun etiketlerin lineCount>1 ve hasVisualOverflow=false olduğunu, callback'lerin çalıştığını, pasif/yüklenen durumlarda çalışmadığını, yükleme semantiğini, küçük yükseklikte en az 48 dp'yi ve fillMaxWidth=false içerik genişliğini denetler. Mevcut üç hesap silme penceresi ve üç profil hata penceresi testi aynı koşuda geçti. Testte kullanılan bir boyut yardımcı API'si mevcut Compose sürümünde bulunmadığı için gerçek sınır ölçümüne çevrildi; son test paketi derlendi.

## Kalanlar

Bu ortak bileşen ve seçili diyalog kabulüdür; her çağıran ekran görsel olarak tamamlanmış sayılmaz. Fiziksel telefon, TalkBack sesli kullanım ve tüm gerçek tema animasyonları açık kalır. Sıradaki yerel iş giriş/kayıt ekranlarında bu düğmelerin klavye, dar görünüm ve büyük yazıyla kullanımıdır. Tam navigasyon, süreç ölümü ve canlı yayın kapıları da açık kalır.