# HealthForge — Traceability Matrix

**Version:** 0.1 (LOCKED — Mapping aller REQ-IDs aus ReqSpec v0.2 auf geplante Implementation-Files)
**Datum:** 2025-05-25
**Vorgängerdokumente:** [ReqSpec.md](ReqSpec.md) v0.2, [Architecture.md](Architecture.md) v0.2, [GUI.md](GUI.md) v0.1

> Diese Matrix verfolgt den Implementierungsstatus jeder Requirement-ID. Sie wird **nach
> jeder erledigten Aufgabe** aktualisiert (per Workflow-Rule).

---

## Status-Legende

| Symbol | Bedeutung |
|---|---|
| ❌ | Nicht gestartet |
| 🟡 | In Arbeit / teilweise implementiert |
| ✅ | Vollständig implementiert + manuell verifiziert |
| ⏭ | In dieser Phase nicht relevant (Meta-REQ, z.B. Vision) |

## Phase-Legende

| Phase | Zeitfenster |
|---|---|
| **P1** | Foundation: Auth, Profile, Lebensmittel-DB, Supplements (lokal), Home, Onboarding |
| **P2** | Recipes: Rezepte CRUD, Plan-Tab manuell, Community-Ratings |
| **P3** | Community: Gruppen, Log/Tagebuch, Reminders, Export, Reports, Wasser-Reminders |
| **P4** | Power: User-Ingredients, Field-PR, Auto-Planner, Insights, Full Admin UI (Barcode-Scanner entfernt) |
| **META** | Übergreifend (keine Code-Datei direkt) |

---

## §3 Vision & Scope (Meta — keine Code-Datei)

| REQ-ID | Phase | Status | Implementation-File / Notiz |
|---|---|:-:|---|
| REQ-VISION-001 | META | ⏭ | Strategisches Statement |
| REQ-VISION-002 | META | ⏭ | Strategisches Statement |
| REQ-VISION-003 | META | ⏭ | Strategisches Statement |
| REQ-VISION-004 | META | ⏭ | Garantie durch REQ-PROFILE-001/002 + REQ-INTAKE-002 + REQ-LOG-001 |
| REQ-VISION-005 | META | ⏭ | Realisiert durch REQ-GROUP-001..007 |

## §3 Persona (Meta)

| REQ-ID | Phase | Status | Implementation-File / Notiz |
|---|---|:-:|---|
| REQ-PERSONA-001 | META | ⏭ | Strategisches Statement |
| REQ-PERSONA-002 | META | ⏭ | Realisiert durch REQ-QUALITY-FILTER-001 |
| REQ-PERSONA-003 | META | ⏭ | Out-of-scope-Statement |
| REQ-PERSONA-004 | META | ⏭ | Out-of-scope-Statement |

## §3 Platform / I18N / Units

| REQ-ID | Phase | Status | Implementation-File / Notiz |
|---|---|:-:|---|
| REQ-PLATFORM-001 | P1 | ✅ | `android_app/app/build.gradle.kts` (Android-only per Final-Review 2026-05-26; iOS/Web explizit out-of-scope) |
| REQ-PLATFORM-002 | P1 | ✅ | `android_app/app/build.gradle.kts` (minSdk 26, target/compile 35 — Final) |
| REQ-PLATFORM-003 | P1 | ✅ | `deploy/docker-compose.dev.yml`, `deploy/docker-compose.prod.yml`, `deploy/Caddyfile` (Prod-ready Skelett; Aktivierung im Release-Gate-Schritt) |
| REQ-I18N-001 | P1 | ✅ | `resourceConfigurations += setOf("de")` in `app/build.gradle.kts` + alle UI-Strings inline auf Deutsch (P1.S3) |
| REQ-I18N-002 | P1 | ✅ | Inline-DE-Strings in Onboarding + Profile + Auth-Screens; AllergenType/FodmapType haben `germanLabel` (P1.S3) |
| REQ-UNITS-001 | P1 | ❌ | `domain/model/Unit.kt` (g, ml, °C, piece) |

---

## §4.1 Navigation (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-NAV-001 | P1 | ✅ | `android_app/.../presentation/main/MainShell.kt` (NavigationBar 5 Items: Home/Plan/Essen/Log/Profil) |
| REQ-NAV-002 | P1 | ✅ | `android_app/.../presentation/essen/EssenScreen.kt` (TabRow 3 Sub-Tabs: Lebensmittel/Rezepte/Supplements) |
| REQ-NAV-003 | P1 | ✅ | `presentation/common/PhasePlaceholder.kt` (zentrale Komponente: Icon + Title + Description + optional PhaseLabel); `presentation/plan/PlanScreen.kt` → P2-Label „Mahlzeiten-Wochenplaner“; `presentation/log/LogScreen.kt` → P3-Label „Symptom-Tagebuch“; `EssenScreen.SubTabPlaceholder` für Rezepte+Supplements (P1.S8 refactor) |
| REQ-NAV-004 | P3 | ✅ | `presentation/log/LogScreen.kt` (Symptom-Tagebuch vollständig in P3.S1+P3.S4); Verlauf-Button: `HomeScreen.kt` TopAppBar action → `MainRoutes.INTAKE_HISTORY` |

## §5.1 Auth (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-AUTH-001 | P1 | ✅ | `server/auth/AuthService.kt` + `PasswordEncoderConfig` (BCrypt cost 12) — server compiles + jar built |
| REQ-AUTH-002 | P1 | ✅ | `server/auth/AuthController.kt` (InviteService + register flow, V2 invites table) |
| REQ-AUTH-003 | P1 | ✅ | `server/auth/AuthController.kt::InviteAdminController` + `admin-ui/src/pages/InvitesPage.tsx` |
| REQ-AUTH-004 | P1 | ✅ | `server/auth/AuthService.kt::verifyEmail` + `common/MailService.kt` (de-DE templates) |
| REQ-AUTH-005 | P1 | ✅ | `server/auth/JwtService.kt` (HS512, 15min Access / 30d Refresh, SHA-256 hash, rotation) |
| REQ-AUTH-006 | P1 | ✅ | `server/auth/AuthService.kt::requestPasswordReset/resetPassword` |
| REQ-AUTH-007 | P1 | ✅ | `android_app/data/prefs/SecureTokenStore.kt` (EncryptedSharedPreferences AES256_GCM) |

## §5.2 Profile (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-PROFILE-001 | P1 | ✅ | `android_app/.../data/db/entities/ProfileEntities.kt` + `data/db/SqlCipherKeyProvider.kt` + `di/DatabaseModule.kt` (P1.S3) |
| REQ-PROFILE-002 | P1 | ✅ | Server-DTO `UserDto.kt` enthält keine Allergie/Intoleranz-Felder; Android `ProfileRepository` schreibt nur lokal (P1.S3) |
| REQ-PROFILE-003 | P1 | ✅ | `server/user/UserEntity.kt` (P1.S2) |
| REQ-PROFILE-004 | P1 | ✅ | `data/db/entities/Enums.kt::AllergenType` (EU-14) + `OnboardingScreen.kt::StepAllergies` + `data/db/dao/AllergyDao.kt` (P1.S3) |
| REQ-PROFILE-005 | P1 | ✅ | `data/db/entities/Enums.kt::FodmapType` + `OnboardingScreen.kt::StepIntolerances` + `data/db/dao/IntoleranceDao.kt` (P1.S3) |
| REQ-PROFILE-006 | P1 | ✅ | `OnboardingScreen.kt` (14 Steps konsolidiert) + `domain/NutritionMath.kt` (Mifflin–St Jeor + TDEE + Macros) (P1.S3) |

## §5.3 Ingredient Database (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-INGR-001 | P1 | ✅ | `server/ingredient/IngredientEntity.kt` + Flyway `V3__ingredient_schema.sql` (Schema + JPA) + `V4__dev_seed_ingredients.sql` (15 Dev-Items mit Allergen/FODMAP-Cases; idempotent ON CONFLICT). Produktion: ETL-Pfad ersetzt Seed später. |
| REQ-INGR-002 | P1 | ✅ | `server/ingredient/IngredientSearchRepository.kt` (FTS + `hf_immutable_unaccent`), `IngredientController.search` mit `excludeAllergens` / `excludeFodmap` Filtern |
| REQ-INGR-003 | P1 | 🟡 | `server/etl/Importers.kt::SighiImporter` Code komplett; wartet auf CSV-Datei `resources/seed/sighi.csv` (externe Lizenzklärung). **Akzeptiert als MVP-Fallback** (Final-Review 2026-05-26): läuft als `SKIPPED_NO_FILE`, blockiert v1.0 nicht. |
| REQ-INGR-004 | P1 | ❌ | OFF-Filter/Dedupe-Regeln noch nicht implementiert (P1.S4.1) |
| REQ-INGR-005 | P1 | ❌ | OffScheduler + Sticky-Field-Logic noch offen (P3.S2 Field-PR-Workflow) |
| REQ-INGR-006 | META | ⏭ | Lizenzhinweis — keine Code-Datei (Out-of-scope-Garantie) |

## §5.4 Search & Filter (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-SEARCH-001 | P1 | ✅ | `server/ingredient/IngredientSearchRepository.kt` (ILIKE-Substring auf `hf_immutable_unaccent(lower(name_de))` + GIN-Trigram-Indizes V5; FTS-german war unzureichend für Compound-Wörter) |
| REQ-SEARCH-002 | P1 | ✅ | V1 (`unaccent` extension) + V3 (`hf_immutable_unaccent` IMMUTABLE-Wrapper) + ViewModel `applyProfileFilters` |
| REQ-SEARCH-003 | P1 | ✅ | `server/ingredient/IngredientController.kt::search(@RequestParam q, limit, excludeAllergens, excludeFodmap)` |
| REQ-SEARCH-004 | P1 | 🟡 | Inline-Filter in `LebensmittelViewModel.applyProfileFilters()`/`toggleApplyProfileFilters()`. **Akzeptiert als MVP-Fallback** (Final-Review 2026-05-26): funktional komplett; UseCase-Extraktion ist Post-v1.0 Refactor-Kandidat, kein Verhalten-Unterschied. |
| REQ-SEARCH-005 | P1 | ✅ | `LebensmittelScreen.kt::IngredientRow` rendert Histamin-Score + FODMAP-AssistChips (German-Labels via `FodmapType.germanLabel`); Allergene als „Enthält:“-Zeile. (Hotfix 2026-05-26) |

## §5.5 Recipes (P2)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-RECIPE-001 | P2 | ✅ Backend | `server/recipe/RecipeEntity.kt` + Flyway `V6__recipes.sql` (Client P2.S2) |
| REQ-RECIPE-002 | P2 | ✅ Backend | `RecipeEntity.authorId` FK + `RecipeService.create()` (Client P2.S3) |
| REQ-RECIPE-003 | P2 | ✅ Backend | `RecipeEntity.visibility` enum + `groupId` CHECK constraint in `V6__recipes.sql` |
| REQ-RECIPE-004 | P2 | ✅ Backend | `server/recipe/RecipeLikeEntity.kt` + `RecipeService.like()/unlike()` + `POST /v1/recipes/{id}/like` (Client P2.S2) |
| REQ-RECIPE-005 | P2 | ✅ | `RecipeService.validate()` + Client `RecipeEditViewModel.validate()` (title/servings/prep/slot_tags/ingredients/steps) |
| REQ-RECIPE-006 | P2 | ✅ | `server/media/ImageUploadService.kt` (Thumbnailator 256/800/1600) + `POST /v1/media/upload`; Client: `MediaRepository.uploadImage()` mit 1080px / JPEG Q85 / EXIF-Rotate |
| REQ-RECIPE-007 | P2 | ✅ | `server/recipe/RecipeNutritionCompute.kt` (live aus `ingredients.per_100g`, Unit-Normalisierung, `missing_ingredients`) |
| REQ-RECIPE-008 | P2 | ✅ | `RecipeService.update()/softDelete()` → `ApiException(FORBIDDEN, NOT_OWNER)`; Client: `RecipeDetailScreen` Edit-IconButton (Server-403 als Snackbar) |
| REQ-RECIPE-009 | P2 | ⏳ Server-side | `RecipeStatus.REMOVED` Soft-Delete enforced in Browse/Detail; IntakeEntry-Snapshot kommt mit P4 Plan-Tab |

## §5.6 Offline / Read-Cache (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-OFFLINE-001 | P1 | ❌ | `data/repository/IngredientRepository.kt`, `RecipeRepository.kt` (Room-first) + ETag-Handling |
| REQ-OFFLINE-002 | P1 | ❌ | `domain/usecase/RequireOnlineUseCase.kt` (zentrale Online-Pflicht-Guard) |
| REQ-OFFLINE-003 | P1 | ❌ | `presentation/common/StaleDataIndicator.kt` + `OfflineBanner.kt` |

## §5.7 Intake Log (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-INTAKE-001 | P1 | ✅ | `presentation/home/components/QuickAddDialog.kt` + `data/repository/IntakeRepository.kt` (P1.S6) |
| REQ-INTAKE-002 | P1 | ✅ | nur lokal: `data/db/dao/IntakeDaos.kt` (Room v2, kein Server-Endpoint) |
| REQ-INTAKE-003 | P1 | ✅ | `data/db/entities/IntakeEntities.kt` `snapshotName/Brand/...PerHundred` |
| REQ-INTAKE-004 | P1 | ✅ | `domain/IsIntakeEditableUseCase.kt` (7-Tage via `Duration.between`) |

## §5.8 Supplements (P1 + P3 für Peer-Review)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-SUPP-001 | P1 | ✅ | `data/db/entities/SupplementEntities.kt` (SupplementEntity) + `presentation/supplements/SupplementEditScreen.kt` + `SupplementsViewModels.kt::SupplementEditViewModel` (P1.S7) |
| REQ-SUPP-002 | P1 | ✅ | `data/db/dao/SupplementDaos.kt::SupplementDao` + `data/repository/SupplementRepository.kt` (Room v3, lokal-only) (P1.S7) |
| REQ-SUPP-003 | P1 | ✅ | Nährwerte je Dosis in `SupplementEntity` + Edit-Form; Verbuchung als Intake via `IntakeEntryEntity(sourceType=SUPPLEMENT, sourceId=supplement.id, portionGrams=defaultDose)` — getriggert aus `notification/AlarmReceiver.kt::handleTaken` (Notification-Action „Genommen") **und** aus `presentation/home/HomeViewModel.kt::markSupplementTaken` (Checkbox in HomeScreen-Checkliste) (P1.S8) |
| REQ-SUPP-004 | P3 | ✅ | Server: `server/.../supplement/SupplementEntities.kt` + `SupplementService.kt` + `SupplementController.kt` (POST `/v1/supplements/suggestions`, GET `/v1/supplements/public`) + `AdminSupplementController.kt` (Admin-Queue + approve/reject), Migrationen `V9__supplement_peer_review.sql`. Android: `data/network/SupplementApi.kt` + `SupplementRepository.suggestPublic()` + Button „Für globalen Katalog vorschlagen" in `SupplementEditScreen.kt`. Admin-UI: `admin-ui/src/pages/SupplementsQueuePage.tsx` + Route `/supplements`. (P3.S4 Slice 2) |
| REQ-SUPP-005 | P1 | ✅ | `data/db/entities/SupplementEntities.kt::SupplementReminderEntity` + `notification/AlarmScheduler.kt` + `notification/AlarmReceiver.kt` (ONCE/DAILY/WEEKLY + re-arm) (P1.S7) |
| REQ-SUPP-006 | P1 | ✅ | `presentation/essen/EssenScreen.kt` Sub-Tab "Supplements" → `SupplementsScreen` (P1.S7) |
| REQ-SUPP-007 | P1 | ✅ | `data/db/entities/SupplementEntities.kt` (Supplements sind separate Entity ohne `RecipeIngredient`-Referenz; kein Cross-Validator nötig). Rezept-Zutaten-Validierung deckt REQ-RECIPE-005 ab. (Final-Review 2026-05-26) |

## §5.9 Home (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-HOME-001 | P1 | ✅ | `presentation/home/HomeScreen.kt` + `components/MacroRing.kt` (P1.S6) |
| REQ-HOME-002 | P1 | ✅ | `domain/ComputeNutrientTargetsUseCase.kt` + `NutritionMath` (Mifflin–St Jeor) |
| REQ-HOME-003 | P1 | ✅ | `components/QuickAddDialog.kt` (Extended-FAB im HomeScreen) |
| REQ-HOME-004 | P1 | ✅ | `HomeScreen.kt`: Ringe + Liste max 5 + QuickAdd + Water + DateNav + History-Btn + Supplement-Checkliste (`presentation/home/components/SupplementChecklist.kt` + `HomeViewModel.supplementChecklist` Flow filtert auf `enabled && isDueToday(day)`; Strike-Through + grünes CheckCircle nach Tap; sortiert nach taken,hour,minute) (P1.S8) |
| REQ-HOME-005 | P1 | ✅ | `presentation/home/IntakeHistoryScreen.kt` (chronologisch + Day-Gruppen) |

## §5.12 Ratings (P1 lokal / P2 community)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-RATING-001 | P1 | ❌ | `data/local/RecipeRatingLocalDao.kt` + `IngredientRatingLocalDao.kt` |
| REQ-RATING-002 | P2 | ✅ Backend | `RecipeController::upsertCommunityRating` + `recipe_ratings_community` Tabelle (V6) (Client P2.S2) |
| REQ-RATING-003 | P1+P2 | ❌ | Trennung lokal/Server, kein gemeinsamer Constraint |
| REQ-RATING-004 | META | ⏭ | Out-of-scope-Garantie (kein Endpoint, kein UI-Element) |
| REQ-RATING-005 | P2 | ✅ Backend | `RecipeController::revokeCommunityRating` (DELETE `/v1/recipes/{id}/community-rating`) (Client P2.S2) |

## §5.13 Onboarding (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-ONBOARD-001 | P1 | ✅ | `presentation/onboarding/OnboardingScreen.kt` (14 Steps; Register ist Teil von P1.S2-AuthFlow) (P1.S3) |
| REQ-ONBOARD-002 | P1 | 🟡 | `OnboardingScreen.kt::StepAllergies/StepIntolerances` — alle Felder nullable, Skip via Weiter ohne Eingabe. Warning-Dialog bei Skip im Backlog P1.S3.1. **Akzeptiert als MVP-Fallback** (Final-Review 2026-05-26): Nutzer kann Warnungen aktiv ignorieren statt Hard-Block. |
| REQ-ONBOARD-003 | P1 | ✅ | `presentation/profile/ProfileScreen.kt::onRestartOnboarding` (P1.S3) |

## §5.x Admin (P1 Minimal + P3 Full)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-ADMIN-001 | P1 | ❌ | `admin-ui/` Vite-Setup + `server/admin/AdminAuthFilter.kt` + Caddy `admin.healthforge.endgear.de` |
| REQ-ADMIN-002 | P1 | 🟡 | Server-Endpoints `POST /admin/etl/run`, `GET /admin/etl/runs/{src}` ✅ (`server/etl/EtlController.kt`); Reports + Users (P3.S3) ✅ (`community/AdminReportController.kt`, `auth/AdminUserController.kt`, `admin-ui/src/pages/RecipeReportsPage.tsx`, `admin-ui/src/pages/UsersPage.tsx`). **ETL-UI (JobsPage) akzeptiert als Post-v1.0 Backlog** (Final-Review 2026-05-26): ETL bleibt manuell via Postman/curl testbar; explizit Drift #1 in P4.S4 dokumentiert. |
| REQ-ADMIN-003 | P3/P4 | ❌ | siehe REQ-ADMIN-FULL-001/002 |

## §6.1 Groups (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-GROUP-001 | P3 | ✅ | Backend `V7__groups.sql` + `group/GroupEntity.kt`; Client `data/network/GroupApi.kt` + `presentation/groups/GroupsScreen.kt` (TabRow Meine|Entdecken) |
| REQ-GROUP-002 | P3 | ✅ | `GroupEntity` Felder + `GroupSummaryDto`/`GroupMemberDto` mit name/description/visibility/invite_code/owner_id/member_count/my_role |
| REQ-GROUP-003 | P3 | ✅ | Backend `GroupController.kt::create/joinByCode/joinPublic/leave`; Client `GroupsScreen.kt` FAB+Join-Code-Dialog + Discover-Beitreten + `GroupDetailScreen.kt` Leave-Button |
| REQ-GROUP-004 | P3 | ✅ | Backend `removeMember/transferOwnership` (2-step demote→promote); Client `GroupDetailScreen.kt` Owner-IconButtons (Transfer/Remove) mit AlertDialog-Confirm |
| REQ-GROUP-005 | P3 | ✅ | Backend `RecipeRepository::VisibilityFilter.PublicOrOwnOrGroup` + `RecipeService.detail` membership-check; Client `RecipeEditScreen.kt` GROUP-Chip + `GroupPickerSection` |
| REQ-GROUP-006 | P3 | ✅ | `RecipeDetailScreen.kt` AssistChip `"Allgemein"|"Privat"|"Gruppe"` |
| REQ-GROUP-007 | P3 | ✅ | Server `server/.../community/RecipeReportController.kt` (POST `/v1/recipes/{id}/reports`) + `community/AdminReportController.kt` (Admin-Workflow) + `community/ReportService.kt` + Android `presentation/essen/rezepte/RecipeDetailScreen.kt` Report-Icon → Dialog; Admin-UI `admin-ui/src/pages/RecipeReportsPage.tsx` (Resolve/Dismiss/Rezept-Löschen) |

## §6.2 Meal Plan (P2)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-PLAN-001 | P2 | ✅ | `data/db/entities/MealPlanEntities.kt` (`MealPlanSlotEntity`, `MealPlanItemEntity`) |
| REQ-PLAN-002 | P2 | ✅ | `presentation/plan/PlanScreen.kt` + `SlotItemPicker` (ModalBottomSheet, Tabs Rezept/Zutat) |
| REQ-PLAN-003 | P2 | ✅ | nur Room (kein Sync) — Plan ist lokal-only per Spec |
| REQ-PLAN-004 | P2 | ✅ | `MealPlanRepository.markConsumed()` → erzeugt `IntakeEntryEntity` mit Snapshot-Nährwerten |
| REQ-PLAN-005 | P2 | ✅ | `PlanViewModel` + `PlanScreen` (DaySelectorRow 7 Tage, deleteSlot/Item) |

## §6.3 Shopping List (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-SHOP-001 | P3 | ✅ | `android_app/.../domain/shopping/BuildShoppingListUseCase.kt` — Aggregation per (ingredientId, unit); RECIPE-Items skaliert via `amount/servings`; INGREDIENT-Items direkt (Unit=g) |
| REQ-SHOP-002 | P3 | ✅ | `android_app/.../data/db/entities/ShoppingListItemEntity.kt` + `dao/ShoppingListDao.kt` + `presentation/shopping/ShoppingListScreen.kt` (Datumsbereich, Generate, Checkbox-Strike-Through) |
| REQ-SHOP-003 | P3 | ✅ | `presentation/shopping/ShoppingListScreen.kt` + `ShoppingListItemEntity.category` (Group-by-Category in UI; MVP-Fallback „Sonstiges“ für Items ohne Mapping ist akzeptiert — Final-Review 2026-05-26). |

## §6.4 Symptom-Tagebuch (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-LOG-001 | P3 | ✅ | `data/db/entities/LogEntities.kt` (`LogEntryEntity` + Sym/Tag-Joins) — Room v5 / SQLCipher, local-only |
| REQ-LOG-002 | P3 | ✅ | `LogEntryEntity` Felder + `presentation/log/LogScreen.kt` Quick-Add + `LogEntryFormScreen.kt` Edit |
| REQ-LOG-003 | P3 | ✅ | `data/db/LogDefaultSymptomSeed.kt` (15 dt. Defaults) + `presentation/log/CustomSymptomManagerScreen.kt` |
| REQ-LOG-004 | P3 | ✅ | `LogEntryDao.upsertWithChildren` (kein UNIQUE auf Datum; `id=0L` triggert Insert) |
| REQ-LOG-005 | P3 | ✅ | `presentation/log/LogChartsScreen.kt` (Compose Canvas Line-Charts: Mood + Severity-Ø; 7/30 Tage) — Vico-Migration tracked als Future-Improvement |
| REQ-LOG-006 | P3 | ✅ | `domain/IsLogEntryEditableUseCase.kt` (7-Tage); enforced in `LogFormViewModel.save()` + `LogEntryFormScreen` editable-gate |

## §6.5 Reminders (P1 Supplement / P2 Meal)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-REMIND-001 | P1 | ✅ | Supplement-Reminders ✅ (REQ-SUPP-005); Wasser-Reminder ✅ (`notification/WaterReminderScheduler.kt` + `WaterReminderPrefs.kt` + `AlarmReceiver.ACTION_WATER_FIRE` + `BootReceiver` re-schedule; Toggle in `WaterTracker.kt` über `HomeViewModel.setWaterReminderEnabled`; Fenster 08–22 lokal, Default-Intervall 2 h, opt-in per ReqSpec MAY-Klausel). Meal-Reminder deferred P2.S4b (Slot-Entity hat `timeOfDayMinutes`; akzeptierte Lazy-Spezifikation, kein v1.0-Blocker). (Hotfix 2026-05-26) |
| REQ-REMIND-002 | P1 | ✅ | `notification/AlarmScheduler.kt` (setExactAndAllowWhileIdle + Fallback) + `notification/BootReceiver.kt` (Re-Schedule nach BOOT_COMPLETED) + Manifest `RECEIVE_BOOT_COMPLETED`/`SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM` (P1.S7) |
| REQ-REMIND-003 | — | 🗑️ REMOVED (2026-05-25) | FCM gestrichen — In-App-Badge + optional Email-Digest stattdessen |
| REQ-REMIND-004 | P1 | ✅ | `notification/NotificationPermissionFlow.kt` (POST_NOTIFICATIONS Runtime-Request API 33+) + `notification/NotificationChannels.kt` (ch_supplement/meal/water) + `HealthForgeApp.onCreate` ensure (P1.S7) |

## §6.6 Wasser-Tracker (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-WATER-001 | P1 | ✅ | `presentation/home/components/WaterTracker.kt` (P1.S6) |
| REQ-WATER-002 | P1 | ✅ | `WaterTracker.kt`: +250/+500-Buttons + Custom-Dialog in `HomeScreen` |
| REQ-WATER-003 | P1 | ✅ | `presentation/profile/ProfileScreen.kt` Slider (500..5000 ml) + `ProfileViewModel.setWaterGoalMl`; gespeichert in `user_profile.waterGoalMl` (Default 2000) |
| REQ-WATER-004 | P1 | ✅ | `data/db/entities/IntakeEntities.kt` `WaterIntakeEntity` + `WaterIntakeDao.deleteById` |

## §6.7 Export (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-EXPORT-001 | P3 | ✅ | `server/export/ExportService.kt` (Server-Anteil: Account, eigene Rezepte, Supplement-Vorschläge) + `android_app/.../domain/usecase/BuildLocalExportUseCase.kt` (Lokal-Anteil: Profil, Intake, Wasser, Logs, Supplements, Reminder) |
| REQ-EXPORT-002 | P3 | ✅ | `android_app/.../presentation/profile/ExportScreen.kt` + Einstieg in `ProfileScreen.kt` ("Daten exportieren") |
| REQ-EXPORT-003 | P3 | ✅ | `ExportService.kt::buildPayload` (Server) + `BuildLocalExportUseCase.invoke()` (Lokal); zwei getrennte Dateien gespeichert in `Downloads/HealthForge/` |
| REQ-EXPORT-004 | P3 | ✅ | `ExportService.kt::toPdf` (OpenPDF 1.3.43, LGPL) + `ExportService.kt::toJson` (Jackson) + `BuildLocalExportUseCase` (Moshi) |

## §7 Power Features (P4)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-INGR-USER-001 | P4 | ✅ | `server/ingredient/IngredientController.kt::suggest` + `IngredientSubmissionService.suggest` + `admin-ui/src/pages/IngredientQueuePage.tsx` + Android `LebensmittelScreen` Suggest-Dialog |
| REQ-INGR-USER-002 | P4 | ✅ | `IngredientSearchRepository.search(viewerId)` (PENDING nur für Submitter) + `IngredientController.byId/byBarcode` Visibility-Filter |
| REQ-FIELDPR-001 | P4 | ✅ | `server/ingredient/IngredientController.kt::proposeFieldChange` + `IngredientSubmissionService.proposeFieldChange` + `admin-ui/src/pages/FieldPrPage.tsx` + Android `IngredientReviewDialogs.kt::FieldPrDialog` |
| REQ-FIELDPR-002 | P4 | ✅ | Field-PR speichert nur Pending-Vorschlag; `IngredientEntity` wird erst bei `approveFieldPr` mutiert |
| REQ-FIELDPR-003 | P4 | ✅ | `AdminIngredientReviewController` (`@PreAuthorize hasRole ADMIN`) + `RejectReviewRequest.note` persistiert in `ingredient_field_pr.review_note` |
| REQ-AUTOPLAN-001 | P4 | ✅ | `presentation/plan/PlanScreen.kt` (AutoAwesome-Button im TopBar) + `AutoPlanDialogs.kt::AutoPlanGenerateDialog` |
| REQ-AUTOPLAN-002 | P4 | ✅ | `server/autoplan/BeamSearchPlanner.kt` + `AutoPlanService.kt` (Server-side beam search) |
| REQ-AUTOPLAN-003 | P4 | ✅ | `server/autoplan/AutoPlanDtos.kt::AutoPlanGenerateRequest` (slots, exclude_allergens, prep_minutes_max, more_often, avoid, beam_width, seed) |
| REQ-AUTOPLAN-004 | P4 | ✅ | `presentation/plan/AutoPlanDialogs.kt::AutoPlanPreviewScreen` + `AutoPlanViewModel.removeSlot/commit` |
| REQ-INSIGHT-001 | P4 | ✅ | `android_app/.../domain/insights/LiftCorrelationCalculator.kt` (`INSIGHT_MIN_LOG_DAYS=14`, Lock-Screen in `InsightsScreen.kt::LockedPane`) |
| REQ-INSIGHT-002 | P4 | ✅ | `LiftCorrelationCalculator.kt` Thresholds `INSIGHT_MIN_LIFT=1.5`, `INSIGHT_MIN_N=3` |
| REQ-INSIGHT-003 | P4 | ✅ | `LiftCorrelationCalculator.kt::compute` — `score = lift × (avgSeverity/5)` |
| REQ-INSIGHT-004 | P4 | ❌ | Garantie: keine Network-Aufrufe in Insights-Modul (Lint-Check empfohlen) |
| REQ-BARCODE-001 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut: no barcode scanner |
| REQ-BARCODE-002 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut |
| REQ-BARCODE-003 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut |
| REQ-ADMIN-FULL-001 | P3/P4 | ✅ | Sidebar-Layout: `admin-ui/src/components/Layout.tsx`; Pages: `DashboardPage.tsx`, `StatisticsPage.tsx`, `AuditLogPage.tsx`, plus bestehende Queues (Invites/Reports/Ingredients/Field-PRs/Supplements/Users). Server: `de/healthforge/admin/AdminStatsController.kt` (`/admin/v1/stats/dashboard` + `/statistics`), `AdminAuditController.kt` (`/admin/v1/audit` mit Filter actor/action/from/to/limit). |
| REQ-ADMIN-FULL-002 | P1 | ❌ | `users.role` ENUM-Spalte; Setzen nur via DB-SQL (kein UI) |

## §10 Data Quality (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-QUALITY-001 | P1 | ❌ | `dataQuality`-Spalte noch nicht modelliert (kommt mit Vollständigkeits-Trigger in P1.S5) |
| REQ-QUALITY-002 | P1 | ❌ | `allergensKnown` Bool noch nicht modelliert — derzeit nur `allergens_json` (P1.S5) |
| REQ-QUALITY-003 | P1 | ✅ | `IngredientEntity.histamineScore: Short?` (0..3) + V3-Schema `CHECK (histamine_score BETWEEN 0 AND 3)` + V4-Seed-Daten; angezeigt in `LebensmittelScreen.kt::IngredientRow` |
| REQ-QUALITY-004 | P1 | ✅ | `IngredientEntity.fodmapFlagsJson` (TEXT JSON, default `[]`) + V3-Schema + V4-Seed-Daten; gerendert als AssistChips in `LebensmittelScreen.kt::IngredientRow` (FODMAP-Quality-Badges, Hotfix 2026-05-26) |
| REQ-QUALITY-005 | P1 | ✅ | `IngredientEntity.source: IngredientSource` ENUM (BLS/SIGHI/OFF/USER/MANUAL) |
| REQ-QUALITY-UI-001 | P1 | ✅ | `presentation/lebensmittel/LebensmittelScreen.kt::IngredientRow` zeigt FODMAP-AssistChips (`FodmapType.germanLabel`) + Histamin-Score-Text + Allergen-Zeile + Quelle. (Hotfix 2026-05-26 — ehemaliges `IngredientDetailScreen.kt` konsolidiert in LebensmittelScreen.) |
| REQ-QUALITY-UI-002 | P2 | ❌ | `presentation/essen/rezepte/RecipeDetailScreen.kt::QualityRollupBanner` |
| REQ-QUALITY-FILTER-001 | P1 | ❌ | `BuildSearchFiltersUseCase.kt` (`excludeUnknownAllergens=true` default) |
| REQ-QUALITY-FILTER-002 | P1 | ❌ | `presentation/essen/lebensmittel/FilterDialog.kt` (Toggle + Warning-Chip in Result) |
| REQ-QUALITY-FIX-001 | P1 | ❌ | `admin-ui/src/pages/IngredientEditorPage.tsx` (Edit-Form) |
| REQ-QUALITY-FIX-002 | P4 | ✅ | siehe REQ-FIELDPR-001..003 |

---

## Aggregierte Statistik (Final-Review 2026-05-26)

| Kategorie | Count | Anteil von 133 |
|---|---:|---:|
| ✅ Vollständig | **106** | 79.7 % |
| 🟡 In Arbeit / MVP-Fallback (alle akzeptiert) | 4 | 3.0 % |
| ❌ Backlog Post-v1.0 | 18 | 13.5 % |
| ⏳ In-flight | 1 | 0.8 % |
| 🗑️ Removed (Scope-Cut) | 4 | 3.0 % |
| **Implementierbare REQ-IDs (Σ)** | **133** | 100 % |
| ⏭ META (Vision/Persona/Out-of-Scope) | 11 | — |
| **Total REQ-IDs in Matrix** | **144** | — |

> **Release-Gate-Lesart**: Bei v1.0 sind 106 von 129 in-Scope-Items ✅ (82.2 %); die 4 verbleibenden 🟡
> sind ausdrücklich als MVP-Fallback dokumentiert (REQ-INGR-003 SighiImporter wartet auf CSV,
> REQ-SEARCH-004 UseCase-Refactor, REQ-ONBOARD-002 Warning-Dialog, REQ-ADMIN-002 ETL-Jobs-UI).
> Die 18 ❌ und 1 ⏳ sind Post-v1.0-Backlog (Quality-UI/Quality-Filter, OFF-Importer, Recipe-Snapshot
> in IntakeEntry, Offline-Read-Cache, Rating-LocalDao).

> Hinweis: META-IDs sind keine implementierbaren REQ-IDs. Einige REQ-IDs sind über mehrere Phasen
> verteilt (z. B. REQ-RATING-003 P1+P2 — gezählt einmal in der ersten Phase).

---

## Update-Regel

Nach jeder erledigten Aufgabe wird:
1. Status der betroffenen REQ-IDs hier auf 🟡 (in Arbeit) oder ✅ (fertig) gesetzt
2. Implementation-File-Spalte um echte File-Pfade aktualisiert (falls Spec-Path abweicht)
3. Aggregierte Statistik neu berechnet (manuell oder per Skript)
4. [docs/SprintPlan.md](SprintPlan.md) ebenfalls aktualisiert (Sprint-Deliverable-Checkboxen)

---

**Ende TraceabilityMatrix v0.1.**

---

## §8 P6 Re-Spec REQ-IDs (eingefügt 2026-05-26, LOCKED via P6.S1)

| REQ-ID | Phase | Status | Implementation-File (geplant) |
|---|---|:-:|---|
| REQ-DESIGN-001 | P6.S2 | ⏳ | `android_app/.../presentation/theme/Color.kt`, `Theme.kt` |
| REQ-TYPO-001 | P6.S2 | ⏳ | `android_app/.../presentation/theme/Typography.kt`, `res/font/` |
| REQ-COMP-001 GlassCard | P6.S3 | ⏳ | `presentation/common/components/GlassCard.kt` |
| REQ-COMP-002 SectionPill | P6.S3 | ⏳ | `presentation/common/components/SectionPill.kt` |
| REQ-COMP-003 GradientFab | P6.S3 | ⏳ | `presentation/common/components/GradientFab.kt` |
| REQ-COMP-004 GradientButton | P6.S3 | ⏳ | `presentation/common/components/GradientButton.kt` |
| REQ-COMP-005 AmbientBackdrop | P6.S3 | ⏳ | `presentation/common/components/AmbientBackdrop.kt` |
| REQ-COMP-006 GradientText | P6.S3 | ⏳ | `presentation/common/components/GradientText.kt` |
| REQ-COMP-007 SegmentedTabs | P6.S3 | ⏳ | `presentation/common/components/SegmentedTabs.kt` |
| REQ-COMP-008 SeverityBar | P6.S3 | ⏳ | `presentation/common/components/SeverityBar.kt` |
| REQ-HOME-PIN-001 | P6.S4 | ⏳ | `presentation/home/HomeScreen.kt`, `PinnedNutrientsManager.kt`, `V12__per_nutrient_goals.sql` |
| REQ-ONBOARD-SLIDER-001 | P6.S4 | ⏳ | `presentation/onboarding/OnboardingScreen.kt` |
| REQ-WATER-REMOVE-001 | P6.S7 | ⏳ | `presentation/home/WaterTracker.kt` |
| REQ-WATER-ALARM-HELPER-001 | P6.S7 | ⏳ | `presentation/home/WaterAlarmCard.kt` |
| REQ-INTAKE-ADD-FLOW-001 | P6.S5/S7 | ⏳ | `presentation/lebensmittel/LebensmittelScreen.kt` nav-arg |
| REQ-WORDING-LOCK-001 | P6.S5 | ⏳ | `presentation/plan/PlanAddSheet.kt`, `strings.xml` |
| REQ-LIST-PRELOAD-001 | P6.S5 | ⏳ | `presentation/lebensmittel/IngredientScreen.kt`, `RecipeScreen.kt` |
| REQ-LOG-EVENT-001..006 | P6.S6 | ⏳ | `presentation/log/LogScreen.kt`, `LogRepository.kt`, `V13__log_event_schema.sql` |
| REQ-PROFILE-GOALS-001 | P6.S5 | ⏳ | `presentation/profile/ProfileScreen.kt`, `V12__per_nutrient_goals.sql` |

**Superseded:** alte REQ-LOG-001..006 (Tagebuch-Modell) sind durch REQ-LOG-EVENT-001..006 ersetzt; im §6 oben als „⛔ superseded by REQ-LOG-EVENT-* (P6.S6)" zu markieren wenn Cleanup nötig.

**End of §8.**
