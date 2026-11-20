# 20 Kasım Profil durumlarında dar ekran ve erişilebilirlik

Profil yükleme/hata bileşeni açık ve koyu temada, yüzde 200 yazıyla tablet üzerinde sınırlanmış telefon görünüm alanlarında test edildi. 320×480 dp açık telefon görünümü ve 480×240 dp kısa yatay görünüm kullanıldı; cihaz çözünürlüğü, yazı ayarı veya yönü değiştirilmedi.

## Düzeltmeler

Yükleme göstergesine Profil bilgileri yükleniyor erişilebilir açıklaması eklendi. Durum başlığı heading ve polite live region olarak işaretlendi; gecikme/hata metni değişiklikleri için semantik bildirim sağlandı.

Görsel incelemede koyu tema Tekrar dene düğmesinin yazısının varsayılan Material mor tonunu aldığı görüldü. Bu bileşenin düğme zemini ve yazısı uygulama tema renklerine açıkça bağlandı. Koyu temada turkuaz zemin üzerindeki metin koyu zemin rengine, açık temada vurgu üzerindeki metin onAccent rengine bağlandı; Geri dön de tema vurgusunu kullanıyor. Bu düzenleme bütün uygulamanın renk sistemini değiştirmez.

## Doğrulama

Son üretim kaynağıyla 631 JVM testi ve 6 UI testi geçti; sıfır atlama/hata. Üç yeni responsive/semantik senaryo ve önceki üç profil yükleme senaryosu birlikte çalıştı. Metin düzeninde hasVisualOverflow olmadığı, kısa görünümde kaydırarak eylemlere ulaşıldığı, iki düğmenin bağımsız callback'leri ve açık telefon görünümünde en az 48 dp eylem yüksekliği doğrulandı. Yükleme açıklaması, başlık semantiği ve gecikmeye geçiş de sınandı.

Açık tema hata metni ve koyu tema eylem renkleri sentetik ekran görüntülerinden ayrıca incelendi. Kısa yatay görünümde içerik kaydırılır; yakalanan görüntü kaydırma sonunda eylemleri gösterir. Ekran görüntüleri yalnız test metni içerir. QA paketi koşu sonunda kaldırıldığından görüntüler önce geçici cihaz konumuna alındı, sonra bilgisayarın geçici klasörüne çekildi; cihaz kopyaları silindi.

İlk responsive koşusu kontrollü saat kapalıyken gerçek kaydırma beklediği için test altyapısında takıldı. Yalnız QA süreç durduruldu; gecikme eşiği kontrolünden sonra otomatik saat ilerletilerek kaydırma testi düzeltildi. Son koşular hatasız tamamlandı; bu müdahale bir üretim çökmesi olarak sayılmaz.

## Sınırlar ve sıradaki

Bu, tablette kısıtlanmış görünüm alanıyla gerçek bileşen kabulüdür. Fiziksel telefon, gerçek cihaz yön değişimi, TalkBack'in sesli kullanım akışı ve tüm ürün navigasyonu bu koşunun kapsamında değildir. Semantik testleri tek başına tüm uygulamanın erişilebilir olduğunu kanıtlamaz.

Koyu tema varsayılan Material yazı rengi başka düğmelerde de görülebilir. Sıradaki yerel inceleme diğer ekranların düğme renkleri ve tema bütünlüğüdür; ortak tema token'larını değiştirmeden önce yaygın kullanım noktaları ve uygun test kapsamı belirlenecek. Önceki süreç ölümü, tam navigasyon ve canlı yayın kapıları açık kalır.