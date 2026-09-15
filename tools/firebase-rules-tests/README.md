# Yerel Firebase kural testleri

Node.js 20/22 ve JDK 17 ile:

```text
npm ci
npm test
```

İlk çalıştırma emülatör dosyalarını indirir. Testler yalnızca `demo-havamania-audit` projesini, Firestore için `127.0.0.1:8086` ve Storage için `127.0.0.1:9196` adreslerini kullanır. Demo proje başka servisleri kullanmaya çalışırsa Firebase CLI bunu reddeder; test dosyası da emülatör ortam değişkenleri yoksa çalışmaz. Gerçek Firebase hesabı veya giriş gerekmez.

`prepare.mjs`, her çalıştırmadan önce uygulamanın gerçek kural dosyalarını `.generated` dizinine kopyalar. Bu dizin, indirilen bağımlılıklar ve emülatör logları Git'e eklenmez. Emülatörler test bittiğinde kapanır.

Kapsam: hesap sahibinin erişimi, başka kullanıcının engellenmesi, iç içe Firestore belgeleri, silme işaretinin eski oturumla yeniden yazmayı engellemesi, fotoğraf sahipliği/türü/boyutu ve silme yetkisi.
