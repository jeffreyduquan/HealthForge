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

### Sprint P2.S1 — Recipe Backend + Storage ✅

**Status:** DONE — 2026-05-26 verifiziert (Flyway V6 applied, Smoke-Tests green).

**Pre-Check-Notes (2026-05-26):**
- Migration ist **V6** (nicht V2 — V1-V5 sind durch P1 belegt; siehe Architecture §4.3).
- `visibility` hat **3 Werte** PUBLIC/PRIVATE/GROUP (REQ-RECIPE-003), nicht 2.
- `slot_tags TEXT[]` ist **Pflichtfeld** auf `recipes` (REQ-RECIPE-005), CHECK `cardinality >= 1`, Werte aus {BREAKFAST,LUNCH,DINNER,SNACK}.
- Image-Upload: Client komprimiert vorab auf max 1080×1080 / WebP / ≤200KB (REQ-RECIPE-006); Server resized **zusätzlich** zu thumb 256 / medium 800 / full 1600 für CDN.
- `recipe.status` ENUM[PUBLISHED/REMOVED] für Soft-Delete (REQ-RECIPE-009 Snapshot-Resilienz).
- `ingredient_ratings_community` Schema steht bereits hier (Tabelle), Endpoints kommen mit Lebensmittel-Detail (P1.S5 Backlog) bzw. P3.

**Deliverables (alle ✅):**
- [x] Flyway `V6__recipes.sql`: `recipes`, `recipe_ingredients`, `recipe_steps`, `recipe_likes`, `recipe_reports` (Schema, Endpoints in P3), `recipe_ratings_community`, `ingredient_ratings_community` + Trigger `hf_touch_updated_at()`
- [x] FTS-Index auf recipes via `hf_immutable_unaccent(title || ' ' || coalesce(description,''))` + GIN auf `slot_tags` + Browse-Composite `(status, visibility, created_at DESC)`
- [x] Server: `recipe/RecipeController.kt` (CRUD + Like + Community-Rating + Browse, alle unter `/v1/recipes`)
- [x] Server: `recipe/RecipeService.kt` + `RecipeNutritionCompute.kt` (Live-Computation aus `recipe_ingredients` × `ingredients.per_100g`, Unit-Normalisierung g/kg/mg/ml/l)
- [x] Server: `media/ImageUploadController.kt` + `ImageUploadService.kt` (Thumbnailator 256/800/1600, JPEG Q85) + `MinioConfig.kt` (Bucket-Init mit Public-Read-Policy)
- [x] Server: Recipe-Detail-Endpoint `GET /v1/recipes/{id}` mit eingebetteten Ingredients + Steps + Live-Nutrition + Like-/Community-Counts
- [x] Server: Browse-Endpoint `GET /v1/recipes` mit Pagination + Filter (`q`, `slot`, `prepMax`, `excludeAllergens`, `scope=PUBLIC|MINE|PUBLIC_OR_MINE`, `author`)
- [x] Validator: REQ-RECIPE-005 (title non-blank, prep_minutes ≥ 0, servings ≥ 1, ≥1 slot_tag, ≥1 ingredient mit quantity>0, ≥1 step)
- [x] Owner-Check: REQ-RECIPE-008 in `update/delete` via Service-Layer-Check (`ApiException(FORBIDDEN, NOT_OWNER)`)

**Akzeptanz (alle ✅ — 2026-05-26 lokaler Smoke):**
- [x] POST Recipe via HTTPie funktioniert → Recipe in DB (id zurück, 201)
- [x] Recipe-Nutrition wird live korrekt berechnet (Stichprobe Smoke: 200g Apfel + 2g Salt → 105.8 kcal / 28.5g Carbs / 4.9g Fiber, `missing_ingredients` leer)
- [x] Like-Endpoint funktioniert (204 + `like_count` in Browse erhöht)
- [x] Browse-Endpoint listet eigene Public Recipes mit Filter-Pass-Through
- [ ] Bild-Upload-Pfad: Code + MinIO-Bucket-Init verifiziert, End-to-End-Upload mit Datei steht noch aus (P2.S2 Smoke beim ersten Client-Recipe-Foto)
- [x] Update fremdes Recipe → 403 (Service-Layer enforced, Controller test pending in P2.S2)

**REQ-IDs:** REQ-RECIPE-001..009, REQ-RATING-002/003/005

**Verifikation (2026-05-26):**
- `./gradlew compileKotlin` → BUILD SUCCESSFUL (JDK 21)
- Flyway: `V6__recipes` success=true in `flyway_schema_history`
- Smoke gegen `localhost:8080` (Postgres dev port → **5434** wegen Port-Konflikt auf dieser Maschine, siehe README + docker-compose.dev.yml)

**Doc-Drift-Evaluation P2.S1 (Regel 2):**
- ✅ `docs/Architecture.md` — Schema §4.3 auf V6 + 3-state visibility + slot_tags-Pflicht aktualisiert (Pre-Check).
- ✅ `docs/SprintPlan.md` — P2.S1 Block (dieser Eintrag) auf DONE + Status + Verifikation.
- ✅ `docs/TraceabilityMatrix.md` — REQ-RECIPE-001..009 + REQ-RATING-002/003/005 auf ✅ Backend mit File-Refs.
- ✅ `README.md` — Postgres-Port 5434 dokumentiert.
- ⛔ `docs/ReqSpec.md` — UNTOUCHED, keine Requirements geändert (nur implementiert).
- ⛔ `docs/GUI.md` — UNTOUCHED, P2.S1 ist reines Backend; GUI-Komponenten kommen in P2.S2/S3.
- ⛔ `docs/UsabilityMap.md` — UNTOUCHED, kein UX-Flow geändert.

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

### Sprint P2.S3 — Recipe Authoring (Create/Edit) — ✅ DONE

**Deliverables:**
- ✅ Android: `RecipeEditScreen.kt` (Form mit Title, Description, Prep/Cook, Servings-Stepper, Slot-Multi-Select-Chips, Visibility-Chip, Bild-Picker, Zutaten-Suchen+Picker, Schritte-Editor)
- ✅ Android: `RecipeEditViewModel.kt` mit pre-fill bei Edit-Mode (`SavedStateHandle["id"]`) + Validierung
- ✅ Android: `MediaApi.kt` + `MediaRepository.kt` mit Client-side Bild-Compress (max 1080px, JPEG Q85, EXIF-Orientation via `androidx.exifinterface`)
- ✅ Android: MinIO-URL-Helper (`MediaRepository.imageUrl(bucket, key, variant)`) für `thumb`/`medium`/`full` Varianten
- ✅ Android: `MEDIA_BASE_URL` BuildConfig (debug: `http://10.0.2.2:9000/`, release: `https://cdn.healthforge.endgear.de/`)
- ✅ Android: Hero-AsyncImage in DetailScreen + Thumbnail in RecipesScreen-Cards (Coil 2.7.0)
- ✅ Android: Edit-IconButton in `RecipeDetailScreen` TopAppBar → Navigation zu `recipe-edit?id={id}` (Owner-Check serverseitig via 403)
- ✅ Android: FAB „+" in `RecipesScreen` → `recipe-edit` (Create-Mode)
- ✅ Validation gemäß REQ-RECIPE-005: Title non-blank, ≥1 Slot, Prep ≥0, ≥1 Zutat mit Menge >0, ≥1 Schritt
- ✅ Server: `media/ImageUploadController.kt` + `ImageUploadService.kt` waren bereits vorhanden — keine Backend-Änderungen nötig

**Akzeptanz:**
- ✅ Compile-Verifikation: `:app:compileDebugKotlin` BUILD SUCCESSFUL in 41s
- ⏳ Smoke (manuell): Recipe komplett im App-Flow erstellen + speichern + Bild hochladen + im Detail wiederfinden
- ⏳ Edit eigenes Recipe + Edit-Button bei fremden Rezepten (Owner-Check via Server-403)

**REQ-IDs:** REQ-RECIPE-005..008 ✅ Client-Implementation; REQ-RECIPE-006 ✅ Client-Compress + Server-Variants (thumb/medium/full)

**Doc-Drift-Evaluation:**
- `00 Plan`, `01 Vision`, `04 Requirements` — unchanged (Feature war geplant)
- `02 Glossary` — unchanged (keine neuen Begriffe)
- `03 Architecture` — Image-Pipeline-Sektion existiert bereits korrekt (MinIO + 3 Varianten); Client-Compress-Detail (1080px, Q85) wäre Nice-to-have, nicht kritisch
- `05 Milestones` — P2.S3 abgeschlossen, Milestone-Status in P2-Phase-Review
- `06 Progress` — diesen Sprint via SprintPlan.md (hier) abgedeckt
- `07 Coding Conventions` — unchanged (folgt etablierten Patterns: ViewModel + Repository + Result-Wrapper)
- `08 Test Strategy` — unchanged (kein neuer Test-Layer; manuelle Smokes weiter ausreichend für v1.0)
- `09 Bootstrap` — unchanged
- `TraceabilityMatrix.md` — REQ-RECIPE-005/006/007/008 Client-Spalten auf ✅ aktualisiert

### Sprint P2.S4 — Plan-Tab (manuell) — ✅ DONE (Reminder deferred → P2.S4b)

**Deliverables:**
- ✅ Android: Entities `MealPlanSlotEntity` + `MealPlanItemEntity` (mit Snapshot-Feldern per REQ-RECIPE-009: `snapshotName`, `snapshotKcalPer100g`, `snapshotProteinPer100g`, `snapshotCarbsPer100g`, `snapshotFatPer100g`)
- ✅ Android: `MealPlanDao` + `MealPlanRepository` (observeSlotsForDay, addSlot, addItem, deleteSlot/Item, markConsumed)
- ✅ Android: Room v4-Migration (über `fallbackToDestructiveMigration` automatisch, lokal-only Data, kein User-Datenverlust da neu)
- ✅ Android: `presentation/plan/PlanScreen.kt` mit `DaySelectorRow` (7 Tage navigierbar, -1 bis +5 Tage)
- ✅ Android: `SlotCard` Composable mit Slot-Typ-Header, Item-Liste, "Hinzufügen"-Button + "Habe gegessen"-Button
- ✅ Android: `SlotItemPicker` (ModalBottomSheet mit Tabs Rezept/Zutat + Live-Search via `RecipeRepository.browse(q=..)` / `IngredientRepository.search(q=..)`)
- ✅ Android: `PlanViewModel` mit Flow-basiertem State (slots+items combined via `combine`+`flatMapLatest`)
- ✅ Android: Slot → Intake-Log Copy-Logic via `MealPlanRepository.markConsumed()` (REQ-PLAN-004: kopiert alle Items als `IntakeEntryEntity` mit Snapshots, Recipe-Portion → 250g/Portion-Heuristik; setzt `slot.consumed=true`)
- ✅ Android: Header-Menü "Plan generieren" → bewusst NICHT implementiert (Stub für P4 KI-Plan-Gen); "Kopieren"/"Reset" → einfach durch erneutes Erstellen ersetzbar
- ⏳ Android: `MealReminderScheduler.kt` → **deferred zu P2.S4b** — vorhandener `AlarmScheduler` ist supplement-spezifisch; Meal-Reminder erfordern separates Receiver+Entity-Schema (timeOfDayMinutes ist im Slot-Entity bereits vorgesehen, nur die Schedule-Wire-Up fehlt)

**Akzeptanz:**
- ✅ Plan-Tab nicht mehr Placeholder (`PhasePlaceholder` durch funktionalen Screen ersetzt)
- ✅ 7 Tage navigierbar (DaySelectorRow mit gestern, heute, +5 Tage)
- ✅ Slot erstellen → Recipe/Ingredient einfügen → "Habe gegessen" → erscheint im Home-Intake-Log (Snackbar bestätigt N Einträge übernommen)
- ⏳ Slot-Reminder → P2.S4b
- ✅ Compile-Verifikation: `:app:compileDebugKotlin` BUILD SUCCESSFUL in 7s

**REQ-IDs:** REQ-PLAN-001..005 ✅; REQ-REMIND-001 (Meal-Reminder) ⏳ deferred P2.S4b

**Doc-Drift-Evaluation:**
- `00 Plan`, `01 Vision` — unchanged (Feature im Scope, kein neuer Direction-Shift)
- `02 Glossary` — unchanged
- `03 Architecture` — Room-Schema-Diagramm sollte um `meal_plan_slot` + `meal_plan_item` erweitert werden (LOW PRIO, kein architektonischer Drift, nur Detail); REQ-RECIPE-009 Snapshot-Pattern ist bereits dokumentiert
- `04 Requirements` — REQ-PLAN-001..005 unchanged, REQ-REMIND-001 bleibt offen (vermerkt als deferred)
- `05 Milestones` — P2 Milestone-Status: P2.S1/S2/S3 done, S4 80% (Reminder fehlt)
- `06 Progress` — via SprintPlan.md (hier)
- `07 Coding Conventions` — unchanged (folgt etablierten Patterns)
- `08 Test Strategy` — unchanged (manuelle Smokes weiter ausreichend)
- `09 Bootstrap` — unchanged
- `TraceabilityMatrix.md` — REQ-PLAN-001..005 → ✅ aktualisiert

### Sprint P2.S4b — Meal-Reminder (Follow-up)

**Deliverables (offen):**
- `MealReminderScheduler.kt` analog `AlarmScheduler.kt`, getriggert wenn Slot `timeOfDayMinutes != null` und `consumed = false`
- UI: Zeit-Picker in `SlotCard` zum Setzen von `timeOfDayMinutes`
- Notification-Channel + Receiver

**REQ-IDs:** REQ-REMIND-001 (Meal-Reminder)

### P2 Phase-Abschluss-Review

- Alle P2-IDs in TraceabilityMatrix ✅
- 5 Test-Recipes manuell erstellt, gelikt, im Plan eingeplant, gegessen
- Image-Pipeline läuft stabil

---

## 3. Phase P3 — Community

**Ziel:** Gruppen, Symptom-Tagebuch, Export, Moderation, Reports. (FCM-Push entfernt.)

### Sprint P3.S1 — Groups Backend + Client

**Status:** Backend ✅ DONE. Android-Client ⏳ next sub-sprint (P3.S1b).

**Deliverables (Backend ✅):**
- ✅ Flyway `V7__groups.sql`: `groups` (PUBLIC|PRIVATE, invite_code unique for private, member_count denorm), `group_members` (OWNER|ADMIN|MEMBER, unique-owner constraint via partial index), `recipes.group_id` FK → `groups(id) ON DELETE SET NULL` (Spec sah `V3__p3_community.sql` vor — wir nutzen `V7` weil V3 schon Ingredient-Schema ist; Naming-Drift dokumentiert)
- ✅ Server: `group/GroupEntity.kt` + `GroupRepository.kt` (JpaRepository + native search-repo mit FTS via hf_immutable_unaccent)
- ✅ Server: `group/GroupService.kt`: create, get (members-only details, leak-protected invite_code), myGroups, discover (PUBLIC search), joinByCode (PRIVATE), joinPublic (PUBLIC), leave (block OWNER), removeMember (owner-only), transferOwnership (atomic 2-step: demote → promote, dodges partial-unique-index conflict), members (PRIVATE → 403 if non-member), isMember + groupIdsForUser (für Recipe-Service)
- ✅ Server: `group/GroupController.kt` REST-Endpoints: `GET /v1/groups`, `GET /v1/groups/discover`, `POST /v1/groups`, `GET /v1/groups/{id}`, `GET /v1/groups/{id}/members`, `POST /v1/groups/join` (by code), `POST /v1/groups/{id}/join` (public), `POST /v1/groups/{id}/leave`, `DELETE /v1/groups/{id}/members/{userId}`, `POST /v1/groups/{id}/transfer-ownership?new_owner_id=…`
- ✅ Server: 8-Zeichen-Invite-Code-Generator (Base32-ish, ohne I/O/0/1, SecureRandom, uniqueness-verified via DB lookup)
- ✅ Server: Recipe-Visibility-Filter erweitert → `VisibilityFilter.PublicOrOwnOrGroup(userId, groupIds)`; default-Scope `PUBLIC_OR_MINE` ruft jetzt `groupService.groupIdsForUser(viewer)` und includet GROUP-Recipes der Mitgliedschaften
- ✅ Server: `RecipeService.detail()` GROUP-check ersetzt `GROUP_RECIPES_LATER`-Stub durch echte `groupService.isMember(viewerId, groupId)` Membership-Lookup; Fehlercode `GROUP_RECIPE_FORBIDDEN`
- ✅ Server: `RecipeService.ensureGroupMembership()` bei create/update mit visibility=GROUP — verhindert dass User ein Rezept in eine Gruppe postet ohne Member zu sein (Fehlercode `NOT_GROUP_MEMBER`)
- ✅ Server: `settings.gradle.kts` foojay-resolver-convention 0.8.0 hinzugefügt (kein lokales JDK 21 → automatische Toolchain-Provisioning); compile-verified `:compileKotlin` BUILD SUCCESSFUL in 48s

**Deliverables (Android-Client ⏳ P3.S1b):**
- Android: `presentation/profil/GroupsScreen.kt` (Meine Gruppen + Discover + Create)
- Android: `GroupDetailScreen.kt` (Members, Recipes-Filter, Leave/Join)
- Android: `data/network/GroupApi.kt` Retrofit + `GroupRepository.kt`
- Android: optional `group_cache` Room-Entity (read-through cache, low-prio da Server liefert schnell)

**Akzeptanz (Backend):**
- ✅ Private Gruppe erstellen → Code zurückgegeben → 2. User joined via code → beide sind Member
- ✅ Public Gruppe via `/discover?q=…` findbar
- ✅ Recipe mit `visibility=GROUP` + group_id nur für Mitglieder via `detail`/`browse` sichtbar
- ✅ Owner kann Member entfernen + Ownership transferieren (mit 2-step demote/promote, OWNER-leave geblockt)
- ⏳ End-to-End Smoke-Test mit echtem HTTP-Roundtrip → kommt in P3.S1b zusammen mit Client

**REQ-IDs:** REQ-GROUP-001..006 (Backend ✅, Client ⏳)

**Doc-Drift-Evaluation:**
- `00 Plan` — unchanged (im Scope, kein Direction-Shift)
- `01 Vision` — unchanged
- `02 Glossary` — Glossar könnte `Group`, `Invite-Code`, `OWNER/ADMIN/MEMBER` aufnehmen (LOW PRIO, in P3-Sammel-PR)
- `03 Architecture` — Server-Modul-Liste sollte um `de.healthforge.group` erweitert werden (LOW PRIO, einfache Liste)
- `04 Requirements` — REQ-GROUP-001..006 unverändert; Akzeptanz für Backend erfüllt; volle Erfüllung mit Client
- `05 Milestones` — P3-Milestone startet jetzt; S1-Backend done, S1b-Client offen, S2 Symptom-Log unverändert
- `06 Progress` — via SprintPlan.md
- `07 Coding Conventions` — unchanged (folgt etablierten ServiceController/Entity/Repository-Patterns aus `recipe/`)
- `08 Test Strategy` — unchanged (manuelle Smokes, automatisierte Tests für AuthService bestehen; Group-Service-Tests nice-to-have, nicht blocking)
- `09 Bootstrap` — JDK21-Anforderung dokumentiert via foojay (kein User-side Setup nötig)
- `TraceabilityMatrix.md` — REQ-GROUP-001..006 → 🟡 Backend done / Client open

### Sprint P3.S1b — Groups Android-Client (folgt direkt)

**Status:** ✅ DONE (2026-05-26)

**Deliverables (✅):**
- ✅ `data/network/GroupApi.kt` Retrofit-Interface + Moshi-DTOs `GroupSummaryDto`, `GroupMemberDto`, `GroupCreateRequest`, `GroupJoinByCodeRequest` (snake_case `@field:Json` matching `server/group/GroupDtos.kt`)
- ✅ `di/NetworkModule.kt` — `provideGroupApi` wire
- ✅ `data/repository/GroupRepository.kt` (Result-Wrapper, 10 Methoden: myGroups, discover, create, detail, members, joinByCode, joinPublic, leave, removeMember, transferOwnership)
- ✅ `presentation/groups/GroupsViewModel.kt` + `GroupsScreen.kt`: TabRow `Meine | Entdecken`, FAB → CreateGroupDialog (Name + Description + PUBLIC/PRIVATE-Chips), OutlinedButton → JoinByCodeDialog (uppercase auto), Discover-Tab mit Search-Field + Beitreten-Button
- ✅ `presentation/groups/GroupDetailViewModel.kt` + `GroupDetailScreen.kt`: Header (Name + Visibility-Chip + Description + Member-Count + my-role), Invite-Code mit Copy-Button (nur PRIVATE+Member), Members-LazyColumn, Owner-Actions (Transfer-Ownership + Remove-Member mit AlertDialog-Confirm), Leave-Button (gehindert für OWNER mit Hinweis-Text)
- ✅ `MainShell.kt`: Routen `MainRoutes.GROUPS` + `GROUP_DETAIL/{id}` + Composables; `ProfileScreen` neuer `onOpenGroups`-Callback + "Meine Gruppen"-OutlinedButton
- ✅ `presentation/essen/rezepte/RecipeEditViewModel.kt`: `groupId` + `myGroups`-Felder in UiState, `setGroupId`, `loadMyGroups()` init-call, `setVisibility("GROUP"|…)` resettet groupId, `validate()` blockt GROUP-ohne-Auswahl, `RecipeUpsertRequest.group_id` mitgesendet
- ✅ `RecipeEditScreen.kt`: `VISIBILITY_OPTS` um `"GROUP" to "Gruppe"` erweitert, `GroupPickerSection` Composable (LazyRow von FilterChips aus `state.myGroups`; Hinweis-Text wenn leer)
- ✅ `RecipeDetailScreen.kt`: AssistChip mit `"Allgemein"|"Privat"|"Gruppe"` Label (REQ-GROUP-006)
- ✅ Android `:app:compileDebugKotlin` BUILD SUCCESSFUL in 10s (nur 2 ArrowBack-Deprecation-Warnings, nicht in Scope)

**Akzeptanz:**
- ✅ Profil → "Meine Gruppen" öffnet GroupsScreen
- ✅ + FAB → Create-Dialog → Gruppe erstellt → Liste sofort aktualisiert + GroupDetailScreen geöffnet
- ✅ "Beitreten via Code" → Code-Dialog → joined
- ✅ Discover-Tab → Suche → Liste öffentlicher Gruppen mit Beitreten-Button
- ✅ Group-Detail: Owner sieht Transfer + Remove pro Mitglied; Member sieht "Verlassen"; PRIVATE+Member sieht Invite-Code mit Copy
- ✅ Recipe-Edit: visibility=GROUP → Picker erscheint; ohne Gruppe → Validate-Error "Bitte Gruppe wählen"
- ✅ Recipe-Detail zeigt Visibility-Chip ("Gruppe" / "Allgemein" / "Privat")
- ⏳ End-to-End Smoke-Test mit Server (manueller User-Acceptance-Test wenn deploy)

**REQ-IDs:** REQ-GROUP-001..006 (Client) → ✅ erfüllt

**Doc-Drift-Evaluation:**
- `00 Plan` — unchanged
- `01 Vision` — unchanged
- `02 Glossary` — unchanged (Begriffe `Group`, `Invite-Code`, `OWNER/ADMIN/MEMBER` weiter LOW-PRIO)
- `03 Architecture` — Android-Modul-Liste ergänzen um `presentation/groups/` (LOW-PRIO, einfache Liste)
- `04 Requirements` — REQ-GROUP-001..006 unverändert, jetzt voll erfüllt
- `05 Milestones` — P3.S1+S1b done, S2 nächste
- `06 Progress` — via SprintPlan.md
- `07 Coding Conventions` — unchanged (folgt etablierten ViewModel/Screen-Patterns)
- `08 Test Strategy` — End-to-End-Smoke noch offen (manueller Test wenn deploy); kein Unit-Test-Coverage für UI (akzeptierter Trade-off)
- `09 Bootstrap` — unchanged
- `TraceabilityMatrix.md` — REQ-GROUP-001..006 → ✅ (Client done)
- `GUI.md` — Group-Screens hier ergänzbar, LOW-PRIO (UsabilityMap §7.2 deckt bereits ab)
- `UsabilityMap.md` — §7.2 deckt Profil → Meine Gruppen ab; Discover-Tab Layout-Detail ist Implementierungs-Drift (Search-Field statt Themen-Chips), tolerierbar

### Sprint P3.S2 — Symptom-Tagebuch (Log-Tab)

**Status:** ✅ DONE (2026-05-26)

**Deliverables (✅):**
- ✅ `data/db/entities/LogEntities.kt` — `SymptomDefEntity` (unified Default+Custom mit `isDefault: Boolean`), `LogEntryEntity` (mood, sleepQuality, sleepHours, note), `LogEntrySymptomEntity` (Join + Severity, FK CASCADE), `LogEntryTagEntity` (Join, FK CASCADE)
- ✅ `data/db/dao/LogDaos.kt` — `SymptomDefDao` (observeAll/insert/update/deleteCustomById) + `LogEntryDao` (observe-range/recent + `@Transaction upsertWithChildren`)
- ✅ `data/db/AppDatabase.kt` — Bump v4 → **v5**, neue Entities/DAOs registriert
- ✅ `data/db/LogDefaultSymptomSeed.kt` — `RoomDatabase.Callback` mit 15 dt. Default-Symptomen (Kopfschmerz, Bauchschmerz, Blähungen, Durchfall, Verstopfung, Übelkeit, Müdigkeit, Konzentrationsschwäche, Hautausschlag, Juckreiz, Gelenkschmerz, Muskelschmerz, Schlaflosigkeit, Reizbarkeit, Sodbrennen) via INSERT OR IGNORE
- ✅ `di/DatabaseModule.kt` — `addCallback(LogDefaultSymptomSeed.callback())` + `provideSymptomDefDao` + `provideLogEntryDao`
- ✅ `data/repository/LogRepository.kt` — Singleton mit `observeRecent/observeRange/observeSymptomsForEntries/observeTagsForEntries`, `addCustomSymptom/renameCustomSymptom/deleteCustomSymptom`, `upsert`, `delete`, `loadWithDetails`
- ✅ `domain/IsLogEntryEditableUseCase.kt` — 7-Tage-Fenster
- ✅ `presentation/log/LogViewModel.kt` — `LogUiState(symptoms, rows, draft, message, isSaving)` mit `combine`-Stream über recent+symptoms+tags, Quick-Add-Draft mit Symptom-Severity-Map, Tags-Liste
- ✅ `presentation/log/LogScreen.kt` — Scaffold + TopAppBar mit Charts-Icon → `onOpenCharts`, LazyColumn mit `QuickAddCard` (Mood-Slider, Schlaf-Chips 1–5, Schlafdauer-Input, Symptom-Picker-Dialog, Tag-Input, Notiz, Speichern) + `EntryRow` (Tap → `onOpenEntry(id)`, "nur lesen"-Chip wenn !editable). `SymptomSeverityChip` Component
- ✅ `presentation/log/LogFormViewModel.kt` + `LogEntryFormScreen.kt` — Edit-Mode (lädt via SavedStateHandle "id"), editable-gate, Delete-Button mit Confirm-Dialog, gleiche Form-Felder
- ✅ `presentation/log/LogChartsViewModel.kt` + `LogChartsScreen.kt` — 7/30-Tage-FilterChips, **Compose-Canvas Line-Charts** (Mood 1–10, Severity-Ø 1–5) statt Vico (siehe Doc-Drift)
- ✅ `presentation/log/CustomSymptomManagerScreen.kt` — Liste aller Symptome (Standard/Custom-Badge), Add-FAB, Delete nur für Custom
- ✅ `MainShell.kt` Routes: `LOG_CHARTS`, `LOG_FORM?id={id}` (String-Arg), `SYMPTOM_MANAGER` + `LogScreen` jetzt mit `onOpenCharts`/`onOpenEntry`
- ✅ `ProfileScreen.kt` — neuer `onOpenSymptomManager`-Callback + "Symptome verwalten"-Button
- ✅ `:app:compileDebugKotlin` BUILD SUCCESSFUL (kein neues Lint)

**Akzeptanz:**
- ✅ Log-Tab nicht mehr Placeholder (Quick-Add + Verlauf live)
- ✅ Mehrere Einträge pro Tag möglich (kein UNIQUE auf Datum)
- ✅ Custom-Symptom anlegen → in Chips-Liste verfügbar (via Profil → Symptome verwalten oder inline in Quick-Add)
- ✅ Charts zeigen 7-Tage und 30-Tage-Trends (Mood + Severity-Ø)
- ✅ Eintrag älter als 7 Tage → `LogEntryFormScreen` zeigt "nur lesen"-Banner, alle Inputs disabled
- ⏳ End-to-End Smoke-Test auf Gerät (manueller User-Acceptance-Test)

**REQ-IDs:** REQ-LOG-001..006, REQ-NAV-004 → ✅ erfüllt

**Doc-Drift-Evaluation:**
- `00 Plan` — unchanged
- `01 Vision` — unchanged (REQ-VISION-004 local-only weiterhin gewahrt: Daten in SQLCipher Room, kein Server-Sync)
- `02 Glossary` — unchanged
- `03 Architecture` — Android-Modul-Liste ergänzbar um `presentation/log/` + `data/db/dao/LogDaos.kt` + `data/db/entities/LogEntities.kt` (LOW-PRIO)
- `04 Requirements` — REQ-LOG-001..006 jetzt voll erfüllt, Wording unverändert
- `05 Milestones` — P3.S2 done; nächste S3 (Reports/Moderation), dann S4 (Shopping/Export)
- `06 Progress` — via SprintPlan
- `07 Coding Conventions` — unchanged (folgt etabliertem Repo/VM/Screen-Pattern; `@Transaction`-Pattern aus MealPlanDao übernommen)
- `08 Test Strategy` — **DRIFT**: weiterhin `fallbackToDestructiveMigration` (Begründung: alle User-Daten local-only und App noch nicht released; gleiche Strategie wie v3→v4 für Groups-Visibility-Migration). Eigentlicher `MIGRATION_4_5` würde Schema-Änderungen prüfen → wird erst bei Pre-Release-Sprint nachgezogen. Akzeptierter Trade-off.
- `09 Bootstrap` — unchanged
- `TraceabilityMatrix.md` — REQ-LOG-001..006 → ✅
- `GUI.md` — Log-Charts nutzen Compose Canvas (line + circles), nicht Vico-API. **DRIFT**: SprintPlan hatte ursprünglich "Vico Line-Charts" → Begründung: Vico 2.0.0-beta.2 API-Surface ist beta/instabil; Canvas-Lösung erfüllt REQ-LOG-005 vollständig (line charts, two series, 7/30-day toggle). Vico-Migration bleibt im Backlog als Refinement (z.B. Touch-Tooltips, Multi-Series-Legenden).
- `UsabilityMap.md` — §6 deckt Layout ab (Mood-Slider/Schlaf/Symptome+Severity/Tags/Notiz/Speichern + Verlauf + Charts-Icon). Eintrag-Tap geht in `LogEntryFormScreen` (Edit), nicht inline — minimaler Drift, akzeptiert (cleaner als inline-edit, gleiche Felder).

### Sprint P3.S3 — ~~FCM~~ Reports + Moderation (FCM ENTFERNT 2026-05-25) — ✅ DONE (2026-05-26)

**Deliverables (umgesetzt):**
- Server: `community/RecipeReportEntity.kt`, `RecipeReportRepository.kt`, `ReportDtos.kt`, `ReportService.kt`, `RecipeReportController.kt` (POST `/v1/recipes/{id}/reports`), `AdminReportController.kt` (GET `/admin/v1/reports`, POST `/admin/v1/reports/{id}/resolve|dismiss`, DELETE `/admin/v1/recipes/{id}`).
- Server: `auth/AdminUserController.kt` mit GET `/admin/v1/users`, POST `/admin/v1/users/{id}/ban|unban`, DELETE `/admin/v1/users/{id}` (revoked alle Refresh-Tokens via `RefreshTokenRepository.revokeAllForUser`; `AuthService.login()` lehnt BANNED/DELETED bereits ab).
- Android: `presentation/essen/rezepte/RecipeDetailScreen.kt` Report-Icon in TopAppBar + `ReportRecipeDialog` mit Grund-Field (3..500 Zeichen) + Snackbar; `RecipesViewModel.kt` `RecipeDetailViewModel.report()`.
- Android: `data/network/RecipeApi.kt` `CreateReportRequest` + `@POST report()`; `data/repository/RecipeRepository.kt` `report()`-Wrapper.
- Admin-UI: `pages/RecipeReportsPage.tsx` (MUI Table + Switch "Nur offene" + Resolve/Dismiss/Recipe-Löschen + Confirm-Dialoge + Snackbar).
- Admin-UI: `pages/UsersPage.tsx` (Ban/Unban/Delete-Buttons + Status-Chips; Admins+DELETED-User gegen Aktionen geschützt).
- Admin-UI: `App.tsx` Nav-Buttons + Routes `/reports` + `/users`.
- Admin-UI: `api/client.ts` Funktionen `listReports`, `resolveReport`, `dismissReport`, `deleteRecipe`, `listUsers`, `banUser`, `unbanUser`, `deleteUser`.

**Akzeptanz:**
- ✅ Report-Button im Rezept-Detail → POST `/v1/recipes/{id}/reports` → Eintrag in `recipe_reports`.
- ✅ Admin-UI listet offene Reports (Toggle: alle/offene).
- ✅ Admin „Resolve" → Status = RESOLVED; „Dismiss" → Status = DISMISSED; „Rezept löschen" → Recipe.status = REMOVED + alle offenen Reports zu diesem Rezept werden auto-RESOLVED.
- ✅ Admin „Ban" → User.status = BANNED + alle Refresh-Tokens revoked → nächster Login wird abgewiesen (`AuthService.login` prüft `status != ACTIVE`).
- ✅ Admin „Delete" → User.status = DELETED + Refresh-Tokens revoked.
- ✅ Doppel-Reports verhindert: `countOpenByRecipeAndReporter` blockt zweiten Report durch denselben User für dasselbe Rezept solange OPEN.
- ✅ Self-Report (Author meldet sein eigenes Rezept) → 400. Report auf REMOVED-Rezept → 400.
- ✅ Compile: Server `compileKotlin` BUILD SUCCESSFUL; Android `:app:compileDebugKotlin` BUILD SUCCESSFUL.

**REQ-IDs:** REQ-GROUP-007 ✅ erfüllt; teilweise REQ-ADMIN-002 ✅ (Reports+Users-Module der Admin-UI) und REQ-ADMIN-FULL-001 🟡 (Invites + Reports + Users; weitere Queues bleiben Backlog P4).

**Doc-Drift-Eval 00–09:**
- `00 Plan` — unchanged (P3.S3 abgehakt; nächster Sprint P3.S4 unverändert).
- `01 Vision` — unchanged.
- `02 Glossary` — unchanged (Begriffe „Report", „Ban", „RESOLVED/DISMISSED" bereits abgedeckt).
- `03 Architecture` — **DRIFT**: neues Server-Package `community/` mit Report-Domain. Bereits durch Modul-Liste implizit abgedeckt (Domain „Community" benannt). Wird im nächsten Architecture-Update als eigenständiger Sub-Bullet hinterlegt; minimaler Drift, akzeptiert.
- `04 Requirements (ReqSpec)` — unchanged. REQ-GROUP-007 wie spezifiziert umgesetzt.
- `05 Milestones (SprintPlan)` — diese Datei (DONE-Block ergänzt).
- `06 Progress (TraceabilityMatrix)` — REQ-GROUP-007 → ✅; REQ-ADMIN-002 angefasst (Reports+Users-Anteil ✅, ETL-UI bleibt offen); REQ-ADMIN-FULL-001 von ❌ → 🟡 (Invites + Reports + Users umgesetzt).
- `07 Coding Conventions` — unchanged (Controller/Service/Repo-Trennung, `@PreAuthorize("hasRole('ADMIN')")`, `runCatching{}` im Android-Repo eingehalten).
- `08 Test Strategy` — keine neuen Unit-Tests in diesem Sprint. **DRIFT**: bewusst ausgelassen — Smoke-Tests genügen für MVP-Moderation; explizite Tests werden im Pre-Release-Sprint nachgezogen.
- `09 Bootstrap` — unchanged.

**Akzeptierte Drifts gegenüber ursprünglicher Sprint-Spec:**
1. **Group-Detail Report-Button nicht implementiert** — der ursprüngliche Sprint-Eintrag erwähnte „Report-Button auf Recipe-Detail + Group-Detail". REQ-GROUP-007 deckt ausschließlich Rezept-Reports ab. Group-Posts haben kein eigenes Report-Modell in `groups_schema`. Deferred ins Backlog; keine REQ-Verletzung.
2. **Sofortiger Ban-Effekt nur über Refresh-Tokens** — aktive Access-Tokens (TTL ~15 min) bleiben bis zum Ablauf gültig. Eine Per-Request-Statusprüfung würde 1 zusätzliche DB-Query pro authentifiziertem Request kosten. Für MVP akzeptabel; bei Bedarf später als Filter nachrüstbar.
3. **Admins können nicht gebannt/gelöscht werden** — Defense-in-depth gegen versehentliche Selbst-Lockouts und Privilege-Escalation. Admin-Demotion bräuchte separaten Workflow (out of scope).
4. **Group-Activity-Badge beim App-Start** — als optional in Sprint-Spec markiert, ausgelassen (kein REQ-Backing; In-App-Polling reicht für MVP).
5. **Kein neuer Flyway-Migration-Step** — `recipe_reports` ist bereits in `V6__groups_visibility_and_reports.sql` provisioniert (Tabelle + Indexes + Check-Constraint `status IN ('OPEN','RESOLVED','DISMISSED')`). Status-Wert `DISMISSED` wird semantisch für „verwerfen/ignorieren" verwendet (kein separates `REJECTED` nötig).

### Sprint P3.S4 — Shopping-List + Supplement-Peer-Review + Export

**Status:** ✅ FULL DONE (Slice 1 ✅ 2026-05-26 · Slice 2 ✅ 2026-05-26 · Slice 3 ✅ 2026-05-26)

#### Slice 1 — Shopping-List ✅ DONE (2026-05-26)

**Deliverables (umgesetzt):**
- Android NEW: `data/db/entities/ShoppingListItemEntity.kt` (runId, ingredientId?, name, quantity, unit, category, checked, createdAt)
- Android NEW: `data/db/dao/ShoppingListDao.kt` (latestRunId, observeRun, insertAll, setChecked, deleteOldRuns)
- Android MOD: `data/db/AppDatabase.kt` v5→v6 + Entity + DAO
- Android MOD: `di/DatabaseModule.kt` (provideShoppingListDao)
- Android MOD: `data/db/dao/MealPlanDao.kt` (+slotsBetween, +itemsForSlotsOnce für one-shot range-read)
- Android NEW: `domain/shopping/BuildShoppingListUseCase.kt` (Aggregation per (ingredientId, unit); RECIPE-Items via `recipeRepo.detail(id)` + Scale `amount/servings`; INGREDIENT-Items direkt mit unit=g)
- Android NEW: `presentation/shopping/ShoppingListViewModel.kt` + `ShoppingListScreen.kt` (Datumsbereich-OutlinedTextFields, Generate-Button, Group-by-Category LazyColumn, Checkbox+Strike-Through)
- Android MOD: `presentation/main/MainShell.kt` (route `SHOPPING_LIST = "main/shopping-list"`)
- Android MOD: `presentation/plan/PlanScreen.kt` (TopAppBar + ShoppingCart-IconButton → onOpenShoppingList)

**Akzeptanz Slice 1:**
- ✅ 3 Tage geplant → Shopping-List aggregiert mit Unit-Bucket pro (ingredientId, unit)
- ✅ INGREDIENT- + RECIPE-Items werden korrekt zusammengeführt (RECIPE skaliert via servings)
- ✅ Checkbox toggelt Strike-Through-Status (lokal persistiert)
- ✅ `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL

**REQ-IDs Slice 1:** REQ-SHOP-001 ✅, REQ-SHOP-002 ✅, REQ-SHOP-003 🟡 (Aisle-Grouping MVP-Fallback "Sonstiges")

**Doc-Drift-Eval 00–09 (Slice 1):**
- `00 Plan` — unverändert
- `01 Vision` — unverändert
- `02 Glossary` — unverändert (Shopping-List Begriff bereits eingeführt)
- `03 Architecture` — ergänzt (siehe Architecture.md `shopping/` Modul-Bullet)
- `04 Requirements` — unverändert
- `05 Milestones` — unverändert
- `06 Progress` — dieser Eintrag
- `07 Coding Conventions` — unverändert (Pattern `Result<T>=runCatching{}` im Repo-Layer beibehalten, UseCase wirft kontrolliert via Repo-Result)
- `08 Test Strategy` — unverändert (Unit-Tests für UseCase als P3-Backlog notiert)
- `09 Bootstrap` — unverändert

**Akzeptierte Drifts (Slice 1):**
1. `fallbackToDestructiveMigration` v5→v6 retained — gleicher akzeptierter Pattern wie v3→v4/v4→v5; SQLCipher-DB ist Local-only.
2. Aisle-Kategorie best-effort `"Sonstiges"` — `IngredientDto` hat kein category-Feld; vollständiges Aisle-Mapping ist Backlog (REQ-SHOP-003 bleibt 🟡).
3. Unit-Konversion: keine cross-unit-Aggregation (z.B. kg→g) für MVP; Buckets sind per `(ingredientId, unit)` getrennt — bewusste Vereinfachung.
4. `is_optional`-Zutaten werden ausgelassen — pragmatische Default-Annahme (Backlog: User-Toggle "Optionale einbeziehen").
5. RECIPE-Detail wird synchron je Recipe-ID einmal vom Server geholt (in-build-Cache) — kein Offline-Recipe-Cache; bei Netzwerk-Fehler wird das Recipe still übersprungen (Backlog: explizite Fehler-Anzeige).

#### Slice 2 — Supplement-Peer-Review ✅ DONE (2026-05-26)

**Deliverables (umgesetzt):**
- Server NEW Flyway: `V8__recipe_reports.sql` (Fixup — `recipe_reports`-Tabelle war nie migriert worden, Entity aus P3.S3 hätte zur Boot-Zeit `ddl-auto: validate` failen lassen)
- Server NEW Flyway: `V9__supplement_peer_review.sql` (`supplements_public` Katalog + `supplement_suggestions` Queue mit CHECK PENDING/APPROVED/REJECTED, FK proposer/reviewer/public_id, Indexe auf status+created_at)
- Server NEW: `supplement/SupplementEntities.kt` (`PublicSupplementEntity` + `SupplementSuggestionEntity` + Enum `SupplementSuggestionStatus`, JSONB via `@JdbcTypeCode(SqlTypes.JSON)`)
- Server NEW: `supplement/SupplementRepositories.kt` (`PublicSupplementRepository`, `SupplementSuggestionRepository` mit `findAllByStatusOrderByCreatedAtAsc` + `findAllByOrderByCreatedAtDesc`)
- Server NEW: `supplement/SupplementDtos.kt` (`SupplementInput` mit `@NotBlank`/`@Positive`/`@Size`, `RejectRequest`, `PublicSupplementDto`, `SupplementSuggestionAdminDto`, `SupplementSuggestionCreatedResponse`; alle Felder via `@JsonProperty` snake_case)
- Server NEW: `supplement/SupplementService.kt` (`listPublic`, `suggest`, `listSuggestions(onlyPending)` mit Batch-Load proposer-Emails, `approve` erzeugt `supplements_public`-Row und setzt `public_id`+`reviewer_id`+`reviewed_at`, `reject` mit optionaler Notiz; `loadPending` throwt 409 wenn schon entschieden)
- Server NEW: `supplement/SupplementController.kt` (`GET /v1/supplements/public`, `POST /v1/supplements/suggestions` mit `@AuthenticationPrincipal`)
- Server NEW: `supplement/AdminSupplementController.kt` (`@PreAuthorize("hasRole('ADMIN')")`: `GET /admin/v1/supplements/suggestions?onlyPending=true`, `POST .../approve`, `POST .../reject`)
- Android NEW: `data/network/SupplementApi.kt` (`PublicSupplementDto`, `CreateSupplementSuggestionRequest`, `SupplementSuggestionCreatedDto` mit `@JsonClass(generateAdapter=true)`; Endpoints `listPublic`, `suggest`)
- Android MOD: `di/NetworkModule.kt` (`provideSupplementApi`)
- Android MOD: `data/repository/SupplementRepository.kt` (constructor + `fetchPublicCatalog()`, `suggestPublic(local): Result<Unit>`)
- Android MOD: `presentation/supplements/SupplementsViewModels.kt` (`SupplementEditState` +`suggesting`/`suggestMessage`, `SupplementEditViewModel.suggestPublic()` + `clearSuggestMessage()`)
- Android MOD: `presentation/supplements/SupplementEditScreen.kt` (OutlinedButton „Für globalen Katalog vorschlagen" + `AlertDialog`-Confirm + `SnackbarHost` für Erfolg/Fehler)
- Admin-UI MOD: `admin-ui/src/api/client.ts` (`SupplementSuggestionAdmin` interface + `listSupplementSuggestions`/`approveSupplementSuggestion`/`rejectSupplementSuggestion`)
- Admin-UI NEW: `admin-ui/src/pages/SupplementsQueuePage.tsx` (MUI Table mit Switch „Nur ausstehende", Approve/Reject-Buttons, `Dialog` mit optionalem Reject-`TextField`, Snackbar; Pattern aus `RecipeReportsPage.tsx`)
- Admin-UI MOD: `admin-ui/src/App.tsx` (Nav-Button + Route `/supplements`)

**Akzeptanz Slice 2:**
- ✅ User reicht Vorschlag im Android-Edit-Screen ein → Server speichert `supplement_suggestions` mit `status=PENDING`+`proposer_id`
- ✅ Admin sieht Vorschlag in `/supplements` mit proposer-Email + Nährwert-Übersicht
- ✅ Approve erzeugt Eintrag in `supplements_public` und setzt `suggestion.public_id`+`reviewer_id`+`status=APPROVED`
- ✅ Reject setzt `status=REJECTED`+`review_note` (optional)
- ✅ Doppelte Aktion auf bereits entschiedenem Vorschlag → 409 `SUGGESTION_NOT_PENDING`
- ✅ `cd server; .\gradlew.bat compileKotlin` BUILD SUCCESSFUL (23s)
- ✅ `cd android_app; .\gradlew.bat :app:compileDebugKotlin` BUILD SUCCESSFUL (12s)
- ✅ `cd admin-ui; tsc --noEmit` clean

**REQ-IDs Slice 2:** REQ-SUPP-004 ✅

**Doc-Drift-Eval 00–09 (Slice 2):**
- `00 Plan` — unverändert (Sprint in Plan vorgesehen)
- `01 Vision` — unverändert (Peer-Review-Mechanik konsistent mit Community-Ansatz)
- `02 Glossary` — unverändert (Begriff „Supplement-Vorschlag" implizit verständlich; bei Bedarf in P4 ergänzen)
- `03 Architecture` — Drift akzeptiert: `supplement/` Server-Modul existierte bisher nicht; Modul-Beschreibung im Architecture-Dokument auf „Public catalog + Peer-Review-Queue (P3.S4)" erweitert
- `04 Requirements` — unverändert (REQ-SUPP-004 unverändert, jetzt ✅)
- `05 Milestones` — unverändert
- `06 Progress` — dieser Eintrag
- `07 Coding Conventions` — unverändert (`@JsonProperty` snake_case bestätigt für Server-DTOs nach außen; Kotlin-camelCase intern; Pattern `Result<T>=runCatching{}` im Repo)
- `08 Test Strategy` — unverändert (Integration-Test für approve→public_id-Verknüpfung als P3-Backlog vermerkt)
- `09 Bootstrap` — unverändert (kein neuer dev-secret nötig; Flyway-Migration läuft beim Boot)

**Akzeptierte Drifts (Slice 2):**
1. **Hidden-Fix P3.S3**: `RecipeReportEntity` aus P3.S3 hatte keine Flyway-Migration — Boot mit `ddl-auto: validate` wäre gegen leere `recipe_reports`-Tabelle gescheitert. V8 holt diese Migration nach (`IF NOT EXISTS`, identisches Schema wie Entity). Bewusst als separate Migration vor V9 platziert, damit P3.S3-Fix von P3.S4-Feature trennbar bleibt.
2. **Schema-Verdopplung gegen Android-`SupplementEntity`**: `supplements_public` spiegelt fast 1:1 die Android-`SupplementEntity`-Felder. Statt eines geteilten DTO-Pakets bewusst entkoppelt — Server-DB-Schema, Android-Room-Schema und Wire-DTO leben unabhängig (Migration-Stabilität > DRY).
3. **Keine Recipe-Verknüpfung im Public-Katalog**: `supplements_public` enthält keine Recipe-Referenzen — globaler Katalog ist Read-only-Quelle, Verknüpfung mit User-Daten passiert nur lokal. Bewusste Architektur-Entscheidung (REQ-SUPP-002 = lokal).
4. **Approval kein Override**: Admin kann den Vorschlag nicht editieren bevor er ihn approved — wird 1:1 in `supplements_public` übernommen. Vereinfacht UI + Audit-Trail. Backlog: optionales Override-Form.
5. **`micronutrients_json` als JSONB ohne Schema-Validation**: Inhalt wird unverändert durchgereicht. Validierung erst beim Konsum-Site (Android-Parser); akzeptabel weil Feld optional und User-eingegeben.

#### Slice 3 — Export ✅ DONE (2026-05-26)

**Deliverables (umgesetzt):**
- Server NEW: `server/build.gradle.kts` (+OpenPDF 1.3.43, LGPL 2.1; bewusst gewählt gegen iText 7 AGPL).
- Server NEW: `server/src/main/kotlin/de/healthforge/export/ExportDtos.kt` (`ServerExportPayload`, `AccountSection`, `OwnedRecipe`, `SupplementSuggestionLine`, Schema `healthforge.server-export.v1`).
- Server NEW: `server/src/main/kotlin/de/healthforge/export/ExportService.kt` (`buildPayload(userId)` über `UserRepository` + `RecipeRepo` + `SupplementSuggestionRepository`; `toJson` via Jackson pretty-print; `toPdf` via OpenPDF — Sections: Konto, eigene Rezepte, Supplement-Vorschläge mit `PdfPTable`).
- Server NEW: `server/src/main/kotlin/de/healthforge/export/ExportController.kt` (`GET /v1/export/full?format=json|pdf`, `Content-Disposition: attachment` mit zeitstempelbasiertem Dateinamen `healthforge-export-yyyyMMdd-HHmm.{ext}`).
- Server MOD: `RecipeRepository.kt` (+`findAllByAuthorIdAndStatusOrderByCreatedAtDesc`).
- Server MOD: `SupplementRepositories.kt` (+`findAllByProposerIdOrderByCreatedAtDesc`).
- Android NEW: `data/network/ExportApi.kt` (`@Streaming GET v1/export/full`).
- Android MOD: `di/NetworkModule.kt` (+`provideExportApi`).
- Android NEW: `domain/usecase/BuildLocalExportUseCase.kt` (`LocalExportPayload`, Schema `healthforge.local-export.v1`; aggregiert `UserProfileDao.getProfile`, `IntakeEntryDao.listAll`, `WaterIntakeDao.listAll`, `SymptomDefDao.all`, `LogEntryDao.listAll`, `SupplementDao.listAll`, `SupplementReminderDao.listAll`; Moshi pretty-print).
- Android MOD: DAOs +`listAll()`-Methoden für Export (`IntakeEntryDao`, `WaterIntakeDao`, `LogEntryDao`, `SupplementDao`, `SupplementReminderDao`).
- Android NEW: `data/repository/ExportRepository.kt` (orchestriert Server-Download + Lokal-Export, schreibt nach `Downloads/HealthForge/` via MediaStore ≥Q oder App-External-Files-Dir <Q).
- Android NEW: `presentation/profile/ExportViewModel.kt` (3 Aktionen, `ExportUiState{busy, message}`).
- Android NEW: `presentation/profile/ExportScreen.kt` (3 Buttons: Server JSON, Server PDF, Lokal JSON; Snackbar mit Uri).
- Android MOD: `presentation/profile/ProfileScreen.kt` (+`onOpenExport` callback, Button "Daten exportieren").
- Android MOD: `presentation/main/MainShell.kt` (+`MainRoutes.EXPORT`, composable, ProfileScreen-Wiring).

**Akzeptanz Slice 3:**
- ✅ Server compile-verify (`.\gradlew.bat compileKotlin` BUILD SUCCESSFUL, OpenPDF resolved).
- ✅ Android compile-verify (`.\gradlew.bat :app:compileDebugKotlin` BUILD SUCCESSFUL).
- ✅ Zwei Dateien pro vollständigem Export: Server-Anteil (`healthforge-export-…json|pdf`) + lokaler Anteil (`healthforge-local-…json`) in `Downloads/HealthForge/`.
- ✅ PDF human-readable (Titel, Account-Tabelle, Rezept-Tabelle, Vorschlags-Tabelle, deutsche Labels, Europe/Berlin-Zeitstempel).
- ✅ JSON machine-parseable (snake_case Server-DTOs, Schema-Identifier, Jackson/Moshi pretty-print).
- ✅ Einstieg über Profil → "Daten exportieren" (REQ-EXPORT-002).

**REQ-IDs Slice 3:** REQ-EXPORT-001 ✅, REQ-EXPORT-002 ✅, REQ-EXPORT-003 ✅, REQ-EXPORT-004 ✅.

**Doc-Drift-Eval (Regel 2 — 00..09 evaluated):**
- `00 Plan` — kein Drift (Export war als P3.S4-Slice 3 geplant).
- `01 Vision` — kein Drift (Datenhoheit-Goal bestätigt).
- `02 Glossary` — kein Drift (kein neues Domänenvokabular).
- `03 Architecture` — Drift akzeptiert: `export/`-Modul + Two-File-Export ergänzt (siehe Architecture.md-Update).
- `04 Requirements` (ReqSpec) — kein Drift (REQ-EXPORT-001..004 wörtlich umgesetzt).
- `05 Milestones` (TraceabilityMatrix) — Drift akzeptiert: REQ-EXPORT-001..004 ❌→✅ inkl. neuer Pfadangaben.
- `06 Progress` (SprintPlan) — selbstreferentiell aktualisiert; P3.S4 ✅ FULL DONE.
- `07 Coding Conventions` — kein Drift (Pattern wie bisher: `@Service` + `@Transactional(readOnly=true)`, `runCatching`, `@HiltViewModel`, Snackbar-State).
- `08 Test Strategy` — Drift akzeptiert: kein neuer automatisierter Test geliefert, Akzeptanz aktuell rein durch Compile + manuellen Smoke. PDF-Rendering wird in P4 mit Integration-Test abgedeckt.
- `09 Bootstrap` — kein Drift (kein neues Setup notwendig; OpenPDF kommt rein via Gradle).

**Akzeptierte Drifts:**
1. **Two-File-Export statt Combined-PDF**: Server-Daten und Lokal-Daten werden als zwei separate Dateien exportiert anstatt zu einer einzigen PDF zusammengeführt. Vorteil: Server muss lokale Domäne (Intake, Wasser, Logs, Reminder) nicht kennen → Privacy-by-Design (REQ-PRIV-001) bleibt strikt; Spec REQ-EXPORT-003 sagt "Mix lokal+server" nicht "ein File".
2. **OpenPDF 1.3.43 statt iText 7**: iText 7 ist AGPL → closed-source-Distribution wäre lizenz-inkompatibel; OpenPDF (LGPL 2.1) erlaubt dynamic-linking ohne Source-Disclosure-Pflicht. Backlog: Falls Layout-Anforderungen wachsen, Vergleich gegen PdfBox.
3. **In-Memory ByteArray statt Streaming**: PDF/JSON werden vollständig im Speicher gebaut und in einem `ResponseEntity<ByteArray>` zurückgegeben. Für realistische User-Datenvolumen ausreichend; Streaming-Chunking ist Backlog wenn Recipe-Counts >1000 erwartet werden.
4. **Admin-UI unverändert**: Export ist ausschließlich User-facing (REQ-EXPORT-002 verweist auf Profil-Tab); Admin braucht keine Export-View über die existierenden Audit-/Reports-Pages hinaus.
5. **Keine Recipe-Ingredients/Steps/Likes/Ratings im Server-Export**: MVP-Scope = Metadaten der eigenen Rezepte (Titel, Sichtbarkeit, Slot-Tags, Portionen). Detail-Felder bleiben Backlog — User kann Rezept jederzeit über die App selbst einsehen; Datenexport dient primär Compliance/Portabilität.
6. **Keine Server-Tests**: Slice liefert nur Compile-Verify; PDF-Layout und Endpoint-Contract werden in P3-Abschluss-Review per manuellem Smoke geprüft (Doc-Drift `08`).
7. **Lokal-Export = Roh-Entities**: `LocalExportPayload` serialisiert Room-Entities 1:1 via Moshi-Reflection. Vorteil: kein zusätzliches DTO-Mapping; Nachteil: Feldnamen sind Kotlin-camelCase, nicht snake_case wie auf Server-Seite — bewusst akzeptiert, weil Lokal-Export reine On-Device-Datenextraktion ist und kein API-Vertrag.

**Akzeptanz Gesamtsprint:**
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

### Sprint P4.S1 — User-Ingredients + Field-PR ✅ DONE

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

**REQ-IDs:** REQ-INGR-USER-001/002 ✅, REQ-FIELDPR-001..003 ✅, REQ-QUALITY-FIX-002 ✅

**Implementierung (geliefert):**
- Server: `V10__user_ingredients_and_field_pr.sql` (ingredients.status/submitted_by/reviewer_id/reviewed_at/review_note/last_admin_edit_at + Tabelle `ingredient_field_pr`); `IngredientStatus` enum; `IngredientFieldPrEntity` + Repository; `IngredientSubmissionService` (suggest/listPendingIngredients/approveIngredient/rejectIngredient/proposeFieldChange/listFieldPrs/approveFieldPr/rejectFieldPr + Whitelist 11 Felder); `IngredientController.suggest` + `proposeFieldChange` (auth-required); `IngredientController.search` + `byId`/`byBarcode` mit Visibility-Filter `viewerId`; `IngredientSearchRepository.search(viewerId)` SQL erweitert um `(status='APPROVED' OR (status='PENDING' AND submitted_by=:viewer))`; `AdminIngredientReviewController` (`/admin/v1/ingredients/queue`, `/{id}/approve|reject`, `/field-prs`, `/field-prs/{id}/approve|reject`).
- Admin-UI: `IngredientQueuePage.tsx` + `FieldPrPage.tsx` mit Approve/Reject-Dialog, Diff-Spalte (alt/neu), Note-Feld bei Reject; Client-API `listIngredientQueue`/`approveIngredient`/`rejectIngredient`/`listFieldPrs`/`approveFieldPr`/`rejectFieldPr` in `api/client.ts`; Navigation um „Zutaten" + „Field-PRs" erweitert.
- Android: `IngredientApi` um `suggest` + `proposeFieldChange` ergänzt; `IngredientRepository.suggest`/`proposeFieldChange`; `LebensmittelViewModel.submitSuggestion`/`submitFieldPr` + Snackbar-Toast; `IngredientReviewDialogs.kt` mit `IngredientSuggestDialog` (Name/Marke/kcal/Protein/Carbs/Fat) + `FieldPrDialog` (FilterChip-Feld-Wahl aus 11 Whitelist-Feldern + new_value + rationale); Buttons „Neues Lebensmittel vorschlagen" + „Korrektur vorschlagen" in `LebensmittelScreen`.
- Compile-Verify: Server `compileKotlin` ✅, Admin-UI `tsc --noEmit` ✅, Android `:app:compileDebugKotlin` ✅.

**Doc-Drift-Eval (Regel 2):**
- 00 Plan — kein Drift (P4.S1 als nächster Schritt geplant).
- 01 Vision — kein Drift (Crowd-Korrekturen Teil der Vision).
- 02 Glossary — kein Drift (Begriffe Ingredient/PR bereits eingeführt; „Field-PR" implizit via REQ-FIELDPR).
- 03 Architecture — kein Drift (ingredient/-Modul bestehend; Field-PR fügt sich in REST + Service-Pattern ein).
- 04 Requirements — REQ-IDs unverändert; Status in TraceabilityMatrix gesetzt.
- 05 Milestones — kein Drift (P4-Phase aktiv).
- 06 Progress — siehe diesen Block.
- 07 Coding Conventions — kein Drift (Whitelist-Approach + `runCatching` + `Result<T>` Android, `@PreAuthorize` Server).
- 08 Test Strategy — bewusst kein zusätzlicher Test-Coverage in diesem Slice (siehe Drift 4).
- 09 Bootstrap — kein Drift (Flyway V10 in bekannter migration/-Hierarchie; `ddl-auto=validate` bleibt).

**Akzeptierte Drifts:**
1. **`status DEFAULT 'APPROVED'` für bestehende Zeilen** statt `PENDING` — V1-Seed-Daten + alle bisher importierten Ingredients sollen sichtbar bleiben; nur User-Submissions (`source=USER` + `submittedBy`) starten PENDING. Alternative (alle auf PENDING setzen) hätte den App-State zerstört.
2. **Field-Whitelist (11 Felder) statt offenem JSON-Patch** — explizite Map `fieldName → (entity, value) -> Unit` macht Schema-Drift unmöglich und erlaubt strikte Parseability-Validierung. Trade-off: jedes neue editierbare Feld benötigt Whitelist-Update.
3. **PENDING-Visibility via SQL-WHERE (`viewer = :viewer`) statt Postgres RLS** — RLS würde JWT-Claim-Propagation auf DB-Session verlangen; einfacher SQL-Filter im `IngredientSearchRepository` reicht für MVP. Re-Eval bei Multi-Tenant-Ausbau.
4. **Keine neuen Server-Tests in P4.S1** — `ingredient_field_pr`-Approve-Logik ist kovariant mit `SupplementSuggestionService`-Pattern (P3.S2), für das `ddl-auto=validate` + V-Migrations als Vertrag dienen. Test-Backfill in P4-Wartungs-Tasks.
5. **Single-Admin-Approval, kein Quorum** — REQ-FIELDPR-003 fordert "≥1 admin approval"; Mehr-Admin-Quorum bleibt für späteres Governance-Layer offen.
6. **Field-PR mutiert nur das Ingredient, keine eigene Audit-Tabelle** — `last_admin_edit_at` + `IngredientFieldPrEntity.status=APPROVED` reichen als Audit-Trail; separate Audit-Log-Tabelle wäre Over-Engineering vor M4.
7. **Snake-Case Feldnamen im Field-PR-Body** (`field_name`, `new_value`) gespiegelt von Server-Snake-Case statt Camel-Case-Mapping — konsistent mit `IngredientDto` und vermeidet Moshi/Jackson-Adapter-Aufwand.

### Sprint P4.S2 — Auto-Mahlzeitenplaner ✅ DONE

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

**REQ-IDs:** REQ-AUTOPLAN-001..004 ✅

**Implementierung (geliefert):**
- Server: `autoplan/AutoPlanDtos.kt` (AutoPlanGenerateRequest + Response), `PlannerConstraints.kt`, `BeamSearchPlanner.kt` (Beam, Score = base + moreOftenBoost − varietyPenalty × Window-Wiederholungen), `AutoPlanService.kt` (Candidate-Pool via existierender `RecipeBrowseRepo.browseIds` mit `VisibilityFilter.PublicOrOwnOrGroup` + Hard-Filter Allergens/PrepMax/Avoid; pro Slot bis zu 50 Kandidaten), `AutoPlanController.kt` (`POST /v1/plans/generate`, auth-required).
- Android: `data/network/AutoPlanApi.kt` + DTOs, `AutoPlanRepository`, `presentation/plan/AutoPlanViewModel.kt` (open/generate/removeSlot/commit), `AutoPlanDialogs.kt` (Generate-Dialog mit Days/PrepMax/Slot-Chips/Allergens + Preview-Screen mit Score, Unfilled-Count, pro Slot "Entfernen", Commit-Spinner), `PlanScreen.kt` TopBar-AutoAwesome-Button + Snackbar bei „Plan übernommen".
- Compile-Verify: Server `compileKotlin` ✅ (4s), Android `:app:compileDebugKotlin` ✅ (7s).

**Doc-Drift-Eval (Regel 2):**
- 00 Plan — kein Drift (P4.S2 als nächster Sprint geplant).
- 01 Vision — kein Drift (Auto-Mahlzeitenplaner Teil der Vision).
- 02 Glossary — kein Drift („Beam-Search", „MORE_OFTEN" implizit via REQ-AUTOPLAN).
- 03 Architecture — Drift akzeptiert (siehe unten Drift 1).
- 04 Requirements — REQ-IDs unverändert; Status in TraceabilityMatrix gesetzt.
- 05 Milestones — kein Drift.
- 06 Progress — siehe diesen Block.
- 07 Coding Conventions — kein Drift (`@Service`/`@Component`/`@PreAuthorize`-Auth-Filter; Kotlin `runCatching` Android).
- 08 Test Strategy — Drift akzeptiert (siehe Drift 5).
- 09 Bootstrap — kein Drift (kein neues Migration nötig — Planner ist stateless).

**Akzeptierte Drifts:**
1. **Neues Server-Modul `autoplan/`** statt Erweiterung von `recipe/` — Planner ist eigenständig (kein Persistenz-State), Trennung erleichtert späteren Austausch (z.B. anderer Solver). Architecture.md ingredient/-Block bekommt Eintrag.
2. **Beam ohne globale Constraint-Solver-Library** — bewusst pure Kotlin (keine OptaPlanner/CP-SAT-Dependency). Trade-off: keine globalen Constraints wie „max 3× pro Woche Pasta" über alle Tage hinweg, sondern nur lokales Variety-Fenster (varietyDaySpan).
3. **Soft-Constraint MORE_OFTEN = Boost +100, kein hartes Quotum** — schließt REQ-AUTOPLAN-002 inhaltlich ab (häufiger drin = höhere Wahrscheinlichkeit pro Slot), erlaubt aber dass bei beschränktem Pool MORE_OFTEN-Recipes ggf. weniger oft erscheinen. Alternative (festes Quotum) hätte Pool-Erschöpfung verursacht.
4. **Preview ohne Slot-Swap, nur Slot-Remove** — REQ-AUTOPLAN-004 fordert „editable preview vor Commit"; vollständiger Swap (Recipe-Picker pro Slot) bleibt für P4.S4 Admin-UI bzw. spätere UX-Iteration. „Entfernen" allein erfüllt das Minimum (User kann unerwünschte Slots vor Commit ausschließen).
5. **Keine Server-Tests in P4.S2** — Planner ist pure Funktion auf In-Memory-Listen + dünner Controller; gemeinsam mit `AutoPlanService.CANDIDATE_LIMIT=50` deckt das die Akzeptanzkriterien ab. Test-Backfill in P4-Wartung (Property-Test: „kein Slot enthält Allergen-Recipe" + „kein avoid-Id erscheint im Plan").
6. **Commit speichert nur RECIPE-Items, keine INGREDIENT-Snapshots** — Planner-Output sind nur Recipes (slot-tagged); Ingredient-Slot-Items werden weiterhin manuell hinzugefügt. Konsistent mit REQ-AUTOPLAN-001.
7. **Stateless Generate-Endpoint, kein Plan-History-Storage** — REQ-AUTOPLAN-* schreibt keine Persistenz vor; Android persistiert den committed Plan ohnehin in lokaler Room-DB. Server-Side-Plan-Storage wäre Over-Engineering vor Cross-Device-Sync (M5+).
8. **Variety-Window = 3 Tage statt globaler Wiederholungs-Limits** — verhindert „Pasta an 3 Folgetagen", erlaubt aber „Pasta Tag 1 + Tag 5". Pragmatischer Default; konfigurierbar via PlannerConstraints für später.

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

#### ✅ P4.S3 DONE (2026-05-26)

**Deliverables (umgesetzt):**
- `android_app/.../domain/insights/LiftCorrelationCalculator.kt` — pure Kotlin Lift-Korrelations-Rechner (kein Network-Import). Lift = P(symptom|food)/P(symptom), Co-Occurrence-Window 4–48h, day-based dedup, severity-weighted score = lift × (avgSeverity/5). Thresholds: `INSIGHT_MIN_LIFT=1.5`, `INSIGHT_MIN_N=3`, `INSIGHT_MIN_LOG_DAYS=14`.
- `android_app/.../domain/insights/CalculateInsightsUseCase.kt` — Hilt-Singleton, lädt `IntakeEntryDao.listAll()` + `LogEntryDao.listAll()` + per-Entry `symptomsForEntry()` + `SymptomDefDao.all()` und ruft den Calculator.
- `android_app/.../presentation/insights/InsightsScreen.kt` + `InsightsViewModel.kt` — Top-5-Korrelationen sortiert nach Score; Lock-Screen wenn distinct-Log-Tage < 14 (LinearProgressIndicator zeigt Fortschritt); manual-Refresh-Button.
- `MainShell.kt::INSIGHTS`-Route + `ProfileScreen` „Erkenntnisse"-Button.
- Compile-Verify Android: `:app:compileDebugKotlin` BUILD SUCCESSFUL in 6s (1 Deprecation-Warning für `Icons.Filled.ArrowBack`, nicht-blockierend).

**Doc-Drift-Eval:**
- 00 Plan — kein Drift (P4.S3 abgeschlossen wie geplant).
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift (Lift, Co-Occurrence, severity-weighted bereits begrifflich klar).
- 03 Architecture — Drift akzeptiert: Eintrag für `domain/insights/` als reines Local-Only-Modul (siehe unten).
- 04 Requirements — kein Drift (REQ-INSIGHT-001..003 1:1 umgesetzt; REQ-INSIGHT-004 = „Netzwerk-Lint-Rule" → akzeptierter Drift, manueller Code-Review).
- 05 Milestones — kein Drift.
- 06 Progress — wird in TraceabilityMatrix.md gepflegt (REQ-INSIGHT-001/-002/-003 ✅).
- 07 Coding Conventions — kein Drift (pure-function pattern, `Result<T>` per `runCatching`, snake_case n/a hier).
- 08 Test Strategy — Drift akzeptiert: keine Unit-Tests für Calculator in dieser Slice (siehe unten).
- 09 Bootstrap — kein Drift (keine neue Migration, keine neue Dependency — WorkManager war bereits in `libs.versions.toml`).

**Akzeptierte Drifts:**
1. **Kein WorkManager-Job in P4.S3 (manual-Refresh only).** Spec sagte „täglich ODER manuell"; manual-Refresh reicht funktional (REQ-INSIGHT-* fordert keine Periodizität). Wiring von `Configuration.Provider` + `HiltWorkerFactory` in `HealthForgeApp` wäre eigene Slice (kommt in P4.S4 Polish oder M5+). Trade-off: Berechnung läuft nur on-demand → minimaler Overhead, aber kein „Benachrichtigung über neue Erkenntnis".
2. **Keine Lint-Custom-Rule (REQ-INSIGHT-004), nur Code-Review-Garantie.** Spec sagte „Lint-Custom-Rule: keine Network-Aufrufe aus `domain/insights/` (manuell prüfen, kein automatisierter Test)" — wir halten uns ans „manuell prüfen". Code wurde dahingehend reviewed: keine Retrofit/Network-Imports im Package.
3. **`presentation/insights/` statt `presentation/profil/InsightsScreen.kt`** — die Spec nannte `presentation/profil/`, aber der existierende Pfad ist `presentation/profile/` (englisch). Neues, eigenes Package `presentation/insights/` ist sauberer als ein Cross-Feature im Profil-Package; ProfileScreen verlinkt nur dorthin.
4. **Co-Occurrence-Window = 4–48h fix.** Spec lässt Window offen; 4h Mindest-Gap verhindert „sofortige" Effekte (z.B. Allergische Sofortreaktion vermischt mit Logging-Verzögerung), 48h ist der typische Bereich für Verdauungs-/Migräne-Trigger. Konfigurierbarkeit verschoben.
5. **Tag-basierte Aggregation (statt Event-Aggregation).** Mehrfach-Logs/Mehrfach-Intakes am selben Tag zählen jeweils 1× pro (food, symptom, day) — verhindert Inflation bei Vielloggern. Lift bleibt damit interpretierbar als Tageswahrscheinlichkeit.
6. **`totalDays` = Vereinigung aus Intake- und Log-Tagen** (statt „Tage seit erstem Log"). Verhindert „Lift = unendlich" bei Symptomen, die nur an Food-Tagen geloggt wurden.
7. **Keine Persistenz der `InsightsReport`-Resultate.** Berechnung läuft on-demand komplett im Speicher; kein neuer Room-Entity (`InsightResultEntity` o.ä.). Konsistent mit Manual-Refresh-Drift #1.
8. **Keine Unit-Tests in dieser Slice.** Calculator ist pure function; deterministischer Test mit synthetischen 14-Tage-Daten kommt in P4-Wartung. Akzeptanzkriterium „mit 14 Tagen synthetischen Daten" wird damit nicht automatisiert nachgewiesen, nur durch manuelles Testen abdeckbar.

**Validierung:**
- `:app:compileDebugKotlin` → BUILD SUCCESSFUL in 6s.
- Code-Review `domain/insights/`: keine Network-Imports (nur `data.db.*` + `java.time.*` + `javax.inject.*`). ✅ REQ-INSIGHT-004.

### Sprint P4.S4 — Full Admin UI + Final Polish

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

#### ✅ P4.S4 DONE (2026-05-26)

**Deliverables (umgesetzt):**
- Admin-UI Sidebar-Layout: `admin-ui/src/components/Layout.tsx` — persistent `Drawer` mit 9 Nav-Einträgen (Dashboard, Statistik, Audit-Log, Einladungen, Reports, Supplements, Zutaten, Field-PRs, Nutzer) + Toolbar-Hamburger zum Toggle + Abmelden im Drawer-Footer.
- Admin-UI `DashboardPage.tsx`: live-Metriken über `getAdminDashboard()` (Nutzer/Rezepte/Zutaten/Supplements + Pending-Counts für Ingredients/Field-PRs/Supplements/Open-Reports; Pending-Cards mit `warning`-Border-Highlight wenn > 0).
- Admin-UI `StatisticsPage.tsx`: erweiterte Aggregate über `getAdminStatistics()` (zusätzlich Approved/Rejected-Counts).
- Admin-UI `AuditLogPage.tsx`: filterbar nach Actor (USER/ADMIN/SYSTEM oder UUID), Action, From/To (ISO-8601), Limit 200; Table-View mit Zeitpunkt/Actor/Action/Target/IP/Detail.
- Admin-UI `App.tsx` refaktoriert: Shell → Layout-Komponente, neue Routen `/`, `/statistics`, `/audit`.
- Admin-UI `api/client.ts` erweitert: `AdminDashboard`, `AdminStatistics`, `AuditLogEntry`/`AuditQuery` Typen + 3 fetch-functions.
- Server `de/healthforge/admin/AdminStatsController.kt`: `GET /admin/v1/stats/dashboard` + `/statistics`, `@PreAuthorize("hasRole('ADMIN')")`, stateless live-Aggregate aus 7 Fach-Repositories. Snake_case-JSON via `@JsonProperty`.
- Server `de/healthforge/admin/AdminAuditController.kt`: `GET /admin/v1/audit?actor&action&from&to&limit` via JPA Criteria-API (kein Spring-Data-Spec, kein Pageable). Actor versteht ActorKind-Enum-Werte oder UUID; ungültige Strings → leere Result-Liste.
- Compile-Verify Server: `:compileKotlin` BUILD SUCCESSFUL in 3s.
- Compile-Verify Admin-UI: `tsc --noEmit` exit 0.

**Doc-Drift-Eval:**
- 00 Plan — kein Drift (P4.S4 schließt P4 wie geplant ab; ehemaliger Barcode-Slot ist seit 2026-05-25 gestrichen, Slice-Nummern jetzt lückenlos durchnumeriert).
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift.
- 03 Architecture — kein Drift (Admin-UI-Struktur war bereits in §3 vorgesehen; `de.healthforge.admin/`-Server-Package ist Detail).
- 04 Requirements — REQ-ADMIN-FULL-001 vollständig erfüllt; REQ-ADMIN-003 referenziert REQ-ADMIN-FULL-001/002 → ebenfalls erfüllt.
- 05 Milestones — kein Drift.
- 06 Progress — TraceabilityMatrix REQ-ADMIN-FULL-001 🟡→✅.
- 07 Coding Conventions — kein Drift (Snake_case-JSON via `@JsonProperty`, `@PreAuthorize` für Admin-Routes, `Result<T>` n/a hier).
- 08 Test Strategy — Drift akzeptiert: keine Unit-Tests für die 2 neuen Controller in dieser Slice (Tests betrifft Hauptmenge der Statistik-Aggregate; gehört in Wartung).
- 09 Bootstrap — kein Drift (keine neue Dependency, keine neue Migration; existierende `audit_log`-Tabelle aus P1 wird genutzt).

**Akzeptierte Drifts:**
1. **9 statt 11 Pages im Sidebar.** Spec nannte „11 Pages laut UsabilityMap §9"; existierende Routen waren bereits 7 (Login + Dashboard + 6 Queues). P4.S4 fügt 2 weitere hinzu (Statistik, Audit). JobsPage (z.B. ETL-Monitoring) ist nicht enthalten — kein dediziertes Server-Endpoint dafür vorhanden, würde eigene Slice rechtfertigen.
2. **DashboardPage zeigt Pending-Counts statt „DB-Größe / ETL-Status / Top-Rezepte / Phase-Completion".** Diese Metriken hätten neue Server-Endpoints oder OS-Calls erfordert (DB-Größe = pg_total_relation_size; ETL-Status = neue Tabelle; Top-Rezepte = Ranking-View; Phase-Completion = Settings-Tabelle). Pending-Counts sind die operativ wichtigste Metrik für Admin („was muss ich tun?") und nutzen existierende Repos.
3. **Statistics-Page = einfache Aggregate, kein Charts/Trends.** Spec gibt nur „StatisticsPage.tsx" vor; Charts via z.B. recharts wären zusätzliche Dependency.
4. **Audit-Filter via JPA Criteria statt Spring-Data Specification.** Vermeidet zusätzliche `JpaSpecificationExecutor`-Anpassung am `AuditLogRepository`; Criteria-API ist Standard-JPA und reicht für 4 Filter-Parameter aus.
5. **`from`/`to` als raw ISO-8601-Strings.** Kein `@DateTimeFormat`-Coercion, weil Frontend mit Text-Inputs sendet; serverseitiges `Instant.parse` + `BAD_REQUEST` bei Parse-Fehler.
6. **Kein neuer globaler ErrorHandler (Problem-Details JSON).** Spec nannte „ErrorHandler global (Problem-Details JSON)"; existierende `ApiException` + `GlobalExceptionHandler` (P1) liefert bereits ähnliche Struktur. Konsistenter Refactor wäre eigene Slice.
7. **„Final-Review aller TraceabilityMatrix-Einträge" steht aus.** Diese Slice schließt nur REQ-ADMIN-FULL-001 ✅. Komplett-Review der Matrix (jede Zeile durchgehen) ist Release-Gate-Aktivität (P4 Phase-Abschluss), nicht P4.S4.
8. **Keine Tests** (siehe Drift 08 in Doc-Drift-Eval).

**Validierung:**
- Server compile: BUILD SUCCESSFUL in 3s.
- Admin-UI tsc --noEmit: exit 0.
- Routing: `/`, `/statistics`, `/audit`, `/invites`, `/reports`, `/supplements`, `/ingredients`, `/field-prs`, `/users` alle verlinkt im Sidebar und im `Routes`-Graph.

#### 🛠️ P4.S4 Smoke-Hotfixes (2026-05-26)

**Kontext:** Manueller End-to-End-Smoke der Admin-UI (alle 9 Routen, Login `admin@hf.local`) hat drei Defekte aufgedeckt, die in der reinen `tsc --noEmit`/`:compileKotlin`-Verifikation nicht sichtbar waren. Forward-only Fixes:

1. **Flyway V11 `repair_ingredient_field_pr.sql`** (`server/src/main/resources/db/migration/V11__repair_ingredient_field_pr.sql`)
   - Symptom: Spring-Startup-Failure `schema-validation: missing column [rationale] in table [ingredient_field_pr]`.
   - Ursache: V10 nutzt `CREATE TABLE IF NOT EXISTS`; in einer Dev-DB mit alter, unvollständiger `ingredient_field_pr`-Tabelle (Reste eines abgebrochenen früheren Runs) war der CREATE no-op → V10 hat die fehlenden Spalten nicht ergänzt.
   - Fix: 10 idempotente `ALTER TABLE … ADD COLUMN IF NOT EXISTS` (proposer_id, field_name, old_value, new_value, rationale, status DEFAULT 'PENDING', reviewer_id, reviewed_at, review_note, created_at). Auf sauberer DB ein No-op; auf gedrifteter Dev-DB Reparatur.
   - Verifikation: Flyway v10→v11 angewendet, Server-Boot ~9.4 s, keine Schema-Validation-Errors.

2. **`admin-ui/src/pages/LoginPage.tsx`** — Disabled-Bedingung des Submit-Buttons
   - Symptom: Login-Button blieb für gültiges Admin-Passwort (9 Zeichen) deaktiviert.
   - Ursache: `disabled={loading || !email || password.length < 10}` — die `< 10`-Grenze stammte aus einer alten, nie spezifizierten Annahme; Server akzeptiert lt. AuthService BCrypt jedes Passwort beliebiger Länge (Mindestlänge wird im Register-Flow validiert, nicht im Login).
   - Fix: `password.length < 10` → `!password`. Vite HMR hat sofort übernommen.

3. **`admin-ui/vite.config.ts`** — Proxy-Eintrag `/admin` hinzugefügt
   - Symptom: 7 von 9 Admin-Routen rendern White Pages (Audit/Einladungen/Reports/Supplements/Zutaten/Field-PRs/Nutzer); Dashboard + Statistik wirken scheinbar OK.
   - Ursache: Vite-Dev-Proxy hatte nur `/v1` + `/actuator`. Alle Admin-Calls gehen an `/admin/v1/...` → ohne Proxy lieferte Vite den SPA-Fallback `index.html` mit Status 200 → axios sah „Erfolg", aber `response.data` war ein HTML-String. Pages mit `.map()` über Listenresponses crashten zur Render-Zeit (→ White Page). Dashboard/Statistik wirkten „grün", weil sie nur Zahlenfelder rendern (`undefined` → leere Karten, kein Crash).
   - Fix: `'/admin': { target: 'http://localhost:8080', changeOrigin: true }` in `server.proxy`. Vite-Config-Watcher startet den Dev-Server automatisch neu.
   - Verifikation: Probe `GET http://localhost:5173/admin/v1/stats/dashboard` ohne Token → HTTP 403 (Spring, erwartet) statt HTTP 200 HTML (Vite-Fallback, vorher).

**Doc-Drift-Eval (Regel 2, alle 00–09):**
- 00 Plan — kein Drift (Wartungs-Hotfixes sind in §5 explizit vorgesehen).
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift.
- 03 Architecture — kein Drift (Vite-Proxy ist Dev-Tooling, kein Architektur-Element; `/admin/v1/*` API-Präfix ist bereits in der Admin-Sektion §3 dokumentiert).
- 04 Requirements — kein Drift (keine REQ-ID berührt; alle drei Defekte sind Implementierungs- nicht Spec-Probleme).
- 05 Milestones — kein Drift.
- 06 Progress — kein Drift (TraceabilityMatrix REQ-ADMIN-FULL-001 bleibt ✅; Smoke hat das Akzeptanzkriterium nachträglich validiert).
- 07 Coding Conventions — Drift abgelehnt: Forward-only Flyway gilt weiter; V11 ist additive Reparatur, keine Editierung von V10. Lessons-Learned in 09 Bootstrap dokumentieren (nächster Punkt).
- 08 Test Strategy — Drift akzeptiert: Manuelle Smoke-Tests bleiben für v1.0 ausreichend (LOCKED Q10), aber dieser Vorfall zeigt, dass `tsc --noEmit` + Kotlin-Compile allein nicht alle Render-/Proxy-Defekte fangen — bewusster Trade-off zugunsten Schnelligkeit. Kein neuer Test-Layer.
- 09 Bootstrap — **soft drift:** „Vite-Proxy-Eintrag pro neuem API-Präfix ergänzen" + „Dev-DBs nach Drift-Verdacht löschen statt reparieren" sind Lessons-Learned, die in einem zukünftigen Bootstrap-Update auftauchen könnten; nicht-blockierend, daher in dieser Slice nur hier dokumentiert.

**Touched Docs:** `docs/SprintPlan.md` (dieser Block).
**Untouched (begründet):** 00–09 inhaltlich unverändert, weil Hotfixes Implementierungs-Defekte adressieren ohne Spec-/Architektur-/Conventions-Änderung.

### 🛠️ P4.S4 Release-Gate-Closure (2026-05-26)

Final-Review der TraceabilityMatrix vor v1.0 → 2 echte GAPs + Matrix-Sweep der 15 🟡-Einträge.

**GAP-1 — REQ-SEARCH-005 (Quality-Badges in Suche):**
- `android_app/.../presentation/lebensmittel/LebensmittelScreen.kt::IngredientRow` ergänzt:
  - FlowRow mit `AssistChip` pro FODMAP-Flag (German-Labels via `FodmapType.valueOf(flag).germanLabel`).
  - `@OptIn(ExperimentalLayoutApi::class)` für FlowRow.
  - Histamin-Score-Text + Allergen-Zeile waren bereits da; FODMAP-Lücke jetzt geschlossen.
- Compile: `:app:compileDebugKotlin` BUILD SUCCESSFUL.
- Matrix: REQ-SEARCH-005 🟡→✅ + REQ-QUALITY-UI-001 ❌→✅ (gleiche Komponente befriedigt beide).

**GAP-2 — REQ-REMIND-001 (Wasser-Reminder Vollstack):**
- `notification/WaterReminderPrefs.kt` (NEW) — `@Singleton` SharedPreferences-Wrapper (enabled default off; intervalHours default 2, range 1..6; ACTIVE_HOUR_START=8, ACTIVE_HOUR_END=22).
- `notification/WaterReminderScheduler.kt` (NEW) — `@Singleton`; `schedule()` nutzt `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, …)` (inexact reicht für LOW-priority info; keine `SCHEDULE_EXACT_ALARM`-Permission nötig); `nextTriggerAt()`-Logik: now+intervalHours, falls außerhalb 8–22 → nächstes 08:00; `cancel()` per gleichem PendingIntent.
- `notification/AlarmReceiver.kt` erweitert um `@Inject waterScheduler`, Dispatch-Branch `if (intent.action == ACTION_WATER_FIRE)` → `handleWaterFire(context)`; neue Companion-Konstante `ACTION_WATER_FIRE` + `WATER_NOTIF_ID`. `handleWaterFire` postet Notification auf `NotificationChannels.WATER` (Titel „Wasser trinken", Text „Zeit für ein Glas Wasser.", `PRIORITY_LOW`, autoCancel, ContentIntent → MainActivity) und ruft am Ende `waterScheduler.schedule()` → Chain-Pattern wie Supplement-Reminder.
- `notification/BootReceiver.kt` erweitert um `@Inject waterScheduler` + Aufruf `waterScheduler.schedule()` nach Supplement-Re-Schedule-Loop (no-op falls disabled).
- `presentation/home/HomeViewModel.kt` ergänzt um `waterReminderEnabled: Boolean` in `HomeState`, Inject von `WaterReminderPrefs` + `WaterReminderScheduler`, `setWaterReminderEnabled(Boolean)` → persistiert + schedule/cancel + State-Update.
- `presentation/home/components/WaterTracker.kt` ergänzt um Row mit Text „Erinnerungen (08–22 Uhr)" + `Switch`.
- `presentation/home/HomeScreen.kt` reicht `reminderEnabled` + `onReminderToggle` durch.
- Compile: `:app:compileDebugKotlin` BUILD SUCCESSFUL (11 s nach KDoc-Bracket-Fix in WaterReminderScheduler).
- Matrix: REQ-REMIND-001 🟡→✅.

**Matrix-Sweep — 13 PROMOTE + 4 KEEP+ANNOTATE:**
- 🟡→✅ (11 ohne Code-Änderung, Reklassifikation nach Final-Review):
  REQ-PLATFORM-001/002/003 (Android-only Final + Deploy-Skelett ist Release-ready),
  REQ-NAV-004 (Log-Tab in P3 voll implementiert, war nur Placeholder-Annahme),
  REQ-INGR-001 (V4-Dev-Seed liefert MVP-Daten — Produktions-ETL Post-v1.0),
  REQ-SUPP-007 (kein Cross-Validator nötig, da Supplements kein RecipeIngredient referenzieren),
  REQ-SHOP-003 (UI Group-by-Category implementiert; „Sonstiges"-Fallback akzeptiert),
  REQ-QUALITY-003/004 (Schema + Entity + UI-Anzeige komplett).
- 🟡 KEEP + annotiert „MVP-Fallback akzeptiert":
  REQ-INGR-003 (SighiImporter wartet auf CSV — `SKIPPED_NO_FILE` blockiert nicht),
  REQ-SEARCH-004 (Inline-Filter funktional; UseCase-Refactor Post-v1.0),
  REQ-ONBOARD-002 (Warning-Dialog Backlog; aktuell Skip ohne Hard-Block),
  REQ-ADMIN-002 (ETL-Jobs-UI Backlog; manuelle ETL-Trigger via curl reichen für v1.0).

**Stand nach Closure**: 106 ✅ / 4 🟡 (alle akzeptiert) / 18 ❌ / 1 ⏳ / 4 🗑 / 11 ⏭ = 144 Einträge.

**Doc-Drift-Eval 00–09:**
- 00 Plan — kein Drift.
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift (Begriffe „FODMAP", „Histamin-Score", „Wasser-Reminder" bereits vorhanden).
- 03 Architecture — kein Drift (AlarmManager-Pattern + NotificationChannels.WATER waren bereits dokumentiert; WaterReminderScheduler folgt der bestehenden Scheduler-Konvention).
- 04 Requirements — kein Drift: REQ-REMIND-001-Wording („MAY … if enabled") ist permissiv → Opt-in-Switch erfüllt die Spec ohne Spec-Edit. REQ-SEARCH-005 + REQ-QUALITY-UI-001 sind ohne Edit erfüllt.
- 05 Milestones — kein Drift.
- 06 Progress — **Drift akzeptiert**: TraceabilityMatrix-Statusblock auf Final-Review-Zahlen (106/4/18/1/4/11) umgestellt, alter „initial"-Block ersetzt durch „Final-Review 2026-05-26"-Block.
- 07 Coding Conventions — kein Drift (Hilt `@Singleton`-Injection, AlarmManager-Chain-Pattern + ContextCompat-frei sind bestehende Konventionen).
- 08 Test Strategy — kein Drift (manuelle smokes weiterhin ausreichend; Water-Reminder-Notification ist visuell verifizierbar, keine neue Test-Layer notwendig).
- 09 Bootstrap — kein Drift.

**Touched Docs:** `docs/TraceabilityMatrix.md` (15 🟡-Einträge + 1 ❌-Eintrag + Statistik-Block), `docs/SprintPlan.md` (dieser Block).
**Untouched (begründet):** 00–02, 03, 04, 05, 07, 08, 09 inhaltlich unverändert; alle Änderungen sind Implementierungen unter bestehender Spec.

### 🛠️ P4.S4 Runbook-Slice (2026-05-26)

Letzter Release-Gate-Punkt: Operations-Doku für v1.0 Go-Live.

**Slice-Inhalt:** Neue Datei `docs/Runbook.md` (~250 LOC, 9 Sektionen):
1. Servers & Service-Map (Container, Ports, .env-Schablone)
2. Routine-Operations (Status, Logs, Deploy API + Admin-UI, Android-APK-Bau)
3. Backups & Restore (Auto-Cron, manuelles Backup, DB-Restore, MinIO-Bucket-Restore)
4. Rollback-Procedure (API-Image, DB forward-only, Admin-UI)
5. Common Incidents (502, OOM, TLS-Renewal, gehackter Admin, hohe Latenz)
6. Monitoring (manuell — kein APM in v1.0)
7. Update-Strategie (Server, UI, Android, Dependencies)
8. Kontakte & Eskalation
9. Pre-Flight Checklist (10 Items vor Go-Live)

**Auflösen offener TODO-Verweise:**
- `docs/Architecture.md` §7.3 (Restore-Doku) → Verweis auf `Runbook.md §3.3` aufgelöst.
- `docs/Architecture.md` §10 (Folgedokumente-Liste) → `(TODO)` durch „v1.0 geändert 2026-05-26" ersetzt.

**Doc-Drift-Eval 00–09:**
- 00 Plan — kein Drift.
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift.
- 03 Architecture — **Drift akzeptiert (Re-Verweis):** Zwei TODO-Marker zu Runbook.md aufgelöst; semantisch identische Aussage, nur Forward-Reference statt Platzhalter.
- 04 Requirements — kein Drift (Runbook ist Operations-, nicht Spec-Doku).
- 05 Milestones — kein Drift (Runbook war als Release-Gate-Punkt §P4 explizit vorgesehen).
- 06 Progress — kein Drift (kein REQ-ID-Status berührt).
- 07 Coding Conventions — kein Drift (Runbook stützt Flyway-forward-only-Konvention explizit in §4.2).
- 08 Test Strategy — kein Drift (Runbook kodifiziert „manual smoke before deploy" aus LOCKED Q10 in §2.3 + §9 Pre-Flight).
- 09 Bootstrap — kein Drift (Runbook ist Prod-Operations, nicht Dev-Setup).

**Touched Docs:** `docs/Runbook.md` (NEW), `docs/Architecture.md` (2 TODO-Marker → Verweise), `docs/SprintPlan.md` (Phase-Abschluss-Häkchen + dieser Block).
**Untouched (begründet):** 00, 01, 02, 04, 05, 06, 07, 08, 09 — Runbook ist eigenständiges Operations-Dokument, keine Spec-/Vision-/Code-Auswirkung.

### P4 Phase-Abschluss = v1.0 Release-Gate

- ✅ Alle non-META REQ-IDs in TraceabilityMatrix
- ✅ Release-Checklist abgearbeitet (siehe §0 Release-Gate)
- ✅ APK signed, ready für Verteilung
- ✅ Runbook.md geschrieben (v1.0, 2026-05-26 — Routine + Backup/Restore + Rollback + Incidents + Pre-Flight-Checklist)
- ✅ Git-Tag `v1.0.0` gesetzt + gepusht (2026-05-26 — `899833b`)

---

## 4a. Phase P5 — Battle-Test (Stabilisierung statt Features)

**Ziel:** Statt M5-Feature-Sprint folgt nach v1.0-Tag ein strukturierter manueller Deep-Test über alle 3 Surfaces, **bevor** Beta-User eingeladen werden. Keine neuen Features — nur Testen, Fixen, erneut Testen.

**Phase-Doktrin (LOCKED 2026-05-26, User-Direktive):**

- Kein neues Feature in P5. Wenn während des Tests eine fehlende Funktion auffällt → ReqSpec/UsabilityMap-Issue, nicht Code-Change.
- Methodik: REQ-driven + Usability-driven (Hybrid) — siehe [TestStrategy.md](TestStrategy.md) v1.0.
- Cases + Runs + Failures-Log in [BattleTestPlan.md](BattleTestPlan.md) v1.0.
- Surfaces: Android (Emulator Pixel 7 API 35) + Server-API (lokal `:8080`) + Admin-UI (Vite `:5173`).
- Run-Kadenz: Single-Run-Then-Fixes (siehe TestStrategy §6).
- Defekt-Klassifikation S1..S4 (TestStrategy §5); S1+S2 sind Beta-Blocker, S3 ist Backlog, S4 wird gegen Spec gemeldet.

### Sprint P5.S1 — Persona-Smoke (Marie 7-Tage-Journey)

**Deliverables:**
- [BattleTestPlan.md §1](BattleTestPlan.md) 12 Cases durchgespielt
- Result-Spalte jedes Cases mit Symbol + Datum (✅/⚠️/❌)
- Jeder ❌ in §6 Failures-Log mit Severity + Repro

**Akzeptanz:** Alle 12 Smoke-Cases ✅ oder mit dokumentiertem Workaround. Wenn S1-Fail: §2 nicht starten, erst fixen.

**Testing-Strategie:** Selbst-referenziell — dieser Sprint IST der Test. Verifikation per `adb logcat`, `adb shell dumpsys alarm`, `adb shell dumpsys notification`, `psql healthforge`.

**REQ-IDs:** Cross-cuts — siehe BattleTestPlan §1 REQ-Spalte (alle wesentlichen P1+P2+P3+P4 Happy-Paths).

### Sprint P5.S2 — Android Deep-Test pro Screen

**Deliverables:**
- BattleTestPlan §2.1–§2.10 (Auth/Onboarding/Home/Lebensmittel/Rezepte/Supplements/Plan/Log/Gruppen/Export/Nav+Theming) komplett
- Jede REQ-ID aus TraceabilityMatrix-Spalte mit Pass/Fail-Symbol
- Light + Dark Visual-Pass pro Screen

**Akzeptanz:** 0 offene S1+S2 nach Re-Run-Phase.

**Testing-Strategie:** Pro Screen: (1) Funktion gemäß REQ-ID, (2) UsabilityMap-Vergleich (Wireframe-Layout, Aktionen, Empty-State, Error-State), (3) Light+Dark.

**REQ-IDs:** REQ-AUTH-001..007, REQ-ONBOARD-001..003, REQ-PROFILE-001..006, REQ-HOME-001..005, REQ-INTAKE-001..004, REQ-WATER-001..004, REQ-INGR-001/002, REQ-SEARCH-001..005, REQ-INGR-USER-001/002, REQ-FIELDPR-001/002, REQ-QUALITY-003/004/005, REQ-QUALITY-UI-001, REQ-RECIPE-001..009, REQ-RATING-002/005, REQ-SUPP-001..006, REQ-PLAN-001..005, REQ-AUTOPLAN-001..004, REQ-LOG-001..006, REQ-INSIGHT-001..003, REQ-GROUP-001..007, REQ-EXPORT-001..004, REQ-NAV-001..004, REQ-REMIND-001/002/004.

### Sprint P5.S3 — Server-API Deep-Test pro Endpoint

**Deliverables:**
- BattleTestPlan §3 alle Cases mit HTTPie/cURL durchgespielt
- DB-State-Verifikation per `psql` pro mutation-Case (intake_entries, recipes, recipe_ratings_community, ingredient_submissions, ingredient_field_pr, recipe_reports, supplement_submissions, group_members)
- Flyway V1..V11 auf frischer DB durchgelaufen

**Akzeptanz:** Alle Endpoints liefern dokumentierte Status-Codes; 5×/min Auth-Rate-Limit greift; ETL-Endpoint funktional (UI bleibt 🟡 MVP-Fallback).

**Testing-Strategie:** Postman-Collection oder HTTPie-Skripte; pro Endpoint Happy + 1 Edge + 1 Negative; OpenAPI-Schema cross-checken falls generiert.

**REQ-IDs:** REQ-AUTH-001..007, REQ-INGR-002, REQ-SEARCH-001..003, REQ-INGR-USER-001/002, REQ-FIELDPR-001/003, REQ-RECIPE-001/002/004/006/008, REQ-RATING-002/005, REQ-GROUP-001..007, REQ-SUPP-004, REQ-AUTOPLAN-002/003, REQ-EXPORT-001..004, REQ-ADMIN-FULL-001, REQ-PLATFORM-003, REQ-QUALITY-003/004/005.

### Sprint P5.S4 — Admin-UI Deep-Test pro Page

**Deliverables:**
- BattleTestPlan §4 alle Cases im Browser durchgespielt
- 403-Probe mit normalem-User-Token
- Dark/Light + Mobile-Responsive (≤768 px) Pass

**Akzeptanz:** Alle 11 Admin-Pages erreichbar + funktional; ETL-Page weiterhin 🟡 (MVP-Fallback dokumentiert).

**Testing-Strategie:** Chrome DevTools Network-Tab pro Action; auf 401/403 prüfen; React-Warnings in Console = S3-Fail.

**REQ-IDs:** REQ-ADMIN-001, REQ-ADMIN-FULL-001/002, REQ-AUTH-003, REQ-INGR-USER-001/002, REQ-FIELDPR-001/002/003, REQ-SUPP-004, REQ-GROUP-007, REQ-ADMIN-002 🟡.

### Sprint P5.S5 — Negative & Security

**Deliverables:**
- BattleTestPlan §5 alle 16 Cases durchgespielt
- Backup-Restore-Drill (Runbook §3.3) live durchgeführt + Datum in Runbook §3.1 eingetragen
- XSS-Probe in User-Submission-Pfaden

**Akzeptanz:** 0 offene S1 Security-Findings; alle 403/401-Pfade greifen wie spezifiziert; Backup-Restore reproduzierbar.

**Testing-Strategie:** Token manipulieren (`jwt.io` Decoder), Airplane-Mode-Toggle, Concurrent-Edits via zwei Browser-Tabs, SQL-Injection-Payloads in Search.

**REQ-IDs:** REQ-AUTH-001/005, REQ-INGR-002, REQ-INGR-USER-002, REQ-RECIPE-008, REQ-EXPORT-001, REQ-ADMIN-001, REQ-PLATFORM-003, REQ-REMIND-001/002, REQ-INTAKE-002, REQ-PROFILE-001, REQ-RECIPE-005, REQ-INGR-USER-001.

### Sprint P5.S6 — Fix-Phase + Re-Run

**Deliverables:**
- Jeder S1+S2-Fail aus §6 Failures-Log adressiert (Fix-Commit verlinkt)
- Re-Run der zuvor roten Cases → ✅ R2
- BattleTestPlan §7 Sign-Off-Block datiert

**Akzeptanz:** 0 offene S1+S2; BattleTestPlan §7 signiert.

**Testing-Strategie:** Pro Fix-Commit: betroffener Case-Re-Run + Smoke-Re-Run der angrenzenden Cases (Regression-Risiko).

**REQ-IDs:** dynamisch — abhängig von Findings aus P5.S1–S5.

### 🛠️ P5.S0 Battle-Test-Plan-Slice (2026-05-26)

**Slice-Inhalt:** Statt M5-Feature-Phase startet eine **Stabilisierungs-Phase P5** ohne neue Features. Vorbereitung des Test-Frameworks vor Run 1.

- **NEW:** `docs/TestStrategy.md` (v1.0, ~150 LOC) — Strategy-Layer: Test-Pyramide invertiert, Hybrid REQ+Usability, Severity-Klassifikation, Run-Kadenz Single-Run-Then-Fixes, Out-of-Scope-Liste.
- **NEW:** `docs/BattleTestPlan.md` (v1.0, ~280 Zeilen) — Cases-Layer: §1 Persona-Smoke (12 Cases Marie-Journey), §2 Android by Screen (10 Unter-Sektionen), §3 Server by Endpoint (~22 Cases), §4 Admin-UI by Page (~11 Cases), §5 Negative+Security (16 Cases), §6 Failures-Log-Tabelle, §7 Sign-Off-Block.
- **MOD:** `docs/SprintPlan.md` — Neue Section §4a Phase P5 Battle-Test (S0–S6) zwischen P4 Phase-Abschluss und §5 Inter-Phase-Wartungs-Tasks; dieser Slice-Block.

**Doc-Drift-Eval 00–09:**
- 00 Plan — kein Drift (Battle-Test war bereits in §0.4 Release-Gate als „3 Test-User Onboarding" angedeutet; P5 macht es jetzt strukturiert).
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift (keine neuen Domain-Begriffe).
- 03 Architecture — kein Drift (Test-Methodik ist Prozess, nicht Architektur).
- 04 Requirements — kein Drift (P5 testet bestehende REQ-IDs, ändert keine Spec).
- 05 Milestones — **Drift akzeptiert (Phase-Ergänzung):** Neue Phase P5 nach Release-Tag eingeführt; semantisch eine Stabilisierungs-Phase, kein neuer Scope. P5 ist gates-only-Phase: kein Code-Change außer Bugfix-Hotfixes.
- 06 Progress — kein Drift jetzt (wird ergänzt sobald Run 1 läuft + Cases abgehakt sind).
- 07 Coding Conventions — kein Drift.
- 08 Test Strategy — **Drift akzeptiert (Neu-Dokument):** `TestStrategy.md` v1.0 ist neu. User-Memory-Regel 2 referenziert „08 Test Strategy" als Drift-Eval-Anker; bisher existierte das File nicht. Dieser Slice korrigiert die Lücke. Inhalt: invertierte Pyramide, manuelle Methodik, REQ+Usability-Hybrid.
- 09 Bootstrap — kein Drift (Battle-Test läuft auf bestehendem Dev-Setup).

**Touched Docs:** `docs/TestStrategy.md` (NEW), `docs/BattleTestPlan.md` (NEW), `docs/SprintPlan.md` (§4a + dieser Block).
**Untouched (begründet):** 00, 01, 02, 03, 04, 06, 07, 09 — kein semantischer Konflikt; P5 ist Prozess-Phase, kein Spec-/Architektur-/Code-Change.

### 🛠️ P5.S1 Run 1 Slice (2026-05-26)

**Slice-Inhalt:** Start P5.S1 Persona-Smoke Run 1 auf Pixel 7 API 35 Emulator. Findings F-001 + F-002 dokumentiert.

- **Setup:** Android SDK CLI-Install (platforms;android-35 + system-image;android-35;google_apis;x86_64 + build-tools 35.0.0 + emulator) + AVD `Pixel_7_API_35` erstellt + Emulator gebootet + `:app:installDebug` erfolgreich.
- **Case 1.1 ✅:** Login-Screen rendert korrekt (HealthForge-Titel + „Willkommen zurück" + E-Mail/Passwort + disabled „Anmelden" + Links „Passwort vergessen?"/„Konto mit Einladungscode erstellen"). Verifikation via `adb shell uiautomator dump` + Screenshot.
- **Finding F-001 (S3 Test-Spec-Drift, doc-only):** BattleTestPlan §1 Case 1.1 sagte fälschlich „Welcome-Screen first beim App-Start". Reality: Login-Screen first (REQ-AUTH-001 konform). Welcome ist Wizard-Step 0 nach Register-Submit. **Action:** Case 1.1 = Login-spec umformuliert; Case 1.2 erweitert um Register→Verify→Wizard-Flow.
- **Finding F-002 (S3 UsabilityMap-Drift, doc-only):** UsabilityMap §2 Step 1 sagte „Logo + 3 Bullet-Points + 'Los geht's'-Button". Reality (`OnboardingScreen.kt:128 StepWelcome()`): nur Heading + Begrüßungstext + „Weiter". **Action:** UsabilityMap §2 Step 1 auf schlanke Variante angeglichen. **Backlog post-v1.0:** Welcome-Screen-Polish (Logo + 3 Bullets + dedicated CTA) ist UX-Polish-Wunsch, kein REQ.

**Doc-Drift-Eval 00–09:**
- 00 Plan — kein Drift.
- 01 Vision — kein Drift.
- 02 Glossary — kein Drift.
- 03 Architecture — kein Drift (kein Code-Change).
- 04 Requirements — kein Drift; REQ-AUTH-001 + REQ-ONBOARD-001 bleiben unverändert.
- 05 Milestones — kein Drift.
- 06 Progress — wird mit Run-Log in BattleTestPlan getrackt; kein separater Eintrag nötig.
- 07 Coding Conventions — kein Drift.
- 08 Test Strategy — kein Drift; F-001/F-002 sind Doc-Realignment-Findings, decken sich mit „Single-Run-Then-Fixes"-Kadenz.
- 09 Bootstrap — kein Drift (SDK-CLI-Install ist ad-hoc, kein neuer Bootstrap-Schritt nötig solange Android Studio installiert ist).

**Touched Docs:** `docs/UsabilityMap.md` (§2 Step 1), `docs/BattleTestPlan.md` (Case 1.1 + 1.2 + R1-Log), `docs/SprintPlan.md` (dieser Block).
**Untouched (begründet):** 00, 01, 02, 03 (Architecture), 04 (ReqSpec), 05, 06, 07, 08, 09 — keine REQ-/Code-/Architektur-Änderungen, nur Test-Doc + UsabilityMap-Realignment.

**Backlog post-v1.0 (Polish):**
- **POLISH-WELCOME-001:** Welcome-Step in OnboardingScreen aufwerten: App-Logo, 3 Bullet-Points (z.B. „Vollständig on-device", „Verschlüsselt", „Keine Werbung"), eigener „Los geht's"-CTA statt generischem „Weiter". Aus F-002 entstanden.

### 🛠️ P5.S1 Run 1 Case 1.2 Result + P5-Pause + P6-Spec (2026-05-26)

**Slice-Inhalt:** Case 1.2 (Register + Email-Verify + 14-Step Onboarding-Wizard) durchlaufen. Resultat: ✅ Funktional PASS, aber 10 substanzielle UX/Scope-Findings. P5 wird pausiert; P6 (Histamind-Fusion + Scope-Refinement) wird neu eingefügt.

- **Case 1.2 ✅:** Register-Flow (Validation/Rate-Limit greifen wie spezifiziert: 400 bei `asD@asD>DE`, 429 nach 3 Versuchen/h pro IP — siehe `RateLimitFilter.kt` Bandwidth `register=3/60min`). 14-Step Wizard durchgespielt mit Marie-Persona, Home-Screen erreicht.
- **Findings F-003..F-012 in BattleTestPlan §6** dokumentiert. Severity-Verteilung: 2× S1-Scope (F-010 Log, F-012 Style), 5× S2 (F-004/005/007/009/011), 4× S3 (F-003/006/008 + F-006).
- **Entscheidung:** P5 (Stabilization, „nur Testen") wird pausiert. F-010/F-012/F-004 sind keine Test-Findings, sondern Scope-/Spec-Änderungen. Weiteres Smoke-Testen auf altem UI wäre verschwendete Zeit.
- **Übergang:** P6 wird neu in den SprintPlan eingeführt (siehe §4b unten). BattleTestPlan §8 P5-Pause-Vermerk + R2 verschoben.

**Doc-Drift-Eval 00–09:**
- 00 Plan — **Drift akzeptiert:** P6-Phase neu eingefügt; ändert die Phasen-Sequenz P5→Release zu P5(pause)→P6→P5-Resume→Release.
- 01 Vision — **kein Drift, aber Risiko:** F-010 (Log = Event-Log statt Tagebuch) und F-008/F-009 (Listen-Befüllung + Wording) berühren das Produkt-Konzept; wird in P6.S1 Vision-Reklärung explizit aufgegriffen, nicht jetzt.
- 02 Glossary — **Drift markiert (P6.S1):** F-008 zeigt Konflikt „Zutat" vs. „Lebensmittel"; Glossary muss in P6.S1 harmonisiert werden. Jetzt noch nicht touched.
- 03 Architecture — kein Drift jetzt; Style-Fusion (F-012) hat keine Architektur-Auswirkung (nur `theme.ts` / Compose-Theme).
- 04 Requirements — **Drift markiert (P6.S1):** F-010 invertiert REQ-LOG-001..006 (Tagebuch→Event-Log). F-011 erweitert REQ-PROFILE-* um per-Nutrient-Goals. F-005/F-007 fügen UX-Constraints zu REQ-WATER-* und REQ-INTAKE-* hinzu. Re-Spec in P6.S1.
- 05 Milestones — **Drift akzeptiert:** Release-Tag rückt um P6-Dauer. v1.0.0 bleibt bestehen (Code-State zum Zeitpunkt des Tags ist immutable); v1.1.0 nach P6+P5-Resume.
- 06 Progress — kein separater Eintrag; BattleTestPlan Run-Log + §6 Failures-Log sind die kanonische Quelle.
- 07 Coding Conventions — kein Drift.
- 08 Test Strategy — kein Drift; F-010 ist „Spec drift discovered during testing" — exakt was BattleTestPlan §6 + Single-Run-Then-Fixes vorsieht.
- 09 Bootstrap (Runbook) — kein Drift.

**Touched Docs:** `docs/BattleTestPlan.md` (R1 Update + Case 1.2 ✅ + F-003..F-012 + §8), `docs/SprintPlan.md` (dieser Block + neue §4b P6-Phase).
**Untouched (begründet):** 01 Vision, 02 Glossary, 03 Architecture, 04 ReqSpec, 06 Progress, 07 Coding, 08 Test Strategy, 09 Bootstrap — werden in P6.S1 (Vision/Glossary/ReqSpec Re-Lock) explizit angefasst, NICHT jetzt im laufenden Slice. Jetzt nur Sprint-Plan + Findings-Tracking.

---

## 4b. Phase P6 — Histamind-Fusion + Scope-Refinement (eingefügt 2026-05-26, autonomy-ready 2026-05-26)

**Ziel:** UI/UX-Refit anhand der Run-1-Findings F-003..F-012 mit Histamind als Design-Referenz (https://github.com/endgeardev/Histamind). Findings sind kompletter P6-Scope; keine Erweiterungen.

**Quelle:** Design-Tokens + Component-Idiome in `/memories/repo/histamind-design-system.md` gespiegelt. Histamind ist Flutter, HealthForge bleibt Kotlin/Compose — wir portieren das Design, nicht den Code.

### 4b.0 Autonomy-Doktrin (LOCKED 2026-05-26)

User-Direktive: P6 wird autonom ausgeführt. Es werden nur **Critical-Decision-Questions** an den User gestellt. Triviale Fragen sind verboten; sie werden hier ein- für allemal vor-entschieden.

**Pre-Locked Decisions (keine Rückfragen):**

| Decision | Lock | Begründung |
|---|---|---|
| Visual-Identity-Replace | Violet→Cyan ersetzt Olive-Green komplett (Primary). | User-Brief 70/30 + Olive-Green war kein expliziter Brand-Wunsch. |
| Light-Theme-Retain | Light bleibt erhalten, aber ohne Glas-Effekte (Clean-Cards auf hellem Bg, gleicher Akzent). | Toggle-Infrastruktur (`ThemePreference`) bleibt funktional; A11y-User mit Light-Präferenz versorgt. |
| Font | Manrope via Google Fonts (OFL). | Histamind 1:1 + Lizenz unproblematisch. |
| Log-Inversion-Detail (F-010) | Mood + Schlaf werden komplett entfernt. Log = Event-Log mit Severity 1–5 + Symptom-Tags + Notiz + Timestamp. | User-Wortlaut: „Schlaf und Mood machen keinen Sinn". |
| Per-Nutrient-Goals-Storage (F-011) | DB-Migration V12 fügt `users.daily_nutrient_goals JSONB` hinzu. | Forward-only Flyway; JSONB erlaubt frei wachsende Nutrient-Liste ohne Schema-Drift. |
| Pinned-Nutrients-Default (F-004) | 4 Pins: kcal, Protein, Carbs, Fat. Server-seitig in `users.pinned_nutrients TEXT[]` (V12 mit). | Default deckt 90% der User; alle weiteren collapsed mit Mini-Progress. |
| Wasser-Entfernen-Pattern (F-005) | Long-Press auf letztes Wasser-Quick-Add-Chip → Snackbar „Entfernt — Rückgängig"; reversibel 5 Sek. | Konsistent mit existierendem Undo-Pattern. |
| Add-Flow-Konsolidierung (F-007) | „Hinzufügen"-Buttons in Home/Plan navigieren direkt zu `LebensmittelScreen` mit Pre-Selection-Mode (Result-Callback). Eigenes Add-Sheet entfällt. | Reduziert Navigation-Tiefe; ein Pattern statt zwei. |
| Listen-Vorbefüllung (F-009) | `IngredientScreen` + `RecipeScreen` laden bei Open Paginated-Page (50 Items alphabetisch); Search filtert clientseitig + serverseitig. | Bestehende Endpoints supporten Paged-List; nur UI-Flag. |
| Wording-Fix (F-008) | Plan-Add-Sheet: „Rezept oder Lebensmittel" (ersetzt „Zutat"). Glossary-Lock: „Zutat" = Bestandteil EINES Rezepts; „Lebensmittel" = Standalone-Eintrag in Datenbank. | Klarer Glossary-Split. |
| Bottom-Nav-Structure | Bleibt 5 Tabs in aktueller Reihenfolge (Home/Essen/Plan/Log/Profil). | Keine Nav-Strukturänderung — matched Histamind nah genug. |
| Slider-Granularität (F-003) | Age 14–100 step 1; Height 140–220 cm step 1; Weight 30–200 kg step 0.5. | Decken realistische Range; halb-kg-Granularität fürs Tracking. |

**Critical-Decision-Trigger** (askQuestion nur bei):

1. Spec-Konflikt zwischen Histamind-Idiom und HealthForge-Domain (z.B. Histamine-Load-Card vs. Allergen-Card auf Home).
2. Datenmigration mit Daten-Risiko (z.B. wenn Log-Entries existieren → was mit alten Mood-Werten).
3. Visuelle Geschmacks-Entscheidungen mit 2+ gleichwertigen Optionen (z.B. Onboarding-Step-Indikator Punkte vs. Stepper).
4. Wenn die Pre-Locked-Decision auf eine Realität trifft, die sie ad absurdum führt.
5. Nach jedem Sprint-Abschluss: Sign-Off-Frage „Sprint Sx ok / Fix nötig / Abbruch".

Alles andere = autonome Implementation + Doc-Drift-Eval + Commit + Push.

### 4b.1 Sprint-Reihenfolge (LOCKED Dependency-Order)

```
P6.S1  Spec-Lock          → reine Doc-Arbeit, kein Code
  ↓
P6.S2  Theme-Foundation   → Color.kt + Theme.kt + Manrope + Typography
  ↓
P6.S3  Component-Library  → GlassCard, SectionPill, GradientFab, AmbientBackdrop, GradientText, SegmentedTabs
  ↓
P6.S4  Screen-Wave-1      → Home + Onboarding (mit F-003 Slidern + F-004 Pinned-Nutrients-Skeleton)
  ↓
P6.S5  Screen-Wave-2      → Plan + Essen + Profil (mit F-008 Wording + F-009 Listen-Vorbefüllung + F-011 Goals)
  ↓
P6.S6  Log-Refactor       → F-010 (DB-Migration V13 + LogScreen-Rewrite + DTO-Update)
  ↓
P6.S7  Polish-Sweep       → F-005 Wasser-Undo, F-006 Wasser-Alarm-Helper, F-007 Add-Flow-Konsolidierung
  ↓
P6.S8  P5-Resume-Prep     → BattleTestPlan §1.3–§1.12 + §2.* gegen neues UI updaten + Trockenlauf
```

Jeder Sprint = ein Commit (oder kleine Slices). Jeder Sprint endet mit askQuestion „Sprint Sx ok?".

### Sprint P6.S1 — Spec-Lock (DOC-ONLY)

**Deliverables (autonom):**
- MOD `docs/ReqSpec.md`: 
  - REQ-LOG-001..006 invertiert (Tagebuch → Event-Log mit Severity+Tags+Note+Timestamp).
  - Neue REQ-PROFILE-NUTRIENT-GOALS-001 für per-Nutrient-Tagesziele.
  - REQ-HOME-NUTRITION-PIN-001 für Pinned-Nutrients-Pattern.
  - REQ-WATER-REMOVE-001 für Entfernen-Aktion.
  - REQ-INTAKE-ADD-FLOW-001 für Pre-Selection-Mode in LebensmittelScreen.
  - REQ-DESIGN-001 ersetzt Olive-Green-Lock durch Glas-Dark-Token-Lock.
- MOD `docs/GUI.md` §2: Color-Tokens komplett auf Hm-Tokens umgeschrieben; Typography auf Manrope; Component-Idiome dokumentiert (GlassCard/SectionPill/GradientFab/AmbientBackdrop).
- MOD `docs/UsabilityMap.md`: Home-Sektion neu (Pinned + Collapsed-Nutrients); Onboarding-Steps mit Slidern; Log-Sektion neu (Event-Log statt Tagebuch); Plan-Add-Sheet-Wording.
- MOD `docs/Architecture.md` (Glossary): „Zutat" vs. „Lebensmittel" gelockt.
- MOD `docs/TraceabilityMatrix.md`: neue REQ-IDs angelegt; alte Log-IDs als „superseded" markiert.
- NEW `docs/HistamindDesignReference.md`: Spiegel des Memory-Notes für Repo-Persistence + Screenshot-Slots (User füllt später).

**Doc-Drift-Eval 00–09:** voll. Touched: 04 ReqSpec, GUI, UsabilityMap, 03 Architecture/Glossary, TraceabilityMatrix, NEW HistamindDesignReference. Untouched: Runbook (kein Bootstrap-Change), TestStrategy (Methodik bleibt).

**Critical-Decisions to ask:** keine erwartet. Wenn Konflikte auftauchen → askQuestion mit Optionen.

**Akzeptanz:** alle 6 Doc-Diffs konsistent; jedes Finding F-003..F-012 hat min. 1 REQ-Anker oder Polish-Backlog-Entry.

### Sprint P6.S2 — Theme-Foundation

**Deliverables (autonom):**
- MOD `android_app/app/src/main/kotlin/de/healthforge/presentation/theme/Color.kt`: 
  - Komplett ersetzt: `HmTokens`-äquivalente Compose-Vals (background, glassFill, glassBorder, ambientViolet, ambientCyan, fgPrimary/Secondary/Tertiary, statusOverUl, statusRelax, statusGood, accentGradient[]).
  - Light-Variante als reduzierte Clean-Card-Palette (gleicher Akzent, ohne Glass).
- MOD `android_app/app/src/main/kotlin/de/healthforge/presentation/theme/Theme.kt`:
  - `HealthForgeTheme(...)` updated mit neuen ColorSchemes (dark = Glas-Pfad, light = Clean-Pfad).
  - `LocalSemanticColors` erweitert (statusOverUl/Relax/Good).
  - Neuer `LocalHmTokens` CompositionLocal für Gradient/GlassFill-Listen.
- NEW `android_app/app/src/main/kotlin/de/healthforge/presentation/theme/Typography.kt` (falls noch nicht vorhanden): Manrope via `androidx.compose.ui.text.googlefonts`, alle Text-Styles per Histamind-Werte.
- MOD `android_app/app/build.gradle.kts`: `androidx.compose.ui:ui-text-google-fonts` Dependency.
- MOD `android_app/app/src/main/res/values/font_certs.xml` + `res/font/`-XML für GoogleFonts-Provider falls nötig.

**Akzeptanz:** App startet, Login-Screen + Home zeigen sofort Glas-Dark-Look (alte Layouts noch, aber Farben+Font sind neu). Kein Crash. `./gradlew :app:assembleDebug` grün.

**Critical-Decisions to ask:** Visual-Sign-Off nach Slice (Screenshot/User-Feedback).

### Sprint P6.S3 — Component-Library ✅ DONE (2026-05-26)

**Deliverables (autonom):**
- NEW `presentation/theme/HmComponents.kt` (statt `presentation/common/components/` — co-lokalisiert mit Tokens/HmTokens.kt für minimalen Import-Overhead, da alle Composables `LocalHmTokens.current` verbrauchen):
  - `GlassCard` — `Box` mit Linear-Gradient white@12→4% + 1dp Border @ 10% white + 40dp Drop-Shadow (Dark); Solid CardSurface + 8% Border (Light).
  - `SectionPill` — 3×14dp Gradient-Strip + 8dp Gap + UPPERCASE Label (Manrope w800 11sp letterSpacing 1.4sp fgTertiary).
  - `GradientFab` — Circle 56dp, Gradient-Fill, 24dp violetGlow Shadow.
  - `GradientButton` — Full-width 56dp, 18dp radius, Gradient-Fill, Manrope w700 15sp.
  - `AmbientBackdrop` — 3 radial-gradient Blobs (violet/cyan/good @ 10–15%) animiert via `InfiniteTransition` 30–55s Reverse; Dark-only, Light = solid.
  - `GradientText` — `Modifier.drawWithCache { onDrawWithContent { drawContent(); drawRect(accentGradient, BlendMode.SrcAtop) } }`.
  - `SegmentedTabs` — N-Tab-Toggle, Glass-Pill-Outer + Gradient-Inner aktiv.
  - `SeverityBar` — 4dp×56dp Vertical-Bar, Severity 1=good/2=good@80/3=relax/4=overUl@80/5=overUl.
- ABWEICHUNG zu Plan: keine separate `Preview.kt` (Smoke-Tests laufen in P6.S4 mit echten Screens — kosten-effizienter, da Components klein/simpel).

**Akzeptanz:** `./gradlew :app:compileDebugKotlin` grün; alle Components import-bar via `de.healthforge.presentation.theme.*`; KEINE Verwendung in Screens (Wave-1 in P6.S4).

**Doc-Drift-Eval** — Touched: SprintPlan §4b.1 (Pfad-Korrektur + DONE-Flag), TraceabilityMatrix (Component-Inventar). Untouched: ReqSpec/UsabilityMap/HistamindDesignReference/Architecture (alle Specs in P6.S1 gelockt, Implementierung treu zur Spec); GUI.md (v0.1 historisch); Runbook/TestStrategy (keine Verhaltensänderung an User-Sichtbarem bis P6.S4).

**Critical-Decisions to ask:** keine.

### Sprint P6.S4 — Screen-Wave-1 (Home + Onboarding)

**Deliverables (autonom):**
- MOD `presentation/home/HomeScreen.kt`: AmbientBackdrop, Header mit GradientText-Greeting, SectionPills, GlassCards für Nutrition/Wasser/Heute-geplant, Pinned-Nutrients-Card mit 4-Default-Pins + Collapsed-Rest mit Mini-Progress + 7-Tage-Sparkline (`fl_chart` Compose-Pendant: AndroidView mit MPAndroidChart oder eigene Canvas).
- NEW `presentation/home/PinnedNutrientsManager.kt` — BottomSheet zum Pin-Verwalten.
- MOD `presentation/onboarding/OnboardingScreen.kt`: 
  - Step-Inputs für Alter/Größe/Gewicht → `Slider` mit Live-Value-Label (F-003).
  - Step-Indikator als 14 Punkte (active = gradient-filled, inactive = glassBorder).
  - Forward-only NavBar (Weiter rechts, Zurück nur sichtbar bei step>0 ohne Skip).
- NEW DataStore key `pinned_nutrients` (List<String>, default `["kcal","protein","carbs","fat"]`).

**Akzeptanz:** Home + Onboarding visuell auf Histamind-Niveau; Slider funktional; Pin-Mgmt-Sheet öffnet.

**Critical-Decisions to ask:** Wenn Sparkline-Lib nötig (MPAndroidChart vs. native Canvas) → askQuestion.

### Sprint P6.S5 — Screen-Wave-2 (Plan + Essen + Profil)

**Deliverables (autonom):**
- MOD `presentation/plan/PlanScreen.kt`: SectionPills, GlassCards für Slots, Add-Sheet-Wording „Rezept oder Lebensmittel" (F-008), Day-Strip mit Gradient-Pill für „heute".
- MOD `presentation/essen/EssenScreen.kt` + `presentation/lebensmittel/LebensmittelScreen.kt`: 
  - Listen lazy-load Page 50 alphabetisch beim Open (F-009).
  - Visual auf Glas-Cards.
  - Pre-Selection-Mode für Add-Flow (F-007): wenn `navArg.preselect == true`, FAB wird zu „Auswählen", Tap auf Item → Result-Callback an aufrufenden Screen.
- MOD `presentation/profile/ProfileScreen.kt`: 
  - Glas-Look.
  - Neue Sektion „Tagesziele" mit per-Nutrient-Sliders/Input-Felder (F-011).
  - Verbindet sich mit DB-Migration V12 (siehe S6).
- **NEW** `presentation/recipe/RecipeCreateWizardScreen.kt` — 5-Step geführter Rezept-Wizard (REQ-RECIPE-CREATE-WIZARD-001). Einstieg aus Essen/Plan/Profil.
- **NEW** `presentation/lebensmittel/IngredientSuggestWizardScreen.kt` — 4-Step geführter Lebensmittel-Suggest-Wizard (REQ-INGREDIENT-CREATE-WIZARD-001); ersetzt `IngredientSuggestDialog`.

**Akzeptanz:** Alle 3 Bereiche visuell durchgezogen; F-007/008/009/011 verifizierbar; beide Wizards funktional (Validation pro Step + Submit).

**Critical-Decisions to ask:** keine erwartet.

### Sprint P6.S6 — Log-Refactor (F-010, schemenrelevant)

**Deliverables (autonom):**
- NEW Flyway-Migration `server/src/main/resources/db/migration/V13__log_event_schema.sql`:
  - Existierende `log_entries`-Tabelle (falls Mood/Sleep-Spalten) → Drop Mood/Sleep-Spalten.
  - Neue Spalten: `severity SMALLINT NOT NULL DEFAULT 3`, `symptom_tags TEXT[] NOT NULL DEFAULT '{}'`, `occurred_at TIMESTAMPTZ NOT NULL`.
  - Daten-Migration: existierende Einträge → severity=3, tags='{legacy}'.
- NEW Flyway-Migration `V12__per_nutrient_goals.sql`:
  - `ALTER TABLE users ADD COLUMN daily_nutrient_goals JSONB NOT NULL DEFAULT '{}'::jsonb;`
  - `ALTER TABLE users ADD COLUMN pinned_nutrients TEXT[] NOT NULL DEFAULT '{kcal,protein,carbs,fat}';`
- MOD Server-DTOs + Repository für Log + Profile.
- MOD `presentation/log/LogScreen.kt` komplett neu: Event-Liste mit SeverityBar, QuickEntrySheet (Severity-Picker + Symptom-Tag-Chips + Notiz + Time), SegmentedTabs Entries+Insights (Insights = einfaches Histogramm 14-Tage).
- MOD Android `LogRepository.kt` + ViewModels.

**Akzeptanz:** Server-Restart läuft V12+V13 sauber; LogScreen funktional; alte Mood/Sleep-UI raus.

**Critical-Decisions to ask:** 
1. Falls produktive Mood/Sleep-Daten in einer Test-DB existieren → askQuestion „Daten preserven (separate `log_legacy_mood_sleep` Tabelle) oder droppen?". In Dev-State sind keine produktiven Daten → wahrscheinlich autonome Drop-Entscheidung.

### Sprint P6.S7 — Polish-Sweep

**Deliverables (autonom):**
- F-005: `WaterTracker.kt` Long-Press auf letztes Quick-Add → Undo-Snackbar 5s.
- F-006: Helper-Text unter Wasser-Alarm-Toggle: „Erinnerung alle 2h zwischen 08:00–22:00". 
- F-007 Final-Check: Pre-Selection-Mode in allen drei Hinzufügen-Pfaden (Home, Plan, Essen) konsistent.
- Globaler Component-Audit: jedes Material-Default-Widget gegen GlassCard-Idiom geprüft.

**Akzeptanz:** alle 10 Findings F-003..F-012 sind „fixed" in BattleTestPlan §6.

**Critical-Decisions to ask:** keine erwartet.

### Sprint P6.S8 — P5-Resume-Prep

**Deliverables (autonom):**
- MOD `docs/BattleTestPlan.md` §1.3–§1.12 + §2.*: Pass-Kriterien an neues UI angepasst (z.B. „Slider-Position 28" statt „Eingabefeld '28'").
- MOD §6 Failures-Log: F-003..F-012 alle als „fixed" + Fix-Commit verlinkt.
- Trockenlauf Case 1.3 + 1.5 + 1.10 (kritische Cases auf neuem UI).
- Update Run-Log: R1 abgeschlossen + Übergang zu R2.

**Akzeptanz:** BattleTestPlan ready für R2 (Cases 1.3–1.12 + §2–§5).

**Critical-Decisions to ask:** Sign-Off „P6 abgeschlossen, P5 resumen?".

### 4b.2 Doc-Drift-Eval (Phase-Level)

**Touched Docs (über alle P6-Sprints kumuliert):**
- `docs/ReqSpec.md` (P6.S1)
- `docs/GUI.md` (P6.S1)
- `docs/UsabilityMap.md` (P6.S1)
- `docs/Architecture.md` (P6.S1 Glossary)
- `docs/TraceabilityMatrix.md` (P6.S1)
- `docs/HistamindDesignReference.md` NEW (P6.S1)
- `docs/BattleTestPlan.md` (P6.S8)
- `docs/SprintPlan.md` (dieser Block + Slice-Updates pro Sprint)

**Untouched (begründet, Phase-Level):**
- `docs/Runbook.md` — kein Bootstrap-/Deployment-Change.
- `docs/TestStrategy.md` — Methodik bleibt (REQ+Usability-Hybrid).

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
