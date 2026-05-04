# Havamania Windows Setup Script

Write-Host "Havamania Onboarding Kurulumu Basliyor (Windows)..."

# 1. .env dosyalarini olustur
if (-not (Test-Path ".env")) {
    Write-Host "Kök dizinde .env dosyasi olusturuluyor..."
    Copy-Item ".env.example" ".env"
}

if (-not (Test-Path "backend/.env")) {
    Write-Host "backend/.env dosyasi olusturuluyor..."
    Copy-Item "backend/.env.example" "backend/.env"

    # DATABASE_URL'i Docker icindeki servis adıyla guncelle
    (Get-Content "backend/.env") -replace "localhost:5432", "postgres:5432" | Set-Content "backend/.env"
}

# 2. Docker Imajlarini Derle
Write-Host "Docker imajlari derleniyor..."
docker compose build

# 3. Veritabanini Baslat
Write-Host "Veritabani baslatiliyor..."
docker compose up -d postgres

# 4. Veritabaninin Hazir Olmasini Bekle
Write-Host "Veritabani baglantisi bekleniyor..."
$retries = 0
while ($retries -lt 30) {
    try {
        $ready = docker compose exec postgres pg_isready -U postgres
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Veritabani hazir!"
            break
        }
    } catch {
        # Ignore errors during wait
    }
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 2
    $retries++
}

# 5. Veritabani Goclerini (Migrations) Calistir
Write-Host "Veritabani tablolari olusturuluyor (Alembic)..."
docker compose run --rm backend alembic upgrade head

# 6. Test Kullanicisi Olustur
Write-Host "Test kullanicisi olusturuluyor..."
docker compose run --rm backend python create_test_user.py

# 7. Tum Servisleri Baslat
Write-Host "Arka plan servisleri baslatiliyor..."
docker compose up -d

Write-Host ""
Write-Host "KURULUM BASARIYLA TAMAMLANDI!"
Write-Host "----------------------------------------"
Write-Host "Backend API: http://localhost:8000"
Write-Host "Swagger UI : http://localhost:8000/docs"
Write-Host ""
Write-Host "Test Kullanici Bilgileri:"
Write-Host "Email   : test@example.com"
Write-Host "Sifre   : password123"
Write-Host "----------------------------------------"
Write-Host "Mobil uygulamayi calistirmak icin:"
Write-Host "   cd mobile ; npm run start"
Write-Host "----------------------------------------"
