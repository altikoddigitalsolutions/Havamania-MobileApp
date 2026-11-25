# 24 Kasım Hesap başlangıcı ve kişiselleştirme yönlendirmesi

Oturum açıldığında yeni NavController'ın doğrudan hava rotasından başlaması düzeltildi. Önceki kişiselleştirme kontrolü yalnız giriş rotalarında çalıştığı için yeni kullanıcı hava ekranına geçebiliyordu. Authenticated kullanıcı artık ACCOUNT_LOADING başlangıcından geçiyor; yalnız aynı UID'ye ait profil geldiğinde kişiselleştirme veya hava hedefi seçiliyor.

## Üretim değişiklikleri

- AccountEntryRedirect ortak bootstrap yönlendirmesini yönetiyor. Onboarding tamamlanmamışsa kişiselleştirme; tamamlanmışsa hava seçiliyor. Profil yoksa veya önceki hesaba aitse bekleme korunuyor. Hesap yokken geç gelen profil oturumu açmıyor.
- Bekleme rotası gerçek ProfileUnavailableContent bileşenini kullanıyor: gecikme açıklaması, hata, retry ve çıkış mevcut. Sistem geri ve Geri dön oturumu kapatıyor. Aynı UID'nin normal ekranları profil güncellemelerinde zorla başka rotaya taşınmıyor; yönlendirme bootstrap rotalarıyla sınırlı.
- İlk back stack girdisi oluşmadan kullanılan hava varsayımı kaldırıldı. Bekleme ve kişiselleştirmede alt sekmeler gizlendi. Bildirim yönlendirme kapısı ACCOUNT_LOADING rotasında da bekliyor; mevcut bildirim birim testi genişletildi.
- Grafiğin başlangıcı geçici bekleme rotasına dönüştüğü için sekme ve Profil → Seyahatler işlemleri accountTabOptions ile gerçek WEATHER_ROOT kökünü kullanıyor. Bekleme rotasının geri yığınına yeniden girmesi önleniyor.
- İlk kişiselleştirmede üst geri düğmesinin kökte etkisiz kalmaması için güvenli çıkış eklendi. Alt yığın varsa önceki ekrana dönüyor; kökteyse oturum kapanıyor. Eski ekran callback'i güncel entry ile eşleşmiyorsa işlem yapmıyor.

## Doğrulama

Son kaynakla 656 JVM testi, sıfır hata ve sıfır atlanan sonuç verdi. Yeni AccountEntryTargetTest üç senaryoda UID ayrımını, onboarding hedefini ve çıkış sonrası geç profili kapsıyor. Bildirim bekleme kontrolü mevcut test içinde doğrulandı.

Samsung SM-T500 Android 12 üzerinde AccountEntryNavigationDeviceTest: 5 test, 0 hata, 0 atlanan. Gerçek ActivityScenario.recreate, gerçek NavHost/NavController ve üretim yönlendirme/bekleme/sekme seçenekleri kullanıldı:

1. Profil gecikmesi ve beklerken recreate; yeni kullanıcıda kişiselleştirme; tamamlamadan hava hedefi yok; tamamladıktan ve yeniden oluşturmadan sonra giriş/kayıt/bekleme önceki entry olarak dönmüyor.
2. Profil hatası, retry callback'i, sistem geriyle güvenli başlangıç ve çıkış sonrası geç profilin hesabı yeniden açmaması.
3. A → B hesap değişimi ve recreate sırasında eski A profilinin B'nin hava hedefini açmaması.
4. Hava → takvim → profil → takvim geçişinde önceki entry'nin hava kökü olması; gerçek sistem geriyle havaya dönüş.
5. İlk kişiselleştirmedeki üst geri callback'inin güvenli başlangıca çıkması.

Test Activity'si debug-only ve dışa kapalıdır; enjekte edilmiş yerel StateFlow'lar kullanır. Hedef ekran gövdeleri test işaretçileridir. Bu WeatherPremiumActivity'nin bütün ürün ekranlarıyla, AuthViewModel/ProfileViewModel/Firebase SDK ile uçtan uca kabulü değildir. Kişiselleştirme formunun kendi alanları veya kaydetme servisi bu testte çalıştırılmadı.

## Test altyapısında düzeltilenler

İlk derleme geçici KSP yedeğinde eksik dosya nedeniyle durdu. Yalnız test komutunda tırnaklı -Pksp.incremental=false kullanılarak aşıldı; ilk PowerShell parametre bölünmesi de düzeltildi. Proje Gradle ayarına eklenmedi.

İlk cihaz koşuları rota beklemesinde zaman aşımına uğradı. Test beklemesinin Compose saatini ilerletmesi eklendi. Asıl test gözlem hatası da giderildi: debug Activity rota durumunu yalnız SideEffect içinde okuduğu için değişen rota kompozisyon tarafından izlenmiyordu. Rota kompozisyon içinde okunup SideEffect'e değer olarak verildi. Bu test fixture hatasıydı; üretim Activity zaten aktif rotayı kompozisyon içinde okuyordu. Son koşu tüm beş testi yeniden çalıştırıp geçti. Git diff --check temiz.

## Sıradaki kabul

1. Önceki gerçek SDK kabulünü bu navigasyon akışıyla birleştir: yeni profil belgesinin oluşması, gerçek Auth/Profile ViewModel'leri ve onboarding=false kullanıcısının hedef rotası tek Activity senaryosunda doğrulanmalı.
2. Tam WeatherPremiumActivity, kişiselleştirme formu kaydı ve ayarlardan manuel kişiselleştirmeye girip geri çıkma; soğuk deep link/bildirim girişleri ayrıca sınanmalı. Kaynak kapıları bu tur güncellendi, bütün giriş yolları kabul edilmiş sayılmıyor.
3. Süreç kapanmasında kalıcı yarım kayıt işareti/toparlanma ve çıkıştan sonra geç SDK oturum değişimi açık.

Canlı Firebase/hesap/servis işlemi veya Play paketine yükleme yapılmadı. Kişisel gradle.properties korundu. Commit/push yapılmadı.