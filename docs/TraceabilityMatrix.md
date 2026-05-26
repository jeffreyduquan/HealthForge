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
| REQ-PLATFORM-001 | P1 | 🟡 | `android_app/app/build.gradle.kts` (single platform; only Android target configured) |
| REQ-PLATFORM-002 | P1 | 🟡 | `android_app/app/build.gradle.kts` (minSdk=26, target/compile=35) |
| REQ-PLATFORM-003 | P1 | 🟡 | `deploy/docker-compose.dev.yml`, `deploy/docker-compose.prod.yml`, `deploy/Caddyfile` (Skelett, prod-Aktivierung in P1.S8) |
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
| REQ-NAV-004 | P1🟡/P3 | 🟡 | Bottom-Tab Log = `PhasePlaceholder("P3")` (Symptom-Tagebuch); Verlauf-Button: `HomeScreen.kt` TopAppBar action → `MainRoutes.INTAKE_HISTORY` |

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
| REQ-INGR-001 | P1 | 🟡 | `server/ingredient/IngredientEntity.kt` + Flyway `V3__ingredient_schema.sql` (Schema + JPA ✅; Seed-Daten Backlog P1.S4.1) |
| REQ-INGR-002 | P1 | ✅ | `server/ingredient/IngredientSearchRepository.kt` (FTS + `hf_immutable_unaccent`), `IngredientController.search` mit `excludeAllergens` / `excludeFodmap` Filtern |
| REQ-INGR-003 | P1 | 🟡 | `server/etl/Importers.kt::SighiImporter` (Update-only Histamine — wartet auf CSV) |
| REQ-INGR-004 | P1 | ❌ | OFF-Filter/Dedupe-Regeln noch nicht implementiert (P1.S4.1) |
| REQ-INGR-005 | P1 | ❌ | OffScheduler + Sticky-Field-Logic noch offen (P3.S2 Field-PR-Workflow) |
| REQ-INGR-006 | META | ⏭ | Lizenzhinweis — keine Code-Datei (Out-of-scope-Garantie) |

## §5.4 Search & Filter (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-SEARCH-001 | P1 | ✅ | `server/ingredient/IngredientSearchRepository.kt` (ILIKE-Substring auf `hf_immutable_unaccent(lower(name_de))` + GIN-Trigram-Indizes V5; FTS-german war unzureichend für Compound-Wörter) |
| REQ-SEARCH-002 | P1 | ✅ | V1 (`unaccent` extension) + V3 (`hf_immutable_unaccent` IMMUTABLE-Wrapper) + ViewModel `applyProfileFilters` |
| REQ-SEARCH-003 | P1 | ✅ | `server/ingredient/IngredientController.kt::search(@RequestParam q, limit, excludeAllergens, excludeFodmap)` |
| REQ-SEARCH-004 | P1 | 🟡 | Inline im `LebensmittelViewModel` (ProfileRepository hydrate + Toggle); separates `BuildSearchFiltersUseCase` → P2 |
| REQ-SEARCH-005 | P1 | 🟡 | siehe REQ-QUALITY-UI-001/002 (Quality-Badges + Warning-Chip → P1.S6) |

## §5.5 Recipes (P2)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-RECIPE-001 | P2 | ❌ | `server/recipe/RecipeEntity.kt` + Flyway `V2__p2_recipes.sql` |
| REQ-RECIPE-002 | P2 | ❌ | `RecipeEntity.ownerId` FK |
| REQ-RECIPE-003 | P2 | ❌ | `RecipeEntity.visibility` enum + `groupId` Constraint |
| REQ-RECIPE-004 | P2 | ❌ | `server/recipe/RecipeLikeEntity.kt` + `RecipeService.like()` |
| REQ-RECIPE-005 | P2 | ❌ | `presentation/essen/rezepte/RecipeEditScreen.kt` + Validator |
| REQ-RECIPE-006 | P2 | ❌ | `server/media/ImageUploadController.kt` + Thumbnailator + MinIO-Client |
| REQ-RECIPE-007 | P2 | ❌ | `domain/usecase/ComputeRecipeNutritionUseCase.kt` (computed-on-read) |
| REQ-RECIPE-008 | P2 | ❌ | `server/recipe/RecipeController.kt::update` Ownership-Check |
| REQ-RECIPE-009 | P2 | ❌ | `data/local/entity/IntakeEntryEntity.kt` (snapshot fields: name, kcal, macros) |

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
| REQ-SUPP-004 | P3 | ❌ | `server/supplement/SuggestionController.kt` + `admin-ui/src/pages/SupplementsQueuePage.tsx` (deferred per Scope-Entscheidung P1.S7) |
| REQ-SUPP-005 | P1 | ✅ | `data/db/entities/SupplementEntities.kt::SupplementReminderEntity` + `notification/AlarmScheduler.kt` + `notification/AlarmReceiver.kt` (ONCE/DAILY/WEEKLY + re-arm) (P1.S7) |
| REQ-SUPP-006 | P1 | ✅ | `presentation/essen/EssenScreen.kt` Sub-Tab "Supplements" → `SupplementsScreen` (P1.S7) |
| REQ-SUPP-007 | P1 | 🟡 | Supplements existieren als separates Entity ohne Recipe-Verknüpfung (kein Validator nötig solang RecipeIngredient nicht referenziert); Re-validate bei P2 Recipe-Engine |

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
| REQ-RATING-002 | P2 | ❌ | `server/community/CommunityRatingController.kt` + `recipe_ratings_community` Tabelle |
| REQ-RATING-003 | P1+P2 | ❌ | Trennung lokal/Server, kein gemeinsamer Constraint |
| REQ-RATING-004 | META | ⏭ | Out-of-scope-Garantie (kein Endpoint, kein UI-Element) |
| REQ-RATING-005 | P2 | ❌ | `CommunityRatingController.kt::deleteVote` |

## §5.13 Onboarding (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-ONBOARD-001 | P1 | ✅ | `presentation/onboarding/OnboardingScreen.kt` (14 Steps; Register ist Teil von P1.S2-AuthFlow) (P1.S3) |
| REQ-ONBOARD-002 | P1 | 🟡 | `OnboardingViewModel.kt::OnboardingState` (alle Felder nullable, Skip via Weiter ohne Eingabe). **Offen:** Warning-Dialog bei Allergy/FODMAP-Skip — Backlog P1.S3.1 |
| REQ-ONBOARD-003 | P1 | ✅ | `presentation/profile/ProfileScreen.kt::onRestartOnboarding` (P1.S3) |

## §5.x Admin (P1 Minimal + P3 Full)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-ADMIN-001 | P1 | ❌ | `admin-ui/` Vite-Setup + `server/admin/AdminAuthFilter.kt` + Caddy `admin.healthforge.endgear.de` |
| REQ-ADMIN-002 | P1 | 🟡 | Server-Endpoints `POST /admin/etl/run`, `GET /admin/etl/runs/{src}` ✅ (`server/etl/EtlController.kt`); Admin-UI-Seiten Backlog P3.S1 |
| REQ-ADMIN-003 | P3/P4 | ❌ | siehe REQ-ADMIN-FULL-001/002 |

## §6.1 Groups (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-GROUP-001 | P3 | ❌ | `server/group/GroupEntity.kt` (`type` ENUM) + Flyway `V3__p3_community.sql` |
| REQ-GROUP-002 | P3 | ❌ | `GroupEntity.kt` Felder |
| REQ-GROUP-003 | P3 | ❌ | `server/group/GroupController.kt::create/join/leave` + `presentation/profil/GroupsScreen.kt` |
| REQ-GROUP-004 | P3 | ❌ | `GroupController.kt::removeMember/transferOwnership` |
| REQ-GROUP-005 | P3 | ❌ | RecipeEntity `visibility=GROUP` + Filter in `RecipeRepository.findVisibleFor(userId)` |
| REQ-GROUP-006 | P3 | ❌ | `presentation/essen/rezepte/RecipeDetailScreen.kt` (Group-Label) |
| REQ-GROUP-007 | P3 | ❌ | `server/community/ReportController.kt` + `admin-ui/src/pages/RecipeReportsPage.tsx` |

## §6.2 Meal Plan (P2)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-PLAN-001 | P2 | ❌ | `data/local/entity/MealPlanDayEntity.kt`, `MealPlanSlotEntity.kt` |
| REQ-PLAN-002 | P2 | ❌ | `presentation/plan/PlanScreen.kt` + `SlotPickerBottomSheet.kt` |
| REQ-PLAN-003 | P2 | ❌ | nur Room (kein Sync) |
| REQ-PLAN-004 | P2 | ❌ | `presentation/plan/MealSlot.kt::habeGegessenButton` → `IntakeRepository.addFromSlot()` |
| REQ-PLAN-005 | P2 | ❌ | `notification/MealReminderScheduler.kt` |

## §6.3 Shopping List (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-SHOP-001 | P3 | ❌ | `domain/usecase/BuildShoppingListUseCase.kt` (Unit-Normalization) |
| REQ-SHOP-002 | P3 | ❌ | `data/local/entity/ShoppingListItemEntity.kt` + `presentation/plan/ShoppingListScreen.kt` |
| REQ-SHOP-003 | P3 | ❌ | `ShoppingListItemEntity.category` + Group-by-Category in UI |

## §6.4 Symptom-Tagebuch (P3)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-LOG-001 | P3 | ❌ | `data/local/entity/LogEntryEntity.kt` (SQLCipher) — kein Server-Endpoint |
| REQ-LOG-002 | P3 | ❌ | `LogEntryEntity.kt` Felder + `presentation/log/LogEntryFormScreen.kt` |
| REQ-LOG-003 | P3 | ❌ | `data/local/entity/CustomSymptomEntity.kt` + Default-Seed via `resources/seed/symptoms_default.json` |
| REQ-LOG-004 | P3 | ❌ | `LogDao.kt::insert` (kein UNIQUE-Constraint auf Datum) |
| REQ-LOG-005 | P3 | ❌ | `presentation/log/LogChartsScreen.kt` (Line-Charts via Vico) |
| REQ-LOG-006 | P3 | ❌ | `domain/usecase/IsLogEntryEditableUseCase.kt` (7-Tage-Logik) |

## §6.5 Reminders (P1 Supplement / P2 Meal)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-REMIND-001 | P1 | 🟡 | Supplement-Reminders: ✅ siehe REQ-SUPP-005; Plan/Water Reminders: weiterhin offen |
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
| REQ-EXPORT-001 | P3 | ❌ | `server/export/ExportService.kt` + `domain/usecase/BuildLocalExportUseCase.kt` (Combination Client+Server) |
| REQ-EXPORT-002 | P3 | ❌ | `presentation/profil/ExportScreen.kt` |
| REQ-EXPORT-003 | P3 | ❌ | `ExportService.kt::buildPayload` (Mix lokal+server) |
| REQ-EXPORT-004 | P3 | ❌ | `PdfRenderer.kt` (iText/PdfBox) + `JsonExporter.kt` (Moshi) |

## §7 Power Features (P4)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-INGR-USER-001 | P4 | ❌ | `server/ingredient/UserSuggestionController.kt` + `admin-ui/src/pages/IngredientQueuePage.tsx` |
| REQ-INGR-USER-002 | P4 | ❌ | `IngredientRepository.findUsableFor(userId)` (PENDING nur für Submitter) |
| REQ-FIELDPR-001 | P4 | ❌ | `server/ingredient/FieldPrController.kt` + `admin-ui/src/pages/FieldPrPage.tsx` |
| REQ-FIELDPR-002 | P4 | ❌ | `FieldPrEntity.status` Logik in `IngredientService.getDisplay()` |
| REQ-FIELDPR-003 | P4 | ❌ | `FieldPrService.approve/reject` mit `rejection_reason` |
| REQ-AUTOPLAN-001 | P4 | ❌ | `presentation/plan/PlanScreen.kt::generateButton` |
| REQ-AUTOPLAN-002 | P4 | ❌ | `server/autoplan/BeamSearchPlanner.kt` (Server-side, LOCKED Q4) |
| REQ-AUTOPLAN-003 | P4 | ❌ | `AutoPlanRequestDto.kt` (Inputs payload) |
| REQ-AUTOPLAN-004 | P4 | ❌ | `presentation/plan/AutoPlanPreviewScreen.kt` (Editable before Commit) |
| REQ-INSIGHT-001 | P4 | ❌ | `domain/insights/LiftCorrelationCalculator.kt` (lokal, WorkManager) |
| REQ-INSIGHT-002 | P4 | ❌ | `LiftCorrelationCalculator.kt` Thresholds (lift > 1.5, n ≥ 3) |
| REQ-INSIGHT-003 | P4 | ❌ | `LiftCorrelationCalculator.kt::weightedByCount` |
| REQ-INSIGHT-004 | P4 | ❌ | Garantie: keine Network-Aufrufe in Insights-Modul (Lint-Check empfohlen) |
| REQ-BARCODE-001 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut: no barcode scanner |
| REQ-BARCODE-002 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut |
| REQ-BARCODE-003 | — | 🗑️ REMOVED (2026-05-25) | Scope-cut |
| REQ-ADMIN-FULL-001 | P3/P4 | ❌ | `admin-ui/src/pages/` (Dashboard, IngredientQueue, FieldPrQueue, SupplementsQueue, ReportsQueue, UsersPage, InvitesPage, JobsPage, AuditLogPage, StatisticsPage, Layout) |
| REQ-ADMIN-FULL-002 | P1 | ❌ | `users.role` ENUM-Spalte; Setzen nur via DB-SQL (kein UI) |

## §10 Data Quality (P1)

| REQ-ID | Phase | Status | Implementation-File |
|---|---|:-:|---|
| REQ-QUALITY-001 | P1 | ❌ | `dataQuality`-Spalte noch nicht modelliert (kommt mit Vollständigkeits-Trigger in P1.S5) |
| REQ-QUALITY-002 | P1 | ❌ | `allergensKnown` Bool noch nicht modelliert — derzeit nur `allergens_json` (P1.S5) |
| REQ-QUALITY-003 | P1 | 🟡 | `IngredientEntity.histamineScore: Short?` (0..3) ✅ in V3-Schema + Entity |
| REQ-QUALITY-004 | P1 | 🟡 | `IngredientEntity.fodmapFlagsJson` (TEXT JSON) ✅ in V3-Schema + Entity |
| REQ-QUALITY-005 | P1 | ✅ | `IngredientEntity.source: IngredientSource` ENUM (BLS/SIGHI/OFF/USER/MANUAL) |
| REQ-QUALITY-UI-001 | P1 | ❌ | `presentation/essen/lebensmittel/IngredientDetailScreen.kt::QualityBadgeRow` |
| REQ-QUALITY-UI-002 | P2 | ❌ | `presentation/essen/rezepte/RecipeDetailScreen.kt::QualityRollupBanner` |
| REQ-QUALITY-FILTER-001 | P1 | ❌ | `BuildSearchFiltersUseCase.kt` (`excludeUnknownAllergens=true` default) |
| REQ-QUALITY-FILTER-002 | P1 | ❌ | `presentation/essen/lebensmittel/FilterDialog.kt` (Toggle + Warning-Chip in Result) |
| REQ-QUALITY-FIX-001 | P1 | ❌ | `admin-ui/src/pages/IngredientEditorPage.tsx` (Edit-Form) |
| REQ-QUALITY-FIX-002 | P4 | ❌ | siehe REQ-FIELDPR-001..003 |

---

## Aggregierte Statistik (initial)

| Phase | Total | ❌ | 🟡 | ✅ |
|---|---|---|---|---|
| **META** | 11 | — | — | — |
| **P1** | 64 | 64 | 0 | 0 |
| **P2** | 16 | 16 | 0 | 0 |
| **P3** | 27 | 27 | 0 | 0 |
| **P4** | 17 | 17 | 0 | 0 |
| **Total** | **135** | **124** | **0** | **0** |

> Hinweis: META-IDs sind keine implementierbaren REQ-IDs (Vision/Persona/Out-of-scope-Statements). Sie zählen
> nicht in die Implementation-Quote. Ein paar IDs sind über mehrere Phasen verteilt
> (z.B. REQ-RATING-003 P1+P2 — gezählt einmal in der ersten Phase).
> Konkrete Counts sind Schätzwerte — bei der ersten Implementation-Phase
> P1 wird die Tabelle gegen-validiert.

---

## Update-Regel

Nach jeder erledigten Aufgabe wird:
1. Status der betroffenen REQ-IDs hier auf 🟡 (in Arbeit) oder ✅ (fertig) gesetzt
2. Implementation-File-Spalte um echte File-Pfade aktualisiert (falls Spec-Path abweicht)
3. Aggregierte Statistik neu berechnet (manuell oder per Skript)
4. [docs/SprintPlan.md](SprintPlan.md) ebenfalls aktualisiert (Sprint-Deliverable-Checkboxen)

---

**Ende TraceabilityMatrix v0.1.**
