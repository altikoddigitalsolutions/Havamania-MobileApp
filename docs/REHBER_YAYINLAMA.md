# Güncellenebilir seyahat rehberi

Android artık paket içindeki rehberin üzerine Firebase Firestore'dan editoryal içerik alabilir. Bu kodun eklenmesi canlı içeriğin yayımlandığı anlamına gelmez. Güvenlik kuralları ve içerik ayrıca dağıtılmalıdır.

## Çalışma biçimi

- Rehber açıldığında disk önbelleği okunur ve sunucu kontrol edilir. Ekran açıkken altı saatte bir tekrar kontrol edilir. Aynı konumun ardışık denemeleri en az 15 dakika aralıklıdır.
- İşlem ekran yaşam döngüsüne bağlıdır; arka planda sürekli sorgu veya konum takibi yoktur.
- Yalnızca konumun normalleştirilmiş il/ilçe anahtarının SHA-256 özeti gönderilir. İstek verisine koordinat, UID veya kişisel tercihler eklenmez; Firebase SDK mevcut oturumun kimlik doğrulamasını kullanabilir. Bu özet bir anonimlik garantisi değildir; açık editoryal kaydın adresidir.
- Artan sürüm, konum eşleşmesi, geçerlilik tarihi, metin boyutları, seyahat türleri ve HTTPS resmî kaynaklar doğrulanır. Süresi dolan içerik kullanılmaz.
- Çevrimdışında son geçerli içerik veya paket içi rehber korunur. Konuma ait veri yoksa başka ilçenin içeriği gösterilmez.
- Bellek ve disk önbelleği en fazla 64 konum içindir. Disk erişimi IO iş parçacığındadır.

## Yayın sırası

1. `mobile/firebase-rules/firestore.rules` kurallarını doğru Firebase projesine dağıtın. `travel_guides` tekil kayıt okumaya açık; listeleme ve istemci yazmaları kapalıdır. Kullanıcı verisi kuralları değişmez.
2. Aşağıdaki şemaya uygun UTF-8 JSON hazırlayın. Yer adlarını, kaynakları, etkinlik tarihlerini ve çalışma koşullarını editoryal olarak doğrulayın. `validUntilEpochMs` gelecekteki geçerlilik sonudur; örnek sayı yerine gerçek tarih yazın.
3. Ön kontrol: `python tools/publish_travel_guide.py rehber.json`. Bu komut yazma yapmaz; kayıt yolunu verir.
4. Yetkili sunucuda Application Default Credentials ile `python tools/publish_travel_guide.py rehber.json --publish --project GERCEK_PROJE_KIMLIGI`. Yönetici anahtarlarını uygulamaya veya Git'e eklemeyin. Araç önceki sürümden büyük sürüm gerektirir ve transaction kullanır.
5. Yeni Android sürümünde rehberi açarak içerik geçişini, çevrimdışını ve süre dolumunu doğrulayın.

```json
{
  "city": "İzmir",
  "district": "Urla",
  "version": 1,
  "validUntilEpochMs": 0,
  "description": "Urla'da üretim ve sanat odaklı bir gün",
  "visit": "İskele'deki Klazomenai Antik Kenti'ni ayrı bir durak olarak planla.",
  "food": "Mevsiminde enginar ve o gün hazırlanan Ege otlarını sor.",
  "experience": "Merkezde Malgaca Pazarı ile Sanat Sokağı'nı aynı yürüyüşe bağla.",
  "experiences": [{
    "title": "Üretimden sanata bir Urla günü",
    "category": "Yerel yaşam",
    "description": "İskele'deki Klazomenai ziyaretinden sonra merkeze geç. Malgaca Pazarı ve Sanat Sokağı için yürüyüş molası ayır; iki bölge arasındaki ulaşımı ayrıca planla.",
    "sourceUrl": "https://goturkiye.com/culturaljourneys/urla",
    "tripTypes": ["CULTURE", "WEEKEND"]
  }]
}
```

Bu örnek süresi sıfır olduğu için doğrudan yayımlanamaz. Canlı etkinlik tarama, rezervasyon/yer uygunluğu ve otomatik içerik üretme işi eklenmedi. Rehberin güncel kalması için doğrulanmış yeni sürümlerin editör veya yetkili bir yayın servisi tarafından yayımlanması gerekir.
