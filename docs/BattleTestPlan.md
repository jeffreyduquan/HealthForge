# HealthForge — Battle Test Plan

- **Version:** 1.0 (2026-05-26)
- **Status:** Active — Run 1 ausstehend
- **Companion:** [TestStrategy.md](TestStrategy.md), [TraceabilityMatrix.md](TraceabilityMatrix.md), [UsabilityMap.md](UsabilityMap.md)
- **Scope:** v1.0 manueller Deep-Test über alle 3 Surfaces (Android, Server, Admin-UI)
- **Methodik:** REQ-driven + Usability-driven (Hybrid)
- **Device-Setup:** Pixel 7 Emulator (API 35) · Spring-Server lokal `:8080` · Admin-UI Vite `:5173`
- **Run-Kadenz:** Single-Run-Then-Fixes (vgl. TestStrategy §6)

---

## Ergebnis-Legende

| Symbol | Bedeutung |
|---|---|
| ⬜ | Nicht ausgeführt |
| ✅ | Pass (Funktion + Usability beide grün) |
| ⚠️ | Pass mit Notes (Minor Smell, S3, kein Blocker) |
| ❌ | Fail (S1 oder S2 — Eintrag in §6 Failures-Log) |
| ⏭ | Skipped (Begründung in Notes) |
| 🟡 | MVP-Fallback akzeptiert (gemäß TraceabilityMatrix Final-Review) |

Pro Case in der **Result**-Spalte: Symbol + Datum (`✅ 2026-05-27`) + ggf. Run-ID
(`R2`). Ein leeres Symbol bedeutet noch nicht ausgeführt.

---

## Run-Log

| Run | Datum | Surface | Ergebnis kurz |
|---|---|---|---|
| R1 | _pending_ | Full Sweep §1–§5 | _ausstehend_ |
| R2 | _pending_ | Re-Run S1+S2 fails | _ausstehend_ |

---

## §1 Persona-Smoke — Marie 7-Tage-Journey

Vor §2–§5 durchzuführen. Wenn §1 kippt: erst fixen, dann Tiefe.

| # | Schritt | Pass-Kriterium | REQ-IDs | UsabilityMap-Anker | Result | Notes |
|---|---|---|---|---|:-:|---|
| 1.1 | Frischer Emulator: App installieren, Welcome-Screen | Logo + 3 Bullets + „Los geht's"-Button sichtbar; kein Crash | REQ-ONBOARD-001 | §2 Step 1 | ⬜ | |
| 1.2 | Onboarding 14 Steps mit realistischer Eingabe (Email/Passwort, Marie, weiblich, 168 cm, 62 kg, moderat, Erhalten, Histamin-Toggle ON, Laktose ON) | Alle Steps forward-only, Validierung pro Step, Confetti am Ende | REQ-ONBOARD-001, REQ-PROFILE-001..006, REQ-AUTH-002 | §2 Steps 1–17 | ⬜ | |
| 1.3 | Home öffnet, leere Ringe + Wasser 0/2000 ml + leere Liste | 4 Ringe sichtbar (kcal/P/F/C); Wasser-Block mit +250/+500/Custom; Empty-State „Heute noch nichts geloggt." | REQ-HOME-001..005, REQ-WATER-001 | §3.1 + §12 | ⬜ | |
| 1.4 | Tag 1: Quick-Add Müsli 80 g → Wasser +500 → +500 → Apfel 150 g | Ringe füllen sich live; Snackbar Undo erscheint; Liste zeigt 2 Items + Wasser 1000 ml | REQ-INTAKE-001..004, REQ-HOME-003, REQ-WATER-002, REQ-INGR-001/002 | §3.2 + §8.1 | ⬜ | |
| 1.5 | Essen → Rezepte → FAB+ → Rezept „Linsen-Curry" anlegen (4 Portionen, 2 Zutaten, 3 Schritte, public) | Speichern OK; Rezept erscheint in Liste; Detail zeigt Nährwerte live | REQ-RECIPE-001/002/005/007 | §5.4–§5.6 | ⬜ | |
| 1.6 | Plan-Tab: Mo–Mi je 1 Slot mit Linsen-Curry; Do leerer Slot | Slots werden gespeichert; Wechsel zwischen Tagen erhält State | REQ-PLAN-001/002/005 | §4.1/4.2 | ⬜ | |
| 1.7 | Plan ⋮ → „Einkaufsliste erstellen" → 7-Tage-Bereich → Generieren | Liste mit aggregierten Items (Linsen 3×200 g = 600 g), Kategorie-Gruppen, Strict-Mode-Hinweis falls aktiv | REQ-SHOP-001/002/003 | §4.4 + ShoppingScreen | ⬜ | |
| 1.8 | Supplements-Tab → Vitamin D 1000 IE anlegen mit Daily-Reminder 08:00 | Supplement in Liste; nächster AlarmManager-Eintrag in `adb shell dumpsys alarm` sichtbar | REQ-SUPP-001/002/005, REQ-REMIND-001/002 | §5.7 | ⬜ | |
| 1.9 | Wasser-Reminder Toggle ON (Home WaterTracker) | Toggle bleibt nach Recompose ON; SharedPreferences `hf_water_reminder` enthält `enabled=true`; AlarmManager-Eintrag mit Action `WATER_REMINDER_FIRE` | REQ-REMIND-001 | §3.2 + WaterTracker | ⬜ | |
| 1.10 | Log-Tab: Eintrag Mood 7, Schlaf 4★/7.5 h, Symptom „Kopfschmerz" Severity 3, Notiz | Eintrag erscheint im Verlauf, editierbar | REQ-LOG-001..006 | §6 | ⬜ | |
| 1.11 | Profil → „Daten exportieren" → PDF + JSON | Beide Dateien in `Downloads/HealthForge/`; PDF öffnet ohne Crash; JSON ist valides JSON mit Profil + Intake + Log | REQ-EXPORT-001..004 | §7.2 | ⬜ | |
| 1.12 | App force-stop → Emulator-Reboot (`adb reboot`) → App öffnen | Supplement-Reminder + Water-Reminder wieder im `dumpsys alarm` sichtbar (BootReceiver) | REQ-REMIND-002 | — | ⬜ | |

**§1 Gate:** Alle 12 Cases ✅ → §2 starten. Mind. ein ❌ → fixen, §1 wiederholen.

---

## §2 Android — REQ + Usability pro Screen

### §2.1 Auth & Onboarding

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Login mit Email/Passwort | Token persistiert in EncryptedSharedPreferences (`adb shell run-as de.healthforge cat shared_prefs/secure_tokens.xml` zeigt verschlüsselten Blob) | REQ-AUTH-001/005/007 | §2 Step 2 | ⬜ | |
| Register mit Invite-Code (valid + invalid) | Invalid → Fehler „Code ungültig"; valid → Email-Verify-Screen | REQ-AUTH-002/003 | §2 Step 2+3 | ⬜ | |
| Email-Verify Link-Klick | User-Status in DB `email_verified=true` | REQ-AUTH-004 | §2 Step 3 | ⬜ | |
| Passwort vergessen → Reset-Email → neues PW setzen | Login mit neuem PW funktioniert; alter PW abgelehnt | REQ-AUTH-006 | Profile → Account | ⬜ | |
| Onboarding Step 11 (Allergien) Skip ohne Eingabe | Weiter ohne Warnung möglich (MVP-Fallback) | REQ-ONBOARD-002 🟡 | §2 Step 11 | 🟡 | MVP-Fallback per Final-Review akzeptiert |
| Onboarding wiederholen aus Profil | Wizard öffnet mit prefilled Werten | REQ-ONBOARD-003 | §7.2 | ⬜ | |

### §2.2 Home

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Makro-Ringe nach Quick-Add | Ringe animieren live; Werte stimmen mit `psql … intake_entries` | REQ-HOME-001/002, REQ-INTAKE-001 | §3.1 | ⬜ | |
| Datums-Nav Vor/Zurück + Picker | Wechsel zeigt Daten dieses Tages; Empty-State korrekt | REQ-HOME-005 | §3.1 | ⬜ | |
| Quick-Add (Lebensmittel) | Mengen-Dialog Live-Preview; Speichern OK; Snackbar Undo funktioniert | REQ-INTAKE-001..003, REQ-HOME-003 | §8.1 | ⬜ | |
| Quick-Add Snapshot (Ingredient ändern → Intake-Wert unverändert) | Edit Ingredient `kcal_per_100g` in DB; Home-Listenwert für alten Intake bleibt | REQ-INTAKE-003 | — | ⬜ | |
| 7-Tage-Edit-Fenster | Intake-Eintrag älter 7 Tage: Edit-Dialog disabled | REQ-INTAKE-004 | §3.2 | ⬜ | |
| Wasser +250/+500/Custom | Snackbar mit Undo; Custom-Dialog akzeptiert nur Zahl | REQ-WATER-001..004 | §3.1 | ⬜ | |
| Supplement-Checkliste am Tag mit fälligem Reminder | Checkbox tap → Strike-Through + grünes Check; IntakeEntry erzeugt (`sourceType=SUPPLEMENT`) | REQ-HOME-004, REQ-SUPP-003 | §3.1 | ⬜ | |
| Verlauf-Button → IntakeHistory-Screen | Chronologisch + Day-Gruppen + Date-Picker | REQ-HOME-005, REQ-NAV-004 | §3.1 | ⬜ | |

### §2.3 Essen → Lebensmittel

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Suche „müsli" findet Treffer | Liste zeigt ≥1 Item; FTS+Trigram trifft auch „Muesli" (unaccent) | REQ-INGR-001/002, REQ-SEARCH-001/002 | §5.2 | ⬜ | |
| Filter Allergene aus Profil prefilled | Filter-Sheet öffnet mit Marie's Laktose-Toggle ON | REQ-SEARCH-002/003 | §5.2 | ⬜ | |
| FODMAP+Histamin Chips in Row | AssistChips mit German-Labels (Fructose/Lactose/…), Histamin-Score 0–3 | REQ-SEARCH-005, REQ-QUALITY-003/004, REQ-QUALITY-UI-001 | §5.2 | ⬜ | |
| Detail zeigt Allergen-Block + FODMAP + Histamin + Quelle | „Enthält:" / „Frei von:" / „Unbekannt:" + Quelle BLS/SIGHI/USER | REQ-QUALITY-005, REQ-QUALITY-UI-001 | §5.3 | ⬜ | |
| Inline-Filter „Profil-Filter aktiv" Toggle | OFF → mehr Items sichtbar; ON → laktosehaltige verschwinden (MVP-Fallback statt UseCase-Layer) | REQ-SEARCH-004 🟡 | §5.2 | 🟡 | UseCase-Refactor Backlog |
| Lebensmittel-Vorschlag (User-Submit) | POST `/v1/ingredients/suggestions` → in Admin-Queue sichtbar | REQ-INGR-USER-001 | §5.2 Empty-State | ⬜ | |
| Field-PR vorschlagen (z.B. Histamin-Score) | Dialog → Submit → in Admin Field-PR-Queue sichtbar | REQ-FIELDPR-001/002 | §5.3 | ⬜ | |
| PENDING-Ingredient nur für Submitter sichtbar | Suche als Fremd-User findet das PENDING-Item NICHT | REQ-INGR-USER-002 | — | ⬜ | |

### §2.4 Essen → Rezepte

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Rezept anlegen 5 Steps | Validierung pro Step (Titel/Servings/Prep/Slot-Tags/Zutaten/Schritte) | REQ-RECIPE-001/005 | §5.6 | ⬜ | |
| Bild-Upload | 1080px JPEG Q85; Server speichert 3 Sizes (256/800/1600); CDN-URL in DTO | REQ-RECIPE-006 | §5.6 Step 1 | ⬜ | |
| Sichtbarkeit Public/Private/Gruppe | Group-Picker zeigt nur eigene Gruppen; PRIVATE nur für Owner sichtbar | REQ-RECIPE-003, REQ-GROUP-005/006 | §5.6 Step 2 | ⬜ | |
| Nährwerte live im Detail | „pro Portion" Aggregat-Block; bei fehlender Zutat „missing_ingredients" markiert | REQ-RECIPE-007 | §5.5 | ⬜ | |
| User-Allergen-Warning im Detail | Marie (Laktose) sieht „⚠️ Enthält für dich: Lactose" wenn Rezept Milch-Zutat hat | REQ-RECIPE-005 | §5.5 | ⬜ | |
| Like-Button | ❤-Counter steigt; zweiter Tap → unlike | REQ-RECIPE-004 | §5.5 | ⬜ | |
| Rating Community (👍 / 👎 / ↑häufiger / ↓vertrage nicht) | DB-Tabelle `recipe_ratings_community` Eintrag; Revoke löscht ihn | REQ-RATING-002/005 | §5.5 | ⬜ | |
| Edit/Delete nur Owner | Fremd-User → kein Edit-Icon; Server-403 falls direkt aufgerufen | REQ-RECIPE-008 | §5.5 | ⬜ | |
| Soft-Delete versteckt aus Browse | `RecipeStatus.REMOVED` → Liste filtert raus | REQ-RECIPE-009 ⏳ | — | ⬜ | Recipe-Snapshot in IntakeEntry: Backlog |
| Rezept melden | Dialog → Report → in Admin Reports-Queue sichtbar | REQ-GROUP-007 | §5.5 | ⬜ | |

### §2.5 Essen → Supplements

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Supplement lokal anlegen mit Reminder ONCE/DAILY/WEEKLY | 3 separate Tests; jeder Mode in `dumpsys alarm` korrekt | REQ-SUPP-001/002/005, REQ-REMIND-002 | §5.7 | ⬜ | |
| „Jetzt eingenommen"-Button | IntakeEntry mit Snapshot-Dosis erstellt | REQ-SUPP-003 | §5.7 Detail | ⬜ | |
| „Vorschlag an Server senden" → Server-Queue | POST `/v1/supplements/suggestions`; Admin sieht Eintrag | REQ-SUPP-004 | §5.7 Detail | ⬜ | |
| Notification Tap → MainActivity öffnet | PendingIntent → MainActivity startet bzw. focused | REQ-REMIND-002 | — | ⬜ | |

### §2.6 Plan-Tab

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Slot-Picker Rezept/Zutat | ModalBottomSheet mit Tabs; Suche funktioniert | REQ-PLAN-001/002 | §4.2 | ⬜ | |
| Slot „Habe gegessen" ✓ | IntakeEntry mit Default-Portion erzeugt | REQ-PLAN-004 | §4.2 | ⬜ | |
| Plan ist lokal-only (kein Server-Sync) | Network-Inspector zeigt keinen `/plan/*`-Call | REQ-PLAN-003 | — | ⬜ | |
| Auto-Planner-Dialog 4 Steps | Slots/Tage/Override/Strict → Loading → Preview → Übernehmen | REQ-AUTOPLAN-001/003/004 | §4.3 | ⬜ | |
| Auto-Planner Server-Beam-Search liefert ≥1 Vorschlag pro Slot | Preview zeigt nicht-leere Slots; respektiert exclude_allergens | REQ-AUTOPLAN-002/003 | §4.3 | ⬜ | |

### §2.7 Log-Tab (Symptom-Tagebuch)

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Eintrag mit allen Feldern speichern | Mood-Slider live, Symptom-Picker funktional, Tags free-add | REQ-LOG-001/002 | §6.1 | ⬜ | |
| Custom-Symptom anlegen + verwenden | Symptom in Picker auswählbar; gespeichert | REQ-LOG-003 | §6.2 | ⬜ | |
| Mehrere Einträge pro Tag | „+ Weiterer Eintrag heute" zeigt 2 Blöcke | REQ-LOG-004 | §6.1 | ⬜ | |
| Verlauf zeigt Mood-Linie + Symptom-Heatmap | LogChartsScreen 7/30 Tage Toggle | REQ-LOG-005 | §6.2 | ⬜ | |
| Edit nur innerhalb 7 Tage | Älterer Eintrag → Form disabled mit Hinweis | REQ-LOG-006 | §6.2 | ⬜ | |
| Insights-Lock unter 14 Log-Tagen | InsightsScreen zeigt LockedPane | REQ-INSIGHT-001 | §6 → Charts | ⬜ | |
| Insights nach 14 Log-Tagen | Lift-Ranking sichtbar (`lift ≥ 1.5`, n ≥ 3) | REQ-INSIGHT-002/003 | — | ⬜ | |

### §2.8 Gruppen (P3)

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Gruppe erstellen (public/private) | Eintrag in `groups`-Tabelle; in „Meine Gruppen" sichtbar | REQ-GROUP-001/002/003 | §7.2 | ⬜ | |
| Beitreten via Invite-Code | Code-Eingabe → Member-Row erzeugt | REQ-GROUP-003 | §7.2 | ⬜ | |
| Beitreten public via Discover | Discover-Liste → Beitreten → Member-Row | REQ-GROUP-003 | §7.2 | ⬜ | |
| Owner kickt Member | Confirm-Dialog → Member-Row gelöscht | REQ-GROUP-004 | §7.2 | ⬜ | |
| Ownership transfer (2-Step) | Demote→Promote; alter Owner ist normales Member | REQ-GROUP-004 | §7.2 | ⬜ | |
| Rezept mit visibility=GROUP nur für Mitglieder | Nicht-Mitglied → Server-403 / nicht in Liste | REQ-GROUP-005 | §5.5 | ⬜ | |
| Rezept-Sichtbarkeit-Chip „Gruppe" | AssistChip im Detail | REQ-GROUP-006 | §5.5 | ⬜ | |

### §2.9 Export & Profil

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Profil-Edit Display-Name, Größe, Gewicht | Werte persistiert in Room+Server-DTO | REQ-PROFILE-001/003 | §7.2 | ⬜ | |
| Strict-Mode-Toggle global | Toggle in Profil → Lebensmittel-Filter respektiert | REQ-PROFILE-004/005 | §7.2 | ⬜ | |
| Daten-Export PDF + JSON enthält alle Sektionen | Server-Anteil (Account, Rezepte, Supplement-Vorschläge) + Lokal-Anteil (Profil, Intake, Wasser, Logs, Supplements, Reminder) | REQ-EXPORT-001..004 | §7.2 | ⬜ | |
| Konto löschen 2-Step Confirm | Password-Input → DELETE; lokale DB gewipt; Logout | REQ-PROFILE-001 + AccountDelete | §7.2 | ⬜ | |
| Theme System/Light/Dark | Wechsel ohne App-Restart | — | §10.1 | ⬜ | |
| Onboarding wiederholen | Wizard öffnet prefilled | REQ-ONBOARD-003 | §7.2 | ⬜ | |

### §2.10 Navigation & Theming

| Case | Pass-Kriterium | REQ-IDs | UsabilityMap | Result | Notes |
|---|---|---|---|:-:|---|
| Bottom-Nav 5 Tabs schaltbar | Jeder Tab erreichbar; State pro Tab erhalten beim Wechsel | REQ-NAV-001 | §1.1 | ⬜ | |
| Essen 3 Sub-Tabs | Lebensmittel/Rezepte/Supplements wechselbar; State erhalten | REQ-NAV-002 | §1.2 | ⬜ | |
| Phase-Placeholder weg in finaler Build | Kein „Bald verfügbar"-Screen mehr (alle in P4 final) | REQ-NAV-003 | §11 | ⬜ | |
| 48 dp Touch-Target + contentDescription | Stichproben via `uiautomator dump`: alle Buttons ≥48 dp | — | §14 | ⬜ | |
| Light + Dark visuelle Prüfung jedes Screens | Kein „black blob" in Dark; kein weißer Glow in Light | — | §10 | ⬜ | |

---

## §3 Server — REQ + Endpoint

| Case | Pass-Kriterium (cURL/HTTPie + DB-Check) | REQ-IDs | Result | Notes |
|---|---|---|:-:|---|
| `POST /v1/auth/register` happy | 201 + verify-Email gesendet | REQ-AUTH-002 | ⬜ | |
| `POST /v1/auth/login` 5×/min Rate-Limit | 6. Request → 429 | REQ-AUTH-001 | ⬜ | |
| `POST /v1/auth/refresh` Rotation | Alter Refresh-Token wird ungültig | REQ-AUTH-005 | ⬜ | |
| `POST /v1/auth/verify` Token-Klick | `users.email_verified=true` | REQ-AUTH-004 | ⬜ | |
| `POST /v1/auth/password-reset` + `/reset` | Token-only-Use; zweiter Versuch 400 | REQ-AUTH-006 | ⬜ | |
| `GET /v1/ingredients/search?q=…&excludeAllergens=…&excludeFodmap=…` | Filter wirkt; FTS+Trigram-Indizes (V5) genutzt (EXPLAIN ANALYZE prüfen) | REQ-INGR-002, REQ-SEARCH-001/002/003 | ⬜ | |
| `GET /v1/ingredients/{id}` zeigt Quality-Felder | `histamine_score`, `fodmap_flags`, `source` enthalten | REQ-QUALITY-003/004/005 | ⬜ | |
| `POST /v1/ingredients/suggestions` | Eintrag in `ingredient_submissions` PENDING | REQ-INGR-USER-001 | ⬜ | |
| `POST /v1/ingredients/{id}/field-pr` | Eintrag in `ingredient_field_pr` PENDING | REQ-FIELDPR-001 | ⬜ | |
| `POST /v1/recipes` + `PATCH` + `DELETE` (Owner-Check) | Fremd → 403 NOT_OWNER | REQ-RECIPE-001/002/008 | ⬜ | |
| `POST /v1/recipes/{id}/like` + `DELETE` | Like-Counter steigt/fällt; idempotent | REQ-RECIPE-004 | ⬜ | |
| `PUT /v1/recipes/{id}/community-rating` + `DELETE` | Eintrag in `recipe_ratings_community`; Revoke löscht | REQ-RATING-002/005 | ⬜ | |
| `POST /v1/media/upload` 3-Size-Pipeline | Response enthält 3 URLs (256/800/1600); CDN reachable | REQ-RECIPE-006 | ⬜ | |
| `POST /v1/groups` + join/leave + transferOwnership | DB-Constraint verhindert Owner-Leave ohne Transfer | REQ-GROUP-001..004 | ⬜ | |
| `GET /v1/recipes?visibility=GROUP` Membership-Filter | Nicht-Mitglied bekommt das Rezept NICHT | REQ-GROUP-005 | ⬜ | |
| `POST /v1/recipes/{id}/reports` | Eintrag in `recipe_reports` | REQ-GROUP-007 | ⬜ | |
| `POST /v1/supplements/suggestions` + `GET /v1/supplements/public` | Public-Liste nur APPROVED | REQ-SUPP-004 | ⬜ | |
| `POST /v1/autoplan/generate` Beam-Search | Response in <3 s lokal; respektiert exclude_allergens | REQ-AUTOPLAN-002/003 | ⬜ | |
| `GET /v1/export/me.pdf` + `me.json` | Beide MIME-korrekt; PDF lesbar | REQ-EXPORT-001..004 | ⬜ | |
| Admin `POST /admin/v1/ingredients/{id}/approve` | nur ADMIN-Role; sonst 403 | REQ-FIELDPR-003, REQ-INGR-USER-001 | ⬜ | |
| Admin `GET /admin/v1/audit?actor=…&from=…` | Filter funktional; pagination | REQ-ADMIN-FULL-001 | ⬜ | |
| Admin `POST /admin/v1/etl/run` (ETL) | 🟡 MVP-Fallback: nur via Postman/curl, keine UI-Seite | REQ-ADMIN-002 🟡 | 🟡 | ETL-UI Backlog |
| Flyway V1..V11 auf leerer DB | `flyway info` → alle „Success", keine pending | REQ-PLATFORM-003 | ⬜ | |

---

## §4 Admin-UI — REQ + Page

| Case | Pass-Kriterium | REQ-IDs | Result | Notes |
|---|---|---|:-:|---|
| Admin-Login (Email+Pwd, role=ADMIN) | Login OK; Cookie HttpOnly+Secure+SameSite=Strict | REQ-ADMIN-001 | ⬜ | |
| Login als normaler User → 403 | Backend `AdminAuthFilter` blockt | REQ-ADMIN-001, REQ-ADMIN-FULL-002 | ⬜ | |
| Dashboard KPI-Cards | User-Count, Ingr-Total, Pending-Queues (3), DB-Größe, Phase | REQ-ADMIN-FULL-001 | ⬜ | |
| Statistics-Page Charts laden | Tages-Linien für User-Wachstum, DB-Wachstum | REQ-ADMIN-FULL-001 | ⬜ | |
| Audit-Log mit Filter actor/action/from/to | Filter-Combo wirkt; Pagination | REQ-ADMIN-FULL-001 | ⬜ | |
| Invites-Page neuer Code + deaktivieren | Code in DB; deaktiviert → 400 bei Register | REQ-AUTH-003 | ⬜ | |
| Ingredient-Queue Approve/Reject | Bei Approve: Item öffentlich sichtbar in Android-Search | REQ-INGR-USER-001/002 | ⬜ | |
| Field-PR Approve/Reject mit Note | Bei Approve: `ingredients.histamine_score` mutiert | REQ-FIELDPR-001/002/003 | ⬜ | |
| Supplement-Queue Approve/Reject | Bei Approve: in `/v1/supplements/public` sichtbar | REQ-SUPP-004 | ⬜ | |
| Recipe-Reports-Page Resolve/Dismiss/Delete | Bei Delete: Rezept `status=REMOVED` | REQ-GROUP-007 | ⬜ | |
| Users-Page Ban/Unban/Delete | Banned User → Login 403 | REQ-ADMIN-FULL-001 | ⬜ | |
| Dark/Light + Mobile-Responsive | MUI dark mode toggle; ≤ 768 px Sidebar collapsed | — | ⬜ | |

---

## §5 Negative & Security

| Case | Pass-Kriterium | REQ-IDs | Result | Notes |
|---|---|---|:-:|---|
| JWT manipuliert (Signatur invalid) | 401 INVALID_TOKEN | REQ-AUTH-005 | ⬜ | |
| JWT expired (15 min Access) | 401 + Client triggert Refresh-Flow | REQ-AUTH-005 | ⬜ | |
| Refresh-Token cross-account | Auf User-B-Token User-A's Access anfragen → 401 | REQ-AUTH-005 | ⬜ | |
| Login Rate-Limit Bucket4j | 6×/min → 429 | REQ-AUTH-001 | ⬜ | |
| SQL-Injection in Search (`%' OR 1=1--`) | Keine Daten-Leak; FTS-Query escaped | REQ-INGR-002 | ⬜ | |
| User-A schreibt Field-PR auf Ingr → User-B's PENDING-Ingr nicht sichtbar | `IngredientSearchRepository.search(viewerId)` Visibility | REQ-INGR-USER-002 | ⬜ | |
| User-A versucht User-B's Rezept zu PATCHen | 403 NOT_OWNER | REQ-RECIPE-008 | ⬜ | |
| User-A liest User-B's Export `/v1/export/me` | Niemals User-B-Daten — `/me`-Pattern | REQ-EXPORT-001 | ⬜ | |
| User-A pingt `/admin/v1/*` | 403 (AdminAuthFilter) | REQ-ADMIN-001 | ⬜ | |
| Airplane-Mode → Quick-Add → Re-Connect | Eintrag lokal in Room; UI zeigt Offline-Banner (falls REQ-OFFLINE-003 implementiert — sonst Notiz) | REQ-INTAKE-002, REQ-OFFLINE-003 ❌ | ⬜ | OFFLINE-003 Backlog |
| Concurrent-Edit (2 Sessions auf Profil) | Letzter Write gewinnt; kein Crash | REQ-PROFILE-001 | ⬜ | |
| Allergen-Warning bei manipuliertem Recipe (Zutat hinzugefügt) | Detail zeigt Warning sofort beim Re-Open | REQ-RECIPE-005 | ⬜ | |
| Reminder-Fire auf gestoppter App | Notification erscheint trotzdem (AlarmManager OS-side) | REQ-REMIND-001/002 | ⬜ | |
| Wasser-Reminder außerhalb Aktiv-Fenster (22–08) | Kein Reminder; nächster ist morgen 08:00+intervall | REQ-REMIND-001 | ⬜ | |
| Lebensmittel-Vorschlag mit XSS-Payload `<script>` im Namen | Server escaped beim Render in Admin-UI | REQ-INGR-USER-001 | ⬜ | |
| DB-Backup → Restore (Runbook §3.3) | Pre-flight Übung; restored DB konsistent | REQ-PLATFORM-003 | ⬜ | |

---

## §6 Failures-Log

Jeder ❌-Fail wird hier mit Severity + Repro + Fix-Status getrackt.

| ID | Datum | Surface | Case | Severity | Repro (3 Zeilen) | Status | Fix-Commit |
|---|---|---|---|---|---|---|---|
| _F-001_ | _pending_ | _—_ | _—_ | _—_ | _—_ | _open_ | _—_ |

**Workflow:**

1. ❌ in Result-Spalte setzen → Eintrag hier mit F-nnn
2. Severity nach TestStrategy §5 (S1=Blocker, S2=Major, S3=Minor)
3. Fix-Commit nach Behebung verlinken
4. Re-Run-Result in original-Case auf ✅ R2

---

## §7 Sign-Off

Diese Sektion wird datiert + signiert sobald §1–§5 mindestens 1× ohne offene S1+S2-Fails durchgelaufen sind. Vorher leer lassen.

- **Run 1 abgeschlossen:** _Datum offen_
- **Run 2 (Re-Run S1+S2) abgeschlossen:** _Datum offen_
- **Negative+Security §5 grün:** _Datum offen_
- **Persona-Smoke §1 grün:** _Datum offen_
- **Sign-Off (v1.0 ship-ready für Beta):** _Datum offen — Solo-Operator_

> Vor Sign-Off: TraceabilityMatrix-Statistik final reviewen; SprintPlan-Phase-Abschluss-Block ergänzen; ggf. APK signieren und an Beta-User verteilen (siehe Runbook §2.5).

---

*End of BattleTestPlan.md v1.0.*
