# Asistan rıza sözleşmesi — yerel backend, 30 Ekim 2026

Bu sözleşme sunucu tarafındaki temel korumayı açıklar. Android onay/geri çekme ekranı yerel sözleşmeye bağlandı; platform panelinin canlı metin servisi henüz bağlanmadı. Hukuken onaylanmış metinler yerine örnek metin üretilmedi. Canlıya dağıtım yapılmadı.

## Yapılandırma ve yayın sırası

1. Önce Alembic `upgrade head`: 0014 istek kaydı ve 0015 rıza tabloları uygulanmalı.
2. Onaylanmış metin ve pozitif sürüm sunucuda ASSISTANT_CONSENT_TEXT / ASSISTANT_CONSENT_VERSION olarak tanımlanmalı. Ayrı sağlık metni/sürümü ASSISTANT_HEALTH_CONSENT_TEXT / ASSISTANT_HEALTH_CONSENT_VERSION ile isteğe bağlıdır.
3. Bu sürüm metni ortam yapılandırmasından okur; değişiklik süreç yeniden başlatıldığında yüklenir. Platformun canlı metin/önbellek adaptörü henüz yok.
4. Android metin gösterimi, açık onay ve Ayarlar’dan geri çekme eklendi. Yayın öncesi gerçek oturumla backend–Android entegrasyonu ve onaylanmış metin yapılandırması birlikte doğrulanmalı. Metin/sürüm eksikse uzak ask 503; metin hazır ama kullanıcının güncel kaydı yoksa 428 olur. Önceki Android istemcisi otomatik onaylı sayılmaz.

## Kimlik ve uçlar

GET ve POST `/v1/chatbot/consent` Firebase/backend oturum doğrulamasını kullanır. Kullanıcı kimliği yalnızca doğrulanan hesaptan türetilir. Başarılı yanıtlar `Cache-Control: no-store` içerir.

GET cevabı: `version`, `text`, `text_hash`, `health` (aynı üç alan veya null), `accepted`, `health_accepted`, `health_review_required`. Son alan, önceden verilmiş sağlık onayının metni/sürümü değiştiyse true olur; istemci sağlık kutusunu yeniden ve işaretsiz göstermelidir.

POST alanları:

| Alan | Davranış |
|---|---|
| assistant_accepted | Zorunlu gerçek JSON boolean. False genel ve sağlık onayını geri çeker. |
| consent_version / consent_hash | True kabulünde GET ile kullanıcıya gösterilen metnin sürümü ve text_hash değeri gerekir. |
| health_accepted | Varsayılan false; genel onaydan ayrı, isteğe bağlı boolean. |
| health_consent_version / health_consent_hash | Sağlık true ise GET health nesnesindeki güncel sürüm ve özet gerekir. |

Gösterim ve kabul arasında metin veya sürüm değişmişse 409 dönülür; yeni metin tekrar gösterilmelidir. False geri çekme için metin servisi/config hazır olmak zorunda değildir. Sunucu kaydı ve audit olayı aynı transaction içinde yazılır. Kayıtlar boolean, sürüm, SHA-256 metin özeti ve sunucu zamanı içerir; sağlık/sohbet metni içermez. Eski metnin birebir arşivi henüz tutulmuyor; onaylanmış metin sürümlerinin değişmez arşivi platform/hukuk işidir.

## Ask koruması

Normal ve premium uzak ask çağrısı güncel sunucu onay kaydı olmadan kota ayırmaz veya köprüyü çağırmaz (428). İstemci consent_version gönderirse güncel sürümle eşleşmelidir. Yalnızca sürüm numarası göndermek onay kaydı oluşturmaz.

Profil `health_sensitivities` alanı ancak ayrı sağlık kaydı güncel sürüm/özetle eşleşiyorsa VE o istekte `health_consent: true`, `health_consent_version` güncel sürüm olarak gönderilmişse bağlama eklenir. Diğer durumlarda alan tamamen çıkarılır; normal asistan kullanılabilir. Kota transaction'ından sonra onay yeniden okunur; o sırada genel onay geri çekilmişse rezervasyon iade edilir ve 428 dönülür. Dış servise çoktan ulaşan isteği geri çağırma veya eski cevaplardan sağlık bilgisini silme garantisi yoktur.

Bu filtre yalnızca tanımlı profil sağlık alanını kapsar. Kullanıcının soru metnine veya başka serbest metin alanına sağlık bilgisi yazmasını tespit/sansür etmez. Yerel hava yanıtları bu uzak backend çağrısını kullanmaz; Android ekranındaki bilgilendirme bu ayrımı doğru anlatmalıdır.

## Açık işler

- Android gerçek oturumla GET/POST, 428/409, metin güncelleme, farklı hesaplar ve Ayarlar’dan geri çekme için uçtan uca staging doğrulaması. Yerel denetleyici/UI bağlaması hazırdır.
- Platform sözleşmesinin güncel metin/hash ve sunucu onayıyla eşlenmesi; anahtar, silme, idempotency ve sonuç kurtarma entegrasyonu.
- Rızanın hukuki dayanak yerine otomatik yeterli sayılmaması; aktarım dayanağı, yayımlanmış metin arşivi ve saklama politikalarının onayı.
- Canlı Firebase/backend/platform ve cihaz kontrolleri.

## Yerel kanıt

Backend: 95 geçti, 6 PostgreSQL testi URL olmadığı için ayrı koşuya bırakıldı. İzole PostgreSQL16: 6 geçti (0015 dahil migration, rıza upsert/audit, hesap silmede CASCADE). Rıza testleri A–B ayrımı, sahte/eskimiş kabul, metin değişimi, isteğe bağlı sağlık, geri çekme ve kota sırasında geri çekme senaryolarını kapsar. Gerçek kullanıcı verisi veya canlı servis kullanılmadı.


## Android davranışı — 30 Ekim devamı

- Asistan ve Ayarlar’da Asistan izinleri girişi var. Genel metin sunucudan alınır; sağlık kutusu ilk kabulde işaretsizdir ve isteğe bağlıdır. Sağlık metni değişmişse metadata yenilendiğinde tekrar gösterilir. Geçerli eski onayları sessizce yeni metne taşımak yoktur.
- Kabul, gösterilen sürüm ve metin özetiyle POST edilir; ardından GET ile sunucu durumu tekrar okunur. Başarısız kayıt kabul edilmiş gibi gösterilmez. Onay yetkisi DataStore’da varsayılmaz; kullanıcıya bağlı sunucu kaydı esastır, hesap değişiminde bellek durumu ve bekleyen soru temizlenir.
- Uzak soru izin kontrolünde bekletilir; onaydan sonra aynı sohbet/hesap doğrulanarak gönderilir. 428 sonrası yeniden onayda aynı mesaj UUID’si kullanılır. Vazgeçme, geri çekme, hesap veya sohbet değişimi bekleyen gönderimi iptal eder. Yerel selamlama/hava yanıtları için uzak servis onayı aranmaz.
- Onay sorgulama/kaydetme/geri çekme işlemleri süreyle sınırlıdır; zaman aşımında meşgul durumu kaldırılır. Metin içindeki HTTPS bağlantıları ayrı tıklanabilir düğmeler olarak sunulur. Uzun metin ve düğmeler tek kaydırılabilir yüzeyde; büyük yazıda sabit alt bölümün taşması önlenir.
- Sunucu zaten güncel onayı biliyorsa, soru öncesi kontrol tekrar kabul zorlamadan devam eder. Kullanıcı izin yönetimini kendisi açmışsa seçimleri inceleyip kaydedebilir. Sağlık onayı yoksa sağlık alanı ask isteğinde etkinleştirilmez.
- Yedi yeni JVM testiyle Android toplamı 196/0 hata. Backend’de health_review_required işareti için 11 rıza testi ve Ruff geçti. Canlı platform/Firebase entegrasyonu bu sonuçlarla doğrulanmış sayılmaz.
Cihaz teyidi: SM-T500 Android12 üzerinde iki saf diyalog testi geçti; koyu %200 yazı/uzun metin/checkbox ve açık tema/sağlık olmadan kabul. Son JVM toplamı 196 (7 yeni rıza testi), 0 hata/atlama. Gerçek hesapta kabul/geri çekme ve canlı servis doğrulaması yapılmadı.


Platform ask devamı: anahtar/HTTPS, konuşma/ton/bağlam ve güvenli yanıt/hata eşleme kodu hazır. Canlı anahtar/metin adaptörü ve delete-user hâlâ açık; upstream 428 eski yerel metinle yeniden kabul döngüsüne sokulmaz, 503 consent_sync_required ile durur. Son sözleşme ve yayın sınırları ASISTAN_PLATFORM_KOPRUSU.md içinde. Son yerel genel kanıt 112 backend ve 203 Android JVM; bu sayılar canlı entegrasyon kanıtı değildir.

## 30 Ekim — İstemci geri çekme niyeti ve hata UX'i
Genel uzak asistan izni geri çekilirken WithdrawalTrackingConsentGateway önce UID bazlı SharedPreferences pending bayrağını IO'da commit eder, sonra POST eder. Başarı yalnızca assistant_accepted=false ve health_accepted=false yanıtıyla kabul edilir; ardından bayrak kaldırılır. Bozuk/missing yanıt, ağ veya disk hatası başarı göstermez. Yükleme ve yeniden onay pending bayrak varken durur; yeni controller bayrağı görür ve kullanıcıya geri çekme retry'si sunar. AiAssistantRepository normal ve 401 retry gönderiminden önce bu bayrağı kontrol eder. Hesap silme ilgili UID bayrağını temizler.

Controller'da başarısız revoke önceki accepted=true durumunu geri getirmez. Dialog kısa içerikte doğal yüksekliğe, uzun içerikte ekran sınırı ve scroll'a sahiptir. Retry LOAD/REVOKE ayrılır; pending ekranını yeniden açmak otomatik POST yapmaz. Başarı açık mesajla bildirilir, onay yeniden yönetme ayrı eylemdir. 227 JVM ve 4 izole tablet dialog testi geçti; gerçek servis sürümü/auth/metin yapılandırması doğrulanmadı.

Kapsam sınırı: önceden yola çıkmış istekler geri çağrılamaz. Eşzamanlı eski onay/geri çekme yazılarının sunucu sıralaması ve yalnızca sağlık onayını kaldırma kaydı başarısız olduğunda yerel hassas veri engeli ayrıca incelenmeli. Bu güncelleme genel geri çekme niyetini kapsar; tüm dağıtık yarışları çözdüğü iddia edilmez. Gerçek izin servisi bağlantısı P1 yayın kontrolünde açık kalır.
