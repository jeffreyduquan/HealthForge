# HealthForge â€” Architecture

**Version:** 0.2 (LOCKED â€” alle Architektur-Entscheidungen final fÃ¼r v1.0)
**Datum:** 2025-05-25
**Scope:** v1.0 Unified Release (Phase P1â€“P4)
**VorgÃ¤ngerdokumente:** [ReqSpec.md](ReqSpec.md) v0.2, [UsabilityMap.md](UsabilityMap.md) v0.1
**Changelog:**
- v0.2 (2025-05-25): Alle 12 Open Questions aus v0.1 Â§9 gelockt â†’ siehe Â§9 Locked Decisions.
- v0.1 (2025-05-25): Initial Draft.

> Dieses Dokument legt die technische Architektur fest. Alle Entscheidungen in Â§9 sind
> LOCKED. Ã„nderungen erfordern explizite Zustimmung.

---

## 1. High-Level Topology

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”         â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚   Android Client (Kotlin)   â”‚         â”‚       Admin Web UI (React)       â”‚
â”‚   - Compose + Hilt          â”‚         â”‚   - Vite + TypeScript + MUI      â”‚
â”‚   - Room (SQLCipher)        â”‚         â”‚   - Served as /admin via Caddy   â”‚
â”‚   - DataStore               â”‚         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
â”‚   - Retrofit/OkHttp/Moshi   â”‚                        â”‚
â”‚                              â”‚                        â”‚ HTTPS
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                        â”‚
               â”‚ HTTPS (TLS 1.3)                       â”‚
               â”‚ JWT Bearer                            â”‚
               â–¼                                       â–¼
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â”‚      Caddy 2 (Reverse Proxy + auto-TLS)            â”‚
        â”‚      Domains: api.healthforge.de, admin.healthforge.de â”‚
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                               â”‚
            â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
            â–¼                  â–¼                  â–¼
    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
    â”‚ Spring Boot 3 â”‚  â”‚   MinIO       â”‚  â”‚ PostgreSQL16 â”‚
    â”‚ (Kotlin/JVM)  â”‚  â”‚ (S3-API)      â”‚  â”‚              â”‚
    â”‚ Port 8080     â”‚  â”‚ Port 9000     â”‚  â”‚ Port 5432    â”‚
    â””â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
            â”‚ Cron jobs
    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
    â”‚  BLS ETL  â”‚  OFF ETL         â”‚
    â”‚  (BLS 4.0)â”‚  (Open Food Factsâ”‚
    â”‚  worker   â”‚   dumpâ†’publish)   â”‚
    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

Alle Komponenten laufen als Docker-Container auf einem **Netcup VPS** (Single-Host
Deployment fÃ¼r v1.0). Orchestrierung via **docker-compose**.

---

## 2. Tech Stack Reference

| Layer | Technologie | Version | BegrÃ¼ndung |
|---|---|---|---|
| Android | Kotlin | 2.0+ | Standard |
| Android UI | Jetpack Compose | BOM 2025.x | Modern, deklarativ |
| Android DI | Hilt | 2.51+ | Standard fÃ¼r Compose |
| Android DB | Room + SQLCipher | 2.6+ / 4.6+ | VerschlÃ¼sselte lokale DB |
| Android Net | Retrofit 2 + OkHttp 4 + Moshi | latest | JSON via Moshi (Kotlin-friendly) |
| Android Auth-Storage | EncryptedSharedPreferences + Keystore | androidx.security | Token-Speicher |
| Server | Spring Boot | 3.3+ | Kotlin-first stable |
| Server Lang | Kotlin | 2.0+ | Konsistenz mit Client |
| Server DB | PostgreSQL | 16 | Stabil + JSONB fÃ¼r flexible Felder |
| Server Migrations | Flyway | 10+ | Standard, versioned SQL |
| Server Obj-Storage | MinIO | latest stable | S3-API, self-hosted |
| Server API-Doc | springdoc-openapi | 2.5+ | OpenAPI 3.1 |
| Server Auth | spring-security-jwt (custom) | â€” | siehe Â§6.3 |
| Server Rate-Limit | Bucket4j + Caffeine | 8+ | In-Memory ausreichend fÃ¼r v1.0 |
| Server Metrics | Micrometer + Prometheus | â€” | scrape via Caddy-protected endpoint |
| Server Logs | Logback + Logstash-encoder | â€” | JSON structured logs |
| Push | ~~Firebase Cloud Messaging~~ ENTFERNT | â€” | In-App-Badge + optional Email-Digest |
| Admin UI | React 18 + Vite + TypeScript + MUI | latest | Schnelles Setup, robuste Components. EnthÃ¤lt APK-Release-Management |
| Reverse Proxy | Caddy (HealthForge) | 2.8+ | LÃ¤uft parallel zu Caddy (Dwight) auf Ports 8080/8443 (HTTP-only). HTTPS nicht mÃ¶glich da dwight Port 80/443 blockiert |
| Reverse Proxy | Caddy (Dwight) | 2-alpine | Bestehendes Projekt auf Ports 80/443 â€” lÃ¤uft unverÃ¤ndert |
| CI/CD | GitHub Actions | â€” | SSH-Deploy + Container-Build + SCP-Frontend-Deploy |

---

## 3. Client Architecture (Android)

### 3.1 Layered Structure (Clean Architecture light)

```
app/
â”œâ”€â”€ presentation/        â† Compose Screens + ViewModels (Hilt)
â”‚   â”œâ”€â”€ home/             â† Home-Tab (ErnÃ¤hrungsÃ¼bersicht + Mahlzeiten-Wochenplaner, ex-Plan integriert)
â”‚   â”œâ”€â”€ essen/
â”‚   â”‚   â”œâ”€â”€ lebensmittel/
â”‚   â”‚   â”œâ”€â”€ rezepte/
â”‚   â”‚   â””â”€â”€ supplements/
â”‚   â”œâ”€â”€ log/
â”‚   â”œâ”€â”€ profil/
â”‚   â”œâ”€â”€ onboarding/
â”‚   â””â”€â”€ common/         â† reusable Composables
â”œâ”€â”€ domain/             â† UseCases (suspend functions), pure Kotlin
â”‚   â”œâ”€â”€ model/          â† domain entities (no Android deps)
â”‚   â”œâ”€â”€ usecase/
â”‚   â”œâ”€â”€ insights/       â† Bayesian Lift-Korrelations-Rechner, **local-only** (kein Network-Import, REQ-INSIGHT-004) (P4.S3)
â”‚   â””â”€â”€ repository/     â† Repository interfaces
â”œâ”€â”€ data/               â† Repository implementations
â”‚   â”œâ”€â”€ local/          â† Room DAOs, entities, SQLCipher
â”‚   â”œâ”€â”€ remote/         â† Retrofit services, DTOs, mappers
â”‚   â”œâ”€â”€ prefs/          â† DataStore + EncryptedSharedPreferences
â”‚   â””â”€â”€ sync/           â† WorkManager jobs (recipe-cache, group-sync)
â”œâ”€â”€ di/                 â† Hilt modules
â””â”€â”€ util/
```

**Datenfluss:** `Compose UI â†’ ViewModel (StateFlow) â†’ UseCase â†’ Repository â†’ DataSource (Room | Retrofit)`.

### 3.2 Room Schema (Client-side, verschlÃ¼sselt)

| Tabelle | Zweck | Phase |
|---|---|---|
| `user_profile` | Lokales Profil (Stammdaten, Ziele, Sport) | P1 |
| `allergy` | User-Allergien (M:N implicit) | P1 |
| `intolerance` | User-Intoleranzen + Schweregrad | P1 |
| `condition` | Conditions (z.B. Reflux, IBS) | P1 |
| `ingredient_cache` | Read-only Cache vom Server | P1 |
| `ingredient_fts` | FTS4 Virtual Table fÃ¼r Such-Performance | P1 |
| `ingredient_rating` | Lokales Rating (MORE_OFTEN / INTOLERANT) | P1 |
| `recipe_cache` | Server-Rezepte gecacht | P2 |
| `recipe_local` | User-eigene Rezepte (nicht synced) | P2 |
| `recipe_ingredient` | Rezept â†” Zutat M:N (Menge, Einheit) | P2 |
| `recipe_step` | Schritt-Liste pro Rezept | P2 |
| `recipe_rating_local` | Lokales Rating (MORE_OFTEN / INTOLERANT) | P2 |
| `supplement` | Lokal angelegte Supplements | P1 |
| `supplement_intake` | Zeitstempel pro Einnahme | P1 |
| `supplement_reminder` | ZeitplÃ¤ne (AlarmManager-mirror) | P1 |
| `intake_entry` | Verzehrte Lebensmittel/Rezepte (Tag/Zeit/Menge) | P1 |
| `water_intake` | Wasser-Logs pro Tag | P1 |
| `meal_plan_day` | Tagesplan-Header | P2 |
| `meal_plan_slot` | Slot pro Tagesplan (Mahlzeit-Typ, Zeit, Item-Refs) | P2 |
| `shopping_list_item` | Einkaufsliste (aggregiert aus Plan) | P3 |
| `log_entry` | Symptom-Tagebuch-Eintrag pro Tag | P3 |
| `log_symptom` | Symptom-Pos. mit Severity 1â€“5 | P3 |
| `log_tag` | User-Tags (z.B. "Stress", "Periode") | P3 |
| `custom_symptom` | Benutzerdefinierte Symptomliste | P3 |
| `group_cache` | Gemeinsame Gruppen (Read-Cache) | P3 |
| `pending_op` | Outbox-Pattern: Aktionen offline â†’ spÃ¤ter syncen | P1 |

**Migrations:** Auto-migration wo mÃ¶glich, sonst manuell. Room-Version bumpen pro Release.

**FTS:** `ingredient_fts` content-rowid linked auf `ingredient_cache.id`, Token-Splitting
fÃ¼r Deutsch (unicode61 + porter stemming optional).

**SQLCipher Key:** Random 32-byte key generiert beim ersten Start, gespeichert in
**EncryptedSharedPreferences** (Android Keystore-backed). Key wird nicht migriert /
exportiert.

### 3.3 Networking

- **Base URL:** `https://api.healthforge.de/v1/` (Build-Config-Variable: `BASE_URL`)
- **Auth:** OkHttp Authenticator + Interceptor injecten `Authorization: Bearer <access>`
  Header. Bei 401 â†’ Refresh-Flow (siehe Â§6.3).
- **Timeouts:** Connect 10s, Read 30s, Write 30s.
- **Retry:** Idempotente GETs â†’ 3 Retries mit exponentiellem Backoff (1s, 2s, 4s).
  POST/PUT/DELETE â†’ kein automatischer Retry (User-initiierte Retry-Button).
- **Cache:** OkHttp HTTP-Cache (50 MB) fÃ¼r GET-Responses mit `Cache-Control: max-age=...`.
- **Offline:** Repository checkt zuerst Room â†’ falls leer/stale, lÃ¤dt von Server â†’
  schreibt Cache. Bei Netz-Fehler â†’ liefert Cache mit "stale"-Flag.

### 3.4 Sync Strategie

- **Read-Cache** (Ingredients, Rezepte, Gruppen): `If-Modified-Since` + ETag.
  Background-Refresh via WorkManager (Constraint: WiFi + Charging optional).
- **Outbox-Pattern** fÃ¼r Writes: Aktionen die offline gemacht werden (z.B. Recipe Like)
  landen in `pending_op` â†’ WorkManager sendet sie sobald online.
- **Konflikte:** Server-wins fÃ¼r Lebensmittel/Supplements. Lokale Rezepte: User entscheidet
  beim Konflikt (P2-Feature, P1: noch keine Konflikte mÃ¶glich).

### 3.5 Push-Notifications

- ~~**FCM Token-Registration:** Beim Login â†’ POST `/devices/register`.~~ ENTFERNT (2026-05-25).
- **Topic-Subscriptions:** `group_<groupId>` pro Mitgliedschaft.
- **Lokale Reminders:** **AlarmManager** (exact-alarm Permission auf Android 14+ Ã¼ber
  `USE_EXACT_ALARM` oder Inexact als Fallback). Notification-Channel pro Reminder-Typ
  (Wasser / Mahlzeit / Supplement).

---

## 4. Server Architecture

### 4.1 Module Layout (Single Spring Boot App)

```
server/
â”œâ”€â”€ src/main/kotlin/de/healthforge/
â”‚   â”œâ”€â”€ HealthForgeApplication.kt
â”‚   â”œâ”€â”€ auth/           â† JWT, Login, Refresh, Register, Invite-Validation
â”‚   â”œâ”€â”€ user/           â† Profile, Devices, Preferences (server-side mirror)
â”‚   â”œâ”€â”€ ingredient/     â† CRUD, Search, ETL-trigger, User-Suggest (PENDING), Field-PR (Whitelist-11) + Admin-Approve (P4.S1)
â”‚   â”œâ”€â”€ recipe/         â† CRUD, Browse, Like, Report, Comment-FREE-TEXT-OUT
â”‚   â”œâ”€â”€ autoplan/       â† Beam-Search-Mahlzeitenplaner, stateless `POST /v1/plans/generate` (P4.S2)
â”‚   â”œâ”€â”€ supplement/     â† Public catalog + Peer-Review-Queue (P3.S4 Slice 2)
â”‚   â”œâ”€â”€ group/          â† Privat/Ã–ffentlich, Members, Invites, Feed
â”‚   â”œâ”€â”€ community/      â† Community Ratings (RECOMMEND/NOT_RECOMMEND) + Recipe-Reports (P3.S3)
â”‚   â”œâ”€â”€ admin/          â† Endpoints fÃ¼r Admin-Web-UI (Mod + Approval); cross-cutting Stats/Audit: `AdminStatsController` (/v1/stats/{dashboard,statistics}) + `AdminAuditController` (/v1/audit) (P4.S4)
â”‚   â”œâ”€â”€ etl/            â† OFF Importer, Scheduled Jobs (@Scheduled / Quartz)
â”‚   â”œâ”€â”€ media/          â† MinIO Presigned URLs + image-resize pipeline
â”‚   â”œâ”€â”€ export/         â† PDF/JSON DSGVO Export (P3.S4 Slice 3 â€” Server-Anteil: Account + eigene Rezepte + Supplement-VorschlÃ¤ge; OpenPDF 1.3.43 LGPL)
â”‚   â”‚â”€â”€ ~~notification/~~   â† ENTFERNT (FCM gestrichen)
â”‚   â”œâ”€â”€ ratelimit/      â† Bucket4j Filter
â”‚   â”œâ”€â”€ config/         â† Beans, Properties
â”‚   â””â”€â”€ common/         â† Errors, Validators, Audit
â””â”€â”€ src/main/resources/
    â”œâ”€â”€ application.yml
    â”œâ”€â”€ db/migration/   â† Flyway V1__init.sql, V2__..., etc.
    â””â”€â”€ openapi/        â† (auto-generated)
```

**API-Versionierung:** Path-prefix `/v1/`. Bei Breaking Change â†’ `/v2/` parallel.

### 4.2 PostgreSQL Schema (Highlevel)

> Konkrete DDL wird in Flyway-Migration `V1__init.sql` ausformuliert. Hier nur
> Tabellen-Ãœbersicht + wichtigste Indizes.

**Auth & User:**

- `users` (id UUID PK, email UNIQUE, password_hash, display_name, status, role, created_at, last_login_at)
- `refresh_tokens` (id UUID PK, user_id FK, token_hash, expires_at, revoked_at, device_id)
- `invites` (id UUID PK, code UNIQUE, created_by FK, used_by FK NULL, expires_at, used_at)
- ~~`devices` (id UUID PK, user_id FK, fcm_token, platform, last_seen_at)~~ ENTFERNT

**Ingredients (Master-Catalog):**

- `ingredients` (id UUID PK, off_id NULL UNIQUE, source ENUM, name_de, brand NULL,
  per_100g JSONB, allergens TEXT[], status ENUM[DRAFT/PUBLISHED/ARCHIVED], created_by FK NULL,
  last_etl_at, last_admin_edit_at, version INT)
  - Index: `gin(to_tsvector('german', name_de || ' ' || brand))` fÃ¼r FTS
  - Index: `(status, name_de)`
  - Sticky Admin-Edit: bei ETL-Update werden Felder, die `last_admin_edit_at > last_etl_at`
    haben, **nicht** Ã¼berschrieben.
- `ingredient_field_pr` (id UUID PK, ingredient_id FK, field, old_value, new_value,
  proposed_by FK, status, reviewed_by FK NULL, reviewed_at)
- `ingredient_user_suggestions` (id UUID PK, name_de, per_100g JSONB, proposed_by FK, status)

**Recipes (LOCKED 2026-05-26 â€” P2.S1 Schema, aligned with REQ-RECIPE-001..009):**

- `recipes` (id UUID PK, author_id FKâ†’users, title, description NULL, image_key NULL,
  servings INT DEFAULT 1, prep_minutes INT, cook_minutes INT NULL (optional, nicht von ReqSpec verlangt),
  slot_tags TEXT[] NOT NULL CHECK (cardinality(slot_tags) >= 1) â€” Werte aus {BREAKFAST,LUNCH,DINNER,SNACK},
  status ENUM[PUBLISHED/REMOVED] DEFAULT 'PUBLISHED' (Soft-Delete fÃ¼r REQ-RECIPE-009 Snapshot-Resilienz),
  visibility ENUM[PUBLIC/PRIVATE/GROUP] NOT NULL (REQ-RECIPE-003),
  group_id UUID NULL FKâ†’groups (only when visibility=GROUP; CHECK constraint),
  is_official BOOL DEFAULT FALSE (fÃ¼r Admin-curated Recipes, in P2 ungenutzt aber bereitgehalten),
  created_at, updated_at)
  - Index: `gin(to_tsvector('german', hf_immutable_unaccent(title || ' ' || coalesce(description,''))))` fÃ¼r FTS
  - Index: `(status, visibility, created_at DESC)` fÃ¼r Browse
  - Index: `(author_id)` fÃ¼r "Meine Rezepte"
- `recipe_ingredients` (recipe_id FK, ingredient_id FKâ†’ingredients, quantity NUMERIC, unit TEXT, position INT, optional BOOL DEFAULT FALSE, PK (recipe_id, position))
- `recipe_steps` (recipe_id FK, position INT, text TEXT, image_key TEXT NULL, PK (recipe_id, position))
- `recipe_likes` (recipe_id FK, user_id FK, created_at, PK (recipe_id, user_id)) â€” REQ-RECIPE-004 Saved-Liste
- `recipe_reports` (id UUID PK, recipe_id FK, reporter_id FK, reason TEXT, status ENUM[OPEN/RESOLVED] DEFAULT 'OPEN', created_at) â€” Schema in P2, Endpoints P3
- `recipe_ratings_community` (recipe_id FK, user_id FK, value ENUM[RECOMMEND/NOT_RECOMMEND], created_at, PK (recipe_id, user_id)) â€” REQ-RATING-002/005

**Supplements:**

- `supplements_public` (id UUID PK, name_de, brand, unit_label, default_dose, kcal/protein/carbs/fat_per_dose, micronutrients_json JSONB, notes, created_by FK NULL, created_at) â€” REQ-SUPP-004 globaler Lese-Katalog (P3.S4 Slice 2)
- `supplement_suggestions` (id UUID PK, proposer_id FK, name_de, brand, unit_label, default_dose, NÃ¤hrwerte, status ENUM[PENDING/APPROVED/REJECTED] DEFAULT 'PENDING', reviewer_id FK NULL, reviewed_at, review_note, public_id FK NULL, created_at) â€” Admin-Peer-Review-Queue (P3.S4 Slice 2)

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
  `recipe_reports`, kann ggf. konsolidiert werden â€” DEFAULT: separate Tabellen pro Domain).

### 4.3 Flyway Migrations

Die ursprÃ¼ngliche Phasen-Numerierung (V1=P1, V2=P2, ...) wurde durch das natÃ¼rliche Sprint-Wachstum Ã¼berholt â€” Flyway ist forward-only-strict-monoton, also gilt jetzt **sequentiell-pro-Sprint**:

- `V1__bootstrap.sql` â€” Postgres-Extensions (P1.S1)
- `V2__auth_schema.sql` â€” users/refresh_tokens/invites (P1.S2)
- `V3__ingredient_schema.sql` â€” ingredients + ETL-Tabellen + `hf_immutable_unaccent`-Wrapper (P1.S4)
- `V4__dev_seed_ingredients.sql` â€” Dev-Seed (P1.S4)
- `V5__ingredient_trgm_indexes.sql` â€” pg_trgm-Indizes fÃ¼r Fuzzy-Search (P1.S5)
- `V6__recipes.sql` â€” Rezepte + Likes + Reports + Community-Ratings (P2.S1, **next**)
- Weitere Sprints: V7+ entsprechend (siehe SprintPlan).
- **V12__nutrients_overhaul.sql** (P7.S1) â€” `ingredients.micronutrients_json JSONB` + `ingredients.fdc_id BIGINT UNIQUE` (REQ-INGR-MICRONUTRIENTS-001).
- Nur **forward-only**, never editieren bestehende Files.
- Repeatable: `R__seed_official_supplements.sql` fÃ¼r statische Refdaten.

### 4.4 MinIO Bucket-Struktur

| Bucket | Public? | Lifecycle | Inhalt |
|---|---|---|---|
| `recipes` | public-read (CDN-friendly) | â€” | Rezept-Bilder (resized) |
| `ingredients` | public-read | â€” | Optional Ingredient-Bilder (mostly OFF-CDN) |
| `supplements` | public-read | â€” | Supplement-Bilder |
| `avatars` | public-read | â€” | User-Avatare (klein, â‰¤256px) |
| `exports` | private | TTL 7 Tage | DSGVO-Export-Files (presigned URL) |
| `backups` | private | TTL 30 Tage | DB-Dumps via Cron |

**DEFAULT** fÃ¼r Bilder: Client uploaded auf Spring-Endpoint `POST /media/upload` â†’
Server resized auf 3 GrÃ¶ÃŸen (thumb 256px, medium 800px, full 1600px) via
**ImageIO/Thumbnailator** â†’ PUT zu MinIO â†’ response gibt Key zurÃ¼ck. Client speichert
nur Key. URL-Konstruktion: `https://cdn.healthforge.de/<bucket>/<key>` (Caddy
serviert `cdn.` â†’ MinIO public-read direkt).

### 4.5 OFF ETL Pipeline (DEPRECATED in P7 â€” siehe Â§4.5b BLS)

> **P7-Decision (2026-05-27):** OFF wird als prim\u00e4re Quelle aufgegeben (Vitamin-/Mineralstoff-Coverage < 5 %). Die folgende Spezifikation bleibt zur historischen Referenz; die Importer-Skelette werden auf `@Deprecated` markiert und nicht weiter aktiv gepflegt.

**Strategie:** Einmaliger Full-Import beim ersten Deploy (~3 GB Dump), danach tÃ¤glich
nur inkrementelle Deltas via OFF REST API.

- **Source:** [OpenFoodFacts](https://world.openfoodfacts.org/)
- **Initial Bootstrap (einmalig):**
  1. Admin-Trigger `POST /admin/etl/off/bootstrap` (oder manueller `import-off` CLI-Befehl).
  2. Streamender Download des JSONL-Dumps (`~3 GB`, gz).
  3. Filter `countries_tags` enthÃ¤lt `germany`.
  4. Stage â†’ Merge â†’ Index (~10â€“30 min).
  5. Log in `etl_runs` (kind=`bootstrap`).
- **Daily Incremental (recurring):**
  - **Trigger:** `@Scheduled(cron = "0 0 3 * * *")` â€” tÃ¤glich 03:00 UTC.
  - **Phasen:**
    1. Letzte Run-Timestamp aus `etl_runs` lesen.
    2. OFF API `GET /api/v2/search?last_modified_t__gt=<ts>&countries_tags=germany&page_size=100`
       â€” paginieren bis kein Ergebnis mehr.
    3. Pro Produkt: Stage â†’ Merge (sticky-fields beachten).
    4. Log in `etl_runs` (kind=`incremental`, stats).
  - **Erwarteter Traffic:** wenige MB statt GB pro Tag.
- **Sticky-Admin-Fields:** Bei Update werden Felder mit `last_admin_edit_at > last_etl_at`
  **nicht** Ã¼berschrieben.
- **Idempotenz:** ETL kann beliebig oft laufen, neuere Daten gewinnen auÃŸer bei sticky-fields.
- **Manual Trigger:** Admin-Endpoint `POST /admin/etl/off/run` (Admin-only, rate-limited).

### 4.5b BLS ETL Pipeline (P7 — aktive Baseline)

**Source:** BLS 4.0 FoodData CSV (`seed/bls_4_0.csv`) als Primärquelle für Makro- und Mikronährstoffe.

**Komponenten:**
- `server/etl/Importers.kt` — BLS-Importer (`BlsImporter`) inkl. Makro-/Mikro-Mapping.
- `server/etl/ApplyBlsCuration.kt` — Curation-Schicht auf BLS-Codes (SIGHI/Allergene/FODMAP).
- `seed/bls_curation.csv` / `seed/bls_sighi.csv` — Referenzdaten für Histamin-/SIGHI- + Allergen-/FODMAP-Zuordnung.

**Mapping:**
- BLS-Nährstoffcodes werden auf internen `NutrientCatalog` gemappt (Makros + Mikros).
- Werte landen in `ingredients`-Spalten (`kcal`, `protein`, `fat`, `carbs`, `satfat`, `fiber`, `sugar`, `salt`) sowie `micronutrients_json`.

**Pipeline (Bootstrap):**
1. Beim ersten App-Start (oder manuellem Trigger) startet der BLS-Importer.
2. Pro Datensatz: Upsert in `ingredients` (`source='BLS'`) inklusive SIGHI/FODMAP/Allergen-Curation.
3. Log in `etl_runs` für die Auswertung.

**Re-Sync:** `POST /admin/etl/run?source=BLS` bei Pipeline- oder Seed-Updates.

**Sticky-Admin-Fields:** Felder mit `last_admin_edit_at > last_etl_at` werden nicht überschrieben。

### 4.5c OFF ETL Pipeline (DEPRECATED in P7 — nicht mehr aktiv)

OFF bleibt als Fallback, aber nicht mehr als Primärquelle. OFF-Importer sind als Legacy gekennzeichnet und werden nur gezielt/defensiv genutzt.

### 4.6 Image Pipeline

- Client komprimiert vor Upload (max 2048px KantenlÃ¤nge, JPEG Q85) â†’ reduces traffic.
- Server validiert (max 8 MB, MIME-Whitelist: `image/jpeg`, `image/png`, `image/webp`).
- Server resized (Thumbnailator) â†’ 3 Varianten â†’ MinIO.
- Original wird verworfen (Privacy-Bonus: EXIF stripped).

---

## 5. Admin Web UI

- **Stack:** React 18 + Vite + TypeScript + MUI + React-Router 6 + TanStack Query +
  Axios.
- **Auth:** Eigener Login (gleiche `users`-Tabelle, role=ADMIN). JWT in httpOnly Cookie
  (CSRF via SameSite=Lax + CSRF-Token Header).
- **Build & Serve:** Static Build â†’ Caddy serviert von `admin.healthforge.de`.
  Reverse-Proxy auf `/api/*` zum Backend.
- **Seiten:** siehe [UsabilityMap Â§9](UsabilityMap.md) â€” 11 Seiten.

---

## 6. Cross-Cutting Concerns

### 6.1 Security

- TLS 1.3 enforced via Caddy.
- HSTS, X-Content-Type-Options, X-Frame-Options, CSP Header via Caddy.
- Passwords: **bcrypt** cost factor 12.
- JWT-Signing: **HS512** (symmetrisch) mit Secret aus env (LOCKED Q6). Rotation manuell
  bei Bedarf â€” kein Upgrade auf RS256 fÃ¼r v1.0 vorgesehen.
- Input-Validation: Bean-Validation (`jakarta.validation`) + custom Validators.
- SQL-Injection: nur JPA-Repos + Parameterized Queries, **kein** String-Concat.
- File-Uploads: MIME-Whitelist + Magic-Bytes-Check + GrÃ¶ÃŸe.
- Rate-Limiting (Bucket4j + **Caffeine in-process**, LOCKED Q12):
  - Anonyme Endpoints (Login, Register): 5/min/IP
  - Auth-Endpoints global: 60/min/User
  - Admin: 120/min/User
  - Search: 30/min/User

  Counter im RAM des Spring-Boot-Prozesses. Bei Restart: Counter zurÃ¼ckgesetzt (akzeptabel).
  Single-VPS-Setup â†’ kein Redis nÃ¶tig.

### 6.2 Audit

Jede mutierende Admin-Aktion â†’ `audit_log`-Eintrag (Actor, Action, Target, Payload-Diff).

**Retention:** Rolling **90 Tage** (LOCKED Q11). Cron-Job lÃ¶scht EintrÃ¤ge Ã¤lter als 90 Tage
tÃ¤glich um 04:00 UTC. Bei kritischen VorfÃ¤llen kÃ¶nnen Admins betroffene EintrÃ¤ge vorher
in eine separate `audit_archive`-Tabelle kopieren (P3+).

### 6.3 JWT Flow

```
1. POST /auth/login {email, password}                  â†’ {access (15min), refresh (30d)}
2. Authorization: Bearer <access>                      â†’ 200 OK
3. Bei 401 (expired):
   POST /auth/refresh {refresh}                        â†’ {access (neu), refresh (neu)}
   (Rotation: alter refresh wird in DB als revoked markiert)
4. POST /auth/logout {refresh}                         â†’ DB-Revocation
```

- Access: stateless JWT.
- Refresh: opaque Token, gehashed in DB (`refresh_tokens`). Bei Verdacht (z.B. genutzter
  revokierter Token) â†’ **Token-Family** des Users wird komplett revoked (Re-Login forced).
- Device-Binding optional: Refresh-Token an `device_id` gebunden (P2+).

### 6.4 Observability

- **Metrics:** Micrometer â†’ Prometheus-Endpoint `/actuator/prometheus` (Caddy
  Basic-Auth protected).
- **Logs:** Logback JSON â†’ STDOUT â†’ Docker collects â†’ optional Loki spÃ¤ter.
- **Tracing:** Out-of-scope fÃ¼r v1.0 (DEFAULT).

### 6.5 Internationalisierung

- v1.0: nur **de_DE**. Server-side: alle User-facing-Strings hartcodiert deutsch.
  Client: `strings.xml` deutsch.
- Architektur unterstÃ¼tzt aber `Accept-Language` Header â€” zukunftssicher.

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
  backup:       # alpine + cron, scheduled pg_dump â†’ MinIO bucket "backups"
```

Networks: `internal` (alle), `web` (nur caddy expose 80/443).

### 7.2 CI/CD (GitHub Actions)

- **`server.yml`:** push to `main` â†’ run tests â†’ build Docker image â†’ push to GHCR â†’
  SSH zum VPS â†’ `docker compose pull api && docker compose up -d api` â†’ smoke-test.
- **`admin-ui.yml`:** push â†’ Vite build â†’ rsync `dist/` zum VPS â†’ Caddy serviert
  automatisch (keine Restart nÃ¶tig).
- **`android.yml`:** push tag `v*` â†’ Gradle assembleRelease â†’ signing â†’ Artefact (APK)
  â†’ Release-Assets. (Play-Store-Upload P4-optional via fastlane.)
- **Secrets:** GitHub Secrets fÃ¼r `SSH_KEY`, `VPS_HOST`, `GHCR_TOKEN`, `KEYSTORE_PASS`.

### 7.3 Backups (LOCKED Q8: Lokal im selben VPS)

- **PostgreSQL:** `pg_dump` tÃ¤glich 02:00 â†’ komprimiert (zstd) â†’ MinIO Bucket `backups/`
  Retention 30 Tage.
- **MinIO:** Buckets-Replikation manuell bei Bedarf (P4-Optional: zweiter MinIO-Node oder off-site Sync).
- **Restore:** Dokumentiert in `docs/Runbook.md` Â§3.3.
- **Risiko-Akzeptanz:** Bei Total-Loss des VPS sind Backups mit verloren. FÃ¼r v1.0
  akzeptiert (User-Decision). Off-site-Sync nach Hetzner Storage Box ist mÃ¶gliches
  Upgrade in P4.

### 7.4 Environments

- **Production:** `api.healthforge.endgear.de`, `admin.healthforge.endgear.de`, `cdn.healthforge.endgear.de`
- **Staging:** **KEINE** Staging-Umgebung in v1.0 (LOCKED Q9). Workflow: Local-Dev â†’ direkt Prod.
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

## 9. Locked Decisions (alle 12 Open Questions geklÃ¤rt)

Alle Entscheidungen final fÃ¼r v1.0. Ã„nderungen erfordern Doc-Versionsbump.

| # | Frage | Entscheidung | BegrÃ¼ndung |
|---|---|---|---|
| Q1 | Admin-UI Stack | **React 18 + Vite + TypeScript + MUI** | Schnelles Setup, fertige Components |
| Q2 | OFF-Datenquelle | **Initial Full-Import + tÃ¤glich inkrementell via API** | Spart Traffic langfristig |
| Q3 | Image-Delivery | **CDN-Subdomain `cdn.healthforge.endgear.de` public-read** | Performant, Cache-friendly |
| Q3b | Domain-Schema | **api.healthforge.endgear.de / admin.healthforge.endgear.de / cdn.healthforge.endgear.de** | User-Domain `endgear.de` mit nested Subdomain |
| Q4 | Auto-Planner (P4) | **Server-side Beam-Search** | Zentral, simpler |
| Q5 | Bayesian Insights (P4) | **Nur lokal auf Client** | Privacy maximal |
| Q5b | Profile + Symptom-Log Storage | **Nur lokal auf Client (Room/SQLCipher)** \u2014 NIE auf Server (REQ-PROFILE-001/002). Server-`users`-Tabelle hat nur Auth-Felder; es gibt KEINE Server-`log_entries`-Tabelle. Konsequenz f\u00fcr P6.S6: Schema-\u00c4nderungen f\u00fcr Profile-Goals + Log-Events laufen \u00fcber Room-Migration (Schema-Bump 6\u21927), nicht Flyway. | Konsistente Privacy-Boundary; Profile-Daten (H\u00f6he, Gewicht, Ziele, Mahlzeiten, Allergien) und Symptom-Tagebuch verlassen das Ger\u00e4t nicht |
| Q6 | JWT-Algorithmus | **HS512 (symmetrisch)** fÃ¼r v1.0 | Einfach, ausreichend |
| Q7 | Object-Storage | **MinIO self-hosted** im docker-compose | Volle Kontrolle |
| Q8 | Backups | **Lokal auf MinIO** im selben VPS + tÃ¤glich `pg_dump` (30 Tage Retention) | Pragmatisch, Risiko akzeptiert |
| Q9 | Staging-Environment | **KEINE Staging** in v1.0 â€” nur Local-Dev + Prod | Solo-Dev-Setup |
| Q10 | Test-Strategie | **Keine automatisierten Tests** fÃ¼r v1.0 | User-Decision, manueller Smoke-Test vor Deploy |
| Q11 | Audit-Log-Retention | **Rolling 90 Tage** | Balance DB-GrÃ¶ÃŸe vs. Forensik |
| Q12 | Rate-Limit-Storage | **Caffeine in-process** | Single-VPS, kein Redis nÃ¶tig |

### Konsequenzen / Hinweise

- **Q9 (keine Staging) + Q10 (keine Tests)** â†’ HÃ¶heres Risiko bei Prod-Deploys.
  **Mitigation:** sehr kleine Commits/Releases, jeder Deploy manuell smoke-getestet,
  Rollback-Strategy dokumentieren in `Runbook.md`. Roll-Back-Plan: `docker compose pull api:<previous-tag> && docker compose up -d api`.
- **Q8 (Backups lokal)** â†’ Bei VPS-Total-Loss sind Backups mit weg. Akzeptiert fÃ¼r v1.0,
  Off-site-Sync spÃ¤ter mÃ¶glich.
- **Q3 (Subdomains)** â†’ DNS-Records bei Domain-Registrar (Netcup/Cloudflare/etc.) fÃ¼r
  3 Subdomains anlegen, Caddy issues TLS automatisch.

---

## 10. AnhÃ¤nge / Folgedokumente

- `docs/Runbook.md` â€” Restore, Incident-Response, On-Call Procedures â€” v1.0 geÃ¤ndert 2026-05-26
- `docs/API.md` â€” OpenAPI 3.1 (auto-generated from springdoc) (TODO)
- `docs/SprintPlan.md` â€” Phase-by-Phase Deliverables (TODO)
- `docs/TraceabilityMatrix.md` â€” REQ â†’ File-Mapping (TODO)
- `docs/GUI.md` â€” Design-Tokens, Components (TODO)
- `docs/HistamindDesignReference.md` â€” P6 Master-Designquelle (LOCKED 2026-05-26)

---

**Ende Architecture v0.1 DRAFT.**

---

## Anhang G â€” P6 Glossary-Lock (eingefÃ¼gt 2026-05-26, P6.S1)

**Trigger:** Finding F-008 (Wording-Inkonsistenz â€žZutat" vs. â€žLebensmittel" im Plan-Add-Sheet).

| Begriff | LOCKED Definition |
|---|---|
| **Lebensmittel** | Standalone-Eintrag in der `ingredients`-Tabelle. Hat NÃ¤hrwerte, Allergen-Flags, Histamin-SIGHI, Barcode etc. Wird vom User direkt in Intake/Plan gewÃ¤hlt. **Anzeige-Wording app-weit:** â€žLebensmittel". |
| **Zutat** | Bestandteil EINES Rezepts (Rezept-internes Konzept). In der Rezept-Definition referenziert ein Rezept N Zutaten = N `recipe_ingredients`-Rows mit Mengenangabe; jede Zutat verweist auf 1 `ingredients`-Row. AuÃŸerhalb von Rezepten taucht â€žZutat" nicht auf. |
| **Rezept** | Komposition aus Zutaten mit Mengen + ggf. Zubereitungstext. Steht in `recipes`-Tabelle. |
| **Mahlzeit** | Konkreter Intake-Event mit Datum/Uhrzeit/Slot (FrÃ¼hstÃ¼ck/Mittag/...) und N geplanten/konsumierten Items (Rezept- oder Lebensmittel-Refs). |
| **Item** (Plan-Slot) | Ein Lebensmittel ODER Rezept im Plan-Slot. SchlieÃŸt â€žZutat" nicht ein. |
| **Event** (Log) | Symptom-Event-Datensatz im Log (severity + tags + note + time). Ersetzt vorherigen â€žLog-Entry"-Begriff (Tagebuch-Modell). |
| **Pinned Nutrient** | Vom User markierter N\u00e4hrstoff, der auf Home als Progress-Karte sichtbar bleibt. Persistiert **device-local** in `UserProfileEntity.pinnedNutrientsJson` (Room/SQLCipher, Privacy-Boundary REQ-PROFILE-001/002). Default seit P7: `[\"kcal\",\"protein\",\"carbs\",\"fat\",\"water\"]`. Verwaltung erfolgt im Home-Tab (REQ-HOME-NUTRIENT-LIST-001). |\n| **Nutrient Catalog** | Statische Liste der ~30 unterst\u00fctzten N\u00e4hrstoffe (8 Makros + 13 Vitamine + 11 Mineralstoffe + Pseudo-`water`). Definiert in `domain/nutrition/NutrientCatalog.kt` (Android) und `de.healthforge.domain.nutrition.NutrientCatalog.kt` (Server, gespiegelt). Pro Eintrag: `key`, `displayDe`, `unit`, `defaultPerDay(profile)`, `category`. Quelle: DGE-Referenzwerte (REQ-NUTRIENT-CATALOG-001). |\n| **Micronutrients-JSON** | Spalte `ingredients.micronutrients_json JSONB` (V12). Map<`NutrientCatalog.key`, mg-oder-\u00b5g-pro-100g>. Befuellt durch BLS-4.0-Import (Makros + Mikros). Rezept-Aggregation: live ueber `recipe_ingredients`. |\n| **Water Deficit Scheduler** | `notification/WaterDeficitScheduler.kt` (Android). Ueberwacht `consumed_ml < target_ml(now) - 100ml` und triggert Eskalations-Alarme 30\u219215\u219210\u21925 min mit 5-min Debounce nach Slider-Interaktion. Silent-Window 22\u201308. Ersetzt `WaterReminderScheduler` (REQ-HOME-WATER-ALARM-001). |\n| **Ghost-Target** | Virtuelle Soll-Linie im Wasser-Tracking, linear interpoliert ueber Aktiv-Fenster 08\u201322. Wird als zweite Progress-Schicht in `WaterBarWithGhost` gerendert (transparent hinter realem Progress). Snooze verschiebt sie virtuell +30 min. |

**Wording-Regel:** In UI-Strings/Sheet-Titeln/Buttons: â€žLebensmittel" und â€žRezept" sind die einzigen erlaubten User-facing-Begriffe fÃ¼r DB-Items. â€žZutat" erscheint NUR in der Rezept-Definition-UI (z.B. â€žZutaten dieses Rezepts").

**End of Anhang G.**




