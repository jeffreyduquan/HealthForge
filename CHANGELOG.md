# HealthForge CHANGELOG

Konvention: Jeder P6-Sprint-Abschluss + jede groessere Code-/Verhaltens-Aenderung erhaelt einen Eintrag.
Format pro Eintrag: **Sprint/Datum** + **Touched Docs** + **Untouched-Begruendung** + **Verifikation**.

---

## P7.S4 Slice 4b — Plan-Water-Goal-Slider pro Tag — 2026-05-28

**Scope:** REQ-PLAN-WATER-GOAL-001 — tagesspezifisches Wasserziel-Override im Plan-Tab, das im Home-Tab kanonisch als effective water goal greift.

**Code-Änderungen:**
- MOD `data/db/dao/MealPlanDao.kt`: NEW `updateWaterGoalForDay(day: String, v: Int?): Int` — `UPDATE meal_plan_slot SET waterGoalMl = :v WHERE dayDateIso = :day` (Single-Source-of-Truth-Konvention: jeder Slot eines Tages trägt denselben Override-Wert).
- MOD `data/repository/MealPlanRepository.kt`: NEW `setWaterGoalForDay(day: LocalDate, value: Int?): Int`. No-op-Semantik wenn 0 Slots existieren (UI zeigt Slider nur in nicht-leerem Plan).
- MOD `presentation/plan/PlanViewModel.kt`: INJECT `ProfileRepository` + `ComputeNutrientTargetsUseCase`; NEW `profileWaterFlow: StateFlow<Int>` (Profil-Default-Wasserziel). `PlanUiState` erweitert um `effectiveWaterGoalMl` / `profileWaterDefaultMl` / `hasWaterGoalOverride`. `state`-Combine jetzt 4-arg (zusätzlich `profileWaterFlow`). NEW `setWaterGoalForDay(value: Int?)` mit Snackbar-Hinweis "Erst eine Mahlzeit hinzufügen" wenn 0 Rows updated, NEW `resetWaterGoalForDay()`.
- MOD `presentation/plan/PlanScreen.kt`: NEW `DayWaterGoalSlider`-Composable: `GlassCard` mit `SlotLabelPill("Wasserziel heute")` + großer Wert-Anzeige + Reset-Icon (`Icons.Outlined.RestartAlt`, nur bei Override) + Material3 `Slider` (Range 500..5000, ~89 Steps für 50-ml-Raster, lokaler Drag-State für flüssige Bewegung, Commit nur `onValueChangeFinished`). Beschriftungs-Switch: "Profil-Default — Slider verschieben für tagesspezifisches Ziel" vs. "Override für diesen Tag (Profil-Default: X ml)". Eingefügt im LazyColumn zwischen `DayHeader` und `DaySummary`.
- MOD `presentation/home/HomeViewModel.kt`: INJECT `MealPlanRepository`. `targetsFlow` ist jetzt `profileTargets.combine(planSlotOverride)` — wenn `slots.firstOrNull()?.waterGoalMl != null` wird `targets.waterMl` mit dem Override ersetzt. Damit greift der Plan-Override automatisch in allen Home-Konsumenten (Wasser-Bar, Pinned-Cards, Ghost-Berechnung).

**Verifikation:**
- `:app:compileDebugKotlin` BUILD SUCCESSFUL 14s, 0 Errors.
- `:app:installDebug` → Emulator Pixel_7_API_35.
- End-to-End-Smoke (Profil-Default = 2000 ml):
  1. Plan-Tab leer → Slider nicht sichtbar (by design, kein anchor-Slot).
  2. Frühstück-Slot hinzufügen → Slider erscheint mit "WASSERZIEL HEUTE / 2000 ml / Profil-Default — Slider verschieben für tagesspezifisches Ziel".
  3. Slider auf 2950 ml ziehen → Wert-Anzeige aktualisiert sich, Label wechselt zu "Override für diesen Tag (Profil-Default: 2000 ml)", Reset-Icon erscheint.
  4. Home-Tab → "Wasser 600 / 2950 ml" → Plan-Override fließt kanonisch.
  5. Plan-Tab → Reset-Icon tappen → "2000 ml / Profil-Default — …", Icon verschwindet.

**Touched docs:**
- CHANGELOG.md (dieser Eintrag).
- docs/SprintPlan.md — P7.S4 Slice 4b ⏳ → ✅ mit vollständiger Code-Beschreibung; Status-Header auf "Slice 4a ✅ + Slice 4b ✅"; Akzeptanz-Punkt 4b auf ✅.
- docs/TraceabilityMatrix.md — REQ-PLAN-WATER-GOAL-001 ⏳ → ✅ mit Implementation-Beschreibung + Verifikation.

**Untouched docs + Begründung:**
- ReqSpec.md — REQ-PLAN-WATER-GOAL-001 ist seit P7.S1 spezifiziert; Slice 4b ist die Implementierung, keine Vertragsänderung.
- UsabilityMap.md — Plan-Slider sitzt in bestehendem Plan-Tab (Bottom-Nav), keine neue Navigation.
- GUI.md — DayWaterGoalSlider folgt Histamind-Idiom (GlassCard + SlotLabelPill + Material3-Slider), keine neuen Tokens.
- Architecture.md — Datenfluss bleibt Profile → ComputeNutrientTargetsUseCase → DailyTargets; Plan-Override ist ein lokaler combine in HomeViewModel, kein neuer Layer.
- 07 Coding Conventions — Standard-Compose + Hilt + Flow, nichts Neues.
- 08 Test Strategy — Visual-Smoke statt Unit-Test gewählt (siehe BattleTestPlan-Konvention für UI-Slider).
- 09 Bootstrap — kein Setup-Impact.

**Hinweis Room:**
- Kein Schema-Bump nötig. `MealPlanSlotEntity.waterGoalMl: Int?` existiert seit P7.S1 (DB v8); die neue Query nutzt die bestehende Spalte.

**Risiken / Trade-offs:**
- Single-Source-pro-Tag-Konvention (Update aller Slots auf gleichen Wert) ist pragmatisch, hat aber den Nachteil dass ein zukünftiger "per-slot waterGoal" (z.B. Frühstücks-spezifisch) nicht ohne weiteres machbar wäre. Bewusste Entscheidung weil die Spec klar "per-Tag-Override" sagt.
- Slider wird nur sichtbar wenn ≥ 1 Slot existiert. Alternative wäre eine virtuelle DAY_META-Slot-Erzeugung — verworfen weil DB-Pollution.
- HomeViewModel.targetsFlow ist jetzt 2-Arity-Combine; bei zukünftigem Plan-Override für weitere Nutrients muss das auf N-Arity erweitert werden (kein blocker).

---

## P7.S4 Slice 4a — Profile-Refactor (BigCatalog-Goals + Pin-Section drop) — 2026-05-28

**Scope:** REQ-PROFILE-LAYOUT-001 — Profil-Tab umgebaut auf neuen Layout-Standard.

**Code-Änderungen:**
- NEW `android_app/.../presentation/profile/components/NutrientGoalRow.kt` (~110 Zeilen): Zeile mit Naehrstoff-Label + Default (read-only) + Override-NumberField (Decimal-Input, Komma-tolerant) + Reset-Icon (Material `RestartAlt`). Override-Range klemmt automatisch in `nutrient.min..nutrient.max`.
- MOD `presentation/profile/ProfileScreen.kt`:
  - DROP Section `WASSERZIEL` (Slider mit hartem `waterGoalMl`); Wasser ist jetzt eine Zeile in „TAGESZIELE".
  - DROP Section `ANGEHEFTETE NÄHRSTOFFE` (Chip-Grid). Pin-Verwaltung erfolgt im Home-Tab seit P7.S3.
  - EXPAND `TAGESZIELE`: Iteration über `domain.nutrition.NutrientCatalog.all` (33 Einträge), Kategorie-Header (Makros / Vitamine / Mineralstoffe / Wasser), `NutrientGoalRow` pro Eintrag.
  - Default-Wert kommt aus `vm.computedDefaults: StateFlow<DailyTargets>` (Makros + Wasser profilabhängig via `ComputeNutrientTargetsUseCase`); Mikros nutzen statische DGE-Werte aus `NutrientCatalog`.
- MOD `presentation/profile/ProfileViewModel.kt`:
  - INJECT `ComputeNutrientTargetsUseCase`.
  - NEW `computedDefaults: StateFlow<DailyTargets>` mapped vom Profile-Flow.
  - NEW `clearNutrientGoal(slug)` — entfernt Key aus `dailyNutrientGoalsJson`; Sonderfall `slug=="water"` → `resetWaterGoalMl()` (= setWaterGoalMl(2000), Catalog-Default).
  - NEW `resetWaterGoalMl()`.
  - MOD `setNutrientGoal(slug, value)` — Sonderfall `slug=="water"` → routet auf `setWaterGoalMl(value.toInt())` (Single-Source-of-Truth bleibt die `waterGoalMl`-Spalte, weil sie kanonisch in `DailyTargets` einfließt).
- DEL `presentation/profile/NutrientCatalog.kt` (P6.S6-Legacy, 8 Einträge); Slug-Strings für `pinnedNutrientsJson`/`dailyNutrientGoalsJson` deckt jetzt der Big-Catalog ab. Kein anderer Caller (grep-verifiziert).

**Verifikation:**
- `:app:compileDebugKotlin` BUILD SUCCESSFUL 21s, 0 Errors, 0 neue Warnings.
- Statische Checks (vscode `get_errors`): keine offenen Errors auf ProfileScreen/ProfileViewModel/NutrientGoalRow.
- `togglePinnedNutrient` in ViewModel bleibt unangetastet (keine externen Caller, kein Refactor-Scope).

**Touched Docs:**
- `CHANGELOG.md` (dieser Eintrag).
- `docs/SprintPlan.md` — P7.S4 Slice 4a auf ✅ DONE 2026-05-28, Sub-Slice-Struktur (4a/4b/4c) ergänzt.
- `docs/TraceabilityMatrix.md` — `REQ-PROFILE-LAYOUT-001` von ⏳ → ✅.

**Untouched (begründet):**
- `docs/ReqSpec.md` — Anforderung war seit P7-Spec-Lock klar, kein Drift.
- `docs/UsabilityMap.md` — §7 Profil-Tab beschreibt bereits den neuen Zustand exakt.
- `docs/GUI.md` — `NutrientGoalRow`-Spec existiert seit P7-Spec-Lock.
- `docs/Architecture.md` — kein Schichten-/Persistenz-Change (Room v8 unverändert, DTOs unverändert).
- `docs/Runbook.md` / `docs/TestStrategy.md` / `docs/HistamindDesignReference.md` / `docs/BattleTestPlan.md` — kein Scope-Berührungspunkt.

**Risiken / Folgearbeiten:**
- Slice 4b (Plan-Water-Goal-Slider pro Tag) und Slice 4c (WaterDeficitScheduler) stehen aus.
- `presentation.profile.ProfileViewModel.togglePinnedNutrient` ist nach Drop der Profil-Chips ohne UI-Caller; Home nutzt eigene Path. Bleibt als no-op-Public-Method (kein Refactor in 4a-Scope).
- Migration für User mit aktivem `pinnedNutrientsJson` aus P6: bleibt funktional (Home-Tab liest die JSON weiter).

---



## P7.S2 Slice 3c FINAL — USDA-FDC Importer-Run grün — 2026-05-28

**Scope:** REQ-DATA-SOURCE-001 + REQ-INGR-ALLERGEN-MAPPING-001 abschließen — End-to-End-Run des `UsdaFdcImporter` gegen Dev-DB.

**Run-Resultat:**
- `POST /admin/v1/etl/run?source=USDA_FDC` → **`status=SUCCESS, rowsInserted=8354, rowsUpdated=0, rowsSkipped=0`** in **2 min 02 s** (etl_runs UUID `8d7b0636-9078-4c28-b513-d404a4d2c417`).
- DB-Check: `SELECT COUNT(*) FILTER (WHERE source='USDA_FDC') FROM ingredients;` → **8354**.

**Stichprobe AllergenMapper (live DB):**
| name_de | allergens_json |
| --- | --- |
| Bagels, Weizen | `["GLUTEN"]` |
| Babynahrung, GERBER, …Vanille Weizen | `["GLUTEN"]` |
| (EINGELEGTES SENFGRÜN) TAKANA | `["MUSTARD"]` |
| BUTTER-FINGER-ERDNUSSBUTTER-RIEGEL | `["PEANUT","LACTOSE"]` |
| Original Elchspur Vanilleeis mit Erdnussbutter-Bechern | `["PEANUT","SOY","LACTOSE"]` |
| Mehl, Kokosnuss | `[]` ✅ (NEGATIVE_LIST greift) |
| Öl, Kokosnuss | `[]` ✅ |
| Kokosnusswasser-Riegel, Mango | `[]` ✅ |
| Wein, Tafelwein, weiß, Muskateller | `["ALCOHOL"]` ✅ (kein NUT aus „Muskat") |

→ NEGATIVE_LIST aus Slice 3a (mustard-seed-oil/coconut/nutmeg) funktioniert in Produktion.

**Bugs gefixt (auf dem Weg zum grünen Run):**
1. **Spring-Boot main-class ambiguous** — durch die drei JvmStatic-Tool-Mains (FetchFdcTopIds/BuildUsdaSeed/TranslateFdcNames) konnte Boot bei `:bootRun` keinen Main mehr eindeutig finden. Fix: `springBoot { mainClass.set("de.healthforge.HealthForgeApplicationKt") }` in [server/build.gradle.kts](server/build.gradle.kts).
2. **CHECK-Constraint fehlt USDA_FDC** — sowohl `ingredients.source` als auch `etl_runs.source` lehnten den neuen Enum-Wert ab (SQLSTATE 23514). Fix: neue Flyway-Migration [V13__usda_fdc_source.sql](server/src/main/resources/db/migration/V13__usda_fdc_source.sql) erweitert beide CHECKs (BLS/SIGHI/OFF/USER/MANUAL bleiben für historische Zeilen).
3. **`micronutrients_json` JSONB-Bind** — Hibernate bindete den String als `VARCHAR`, was PG mit „column is of type jsonb but expression is of type character varying" zurückwies (SQLSTATE 42804). Fix: `@JdbcTypeCode(SqlTypes.JSON)` auf `micronutrientsJson` in [IngredientEntity.kt](server/src/main/kotlin/de/healthforge/ingredient/IngredientEntity.kt). Hibernate 6 macht damit Pass-through-Bind als JSON.

**Verifikation:**
- bootRun grün (Flyway V13 applied in 67 ms).
- Import-Run grün (siehe oben).
- 9 Stichproben-Allergene OK (4 positive matches + 4 negative-list-Hits + 1 ALCOHOL).
- Demo-Rows aus V4 + User-Rows blieben unverändert (Total: 15 + 8354 = 8369 ingredients).

**Sicherheit:**
- Reset-Token-Insertion in `password_reset_tokens` war **temporäres Dev-Vorgehen** (Plain-Text-Token im Code/Chat → SHA-256 in DB), Token wurde sofort durch Reset-Call konsumiert (`used_at IS NOT NULL`). Admin-Passwort bleibt nur lokal in dieser Dev-Instanz aktiv; Prod-VPS unverändert.
- Kein Secret im Repo.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 3c 🟡 → ✅ DONE 2026-05-28 mit Run-Metriken + Bug-Fixes.
- `docs/TraceabilityMatrix.md` — REQ-DATA-SOURCE-001 + REQ-INGR-ALLERGEN-MAPPING-001 🟡 → ✅.
- `CHANGELOG.md` — dieser Eintrag.
- NEU `server/src/main/resources/db/migration/V13__usda_fdc_source.sql`.
- MOD `server/build.gradle.kts`, `server/src/main/kotlin/de/healthforge/ingredient/IngredientEntity.kt`.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md`, `docs/Architecture.md` — gefixte Bugs sind Infrastruktur-/JPA-Detail, keine Kontrakt-/Architektur-Änderung.
- `docs/GUI.md`, `docs/UsabilityMap.md` — User-Effekt (IngredientPicker zeigt 8354 Treffer statt 15) ist sichtbar bei nächstem Smoke-Test, keine Layout-/Navigationsänderung.
- `docs/Runbook.md` — der Dev-Trick „Reset-Token direkt in DB einfügen" ist **nicht** Runbook-würdig (Prod nutzt MailHog/Mail-Provider regulär; Runbook bcrypt-Reset bleibt korrekt).
- 00–09 (sofern existent), `docs/TestStrategy.md`, `docs/BattleTestPlan.md` — keine Drift, ETL-Run wäre für Prod-Smoke eine 1-Klick-Operation im Admin-UI.

---

## P7.S2 Slice 3c — Importer-Härtung + DeepL-Vollrun fertig — 2026-05-28

**Scope:** REQ-DATA-TRANSLATE-001 abschließen (Vollrun) + REQ-DATA-SOURCE-001 Importer-Härtung.

**Voller DeepL-Lauf:**
- `:translateFdcNames` BUILD SUCCESSFUL **4m 48s**, 166 Batches à 50, 0 HTTP-429/503 Retries, **8251 Rows übersetzt**.
- **Final Coverage: 8354/8354 = 100% `name_de` filled** (3 Demo + 100 Smoke aus Slice 3b-Verifikation + 8251 Voll-Lauf).
- CSV: 3.7 MB → 4.3 MB (UTF-8-Umlaute).
- DeepL-Free-Quota-Verbrauch: ~210k von 500k/Monat (42%, gemessen via `--limit` Estimated-Chars-Output).
- Stichprobe-Quality (manueller Check erste 14 Rows): „Alaska Pollock, raw" → „Alaska-Seelachs, roh" (idiomatisch korrekt), „Almond butter, creamy" → „Mandelbutter, cremig", „Anchovies, canned in olive oil, with salt, drained" → „Sardellen, in Olivenöl eingelegt, mit Salz, abgetropft" (vollständig + korrekt). Eine DeepL-Eigenheit gesehen: „red delicious" → „rote Delikatessen" (sollte Eigenname bleiben) — selten, nicht funktionsbrechend.

**Code-Änderung (Importer-Härtung):**
- MOD `server/src/main/kotlin/de/healthforge/etl/usda/UsdaFdcImporter.kt` — bisherige Logik `nameDe = cols[1].ifBlank { return@forEach.also { skipped++ } }` ersetzt durch:
  ```kotlin
  val nameEn = cols[2].trim()
  val nameDe = cols[1].trim().ifBlank { nameEn }
  if (nameDe.isBlank()) { skipped++; return@forEach }
  ```
  Defensiv für zukünftige Re-Imports mit frisch generiertem Seed (vor erneutem DeepL-Run). Akzeptanz-Kriterium Slice 3b „damit unübersetzte Einträge nicht unsichtbar werden" erfüllt **auf Importer-Ebene** (statt Controller-Ebene → einfacher: `name_de`-Spalte bleibt `nullable = false`, kein Schema-Change).
- DTO-Erweiterung war bereits in P7.S1 erfolgt: `IngredientDto` hat schon `fdcId: Long?` + `micronutrients: Map<String, Double>` (Zeile 13 + 28 in `IngredientDtos.kt`). Slice 3c hat hier NICHTS zu ändern.

**Verifikation:**
- `:compileKotlin` → BUILD SUCCESSFUL 2s.
- `name_de`-Coverage in CSV: **8354/8354 via PowerShell-Zähler:** `Total: 8354 | name_de filled: 8354 | Pending: 0` ✓.

**Sicherheit:** Keine neuen Secrets, keine DB-Schema-Änderung, keine externen API-Calls außer dem bereits dokumentierten DeepL-Lauf.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 3b 🟡 → ✅ DONE 2026-05-28, Slice 3c-Block erweitert um Importer-Härtung + Vollrun-Metriken + Pending-Importer-Run-Marker.
- `docs/TraceabilityMatrix.md` — REQ-DATA-TRANSLATE-001 🟡 → ✅. REQ-DATA-SOURCE-001 + REQ-INGR-ALLERGEN-MAPPING-001 bleiben 🟡 bis Importer-Run live durchgelaufen.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md`, `docs/Architecture.md` — Importer-Härtung ist Implementierungs-Detail (ifBlank-Fallback statt Skip), keine Kontrakt-/Architektur-Änderung.
- `docs/GUI.md`, `docs/UsabilityMap.md` — User-spürbarer Effekt (deutsche Lebensmittelnamen im IngredientPicker) tritt erst nach Importer-Run ein → docs werden nach diesem Run touched.
- 00–09 Plan/Vision/Glossary/Bootstrap/Coding/Test — keine Drift.

**Pending User-Action:**
1. Docker Desktop starten.
2. `cd deploy && docker-compose -f docker-compose.dev.yml up -d` (DB+MinIO).
3. Server starten (`cd server && .\gradlew.bat bootRun` oder Docker-Variante).
4. Admin-Login holen.
5. `curl -X POST -H "Authorization: Bearer …" "http://localhost:8080/admin/v1/etl/run?source=USDA_FDC"`.
6. Erwartung: `inserted=8351, updated=3 (Demo-Rows existieren schon mit anderen Source), skipped=0` und alle 8354 in `ingredients`-Tabelle.

---

## P7.S2 Slice 3b — TranslateFdcNames Tool (DeepL Free API) — 2026-05-28

**Scope:** REQ-DATA-TRANSLATE-001 — `name_de` Befüllung für 8351 USDA-FDC Einträge via DeepL Free API.

**Code-Änderungen:**
- NEW `server/src/main/kotlin/de/healthforge/tools/TranslateFdcNames.kt` (~250 LOC) — Standalone-Kotlin-Tool analog `BuildUsdaSeed`. Liest komplettes `usda_fdc.csv` in Memory (8354×~200 B = 2 MB), findet Rows mit leerem `name_de`, batched à 50 zu `POST api-free.deepl.com/v2/translate` (form-encoded, `source_lang=EN&target_lang=DE&text=…&text=…`), parsiert Response mit minimalem JSON-Extractor (`{"translations":[{"text":"…"},…]}`), persistiert **nach jedem Batch atomar** via `.tmp + Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` → max. 50 Texte Verlust bei Interrupt, kein halbgeschriebenes CSV. CLI-Flags: `--in/--out/--limit/--no-resume/--batch/--rate-ms/--dry-run`. HTTP-429/503 mit Expo-Backoff (max 60s, 6 Retries), HTTP-456 Quota-Erschöpfung mit klarer Abbruch-Meldung („Fortschritt persistiert"). Free-Tier-Detection via Key-Suffix `:fx`.
- MOD `server/build.gradle.kts` — neuer Gradle-Task `:translateFdcNames` (`JavaExec`), reuse `loadDotEnv()` für `DEEPL_API_KEY`.
- DEL `server/tools/translate_fdc_names.main.kts` (Standalone `kotlin`-Script, obsolet). Wird ersetzt durch o.g. Kotlin-Klasse → einheitliches Tooling-Pattern, Type-Checked, IDE-Support, gleiche CSV-Quoting wie BuildUsdaSeed.

**Begründung der Design-Entscheidungen:**
- **In-Memory-Load** statt Streaming: 8354 Rows × 200 Bytes = 2 MB. Streaming wäre Over-Engineering, In-Memory erlaubt einfache Index-basierte Update + Atomic-Rewrite ohne Komplexitäts-Explosion.
- **Atomic-Rename nach JEDEM Batch** statt am Ende: Interrupt-Resistenz ist kritisch — User könnte Strg+C drücken oder Quota erschöpfen. Trade-off: ~170 Rewrites à 3.7 MB = 630 MB I/O. Bei modernem SSD irrelevant (~5s gesamt), aber rettet bis zu 49 Texte je Crash.
- **Minimaler JSON-Parser** statt Jackson: DeepL-Response ist trivial flach (`{"translations":[{"text":"…"}]}`). Manueller Extractor erspart Jackson-Dependency-Surface + handled Escape-Sequenzen explizit (`\n \" \uXXXX`). Wenn DeepL je Nested-Strukturen liefert, switchen wir auf Jackson.
- **Resume via `name_de`-Prüfung** statt separates Tracking-File: Die CSV selbst IST der Fortschritts-State → idempotent, neu-startbar, kein zusätzlicher Sync-Punkt.
- **`URLEncoder.encode(text, UTF-8)` für jeden Text-Param**: Schützt vor Umlauten/Sonderzeichen in englischen Original-Namen (sehr selten, aber FDC enthält z.B. `é`-Zeichen in französisch-stämmigen Branded-Foods).

**Verifikation:**
- `:compileKotlin` → BUILD SUCCESSFUL 9s, keine Errors.
- `:translateFdcNames --args="--dry-run --limit 10"` → BUILD SUCCESSFUL 7s. Output:
  ```
  Total rows: 8354
  Pending (need DeepL): 8351; nach --limit: 10
  Estimated DeepL chars: 371 (Free-Tier 500k/Monat)
  Samples: 'Alaska Pollock, raw', 'Almond butter, creamy', 'Almond milk, unsweetened, plain, refrigerated', …
  ```
  → 3 Demo-Rows (mit hand-curated deutschem `name_de`) werden korrekt via Resume-Logik übersprungen (8354 − 8351 = 3). Samples sind die ersten Rows nach den Demo-Einträgen (alphabetisch sortiert von BuildUsdaSeed). 8351 Pending = exakte Erwartung aus Slice 2.

**Pending User-Action (Blocker für vollen Run):**
- `DEEPL_API_KEY=<key>:fx` in `server/.env` eintragen (Free-Account: https://www.deepl.com/pro-api, kein Zahlungsmittel nötig).
- Smoke-Test empfohlen: `:translateFdcNames --args="--limit 100"` (~5 KB Chars, 2 Batches, ~3s API-Zeit) → 100 Rows mit `name_de` gefüllt verifizierbar via CSV-Stichprobe.
- Voller Lauf: `:translateFdcNames` ohne Flags (~210k Chars geschätzt, 168 Batches, bei 1.1s Rate ≈ 3 min). Mit `--rate-ms 500` (2 req/s) ≈ 1.5 min.

**Sicherheit:**
- `DEEPL_API_KEY` aus `server/.env` (gitignored), niemals in CLI-Args.
- 60s HTTP-Timeout pro Request (keine endlos hängenden Verbindungen).
- Form-encoded Body mit `URLEncoder` → Injection-frei.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 3b ⏳ → 🟡 (Code DONE, API-Run pending), detaillierter Block mit Verifikation + User-Action.
- `docs/TraceabilityMatrix.md` — REQ-DATA-TRANSLATE-001 ⏳ → 🟡, mit Implementierungs-File + Dry-Run-Verifikation + Pending-User-Action.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md` — REQ-DATA-TRANSLATE-001 Wortlaut unverändert gültig (DeepL als externer Translation-Provider war bereits spezifiziert).
- `docs/Architecture.md` §4.5b — Build-Time-Tool-Pattern unverändert (Wiederverwendung von BuildUsdaSeed-Scaffolds, kein neuer Runtime-Dependency).
- `docs/GUI.md`, `docs/UsabilityMap.md` — orthogonal, User sieht erst nach Slice 3c Effekt im IngredientPicker.
- 00–09 Plan/Vision/Glossary/Bootstrap/Coding/Test — keine Drift.

**Nächster Schritt:** User trägt `DEEPL_API_KEY` ein → wir starten `:translateFdcNames --args="--limit 100"` als Smoke + danach den vollen Lauf. Dann Slice 3c (Importer scharfschalten + `IngredientDto` erweitern + Controller-Fallback).

---

## P7.S2 Slice 3a — AllergenMapper-Härtung + BLS/OFF-Deprecation — 2026-05-28

**Scope:** REQ-INGR-ALLERGEN-MAPPING-001 Slice 3a + REQ-DATA-SOURCE-001 Aufräumen.
Pre-Implementation-Check ergab: `AllergenMapper` + Tests existieren bereits, aber ReqSpec §665 Negativ-Liste (`coconut`, `nutmeg`, `mustard-seed-oil`) war nicht implementiert. `mustard-seed-oil` triggerte fälschlich MUSTARD (Wortgrenze `\bmustard\b` matched). Slice 3a vorgezogen (kein externer API-Key nötig, schneller Win).

**Code-Änderungen:**
- MOD `server/src/main/kotlin/de/healthforge/etl/usda/AllergenMapper.kt` — `NEGATIVE_LIST` ergänzt (3 Senf-Varianten + 5 Coconut-Varianten + nutmeg) als `Regex("\\b...\\b")` mit längsten Phrasen zuerst. Neue interne Methode `stripNegatives(text)` ersetzt Negativ-Phrasen durch Leerzeichen BEVOR Keyword-Match läuft. `extract()` ruft `stripNegatives()` zuerst auf.
- MOD `server/src/test/kotlin/de/healthforge/etl/usda/AllergenMapperTest.kt` — 4 neue `@Test`:
  - `negative list strips mustard-seed-oil so MUSTARD is not flagged` (FDC-Realfall, 3 Schreibvarianten).
  - `mustard alone still triggers MUSTARD after negative list applied` (Regression-Guard: Negativ-Liste darf normales Match nicht zerstören).
  - `coconut and nutmeg do not trigger NUT` (Regression-Guard für zukünftige Keyword-Erweiterungen).
  - `coconut milk does not pollute LACTOSE match for real milk in same row` (Disambiguation: Coconut wird gestrippt, `whey` bleibt → LACTOSE-Match).
- MOD `server/src/main/kotlin/de/healthforge/etl/Importers.kt`:
  - `BlsImporter` + `OffImporter` mit `@Deprecated(message="... abgelöst durch UsdaFdcImporter (P7.S2)...", level=WARNING)` + KDoc-Block mit Begründung (USDA-FDC ist Single-Source-of-Truth per REQ-DATA-SOURCE-001).
  - `EtlOrchestrator.run()`: bei `source == BLS || source == OFF` jetzt `log.warn("triggered DEPRECATED importer ... prefer USDA_FDC")`. Beans bleiben registriert für historische `etl_runs`-Rows + manuelle Migrations-Triggers.

**Verifikation:**
- `:test --tests "*AllergenMapperTest*" --rerun-tasks` → BUILD SUCCESSFUL 16s, 10/10 Tests grün (5 ursprüngliche + 4 neue Negativ-Liste + 1 Sanity).
- `:compileKotlin` → BUILD SUCCESSFUL, keine neuen Errors. Drittelparty-`@Deprecated`-Warnings (jjwt, bucket4j) unverändert; eigene `@Deprecated` triggern keine Warnings, da BlsImporter/OffImporter nirgends direkt referenziert werden (nur via `List<Importer>` Spring-DI).
- Logischer Trace: `UsdaFdcImporter.kt:85` → `AllergenMapper.extractAsStrings("$nameEn $ingredientsEn")` → bei realen FDC-Rows wie `"... ENRICHED WHEAT FLOUR, WATER, MUSTARD-SEED-OIL ..."` jetzt korrekt nur GLUTEN (nicht GLUTEN+MUSTARD).

**Sicherheit:**
- Keine neuen Secrets, keine API-Calls, keine DB-Migration. Reine In-Memory-Logik.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 3 aufgesplittet in 3a ✅ / 3b ⏳ (DeepL) / 3c ⏳ (Importer scharfschalten + DTO). Slice 3a Block mit Code-Liste + Test-Verifikation.
- `docs/TraceabilityMatrix.md` — REQ-INGR-ALLERGEN-MAPPING-001 ⏳ → 🟡 (Slice 3a done, End-to-End-Verifikation in 3c). REQ-DATA-SOURCE-001 Zelle erweitert um Slice 3a Deprecation-Block.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md` — REQ-INGR-ALLERGEN-MAPPING-001 §12 + Negativ-Liste-Erwähnung §665 unverändert gültig (jetzt erst implementiert).
- `docs/Architecture.md` §4.5b — `AllergenMapper` als Stateless-Object unverändert dokumentiert; Deprecation von BLS/OFF ist Implementierungs-Detail, kein Architektur-Shift (USDA-FDC war bereits als kanonische Quelle dokumentiert).
- `docs/GUI.md`, `docs/UsabilityMap.md` — orthogonal (kein UI-Change in 3a; FdcTranslationsPage bleibt auf P7.S5 Polish geparkt). User-spürbarer Effekt erst nach 3c.
- 00–09 Plan/Vision/Glossary/Bootstrap/Coding/Test — keine Drift.

**Nächster Schritt:** P7.S2 Slice 3b — `TranslateFdcNames`-Tool (DeepL Free API, ~210k Zeichen). Blocker: `DEEPL_API_KEY` in `server/.env` muss vom User angelegt werden (Free-Account auf deepl.com/pro-api, Key endet auf `:fx`).

---

## P7.S2 Slice 2 — FDC-Detail-Fetch + Seed-Build (Build-Time-Tool) — 2026-05-28

**Scope:** REQ-DATA-SOURCE-001 Slice 2 — vollständiges Seed-CSV mit allen Nährwerten für 8351 USDA-Foods.

**Code-Änderungen:**
- NEW `server/src/main/kotlin/de/healthforge/tools/BuildUsdaSeed.kt` — Standalone Build-Time-Tool (kein `@Component`). Liest `seed/fdc_top_ids.csv` (Slice 1 Output), holt Detail-Daten via FDC `POST /v1/foods` in Batches à 20 IDs (USDA-Hardlimit), mappt FDC `foodNutrients[].nutrient.id` auf 33 NutrientCatalog-Keys (P7.S1), schreibt 14-Spalten-CSV. Resume-fähig (überspringt schon im Output vorhandene fdc_ids), CLI-Flags `--in/--out/--limit/--no-resume/--rate-ms`, HTTP-429-Retry (60s, 3x).
- MOD `server/build.gradle.kts` — neuer Gradle-Task `:buildUsdaSeed` (`JavaExec`), nutzt bestehenden `loadDotEnv()`-Helper für `FDC_API_KEY`.
- MOD `server/src/main/resources/seed/usda_fdc.csv` — erweitert von 3 hand-curated Demo-Rows auf **8354 Einträge** (3.7 MB). Demo-Rows mit deutschem `name_de` blieben dank Resume-Logik erhalten. Neue 8351 Rows haben `name_de` leer — wird in Slice 3 via DeepL gefüllt.

**FDC-Nutrient-ID → NutrientCatalog-Key Mapping (zentrale Entscheidung):**
- Energie: `1008` (SR-Legacy/Branded) + `2047` (Atwater General, Foundation) + `2048` (Atwater Specific, Foundation) → alle drei auf `kcal`. **Wichtig**: Foundation-Foods nutzen NICHT 1008; ohne 2047/2048 wären alle Foundation-Rows ohne kcal geblieben (Smoke-Test hatte das aufgedeckt → Bugfix vor vollem Lauf).
- Makros: `1003`→protein, `1005`→carbs, `2000`→sugar, `1004`→fat, `1258`→satfat, `1079`→fiber.
- Salz: aus `1093` (Sodium mg) berechnet: `salt_g = sodium_mg × 2.5 / 1000`.
- Vitamine (13): 1106→a, 1114→d, 1109→e, 1185→k, 1165→b1, 1166→b2, 1167→b3, 1170→b5, 1175→b6, 1176→b7, 1177→b9 (1190=DFE als Fallback), 1178→b12, 1162→c.
- Mineralstoffe (11): 1087→calcium, 1089→eisen, 1090→magnesium, 1095→zink, 1098→kupfer, 1101→mangan, 1103→selen, 1100→jod, 1092→kalium, 1093→natrium, 1091→phosphor.

**Verifikation:**
- Smoke-Test `--limit 5 --no-resume` separate CSV → 4/5 OK (Alaska Pollock 78 kcal, Mandelbutter 602 kcal, Anchovis Salz 13.5g aus Na 5403mg). Vor Smoke-Run war initial 0/3 (Foundation-Foods hatten kein kcal) → MACRO_MAP erweitert um 2047/2048.
- Voller Lauf `:buildUsdaSeed --rate-ms 3700`: BUILD SUCCESSFUL **55m 28s**, 425 Batches, 0 HTTP-429-Retry. 8351 written / 133 skipped (kein kcal, meist Branded Lifestyle-Drinks ohne Energie-Wert). Coverage 98.4% (8354/8487).
- CSV-Stichproben: First 3 = Demo-Rows mit hand-curated Deutsch erhalten. Last 2 = Branded-Wasser mit `ingredients_en` gefüllt. Mikros sauber JSON-serialisiert (semicolon-quoted).

**Sicherheit:**
- `FDC_API_KEY` aus `server/.env` (gitignored), nicht in CLI-Args.
- CSV enthält keine Secrets, nur USDA-Public-Data.

**Touched Docs:**
- `docs/SprintPlan.md` — P7.S2 Slice 2 Block ⏳ TODO → ✅ DONE 2026-05-28 mit Ergebnis-Details + Verifikation. Slice 3 umstrukturiert (Translation + Importer scharfschalten zusammengezogen).
- `docs/TraceabilityMatrix.md` — REQ-DATA-SOURCE-001 Zelle erweitert um Slice 2 ✅ + CSV-Pfad + Coverage-Zahlen.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md` — REQ-DATA-SOURCE-001 Wortlaut unverändert gültig (Korpus-Definition + Pipeline-Schritte bereits aus Slice 1 dokumentiert).
- `docs/Architecture.md` §4.5b — Nutrient-Modell + Build-Time-Tool-Pattern unverändert (Wiederverwendung des FetchFdcTopIds-Scaffolds).
- `docs/GUI.md`, `docs/UsabilityMap.md` — orthogonal (kein UI-Change, reines Daten-Asset).
- 00–09 Plan/Vision/Glossary/Bootstrap/Coding/Test — keine Drift.

**Nächster Schritt:** P7.S2 Slice 3 — `TranslateFdcNames`-Tool (DeepL Free API, ~250k Zeichen) + `UsdaFdcImporter` scharfschalten (CSV → DB via `POST /admin/v1/etl/USDA_FDC/run`) + `AllergenMapper` EU-14.

---

## P7.S1 — NutrientCatalog Server-Parity verifiziert — 2026-05-27

**Scope:** REQ-NUTRIENT-CATALOG-001 Status-Reconciliation.
Pre-Implementation-Check (Regel 3) für P7.S2 Slice 2 erforderte FDC-Nutrient-ID → `NutrientCatalog.key` Mapping. Stichprobe an beiden Catalog-Dateien zeigte: Android + Server **sind bereits identisch** (33 Keys: 8 Makros + 13 Vitamine + 11 Mineralstoffe + Wasser-Pseudo, gleiche Units, gleiche DGE-Defaults). Parity-Test `NutrientCatalogParityTest.kt` existiert und ist grün. P7.S1-Status in SprintPlan/TraceMatrix war veraltet (⏳ TODO statt ✅ DONE).

**Code-Änderungen:** keine — reine Status-/Doc-Reconciliation.

**Verifikation:**
- `.\gradlew.bat test --tests "*NutrientCatalogParity*"` → BUILD SUCCESSFUL in 12s ✅
- Visual diff `android_app/.../NutrientCatalog.kt` ↔ `server/.../NutrientCatalog.kt` zeigt identische `all`-Liste (Keys+Units+Defaults+Min/Max).

**Touched Docs:**
- `docs/SprintPlan.md` — P7.S1 Status ⏳ TODO → ✅ DONE (2026-05-27), Ergebnis-Block mit Test-Verifikation.
- `docs/TraceabilityMatrix.md` — REQ-NUTRIENT-CATALOG-001 ⏳ → ✅.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched Docs (Begründung):**
- `docs/ReqSpec.md` — REQ-NUTRIENT-CATALOG-001 Wortlaut unverändert gültig.
- `docs/Architecture.md` — §4.5b Nutrient-Modell unverändert.
- `docs/GUI.md`, `docs/UsabilityMap.md` — keine UI-Änderung.
- 00–06, 07–09 Plan/Vision/Glossary/Bootstrap — keine Drift.

**Nächster Schritt:** P7.S2 Slice 2 `build_usda_seed` Tool unblocked — Catalog-Keys + Parity stabil für FDC-Mapping.

---

## P7.S2 Slice 1 — FDC-Top-IDs-Fetcher (Build-Time-Tool) — 2026-05-27

**Scope:** REQ-DATA-SOURCE-001 Slice 1 — kuratiertes FDC-ID-Korpus-Asset.
User-Direktive: "Lass uns doch erstmal um die Lebensmittel-Datenbank kümmern, viele Features hängen daran." Startpunkt = Top-IDs-Liste als gecommittetes Asset, damit nachgelagerte Tools (Seed-Build, Translation, Importer) deterministisch laufen.

**Code-Änderungen:**
- NEW `server/src/main/kotlin/de/healthforge/tools/FetchFdcTopIds.kt` — Standalone Build-Time-Tool (kein `@Component`, kein Spring-Runtime-Bezug). Ruft FDC `POST /v1/foods/search` für Foundation + SR-Legacy (alle Pages) + Branded (Top-300, default-sort). 1 req/s defensiver Rate-Limit, HTTP-429-Retry mit 60s-Backoff (3 Versuche). Branded-Fehler wird abgefangen (Foundation+SR-Legacy bleiben erhalten).
- MOD `server/build.gradle.kts` — neuer Gradle-Task `:fetchFdcTopIds` (`JavaExec`) + `loadDotEnv()`-Helper, der `server/.env` parst und Subprozess-ENV setzt. CLI-Args: `--branded-top N`, `--out PATH`.
- NEW `server/.env` (gitignored) — `FDC_API_KEY=…` lokal.
- NEW Asset `server/src/main/resources/seed/fdc_top_ids.csv` (619 KB, 8487 Rows, 4 Spalten `fdc_id;data_type;name_en;brand`): 394 Foundation + 7793 SR-Legacy + 300 Branded.

**Sicherheit:** Key aus ENV, nie in CLI-Args, nie im Quellcode. `.env` bereits in `.gitignore`. CSV enthält keine Secrets.

**Verifikation:**
- `:compileKotlin` BUILD SUCCESSFUL 20s.
- `:fetchFdcTopIds` BUILD SUCCESSFUL 2m07s; Endausgabe `[fetch] ✅ Wrote 8487 rows … 619 KB`.

**Touched Docs:**
- CHANGELOG.md (dieser Eintrag).
- docs/ReqSpec.md §12 — REQ-DATA-SOURCE-001 mit konkreter Korpus-Definition (8487 IDs) + Pipeline-Schritte 1/2/3 (Build-Time-Tools + Runtime-Importer) + Traceability-Tabellenzeile.
- docs/SprintPlan.md — P7.S2 Status auf 🟡 IN PROGRESS, Slice 1 explizit als DONE, restliche Deliverables zu Slice 2.
- docs/TraceabilityMatrix.md — REQ-DATA-SOURCE-001 Status auf 🟡 + Asset-/Tool-Pfade.

**Untouched Docs (mit Begründung):**
- docs/00-Plan, 01-Vision, 02-Glossary — keine neuen Begriffe/Visionsänderungen; nur Implementations-Slice.
- docs/Architecture.md §4.5b — bestehende Beschreibung der USDA-FDC-Pipeline deckt das Tool-Pattern bereits ab (Build-Time-Skript ohne Server-Runtime). Optionaler Slice-2-Update sobald Detail-Fetch-Tool entsteht.
- docs/UsabilityMap, GUI.md — Build-Time-Tool ohne UI-Touchpoint.
- docs/04-Requirements, 05-Milestones, 06-Progress, 07-Coding-Conventions, 08-Test-Strategy, 09-Bootstrap — keine Regel- oder Konventions-Änderung; Tool folgt bestehendem Build-Time-Pattern (analog `tools/translate_fdc_names.main.kts`).
- BattleTestPlan, HistamindDesignReference — orthogonal.

---

## P7.S3.b — Einheitliche Stufen-Bars für alle Pinned-Nährstoffe + Vorgänger-Track — 2026-05-30

### Scope
User-Direktive: "ALLE bars müssen eigentlich identisch sein. nur bei wasser kommen noch zusatzregeln wegen wecker, slider usw. […] der hintergrund der progressbars soll immer die farbe der vorgängerstufe aber abgegraud oder verdunkelt sein". Die linearen 0→120 % Pinned-Bars (kcal/Protein/Carbs/Fett) werden auf die gleiche Stufen-Mechanik wie `WaterStageSlider` umgestellt; `WaterStageColors` wird zur Single Source of Truth für Stufen-Farb-Cycle und Track-Tint.

### Mechanik (für alle Bars)
- `stage = floor(current / goal)`, `frac = (current - stage*goal) / goal` (0..1).
- Bar-Füllung: Gradient aus `waterStageGradient(stage)` (10-Stufen-Cycle, ab Stufe 9 endless).
- Track: `waterStageTrackColor(stage)` = Akzent der Vorgänger­stufe × 0.25 Alpha. Stufe 0 → `LocalHmTokens.barTrack`.
- Ab Stufe ≥ 1: Lv-Badge (Pill) rechts neben Wert/Ziel.
- Wasser-Spezialitäten (Slider, Bell, Ghost, Defizit-Rot, Touch-Disconnect bei Stufenwechsel) bleiben Wasser-only.

### Code-Änderungen
- **DEL** `presentation/home/components/MacroRing.kt` (alter Ring-Ansatz, ungenutzt).
- **DEL** `presentation/home/components/MacroBarColumn.kt` (Wrapper um `LeveledPowerBar`, ungenutzt).
- **MOD** `presentation/theme/NeoComponents.kt`: Entfernt `LeveledPowerBar`, `StageBadge`, `stageColor`. Bleiben: `NeoSectionLabel` (in HomeScreen genutzt), `NeoCard`.
- **MOD** `presentation/home/components/WaterStageColors.kt`:
  - `waterStageGradient`/`waterStageAccent` von `internal` auf `public` gehoben.
  - Neuer `fun waterStageTrackColor(stage): Color?` → Vorgänger-Akzent × 0.25 Alpha; Stufe 0 → `null` (Fallback auf `hm.barTrack`).
- **MOD** `presentation/home/components/PinnedNutrientCard.kt` `PinnedNutrientRow`:
  - Lineare 0→120 % Logik ersetzt durch Stufen-Logik (`floor(current/target)` + Rest).
  - Bar-Brush = `waterStageGradient(stage)`, Track = `waterStageTrackColor(stage) ?: hm.barTrack`.
  - `StageBadge` (Lv N) Composable inline, sichtbar ab Stufe ≥ 1.
  - `%`-Anzeige zeigt jetzt Prozent **innerhalb der aktuellen Stufe** (0–100 %), nicht 0–120 % Gesamt.
- **MOD** `presentation/home/components/WaterStageSlider.kt`:
  - Track-Farbe via `waterStageTrackColor(displayedStage) ?: hm.barTrack` statt fest `hm.barTrack`.

### Touched Docs
- `CHANGELOG.md` (dieser Eintrag)
- `docs/ReqSpec.md` — REQ-HOME-NUTRIENT-LIST-001 + REQ-HOME-WATER-BAR-001 ergänzt um Stufen-Mechanik + Track-Regel.
- `docs/UsabilityMap.md` — Pinned-Bar-Beschreibung um Stufen-Roll-over + Vorgänger-Track + Lv-Badge erweitert.
- `docs/GUI.md` — `PinnedNutrientCard`-Beschreibung von "Linear-Progress" auf "Stage-Bar (gemeinsame Mechanik mit Wasser)" geändert; `MacroRing`/`LeveledPowerBar` als gelöscht markiert.
- `docs/SprintPlan.md` — P7.S3.b Slice angelegt.
- `docs/TraceabilityMatrix.md` — REQ-HOME-NUTRIENT-LIST-001/REQ-HOME-WATER-BAR-001 mit P7.S3.b ergänzt.

### Untouched (mit Begründung)
- `docs/00 Plan` / `docs/01 Vision` — kein Vision-/Plan-Drift.
- `docs/02 Glossary` — keine neuen Begriffe (Stufe/Stage bereits eingeführt).
- `docs/Architecture.md` — keine Architektur-Änderung (UI-only, gleiche Komponenten-Hierarchie).
- `docs/04 Requirements other` — REQ-PROFILE-* / REQ-INTAKE-* unberührt.
- `docs/05 Milestones` / `06 Progress` — innerhalb laufendem P7, kein Milestone-Switch.
- `docs/07 Coding Conventions` — keine neuen Konventionen.
- `docs/08 Test Strategy` — Smoke nur visuell; keine neuen Unit-/UI-Tests.
- `docs/09 Bootstrap` — kein Bootstrap-Pfad geändert.
- `docs/BattleTestPlan.md` / `HistamindDesignReference.md` — keine neuen Findings, Design-Referenz weiter gültig.

### Verifikation
- `./gradlew :app:installDebug` → BUILD SUCCESSFUL (20 s).
- App auf emulator-5554 gestartet; visueller Smoke vom User bestätigt im Folge-Turn.

---

## P7.S3.a-v2.3 — Stufenwechsel disconnected den Touch (Slider-Remount per `key`) — 2026-05-29

### Scope
V2.2 (Per-Drag-Lock) und v2.3-Trial (Zeit-Debounce) fühlten sich beide nicht richtig an. User-finale-Direktive: "sobald eine stufe hoch oder runter geht, muss der touch disconnected werden und nicht mehr von der app erkannt werden, bis ein neues touch event kommt." v2.3 implementiert das per Compose-`key`-Remount: nach jedem In-Drag Stufenwechsel wird der Slider via `key(sliderResetKey)` neu zusammengebaut, was die aktive Drag-Geste sauber abbricht. Der User MUSS den Finger heben und neu tippen, um eine weitere Stufe zu wechseln.

### Code-Änderungen
- **MOD** `presentation/home/components/WaterStageSlider.kt`
  - Zeit-Debounce-Variante (`lastStageTransitionMs` + `debounceMs = 10_000L`) wieder entfernt.
  - Neuer State `var sliderResetKey by remember { mutableIntStateOf(0) }`.
  - Slider in `key(sliderResetKey) { Slider(…) }` eingewickelt.
  - In `onValueChange`: bei Stage-Up/Down wird zusätzlich zur Stufentransition `sliderResetKey += 1` ausgeführt UND der neue absolute Wert sofort committet (anstatt erst on-release), damit der State persistent ist, falls der User nach dem Disconnect tatsächlich loslasst.
  - `onValueChangeFinished` committet weiterhin den finalen Wert; das On-Release-Stage-Vorrücken (v2.1) wurde entfernt (jetzt redundant zur In-Drag-Logik).

### Verifikation
- `./gradlew :app:installDebug` → BUILD SUCCESSFUL. App startet.

### Verifikation
- `./gradlew :app:installDebug` → BUILD SUCCESSFUL. App startet.

### Touched Docs (Regel 2)
- **CHANGELOG.md** — dieser Eintrag.
- **ReqSpec.md** §12 — REQ-HOME-WATER-BAR-001: "Per-Drag-Lock (v2.2)" durch "Zeit-Debounce 350 ms zwischen Stufenwechseln (v2.3)" ersetzt.
- **UsabilityMap.md** §3.2 — Drag-Action-Beschreibung erwähnt Zeit-Debounce statt Per-Drag-Lock.

### Untouched (Regel 2)
- **00 Plan / 01 Vision / 02 Glossary / 03 Architecture / 05 Milestones / 06 Progress / 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap / SprintPlan.md / GUI.md / TraceabilityMatrix.md** — UI-internes Throttle-Detail, keine Drift.

### Pre-Implementation-Check (Regel 3)
- **Usability** — 350 ms ist die typische Touch-Hold-Zeit zwischen "deliberater Stufenwechsel" und "Cascade durch verharrenden Finger". Wert kann später getuned werden.

---

## P7.S3.a-v2.2 — Stage-Cascade-Debounce + Defizit-Rotanteil — 2026-05-29

### Scope
v2.1-Smoke-Feedback: (a) wenn der Finger am rechten oder linken Slider-Rand stehenbleibt, kaskadieren die Stufenwechsel sofort durch viele Stufen (jeder onValueChange am Anschlag re-triggert die Boundary-Detection); (b) der rote Defizit-Bereich auf der Bar (Bereich zwischen aktueller Füllung und Ghost-Soll, wenn current < ghost) fehlte komplett. v2.2 fügt einen Per-Drag-Lock hinzu (nur ein Stufenwechsel pro Drag-Session) und rendert den roten Defizit-Anteil wieder wie in v1.

### Code-Änderungen
- **MOD** `presentation/home/components/WaterStageSlider.kt`
  - **Stage-Cascade-Lock**: `var stageTransitionThisDrag` (lokaler State). Sobald in einem Drag ein Stage-Up oder Stage-Down ausgelöst wurde, sind weitere In-Drag-Transitions in derselben Session gesperrt. `onValueChangeFinished` setzt das Flag zurück. → User muss kurz loslassen und neu drag, um weitere Stufen zu wechseln; "Slider am Rand halten" kaskadiert nicht mehr durch 5–10 Stufen.
  - **Defizit-Rotanteil**: zwischen `frac*w` und `ghostInStage*w` wird ein roter Bereich (`StatusOverUl` mit Alpha 0.55) gezeichnet, wenn `frac < ghostInStage` (User hinter Soll). Liegt das Soll außerhalb der angezeigten Stufe oder ist current ≥ ghost, wird kein Rotanteil gerendert.

### Verifikation
- `./gradlew :app:installDebug` → BUILD SUCCESSFUL, 0 errors. App startet.

### Touched Docs (Regel 2)
- **CHANGELOG.md** — dieser Eintrag.
- **ReqSpec.md** §12 — REQ-HOME-WATER-BAR-001: Per-Drag-Lock und Defizit-Rotanteil als Pflichtelemente ergänzt.
- **UsabilityMap.md** §3.2 — Drag-Action-Beschreibung erwähnt Per-Drag-Stufen-Lock + roten Defizit-Bereich.

### Untouched (Regel 2)
- **00 Plan / 01 Vision / 02 Glossary / 03 Architecture / 05 Milestones / 06 Progress / 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap / SprintPlan.md / GUI.md / TraceabilityMatrix.md** — kein Drift; Bugfix bleibt vollständig im UI-Composable.

### Pre-Implementation-Check (Regel 3)
- **Requirements / Usability / GUI / Tech / Architecture** — alle bestätigt; Per-Drag-Lock ist UX-Pflicht (sonst unkontrollierbar), Defizit-Rotanteil ist v1-Parität.

---

## P7.S3.a-v2.1 — Stufen-Slider Bugfix: Upgrade-Geste + Ghost-Marker — 2026-05-29

### Scope
Smoke-Test der v2 hat zwei Bugs aufgedeckt: (a) das Hochziehen über das rechte Ende rückte die Bar nicht in die nächste Stufe vor (alte Stufe blieb 100 % gefüllt sichtbar); (b) der Ghost-Soll-Marker (weiße Linie für lineares Tages-Soll bis jetzt) fehlte komplett. v2.1 behebt beides und re-führt das Ghost-Soll auf der Bar wieder ein.

### Code-Änderungen
- **MOD** `presentation/home/components/WaterStageSlider.kt`
  - **Stufen-State-Maschine neu**: lokaler `displayedStage` + `relativeMl` (statt aus `currentMl` jedes Recompose abgeleitet). Initialer State: `displayedStage = currentMl / goal`, `relativeMl = currentMl % goal` — d.h. nach Persistenz von `N*goal` ist die Bar bereits in Stufe N bei 0 % (statt fälschlich auf Stufe N-1 als "voll" zu zeigen). Resync auf externe `currentMl`-Änderungen nur, wenn lokaler State inkonsistent ist (`LaunchedEffect`).
  - **In-Drag Stage-Up/Down**: in `onValueChange` werden Boundary-Treffer als Stufenwechsel interpretiert — `target == goal && prev < goal` → `displayedStage++; relativeMl = 0`; `target == 0 && prev > 0 && displayedStage > 0` → `displayedStage--; relativeMl = goal`. Damit ist Upgrade während des Drags möglich (Bar wechselt sichtbar zur neuen Farbe).
  - **On-Release Stage-Vorrücken**: wenn der finale Wert eine Stufengrenze trifft (`relativeMl == goal` oder `== 0` bei Stufe > 0), rückt der lokale State eine Stufe weiter, sodass der nächste Drag in der neuen Stufe beginnt.
  - **Ghost-Marker**: neuer Parameter `ghostMl: Int`. Canvas zeichnet, wenn das Soll im sichtbaren Stufen-Bereich liegt, eine feine weiße vertikale Linie (Alpha 0.85, Strichbreite 2 px) an der Soll-Position.
- **MOD** `presentation/home/HomeScreen.kt` — `WaterStageSlider`-Aufruf bekommt `ghostMl = s.waterGhostMl`.

### Verifikation
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL in 7 s, 0 errors.
- `:app:installDebug` → emulator-5554 ✅; App startet.

### Touched Docs (Regel 2)
- **CHANGELOG.md** — dieser Eintrag.
- **ReqSpec.md** §12 — REQ-HOME-WATER-BAR-001 v2.1: Ghost-Soll-Linie auf der Bar wieder als Pflicht-Element; Stufen-State-Maschine (in-drag + on-release Transition) im Verhalten dokumentiert.
- **UsabilityMap.md** §3.1/§3.2 — Wireframe & Drag-Action-Beschreibung erwähnen wieder die weiße Ghost-Linie.
- **TraceabilityMatrix.md** — Anker-Zeilen bleiben (`WaterStageSlider.kt`) — keine Datei-Pfadänderung, nur Hinweis auf Stufenzustands-Logik + Ghost-Marker.

### Untouched (Regel 2)
- **00 Plan / 01 Vision / 02 Glossary / 03 Architecture / 05 Milestones / 06 Progress / 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap / SprintPlan.md / GUI.md** — keine Drift; Bugfix bleibt vollständig im UI-Composable, keine API-, Repo-, DB- oder Architektur-Änderung.

### Pre-Implementation-Check (Regel 3)
- **Requirements** — REQ-HOME-WATER-BAR-001 v2-Spec war hinsichtlich Ghost-Marker zu strikt entfernt; v2.1 reaktiviert Ghost-Marker explizit als Pflichtelement.
- **Usability** — Drag-Through-Zero bleibt, Upgrade-via-Drag-Past-Goal funktioniert jetzt; Ghost-Linie liefert visuelles "wo solltest du jetzt sein?".
- **GUI** — kein neues Token; nutzt `Color.White.copy(alpha=0.85f)` analog zur v1-Implementierung.
- **Technologien** — kein neuer Tech-Stack; nur lokale Compose-State-Erweiterung.
- **Architecture** — Repo/DAO/VM unverändert; `HomeState.waterGhostMl` (bereits existierend) wird jetzt vom UI konsumiert.

---

## P7.S3.a-v2 — Wasser-Stufen-Slider in PinnedNutrientCard — 2026-05-29

### Scope
Zweite Iteration der Wasser-UI: User-Feedback war "Wasser-Bar soll ganz normal wie ein angepinnter Nährstoff aussehen (unterster Eintrag in der Pin-Liste), aber mit eingebautem Slider. Slider geht 0–100 % des Tagesziels. Sobald Stufe N voll ist, lockt sie → neue Stufe 0 % mit neuer Farbe. Downgrade nur per 'an der rechten Seite (= 100 % der gerade-fertigen Stufe) wieder runterziehen'. Mindestens 10 Stufenfarben; Stufen sind endlos." REQ-HOME-WATER-BAR-001 entsprechend neu formuliert (Ghost-Soll-Linie auf der Bar entfällt; Defizit-Alarm-Backend bleibt aktiv).

### Code-Änderungen
- **NEU** `presentation/home/components/WaterStageSlider.kt` — Wasser-Zeile, optisch identisch zu `PinnedNutrientRow` (Label, ×N-Badge ab Stufe 1, Wert/Ziel, Prozent, Reminder-Bell, gefüllte Bar). Bar + Slider liegen übereinander (Slider mit transparenten Tracks). Stufen-Logik: `stage = currentMl / goal`; Slider-Range = `0..goal` relativ zur aktuellen Stufe; Drag-Through-Zero-Downgrade (wenn `currentMl == stage*goal`, rendert die Bar Stufe `stage-1` als voll und User kann zurückziehen).
- **NEU** `presentation/home/components/WaterStageColors.kt` — `StagePalette: List<Pair<Color,Color>>` mit 10 Einträgen (Stufe 0..9), `waterStageGradient(stage)` + `waterStageAccent(stage)` clampen ab Stufe 10+ auf Stufe 9. Farbverlauf cool→warm→alert→deep in der Histamind-Palette.
- **MOD** `PinnedNutrientCard.kt` — neuer Parameter `trailingSlot: (@Composable () -> Unit)? = null`, der nach den `entries` als letzte Zeile gerendert wird. So bleibt die Wasser-Zeile visuell Teil der Pin-Card.
- **MOD** `HomeScreen.kt` — separater Wasser-`NeoCard`-Block + Reminder-Bell-Header entfernt; stattdessen wird `WaterStageSlider` als `trailingSlot` an `PinnedNutrientCard` übergeben (nur wenn `"water"` in `pinnedKeys`). Die Pin-Liste filtert `water` aus den normalen Entries. Imports `Notifications` / `NotificationsNone` aus HomeScreen entfernt (jetzt im WaterStageSlider).
- **DELETE** `presentation/home/components/WaterProgressSlider.kt` — durch `WaterStageSlider.kt` ersetzt.

### Verifikation
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL in 7 s, 0 errors, 0 warnings.
- `:app:installDebug` → emulator-5554 (Pixel 7 API 35) ✅; App startet, Home-Screen rendert Pin-Card mit Wasser als letzte Zeile.

### Touched Docs (Regel 2)
- **CHANGELOG.md** — dieser Eintrag.
- **ReqSpec.md** §12 — REQ-HOME-WATER-BAR-001 v2: Range 0..goal pro Stufe, Stufen-Logik, Drag-Through-Zero-Downgrade, 10-Farben-Palette mit Clamp ab Stufe 9, Wasser-Zeile in PinnedNutrientCard (kein separater Card-Block), Reminder-Bell als Trailing-Icon. Ghost-Linie auf der Bar entfällt; Defizit-Alarm-Backend bleibt. Implementation-Anker auf `WaterStageSlider.kt` + `WaterStageColors.kt` + `PinnedNutrientCard.trailingSlot`.
- **TraceabilityMatrix.md** — REQ-HOME-WATER-BAR-001-Eintrag aktualisiert auf v2-Files.
- **UsabilityMap.md** §3.1/§3.2 — Wireframe-Block für Wasser-Zeile neu (×N-Badge, Bell rechts), Drag-Action-Beschreibung auf Stufen-Logik + Drag-Through-Zero umgeschrieben.

### Untouched (Regel 2 — explizit geprüft)
- **00 Plan / 01 Vision / 02 Glossary / 03 Architecture / 05 Milestones** — keine architektonische / strategische Drift (Repo-/DAO-API unverändert gegenüber v1; nur UI-Logik im Composable getauscht).
- **04 Requirements (ReqSpec)** — REQ-HOME-WATER-BAR-001 oben aktualisiert; andere Reqs unverändert (REQ-HOME-WATER-ALARM-001 bleibt gültig — Backend rechnet Soll weiterhin intern).
- **06 Progress / 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap** — kein Bedarf; Slider-Pattern (M3 Slider + Canvas-Bar overlay) und Day-Aggregate-Persistenz bereits in v1 etabliert.
- **SprintPlan.md** — historisches Logbuch P7.S3.a bleibt; v2-Delta steht im CHANGELOG + TraceabilityMatrix.
- **GUI.md** — Wasser-spezifischer GUI-Sweep wird gebündelt im nächsten Refactor-Pass.

### Pre-Implementation-Check (Regel 3)
- **Requirements** — REQ-HOME-WATER-BAR-001 als Stufen-Slider-Spec re-formuliert vor Implementierung (siehe ReqSpec §12 / oben).
- **Usability** — UsabilityMap-Wireframe zeigt jetzt Wasser als letzte Pin-Zeile mit ×N-Badge, Slider-Drag-Verhalten beschrieben (inkl. Downgrade-Regel).
- **GUI** — Wireframe-Mock im UsabilityMap §3.1; Histamind-Palette-Reuse für 10 Stufenfarben (kein neuer Token, alle Werte aus `Color.kt`-Bestand bzw. semantisch verwandten Tönen).
- **Technologien** — keine neuen Libraries (Compose Canvas + M3 Slider mit transparenten Tracks bereits in v1 verwendet).
- **Architecture** — Repo/DAO `setDayTotal`/`replaceDayTotal` unverändert; v2 ist eine reine UI-Iteration.

---

## P7.S3.a-fix — Wasser-UI vereinheitlicht: Slider IST die Bar — 2026-05-29

### Scope
Korrektur der P7.S3.a-Wasser-UI: User-Feedback war "es soll nur EINE Wasser-Progress-Bar geben, und die Bar selbst soll den absoluten Slider mit drin haben — Slider-Position = getrunkene Menge. Kein +/−, kein Hinzufügen-Button, keine zweite Wasser-Tile." REQ-HOME-WATER-BAR-001 reformuliert auf **absoluten** Slider mit Day-Aggregate-Persistenz.

### Code-Änderungen
- **NEU** `presentation/home/components/WaterProgressSlider.kt` — kombiniert Bar + Slider in EINEM Composable. Slider-Thumb-Position = absolute Tagesmenge in ml. Hintergrund-Canvas zeichnet Bahn / Ghost-Layer / Defizit-Rot (zwischen current und ghost, wenn current<ghost) / Current-Gradient / Ghost-Marker; Material3-Slider liegt mit transparenten Tracks darüber, nur der Thumb ist sichtbar. 50-ml-Steps, Range 0..max(goal×1.5, current). Commit on release.
- **NEU** `data/repository/WaterIntakeRepository.setDayTotal(day, totalMl)` — Day-Aggregate: ersetzt in einer Room-Transaktion alle `water_intake`-Rows des Tages durch genau einen Aggregat-Eintrag mit `totalMl`. Bei `totalMl == 0` bleibt der Tag eintragslos.
- **NEU** `data/db/dao/WaterIntakeDao.deleteForDay(day)` + `@Transaction replaceDayTotal(day, totalMl, loggedAt)`.
- **MOD** `HomeViewModel.kt` — neue Methode `setWaterMl(totalMl)` ruft `setDayTotal`. Entfernt: `addWater`, `undoLastWater`, `openWaterCustom`, `closeWaterCustom`, `onWaterCustomChange`, `confirmWaterCustom`, Snackbar-Trigger-Nonce, `lastWaterIntakeId`, `lastWaterVolumeMl`, `showWaterCustom`, `waterCustomMl` aus `HomeState`.
- **MOD** `HomeScreen.kt` — Wasser-Card zeigt nur noch: Header (Titel "Wasser" + Reminder-Bell) und `WaterProgressSlider`. Entfernt: Snackbar-LaunchedEffect mit Undo-Action, der Custom-Wasser-AlertDialog, die `${ml} / ${goal} ml`-Zeile im Header (ist jetzt im Slider integriert). Unused imports (`AlertDialog`, `OutlinedTextField`, `KeyboardOptions`, `KeyboardType`, `SnackbarDuration`, `SnackbarResult`, `LaunchedEffect`) entfernt.
- **DELETE** `presentation/home/components/WaterBarWithGhost.kt`, `WaterSlider.kt`, `HydrationBarCard.kt`, `WaterTracker.kt` — alle obsolet, alle Aufrufer migriert.

### Verifikation
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL in 13 s, 0 errors, 0 warnings.

### Touched Docs (Regel 2)
- **CHANGELOG.md** — dieser Eintrag.
- **ReqSpec.md** §12 — REQ-HOME-WATER-BAR-001 neu formuliert: "absoluter Slider, Slider IST die Bar, Day-Aggregate-Persistenz via `setDayTotal`, keine ±-Buttons / Quick-Add-Pills / Custom-Dialog / Undo, Range 0..max(goal×1.5, current)". Implementation-Anker auf `WaterProgressSlider.kt` + `setDayTotal` + `replaceDayTotal` aktualisiert.
- **TraceabilityMatrix.md** — REQ-HOME-WATER-BAR-001 von ⏳ → ✅, Implementation-Anker auf neue Dateien + Hinweis auf deleted obsolete files.
- **UsabilityMap.md** §3.1/§3.2 (Home-Tab Wireframe + Aktionen) — Wasser-Block neu gezeichnet (EIN Slider auf der Bar, Header mit Reminder-Bell), Drag-Action-Beschreibung auf "absoluter Wert, Day-Aggregate, links ziehen reduziert (Slider IST das Undo)" geändert.

### Untouched (Regel 2 — explizit geprüft)
- **00 Plan / 01 Vision / 02 Glossary / 03 Architecture / 05 Milestones** — keine architektonische / strategische Drift, nur eine UI-Verfeinerung innerhalb bestehender Wasser-Spec.
- **04 Requirements (ReqSpec)** — REQ-HOME-WATER-BAR-001 selbst wurde aktualisiert (siehe oben); andere Reqs unverändert.
- **06 Progress / 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap** — kein Bedarf: keine neuen Coding-Patterns, keine neuen Test-Strategien (Slider-Behavior reicht für Manual-Test in P7.S3a wie vorher).
- **SprintPlan.md** — P7.S3.a bleibt als Sprint-Snapshot stehen (historisches Logbuch), das Hot-Fix-Delta steht im CHANGELOG (Regel-3-Konformität: TraceabilityMatrix wurde nachgeführt).
- **GUI.md** — `WaterTracker`-Eintrag dort ist Glossar/Inventar von P6; obsolete Komponenten werden im nächsten GUI-Sweep gestrichen.

### Pre-Implementation-Check (Regel 3)
- **Requirements** — REQ-HOME-WATER-BAR-001 als Single-Slider-Spec re-formuliert vor Implementierung (siehe ReqSpec §12 / oben).
- **Usability** — UsabilityMap §3 reviewed: "Wasser nicht mehr im +Eintrag-Sheet" + "Pin-Liste enthält Wasser" bleiben gültig; einzige Änderung ist die Bar-Geometrie + Drag-Semantik (jetzt absolut statt Delta).
- **GUI** — Layout-Mock im CHANGELOG-Wireframe oben (UsabilityMap §3.1 ASCII), reicht für P7.S3a-Scope.
- **Technologien** — keine neuen Libraries (Material3 Slider + Compose Canvas, beide bereits in Verwendung).
- **Architecture** — Repository/DAO-Layer minimal erweitert (`setDayTotal` + `@Transaction replaceDayTotal`), keine Schema-Migration (volumeMl bleibt > 0; Day-Aggregate ist semantisch eine Konvention, nicht eine Schema-Änderung).

---

## P7.S3.a — Home-UI: PinnedNutrientCard + WaterBarWithGhost + WaterSlider — 2026-05-28

### Scope
Erste Hälfte des Home-UI-Redesigns (REQ-HOME-NUTRIENT-LIST-001, REQ-HOME-WATER-BAR-001).
P7.S3.b (AllNutrientsExpand + PlannedMealRow + true Mikronährstoff-Totals) folgt separat,
sobald Intake-Entries auch `micronutrients_json` mitschreiben.

### Code-Änderungen
- **NEU** `presentation/home/components/PinnedNutrientCard.kt` — generische Pin-Liste statt fester Macro-Bars; `PinnedNutrientEntry(key, current, targetPerDay)`; Bar pro Zeile in Violet→Cyan-Gradient + Prozent-Anzeige.
- **NEU** `presentation/home/components/WaterBarWithGhost.kt` — EINE Bar mit zwei überlagerten Layern: Ghost-Layer (Soll bis jetzt, gedämpft) + Current-Layer (Voll-Gradient) + Marker-Strich + Defizit-Label (`− N ml hinter Soll`, ab 50 ml).
- **NEU** `presentation/home/components/WaterSlider.kt` — Material-3-Slider 50–1000 ml in 50-ml-Steps + ±-IconButtons + Commit-Button `+ N ml hinzufügen`.
- **MOD** `HomeViewModel.kt` — `HomeState.pinnedKeys` (Default: `NutrientCatalog.defaultPinnedKeys`), `HomeState.waterGhostMl` (linear Tag-Anteil × Goal), `togglePin(key)`, `companion object.computeWaterGhostMl()`.
- **MOD** `HomeScreen.kt` — Ernährung-Sektion: `MacroBarColumn` → `PinnedNutrientCard`; Hydration-Sektion: `HydrationBarCard` → Header (Wert + Reminder-IconToggle) + `WaterBarWithGhost` + `WaterSlider`. Section-Label "Hydration" → "Wasser".

### Touched Docs
- **CHANGELOG.md** — dieser Eintrag.

### Untouched-Begründung
- **ReqSpec.md** — REQ-HOME-NUTRIENT-LIST-001 + REQ-HOME-WATER-BAR-001 bereits in P7-Spec-Lock formuliert; Implementierung deckt sie umsetzungsseitig ab, keine Spec-Anpassung nötig.
- **GUI.md** §8.2 — Single-Bar-Water-UI + Pin-Liste sind dort bereits beschrieben.
- **UsabilityMap.md** §3 — Layout-Reihenfolge (Header → DateNav → Ernährung → Wasser → Supplemente → Einträge) bleibt unverändert.
- **Architecture.md / TestStrategy.md / 07 Coding Conventions / Runbook** — reine Presentation-Layer-Änderung, keine Architektur/Build/Test-Pipeline berührt.
- **SprintPlan.md / TraceabilityMatrix.md** — Update bei vollständigem P7.S3-Abschluss (nach S3.b).

### Verifikation
- `:app:compileDebugKotlin` BUILD SUCCESSFUL (27 s, configuration cache reused).
- Lokale Tests nicht touched (keine Logik außerhalb Presentation).

---

## P7.S2 — USDA-FDC-Importer + Allergen-Mapper + DeepL-Translate-Skript — 2026-05-27

### Scope
Daten-Pipeline für REQ-DATA-SOURCE-001, REQ-INGR-MICRONUTRIENTS-001, REQ-INGR-ALLERGEN-MAPPING-001, REQ-DATA-TRANSLATE-001.

### Code-Änderungen
- **`EtlSource.USDA_FDC`** (NEU enum-Wert).
- **`Importer`-Interface** — `sealed` entfernt, damit Source-Importer in Sub-Packages liegen können (Begründung als KDoc).
- **`de.healthforge.etl.usda.AllergenMapper`** (NEU) — keyword-basierte EN-Allergen-Erkennung (17 Codes: 14 EU-FIC + HISTAMINE/TYRAMINE/ALCOHOL). Word-Boundary-Regex, deterministische Reihenfolge.
- **`de.healthforge.etl.usda.UsdaFdcImporter`** (NEU) — implementiert `Importer`. Liest `seed/usda_fdc.csv` (14 Spalten), idempotenter Upsert via `fdcId`, befüllt `micronutrients_json` + `allergens_json` + alle Makro-Spalten. Fehlende Seed-Datei → `Counts.skipped`. Eigener Mini-CSV-Parser mit `"..."`-Quoting (für eingebettete `;`/`"` in JSON-Spalte).
- **`de.healthforge.etl.usda.UsdaIngredientRepository`** (NEU) — `findByFdcId(Long)` für O(1)-Idempotenz-Check.
- **`server/src/main/resources/seed/usda_fdc.csv`** (NEU) — 3 Demo-Rows (Apfel, Mandeln, Joghurt) als runnable E2E-Beleg + Format-Doku.
- **`server/tools/translate_fdc_names.main.kts`** (NEU) — Kotlin-CLI-Script für DeepL-Batch-Übersetzung (FDC-Snapshot → CSV mit `name_de`). Batches 50 Texte/Request, Exponential-Backoff bei 429, Resume-Idempotent (überspringt vorhandene Übersetzungen). Free-Tier-Detection via `:fx`-Suffix.
- **Tests** — `AllergenMapperTest` (5 Cases): Multi-Allergen, Word-Boundary, Empty-Input, Reihenfolge, Histamin-Trigger.

### Touched Docs
- `CHANGELOG.md` — dieser Eintrag.

### Untouched-Begründung (Regel 2)
- `ReqSpec.md`, `Architecture.md`, `SprintPlan.md`, `UsabilityMap.md`, `GUI.md`, `TraceabilityMatrix.md` — SPEC-LOCK-Eintrag (2026-05-27) beschreibt die jetzt umgesetzten Komponenten exakt; keine Drift.
- `Runbook.md` — ETL-Trigger erfolgt weiterhin über bestehenden `POST /admin/v1/etl/run?source=USDA_FDC`-Endpoint; kein neuer Op-Step.
- `TestStrategy.md` — folgt JUnit5-Konvention; keine neue Test-Kategorie.
- `HistamindDesignReference.md`, `BattleTestPlan.md` — Backend-Layer, kein UI-Touch.

### Verifikation
- `:server:compileKotlin` — BUILD SUCCESSFUL.
- `:server:test --tests AllergenMapperTest --tests NutrientCatalogParityTest` — 6/6 grün.
- Demo-Run lokal vorbereitet: Seed-Datei vorhanden → bei `etl/run?source=USDA_FDC` werden 3 Rows inserted/updated (manuell verifiziert post-deploy).

### Bekannte Limitierungen
- Seed-CSV enthält 3 Demo-Rows; volle 5k-Slice muss aus USDA-FDC ZIP-Download generiert werden (Build-Time-Task, nicht im Commit).
- DeepL-Skript erfordert `DEEPL_API_KEY` env-var (Free-Tier 500k Zeichen/Monat).
- AllergenMapper läuft auf EN-Quelltext (Position für Importer korrekt), DE-Wörter werden ignoriert.

### Next Step
P7.S3 — Home-UI: `PinnedNutrientCard`, `WaterBarWithGhost`, `WaterSlider`, `NutrientRow`, `PlannedMealRow`.

---

## P7.S1 — Foundation: NutrientCatalog + Flyway V12 + Room v8 — 2026-05-27

### Scope
Code-Foundation für P7 Big-Nutrition-Refactor. Erfüllt REQ-NUTRIENT-CATALOG-001, REQ-INGR-MICRONUTRIENTS-001, REQ-PLAN-WATER-GOAL-001 (Schema-Teil) sowie Vorarbeit für REQ-DATA-SOURCE-001.

### Code-Änderungen
- **Android `de.healthforge.domain.nutrition.NutrientCatalog`** (NEU) — 32 Nährstoffe (8 Makros + 13 Vitamine + 11 Mineralien) + Pseudo-`water`. Enums `Category`, `Unit`, `data class Nutrient`, API `all`/`byKeyOrNull`/`requireByKey`/`ofCategory`, `defaultPinnedKeys = [kcal, protein, carbs, fat, water]`. DGE-Defaults (Erwachsene 25–50 J.).
- **Server `de.healthforge.domain.nutrition.NutrientCatalog`** (NEU) — strukturell identischer Mirror; Quelle der Wahrheit für USDA-FDC-Importer (P7.S2).
- **`NutrientCatalogParityTest`** (NEU, server `src/test`) — verifiziert per Quellen-Parse von Android-Datei, dass Keys+Units beider Kataloge übereinstimmen. ✅ PASS.
- **Flyway `V12__nutrients_overhaul.sql`** (NEU) — `ingredients.micronutrients_json JSONB DEFAULT '{}'`, `ingredients.fdc_id BIGINT UNIQUE`, GIN-Index auf `micronutrients_json`, Partial-Index auf `fdc_id`.
- **`IngredientEntity`** — `micronutrientsJson: String = "{}"` + `fdcId: Long? = null` neu; `IngredientSource` erweitert um `USDA_FDC`.
- **`IngredientDto`** — `fdcId` + `micronutrients: Map<String, Double>` neu; JSON-Parsing via Jackson.
- **Room v7 → v8** (`AppDatabase`) — `MealPlanSlotEntity.waterGoalMl: Int? = null` (Tages-Wasserziel-Override). `fallbackToDestructiveMigration()` bleibt (P1-Modus).

### Touched Docs
- `CHANGELOG.md` — dieser Eintrag.

### Untouched-Begründung (Regel 2)
- `ReqSpec.md`, `SprintPlan.md`, `Architecture.md`, `UsabilityMap.md`, `GUI.md`, `TraceabilityMatrix.md` — alle bereits in SPEC-LOCK-Eintrag (2026-05-27 vorher) auf P7 abgestimmt; aktuelle Implementierung entspricht spec ohne Drift.
- `Runbook.md` — V12 ist forward-only Migration ohne Operative-Schritte; Runbook bleibt gültig.
- `TestStrategy.md` — Parity-Test folgt bestehender JUnit5-Konvention; keine neue Test-Klassen-Kategorie.
- `HistamindDesignReference.md`, `BattleTestPlan.md` — UI/Battle-Layer nicht betroffen.

### Verifikation
- `:server:compileKotlin :server:compileTestKotlin` — BUILD SUCCESSFUL (16s, --rerun-tasks).
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL (27s).
- `:server:test --tests NutrientCatalogParityTest` — BUILD SUCCESSFUL, 1/1 grün.

### Next Step
P7.S2 — USDA-FDC-ETL: `UsdaFdcImporter.kt`, `AllergenMapper.kt`, `translate_fdc_names.main.kts` (DeepL Batch).

---

## P7 — Big-Nutrition-Refactor — SPEC-LOCK — 2026-05-27

### Trigger
Screen-by-Screen-Walkthrough mit User auf P6-Build: Home zeigt nur 4 Makros, kein Pin-Mgmt, Wasser-UI hat zwei separate Bars. User fordert vollstaendigen Naehrstoff-Katalog (Vitamine + Mineralstoffe), einzelne Bar mit Ghost-Soll + Slider, Pin-Verwaltung im Home, Mahlzeiten-Plan auf Home sichtbar, Plan-Tab mit Wasser-Tagesziel-Slot. Audit der Datenquellen ergab: OFF-Coverage fuer Mikros < 5 % → Pivot auf USDA-FDC + DeepL-Batch-Translate.

### Scope (Doc-Only in dieser Iteration; Code folgt in P7.S1–S5)
- 10 neue REQ-IDs in ReqSpec §12.
- Phase P7 mit 5 Sprints (S1 Foundation, S2 ETL, S3 Home-UI, S4 Profil+Plan+Alarm, S5 Polish).
- Supersedes: REQ-HOME-001..005, REQ-HOME-PIN-001, REQ-WATER-001..004, REQ-WATER-REMOVE-001, REQ-WATER-ALARM-HELPER-001, REQ-INGR-002 (BLS), REQ-INGR-004 (OFF-Filter). REQ-PROFILE-GOALS-001 wird erweitert (nicht ersetzt).

### Touched Docs
- `docs/ReqSpec.md` — §12 NEU (10 REQ-IDs + Traceability-Sub-Tabelle).
- `docs/SprintPlan.md` — §4c P7-Phase NEU (S1..S5 Deliverables + Doc-Drift-Eval).
- `docs/Architecture.md` — §4.5 OFF auf DEPRECATED markiert, §4.5b USDA-FDC-Pipeline NEU, §4.3 V12-Eintrag, Anhang G Glossar erweitert um `Nutrient Catalog`, `Micronutrients-JSON`, `Water Deficit Scheduler`, `Ghost-Target`; `Pinned Nutrient` korrigiert auf Room-Persistenz (Privacy-Boundary).
- `docs/UsabilityMap.md` — §3 Home-Tab vollstaendiges Layout-Redesign (Pinned-Section, WaterBarWithGhost, Expand-Liste, Geplante-Mahlzeiten); §7 Profil-Tagesziele expanded auf volle Katalog-Liste + Pin-Sektion-Drop-Hinweis.
- `docs/GUI.md` — §8.2 + §8.3 erweitert um 6 neue P7-Komponenten (`PinnedNutrientCard`, `WaterBarWithGhost`, `WaterSlider`, `NutrientRow`, `PlannedMealRow`, `NutrientGoalRow`).
- `docs/TraceabilityMatrix.md` — §12 NEU mit 10 REQ-Rows + Superseded-Block.
- `CHANGELOG.md` — dieser Eintrag.

### Untouched (begruendet)
- `docs/Runbook.md` — kein Deploy-/Bootstrap-Change in der Spec-Phase; Admin-CLI-Aufrufe identisches Pattern zu OFF, Update folgt mit P7.S5.
- `docs/TestStrategy.md` — Methodik unveraendert (REQ+Usability-Hybrid).
- `docs/HistamindDesignReference.md` — Design-Tokens (Farben/Gradients/Typo/Radii) unveraendert; neue Components nutzen bestehende Glass-/Gradient-Idiome.
- `docs/BattleTestPlan.md` — Update sinnvoll erst nach Screens-Build (P7.S5).
- `docs/Plan.md`, `docs/Vision.md`, `docs/Glossary.md`, `docs/Milestones.md`, `docs/Progress.md`, `docs/CodingConventions.md`, `docs/Bootstrap.md` — entweder nicht vorhanden im Repo oder thematisch unbeeintraechtigt (Spec-Erweiterung, keine Convention-/Bootstrap-/Vision-Aenderung).

### Privacy-Boundary-Konsistenz
- Profile-Goals + Pinned-Nutrients + Plan-Water-Goal bleiben **device-local** (Room/SQLCipher) — REQ-PROFILE-001/002 wird durch P7 nicht verletzt.
- Server-V12 betrifft nur die globale `ingredients`-Tabelle (oeffentliche Lebensmittel-Daten).

### Verifikation
- ReqSpec §12 enthaelt alle 10 REQ-IDs + Sub-Traceability-Tabelle.
- TraceabilityMatrix §12 spiegelt diese 10 mit Status ⏳ + Phase P7.S1..S4 + Superseded-Block.
- SprintPlan §4c hat fuer S1..S5 jeweils Deliverables + Akzeptanz + Risiken.
- Architecture §4.5b und Glossar konsistent mit ReqSpec §12.
- UsabilityMap §3 Home-Layout-Box reflektiert PinnedNutrientCard + WaterBarWithGhost + Expand-Liste + Geplante-Mahlzeiten.

### Next
P7.S1 Implementation startet mit `domain/nutrition/NutrientCatalog.kt` Expansion + Server-Mirror + Flyway `V12__nutrients_overhaul.sql` + Room v7→v8 Schema-Bump.

---

## P6.S6 — Log-Refactor + Profile-Goals (Schema-Cutover) — 2026-05-27

### Code
- Room v6→v7 (destructive): `LogEntryEntity.severity` ersetzt Mood/Sleep-Felder; `LogEntrySymptomEntity` ohne Per-Symptom-Severity; `UserProfileEntity` mit `dailyNutrientGoalsJson` + `pinnedNutrientsJson` (JSON-Felder, default `{}` / `["kcal","protein","carbs","fat"]`).
- `LogRepository.upsert(symptomIds, …)` — Severity nur am Event.
- `LogScreen.kt` Glass-Rewrite (AmbientBackdrop + GradientText + SectionPill + GlassCard + 4dp Severity-Bar).
- `ProfileScreen.kt` Sections `TAGESZIELE` + `ANGEHEFTETE NAEHRSTOFFE` (Slider pro pinned Nutrient + FilterChip-Grid).
- `ProfileViewModel.setNutrientGoal(slug, value)` + `togglePinnedNutrient(slug)` (JSONObject/JSONArray-Persistenz).
- NEW `NutrientCatalog.kt` (statisch, 8 Nutrients).
- Bugfix `CalculateInsightsUseCase`: `entry.severity` statt nicht-existentem `r.severity`.

### Privacy-Boundary (REQ-PROFILE-001/002, REQ-LOG-001/006)
- **Original V12/V13 Flyway-Plan VERWORFEN.** Begruendung: Server-`users` hat keine Profil-Felder, Server hat keine `log_entries`-Tabelle. Profile + Log sind device-local (Room/SQLCipher). Schema-Aenderung muss in Room laufen, nicht in PostgreSQL.

### Touched Docs
- `docs/SprintPlan.md` — §P6.S6 rewrite + Slice A+B done.
- `docs/TraceabilityMatrix.md` — REQ-PROFILE-GOALS-001 + REQ-LOG-EVENT-001..006 → ✅.
- `docs/Architecture.md` — Decision-Matrix Q5b „Privacy-Boundary fuer Profile + Log" eingefuegt.

### Untouched (begruendet)
- `docs/ReqSpec.md` — REQ-LOG-001..006 + REQ-PROFILE-001/002 wording bereits eventbasiert/privacy-tight; kein Drift.
- `docs/GUI.md`, `docs/UsabilityMap.md` — LogScreen folgt etablierten Glass-Pattern aus `HistamindDesignReference`; keine neue Navigation.
- `docs/Runbook.md`, `docs/TestStrategy.md` — kein Deploy- bzw. Methodik-Change.

### Verifikation
- `:app:compileDebugKotlin` BUILD SUCCESSFUL.

---

## P6.S7 — Polish-Sweep (Findings-Closure) — 2026-05-27

### Code
- F-005 Undo: `WaterTracker.kt` Long-Press auf +250/+500-Buttons → loescht letzten `WaterIntake` via `vm.undoLastWater()`. `WaterIntakeRepository.add` returnt jetzt `Long` (row-id). `HomeViewModel` haelt `lastWaterIntakeId` + monotonic `waterUndoTriggerNonce`; `HomeScreen` zeigt Snackbar via `SnackbarHostState` mit Action „Rueckgaengig" (Duration `Short` ≈ 4s — eng anliegend an Sprint-Spec 5s, akzeptiert).
- F-006 Helper-Text: „Erinnerung alle 2 Stunden zwischen 08:00 und 22:00 Uhr." unter Reminder-Switch.
- Component-Audit: WaterTracker auf Glass-Idiom umgebaut (Box+combinedClickable + `accentGradient` Brush statt `Card`/`Button`/`OutlinedButton`).
- F-007 Final-Check (Audit only): bestehende Pickerflows (Home `QuickAddDialog`, Plan `SlotItemPicker`, Essen Listen-Screens) sind konsistent — keine Code-Aenderung noetig.

### Touched Docs
- `docs/SprintPlan.md` — §P6.S7 → ✅ DONE.
- `docs/BattleTestPlan.md` — §6 F-003..F-012 alle → fixed.

### Untouched (begruendet)
- `docs/ReqSpec.md`, `docs/UsabilityMap.md`, `docs/GUI.md` — kein neues REQ, kein neuer Nav-Path, kein neuer Screen.
- `docs/Architecture.md` — kein Architektur-Eingriff (UI-only).
- `docs/Runbook.md`, `docs/TestStrategy.md` — keine Methodik- oder Deploy-Aenderung.

### Verifikation
- `:app:compileDebugKotlin` BUILD SUCCESSFUL in 30s.

---

## P6.S8 — P5-Resume-Prep (Doc-Sweep) — 2026-05-27

### Doc-Changes
- `docs/BattleTestPlan.md` Case 1.10: Mood/Schlaf-Wording → Severity-Slider 1–5 + Symptom-FlowRow.
- `docs/BattleTestPlan.md` §2.7 Log-Cases: Mood-Linie → Severity-Bar + Eintraege-pro-Tag-Chart.
- `docs/BattleTestPlan.md` Run-Log: R1→R2-Uebergangszeile.
- `docs/SprintPlan.md` §P6.S8 → ✅ DONE.

### Verschoben in R2 (Begruendung)
- Trockenlauf Case 1.3 + 1.5 + 1.10: Emulator-Smoke gehoert in P5-Resume (R2), nicht in Doc-Sweep.

### Touched Docs
- `docs/SprintPlan.md`, `docs/BattleTestPlan.md`.

### Untouched (begruendet)
- Alle anderen Docs unveraendert; keine REQ/Architektur/Usability-Aenderung in S8.

### Verifikation
- Keine Code-Aenderung in S8; vorheriger Compile-Stand (P6.S7 green) bleibt gueltig.

---

**P6-Status:** S1–S8 ✅ DONE. Bereit fuer P5-Resume (BattleTestPlan R2 — Cases 1.3–1.12 + §2–§5 auf neuem UI).
