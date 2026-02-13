# Havamania Proje Kurulum Rehberi 🚀

Merhaba! Bu rehber, kodlama dünyasına yeni adım atmış biri olsan bile Havamania projesini bilgisayarında sorunsuz bir şekilde çalıştırman için hazırlandı. Adımları sırasıyla takip etmen yeterli.

## 📋 Ön Hazırlıklar

Başlamadan önce bilgisayarında şu programların kurulu olduğundan emin ol:
1.  **Docker Desktop**: Veritabanı için gerekli. [Buradan indir](https://www.docker.com/products/docker-desktop/).
2.  **Python 3.10+**: Arka plan (Backend) işlemleri için.
3.  **Node.js**: Mobil uygulama paketleyicisi için.
4.  **Git**: Kodu indirmek için.

---

## 🛠 Adım Adım Kurulum

### 1. Projeyi Bilgisayarına İndir
Terminali aç ve projenin olduğu klasöre git.

### 2. Ayar Dosyalarını Hazırla (.env)
Proje klasöründe iki adet önemli ayar dosyası oluşturmalısın:
- Ana klasörde `.env` dosyası oluştur ve içini `.env.example` içeriğiyle doldur.
- `backend` klasörü içinde `.env` dosyası oluştur ve içini `backend/.env.example` içeriğiyle doldur.
  > **Önemli:** Veritabanı bağlantısı için port numarasının `5433` olduğundan emin ol (`localhost:5433`).

### 3. Veritabanını Başlat (Docker)
Terminalde şu komutu yaz:
```bash
docker compose -f docker-compose.staging.yml up -d postgres
```
Bu komut, Docker üzerinde projenin veritabanını senin için kuracaktır.

### 4. Arka Plan (Backend) Kurulumu
Backend klasörüne git ve gerekli kütüphaneleri yükle:
```bash
# Sanal ortam oluştur (Bağımlılıkların karışmaması için)
python3 -m venv .venv
# Sanal ortamı aktif et
source .venv/bin/activate
# Kütüphaneleri yükle
pip install -e "./backend"
```

### 5. Veritabanı Tablolarını Oluştur
Veritabanının hazır hale gelmesi için şu komutu çalıştır:
```bash
cd backend
alembic upgrade head
```

### 6. Mobil Uygulama Kurulumu
Mobil klasörüne git ve gerekli paketleri yükle:
```bash
cd mobile
npm install
```

---

## 🏃‍♂️ Uygulamayı Çalıştırma

Artık her şey hazır! Uygulamayı başlatmak için iki ayrı terminal penceresi kullanmalısın:

**Terminal 1 (Backend):**
```bash
make backend-run
```

**Terminal 2 (Mobil):**
```bash
make mobile-start
```

---

## ❓ Sorun Giderildi (Sıkça Karşılaşılanlar)

- **Beyaz Ekran Sorunu**: Mobil uygulama terminalinde **'i'** (iOS için) veya **'a'** (Android için) tuşuna basarak simülatörü başlatmayı unutma.
- **Port 5432 Hatası**: Eğer bilgisayarında başka bir veritabanı çalışıyorsa hata alabilirsin. Bu yüzden biz bu projede **5433** portunu kullanıyoruz.
- **Modül Bulunamadı**: `npm install` komutunu `mobile` klasöründe çalıştırdığından emin ol.

Tebrikler, artık Havamania geliştiricisisin! 🎉
