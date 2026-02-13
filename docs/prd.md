# Havamania PRD

## 1) Urun Ozeti
Havamania, anlik hava durumu, konum bazli tahmin, akilli uyarilar ve hava durumu odakli chatbot deneyimini tek uygulamada birlestiren freemium bir mobil uygulamadir.  
Frontend: React Native  
Backend: FastAPI  
Veritabani: PostgreSQL

## 2) Problem ve Firsat
- Kullanicilar birden fazla uygulama arasinda gecis yapmadan hizli, guvenilir ve anlasilir hava durumu bilgisi almak istiyor.
- Ozellikle konuma ozel ani degisikliklerde dogru zamanda uyarilma ihtiyaci var.
- Teknik olmayan ortalama kullanici icin mevcut uygulamalar fazla karmasik olabiliyor.

## 3) Hedefler
- 3 dokunus icinde kullanicinin bulundugu konum icin anlik hava durumunu gostermek.
- Konum bazli takip ve tahmin ayarlarini profil uzerinden kolayca yonetilebilir yapmak.
- Temel ozelliklerle guclu bir ucretsiz deneyim sunmak, gelismis ozellikleri abonelik ile acmak.
- "Mercury weather" benzeri modern, temiz, teknolojik bir arayuz sunmak.

## 4) Hedef Disi (MVP)
- Kendi meteorolojik modelimizi egitmek.
- Sosyal medya benzeri kullanici-uretimi icerik.
- Web panel ile tam yonetim arayuzu (ilk fazda zorunlu degil).

## 5) Hedef Kullanici Profilleri
- Sehirde yasayan gundelik kullanici: "Bugun yagmur var mi, ne giymeliyim?"
- Acik hava aktivitesi yapan kullanici: "Ruzgar, yagis, saatlik degisim nasil?"
- Risk hassasiyeti yuksek kullanici: "Kritik hava olaylarinda aninda bildirim almak istiyor."

## 6) Kapsam ve Ozellik Seti

### 6.1 Temel (Ucretsiz) Ozellikler
- Open-Meteo API uzerinden anlik hava durumu.
- Saatlik tahmin (24 saate kadar).
- Gunluk tahmin (7 gun).
- Temel harita/radar onizleme.
- Temel hava uyarilari listesi.
- Chatbot ile sinirli sayida gunluk soru-cevap.
- Profilden birincil konum secimi.
- Profilden bildirim tercihleri (ac/kapat).

### 6.2 Premium (Abonelik) Ozellikler
- Gelismis hava veri saglayici katmani (ucretli API'lere gecis).
- Coklu konum takibi.
- Gelismis harita katmanlari (ruzgar, yagis yogunlugu, sicaklik katmani gecmisi).
- Daha gelismis/erken uyarilar.
- Uzun vadeli tahmin gorunumleri.
- Chatbot icin daha yuksek kullanim limiti veya limitsiz kullanim.
- Reklamsiz deneyim (reklam eklenecekse premium'da kapali).

### 6.3 Taslak Ekranlarla Uyumluluk
Asagidaki ekranlar urun akisina dogrudan dahildir:
- `HomeScreen.tsx`: anlik durum, saatlik-gunluk tahmin, metrik kartlari.
- `MapScreen.tsx`: konum, harita katmanlari, zaman cizelgesi.
- `AlertsScreen.tsx`: kritik ve aktif uyarilar.
- `SettingsScreen.tsx`: profil, tema, birimler, bildirim tercihleri.
- `LoginScreen.tsx` / `SignUpScreen.tsx`: kimlik dogrulama.

## 7) Freemium Fiyatlandirma Modeli

| Ozellik | Ucretsiz | Premium |
|---|---|---|
| Anlik hava durumu | Evet | Evet |
| Saatlik/Gunluk tahmin | 24s/7g | 72s/14g (veya daha fazla) |
| Kayitli konum sayisi | 1 | 5+ |
| Uyari turleri | Temel | Gelismis + oncelikli |
| Harita katmanlari | Sinirli | Tam |
| Chatbot kullanim limiti | Gunluk limitli | Yuksek limit/lmitsiz |
| Reklam | Acik (opsiyonel) | Kapali |

Not: Fiyatlar ulke bazli mobil magaza politikasina gore tanimlanacaktir.

## 8) Kullanici Akislari
- Ilk acilis: onboarding -> konum izni -> kayit/giris -> ana ekran.
- Hava durumu kontrolu: ana ekran -> detay kartlari -> harita.
- Uyari takibi: ana ekran/alt menuden uyarilar -> detay goruntule.
- Profil yonetimi: ayarlar -> konum/tercihler/bildirim.
- Premium donusum: premium ozellik tetigi -> paywall -> satin alim -> ozellik acilisi.

## 9) Fonksiyonel Gereksinimler
- FR-01: Sistem GPS veya secili sehir ile anlik hava verisi cekmelidir.
- FR-02: Veriler en gec 60 saniye onceki sunucu cache'inden servis edilebilmelidir.
- FR-03: Kullanici profilinde konum ve bildirim tercihleri saklanmalidir.
- FR-04: Kullanici chatbot'a soru sorabilmeli ve yanit alabilmelidir.
- FR-05: Chatbot entegrasyonu mevcut chatbot servisi uzerinden yapilmalidir.
- FR-06: Uygulama abonelik durumuna gore ozellikleri acip kapatmalidir.
- FR-07: Uyarilar siddet seviyesine gore gruplandirilarak gosterilmelidir.
- FR-08: Kullanici birim tercihlerini (C/F, km/s vb.) degistirebilmelidir.
- FR-09: Konum bazli tahmin ac/kapat ayari profil ekranindan degistirilebilmelidir.

## 10) UX Gereksinimleri
- Birincil aksiyonlar tek elle kullanimda erisilebilir olmali.
- En sik kullanilan 3 islem (hava durumu, harita, uyarilar) alt navigasyonda net gorunmeli.
- Ortalama kullanici 30 saniye icinde "simdi hava nasil" sorusunun cevabini bulabilmeli.
- Yazilar ve veri kartlari dusuk dikkat seviyesinde bile hizli taranabilir olmali.

## 11) Basari Metrikleri
- D1 retention >= %35
- D7 retention >= %18
- Ucretsiz -> premium donusum >= %2.5
- Haftalik aktif kullanicilarin %60'i konum bazli ozelligi aktif kullaniyor olmali.
- Hava verisi API basarisizlik orani < %1
- Ortalama API cevap suresi (p95) < 400ms

## 12) Kabul Kriterleri (MVP Cikis)
- Kullanici kayit/giris yapip anasayfada anlik hava verisini gorebiliyor.
- Profilde konum ve bildirim tercihleri kaydedilip tekrar yukleniyor.
- Uyarilar kritik/aktif sekmesinde ayrisiyor.
- Chatbot sorulara yanit uretebiliyor.
- Premium paywall tetigi calisiyor ve abonelik durumuna gore ozellikler aciliyor.
- En az bir market (iOS veya Android) test dagitimi alinmis.

## 13) Riskler ve Onlemler
- API saglayici bagimliligi: veri saglayici adapter katmani ile degistirilebilirlik.
- Abonelik dogrulama hatalari: server-side receipt validation + webhook senkronizasyonu.
- Konum izin reddi: manuel sehir secimi fallback.
- Chatbot gecikmesi: timeout, retry ve "yanit gecikiyor" UX fallback'i.

## 14) Fazlama
- Faz 1 (MVP): Temel hava, profil, uyarilar, chatbot entegrasyonu, abonelik temel akisi.
- Faz 2: Gelismis harita katmanlari, coklu konum, premium deger paketinin guclendirilmesi.
- Faz 3: Ucretli gelismis API gecisi ve model tabanli tahmin iyilestirmeleri.
