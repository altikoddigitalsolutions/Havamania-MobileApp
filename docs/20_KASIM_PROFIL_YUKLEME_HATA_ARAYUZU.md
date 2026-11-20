# 20 Kasım Profil yükleme ve hata ekranı

Profil verisi bulunmadığında ekran her durumda yükleme göstergesi gösteriyordu. Hata penceresi kapatılsa da arka planda Profil yükleniyor yazısı kalabiliyordu. Önbelleksiz çevrimdışı hesapta ise repository güvenli biçimde Loading durumunda kalırken kullanıcıya açıklama ve yeniden deneme sunulmuyordu.

## Düzeltme

ProfileUnavailableContent gerçek ProfileScreen'e bağlandı. İlk yüklemede açıklama ve Geri dön gösterilir. Sekiz saniye sonunda veri hâlâ gelmemişse sonsuz gösterge yerine Profil henüz yüklenemedi açıklaması ve Tekrar dene düğmesi görünür. Bu süre bağlantının kesin olarak kopuk olduğu anlamına gelmez; metin bağlantıyı kontrol etmeyi önerir.

Gerçek ProfileState.Error durumunda hata metni ve iki eylem hemen görünür. Mevcut hata penceresi kapatıldığında arka planda açıklamasız yükleme göstergesi kalmaz. Tekrar dene, gerçek ProfileViewModel.refresh çağrısını ve ekrandaki bekleme sayacını yeniden başlatır. Hesap değişimi bekleme sayacını sıfırlar; oturum yoksa yeniden deneme sunulmaz. Geri dön mevcut aktif ekran navigasyon callback'ini kullanır.

Bu UI değişikliği cache eksiğini sunucuda bulunmayan profil saymaz, otomatik hesap kaydı açmaz ve repository güvenlik kararlarını değiştirmez. Bağlantı geri geldiğinde mevcut dinleyiciyle toparlanma devam eder.

## Doğrulama

631 JVM testi ve tablette 6 UI testi geçti; sıfır atlama/hata. Yeni üç test büyük yazıda sekiz saniyelik gecikme, yeniden deneme sayacının sıfırlanması, hata metni, bağımsız geri dönüş ve hesap/çıkış durumunu sınar. Önceki üç profil hata penceresi testi de aynı koşuda geçti. Android debug derlemesi ve git diff --check başarılı.

Testler gerçek üretim yükleme bileşeniyle izole Compose kabulüdür. Tam ProfileScreen navigasyonu, süreç ölümü ve ilk kurulumdan çevrimdışı giriş uçtan uca kabulü açık. Gerçek geri dönüş callback bağlantısı kaynakta doğrulandı; tam uygulama gezinme testi bu koşuda yapılmadı. Önceki beş özel Firebase çevrimdışı senaryosu ayrı kanıttır.

## Sıradaki

Yeni yükleme/hata bileşenini gerçek üretim ViewModel'leriyle birleşik Activity testine ekleyip önbelleksiz çevrimdışı hesapta gecikme açıklaması, yeniden deneme ve ağ geri geldiğinde toparlanmayı aynı senaryoda doğrula. Tam ürün navigasyonu ve süreç ölümü kapıları açık kalır.