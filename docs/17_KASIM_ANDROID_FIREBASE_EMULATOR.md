# 17 Kasım — Android SDK ile yerel Firebase kabulü

## Tamamlanan çalışma

FirebaseCityContractDeviceTest ayrı bir FirebaseApp kurar; proje kimliği sabit demo-havamania-audit, Auth 127.0.0.1:9096, Firestore 127.0.0.1:8086. Varsayılan uygulama oturumu kullanılmaz. Her testte yerel anonim kullanıcı oluşturulur; kalıcı Firestore cache kapalıdır. Test yalnız firebaseEmulatorContract=true açık argümanıyla çalışır; normal koşuda emülatöre bağlanmadan atlanır.

Koşucu npm run test:android ile Auth/Firestore emülatörlerini başlatır. PowerShell betiği yalnız bu localhost ortam değişkenlerini kabul eder, tek bağlı cihaz ister, çakışan reverse eşlemelerinde durur. Yalnız kendi eklediği ADB tcp:8086/tcp:9096 reverse eşlemelerini finally içinde kaldırır. QA applicationId com.havamania ve geçici build dizini init script ile seçilir; Play paketine veya kişisel gradle.properties dosyasına müdahale edilmez.

İlk koşuda debug network-security-config 127.0.0.1 HTTP trafiğini reddetti. Yalnız debug XML'ine loopback adresi eklendi; main/release ağ politikası değişmedi. İkinci koşuda profil testinin hatalı beklentisi düzeltildi: harita değişmeden yalnız ad yazmak legacy uyumluluğu nedeniyle hâlâ izinli. Negatif test artık harita da değiştirilirken ad çelişkisini sınar; kurallar testin geçmesi için gevşetilmedi.

## Sonuç

Son npm run test:android başarılı. SM-T500 Android 12 üzerinde 3/3 test, 0 hata/atlama:

- Gerçek Android SDK ile profil merge; başka alanların korunması; Source.SERVER onaylı görüntüde Long kimlik/koordinat serileştirmesinin üretim çözümleyicisiyle birebir geri okunması. Değişen harita ile çelişkili ad PERMISSION_DENIED; mevcut profil korunur.
- Gerçek Firestore runTransaction içinde üretim stageCityDocumentMutation kullanılır. Aynı ID/farklı koordinatlı kaydın silinmesi reddedilir, SDK exception cause zincirinde kimlik çakışması korunur ve uzak kayıt değişmez. Kapı kapalıyken işlem uygulanmaz; eşleşen kayıt silinir; olmayan kaydı yeniden silmek başarılı no-op; aynı yola geçerli yeni kayıt yazılır.
- Emülatör Auth kullanıcısı başka hesabın profilini Android SDK üzerinden okuyamaz/yazamaz.

Bu tur üretim Kotlin iş akışı değiştirilmedi; test altyapısı ve debug loopback izni eklendi. Önceki 608 JVM, 13 UI/DataStore ve 24 JS emülatör testi yeniden koşulmadı. Yeni testler tam ThemeViewModel/ekran uçtan uca akışı değil; SDK + üretim yardımcıları + yerel kurallar kabulüdür. Zorlanmış eşzamanlı transaction retry, süreç ölümü, çevrimdışı bekleyen yazı, hesap değişimi sırasında geç commit ve Android üzerinden silme işareti yarışı hâlâ açık.

## Yeniden çalıştırma

Önkoşullar: Windows PowerShell, Node/npm bağımlılıkları (bu dizinde npm ci), JDK 17 JAVA_HOME, Android SDK ANDROID_HOME ve tek yetkili bağlı test cihazı. tools/firebase-rules-tests dizininde npm run test:android çalıştırılır. Demo emülatörleri ve kendi reverse portları hata halinde de kapatılır; önceden var olan doğru reverse eşlemeleri korunur. Kullanıcı arayüzü gerektirmeyen testlerdir. Firebase CLI/kütüphane uyarıları ayrı araç bakımı maddesi olarak izlenir.

## Sırada

SDK üzerinde ağ kesintisi/oturum değişimi/yarış kabulünü genişletmek ve eski istemcilerin yalnız ad yazıp stale haritayı bırakması için uyumluluk politikası. Canlı Firebase kuralları dağıtılmadı; canlı servis doğrulaması ve Play yayın kabul kapıları açık. Push/release yapılmadı.
