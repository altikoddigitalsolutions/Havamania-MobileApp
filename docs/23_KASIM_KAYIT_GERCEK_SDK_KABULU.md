# 23 Kasım Kayıt hazırlığının gerçek SDK kabulü

Önceki kayıt hazırlığı düzeltmesi gerçek AuthViewModel ve demo Firebase Auth/Firestore SDK'larıyla doğrulandı. Tek SDK senaryosu sıfır hata ve sıfır atlanan testle geçti. Bu tur yeni bir üretim hatası bulunmadı; test edilebilirlik sınırı ve tekrar çalıştırma komutu eklendi.

## Doğrulanan zincir

Ayrı named FirebaseApp, demo-havamania-audit projesi ve yalnız localhost Auth/Firestore emülatörleri kullanıldı. Üretim AuthViewModel'in public Application constructor'ı korundu; internal constructor'a SDK bağımlılıkları ve transaction öncesinde bekletme/hata uygulayan isteğe bağlı test callback'i eklendi. Normal üretim yolunda callback yoktur.

Gerçek e-posta/parola SDK hesabı yalnız demo emülatöründe oluşturuldu. Gerçek AuthStateListener oturum olayını aldıktan sonra, profil yazımı bekletilirken ViewModel Loading durumunda kaldı; Activity'nin yönlendirme için kullandığı currentUser null döndü. Yinelenen kayıt çağrısı ikinci profil yazımı başlatmadı.

İlk profil yazımı öncesinde kontrollü IOException üretildi. ViewModel Error durumuna geçti; SDK hesabı kaldı fakat yönlendirme kullanıcısı null olarak korundu. Yeniden deneme aynı UID ile ilerledi; hesap yeniden oluşturulmadan gerçek SDK ad güncellemesi ve gerçek Firestore transaction tamamlandı. Authenticated ve yönlendirmeye açılan UID doğruydu. Çıkışta Idle/null doğrulandı.

Transaction öncesinde demo hesaba mevcut profil belgesi yazıldı. Transaction'dan sonra sunucudan okunan ad/e-posta güncel; biyografi, ilgi alanları, onboardingCompleted ve createdAt=123 korunmuş; updatedAt eklenmişti. Böylece merge ve mevcut oluşturulma tarihini koruma gerçek SDK/kural yolu üzerinde doğrulandı.

## Çalıştırma ve sonuç

Test komutu tools/firebase-rules-tests dizinindeki npm run test:android:registration. Komut yalnız demo emülatörlerini açar, izinli tek cihazda com.havamania QA paketini kullanır ve test sonunda emülatörleri/eklediği ADB reverse bağlantılarını kapatır. Son kontrolde ADB reverse listesi boştu. com.havamania.mobile Play paketine işlem yapılmadı.

Samsung SM-T500 Android 12: FirebaseAuthRegistrationDeviceTest için XML 1 test, 0 hata, 0 atlanan. Ardından son kaynakla testDebugUnitTest yeniden çalıştı: 653 test, 0 hata, 0 atlanan. Git diff --check temiz. Test kaynak dosyasının ilk yazımında çalışma dizini düzeltilmişti; çalıştırılan son SDK paketi doğru test kaynağıyla derlendi ve geçti.

## Kapsam sınırı ve sıradaki işler

- Bu AuthViewModel/SDK kabulüdür. Tam Activity/NavHost, ProfileViewModel ve DataStore zinciri bu senaryoda birlikte çalıştırılmadı. currentUser kapısının doğrulanması gerçek ekrandaki rota değişiminin tamamlandığı anlamına gelmez.
- İlk hata transaction öncesine enjekte edildi; fiziksel internet kesintisi veya gerçek SDK transport hatası değildir. Gerçek transaction başarılı yeniden denemede çalıştı.
- Mevcut profil belgesi ve onboardingCompleted=true alanının korunması sınandı. Yeni belge oluşturma, onboardingCompleted=false kullanıcısının kişiselleştirme hedefine gitmesi ve geri/Activity yenileme sıradaki kabul.
- Süreç kapanınca yarım kayıt hazırlığı işaretinin kalıcılığı, tekrar açılış ve çıkıştan sonra geç SDK oturum değişimi açık. Canlı Firebase veya mağaza kabulü kapanmadı.

Canlı hesap/servis veya Firebase kural dağıtımı yapılmadı. Kişisel Gradle ayarı değiştirilmedi; commit/push yapılmadı.