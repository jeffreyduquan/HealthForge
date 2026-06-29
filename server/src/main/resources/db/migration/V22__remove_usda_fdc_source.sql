-- Clean single source-of-truth: BLS as baseline (+ SIGHI/FODMAP/Allergene via Post-Import Mapping).
-- Entfernt bestehende USDA_FDC-Einträge vollständig und erzwingt neue Constraints ohne USDA_FDC.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'recipe_ingredients') THEN
        DELETE FROM recipe_ingredients
        WHERE ingredient_id IN (SELECT id FROM ingredients WHERE source = 'USDA_FDC');
    END IF;

    DELETE FROM etl_runs WHERE source = 'USDA_FDC';
    DELETE FROM ingredients WHERE source = 'USDA_FDC';
END $$;

ALTER TABLE ingredients
    DROP CONSTRAINT IF EXISTS ingredients_source_check;
ALTER TABLE ingredients
    ADD CONSTRAINT ingredients_source_check
    CHECK (source IN ('BLS','SIGHI','OFF','USER','MANUAL'));

ALTER TABLE etl_runs
    DROP CONSTRAINT IF EXISTS etl_runs_source_check;
ALTER TABLE etl_runs
    ADD CONSTRAINT etl_runs_source_check
    CHECK (source IN ('BLS','SIGHI','OFF'));
