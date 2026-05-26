# HealthForge — Architecture

**Version:** 0.2 (LOCKED — alle Architektur-Entscheidungen final für v1.0)
**Datum:** 2025-05-25
**Scope:** v1.0 Unified Release (Phase P1–P4)
**Vorgängerdokumente:** [ReqSpec.md](ReqSpec.md) v0.2, [UsabilityMap.md](UsabilityMap.md) v0.1
**Changelog:**
- v0.2 (2025-05-25): Alle 12 Open Questions aus v0.1 §9 gelockt → siehe §9 Locked Decisions.
- v0.1 (2025-05-25): Initial Draft.

> Dieses Dokument legt die technische Architektur fest. Alle Entscheidungen in §9 sind
> LOCKED. Änderungen erfordern explizite Zustimmung.

---

## 1. High-Level Topology

```
┌─────────────────────────────┐         ┌──────────────────────────────────┐
│   Android Client (Kotlin)   │         │       Admin Web UI (React)       │
│   - Compose + Hilt          │         │   - Vite + TypeScript + MUI      │
│   - Room (SQLCipher)        │         │   - Served as /admin via Caddy   │
│   - DataStore               │         └──────────────┬───────────────────┘
│   - Retrofit/OkHttp/Moshi   │                        │
│                              │                        │ HTTPS
└──────────────┬──────────────┘                        │
               │ HTTPS (TLS 1.3)                       │
               │ JWT Bearer                            │
               ▼                                       ▼
        ┌────────────────────────────────────────────────────┐
        │      Caddy 2 (Reverse Proxy + auto-TLS)            │
        │      Domains: api.healthforge.de, admin.healthforge.de │
        └──────────────────────┬─────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    ┌───────────────┐  ┌───────────────┐  ┌──────────────┐
    │ Spring Boot 3 │  │   MinIO       │  │ PostgreSQL16 │
    │ (Kotlin/JVM)  │  │ (S3-API)      │  │              │
    │ Port 8080     │  │ Port 9000     │  │ Port 5432    │
    └───────┬───────┘  └───────────────┘  └──────────────┘
            │ Cron jobs
            ▼
    ┌───────────────┐
    │  OFF ETL      │  (Open Food Facts dump → staging → publish)
    │  worker       │
    └───────────────┘
```

Alle Komponenten laufen als Docker-Container auf einem **Netcup VPS** (Single-Host
Deployment für v1.0). Orchestrierung via **docker-compose**.

---

## 2. Tech Stack Reference

| Layer | Technologie | Version | Begründung |
|---|---|---|---|
| Android | Kotlin | 2.0+ | Standard |
| Android UI | Jetpack Compose | BOM 2025.x | Modern, deklarativ |
| Android DI | Hilt | 2.51+ | Standard für Compose |
| Android DB | Room + SQLCipher | 2.6+ / 4.6+ | Verschlüsselte lokale DB |
| Android Net | Retrofit 2 + OkHttp 4 + Moshi | latest | JSON via Moshi (Kotlin-friendly) |
| Android Auth-Storage | EncryptedSharedPreferences + Keystore | androidx.security | Token-Speicher |
| Server | Spring Boot | 3.3+ | Kotlin-first stable |
| Server Lang | Kotlin | 2.0+ | Konsistenz mit Client |
| Server DB | PostgreSQL | 16 | Stabil + JSONB für flexible Felder |
| Server Migrations | Flyway | 10+ | Standard, versioned SQL |
| Server Obj-Storage | MinIO | latest stable | S3-API, self-hosted |
| Server API-Doc | springdoc-openapi | 2.5+ | OpenAPI 3.1 |
| Server Auth | spring-security-jwt (custom) | — | siehe §6.3 |
| Server Rate-Limit | Bucket4j + Caffeine | 8+ | In-Memory ausreichend für v1.0 |
| Server Metrics | Micrometer + Prometheus | — | scrape via Caddy-protected endpoint |
| Server Logs | Logback + Logstash-encoder | — | JSON structured logs |
| Push | ~~Firebase Cloud Messaging~~ ENTFERNT | — | In-App-Badge + optional Email-Digest |
| Admin UI | React 18 + Vite + TypeScript + MUI | latest | Schnelles Setup, robuste Components |
| Reverse Proxy | Caddy | 2.8+ | auto-TLS via Let's Encrypt |
| CI/CD | GitHub Actions | — | SSH-Deploy + Container-Build |

---

## 3. Client Architecture (Android)

### 3.1 Layered Structure (Clean Architecture light)

```
app/
├── presentation/        ← Compose Screens + ViewModels (Hilt)
│   ├── home/
│   ├── plan/
│   ├── essen/
│   │   ├── lebensmittel/
│   │   ├── rezepte/
│   │   └── supplements/
│   ├── log/
│   ├── profil/
│   ├── onboarding/
│   └── common/         ← reusable Composables
├── domain/             ← UseCases (suspend functions), pure Kotlin
│   ├── model/          ← domain entities (no Android deps)
│   ├── usecase/
│   └── repository/     ← Repository interfaces
├── data/               ← Repository implementations
│   ├── local/          ← Room DAOs, entities, SQLCipher
│   ├── remote/         ← Retrofit services, DTOs, mappers
│   ├── prefs/          ← DataStore + EncryptedSharedPreferences
│   └── sync/           ← WorkManager jobs (recipe-cache, group-sync)
├── di/                 ← Hilt modules
└── util/
```

**Datenfluss:** `Compose UI → ViewModel (StateFlow) → UseCase → Repository → DataSource (Room | Retrofit)`.

### 3.2 Room Schema (Client-side, verschlüsselt)

| Tabelle | Zweck | Phase |
|---|---|---|
| `user_profile` | Lokales Profil (Stammdaten, Ziele, Sport) | P1 |
| `allergy` | User-Allergien (M:N implicit) | P1 |
| `intolerance` | User-Intoleranzen + Schweregrad | P1 |
| `condition` | Conditions (z.B. Reflux, IBS) | P1 |
| `ingredient_cache` | Read-only Cache vom Server | P1 |
| `ingredient_fts` | FTS4 Virtual Table für Such-Performance | P1 |
| `ingredient_rating` | Lokales Rating (MORE_OFTEN / INTOLERANT) | P1 |
| `recipe_cache` | Server-Rezepte gecacht | P2 |
| `recipe_local` | User-eigene Rezepte (nicht synced) | P2 |
| `recipe_ingredient` | Rezept ↔ Zutat M:N (Menge, Einheit) | P2 |
| `recipe_step` | Schritt-Liste pro Rezept | P2 |
| `recipe_rating_local` | Lokales Rating (MORE_OFTEN / INTOLERANT) | P2 |
| `supplement` | Lokal angelegte Supplements | P1 |
| `supplement_intake` | Zeitstempel pro Einnahme | P1 |
| `supplement_reminder` | Zeitpläne (AlarmManager-mirror) | P1 |
| `intake_entry` | Verzehrte Lebensmittel/Rezepte (Tag/Zeit/Menge) | P1 |
| `water_intake` | Wasser-Logs pro Tag | P1 |
| `meal_plan_day` | Tagesplan-Header | P2 |
| `meal_plan_slot` | Slot pro Tagesplan (Mahlzeit-Typ, Zeit, Item-Refs) | P2 |
| `shopping_list_item` | Einkaufsliste (aggregiert aus Plan) | P3 |
| `log_entry` | Symptom-Tagebuch-Eintrag pro Tag | P3 |
| `log_symptom` | Symptom-Pos. mit Severity 1–5 | P3 |
| `log_tag` | User-Tags (z.B. "Stress", "Periode") | P3 |
| `custom_symptom` | Benutzerdefinierte Symptomliste | P3 |
| `group_cache` | Gemeinsame Gruppen (Read-Cache) | P3 |
| `pending_op` | Outbox-Pattern: Aktionen offline → später syncen | P1 |

**Migrations:** Auto-migration wo möglich, sonst manuell. Room-Version bumpen pro Release.

**FTS:** `ingredient_fts` content-rowid linked auf `ingredient_cache.id`, Token-Splitting
für Deutsch (unicode61 + porter stemming optional).

**SQLCipher Key:** Random 32-byte key generiert beim ersten Start, gespeichert in
**EncryptedSharedPreferences** (Android Keystore-backed). Key wird nicht migriert /
exportiert.

### 3.3 Networking

- **Base URL:** `https://api.healthforge.de/v1/` (Build-Config-Variable: `BASE_URL`)
- **Auth:** OkHttp Authenticator + Interceptor injecten `Authorization: Bearer <access>`
  Header. Bei 401 → Refresh-Flow (siehe §6.3).
- **Timeouts:** Connect 10s, Read 30s, Write 30s.
- **Retry:** Idempotente GETs → 3 Retries mit exponentiellem Backoff (1s, 2s, 4s).
  POST/PUT/DELETE → kein automatischer Retry (User-initiierte Retry-Button).
- **Cache:** OkHttp HTTP-Cache (50 MB) für GET-Responses mit `Cache-Control: max-age=...`.
- **Offline:** Repository checkt zuerst Room → falls leer/stale, lädt von Server →
  schreibt Cache. Bei Netz-Fehler → liefert Cache mit "stale"-Flag.

### 3.4 Sync Strategie

- **Read-Cache** (Ingredients, Rezepte, Gruppen): `If-Modified-Since` + ETag.
  Background-Refresh via WorkManager (Constraint: WiFi + Charging optional).
- **Outbox-Pattern** für Writes: Aktionen die offline gemacht werden (z.B. Recipe Like)
  landen in `pending_op` → WorkManager sendet sie sobald online.
- **Konflikte:** Server-wins für Lebensmittel/Supplements. Lokale Rezepte: User entscheidet
  beim Konflikt (P2-Feature, P1: noch keine Konflikte möglich).

### 3.5 Push-Notifications

- ~~**FCM Token-Registration:** Beim Login → POST `/devices/register`.~~ ENTFERNT (2026-05-25).
- **Topic-Subscriptions:** `group_<groupId>` pro Mitgliedschaft.
- **Lokale Reminders:** **AlarmManager** (exact-alarm Permission auf Android 14+ über
  `USE_EXACT_ALARM` oder Inexact als Fallback). Notification-Channel pro Reminder-Typ
  (Wasser / Mahlzeit / Supplement).

---

## 4. Server Architecture

### 4.1 Module Layout (Single Spring Boot App)

```
server/
├── src/main/kotlin/de/healthforge/
│   ├── HealthForgeApplication.kt
│   ├── auth/           ← JWT, Login, Refresh, Register, Invite-Validation
│   ├── user/           ← Profile, Devices, Preferences (server-side mirror)
│   ├── ingredient/     ← CRUD, Search, ETL-trigger, Field-PR-Approve
│   ├── recipe/         ← CRUD, Browse, Like, Report, Comment-FREE-TEXT-OUT
│   ├── supplement/     ← Suggestions queue + global catalog
│   ├── group/          ← Privat/Öffentlich, Members, Invites, Feed
│   ├── community/      ← Community Ratings (RECOMMEND/NOT_RECOMMEND)
│   ├── admin/          ← Endpoints für Admin-Web-UI (Mod + Approval)
│   ├── etl/            ← OFF Importer, Scheduled Jobs (@Scheduled / Quartz)
│   ├── media/          ← MinIO Presigned URLs + image-resize pipeline
│   ├── export/         ← PDF/JSON DSGVO Export (P3)
│   │── ~~notification/~~   ← ENTFERNT (FCM gestrichen)
│   ├── ratelimit/      ← Bucket4j Filter
│   ├── config/         ← Beans, Properties
│   └── common/         ← Errors, Validators, Audit
└── src/main/resources/
    ├── application.yml
    ├── db/migration/   ← Flyway V1__init.sql, V2__..., etc.
    └── openapi/        ← (auto-generated)
```

**API-Versionierung:** Path-prefix `/v1/`. Bei Breaking Change → `/v2/` parallel.

### 4.2 PostgreSQL Schema (Highlevel)

> Konkrete DDL wird in Flyway-Migration `V1__init.sql` ausformuliert. Hier nur
> Tabellen-Übersicht + wichtigste Indizes.

**Auth & User:**

- `users` (id UUID PK, email UNIQUE, password_hash, display_name, status, role, created_at, last_login_at)
- `refresh_tokens` (id UUID PK, user_id FK, token_hash, expires_at, revoked_at, device_id)
- `invites` (id UUID PK, code UNIQUE, created_by FK, used_by FK NULL, expires_at, used_at)
- ~~`devices` (id UUID PK, user_id FK, fcm_token, platform, last_seen_at)~~ ENTFERNT

**Ingredients (Master-Catalog):**

- `ingredients` (id UUID PK, off_id NULL UNIQUE, source ENUM, name_de, brand NULL,
  per_100g JSONB, allergens TEXT[], status ENUM[DRAFT/PUBLISHED/ARCHIVED], created_by FK NULL,
  last_etl_at, last_admin_edit_at, version INT)
  - Index: `gin(to_tsvector('german', name_de || ' ' || brand))` für FTS
  - Index: `(status, name_de)`
  - Sticky Admin-Edit: bei ETL-Update werden Felder, die `last_admin_edit_at > last_etl_at`
    haben, **nicht** überschrieben.
- `ingredient_field_pr` (id UUID PK, ingredient_id FK, field, old_value, new_value,
  proposed_by FK, status, reviewed_by FK NULL, reviewed_at)
- `ingredient_user_suggestions` (id UUID PK, name_de, per_100g JSONB, proposed_by FK, status)

**Recipes:**

- `recipes` (id UUID PK, author_id FK, title, description, image_key NULL, servings,
  prep_minutes, cook_minutes, status, visibility ENUM[PRIVATE/PUBLIC],
  is_official BOOL, created_at, updated_at)
- `recipe_ingredients` (recipe_id FK, ingredient_id FK, quantity, unit, position)
- `recipe_steps` (recipe_id FK, position, text)
- `recipe_likes` (recipe_id FK, user_id FK, PRIMARY KEY composite)
- `recipe_reports` (id UUID PK, recipe_id FK, reporter_id FK, reason, status)
- `recipe_ratings_community` (recipe_id FK, user_id FK, value ENUM[RECOMMEND/NOT_RECOMMEND],
  PRIMARY KEY composite)

**Supplements:**

- `supplements_catalog` (id UUID PK, name_de, brand, form, default_dose, nutrients JSONB,
  status, created_by FK NULL)
- `supplement_suggestions` (id UUID PK, name_de, brand, proposer_id FK, status, payload JSONB)

**Community Ratings (Lebensmittel):**

- `ingredient_ratings_community` (ingredient_id FK, user_id FK, value, PRIMARY KEY composite)

**Groups:**

- `groups` (id UUID PK, name, description, type ENUM[PRIVATE/PUBLIC], invite_code UNIQUE NULL,
  owner_id FK, created_at)
- `group_members` (group_id FK, user_id FK, role ENUM[OWNER/ADMIN/MEMBER], joined_at,
  PRIMARY KEY composite)
- `group_posts` (id UUID PK, group_id FK, author_id FK, body, attachment_key NULL, created_at)

**Admin / Audit:**

- `audit_log` (id BIGSERIAL PK, actor_id FK NULL, action, target_type, target_id, payload JSONB, created_at)
- `etl_runs` (id BIGSERIAL PK, kind, started_at, finished_at, status, stats JSONB, error TEXT NULL)

**Reports (P3):**

- Common `reports` table unified for recipes / posts / users (already partially above as
  `recipe_reports`, kann ggf. konsolidiert werden — DEFAULT: separate Tabellen pro Domain).

### 4.3 Flyway Migrations

- Pro Phase eigene Migration-Files: `V1__p1_init.sql`, `V2__p2_recipes.sql`,
  `V3__p3_community.sql`, `V4__p4_power.sql`.
- Nur **forward-only**, never editieren.
- Repeatable: `R__seed_official_supplements.sql` für statische Refdaten.

### 4.4 MinIO Bucket-Struktur

| Bucket | Public? | Lifecycle | Inhalt |
|---|---|---|---|
| `recipes` | public-read (CDN-friendly) | — | Rezept-Bilder (resized) |
| `ingredients` | public-read | — | Optional Ingredient-Bilder (mostly OFF-CDN) |
| `supplements` | public-read | — | Supplement-Bilder |
| `avatars` | public-read | — | User-Avatare (klein, ≤256px) |
| `exports` | private | TTL 7 Tage | DSGVO-Export-Files (presigned URL) |
| `backups` | private | TTL 30 Tage | DB-Dumps via Cron |

**DEFAULT** für Bilder: Client uploaded auf Spring-Endpoint `POST /media/upload` →
Server resized auf 3 Größen (thumb 256px, medium 800px, full 1600px) via
**ImageIO/Thumbnailator** → PUT zu MinIO → response gibt Key zurück. Client speichert
nur Key. URL-Konstruktion: `https://cdn.healthforge.de/<bucket>/<key>` (Caddy
serviert `cdn.` → MinIO public-read direkt).

### 4.5 OFF ETL Pipeline (LOCKED Q2: Initial Full + täglich inkrementell)

**Strategie:** Einmaliger Full-Import beim ersten Deploy (~3 GB Dump), danach täglich
nur inkrementelle Deltas via OFF REST API.

- **Source:** [OpenFoodFacts](https://world.openfoodfacts.org/)
- **Initial Bootstrap (einmalig):**
  1. Admin-Trigger `POST /admin/etl/off/bootstrap` (oder manueller `import-off` CLI-Befehl).
  2. Streamender Download des JSONL-Dumps (`~3 GB`, gz).
  3. Filter `countries_tags` enthält `germany`.
  4. Stage → Merge → Index (~10–30 min).
  5. Log in `etl_runs` (kind=`bootstrap`).
- **Daily Incremental (recurring):**
  - **Trigger:** `@Scheduled(cron = "0 0 3 * * *")` — täglich 03:00 UTC.
  - **Phasen:**
    1. Letzte Run-Timestamp aus `etl_runs` lesen.
    2. OFF API `GET /api/v2/search?last_modified_t__gt=<ts>&countries_tags=germany&page_size=100`
       — paginieren bis kein Ergebnis mehr.
    3. Pro Produkt: Stage → Merge (sticky-fields beachten).
    4. Log in `etl_runs` (kind=`incremental`, stats).
  - **Erwarteter Traffic:** wenige MB statt GB pro Tag.
- **Sticky-Admin-Fields:** Bei Update werden Felder mit `last_admin_edit_at > last_etl_at`
  **nicht** überschrieben.
- **Idempotenz:** ETL kann beliebig oft laufen, neuere Daten gewinnen außer bei sticky-fields.
- **Manual Trigger:** Admin-Endpoint `POST /admin/etl/off/run` (Admin-only, rate-limited).

### 4.6 Image Pipeline

- Client komprimiert vor Upload (max 2048px Kantenlänge, JPEG Q85) → reduces traffic.
- Server validiert (max 8 MB, MIME-Whitelist: `image/jpeg`, `image/png`, `image/webp`).
- Server resized (Thumbnailator) → 3 Varianten → MinIO.
- Original wird verworfen (Privacy-Bonus: EXIF stripped).

---

## 5. Admin Web UI

- **Stack:** React 18 + Vite + TypeScript + MUI + React-Router 6 + TanStack Query +
  Axios.
- **Auth:** Eigener Login (gleiche `users`-Tabelle, role=ADMIN). JWT in httpOnly Cookie
  (CSRF via SameSite=Lax + CSRF-Token Header).
- **Build & Serve:** Static Build → Caddy serviert von `admin.healthforge.de`.
  Reverse-Proxy auf `/api/*` zum Backend.
- **Seiten:** siehe [UsabilityMap §9](UsabilityMap.md) — 11 Seiten.

---

## 6. Cross-Cutting Concerns

### 6.1 Security

- TLS 1.3 enforced via Caddy.
- HSTS, X-Content-Type-Options, X-Frame-Options, CSP Header via Caddy.
- Passwords: **bcrypt** cost factor 12.
- JWT-Signing: **HS512** (symmetrisch) mit Secret aus env (LOCKED Q6). Rotation manuell
  bei Bedarf — kein Upgrade auf RS256 für v1.0 vorgesehen.
- Input-Validation: Bean-Validation (`jakarta.validation`) + custom Validators.
- SQL-Injection: nur JPA-Repos + Parameterized Queries, **kein** String-Concat.
- File-Uploads: MIME-Whitelist + Magic-Bytes-Check + Größe.
- Rate-Limiting (Bucket4j + **Caffeine in-process**, LOCKED Q12):
  - Anonyme Endpoints (Login, Register): 5/min/IP
  - Auth-Endpoints global: 60/min/User
  - Admin: 120/min/User
  - Search: 30/min/User

  Counter im RAM des Spring-Boot-Prozesses. Bei Restart: Counter zurückgesetzt (akzeptabel).
  Single-VPS-Setup → kein Redis nötig.

### 6.2 Audit

Jede mutierende Admin-Aktion → `audit_log`-Eintrag (Actor, Action, Target, Payload-Diff).

**Retention:** Rolling **90 Tage** (LOCKED Q11). Cron-Job löscht Einträge älter als 90 Tage
täglich um 04:00 UTC. Bei kritischen Vorfällen können Admins betroffene Einträge vorher
in eine separate `audit_archive`-Tabelle kopieren (P3+).

### 6.3 JWT Flow

```
1. POST /auth/login {email, password}                  → {access (15min), refresh (30d)}
2. Authorization: Bearer <access>                      → 200 OK
3. Bei 401 (expired):
   POST /auth/refresh {refresh}                        → {access (neu), refresh (neu)}
   (Rotation: alter refresh wird in DB als revoked markiert)
4. POST /auth/logout {refresh}                         → DB-Revocation
```

- Access: stateless JWT.
- Refresh: opaque Token, gehashed in DB (`refresh_tokens`). Bei Verdacht (z.B. genutzter
  revokierter Token) → **Token-Family** des Users wird komplett revoked (Re-Login forced).
- Device-Binding optional: Refresh-Token an `device_id` gebunden (P2+).

### 6.4 Observability

- **Metrics:** Micrometer → Prometheus-Endpoint `/actuator/prometheus` (Caddy
  Basic-Auth protected).
- **Logs:** Logback JSON → STDOUT → Docker collects → optional Loki später.
- **Tracing:** Out-of-scope für v1.0 (DEFAULT).

### 6.5 Internationalisierung

- v1.0: nur **de_DE**. Server-side: alle User-facing-Strings hartcodiert deutsch.
  Client: `strings.xml` deutsch.
- Architektur unterstützt aber `Accept-Language` Header — zukunftssicher.

---

## 7. Deployment

### 7.1 docker-compose Topologie (Production)

```yaml
services:
  caddy:        # Reverse Proxy + auto-TLS, ports 80/443
  api:          # Spring Boot, internal only
  postgres:     # 16-alpine, volume "pgdata"
  minio:        # latest, volume "minio-data", ports closed (proxied by Caddy)
  admin-ui:     # Static built React app, served by nginx-alpine or caddy
  backup:       # alpine + cron, scheduled pg_dump → MinIO bucket "backups"
```

Networks: `internal` (alle), `web` (nur caddy expose 80/443).

### 7.2 CI/CD (GitHub Actions)

- **`server.yml`:** push to `main` → run tests → build Docker image → push to GHCR →
  SSH zum VPS → `docker compose pull api && docker compose up -d api` → smoke-test.
- **`admin-ui.yml`:** push → Vite build → rsync `dist/` zum VPS → Caddy serviert
  automatisch (keine Restart nötig).
- **`android.yml`:** push tag `v*` → Gradle assembleRelease → signing → Artefact (APK)
  → Release-Assets. (Play-Store-Upload P4-optional via fastlane.)
- **Secrets:** GitHub Secrets für `SSH_KEY`, `VPS_HOST`, `GHCR_TOKEN`, `KEYSTORE_PASS`.

### 7.3 Backups (LOCKED Q8: Lokal im selben VPS)

- **PostgreSQL:** `pg_dump` täglich 02:00 → komprimiert (zstd) → MinIO Bucket `backups/`
  Retention 30 Tage.
- **MinIO:** Buckets-Replikation manuell bei Bedarf (P4-Optional: zweiter MinIO-Node oder off-site Sync).
- **Restore:** Dokumentiert in `docs/Runbook.md` (TODO).
- **Risiko-Akzeptanz:** Bei Total-Loss des VPS sind Backups mit verloren. Für v1.0
  akzeptiert (User-Decision). Off-site-Sync nach Hetzner Storage Box ist mögliches
  Upgrade in P4.

### 7.4 Environments

- **Production:** `api.healthforge.endgear.de`, `admin.healthforge.endgear.de`, `cdn.healthforge.endgear.de`
- **Staging:** **KEINE** Staging-Umgebung in v1.0 (LOCKED Q9). Workflow: Local-Dev → direkt Prod.
  Risiko-Mitigation: vor Deploy lokal smoke-testen, kleine Releases, manuelle Verification.
- **Local Dev:** docker-compose.dev.yml mit `localhost:8080`, kein TLS.

---

## 8. Phase Rollout

| Phase | Server-Module aktiv | Client-Tabs aktiv | Sonstiges |
|---|---|---|---|
| **P1** Foundation | auth, user, ingredient (read), supplement (lokal+suggestions), media | Home, Essen/Lebensmittel, Essen/Supplements, Profil (Plan/Log = Placeholder) | OFF-ETL initial run, Admin-UI Dashboard+Ingredient-Queue |
| **P2** Recipes | + recipe, community-ratings | + Essen/Rezepte, + Plan (manuell) | Image-Pipeline live |
| **P3** Community | + group, export | + Log (Tagebuch), + Profil/Gruppen | PDF/JSON Export, Reports (FCM entfernt) |
| **P4** Power | + etl-pr, + ingredient-field-pr, + auto-planner (server-or-client), + insights (client) | + Auto-Plan-Generator, + Insights | Full Admin-UI, Bayesian-Lokal-Modul (Barcode entfernt) |

---

## 9. Locked Decisions (alle 12 Open Questions geklärt)

Alle Entscheidungen final für v1.0. Änderungen erfordern Doc-Versionsbump.

| # | Frage | Entscheidung | Begründung |
|---|---|---|---|
| Q1 | Admin-UI Stack | **React 18 + Vite + TypeScript + MUI** | Schnelles Setup, fertige Components |
| Q2 | OFF-Datenquelle | **Initial Full-Import + täglich inkrementell via API** | Spart Traffic langfristig |
| Q3 | Image-Delivery | **CDN-Subdomain `cdn.healthforge.endgear.de` public-read** | Performant, Cache-friendly |
| Q3b | Domain-Schema | **api.healthforge.endgear.de / admin.healthforge.endgear.de / cdn.healthforge.endgear.de** | User-Domain `endgear.de` mit nested Subdomain |
| Q4 | Auto-Planner (P4) | **Server-side Beam-Search** | Zentral, simpler |
| Q5 | Bayesian Insights (P4) | **Nur lokal auf Client** | Privacy maximal |
| Q6 | JWT-Algorithmus | **HS512 (symmetrisch)** für v1.0 | Einfach, ausreichend |
| Q7 | Object-Storage | **MinIO self-hosted** im docker-compose | Volle Kontrolle |
| Q8 | Backups | **Lokal auf MinIO** im selben VPS + täglich `pg_dump` (30 Tage Retention) | Pragmatisch, Risiko akzeptiert |
| Q9 | Staging-Environment | **KEINE Staging** in v1.0 — nur Local-Dev + Prod | Solo-Dev-Setup |
| Q10 | Test-Strategie | **Keine automatisierten Tests** für v1.0 | User-Decision, manueller Smoke-Test vor Deploy |
| Q11 | Audit-Log-Retention | **Rolling 90 Tage** | Balance DB-Größe vs. Forensik |
| Q12 | Rate-Limit-Storage | **Caffeine in-process** | Single-VPS, kein Redis nötig |

### Konsequenzen / Hinweise

- **Q9 (keine Staging) + Q10 (keine Tests)** → Höheres Risiko bei Prod-Deploys.
  **Mitigation:** sehr kleine Commits/Releases, jeder Deploy manuell smoke-getestet,
  Rollback-Strategy dokumentieren in `Runbook.md`. Roll-Back-Plan: `docker compose pull api:<previous-tag> && docker compose up -d api`.
- **Q8 (Backups lokal)** → Bei VPS-Total-Loss sind Backups mit weg. Akzeptiert für v1.0,
  Off-site-Sync später möglich.
- **Q3 (Subdomains)** → DNS-Records bei Domain-Registrar (Netcup/Cloudflare/etc.) für
  3 Subdomains anlegen, Caddy issues TLS automatisch.

---

## 10. Anhänge / Folgedokumente

- `docs/Runbook.md` — Restore, Incident-Response, On-Call Procedures (TODO)
- `docs/API.md` — OpenAPI 3.1 (auto-generated from springdoc) (TODO)
- `docs/SprintPlan.md` — Phase-by-Phase Deliverables (TODO)
- `docs/TraceabilityMatrix.md` — REQ → File-Mapping (TODO)
- `docs/GUI.md` — Design-Tokens, Components (TODO)

---

**Ende Architecture v0.1 DRAFT.**
