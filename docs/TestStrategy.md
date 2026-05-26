# HealthForge — Test Strategy

- **Version:** 1.0 (2026-05-26)
- **Companion-Docs:** [BattleTestPlan.md](BattleTestPlan.md), [TraceabilityMatrix.md](TraceabilityMatrix.md), [UsabilityMap.md](UsabilityMap.md), [ReqSpec.md](ReqSpec.md)
- **Sprache:** de_DE
- **Scope:** v1.0 Release + Post-v1.0 Maintenance

---

## 1. Ziel

Diese Strategie definiert **wie** HealthForge getestet wird. Das **was** (konkrete
Testcases mit Pass/Fail-Tracking) lebt in [BattleTestPlan.md](BattleTestPlan.md).

HealthForge ist eine **Solo-betriebene Beta-App** für eine kleine, eingeladene
Nutzerschaft. Daraus folgen drei Konsequenzen, die alle weiteren Test-Entscheidungen
bestimmen:

1. **Manuell schlägt automatisiert.** Keine CI-getriebene Selenium/Espresso-Suite — die
   Setup-Kosten amortisieren sich bei 1 Entwickler + ≤50 Beta-Usern nicht. Stattdessen
   strukturierte manuelle Durchläufe mit dokumentiertem Pass/Fail.
2. **REQ-IDs sind die Wahrheit.** Jeder Testcase verlinkt auf mindestens eine REQ-ID
   aus [ReqSpec.md](ReqSpec.md). Coverage = Anteil der ✅-REQ-IDs aus
   [TraceabilityMatrix.md](TraceabilityMatrix.md) mit mindestens einem grünen Run im
   BattleTestPlan.
3. **Usability ist gleichberechtigt.** Pro Screen wird zusätzlich zur Funktion auch der
   UX-Smell geprüft (anhand [UsabilityMap.md](UsabilityMap.md) §1–§14).

---

## 2. Test-Pyramide (invertiert)

Die klassische Test-Pyramide (viele Unit-Tests, wenige E2E) wird hier bewusst
**invertiert**:

```
              ┌─────────────────────┐
              │  Manual Persona-E2E │   Hauptanteil
              │  (Marie-Journey)    │
              └─────────────────────┘
            ┌─────────────────────────┐
            │  Manual REQ-driven      │   Tiefe
            │  Surface-Screen-Tests   │
            └─────────────────────────┘
          ┌─────────────────────────────┐
          │  Manual Negative & Security │   Härte
          └─────────────────────────────┘
        ┌─────────────────────────────────┐
        │  Smoke Build/Compile (Gradle)   │   Sicherheitsnetz
        └─────────────────────────────────┘
```

**Begründung:**
- E2E manuell deckt die größte Risiko-Klasse (Cross-Surface-Kontrakte: Admin approved
  Field-PR → Android sieht Effekt nach Sync) am direktesten ab.
- REQ-driven Screen-Tests fangen Regressions an der UI-Logik.
- Negative/Security härtet vor Beta-Launch.
- Compile-Checks (`./gradlew :app:assembleDebug`, `./gradlew :server:bootJar`,
  `npm run build`) bleiben das einzige automatische Gate.

---

## 3. Test-Surfaces

| Surface | Zugriff | Tooling |
|---|---|---|
| Android-App | Emulator (Pixel 7 API 35) — Real-Device optional Post-v1.0 | Android Studio + `adb shell uiautomator dump` für Layout-Audit |
| Server-API | `http://localhost:8080/v1/*` + `http://localhost:8080/admin/v1/*` | HTTPie / curl / Postman; DB-Inspect via `psql` an Docker-Postgres |
| Admin-UI | `http://localhost:5173/` (Vite-Dev) | Browser (Chrome DevTools) |

**Multi-Device-Matrix** ist explizit out-of-scope für v1.0 (Solo-Beta). Real-Device-Pass
findet einmalig Post-v1.0 vor Beta-Verteilung statt.

---

## 4. Test-Methodik

### 4.1 Hybrid REQ + Usability

Jeder Screen-Testcase im BattleTestPlan hat zwei Spalten:

- **REQ-Spalte:** Liste der REQ-IDs, die der Case validiert (Pass/Fail bindet an die
  Matrix).
- **Usability-Spalte:** Anker an [UsabilityMap.md](UsabilityMap.md) (z.B. „§3.1
  Home-Layout") + 1-Satz-Heuristik („Macro-Ringe live nach Quick-Add?", „Datums-Nav
  Vor/Zurück 1-Tap?").

Beide müssen grün sein, damit der Case grün ist. Ein funktional korrekter Screen mit
broken UX bleibt rot.

### 4.2 Persona-Smoke vor Tiefe

Vor dem REQ-Drill wird die **Marie-Journey** als E2E-Smoke durchlaufen
(BattleTestPlan §1). Wenn die Smoke kippt, lohnt der Tiefen-Pass nicht — erst Smoke
fixen.

Marie-Journey = exemplarischer 7-Tage-Flow:
1. Onboarding (alle 14 Steps mit realistischer Eingabe)
2. 3 Tage Quick-Logging (Home → Quick-Add)
3. 1 Wochenplan füllen (Plan-Tab manuell oder Auto-Planner)
4. Einkaufsliste generieren + abhaken
5. Symptom-Log 3× füllen
6. Supplement-Reminder anlegen, Tick beobachten
7. Daten-Export (PDF + JSON) ausführen + verifizieren

### 4.3 Negative & Security

Nach Persona + REQ folgt ein gezielter Negative-Pass (BattleTestPlan §5):

- Auth-Bypass (JWT manipuliert, abgelaufen, Cross-Account)
- SQL-Injection in Ingredient-Search (`%' OR 1=1--`)
- Concurrent-Edits (zwei Sessions auf dasselbe Profil)
- Offline-Mode-Resilienz (Airplane-Mode → Aktionen → Re-Connect)
- Token-Expiry (15min Access ablaufen lassen, prüfen Refresh-Flow)
- Admin-Privilege-Escalation (normaler User pingt `/admin/v1/*` → 403)
- Data-Export-Privacy (Export anderer User darf nicht zugreifbar sein)

---

## 5. Defekt-Klassifikation

| Severity | Definition | Reaktion |
|---|---|---|
| **S1 (Blocker)** | Crash, Datenverlust, Auth-Bypass, fehlerhafte Allergen-Warnung | Pre-Beta fix-blockierend; eintragen in BattleTestPlan Failures-Log + sofort beheben |
| **S2 (Major)** | Funktion broken aber Workaround vorhanden; UX inkonsistent zu UsabilityMap | Fix vor Beta-Verteilung; im Failures-Log mit TODO |
| **S3 (Minor)** | Kosmetik, Tippfehler, Edge-Case-Glitch | Backlog (Post-v1.0); im Failures-Log, kein Blocker |
| **S4 (Wish)** | Idee/Verbesserung außerhalb Spec | NICHT im BattleTestPlan — als Issue gegen ReqSpec/UsabilityMap notieren |

---

## 6. Run-Kadenz

**Single-Run-Then-Fixes** (gewählt am 2026-05-26):

1. **Run 1 (Full Sweep):** Kompletter BattleTestPlan §1–§5 durchspielen, Pass/Fail
   inline eintragen, Fails ins Failures-Log §6.
2. **Fix-Phase:** S1+S2 beheben; pro Fix Commit-Referenz im Failures-Log.
3. **Run 2 (Re-Run, fokussiert):** Nur die zuvor roten Cases erneut. Wiederholen bis
   0 S1+S2-Fails.
4. **Sign-Off:** BattleTestPlan §7 (Sign-Off-Block) wird datiert + signiert.

Wöchentliche Voll-Durchläufe sind **nicht** vorgesehen (Solo-Overhead). Vor jeder
Release-APK-Verteilung (Post-v1.0-Updates) wird mindestens die Persona-Smoke (§1)
+ Negative (§5) erneut gefahren.

---

## 7. Verifikations-Tools

| Tool | Zweck |
|---|---|
| `adb shell uiautomator dump` | Layout-Tree-Snapshot zum Vergleich mit UsabilityMap-Wireframes |
| `adb logcat -s HealthForge:*` | Crash-Stacks, Reminder-Fire-Logs |
| `adb shell dumpsys alarm` | Geplante AlarmManager-Einträge prüfen (Supplement + Water) |
| `adb shell dumpsys notification` | Notification-Channel-Status (Importance, Visibility) |
| `psql healthforge` | DB-State direkt nach Action prüfen (`SELECT * FROM intake_entries ORDER BY id DESC LIMIT 5;`) |
| HTTPie | API-Calls mit Headers (`http :8080/v1/me Authorization:"Bearer …"`) |
| Browser DevTools | Admin-UI Netzwerk-Tab, Console für React-Warnings |

---

## 8. Was NICHT getestet wird (Out-of-Scope v1.0)

- Performance-Lasttests (>100 concurrent users) — Solo-Beta nicht relevant
- iOS, Web-Client — explizit out-of-scope per REQ-PLATFORM-001
- Barcode-Scanner — entfernt 2026-05-25 (REQ-BARCODE-001/002/003 🗑️)
- Push-Notifications via FCM — entfernt 2026-05-25 (REQ-REMIND-003 🗑️)
- A/B-Tests, Telemetrie — Privacy-First-Constraint
- Lokalisierung in EN/FR/… — nur de_DE per REQ-I18N-001
- Multi-Tenancy — Single-Instance per Architecture.md
- Automatisierte UI-Snapshots / Espresso / Selenium — bewusst manuell, siehe §2

---

## 9. Eskalation

Wenn während eines Runs eine S1 entdeckt wird, die nicht innerhalb derselben Session
fixbar ist:

1. BattleTestPlan §6 Failures-Log Eintrag mit voller Repro
2. SprintPlan §P4 Phase-Abschluss-Sektion: explizite Notiz „v1.0 BLOCKED bis S1-#nnn fixed"
3. Sign-Off-Block in BattleTestPlan §7 darf **nicht** datiert werden
4. Architecture.md prüfen ob Architektur-Change nötig → ggf. Doc-Drift-Eval auslösen

---

*End of TestStrategy.md v1.0.*
