# HealthForge CHANGELOG

Konvention: Jeder P6-Sprint-Abschluss + jede groessere Code-/Verhaltens-Aenderung erhaelt einen Eintrag.
Format pro Eintrag: **Sprint/Datum** + **Touched Docs** + **Untouched-Begruendung** + **Verifikation**.

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
