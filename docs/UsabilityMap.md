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
| 1 | Welcome | Heading „Willkommen bei HealthForge" + kurzer Begrüßungstext (verschlüsselte Daten on-device) + „Weiter"-Button | nein |
| 2 | Auth | _separater Register-Screen vor Wizard_ — Einladungscode, Anzeigename, Email, Passwort (×2) | nein |
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

## 3. Home-Tab (P7-Refactor)

### 3.1 Layout (vertikal, scrollbar)

```
┌─────────────────────────────────┐
│ ← Mo, 25.05.2026 →              │ Datum-Nav
├─────────────────────────────────┤
│ ANGEHEFTETE NÄHRSTOFFE           │ SectionPill
│ ┌──────────────────────────────┐ │ PinnedNutrientCard
│ │ kcal      1450 / 2100  -650 │ │  (groesser, Linear-Bar + Delta)
│ │ █████████████░░░░░░░░ 70 % │ │
│ └──────────────────────────────┘ │
│  … weitere Pins (Protein/Carbs/Fett) …
│ ┌────────────────────────────┐ │ WaterStageSlider (letzte Pin-Zeile)
│ │ Wasser  ×2   700 / 2000 ml  35 %  ὑ4│ │  Label · Stufen-Badge · Wert/Ziel ·
│ │ ███████████▌▒▒▒▒▒▒▒▒▒▒▒▒▒●│ │  Prozent · Reminder-Bell
│ └────────────────────────────┘ │  Bar = Slider, Thumb = Position in Stufe
├─────────────────────────────────┤
│ [▾ Alle Nährstoffe anzeigen]    │ Expander
│  ◍ Vitamin C   65 / 110 mg    │ NutrientRow (Mini-Bar + Pin-Icon)
│  ◍ Eisen        8 / 15  mg    │   Pin-Tap toggelt sofort
│  …                                │
├─────────────────────────────────┤
│ GEPLANTE MAHLZEITEN HEUTE       │ SectionPill
│  ☐ 12:00 Linsensuppe (Plan)    │ PlannedMealRow
│  ☑ 08:00 Müsli (gegessen)      │ Undo-Snackbar 60s
│  …                                │
├─────────────────────────────────┤
│         [+ Eintrag]              │ Großer Button
└─────────────────────────────────┘
```

### 3.2 Aktionen
- **Datum-Nav:** Pfeile + Tap auf Datum → Date-Picker.
- **PinnedNutrientCard Tap:** Detail-Sheet (geplant vs. tatsächlich, Restmenge, Quellen-Aufschlüsselung pro Mahlzeit).
- **Pinned-Bars Stufen-Anzeige (P7.S3.b, einheitlich mit Wasser):** Alle Pin-Bars (kcal/Protein/Carbs/Fett/Wasser) zeigen den Konsum als Stufen-Bar. Stufe N = `N×goal..(N+1)×goal`. Bar-Füllung = Prozent **innerhalb der aktuellen Stufe** (0–100 %). Farbe = `waterStageGradient(stage)` (10-Stufen-Cycle, ab Stufe 9 endless). Track = Akzent der **Vorgängerstufe × 0.25 Alpha** (Stufe 0 → neutraler `barTrack`). Ab Stufe ≥ 1 erscheint rechts ein Lv-Badge. Überkonsum (> 100 % Tagesziel) führt zu Stufen-Roll-over mit neuer Farbe — analog Wasser.
- **WaterStageSlider drag (Stufen-Logik, v2.3):** Wasser ist die letzte Zeile in der `PinnedNutrientCard`, optisch identisch zu den anderen Pin-Bars. Range = 0..goal (0–100 % der aktuellen Stufe). 50-ml-Steps. Stufe N umfasst `N×goal..(N+1)×goal`. **In-Drag Stage-Up**: erreicht der Slider 100 %, schaltet die Bar in die nächste Stufe (Bar 0 %, neue Farbe, Thumb am linken Rand). **In-Drag Stage-Down (Drag-Through-Zero)**: erreicht der Slider in einer Stufe > 0 das untere Ende, schaltet die Bar eine Stufe zurück (Bar 100 %, Thumb am rechten Rand). **Touch-Disconnect bei Stufenwechsel**: Sobald während eines Drags ein Stage-Up/Down ausgelöst wird, wird die aktive Geste per `key`-Remount des Sliders abgebrochen. Für weitere Stufenwechsel muss der User loslassen und neu tippen. Cascade-Effekt konstruktionsbedingt unmöglich. Beim Loslassen genau an einer Stufengrenze rückt der State zusätzlich noch eine Stufe weiter (oben) bzw. zurück (unten). Stufen 0..9 haben je eine eigene Farbe aus der Histamind-Palette; ab Stufe 10+ bleibt die Farbe gleich. Stufen sind endlos. **Ghost-Soll-Marker**: feine weiße vertikale Linie an der Soll-Position innerhalb der gerade angezeigten Stufe. **Defizit-Rotanteil**: Bereich zwischen aktueller Füllung und Soll wird rot (StatusOverUl) gefärbt, wenn current < Soll und beide in derselben Stufe. Persistenz via `WaterIntakeRepository.setDayTotal` (Day-Aggregate).
- **Reminder-Bell-Toggle:** Trailing-Icon der Wasser-Zeile (statt eigener Card-Header). Stoppt nur Defizit-Notifications. Persistiert in `WaterReminderPrefs`.
- **Expand „Alle Nährstoffe anzeigen“:** zeigt komplette Katalog-Liste (~30 Einträge) mit Pin-Toggle pro Zeile. Pin-Tap persistiert sofort in `UserProfileEntity.pinnedNutrientsJson` (kein Save-Button).
- **Geplante-Mahlzeiten-Checkbox:** ☑ → `intake_entries`-Insert mit Snapshot der Nutrient-Werte; ☐-Undo binnen 60 s per Snackbar.
- **+ Eintrag:** Bottom-Sheet-Picker (Lebensmittel/Rezept/Supplement — Wasser ist nicht mehr hier, weil im Home-Card direkt steuerbar).

### 3.3 Pin-Verwaltung
- **Default-Pins** (nach Onboarding): `kcal, protein, carbs, fat, water` (5 Stück).
- **Hinzufügen:** Expand „Alle Nährstoffe anzeigen“ → Pin-Icon-Tap auf Zeile.
- **Entfernen:** Pin-Icon-Tap auf Karte oder Zeile.
- **Mindest-Pin:** keine (auch 0 Pins erlaubt; Liste läuft über Expand).
- **Reihenfolge:** Insert-Reihenfolge in JSON-Array, kein Drag-Sort in P7 (Polish-Backlog).

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

**Nährwert-Ziele (Tagesziele — P7-Expand):** Liste **aller** Katalog-Nährstoffe (~30 Einträge: Makros + Vitamine + Mineralstoffe + Wasser). Pro Zeile: Default-Wert (read-only), Override-Input (NumberField), Reset-Icon. Override persistiert device-local in `UserProfileEntity.dailyNutrientGoalsJson` (Privacy-Boundary REQ-PROFILE-001/002). Reset löscht den Key. Wasser-Goal ist hier (nicht mehr in eigener Reminder-Section).

*Pin-Verwaltung* erfolgt im **Home-Tab** (P7, REQ-HOME-NUTRIENT-LIST-001) — Profil-Sektion „Angeheftete Nährstoffe“ (P6) ist entfernt.

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

---

## §X P6 Re-Spec — Screen-Patterns (eingefügt 2026-05-26, LOCKED via P6.S1)

**Note:** Die §§1–9 oben sind das ursprüngliche v0.1-LOCKED-Konzept. Ab P6.S4 werden Screens nach Hm-Patterns rebuilt. Master-Quelle: [HistamindDesignReference.md §6](HistamindDesignReference.md).

### X.1 Home (P6.S4, supersedes §3 Home)

Stack-Layout (bottom→top):
1. **AmbientBackdrop** (Layer-0, dark-only) — driftende Blobs in Violet/Cyan/Mint.
2. **TopBar** — Greeting (GradientText `headlineLarge` „Hallo, $name") + Datum (`bodyMedium` fgSecondary). Rechts: Pin-Mgmt-Icon (öffnet PinnedNutrientsManager-Sheet).
3. **SectionPill „HEUTE GEPLANT"** + GlassCard mit 3 Meal-Slot-Rows.
4. **SectionPill „ERNÄHRUNG"** + GlassCard:
   - 4 große Progress-Ringe (Pinned-Nutrients default kcal/Protein/Carbs/Fat) horizontal scrollbar.
   - Δ-Tag pro Ring („im Ziel" `statusGood` / „über" `statusOverUl` / „unter" `statusRelax`).
   - „Weitere anzeigen" Expand-Toggle → Linear-Mini-Progress pro weiterer Nährstoff.
5. **SectionPill „WASSER"** + GlassCard mit Quick-Add-Chips (250/500/750ml), Counter, Today-History; Long-Press auf Chip = Undo-Snackbar.
6. **SectionPill „LETZTE NOTIZEN"** + GlassCard mit Mini-Log-Liste (letzte 3 Events mit SeverityBar).
7. **GradientFab** unten-rechts → Add-Flow (siehe Pre-Selection-Mode).

### X.2 Onboarding (P6.S4, supersedes §2 Onboarding)

14-Step-Wizard, forward-only, kein Skip.

Layout pro Step:
- Step-Indikator oben (14 Punkte horizontal, aktiver Punkt animiert auf accentGradient).
- GradientText `headlineLarge` Titel.
- `bodyLarge` Erklärung (1–2 Zeilen).
- Input-Area:
  - Slider für Alter/Größe/Gewicht/Aktivität (siehe REQ-ONBOARD-SLIDER-001).
  - Chip-Multi-Select für Allergene/Intoleranzen/Diäten.
  - SegmentedTabs für binary Choices (z.B. Geschlecht).
- Bottom-Bar: Links „Zurück" (Outlined-Button, ab Step>0), rechts „Weiter" (GradientButton).
- Step 14: Summary + „Los geht's" (GradientButton, ganz breit).

### X.3 Plan (P6.S5, supersedes §4 Plan)

- Day-Strip oben: 7 Glass-Chips (Mo–So), heute mit accentGradient-Pill-Background.
- Pro Tag: 4 Mini-GlassCards (Frühstück/Mittag/Abend/Snack), jede mit:
  - Slot-Titel (`labelLarge`).
  - Liste der Items (Rezept- oder Lebensmittel-Refs).
  - Add-Button → Plan-Add-Sheet.
- **Plan-Add-Sheet:**
  - Sheet-Titel: „Rezept oder Lebensmittel" (Wording-Lock F-008).
  - SegmentedTabs „Rezepte" / „Lebensmittel".
  - Liste vorgefüllt (50 Items alphabetisch).
  - Bei Auswahl: Mengenangabe + „Hinzufügen" (GradientButton).

### X.4 Essen / Lebensmittel (P6.S5, supersedes §5 Essen)

- Search-Bar sticky oben.
- Liste lazy-load 50 alphabetisch on-open (REQ-LIST-PRELOAD-001).
- Pro Item: GlassCard mit Name, Marke, Nährstoff-Zusammenfassung (4 Pinned-Werte als Mini-Chips).
- FAB → IngredientSuggest-Flow (bestehend, nur visuell adaptiert).
- **Pre-Selection-Mode** (REQ-INTAKE-ADD-FLOW-001): wenn nav-arg `preselect=true` gesetzt, FAB wird zu „Auswählen" und Tap auf Item liefert Result-Callback.

### X.5 Log (P6.S6, INVERSION — supersedes §6 Log)

**Konzept-Inversion:** Tagebuch (Mood+Schlaf+Symptom) → **Event-Log** (Symptom-Event mit Severity).

- SegmentedTabs „Einträge" / „Insights" oben.
- **Tab Einträge:**
  - Chronologische Liste (neueste zuerst).
  - Pro Event-Row: SeverityBar (4dp links, Farbe nach Severity) + Symptom-Tag-Chips + Zeit (`labelMedium`) + Notiz-Preview (`bodySmall`, max 2 Zeilen).
  - FAB → QuickEntrySheet.
- **QuickEntrySheet:**
  - Severity-Picker: 5 große Gradient-Chips „1 leicht" .. „5 stark".
  - Symptom-Tag-Chips (multi-select, Default-Liste konfigurierbar in Profil).
  - Notiz-Textfeld (multiline, optional).
  - Time-Picker (default „jetzt").
  - „Speichern" (GradientButton).
- **Tab Insights:**
  - 14-Tage-Bar-Chart (Severity-Summen pro Tag).
  - Top-3-Symptome (Bar-List mit Häufigkeit).
  - „Vollständiger Verlauf" Link → erweiterte Statistik (Future-Backlog).

### X.6 Profil (P6.S5, supersedes §7 Profil)

Scroll-Liste aus GlassCards:
- **Konto** — E-Mail, „Abmelden" (Outlined-Button).
- **Tagesziele** (REQ-PROFILE-GOALS-001) — per-Nutrient-Editor mit kcal/Protein/Carbs/Fat + „+ Nährstoff hinzufügen" für Mikronährstoffe.
- **Pinned Nutrients** — zeigt aktuelle Pins als Chips; Tap → PinnedNutrientsManager-Sheet.
- **Symptom-Tags** — Editor für Default-Tags im Log.
- **Erinnerungen** — Wasser-Alarm-Toggle mit Helper-Text (REQ-WATER-ALARM-HELPER-001).
- **Theme** — Light/Dark/System-Toggle.
- **Daten** — Exportieren (PDF/JSON) + Account löschen.
- **Info** — App-Version, Lizenzen.

### X.7 Auth-Screens (P6.S4, light-touch)

- Login/Register/EmailVerify/ForgotPassword: AmbientBackdrop + zentrale GlassCard mit Logo (GradientText „HealthForge") + Form + GradientButton.
- Keine separate Layout-Änderung gegenüber v0.1, nur visuell auf Hm-Tokens umgestellt.

**End of §X P6 Re-Spec UsabilityMap.**
