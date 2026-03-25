# Havamania - Stajyer Onboarding Kılavuzu 🚀

Bu kılavuz, projeyi 10 dakika içinde çalışır hale getirmeniz için hazırlanmıştır.

## 📋 Ön Gereksinimler

Başlamadan önce bilgisayarınızda şunların kurulu olduğundan emin olun:
1. **Docker Desktop** (veya Docker Engine + Compose)
2. **Node.js** (v18+ önerilir) - *Mobil uygulama için*
3. **Git**

---

## 🛠️ Hızlı Kurulum (10 Dakika)

1. **Projeyi indirin ve ana dizine gidin:**
   ```bash
   git clone <PROJE_URL_ADRESI>
   cd Havamania-MobileApp
   ```

2. **Tek tuşla kurulum scriptini çalıştırın:**
   ```bash
   bash setup.sh
   # Veya Makefile kullanıyorsanız:
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

2. **Bağımlılıkları yükleyin (İlk sefer için):**
   ```bash
   npm install
   ```

3. **Uygulamayı başlatın:**
   ```bash
   npm run start
   ```

*Fiziksel cihazda çalıştırmak için:*
- Telefonunuzu USB ile bağlayın ve **USB Hata Ayıklama**'yı açın.
- `npm run android` komutunu çalıştırın.

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

tablete bağlantı için
adb pair 192.168.0.19:35531

mac için:
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1
