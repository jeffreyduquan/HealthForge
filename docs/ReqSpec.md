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
