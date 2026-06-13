-- =================================================================
-- MANUAL → USDA_FDC REMAPPING + DELETE
-- Erstellt von Copilot am 2026-06-13
-- REVIEW VOR AUSFÜHRUNG!
-- =================================================================
BEGIN;

-- ─── STEP 1: Update recipe_ingredients references ───

-- Avocado
UPDATE recipe_ingredients SET ingredient_id = '9f31ade7-4775-4099-9ecf-aeb14cb493ef'
WHERE ingredient_id = 'bba0f5e5-3c51-46fd-b710-81ec654e2436';

-- Balsamico-Essig → Essig Weißwein (nächstbeste)
UPDATE recipe_ingredients SET ingredient_id = '1d10ec54-bc24-4246-8431-622c47a68029'
WHERE ingredient_id = '70087420-c8ed-4e43-92d0-16058f5a7280';

-- Beerenmischung → Heidelbeere (vertretbare Näherung)
UPDATE recipe_ingredients SET ingredient_id = '5a7a8d92-7012-403d-866f-c7bd34c035e0'
WHERE ingredient_id = '864f8d5c-2381-4c63-b671-5fe2c2d5d9ee';

-- Brokkoli
UPDATE recipe_ingredients SET ingredient_id = '2d5a15a0-ff08-491d-9fe5-3c3f53550980'
WHERE ingredient_id = 'fa4eea32-7841-4afa-9514-f9a323c23aa4';

-- Butter
UPDATE recipe_ingredients SET ingredient_id = 'efdbb557-094f-482f-adc0-f486fbb0300e'
WHERE ingredient_id = 'e09f1876-87fd-4e29-85e4-c6eb07da3071';

-- Ciabatta
UPDATE recipe_ingredients SET ingredient_id = '651258f0-bc20-4950-b85d-bdb703f68a00'
WHERE ingredient_id = '7dc77863-8ff0-4f8f-a34a-f096f235620e';

-- Haferflocken
UPDATE recipe_ingredients SET ingredient_id = 'c7917610-0671-4d48-a412-da1ce4990575'
WHERE ingredient_id = 'f9d103ed-0c28-4028-a101-577089ed0924';

-- Honig
UPDATE recipe_ingredients SET ingredient_id = '96fd824a-06a8-48ff-8693-1a7495e8358b'
WHERE ingredient_id = '7750d7c4-dda8-4ae2-adb6-5a5cbfe8f98b';

-- Hähnchenbrust
UPDATE recipe_ingredients SET ingredient_id = 'dc4024ec-c125-4160-9dcb-d2683cc2a26b'
WHERE ingredient_id = 'c7261545-4cc5-46c9-b043-9c2c1a12e785';

-- Hühnerei → Hühnerei gekocht (kcal 155 passt perfekt)
UPDATE recipe_ingredients SET ingredient_id = '39c76383-00c9-4b02-97d1-0b05dbf58e9e'
WHERE ingredient_id = '134a27ea-f2e7-4b3a-b56b-860115c8d5d2';

-- Kartoffel
UPDATE recipe_ingredients SET ingredient_id = '053b80a1-d612-4b9b-9bc1-912daa2e3a4b'
WHERE ingredient_id = '831601c7-e874-456f-a7e9-7ffc4ab2abf9';

-- Kartoffelmehl → Speisestärke Kartoffel (357 kcal ~ 357 kcal)
UPDATE recipe_ingredients SET ingredient_id = '29637cdd-c104-4553-aefd-cc5437e1a207'
WHERE ingredient_id = '60d79064-5661-48ad-bf55-ab2229a0800a';

-- Knoblauch
UPDATE recipe_ingredients SET ingredient_id = 'ac1b8069-3da0-4171-b8f2-cfba25584a75'
WHERE ingredient_id = '69ad1a86-a830-41ae-a0b2-ce8c42a1bb62';

-- Kokosmilch
UPDATE recipe_ingredients SET ingredient_id = 'b801be17-488a-41a9-adf8-aae99401e200'
WHERE ingredient_id = '038fb617-0239-4bfd-b6e7-8c548bab15d3';

-- Kräuter der Provence
UPDATE recipe_ingredients SET ingredient_id = '0621bd50-fefe-4499-9aac-baa0f1b6f7e9'
WHERE ingredient_id = '7e6bbb29-a10c-4235-a34a-c6d52d6122cf';

-- Kümmel gemahlen → Kümmel ganz (333 kcal ~ 333 kcal)
UPDATE recipe_ingredients SET ingredient_id = '1765dcb5-2bb8-4d65-8976-efcd37451c35'
WHERE ingredient_id = 'a5fa13ff-7186-4198-92f9-3ec60e447a3c';

-- Lorbeerblatt
UPDATE recipe_ingredients SET ingredient_id = '42201630-0e34-429e-af6a-7a5cb960b821'
WHERE ingredient_id = '29154ede-0298-4da8-bb56-8d629bf664f8';

-- Möhre → Karotte
UPDATE recipe_ingredients SET ingredient_id = 'bac525a9-02ac-4a6b-b971-3afee5124b9d'
WHERE ingredient_id = '84a94496-106b-44e8-b3dc-79fec42c0551';

-- Naturjoghurt → Joghurt natur
UPDATE recipe_ingredients SET ingredient_id = '20691281-2851-41a4-9408-f6c543f070b6'
WHERE ingredient_id = '789b9679-61f3-4e03-b75c-0c20bcc0249d';

-- Naturtofu → Tofu fest
UPDATE recipe_ingredients SET ingredient_id = '867400e7-2cb4-4832-ac3a-57a44a3d3b00'
WHERE ingredient_id = 'c6b7e5df-f05f-43f0-aba0-181dc0724ec4';

-- Olivenöl
UPDATE recipe_ingredients SET ingredient_id = '09c33fba-9a13-4150-bc1b-d71f1150f02b'
WHERE ingredient_id = 'e700ea03-a5f9-4349-83ca-efd7928aee77';

-- Paprika rot
UPDATE recipe_ingredients SET ingredient_id = 'bd3df986-9bb1-4795-9c29-e68f9749d65a'
WHERE ingredient_id = '702ccfe3-cbc2-4f1c-9fa0-4cdda46df250';

-- Parmesan
UPDATE recipe_ingredients SET ingredient_id = '02f5f061-ef86-47de-97d1-a01e70152e57'
WHERE ingredient_id = 'd06e1f94-0746-4b2d-939d-56945995d438';

-- Pfeffer schwarz
UPDATE recipe_ingredients SET ingredient_id = '6a977980-d4d9-4ae1-a957-3081ad68e15e'
WHERE ingredient_id = '56a926f6-ce62-48c3-8351-2f61debae7b6';

-- Rinderhackfleisch → Rinderhack (254 kcal ~ 250 kcal)
UPDATE recipe_ingredients SET ingredient_id = '4fa40d89-db92-4e21-8060-f65e72445623'
WHERE ingredient_id = '29f5193a-a1b1-447a-ac2d-940c6c84c76a';

-- Rucola
UPDATE recipe_ingredients SET ingredient_id = '273d5744-f194-4dc2-8f6c-779d1716b633'
WHERE ingredient_id = 'c39d6ac8-4c33-40e1-8a54-b55908b2bb28';

-- Räuchertofu → Tofu geräuchert
UPDATE recipe_ingredients SET ingredient_id = '9b6a5b0a-d3c8-464a-a1bc-fa7253038c70'
WHERE ingredient_id = 'c49b72a7-c5e3-4522-8c9d-848d29787d58';

-- Sahne → Schlagsahne
UPDATE recipe_ingredients SET ingredient_id = 'fb24f8f4-192a-459d-9032-44b51ecc63cf'
WHERE ingredient_id = '68e500bd-9754-4c00-8936-b086851cba9d';

-- Salz
UPDATE recipe_ingredients SET ingredient_id = 'efc91696-ae77-47c7-8c13-a2eb8d7c4fcc'
WHERE ingredient_id = '765c9315-0997-4ada-8af0-a466700a6ee8';

-- Sauerkraut
UPDATE recipe_ingredients SET ingredient_id = 'e7f0b379-5cc0-431f-9bd4-6d2e4cd42dd8'
WHERE ingredient_id = 'a090d63d-4085-4709-86e0-99abeaabe748';

-- Schnittlauch
UPDATE recipe_ingredients SET ingredient_id = 'c5ae1a13-0607-4fcc-8b16-e4defc329633'
WHERE ingredient_id = '31c3b6d1-f20d-41b2-aed4-59fc9d46d1a0';

-- Sellerie → Staudensellerie
UPDATE recipe_ingredients SET ingredient_id = '6af97d39-cd64-455a-8968-1b4d5acda4d3'
WHERE ingredient_id = 'c01dc184-2b26-41bb-afe5-d943b64a4549';

-- Spaghetti
UPDATE recipe_ingredients SET ingredient_id = 'dd6cedee-9c05-4f7e-bbe2-404c903b2459'
WHERE ingredient_id = '77fda05c-b619-40d3-8233-4ec7e9d6e77e';

-- Tomate
UPDATE recipe_ingredients SET ingredient_id = '93d81d7f-6f54-4c10-a972-a7e127a5e774'
WHERE ingredient_id = 'e56b9ce9-b30b-4b70-9713-114e706ba4e3';

-- Tomatenmark
UPDATE recipe_ingredients SET ingredient_id = '658a6db2-ff7d-4b19-a923-4c8c1a724e8f'
WHERE ingredient_id = 'e28f4807-1fc6-4306-a5e0-83452cf0f63c';

-- Vanilleextrakt
UPDATE recipe_ingredients SET ingredient_id = 'b5bfda4b-4f6f-4d3b-8caf-d1b9123a074e'
WHERE ingredient_id = '44adb958-f2ee-4021-812b-8fe45995cb0a';

-- Vollkornbrot
UPDATE recipe_ingredients SET ingredient_id = '2e391e26-f063-4bd9-90aa-6f7c9cc33d81'
WHERE ingredient_id = '51891d77-1f58-49b5-b6af-2feba3fd4f90';

-- Vollmilch 3,5% → Vollmilch
UPDATE recipe_ingredients SET ingredient_id = '802c480d-e21d-4928-9307-010c6f4976ec'
WHERE ingredient_id = '97c4c89a-72d8-416e-9cb8-dee66c5273e0';

-- Weizenmehl Type 405
UPDATE recipe_ingredients SET ingredient_id = '98cc7e63-fa8e-4b5c-9377-575d98c3edd7'
WHERE ingredient_id = '2523b22a-81b1-4a79-87af-9d39f6419820';

-- Reis (ungekocht) → Basmati Reis (kcal 366 ~ 365)
UPDATE recipe_ingredients SET ingredient_id = '7bd6f123-e659-45ea-89e0-ec24e661e52e'
WHERE ingredient_id = 'f81dd14a-ad0a-4a57-a2fa-9711d32d0ccd';

-- Zimt gemahlen
UPDATE recipe_ingredients SET ingredient_id = 'b83a04a2-5841-40ef-9da1-56541e978139'
WHERE ingredient_id = 'bc20567a-7b3c-440b-8043-b8c8074a9f84';

-- Zitronensaft → Zitrone (nächstbeste)
UPDATE recipe_ingredients SET ingredient_id = '883af0ce-fdc4-4dc6-9cf2-f482fcd9c302'
WHERE ingredient_id = '01564485-94b9-4915-8213-e4ffaf81af29';

-- Zucchini
UPDATE recipe_ingredients SET ingredient_id = 'a374e69a-b4b1-4a9b-bafe-f2872279c93b'
WHERE ingredient_id = '91dfe232-7275-4985-82c4-4607dfc46fdc';

-- Zucker → Zucker weiß
UPDATE recipe_ingredients SET ingredient_id = '0c92f869-250e-4fed-afeb-2a71f88a0d8b'
WHERE ingredient_id = 'edecab43-6bc6-4413-b5f9-d0db7a76a117';

-- Zwiebel
UPDATE recipe_ingredients SET ingredient_id = '368687c6-b12a-4a2d-8940-b416775ca761'
WHERE ingredient_id = '5216f9fa-1822-42ca-aad7-64b958f3fb2f';

-- Weißkohl
UPDATE recipe_ingredients SET ingredient_id = 'f47b9718-b00d-4317-9958-839f81c7805d'
WHERE ingredient_id = '6a347d86-abf8-4fca-b6f5-5d7e7f2c36d2';

-- Hühnerbrühe → keine FDC-Entsprechung, aus Rezept entfernen
DELETE FROM recipe_ingredients WHERE ingredient_id = '1beff526-4b19-48ab-b9e7-760b979dc4dc';

-- === UNMATCHED (keine Rezept-Referenzen, direkt löschbar) ===
-- Keine mehr übrig.

-- ─── STEP 2: Prüfe dass keine Rezepte mehr auf MANUAL zeigen ───
\echo '=== Verbleibende MANUAL-Refs nach Remap ==='
SELECT COUNT(*) AS remaining_manual_refs
FROM recipe_ingredients ri
JOIN ingredients i ON i.id = ri.ingredient_id
WHERE i.source = 'MANUAL';

-- ─── STEP 3: Lösche alle MANUAL-Ingredients ───
\echo '=== Lösche MANUAL-Ingredients ==='
DELETE FROM ingredients WHERE source = 'MANUAL';

\echo '=== Gelöscht ==='
COMMIT;
\echo '=== FERTIG: Alle MANUAL-Ingredients gemappt & gelöscht ==='
