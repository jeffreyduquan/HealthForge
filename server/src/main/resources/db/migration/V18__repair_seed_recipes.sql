-- HealthForge — Repair V17 if it failed on production (missing ingredient references).
-- V17 may have partially failed on production because recipe_ingredients referenced
-- Zutaten mit dev-* source_ids (aus V4 dev-seed, die auf Produktion nie gelaufen ist).
-- Diese Migration:
--  1. Entfernt den fehlgeschlagenen V17-Eintrag aus flyway_schema_history (falls vorhanden)
--  2. Legt fehlende Basis-Zutaten an (falls V17 nicht durchkam)
--  3. Erstellt die offiziellen Rezepte, falls sie fehlen

-- Step 1: Clean up failed V17 migration entry
DELETE FROM flyway_schema_history WHERE version = '17' AND success = false;

-- Step 2: Ensure all seed ingredients exist (idempotent)
INSERT INTO ingredients (name_de, brand, source, source_id, energy_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, sugar_g_per_100g, fat_g_per_100g, satfat_g_per_100g, fiber_g_per_100g, salt_g_per_100g, histamine_score, allergens_json, fodmap_flags_json, locked)
SELECT * FROM (VALUES
  ('Weizenmehl Type 405', NULL, 'MANUAL', 'seed-weizen-405', 348, 10.6, 71.0, 0.7, 1.0, 0.2, 4.0, 0.01, 0, '["GLUTEN"]', '["FRUCTANS"]', true),
  ('Vollmilch 3,5%',      NULL, 'MANUAL', 'seed-vm-35',      64,  3.4,  4.8, 4.8, 3.5, 2.2, 0.0, 0.10, 0, '["MILCH"]',  '["LACTOSE"]', true),
  ('Hühnerei',            NULL, 'MANUAL', 'seed-ei',         155, 13.0,  1.1, 1.1,11.0, 3.3, 0.0, 0.30, 1, '["EIER"]',   '[]',          true),
  ('Naturjoghurt',        NULL, 'MANUAL', 'seed-joghurt',     61,  3.5,  4.7, 4.7, 3.2, 2.0, 0.0, 0.10, 2, '["MILCH"]',  '["LACTOSE"]', true),
  ('Tomate',              NULL, 'MANUAL', 'seed-tomate',      18,  0.9,  3.9, 2.6, 0.2, 0.0, 1.2, 0.01, 2, '[]',         '[]',          true),
  ('Zwiebel',             NULL, 'MANUAL', 'seed-zwiebel',     40,  1.1,  9.3, 4.2, 0.1, 0.0, 1.7, 0.00, 0, '[]',         '["FRUCTANS"]',true),
  ('Knoblauch',           NULL, 'MANUAL', 'seed-knoblauch',   149,  6.4, 33.0, 1.0, 0.5, 0.1, 2.1, 0.02, 1, '[]',         '["FRUCTANS"]',true),
  ('Vollkornbrot',        NULL, 'MANUAL', 'seed-vk-brot',     247,  8.4, 41.0, 3.5, 3.3, 0.5, 7.4, 1.30,1, '["GLUTEN"]', '["FRUCTANS"]',true)
) AS vals(name_de, brand, source, source_id, energy_kcal_per_100g, protein_g_per_100g, carbs_g_per_100g, sugar_g_per_100g, fat_g_per_100g, satfat_g_per_100g, fiber_g_per_100g, salt_g_per_100g, histamine_score, allergens_json, fodmap_flags_json, locked)
WHERE NOT EXISTS (
    SELECT 1 FROM ingredients i WHERE i.source = 'MANUAL' AND i.source_id = vals.source_id
);

-- Step 3: Create official recipes if they don't exist (by title)
DO $$
DECLARE
    admin_id UUID;
    recipe_id UUID;
    recipe_titles TEXT[] := ARRAY[
        'Pfannkuchen (klassisch)',
        'Rührei (einfach)',
        'Spaghetti Aglio e Olio',
        'Kartoffelsuppe',
        'Hähnchenbrust mit Reis und Brokkoli',
        'Haferflocken-Porridge',
        'Spiegelei mit Bratkartoffeln',
        'Joghurt mit Beeren und Honig',
        'Vollkornbrot mit Avocado und Ei',
        'Bunte Gemüsepfanne'
    ];
BEGIN
    SELECT id INTO admin_id FROM users WHERE role = 'ADMIN' ORDER BY created_at ASC LIMIT 1;
    IF admin_id IS NULL THEN
        admin_id := '00000000-0000-0000-0000-000000000001'::UUID;
    END IF;

    -- Only create recipes that don't exist yet
    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Pfannkuchen (klassisch)') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Pfannkuchen (klassisch)',
                'Lockere, dünne Pfannkuchen nach Omas Art. Mit Zimt und Zucker oder herzhaft gefüllt.',
                'https://picsum.photos/seed/pfannkuchen/800/600', 4, 10, 15,
                ARRAY['BREAKFAST','LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-weizen-405' LIMIT 1), 250, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' LIMIT 1), 500, 'ml'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-ei' LIMIT 1), 3, 'Stück'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-zucker' LIMIT 1), 30, 'g'),
            (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 20, 'g');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Mehl, Milch, Eier, Salz und Zucker in einer Schüssel glatt rühren. 5 Minuten quellen lassen.'),
            (recipe_id, 1, 'Butter in einer beschichteten Pfanne bei mittlerer Hitze schmelzen.'),
            (recipe_id, 2, 'Eine Kelle Teig in die Pfanne geben, schwenken und bei mittlerer Hitze 2 Minuten backen, bis die Unterseite goldbraun ist.'),
            (recipe_id, 3, 'Pfannkuchen wenden und weitere 1-2 Minuten backen.'),
            (recipe_id, 4, 'Warm servieren – mit Zimt und Zucker oder nach Belieben belegt.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Rührei (einfach)') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Rührei (einfach)',
                'Saftiges Rührei mit Schnittlauch – perfekt zum Frühstück oder Abendbrot.',
                'https://picsum.photos/seed/ruehrei/800/600', 2, 5, 8,
                ARRAY['BREAKFAST','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-ei' LIMIT 1), 4, 'Stück'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' LIMIT 1), 50, 'ml'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-butter' LIMIT 1), 10, 'g'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
            (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-schnittlauch' LIMIT 1), 10, 'g');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Eier in einer Schüssel aufschlagen, Milch zugeben und mit Schneebesen verquirlen. Mit Salz und Pfeffer würzen.'),
            (recipe_id, 1, 'Butter in einer beschichteten Pfanne bei niedriger bis mittlerer Hitze schmelzen.'),
            (recipe_id, 2, 'Eimasse in die Pfanne geben und mit einem Spatel langsam von außen nach innen schieben, bis die Eier gestockt sind (ca. 5-7 Minuten).'),
            (recipe_id, 3, 'Nicht zu lange braten – Rührei soll saftig bleiben! Mit Schnittlauch bestreut servieren.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Spaghetti Aglio e Olio') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Spaghetti Aglio e Olio',
                'Einfaches, aber aromatisches Pastagericht mit Knoblauch und Olivenöl – in 15 Minuten fertig.',
                'https://picsum.photos/seed/spaghetti/800/600', 2, 5, 15,
                ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-spaghetti' LIMIT 1), 250, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-olivenoel' LIMIT 1), 60, 'ml'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-knoblauch' LIMIT 1), 4, 'Zehen'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'EL'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise'),
            (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-parmesan' LIMIT 1), 30, 'g');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Spaghetti in reichlich Salzwasser al dente kochen. 100 ml Nudelwasser auffangen.'),
            (recipe_id, 1, 'Knoblauch in dünne Scheiben schneiden. Olivenöl in einer Pfanne erhitzen und Knoblauch darin goldgelb braten (nicht verbrennen lassen!).'),
            (recipe_id, 2, 'Nudelwasser zugeben, dann die abgetropften Spaghetti in die Pfanne geben und gut vermengen.'),
            (recipe_id, 3, 'Mit Pfeffer und frisch geriebenem Parmesan servieren.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Kartoffelsuppe') THEN
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
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-zwiebel' LIMIT 1), 1, 'Stück'),
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
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Hähnchenbrust mit Reis und Brokkoli') THEN
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
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Haferflocken-Porridge') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Haferflocken-Porridge',
                'Wärmender Frühstücks-Porridge mit Beeren und Honig – Energie für den Tag.',
                'https://picsum.photos/seed/porridge/800/600', 2, 5, 10,
                ARRAY['BREAKFAST']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-haferflocken' LIMIT 1), 100, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-vm-35' LIMIT 1), 300, 'ml'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-beeren' LIMIT 1), 100, 'g'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-honig' LIMIT 1), 20, 'g'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-zimt' LIMIT 1), 1, 'Prise'),
            (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-vanille' LIMIT 1), 5, 'ml');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Haferflocken mit Milch und Vanilleextrakt in einem Topf bei mittlerer Hitze unter Rühren aufkochen.'),
            (recipe_id, 1, 'Hitze reduzieren und 5 Minuten köcheln lassen, bis der Porridge cremig ist. Dabei gelegentlich umrühren.'),
            (recipe_id, 2, 'Porridge in Schalen füllen. Beeren darauf verteilen, mit Honig beträufeln und mit Zimt bestäuben.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Spiegelei mit Bratkartoffeln') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Spiegelei mit Bratkartoffeln',
                'Herzhafter Klassiker – goldbraune Bratkartoffeln mit knusprigem Spiegelei.',
                'https://picsum.photos/seed/spiegelei/800/600', 2, 10, 20,
                ARRAY['BREAKFAST','LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-kartoffel' LIMIT 1), 600, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-ei' LIMIT 1), 4, 'Stück'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-zwiebel' LIMIT 1), 1, 'Stück'),
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
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Joghurt mit Beeren und Honig') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Joghurt mit Beeren und Honig',
                'Schnelles, gesundes Frühstück oder Dessert – in 2 Minuten fertig.',
                'https://picsum.photos/seed/joghurt-beeren/800/600', 1, 2, 0,
                ARRAY['BREAKFAST','SNACK']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-joghurt' LIMIT 1), 200, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-beeren' LIMIT 1), 75, 'g'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-honig' LIMIT 1), 15, 'g'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-haferflocken' LIMIT 1), 15, 'g');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Joghurt in eine Schüssel geben.'),
            (recipe_id, 1, 'Beeren waschen, auf dem Joghurt verteilen.'),
            (recipe_id, 2, 'Mit Honig beträufeln und Haferflocken darüber streuen. Sofort servieren.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Vollkornbrot mit Avocado und Ei') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Vollkornbrot mit Avocado und Ei',
                'Trender, sättigender Avocado-Toast mit Spiegelei – gesund und lecker.',
                'https://picsum.photos/seed/avocado-toast/800/600', 2, 5, 8,
                ARRAY['BREAKFAST','LUNCH','SNACK']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-vk-brot' LIMIT 1), 4, 'Scheiben'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-avocado' LIMIT 1), 1, 'Stück'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-ei' LIMIT 1), 2, 'Stück'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-zitronensaft' LIMIT 1), 10, 'ml'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-salz' LIMIT 1), 1, 'Prise'),
            (recipe_id, 5, (SELECT id FROM ingredients WHERE source_id = 'seed-pfeffer' LIMIT 1), 1, 'Prise');
        INSERT INTO recipe_steps (recipe_id, position, text) VALUES
            (recipe_id, 0, 'Brot toasten, bis es goldbraun und knusprig ist.'),
            (recipe_id, 1, 'Avocado halbieren, Kern entfernen, Fruchtfleisch mit einer Gabel zerdrücken. Zitronensaft, Salz und Pfeffer untermischen.'),
            (recipe_id, 2, 'In einer Pfanne etwas Öl erhitzen, Eier als Spiegelei braten (Eigelb flüssig oder fest nach Wunsch).'),
            (recipe_id, 3, 'Avocado-Creme auf dem Toast verteilen, Spiegelei darauf setzen. Mit Salz und Pfeffer würzen.');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM recipes WHERE title = 'Bunte Gemüsepfanne') THEN
        INSERT INTO recipes (id, author_id, title, description, image_key, servings, prep_minutes, cook_minutes, slot_tags, status, visibility, is_official)
        VALUES (gen_random_uuid(), admin_id, 'Bunte Gemüsepfanne',
                'Schnelle, bunte Gemüsepfanne mit Paprika, Zucchini und Tomaten – als Hauptgericht oder Beilage.',
                'https://picsum.photos/seed/gemuesepfanne/800/600', 2, 10, 15,
                ARRAY['LUNCH','DINNER']::text[], 'PUBLISHED', 'PUBLIC', true)
        RETURNING id INTO recipe_id;
        INSERT INTO recipe_ingredients (recipe_id, position, ingredient_id, quantity, unit) VALUES
            (recipe_id, 0, (SELECT id FROM ingredients WHERE source_id = 'seed-zucchini' LIMIT 1), 300, 'g'),
            (recipe_id, 1, (SELECT id FROM ingredients WHERE source_id = 'seed-paprika' LIMIT 1), 2, 'Stück'),
            (recipe_id, 2, (SELECT id FROM ingredients WHERE source_id = 'seed-tomate' LIMIT 1), 200, 'g'),
            (recipe_id, 3, (SELECT id FROM ingredients WHERE source_id = 'seed-zwiebel' LIMIT 1), 1, 'Stück'),
            (recipe_id, 4, (SELECT id FROM ingredients WHERE source_id = 'seed-knoblauch' LIMIT 1), 2, 'Zehen'),
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
    END IF;
END $$;
