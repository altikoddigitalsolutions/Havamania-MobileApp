# Asistan platform v3 bağlantısı — 30 Ekim 2026

## Kodda tamamlananlar

- Backend köprü adresi `https://havamania.com/api/bridge`, zaman aşımı 50 saniye. Bearer anahtarı yalnızca sunucu ortamından okunur.
- Android yeni soruya UUID üretir; tekrar denemede aynı `request_id` ve konuşma kimliği kullanılır. Sorunun kendisi ile şehir/hava/seyahat bağlamı ayrı alanlardır.
- `context.weather_data` verilen sözleşmeye göre doğrulanır: saat farkı içeren gerçek veri zamanı, IANA saat dilimi, en fazla 10 ardışık ve benzersiz tarih, sayısal sınırlar ve null alanlar hariç 4096 UTF-8 bayt. Diğer bağlamın 8 KB sınırı ayrıdır. Bilinmeyen alanlar atılır; koordinat gönderilmez.
- Android yalnızca mevcut ve doğrulanabilir hava alanlarını gönderir. Eksik kaynağa veya ölçüme değer uydurmaz. Eski veri ve gelecek zaman sapması işaretlenir.
- Zaman aşımında otomatik model tekrarı yapılmaz. Kullanıcının tekrar denemesi ilk platform gövdesini ve kimliğini korur. Başarılı yanıt yerel tekrar kaydından dönebilir.
- Günlük kota Europe/Istanbul gününe göre atomik ayrılır; başarısız işlemde iade edilir. Başarılı tekrar ikinci kez düşmez.
- `user_deleted`, `request_in_progress` ve kimlik/içerik çatışmaları ayrı hata durumlarıdır. Android bunları başarılı asistan yanıtı gibi göstermez.
- `personalization:false` profil, seyahat ve sağlık bağlamını kaldırır. Yerel sağlık izni engeli yeniden denemelerde de uygulanır.

## Tekrar kaydı ve veri saklama

Migration `0019_chatbot_replay`, ilk platform gövdesini ve başarılı yanıtı kullanıcıya bağlı saklar. Başarılı işlemde gönderim gövdesi temizlenir; yanıtın tekrar penceresi 24 saattir. Başarısız işlem gövdesi, izin verilen profil/sağlık/seyahat bilgilerini içerebilir. Hesap silindiğinde bağlı kayıtlar da silinir.

24 saat dolması tek başına fiziksel temizleme değildir. Sunucuda düzenli görev olarak `python -m app.services.chatbot_replay` çalıştırılmalıdır; süre aşımındaki gövde ve yanıtları temizler. Bu görev henüz canlı sunucuya kurulmadı.

Tekrar kimliği aynıyken hava yenilenirse ilk snapshot korunur. Soru, şehir, seyahat, profil, mod, ton veya gizlilik tercihi değişirse içerik çatışması oluşur; yeni soru için yeni kimlik gerekir. Eski sürümden kalan, platform kimliği bilinmeyen kayıtlar güvenli biçimde yeniden çalıştırılmaz.

## Yerel doğrulama

- Yeni sözleşmeyi kapsayan hedefli backend paketi: 77 test geçti.
- İzole PostgreSQL: 13 test geçti; migration, eşzamanlı aynı kimlik, tek kota tüketimi, süre sonu temizliği ve kullanıcı silme kapsamda.
- Android: 236 birim testi geçti, debug APK derlendi.
- Önceki sağlık izni düzeltmesi: tablette 8 izole cihaz testi geçti.

Platform HTTP çağrıları testlerde sahtedir; bu sonuçlar canlı servis doğrulaması değildir.

## Canlıya geçişte kalanlar

1. Platform v3 dağıtımının tamamlandığı doğrulanmalı.
2. Panelde üretilen anahtar doğrudan backend secret ortamına girilmeli; mesajla veya Git ile paylaşılmamalı.
3. Migration uygulanmalı; tekrar kaydı temizleme görevi kurulmalı. İstanbul gününe geçişin mevcut günlük sayaçlara etkisi dağıtım anında kontrol edilmeli.
4. `.env.example` platform izin kaynağını kullanır. Canlı ortamda da `ASSISTANT_CONSENT_SOURCE=platform` seçilmeli; gerçek anahtarla `/consent` erişimi doğrulanmalı. Bu dosyanın değişmesi canlı ortamı değiştirmez.
5. Gerçek izin alma/geri çekme, konuşma devamı, zaman aşımı sonrası aynı kimlik ve kullanıcı A–B izolasyonu uçtan uca denenmeli.
6. Sağlık verisinin hukuki dayanağı, saklama süresi ve yurt dışı aktarım açıklamaları yayın öncesi onaylanmalı.

Canlı bağlantı ve dağıtım bu çalışma sırasında yapılmadı. Tablet ekranındaki izin metni alınamaması hatasının canlı sunucu nedeni henüz doğrulanmadı.

Son tam backend koşusu: 188 geçti, 13 PostgreSQL testi bu koşuda atlandı; bu 13 test ayrı izole PostgreSQL koşusunda geçti. Ruff başarılı. Kütüphanelerden 6 kullanım dışı API uyarısı mevcut.

## Ek doğrulama — HTTP önbelleği ve hata akışı

Asistan yollarına merkezi ASGI koruması eklendi: Cache-Control: no-store ve Pragma: no-cache. Başarılı/tekrar yanıtları yanında kimlik doğrulama, şema doğrulama ve platform hata cevapları da kapsamda; diğer yollar etkilenmez. Bu HTTP önbellek koruması, sunucudaki 24 saatlik kontrollü tekrar kaydını kaldırmaz.

Platformdan gelen 409 user_deleted / request_in_progress / conversation_conflict ve 422 request_id_conflict ayrı ayrı doğrulandı. Hatalarda kota iade edilir, upstream mesajı aktarılmaz. İşleniyor hatasından sonra tekrar aynı kimlik ve gövdeyi kullanır; başarılı olunca kota bir kez düşer. Sahte HTTP transport ile 65 ilgili regresyon testi geçti, Ruff ve git diff --check başarılı. Bu adımda Android kaynakları ve veritabanı şeması değişmedi; canlı servis çağrısı yapılmadı.

## Ek düzeltme — izin sürümü değişince eski isteği kullanmama

Tekrar kontrolünün parmak izine çözümlenen asistan/sağlık izin sürümleri eklendi. Önceden, istemci consent_version göndermemişse yeni metin onaylandıktan sonra eski sürümlü dondurulmuş gövde tekrar kullanılabiliyordu. Artık yeni onaydan sonra eski kimlik 422 request_conflict verir; ne platforma gönderilir ne eski başarılı cevap döndürülür. Yeni kimlik güncel sürümle çalışır.

Zaman aşımında kalan istek ve tamamlanmış yanıt senaryoları ayrı regresyon testleriyle doğrulandı. İlgili paket: 33 geçti; Ruff ve diff kontrolü başarılı. Önceki parmak iziyle oluşturulan kayıtlar da güvenli biçimde çatışma verir; yeni kimlik gerekir. Şema migration'ı gerekmez. Canlı doğrulama ve dağıtım açık.

## Ek düzeltme — eksik veya bozuk tekrar kaydı

Başarısız kaydın platform_payload alanı boşken güncel gövdeden yeniden istek oluşturma kaldırıldı. Kayıt kullanıcı/istek kimliği, soru, konuşma, mod, ton, kişiselleştirme ve izin sürümleri ile eşleşmeli. Eksik veya tutarsız kayıt 409 request_expired ile kapanır; platform çağrısı ve yeni kota rezervasyonu yapılmaz. Tamamlanmış fakat eksik/geçersiz yanıt da sonsuz işleniyor veya sunucu hatası yerine aynı güvenli terminal durumuna geçer. Önceden başarılı işlem için tüketilen kota korunur.

8 yeni bozuk kayıt senaryosu dahil ilgili backend paketi 41/41 geçti. Sağlam zaman aşımı tekrarları ve izin sürümü değişimi regresyonları da geçti. Ruff ve diff kontrolü başarılı. Android RequestExpired durumunu zaten terminal hata olarak işler. Migration ve Android kaynak değişikliği gerekmedi; canlı çağrı yapılmadı.

## Ek doğrulama — fiziksel temizleme sınırı

Tekrar kaydının tam replay_until anındaki geçersizlik ve temizleme koşulları eşitlendi (<=). Altı yeni senaryo: başarılı/yarım kalmış kayıtlar, süreden bir saniye önce/tam bitişte/bir saniye sonra. Başka owner ile temizleme veriyi değiştirmez; süresi dolmamış veri korunur. Süresi dolan gövde/yanıt temizlenirken kimlik ve parmak izi korunur. İlgili regresyon paketi 47/47, Ruff başarılı. Sunucuda periyodik temizleme görevinin kurulması hâlâ açık; testler bunun yerine geçmez.

## Ek düzeltme — hava verisinde UTF-8 boyut sınırı

Hava snapshot boyutu boşluksuz JSON'un UTF-8 bayt sayısıyla ölçülür. Önceki json.dumps varsayılan boşlukları sınıra dahil ederek, geçerli bir 4096 baytlık nesneyi gereksiz yere reddedebiliyordu. Null ve bilinmeyen alanlar ayıklandıktan sonra ölçüm yapılır. Yeni regresyonlar Türkçe/çok baytlı karakterlerle 4095 ve 4096 baytın kabulünü, 4097 baytın reddini ve null/bilinmeyen alanların bütçeye katılmamasını kapsar. Platformun canlı şema uygulaması ayrıca doğrulanmalıdır.

Bu adımın doğrulaması: yeni 4 boyut testi ve platform v3 regresyonları dahil 40/40 geçti; Ruff başarılı. Android ve migration değişmedi; canlı çağrı yapılmadı.

## Ek düzeltme — geçersiz saat farkını normalleştirmeme

Python datetime.fromisoformat, +03:99 gibi geçersiz saat farklarını +04:39 olarak normalleştirebilir. Şemaya saat/dakika fark bileşenleri için açık kontrol eklendi; bu girdiler artık reddedilir. Geçerli Z, negatif farklar ve +05:45 gibi çeyrek saat farkları korunur; saniye kesri metni değiştirilmez. Bu düzeltme mevcut 4096 bayt sınırı çalışmasıyla aynı yerel değişiklik grubundadır. Geliştirme başlatma komutlarında değişiklik yapılmadı.

Son doğrulama: 10 yeni saat farkı senaryosu ve önceki regresyonlar dahil 50/50 test geçti. Ruff/diff kontrolü başarılı; canlı çağrı ve push yapılmadı.

## Ek düzeltme — güvenli istek doğrulama hataları

API testlerinde NaN/Infinity hava değerlerinin FastAPI'nin varsayılan doğrulama cevabında JSON'a çevrilememesi ve diğer hatalarda giriş değerlerinin geri yansıtılması yeniden üretildi (4 test düzeltme öncesi başarısız). Asistan yollarına özel RequestValidationError handler eklendi: 422 cevabı yalnızca alan yolu, hata türü ve sabit mesaj içerir; input ve ctx serileştirilmez. no-store korunur. Diğer API yolları mevcut FastAPI handler'ına devredilir. Geçersiz istekte platform çağrısı yapılmaması ve kotanın değişmemesi regresyon kapsamındadır.

Doğrulama: 4 yeni API regresyonu dahil ilgili paket 65/65 geçti; Ruff/diff kontrolü başarılı. Test HTTP'si sahte, canlı çağrı yapılmadı. Bu düzeltme ve son hava boyutu/saat kontrolleri henüz push edilmedi.

## Ek düzeltme — köprü hatasının traceback gizliliği

Sohbet köprüsünün güvenli BridgeError mesajı, alttaki transport/Pydantic hatasına açık exception chain ile bağlıydı. Bağlantı, timeout, okuma ve yanıt doğrulama hataları artık `from None` ile standart traceback'te alttaki özel detayları göstermez. Silme köprüsündeki mevcut davranışla tutarlıdır. Güvenli hata kodları, timeout sayacı ve yalnızca connect hatasındaki tek tekrar korunur.

5 yeni senaryo dahil köprü regresyonları 39/39 geçti. Testler standart traceback metninde özel sentinel değerinin görünmediğini, cause zincirinin bastırıldığını ve çağrı sayılarını doğrular. Ruff/diff kontrolü başarılı. Bu, ham exception context'ini bellekten silmez; Sentry/altyapı tarafındaki body, local-variable ve log saklama ayarlarının canlı denetimi ayrıca açıktır. Gerçek servis çağrısı ve push yapılmadı.

## Ek düzeltme — backend Sentry hata raporu gizliliği

Sentry kurulumu artık send_default_pii=False, max_request_body_size=never ve include_local_variables=False kullanır. before_send filtresi hata raporunda request, user, breadcrumbs, extra, contexts, tags, logentry ve serbest mesaj alanlarını taşımaz. Exception değeri sabit mesajla değiştirilir; hata türü, modülü ve filename/function/lineno gibi stack konumu korunur. Frame vars ve kaynak bağlamı çıkarılır. Böylece rapor daha az ayrıntılıdır; teşhis hata türü ve kod konumuna dayanır.

5 yeni test dahil ilgili gizlilik/köprü paketi 44/44 geçti. Kurulu Sentry SDK ile gerçek envelope üretimi de bellek içi Transport üzerinden doğrulandı; dış servis veya gerçek DSN kullanılmadı. DSN yokken SDK başlatılmaz. Girdi nesnesinin değişmediği de doğrulandı. Ruff/diff kontrolü başarılı.

Bu koruma backend hata raporları içindir. Canlı Sentry projesi saklama/erişim ayarları, proxy/uygulama stdout logları ve ayrıca etkinleştirilebilecek transaction/profiling kanalları henüz uçtan uca denetlenmedi. Mevcut eski raporları silmez; dağıtım ve canlı doğrulama açıktır. Son yerel değişiklikler push edilmedi.

## Ek kontrol — backend metrikleri

Dinamik URL değerlerinin metrik etiketine girmemesi mevcut rota şablonu yaklaşımıyla korunuyor. Yeni düzeltme: standart dışı HTTP yöntemleri tek OTHER etiketine toplanır; rastgele yöntem isimleri ayrı Prometheus zaman serileri üretmez. Beklenmeyen Exception durumunda gecikme ve 500 sonucu bir kez kaydedilir, hata normal handler'a yeniden iletilir. İptal gibi BaseException durumları yakalanmaz.

3 yeni middleware testi; dinamik/eksik yollar, 20 farklı özel yöntem ve 500 senaryosunu kapsar. Metrik endpoint erişimi ve Sentry testleriyle birlikte 10/10 geçti. Ruff başarılı. Ölçüm middleware'i testlerinde gerçek servis yoktur; stdout/proxy erişim loglarının canlı denetimi bu kontrolle kapanmaz. Son yerel değişiklikler push edilmedi.

## Ek düzeltme — takip kimliği ve genel 500 logu

X-Request-ID yalnızca tek başlık ve standart 36 karakter UUID ise korunur (büyük harf normalize edilir). Boş/uzun/kontrol karakterli/tekrarlı/serbest metin başlıkları yeni UUID ile değiştirilir. UUID olmayan eski serbest metin takip kimlikleri artık aynen yansıtılmaz; bu kimlik, asistanın request_id tekrar anahtarından ayrıdır.

Genel 500 handler'ı artık ham exception mesajı veya zinciri loglamaz. Hata türü, takip kimliği, rota şablonu ve dosya adı/satır/fonksiyon bilgisi korunur; ham dinamik URL ve hata girdileri yazılmaz. 500 cevabı no-store ve mevcut takip kimliği başlığı içerir. Testteki özel exception/URL değerleri loga taşınmadı. Bu azaltılmış hata ayrıntısı, önceki Sentry filtresiyle tutarlıdır; ayrı altyapı/access loglarını değiştirmez.

7 yeni takip testi + metrik/Sentry regresyonları 15/15 geçti. Tam backend koşusunun sonucu ayrıca kaydedilecektir. Canlı servis çağrısı yok; son yerel değişiklikler push edilmedi.

Son tam backend doğrulaması: 247 geçti, ayrı PostgreSQL ortamı isteyen 13 test atlandı; 6 bağımlılık deprecation uyarısı kaldı. Bu adımda PostgreSQL audit yeniden çalıştırılmadı. Ruff ve diff kontrolü başarılı. Son yerel kaynak değişiklikleri push edilmedi.
