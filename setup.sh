#!/bin/bash

# Hata durumunda scripti durdur
set -e

echo "🚀 Havamania Onboarding Kurulumu Başlıyor..."

# 1. .env dosyalarını oluştur
if [ ! -f .env ]; then
    echo "📄 Kök dizinde .env dosyası oluşturuluyor..."
    cp .env.example .env
fi

if [ ! -f backend/.env ]; then
    echo "📄 backend/.env dosyası oluşturuluyor..."
    cp backend/.env.example backend/.env
    # Docker içindeki postgres servisine bağlanması için DATABASE_URL'i güncelle
    sed -i '' 's/localhost:5432/postgres:5432/g' backend/.env || sed -i 's/localhost:5432/postgres:5432/g' backend/.env
fi

# 2. Docker İmajlarını Derle
echo "🐳 Docker imajları derleniyor..."
docker compose build

# 3. Veritabanını Başlat
echo "🐘 Veritabanı başlatılıyor..."
docker compose up -d postgres

# 4. Veritabanının Hazır Olmasını Bekle
echo "⏳ Veritabanı bağlantısı bekleniyor..."
until docker compose exec postgres pg_isready -U postgres > /dev/null 2>&1; do
  sleep 2
  echo -n "."
done
echo " ✅ Veritabanı hazır!"

# 5. Veritabanı Göçlerini (Migrations) Çalıştır
echo "🔧 Veritabanı tabloları oluşturuluyor (Alembic)..."
docker compose run --rm backend alembic upgrade head

# 6. Test Kullanıcısı Oluştur
echo "👤 Test kullanıcısı oluşturuluyor..."
docker compose run --rm backend python create_test_user.py

# 7. Tüm Servisleri Başlat
echo "🌐 Arka plan servisleri başlatılıyor..."
docker compose up -d

echo ""
echo "🎉 KURULUM BAŞARIYLA TAMAMLANDI!"
echo "----------------------------------------"
echo "Backend API: http://localhost:8000"
echo "Swagger UI : http://localhost:8000/docs"
echo ""
echo "🔑 Test Kullanıcı Bilgileri:"
echo "Email   : test@example.com"
echo "Şifre   : password123"
echo "----------------------------------------"
echo "👉 Mobil uygulamayı çalıştırmak için:"
echo "   cd mobile && npm run start"
echo "----------------------------------------"
