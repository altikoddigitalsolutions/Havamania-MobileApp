# B5-1 — Otomatik rehber yerel kabulü

## Düzeltilenler

1. Bildirim/hedef seyahat odağı, Room her yeni liste yayımladığında eski tarihi tekrar seçiyordu. Odak artık ekran oturumunda bir kez uygulanır; başarılı yeni kayıt bekleyen eski odağı da tüketir.
2. Room silinmiş hesap/kayıt veya başka kullanıcıya ait aynı kimlikteki kaydı reddettiğinde ViewModel başarı callback'i çalışabiliyordu. Yerel kayıt artık yazılan sürümü veya null döndürür; yalnızca gerçekten yazılmış kayıt rehberi açabilir.
3. Telefon düzeninde yükseklik animasyonu ve bir piksellik kaydırma hedefi rehberin ekran dışında kalmasına yol açıyordu. Rehber doğrudan yerleşir; otomatik kaydırma açıklama metninin gerçek alanını görünür kılar.
4. Rehber metnine dokunma, kapsayıcı seyahat kartının güzergâh eylemine ulaşabiliyordu. Rehber açıkken kapsayıcının genel tıklaması kaldırıldı; rehber ve açık işlem düğmeleri kullanılabilir kalır.
5. Takvim hata mesajlarını SnackbarHostState'e gönderiyor ama SnackbarHost çizmediği için kullanıcı çift kayıt gibi hataları göremiyordu. Ortak ekran bileşenine snackbar alanı eklenip takvime bağlandı. Kullanıcıya gösterilen geçmiş tarih mesajından teknik kod ön eki kaldırıldı.

## Son doğrulama

Samsung SM-T500 / Android 12 üzerinde gerçek TravelPlannerScreen, seyahat formu, TravelViewModel ve Room ile **7/7 cihaz testi geçti**:

- 390 dp telefon düzeninde yeni kayıt sonrası rehberin otomatik görünmesi.
- Tablet düzeninde yeni kayıt sonrası rehberin otomatik görünmesi.
- Eski bildirim hedefinin geç gelmesinin yeni rehberi gizlememesi.
- Arşiv filtresinden yeni kayıt oluşturunca doğru yaklaşan seyahat rehberinin açılması.
- Düzenleme sonrası rehberin açılması ve ikinci kayıt oluşmaması.
- Çift kaydın reddedilmesi, hatanın görünmesi ve rehberin açılmaması.
- Yanlış hesap, geçersiz koordinat, geçmiş tarih, silme işareti ve başka hesaba ait aynı kimlik için başarı callback'inin ve yerel yazmanın engellenmesi.

Yeni kayıt testleri ayrıca Room yeniden yayını/yükleme sonrasında rehberin görünür kaldığını, metne dokunmanın güzergâh açmadığını ve sistem geri hareketinin önce rehberi kapattığını doğrular.

**457 JVM testi hatasız geçti.** Aynı geliştirme turunda mevcut 4 seyahat silme/kuyruk/migration testi ve 1 analiz sürüm/hesap/silme testi de tek tek başarılı oldu. İlk cihaz turlarındaki seçici hataları, kilitli ekran ve yakalanan gerçek görünürlük/hata bildirimi sorunları giderildikten sonra yukarıdaki 7 testin tamamı yeniden geçti.

## İzolasyon ve sınırlar

- Test uygulama kimliği com.havamania; Play'deki com.havamania.mobile kurulumu değiştirilmedi.
- Room yalnızca bellek içi test veritabanıdır. Geocoding sabit test sonucu, rehber yenilemesi no-op ve bağlantı gözlemcisi çevrimdışı olarak sağlandı. Gerçek koordinat araması veya canlı hesaba yazma yapılmadı.
- Telefon düzeni tablette 390 dp genişlikte sınandı; fiziksel telefonda kabul yerine geçmez. Hazır konum verisiyle çevrimdışı kayıt test edildi; çevrimdışı şehir araması doğrulanmış sayılmaz.
- Başka hesapla kaydetme ve kayıt kimliği izolasyonu sınandı. Gerçek Firebase A→B oturum değişimi, fiziksel telefon, yeniden oluşturma/süreç ölümü ve Play sürümündeki son kabul ayrıca açıktır.
- Bu tur yeni release AAB üretmedi. Bağlı test için ayrı QA paketleri derlendi.

Yerel ana akış doğrulandı. B5-1 geniş kapanışı kalan cihaz/oturum koşulları nedeniyle açık tutulur; sıradaki bağımsız yerel iş B5-2 konum izin dayanıklılığıdır.
