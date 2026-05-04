.PHONY: backend-run backend-test backend-lint mobile-start mobile-test dev-setup dev-up dev-down

# Lokal Çalıştırma (Fallback)
backend-run:
	cd backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Docker İçinde Çalıştırma (Önerilen)
backend-test:
	docker compose run --rm backend pytest -q

backend-lint:
	docker compose run --rm backend ruff check app tests

mobile-start:
	cd mobile && npm run start

mobile-android:
	cd mobile && npm run android

mobile-ios:
	cd mobile && npm run ios

mobile-test:
	cd mobile && npm test

dev-setup:
ifeq ($(OS),Windows_NT)
	powershell -ExecutionPolicy Bypass -File setup.ps1
else
	bash setup.sh
endif

dev-up:
	docker compose up -d

dev-down:
	docker compose down
