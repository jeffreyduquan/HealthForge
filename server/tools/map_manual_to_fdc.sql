-- =================================================================
-- MANUAL → USDA_FDC Ingredient Replacement Mapping
-- Findet für jede MANUAL-Zutat die beste USDA_FDC-Alternative
-- =================================================================

WITH manual AS (
    SELECT id, name_de, source FROM ingredients WHERE source = 'MANUAL'
),
-- Für jede MANUAL-Zutat: finde FDC-Zutat mit ähnlichstem Namen
-- (einfaches LIKE auf den ersten Wort-Token)
ranked AS (
    SELECT DISTINCT ON (m.id)
        m.id AS manual_id,
        m.name_de AS manual_name,
        f.id AS fdc_id,
        f.name_de AS fdc_name,
        f.energy_kcal_per_100g AS fdc_kcal,
        similarity(LOWER(m.name_de), LOWER(f.name_de)) AS sim
    FROM manual m
    CROSS JOIN ingredients f
    WHERE f.source = 'USDA_FDC'
      AND similarity(LOWER(m.name_de), LOWER(f.name_de)) > 0.15
    ORDER BY m.id, similarity(LOWER(m.name_de), LOWER(f.name_de)) DESC
)
SELECT 
    manual_name,
    manual_id,
    fdc_name,
    fdc_id,
    fdc_kcal,
    ROUND(sim::numeric, 2) AS similarity
FROM ranked
ORDER BY manual_name;
