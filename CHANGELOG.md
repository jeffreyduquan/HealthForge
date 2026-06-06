# HealthForge CHANGELOG

Konvention: Jeder P6-Sprint-Abschluss + jede groessere Code-/Verhaltens-Aenderung erhaelt einen Eintrag.
Format pro Eintrag: **Sprint/Datum** + **Touched Docs** + **Untouched-Begruendung** + **Verifikation**.

---

## Bugfix: Admin-UI 404 — Stale Docker-Bind-Mount (2026-06-06)

**Scope:** Admin-UI unter http://admin.healthforge.endgear.de:8080 zeigte leere Seite / 404. Ursache: Caddy-Container wurde gestartet, bevor `admin-ui-dist/` existierte → Docker legte leeres Root-Verzeichnis an → später erstelltes Verzeichnis hatte anderen Inode → Bind-Mount zeigte ins Leere.

**Fix:**
- Caddy-Container restartet (bind mount neu evaluiert)
- CI/CD-Prävention in `.github/workflows/server.yml`:
  - Neuer Step: `mkdir -p + chown` auf `admin-ui-dist/` VOR dem SCP
  - `docker compose exec caddy reload` ersetzt durch `docker compose up -d caddy --force-recreate` (zwingt komplette Neu-Evaluierung des Bind-Mounts)

**Touched Docs:** Keine (reiner Bugfix, keine Architektur-/Req-Änderung)

**Touched Code:**
- MOD `.github/workflows/server.yml` — Pre-SCP dir-ensure + Caddy force-recreate

**Verifikation:** Seite lädt; Login-Formular wird angezeigt; kein 404 mehr.

---

## Bugfix: APK-Download-Link SSL-Fehler (2026-06-06)

**Scope:** Geteilter APK-Download-Link verwendete hartkodiertes `https://` – aber der Server läuft HTTP-only (parallel zu dwight auf 80/443). Ergebnis: `ERR_SSL_PROTOCOL_ERROR` beim Öffnen des Links.

**Fix:**
- `AdminReleaseController.kt` – `"https://api.healthforge.endgear.de/..."` durch konfigurierbare Property `${healthforge.api.public-url}` ersetzt
- `application.yml` – Neue Property `healthforge.api.public-url` mit Default `${API_PUBLIC_URL:http://localhost:8080}`
- `docker-compose.prod.yml` – `API_PUBLIC_URL: http://api.healthforge.endgear.de:8080` hinzugefügt; `CORS_ORIGINS` auf `http://` korrigiert

**Touched Docs:** Keine (reiner Bugfix, keine Architektur-/Req-Änderung)

**Touched Code:**
- MOD `AdminReleaseController.kt` — Download-URL aus Property statt hartkodiert
- MOD `application.yml` — Neue Property `healthforge.api.public-url`
- MOD `deploy/docker-compose.prod.yml` — `API_PUBLIC_URL` + korrigiertes `CORS_ORIGINS`

**Verifikation:** API-Endpunkt liefert 200 mit Presigned-URL über HTTP; neuer Download-Link verwendet `http://api.healthforge.endgear.de:8080/...`.

---

## Bugfix: Presigned MinIO-URL enthält internen Hostnamen (2026-06-06)

**Scope:** Der geklickte Download-Link gab JSON mit `"url":"http://minio:9000/..."` zurück – von außen nicht auflösbar (DNS_PROBE_FINISHED_NXDOMAIN). Auch auf dem Handy unbrauchbar.

**Fix:**
- `fixMinioUrl()`-Helfer in beiden Controllern: Ersetzt den internen MinIO-Host (`minio:9000`) in Presigned-URLs durch die öffentliche CDN-Base-URL
- `MINIO_PUBLIC_BASE_URL` in `docker-compose.prod.yml` auf `http://cdn.healthforge.endgear.de:8080` korrigiert (war fälschlich `https://cdn...`)
- `PublicReleaseController` bekam `publicBaseUrl`-Injection (war vorher nur im `AdminReleaseController`)

**Touched Docs:** Keine (reiner Bugfix)

**Touched Code:**
- MOD `AdminReleaseController.kt` — `fixMinioUrl()` angewendet in `downloadUrl()`
- MOD `PublicReleaseController.kt` — `publicBaseUrl` injiziert + `fixMinioUrl()` angewendet in `download()`
- MOD `deploy/docker-compose.prod.yml` — `MINIO_PUBLIC_BASE_URL` auf HTTP + Port 8080

**Verifikation:** Nach Deploy wird in der Presigned-URL `cdn.healthforge.endgear.de:8080` statt `minio:9000` verwendet. APK kann von extern (auch Handy) heruntergeladen werden.

---

## Feature: Download-GUI für geteilte APK-Links (2026-06-06)

**Scope:** Der geteilte Download-Link (`/v1/releases/{id}?code=...`) zeigt jetzt eine mobile-freundliche HTML-Seite mit Versionsinfo, Changelog und Download-Button. Nach dem Download wird der Button ausgegraut ("Bereits heruntergeladen").

**Neu:**
- `PublicReleaseController.sharePage()` — neuer Endpoint `GET /v1/releases/{id}?code=...` serviert HTML-Seite
- Drei Status: `valid` → Download-Button blau | `used` → Button ausgegraut | `expired` → Button rot
- Responsive Design (CSS), optimiert für Handy-Browser
- Generierte Share-URL zeigt jetzt auf die neue GUI-Seite (statt direktem Download)

**Touched Docs:** Keine

**Touched Code:**
- NEW `PublicReleaseController.kt` — `sharePage()` + `buildSharePage()` + `formatFileSize()` + `formatDateTime()` + `escapeHtml()`
- MOD `AdminReleaseController.kt` — Share-URL auf `.../{id}?code=...` geändert

**Verifikation:** `curl http://api.../v1/releases/{id}?code=...` liefert HTML 200; bei verwendetem Code erscheint "✓ Bereits heruntergeladen" mit deaktiviertem Button.

---

## Production-Deploy + CI/CD + APK-Release-Feature (P1.S8 Phase 2) — 2026-06-06

**Scope:** Erster Production-Deploy auf Netcup VPS (159.195.151.92). Vollständiger Stack (Postgres, MinIO, API, Caddy, Backup) live. CI/CD für Server + Admin-UI aktiviert. APK-Release-Verwaltung im Admin-UI.

**Touched Code:**
- NEW `server/src/main/kotlin/de/healthforge/admin/AdminReleaseController.kt` — APK-Upload/List/Delete/Download-Endpoints
- NEW `server/src/main/kotlin/de/healthforge/admin/ApkRelease.kt` — JPA-Entity für `apk_releases`
- NEW `server/src/main/kotlin/de/healthforge/admin/ApkReleaseRepo.kt` — Spring-Data-Repository
- NEW `server/src/main/resources/db/migration/V15__apk_releases.sql` — Flyway-Migration
- NEW `admin-ui/src/pages/ReleasesPage.tsx` — APK-Release-UI (Upload-Dialog, Tabelle, Download)
- MOD `admin-ui/src/App.tsx` — Route `/releases` hinzugefügt
- MOD `admin-ui/src/components/Layout.tsx` — Nav-Eintrag "APK Releases"
- MOD `admin-ui/src/api/client.ts` — API-Funktionen für Releases + Multipart-Fix
- MOD `deploy/docker-compose.prod.yml` — Image `jawra`→`jeffreyduquan`; Caddy-Ports 80/443→8080/8443; CORS auf HTTP
- MOD `deploy/Caddyfile` — HTTP-only (parallel zu dwight); Request-Body-Limit 100MB
- MOD `.github/workflows/server.yml` — Docker-Publish + SSH-Deploy-Job
- MOD `.github/workflows/admin-ui.yml` — SCP-Deploy-Job
- MOD `server/src/main/resources/application.yml` — Mail-Health-Check deaktiviert; Multipart-Limit 100MB
- MOD `server/src/main/kotlin/de/healthforge/media/ImageUploadService.kt` — Bucket `releases` hinzugefügt

**Touched Docs:**
- `docs/ReqSpec.md` — REQ-ADMIN-004/005/006 hinzugefügt (APK Release, Download, Auto-Deploy)
- `docs/Runbook.md` — Ports aktualisiert (8080/8443); CI/CD-Workflow beschrieben; APK-Release-Management ergänzt
- `docs/SprintPlan.md` — P1.S8 Deploy-Items auf ✅; APK-Release ergänzt; 637 USDA-Zutaten importiert vermerkt
- `docs/TraceabilityMatrix.md` — REQ-ADMIN-001→✅, REQ-ADMIN-003→🟡, REQ-ADMIN-004/005/006→✅
- `docs/Architecture.md` — Caddy parallel dwight dokumentiert; APK-Release erwähnt
- `CHANGELOG.md` — dieser Eintrag

**Untouched (begründet):**
- `GUI.md` / `UsabilityMap.md` — APK-Release ist Admin-Feature ohne GUI-Spec (dev-internal tool)
- `TestStrategy.md` / `BattleTestPlan.md` — kein neues Testverfahren, nur Feature-Erweiterung
- `HistamindDesignReference.md` / `IngredientDbAudit-2026-05-31.md` — nicht betroffen

**Verifikation:**
- API: `GET http://api.healthforge.endgear.de:8080/actuator/health` → `{"status":"UP"}`
- Admin-UI: `http://admin.healthforge.endgear.de:8080` → Login → Dashboard + Alle Seiten erreichbar
- APK-Upload: APK v0.1.0 (45 MB) erfolgreich hochgeladen, in DB (`apk_releases`) + MinIO gespeichert
- ETL: 637 USDA-Zutaten importiert (source=USDA_FDC)
- CI/CD: server-ci #13 ✅ + admin-ui-ci #5 ✅ (build→docker-publish→deploy)
- Flyway: V15 sauber auf VPS-DB migriert

## Magermilch-Bugfix + Fluid-Variante (P7.S3 Slice 1 Hotfix-7) — 2026-05-31

**Scope:** Smoketest-Befund: „Magermilch" hatte 362 kcal/100g — entspricht Magermilchpulver (FDC 172195 = „Milk, dry, nonfat"), nicht der flüssigen Form. Klassischer Translation-Bug der gleichen Klasse wie Hotfix-2. Da Pulverform (Smoothies/Backen) real nützlich ist: Rename + parallele Aufnahme der fluid-Variante.

**Touched Code:**
- MOD `server/src/main/resources/seed/usda_fdc_curated.csv` — FDC 172195 umbenannt „Magermilch" → „Magermilchpulver"; neue Zeile FDC 171269 (SR Legacy „Milk, nonfat, fluid, with added vitamin A and vitamin D") als „Magermilch" hinzugefügt (34 kcal, 3.37 g Protein, 0.08 g Fett, Ca 122 mg, Vit D 1.2 µg).
- NEW `server/tools/fix_magermilch.ps1` — Patch-Skript.

**Verifikation:**
- ETL-Run `d5ebb66d-…` USDA_FDC: 1 inserted (171269), 636 updated.
- ETL-Run `2d41e295-…` SIGHI: 3 updated (Re-Apply Schwertfisch/BBQ/Pesto=3 nach CSV-Edits aus Hotfix-6), Magermilch (171269) → Score 0 (analog Milch).
- DB-Check: Magermilch=34 kcal ✓ / Magermilchpulver=362 kcal ✓ / Vollmilch=61 kcal unverändert.

**Touched Docs:**
- `docs/SprintPlan.md` — Hotfix-7 Sub-Eintrag.
- `docs/IngredientDbAudit-2026-05-31.md` — Coverage-Update (637 Ingredients statt 636).
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `ReqSpec.md` / `Architecture.md` / `GUI.md` / `UsabilityMap.md` / `TestStrategy.md` / `Runbook.md` / `BattleTestPlan.md` / `TraceabilityMatrix.md` — reine Datenpflege analog Hotfix-2/3, keine Code-/UX-/Test-Pfad-Änderung.

**Bekannte verwandte Issues (Folge-Backlog):**
- **„Fettarme Milch" (FDC 167697)** ist tatsächlich „Milk, buttermilk, fluid, cultured, reduced fat" — also fettarme Buttermilch, NICHT fettarme Milch. Müsste umbenannt werden zu „Buttermilch fettarm" und durch echte fluid-low-fat Milk (z. B. FDC 746782 / 170874) ersetzt werden.
- Anderes Smoke-Test-Pattern: „Mayonnaise" vs „Mayonnaise leicht" — gewollte Kuratierung (USDA-FDC unterscheidet nutritionell distinkte Fett-Varianten, beibehalten).

---

## Score-1-Audit & Korrektur Eigenbewertungen (P7.S3 Slice 1 Hotfix-6) — 2026-05-31

**Scope:** User-Challenge zur Score-1-Verteilung (9.4 %, 60 Rows): „stehen die auf unknown? oder hast du ihnen einfach einen Wert gegeben?". Audit ergab 12 Einträge ohne direkten SIGHI-Merkblatt-Bezug (eigene analoge Klassifikation in Hotfix-5). Pro-Item-Audit als Ernährungsberater + Korrektur/NULL-Setzung. Plus Bug-Fix: „Wild"-Substring greift fälschlich auf „Wildreis" (sollte 0 sein).

**Touched Code:**
- MOD `server/src/main/resources/seed/sighi.csv` — 9 Eigenbewertungs-Regeln entfernt (Sriracha/Mayonnaise/Currypulver/Sumach/Lupinen/Veggie Burger/Nougat/Energy Drink/Rosine); 3 Korrekturen (Schwertfisch 1→3 analog Thunfisch, BBQ 1→3 tomatenbasiert, Pesto 1→3 Parmesan-Hartkäse).
- NEW `server/tools/score1_audit_cleanup.ps1` — Audit-Cleanup-Skript.
- DB-UPDATE (direkt, ohne Migration): 10 × `histamine_score = NULL` (Sriracha, Mayonnaise, Mayonnaise leicht, Currypulver, Sumach, Lupinen, Veggie Burger, Nougat, Energy Drink, Rosine), 3 × `= 3` (Schwertfisch, BBQ Sauce, Pesto), 1 × `= 0` (Wildreis-Bug).

**Verifikation:**
- Neue Verteilung: **381 × Score 0 (60.0 %) / 46 × Score 1 (7.2 %, alle direkt-SIGHI) / 199 × Score 3 (31.3 %) / 10 × NULL (1.6 %, transparent „unbekannt")**.
- Datenintegrität: Score 1 enthält nur noch SIGHI-Merkblatt-direkte Klassifikationen (Aal, Lachs, Buttermilch/Joghurt/Kefir/Skyr/Crème fraîche/Schmand/Feta, Buchweizen, Espresso/Kaffee/Tee, Senf, Apfelessig, Wild→Hirsch/Reh/Fasan, Hackfleisch→Rinderhack, Kochschinken, Erbsen, Hafermilch, Sauerteig→Pumpernickel, Weizenkeime).

**Audit-Begründungen pro Item:**
- **Schwertfisch 1→3**: Hochsee-Raubfisch analog Thunfisch (SIGHI=3), DAAB-Empfehlung „zu meiden".
- **BBQ Sauce 1→3**: Tomate (SIGHI=3) + häufig Worcester (=3) als Hauptbestandteil.
- **Pesto 1→3**: enthält Parmesan = Hartkäse (SIGHI=3).
- **Sriracha → NULL**: fermentierte Chili-Sauce, kurzfermentiert mit Essig, Datenlage uneinheitlich.
- **Mayonnaise + leicht → NULL**: variabel je nach Essig-Typ (Apfel=1 / Wein=3) und Senf-Anteil.
- **Currypulver → NULL**: variabel (mild ≠ scharf); SIGHI listet nur „scharfes Curry=3".
- **Sumach → NULL**: keine SIGHI-/DAAB-Klassifikation verfügbar.
- **Lupinen → NULL**: Süßlupine ≠ Sojabohne, DAAB-Studienlage unklar.
- **Veggie Burger → NULL**: komplett variable Zusammensetzung.
- **Nougat → NULL**: mit/ohne Schoki völlig unterschiedlich (0 ↔ 3).
- **Energy Drink → NULL**: kein SIGHI-Match, Koffein-Analogie zu Kaffee nicht ausreichend belegt.
- **Rosine → NULL**: Trockenobst nicht explizit in SIGHI, klinische Studienlage uneinheitlich.
- **Wildreis 1→0 (Bug-Fix)**: SIGHI listet „Wildreis=0" explizit, wurde aber durch „Wild;1"-Substring überschrieben (Max-Score-Wins). DB-Override; CSV-Regel-Verfeinerung (Word-Boundary statt Substring) bleibt offener Tech-Debt.

**Touched Docs:**
- `docs/SprintPlan.md` — Hotfix-6 Sub-Eintrag.
- `docs/IngredientDbAudit-2026-05-31.md` — Histamin-Verteilung aktualisiert, Transparenz-Note ergänzt.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `ReqSpec.md` / `Architecture.md` / `GUI.md` / `UsabilityMap.md` / `TestStrategy.md` / `Runbook.md` / `BattleTestPlan.md` — keine Code-/Verhaltens-/UX-Änderung; reine Datenpflege + Audit.
- `TraceabilityMatrix.md` — REQ-INGR-003 / REQ-QUALITY-003 (NULL = unbekannt) bereits dokumentiert; keine neue Anforderung.

**Offener Tech-Debt:**
- SighiImporter-Substring-Matcher Word-Boundary-Fix: „Wild" sollte nicht auf „Wildreis" greifen. Lösung: Token-basiertes Matching oder Negativ-Liste (REQ-INGR-003 Erweiterung in Folge-Sprint).
- 10 NULL-Rows können von Community/User-Override später ergänzt werden.

---

## SighiImporter Keywords Compound/Region/Sorte (P7.S3 Slice 1 Hotfix-5) — 2026-05-31

**Scope:** Audit-Snapshot zeigte 150 Ingredients ohne Histamin-Score (23.6 % der 636). Ursache: SIGHI-Substring-Matcher erkannte generische SIGHI-Kategorien (z. B. „Wurst", „Hartkäse"), aber nicht konkrete Produkt-/Regional-/Sorten-Namen wie „Mortadella", „Bergader", „Pak Choi", „Cantaloupe", „Bagel". Quick-Win analog Hotfix-4: kuratierte Supplement-Sektion in `sighi.csv`.

**Touched Code:**
- MOD `server/src/main/resources/seed/sighi.csv` — Hotfix-5-Supplement-Sektion (~140 neue Keywords) gruppiert nach Fleisch/Fisch/Milch/Hefe/Saucen/Brot/Alkohol/Süß/Obst/Gewürze/Öle/Gemüse. SIGHI-Merkblatt-Klassifikation (0/1/3) konsequent angewandt.

**Verifikation:**
- ETL-Runs: `c71bd613-…` (148 updated) + `2aea5a8a-…` (+3 final-fixups Chia Samen/Kokosflocken/Pekannuss).
- **Histamin-Coverage: 486 → 636 (100 %).** 0 verbleibende NULL-Rows.
- Verteilung Final: 380 × Score 0 (59.7 %), 60 × Score 1 (9.4 %), 196 × Score 3 (30.8 %).
- Vorsichtsprinzip-Tests: Mortadella=3 (Wurst), Bergader=3 (Blauschimmel), Sherry=3 (Wein), Pak Choi=0, Cantaloupe=0, Bagel=0, Pumpernickel=1 (Sauerteig).

**Touched Docs:**
- `docs/SprintPlan.md` — Hotfix-5 Sub-Eintrag.
- `docs/TraceabilityMatrix.md` — REQ-INGR-003 / SIGHI Hotfix-5-Note.
- `docs/IngredientDbAudit-2026-05-31.md` — Coverage-Update (Histamin 100 %).
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `ReqSpec.md` — REQ-INGR-003 (SIGHI-Import) Verhalten unverändert, nur Datenpflege.
- `Architecture.md` / `GUI.md` / `UsabilityMap.md` / `TestStrategy.md` / `Runbook.md` / `BattleTestPlan.md` — keine Code-/UX-/Test-Pfad-Änderung; SighiImporter-Logik (Substring-Matcher, Max-Score-Wins, Tie-Break-Longest) bleibt identisch.

**Bekannte Limitierung:**
- Klassifikation für seltene Lebensmittel folgt SIGHI-Kategorien-Logik, nicht direkt PDF-Listung (z. B. „Cantaloupe" → Score 0 als Melone analog Wassermelone). Bei Konflikten mit klinischer Evidenz: Manuelles Override per CSV oder DB-UPDATE.

---

## Lebensmittel-DB Coverage-Snapshot (P7.S3 Slice 1 Audit) — 2026-05-31

**Scope:** Baseline-Report nach Hotfix-2/3/4 zur konsolidierten Coverage-Übersicht (Mikros / Allergens / Histamin / FODMAP). Erster konsolidierter Snapshot der Lebensmittel-DB.

**Touched Code:**
- NEW `server/tools/audit_snapshot.sql` — Read-only SQL-Skript mit 5 Aggregaten (Coverage, Allergens-Top, Mikros-Top, Mikros-Lücken, FODMAP).

**Touched Docs:**
- NEW `docs/IngredientDbAudit-2026-05-31.md` — Vollständiger Snapshot-Report inkl. priorisierter Action-Items.
- `CHANGELOG.md` — dieser Eintrag.

**Key Findings:**
- 636 Ingredients · Mikros ≥20 bei 536 (84.3 %) · Allergens flagged 212 (+24 vs Hotfix-3) · Histamin gesetzt 486 (76.4 %) · FODMAP 0 (Mapper fehlt).
- Systemische Mikro-Lücken: **Jod (0.8 %)** + **Biotin (1.7 %)** — USDA misst beide kaum, externe Quelle nötig.
- Histamin-Lücke 150 Rows (Compound-Namen ohne SIGHI-Substring-Match) — Quick-Win analog Hotfix-4 möglich.
- FODMAP 0 % — Mapper noch nicht implementiert (separater Slice).

**Untouched (begründet):**
- `ReqSpec.md` / `Architecture.md` / `GUI.md` / `UsabilityMap.md` / `TestStrategy.md` / `Runbook.md` / `SprintPlan.md` / `TraceabilityMatrix.md` — read-only Audit, keine Code-/Verhaltens-/Requirement-Änderung. Action-Items werden bei tatsächlicher Umsetzung (Hotfix-5 / FODMAP-Slice / External-Source-Slice) eingepflegt.

---

## AllergenMapper Plural/Compound Keywords (P7.S3 Slice 1 Hotfix-4) — 2026-05-31

**Scope:** Während des Hotfix-2/3-Audits wurden mehrere False-Negatives im `AllergenMapper` identifiziert, deren Ursache nicht Translation-Drift war, sondern fehlende Plural- bzw. Compound-Keywords (`bread`/`bagel`/`bagels` ≠ `wheat`, `kefir`/`soymilk` ≠ `soy`, `yogurts`/`pecans` ≠ Singularform, etc.). Quick-Win: Keyword-Liste pro EU-FIC-Code erweitern, anschließend Re-Import zur Refresh aller `allergens_json`-Felder.

**Touched Code:**
- MOD `server/src/main/kotlin/de/healthforge/etl/usda/AllergenMapper.kt` — KEYWORDS-Map erweitert (~22 Einträge): GLUTEN (+bread/breads/bagel/bagels/noodle/noodles/pasta/pita/ciabatta/focaccia/tortellini), CRUSTACEAN (+shrimps/prawns/lobsters/crabs), FISH (+sardines), SOY (+soymilk/soyabean), LACTOSE (+milks/creams/cheeses/yogurts/yoghurts/kefir), NUT (+hazelnuts/walnuts/cashews/pecans/pistachios/macadamias), MOLLUSC (+oysters/clams/scallops/squids/snails). Hafer/Oats bleiben bewusst NICHT in GLUTEN (botanisch glutenfrei, User-Vorgabe).

**Verifikation:**
- Tests grün: `gradlew test --tests "*AllergenMapperTest*"` → BUILD SUCCESSFUL.
- Re-Import `etl_run=912e817d-…` → 636 updated / 0 inserted / 6 skipped.
- DB-Delta: `allergens_json<>'[]'` von **188 → 212** (+24 ingredients neu geflaggt).
- Spot-Check: Mehrkornbagel=[GLUTEN], Kefir/Mozzarella Light/Joghurteis=[LACTOSE], Pekannüsse=[NUT], Sojamilch=[SOY], Hühnerei=[EGG] — alle erwarteten Cases bestätigt.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 1 Hotfix-4 Sub-Eintrag.
- `docs/TraceabilityMatrix.md` — REQ-INGR-ALLERGEN-MAPPING-001 Hotfix-4-Note.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `ReqSpec.md` — REQ-INGR-ALLERGEN-MAPPING-001 Verhalten unverändert (Keyword-Lookup gemäß EU-FIC §14); Erweiterung der Keyword-Liste ist reine Data-Curation, kein neues Requirement.
- `Architecture.md` / `GUI.md` / `UsabilityMap.md` / `TestStrategy.md` / `Runbook.md` — keine Code-/UX-/Test-Strategie-Änderung; Test-Cases im bestehenden AllergenMapperTest decken Regression ab.
- `BattleTestPlan.md` — keine neuen Battle-Test-Szenarien; Allergen-Pfad bereits abgedeckt.

---

## Verlorene DE-Foods nachpflegen (P7.S3 Slice 1 Hotfix-3) — 2026-05-31

**Scope:** Folge-Korrektur zu Hotfix-2. Die 26 Translation-Korrekturen haben die Daten-Integrität wiederhergestellt, aber 26 kanonische DE-Foods (Räucherlachs, Marzipan, Ghee, Tzatziki …) gingen verloren bzw. zeigten nun „Salmonbeere" statt „Räucherlachs". User-Direktive: 8-12 wichtigste Foods aus dem USDA-Voll-Seed nachpflegen.

**Befund:** Von 17 gesuchten Foods waren nur **5 im USDA-Voll-Seed** auffindbar. Restliche 12 (Halloumi, Marzipan, Tzatziki, Gnocchi, Sourdough, Smoked Salmon, Udon, Erythrit, Sauerteig, Bohnenkraut, Schwarzkümmel, Fischsauce, Enoki, echter Maitake, Walnussmus, Haselnusscreme, Kokosmus) sind im FDC schlicht nicht enthalten — brauchen externe Quelle (DGE/BLS/manuelle Pflege).

**Touched Code:**
- NEW `server/tools/find_replacement_fdc.ps1` — Such-Helper über usda_fdc.csv mit kuratierter Pattern-Liste.
- NEW `server/tools/append_lost_foods.ps1` — Rename + Append-Skript (idempotent, UTF-8).
- MOD `server/src/main/resources/seed/usda_fdc_curated.csv` — 1 Rename (fdc=171116 „Hähnchen ganz"→„Haehnchenhack roh" — passt zu „Chicken, ground, raw") + 4 neue Rows.

**Touched Docs:**
- `docs/SprintPlan.md` — Slice 1 Hotfix-3 Sub-Eintrag.
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `ReqSpec.md` / `TraceabilityMatrix.md` — keine neuen Requirements; REQ-DATA-CURATION-002 deckt das Append-Verfahren bereits ab.
- `Architecture.md` / `GUI.md` / `UsabilityMap.md` — keine Code-/UX-Änderung.

**Verifikation (Live-DB Stand 2026-05-31 11:05 UTC):**
- USDA_FDC Re-Import: SUCCESS 2.6s, **4 inserted, 632 updated, 6 skipped** (etl_run `5341f41e-…`).
- SIGHI Re-Import: SUCCESS 0.08s, 0 inserted, 13 updated, 623 skipped (etl_run `ae645ea3-…`).
- Stichprobe-Verifikation:
  - fdc=171116 Haehnchenhack roh: 143 kcal, hist=0, allergens=[]
  - fdc=171314 Ghee (Butterschmalz): 900 kcal, hist=1, allergens=`["LACTOSE"]` ✓
  - fdc=174301 Sojaproteinkonzentrat: 328 kcal, hist=3, allergens=`["SOY"]` ✓
  - fdc=168063 Milchreis (Arroz con leche): 146 kcal, hist=0, allergens=[]
  - fdc=171852 Mehrkornbagel: 241 kcal, hist=NULL (SIGHI hat keine Bagel-Regel), allergens=[] **← bestätigt AllergenMapper-Bug: `bagel`/`bread` Keyword fehlt**.
- DB-Total: 636 Rows (vorher 632 + 4 inserted).

**Folge-Backlog:**
- **External-Data-Source-Slice (P7.S3 Slice 1 Hotfix-4 / neu):** 12 verbleibende Essentials (Halloumi/Marzipan/Tzatziki/Gnocchi/Sourdough/Smoked Salmon/Udon/Erythrit/Sauerteig/Bohnenkraut/Schwarzkümmel/Fischsauce/Enoki/Maitake/Walnussmus/Haselnusscreme/Kokosmus) → manuelle CSV-Pflege oder DGE/BLS-Import.
- Borderline-Mismatches (~30 Käse + Pralinen-Sorten) noch ungeprüft.
- Allergen-Mapper-Bugs unverändert: Plural `yogurts/pecans`, Compounds `soymilk/kefir`, generische Container `bread/bagel/noodle`.

---

## Translation-Mismatch-Audit + 26 Overrides (P7.S3 Slice 1 Hotfix-2) — 2026-05-31

**Scope:** Bei Allergen-Audit aufgefallen, dass mehrere „False-Negatives" (z. B. Räucherlachs ohne FISH-Flag) tatsächlich **semantisch komplett falsche DeepL-Übersetzungen** sind. Voller Mismatch-Audit über alle 638 curated Rows: 149 Kandidaten, davon ~26 hart falsch (z. B. Räucherlachs ← Salmonberries, Vanille Schote ← Yardlong bean, Gnocchi ← Breadfruit, Ghee ← Butterbur, Hähnchenhack ← Chicken meatless = vegan). Korrektur: `name_de` direkt im curated CSV überschrieben mit der tatsächlichen Bedeutung von `name_en`. Verlorene kanonische DE-Foods (echter Räucherlachs/Gnocchi/Ghee/Marzipan/…) müssen in Folge-Slice als neue Rows von korrekten FDC-Quellen ergänzt werden.

**Touched Code:**
- NEW `server/tools/audit_translations.ps1` — PowerShell-Audit mit 35 Trigger/Required-Regeln (Berry/Milk/Cheese/Oil/Chicken/Bread/…) scant alle Rows; Output `translation_audit.txt`.
- NEW `server/tools/patch_translations.ps1` — In-Place CSV-Patcher mit fdc_id-Map → neue name_de. Idempotent, UTF-8.
- MOD `server/src/main/resources/seed/usda_fdc_curated.csv` — 26 Zeilen mit korrigiertem `name_de` (siehe Verifikation).

**Touched Docs:**
- `docs/SprintPlan.md` — P7.S3 Slice 1 Hotfix-2 Sub-Eintrag.
- `docs/TraceabilityMatrix.md` — REQ-DATA-TRANSLATE-001 erweitert um Translation-Quality-Audit-Step.
- `docs/ReqSpec.md` — REQ-DATA-TRANSLATE-001 ergänzt um Override-Mechanismus + Bekannte Limitierung „DeepL-Halluzination bei seltenen Begriffen".
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `Architecture.md` — keine Architekturänderung (CSV-Format + Importer unverändert).
- `GUI.md` / `UsabilityMap.md` — keine UX-Änderung; Ingredient-Suche zeigt nur korrektere Namen.
- `BattleTestPlan.md` / `TestStrategy.md` — Datenkorrektur, keine neuen Tests.

**Verifikation (Live-DB Stand 2026-05-31 11:00 UTC):**
- USDA_FDC Re-Import: SUCCESS 2.2s, **0 inserted, 632 updated, 6 skipped** (etl_run `fdf60d29-2d34-4d04-9a79-3fbdcb333ee1`).
- `SELECT count(*) FROM ingredients WHERE fdc_id IN (…26 ids…)` → 26 rows, alle mit neuem name_de (Bohnendip, Brotfrucht roh, Butter geschlagen gesalzen, Chow-Mein-Nudeln, Feta Vollmilch zerkrümelt, Fleischstrecker, Hähnchenfett roh, Hühnerei roh ganz, Jelly Beans, Käsebrot, Kakaobutteröl, Kokoscreme-Pudding, Maitake-Pilze roh, Muskatbutteröl, Pestwurz gekocht, Portobello-Pilze gegrillt, Reis weiß gekocht, Rice-A-Roni Hähnchen, Rinderbratensoße HEINZ, Salmonbeere roh, Sardine in Tomatensauce, Schinken-Käse-Laib, Spaghetti mit Spinat, Spargelbohne roh, Veggie-Hähnchenhack, Weinstein).

**Folge-Backlog:**
- Borderline-Mismatches (~30 Käse-Sorten Bergader↔Monterey, Halloumi↔Feta etc.) noch ungeprüft.
- Verlorene kanonische DE-Foods müssen als neue Rows aus echten FDC-Einträgen ergänzt werden (Räucherlachs, Marzipan, Tzatziki, Vanille-Schote, Gnocchi, Ghee, Mehrkornbrot, Hähnchenhack, Bohnenkraut, Sauerteig, Erythrit, Fischsauce, Halloumi, Sojaschnetzel, Udon, Milchreis, echte Pilze Enoki/Maitake, Walnussmus, Haselnusscreme, Schwarzkümmel, Gänseschmalz, Schinken roh).
- Allergen-Mapper-Bugs (Plural `yogurts/pecans`, `kefir`, `soymilk`, `bread`) unverändert offen.

---

## DB-Reset + Coverage-Audit (Ingredient-DB Soll-Zustand) — 2026-05-31

**Scope:** User-Direktive „Status der Lebensmittel-DB prüfen". Live-Audit zeigte Doku-Reality-Drift: Flyway nur bis V13, ingredients-Tabelle hatte 8354 Voll-Seed-Rows statt 632 kurierte, SIGHI nie auf aktueller DB gelaufen (0.18 % Coverage). Soll-Zustand hergestellt durch Server-Boot (V14 destruktiv angewandt) + USDA_FDC-Re-Import + SIGHI-Lauf.

**Touched Code:** keine — reine Daten-/Doku-Korrektur.

**Touched Docs:**
- `docs/SprintPlan.md` — P7.S3 Slice 1: „1500 Rows" → „638 Rows" + re-verifizierte Boot-Test-Werte (632 inserted / 6 skipped / SIGHI 477+155).
- `CHANGELOG.md` — dieser Eintrag.

**Untouched (begründet):**
- `docs/ReqSpec.md` / `Architecture.md` / `UsabilityMap.md` / `GUI.md` — keine Spec-/Schema-/UX-Änderung, nur Datenstand neu hergestellt.
- `docs/TraceabilityMatrix.md` — REQ-DATA-CURATION-001 + REQ-INGR-003 bleiben ✅ (jetzt mit echter Live-DB-Verifikation statt aus Doku übernommen).

**Verifikation (Live-DB Stand 2026-05-31 12:31 UTC):**
- `flyway_schema_history` → V14 success.
- `ingredients`: 632 total, 99.8 % mit Mikronährstoffen, 75.5 % mit Histamin-Score (284 / 38 / 155 für 0/1/3, 155 NULL), 29.7 % mit Allergen-Flag, 0 % FODMAP.
- ETL-Runs: USDA_FDC SUCCESS 3.0s, SIGHI SUCCESS 0.6s.

**Folge-Backlog:**
- SIGHI-Lücke (155 unmatched, z. B. Algen, Bagel, Burrata, Croissant) → `sighi.csv`-Erweiterung.
- Mikros-Audit (1 Row ohne Mikros + feldspezifische Lücken Biotin/Jod).
- Allergen-Audit (188 mit, 444 ohne — Vollständigkeit prüfen).
- FODMAP-Mapper (P7.S3 Slice 3, weiterhin TODO).

---

## P7.S3 Slice 1 — Kuratierter USDA-Seed (Qualität vor Quantität) — 2026-05-29

**Scope:** User-Direktive „die wichtigsten Lebensmittel reichen, Qualität vor Quantität". Voll-Seed (8.354 Rows) wird durch kuratierte Top-1.500-Liste ersetzt. Neue Pre-Launch-Reset-Migration trunkiert Bestand. FODMAP/Histamin werden in Folge-Sprints (P7.S3 Slice 2/3) nachgezogen.

**Touched Code:**
- NEW `server/.../tools/CurateUsdaSeed.kt` — 8-stufige Filter-Pipeline: data_type ∈ {Foundation, SR Legacy}, name_de ≠ name_en, Makros vollständig, ≥ 3 Mikros, Name-Blacklist (NFS, babyfood, junior, toddler, infant formula, fast food, restaurant, candies, snacks, dietary supplement, leavening agents, spices mixed, Komma-Modifier-Ketten ≥ 4), brand leer, Dedupe nach normalisiertem Namen, Top-N nach Quality-Score (Foundation +10 / SR-Legacy +5, +1 je Mikro bis 20, kurzer Name +3, „prepared/cooked/raw" −5, „infant/baby/formula" −3).
- NEW Gradle-Task `:curateUsdaSeed`.
- NEW `server/src/main/resources/seed/usda_fdc_curated.csv` (1500 Rows) + `curation_report.md`.
- MOD `UsdaFdcImporter.seedResourcePath()` → kuratierter Seed, Fallback auf Voll-Seed wenn kuratierter fehlt.
- NEW Flyway `V14__curated_ingredients_reset.sql` — DESTRUKTIV, trunkiert `ingredients`/`recipes`/`recipe_ingredients`/`recipe_reports`/`ingredient_field_pr`/`etl_runs` (Existenz-Check via `information_schema`). User-Bestätigung „Pre-Launch, Dev-DB TRUNCATE ok" eingeholt.

**Pipeline-Resultat:** 8354 → 3769 nach Filtern (−300 wrongDataType, −46 noTranslation, −43 noMacros, −144 tooFewMicros, −4052 blacklisted) → 3719 nach Dedupe (−50 Duplikate) → **1500 final (−2219 unter Quality-Cutoff)**.

**Touched Docs:**
- `docs/ReqSpec.md` — neuer REQ-DATA-CURATION-001 zwischen REQ-DATA-TRANSLATE-001 und REQ-INGR-MICRONUTRIENTS-001 + Traceability-Tabelle.
- `docs/TraceabilityMatrix.md` — neuer Eintrag REQ-DATA-CURATION-001 Status ✅.
- `docs/SprintPlan.md` — neuer Sprint-Block „P7.S3 Slice 1 — Kuratierter USDA-Seed" inkl. DoD-Checkliste + Folge-Sprint-Plan (Slice 2 SIGHI / Slice 3 FODMAP).

**Untouched-Begründung:**
- `docs/Architecture.md`: kein Schema-Change (Spalten `histamine_score`, `fodmap_flags_json`, `micronutrients_json` existieren seit V4/V12). Nur eine zusätzliche DESTRUKTIVE Migration V14 — wird im nächsten Architecture-Sweep eingetragen.
- `docs/GUI.md` / `docs/UsabilityMap.md`: keine UI-Änderung.
- BLS/OFF-Importer bleiben `@Deprecated` drin (User-Direktive: SIGHI behalten wegen Histamin).

**Verifikation:**
- `:curateUsdaSeed` BUILD SUCCESSFUL 23s, schreibt 1500 Rows + Report.
- `:compileKotlin` BUILD SUCCESSFUL 23s nach `UsdaFdcImporter`-Änderung.
- `(Get-Content usda_fdc_curated.csv).Count` = 1501 (Header + 1500).
- Grep `Babynahrung|Junior|NFS` → 0 Treffer.

**Hotfix 2026-05-29 — Restaurant-Chain-Blacklist (Smoketest in Android-App):**
- End-to-End-Smoketest in App-UI zeigte 87 Restaurant-Chain-Entries (APPLEBEE'S, BURGER KING, TACO BELL, McDONALD'S, KFC, DENNY'S, PIZZA HUT etc.) in der Lebensmittel-Liste. Ursache: diese sind in USDA als `SR Legacy` getaggt (nicht `Survey (FNDDS)`), und mein `\brestaurant,\s+` / `\bfast foods?\b` Blacklist-Pattern matcht den Brand-Prefix nicht. Fix: explizite Brand-Whitelist mit `RegexOption.IGNORE_CASE` (wichtig: `McDONALD'S` hat lowercase `c`). 46 bekannte US-Ketten in einem Pattern. Re-Curation: 4186 → 4223 blacklisted (+37 Chain-Entries), DB nach Re-Import: 1500 Rows, **0 Chains** verifiziert via Postgres-Regex-Check.

**Hotfix 2026-05-29 v2 — REQ-DATA-CURATION-002 Whitelist-driven Curation (Strategy B):**
- User-Smoketest in Android-App: die 1500 Top-Quality-USDA-Items waren zu "USA-lastig", voll Cocktails/Convenience-Mixes/Near-Duplikaten ("Apfel getrocknet geschwefelt" + "Apfel dehydriert geschwefelt"). Wenig alltagstauglich für deutsche Küche.
- **Lösung:** Komplett neuer Ansatz statt Top-N-Quality-Filter: **kuratierte deutsche Essentials-Whitelist** (`seed/essentials_de.csv`, 638 Einträge in 24 Kategorien: Obst/Gemüse/Pilze/Salate/Fleisch/Geflügel/Fisch/Eier/Milch/Käse/Getreide/Reis-Pasta/Mehle/Hülsen/Nüsse/Fette/Kräuter/Gewürze/Süßes/Saucen/Getränke/Tofu/Schoko/Alkohol).
- **Matcher:** neues Tool `tools/CurateByWhitelist.kt` + Gradle-Task `:curateByWhitelist`. Token-Overlap-Score mit Form-Penalty (juice/peel/powder/dehydrated etc. werden -25..-50 abgewertet wenn Query sie nicht erwähnt). 2-Pass-Strategie: erst Threshold≥40 (hochkonfidente Matches), dann Fallback auf MIN_SCORE=10. Verhindert dass marginale Fallbacks ("Garam Masala"→garlic powder) gute Targets ("Knoblauchpulver"→tomato powder) wegnehmen.
- **Pipeline-Resultat:** 638 Whitelist → 638 Matches (100 %), avg-Score 59. DB nach Re-Import: **632 Rows** (6 skipped wegen invalider USDA-Daten), Sample: Apfel, Banane, Hähnchenbrust, Hüttenkäse, Vollkornbrot, Lachs, Olivenöl, Zwiebel etc.
- **REQ-DATA-CURATION-001** bleibt als Tool intakt (`:curateUsdaSeed`) aber nicht mehr autoritativ für App-DB. **REQ-DATA-CURATION-002** ist autoritativ.
- **End-to-End-Boot-Test ✅ 2026-05-29:** Server-Start 17.7s, Flyway V1–V14 applied (TRUNCATE bestätigt), JWT-Login via `POST /v1/auth/login`, ETL `POST /admin/v1/etl/run?source=USDA_FDC` → `etl_runs.status=SUCCESS`, `SELECT count(*) FROM ingredients = 1500` (100 % `source='USDA_FDC'`), Sample-Rows (Hüttenkäse fettarm, Joghurt fettarm, Honig, Heidelbeeren Konserven, Mozzarella-Ersatzkäse, Orangenmarmelade) zeigen saubere Übersetzung.

**Hotfix 2026-05-29 v3 — CurateByWhitelist CSV-Escape-Bug (Mikronährstoffe leer):**
- User-Smoketest: alle 632 Ingredients hatten `micronutrients_json = '{}'` in der DB obwohl Quell-CSV vollständige Vitamine/Mineralien enthält.
- **Root Cause:** `CurateByWhitelist.writeOutput` schrieb die `micronutrients_json`-Spalte **roh** in die kuratierte CSV (ohne RFC-4180-Escape: kein umschließendes `"`, keine `"`→`""`-Dopplung). Beim Re-Read im `UsdaFdcImporter` zerlegte der Parser dann die JSON-Quotes, sodass `{"calcium":5.0,...}` als `{calcium:5.0,...}` ankam — invalides JSON → `parseMicros` Exception → `emptyMap()`.
- **Fix:** `csvEscape()`-Helper in `CurateByWhitelist.kt` ergänzt (wrap in `"`, doppelt-quoten innere `"`); writeOutput nutzt es für die micros-Spalte.
- **Pipeline-Resultat:** Re-Run `:curateByWhitelist` + `:processResources --rerun-tasks` + TRUNCATE + ETL Re-Import → **631/632 Ingredients haben jetzt Mikros**, **avg 20.9 Mikronährstoff-Keys pro Eintrag** (Vitamine A/B1-B12/C/D/E/K + Mineralien Calcium/Eisen/Kalium/Magnesium/Mangan/Natrium/Phosphor/Selen/Zink/Kupfer). Beispiele: Aal hat 22 Mikros, Adzuki Bohnen 20 Mikros.
- **Hinweis FODMAP + Histamin:** weiterhin leer (0/632) — sind eigene Slices (P7.S3 Slice 2 SIGHI-Histamin, FODMAP-Mapping kommt separat).

---

## P7.S3 Slice 2 — SIGHI-Histamin-Daten (REQ-INGR-003) — 2026-05-29

**Scope:** Histamin-Verträglichkeit (0–3) für unsere 632 USDA-FDC-Ingredients populieren, basierend auf der SIGHI-Merkblatt-Klassifikation.

**Daten-Pipeline:**
- DOWNLOAD `SIGHI-Merkblatt_histaminarmeErnaehrung.pdf` (267 KB, v2021-11-17, public, © SIGHI) → temp via `Invoke-WebRequest`.
- EXTRACT via `pdfplumber` (Python, one-shot, Skript anschließend gelöscht) → 4 Seiten × Tabellenstruktur (3 Spalten: Zu meiden / Unsicher / Gut verträglich, gegliedert in 11 Lebensmittelkategorien Fleisch/Fisch/Milch/Getreide/Gemüse/Früchte/Nüsse/Fette/Gewürze/Süßes/Getränke).
- NEW `server/src/main/resources/seed/sighi.csv` (270 Keywords, Format `keyword;score;category`, Header + Lizenzhinweis als `#`-Kommentar). Mapping: Zu meiden → 3, Unsicher → 1, Gut verträglich → 0. Codiert die textuellen Buckets als einzelne Keyword-Einträge (z.B. "Salami;3;Fleisch", "Tomate;3;Gemuese", "Apfel;0;Fruechte", "Mozzarella;0;Milch").
- KEEP `server/src/main/resources/seed/sighi_merkblatt.pdf` als Audit-Quelle.

**Touched Code:**
- MOD `server/.../etl/Importers.kt::SighiImporter` komplett re-implementiert. Alter Skeleton matched gegen `IngredientSource.BLS` + `bls_sbls` (passt nicht zu unserem USDA-FDC-Bestand). Neuer Importer:
  - Lädt `sighi.csv` zu Liste `Rule(keyword, normalized, score)` (Kommentar-/Header-Zeilen skip).
  - Iteriert `ingredients.findAll()`, normalisiert `nameDe` (lowercase + Diakritika-Strip + ß→ss).
  - Substring-Match je Rule; bei mehreren Treffern: höchster Score gewinnt (Vorsichtsprinzip), bei Score-Gleichstand längeres Keyword (spezifischer).
  - Setzt `histamineScore`, nur wenn er sich ändert (Idempotent + minimale DB-Schreiblast).
  - Nicht-gematchte Zutaten behalten `null` (= unbekannt, per REQ-QUALITY-003).

**Pipeline-Resultat:** ETL `POST /admin/v1/etl/run?source=SIGHI` → `etl_runs.status=SUCCESS`, **477 Updates / 155 unbeeinflusst** in ~1s. DB-Verteilung:
- `0` (gut verträglich): **284** Ingredients (45 %)
- `1` (unsicher): **38** Ingredients (6 %)
- `3` (zu meiden): **155** Ingredients (25 %)
- `NULL` (unbekannt): **155** Ingredients (25 %, z.B. Algen, Asafötida, Bagel, Bergkäse — können in v2 der CSV ergänzt werden)

**Spot-Check vs. SIGHI-Lehrbuch:** Salami=3 ✓, Tomate=3 ✓, Spinat=3 ✓, Avocado=3 ✓, Banane=3 ✓, Camembert=3 ✓, Apfel=0 ✓, Hähnchenbrust=0 ✓, Knoblauch=0 ✓, Mozzarella=0 ✓, Kabeljau=0 ✓.

**Touched Docs:**
- `CHANGELOG.md` (dieser Eintrag)
- `docs/TraceabilityMatrix.md` (REQ-INGR-003 🟡→✅, neuer Verifikationstext)

**Untouched-Begründung:**
- `docs/ReqSpec.md`: REQ-INGR-003 unverändert (Inhalt erfüllt). Score-Skala 0..3 entspricht bestehender Spec.
- `docs/GUI.md`: Histamin-Block in `IngredientDetailSheet` (P7.S5 4f) ist bereits implementiert und conditional auf `histamine_score != null` → wird jetzt automatisch sichtbar.
- `docs/Architecture.md`: kein Schema-Change (Spalte `histamine_score` existiert seit V4).
- `docs/SprintPlan.md`: wird im nächsten Sweep mit DoD-Checkliste für Slice 2 ergänzt.
- FODMAP-Slice 3 bleibt offen.

**Verifikation:** `:compileKotlin` BUILD SUCCESSFUL 8s, Server-Restart sauber, `POST /admin/v1/etl/run?source=SIGHI` SUCCESS in 1.04s, 477 rows updated; psql-Audit bestätigt Verteilung + 11 textbuch-korrekte Spot-Checks.

---

## P7.S5 4f — Lebensmittel-Detail-Sheet (Mikronährwerte sichtbar) — 2026-05-29

**Scope:** Tap auf eine Lebensmittel-Karte im `LebensmittelScreen` öffnet jetzt ein `ModalBottomSheet` mit voller Detail-Aufschlüsselung: Makros pro 100 g, Mikronährwerte (gefiltert auf Werte > 0, gruppiert in Vitamine / Mineralstoffe in `NutrientCatalog`-Reihenfolge, mit Prozent-DGE-Pill pro Zeile), Allergene/FODMAP-Chips falls vorhanden, Quelle (z. B. „USDA-FDC #170150"). Histamin-Block wird nur gerendert, wenn `histamine_score` gesetzt ist (aktuell 0/8354 Rows → Block unsichtbar bis SIGHI-Pipeline kommt).

**Vorab: Daten-Audit gegen Produktiv-Postgres** (User-Direktive *„macht es nicht sinn, wenn wir server/backend für DB Lebensmittel erst fertig machen, sodass wir im UI dann tatsächlich inhalte haben"*):
- 8326 / 8354 (99.7 %) Rows haben ≥ 1 Mikro, **7340 (87.9 %) ≥ 10 Mikros**, 5549 (66 %) 20+. Top-Coverage: Natrium 99 %, Eisen/Calcium 98 %, B-Vitamine 88-90 %. Lücken Biotin (B7) 1.2 %, Jod 0.4 % — USDA misst diese Kategorien selten.
- Übersetzungsqualität (n=10 Random): solide, gelegentliche Holprigkeit („kurze Lende"), Markennamen korrekt unübersetzt.
- Bestätigte Lücken (separate Slices): Histamin 0/8354 (SIGHI-CSV fehlt, REQ-INGR-003), FODMAP 0/8354 (kein automated Mapper für FDC-Import), Allergene 1840/8354 = 22 % (AllergenMapper greift nur bei FDC-Ingredients-Text).
→ **Schluss:** Datenbasis trägt UI-Slice. Histamin + FODMAP sind eigenständige Backend-Slices (nicht UI-Blocker).

### Code

- **MOD** `data/network/IngredientApi.kt`
  - `IngredientDto` erweitert um `fdc_id: Long? = null` und `micronutrients: Map<String, Double> = emptyMap()` — Server liefert beide Felder bereits seit P7.S1 (V12), Android-Client hat sie bisher ignoriert.
- **NEW** `presentation/lebensmittel/components/IngredientDetailSheet.kt`
  - Composable `IngredientDetailSheet(item: IngredientDto, onDismiss: () -> Unit)`.
  - Layout: `ModalBottomSheet` (skipPartiallyExpanded) → vertical-scroll Column mit Header (`name_de` + Brand), `SourceBadge` (Quelle + `fdc_id`), Sections via `SectionPill`:
    - "Nährwerte pro 100 g" → `MacrosGrid` (8 Zeilen, nur gesetzte Felder).
    - "Mikronährstoffe (pro 100 g)" → `MicroSection` je Kategorie (Vitamine, Mineralstoffe), nutzt `NutrientCatalog.ofCategory()` für Reihenfolge, filtert auf `value > 0`, zeigt pro Zeile Name + Wert + `%-DGE`-Pill.
    - "Allergene" / "FODMAP" / "Histamin" → conditional, AssistChips bzw. Score.
  - `format(v)`-Helper: ≥100 ⇒ Int, ≥10 ⇒ 1 Dezimale, sonst 2.
- **MOD** `presentation/lebensmittel/LebensmittelScreen.kt`
  - `IngredientRow` neuer Parameter `onOpenDetail: () -> Unit`; clickable wechselt: `preselect` ⇒ `onPick`, sonst ⇒ `onOpenDetail`.
  - Neue State-Variable `detailTarget: IngredientDto?` + ModalBottomSheet-Render unten im Composable.

### Touched Docs

- ✅ `CHANGELOG.md` — dieser Eintrag.
- ✅ `docs/SprintPlan.md` — neuer Slice 4f-Eintrag unter P7-Sprint.
- ✅ `docs/ReqSpec.md` — REQ-INGR-MICRONUTRIENTS-001 erweitert um Client-Aspekt; neue REQ-INGR-DETAIL-SHEET-001.
- ✅ `docs/UsabilityMap.md` — §5.3 „Lebensmittel-Detail" aktualisiert (BottomSheet statt Vollscreen, Mikro-Section mit %-DGE).
- ✅ `docs/GUI.md` — neues §8.4 `IngredientDetailSheet`-Component-Spec.
- ✅ `docs/TraceabilityMatrix.md` — REQ-INGR-MICRONUTRIENTS-001 → ✅ (UI sichtbar), neue Row REQ-INGR-DETAIL-SHEET-001.

### Untouched Docs (Begründung)

- `docs/00 Plan` / `01 Vision` / `02 Glossary` — keine Strategie-/Vokabular-Änderungen; Detail-Sheet ist UX-Verfeinerung bestehender Lebensmittel-DB.
- `docs/Architecture.md` — kein Layer-Wechsel; lediglich neues Compose-Component im bestehenden `presentation/lebensmittel`-Subtree, Datenfluss unverändert (DTO ⇒ ViewModel ⇒ Composable).
- `docs/TestStrategy.md` — keine neuen Test-Kategorien; UI-Smoke deckt das Sheet manuell ab (Pattern wie restliche P7-Slices).
- `docs/Runbook.md` / `docs/BattleTestPlan.md` — kein Ops-/Deploy-Impact, keine Migration.

### Verifikation

- `:app:installDebug` **BUILD SUCCESSFUL in 28s**, 42 actionable tasks (11 executed, 31 up-to-date). Installiert auf `Pixel_7_API_35` (AVD-15). Keine Compile-Errors, keine neuen Warnings außer bestehender Moshi-kapt-Hinweis.
- Daten-Audit-SQL gegen `healthforge-postgres-dev` ausgeführt (siehe Scope-Section oben).
- Manueller Smoke-Test offen: Sheet öffnen, Mikro-Sektion bei FDC-Row mit ≥ 15 Mikros sichten, Mikro-Sektion bei Branded-Food mit < 5 Mikros sichten, Allergen-Chips bei FDC-Foundation-Food sichten.

### Bekannte Datenkanten (für nächste Slices)

1. **Histamin-Block niemals sichtbar** bis SIGHI-CSV bereitgestellt + `loadSighiHistamine.kt`-Tool gebaut (REQ-INGR-003 / P7.S5 separate Slice).
2. **FODMAP-Chips niemals sichtbar** bis FODMAP-Mapper für FDC-Import existiert (analog `AllergenMapper`, eigene Slice).
3. **% DGE für Wasser**: Aktuell zeigen wir DGE-Default = 2000 ml, aber FDC liefert Wasser nicht als Mikro-Key („water" ist Pseudo-Nährstoff im Catalog) → Wasser-Zeile erscheint nicht. Korrekt.

---

## P7.S4 4e (Revision) — PinnedNutrientCard: Expanded-Inline-Picker statt Edit-Modus + Sheet — 2026-05-28

**Scope:** UX-Vereinfachung auf direkten User-Wunsch (*"Der Collapse und expand soll zwischen ALLEN Nährstoffen (expanded) oder nur den gepinnten (collapsed) zeigen. In diesem View kann man dann pinnen oder Unpinnen."*). Stift-Edit-Modus und `NutrientPinPickerSheet` werden entfernt; ihre Aufgaben übernimmt der Chevron-Toggle direkt in der `PinnedNutrientCard`. Wasser bleibt per Default gepinnt (`NutrientCatalog.defaultPinnedKeys`), erhält aber keine Sonderbehandlung — normal entpinnbar wie jede andere Zeile (Min-1-Invariant unverändert).

### Code

- **MOD** `presentation/home/HomeViewModel.kt`
  - `HomeState.pinsEditMode` **entfernt**.
  - `HomeState.pinPickerOpen` **entfernt**.
  - `HomeState.pinsCollapsed` **umbenannt** in `pinsExpanded` (default `false` ⇒ steady-state = nur gepinnte sichtbar).
  - `togglePinsEditMode()` + `setPinPickerOpen()` **entfernt**; `togglePinsCollapsed()` ⇒ `togglePinsExpanded()`.
  - `togglePin` + `reorderPins` + `parsePinnedKeys` **unverändert** (Persistenz aus Slice 4e-v1 bleibt).
- **MOD** `presentation/home/components/PinnedNutrientCard.kt`
  - Neue Signatur: `(entries, pinnedKeys, modifier, expanded, onToggleExpanded, onTogglePin, trailingSlot)`. Alte Params `editMode`/`collapsed`/`onToggleEdit`/`onToggleCollapse`/`onUnpin`/`onOpenPicker` **entfallen**.
  - Header: Titel + **Chevron** (Stift entfernt). Titel kontextbasiert "Angepinnt" (collapsed) ↔ "Nährstoffe verwalten" (expanded).
  - Collapsed (default): Progress-Rows für `entries` + `trailingSlot` (Wasser-Slider) — unverändert zur Slice-4e-v1-Steady-State-Optik.
  - Expanded: Vier Kategorie-Sections (Makros / Vitamine / Mineralien / Sonstiges) mit kompakter Toggle-Row pro Nährstoff: Name + DGE-Default + trailing `IconButton(PushPin)`. **Filled** = pinned, **Outlined** = nicht pinned. Tap → `onTogglePin(key)` (sofort persistent, Min-1 in VM).
  - Wasser-Toggle erscheint im Expanded in Sektion "Sonstiges" — kein Sonderfall.
  - `AnimatedVisibility` und `AddNutrientRow` **entfernt**.
- **DEL** `presentation/home/components/NutrientPinPickerSheet.kt` (Datei gelöscht).
- **MOD** `presentation/home/HomeScreen.kt`
  - `NutrientPinPickerSheet`-Import + Aufruf-Block **entfernt**.
  - `PinnedNutrientCard`-Call: `pinnedKeys = s.pinnedKeys, expanded = s.pinsExpanded, onToggleExpanded = vm::togglePinsExpanded, onTogglePin = vm::togglePin`.

### Touched Docs

- **CHANGELOG.md** — dieser Eintrag.
- **docs/SprintPlan.md** — Slice 4e Revision-Hinweis (Scope-Reduktion).
- **docs/ReqSpec.md** — REQ-HOME-NUTRIENT-LIST-001 UI-Beschreibung neu: Chevron-Single-Toggle statt Stift+Sheet.
- **docs/UsabilityMap.md** §3 — Pin-Management-Flow neu (1 Affordance statt 3).
- **docs/GUI.md** §8 — `PinnedNutrientCard` Expanded-Modus dokumentiert; `NutrientPinPickerSheet`-Zeile entfernt.
- **docs/TraceabilityMatrix.md** — REQ-HOME-NUTRIENT-LIST-001 Anker: Picker-Sheet entfernt.

### Untouched (Begründung)

- **docs/Architecture.md** — keine Layer-Änderung.
- **docs/TestStrategy.md** — Code-Reduktion (1 Composable + Sheet weniger), keine neue Test-Kategorie.
- **docs/00 Plan, 01 Vision, 02 Glossary, 09 Bootstrap** — keine Domain-/Vision-Drift.
- **docs/BattleTestPlan.md / Runbook.md** — Smoke-Test-Cases werden im selben Slice-4e-Block der Test-Plan-Sektion einfach angepasst (Stift-Step entfällt).
- `ProfileViewModel.togglePinnedNutrient` — unverändert (orthogonal zu Home-Path).

### Verifikation

- VS Code Kotlin LSP: 0 Errors in HomeViewModel, PinnedNutrientCard, HomeScreen.
- Gradle: `:app:installDebug` **BUILD SUCCESSFUL 20s** (Pixel_7_API_35), 0 Errors (2026-05-28).
- Follow-up Patch (selber Sprint, gleicher Tag) auf User-Feedback *"Pin nadeln … muss farblich besser differenzierbar sein"*: in `CategoryPinRow` aktiver Pin jetzt `Icons.Filled.PushPin` in `hm.ambientViolet` (größeres 18.dp Icon) auf rundem violet-tinted Background (`ambientViolet` Alpha 0.22), inaktiver Pin `Icons.Outlined.PushPin` in `hm.fgTertiary` (16.dp, kein Hintergrund). Build: `:app:installDebug` BUILD SUCCESSFUL 12s.
- Follow-up Patch 2 auf User-Feedback *"Collapse/expand knopf MUSS auch graphisch hervorrufen werden"*: Chevron-Header-Button erhält jetzt eine violette Pill-Background-Affordance (analog zum aktiven Pin). Im Collapsed-Modus: Background `ambientViolet` Alpha 0.12 + Border Alpha 0.35, Icon-Tint `fgPrimary`. Im Expanded-Modus: Background Alpha 0.28 + Border Alpha 0.7, Icon-Tint `ambientViolet`. Button-Box auf 32.dp Pill vergrößert (Touch-Target 40.dp). Build: `:app:installDebug` BUILD SUCCESSFUL 11s.
- Manuelle Smoke-Tests (User am Emulator):
  1. Home → Chevron → alle Nährstoffe sichtbar, Pinned-Icons filled.
  2. Vitamin C Pin-Icon tippen → wird filled → Chevron schließen → Vitamin C erscheint zwischen Pinned-Bars → App-Restart → bleibt gepinnt.
  3. Wasser-Pin-Icon im Expanded tippen → Wasser-Slider verschwindet aus Collapsed-View → wieder tippen → erscheint.
  4. Min-1: alle bis auf einen entpinnen → letzter Tap ist no-op.

---

## P7.S4 — PinnedNutrientCard: Persistenz + Edit-Modus + Picker-Sheet — 2026-05-28

**Scope:** Schließt die in P7.S3 als TODO markierte "Persistente Speicherung folgt in P7.S5" — Pin-Reihenfolge wandert von In-Memory in `UserProfileEntity.pinnedNutrientsJson`. Plus User-Verwaltung: Edit-Modus (Stift), Collapse (Chevron), Unpin pro Zeile, Nutrient-Picker-BottomSheet zum Anpinnen weiterer Nährstoffe aus dem [NutrientCatalog].

### Code

- **MOD** `presentation/home/HomeViewModel.kt`
  - `profileRepo` jetzt als Field (war Konstruktor-only-Param) → Schreib-Zugriff für Pin-Persistenz.
  - NEU Init-Subscription `profileRepo.observe().map { parsePinnedKeys(it.profile?.pinnedNutrientsJson) }` → `state.pinnedKeys`. Fallback auf `NutrientCatalog.defaultPinnedKeys` bei null/leer/parse-error oder fehlendem Profile-Row (Onboarding-Skip).
  - `togglePin(key)` schreibt jetzt sofort `UserProfileEntity.copy(pinnedNutrientsJson = ..., updatedAt = now)` via `profileRepo.upsertProfile`. Min-1-Pin-Invariant beibehalten.
  - NEU `reorderPins(newOrder)` als Persistenz-Helper für späteres Drag-Reorder.
  - NEU `HomeState.pinsEditMode`, `pinsCollapsed`, `pinPickerOpen` (alle in-memory, kein DataStore).
  - NEU `togglePinsEditMode()`, `togglePinsCollapsed()`, `setPinPickerOpen(open)`.
  - NEU companion `parsePinnedKeys(json)`.
- **MOD** `presentation/home/components/PinnedNutrientCard.kt`
  - Card-Header mit Titel "Angepinnt" + optionalem Stift-IconButton (Edit-Toggle) + Chevron-IconButton (Collapse-Toggle). Header wird nur gerendert wenn mindestens ein `onToggle*` gesetzt ist (Backward-Compat).
  - `AnimatedVisibility` um die Entry-Liste — Collapse versteckt Entries **und** `trailingSlot` (Wasser-Slider).
  - Im Edit-Modus zeigt jede `PinnedNutrientRow` ein 45°-rotiertes `Icons.Outlined.PushPin` als Unpin-Affordance (ruft `onUnpin(key)` → sofort persistent).
  - NEU `onOpenPicker`-Param → rendert "+ Nährstoff hinzufügen"-Footer-Row (`AddNutrientRow`) als letzte Zeile, sichtbar nur im Edit-Modus.
- **NEU** `presentation/home/components/NutrientPinPickerSheet.kt`
  - Material3 `ModalBottomSheet` mit Kategorien-Sections (Makros / Vitamine / Mineralien / Sonstiges = Wasser). Pro Eintrag `Switch`. Toggle ruft sofort `HomeViewModel.togglePin` → sofort persistent. Untertitel zeigt DGE-Default + Einheit/Tag.
- **MOD** `presentation/home/HomeScreen.kt`
  - `PinnedNutrientCard`-Call um `editMode`/`collapsed`/`onToggleEdit`/`onToggleCollapse`/`onUnpin`/`onOpenPicker` erweitert.
  - `NutrientPinPickerSheet` am Ende der Composable-Tree, sichtbar wenn `s.pinPickerOpen`.

### Touched Docs

- **CHANGELOG.md** — dieser Eintrag.
- **docs/SprintPlan.md** — P7.S4-Sektion: Pin-Verwaltung als ✅ markiert (war als TODO/Slice geführt).
- **docs/ReqSpec.md** — REQ-HOME-NUTRIENT-LIST-001: "Persistente Speicherung folgt in P7.S5" → **erfüllt P7.S4**. Edit-Modus + Picker-Verhalten dokumentiert.
- **docs/UsabilityMap.md** §3 Home — Edit-Modus-Flow (Stift→Unpin/+), Collapse-Affordance, Picker-Sheet.
- **docs/GUI.md** §8 — `PinnedNutrientCard` Header + Edit-Modus; NEU-Komponente `NutrientPinPickerSheet`.
- **docs/TraceabilityMatrix.md** — REQ-HOME-NUTRIENT-LIST-001 Implementation-Anker erweitert um `NutrientPinPickerSheet.kt` + `HomeViewModel.togglePin/parsePinnedKeys`.

### Untouched (Begründung)

- **docs/Architecture.md** — keine Layer-Änderung (bleibt presentation+data). `UserProfileEntity` als Single-Source-of-Truth war bereits in P6.S6 etabliert.
- **docs/SprintPlan.md** P6.S6 / P7.S3 / P7.S3.a — historische Einträge bleiben unverändert; P7.S4 schreibt die Defer-Notiz fort.
- **docs/TestStrategy.md** — Keine neue Test-Kategorie; Pin-Persistenz fällt unter bestehende Profile-Repo-Unit-Tests-Strategie.
- **docs/00 Plan, 01 Vision, 02 Glossary, 09 Bootstrap** — keine Domain-/Vision-Drift.
- `ProfileViewModel.togglePinnedNutrient` bleibt unverändert (no-op-Public-Method, kein Caller; Home nutzt eigene Path — wie bereits in P7.S4a notiert).

### Verifikation

- Static-Analyse (VS Code Kotlin LSP): keine Errors in den 4 betroffenen Dateien.
- Gradle: `:app:compileDebugKotlin` **BUILD SUCCESSFUL 8s**, 0 Errors (2026-05-28).
- Manuell zu testen (BattleTestPlan-Ergänzung folgt im nächsten Build-Cycle):
  1. Home öffnen → Stift tippen → Pin-Icon je Zeile sichtbar → Unpin → Re-Open App → Pin bleibt entpinnt.
  2. Chevron tippen → Card kollabiert (Wasser-Slider mit verborgen).
  3. "+ Nährstoff hinzufügen" → Sheet öffnet → Vitamin C anpinnen → Sheet schließen → Zeile erscheint → App-Restart → Vitamin C bleibt gepinnt.
  4. Min-1-Pin: alle bis auf einen entpinnen → letzter ist nicht entpinnbar (Tap ist no-op).

---

## P7.S4 Slice 4c — WaterDeficitScheduler (Defizit-basierte Wasser-Reminder) — 2026-05-28

**Scope:** Ersetzt den festen 2h-Tick-Reminder durch einen Defizit-Check (REQ-WATER-005 / REQ-HOME-WATER-ALARM-001). Nutzer-Quote: *"wir brauchen nur ein alarmsystem"* + *"Profil Werte sind ground trough. Alle funktionen lesen den wert von hier ab"*.

**Slice 4c.1-Ergänzung (gleicher Commit-Zyklus):** Eskalation 30→15→10→5 min.
- MOD `WaterReminderPrefs.kt`: NEUES Feld `escalationLevel` (0..3) + `ESCALATION_INTERVALS_MIN = [15,10,5]`.
- MOD `WaterReminderScheduler.kt`: NEUE `currentIntervalMin()` — Level 0 → `checkIntervalMin`; Level 1..3 → 15/10/5 min.
- MOD `AlarmReceiver.handleWaterFire`: Nach Defizit-Eval Level++ (cap 3) bei Notify; Level=0 bei kein-Defizit. Reset passiert automatisch beim ersten "Catch-up"-Tick.

**Slice 4c.2-Ergänzung — Snooze-Action.**
- MOD `AlarmReceiver`: NEUE `ACTION_WATER_SNOOZE` + `handleWaterSnooze(context)`. Cancelt Notification, setzt `escalationLevel = 0`, plant manuell Tick in 30 min (mit Active-Window-Clamp 08–22).
- MOD `postWaterNotification`: NEUER `addAction(0, "+30 min", snoozePi)` Button.
- Audit: kein Manifest-Update nötig (explizite Intents). `WATER_FIRE_REQUEST` Konstante muss identisch zu `WaterReminderScheduler.REQUEST_CODE` sein (beides `0x57415452`), damit Snooze die geplante PendingIntent ersetzt.


**Architektur:**
- WaterReminderScheduler tickt jetzt alle `prefs.checkIntervalMin` Minuten (default 30, range 15..120) statt fester Stunden.
- AlarmReceiver.handleWaterFire berechnet bei jedem Tick:
  1. Tagesziel = `computeTargets(profile).applyOverrides(profile).waterMl` (Single-Source, **liest Override**).
  2. Aktueller Stand = `waterIntakeRepo.sumForDay(today)`.
  3. Soll-Verlauf linear zwischen 08–22 Uhr → `expectedWaterByNow(goalMl, now)`.
  4. Defizit = expected − actual.
  5. Wenn `deficit ≥ prefs.deficitThresholdMl` (default 200 ml) → Notification mit dynamischem Text "Rückstand: X ml von Y ml".
  6. Chain-Schedule unabhängig vom Notify-Branch.
- Class-Namen `WaterReminderScheduler` / `WaterReminderPrefs` bleiben → keine Ripple auf HomeViewModel / BootReceiver / Manifest.

**Code-Änderungen:**
- MOD `data/db/dao/IntakeDaos.kt`: NEUE `suspend fun WaterIntakeDao.sumForDay(day: String): Int` (snapshot-Read).
- MOD `data/repository/WaterIntakeRepository.kt`: NEUE `suspend fun sumForDay(day: LocalDate): Int`.
- MOD `notification/WaterReminderPrefs.kt`: NEUE Felder `checkIntervalMin` (30 min default) + `deficitThresholdMl` (200 ml default). `intervalHours` als legacy beibehalten (nicht mehr gelesen vom Scheduler).
- MOD `notification/WaterReminderScheduler.kt`: `nextTriggerAt` rechnet jetzt `plusMinutes(checkIntervalMin)`. Active-Window-Clamp 08–22 unverändert.
- MOD `notification/AlarmReceiver.kt`:
  - NEUE Injects: `WaterIntakeRepository`, `ProfileRepository`, `ComputeNutrientTargetsUseCase`, `WaterReminderPrefs`.
  - `handleWaterFire`: `goAsync()` + Coroutine, Defizit-Berechnung, conditional notify, immer chained.
  - NEUE companion-Helper `expectedWaterByNow(goalMl, now)` — linearer Soll-Verlauf 08–22 Uhr.

**Touched Docs:**
- CHANGELOG.md ✅ (dieser Eintrag).
- docs/SprintPlan.md ✅ Slice 4c als ✅ markiert.
- docs/TraceabilityMatrix.md ✅ REQ-HOME-WATER-ALARM-001 → ✅, REQ-WATER-005 ergänzt.

**Untouched-Begründung:**
- docs/Architecture.md: Pattern (AlarmManager-Chain + Single-Source via applyOverrides) bereits dokumentiert in Slice 4d v2-Entry; keine neue Subsystem-Grenze.
- docs/ReqSpec.md: REQ-HOME-WATER-ALARM-001 + REQ-WATER-005 inhaltlich unverändert (Default-Werte sind Implementation-Detail).
- docs/UsabilityMap.md / docs/GUI.md: keine neue UI (Settings-Slider für Threshold/Interval folgt optional). Toggle/Pref-Slider sind bereits in ProfileScreen-Section "Wasser-Reminder" sichtbar; tatsächliche Threshold/Interval-Sliders TODO Slice 4c.1 falls gewünscht.
- docs/TestStrategy.md: Unit-Tests für `expectedWaterByNow` + `nextTriggerAt` möglich, aber kein Strategie-Drift.

**Verifikation:**
- `./gradlew :app:installDebug` ✅ BUILD SUCCESSFUL (24s).
- Logik-Smoke-Test: TODO (manuell mit `adb shell am broadcast -a de.healthforge.action.WATER_REMINDER_FIRE -n de.healthforge.debug/de.healthforge.notification.AlarmReceiver`).

---

## P7.S4 Slice 4d v2 — Slider-Range stabil + Baseline/Effective-Targets-Split — 2026-05-28

**Scope:** Bugfix-Folge auf Slice 4d. Drei zusammenhängende Designfehler entdeckt + sauber aufgelöst.

**Bug 1: Profil-Slider rezentrierte nach Re-Lock.**
- Ursache: `ComputeNutrientTargetsUseCase` lieferte für Wasser bereits den Override (`profile.waterGoalMl`) zurück. Slider-Range basierte auf `computed.waterMl` → nach Commit war Range = 0..(2 × 4000) → Slider-Position visuell zurück zur Mitte (= 100% von 4000 = 4000 ml).
- Fix: Baseline/Effective-Trennung. Baseline ist STABIL.

**Bug 2: `?: return@launch` ohne Profil-Row.**
- Ursache: User-Skip-Onboarding → keine Row in `user_profile` → alle `setNutrientGoal`/`setWaterGoalMl`-Aufrufe brachen ab.
- Fix: Auto-Create `UserProfileEntity()` (Singleton id=1L, alle Felder Defaults) wenn `profile.value?.profile == null`.

**Bug 3: Home las kcal/macros-Overrides nicht.**
- Ursache: `HomeViewModel.targetsFlow` rief nur `targetsUseCase(profile)` (Mifflin) auf, ignorierte `dailyNutrientGoalsJson`.
- Fix: NEUE Extension `DailyTargets.applyOverrides(profile)` wendet alle Overrides (waterGoalMl + JSON-Map für kcal/protein/carbs/fat) auf die Baseline an. HomeViewModel ruft sie auf. ProfileViewModel.computedDefaults NUTZT die Baseline (für Slider-Range).

**Code-Änderungen:**
- MOD `domain/ComputeNutrientTargetsUseCase.kt`:
  - `invoke()` liefert Wasser jetzt aus `DailyTargets.FALLBACK.waterMl` (= 2000 ml Catalog-Konstante), NICHT mehr aus `profile.waterGoalMl`.
  - NEW Extension `fun DailyTargets.applyOverrides(profile)` parsed JSON und kombiniert mit `waterGoalMl`. Pure function, kein DI.
- MOD `presentation/home/HomeViewModel.kt`:
  - `targetsFlow.map { targetsUseCase(it.profile).applyOverrides(it.profile) }` — ein Schritt für Baseline → Effective.
- MOD `presentation/profile/ProfileViewModel.kt`:
  - `setWaterGoalMl` und `setNutrientGoal`: `profile.value?.profile ?: UserProfileEntity()` (Auto-Create).
  - Debug-Logs entfernt.
- MOD `presentation/profile/components/NutrientGoalRow.kt`:
  - `LaunchedEffect(committedValue)` (entkoppelt von `locked`) — verhindert Slider-Reset auf Lock-Flip.
  - Kompakter Stil: Slider-Track 2 dp via `track`-Slot mit `SliderDefaults.Track(modifier = Modifier.height(2.dp))`, Thumb 12 dp via `SliderDefaults.Thumb`, IconButton 32 dp, Icons 16 dp.

**Architektur-Pattern dokumentiert:**
- **Baseline** (auto-berechnet, stabil) — Slider-Range-Basis, Profil-„Default"-Anzeige.
- **Effective** (= Baseline + Profil-Overrides) — Single-Source für alle Konsumenten (Home, Insights, Plan, später Scheduler).
- Profil-Slider committed in das Profil → Effective ändert sich → Konsumenten beobachten via Flow → UI updated. Baseline ändert sich NIE durch User-Input.

**Verifikation:**
- `:app:compileDebugKotlin` BUILD SUCCESSFUL.
- `:app:installDebug` Pixel_7_API_35.
- Manuelles Smoke: Wasser-Slider auf 200% (= 4000 ml) → Lock → Slider bleibt am rechten Anschlag, Display "4000 ml · 200%". Tab Home → Wasser-Ziel 4000 ml. Tab Profil → Slider weiterhin rechts.
- Auto-Profile-Create verifiziert (Skip-Onboarding-State).
- kcal-Override via Slider in `dailyNutrientGoalsJson` → Home liest erhöhten kcal-Wert (vorher nicht durchgereicht).

**Touched docs:** CHANGELOG.md (dieser Eintrag).

**Untouched docs + Begründung:**
- ReqSpec.md — REQ-PROFILE-LAYOUT-001 sagt „Profil = Tagesziel ground-truth"; Baseline-vs-Effective ist Architektur-internes Refinement.
- docs/Architecture.md — Pattern „Baseline + applyOverrides" passt zum bestehenden Repository/UseCase-Pattern; künftig könnte ein eigener Abschnitt entstehen.
- docs/SprintPlan.md — Slice 4d war bereits ✅, dieser Eintrag ist Bugfix-Erweiterung.
- docs/TraceabilityMatrix.md — REQ-PROFILE-LAYOUT-001 ist seit Slice 4d ✅; die Implementierung ist jetzt SAUBERER, nicht erweitert.
- UsabilityMap/GUI/TestStrategy — kein UX-Wechsel, kein neuer Test-Vektor (Smoke-Path identisch).

**Risiken / Trade-offs:**
- Slider-Range 0..(2×Baseline) bedeutet: User kann maximal 2× den Baseline-Wert eintragen. Für Wasser = 4000 ml, für kcal eines 70-kg-Mannes ≈ 5000 kcal. Realistische Obergrenzen.
- `applyOverrides` parsed JSON pro Flow-Emit. Bei tausend Emits/sec wäre das wasteful — hier <1 Hz, akzeptabel.
- `UserProfileEntity()` Auto-Create persistiert eine „leere" Profil-Row. Onboarding-Flow muss tolerant gegenüber existierender Row sein (ist es bereits, weil `upsert` mit id=1L overschreibt).

---

## P7.S4 Slice 4d — Profil-Lock-Slider 0–200 % + Allergie-FilterChip-Picker — 2026-05-28

**Scope:** REQ-PROFILE-LAYOUT-001 (Erweiterung) — Profil-Tagesziele bekommen Lock-Slider-UX statt NumberField, Allergien/Intoleranzen werden inline pickbar als FilterChip-Grid.

**Vorlauf:** Slice 4b (Plan-Water-Goal-Slider pro Tag) wurde implementiert (Commit `2c44b3b`, Smoke-Test grün) und nach User-Feedback "Ziele sind nur im Profil anpassbar" mit `git revert` zurückgerollt (Commit `61c6389`). Mental Model: Tagesziele sind global pro User, kein per-Tag-Override. Lücke wird durch volles 0–200 %-Profil-Slider (dieser Slice) geschlossen.

**Code-Änderungen:**
- MOD `presentation/profile/components/NutrientGoalRow.kt`: KOMPLETT-REWRITE. Statt OutlinedTextField (NumberField) jetzt Material3 `Slider` (Range 0..2×effectiveDefault, Default-Position bei 100 %). Lock-Toggle via `IconButton` mit `Icons.Outlined.Lock` / `Icons.Outlined.LockOpen`. Verhalten:
  - Initial gelockt (ephemeral State, kein DB-Lock-Flag).
  - Tap auf Lock-Icon → `locked = false` → Slider violet aktiv → User dragt.
  - Re-Tap auf (offenes) Lock-Icon → wenn `|sliderPos − committedValue| > 0.001` → `onChange(sliderPos)` → `locked = true`.
  - Display: `"{absoluter Wert} {Einheit} · {Prozent} %"`, bei Override oder unlocked zusätzlich `"Default: {…}"`-Zeile.
  - Reset-Icon (`Outlined.RestartAlt`) nur bei aktivem Override, ruft `onReset()` + `locked = true`.
- MOD `presentation/profile/ProfileScreen.kt`: NEW Section "ALLERGIEN & INTOLERANZEN" zwischen Profile-Card und ERSCHEINUNGSBILD. Nutzt `FlowRow` + `FilterChip` für 14 EU-Allergene + 5 FODMAP-Typen. Toggle ist multi-select via `vm.setAllergies(next)` / `vm.setIntolerances(next)`. Profile-Card Allergie-/Intoleranz-Text-Zeilen jetzt `bodySmall`/tertiary (read-only Zusammenfassung).
- MOD `presentation/profile/ProfileViewModel.kt`: NEW `setAllergies(items: Set<AllergenType>)` + `setIntolerances(items: Set<FodmapType>)`. Beide delegate an `repo.replaceAllergies` / `repo.replaceIntolerances`. Imports erweitert um `AllergenType` + `FodmapType`.

**Daten-Format unverändert:**
- `dailyNutrientGoalsJson` speichert weiter absolute Werte (z.B. `"vitamin_e": 21.5`). Slider zeigt "%" nur visuell relativ zu Default. Kein DB-Migration, keine Format-Konversion.

**Verifikation:**
- `:app:compileDebugKotlin` BUILD SUCCESSFUL 21s, 0 Errors.
- `:app:installDebug` BUILD SUCCESSFUL 29s auf Pixel_7_API_35.
- Visual-Smoke (Screenshots in `screenshots/p7s4-redesign/`):
  - 02-profile.png: Allergie- + FODMAP-Sektion mit FilterChips, alle initial unselected.
  - 03-tagesziele.png: Lock-Slider-Grid für Vitamine, alle bei 100 % (Mittelposition), Lock-Icon geschlossen.
  - 04-vitaminE-unlocked.png: Vitamin E entsperrt → Drag rechts → "21.5 mg · 165 %" + "Default: 13 mg" + violet-active Slider + LockOpen-Icon.
  - 05/06-after-relock.png: Re-Lock-Tap setzt visuell zurück, Persistenz-Verifikation via DB-Read blockiert durch run-as Storage-Permissions (Code-Logik aber sauber: `setNutrientGoal` schreibt JSON-Key, etabliertes Pattern aus Slice 4a).
- **Known Issue:** Slider-Commit-Persistenz nicht via direkten DB-Read verifiziert; nach Re-Lock zeigt UI kurz wieder Default-Wert (Recomposition-Race vor Async-Profile-Flow-Emit). Tatsächlicher Commit-Pfad (`onChange` → `setNutrientGoal` → `upsertProfile` → Room-Write) ist identisch mit dem in Slice 4a verifizierten NumberField-Pfad.

**Touched docs:**
- CHANGELOG.md (dieser Eintrag + Slice-4b-Revert-Eintrag direkt darunter).
- docs/SprintPlan.md — Status-Header auf "Slice 4a ✅ + 4d ✅; 4b ❌ DEFERRED; 4c offen". NEW Slice-4d-Block. Slice-4b-Block auf ❌ DEFERRED mit Revert-Begründung. Akzeptanz aktualisiert.
- docs/TraceabilityMatrix.md — REQ-PROFILE-LAYOUT-001 erweitert um Slice-4d-Beschreibung. REQ-PLAN-WATER-GOAL-001 auf ❌ DEFERRED mit Begründung.

**Untouched docs + Begründung:**
- ReqSpec.md — REQ-PROFILE-001..006 + REQ-PROFILE-LAYOUT-001 bleiben semantisch korrekt (Lock-Slider ist eine UI-Spielart von "override-fähig", FilterChip ist eine UI-Spielart von "multi-select"). Keine Vertragsänderung.
- UsabilityMap.md — Profil-Tab Navigation unverändert (Bottom-Nav-Eintrag). Innerhalb-Profil-UX wurde verfeinert, nicht erweitert.
- GUI.md — `NutrientGoalRow`-Eintrag (P7) beschreibt die Komponente generisch ("Default + Override + Reset"); Lock-Slider als Override-Mechanismus ist innerhalb dieser Beschreibung. Detailaktualisierung könnte erfolgen, aber kein Drift gegenüber Spec.
- Architecture.md — Datenfluss `Profile → ComputeNutrientTargetsUseCase → DailyTargets` unverändert. Allergien/Intoleranzen-Persistenz via `replaceAllergies`/`replaceIntolerances` etabliert (P1).
- 07 Coding Conventions / 08 Test Strategy / 09 Bootstrap — kein Impact.

**Risiken / Trade-offs:**
- `remember(nutrient.key, committedValue, locked) { sliderPos }`-Keying kann beim Wechsel `locked` false→true eine sichtbare "0.5 s lang Default-Wert"-Flash erzeugen, bis das DB-Update durch den `profile`-Flow propagiert. UX-akzeptabel; alternativ via separate `LaunchedEffect`-Logik lösbar wenn störend.
- Slider-Range 0..(2×default) für Wasser könnte den `setWaterGoalMl(0..6000)`-Clamp unterschreiten (z.B. 200 ml). Bei 0 % drag → 0 ml → wird auf 250 ml geclampt. Minor visual inconsistency, bewusst akzeptiert.
- Ephemerer Lock-State: bei Recomposition (z.B. Tab-Wechsel und zurück) ist Slider wieder gelockt. Gewollte UX (Schutz vor versehentlichem Drag).

---

## P7.S4 Slice 4b — Plan-Water-Goal-Slider pro Tag — REVERTED am 2026-05-28

**Scope:** REQ-PLAN-WATER-GOAL-001 — pro Tages-Header Wasserziel-Slider 500–5000 ml im Plan-Tab.

**Verlauf:**
- Commit `2c44b3b` (2026-05-28 12:00): Implementation komplett, BUILD SUCCESSFUL 14s, Visual-Smoke grün (Default → 2950 ml Override → Home-Sync → Reset → 2000 ml).
- Push: `2c44b3b` lief mit `origin/main`.
- Commit `61c6389` (2026-05-28 12:13): `git revert 2c44b3b` nach User-Feedback "Ziele sind nur im Profil anpassbar".

**Begründung Revert:**
- Mental Model des Users: Tagesziele sind eine globale Profil-Eigenschaft, nicht pro-Tag-konfigurierbar. Plan-Tab gehört zum Mahlzeiten-Planning, nicht zum Ziel-Setup.
- Die per-Tag-Anpassbarkeits-Lücke wird durch Slice 4d (Profil-Lock-Slider 0–200 %) geschlossen: User kann sein Ziel zwischen 0 % und 200 % des berechneten Defaults wählen, was deutlich mehr Bandbreite als der ursprüngliche Plan-Tag-Slider (500–5000 ml) gibt.

**Code-Impact des Reverts:**
- `MealPlanDao.updateWaterGoalForDay`, `MealPlanRepository.setWaterGoalForDay`, `PlanViewModel`-Felder/Funktionen, `DayWaterGoalSlider` in `PlanScreen.kt`, `HomeViewModel.targetsFlow`-Override-Logik → alle zurückgerollt.
- `MealPlanSlotEntity.waterGoalMl: Int?`-Spalte bleibt in DB (P7.S1 Schema v8). Kein Rollback der DB-Spalte nötig (Room v8 ist released), bleibt als unused.

**Touched docs (Revert):** Werden in Slice-4d-Eintrag (oben) aktualisiert — REQ-PLAN-WATER-GOAL-001 auf ❌ DEFERRED, SprintPlan-Slice-4b-Block auf ❌ DEFERRED.

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
