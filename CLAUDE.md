# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Project Overview

**Havamania** is a full-stack weather app with two main packages:
- `mobile/` — React Native 0.75 (TypeScript)
- `backend/` — FastAPI + PostgreSQL (Python 3.11)

Weather data is fetched **directly from Open-Meteo** (free, no API key) both on the mobile client and through the backend. The backend additionally handles auth, user profiles, AI chatbot proxying, subscriptions, and alerts.

---

## Commands

### Full Stack (Docker — recommended)
```bash
make dev-setup       # First-time setup: copies .env files, runs migrations, creates test user
make dev-up          # Start PostgreSQL + backend in Docker
make dev-down        # Stop all services
```

### Backend
```bash
make backend-run     # Run locally without Docker: uvicorn on port 8000
make backend-test    # Run pytest inside Docker container
make backend-lint    # Run ruff linter inside Docker container

# Migrations (run inside backend/ or via Docker exec)
alembic upgrade head
alembic revision --autogenerate -m "description"
```

### Mobile
```bash
make mobile-start            # Start Metro bundler (or: cd mobile && npm run start)
make mobile-test             # Run Jest tests
cd mobile && npm run ios     # Build and launch iOS simulator
cd mobile && npm run android # Build and launch Android emulator
npx tsc --noEmit             # TypeScript type check (run from mobile/)

# Single test file
cd mobile && npx jest src/path/to/test.test.ts
```

### Backend Tests (single file)
```bash
docker compose run --rm backend pytest tests/test_auth.py -q
docker compose run --rm backend pytest tests/test_auth.py::test_login -q
```

---

## Architecture

### Mobile Navigation Tree
```
RootNavigator
├── AuthNavigator (unauthenticated)
│   ├── Login
│   └── SignUp
└── MainStack (authenticated)
    ├── Tabs  ← MainTabs (bottom tab navigator)
    │   ├── Weather  → HomeScreen
    │   ├── AIChat   → ChatbotScreen  (floating center button)
    │   └── Profile  → ProfileScreen
    └── Forecast  ← ForecastScreen (full-screen, no tab bar)
```

`HomeScreen` navigates to `Forecast` via `navigation.navigate('Forecast', { lat, lon, city })`. The floating AI Chat button in the tab bar is a custom `tabBarButton` component.

### Mobile State Management
Two Zustand stores:
- **`authStore`** — `isAuthenticated`, `isGuest`, `initializing`, login/logout actions, `loginAsGuest()` for backend-less usage
- **`themeStore`** — `theme: 'dark' | 'light'` (default: `'dark'`), `setTheme()`, `toggleTheme()`

Server state is managed entirely by **React Query** (`@tanstack/react-query`). All API calls go through React Query hooks; do not use local `useState` for remote data.

### Mobile Auth Flow
1. On app start, `RootNavigator` calls `authStore.initSession()`
2. `initSession()` reads tokens from **React Native Keychain** (`havamania-auth` service key)
3. Registers an Axios 401 interceptor that auto-calls `/auth/refresh`
4. If refresh fails → clears session, navigates to Auth
5. `isGuest: true` bypasses all of this; guest users get local chatbot responses

### Mobile API Clients
- **`services/apiClient.ts`** — Axios instance; Android emulator uses `10.0.2.2:8000`, iOS uses `localhost:8000`. Base path: `/v1`.
- **`services/openMeteoApi.ts`** — Direct `fetch()` calls to `api.open-meteo.com` and `geocoding-api.open-meteo.com`. Used for all weather data (works without backend).
- **`services/weatherApi.ts`** — Re-exports from `openMeteoApi.ts`. All weather queries in screens import from here.

### Mobile Theme System
All screens consume colors from `src/theme/index.ts`:
```typescript
const { theme } = useThemeStore();
const C = theme === 'dark' ? DarkColors : LightColors;
// Pass C to internal style factory: const s = makeStyles(C);
```
Use constants `Spacing`, `Radius`, `FontSize` from the same module. Do **not** hardcode pixel/color values in screens.

### Backend Structure
```
backend/app/
├── main.py                     # FastAPI app, middleware stack
├── api/v1/router.py            # Aggregates all route modules
├── api/v1/routes/              # auth, weather, profile, chatbot, subscription, alerts, devices
├── core/                       # config, security (JWT+bcrypt), logging, observability (Sentry+Prometheus)
├── db/                         # SQLAlchemy engine, session, declarative base
├── models/                     # SQLAlchemy ORM models
├── schemas/                    # Pydantic request/response schemas
├── services/
│   ├── weather/                # WeatherService (caching), OpenMeteoProvider, mapper
│   ├── auth_service.py         # signup, login, refresh, logout
│   ├── subscription_service.py # free/premium subscription management
│   ├── chatbot_bridge.py       # Proxies to external chatbot at CHATBOT_BASE_URL
│   └── chatbot_usage.py        # Daily message count enforcement
└── dependencies/               # auth.py (get_current_user), premium.py (require_premium_user)
```

### Backend Auth
JWT with two token types — `"access"` (short-lived) and `"refresh"` (long-lived, stored hashed in DB). `get_current_user` dependency validates the Bearer token on protected routes. `require_premium_user` dependency (HTTP 402 if not premium) gates premium features like map layers, extra locations, and higher chatbot limits.

### Backend Weather Caching
`WeatherService` wraps `OpenMeteoProvider` with in-memory TTL cache:
- Current weather: 180 s
- Hourly: 600 s
- Daily: 1800 s

### Database
PostgreSQL 16 on port **5433** (host), forwarded from container port 5432. Alembic manages migrations; all migration files live in `backend/alembic/versions/`.

---

## Key Ports & URLs (Development)
| Service | URL |
|---------|-----|
| Backend API | `http://localhost:8000/v1` |
| Swagger UI | `http://localhost:8000/docs` |
| Metro bundler | `http://localhost:8081` |
| PostgreSQL | `localhost:5433` |
| External chatbot | `http://localhost:9000` (configurable via `CHATBOT_BASE_URL`) |

---

## Test Credentials
- **Email:** `test@example.com`
- **Password:** `password123`

Created automatically by `make dev-setup` / `setup.sh`.

---

## Known Incomplete Areas
- **In-App Purchases** (`src/services/iapService.ts`): stub returning demo data; real `react-native-iap` integration not implemented.
- **External Chatbot**: backend proxies to `CHATBOT_BASE_URL` (default `localhost:9000`); no chatbot service ships with this repo. Mobile falls back to local rule-based answers when `isGuest` is true or the backend is unavailable.
- **Push Notifications**: device token registration endpoint exists (`/v1/devices`) but no push delivery service is wired up.
