-- =================================================================
-- Audit: MANUAL Ingredients & betroffene Rezepte
-- Führt aus: docker exec -i healthforge-postgres psql -U $POSTGRES_USER -d $POSTGRES_DB
-- =================================================================

\echo '=== MANUAL-Zutaten ==='
SELECT id, name_de, brand, source, energy_kcal_per_100g, protein_g_per_100g
FROM ingredients
WHERE source = 'MANUAL'
ORDER BY name_de;

\echo ''
\echo '=== Rezepte mit MANUAL-Zutaten ==='
SELECT DISTINCT r.id, r.title, ri.ingredient_id, i.name_de AS ingredient_name
FROM recipe_ingredients ri
JOIN recipes r ON r.id = ri.recipe_id
JOIN ingredients i ON i.id = ri.ingredient_id
WHERE i.source = 'MANUAL'
ORDER BY r.title, i.name_de;

\echo ''
\echo '=== Summary ==='
SELECT 
  (SELECT COUNT(*) FROM ingredients WHERE source = 'MANUAL') AS manual_count,
  (SELECT COUNT(DISTINCT ri.recipe_id) 
   FROM recipe_ingredients ri 
   JOIN ingredients i ON i.id = ri.ingredient_id 
   WHERE i.source = 'MANUAL') AS affected_recipes;
