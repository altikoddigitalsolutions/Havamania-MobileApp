# 24 Kasım Birleşik kayıt ve kişiselleştirme SDK kabulü

Gerçek AuthViewModel, ProfileViewModel, demo Auth/Firestore SDK, üretim kişiselleştirme formu, ortak başlangıç yönlendirmesi ve kullanıcıya bağlı QA DataStore kaydı aynı debug Activity senaryosunda doğrulandı. Yeni üretim hatası bulunmadı; önceki düzeltmenin birleşik kabulü ilerledi.

## Doğrulanan akış

Yeni demo hesabı AuthViewModel üzerinden oluşturuldu. Kayıt transaction'ı öncesinde bekleme uygulandı. Bu sırada gerçek ProfileViewModel eksik sunucu profilini oluşturdu; onboardingCompleted=false sunucudan doğrulandı. Profil hazır olsa bile AuthViewModel Loading/currentUser=null durumundayken kişiselleştirme veya hava hedefi açılmadı.

Bekleyen kayıtta gerçek Activity.recreate yapıldı. İki ViewModel'in aynı örneklerinin korunduğu doğrulandı. Bekleme kaldırılınca doğru UID profiliyle kişiselleştirme açıldı; hava hedefi erken görünmedi. Gerçek formda Meteoroloji seçildi. Tekrar recreate sonrasında FilterChip seçimi korundu. TERCİHLERİ KAYDET ile gerçek ViewModel/Firestore/local-pref akışı çalıştı; ardından hava hedefine geçildi.

Sunucudan okunan onboardingCompleted=true, kullanıcı adı ve meteoroloji ilgi alanı doğruydu. Aynı tercih gerçek ProfileViewModel tarafından geri okundu ve gerçek QA DataStore'da doğru UID altında bulundu. Yerel onboarding bayrağı da doğruydu. Yanlış UID veya writesBlocked=true ile yapılan yerel yazma denemeleri tercihi değiştirmedi. Hava hedefinde recreate sonrası giriş/kayıt/bekleme geri entry'si yoktu. Gerçek signOut ile başlangıç rotasına dönüldü.

## Mimari değişiklikler

ProfileViewModel internal constructor'ına isteğe bağlı personalizationBasicsWriter sınırı eklendi; public Android constructor ve varsayılan üretim yolu korundu. Böylece demo SDK hesabı için varsayılan Firebase oturumuna başvurmadan aynı yerel eşleme sınanabiliyor.

ThemeManager.savePersonalizationBasics için açık DataStore/oturum/engel/yazı nesli alan overload eklendi. Üretim Context wrapper'ı da aynı eşlemeyi kullanıyor. UID kontrolü, hesap silme engeli ve PreferenceWriteEpoch koruması korunuyor. Test normal üretim kullanıcı ayarlarına değil yalnız demo UID'ye yazıyor; sonunda ViewModel/listener'lar kapanıyor, yazı nesli geçersiz kılınıyor ve yalnız demo UID anahtarları temizleniyor.

AccountEntrySdkActivity debug-only ve dışa kapalıdır. Factory zorunludur; varsayılan canlı SDK fallback'i yoktur. Auth/Profile modelleri ve kişiselleştirme gövdesi gerçektir. Giriş ve hava gövdeleri işaretçidir.

## Sonuç ve kapsam

npm run test:android:registration artık eski kayıt retry/merge SDK testi ile yeni birleşik Activity testini çalıştırıyor. Son koşu Samsung SM-T500 Android 12 üzerinde 2 test, 0 hata, 0 atlanan verdi. Genişletilen birleşik senaryoda ProfileViewModel geri okuması ve yanlış UID/engel kontrolleri de son koşuda yeniden doğrulandı. Son kaynakla 656 JVM testi yeniden çalıştı ve hatasız geçti. Git diff --check temiz.

Yalnız named FirebaseApp, demo-havamania-audit ve localhost emülatörleri kullanıldı. Emülatörler ve geçici ADB reverse bağlantıları kapandı. Play uygulamasına ve canlı hesaplara işlem yapılmadı. Bu süreç ölümü değildir; recreate ViewModel örneklerini korur. Tam WeatherPremiumActivity, hava verisi, splash/izin akışı, bildirim/deep link ve bütün ürün ekranlarının kabulü kapanmadı. Şimdilik Atla ve kayıt sürerken recreate bu senaryonun kapsamı değildir.

## Sıradaki sıra

1. Yarım kayıt hazırlığının süreç tamamen kapandıktan sonra kalıcı takibi ve güvenli toparlanması.
2. Çıkış sonrası geciken SDK Task/oturum değişimi; başlamış sunucu yazısı coroutine iptaliyle geri alınmaz.
3. Kişiselleştirme kaydı sürerken recreate/geri, Şimdilik Atla, ayarlardan manuel düzenleme ve soğuk deep link/bildirim girişleri.
4. Tam WeatherPremiumActivity ve üretim servisleriyle son kabul; mevcut mağaza/release kontrolleri açık kalır.

Kişisel gradle.properties korundu. Commit/push yapılmadı.