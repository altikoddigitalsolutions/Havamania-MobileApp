# 19 Kasım — Profil snapshot diski ve oturum kuyruğu

## Değişiklik

ThemeManager.saveProfileSnapshot içindeki alan eşlemesi tek uygulama yolunda korundu. Public context sürümü aynı varsayılan Firebase oturumunu, AccountDeletionManager durumunu ve global PreferenceWriteEpoch korumasını kullanıyor. Internal overload ise açık DataStore, güncel oturum sağlayıcısı, engel durumu ve yazı nesli alıyor. Test için üretim koruması kaldırılmadı; eşleme kodu kopyalanmadı.

## Gerçek disk senaryosu

Ayrı demo Auth ve rastgele adlı bağımsız .preferences_pb dosyası kullanılır. Mevcut kullanıcının settings DataStore'una yazılmaz. Gerçek A profil snapshot'ı kaydedilir. DataStore'un edit işlemi bir test bariyerinde bekletilir; A'nın ikinci snapshot'ı kuyruğa alınır. Gerçek demo oturumu B'ye değiştirildikten sonra kuyruk serbest bırakılır; A'nın eski yazısı engellenmelidir.

B snapshot'ı kaydedilir; engel açıkken yeni yazı kabul edilmemelidir. İkinci gerçek DataStore kuyruk bariyerinde yazı nesli geçersiz kılınır; eski neslin yazısı da engellenmelidir. Eski DataStore scope'u tamamen kapatılıp aynı dosya yeni DataStore örneğiyle açılır; A ve B'nin isimleri, A biyografisi/hakkımda/ilgi alanları diskten doğrulanır. Fotoğraf yolu önce yazılarak gerçekten mevcut olduğu, sonra null snapshot ile kaldırıldığı doğrulanır. Sonunda yalnız test dosyası silinir.

Bu aynı süreçte yeni DataStore örneğiyle disk açma kabulüdür; Android süreç ölümü kabulü değildir. Veri alanları üretim eşlemesidir ama public varsayılan Firebase wrapper'ı demo oturumuna çevrilmez. Snapshot'ın profil ViewModel'inden tetiklenip tüm ekrana taşınması henüz bu testin kapsamı değildir.

## Sıradaki

Üretim ProfileViewModel'in snapshotWriter enjeksiyonunu bu gerçek korumalı snapshot overload'una bağla. ThemeViewModel ile aynı hesap/dosya bağlamında ekran yeniden oluşturma kabulünü birleştir; mevcut canlı oturumları ve DataStore'u yeniden yapılandırma. Tam ProfileScreen/SettingsScreen navigasyonu ve süreç ölümü açık kalır.

## Sonuç

631 JVM testi ve Samsung SM-T500 Android 12 üzerinde 15 aktif Android Firebase SDK senaryosu geçti. SDK XML: 21 toplam / 6 özel mod bilerek atlandı / 0 hata. Yeni disk/oturum kuyruğu senaryosu bu 15 içindedir. Aynı dosyayı yeni DataStore örneğiyle açma, iki UID alanlarının ayrılığı, kuyruktaki eski oturum/nesil yazısının reddi ve fotoğraf yolunun temizlenmesi geçti. Yeni üretim hatası bulunmadı. Derleme ve git diff --check başarılı. Önceki özel premium koşusu bu tur yeniden çalıştırılmadı.
