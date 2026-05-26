# HealthForge — Histamind Design Reference

**Version:** 1.0 (LOCKED 2026-05-26 als P6.S1 Spec-Lock)
**Status:** Master-Quelle für Design-System ab P6.S2.
**Vorgängerdokumente:** [GUI.md](GUI.md) (legacy v0.1, ab P6.S2 superseded für Color/Typography/Components).
**Externe Referenz:** https://github.com/endgeardev/Histamind (Flutter) — Designvorlage, nicht Code-Ziel.

> HealthForge bleibt Kotlin/Compose. Dieses Dokument portiert das **Histamind-Design-System** auf Compose-Idiome. Histamind-Code wird nicht 1:1 übernommen — der Look wird rekonstruiert.

---

## 1. Visual-Identity-Lock (P6.S2)

| Aspect | Wert |
|---|---|
| Theme-Primärmodus | Dark (Glas-Pfad) |
| Theme-Sekundärmodus | Light (Clean-Card-Pfad, gleicher Akzent, ohne Glas) |
| Akzent | Violet→Cyan Linear-Gradient (ersetzt Olive-Green komplett) |
| Schriftart | Manrope (Google Fonts, OFL) |
| Visual-Sprache | „DELTA-45 Premium Glass" (Histamind-Terminologie) |
| Fusion-Verhältnis | 70% Histamind-Visual / 30% HealthForge-Domain (Bottom-Nav-Struktur, Domain-spezifische Cards) |

---

## 2. Color Tokens (LOCKED, supersede GUI.md §2)

### 2.1 Dark Theme — Hero-Variante

| Token | Hex / Alpha | Verwendung |
|---|---|---|
| `background` | `#070A12` | Screen-Boden, ganz unten |
| `cardSurface` | `#141A26` @ 70% Alpha | Glas-Floor unter GlassCard-Gradient |
| `glassFillTop` | `#F5F7FA` @ 12% | Oberes Glass-Gradient-Stop |
| `glassFillBottom` | `#F5F7FA` @ 4% | Unteres Glass-Gradient-Stop |
| `glassBorder` | `#FFFFFF` @ 10% | 1dp Card-Border |
| `fgPrimary` | `#F5F7FA` | Haupt-Text, große Headlines |
| `fgSecondary` | `#F5F7FA` @ 80% | Body, Labels |
| `fgTertiary` | `#F5F7FA` @ 50% | Captions, Section-Pill-Label |
| `ambientViolet` | `#7C5CFF` | Gradient-Start, Primary in MaterialColorScheme |
| `ambientCyan` | `#4DD0E1` | Gradient-Ende, Secondary in MaterialColorScheme |
| `statusOverUl` | `#FF5470` | Über-Limit / Error |
| `statusRelax` | `#FFB454` | Warn / Amber |
| `statusGood` | `#22D3A6` | Ok / Within-Range |
| `violetGlow` | `#7C5CFF` @ 43% | Schatten-Tint unter Gradient-FAB/Button |

`accentGradient` = `Brush.linearGradient([ambientViolet, ambientCyan])` (45°, top-left → bottom-right).

### 2.2 Light Theme — Clean-Variante (reduzierte Fidelity)

| Token | Hex / Alpha |
|---|---|
| `background` | `#F4F5F8` |
| `cardSurface` | `#FFFFFF` (kein Alpha, keine Gradient-Fill) |
| `glassBorder` | `#1B1F26` @ 8% (1dp dünner Border) |
| `fgPrimary` | `#1B1F26` |
| `fgSecondary` | `#1B1F26` @ 70% |
| `fgTertiary` | `#1B1F26` @ 50% |
| `ambientViolet` | `#7C5CFF` (gleich) |
| `ambientCyan` | `#4DD0E1` (gleich) |
| `statusOverUl/Relax/Good` | gleich |

Im Light-Mode wird kein Glas-Effekt gerendert; AmbientBackdrop ist deaktiviert (statisches `background`).

---

## 3. Typography Tokens (LOCKED, supersede GUI.md §3 wenn vorhanden)

Schrift: **Manrope** via `androidx.compose.ui.text.googlefonts`.

| Style | Size | Weight | Letter-Spacing | Line-Height | Color | Use |
|---|---|---|---|---|---|---|
| `displayLarge` | 48sp | w700 | -0.8sp | 1.05 | fgPrimary | Hero-Splash |
| `headlineLarge` | 34sp | w700 | -0.5sp | 1.1 | fgPrimary | Screen-Title (Home Greeting) |
| `headlineMedium` | 26sp | w600 | -0.3sp | 1.15 | fgPrimary | Screen-Subtitle |
| `headlineSmall` | 20sp | w600 | -0.2sp | 1.2 | fgPrimary | Card-Title |
| `titleLarge` | 18sp | w600 | 0sp | 1.3 | fgPrimary | Section-Header |
| `titleMedium` | 15sp | w600 | 0sp | 1.3 | fgPrimary | List-Item-Title |
| `bodyLarge` | 16sp | w400 | 0sp | 1.5 | fgPrimary | Body-Text |
| `bodyMedium` | 14sp | w400 | 0sp | 1.45 | fgSecondary | Body-Helper |
| `bodySmall` | 13sp | w400 | 0sp | 1.4 | fgTertiary | Captions |
| `labelLarge` | 13sp | w600 | +0.3sp | 1.2 | fgPrimary | Button-Label |
| `labelMedium` | 12sp | w500 | +0.4sp | 1.2 | fgSecondary | Tab-Label |
| `labelSmall` | 11sp | w500 | +0.6sp UPPERCASE | 1.2 | fgTertiary | Section-Pill-Label, Meta |

**Tabular-Figures:** Zahlen-Anzeigen (Nutrition-Werte, Counter) bekommen `FontFeatureSetting("tnum")`.

---

## 4. Layout & Shape Tokens

| Token | Wert |
|---|---|
| `pagePadding` | 24dp horizontal, 24dp vertikal |
| `cardRadius` | 24dp |
| `buttonRadius` | 18dp |
| `chipRadius` | 14dp |
| `sectionPillStripeSize` | 3dp × 14dp |
| `sectionPillGap` | 8dp (zwischen Stripe und Label) |
| `iconStroke` | 1.5 (Lucide-Compose-Icons mit `strokeWidth=1.5dp`) |
| `cardShadowBlur` | 40dp |
| `cardShadowOffset` | (0, 16dp) |
| `cardShadowColor` | `#000000` @ 25% |
| `fabGlowBlur` | 24dp |
| `fabGlowOffset` | (0, 8dp) |
| `fabGlowColor` | `violetGlow` (`#7C5CFF` @ 43%) |

---

## 5. Component-Idiome (P6.S3-Scope)

### 5.1 GlassCard

```
Box(
  Modifier
    .shadow(40.dp, Shapes.large, ambientColor=Color.Black.copy(alpha=.25f))
    .clip(Shapes.large)
    .background(
      Brush.verticalGradient([glassFillTop, glassFillBottom])
    )
    .border(1.dp, glassBorder, Shapes.large)
    .padding(20.dp)
)
```

Im Light-Mode: `background = cardSurface` (solid), kein Gradient, kein Shadow.

### 5.2 SectionPill

`Row { Box(3×14dp, accentGradient) → 8dp Spacer → Text(label, labelSmall.copy(weight=w800, letterSpacing=1.4sp, color=fgTertiary, uppercase)) }`

### 5.3 GradientFab

```
FloatingActionButton(
  ...,
  containerColor = Color.Transparent,
  modifier = Modifier
    .shadow(24.dp, CircleShape, ambientColor=violetGlow)
    .background(accentGradient, CircleShape)
)
```

### 5.4 GradientButton

Filled-Button mit `accentGradient` als Background, `labelLarge` Text in `fgPrimary`, 18dp radius, 56dp Höhe.

### 5.5 AmbientBackdrop

Stack-Bottom-Layer (`Box(fillMaxSize)`); 3 animierte `Canvas`-Blobs mit radialGradients in `ambientViolet/Cyan/statusGood`@ ~15%, drift via `InfiniteTransition` mit 30–60s Period. Im Light-Mode deaktiviert.

### 5.6 GradientText

Text-Composable mit `Modifier.drawWithCache { onDrawWithContent { drawContent(); drawRect(accentGradient, blendMode=BlendMode.SrcAtop) } }`.

### 5.7 SegmentedTabs

Custom Two-Tab-Toggle, Glass-Pill-Background, aktive Tab mit `accentGradient`-Fill.

### 5.8 SeverityBar (Log)

`Box(4dp × 56dp, color = severityColor)` links neben Log-Entry.
Mapping: Severity 1 → statusGood, 2 → statusGood @ 80%, 3 → statusRelax, 4 → statusOverUl @ 80%, 5 → statusOverUl.

---

## 6. Screen-Patterns (P6.S4 / S5 / S6-Scope)

### 6.1 Home

- AmbientBackdrop layer-0.
- `headlineLarge` Greeting (GradientText) + `bodyMedium` Datum.
- SectionPill „HEUTE GEPLANT" + GlassCard mit nächsten 3 Mahlzeit-Slots.
- SectionPill „ERNÄHRUNG" + GlassCard mit **Pinned-Nutrients** (default 4: kcal/Protein/Carbs/Fat, jeder mit Progress-Ring + Δ-Tag „im Ziel"/„über") + collapsibel „Weitere anzeigen" → Rest mit Mini-Linear-Progress.
- SectionPill „WASSER" + GlassCard mit Quick-Add-Chips (250/500/750ml) + Verlauf (heutige Adds) + Long-Press auf letzten Chip = Undo.
- SectionPill „NOTIZEN" + GlassCard mit Mini-Log-Event-Liste (letzte 3).

### 6.2 Onboarding

- 14 Steps, forward-only.
- Step-Indikator oben: 14 Punkte; active = gradient-filled (animiert beim Wechsel), inactive = glassBorder.
- Pro Step: GradientText-Titel + bodyLarge Sub + Input-Area.
- Numerische Inputs (Alter / Größe / Gewicht / Aktivitätsindex) **als Slider** mit Live-Value-Label (`tabular-figures`):
  - Alter: 14–100, step 1
  - Größe: 140–220 cm, step 1
  - Gewicht: 30–200 kg, step 0.5
  - Aktivität: 1.2 (sedentary) – 1.9 (extra active), step 0.05
- Bottom-Bar: links „Zurück" (ab Step>0), rechts GradientButton „Weiter".

### 6.3 Plan

- SectionPill „WOCHENPLAN" + Day-Strip (7 GlassChips, heute = accentGradient-Pill).
- Pro Tag: 4 Meal-Slots (Frühstück/Mittag/Abend/Snack), jeder als Mini-GlassCard.
- Add-Sheet: Title „Rezept oder Lebensmittel" (Wording-Lock F-008). Source-Tab-Auswahl: „Rezepte" / „Lebensmittel" via SegmentedTabs.

### 6.4 Essen / Lebensmittel

- Lazy-Page 50 alphabetisch on-open (F-009).
- Search-Bar sticky oben.
- Pre-Selection-Mode (F-007): nav-arg `preselect=true` → FAB wird zu „Auswählen" und Tap auf Item liefert Result-Callback an aufrufenden Screen.

### 6.5 Log (P6.S6)

- SegmentedTabs „Einträge" / „Insights".
- Einträge: chronologische Liste, jede Row mit SeverityBar (4dp links) + Titel (Symptom-Tag-Chips) + Zeit + Notiz-Preview.
- QuickEntrySheet (FAB→Sheet): Severity-Picker 1–5 (5 große Gradient-Chips), Symptom-Tag-Chips (multi-select), Notiz-Textfeld, Time-Picker (default „jetzt").
- Insights: 14-Tage-Histogramm (Bar-Chart), Top-3-Symptome.

### 6.6 Profil

- GlassCard „Konto" (E-Mail, Logout).
- GlassCard „Tagesziele" (per-Nutrient-Editor, kcal + Protein/Carbs/Fat + Mikronährstoff-Slots via Plus-Button).
- GlassCard „Pinned Nutrients" (öffnet PinnedNutrientsManager-Sheet).
- GlassCard „Theme" (Light/Dark/System Toggle).
- GlassCard „Daten exportieren / löschen" (DSGVO).

---

## 7. Migration-Plan (P6.S6 DB-Schema)

| Migration | Inhalt |
|---|---|
| `V12__per_nutrient_goals.sql` | `users.daily_nutrient_goals JSONB DEFAULT '{}'`; `users.pinned_nutrients TEXT[] DEFAULT '{kcal,protein,carbs,fat}'` |
| `V13__log_event_schema.sql` | Drop `log_entries.mood`, `log_entries.sleep_hours` (falls vorhanden); Add `log_entries.severity SMALLINT NOT NULL DEFAULT 3`, `log_entries.symptom_tags TEXT[] NOT NULL DEFAULT '{}'`; Daten-Migration: existierende Einträge → severity=3, tags='{legacy}' |

---

## 8. Scope-Klarstellung (was NICHT von Histamind kommt)

- HealthForge behält **Auth/Server/Admin-UI** — Histamind ist offline-only.
- HealthForge behält **Allergen/Intoleranz-Multi-Tracking** — Histamind ist Histamin-mono.
- HealthForge behält die **5-Tab-Nav-Reihenfolge** Home/Essen/Plan/Log/Profil — kein Tab-Restructure.
- HealthForge behält die **deutsche Sprache** (Histamind matched bereits).
- Bayesian Trigger-Insight, HistamineLoad-Score: **nicht** übernommen.

---

## 9. Doc-Anker (REQ-IDs)

Siehe [ReqSpec.md](ReqSpec.md) §11 "P6 Re-Spec" und [TraceabilityMatrix.md](TraceabilityMatrix.md) für REQ-IDs:
- REQ-DESIGN-001 (Visual-Identity-Lock)
- REQ-TYPO-001 (Manrope)
- REQ-COMP-001..008 (Component-Library)
- REQ-HOME-PIN-001 (Pinned-Nutrients)
- REQ-ONBOARD-SLIDER-001 (Slider statt Zahl)
- REQ-WATER-REMOVE-001 (Undo-Pattern)
- REQ-INTAKE-ADD-FLOW-001 (Pre-Selection-Mode)
- REQ-LOG-EVENT-001..006 (Event-Log-Inversion)
- REQ-PROFILE-GOALS-001 (per-Nutrient-Tagesziele)

---

**End of HistamindDesignReference.md v1.0.**
