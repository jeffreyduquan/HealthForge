-- =================================================================
-- Finde OFF/BLS/USER-Alternativen für MANUAL-Zutaten per Name-Match
-- =================================================================

WITH manual AS (
    SELECT id, name_de, source FROM ingredients WHERE source = 'MANUAL'
),
candidates AS (
    SELECT id, name_de, source, energy_kcal_per_100g
    FROM ingredients WHERE source IN ('OFF', 'BLS', 'SIGHI', 'USER', 'USDA_FDC')
)
SELECT 
    m.name_de AS manual_name,
    m.id AS manual_id,
    c.name_de AS candidate_name,
    c.id AS candidate_id,
    c.source AS candidate_source,
    c.energy_kcal_per_100g
FROM manual m
LEFT JOIN candidates c ON 
    -- Match: first word or exact substring
    LOWER(c.name_de) LIKE '%' || LOWER(SPLIT_PART(m.name_de, ' ', 1)) || '%'
    OR LOWER(m.name_de) LIKE '%' || LOWER(SPLIT_PART(c.name_de, ' ', 1)) || '%'
ORDER BY m.name_de, c.source;
