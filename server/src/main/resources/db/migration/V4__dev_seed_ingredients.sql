-- HealthForge — dev seed ingredients for P1 smoketest.
-- Sprint P1.S5/P1.S4.1. Idempotent via ON CONFLICT.
-- 15 realistic German foods covering most allergens / FODMAPs to exercise filters.
--
-- NOTE: nutrient values are illustrative round numbers (NOT to be cited);
-- proper BLS/SIGHI/OFF ETL will replace these in P2.

INSERT INTO ingredients (
    name_de, brand, source, source_id,
    energy_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, sugar_g_per_100g,
    fat_g_per_100g, satfat_g_per_100g, fiber_g_per_100g, salt_g_per_100g,
    histamine_score, allergens_json, fodmap_flags_json, locked
) VALUES
  ('Apfel',                NULL,  'MANUAL', 'dev-apfel',        52,  0.3, 14.0, 10.4, 0.2, 0.0, 2.4, 0.00, 0, '[]',                          '["FRUCTOSE"]',          true),
  ('Banane',               NULL,  'MANUAL', 'dev-banane',       89,  1.1, 23.0, 12.0, 0.3, 0.1, 2.6, 0.00, 1, '[]',                          '["FRUCTANS"]',          true),
  ('Vollkornbrot',         NULL,  'MANUAL', 'dev-vk-brot',     247,  8.4, 41.0,  3.5, 3.3, 0.5, 7.4, 1.30, 1, '["GLUTEN"]',                  '["FRUCTANS"]',          true),
  ('Weizenmehl Type 405',  'Aurora','MANUAL','dev-weizen-405', 348, 10.6, 71.0,  0.7, 1.0, 0.2, 4.0, 0.01, 0, '["GLUTEN"]',                  '["FRUCTANS"]',          true),
  ('Vollmilch 3,5%',       NULL,  'MANUAL', 'dev-vm-35',        64,  3.4,  4.8,  4.8, 3.5, 2.2, 0.0, 0.10, 0, '["MILCH"]',                   '["LACTOSE"]',           true),
  ('Naturjoghurt',         NULL,  'MANUAL', 'dev-joghurt',      61,  3.5,  4.7,  4.7, 3.2, 2.0, 0.0, 0.10, 2, '["MILCH"]',                   '["LACTOSE"]',           true),
  ('Erdnussbutter',        NULL,  'MANUAL', 'dev-erdnussb',    588, 25.0, 20.0,  9.2,50.0, 9.0, 6.0, 1.00, 1, '["ERDNUESSE"]',               '[]',                    true),
  ('Lachs (gegart)',       NULL,  'MANUAL', 'dev-lachs',       208, 20.0,  0.0,  0.0,13.0, 3.0, 0.0, 0.10, 3, '["FISCH"]',                   '[]',                    true),
  ('H\u00fchnerei',        NULL,  'MANUAL', 'dev-ei',          155, 13.0,  1.1,  1.1,11.0, 3.3, 0.0, 0.30, 1, '["EIER"]',                    '[]',                    true),
  ('Tomate',               NULL,  'MANUAL', 'dev-tomate',       18,  0.9,  3.9,  2.6, 0.2, 0.0, 1.2, 0.01, 2, '[]',                          '[]',                    true),
  ('Zwiebel',              NULL,  'MANUAL', 'dev-zwiebel',      40,  1.1,  9.3,  4.2, 0.1, 0.0, 1.7, 0.00, 0, '[]',                          '["FRUCTANS"]',          true),
  ('Knoblauch',            NULL,  'MANUAL', 'dev-knoblauch',   149,  6.4, 33.0,  1.0, 0.5, 0.1, 2.1, 0.02, 1, '[]',                          '["FRUCTANS"]',          true),
  ('Reis (gekocht)',       NULL,  'MANUAL', 'dev-reis',        130,  2.7, 28.0,  0.1, 0.3, 0.1, 0.4, 0.00, 0, '[]',                          '[]',                    true),
  ('Sojadrink',            'Alpro','MANUAL','dev-sojadrink',    33,  3.3,  0.2,  0.2, 1.8, 0.3, 0.6, 0.10, 0, '["SOJA"]',                    '[]',                    true),
  ('Walnusskerne',         NULL,  'MANUAL', 'dev-walnuss',     654, 15.0, 14.0,  2.6,65.0, 6.1, 6.7, 0.00, 1, '["SCHALENFRUECHTE"]',         '[]',                    true)
ON CONFLICT (source, source_id) DO NOTHING;
