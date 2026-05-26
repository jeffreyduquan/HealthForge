# HealthForge — UsabilityMap

- **Version:** 0.1 (LOCKED, 2025-05-25)
- **Companion:** `ReqSpec.md` v0.2
- **Sprache:** de_DE

Diese Datei beschreibt das **UX-Gerüst**: Navigation, Screens, Aktionen, User-Flows. Keine Pixel-Designs (das kommt in `GUI.md`).

---

## 1. Navigation (LOCKED)

### 1.1 Bottom-Navigation (5 Tabs)

```
┌─────────────────────────────────────────┐
│   Home    Plan    Essen    Log    Profil│
└─────────────────────────────────────────┘
```

| Tab | Icon (lucide) | Zweck |
|-----|---------------|-------|
| **Home** | `home` | Heutiger Überblick, Quick-Add, Tagesdashboard |
| **Plan** | `calendar-days` | Mahlzeiten-Wochenplaner |
| **Essen** | `utensils` | Lebensmittel + Rezepte + Supplements (3 Sub-Tabs) |
| **Log** | `book-open` | Symptom-Tagebuch (Mood/Schlaf/Symptome) |
| **Profil** | `user` | Profil, Einstellungen, Gruppen, Export, Account |

### 1.2 Essen-Sub-Tabs (Top-Tabs)

```
┌──────────────────────────────────┐
│ Lebensmittel │ Rezepte │ Supplements │
└──────────────────────────────────┘
```

### 1.3 Tab-Verfügbarkeit nach Phase

| Tab | P1 (Foundation) | P2 (Recipes) | P3 (Community) | P4 (Power) |
|-----|-----------------|--------------|----------------|------------|
| Home | minimal | volle Funktion | + Plan-Links | unverändert |
| Plan | Placeholder "Bald verfügbar" | Placeholder | volle Funktion | + Auto-Planner |
| Essen → Lebensmittel | Browse/Filter/Detail | + Quick-Add | unverändert | unverändert |
| Essen → Rezepte | Placeholder | volle Funktion | + Gruppen-Filter | + Insights-Link |
| Essen → Supplements | Placeholder | volle Funktion | + Reminder | unverändert |
| Log | Placeholder | Placeholder | volle Funktion | + Insights-Tab |
| Profil | Auth/Account | + Profil-Felder | + Gruppen + Export | unverändert |

---

## 2. Onboarding-Wizard

Forward-only, 17 Steps. Skippable Steps mit Warnung markiert.

| # | Step | Inhalt | Skippable |
|---|------|--------|-----------|
| 1 | Welcome | Logo + 3 Bullet-Points zur App + "Los geht's"-Button | nein |
| 2 | Auth | Email, Passwort (×2), Invite-Code | nein |
| 3 | Email-Verify | "Wir haben dir einen Link geschickt" + Wait/Resend-Button | nein |
| 4 | Display-Name | Textfeld, optional anonymisierbar | ja (Default: "User-{n}") |
| 5 | Geburtsdatum | Date-Picker | ja (Default: 30 J) |
| 6 | Geschlecht | Radio: weiblich/männlich/divers/k.A. | ja |
| 7 | Größe | cm-Slider/Input | ja (Default: 170) |
| 8 | Gewicht | kg-Slider/Input | ja (Default: 70) |
| 9 | Aktivitätslevel | Radio: sitzend/leicht/moderat/aktiv/sehr aktiv | ja (Default: moderat) |
| 10 | Ziel | Radio: Erhalten / Abnehmen (-500 kcal) / Zunehmen (+500 kcal) / Custom-kcal | ja |
| 11 | Allergien | Multi-Select aus EU-14 + custom | ja ⚠️ |
| 12 | FODMAP | Toggles pro Saccharid (Fructose/Lactose/Sorbit/Mannit/GOS/Fructan) | ja ⚠️ |
| 13 | Histamin | Toggle "histaminsensibel" + max-SIGHI-Slider | ja |
| 14 | Mahlzeit-Slots | Checkbox: Frühstück/Mittag/Abend/Snacks | ja |
| 15 | Max-Prep-Time | Minuten-Slider (Default 45) | ja |
| 16 | Ziel-Review | Berechnete kcal/Makros zeigen, override-Möglichkeit | ja |
| 17 | Done | "Fertig! Du kannst alles im Profil ändern." + Confetti | nein |

**Nav:** Zurück-Pfeil + Skip-Button (für skippable Steps) + Weiter-Button. Progress-Bar oben.

---

## 3. Home-Tab

### 3.1 Layout (vertikal, scrollbar)

```
┌─────────────────────────────────┐
│ ← Mo, 25.05.2026 →              │ Datum-Nav
├─────────────────────────────────┤
│  ⭕ kcal  ⭕ P  ⭕ F  ⭕ C        │ Makro-Ringe
├─────────────────────────────────┤
│ 💧 Wasser: 1.2 / 2.0 L          │
│  [+250 ml] [+500 ml] [Custom]   │
├─────────────────────────────────┤
│ 💊 Supplements heute            │
│  ☐ Vitamin D  ☑ Magnesium       │
├─────────────────────────────────┤
│ Heute gegessen (3)              │
│  • Müsli 80 g — 350 kcal        │
│  • ...                          │
│  [Alle anzeigen → Verlauf]      │
├─────────────────────────────────┤
│ Schnell hinzufügen              │
│  [Apfel] [Quark] [Linsensuppe]  │ Letzte 6 Refs
├─────────────────────────────────┤
│         [+ Eintrag]              │ Großer Button
└─────────────────────────────────┘
```

### 3.2 Aktionen
- **Datum-Nav:** Pfeile + Tap auf Datum → Date-Picker
- **Ring tippen:** Detail-Sheet (geplant vs. tatsächlich, Restmenge)
- **Wasser +:** sofortiges Speichern, Snackbar mit Undo
- **Supplement-Checkbox:** Eintrag mit `now()` ins Intake-Log
- **Eintrag in Heute-Liste lang-tap:** Edit/Delete
- **Quick-Add-Chip:** öffnet Mengen-Dialog → Speichern
- **+ Eintrag (FAB-Ersatz):** öffnet Bottom-Sheet-Picker (Lebensmittel/Rezept/Supplement/Wasser)
- **Verlauf-Link:** öffnet Intake-History-Screen (eigener Screen, voller chronologischer Verlauf mit Date-Picker, Aggregat-Header, Edit/Delete)

---

## 4. Plan-Tab

### 4.1 Layout (Tages-Liste, vertikal scrollend)

```
┌─────────────────────────────────┐
│ ← Woche 21 (18.–24.05.) →  ⋮    │ Wochen-Nav + Menü
├─────────────────────────────────┤
│ Mo, 18.05.                      │
│  Frühstück  [Müsli + Beeren] ⋮  │
│  Mittag     [+ Hinzufügen]      │
│  Abend      [Linsen-Curry]   ⋮  │
│  Snack      [—]                 │
├─────────────────────────────────┤
│ Di, 19.05. ...                  │
└─────────────────────────────────┘
                            [FAB +]
```

### 4.2 Aktionen
- **FAB +:** öffnet Slot-Picker (Tag + Slot wählen) → Rezept/Lebensmittel-Picker
- **Tap auf leeren Slot:** Picker direkt für diesen Slot
- **Tap auf gefüllten Slot:** Detail-View (zeigt Rezept, Portionsanzahl-Slider)
- **Lang-Tap auf Slot:** Kontextmenü (Verschieben / Löschen / Habe gegessen)
- **Slot ⋮-Icon:** gleiches Menü
- **Wochen-Nav:** Pfeile + Tap auf Wochen-Header → Wochen-Picker
- **Header-Menü (⋮):**
  - "Plan generieren" (Auto-Planner, P4)
  - "Einkaufsliste erstellen"
  - "Plan kopieren auf nächste Woche"
  - "Plan zurücksetzen"
- **"Habe gegessen"-Inline-Button** je Slot (kleiner ✓-Button): kopiert Slot in Intake-Log mit Default-Portion

### 4.3 Auto-Planner-Dialog (P4)
1. "Welche Slots planen?" (Checkboxen: Frühstück/Mittag/Abend/Snack)
2. "Welche Tage?" (Tages-Multi-Select)
3. "Bestehende Einträge überschreiben?" (Toggle)
4. "Strict-Mode aktiv?" (zeigt Toggle-Status aus Profil)
5. "Plan generieren" → Loading → Vorschau → "Übernehmen / Verwerfen"

### 4.4 Einkaufsliste-Screen
- Aus Plan-Tab-Header oder Profil erreichbar
- Aggregierte Items, unit-normalisiert, aisle-grouped (Obst/Gemüse, Milch, ...)
- Check-Off persistent
- Datumsbereich-Auswahl (Default: aktuelle Woche)
- Share-Button (Plaintext-Export)

---

## 5. Essen-Tab

### 5.1 Top-Sub-Tabs

```
┌──────────────────────────────────┐
│ Lebensmittel │ Rezepte │ Supplements │
├──────────────────────────────────┤
│ 🔍 Suchen...                ⚙️ Filter│
└──────────────────────────────────┘
```

### 5.2 Lebensmittel-Sub-Tab

**Layout:** Suchleiste + Filter-Icon + Liste (LazyColumn).

**Listen-Item:**
```
┌─────────────────────────────────┐
│ [Bild] Müsli Knusper            │
│        Marke: Kölln · 380 kcal  │
│        ✓ verified · 0 Allergene │
└─────────────────────────────────┘
```

**Filter-Sheet:**
- Suchtext (oben fix)
- Allergene ausschließen (Multi-Select-Chips, prefilled aus Profil)
- Histamin-Score max (Slider 0–3)
- FODMAP-Levels (Multi-Toggle pro Saccharid)
- Marken-Toggle (Generisch vs. Marke)
- Strict-Mode-Toggle
- "Zurücksetzen" + "Anwenden"

**FAB:** (keine — Barcode-Scanner ENTFERNT)

**Empty-State:** Keine Treffer? → "Lebensmittel vorschlagen" → User-Submission-Form (P4)

### 5.3 Lebensmittel-Detail

```
┌─────────────────────────────────┐
│ [Großes Bild]                   │
├─────────────────────────────────┤
│ Müsli Knusper                   │
│ Kölln  ·  ✓ verified  ·  BLS    │ Badge-Row
├─────────────────────────────────┤
│ Nährwerte pro 100g              │
│  kcal 380  P 8  F 6  C 70       │
│  Ballast 5  Zucker 18  Salz 0.1 │
├─────────────────────────────────┤
│ Allergene                       │
│  ⚠️ Enthält: Gluten             │
│  ✓ Frei von: Milch, Eier        │
│  ? Unbekannt: Sellerie          │
├─────────────────────────────────┤
│ Histamin: 1/3 (SIGHI)           │
├─────────────────────────────────┤
│ FODMAP                          │
│  Fructose: low                  │
│  Lactose: low  ...              │
├─────────────────────────────────┤
│ Quelle: BLS 2023 + Admin        │
│  [Feld korrigieren] (P4)        │
└─────────────────────────────────┘
        [+ Zum Tagebuch]
```

**Aktionen:** [+ Zum Tagebuch] → Mengen-Dialog → Intake-Log. Bei Feld-Korrektur (P4): wähle Feld → neuer Wert + Begründung → Submit zu PR-Queue.

### 5.4 Rezepte-Sub-Tab

**Layout:** Suchleiste + Filter + Liste (Cards).

**Card:**
```
┌─────────────────────────────────┐
│ [Bild 16:9]                     │
│                                 │
├─────────────────────────────────┤
│ Linsen-Curry  ❤ 142  👍 89%     │
│ von @anna · 30 min · vegan      │
│ ✓ verified · 0 Allergene        │
└─────────────────────────────────┘
```

**Filter:**
- Allergene ausschließen (aus Profil)
- Histamin-Slider
- FODMAP-Level
- Kategorie (Frühstück/Mittag/Abend/Snack — Multi)
- Zubereitungszeit max (Slider)
- Suchtext
- "Nur meine Sammlung" Toggle (gelikte/empfohlene)
- "Nur eigene" Toggle
- "Aus Gruppe" Picker (P3)
- Strict-Mode

**FAB:** `+` → Rezept erstellen

### 5.5 Rezept-Detail

```
┌─────────────────────────────────┐
│ [Großes Bild]                   │
├─────────────────────────────────┤
│ Linsen-Curry                    │
│ von @anna · Gruppe: Vegan       │
│ ❤ 142 empfohlen · 30 min · 4 P. │
│ ✓ alle Zutaten verified         │
├─────────────────────────────────┤
│ [👍 Empfehle ich] [👎] [↑ häufiger] [↓ vertrage nicht] │
├─────────────────────────────────┤
│ Portionen: [−] 4 [+]            │
├─────────────────────────────────┤
│ ⚠️ Enthält für dich: Sellerie   │ User-Warning
├─────────────────────────────────┤
│ Aggregat-Nährwerte (4 Port.)    │
│  pro Portion: 420 kcal · ...    │
│  Histamin: 1/3 · FODMAP: low    │
├─────────────────────────────────┤
│ Zutaten (klickbar)              │
│  • 200 g Linsen                 │
│  • 1 Zwiebel                    │
│  ...                            │
├─────────────────────────────────┤
│ Zubereitung                     │
│  1. Linsen waschen ...          │
│  2. ...                         │
├─────────────────────────────────┤
│ [+ Als Mahlzeit buchen]         │
│ [Bearbeiten] (nur Owner)        │
│ [Melden]                        │
└─────────────────────────────────┘
```

### 5.6 Rezept-Erstellen-Form
- Step 1: Basis (Titel, Bild, Beschreibung, Kategorien, Portionen, Prep-Time)
- Step 2: Sichtbarkeit (Public/Private/Gruppe-Picker)
- Step 3: Zutaten (Search-Picker, Menge, Einheit, optional-Flag)
- Step 4: Schritte (Reorderable List, Text per Schritt, optional Bild)
- Step 5: Vorschau + Speichern

### 5.7 Supplements-Sub-Tab

**Liste:** Eigene Supplements + Server-Supplements (Toggle "Nur eigene")

**Card:**
```
│ [Icon] Vitamin D 1000 IE         │
│        täglich · lokal · ⏰ 08:00 │
```

**FAB +:** Supplement anlegen (Form: Name, Bild, Dosis, Nährwerte, Mikronährstoffe, Reminder-Schedule).

**Supplement-Detail:**
- Felder-Anzeige
- Reminder bearbeiten
- "Vorschlag an Server senden" (für lokale, noch nicht eingereichte) → Submit zu Peer-Review (P4)
- "Jetzt eingenommen"-Button → Intake-Log
- Historie der Einnahmen

---

## 6. Log-Tab (Symptom-Tagebuch)

### 6.1 Layout

```
┌─────────────────────────────────┐
│ Heute, 25.05.                   │
│ ┌─────────────────────────────┐ │
│ │ Mood: ○──────●──── 7/10     │ │
│ │ Schlaf: 4/5 · 7.5 h         │ │
│ │ Symptome:                   │ │
│ │  [Kopfschmerz·3] [Bauch·2]  │ │
│ │  [+ Symptom]                │ │
│ │ Tags: [Periode] [+]         │ │
│ │ Notiz: ...                  │ │
│ │ [Speichern]                 │ │
│ └─────────────────────────────┘ │
├─────────────────────────────────┤
│ Verlauf                         │
│  24.05. Mood 5 · 2 Symptome     │
│  23.05. Mood 8 · 0 Symptome     │
│  ...                            │
└─────────────────────────────────┘
                       [Charts ⤴]
```

### 6.2 Aktionen
- **Mood-Slider:** Live-Anzeige, 1–10
- **Schlafqualität:** Sterne 1–5 + Stunden-Input
- **Symptom-Chips:** Tap auf "+ Symptom" → Picker mit Liste + "Eigenes Symptom hinzufügen"-Button. Pro selektiertem Symptom: Severity-Slider 1–5.
- **Tags:** Multi-Select + Free-Add
- **Notiz:** Multiline
- **Speichern:** schreibt Eintrag in Room
- **Mehrere Einträge pro Tag:** "+ Weiterer Eintrag heute"-Button unter dem Tages-Block
- **Verlaufs-Eintrag-Tap:** Edit-Modus (nur innerhalb 7 Tage)
- **Charts ⤴:** öffnet Charts-Screen (Mood-Linie, Symptom-Heatmap pro Woche/Monat, Insights-Link zu P4-Bayesian-Insights)

---

## 7. Profil-Tab

### 7.1 Layout (Sektionsliste)

```
┌─────────────────────────────────┐
│ 👤 anna_h                       │
│ anna@example.com                │
├─────────────────────────────────┤
│ ▸ Account                       │
│ ▸ Profil & Körperdaten          │
│ ▸ Allergien & Unverträglichk.   │
│ ▸ Nährwert-Ziele                │
│ ▸ Supplements                   │
│ ▸ Meine Gruppen          (P3)   │
│ ▸ Reminders & Benachricht.(P3) │
│ ▸ Erscheinungsbild              │
│ ▸ Daten exportieren      (P3)   │
│ ▸ Onboarding wiederholen        │
│ ▸ Über / Lizenzen               │
│ ▸ Daten & Account löschen       │
└─────────────────────────────────┘
```

### 7.2 Unter-Screens

**Account:** Email anzeigen, Passwort ändern, Logout.

**Profil & Körperdaten:** Display-Name, Geburtsdatum, Geschlecht, Größe, Gewicht, Aktivitätslevel, Ziel.

**Allergien & Unverträglichkeiten:** Multi-Select EU-14 + custom, FODMAP-Toggles, Histamin-Slider, Strict-Mode-Toggle (global).

**Nährwert-Ziele:** Berechnete Defaults (kcal/P/F/C) anzeigen, jeweils Override-Input. Reset-Button.

**Supplements:** Liste eigener Supplements (Link zum Essen → Supplements-Tab).

**Meine Gruppen (P3):**
```
[+ Gruppe erstellen]  [Beitreten via Code]
─────────────────────────────────
Vegan (privat) · 23 Mitglieder
Low-FODMAP (öffentlich) · 1.2k
─────────────────────────────────
[Öffentliche Gruppen entdecken →]
```
- Tap auf Gruppe → Gruppen-Detail (Mitglieder-Liste, Gruppen-Rezepte-Filter-Link, beitreten/verlassen, Owner-Aktionen).
- Discover-Screen: Suchleiste, Themen-Chips, Liste öffentlicher Gruppen mit Beitreten-Button.

**Reminders (P3):** Meal-Slot-Reminder-Zeiten, Supplement-Reminder-Übersicht, Wasser-Reminder. (FCM entfernt 2026-05-25.)

**Erscheinungsbild:** Theme (System/Light/Dark), Schriftgröße.

**Daten exportieren (P3):** Button "PDF exportieren" + "JSON exportieren" → Share-Sheet.

**Onboarding wiederholen:** Confirm-Dialog → relaunch Wizard mit prefilled Werten.

**Daten & Account löschen:** zweistufige Bestätigung (Passwort-Eingabe) → DELETE /users/me + lokale Daten wipen + Logout.

---

## 8. Modale Flows

### 8.1 Mengen-Dialog (Add to Intake-Log)

```
┌─────────────────────────────────┐
│ Müsli Knusper                   │
│ Menge: [80] g                   │
│ Zeitpunkt: [jetzt ▾]            │
│ → kcal 304 · P 6 · F 5 · C 56   │ Live-Preview
│           [Abbrechen] [Speichern]│
└─────────────────────────────────┘
```

### 8.2 Picker (Rezept/Lebensmittel/Supplement)

Bottom-Sheet mit Top-Toggle (Lebensmittel / Rezept / Supplement) + Suchleiste + Liste. Tap → Mengen-Dialog.

### 8.3 Snackbars / Toasts
- Speicher-Bestätigung: "Eintrag gespeichert · [Rückgängig]"
- Fehler-Banner: "Keine Verbindung — wird beim nächsten Sync gesendet" (für Write-Ops im Offline-Fallback in P3+)

---

## 9. Admin-Web-UI (Sketch)

### 9.1 Layout

```
┌────────────────────────────────────────────────┐
│ HealthForge Admin       anna_admin ▾   Logout  │
├────────────┬───────────────────────────────────┤
│ Dashboard  │                                   │
│ Lebensmit. │      Content-Area                  │
│  ├ Queue   │                                   │
│  ├ Editor  │                                   │
│  └ Field-PR│                                   │
│ Supplements│                                   │
│  └ Queue   │                                   │
│ Rezepte    │                                   │
│  └ Reports │                                   │
│ User       │                                   │
│ Invites    │                                   │
│ Jobs       │                                   │
│  └ OFF-Sync│                                   │
│ Audit-Log  │                                   │
│ Statistiken│                                   │
└────────────┴───────────────────────────────────┘
```

### 9.2 Seiten

| Seite | Inhalt |
|-------|--------|
| **Dashboard** | KPI-Cards: User-Count, Lebensmittel total, Pending-Queues (3), DB-Größe, Phase-Status |
| **Lebensmittel → Queue** | Tabelle mit pending Submissions, Spalten: Name, Submitter, Datum, Aktion (Approve/Reject/Edit) |
| **Lebensmittel → Editor** | Volltext-Suche + Inline-Edit aller Felder (Nährwerte/Allergene/Histamin/FODMAP/Bild) |
| **Lebensmittel → Field-PR** | Tabelle pending Field-Updates: Lebensmittel, Feld, alt → neu, Submitter, Begründung, Aktion |
| **Supplements → Queue** | analog Lebensmittel-Queue |
| **Rezepte → Reports** | Gemeldete Rezepte, Grund, Reporter, Aktion (Approve report → Recipe verbergen/löschen, oder Reject) |
| **User** | Suche, Filter (active/banned/admin), Detail-Drawer: Account-Info, Aktionen (Ban/Unban/Delete/Admin-Toggle) |
| **Invites** | Liste, "Neuer Code"-Button (mit n-uses + expires), Deaktivieren |
| **Jobs → OFF-Sync** | Last-Run-Status, Trigger-Button, Log-Tail |
| **Audit-Log** | Tabelle: Zeitstempel, Admin, Aktion, Target, Diff |
| **Statistiken** | Charts: User-Wachstum, DB-Wachstum, Top-Rezepte, Top-Gruppen |

### 9.3 Auth
- Eigener Login-Screen (Email/Pwd)
- Backend prüft `users.role = ADMIN`
- Session via JWT (gleicher Endpoint wie App), Cookie für Browser (`SameSite=Strict`, `Secure`, `HttpOnly`)

---

## 10. Theming

### 10.1 Modus
- Light + Dark + System-Default (Default = System).
- Pro-User in Profil änderbar.

### 10.2 Design-Sprache
- **Clean, modern** — weniger "Space/Tech"-Feeling als Histamind.
- Keine schwarzen Hintergründe für Light-Mode, kein extremes Dark-Black-Theme.
- Subtile Akzent-Farbe (vorgeschlagen: warmes Grün `#2E7D5B` für "gesund" — final in `GUI.md`).
- Icon-Library: **lucide** (sparsam), Material-Symbols als Fallback.
- Typography: System-Default (Roboto) + ggf. Inter für Headings.

Details (Farb-Tokens, Spacing, Komponenten-Library) → `GUI.md`.

---

## 11. Placeholder-Screens (Dev-Phasen)

Für noch nicht implementierte Tabs:

```
┌─────────────────────────────────┐
│                                 │
│         📅                       │ Tab-Icon, groß
│                                 │
│   Bald verfügbar                │
│                                 │
│   Der Mahlzeiten-Planer kommt   │
│   in einem kommenden Update.    │
│                                 │
└─────────────────────────────────┘
```

---

## 12. Empty-States (kuratiert)

| Screen | Empty-State-Text |
|--------|------------------|
| Home (Tag ohne Einträge) | "Heute noch nichts geloggt. Tippe + um zu starten." |
| Essen → Rezepte (Filter killt alle) | "Keine Rezepte passen zu deinen Filtern. Filter anpassen?" |
| Suche keine Treffer | "Nichts gefunden. Lebensmittel vorschlagen?" |
| Plan (leerer Tag) | "Keine Mahlzeit geplant. + zum Hinzufügen." |
| Log (kein Eintrag) | "Tagebuch leer. Wie geht's dir heute?" |
| Meine Gruppen (P3) | "Du bist in keiner Gruppe. Beitreten oder erstellen?" |

---

## 13. Loading & Error States

- **Skeleton-Loader** für Listen (animierte Boxen statt Spinner).
- **Inline-Retry** bei Netzwerkfehler ("Erneut versuchen"-Button im Fehler-Banner).
- **Offline-Banner** oben am Screen (orange, persistent) wenn keine Verbindung — Write-Ops disabled mit Tooltip "Online erforderlich".

---

## 14. Accessibility

- Mindest-Touchtarget 48 dp.
- Alle interaktiven Elemente mit `contentDescription`.
- TalkBack-getestet vor Release.
- Schriftgrößen-Skalierung respektieren (`sp`).
- Kontrast ≥ WCAG AA.

---

*End of UsabilityMap.md v0.1 — LOCKED. Details zu Tokens/Farben/Components in `GUI.md`.*
