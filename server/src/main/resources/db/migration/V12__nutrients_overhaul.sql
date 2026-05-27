-- V12 — P7.S1 Big-Nutrition-Refactor (ReqSpec §12, REQ-INGR-MICRONUTRIENTS-001).
--
-- Erweitert die `ingredients`-Tabelle um:
--   * `micronutrients_json` — JSONB-Map<NutrientCatalog.key, Wert pro 100g>
--     (Befüllt durch USDA-FDC-ETL in P7.S2, REQ-DATA-SOURCE-001).
--   * `fdc_id` — USDA FoodData Central ID für idempotenten Re-Sync
--     (REQ-DATA-SOURCE-001).
--
-- Indizes:
--   * UNIQUE auf `fdc_id` (NULL erlaubt für nicht-USDA-Quellen / User-Vorschläge).
--   * GIN auf `micronutrients_json` für künftige Filter-Queries
--     (z.B. "Ingredients mit vitamin_c > X mg/100g").
--
-- Forward-only. Bestehende Rows bekommen Default `'{}'`-JSONB und `NULL` für fdc_id.

ALTER TABLE ingredients
    ADD COLUMN IF NOT EXISTS micronutrients_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE ingredients
    ADD COLUMN IF NOT EXISTS fdc_id BIGINT;

ALTER TABLE ingredients
    DROP CONSTRAINT IF EXISTS uq_ingredients_fdc_id;
ALTER TABLE ingredients
    ADD CONSTRAINT uq_ingredients_fdc_id UNIQUE (fdc_id);

CREATE INDEX IF NOT EXISTS idx_ingredients_fdc_id
    ON ingredients(fdc_id)
    WHERE fdc_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ingredients_micronutrients_gin
    ON ingredients USING gin(micronutrients_json);

COMMENT ON COLUMN ingredients.micronutrients_json
    IS 'P7.S1 / REQ-INGR-MICRONUTRIENTS-001 — Map<NutrientCatalog.key, Wert pro 100g>. Keys gespiegelt aus de.healthforge.domain.nutrition.NutrientCatalog.';
COMMENT ON COLUMN ingredients.fdc_id
    IS 'P7.S1 / REQ-DATA-SOURCE-001 — USDA FoodData Central FDC-ID (NULL für nicht-USDA-Quellen).';
