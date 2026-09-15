# Android yayın öncelikleri — 15 Eylül 2026

Kullanıcı kararı: bu aşamada yalnızca yerel testler. Canlı dağıtım veya gerçek kullanıcı hesabı silme yapılmadı.

| Sıra | İş | Son durum |
| --- | --- | --- |
| 1 | Tam ve yeniden denenebilir hesap silme | Kod, sahte servis testleri, SQL ilişki temizliği ve yerel Firebase kural testleri tamamlandı. Canlı özellik kapalı; uçtan uca test hesabı/dağıtım bekliyor. |
| 2 | Veritabanı geçişleri | İstek başına DDL kaldırıldı; Alembic 0011–0013 eklendi. Eksik Android geçişleri düzeltildi. SQLite ve gerçek tablette veri koruma doğrulandı; PostgreSQL testi bekliyor. |
| 3 | Firebase/backend entegrasyonu | Yerel kurallarda sahiplik ve silme sonrası erişim engeli doğrulandı. Canlı proje, servis hesabı, bucket ve API alan adı doğrulaması ertelendi. |
| 4 | Hava önbelleği ve saat dilimi | Koordinat ayrımı, 100 kayıt sınırı, disk hatasında ağ verisini gösterme, konuma göre seçili saat ve UTC rota sorguları düzeltildi. Tüm rota risk örnekleme ve astronomi senaryoları henüz doğrulanmadı. |
| 5 | Cihaz dayanıklılığı | Oturum değişimi, dinleyici birikimi ve konum isteği iptali düzeltildi; 8 yeni yerel senaryo eklendi. Manuel izin/rotasyon, süreç ölümü ve uzun kullanım matrisi açık. [Ayrıntı](CIHAZ_DAYANIKLILIGI.md). |
| 6 | Tasarım ve erişilebilirlik | Yeni kurtarma ekranı kaydırma/klavye alanı ve 48 dp düğmeyle hazır. Tüm ekranların büyük yazı, TalkBack, telefon/tablet ve iki tema incelemesi tamamlanmadı. |
| 7 | Üretim hizmetleri / mağaza | Ticari hava/rota sağlayıcısı, yasal sayfalar, Data Safety, hesap silme web bağlantısı ve mağaza görselleri bekliyor. |
| 8 | İmzalı release AAB | Gerçek yükleme anahtarı ve release/R8 cihaz testi bekliyor. |

## Önceki tur doğrulama (07b268f)

- Backend: **64 test geçti**, Ruff temiz. Gerçek HTTP taşıyıcıları testlerde engellendi; abonelik testi dış hava servisine bağımlı olmaktan çıkarıldı.
- Android: **58 birim testi geçti**; `assembleDebug` ve `lintDebug` başarılı. Lint: **0 hata, 200 uyarı, 1 ipucu**.
- SM-T500 / Android 12: **2 test geçti**. Bildirim 4→5 ve hava veritabanı 11→17 geçişlerinde eski kayıtlar korundu; farklı kullanıcı kayıtları temizlenmedi. Hava önbelleğinin 100 kayıt sınırı da gerçek SQLite üzerinde doğrulandı.
- Firestore/Storage emülatörleri: **4 test geçti**. Yalnızca yerel demo proje kullanıldı.
- Toplam: **128 başarılı test**. Bu sayı tüm uygulama akışlarının veya canlı ortamın doğrulandığı anlamına gelmez.

## Cihaz dayanıklılığı turu

- Android: **66 birim testi geçti** (önceki 58 + yeni 8). `assembleDebug` ve `lintDebug` başarılı.
- Son APK ile SM-T500 / Android 12 üzerinde **2 izole cihaz testi geçti**.
- Backend ve Firebase kural testleri bu tur yeniden çalıştırılmadı; bu bileşenlerde değişiklik yok.
- Detaylar ve açık cihaz matrisi: [cihaz dayanıklılığı raporu](CIHAZ_DAYANIKLILIGI.md).

## Son debug APK

Yol: `mobile/android/app/build/outputs/apk/debug/app-debug.apk`

SHA-256: `5268567EB63FDDFAB99410543E9A071C6E2382C72418AB531EFB6D26ABAD61CB`

ARM64/x86_64 altı kütüphanenin ELF LOAD hizalaması 16384 bayt. Gerçek 16 KB cihaz testi henüz yapılmadı. Bu paket Google Play yükleme paketi değildir.

Hesap silme mimarisi, kalan veri saklama kapsamı ve dağıtım sırası: [teknik rapor](HESAP_SILME_VE_SUNUCU_GECISLERI.md).
