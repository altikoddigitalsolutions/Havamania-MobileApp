# 25 Kasım — Kişilik ve rozet doğruluğu

## Giderilen kaynak sorunları

Profil kişiliği/rozet kuralları iki UI dosyasından ProfileLabels.kt içine taşındı. Yer adlarında kelime parçası eşleşmesi, yağmurun Kar Tutkunu sayılması, Türkçe harflerin kaçırılması, olumsuz ifadelerin olumlu tercih gibi okunması ve tanınmayan metne Hava Analisti rozeti verilmesi ele alındı. Çok sayıda bilinmeyen ilgi kimliği artık uzmanlık etiketi üretmez.

Önce sekiz regresyon eski kod üzerinde çalıştırıldı; sekizi de beklenen yeni davranışa aykırı sonuçlarla başarısız oldu. Ardından tekil kelime biçimleri, NFC/Türkçe normalleştirme, sınırlı açık olumlu eylem sözlüğü ve olumsuz cümle denetimi eklendi. Kar, karavan ve Ankara ayrı kelimelerdir. Yağmur ayrı rozet taşır. Bare konu/meslek/şehir anımı tek başına rozet üretmez.

## Seçili tercihler ve UI bağlantısı

Doğrudan seçilmiş bilinen ilgi kimlikleri önceliklidir. Eski motorsiklet kimliği korunur, doğru motosiklet metni de anlaşılır. Kayıtlı ilgi/veri şeması değiştirilmez. Kişilik seçili tercihten, yoksa tanınan olumlu metinden gelir; kanıt yoksa nötr Hava Meraklısı kullanılır. Rozetler seçili tercihler önce olacak şekilde kararlı sırada, tekil ve en çok üç adet olur.

Hakkımda kartına mevcut seçili ilgi kümesi geçirilir; remember anahtarları metin ve tercihi birlikte içerir. Cihaz testleri olumlu yağmur metninden olumsuz metne geçişte eski kişilik/rozetin kalkmasını ve eski motosiklet kimliği kaldırılınca iki görünümün güncellenmesini kontrol eder. Bunlar gerçek ortak Compose bileşenleri, sahte yerel veri ve QA paketi kullanır; tam ProfileScreen/NavHost/Firebase hesabı değildir.

## Sınırlar

Bu sınırlı, kurallı kozmetik etiket çıkarımıdır; tüm doğal dili, alayı, zaman değişimini ve her sözcük ekini anladığı iddia edilmez. Belirsiz/olumsuz metinde etiket vermemek tercih edilir. Seçili ilgi ve biyografi çelişirse seçili tercih esas alınır; biyografi checkbox seçimini sessizce değiştirmez. Çelişkileri kullanıcıya çözdüren akış ve asistan/rehber bağlamı ayrı kabul ister. Sağlık/uzmanlık/izin kararları bu yardımcıya bağlanmaz; uzaktaki model veya backend izinleri değiştirilmedi.

Son doğrulama aşağıya eklenecek. Genel görsel matris, canlı Firebase/backend ve mağaza kapıları açık. Push yok.
## Ek olumsuzluk ve yaygın ekler

Bir konuda metinde olumsuzluk görülürse sonraki genel faaliyet cümlesi o konuyu yeniden olumlu rozet yapmaz: Kamp sevmiyorum ama kamp yapıyorum etiketsiz kalır. Konu belirtilmeden gelen olumsuz devam önceki metinsel çıkarımı kaldırır. Bu ihtiyatlı yaklaşım belirsiz veya zaman içinde değişen ifadelerde rozet vermeyebilir; doğal dil anlayışı iddiası yoktur. Seçili tercihler ayrı ve öncelikli kalır.

Yağmurdan hoşlanıyorum, Fotoğrafçılıkla ilgileniyorum ve Karavanla seyahat ediyorum örnekleri için yaygın ek/eylem regresyonu eklendi. Nihai yeni birim senaryosu sayısı 13'tür; önceki 692 tabanına eklenmiştir. Önceki ara kaynak on cihaz testinden geçti; bu son kurallarla tüm birim ve aynı on cihaz senaryosu tekrar doğrulanıyor.
## Son kaynak sonucu

XML toplamı 705 JVM testi, 0 başarısızlık/hata/atlama. SM-T500 Android 12 üzerinde 10/10 cihaz senaryosu da sıfır hata/atlamayla geçti: iki yeni etiket güncelleme kabulü, altı profil kimlik/istatistik/Hakkımda regresyonu ve önceki iki taslak/kayıt regresyonu. Son kurallardan sonra her iki paket de yeniden çalıştı. Test sayıları giderilen ayrı sorun sayısı değildir.

Sekiz başlangıç regresyonu eski kodda hatayı gösterdi; son pakette bunlar ve seçili kaynak önceliği, sınırlı/tekil rozetler, locale/NFC, ardışık olumsuzluk ve yaygın olumlu ekler doğrulandı. Eski ilgi kimlikleri ve saklanan profil verisi değiştirilmedi. UI metin ve tercih değişiminde iki etiketi de yeniledi. Bu tur uzaktaki asistan veya rehber öneri algoritması değiştirilmedi.

Ekran açık tutma ayarı finally ile geri yüklendi. Kişisel gradle.properties değiştirilmedi. Sıradaki yerel kabul: gerçek seçili tercihlerin asistan/rehber bağlamına aktarılması ve kişiselleştirme kapalıyken filtrelenmesi; çelişkili profil kaynakları ve diğer ekranların görsel matrisi açık kalır. Canlı Firebase/backend ve mağaza kapıları kapanmadı. Push yok.