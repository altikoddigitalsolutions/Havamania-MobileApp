# Havamania - Stajyer Onboarding Kılavuzu 🚀

Bu kılavuz, projeyi 10 dakika içinde çalışır hale getirmeniz için hazırlanmıştır.

## 📋 Ön Gereksinimler

Başlamadan önce bilgisayarınızda şunların kurulu olduğundan emin olun:
1. **Docker Desktop** (veya Docker Engine + Compose)
2. **Node.js** (v18+ önerilir) - *Mobil uygulama için*
3. **Git**
4. **Android SDK & Emulator** (Android Studio üzerinden)

---

## 🛠️ Hızlı Kurulum (10 Dakika)

1. **Projeyi indirin ve ana dizine gidin:**
   ```bash
   git clone <PROJE_URL_ADRESI>
   cd Havamania-MobileApp
   ```

2. **Tek tuşla kurulum scriptini çalıştırın:**
   ```bash
   # macOS / Linux / Git Bash için:
   bash setup.sh

   # Windows PowerShell için:
   .\setup.ps1

   # Veya Makefile kullanıyorsanız (Otomatik algılar):
   make dev-setup
   ```

   *Bu script şunları yapar:*
   - `.env` dosyalarını oluşturur.
   - Veritabanını (Postgres) ayağa kaldırır.
   - Tabloları oluşturur (`alembic upgrade`).
   - Test kullanıcısı oluşturur (`create_test_user.py`).
   - Backend servisini başlatır.

---

## 🌐 Servisler ve Erişim

### 1. Backend API (FastAPI)
- **Adres:** `http://localhost:8000`
- **Dokümantasyon (Swagger):** `http://localhost:8000/docs`

### 🔑 Test Kullanıcı Bilgileri
- **Email:** `test@example.com`
- **Şifre:** `password123`

---

## 📱 Mobil Uygulama (React Native)

Backend çalıştıktan sonra mobil uygulamayı başlatmak için:

1. **`mobile` dizinine gidin:**
   ```bash
   cd mobile
   ```

2. **Bağımlılıkları yükleyin:**
   ```bash
   npm install
   ```

3. **Uygulamayı derleyin ve çalıştırın:**
   Yeni bir native kütüphane eklendiğinde veya ilk kurulumda sadece `start` değil, full build almanız gerekir:
   ```bash
   npx react-native run-android
   ```

---

## 🔍 Cihaz Bağlantısı ve Emülatör

### Tablet/Fiziksel Cihaz (Kablosuz)
Android 11+ cihazlarda kablosuz hata ayıklama için:
```bash
adb pair <IP_ADRESI>:<PORT>
# Örnek: adb pair 192.168.0.19:35531
adb connect <IP_ADRESI>:<PORT>
```

### Emülatör Başlatma (Terminalden)
**macOS için:**
```bash
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36
```
**Windows için:**
```bash
%ANDROID_HOME%\emulator\emulator -avd Medium_Phone_API_36
```

---

## 🛠️ Faydalı Komutlar (Makefile)

Ana dizindeyken aşağıdaki komutları kullanabilirsiniz:

| Komut | Açıklama |
| :--- | :--- |
| `make dev-setup` | İlk kurulum ve veritabanı hazırlığı (setup.sh) |
| `make dev-up` | Docker servislerini arka planda başlatır |
| `make dev-down` | Docker servislerini durdurur |
| `make backend-test` | Backend testlerini çalıştırır |
| `make mobile-start` | Mobil Metro Bundler'ı başlatır |
| `make mobile-android` | Uygulamayı Android emülatörde derler ve açar |
| `make mobile-ios` | Uygulamayı iOS simülatörde derler ve açar |

---

## ❓ Sık Karşılaşılan Sorunlar

**Hata:** `Invariant Violation: TurboModuleRegistry.getEnforcing(...): 'RNCImageCropPicker' could not be found.`
- **Çözüm:** Bu hata native bir modülün binary'de olmadığını gösterir. Metro Bundler'ı kapatın ve `npx react-native run-android` komutu ile uygulamayı tekrar derleyin.

---

## 💡 Önemli İş Kuralları (Business Rules)

### 🗺️ Seyahat Önerisi Kuralı
Kullanıcının mevcut aktif şehri (selected/default city) ile hava durumu sorgulanan şehir aynıysa, seyahat planlama ile ilgili hiçbir öneri sunulmamalıdır.

**Örnek:**
- **Aktif şehir:** Balıkesir
- **Kullanıcı sorusu:** "Balıkesir hava durumu ver"

**Bu durumda KAÇINILMASI gereken ifadeler:**
- Seyahat planı oluşturabilirim
- Balıkesir'e gitmeyi planlıyorsan
- Gezi önerileri sunabilirim
- Valiz hazırlamana yardımcı olabilirim
- Rota oluşturabilirim
- Ulaşım önerileri verebilirim

**Bunun yerine yalnızca mevcut şehirdeki günlük yaşamı ilgilendiren öneriler sunulmalıdır:**
- Giyim önerisi
- UV ve güneş korunma önerisi
- Yağış uyarısı
- Rüzgar etkisi ve dış mekan önerileri
- Spor, yürüyüş, koşu gibi aktivite önerileri
- Günlük planlama tavsiyeleri

*Seyahat planlama önerileri yalnızca kullanıcı aktif şehrinden farklı bir şehir hakkında soru sorduğunda veya aktif bir seyahat kaydı bulunduğunda gösterilebilir.*

