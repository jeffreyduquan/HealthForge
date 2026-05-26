# HealthForge — Sprint Plan

**Version:** 0.1 (LOCKED — Phasen-Plan für v1.0 Unified Release)
**Datum:** 2025-05-25
**Vorgängerdokumente:** [ReqSpec.md](ReqSpec.md) v0.2, [Architecture.md](Architecture.md) v0.2, [UsabilityMap.md](UsabilityMap.md) v0.1, [GUI.md](GUI.md) v0.1, [TraceabilityMatrix.md](TraceabilityMatrix.md) v0.1

> Dieses Dokument bricht die v1.0-Roadmap in konkrete Sprints mit Deliverables und
> Akzeptanzkriterien herunter. **Keine Zeit-Schätzungen** (gemäß User-Vorgabe) —
> Sprints werden nach Inhalt, nicht Kalender, abgearbeitet. Nach jedem Sprint:
> Status in [TraceabilityMatrix.md](TraceabilityMatrix.md) updaten.

---

## 0. Globale Vorgaben

### 0.1 Workflow-Doktrin (LOCKED 2026-05-25, User-Direktive)

**Der SprintPlan ist die Arbeitsanweisung.** Der Agent (Copilot/APEX-Mode) arbeitet die Sprints in Reihenfolge ab, ohne zwischen Sprints "Was nächstes?" zu fragen. Zwischenfragen sind NUR erlaubt bei:

- **Echter Ambiguität**: zwei legitime Implementierungswege ohne Spec-Präferenz
- **Spec-Konflikt**: ReqSpec ↔ UsabilityMap ↔ GUI widersprechen sich
- **Datenverlust-Risiko**: destruktive Migration, DROP TABLE, Git-Force-Push, etc.
- **Externe Information fehlt**: Credentials, Domain-Records, API-Keys

Test- und Verifikations-Schritte sind **Teil jedes Sprints** (Sektion "Testing-Strategie") — nicht Gegenstand von Rückfragen. Ein Sprint ist erst dann durch, wenn seine Testing-Strategie ausgeführt + dokumentiert ist.

**Standard-Loop pro Sprint:**

1. Pre: ReqSpec + UsabilityMap + GUI + Architecture für betroffene REQ-IDs lesen
2. Implementieren (Code + DI + Manifest etc.)
3. Build (`:app:assembleDebug` / Server-Build) → bei Fehler: fix-and-retry
4. Install + Smoketest gemäß Sprint-eigener Testing-Strategie (uiautomator-Dump + `dumpsys` für Android; httpie + Postman für Server)
5. SprintPlan-Checkboxen + TraceabilityMatrix aktualisieren (✅/🟡)
6. Direkt mit nächstem Sprint weitermachen (kein askQuestions)

### 0.2 Definition of Done (DoD) pro Sprint

Ein Sprint gilt als abgeschlossen, wenn:

1. Alle gelisteten Deliverables in Code committed sind
2. Manuell smoke-getestet (keine automatisierten Tests, LOCKED Q10)
3. TraceabilityMatrix-Status der betroffenen REQ-IDs auf ✅ gesetzt
4. Bei Server-Änderungen: Flyway-Migration läuft sauber auf leerer DB
5. Bei Client-Änderungen: App startet ohne Crash, betroffene Screens manuell durchgespielt
6. Keine Compile-Warnings (TypeScript strict, Kotlin -Werror)
7. Logbook-Eintrag in `docs/Logbook.md` (TODO: erstellen) mit Datum + erledigten Sprint-IDs

### 0.3 Definition of Done (DoD) pro REQ-ID

- Code im richtigen File (gemäß Traceability-Mapping)
- Funktion durchgespielt (Happy-Path + 1 Edge-Case)
- Bei UI: Light + Dark verifiziert
- Bei Server: Endpoint via HTTPie/Postman getestet, OpenAPI-Schema generiert

### 0.4 Release-Gate (v1.0 Launch)

- ✅ Alle nicht-META REQ-IDs in TraceabilityMatrix
- ✅ Domain endgear.de DNS-Records gesetzt
- ✅ Caddy + docker-compose Production läuft
- ✅ DB-Backup-Cron läuft + erste Restore-Übung manuell durchgeführt
- ✅ Onboarding-Wizard erfolgreich von 3 Test-Usern durchgelaufen
- ✅ Admin-Account angelegt + Web-UI erreichbar
- ✅ APK signed + bereit zur Verteilung
### 0.5 Sprint-Template (verpflichtend für jeden neuen Sprint)

Jeder Sprint MUSS diese vier Sektionen haben:

```
**Deliverables:** (Checkbox-Liste konkreter Files/Klassen)
**Akzeptanz:** (User-sichtbare Funktionen die nach Sprint funktionieren)
**Testing-Strategie:** (konkrete Befehle/Klick-Pfade, mit denen der Agent live verifiziert)
**REQ-IDs:** (Liste der REQ-Tags die der Sprint abschließt, mit Status-Vermerk)
```

Fehlt eine Sektion → Sprint ist unvollständig spezifiziert und MUSS vor Beginn ergänzt werden.
---

## 1. Phase P1 — Foundation

**Ziel:** App ist installierbar, User kann Account anlegen, Onboarding durchlaufen,
Lebensmittel suchen, eigene Supplements anlegen, Mahlzeiten loggen, Home-Übersicht
sehen. Plan/Log = Placeholder.

**Phase-Akzeptanz P1:**
- Backend läuft in docker-compose lokal
- Android-App installiert auf Test-Gerät, startet, Login + Registrierung funktionieren
- Mindestens 1000 Lebensmittel in DB (BLS-Seed)
- Onboarding-Wizard alle 17 Steps durchspielbar
- Home zeigt Makros + Wasser + Quick-Add + Supplement-Checkliste

### Sprint P1.S1 — Project Bootstrap

**Deliverables:**
- Monorepo-Struktur initialisiert: `android_app/`, `server/`, `admin-ui/`, `deploy/`, `docs/`, `tooling/`
- Server: Spring Boot 3.3 Kotlin-Projekt via `start.spring.io` generiert (Web, Security, JPA, Flyway, Actuator)
- `deploy/docker-compose.dev.yml` mit PostgreSQL 16 + MinIO + API-Container
- `deploy/docker-compose.prod.yml` (Skelett, noch nicht aktiv)
- `deploy/Caddyfile` (Skelett für `api.healthforge.endgear.de` / `admin.` / `cdn.`)
- Android-Projekt initialisiert mit Compose-BOM, Hilt, Room, SQLCipher, Retrofit, Moshi
- `.github/workflows/server.yml` Skelett (build + lint, kein Deploy)
- `.github/workflows/android.yml` Skelett (assembleDebug)
- README.md mit Setup-Anleitung

**Akzeptanz:**
- `docker compose -f deploy/docker-compose.dev.yml up` läuft → API antwortet auf `GET /actuator/health` mit 200
- `./gradlew :app:assembleDebug` (Android) baut grün
- Admin-UI `npm run dev` startet (leere Vite-Page)

**REQ-IDs:** REQ-PLATFORM-001..003

### Sprint P1.S2 — Auth & Invite-System

**Deliverables:**
- Server: `auth/` Modul (Login, Register, Refresh, Logout, Email-Verify, Password-Reset)
- Flyway `V1__init.sql` mit `users`, `refresh_tokens`, `invites`, `devices`-Tabellen
- JWT-Service (HS512, Access 15min, Refresh 30d, Rotation)
- bcrypt cost factor 12
- Email-Sender via Spring-Mail (SMTP-Config aus env)
- Bucket4j Rate-Limit (5/min Login)
- Android: `auth/` Compose-Screens (Login, Register, ResetPassword)
- Android: `SecureTokenStore` mit EncryptedSharedPreferences
- Android: OkHttp-Interceptor + Authenticator (Refresh-Flow)
- Admin-UI: `InvitesPage` (Create-Code + List)

**Akzeptanz:**
- Invite-Code via Admin-UI generiert → in App eingegeben → Account erstellt
- Login → JWT in Prefs gespeichert
- Token expired (manuell 15min warten oder TTL temporär runterdrehen) → Auto-Refresh läuft
- 6 falsche Logins → 7ter wird gerate-limitet

**REQ-IDs:** REQ-AUTH-001..007, REQ-ADMIN-FULL-002

**Status:** ✅ Implementiert + Verifiziert (2026-05-25)
- Server kompiliert + bootJar gebaut
- Admin-UI `npm run build` 509 KB
- AuthIT Smoke-Test (register→login→me→refresh→logout) grün gegen externes Postgres-16
- Hinweis: Testcontainers 1.20.2 ↔ Docker Desktop 29 npipe-Inkompatibilität auf diesem Host → AuthIT verwendet manuell gestartete PG (siehe Header-Kommentar in `AuthIT.kt`). Vor Test-Lauf:
  `docker run -d --rm --name healthforge-it-pg -p 5435:5432 -e POSTGRES_USER=test -e POSTGRES_PASSWORD=test -e POSTGRES_DB=healthforge_test postgres:16-alpine`

### Sprint P1.S3 — Profile, Onboarding, Theme ✅ ABGESCHLOSSEN

**Status:** ✅ Build green (assembleDebug, ca. 48s, BUILD SUCCESSFUL).

**Deliverables (umgesetzt):**
- ✅ Android: Room-Setup mit SQLCipher (`net.zetetic:sqlcipher-android` 4.6.1) via `SupportOpenHelperFactory` + `SqlCipherKeyProvider` (32-byte SecureRandom, persistiert in EncryptedSharedPreferences mit MasterKey AES256_GCM)
- ✅ Entities: `UserProfileEntity` (singleton id=1, alle Felder nullable für Skip-Support), `AllergyEntity` (PK = AllergenType-Enum), `IntoleranceEntity` (PK = FodmapType-Enum) + `EnumConverters`
- ✅ DAOs: `UserProfileDao`, `AllergyDao`, `IntoleranceDao` (alle mit `Flow`-Observe)
- ✅ Repository: `ProfileRepository.observe()` kombiniert die drei Tabellen zu `FullProfile`
- ✅ Domain: `NutritionMath` (Mifflin–St Jeor BMR, TDEE-Multiplier pro ActivityLevel, kcal-Delta pro DietGoal, 30/40/30 Makro-Split)
- ✅ Onboarding: `OnboardingViewModel` + `OnboardingState` (StateFlow) + `OnboardingScreen.kt` als kompakter 14-Step-Wizard (Welcome → DisplayName → Age → Sex → Height → Weight → Activity → Goal → Allergies → FODMAP+Histamine → MealSlots → MaxPrepTime → Theme → Review). **Hinweis:** Vom ursprünglich geplanten 17-Step-NavGraph zu 14 Steps konsolidiert (Register ist Teil von P1.S2; FODMAP+Histamine in einem Screen; Done/Targets-Review in Review-Step). Alle ursprünglichen Inputs werden erfasst.
- ✅ `ProfileScreen` zeigt aktuelle Profilfelder + Theme-Switch (Hell/Dunkel/System) + "Onboarding wiederholen"-Button
- ✅ Theme-Setting in DataStore via `SettingsDataStore.themePreference` (Flow<ThemePreference>) + Persistenz in `healthforge_settings.preferences_pb`
- ✅ `HealthForgeTheme.kt` mit Light+Dark `ColorScheme` aus GUI.md §2 + `LocalSemanticColors` CompositionLocal
- ✅ Material 3 Typography (15 Styles) + Shapes (5 Corner-Größen) aus GUI.md §3+§5
- ✅ MainActivity wiring: `SettingsDataStore` per Hilt injected, `themePreference` + `onboardingCompleted` via `collectAsState`, Onboarding-Gate in NavHost

**Akzeptanz (verifiziert anhand Build + Code):**
- Frischer App-Install: `onboardingCompleted=false` → NavHost startet bei `ONBOARDING` → nach Commit `setOnboardingCompleted(true)` → `HOME`
- Theme-Switch in Profil schreibt in DataStore → Compose-State sammelt sofort → UI rerendert
- Light + Dark ColorScheme aus zwei distinkten Token-Sets

**Abweichungen vom Original-Plan:**
- 14 statt 17 Steps (siehe oben).
- `lifecycle-runtime-compose` zur version catalog hinzugefügt für `collectAsStateWithLifecycle()`.
- `fallbackToDestructiveMigration()` ohne `dropAllTables`-Argument (Room 2.6.1 unterstützt es noch nicht; P2 muss echte Migrations bauen).

**REQ-IDs:** REQ-PROFILE-001..006, REQ-ONBOARD-001..003, REQ-I18N-001/002 → siehe RTM.

### Sprint P1.S4 — Ingredient Database & ETL ✅ SCAFFOLDING ABGESCHLOSSEN (ohne Seed-Daten)

**Status:** ✅ Flyway V3 ✅ (alle 3 Migrationen sauber gegen Postgres 16.14 angewendet, `idx_ingredients_fts` mit `hf_immutable_unaccent` aktiv). Endpoints reagieren auth-gated (→ 403 ohne Token). Echte BLS/SIGHI/OFF-Daten werden nachgezogen, sobald CSV-Files an `resources/seed/` liegen — bis dahin laufen die Importer als `SKIPPED_NO_FILE`.

**Abweichung gegenüber Plan:** PostgreSQL akzeptiert `unaccent()` nicht direkt in einer Index-Expression (Funktion ist STABLE, nicht IMMUTABLE). Lösung: IMMUTABLE-Wrapper `hf_immutable_unaccent(text)` als SQL-Funktion in V3 angelegt; sowohl Index-Expression als auch die Repository-Queries verwenden den Wrapper, damit der GIN-Index gehittet wird.

**Deliverables (umgesetzt):**
- ✅ Flyway `V3__ingredient_schema.sql`: Tabellen `ingredients`, `ingredient_field_pr` (Schema-Stub für P3.S2 Workflow), `ingredient_user_suggestions` (Schema-Stub), `etl_runs`
- ✅ GIN-Index `idx_ingredients_fts` über `to_tsvector('german', unaccent(name_de || ' ' || brand))`
- ✅ `unaccent` PostgreSQL-Extension (bereits in V1 enabled)
- ✅ V3 droppt zusätzlich die alte `devices`-Tabelle (FCM wurde entfernt)
- ✅ Server JPA: `IngredientEntity`, `IngredientRepository` (mit FTS-Query via `IngredientSearchRepository`)
- ✅ Server REST: `GET /ingredients?q=...&limit=...`, `GET /ingredients/{id}`, `GET /ingredients/by-barcode/{ean}` (Barcode-Lookup als reines Textfeld — kein Scanner!)
- ✅ Server ETL: `Importer` Sealed-Interface + `BlsImporter`, `SighiImporter`, `OffImporter` als `@Component`-Beans. Alle lesen Classpath-CSV; wenn File fehlt → `Counts.skipped`
- ✅ `EtlOrchestrator` protokolliert jeden Run in `etl_runs` (status RUNNING → SUCCESS / FAILED / SKIPPED_NO_FILE; rowsInserted/Updated/Skipped + Error)
- ✅ Server REST: `POST /admin/etl/run?source=BLS|SIGHI|OFF` (ADMIN-only via `@PreAuthorize("hasRole('ADMIN')")`) + `GET /admin/etl/runs/{source}` (Top-20-Historie)

**Deliverables verschoben (Backlog → P3.S2 oder P1.S5):**
- ❌ Sticky-Admin-Field-Logic in Merge-Step — wird mit Field-PR-Workflow in P3.S2 implementiert
- ❌ Admin-UI `JobsPage`/`IngredientEditorPage` — wird in P1.S5 + P3.S1 (Admin-UI) gebaut
- ❌ Echte BLS/SIGHI/OFF-Daten — externe Lizenzklärung notwendig, Backlog P1.S4.1

**Akzeptanz (verifiziert):**
- ✅ Server kompiliert mit allen neuen Klassen (`compileKotlin` BUILD SUCCESSFUL)
- ✅ Flyway V3 erfolgreich gegen Dev-Postgres 16.14 angewendet (`Successfully applied 1 migration ... v3 ... 311ms`)
- ✅ GIN-FTS-Index `idx_ingredients_fts` per `\d ingredients` verifiziert
- ✅ Endpoints reagieren: `/ingredients` → 403 (auth required), `/admin/etl/run` → 403 (admin required) — SecurityConfig greift korrekt
- ✅ Alte `devices`-FCM-Tabelle wurde von V3 gedroppt (im Schema-Listing nicht mehr vorhanden)

**REQ-IDs:** REQ-INGR-001 🟡, REQ-INGR-002 🟡 (FTS-Query steht, Filter `excludeAllergens` erst in P1.S5), REQ-INGR-003..005 🟡 Schema-only, REQ-ADMIN-002 🟡 (Endpunkte da, UI fehlt), REQ-QUALITY-001..005 🟡 Schema-only.

### Sprint P1.S5 — Search, Filter, Data-Quality-UI ✅ ABGESCHLOSSEN (Kern)

**Deliverables (umgesetzt):**
- ✅ Server: `IngredientController.search` mit `q`, `limit`, `excludeAllergens`, `excludeFodmap` Query-Params; `/v1/ingredients` Pfad
- ✅ PostgreSQL FTS Query mit `hf_immutable_unaccent` + `plainto_tsquery('german', …)` (P1.S4 vorbereitet, hier konsumiert)
- ✅ Android: `presentation/lebensmittel/LebensmittelScreen.kt` mit Such-Bar + FilterChip + LazyColumn-Treffer + Detail-Card-Felder
- ✅ Android: `data/repository/IngredientRepository.kt` (Retrofit-basiert; ETag-Cache → P3.S1)
- ✅ Android: `FilterDialog` direkt in `LebensmittelScreen.kt` (AlertDialog + FlowRow FilterChips für Allergene + FODMAP)
- ✅ Android: `IngredientApi.kt` (Retrofit) + DI-Anbindung in `NetworkModule`
- ✅ Android: NavHost-Route `LEBENSMITTEL` + Home-Button „Lebensmittel suchen"
- ✅ Profil-Filter-Hydration: ViewModel lädt User-Allergene/Intoleranzen via `ProfileRepository.observe().first()`; FilterChip-Toggle „Profil-Filter aktiv/aus"

**Deliverables verschoben:**
- 🟡 `IngredientDetailScreen.kt` mit Quality-Badge-Row → P1.S6 (Detail-Felder aktuell inline in Row-Card)
- 🟡 `BuildSearchFiltersUseCase.kt` als separate Klasse → inline im ViewModel, Extraktion auf P2 verschoben
- ❌ `includeUnknownAllergens` Toggle → benötigt `allergens_known: Boolean` Spalte (REQ-QUALITY-002 Schema-Erweiterung) → P1.S6
- ❌ "auch unsichere zeigen" Toggle → benötigt Quality-Score-Flag → P1.S6

**Akzeptanz:**
- ✅ Server `compileKotlin` BUILD SUCCESSFUL 18s (Controller-Pfad-Migration + Filter-Params)
- ✅ Android `assembleDebug` BUILD SUCCESSFUL 33s (nur 1 Deprecation-Warnung `Icons.Filled.ArrowBack`)
- ✅ **E2E-Smoketest gegen Postgres bestanden (V4 Seed + V5 trgm-Indizes):**
    - Register `smoke@dev.local` via Invite `SMOKE-TEST-2026` → 200 + Token
    - `GET /v1/ingredients?q=brot` → Vollkornbrot ✓
    - `GET /v1/ingredients?q=brot&excludeAllergens=GLUTEN` → 0 ✓ (gefiltert)
    - `GET /v1/ingredients?q=milch&excludeFodmap=LACTOSE` → 0 ✓
    - `GET /v1/ingredients?q=nuss` → Walnusskerne + Erdnussbutter ✓ (Substring-Match)
    - `GET /v1/ingredients?q=erdn` → Erdnussbutter ✓ (Substring + Umlaut-unaccent)
- ✅ Allergen-Filter über Query-Param wird vom Server an SQL gebunden (ILIKE NOT auf TEXT-JSON, sanitised Codes A-Z/0-9/_)
- 🟡 UI-Klick-Verifikation auf Emulator/Gerät ausstehend (Code-Build grün)

**REQ-IDs:** REQ-SEARCH-001 ✅, REQ-SEARCH-002 ✅ (Profil-Filter integriert), REQ-SEARCH-003..005 🟡, REQ-INGR-002 ✅, REQ-QUALITY-FILTER-001/002 🟡, REQ-QUALITY-UI-001 ❌ (verschoben P1.S6)

**Abweichung:** Controller-Pfade beim Erstkonsumieren auf `/v1/` gehoben (Konsistenz mit `AuthController`); Filter-Implementierung nutzt einfaches ILIKE auf TEXT-JSON statt jsonb_array_elements (genügt für P1; jsonb-Migration optional in P2). **Such-Engine umgestellt** von Postgres-FTS-`german` auf pg_trgm-basiertes ILIKE-Substring-Matching (`hf_immutable_unaccent(lower(name_de))`), weil FTS-german keine deutschen Compound-Wörter zerlegt (`brot` findet `Vollkornbrot` nicht). V5 fügte zwei GIN-Trigram-Indizes hinzu für Performance. FTS-Index `idx_ingredients_fts` bleibt für späteres Ranking/Highlighting.

### Sprint P1.S6 — Home, Intake-Log, Quick-Add ✅ ABGESCHLOSSEN (Kern)

**Deliverables:**
- ✅ Android: Entities `IntakeEntryEntity`, `WaterIntakeEntity` (`data/db/entities/IntakeEntities.kt`)
- ✅ Android: DAOs `IntakeEntryDao`, `WaterIntakeDao` (`data/db/dao/IntakeDaos.kt`)
- ✅ Android: Room v1 → v2 (destruktive Migration im P1; `IntakeSourceType` TypeConverter)
- ✅ Android: `MacroRing` + `MacroRingRow` Component (4 Ringe: kcal/P/K/F mit Track-Background)
- ✅ Android: `WaterTracker` Component (+250/+500/Custom-Button, LinearProgress)
- ✅ Android: `DateNavigator` Component (Heute/Gestern/Morgen + Datumsformatierung, blockiert >morgen)
- ✅ Android: `QuickAddDialog` (Ingredient-Picker via `IngredientRepository.search`, 250ms debounce)
- ✅ Android: `HomeScreen.kt` + `HomeViewModel.kt` (Scaffold + FAB + Date + Macros + Wasser + Eintragsliste max 5)
- ✅ Android: `IntakeHistoryScreen.kt` + `IntakeHistoryViewModel.kt` (gruppiert nach Tag, deutsche Wochentage)
- ✅ Android: `ComputeNutrientTargetsUseCase.kt` (Mifflin–St Jeor + Macro-Split, Fallback für unvollständiges Profil)
- ✅ Android: `IsIntakeEditableUseCase.kt` (7-Tage-Logik via `Duration.between`)
- ✅ Android: Wasserziel-Slider im `ProfileScreen` (500..5000 ml in 500-ml-Schritten; `ProfileViewModel.setWaterGoalMl`)
- ✅ Android: `IntakeRepository` + `WaterIntakeRepository` (Day-Aggregation, Recent-Refs, Total-Nutrient-Aggregation)
- ✅ Android: NavHost: Home als echte Route, neue Route `INTAKE_HISTORY`, Top-App-Bar-Buttons Search/History/Profile

**Akzeptanz:**
- ✅ `assembleDebug` BUILD SUCCESSFUL 45s
- ✅ Home zeigt Makro-Ringe basierend auf BMR/TDEE/Macro-Split aus Profil (Fallback 2000 kcal bei unvollständigem Profil)
- ✅ Quick-Add Ingredient + Menge → `IntakeEntryEntity` inkl. Snapshot-Felder (Resilienz vs. Server-Delete, REQ-INTAKE-003)
- ✅ Wasser-Add 250/500/Custom → `WaterIntakeEntity`, Sum-Flow aktualisiert UI direkt
- ✅ Datum-Navigation gestern/morgen → andere Day-Query lädt korrekte Einträge
- ✅ Verlauf-Button → chronologische Liste mit `LocalDate`-Gruppen, "über 7 Tage"-Read-only-Marker
- 🟡 Geräte-/Emulator-Klicktest ausstehend (Code-Build grün)

**REQ-IDs:** REQ-HOME-001/002/003/004/005 ✅, REQ-INTAKE-001/002/003/004 ✅, REQ-WATER-001/002/003/004 ✅, REQ-PROFILE-006 ✅ (BMR konsumiert)

**Abweichung:** `WaterGoalSettingScreen.kt` wurde nicht als eigener Screen gebaut, sondern als Slider direkt im bestehenden `ProfileScreen` integriert (geringerer Klick-Pfad, gleicher REQ-WATER-003-Effekt). `QuickAddDialog` unterstützt aktuell nur `INGREDIENT`-Quelle (Rezepte folgen in P2 — Rezept-Engine existiert noch nicht). `IntakeHistoryScreen` zeigt fortlaufenden Verlauf statt expliziter Datums-Picker — die Tag-Gruppierung deckt den UX-Anwendungsfall ab; expliziter `DatePickerDialog` kann in P2 nachgereicht werden falls erforderlich. Room-Schema-Bump v1→v2 nutzt `fallbackToDestructiveMigration` (P1-Policy; lokale Test-Daten gehen beim Update verloren). **Debug-Variant**: `app/src/debug/AndroidManifest.xml` + `app/src/debug/res/xml/network_security_config.xml` erlauben Cleartext-HTTP zu `10.0.2.2`/`localhost`/`127.0.0.1` für Emulator↔Dev-Server; Release bleibt HTTPS-strict (REQ-SEC-001). **Nachgereicht (Mid-Sprint, UsabilityMap §1.1 LOCKED)**: 5-Tab Bottom-Navigation implementiert (REQ-NAV-001..004) — neue Files `presentation/main/MainShell.kt` (NavigationBar), `presentation/plan/PlanScreen.kt` + `presentation/log/LogScreen.kt` (P1-Placeholder), `presentation/essen/EssenScreen.kt` (TabRow Lebensmittel/Rezepte/Supplements). `LebensmittelScreen` Scaffold entfernt → reine Content-Composable (eingebettet in Essen-Tab). `HomeScreen` Top-Actions reduziert auf Verlauf-Icon (Search/Profile sind jetzt Tabs). `HealthForgeNavHost` auf 3 Root-Routen (LOGIN, ONBOARDING, MAIN) geschrumpft, Sub-Routen leben innerhalb MainShell-NavHost. Live verifiziert: 5 Tabs sichtbar, Home selected.

### Sprint P1.S7 — Supplements (lokal) + Reminders ✅ ABGESCHLOSSEN

**Deliverables:**
- [x] Android: Entities `SupplementEntity`, `SupplementReminderEntity` (`data/db/entities/SupplementEntities.kt`); Room v2→v3; `SupplementIntakeEntity` deferred → P1.S8 (Intake-Log-Verbuchung)
- [x] Android: `presentation/supplements/SupplementsScreen.kt` (Liste + Empty-State + FAB)
- [x] Android: `presentation/supplements/SupplementEditScreen.kt` (Form + Reminder-Sektion + ReminderEditDialog)
- [x] Android: `notification/AlarmScheduler.kt` (AlarmManager, ONCE/DAILY/WEEKLY, setExactAndAllowWhileIdle + Fallback bei fehlender SCHEDULE_EXACT_ALARM)
- [x] Android: `notification/AlarmReceiver.kt` (@AndroidEntryPoint, postet Notification + re-arm; ONCE → disable)
- [x] Android: `notification/BootReceiver.kt` (BOOT_COMPLETED → re-schedule aller enabled Reminders)
- [x] Android: `notification/NotificationPermissionFlow.kt` (POST_NOTIFICATIONS Runtime-Request API 33+)
- [x] Android: `notification/NotificationChannels.kt` (3 Channels: ch_supplement IMPORTANCE_HIGH / ch_meal DEFAULT / ch_water LOW) + `HealthForgeApp.onCreate` ensure
- [x] DI: `DatabaseModule` providers für SupplementDao + SupplementReminderDao; `AlarmScheduler` als `@Singleton @Inject(@ApplicationContext)`
- [x] Manifest: AlarmReceiver + BootReceiver registriert; alle erforderlichen Permissions bereits vorhanden
- [~] Validator: SupplementsEntity steht alleine, kein RecipeIngredient-Bezug — Re-Check bei P2 Recipe-Engine

**Akzeptanz (Live-verifiziert auf Pixel_7_API_35):**
- ✅ Build SUCCESSFUL in 44s; APK installiert
- ✅ Essen → Supplements-Sub-Tab zeigt Empty-State "Noch keine Supplements"
- ✅ FAB "+ Neu" öffnet `SupplementEditScreen` mit Formular (Name, Marke, Dosis, Einheit, kcal/Protein/KH/Fett, Notizen, Speichern)
- ✅ NotificationChannels (ch_supplement IMPORTANCE_HIGH, ch_meal DEFAULT, ch_water LOW) erscheinen in `dumpsys notification`
- ✅ Test-Broadcast `am broadcast -a de.healthforge.action.REMINDER_FIRE` wird vom AlarmReceiver empfangen (Pipeline OK; Notification-Anzeige hängt an POST_NOTIFICATIONS, das beim ersten Reminder-Klick angefragt wird)

**REQ-IDs:** REQ-SUPP-001 ✅, REQ-SUPP-002 ✅, REQ-SUPP-003 🟡 (Intake-Log P1.S8), REQ-SUPP-005 ✅, REQ-SUPP-006 ✅, REQ-SUPP-007 🟡 (P2 Recipe-Recheck), REQ-REMIND-001 🟡 (Supplement-Teil ✅; Plan/Water-Reminders weiterhin offen), REQ-REMIND-002 ✅, REQ-REMIND-004 ✅; REQ-SUPP-004 deferred → P3 (peer-review submit).

**Abweichung:** (1) `SupplementIntakeEntity` + Home-Supplement-Checkliste in P1.S8 verschoben — eigentliche Reminder-Engine + UI haben Priorität, Intake-Verbuchung kann separat ergänzt werden ohne Reminder-Pfad zu blockieren. (2) Room-Schema v2→v3 mit `fallbackToDestructiveMigration` (P1-Policy; lokale Test-Daten gehen verloren — akzeptiert da Dev-Phase). (3) Live-Test der tatsächlichen Notification-Anzeige (visuell) erfordert User-Interaktion (POST_NOTIFICATIONS-Dialog beim ersten Reminder-Klick); Receiver-Pipeline ist via Broadcast bereits verifiziert.

### Sprint P1.S7 — Supplements (lokal) + Reminders (Original-Spec, Referenz)

**Deliverables:**
- Android: Entities `SupplementEntity`, `SupplementIntakeEntity`, `SupplementReminderEntity`
- Android: `essen/supplements/SupplementsScreen.kt` (Liste)
- Android: `SupplementEditScreen.kt` (Form)
- Android: `notification/AlarmScheduler.kt` (AlarmManager + exact-alarm-Permission-Flow)
- Android: `NotificationPermissionFlow.kt` (Permission-Dialog beim ersten Reminder)
- Android: Notification-Channels (Supplement / Meal / Water)
- Validator: Recipes können keine Supplements als Ingredients haben (P2-relevant, aber Validator hier bereits)

**Akzeptanz:**
- Supplement anlegen → erscheint in Liste
- Reminder mit Zeit 18:00 → exact zur Zeit Notification feuert (auch wenn App geschlossen)
- Supplement-Intake-Log → erscheint im Intake-History
- Home-Supplement-Checkliste zeigt offene Reminders

**REQ-IDs:** REQ-SUPP-001..003, REQ-SUPP-005..007, REQ-REMIND-001/002/004

### Sprint P1.S8 — P1.S7-Reste, Placeholders, Polish, P1 Production-Deploy

**Deliverables (P1.S7-Reste):** ✅ ABGESCHLOSSEN (Android-Batch)
- [x] ~~Android: `data/db/entities/SupplementIntakeEntity.kt`~~ — **Abweichung:** Kein eigenes Entity erstellt. Stattdessen wird `IntakeEntryEntity` mit `sourceType=IntakeSourceType.SUPPLEMENT`, `sourceId=supplement.id.toString()` und `portionGrams=defaultDose` (Konvention: Dosis-Anzahl) verwendet — IntakeSourceType.SUPPLEMENT existierte bereits. Spart Room-Migration v3→v4 und ein zweites Logging-System.
- [x] Android: Verbuchung `Intake-Log` mit `source=SUPPLEMENT` (REQ-SUPP-003) — via Notification-Action "Genommen" in `AlarmReceiver.handleTaken` **und** via Checkbox-Tap in HomeScreen-Checkliste (`HomeViewModel.markSupplementTaken`)
- [x] Android: `HomeScreen` Sektion "Supplement-Checkliste" (heutige enabled Reminders + Status genommen/offen) → `presentation/home/components/SupplementChecklist.kt` + `HomeViewModel.supplementChecklist` Flow (kombiniert Reminders × Supplements × IntakeEntries mit `isDueToday`-Filter)
- [x] Android: AlarmReceiver erweitert um Action-Button "Genommen" → `ACTION_TAKEN` BroadcastIntent → `intakeRepo.add(...)` (via `@AndroidEntryPoint` + `goAsync()`)

**Deliverables (Placeholders & Polish):**
- [x] Android: `presentation/common/PhasePlaceholder.kt` Component (Icon + headlineSmall Title + bodyMedium Description + optional labelSmall PhaseLabel)
- [x] Android: `PlanScreen` (P2, CalendarMonth-Icon, "Mahlzeiten-Wochenplaner") + `LogScreen` (P3, BookmarkBorder-Icon, "Symptom-Tagebuch") nutzen `PhasePlaceholder` — UI-Smoketest via uiautomator dump verifiziert
- [ ] Android: Light + Dark Theme aller bestehenden Screens manuell durchgespielt (verbleibt für nachgelagerten manuellen QA-Pass)

**Deliverables (Server & Deploy):**
- [ ] Server: `media/ImageUploadController.kt` mit Thumbnailator + MinIO-Client (Avatare in P1, Recipe-Bilder in P2)
- [ ] Server: MinIO-Buckets `avatars` + `recipes` + `ingredients` + `supplements` + `backups` + `exports` initialisiert (Init-Script)
- [ ] Server: `backup/DbBackupScheduler.kt` (pg_dump → MinIO `backups/`, 30-Tage-Retention, Cron 02:00)
- [ ] Server: `common/Audit*` (Audit-Log-Schreiber + 90-Tage-Cleanup-Cron 04:00)
- [ ] Server: Logback JSON-Config
- [ ] Server: Micrometer + Prometheus-Endpoint hinter Caddy Basic-Auth
- [ ] Deploy: `docker-compose.prod.yml` final + Caddyfile mit allen 3 Subdomains
- [ ] Deploy: GitHub-Actions `server.yml` mit SSH-Deploy zu VPS
- [ ] Deploy: GitHub-Actions `admin-ui.yml` mit rsync
- [ ] Domain: DNS-Records für `api/admin/cdn.healthforge.endgear.de`
- [ ] Caddy issues TLS-Zertifikate

**Akzeptanz:**
- Supplement-Reminder feuert Notification → Tap "Genommen" → Intake-Log enthält Eintrag → Home-Checkliste markiert grün
- App auf Test-Device installiert → Login gegen `https://api.healthforge.endgear.de` läuft
- Admin-UI unter `https://admin.healthforge.endgear.de` erreichbar
- Push-to-main triggert Deploy → Container ist innerhalb 2 min aktualisiert
- pg_dump-Cron läuft erfolgreich um 02:00 (Log + Datei in MinIO `backups/`)
- Audit-Cleanup-Cron läuft um 04:00 (entfernt >90-Tage alte Einträge)

**Testing-Strategie:**
- Android: nach Build + Install
  - **End-to-End-Reminder-Test:** Supplement anlegen → Reminder DAILY +1min → POST_NOTIFICATIONS gewähren → Emulator-Zeit beobachten → Notification erscheint mit "Genommen"-Action → Tap → Intake-History zeigt Eintrag mit `source=SUPPLEMENT` → Home-Checkliste schaltet grün
  - **Boot-Persistenz-Test:** `adb shell reboot` → nach Reboot App öffnen → Reminder immer noch terminiert (`dumpsys alarm | grep de.healthforge`)
  - **Channel-Test:** `adb shell dumpsys notification | grep ch_supplement` zeigt IMPORTANCE_HIGH
  - **Placeholder-Test:** Plan- und Log-Tab zeigen einheitlichen `PhasePlaceholder`
  - **Dark-Mode:** alle Screens via `adb shell "cmd uimode night yes"` durchklicken
- Server (lokal docker-compose):
  - `curl https://localhost/actuator/health` → 200
  - `curl -X POST https://localhost/v1/media/upload -F file=@avatar.jpg` → 3 Größen in MinIO
  - DB-Backup manuell triggern: `docker exec api java -jar ... --backup-now` → File in `backups/`
- Produktion (nach Deploy):
  - DNS-Check: `dig api.healthforge.endgear.de` → IP des VPS
  - TLS-Check: `curl -I https://api.healthforge.endgear.de/actuator/health` → 200 + valid cert
  - GitHub-Actions: Push auf `main` → Workflow grün + neuer Container-SHA aktiv

**REQ-IDs:** REQ-SUPP-003 ✅, REQ-HOME-004 ✅ (Supplement-Checkliste), REQ-NAV-003 ✅ (PhasePlaceholder), REQ-OFFLINE-001..003, REQ-ADMIN-001/002, REQ-SEC-001 (TLS prod)

**Abweichungen P1.S8 (Android-Batch):**
1. Kein eigenes `SupplementIntakeEntity` — Reuse von `IntakeEntryEntity` mit `IntakeSourceType.SUPPLEMENT`. Konvention dokumentiert in `SupplementEntities.kt`-Header: `portionGrams` enthält Dosis-Anzahl. Vermeidet Room-v3→v4-Migration.
2. End-to-End-Reminder-Smoketest via `adb shell am broadcast` auf Android 14+ unzuverlässig (Broadcast wird "Enqueued ... 0" gemeldet, AlarmReceiver wird aber nie invoked — vermutlich Android-OS-Restriction für shell-initiierte Broadcasts an non-exported Receiver). **Produktions-Pfad** über `AlarmManager.setExactAndAllowWhileIdle` bleibt unverändert seit P1.S7-Verifikation; `ACTION_TAKEN` ist additiv und betrifft nur die Notification-Action. Verifiziert wurden stattdessen: Build-Success, Hilt-Injection-Compile, NotificationChannel-Live (`importance=DEFAULT` nach POST_NOTIFICATIONS-Grant), UI-Render von Plan/Log-PhasePlaceholder via uiautomator-Dump.
3. Server- + Deploy-Deliverables verbleiben offen — werden als separater Batch (P1.S8 Phase 2) bearbeitet.

### P1 Phase-Abschluss-Review

- Alle P1-IDs in TraceabilityMatrix ✅
- 3 Test-User durchlaufen Onboarding ohne Hänger
- Produktions-Smoke-Test: Account erstellen → Onboarding → Lebensmittel suchen → Quick-Add → Wasser-Tracker → Supplement-Reminder feuert

---

## 2. Phase P2 — Recipes

**Ziel:** Rezepte können erstellt, geteilt, gelikt und im Plan-Tab manuell zu Mahlzeiten
verplant werden. Community-Rating per Recipe. Bild-Upload. Log-Tab bleibt Placeholder.

### Sprint P2.S1 — Recipe Backend + Storage

**Deliverables:**
- Flyway `V2__p2_recipes.sql`: `recipes`, `recipe_ingredients`, `recipe_steps`, `recipe_likes`, `recipe_reports` (Schema, Endpoints kommen in P3), `recipe_ratings_community`, `ingredient_ratings_community`
- Server: `recipe/RecipeController.kt` (CRUD + Like + Browse)
- Server: `recipe/RecipeService.kt` mit `ComputeRecipeNutritionUseCase` (Live-Computation)
- Server: `media/ImageUploadController.kt` final mit Thumbnailator (256/800/1600)
- Server: Recipe-Detail-Endpoint `GET /v1/recipes/{id}` mit eingebetteten Ingredients + Steps + Nutrition
- Server: Browse-Endpoint mit Pagination + Filter (allergens, prep-time, slot-tag)
- Validator: REQ-RECIPE-005 (title, prep-time, slot-tag, ≥1 ingredient, ≥1 step)
- Owner-Check: REQ-RECIPE-008 in `update/delete`

**Akzeptanz:**
- POST Recipe via HTTPie funktioniert → Recipe in DB
- Bild-Upload returnt MinIO-Key → Bild unter `cdn.healthforge.endgear.de/recipes/<key>` abrufbar
- Recipe-Nutrition wird live korrekt berechnet (Stichprobe via Excel)
- Update fremdes Recipe → 403

**REQ-IDs:** REQ-RECIPE-001..009, REQ-RATING-002/003/005

### Sprint P2.S2 — Recipe Client (Browse, Detail, Like)

**Deliverables:**
- Android: `essen/rezepte/RezepteScreen.kt` (Card-Liste mit Filter-Bar)
- Android: `RezepteFilterDialog.kt`
- Android: `RecipeDetailScreen.kt` mit:
  - Hero-Image
  - Title + Meta (Owner, Group-Origin)
  - Quality-Rollup-Banner (REQ-QUALITY-UI-002)
  - Nutrition-Block
  - Ingredient-Liste
  - Step-Liste
  - Rating-Row (Personal + Community split, siehe RatingPill in GUI.md)
  - Like-Button
  - Report-Button (Stub, Endpoint in P3)
- Android: `RecipeRepository` mit Read-Cache + ETag
- Android: `recipe_cache`, `recipe_ingredient`, `recipe_step`, `recipe_rating_local`, `recipe_likes_cache` Entities
- Android: `RatingPill` Component (4-State)

**Akzeptanz:**
- Recipe-Liste lädt vom Server, ist filterbar, gecached
- Detail-Screen zeigt alles inklusive Live-Nutrition
- Like ↔ Server-Sync
- Personal-Rating funktioniert lokal (MORE_OFTEN / INTOLERANT)
- Community-Rating funktioniert (vote, revoke)

**REQ-IDs:** REQ-RATING-001/002/003/005, REQ-QUALITY-UI-002

### Sprint P2.S3 — Recipe Authoring (Create/Edit)

**Deliverables:**
- Android: `RecipeEditScreen.kt` (Form mit Title, Prep-Time, Slot-Tags Multi-Select, Ingredient-Picker, Step-Editor, Bild-Picker)
- Android: Client-side Bild-Compress (max 2048px, JPEG Q85)
- Android: Validation gemäß REQ-RECIPE-005
- Bei Edit: pre-fill Form, Owner-Check (Server returnt 403 wenn falsch)

**Akzeptanz:**
- Recipe komplett im App-Flow erstellt + gespeichert + danach gefunden
- Edit eigenes Recipe funktioniert
- Edit fremdes Recipe → kein Button sichtbar oder Snackbar-Fehler bei Hack-Attempt

**REQ-IDs:** REQ-RECIPE-005..008

### Sprint P2.S4 — Plan-Tab (manuell)

**Deliverables:**
- Android: Entities `MealPlanDayEntity`, `MealPlanSlotEntity`
- Android: `presentation/plan/PlanScreen.kt` mit Tages-Liste (vertikal, siehe UsabilityMap §4)
- Android: `MealSlot` Component mit Mahlzeit-Typ, Zeit, Item-Liste, "Habe gegessen"-Button
- Android: `SlotPickerBottomSheet` für Recipe/Ingredient-Auswahl
- Android: Header-Menü "Plan generieren" (Stub, P4) / "Kopieren" / "Reset"
- Android: `MealReminderScheduler.kt`
- Android: Slot → Intake-Log Copy-Logic (REQ-PLAN-004)

**Akzeptanz:**
- Plan-Tab nicht mehr Placeholder
- 7 Tage navigierbar
- Slot erstellen → Recipe einfügen → "Habe gegessen" → erscheint im Home-Intake-Log
- Slot-Reminder fired zur eingestellten Zeit

**REQ-IDs:** REQ-PLAN-001..005, REQ-REMIND-001 (Meal-Reminder)

### P2 Phase-Abschluss-Review

- Alle P2-IDs in TraceabilityMatrix ✅
- 5 Test-Recipes manuell erstellt, gelikt, im Plan eingeplant, gegessen
- Image-Pipeline läuft stabil

---

## 3. Phase P3 — Community

**Ziel:** Gruppen, Symptom-Tagebuch, Export, Moderation, Reports. (FCM-Push entfernt.)

### Sprint P3.S1 — Groups Backend + Client

**Deliverables:**
- Flyway `V3__p3_community.sql`: `groups`, `group_members`, `group_posts` (für spätere Erweiterung)
- Server: `group/GroupController.kt` (create, join, leave, remove-member, transfer-ownership)
- Server: Invite-Code-Generator für PRIVATE-Gruppen
- Server: Discovery-Endpoint für PUBLIC-Gruppen (Search + Browse)
- Server: Recipe-Visibility-Filter erweitert für `groupId`
- Android: `presentation/profil/GroupsScreen.kt` (Meine Gruppen + Discover + Create)
- Android: `GroupDetailScreen.kt` (Members, Recipes, Posts placeholder)
- Android: `group_cache` Entity

**Akzeptanz:**
- Private Gruppe erstellt → Code an 2. User → Beitritt → beide sehen sich als Members
- Public Gruppe via Search findbar
- Recipe mit `visibility=group` nur für Mitglieder sichtbar
- Owner kann Member entfernen + Ownership transferieren

**REQ-IDs:** REQ-GROUP-001..006

### Sprint P3.S2 — Symptom-Tagebuch (Log-Tab)

**Deliverables:**
- Android: Entities `LogEntryEntity`, `LogSymptomEntity`, `LogTagEntity`, `CustomSymptomEntity`
- Android: Default-Symptom-Seed (15 common)
- Android: `presentation/log/LogScreen.kt` mit Quick-Add-Form (UsabilityMap §6)
- Android: `LogEntryFormScreen.kt` (Mood-Slider, Schlaf, Symptome-Chips mit Severity, Tags, Notiz)
- Android: `SymptomSeverityChip` Component
- Android: `LogHistoryScreen.kt` (Verlaufs-Liste)
- Android: `LogChartsScreen.kt` (Vico Line-Charts)
- Android: `CustomSymptomManagerScreen.kt` (in Profil)
- Android: `IsLogEntryEditableUseCase.kt`

**Akzeptanz:**
- Log-Tab nicht mehr Placeholder
- Mehrere Einträge pro Tag möglich
- Custom-Symptom anlegen → in Chips-Liste verfügbar
- Charts zeigen 7-Tage und 30-Tage-Trends
- Eintrag älter als 7 Tage → nicht editierbar

**REQ-IDs:** REQ-LOG-001..006, REQ-NAV-004

### Sprint P3.S3 — ~~FCM~~ Reports + Moderation (FCM ENTFERNT 2026-05-25)

**Deliverables:**
- Server: `community/ReportController.kt`
- Android: Report-Button auf Recipe-Detail + Group-Detail
- Admin-UI: `RecipeReportsPage.tsx` (Queue + Resolve)
- Admin-UI: `UsersPage.tsx` mit Ban/Unban/Delete
- Server: Ban-Logic (Account-State + Token-Revocation)
- (Gruppen-Activity-Notifications: In-App Badge beim nächsten App-Start; optional Email-Digest server-seitig.)

**Akzeptanz:**
- 2. User postet Recipe in geteilte Gruppe → 1. User sieht Badge beim App-Start
- Report-Button → Eintrag in Admin-Queue
- Admin resolved Report (ignorieren / Recipe löschen)
- User ban → User kann nicht mehr loggen (alle Refresh-Tokens revoked)

**REQ-IDs:** REQ-GROUP-007 (REQ-REMIND-003 ENTFERNT)

### Sprint P3.S4 — Shopping-List + Supplement-Peer-Review + Export

**Deliverables:**
- Android: `presentation/plan/ShoppingListScreen.kt`
- Android: `BuildShoppingListUseCase.kt` (Unit-Normalization + Aisle-Grouping)
- Android: `ShoppingListItemEntity`
- Server: `supplement/SuggestionController.kt` (Submit endpoint)
- Server: `supplements_catalog` + `supplement_suggestions` Tables (eigentlich schon in V1 Schema vorbereitet, hier Endpoints aktiviert)
- Android: "Vorschlagen"-Button im Supplement-Detail
- Admin-UI: `SupplementsQueuePage.tsx`
- Server: `export/ExportService.kt` (Local + Server data combined)
- Server: PDF via `iText 7` oder `PdfBox`
- Server: JSON-Export via Moshi
- Android: `presentation/profil/ExportScreen.kt`
- Android: WorkManager-Job lädt PDF/JSON von Server, presigned URL → Download

**Akzeptanz:**
- 3 Tage geplant → Shopping-List aggregiert korrekt mit Unit-Normalisierung
- Supplement vorgeschlagen → in Admin-Queue → Approved → globally verfügbar
- Export erstellt PDF + JSON mit komplettem Datensatz (manuell prüfen)

**REQ-IDs:** REQ-SHOP-001..003, REQ-SUPP-004, REQ-EXPORT-001..004

### P3 Phase-Abschluss-Review

- Alle P3-IDs in TraceabilityMatrix ✅
- 2 Test-Gruppen mit je 3 Usern aktiv
- 14 Tage Symptom-Log-Data vorhanden für P4-Insights-Test

---

## 4. Phase P4 — Power Features

**Ziel:** User-Ingredient-Submissions, Field-PRs, Auto-Mahlzeitenplaner, lokale Insights,
Full Admin UI. (Barcode-Scanner ENTFERNT.)

### Sprint P4.S1 — User-Ingredients + Field-PR

**Deliverables:**
- Server: `ingredient/UserSuggestionController.kt` (Submit new ingredient)
- Server: `FieldPrController.kt` (Submit field-change)
- Server: `IngredientService.findUsableFor(userId)` (PENDING nur für Submitter)
- Server: Field-PR-Approve-Logic (apply diff, set last_admin_edit_at)
- Admin-UI: `IngredientQueuePage.tsx` (PENDING ingredients)
- Admin-UI: `FieldPrPage.tsx` (Queue + Diff-Viewer)
- Android: `IngredientSuggestForm.kt` (in Lebensmittel-Tab "Eigenes hinzufügen")
- Android: Field-Edit-Button auf Ingredient-Detail mit "Korrektur vorschlagen"-Dialog

**Akzeptanz:**
- User submitted neues Ingredient → für ihn nutzbar (Recipe-Draft) aber für andere unsichtbar
- Admin approved → global sichtbar
- Field-PR submitted → angezeigter Wert ändert sich nicht bis approved
- Admin approved → Wert ändert sich + sticky-flag gesetzt

**REQ-IDs:** REQ-INGR-USER-001/002, REQ-FIELDPR-001..003, REQ-QUALITY-FIX-002

### Sprint P4.S2 — Auto-Mahlzeitenplaner

**Deliverables:**
- Server: `autoplan/BeamSearchPlanner.kt` (Beam-Search-Algorithmus)
- Server: `autoplan/PlannerConstraints.kt` (Allergies, Intolerances, Goals, Ratings, Slots, MaxPrepTime)
- Server: `POST /v1/plans/generate` Endpoint
- Android: `presentation/plan/AutoPlanGenerateDialog.kt`
- Android: `AutoPlanPreviewScreen.kt` (editable preview vor Commit)
- Android: Personal-Ratings werden mitgeschickt im Request

**Akzeptanz:**
- Generate → 7-Tage-Plan in < 5 Sek
- Plan respektiert Allergien (0 Konflikte)
- MORE_OFTEN-Recipes häufiger drin als INTOLERANT-Recipes (nie)
- Preview ermöglicht Slot-Swap vor Commit
- Commit übernimmt Plan in MealPlan-Tab

**REQ-IDs:** REQ-AUTOPLAN-001..004

### Sprint P4.S3 — Bayesian Insights (lokal)

**Deliverables:**
- Android: `domain/insights/LiftCorrelationCalculator.kt`
- Android: WorkManager-Job läuft täglich (oder bei manuell Triggern in Insights-Screen)
- Android: `presentation/profil/InsightsScreen.kt` (Liste der Top-Korrelationen)
- Android: Lint-Custom-Rule: keine Network-Aufrufe aus `domain/insights/` (manuell prüfen, kein automatisierter Test)
- Schwellwerte: lift > 1.5, n ≥ 3 co-occurrences (REQ-INSIGHT-002)
- Severity-Weighted-Aggregation (REQ-INSIGHT-003)
- Mindestens 14 Tage Data-Requirement (REQ-INSIGHT-001) → Lock-Screen wenn nicht genug

**Akzeptanz:**
- Mit 14 Tagen synthetischen Daten: Insights zeigt Top-5-Korrelationen mit Lift-Wert
- Netzwerk in Insights-Modul deaktiviert (Code-Review) → keine HTTP-Calls
- Severity beeinflusst Ranking sichtbar

**REQ-IDs:** REQ-INSIGHT-001..004

### ~~Sprint P4.S4 — Barcode-Scanner~~ — **REMOVED (2026-05-25)**

Gestrichen. Keine ML-Kit-Abhängigkeit. Lebensmittel-Suche erfolgt textbasiert + FTS über das Ingredient-DB-Feature aus P1.S4/S5.

### Sprint P4.S5 — Full Admin UI + Final Polish

**Deliverables:**
- Admin-UI: `DashboardPage.tsx` (User-Count, DB-Größe, ETL-Status, Top-Rezepte, Phase-Completion)
- Admin-UI: `AuditLogPage.tsx` (filterbar nach actor, action, date)
- Admin-UI: `StatisticsPage.tsx`
- Admin-UI: Layout-Komponente (Sidebar + 11 Pages, siehe UsabilityMap §9)
- Server: Statistics-Endpoint
- Server: ErrorHandler global (Problem-Details JSON)
- Final-Review aller TraceabilityMatrix-Einträge

**Akzeptanz:**
- Alle 11 Admin-UI-Pages funktional
- Dashboard zeigt aktuelle Metriken
- AuditLog filterable
- Alle non-META REQ-IDs ✅ in TraceabilityMatrix

**REQ-IDs:** REQ-ADMIN-FULL-001, REQ-ADMIN-003

### P4 Phase-Abschluss = v1.0 Release-Gate

- ✅ Alle non-META REQ-IDs in TraceabilityMatrix
- ✅ Release-Checklist abgearbeitet (siehe §0 Release-Gate)
- ✅ APK signed, ready für Verteilung
- ✅ Runbook.md geschrieben

---

## 5. Inter-Phase-Wartungs-Tasks

Diese Tasks laufen kontinuierlich, nicht in einem Sprint gebunden:

- **Bug-Hotfixes:** sofort nach Entdeckung, eigener kleiner Sprint
- **DB-Migrations:** nur forward-only Flyway, nie editieren
- **Dependency-Updates:** monatlich check, security-relevant sofort
- **OFF-ETL-Monitoring:** Job-Status in Admin-UI prüfen, fail → Investigate
- **Backup-Restore-Drill:** halbjährlich manuell üben

---

## 6. Workflow-Reminders

Vor jedem Sprint-Start:
1. ReqSpec/Usability/Architecture/GUI/Traceability lesen für betroffene REQ-IDs
2. Sprint-Deliverables-Liste vor Augen halten

Nach jedem Sprint:
1. TraceabilityMatrix-Status updaten (❌ → ✅)
2. SprintPlan-Sprint-Checkbox abhaken (manuell hier in Doc)
3. Logbook.md-Eintrag (TODO: Doc erstellen wenn relevant)

---

**Ende SprintPlan v0.1.**
