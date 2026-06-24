-- =============================================================================
-- HealthForge — Ingredient-Diagnose & Fix
-- Auf dem VPS ausführen:
--   docker exec -i healthforge-postgres psql -U healthforge -d healthforge < diagnose.sql
-- =============================================================================

\echo '========== 1. Flyway-Migrationsstand =========='
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_on DESC 
LIMIT 5;

\echo '========== 2. Ingredient-Counts =========='
SELECT 
    count(*) AS total,
    count(*) FILTER (WHERE source = 'BLS') AS bls,
    count(*) FILTER (WHERE source = 'USDA_FDC') AS usda,
    count(*) FILTER (WHERE source = 'USER') AS user_submitted,
    count(*) FILTER (WHERE status = 'APPROVED') AS approved,
    count(*) FILTER (WHERE status = 'PENDING') AS pending,
    count(*) FILTER (WHERE status = 'REJECTED') AS rejected
FROM ingredients;

\echo '========== 3. Letzte ETL-Runs =========='
SELECT source, status, started_at, finished_at, rows_inserted, rows_updated, rows_skipped, error_message
FROM etl_runs 
ORDER BY started_at DESC 
LIMIT 5;

\echo '========== 4. Stichprobe: erste 5 Ingredients =========='
SELECT id, name_de, source, status, created_at 
FROM ingredients 
ORDER BY name_de 
LIMIT 5;
