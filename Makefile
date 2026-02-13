.PHONY: backend-run backend-test backend-lint mobile-start mobile-test

backend-run:
	cd backend && uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

backend-test:
	cd backend && pytest -q

backend-lint:
	cd backend && ruff check app tests

mobile-start:
	cd mobile && npm run start

mobile-test:
	cd mobile && npm test
