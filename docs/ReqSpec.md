# HealthForge — Requirements Specification

**Version**: 0.2 (Unified-Scope Lock — 2025-05-25)
**Status**: LOCKED. Scope = **v1.0 Single Release covering all former M1+M2+M3 features**. Development proceeds in phases (see §4), but no separate releases.
**Companion docs**: `SprintPlan.md`, `TraceabilityMatrix.md`, `Architecture.md`, `UsabilityMap.md`, `GUI.md`.

This document is the single source of truth for **what HealthForge is**. Anything not in this document or its companion docs is out of scope.

### Changelog
- **v0.2 (2025-05-25)**: Scope merged — M1+M2+M3 → unified v1.0 release. Added REQ-RATING split (local + community). Promoted Supplement-Peer-Review, Meal-Planner, Symptom-Log, Wasser-Tracker, Groups, Reminders, Export, Insights, Barcode, Field-PR, Full Admin UI from "deferred" to in-scope. Locked 5-Tab navigation (Home/Plan/Essen/Log/Profil). Log-Tab repurposed from intake-history → symptom-diary.
- **v0.1 (2025-05-25)**: Initial lock — MVP scope, tech stack, data quality system.

---

## Table of Contents
1. [Vision & Scope](#1-vision--scope)
2. [Personas](#2-personas)
3. [Platform & Languages](#3-platform--languages)
4. [Milestone Overview](#4-milestone-overview)
5. [MVP (M1) Requirements](#5-mvp-m1-requirements)
6. [M2 Outline](#6-m2-outline)
7. [M3 Outline](#7-m3-outline)
8. [Technology Stack (LOCKED)](#8-technology-stack-locked)
9. [High-Level Data Model](#9-high-level-data-model)
10. [Data Quality System](#10-data-quality-system)
11. [Out of Scope](#11-out-of-scope)
12. [Glossary](#12-glossary)

---

## 1. Vision & Scope

HealthForge is a **community-driven healthy-eating Android app** for German-speaking users. It is general-purpose nutrition tracking — not restricted to a specific intolerance — but allergies and food intolerances are **first-class core functionality**.

| Req | Statement |
|---|---|
| REQ-VISION-001 | HealthForge SHALL serve general healthy-eating users, with allergy/intolerance support as a core (not optional) feature set. |
| REQ-VISION-002 | The ingredient database SHALL be German-first and include both whole foods and branded products (e.g. Nutella, Duplo). |
| REQ-VISION-003 | Recipes SHALL be community-driven: users create, share, and consume each other's recipes via a central server. |
| REQ-VISION-004 | Personal health data (allergies, intolerances, symptoms, intake history) SHALL remain on-device and never be uploaded. |
| REQ-VISION-005 | The app SHALL support groups (private + public) as a core v1.0 feature, with `groupId`-scoped recipe visibility designed in from day one. |

---

## 2. Personas

| Req | Statement |
|---|---|
| REQ-PERSONA-001 | **Primary persona**: a German-speaking adult who wants to eat more consciously, may or may not have allergies/intolerances, and is willing to track meals. |
| REQ-PERSONA-002 | **Secondary persona**: an allergy-sufferer (e.g. histamine intolerance, FODMAP, peanut, lactose) who needs reliable allergen filtering when browsing recipes/ingredients. |
| REQ-PERSONA-003 | Multi-profile, caregiver mode, pregnancy/lactation, and children profiles are **out of scope** for MVP. |
| REQ-PERSONA-004 | Medical-grade claims (diagnosis, therapy guidance) are **out of scope** — the app is advisory only. |

---

## 3. Platform & Languages

| Req | Statement |
|---|---|
| REQ-PLATFORM-001 | The MVP SHALL ship as **Android only**. iOS, Web, and Desktop are out of scope. |
| REQ-PLATFORM-002 | Android targets: `minSdk 26`, `targetSdk 35`, `compileSdk 35` (final values confirmed in `Architecture.md`). |
| REQ-PLATFORM-003 | The server SHALL run in Docker on a Netcup VPS, deployed via `docker-compose`. |
| REQ-I18N-001 | Locale = `de_DE` only for MVP. |
| REQ-I18N-002 | All bundled content (ingredient names, recipe titles, UI text, error messages) SHALL be German only. |
| REQ-UNITS-001 | All measurements SHALL be metric (g, mL, °C). Servings/pieces (`piece`, optionally `portion`) are allowed for ingredients/products that ship in discrete units. |

---

## 4. Development Phases (Single Release)

HealthForge ships as **one v1.0 release** containing all features below. Development is phased to manage complexity; unfinished tabs/sections remain visible as "Bald verfügbar" placeholders during dev. Release gate = all REQ-IDs in §5 are ✅ in `TraceabilityMatrix.md`.

| Phase | Theme | Scope summary |
|---|---|---|
| **P1 — Foundation** | Auth + DB + Core UI | Auth, Ingredient DB ETL, server skeleton, Android shell with 5-tab nav, allergen-filtered ingredient browsing, basic home, data quality system, minimal admin web UI, OpenAPI client codegen |
| **P2 — Recipes & Logging** | Recipe authoring + intake | Recipe browsing/creation/edit, Ratings (local + community), Intake-Log (local), Supplements (local + peer-review), Home aggregates, Wasser-Tracker |
| **P3 — Community & Planning** | Groups + Plans + Reminders | Groups (private + public), Manual Meal-Planner, Shopping List, lokale Reminders (AlarmManager), Symptom-Log (Tagebuch), PDF/JSON Export, Reports/Moderation |
| **P4 — Depth & Power** | Smart features + admin tools | User-submitted ingredients, Field-Update-PR + peer-review, Full Admin UI, Auto-Meal-Planner (beam-search), Bayesian Insights |

### 4.1 Navigation (LOCKED)

| Req | Statement |
|---|---|
| REQ-NAV-001 | The app SHALL use a **5-Tab Bottom-Navigation**: `Home`, `Plan`, `Essen`, `Log`, `Profil`. |
| REQ-NAV-002 | The `Essen`-Tab SHALL contain three Top-Sub-Tabs: `Lebensmittel`, `Rezepte`, `Supplements`. |
| REQ-NAV-003 | Tabs not yet implemented in a development phase SHALL render a "Bald verfügbar"-Placeholder. |
| REQ-NAV-004 | The `Log`-Tab SHALL be the **Symptom-Tagebuch** (NOT intake-history). Intake-history is reached from Home via a "Verlauf"-Button. |

---

## 5. Functional Requirements (v1.0 In Scope)

### 5.1 Authentication

| Req | Statement |
|---|---|
| REQ-AUTH-001 | User authentication SHALL use **Email + Password** with bcrypt hashing. |
| REQ-AUTH-002 | Registration SHALL be **invite-only** during MVP: a valid invite code is required. |
| REQ-AUTH-003 | Invite codes SHALL be generated by the Admin in the Admin Web UI. |
| REQ-AUTH-004 | Email verification SHALL be required before any write operations are allowed. |
| REQ-AUTH-005 | The Auth API SHALL issue **JWT** tokens: short-lived `accessToken` (15 min) + `refreshToken` (30 d). |
| REQ-AUTH-006 | Password reset SHALL work via email-token link. |
| REQ-AUTH-007 | The client SHALL store JWTs in `EncryptedSharedPreferences`. |

### 5.2 User Profile (Local-First)

| Req | Statement |
|---|---|
| REQ-PROFILE-001 | The user's allergen list, FODMAP intolerances, and other health-relevant flags SHALL be stored **locally only** (Room DB, encrypted). |
| REQ-PROFILE-002 | The server SHALL NOT receive or persist the user's personal allergen or intolerance settings. |
| REQ-PROFILE-003 | Server-known profile data SHALL be limited to: email, password hash, display name (optional), account state, invite-source. |
| REQ-PROFILE-004 | The user SHALL be able to declare allergies via multi-select (EU-14 baseline list). |
| REQ-PROFILE-005 | The user SHALL be able to declare FODMAP intolerances per type (fructose, lactose, fructans, GOS, polyols). |
| REQ-PROFILE-006 | Onboarding SHALL be a forward-only wizard: age, biological sex, height, weight, allergies, FODMAP intolerances, max prep time, daily meal template. |

### 5.3 Ingredient Database

| Req | Statement |
|---|---|
| REQ-INGR-001 | The server SHALL host a curated ingredient database covering both whole foods and branded products. |
| REQ-INGR-002 | Source mix (locked): **BLS** (Bundeslebensmittelschlüssel) for whole-food baseline, **Open Food Facts** (filtered) for branded products, **Admin-curated overrides** for top-priority items. |
| REQ-INGR-003 | A **SIGHI** mapping CSV (~400 entries, Swiss Interest Group Histamine Intolerance) SHALL be imported as Admin-curated data before MVP launch. |
| REQ-INGR-004 | OFF ETL filter rules (initial): `lang=de OR countries_tags ∋ Germany`; `completeness ≥ 0.5`; `nutriments.energy-kcal_100g present`; dedupe by `(normalized_brand, normalized_name, net_weight_g)` keeping highest completeness. |
| REQ-INGR-005 | OFF resync SHALL run weekly via a server cron job. Admin-curated values are sticky (not overwritten by resync). |
| REQ-INGR-006 | The Monash FODMAP database is **copyrighted** and SHALL NOT be ingested. FODMAP classifications come from public sources or Admin curation only. |

### 5.4 Ingredient Search & Filter

| Req | Statement |
|---|---|
| REQ-SEARCH-001 | Ingredient and recipe search SHALL be **server-side** using PostgreSQL full-text search with German stemming. |
| REQ-SEARCH-002 | Search SHALL be diacritic-insensitive (`müsli == musli`) and case-insensitive. |
| REQ-SEARCH-003 | Filter parameters SHALL be passed as query params (e.g. `?excludeAllergens=peanut,milk&excludeFodmap=lactose`). These are **public filters**, not personalized data. |
| REQ-SEARCH-004 | The client SHALL construct filter queries from the user's local allergy/intolerance profile. |
| REQ-SEARCH-005 | Search results SHALL render a **data quality badge** per ingredient/recipe (see §10). |

### 5.5 Recipes

| Req | Statement |
|---|---|
| REQ-RECIPE-001 | Recipes SHALL be stored on the server in a single central pool. |
| REQ-RECIPE-002 | Each recipe has exactly one **owner** (the creating user). |
| REQ-RECIPE-003 | Recipes SHALL have a `visibility` field with values `public | private | group` (when `group`, a `groupId` is required). |
| REQ-RECIPE-004 | Liking a recipe (`H` save) SHALL NOT copy it — only add a reference in the user's "Saved" list. |
| REQ-RECIPE-005 | Recipe authoring SHALL require: title, prep time, ≥1 slot tag (breakfast/lunch/dinner/snack), ≥1 ingredient with amount+unit, ≥1 step, optional image. |
| REQ-RECIPE-006 | Recipe images SHALL be uploaded to **MinIO** (S3-compatible), max 1080×1080, WebP recommended, max 200 KB. |
| REQ-RECIPE-007 | Recipe nutrition SHALL be computed live from its ingredients (no stored nutrition block on the recipe row). |
| REQ-RECIPE-008 | Editing a recipe SHALL be restricted to its owner. |
| REQ-RECIPE-009 | Deleting a recipe SHALL not break historical Intake-Log entries on the client: the client snapshots the title at intake time. |

### 5.6 Offline Read-Cache

| Req | Statement |
|---|---|
| REQ-OFFLINE-001 | The Android client SHALL maintain a **read-cache** in Room of: liked recipes, recently viewed recipes/ingredients, full user profile, intake log, supplements. |
| REQ-OFFLINE-002 | Write operations (create recipe, like, etc.) SHALL require an online connection in the MVP. |
| REQ-OFFLINE-003 | The cache SHALL surface clearly when data is stale (last-sync timestamp, manual refresh). |

### 5.7 Intake-Log (Local)

| Req | Statement |
|---|---|
| REQ-INTAKE-001 | The user SHALL be able to log "what I ate today" by selecting a recipe or single ingredient with a portion size. |
| REQ-INTAKE-002 | Intake entries SHALL be stored **locally only** (Room). The server SHALL not receive intake data. |
| REQ-INTAKE-003 | Each intake entry SHALL store: timestamp, source type (`recipe | ingredient | supplement`), source id, portion size, snapshot of name + nutrition (for resilience against server-side deletion). |
| REQ-INTAKE-004 | Intake entries within the past 7 days SHALL be editable. Older entries SHALL be read-only. |

### 5.8 Supplements (Local)

| Req | Statement |
|---|---|
| REQ-SUPP-001 | The user SHALL be able to define personal supplements locally (name, default dose, nutrition contributions, micronutrients, optional image). |
| REQ-SUPP-002 | Supplements SHALL initially be stored **locally**. |
| REQ-SUPP-003 | The user SHALL be able to log a supplement intake (date, supplement, amount) the same way as a food intake. |
| REQ-SUPP-004 | The user SHALL be able to **submit** a local supplement for peer-review. Submitted entries enter the Admin queue. Once approved, they become globally available as `supplements_public`. |
| REQ-SUPP-005 | The user SHALL be able to define **Reminders** per supplement (one-shot or repeating schedule). Local AlarmManager for fire. (FCM-Push entfernt 2026-05-25.) |
| REQ-SUPP-006 | Supplements SHALL appear as their own sub-tab under `Essen` (3rd of 3), separate from Lebensmittel/Rezepte. |
| REQ-SUPP-007 | Supplements SHALL NOT be usable as recipe ingredients. |

### 5.9 Home Screen

| Req | Statement |
|---|---|
| REQ-HOME-001 | The Home tab SHALL show the current day's nutrient totals (kcal + macros + selected micronutrients) computed from the local Intake-Log. |
| REQ-HOME-002 | Nutrient targets SHALL be derived from the user's profile (BMR via Mifflin–St Jeor × activity multiplier). |
| REQ-HOME-003 | The user SHALL be able to add a quick intake entry from the Home tab. |
| REQ-HOME-004 | The Home tab SHALL include: heutige Makros (Ringe/Bars), heutige Einträge (Kurzliste max 5), Quick-Add (letzte 6 Refs), Supplement-Checkliste, Wasser-Tracker, Datum-Navigation (gestern/heute/morgen), "Verlauf"-Button → Intake-History-Screen. |
| REQ-HOME-005 | The Home tab SHALL be the entry point to the Intake-History (full chronological list with date picker). |

### 5.12 Ratings

| Req | Statement |
|---|---|
| REQ-RATING-001 | **Personal Ratings** (local only): per recipe, the user can mark `MORE_OFTEN` (↑) or `INTOLERANT` (↓). Stored in Room, never sent to server. Drives the auto-meal-planner (P4). |
| REQ-RATING-002 | **Community Ratings** (server, public): per recipe, the user can vote `RECOMMEND` (↑) or `NOT_RECOMMEND` (↓). Aggregated for ranking. |
| REQ-RATING-003 | Both ratings SHALL be independent (a user can be `INTOLERANT` + `RECOMMEND` simultaneously). |
| REQ-RATING-004 | Free-text comments on recipes are **out of scope**. |
| REQ-RATING-005 | A user's community vote MAY be changed or revoked. |

### 5.13 Onboarding (Full Wizard)

| Req | Statement |
|---|---|
| REQ-ONBOARD-001 | First-launch SHALL run a forward-only onboarding wizard collecting **all profile fields** (10+ steps): Email/Pwd/Invite → display name → age → biological sex → height → weight → activity level → diet goal → allergies → FODMAP intolerances → histamine sensitivity → preferred meal slots → max prep time → nutrient-target review. |
| REQ-ONBOARD-002 | Each step SHALL be skippable for non-essential fields; allergies + FODMAP SHALL be skippable but with a clear warning. |
| REQ-ONBOARD-003 | Re-running onboarding SHALL be reachable from Profil → "Onboarding wiederholen". |

### 5.10 Data Quality System

See §10 below.

### 5.11 Admin Web UI (Phase 1 Subset)

| Req | Statement |
|---|---|
| REQ-ADMIN-001 | A **Admin Web UI** SHALL be reachable at `/admin` with admin-role JWT only. |
| REQ-ADMIN-002 | Phase-1 admin functions: invite-code generation, ingredient curation (edit allergens/SIGHI/FODMAP/nutrition), user listing + ban, OFF resync trigger. |
| REQ-ADMIN-003 | Phase-3 & Phase-4 admin functions (full peer-review queues, moderation, statistics) are specified in §7.5. |

---

## 6. Groups, Planning, Symptoms, Reminders, Export (Phase 3 Dev — In Scope v1.0)

### 6.1 Groups

| Req | Statement |
|---|---|
| REQ-GROUP-001 | Group types: `PRIVATE` (join via invite code/link) and `PUBLIC` (discoverable + free join). |
| REQ-GROUP-002 | Group fields: id, name, type, ownerId, description, members, createdAt. |
| REQ-GROUP-003 | Users SHALL be able to create, join, and leave groups. |
| REQ-GROUP-004 | The group owner SHALL be able to remove members and transfer ownership. |
| REQ-GROUP-005 | Recipes MAY be scoped to a group (`visibility=group` + `groupId`). |
| REQ-GROUP-006 | Recipe detail SHALL show the originating group (or "Allgemein"). |
| REQ-GROUP-007 | A "Melden"-Button on recipes SHALL feed the moderation queue in the Admin Web UI. |

### 6.2 Meal-Planner (Manual)

| Req | Statement |
|---|---|
| REQ-PLAN-001 | A weekly plan view SHALL show 7 days × 4 slots (Frühstück/Mittag/Abend/Snack). |
| REQ-PLAN-002 | Recipes/ingredients SHALL be placeable into slots via picker or drag&drop. |
| REQ-PLAN-003 | Plan data SHALL be stored locally. |
| REQ-PLAN-004 | A "Habe gegessen"-Button on a slot SHALL copy that slot's content into the Intake-Log. |
| REQ-PLAN-005 | Per-slot Reminders SHALL be configurable (local AlarmManager). |

### 6.3 Shopping List

| Req | Statement |
|---|---|
| REQ-SHOP-001 | The shopping list SHALL aggregate ingredients across all planned meals in a chosen date range (unit-normalized). |
| REQ-SHOP-002 | Items SHALL be check-off-able and persist locally. |
| REQ-SHOP-003 | Optional aisle grouping (best-effort categorization from `ingredient.category`). |

### 6.4 Symptom-Log (Tagebuch)

| Req | Statement |
|---|---|
| REQ-LOG-001 | The Log-Tab SHALL be a Symptom-Tagebuch (NOT intake-history). Data stored **locally only** (Room, encrypted). |
| REQ-LOG-002 | Entry fields: timestamp, mood (Slider 1–10), Schlafqualität (1–5), Schlafdauer (h), symptoms (Multi-Select), severity per symptom (1–5), Freitext, Tags. |
| REQ-LOG-003 | Symptom list SHALL be configurable (Defaults: 15 common + user-defined additions). |
| REQ-LOG-004 | Multiple entries per day SHALL be allowed. |
| REQ-LOG-005 | Week/Month overview with line charts (mood + severity-aggregate trends). |
| REQ-LOG-006 | The 7-day window SHALL be editable; older entries read-only. |

### 6.5 Reminders + Push

| Req | Statement |
|---|---|
| REQ-REMIND-001 | Reminders MAY be configured for: meal slots, supplements, water (if enabled). |
| REQ-REMIND-002 | Reminder delivery: local AlarmManager (works offline). |
| REQ-REMIND-003 | ~~FCM Push für Gruppen-Aktivität~~ **REMOVED (2026-05-25):** Keine Firebase-Abhängigkeit. Gruppen-Notifications werden In-App beim nächsten App-Start angezeigt (Badge im Tab) bzw. optional per Email (server-seitig, Phase 3). |
| REQ-REMIND-004 | Notification permission asked at first reminder creation. |

### 6.6 Wasser-Tracker

| Req | Statement |
|---|---|
| REQ-WATER-001 | Home + Log-history SHALL include a Wasser-Tracker. |
| REQ-WATER-002 | Quick-Add buttons (250 ml / 500 ml / custom). |
| REQ-WATER-003 | Daily goal configurable in Profil (default 2000 ml). |
| REQ-WATER-004 | Entries stored locally; deletable. |

### 6.7 Export (DSGVO)

| Req | Statement |
|---|---|
| REQ-EXPORT-001 | The user SHALL be able to export **all** their data (profile, intake, supplements, symptoms, ratings, owned recipes) as **PDF + JSON**. |
| REQ-EXPORT-002 | Export trigger: Profil → "Daten exportieren". |
| REQ-EXPORT-003 | Export SHALL include both local data and server-side data (account, owned recipes, ratings). |
| REQ-EXPORT-004 | PDF SHALL be human-readable; JSON SHALL be machine-parseable. |

---

## 7. Power Features (Phase 4 Dev — In Scope v1.0)

### 7.1 User-Submitted Ingredients + Field-Update-PR

| Req | Statement |
|---|---|
| REQ-INGR-USER-001 | Users SHALL be able to submit new ingredients via a form. Status = `PENDING` until admin review. |
| REQ-INGR-USER-002 | Pending ingredients SHALL be usable by the submitter for recipe drafts but NOT visible to other users until approved. |
| REQ-FIELDPR-001 | Users SHALL be able to propose a change to a single field of an existing ingredient (allergen flag, histamine score, FODMAP level, nutrient value). |
| REQ-FIELDPR-002 | A pending proposal SHALL NOT change the displayed value until approved. |
| REQ-FIELDPR-003 | Peer-review workflow: ≥1 admin approval required; rejection reason stored. |

### 7.2 Auto-Meal-Planner

| Req | Statement |
|---|---|
| REQ-AUTOPLAN-001 | The Plan-Tab SHALL have a "Plan generieren"-Button. |
| REQ-AUTOPLAN-002 | The algorithm SHALL be **beam-search** (adapted from Histamind), running locally against cached server recipes. |
| REQ-AUTOPLAN-003 | Inputs: profile (allergies, intolerances, nutrient goals), personal ratings (MORE_OFTEN / INTOLERANT), preferred slots, max prep time. |
| REQ-AUTOPLAN-004 | The generated plan SHALL be editable before commit (the user can swap individual slots). |

### 7.3 Bayesian Insights

| Req | Statement |
|---|---|
| REQ-INSIGHT-001 | After ≥14 days of combined intake + symptom data, an Insights view SHALL surface lift-based correlations (ingredient/recipe/supplement → symptom). |
| REQ-INSIGHT-002 | Threshold: lift > 1.5 AND ≥3 co-occurrences. |
| REQ-INSIGHT-003 | Severity-weighted (higher symptom severity → higher weight). |
| REQ-INSIGHT-004 | Computation 100% local; never leaves the device. |

### 7.4 ~~Barcode Scanner~~ — **REMOVED**

User decision (2026-05-25): Barcode-Scanning ist nicht Bestandteil des Projekts. REQ-BARCODE-001..003 entfernt. Begründung: Scope-Reduction + keine Google ML-Kit Dependency.

### 7.5 Full Admin UI

| Req | Statement |
|---|---|
| REQ-ADMIN-FULL-001 | Beyond the minimal Phase-1 admin tools, the full Admin Web UI SHALL include: ingredient peer-review queue, field-PR queue, supplement peer-review queue, user moderation (ban/unban/delete), invite-code management, report queue (gemeldete Rezepte/User), statistics dashboard (User-Count, DB-Größe, Top-Rezepte, Phase-Completion-Status). |
| REQ-ADMIN-FULL-002 | Admin role SHALL be assignable to existing users (DB flag); never via self-service.

---

## 8. Technology Stack (LOCKED)

### 8.1 Client

| Component | Choice |
|---|---|
| Language | **Kotlin** |
| UI | **Jetpack Compose** |
| Local DB | **Room** (with SQLCipher encryption for health data) |
| Preferences | **DataStore** |
| Network | **Retrofit + OkHttp** with Kotlinx Serialization |
| API Client | **Auto-generated from OpenAPI spec** |
| Secure Storage | **EncryptedSharedPreferences** for JWT |
| DI | Hilt |
| Min/Target SDK | minSdk 26, targetSdk 35 |

### 8.2 Server

| Component | Choice |
|---|---|
| Language | **Kotlin** |
| Framework | **Spring Boot 3** |
| DB | **PostgreSQL 16** |
| Migrations | **Flyway** |
| Object Storage | **MinIO** (S3-compatible) |
| API Style | **REST** with **OpenAPI 3.1** spec (springdoc-openapi) |
| Auth | **Spring Security** + **JWT** (Access 15 min + Refresh 30 d) |
| Rate Limit | **Bucket4j** |
| Logging | **Structured JSON** (Logstash encoder) |
| Metrics | **Micrometer** → **Prometheus** endpoint |
| Search | Postgres full-text search (`tsvector` with German config) |

### 8.3 Deployment

| Component | Choice |
|---|---|
| Host | Netcup VPS |
| Orchestration | **docker-compose** (no Kubernetes for MVP) |
| Reverse Proxy + TLS | **Caddy** (auto Let's Encrypt) |
| CI/CD | **GitHub Actions** (build + test + auto-deploy to VPS via SSH) |
| Backups | Postgres `pg_dump` daily to MinIO bucket, MinIO data snapshot weekly |

---

## 9. High-Level Data Model

### 9.1 Server (PostgreSQL)

| Table | Purpose |
|---|---|
| `users` | id, email, passwordHash, displayName, role, accountState, emailVerifiedAt, createdAt |
| `invite_codes` | code, issuedBy (admin), claimedBy (user, nullable), expiresAt |
| `ingredients` | id, nameDe, synonymsDe[], category, defaultUnit, nutritionPer100g, allergens, allergensKnown, histamineSighi (nullable), fodmap (nullable jsonb), dataQuality, source, imageRef, version |
| `ingredient_curation_audit` | who/when/what changed on `ingredients` |
| `recipes` | id, ownerId, titleDe, prepMinutes, totalMinutes, slotTags[], steps[], imageRef, visibility, groupId (nullable), createdAt, updatedAt |
| `recipe_ingredients` | recipeId, ingredientId, amount, unit, optional, note |
| `recipe_likes` | userId, recipeId, likedAt |
| `groups` (M2) | id, name, visibility, inviteCode (nullable), ownerId, createdAt |
| `group_members` (M2) | groupId, userId, role, joinedAt |
| `recipe_reports` (M2) | id, recipeId, reportedBy, reason, status |
| `field_proposals` (M3) | id, ingredientId, fieldName, proposedValue, submittedBy, status, reviewedBy, reviewedAt |
| `submitted_ingredients` (M3) | similar to `ingredients` with `status=PENDING/APPROVED/REJECTED` |

### 9.2 Client (Room, encrypted)

| Table | Purpose |
|---|---|
| `local_profile` | single row — age, sex, height, weight, activityLevel, allergies, fodmapToggles, mealTemplate, ... |
| `nutrient_target_overrides` | per-nutrient override values |
| `intake_log` | (id, occurredAt, sourceType, sourceId, snapshotTitle, snapshotNutrition jsonb, portionAmount, portionUnit) |
| `supplements` | (id, nameDe, defaultAmount, defaultUnit, nutritionPer100g/dose, notes) |
| `supplement_intake` | logged via `intake_log` with `sourceType=supplement` |
| `recipe_cache` | server-fetched recipes mirrored locally for offline read |
| `ingredient_cache` | server-fetched ingredients mirrored locally |
| `symptom_entries` (M2) | (id, occurredAt, symptoms[], severity, optionalNote) |
| `water_log` (M2) | (id, timestamp, amountMl) |
| `reminder_config` (M2) | per-slot timings, per-supplement schedules |
| `app_settings` | k/v |

Full DDL + indices live in `Architecture.md`.

---

## 10. Data Quality System

A foundational MVP feature that makes incomplete data explicit rather than guessed.

### 10.1 Quality States

| Req | Statement |
|---|---|
| REQ-QUALITY-001 | Every ingredient SHALL carry a `dataQuality` enum: `COMPLETE | PARTIAL | MINIMAL`. |
| REQ-QUALITY-002 | `Ingredient.allergensKnown: boolean` SHALL explicitly mark whether allergen data is verified (`true`) or unknown (`false`). Default for OFF imports without explicit `allergens_tags` = `false`. |
| REQ-QUALITY-003 | `Ingredient.histamineSighi` SHALL be nullable. `null` = unknown (NOT "0"). |
| REQ-QUALITY-004 | `Ingredient.fodmap` SHALL be nullable as a whole jsonb. `null` = unknown (NOT "all-low default"). |
| REQ-QUALITY-005 | Each ingredient SHALL track its `source`: `BLS | OFF | ADMIN_CURATED | SIGHI_LIST | USER` (M3). |

### 10.2 UI Surfacing

| Req | Statement |
|---|---|
| REQ-QUALITY-UI-001 | Ingredient detail SHALL render a quality badge: ✓ Vollständig / ⚠ Allergene unvollständig / ℹ Histamin-Bewertung fehlt / ℹ FODMAP-Bewertung fehlt. |
| REQ-QUALITY-UI-002 | Recipe detail SHALL roll up the worst quality across its ingredients and surface a banner if any ingredient has incomplete allergen data. |

### 10.3 Filter Behavior

| Req | Statement |
|---|---|
| REQ-QUALITY-FILTER-001 | When a user has declared an allergy and applies the corresponding filter, ingredients (and recipes containing them) with `allergensKnown=false` SHALL be **excluded** by default (precautionary principle). |
| REQ-QUALITY-FILTER-002 | The user MAY toggle "auch unsichere zeigen" on a per-search basis to relax REQ-QUALITY-FILTER-001 — but a yellow warning chip SHALL be shown on each unsure result. |

### 10.4 Correction Path

| Req | Statement |
|---|---|
| REQ-QUALITY-FIX-001 | In MVP, corrections to ingredient quality data SHALL be performed only by the Admin via the Admin Web UI. |
| REQ-QUALITY-FIX-002 | In M3, users SHALL be able to propose corrections (Field-Update-PR). Pending proposals SHALL NOT affect the displayed value. |

---

## 11. Out of Scope (v1.0)

### Permanently Dropped
- iOS, Web, Desktop end-user clients (only Android client + Admin Web UI).
- Multi-profile / family / caregiver mode.
- Pregnancy / lactation tracking.
- Cloud sync of health data (symptoms, intake, allergies stay local-only by design).
- Medical-grade diagnosis or therapy claims.
- Monash FODMAP data ingestion (copyrighted).
- URL recipe scraping / import (replaced by user-authored recipes).
- Multi-language UI (de_DE only).
- Reintroduction phases for HIT.
- Free-text comments on recipes.
- Wearable / smart-watch integration.
- Photo-based food recognition (AI/ML image classification).
- Payment / subscription system.

---

## 12. Glossary

| Term | Definition |
|---|---|
| **BLS** | Bundeslebensmittelschlüssel — German federal food code; curated nutrient database (~1.2k items). |
| **OFF** | Open Food Facts — open product database with global coverage, including branded products. Licensed CC-BY-SA. |
| **SIGHI** | Schweizerische Interessengemeinschaft Histamin-Intoleranz — publishes a free PDF list rating ~400 foods on a 0–3 histamine compatibility scale. |
| **FODMAP** | Fermentable Oligo-, Di-, Mono-saccharides And Polyols — a class of carbs implicated in IBS symptoms. Five sub-types tracked: fructose, lactose, fructans, GOS, polyols. |
| **EU-14** | The 14 major allergens declared under EU regulation 1169/2011. |
| **MinIO** | Self-hosted S3-compatible object storage. |
| **Intake-Log** | Local record of what the user ate/drank/supplemented. Drives Home nutrient totals and (in M3) Bayesian Lift inputs. |
| **Field-Update-PR** | User-submitted proposal to change a specific field (e.g. `histamineSighi`) on an existing ingredient; admin reviews. M3 feature. |

---

*End of ReqSpec.md v0.2 — LOCKED for unified v1.0 scope. UI/API/DB details in companion docs.*

---

## §11 P6 Re-Spec — Histamind-Fusion + UX-Findings (eingefügt 2026-05-26, LOCKED via P6.S1)

**Trigger:** 10 Findings F-003..F-012 aus BattleTestPlan §6, plus User-Direktive „Histamind als Design-Referenz" (https://github.com/endgeardev/Histamind).

**Scope-Disziplin:** Dieser §11 lockt nur die Re-Spec der durch Findings betroffenen REQ-IDs. Keine neuen Domain-Features. Alle visuellen Decisions referenzieren [HistamindDesignReference.md](HistamindDesignReference.md).

### REQ-DESIGN-001 — Visual-Identity-Lock (P6.S2)
LOCKED auf Hm-Token-System (siehe HistamindDesignReference.md §2). Olive-Green wird komplett ersetzt. Light + Dark + System bleibt; Light = Clean-Variante ohne Glas. **Supersedes:** alle vorherigen Color/Typography-LOCKs in GUI.md §§2–3.

### REQ-TYPO-001 — Manrope (P6.S2)
App-weite Schrift: Manrope via Google Fonts (OFL). 12 Text-Styles in HistamindDesignReference.md §3 gelockt. Tabular-Figures für numerische Werte verpflichtend.

### REQ-COMP-001..008 — Compose-Component-Library (P6.S3)
- COMP-001 GlassCard
- COMP-002 SectionPill
- COMP-003 GradientFab
- COMP-004 GradientButton
- COMP-005 AmbientBackdrop
- COMP-006 GradientText
- COMP-007 SegmentedTabs
- COMP-008 SeverityBar

Implementationsspec: HistamindDesignReference.md §5.

### REQ-HOME-PIN-001 — Pinned Nutrients (P6.S4, finding F-004)
User pinnt N Nährstoffe (default 4: kcal/Protein/Carbs/Fat). Pinned werden mit Progress-Ring auf Home gezeigt; alle weiteren collapsed mit Mini-Linear-Progress. Pin-Mgmt-Sheet via Home-Header-Icon oder Profil. Server-Persist: `users.pinned_nutrients TEXT[]` (V12).

### REQ-ONBOARD-SLIDER-001 — Slider statt Zahleninputs (P6.S4, finding F-003)
Alle numerischen Onboarding-Eingaben als Material-3-Slider mit Live-Value-Label:
- Alter: 14–100, step 1
- Größe: 140–220 cm, step 1
- Gewicht: 30–200 kg, step 0.5
- Aktivitäts-Index: 1.2–1.9, step 0.05

14-Step-Indikator als Punkte (active = gradient-filled, inactive = glassBorder). Forward-only ohne Skip.

### REQ-WATER-REMOVE-001 — Wasser-Entfernen (P6.S7, finding F-005)
Long-Press auf letzten Wasser-Quick-Add-Chip in Home → Undo-Snackbar 5 Sek; nach Timeout permanent gelöscht.

### REQ-WATER-ALARM-HELPER-001 — Wasser-Alarm-UX (P6.S7, finding F-006)
Helper-Text unter Wasser-Alarm-Toggle: „Erinnerung alle 2 h zwischen 08:00–22:00". Toggle-State zeigt aktuelles Verhalten ohne Erklärung-Modal.

### REQ-INTAKE-ADD-FLOW-001 — Pre-Selection-Mode (P6.S5/S7, finding F-007)
„Hinzufügen"-Buttons in Home + Plan navigieren zu `LebensmittelScreen` mit nav-arg `preselect=true`. In diesem Modus wird der FAB zu „Auswählen" und Item-Tap liefert ein `Result` an den aufrufenden Screen. Kein separates Add-Sheet mehr.

### REQ-WORDING-LOCK-001 — Glossary-Wording (P6.S5, finding F-008)
Plan-Add-Sheet-Titel: „Rezept oder Lebensmittel" (nicht „Zutat"). Glossary in Architecture.md Anhang G lockt:
- **Zutat** = Bestandteil eines Rezepts (nur intern in Rezept-Definition).
- **Lebensmittel** = Standalone-Eintrag in Datenbank (`ingredients`-Tabelle).

### REQ-LIST-PRELOAD-001 — Listen-Vorbefüllung (P6.S5, finding F-009)
`IngredientScreen` + `RecipeScreen` laden bei Open Paginated-Page (50 Items alphabetisch) ohne Search-Eingabe. Search filtert clientseitig sofort + serverseitig bei >50 Treffern.

### REQ-LOG-EVENT-001..006 — Event-Log-Inversion (P6.S6, finding F-010)
**Supersedes:** vorherige REQ-LOG-001..006 (Tagebuch-Modell mit Mood/Sleep).
- REQ-LOG-EVENT-001: Log ist Event-Liste, nicht Tagebuch. Mood + Schlaf entfernt.
- REQ-LOG-EVENT-002: Jeder Event hat: `severity` (1–5), `symptom_tags` (Multi-Tag), `note` (optional Text), `occurred_at` (Timestamp).
- REQ-LOG-EVENT-003: QuickEntrySheet erfasst Event in <10 Sek (Severity-Picker + Tag-Chips + Notiz + jetzt-Default).
- REQ-LOG-EVENT-004: Severity-Mapping zu Farben in HistamindDesignReference.md §5.8.
- REQ-LOG-EVENT-005: SegmentedTabs „Einträge" / „Insights".
- REQ-LOG-EVENT-006: Insights = 14-Tage-Histogramm + Top-3-Symptome.

DB-Migration: V13 (HistamindDesignReference.md §7).

### REQ-PROFILE-GOALS-001 — Per-Nutrient-Tagesziele (P6.S5, finding F-011)
Profil-Sektion „Tagesziele" mit per-Nutrient-Editor (kcal + Protein/Carbs/Fat + erweiterbare Mikronährstoff-Slots). Werte persistiert in `users.daily_nutrient_goals JSONB` (V12). Home-Pinned-Nutrient-Progress nutzt diese Werte, nicht mehr Default-Berechnung.

### REQ-RECIPE-CREATE-WIZARD-001 — Geführter Rezept-Wizard (P6.S5, User-Input 2026-05-26)
Rezept-Creation als 5-Step-Wizard, forward-only, mit Validation pro Step:
- Step 1: Name (`OutlinedTextField`) + optional Foto (PhotoPicker).
- Step 2: Zutaten-Liste — Search aus `ingredients` (mit REQ-LIST-PRELOAD-001 Behavior) → Add-Button fügt Zutat zur Rezept-Definition mit Mengen-Slider + Einheit-Dropdown.
- Step 3: Portionen (Slider 1–20) + Zubereitungszeit (Slider 0–240 min, step 5).
- Step 4: Zubereitungstext (multiline, optional, mit Helper „Schritt für Schritt empfohlen").
- Step 5: Vorschau-Card (GlassCard mit allen Daten) + „Speichern" GradientButton.

Visuell: AmbientBackdrop, Step-Punkte oben (5 Punkte), gleiches Wizard-Pattern wie Onboarding. Einstieg: aus EssenScreen / PlanScreen / Profil über FAB-Variante.

### REQ-INGREDIENT-CREATE-WIZARD-001 — Geführter Lebensmittel-Suggest-Wizard (P6.S5, User-Input 2026-05-26)
Lebensmittel-Suggest-Flow (ehemals Modal-Dialog `IngredientSuggestDialog`) wird zu 4-Step-Wizard:
- Step 1: Name + Marke + Barcode (optional, mit Scan-Button).
- Step 2: Nährwerte pro 100g (kcal/Protein/Carbs/Fat als Slider, Sub-Nutrients als optional Expansion).
- Step 3: Allergene + Histamin-SIGHI + Diäten (Chip-Multi-Select).
- Step 4: Vorschau + Submit (Status PENDING; landet auf Admin-Queue).

Visuell wie Rezept-Wizard. Server-Endpoint bleibt `POST /api/ingredients/suggest`.

### Traceability

| REQ-ID | Finding | Sprint | Implementation-Anker (geplant) |
|---|---|---|---|
| REQ-DESIGN-001 | F-012 | P6.S2 | `presentation/theme/Color.kt`, `Theme.kt`, `Typography.kt` |
| REQ-TYPO-001 | F-012 | P6.S2 | `presentation/theme/Typography.kt`, `res/font/manrope.xml` |
| REQ-COMP-001..008 | F-012 | P6.S3 | `presentation/common/components/*.kt` |
| REQ-HOME-PIN-001 | F-004 | P6.S4 | `presentation/home/HomeScreen.kt`, `PinnedNutrientsManager.kt`, `V12__` |
| REQ-ONBOARD-SLIDER-001 | F-003 | P6.S4 | `presentation/onboarding/OnboardingScreen.kt` |
| REQ-WATER-REMOVE-001 | F-005 | P6.S7 | `presentation/home/WaterTracker.kt` |
| REQ-WATER-ALARM-HELPER-001 | F-006 | P6.S7 | `presentation/home/WaterAlarmCard.kt` |
| REQ-INTAKE-ADD-FLOW-001 | F-007 | P6.S5/S7 | `presentation/lebensmittel/LebensmittelScreen.kt` nav-arg |
| REQ-WORDING-LOCK-001 | F-008 | P6.S5 | `presentation/plan/PlanAddSheet.kt`, strings.xml |
| REQ-LIST-PRELOAD-001 | F-009 | P6.S5 | `IngredientScreen.kt`, `RecipeScreen.kt`, Repos |
| REQ-LOG-EVENT-001..006 | F-010 | P6.S6 | `presentation/log/LogScreen.kt`, `LogRepository.kt`, `V13__` |
| REQ-PROFILE-GOALS-001 | F-011 | P6.S5 | `presentation/profile/ProfileScreen.kt`, `V12__` |

**End of §11 P6 Re-Spec.**

---

## §12 P7 Big-Nutrition-Refactor (User-Input 2026-05-27)

**Scope-Änderung**: Walk-through Home-Screen mit User ergab grundlegenden Refactor des Nährstoff-Modells. Supersedes/erweitert: REQ-HOME-001..005, REQ-HOME-PIN-001, REQ-PROFILE-GOALS-001, REQ-WATER-001..004, REQ-WATER-REMOVE-001, REQ-WATER-ALARM-HELPER-001, REQ-REMIND-001 (Wasser-Teil), REQ-INGR-004 (OFF-Filter), REQ-INGR-002 (BLS).

### REQ-NUTRIENT-CATALOG-001 — Vollständiger Nährstoff-Katalog
Die App SHALL einen festen Katalog von ~30 Nährstoffen unterstützen (DGE-Vollset, deutsch):

**Makros** (g pro 100g): `kcal`, `protein`, `carbs`, `sugar`, `fat`, `satfat`, `fiber`, `salt`.

**Vitamine** (mg oder µg pro 100g, Einheit pro Nährstoff fix definiert):
`vitamin_a`, `vitamin_d`, `vitamin_e`, `vitamin_k`, `vitamin_b1`, `vitamin_b2`, `vitamin_b3`, `vitamin_b5`, `vitamin_b6`, `vitamin_b7`, `vitamin_b9`, `vitamin_b12`, `vitamin_c`.

**Mineralstoffe** (mg oder µg pro 100g): `calcium`, `eisen`, `magnesium`, `zink`, `kupfer`, `mangan`, `selen`, `jod`, `kalium`, `natrium`, `phosphor`.

**Pseudo-Nährstoff Wasser** (`water`): ml pro Tag, **nicht** aus Mahlzeiten aggregiert (separate `water_intake`-Tabelle).

Katalog-Definition zentral in `domain/nutrition/NutrientCatalog.kt` (key, anzeigeName_de, einheit, default_target_per_day, tdee_factor?). Maschinen-readable für UI + ETL.

### REQ-DATA-SOURCE-001 — USDA-FDC als alleinige Lebensmittel-Quelle
**Supersedes**: REQ-INGR-002 (BLS), REQ-INGR-004 (OFF-Filter).
Lebensmittel-Stammdaten kommen ausschließlich aus **USDA FoodData Central** (`https://api.nal.usda.gov/fdc/v1/`, kostenloser API-Key). OFF + BLS aus Pipeline entfernt (Importer-Skelette werden deprecated, nicht aktiv gepflegt).

**Korpus-Definition (P7.S2 Slice 1, 2026-05-27):** Kuratierter FDC-Korpus von **8.487 Einträgen** = Foundation (394) + SR-Legacy (7.793) + Branded Top-300 (default-sort, marketCountry-frei).

**Pipeline-Schritte:**
1. **Build-Time-Tool** `de.healthforge.tools.FetchFdcTopIds` (Gradle-Task `:fetchFdcTopIds`) ruft FDC `POST /v1/foods/search` ab und schreibt die kuratierte ID-Liste als gecommittetes Asset `server/src/main/resources/seed/fdc_top_ids.csv` (4 Spalten: `fdc_id;data_type;name_en;brand`). Idempotent (überschreibt CSV); ENV-Var `FDC_API_KEY` aus `server/.env` (gitignored).
2. **Build-Time-Tool** `build_usda_seed` (P7.S2 Slice 2, TODO) liest `fdc_top_ids.csv`, ruft `POST /v1/foods/list` mit ID-Batches und erzeugt den finalen 14-Spalten-Seed `usda_fdc.csv` mit Mikronährstoffen.
3. **Runtime-Importer** `UsdaFdcImporter` (existiert) liest `usda_fdc.csv` und upsertet in `ingredients` (Idempotenz via `fdc_id`).

Re-Sync: monatlich via Admin-Endpoint (Tool 1 + 2 + Importer-Run nacheinander).

### REQ-DATA-TRANSLATE-001 — Deutsche Übersetzung via DeepL-Batch
Lebensmittelnamen aus FDC sind englisch. Die App SHALL deutsche Übersetzungen persistieren. Strategie: **DeepL-Free-Tier-API** (500.000 Zeichen/Monat gratis) in einem Batch-Übersetzungs-Skript (`server/scripts/translate_fdc_names.kts`), Output als CSV zur Review durch Admin, dann Bulk-Insert in `ingredients.name_de`.

Nährstoff-Namen werden **nicht** übersetzt — Katalog ist fix deutsch hardcoded (REQ-NUTRIENT-CATALOG-001).

### REQ-INGR-MICRONUTRIENTS-001 — Mikronährstoff-Speicherung (Server)
Server-Flyway V12 erweitert `ingredients` (PostgreSQL):
- `micronutrients_json JSONB DEFAULT '{}'` — Schlüssel = Katalog-Key (REQ-NUTRIENT-CATALOG-001), Wert = numerisch pro 100g.
- `fdc_id BIGINT UNIQUE NULL` — USDA-FDC-ID für Re-Sync-Identifikation.

Mikronährstoffe von Rezepten SHALL live aus `recipe_ingredients × ingredients.micronutrients_json` aggregiert werden (analog REQ-RECIPE-007 für Makros).

**Client-Aufnahme (P7.S5 4f, 2026-05-29):** `data/network/IngredientDto` führt `fdc_id: Long?` + `micronutrients: Map<String, Double>` als optionale Felder (Default `null` / `emptyMap`). UI-Sichtbarmachung im `IngredientDetailSheet` (siehe REQ-INGR-DETAIL-SHEET-001).

### REQ-INGR-ALLERGEN-MAPPING-001 — Allergen-Mapping aus FDC
USDA-FDC liefert `ingredients`-Volltext-String + `labelNutrients`. ETL SHALL daraus die EU-14er-Allergen-Liste per Keyword-Match befüllen (`allergens_json`):

| EU-Allergen | Match-Keywords (case-insensitive) |
|---|---|
| GLUTEN | wheat, barley, rye, spelt, oats (sofern nicht „gluten-free") |
| MILCH | milk, lactose, whey, casein, butter, cheese, cream |
| EI | egg, albumin |
| NUSS | almond, hazelnut, walnut, cashew, pistachio, macadamia, pecan, brazil nut |
| ERDNUSS | peanut |
| SOJA | soy, soya, soybean |
| FISCH | fish, anchovy, tuna, salmon |
| KRUSTENTIER | shrimp, prawn, crab, lobster |
| WEICHTIERE | mollusk, oyster, mussel, squid, octopus |
| SELLERIE | celery |
| SENF | mustard |
| SESAM | sesame |
| SULFITE | sulfite, sulphite, E220-E228 |
| LUPINE | lupin, lupine |

False-Positives (z.B. „coconut" matched „nut") werden via Negativ-Liste (`coconut`, `nutmeg`) ausgeschlossen.

### REQ-HOME-NUTRIENT-LIST-001 — Home-Layout (Pinned + Collapsed)
**Supersedes**: REQ-HOME-001..005, REQ-HOME-PIN-001.

Home-Tab MUSS folgendes Layout zeigen (top-down):
1. **Header**: Datum-Navigation (gestern/heute/morgen).
2. **Pinned-Nutrients-Sektion**: Eine Karte pro gepinntem Nährstoff (default `kcal,protein,carbs,fat,water`), mit **Stufen-Bar** (P7.S3.b: identische Mechanik wie Wasser-Slider), Wert/Ziel und Lv-Badge ab Stufe ≥ 1. Wasser-Karte siehe REQ-HOME-WATER-BAR-001.
3. **„Alle Nährstoffe anzeigen"** Expand-Button → Section blendet kompakte Stufen-Bars für **alle** Katalog-Nährstoffe (REQ-NUTRIENT-CATALOG-001) ein. Pro Zeile: Pin-Icon (Toggle in `users.pinned_nutrients`), Name, Wert/Ziel, Mini-Bar.
4. **Geplante Mahlzeiten heute**: Liste `meal_plan_items` für `today`, jede mit Checkbox „gegessen". Check → Eintrag in `intake_entries` mit Snapshot der Nährwerte. Uncheck innerhalb 60s reversibel (Undo-Snackbar).

**Stufen-Mechanik für alle Pinned-Bars (P7.S3.b, User-Direktive "ALLE bars identisch, Wasser hat nur Zusatzregeln")**:
- `stage = floor(current / goal)`, `frac = (current − stage × goal) / goal` (0..1).
- Bar-Füllung: Gradient aus `waterStageGradient(stage)` (10-Stufen-Cycle, Stufen ≥ 9 endless).
- Track: `waterStageTrackColor(stage)` = Akzent der **Vorgängerstufe** × 0.25 Alpha. Stufe 0 → `LocalHmTokens.barTrack`. (User-Direktive: "der hintergrund der progressbars soll immer die farbe der vorgängerstufe aber abgegraud oder verdunkelt sein".)
- Ab Stufe ≥ 1: Lv-Badge (Pill mit Akzent-Farbe) rechts neben Wert/Ziel.
- Prozent-Anzeige = Prozent **innerhalb der aktuellen Stufe** (0–100 %).
- Wasser ist optisch identisch + erbt die Stufen-Mechanik; Zusatzregeln (Slider, Bell, Ghost, Defizit-Rot, Touch-Disconnect) siehe REQ-HOME-WATER-BAR-001.

Gelöscht in P7.S3.b: `presentation/home/components/MacroRing.kt`, `MacroBarColumn.kt`, sowie `LeveledPowerBar`/`stageColor`/`StageBadge` in `presentation/theme/NeoComponents.kt` (alle ungenutzte Vorgänger-Komponenten).

Pin-Verwaltung erfolgt **ausschließlich** im Home-Screen. Profil-Sektion „Pinned-Nutrients" wird entfernt (siehe REQ-PROFILE-LAYOUT-001).

**Pin-Management UI (P7.S4 Slice 4e, Revision 2026-05-28)**:
- `PinnedNutrientCard` hat **zwei** Modi, gesteuert durch genau **einen** Chevron-IconButton im Header (Stift-Edit-Modus und separates Picker-Sheet wurden in der Revision entfernt).
- **Collapsed (default, `expanded = false`)**: Header-Titel "Angepinnt"; Card zeigt nur die gepinnten Nährstoffe als Progress-Rows + Wasser-Slider als `trailingSlot`. Steady-State der Home-Ansicht.
- **Expanded (`expanded = true`)**: Header-Titel "Nährstoffe verwalten"; Card zeigt **alle** im `NutrientCatalog` definierten Nährstoffe, gruppiert in vier Kategorie-Sections (Makros / Vitamine / Mineralien / Sonstiges = Wasser). Pro Eintrag eine kompakte Toggle-Row mit Name + DGE-Default + trailing `IconButton(PushPin)`. **Filled** = aktuell gepinnt, **Outlined** = nicht gepinnt. Tap auf das Icon ruft `HomeViewModel.togglePin(key)` → persistiert sofort in `UserProfileEntity.pinnedNutrientsJson`.
- **Min-1-Pin-Invariant**: der letzte verbleibende Pin ist nicht entpinnbar (`togglePin` returnt no-op).
- **Wasser**: ist Teil von `NutrientCatalog.defaultPinnedKeys`, erscheint im Expanded-View in Sektion "Sonstiges" und ist normal entpinnbar wie jeder andere Nährstoff (keine UI-Sonderbehandlung mehr seit Revision 2026-05-28). Solange Wasser gepinnt ist, erscheint der `WaterStageSlider` als `trailingSlot` im Collapsed-View.
- Expand-Status ist **in-memory only** (`HomeState.pinsExpanded` — Session-State); nur die Pin-Liste selbst ist persistent.

### REQ-HOME-WATER-BAR-001 — Wasser als Stufen-Slider in der Pinned-Nutrient-Card
Wasser wird als **letzte Zeile innerhalb der `PinnedNutrientCard`** dargestellt — optisch identisch zu den anderen gepinnten Nährstoffen (Label, Wert/Ziel, Prozent, gefüllte Bar), aber die Bar IST gleichzeitig ein Slider:

- **Range pro Anzeige**: `0..goal` (0–100 % des Tagesziels). Slider-Schritt: 50 ml.
- **Stufen-Logik**: Eine *Stufe* entspricht `1×goal`. Aktuelle Stufe = `currentMl / goalMl` (Integer-Div). Stufe 0 deckt `0..goal` ab, Stufe N deckt `N×goal..(N+1)×goal` ab. Stufen sind **endlos**.
- **Stufenwechsel oben (v2.1)**: Sobald der Slider in der aktuellen Stufe das obere Ende (100 %) erreicht, schaltet die Bar **bereits während des Drags** in die nächste Stufe (Bar zeigt 0 %, neue Farbe, Thumb springt an den linken Rand). Beim Loslassen genau an einer Stufengrenze rückt der lokale Anzeige-State zusätzlich um eine Stufe vor, sodass der nächste Drag direkt in der neuen Stufe beginnt.
- **Per-Drag-Stufen-Lock (v2.2)**: Pro Drag-Session ist nur **ein** Stufenwechsel erlaubt. Verharrt der Finger am Slider-Anschlag, kaskadiert es nicht durch mehrere Stufen — der User muss kurz loslassen und neu drag, um die nächste Stufe zu betreten/verlassen. **Ersetzt durch v2.3**.
- **Touch-Disconnect bei Stufenwechsel (v2.3, User-Direktive)**: Sobald während eines Drags ein Stage-Up oder Stage-Down ausgelöst wird, **bricht die App die aktive Geste ab** (Slider wird per Compose-`key`-Remount neu zusammengebaut). Der User MUSS den Finger heben und neu auf den Slider tippen, um eine weitere Stufe zu wechseln. So sind Cascade-Effekte konstruktionsbedingt unmöglich.
- **Downgrade-Regel (Drag-Through-Zero, v2.1)**: Erreicht der Slider in einer Stufe > 0 das untere Ende (0 %) während des Drags, schaltet die Bar **sofort eine Stufe zurück** (Bar zeigt 100 %, Thumb springt an den rechten Rand). Per-Drag-Lock (v2.2) gilt analog. Beim Loslassen genau an einer Stufenuntergrenze (>0-Stufe) rückt der State ebenfalls eine Stufe zurück.
- **Farben Stufe 0..9**: pro Stufe eigenes Gradient-Paar aus der Histamind-Palette (siehe [`WaterStageColors.kt`]). Ab **Stufe 10+** bleibt die Farbe identisch zu Stufe 9.
- **Track-Farbe (P7.S3.b)**: Akzent der Vorgängerstufe × 0.25 Alpha (`waterStageTrackColor`). Stufe 0 → neutraler `LocalHmTokens.barTrack`. Identisch zu allen anderen Pinned-Bars.
- **Stufen-Badge**: Ab Stufe ≥1 erscheint im Header neben "Wasser" ein kleines `×N`-Badge in der Akzent-Farbe der Stufe.
- **Reminder-Bell**: Trailing-Icon der Wasser-Zeile (statt eigener Card-Header). Togglet die Defizit-Erinnerung.
- **Persistenz**: Beim Loslassen wird die absolute Tagesmenge via `WaterIntakeRepository.setDayTotal(day, totalMl)` als **Day-Aggregate** persistiert (alle bisherigen `water_intake`-Rows des Tages werden in einer Room-Transaktion gelöscht und durch genau einen Aggregat-Eintrag mit `totalMl` ersetzt; bei `totalMl == 0` bleibt der Tag eintragslos).
- **Kein** separates Wasser-Card-Block, **keine** ±-Buttons, **keine** Quick-Add-Pills, **kein** Custom-Dialog, **kein** Undo-Snackbar.
- **Ghost-Soll-Marker (v2.1, reaktiviert)**: Auf der Bar wird eine feine **weiße vertikale Linie** (Alpha 0.85, Strichbreite 2 px) an der Position des linearen Tages-Solls bis jetzt (`HomeState.waterGhostMl`) gezeichnet — **sofern das Soll im sichtbaren Stufen-Bereich liegt** (`displayedStage*goal..(displayedStage+1)*goal`). Liegt das Soll außerhalb (z.B. User ist mehrere Stufen voraus oder zurück), wird der Marker in dieser Stufe nicht gerendert. Die Defizit-Alarm-Logik (REQ-HOME-WATER-ALARM-001) berechnet das Soll weiterhin intern und triggert die Bell-Eskalation.
- **Defizit-Rotanteil (v2.2)**: Liegt das Soll **in der aktuell sichtbaren Stufe** UND ist current < Soll, wird der Bereich zwischen aktueller Füllung und Soll in `StatusOverUl` (Alpha 0.55) gerendert — als visuelles "du liegst zurück". Ist current ≥ Soll oder Soll außerhalb der Stufe, wird kein Rotanteil gezeichnet.

### REQ-HOME-WATER-ALARM-001 — Defizit-Eskalation mit Debounce
**Supersedes**: REQ-REMIND-001 (Wasser-Teil), REQ-WATER-ALARM-HELPER-001.

Neuer Scheduler `WaterDeficitScheduler` (ersetzt `WaterReminderScheduler`):
- **Trigger**: `consumed_ml < target_ml(now) − 100ml` (100ml Toleranz).
- **Debounce**: nach jedem Slider-Drag und nach jedem Defizit-Übergang läuft ein 5-min Timer; erst wenn nach 5 min das Defizit immer noch besteht, feuert der erste Alarm. Dadurch löst Slider-„Spielen" (z.B. kurz auf 0 ziehen) keinen sofortigen Alarm aus.
- **Eskalation** (nach erstem Alarm): 30 min → 15 min → 10 min → 5 min (minimum). Reset auf 30 min sobald Defizit < 0.
- **Snooze**: Soll-Linie wird virtuell um +30 min verschoben (Ghost-Progress sinkt entsprechend). Maximal 2× hintereinander snoozbar, dann Eskalation wie gehabt.
- **Aus**: Toggle in Home-Wasser-Karte; stoppt nur Defizit-Alarme (Soll-Linie wird weiter gerendert, Bar bleibt rot bei Defizit).
- **Aktiv-Fenster**: 08:00–22:00 lokal. Außerhalb: **hartes Silent** — keine Notification, kein Sound, kein Vibration. Defizit-Tracking pausiert (Ghost friert auf 100 % ein).

### REQ-PROFILE-LAYOUT-001 — Profil-Sektionen
**Supersedes**: REQ-PROFILE-GOALS-001 (erweitert), entfernt Pinned-Nutrients-Sektion.

Profil-Tab MUSS folgende Sektionen zeigen:
1. **Stammdaten** (Name, Alter, Größe, Gewicht, Sex, Aktivitätslevel, Diet-Goal — bleibt unverändert).
2. **Tagesziele**: Liste **aller** Katalog-Nährstoffe (REQ-NUTRIENT-CATALOG-001) + Wasser. Pro Zeile: berechneter Default-Wert (read-only, klein), User-Override-Input (NumberField), Reset-Icon. User-Override persistiert **device-local** in `UserProfileEntity.dailyNutrientGoalsJson` (Room, Privacy-Boundary REQ-PROFILE-001/002 — der Server kennt die Goals nicht). Room-Schema-Bump 7→8 erweitert das Feld semantisch (Format-Erweiterung um Mikro-Keys; bestehende kcal/protein/carbs/fat-Keys bleiben kompatibel; Reset löscht den Key). Home liest `effective_target = override ?? computed_default`.
3. **Restliche Sektionen** (Allergien, Intoleranzen, Histamin, Wasser-Reminder-Toggle, Dark-Mode etc. — bleiben).

Pinned-Nutrients-Sektion (P6.S6-Pre-Spec) wird **entfernt**.

### REQ-PLAN-WATER-GOAL-001 — Wasser-Tagesziel im Plan
Plan-Tab erhält pro Tag einen optionalen **Wasser-Tagesziel-Slot** (device-local in Room-Tabelle `meal_plan_slots.water_goal_ml NULL`, Room-Schema-Bump 7→8). Default = Profil-Wert. Home liest `effective_water_goal = plan_slot_value ?? profile_value`. Slider 500–5000 ml, Schritt 50 ml.

### REQ-INGR-DETAIL-SHEET-001 — Lebensmittel-Detail als ModalBottomSheet (P7.S5 4f, 2026-05-29)
Tap auf eine Lebensmittel-Karte im `LebensmittelScreen` (Standard-Modus, NICHT Picker-Modus) MUSS einen `ModalBottomSheet` öffnen, der pro 100 g zeigt:
1. **Header**: `name_de`, optional Brand, Source-Badge mit Quelle + `fdc_id` (z. B. „USDA-FDC #170150").
2. **Nährwerte pro 100 g**: kcal, Protein, Kohlenhydrate (+davon Zucker), Fett (+davon gesättigt), Ballaststoffe, Salz — nur Felder mit Wert.
3. **Mikronährstoffe pro 100 g**: Werte aus `IngredientDto.micronutrients` mit `value > 0`, gruppiert in zwei Sektionen via `NutrientCatalog.ofCategory`:
   - „Vitamine" (Catalog-Reihenfolge).
   - „Mineralstoffe" (Catalog-Reihenfolge).
   - Pro Zeile: `displayDe` + Wert + Einheit + Prozent-DGE-Pill (`(value / nutrient.defaultPerDay) × 100`, gerundet).
4. **Allergene** (conditional): AssistChips mit `AllergenType.germanLabel`.
5. **FODMAP** (conditional): AssistChips mit `FodmapType.germanLabel`.
6. **Histamin** (conditional): nur bei `histamine_score != null` (aktuell 0/8354 Rows → Block bis SIGHI-CSV bereitgestellt unsichtbar).

Picker-Modus (`LebensmittelScreen(preselect = true)`) zeigt KEIN Sheet — Tap = direktes `onPick`. Detail-Sheet ist exklusiv für Stöber-Pfad.

Traceability: Mikro-Coverage-Audit gegen Produktiv-Postgres (2026-05-29): 87.9 % der USDA-Rows haben ≥ 10 Mikros, 66 % ≥ 20.

### Traceability (Erweiterung)

| REQ-ID | Sprint | Implementation-Anker |
|---|---|---|
| REQ-NUTRIENT-CATALOG-001 | P7.S1 | `domain/nutrition/NutrientCatalog.kt` |
| REQ-DATA-SOURCE-001 | P7.S2 | `server/tools/FetchFdcTopIds.kt` (Slice 1, ✅ 2026-05-27, 8487 IDs), `server/etl/UsdaFdcImporter.kt`, `EtlOrchestrator` |
| REQ-DATA-TRANSLATE-001 | P7.S2 | `server/scripts/translate_fdc_names.kts`, Admin-CSV-Review |
| REQ-INGR-MICRONUTRIENTS-001 | P7.S1 / P7.S5 4f | `V12__nutrients_overhaul.sql` (Server), `IngredientEntity`, `IngredientDto` (Server + Android), `presentation/lebensmittel/components/IngredientDetailSheet.kt` |
| REQ-INGR-ALLERGEN-MAPPING-001 | P7.S2 | `server/etl/AllergenMapper.kt` |
| REQ-HOME-NUTRIENT-LIST-001 | P7.S3 / P7.S3.b / P7.S4 4e | `presentation/home/HomeScreen.kt`, `NutrientListSection.kt`, `PinnedNutrientCard.kt` (Stufen-Bar in `PinnedNutrientRow`, Header mit Edit/Collapse, Unpin-Affordance, AddNutrientRow), `NutrientPinPickerSheet.kt` (NEU P7.S4), `HomeViewModel.togglePin/reorderPins/parsePinnedKeys` (Persistenz in `UserProfileEntity.pinnedNutrientsJson`) |
| REQ-HOME-WATER-BAR-001 | P7.S3a / P7.S3.b | `presentation/home/components/WaterStageSlider.kt` (Track via `waterStageTrackColor`), `WaterStageColors.kt` (public + `waterStageTrackColor`), `PinnedNutrientCard.kt` (trailingSlot), `data/repository/WaterIntakeRepository.setDayTotal`, `data/db/dao/WaterIntakeDao.replaceDayTotal` |
| REQ-HOME-WATER-ALARM-001 | P7.S3 | `notification/WaterDeficitScheduler.kt`, `AlarmReceiver.kt` (ACTION_WATER_DEFICIT) |
| REQ-PROFILE-LAYOUT-001 | P7.S4 | `presentation/profile/ProfileScreen.kt`, `NutrientGoalRow.kt`, Room v7→v8 |
| REQ-PLAN-WATER-GOAL-001 | P7.S4 | `presentation/plan/PlanScreen.kt`, `MealPlanSlotEntity.waterGoalMl`, Room v7→v8 |
| REQ-INGR-DETAIL-SHEET-001 | P7.S5 4f | `presentation/lebensmittel/LebensmittelScreen.kt` (`detailTarget`-State + `IngredientRow.onOpenDetail`), `presentation/lebensmittel/components/IngredientDetailSheet.kt` |

**End of §12.**
