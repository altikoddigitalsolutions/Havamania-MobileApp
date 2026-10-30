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
