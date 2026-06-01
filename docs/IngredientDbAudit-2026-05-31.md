# Lebensmittel-DB Coverage Snapshot — 2026-05-31

**Stand:** Nach Hotfix-2/3/4/5 (DB-Reset → 26 Translation-Korrekturen → 5 Append/Rename → AllergenMapper Plural/Compound Keywords → SighiImporter ~140 Compound/Region/Sorte-Keywords).
**Quelle:** `audit_snapshot.sql` (`server/tools/`) gegen produktive Postgres (`healthforge` DB).
**Zweck:** Baseline für künftige Hotfix-/Slice-Priorisierung. Vorgänger-Snapshot existierte nicht — dies ist der erste konsolidierte State.

---

## 1) Gesamt-Coverage

| Metrik | Wert | Kommentar |
|---|---|---|
| Total Ingredients | **637** | USDA_FDC + 5 Append-Rows + 1 Magermilch-fluid (Hotfix-7) |
| Mikros ≥ 1 | 635 (99.8 %) | nur 1 Row ohne Mikros |
| Mikros ≥ 10 | 629 (98.9 %) | sehr hoch |
| Mikros ≥ 20 | **536 (84.3 %)** | exzellente Tiefe |
| Allergens flagged | **212 (33.3 %)** | nach Hotfix-4 |
| Histamine score gesetzt | **626 (98.4 %)** | nach Hotfix-6 — 10 NULL transparent („unbekannt") |
| FODMAP flagged | **0 (0.0 %)** | ⚠ Mapper noch nicht implementiert |

---

## 2) Allergens-Verteilung (EU-FIC §14 + Bonus)

| Code | n | Hotfix-4 Effect |
|---|---|---|
| LACTOSE | 77 | +kefir/yogurts/milks/creams/cheeses |
| GLUTEN | 43 | +bread/bagel/noodle/pasta/pita/… |
| FISH | 19 | +sardines |
| NUT | 17 | +hazelnuts/walnuts/pecans/cashews/pistachios/macadamias |
| SOY | 14 | +soymilk/soyabean |
| EGG | 12 | — |
| HISTAMINE | 8 | (in Allergen-Code-Pfad) |
| ALCOHOL | 8 | — |
| MOLLUSC | 7 | +oysters/clams/scallops/squids/snails |
| SESAME | 6 | — |
| CRUSTACEAN | 6 | +shrimps/prawns/lobsters/crabs |
| PEANUT | 5 | — |
| MUSTARD | 4 | — |
| CELERY | 3 | — |
| TYRAMINE | 1 | — |

**Delta vs Pre-Hotfix-4:** 188 → 212 (+24 flagged ingredients, +12.7 %).

**Nicht abgedeckte EU-Codes:** LUPIN, SO2/SULPHITE — keine USDA-Daten-Treffer (erwartbar, da kuratierter 636er Subset).

---

## 3) Mikronährstoff-Tiefe (Top + Lücken)

### Top-Coverage (≥ 90 %)
calcium 635 · eisen 634 · natrium 633 · zink 633 · phosphor 633 · kalium 633 · magnesium 632 · kupfer 630 · vitamin_b1 622 · vitamin_b3 622 · vitamin_b6 618 · vitamin_b2 617 · vitamin_c 606 · vitamin_b9 603 · vitamin_b12 599 · selen 599 · mangan 594 · vitamin_a 592

### Lücken (Action-Items)
| Nutrient | Coverage | Status |
|---|---|---|
| **jod** | **5 / 636 (0.8 %)** | ⚠ systemische Lücke — USDA misst Jod kaum |
| **vitamin_b7 (Biotin)** | **11 / 636 (1.7 %)** | ⚠ systemische Lücke — USDA misst Biotin kaum |
| vitamin_k | 496 (78.0 %) | akzeptabel |
| vitamin_e | 508 (79.9 %) | akzeptabel |
| vitamin_b5 | 550 (86.5 %) | gut |
| vitamin_d | 557 (87.6 %) | gut |

**Befund:** 22 der bekannt-fehlenden 24 Mikros sind Single-Row-Lücken (ohne strukturelle Ursache). **Biotin und Jod** sind die einzigen systemischen Lücken — Quelle USDA-FDC enthält sie kaum. Mitigation nur via externe Quelle (DGE/BLS/Souci) möglich.

---

## 4) Histamin-Coverage (SIGHI-Importer)

| Score | n | Anteil |
|---|---|---|
| 0 (gut verträglich) | 381 | 60.0 % |
| 1 (unsicher) | 46 | 7.2 % |
| 3 (zu meiden) | 199 | 31.3 % |
| **NULL (unbekannt, transparent)** | **10** | **1.6 %** |

**Befund nach Hotfix-6 (Score-1-Audit):** Pro-Item-Audit als Ernährungsberater. Score 1 enthält jetzt ausschließlich direkt aus SIGHI-Merkblatt-2021-11-17 abgeleitete Klassifikationen (Aal, Lachs, Buttermilch, Joghurt, Kefir, Skyr, Feta, Crème fraîche, Schmand, Buchweizen, Espresso, Kaffee, Tee, Senf, Apfelessig, Hafermilch, Pumpernickel/Sauerteig, Weizenkeime, Erbsen, Wild→Hirsch/Reh/Fasan, Hackfleisch→Rinderhack, Kochschinken).

**Korrekturen Hotfix-6:** Schwertfisch 1→3 (Hochsee-Raubfisch), BBQ Sauce 1→3 (Tomatenbasis), Pesto 1→3 (Parmesan), Wildreis 1→0 (Bug-Fix: „Wild"-Substring überschrieb „Wildreis=0").

**Transparenz NULL (10 Rows):** Sriracha, Mayonnaise + Light, Currypulver, Sumach, Lupinen, Veggie Burger, Nougat, Energy Drink, Rosine — Datenlage uneinheitlich oder Zusammensetzung variabel. Community-/User-Override geplant.

**Historische Zwischenstände:**
- vor Hotfix-5: 486/636 klassifiziert (76.4 %).
- nach Hotfix-5: 636/636 klassifiziert (100 %), aber 12 Eigenbewertungen ohne direkte SIGHI-Quelle.
- nach Hotfix-6: 626/636 direkt-SIGHI-klassifiziert (98.4 %), 10 transparent „unbekannt".

---

## 5) FODMAP-Coverage

**Status:** 0 / 636 (0.0 %). Mapper noch nicht implementiert. Sichtbar im UI als leere FODMAP-Chips im `IngredientDetailSheet`. Separater Slice nötig (Monash-University-Datensatz oder kuratierte Manual-CSV).

---

## 6) Action-Items priorisiert

| # | Item | Effort | ROI | Empfehlung |
|---|---|---|---|---|
| 1 | ~~SighiImporter Keyword-Expansion~~ | klein | ~~150 Rows neu klassifiziert~~ | ✅ **Erledigt (Hotfix-5)** |
| 2 | FODMAP-Mapper-Slice (Monash o. manuelle CSV) | mittel | UI-Feature aktiviert | **eigener Slice** |
| 3 | External Source (DGE/BLS/Souci) für 12 fehlende DE-Foods + Biotin/Jod | groß | Daten-Vollständigkeit | **Slice mit Beschaffungs-Phase** |
| 4 | ~30 Borderline-Mismatches Triage | klein-mittel | Qualität Spot | Review-Session |
| 5 | 22 Mikros manuelle Einzel-Lücken (USDA-Web-UI) | groß (Handarbeit) | gering pro Stunde | später / optional |

---

## 7) Reproduktion

```bash
docker cp server/tools/audit_snapshot.sql healthforge-postgres-dev:/tmp/audit.sql
docker exec healthforge-postgres-dev psql -U healthforge -d healthforge -f /tmp/audit.sql
```

Skript ist idempotent und read-only.
