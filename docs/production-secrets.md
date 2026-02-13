# Production Secret Management

## Required Secrets
- `DATABASE_URL`
- `SECRET_KEY`
- `CHATBOT_BASE_URL`
- `SENTRY_DSN_BACKEND`
- `SENTRY_DSN_MOBILE`
- Store API credentials for iOS/Android receipts

## Rules
- Never commit secrets to repository.
- Store secrets in cloud secret manager.
- Rotate secrets every 90 days.
- Access by least-privilege role only.
