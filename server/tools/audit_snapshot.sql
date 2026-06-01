\echo === Coverage Snapshot ===
WITH micro_counts AS (
  SELECT id, (SELECT count(*) FROM jsonb_object_keys(micronutrients_json)) AS n FROM ingredients
)
SELECT
  (SELECT count(*) FROM ingredients) AS total,
  (SELECT count(*) FROM micro_counts WHERE n >= 1)  AS micros_ge1,
  (SELECT count(*) FROM micro_counts WHERE n >= 10) AS micros_ge10,
  (SELECT count(*) FROM micro_counts WHERE n >= 20) AS micros_ge20,
  (SELECT count(*) FROM ingredients WHERE allergens_json <> '[]') AS allergens_flagged,
  (SELECT count(*) FROM ingredients WHERE histamine_score IS NOT NULL) AS hist_set,
  (SELECT count(*) FROM ingredients WHERE histamine_score = 0) AS hist0,
  (SELECT count(*) FROM ingredients WHERE histamine_score = 1) AS hist1,
  (SELECT count(*) FROM ingredients WHERE histamine_score = 3) AS hist3,
  (SELECT count(*) FROM ingredients WHERE fodmap_flags_json <> '[]') AS fodmap_flagged;

\echo === Allergens Top-Codes ===
SELECT code, count(*) AS n
FROM ingredients i, jsonb_array_elements_text(i.allergens_json::jsonb) AS code
GROUP BY code ORDER BY n DESC;

\echo === Mikros Top-Keys (count of ingredients carrying that key) ===
SELECT k, count(*) AS n
FROM ingredients, jsonb_object_keys(micronutrients_json) k
GROUP BY k ORDER BY n DESC LIMIT 30;

\echo === Mikros Low-Coverage Keys (Lücken) ===
SELECT k, count(*) AS n
FROM ingredients, jsonb_object_keys(micronutrients_json) k
GROUP BY k ORDER BY n ASC LIMIT 15;

\echo === FODMAP Distribution ===
SELECT fodmap_flags_json, count(*) AS n FROM ingredients GROUP BY fodmap_flags_json ORDER BY n DESC LIMIT 10;
