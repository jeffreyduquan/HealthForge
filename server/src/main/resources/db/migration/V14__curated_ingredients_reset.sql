-- P7.S3 Slice 1 / REQ-DATA-CURATION-001 — kuratierter Ingredient-Reset.
-- Pre-Launch: existierende Daten dürfen weg, USDA-Importer lädt direkt das
-- kuratierte ~1.500-Foods-Seed (siehe `seed/usda_fdc_curated.csv`).
--
-- WICHTIG: Diese Migration ist DESTRUKTIV. Nach Apply ist die `ingredients`-
-- Tabelle leer. Beim nächsten App-Start läuft `UsdaFdcImporter` und füllt
-- sie aus `seed/usda_fdc_curated.csv` neu. Referenzen aus `recipes` und
-- `ingredient_field_pr` werden mitgelöscht (CASCADE-Verhalten der FKs).
--
-- User-Bestätigung (2026-05-29): "Pre-Launch, Dev-DB TRUNCATE ok"
-- + "Hart löschen + Recipe-Cleanup (riskant)".

-- 1) Abhängige Reports/PRs/Recipes für gelöschte Ingredients aufräumen.
--    Wir lassen die FKs ihre CASCADE-Arbeit machen, müssen aber Tabellen
--    ohne ON DELETE CASCADE manuell leeren.
TRUNCATE TABLE ingredient_field_pr CASCADE;

-- 2) Recipes referenzieren Ingredients über recipe_ingredients (FK CASCADE
--    siehe V6__recipes.sql). Pre-Launch → komplett wegwerfen.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recipe_ingredients') THEN
        EXECUTE 'TRUNCATE TABLE recipe_ingredients CASCADE';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recipe_reports') THEN
        EXECUTE 'TRUNCATE TABLE recipe_reports CASCADE';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'recipes') THEN
        EXECUTE 'TRUNCATE TABLE recipes CASCADE';
    END IF;
END $$;

-- 3) ETL-Run-Historie leeren — damit Importer beim nächsten Boot frisch läuft
--    (Importer entscheidet idempotent via `findByFdcId`, aber sauber ist sauber).
TRUNCATE TABLE etl_runs;

-- 4) Hauptmigration: Ingredients leeren.
TRUNCATE TABLE ingredients CASCADE;
