-- P7.S2 Slice 3c: USDA-FDC als Single-Source-of-Truth (REQ-DATA-SOURCE-001).
-- Erweitert die CHECK-Constraints auf `ingredients.source` und `etl_runs.source`
-- um den Wert `USDA_FDC`. BLS/OFF/SIGHI bleiben für historische Zeilen erhalten,
-- sind aber per Code-Pfad deprecated (siehe Importers.kt @Deprecated).

ALTER TABLE ingredients
    DROP CONSTRAINT IF EXISTS ingredients_source_check;
ALTER TABLE ingredients
    ADD CONSTRAINT ingredients_source_check
    CHECK (source IN ('BLS','SIGHI','OFF','USER','MANUAL','USDA_FDC'));

ALTER TABLE etl_runs
    DROP CONSTRAINT IF EXISTS etl_runs_source_check;
ALTER TABLE etl_runs
    ADD CONSTRAINT etl_runs_source_check
    CHECK (source IN ('BLS','SIGHI','OFF','USDA_FDC'));
