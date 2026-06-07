-- HealthForge — Official seed recipes (10 base dishes).
-- is_official = TRUE marks these as curated base content.
-- Idempotent: checks existence by (source, source_id) for ingredients,
-- and by title for recipes. Images use generic placeholder URLs (picsum.photos);
-- actual MinIO uploads can be done later via admin UI.
--
-- NOTE: If this DB already has ingredients with different names, the
-- sub-selects by name_de will fail → first verify ingredient names exist
-- or manually adjust.

-- ============================================================
-- Helper: ensure needed ingredients exist (idempotent)
-- ============================================================
INSERT INTO ingredients (name_de, brand, source, source_id, energy_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, sugar_g_per_100g, fat_g_per_100g, satfat_g_per_100g, fiber_g_per_100g, salt_g_per_100g, histamine_score, allergens_json, fodmap_flags_json, locked)
SELECT * FROM (VALUES
  ('Butter',              NULL, 'MANUAL', 'seed-butter',            717,  0.9,   0.1,  0.1,  81.0, 51.0, 0.0, 0.01, 0, '["MILCH"]',             '[]',             true),
  ('Salz',                NULL, 'MANUAL', 'seed-salz',                0,  0.0,   0.0,  0.0,   0.0,  0.0, 0.0,100.0, 0, '[]',                    '[]',             true),
  ('Zucker',              NULL, 'MANUAL', 'seed-zucker',            387,  0.0, 100.0,100.0,   0.0,  0.0, 0.0, 0.00, 0, '[]',                    '[]',             true),
  ('Kartoffel',           NULL, 'MANUAL', 'seed-kartoffel',          77,  2.0,  17.5,  0.8,   0.1,  0.0, 2.2, 0.01, 0, '[]',                    '[]',             true),
  ('Möhre',               NULL, 'MANUAL', 'seed-moehre',             41,  0.9,   9.6,  4.7,   0.2,  0.0, 2.8, 0.10, 0, '[]',                    '[]',             true),
  ('Sellerie',            NULL, 'MANUAL', 'seed-sellerie',           16,  0.7,   3.0,  1.8,   0.2,  0.0, 1.6, 0.10, 0, '["SELLERIE"]',          '[]',             true),
  ('Sahne',               NULL, 'MANUAL', 'seed-sahne',             200,  2.0,   3.0,  3.0,  20.0, 13.0, 0.0, 0.10, 0, '["MILCH"]',             '["LACTOSE"]',    true),
  ('Hähnchenbrust',       NULL, 'MANUAL', 'seed-haehnchen',        165, 31.0,   0.0,  0.0,   3.6,  1.0, 0.0, 0.10, 1, '[]',                    '[]',             true),
  ('Brokkoli',            NULL, 'MANUAL', 'seed-brokkoli',           34,  2.8,   7.0,  1.7,   0.4,  0.1, 2.6, 0.04, 1, '[]',                    '[]',             true),
  ('Paprika rot',         NULL, 'MANUAL', 'seed-paprika',            31,  1.0,   6.0,  4.2,   0.3,  0.1, 2.1, 0.01, 0, '[]',                    '[]',             true),
  ('Zucchini',            NULL, 'MANUAL', 'seed-zucchini',           17,  1.2,   3.1,  2.5,   0.3,  0.1, 1.0, 0.01, 0, '[]',                    '[]',             true),
  ('Olivenöl',            NULL, 'MANUAL', 'seed-olivenoel',         884,  0.0,   0.0,  0.0, 100.0, 14.0, 0.0, 0.00, 1, '[]',                    '[]',             true),
  ('Pfeffer schwarz',     NULL, 'MANUAL', 'seed-pfeffer',           251, 10.4,  64.0,  0.6,   3.3,  1.0, 7.0, 0.05, 0, '[]',                    '[]',             true),
  ('Spaghetti',           NULL, 'MANUAL', 'seed-spaghetti',         350, 12.0,  73.0,  2.0,   1.5,  0.3, 3.0, 0.01, 0, '["GLUTEN"]',            '["FRUCTANS"]',   true),
  ('Parmesan',            NULL, 'MANUAL', 'seed-parmesan',          431, 38.0,   4.1,  0.1,  29.0, 17.0, 0.0, 1.60, 1, '["MILCH"]',             '["LACTOSE"]',    true),
  ('Zitronensaft',        NULL, 'MANUAL', 'seed-zitronensaft',       22,  0.1,   6.9,  2.5,   0.0,  0.0, 0.1, 0.00, 1, '[]',                    '[]',             true),
  ('Weißkohl',            NULL, 'MANUAL', 'seed-weisskohl',          25,  1.3,   5.8,  3.2,   0.1,  0.0, 2.5, 0.01, 0, '[]',                    '[]',             true),
  ('Rinderhackfleisch',   NULL, 'MANUAL', 'seed-rinderhack',        250, 17.0,   0.0,  0.0,  20.0,  8.5, 0.0, 0.15, 1, '[]',                    '[]',             true),
  ('Reis (ungekocht)',    NULL, 'MANUAL', 'seed-reis-roh',          365,  7.1,  80.0,  0.1,   0.7,  0.2, 1.4, 0.01, 0, '[]',                    '[]',             true),
  ('Kräuter der Provence',NULL, 'MANUAL', 'seed-kraeuter-provence', 250,  5.0,  50.0,  5.0,   5.0,  1.0, 30.0,0.05, 0, '[]',                    '[]',             true),
  ('Tomatenmark',         NULL, 'MANUAL', 'seed-tomatenmark',        82,  4.0,  19.0, 12.0,   0.5,  0.1, 2.0, 0.10, 2, '[]',                    '[]',             true),
  ('Lorbeerblatt',        NULL, 'MANUAL', 'seed-lorbeer',           313,  7.6,  75.0,  0.0,   8.4,  2.3, 26.0, 0.00, 0, '[]',                    '[]',             true),
  ('Haferflocken',        NULL, 'MANUAL', 'seed-haferflocken',      379, 13.0,  67.0,  1.0,   7.0,  1.2, 10.0, 0.01, 0, '["GLUTEN"]',            '[]',             true),
  ('Honig',               NULL, 'MANUAL', 'seed-honig',             304,  0.3,  82.0, 82.0,   0.0,  0.0, 0.2, 0.01, 0, '[]',                    '["FRUCTOSE"]',   true),
  ('Beerenmischung',      NULL, 'MANUAL', 'seed-beeren',             50,  0.8,  11.5,  7.0,   0.4,  0.0, 4.0, 0.01, 0, '[]',                    '[]',             true),
  ('Avocado',             NULL, 'MANUAL', 'seed-avocado',           160,  2.0,   8.5,  0.7,  15.0,  2.1, 6.7, 0.01, 1, '[]',                    '[]',             true),
  ('Ciabatta',            NULL, 'MANUAL', 'seed-ciabatta',          270,  8.0,  52.0,  1.0,   3.0,  0.5, 2.5, 1.30, 0, '["GLUTEN"]',            '["FRUCTANS"]',   true),
  ('Rucola',              NULL, 'MANUAL', 'seed-rucola',             25,  2.6,   3.7,  2.0,   0.3,  0.0, 1.6, 0.03, 1, '[]',                    '[]',             true),
  ('Balsamico-Essig',     NULL, 'MANUAL', 'seed-balsamico',          88,  0.5,  17.0, 15.0,   0.0,  0.0, 0.0, 0.01, 0, '[]',                    '[]',             true),
  ('Hühnerbrühe',         NULL, 'MANUAL', 'seed-huehnerbruehe',      10,  1.0,   1.0,  1.0,   0.2,  0.1, 0.0, 0.80, 0, '["SELLERIE"]',          '[]',             true),
  ('Schnittlauch',        NULL, 'MANUAL', 'seed-schnittlauch',       30,  3.3,   4.4,  1.9,   0.7,  0.2, 2.5, 0.01, 0, '[]',                    '[]',             true),
  ('Zimt gemahlen',       NULL, 'MANUAL', 'seed-zimt',              247,  3.9,  80.0,  2.2,   1.2,  0.3, 50.0, 0.01, 0, '[]',                    '[]',             true),
  ('Vanilleextrakt',      NULL, 'MANUAL', 'seed-vanille',           288,  0.1,  12.0, 12.0,   0.1,  0.0, 0.0, 0.01, 0, '[]',                    '[]',             true),
  ('Kartoffelmehl',       NULL, 'MANUAL', 'seed-kartoffelmehl',     357,  0.1,  83.0,  0.0,   0.1,  0.0, 3.0, 0.01, 0, '[]',                    '[]',             true),
  ('Sauerkraut',          NULL, 'MANUAL', 'seed-sauerkraut',         19,  0.9,   4.3,  1.0,   0.1,  0.0, 3.0, 0.80, 2, '[]',                    '[]',             true),
  ('Kümmel gemahlen',    NULL, 'MANUAL', 'seed-kuemmel',            333, 20.0,  44.0,  0.6,  15.0,  1.0, 10.0, 0.01, 0, '[]',                    '[]',             true),
  ('Räuchertofu',         NULL, 'MANUAL', 'seed-raeuchertofu',      120, 14.0,   3.0,  0.5,   6.0,  0.9, 1.5, 0.02, 0, '["SOJA"]',              '[]',             true),
  ('Kokosmilch',          NULL, 'MANUAL', 'seed-kokosmilch',        230,  2.3,   5.5,  2.0,  24.0, 21.0, 0.0, 0.03, 0, '[]',                    '[]',             true),
  ('Naturtofu',           NULL, 'MANUAL', 'seed-tofu',               76,  8.1,   1.9,  0.2,   4.8,  0.7, 0.3, 0.01, 0, '["SOJA"]',              '[]',             true)
) AS vals(name_de, brand, source, source_id, energy_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, sugar_g_per_100g, fat_g_per_100g, satfat_g_per_100g, fiber_g_per_100g, salt_g_per_100g, histamine_score, allergens_json, fodmap_flags_json, locked)
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients i WHERE i.source = 'MANUAL' AND i.source_id = vals.source_id
);

-- ============================================================
-- Official recipes (author = first admin user, or fallback)
-- ============================================================
DO $$
DECLARE
    admin_id UUID;
    recipe_id UUID;
BEGIN
    -- Find an admin user, or use a placeholder UUID if none exists
    SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' ORDER BY created_at ASC LIMIT 1;
    IF admin_id IS NULL THEN
        -- If no admin exists yet, use gen_random_uuid() — recipes will be orphaned
        -- until an admin manually reassigns them or a real user claims them.
        admin_id := '00000000-0000-0000-0000-000000000001'::UUID;
    END IF;

    -- ======================== 1. PFANNKUCHEN ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Pfannkuchen (klassisch)',
            'Lockere, dünne Pfannkuchen nach Omas Art. Mit Zimt und Zucker oder herzhaft gefüllt.',
            'https://picsum.photos/seed/pfannkuchen/800/600', 4, 10, 15,
            ARRAY['BREAKFAST','LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-weizen-405' OR source_id = 'dev-weizen-405' LIMIT 1), 250, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' OR source_id = 'dev-vm-35' LIMIT 1), 500, 'ml'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'dev-ei' LIMIT 1), 3, 'Stück'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-zucker' LIMIT 1), 30, 'g'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 20, 'g');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Mehl, Milch, Eier, Salz und Zucker in einer Schüssel glatt rühren. 5 Minuten quellen lassen.'),
        (recipe_id, 1, 'Butter in einer beschichteten Pfanne bei mittlerer Hitze schmelzen.'),
        (recipe_id, 2, 'Eine Kelle Teig in die Pfanne geben, schwenken und bei mittlerer Hitze 2 Minuten backen, bis die Unterseite goldbraun ist.'),
        (recipe_id, 3, 'Pfannkuchen wenden und weitere 1-2 Minuten backen.'),
        (recipe_id, 4, 'Warm servieren – mit Zimt und Zucker oder nach Belieben belegt.');

    -- ======================== 2. RÜHREI ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Rührei (einfach)',
            'Saftiges Rührei mit Schnittlauch – perfekt zum Frühstück oder Abendbrot.',
            'https://picsum.photos/seed/ruehrei/800/600', 2, 5, 8,
            ARRAY['BREAKFAST','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'dev-ei' LIMIT 1), 4, 'Stück'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' OR source_id = 'dev-vm-35' LIMIT 1), 50, 'ml'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 10, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-schnittlauch' LIMIT 1), 10, 'g');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Eier in einer Schüssel aufschlagen, Milch zugeben und mit Schneebesen verquirlen. Mit Salz und Pfeffer würzen.'),
        (recipe_id, 1, 'Butter in einer beschichteten Pfanne bei niedriger bis mittlerer Hitze schmelzen.'),
        (recipe_id, 2, 'Eimasse in die Pfanne geben und mit einem Spatel langsam von außen nach innen schieben, bis die Eier gestockt sind (ca. 5-7 Minuten).'),
        (recipe_id, 3, 'Nicht zu lange braten – Rührei soll saftig bleiben! Mit Schnittlauch bestreut servieren.');

    -- ======================== 3. SPAGHETTI AGLIO E OLIO ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Spaghetti Aglio e Olio',
            'Einfaches, aber aromatisches Pastagericht mit Knoblauch und Olivenöl – in 15 Minuten fertig.',
            'https://picsum.photos/seed/spaghetti/800/600', 2, 5, 15,
            ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-spaghetti' LIMIT 1), 250, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-olivenoel' LIMIT 1), 60, 'ml'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'dev-knoblauch' LIMIT 1), 4, 'Zehen'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'EL'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-parmesan' LIMIT 1), 30, 'g');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Spaghetti in reichlich Salzwasser al dente kochen. 100 ml Nudelwasser auffangen.'),
        (recipe_id, 1, 'Knoblauch in dünne Scheiben schneiden. Olivenöl in einer Pfanne erhitzen und Knoblauch darin goldgelb braten (nicht verbrennen lassen!).'),
        (recipe_id, 2, 'Nudelwasser zugeben, dann die abgetropften Spaghetti in die Pfanne geben und gut vermengen.'),
        (recipe_id, 3, 'Mit Pfeffer und frisch geriebenem Parmesan servieren.');

    -- ======================== 4. KARTOFFELSUPPE ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Kartoffelsuppe',
            'Cremige Kartoffelsuppe mit Möhren und Sellerie – wärmend und sättigend.',
            'https://picsum.photos/seed/kartoffelsuppe/800/600', 4, 15, 35,
            ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-kartoffel' LIMIT 1), 800, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-moehre' LIMIT 1), 200, 'g'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-sellerie' LIMIT 1), 100, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'dev-zwiebel' LIMIT 1), 1, 'Stück'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-huehnerbruehe' LIMIT 1), 800, 'ml'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-sahne' LIMIT 1), 100, 'ml'),
        (recipe_id, 6, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 20, 'g'),
        (recipe_id, 7, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'TL'),
        (recipe_id, 8, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
        (recipe_id, 9, (SELECT id FROM ingredients WHERE source_id = 'seed-lorbeer' LIMIT 1), 2, 'Blätter');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Kartoffeln, Möhren und Sellerie schälen und in kleine Würfel schneiden. Zwiebel fein würfeln.'),
        (recipe_id, 1, 'Butter in einem großen Topf schmelzen, Zwiebel darin glasig dünsten.'),
        (recipe_id, 2, 'Gemüsewürfel zugeben, kurz mitdünsten. Mit Hühnerbrühe ablöschen, Lorbeerblätter zugeben.'),
        (recipe_id, 3, '20-25 Minuten köcheln lassen, bis das Gemüse weich ist. Lorbeerblätter entfernen.'),
        (recipe_id, 4, 'Suppe fein pürieren, Sahne einrühren. Mit Salz und Pfeffer abschmecken. Heiß servieren.');

    -- ======================== 5. HÄHNCHENBRUST MIT REIS UND BROKKOLI ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Hähnchenbrust mit Reis und Brokkoli',
            'Leichtes, proteinreiches Gericht – ideal für Meal-Prep und bewusste Ernährung.',
            'https://picsum.photos/seed/haehnchen-reis/800/600', 2, 10, 25,
            ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-haehnchen' LIMIT 1), 400, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-reis-roh' LIMIT 1), 200, 'g'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-brokkoli' LIMIT 1), 300, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-olivenoel' LIMIT 1), 30, 'ml'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'TL'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
        (recipe_id, 6, (SELECT id FROM ingredients WHERE source_id = 'seed-zitronensaft' LIMIT 1), 15, 'ml'),
        (recipe_id, 7, (SELECT id FROM ingredients WHERE source_id = 'seed-kraeuter-provence' LIMIT 1), 1, 'TL');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Reis nach Packungsanweisung in Salzwasser kochen.'),
        (recipe_id, 1, 'Brokkoli in Röschen teilen, in kochendem Salzwasser 3 Minuten blanchieren, abgießen und kalt abschrecken.'),
        (recipe_id, 2, 'Hähnchenbrust von beiden Seiten mit Salz, Pfeffer und Kräutern würzen.'),
        (recipe_id, 3, 'Olivenöl in einer Pfanne erhitzen, Hähnchenbrust bei mittlerer Hitze 6-7 Minuten pro Seite goldbraun braten.'),
        (recipe_id, 4, 'Hähnchen kurz ruhen lassen, in Scheiben schneiden. Mit Reis und Brokkoli servieren. Zitronensaft darüber träufeln.');

    -- ======================== 6. HAFERFLOCKEN-PORRIDGE ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Haferflocken-Porridge',
            'Wärmender Frühstücks-Porridge mit Beeren und Honig – Energie für den Tag.',
            'https://picsum.photos/seed/porridge/800/600', 2, 5, 10,
            ARRAY['BREAKFAST']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-haferflocken' LIMIT 1), 100, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' OR source_id = 'dev-vm-35' LIMIT 1), 300, 'ml'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-beeren' LIMIT 1), 100, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-honig' LIMIT 1), 20, 'g'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-zimt' LIMIT 1), 1, 'Prise'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-vanille' LIMIT 1), 5, 'ml');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Haferflocken mit Milch und Vanilleextrakt in einem Topf bei mittlerer Hitze unter Rühren aufkochen.'),
        (recipe_id, 1, 'Hitze reduzieren und 5 Minuten köcheln lassen, bis der Porridge cremig ist. Dabei gelegentlich umrühren.'),
        (recipe_id, 2, 'Porridge in Schalen füllen. Beeren darauf verteilen, mit Honig beträufeln und mit Zimt bestäuben.');

    -- ======================== 7. SPEGELEI MIT BRATKARTOFFELN ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Spiegelei mit Bratkartoffeln',
            'Herzhafter Klassiker – goldbraune Bratkartoffeln mit knusprigem Spiegelei.',
            'https://picsum.photos/seed/spiegelei/800/600', 2, 10, 20,
            ARRAY['BREAKFAST','LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-kartoffel' LIMIT 1), 600, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'dev-ei' LIMIT 1), 4, 'Stück'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'dev-zwiebel' LIMIT 1), 1, 'Stück'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-olivenoel' LIMIT 1), 30, 'ml'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 10, 'g'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'TL'),
        (recipe_id, 6, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
        (recipe_id, 7, (SELECT id FROM ingredients WHERE source_id = 'seed-schnittlauch' LIMIT 1), 10, 'g');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Kartoffeln schälen und in ca. 1 cm dicke Scheiben schneiden. Zwiebel halbieren und in Streifen schneiden.'),
        (recipe_id, 1, 'Kartoffeln in kochendem Salzwasser 5 Minuten vorblanchieren, abgießen.'),
        (recipe_id, 2, 'Olivenöl in einer Pfanne erhitzen, Kartoffelscheiben darin bei mittlerer bis hoher Hitze 10 Minuten goldbraun braten. Zwiebelstreifen in den letzten 3 Minuten zugeben.'),
        (recipe_id, 3, 'In einer zweiten Pfanne Butter erhitzen, Eier vorsichtig aufschlagen und bei mittlerer Hitze 3-4 Minuten braten, bis das Eiweiß gestockt ist (Eigelb flüssig lassen).'),
        (recipe_id, 4, 'Bratkartoffeln auf Tellern anrichten, Spiegelei darauf setzen. Mit Salz, Pfeffer und Schnittlauch bestreuen.');

    -- ======================== 8. JOGHURT MIT BEEREN UND HONIG ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Joghurt mit Beeren und Honig',
            'Schnelles, gesundes Frühstück oder Dessert – in 2 Minuten fertig.',
            'https://picsum.photos/seed/joghurt-beeren/800/600', 1, 2, 0,
            ARRAY['BREAKFAST','SNACK']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'dev-joghurt' LIMIT 1), 200, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-beeren' LIMIT 1), 75, 'g'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-honig' LIMIT 1), 15, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-haferflocken' LIMIT 1), 15, 'g');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Joghurt in eine Schüssel geben.'),
        (recipe_id, 1, 'Beeren waschen, auf dem Joghurt verteilen.'),
        (recipe_id, 2, 'Mit Honig beträufeln und Haferflocken darüber streuen. Sofort servieren.');

    -- ======================== 9. VOLLKORNBROT MIT AVOCADO UND EI ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Vollkornbrot mit Avocado und Ei',
            'Trender, sättigender Avocado-Toast mit Spiegelei – gesund und lecker.',
            'https://picsum.photos/seed/avocado-toast/800/600', 2, 5, 8,
            ARRAY['BREAKFAST','LUNCH','SNACK']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'dev-vk-brot' LIMIT 1), 4, 'Scheiben'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-avocado' LIMIT 1), 1, 'Stück'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'dev-ei' LIMIT 1), 2, 'Stück'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-zitronensaft' LIMIT 1), 10, 'ml'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Brot toasten, bis es goldbraun und knusprig ist.'),
        (recipe_id, 1, 'Avocado halbieren, Kern entfernen, Fruchtfleisch mit einer Gabel zerdrücken. Zitronensaft, Salz und Pfeffer untermischen.'),
        (recipe_id, 2, 'In einer Pfanne etwas Öl erhitzen, Eier als Spiegelei braten (Eigelb flüssig oder fest nach Wunsch).'),
        (recipe_id, 3, 'Avocado-Creme auf dem Toast verteilen, Spiegelei darauf setzen. Mit Salz und Pfeffer würzen.');

    -- ======================== 10. GEMÜSEPFANNE (BUNT) ========================
    INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
    VALUES (gen_random_uuid(), admin_id, 'Bunte Gemüsepfanne',
            'Schnelle, bunte Gemüsepfanne mit Paprika, Zucchini und Tomaten – als Hauptgericht oder Beilage.',
            'https://picsum.photos/seed/gemuesepfanne/800/600', 2, 10, 15,
            ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
    RETURNING id INTO recipe_id;

    INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
        (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-zucchini' LIMIT 1), 300, 'g'),
        (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-paprika' LIMIT 1), 2, 'Stück'),
        (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'dev-tomate' LIMIT 1), 200, 'g'),
        (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'dev-zwiebel' LIMIT 1), 1, 'Stück'),
        (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'dev-knoblauch' LIMIT 1), 2, 'Zehen'),
        (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-olivenoel' LIMIT 1), 30, 'ml'),
        (recipe_id, 6, (SELECT id FROM ingredients WHERE source_id = 'seed-kraeuter-provence' LIMIT 1), 1, 'TL'),
        (recipe_id, 7, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'TL'),
        (recipe_id, 8, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise');

    INSERT INTO recipe_steps (recipe_id, position, text) VALUES
        (recipe_id, 0, 'Zucchini waschen und in Scheiben schneiden. Paprika entkernen und würfeln. Tomaten vierteln. Zwiebel in Ringe, Knoblauch fein hacken.'),
        (recipe_id, 1, 'Olivenöl in einer großen Pfanne oder Wok erhitzen. Zwiebelringe darin 2 Minuten glasig dünsten, Knoblauch zugeben.'),
        (recipe_id, 2, 'Zucchini und Paprika zugeben, bei hoher Hitze 5-7 Minuten unter Rühren braten.'),
        (recipe_id, 3, 'Tomaten zugeben, mit Kräutern der Provence, Salz und Pfeffer würzen. Weitere 3 Minuten garen.'),
        (recipe_id, 4, 'Heiß servieren – pur, mit Reis oder als Beilage zu Gegrilltem.');

END $$;
