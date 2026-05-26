# HealthForge

Healthy-eating Android app with Spring Boot backend and React admin UI.

## Tech Stack

| Layer | Stack |
|---|---|
| **Android client** | Kotlin 2.0+, Jetpack Compose, Room (SQLCipher), Retrofit, Hilt, Material 3 |
| **Backend** | Kotlin 2.0+, Spring Boot 3.3+, PostgreSQL 16, Flyway 10+, MinIO, JWT (HS512), Bucket4j, Caffeine |
| **Admin UI** | React 18 + Vite + TypeScript + MUI 5 + TanStack Query |
| **Deploy** | docker-compose + Caddy 2.8+ + GitHub Actions (SSH to Netcup VPS) |
| **Locale** | de_DE only |

## Folder Structure

```
.
├── android_app/         # Android Kotlin/Compose client
├── server/              # Spring Boot Kotlin backend
├── admin-ui/            # React + Vite + TS admin web UI
├── deploy/              # docker-compose, Caddyfile, deploy scripts
├── tooling/             # ETL scripts, seed data, dev scripts
├── docs/                # Project documentation (LOCKED — see docs/ReqSpec.md)
└── .github/workflows/   # CI/CD pipelines
```

## Quick Start (Dev)

### Prerequisites
- Docker Desktop
- JDK 21
- Node.js 20+ (for admin-ui)
- Android Studio Hedgehog+ (with SDK 35)

### Boot Local Services
```powershell
cd deploy
docker compose -f docker-compose.dev.yml up -d
```

Verify:
- API health: http://localhost:8080/actuator/health
- MinIO console: http://localhost:9001 (admin / changeme-minio-pass)
- PostgreSQL: localhost:5434 (healthforge / changeme-pg-pass)

### Run Server (local, outside Docker)
```powershell
cd server
./gradlew bootRun
```

### Run Admin UI
```powershell
cd admin-ui
npm install
npm run dev
```
Opens at http://localhost:5173

### Build Android App
```powershell
cd android_app
./gradlew assembleDebug
```

## Documentation

All spec docs are in [`docs/`](docs/) and LOCKED prior to implementation:
- [ReqSpec.md](docs/ReqSpec.md) — 135 requirements
- [UsabilityMap.md](docs/UsabilityMap.md) — Screen-by-screen UX
- [Architecture.md](docs/Architecture.md) — Tech stack + topology
- [GUI.md](docs/GUI.md) — Visual design tokens
- [TraceabilityMatrix.md](docs/TraceabilityMatrix.md) — REQ-ID → File mapping
- [SprintPlan.md](docs/SprintPlan.md) — P1-P4 sprints

## Production Domains

- `api.healthforge.endgear.de` — REST API
- `admin.healthforge.endgear.de` — React Admin UI
- `cdn.healthforge.endgear.de` — MinIO public bucket for images

## License

Proprietary — All rights reserved.
