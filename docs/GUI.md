# HealthForge — GUI / Design System

**Version:** 0.1 (LOCKED — alle Design-Tokens final für v1.0)
**Datum:** 2025-05-25
**Scope:** Android Client (Material 3) + Admin Web UI (MUI mit gemappten Tokens)
**Vorgängerdokumente:** [ReqSpec.md](ReqSpec.md), [UsabilityMap.md](UsabilityMap.md), [Architecture.md](Architecture.md)

> Dieses Dokument definiert die visuelle Sprache. Konkrete Screen-Layouts stehen in
> [UsabilityMap.md](UsabilityMap.md). Pixel-perfekte Mockups gibt es nicht — Material 3
> wird konsequent angewandt und Tokens unten konsumiert.

---

## 1. Design-Prinzipien

1. **Clean modern** — weniger weißraum-lastig als Histamind, kompakter Komfort
2. **Light + Dark + System** — System ist Default (siehe Profil)
3. **Material 3** als Basis — keine eigene Component-Library bauen
4. **Lucide-Icons sparsam** — nur wo wirklich nötig (Navigation, primäre Aktionen)
5. **Accessibility ab Day 1** — WCAG AA, 48dp Tap-Targets, Content-Descriptions
6. **Deutsch durchgehend** — alle Strings in `strings.xml` (de_DE)

---

## 2. Color Tokens (LOCKED)

**Source-Color (Primary Seed):** `#7CB342` Olive/Apple Green

Material 3 generiert daraus die komplette Palette. Die Werte unten sind die
Production-Token-Werte (vor Implementation via
[Material Theme Builder](https://m3.material.io/theme-builder) verifizieren und exakt
übernehmen).

### 2.1 Light Theme

| Token | Hex | Verwendung |
|---|---|---|
| `primary` | `#4B6A1F` | FAB, Filled-Button, Selection |
| `onPrimary` | `#FFFFFF` | Text auf Primary |
| `primaryContainer` | `#CCEEA1` | Tonal-Button, Chip-Selected |
| `onPrimaryContainer` | `#142000` | Text auf primaryContainer |
| `secondary` | `#57624A` | Sekundäre Akzente |
| `onSecondary` | `#FFFFFF` | |
| `secondaryContainer` | `#DAE7C8` | |
| `onSecondaryContainer` | `#151E0B` | |
| `tertiary` | `#386663` | Akzent für Daten-Visualisierung (Charts) |
| `onTertiary` | `#FFFFFF` | |
| `tertiaryContainer` | `#BCECE8` | |
| `onTertiaryContainer` | `#00201F` | |
| `error` | `#BA1A1A` | Validierungs-Fehler, Lösch-Aktionen |
| `onError` | `#FFFFFF` | |
| `errorContainer` | `#FFDAD6` | Error-Banner-Background |
| `onErrorContainer` | `#410002` | |
| `background` | `#FDFCF5` | Screen-Background (warmweiß) |
| `onBackground` | `#1B1C18` | Haupt-Text |
| `surface` | `#FDFCF5` | Cards, Sheets |
| `onSurface` | `#1B1C18` | |
| `surfaceVariant` | `#E1E4D5` | Tonal-Surface, Outlined-Input-Background |
| `onSurfaceVariant` | `#45483D` | Sekundär-Text, Icon-Tint |
| `outline` | `#75786C` | Outlined-Button, Divider |
| `outlineVariant` | `#C5C8B9` | Subtle-Divider |

### 2.2 Dark Theme

| Token | Hex |
|---|---|
| `primary` | `#B0D17F` |
| `onPrimary` | `#233600` |
| `primaryContainer` | `#355001` |
| `onPrimaryContainer` | `#CCEEA1` |
| `secondary` | `#BECBAD` |
| `onSecondary` | `#2A3420` |
| `secondaryContainer` | `#404B35` |
| `onSecondaryContainer` | `#DAE7C8` |
| `tertiary` | `#A0D0CC` |
| `onTertiary` | `#003735` |
| `tertiaryContainer` | `#1E4E4C` |
| `onTertiaryContainer` | `#BCECE8` |
| `error` | `#FFB4AB` |
| `onError` | `#690005` |
| `errorContainer` | `#93000A` |
| `onErrorContainer` | `#FFDAD6` |
| `background` | `#1B1C18` |
| `onBackground` | `#E4E3DB` |
| `surface` | `#1B1C18` |
| `onSurface` | `#E4E3DB` |
| `surfaceVariant` | `#45483D` |
| `onSurfaceVariant` | `#C5C8B9` |
| `outline` | `#8F9285` |
| `outlineVariant` | `#45483D` |

### 2.3 Semantische Custom-Tokens (über M3 hinaus)

Zusätzlich zu M3 werden folgende **semantische Tokens** definiert (für domänenspezifische
Verwendung):

| Token | Light | Dark | Zweck |
|---|---|---|---|
| `rating.recommend` | `#388E3C` | `#81C784` | Community-Rating: Recommend |
| `rating.notRecommend` | `#D32F2F` | `#EF9A9A` | Community-Rating: Not Recommend |
| `rating.moreOften` | `#4B6A1F` | `#B0D17F` | Lokal-Rating: Mehr-Essen (= primary) |
| `rating.intolerant` | `#BA1A1A` | `#FFB4AB` | Lokal-Rating: Intoleranz (= error) |
| `macro.protein` | `#7E57C2` | `#B39DDB` | Makro-Ring: Protein |
| `macro.carbs` | `#FB8C00` | `#FFB74D` | Makro-Ring: Carbs |
| `macro.fat` | `#FFB300` | `#FFD54F` | Makro-Ring: Fett |
| `macro.calories` | `#4B6A1F` | `#B0D17F` | Makro-Ring: Kalorien (= primary) |
| `water` | `#0288D1` | `#4FC3F7` | Wasser-Tracker |
| `symptom.severity1` | `#C8E6C9` | `#388E3C` | Symptom-Severity 1 (leicht) |
| `symptom.severity5` | `#B71C1C` | `#EF5350` | Symptom-Severity 5 (sehr stark) |
| `severity2-4` interpoliert | — | — | Stufen 2/3/4 sind Farbübergang zwischen 1 und 5 |

---

## 3. Typography (LOCKED)

**Font-Familie:** System-Default
- Android: **Roboto / Roboto Flex** (ab Android 12)
- Sonstige Renderings (Admin-UI): System-UI Stack (`-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, ...`)

### 3.1 Material 3 Type-Scale (verwendet)

| Style | Size | Weight | Line-Height | Verwendung |
|---|---|---|---|---|
| `displayLarge` | 57sp | 400 | 64sp | (selten genutzt) |
| `displayMedium` | 45sp | 400 | 52sp | Onboarding-Welcome |
| `displaySmall` | 36sp | 400 | 44sp | Splash-Screen |
| `headlineLarge` | 32sp | 400 | 40sp | Empty-State-Heroes |
| `headlineMedium` | 28sp | 400 | 36sp | Screen-Titel groß |
| `headlineSmall` | 24sp | 400 | 32sp | Section-Header (Home, Plan) |
| `titleLarge` | 22sp | 500 | 28sp | TopAppBar-Titel, Dialog-Titel |
| `titleMedium` | 16sp | 500 | 24sp | Card-Titel, List-Item-Primärtext |
| `titleSmall` | 14sp | 500 | 20sp | Tab-Labels, Chip-Text |
| `bodyLarge` | 16sp | 400 | 24sp | Haupt-Fließtext (Rezept-Steps) |
| `bodyMedium` | 14sp | 400 | 20sp | List-Item-Sekundärtext |
| `bodySmall` | 12sp | 400 | 16sp | Metadaten, Captions |
| `labelLarge` | 14sp | 500 | 20sp | Button-Text |
| `labelMedium` | 12sp | 500 | 16sp | FilterChip-Text |
| `labelSmall` | 11sp | 500 | 16sp | Tag-Pill-Text, Badges |

**Regel:** Keine eigenen Font-Sizes definieren — immer aus dieser Tabelle.

---

## 4. Spacing & Layout (LOCKED)

**Grid:** 8dp Basis. Alle Margins/Paddings/Gaps Vielfache von 8dp (4dp als
Halb-Schritt nur in begründeten Ausnahmefällen).

### 4.1 Spacing-Skala

| Token | Wert | Verwendung |
|---|---|---|
| `space.xxs` | 4dp | Icon-Text-Abstand inline |
| `space.xs` | 8dp | Tight-Padding, Chip-Innenabstand |
| `space.sm` | 12dp | Card-Padding-vertikal |
| `space.md` | 16dp | Standard-Screen-Padding, Card-Padding |
| `space.lg` | 24dp | Section-Abstand |
| `space.xl` | 32dp | Empty-State-Hero-Padding |
| `space.xxl` | 48dp | (selten) |

### 4.2 Tap-Targets

- **Mindestens 48dp × 48dp** (WCAG AA + Material 3)
- IconButton: 48dp Hit-Area, Icon visuell 24dp
- ListItem: min-height 56dp (single-line) / 72dp (two-line) / 88dp (three-line)

### 4.3 Screen-Layout

- Horizontal-Padding: 16dp (Phone), 24dp (Tablet/Large-Phone wenn > 600dp Width)
- TopAppBar: 64dp Höhe (Small) / 152dp (Large bei Scroll-Top)
- BottomNavigation: 80dp Höhe (M3 Standard)
- FAB: 56dp Standard / 96dp Extended

---

## 5. Shape / Corner-Radius (LOCKED)

| Komponente | Radius |
|---|---|
| Cards | **8dp** |
| Buttons (Filled, Tonal, Outlined, Text) | **12dp** |
| Chips | **8dp** |
| Text-Fields (Outlined) | **8dp** |
| Dialogs | **16dp** |
| Bottom-Sheets | **16dp top corners** |
| Snackbars | **8dp** |
| FAB | **16dp** (regular) / **28dp** (extended) — M3-Default |
| Image-Thumbnails (Liste) | **8dp** |
| Avatar | **Full / Circle** |

---

## 6. Elevation

Material 3 nutzt Elevation hauptsächlich tonal (Surface-Tint), nicht Schatten.

| Komponente | dp | Bemerkung |
|---|---|---|
| Card (default) | 1dp tonal | leichte Surface-Tint |
| Card (raised) | 3dp tonal | bei Selection |
| FAB | 6dp + shadow | Hervorhebung |
| Dialog | 6dp + shadow | über Scrim |
| TopAppBar (scrolled) | 3dp tonal | nur wenn Liste gescrollt |
| BottomNav | 2dp tonal | |

---

## 7. Icons

- **Library:** [Lucide Icons](https://lucide.dev/) (oder Material Icons als Fallback)
- **Größen:** 20dp (inline-small), 24dp (default), 28dp (large)
- **Stroke-Width:** 2px (Lucide-Default)
- **Color:** `onSurfaceVariant` (sekundär), `primary` (aktiv/akzent), `onSurface` (haupt)
- **Verwendungs-Regeln:**
  - **Navigation-Icons** (Bottom-Nav, TopAppBar): immer Icon
  - **Primary-Actions** (FAB, primärer Button): Icon + Text, nicht nur Icon
  - **List-Item-Actions** (Edit, Delete in Lang-Tap-Menü): Icon + Label
  - **Inline-Hinweise** (Allergen-Warnung): Icon + Text-Tooltip
  - **NIEMALS** rein dekorativ ohne semantische Bedeutung

### 7.1 Icon-Mapping (Standard-Set)

| Konzept | Lucide-Icon |
|---|---|
| Home | `home` |
| Plan | `calendar-days` |
| Essen | `utensils` |
| Log (Tagebuch) | `book-open` |
| Profil | `user` |
| Hinzufügen / FAB | `plus` |
| Filter | `sliders-horizontal` |
| Suche | `search` |
| Wasser | `droplet` |
| Supplement | `pill` |
| Rezept | `chef-hat` |
| Lebensmittel | `apple` |
| Mehr-Essen Rating | `thumbs-up` |
| Intoleranz Rating | `alert-triangle` |
| Recommend Community | `heart` |
| Not-Recommend Community | `heart-off` |
| Mehr-Menü | `more-vertical` |
| Schließen | `x` |
| Bearbeiten | `pencil` |
| Löschen | `trash-2` |
| Teilen | `share-2` |
| Einkaufsliste | `shopping-cart` |
| Gruppen | `users` |
| Einstellungen | `settings` |
| Benachrichtigungen | `bell` |
| Export | `download` |
| Barcode | `scan-line` |
| Mood-Slider | `smile` / `frown` |
| Schlaf | `moon` |

---

## 8. Komponenten-Library (Mapping auf M3)

Alle Komponenten kommen aus **Compose Material 3**. Keine custom Re-Implementierung
außer den unten gelisteten Wrapper-Components.

### 8.1 Standard-M3-Komponenten (direkt nutzen)

`Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `FloatingActionButton`,
`ExtendedFloatingActionButton`, `IconButton`, `FilterChip`, `AssistChip`, `InputChip`,
`SuggestionChip`, `Card`, `OutlinedCard`, `ElevatedCard`, `OutlinedTextField`,
`TextField`, `TopAppBar`, `LargeTopAppBar`, `BottomAppBar`, `NavigationBar`,
`NavigationBarItem`, `Tab`, `TabRow`, `ScrollableTabRow`, `Switch`, `Checkbox`,
`RadioButton`, `Slider`, `LinearProgressIndicator`, `CircularProgressIndicator`,
`Snackbar`, `AlertDialog`, `ModalBottomSheet`, `Divider`, `ListItem`.

### 8.2 Custom Wrapper-Components

| Component | Zweck |
|---|---|
| `MacroRing` | Ring-Diagramm für Kalorien/Protein/Carbs/Fett (Home) — 3 konzentrische Rings + Center-Number |
| `WaterTracker` | Horizontale Glas-Reihe mit getappten Füll-Glässern + Plus-Button |
| `RatingPill` | 4-state-Pill (Recommend / NotRecommend / MoreOften / Intolerant) basierend auf Context |
| `AllergenWarningBadge` | Roter Badge mit Allergen-Icon + Text bei Konflikt |
| `MealSlot` | Slot-Card für Plan-Tab mit Mahlzeit-Typ, Zeit, Item-Liste, "Habe gegessen"-Button |
| `SymptomSeverityChip` | FilterChip mit Severity-Color-Background (1–5) |
| `EmptyState` | Hero-Icon + Headline + Body + Optional-Action-Button |
| `OfflineBanner` | Snackbar-Variante oben, persistent, mit Retry-Button |
| `PhasePlaceholder` | Vollscreen-Placeholder für Features die in späterer Phase kommen |
| `DateNavigator` | Pfeil-Links / Datum-Pill / Pfeil-Rechts (Home, Plan, Log) |

### 8.3 Phase-Verfügbarkeit

| Component | P1 | P2 | P3 | P4 |
|---|:-:|:-:|:-:|:-:|
| `MacroRing` | ✅ | | | |
| `WaterTracker` | ✅ | | | |
| `RatingPill` | ✅ (Local) | ✅ (Community) | | |
| `AllergenWarningBadge` | ✅ | | | |
| `MealSlot` | | ✅ | | |
| `SymptomSeverityChip` | | | ✅ | |
| `EmptyState` | ✅ | | | |
| `OfflineBanner` | ✅ | | | |
| `PhasePlaceholder` | ✅ | ✅ | ✅ | — |
| `DateNavigator` | ✅ | | | |

---

## 9. Theming-Implementation (Android)

### 9.1 Theme-Switching

User-Setting in `user_profile.theme_preference`: `LIGHT` / `DARK` / `SYSTEM` (Default).

```kotlin
@Composable
fun HealthForgeTheme(
    pref: ThemePreference,
    content: @Composable () -> Unit
) {
    val darkTheme = when (pref) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HealthForgeTypography,
        shapes = HealthForgeShapes,
        content = content
    )
}
```

### 9.2 Dynamic-Color

**LOCKED:** Dynamic-Color **NICHT** verwenden (Material-You-Wallpaper-Adaption). Grund:
Brand-Farbe `#7CB342` muss konsistent bleiben über alle User-Wallpapers hinweg.

---

## 10. Motion (LOCKED)

- **Standard-Durations:** Short 100ms, Medium 250ms, Long 400ms (Material 3 Tokens)
- **Easing:** `FastOutSlowInEasing` für UI-Transitions, `LinearEasing` für Loader
- **Screen-Transitions:** Compose-Navigation default (Fade + Slide-Forward für Forward, Fade + Slide-Backward für Back)
- **Bottom-Sheet-Open:** 300ms slide-up
- **Dialog-Open:** 250ms fade + scale
- **Reduzierte Motion:** wenn System-Setting `Reduce-Animations` aktiv → alle Durations 0ms, nur Fade-Transitions

---

## 11. Accessibility (LOCKED)

- **Kontrast:** WCAG AA (4.5:1 für Text, 3:1 für Icons/UI)
- **Tap-Targets:** min 48dp × 48dp
- **Content-Descriptions:** auf allen interaktiven Icons (auch wenn Label daneben steht)
- **TalkBack-Navigation:** sinnvolle Lese-Reihenfolge via `semantics { traversalIndex = ... }`
- **Dynamic-Type:** Compose respektiert System-Font-Scale (User-Setting "Schriftgröße")
- **Color-Independent:** keine Information nur durch Farbe (Rating immer Icon + Text + Color)
- **Focus-Indicator:** sichtbarer Outline für Tastatur/Switch-Navigation

---

## 12. Admin Web UI Mapping (MUI)

Admin-UI nutzt **Material UI (MUI 5)** für React. Tokens werden über `createTheme()`
gemappt — gleiche Farben + ähnliche Spacings.

```typescript
const adminTheme = createTheme({
  palette: {
    mode: prefersDark ? 'dark' : 'light',
    primary: { main: '#7CB342' },
    secondary: { main: '#57624A' },
    error: { main: '#BA1A1A' },
    background: { default: prefersDark ? '#1B1C18' : '#FDFCF5' },
  },
  shape: { borderRadius: 8 },           // Cards
  components: {
    MuiButton: { styleOverrides: { root: { borderRadius: 12 } } }
  },
  typography: { fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif' }
});
```

---

## 13. Asset-Standards

- **App-Icon:** Adaptive-Icon (Foreground SVG-basiert + Background-Color `#7CB342`)
- **Splash-Screen:** Vector-Logo zentriert auf `background`-Color, kein Text
- **Empty-State-Illustrations:** Stroke-only Lucide-Style oder einfache 2-Color-SVGs
- **Rezept-Bilder:** 16:9 oder 4:3, ≥ 800px breit nach Server-Resize
- **Avatar-Placeholders:** Initial-Buchstabe auf `primaryContainer`-Background

---

## 14. Zu vermeiden ("Don'ts")

- **Keine Gradients** (außer dezent in Charts) — flat-tonal-design
- **Keine Drop-Shadows** außer M3-default (Elevation)
- **Kein Custom-Font** für v1.0
- **Keine Emojis** als UI-Element (nur in User-generated-Content)
- **Keine Brand-Color-Variationen** außer den definierten Tokens (kein "ähnliches Grün" frei wählen)
- **Keine Mixed-Density-Screens** — pro Screen konsistent Comfortable-Density
- **Keine Capital-Letters-Buttons** (Material 2 Legacy) — Buttons in Sentence-Case

---

## 15. Folgedokumente

- Konkrete Screen-Layouts: [UsabilityMap.md](UsabilityMap.md)
- Code-Implementation der Theme-Tokens: später in `app/src/main/kotlin/de/healthforge/ui/theme/`
- Admin-UI Tokens: später in `admin-ui/src/theme.ts`

---

**Ende GUI v0.1 LOCKED.**
