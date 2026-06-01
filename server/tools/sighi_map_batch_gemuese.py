"""
Batch 4: Gemüse (157 entries, sighi_idx 187-343).
Includes vegetables + herbs (Kräuter were parser-categorized as Gemüse).
Conservative mappings; many alphabet-synonym dubletten marked DUPLICATE_OF=N.
"""
import csv
import os

OUT = os.path.join(os.path.dirname(__file__), 'sighi_usda_mapping.csv')

# rows = (sighi_idx, sighi_keyword, sighi_score, sighi_source_ref, usda_fdc_id, usda_name_de, usda_name_en, match_quality, reasoning)
ROWS = [
    # --- Hülsenfrüchte / Bohnen --------------------------------------------
    (187, 'Ackerbohne (Vicia faba)', '2', 'SIGHI-Leaflet v2.0 (2017)', '175205', 'Puffbohnen (Favabohnen), reife Samen, roh', 'Broadbeans (fava beans), mature seeds, raw', 'EXACT', 'Ackerbohne = Vicia faba = Fava/Puffbohne; DUPLICATE_OF=220 (Favabohne).'),
    # --- Salate --------------------------------------------
    (188, 'Ackersalat', '0', 'SIGHI-Leaflet v2.0 (2017)', '169219', 'Mais-Salat, roh', 'Cornsalad, raw', 'EXACT', 'Ackersalat = Feldsalat = Valerianella locusta = Cornsalad/Mâche.'),
    (189, 'Artischocke', '0', 'SIGHI-Leaflet v2.0 (2017)', '169205', 'Artischocken (Globus oder französisch), roh', 'Artichokes, (globe or french), raw', 'EXACT', 'Globe artichoke raw, klare 1:1.'),
    (190, 'Arugula, Rucola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'Arugula = Rucola = Eruca sativa raw.'),
    (191, 'Aubergine', '2', 'SIGHI-Leaflet v2.0 (2017)', '169228', 'Aubergine, roh', 'Eggplant, raw', 'EXACT', 'Aubergine = Eggplant raw, kanonische Form (2685577 ist Sub-Sample).'),
    (192, 'Avocado', '2', 'SIGHI-Leaflet v2.0 (2017)', '171706', 'Avocados, roh, Kalifornien', 'Avocados, raw, California', 'EXACT', 'California-Sorte als Default (kommerziell dominant); Florida ist Alternative.'),
    (193, 'Bambussprossen', '?', 'SIGHI-Leaflet v2.0 (2017)', '169210', 'Bambussprossen, roh', 'Bamboo shoots, raw', 'EXACT', 'Bamboo shoots raw, klare 1:1.'),
    (194, 'Blattsalate', '0', 'SIGHI-Leaflet v2.0 (2017)', '169248', 'Salat, Eisbergsalat (einschließlich knackiger Sorten), roh', 'Lettuce, iceberg (includes crisphead types), raw', 'APPROX', 'Generischer Sammelbegriff Blattsalat. Iceberg als gängiger Default; Salat ist nicht eine USDA-Generic. Caveat: Eintrag deckt diverse Sorten ab.'),
    (195, 'Blattsellerie, Schnittsellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'NEAR_EXACT', 'Schnittsellerie/Blattsellerie = leaf celery (Apium graveolens var. secalinum). USDA hat nur generisches Celery (Stangenform) raw — Nährwert-Approx.'),
    (196, 'Blaukabis, Blaukraut, Rotkohl', '0', 'SIGHI-Leaflet v2.0 (2017)', '169977', 'Kohl, rot, roh', 'Cabbage, red, raw', 'EXACT', 'Red cabbage raw (169977 kanonisch, 2346408 Sub-Sample).'),
    (197, 'Bleichsellerie, Staudensellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'EXACT', 'Bleichsellerie/Staudensellerie = Apium graveolens var. dulce = Celery stalks raw.'),
    (198, 'Blumenkohl', '0', 'SIGHI-Leaflet v2.0 (2017)', '169986', 'Blumenkohl, roh', 'Cauliflower, raw', 'EXACT', 'Cauliflower raw (kanonisch, 2685573 Sub-Sample).'),
    (199, 'Bohnen allgemein, diverse Arten', '2', 'SIGHI-Leaflet v2.0 (2017)', '175193', 'Bohnen, Nieren, alle Arten, reife Samen, roh', 'Beans, kidney, all types, mature seeds, raw', 'APPROX', 'Generischer Sammelbegriff "Bohnen". Kidney als gängiger Default; deckt nicht alle Bohnenarten ab. Spezifische Sorten haben eigene SIGHI-Einträge.'),
    (200, 'Bohnen: Borlotti-Bohnen', '2', 'SIGHI-Leaflet v2.0 (2017)', '173744', 'Bohnen, bunt, reife Samen, in Dosen, fest und flüssig', 'Beans, pinto, mature seeds, canned, solids and liquids', 'APPROX', 'Borlotti (Cranberry/Roman beans) nicht direkt in USDA; Pinto ist die nächste Verwandte (gleiche Sorte Phaseolus vulgaris, ähnliches Aussehen/Nährstoffe). Caveat: Botanisch eng verwandt aber nicht identisch.'),
    (201, 'Bohnen: Buschbohnen, Grüne Bohnen', '1', 'SIGHI-Leaflet v2.0 (2017)', '2346400', 'Bohnen, Snap, grün, roh', 'Beans, snap, green, raw', 'EXACT', 'Green snap beans = Buschbohnen/Grüne Bohnen raw.'),
    (202, 'Braunkohl, Grünkohl, Krauskohl, Federkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '168421', 'Grünkohl, roh', 'Kale, raw', 'EXACT', 'Kale raw (168421 kanonisch, 323505 Sub-Sample).'),
    (203, 'Brennnessel', '2', 'SIGHI-Leaflet v2.0 (2017)', '169819', 'Brennnesseln, blanchiert (Indianer der nördlichen Prärie)', 'Stinging Nettles, blanched (Northern Plains Indians)', 'NEAR_EXACT', 'Einziger Brennnessel-Eintrag in USDA; blanchiert statt roh — Nährwerte leicht abweichend.'),
    (204, 'Broccoli, Brokkoli', '0', 'SIGHI-Leaflet v2.0 (2017)', '170379', 'Brokkoli, roh', 'Broccoli, raw', 'EXACT', 'Broccoli raw, klare 1:1.'),
    (205, 'Brüsseler Kohl, Rosenkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '170383', 'Rosenkohl, roh', 'Brussels sprouts, raw', 'EXACT', 'Brussels sprouts raw (170383 kanonisch, 2685575 Sub-Sample).'),
    (206, 'Buschbohnen, Grüne Bohnen', '1', 'SIGHI-Leaflet v2.0 (2017)', '2346400', 'Bohnen, Snap, grün, roh', 'Beans, snap, green, raw', 'EXACT', 'DUPLICATE_OF=201 (Buschbohnen alphabet-doublet).'),
    (207, 'Chayote', '?', 'SIGHI-Leaflet v2.0 (2017)', '170402', 'Chayote, Früchte, roh', 'Chayote, fruit, raw', 'EXACT', 'Chayote raw, klare 1:1.'),
    (208, 'Chicorée (Cichorium intybus)', '0', 'SIGHI-Leaflet v2.0 (2017)', '170404', 'Chicoree, Chicorée, roh', 'Chicory, witloof, raw', 'EXACT', 'Witloof Chicory = der klassische Chicorée-Sprössling.'),
    (209, 'Chilisauce, scharf, fermentiert', '2', 'SIGHI-Leaflet v2.0 (2017)', '171186', 'Soße, scharfe Chilis, Sriracha', 'Sauce, hot chile, sriracha', 'NEAR_EXACT', 'Sriracha = fermentierte scharfe Chilisauce, paradigmatisches Beispiel.'),
    (210, 'Chilischoten, scharf, frisch', '1', 'SIGHI-Leaflet v2.0 (2017)', '169395', 'Paprika, Serrano, roh', 'Peppers, serrano, raw', 'NEAR_EXACT', 'Serrano = scharfe Chili-Schote raw als Vertreter; USDA hat keine generische "hot chili pepper raw".'),
    (211, 'Chinakohl', '0', 'SIGHI-Leaflet v2.0 (2017)', '170390', 'Kohl, chinesisch (pak-choi), roh', 'Cabbage, chinese (pak-choi), raw', 'NEAR_EXACT', 'Chinakohl ist eigentlich Napa Cabbage (Brassica rapa subsp. pekinensis). USDA hat nur Pak-Choi (subsp. chinensis) als chinesischen Kohl — verwandt aber nicht identisch.'),
    (212, 'Chinesischer Senfkohl, Chinesischer Blätterkohl, Pak', '0', 'SIGHI-Leaflet v2.0 (2017)', '170390', 'Kohl, chinesisch (pak-choi), roh', 'Cabbage, chinese (pak-choi), raw', 'EXACT', 'Pak-Choi raw, klare 1:1.'),
    (213, 'Cornichons, eingelegt', '2', 'SIGHI-Leaflet v2.0 (2017)', '168558', 'Essiggurken, Gurken, Dill oder koscherer Dill', 'Pickles, cucumber, dill or kosher dill', 'NEAR_EXACT', 'Cornichons = kleine Essiggurken; USDA hat keine spezifische Cornichon-Variante, Dill-Pickles ist nahester Vertreter.'),
    (214, 'Eierfrucht, Eierpflanze, Aubergine', '2', 'SIGHI-Leaflet v2.0 (2017)', '169228', 'Aubergine, roh', 'Eggplant, raw', 'EXACT', 'DUPLICATE_OF=191 (Aubergine alphabet-doublet).'),
    (215, 'Eisbergsalat', '0', 'SIGHI-Leaflet v2.0 (2017)', '169248', 'Salat, Eisbergsalat (einschließlich knackiger Sorten), roh', 'Lettuce, iceberg (includes crisphead types), raw', 'EXACT', 'Iceberg lettuce raw, klare 1:1.'),
    (216, 'Endiviensalat, Endivie (Cichorium endivia)', '0', 'SIGHI-Leaflet v2.0 (2017)', '168412', 'Endivie, roh', 'Endive, raw', 'EXACT', 'Endive raw, klare 1:1.'),
    (217, 'Erbsen: Gartenerbsen', '1', 'SIGHI-Leaflet v2.0 (2017)', '170419', 'Erbsen, grün, roh', 'Peas, green, raw', 'EXACT', 'Green peas raw = Gartenerbsen (Pisum sativum).'),
    (218, 'Essiggemüse', '2', 'SIGHI-Leaflet v2.0 (2017)', '', '', '', 'NO_MATCH', 'Generischer Sammelbegriff für eingelegtes Gemüse. Keine USDA-Generic. Spezifische Varianten (Essiggurken, eingelegte Paprika) sind separat erfasst.'),
    (219, 'Essiggurken, eingelegt', '2', 'SIGHI-Leaflet v2.0 (2017)', '168558', 'Essiggurken, Gurken, Dill oder koscherer Dill', 'Pickles, cucumber, dill or kosher dill', 'EXACT', 'Dill pickles = klassische Essiggurken.'),
    (220, 'Favabohne (Vicia faba)', '2', 'SIGHI-Leaflet v2.0 (2017)', '175205', 'Puffbohnen (Favabohnen), reife Samen, roh', 'Broadbeans (fava beans), mature seeds, raw', 'EXACT', 'Fava beans raw, klare 1:1.'),
    (221, 'Feldsalat', '0', 'SIGHI-Leaflet v2.0 (2017)', '169219', 'Mais-Salat, roh', 'Cornsalad, raw', 'EXACT', 'DUPLICATE_OF=188 (Ackersalat alphabet-doublet); Cornsalad = Valerianella locusta = Mâche.'),
    (222, 'Fenchel', '0', 'SIGHI-Leaflet v2.0 (2017)', '169385', 'Fenchel, Zwiebel, roh', 'Fennel, bulb, raw', 'EXACT', 'Fennel bulb raw (169385 kanonisch, 2747655 Sub-Sample).'),
    (223, 'Fisolen, Buschbohne (Phaseolus vulgaris var. nanus)', '1', 'SIGHI-Leaflet v2.0 (2017)', '2346400', 'Bohnen, Snap, grün, roh', 'Beans, snap, green, raw', 'EXACT', 'Fisolen (österr.) = Grüne Bohnen; DUPLICATE_OF=201.'),
    (224, 'Gartenerbsen (Pisum sativum = Lathyrus oleraceus)', '1', 'SIGHI-Leaflet v2.0 (2017)', '170419', 'Erbsen, grün, roh', 'Peas, green, raw', 'EXACT', 'DUPLICATE_OF=217 (Gartenerbsen alphabet-doublet).'),
    (225, 'Gartenkresse, Garten-Kresse (Lepidium sativum)', '?', 'SIGHI-Leaflet v2.0 (2017)', '168407', 'Kresse, Garten, roh', 'Cress, garden, raw', 'EXACT', 'Garden cress raw, klare 1:1.'),
    (226, 'Garten-Senfrauke, Rucola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'DUPLICATE_OF=190 (Rucola alphabet-doublet).'),
    (227, 'Gemüse-Eibisch, Okra', '1', 'SIGHI-Leaflet v2.0 (2017)', '169260', 'Okra, roh', 'Okra, raw', 'EXACT', 'Okra raw, klare 1:1.'),
    (228, 'Gewürzgurken, eingelegt', '2', 'SIGHI-Leaflet v2.0 (2017)', '168558', 'Essiggurken, Gurken, Dill oder koscherer Dill', 'Pickles, cucumber, dill or kosher dill', 'EXACT', 'Gewürzgurken = Dill pickles; DUPLICATE_OF=219.'),
    (229, 'Grünkohl, Braunkohl, Krauskohl, Federkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '168421', 'Grünkohl, roh', 'Kale, raw', 'EXACT', 'DUPLICATE_OF=202 (Grünkohl alphabet-doublet).'),
    (230, 'Gurken: Gartengurke, Salatgurke, Kukumer', '0', 'SIGHI-Leaflet v2.0 (2017)', '169225', 'Gurke, mit Schale, roh', 'Cucumber, with peel, raw', 'EXACT', 'Cucumber raw with peel; (169225 ist die kanonische Form für frische Salatgurke).'),
    (231, 'Herbstrübe', '1', 'SIGHI-Leaflet v2.0 (2017)', '170465', 'Rüben, roh', 'Turnips, raw', 'EXACT', 'Herbstrübe = Speiserübe = Turnip (Brassica rapa subsp. rapa) raw.'),
    (232, 'Hülsenfrüchte (Soja, Bohnen, Erbsen, Linsen, ...)', '2', 'SIGHI-Leaflet v2.0 (2017)', '', '', '', 'NO_MATCH', 'Generischer Sammelbegriff für alle Hülsenfrüchte. Keine USDA-Generic. Spezifische Sorten (Soja 292, Linsen 252, Erbsen 217, Bohnen 199...) sind separat erfasst.'),
    (233, 'Kaiserschote', '1', 'SIGHI-Leaflet v2.0 (2017)', '170010', 'Erbsen, genießbare Schoten, roh', 'Peas, edible-podded, raw', 'EXACT', 'Kaiserschote = Zuckerschote = Snow pea = edible-podded peas raw.'),
    (234, 'Kappes, Kohl, Kraut, Weißkohl', '0', 'SIGHI-Leaflet v2.0 (2017)', '169975', 'Kohl, roh', 'Cabbage, raw', 'EXACT', 'Generic Kohl = Weißkohl raw.'),
    (235, 'Karfiol, Blumenkohl', '0', 'SIGHI-Leaflet v2.0 (2017)', '169986', 'Blumenkohl, roh', 'Cauliflower, raw', 'EXACT', 'Karfiol (österr.) = Blumenkohl = Cauliflower; DUPLICATE_OF=198.'),
    (236, 'Karotte', '0', 'SIGHI-Leaflet v2.0 (2017)', '170393', 'Karotten, roh', 'Carrots, raw', 'EXACT', 'Carrots raw, klare 1:1.'),
    (237, 'Kefen', '1', 'SIGHI-Leaflet v2.0 (2017)', '170010', 'Erbsen, genießbare Schoten, roh', 'Peas, edible-podded, raw', 'EXACT', 'Kefen (schweiz.) = Zuckerschote = Snow pea; DUPLICATE_OF=233.'),
    (238, 'Kelp (große Braunalgen, Seetang, Blatttang,', '2', 'SIGHI-Leaflet v2.0 (2017)', '168457', 'Seetang, Seetang, roh', 'Seaweed, kelp, raw', 'EXACT', 'Kelp seaweed raw, klare 1:1.'),
    (239, 'Kichererbse', '2', 'SIGHI-Leaflet v2.0 (2017)', '173756', 'Kichererbsen (Garbanzo-Bohnen, Bengalisches Gramm), reife Samen, roh', 'Chickpeas (garbanzo beans, bengal gram), mature seeds, raw', 'EXACT', 'Chickpeas raw; ID via Standard-USDA (171287 ist Mehl-Variante in Kandidaten).'),
    (240, 'Kiefelerbse', '1', 'SIGHI-Leaflet v2.0 (2017)', '170419', 'Erbsen, grün, roh', 'Peas, green, raw', 'APPROX', 'Kiefelerbse = regionaler Begriff für (vermutlich) Gartenerbse. Unsichere Identifikation; behandelt wie generic green peas. Caveat: möglicherweise eigene Sorte gemeint.'),
    (241, 'Knoblauch', '1', 'SIGHI-Leaflet v2.0 (2017)', '169230', 'Knoblauch, roh', 'Garlic, raw', 'EXACT', 'Garlic raw (169230 kanonisch, 1104647 Sub-Sample).'),
    (242, 'Knollensellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '170400', 'Knollensellerie, roh', 'Celeriac, raw', 'EXACT', 'Celeriac raw, klare 1:1.'),
    (243, 'Kohlrabi', '1', 'SIGHI-Leaflet v2.0 (2017)', '168424', 'Kohlrabi, roh', 'Kohlrabi, raw', 'EXACT', 'Kohlrabi raw, klare 1:1 (170061 ist Turnip-greens Falschmatch).'),
    (244, 'Kohlsorten (außer Rosenkohl, Kohlrabi)', '0', 'SIGHI-Leaflet v2.0 (2017)', '169975', 'Kohl, roh', 'Cabbage, raw', 'APPROX', 'Generischer Sammelbegriff. USDA Cabbage (Weißkohl) als Default; deckt nicht alle Kohlsorten ab.'),
    (245, 'Kohlsprossen, Rosenkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '170383', 'Rosenkohl, roh', 'Brussels sprouts, raw', 'EXACT', 'Kohlsprossen (österr.) = Rosenkohl; DUPLICATE_OF=205.'),
    (246, 'Krauskohl, Grünkohl, Braunkohl, Federkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '168421', 'Grünkohl, roh', 'Kale, raw', 'EXACT', 'DUPLICATE_OF=202 (Grünkohl alphabet-doublet).'),
    (247, 'Krautstiele (Beta vulgaris subsp. vulgaris)', '1', 'SIGHI-Leaflet v2.0 (2017)', '169991', 'Mangold, schweizerisch, roh', 'Chard, swiss, raw', 'NEAR_EXACT', 'Krautstiele = Mangold-Stiele; gleicher Botanik-Eintrag wie Mangold (Swiss chard).'),
    (248, 'Kren, Meerrettich', '1', 'SIGHI-Leaflet v2.0 (2017)', '173472', 'Meerrettich, zubereitet', 'Horseradish, prepared', 'NEAR_EXACT', 'USDA hat nur prepared horseradish (mit Essig/Salz), nicht roh. Caveat: Nährwerte leicht abweichend von roher Wurzel.'),
    (249, 'Kresse: Gartenkresse (Lepidium sativum)', '?', 'SIGHI-Leaflet v2.0 (2017)', '168407', 'Kresse, Garten, roh', 'Cress, garden, raw', 'EXACT', 'DUPLICATE_OF=225 (Gartenkresse alphabet-doublet).'),
    (250, 'Kürbisse, div. Sorten', '0', 'SIGHI-Leaflet v2.0 (2017)', '168448', 'Kürbis, Sommer, alle Sorten, roh', 'Squash, summer, all varieties, raw', 'APPROX', 'Generischer Kürbis-Sammelbegriff. USDA Summer Squash all varieties als Default; deckt Winter-Sorten (Hokkaido, Butternut etc.) nicht ab.'),
    (251, 'Lauch', '1', 'SIGHI-Leaflet v2.0 (2017)', '169246', 'Lauch, (Zwiebel und unterer Blattteil), roh', 'Leeks, (bulb and lower leaf-portion), raw', 'EXACT', 'Leeks raw, klare 1:1.'),
    (252, 'Linsen', '2', 'SIGHI-Leaflet v2.0 (2017)', '172420', 'Linsen, roh', 'Lentils, raw', 'EXACT', 'Lentils raw, klare 1:1.'),
    (253, 'Mangold', '1', 'SIGHI-Leaflet v2.0 (2017)', '169991', 'Mangold, schweizerisch, roh', 'Chard, swiss, raw', 'EXACT', 'Swiss chard raw = Mangold.'),
    (254, 'Meerrettich', '1', 'SIGHI-Leaflet v2.0 (2017)', '173472', 'Meerrettich, zubereitet', 'Horseradish, prepared', 'NEAR_EXACT', 'DUPLICATE_OF=248 (Kren alphabet-doublet).'),
    (255, 'Melanzani, Melanzana, Aubergine', '2', 'SIGHI-Leaflet v2.0 (2017)', '169228', 'Aubergine, roh', 'Eggplant, raw', 'EXACT', 'Melanzani (österr./ital.) = Aubergine; DUPLICATE_OF=191.'),
    (256, 'Möhre, Mohrrübe', '0', 'SIGHI-Leaflet v2.0 (2017)', '170393', 'Karotten, roh', 'Carrots, raw', 'EXACT', 'Möhre = Karotte; DUPLICATE_OF=236.'),
    (257, 'Mungbohnen, Mungobohnen (-keimlinge/-sprossen)', '?', 'SIGHI-Leaflet v2.0 (2017)', '174256', 'Mungobohnen, reife Samen, roh', 'Mung beans, mature seeds, raw', 'EXACT', 'Mung beans raw, klare 1:1.'),
    (258, 'Nüsslisalat, Nüssler', '0', 'SIGHI-Leaflet v2.0 (2017)', '169219', 'Mais-Salat, roh', 'Cornsalad, raw', 'EXACT', 'Nüsslisalat (schweiz.) = Feldsalat = Cornsalad; DUPLICATE_OF=188.'),
    (259, 'Okra, Gemüse-Eibisch', '1', 'SIGHI-Leaflet v2.0 (2017)', '169260', 'Okra, roh', 'Okra, raw', 'EXACT', 'DUPLICATE_OF=227 (Okra alphabet-doublet).'),
    (260, 'Oliven', '2', 'SIGHI-Leaflet v2.0 (2017)', '169094', 'Oliven, eingelegt, in Dosen oder Flaschen, schwarz, jumbo-superkolossal', 'Olives, pickled, canned or bottled, black, jumbo-super colossal', 'NEAR_EXACT', 'Schwarze Tafeloliven als Default; USDA hat keine generische "Oliven roh" (kommerziell immer eingelegt).'),
    (261, 'Pak Choi, Pak Choy, Pok Choi', '0', 'SIGHI-Leaflet v2.0 (2017)', '170390', 'Kohl, chinesisch (pak-choi), roh', 'Cabbage, chinese (pak-choi), raw', 'EXACT', 'DUPLICATE_OF=212 (Pak Choi alphabet-doublet).'),
    (262, 'Paprika, Peperoni, milde Sorten', '0', 'SIGHI-Leaflet v2.0 (2017)', '170427', 'Paprika, süß, grün, roh', 'Peppers, sweet, green, raw', 'EXACT', 'Sweet green pepper raw als Vertreter der milden Sorte.'),
    (263, 'Paprika, Peperoni, scharfe Sorten', '2', 'SIGHI-Leaflet v2.0 (2017)', '169395', 'Paprika, Serrano, roh', 'Peppers, serrano, raw', 'NEAR_EXACT', 'Serrano als Vertreter der scharfen Sorten; USDA hat keine generische "hot pepper raw".'),
    (264, 'Paradeiser, Tomaten', '2', 'SIGHI-Leaflet v2.0 (2017)', '170457', 'Tomaten, rot, reif, roh, ganzjähriger Durchschnitt', 'Tomatoes, red, ripe, raw, year round average', 'EXACT', 'Paradeiser (österr.) = Tomate; rote reife Tomate raw als Standard.'),
    (265, 'Pastinaken', '0', 'SIGHI-Leaflet v2.0 (2017)', '170417', 'Pastinaken, roh', 'Parsnips, raw', 'EXACT', 'Parsnips raw (170417 kanonisch, 2747659 Sub-Sample).'),
    (266, 'Peperoni, Paprika, milde Sorten', '0', 'SIGHI-Leaflet v2.0 (2017)', '170427', 'Paprika, süß, grün, roh', 'Peppers, sweet, green, raw', 'EXACT', 'DUPLICATE_OF=262 (Paprika mild alphabet-doublet).'),
    (267, 'Peperoni, Paprika, scharfe Sorten', '2', 'SIGHI-Leaflet v2.0 (2017)', '169395', 'Paprika, Serrano, roh', 'Peppers, serrano, raw', 'NEAR_EXACT', 'DUPLICATE_OF=263 (Paprika scharf alphabet-doublet).'),
    (268, 'Porree', '1', 'SIGHI-Leaflet v2.0 (2017)', '169246', 'Lauch, (Zwiebel und unterer Blattteil), roh', 'Leeks, (bulb and lower leaf-portion), raw', 'EXACT', 'Porree = Lauch; DUPLICATE_OF=251.'),
    (269, 'Räbe', '1', 'SIGHI-Leaflet v2.0 (2017)', '170465', 'Rüben, roh', 'Turnips, raw', 'EXACT', 'Räbe (schweiz.) = Speiserübe = Turnip; DUPLICATE_OF=231.'),
    (270, 'Radieschen und Rettiche (Gattung Raphanus), milde', '0', 'SIGHI-Leaflet v2.0 (2017)', '169276', 'Radieschen, roh', 'Radishes, raw', 'EXACT', 'Radishes raw, klare 1:1.'),
    (271, 'Radieschen und Rettiche (Gattung Raphanus), scharfe', '1', 'SIGHI-Leaflet v2.0 (2017)', '169276', 'Radieschen, roh', 'Radishes, raw', 'NEAR_EXACT', 'USDA differenziert nicht zwischen mild/scharf bei Radieschen; gleicher Eintrag wie 270.'),
    (272, 'Randen', '0', 'SIGHI-Leaflet v2.0 (2017)', '2685576', 'Rote Bete, roh', 'Beets, raw', 'EXACT', 'Randen (schweiz.) = Rote Bete = Beets raw.'),
    (273, 'Rauke, Rucola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'DUPLICATE_OF=190 (Rucola alphabet-doublet).'),
    (274, 'Rettiche und Radieschen (Gattung Raphanus), milde', '0', 'SIGHI-Leaflet v2.0 (2017)', '169276', 'Radieschen, roh', 'Radishes, raw', 'EXACT', 'DUPLICATE_OF=270 (Radieschen mild alphabet-doublet).'),
    (275, 'Rettiche und Radieschen (Gattung Raphanus),', '1', 'SIGHI-Leaflet v2.0 (2017)', '169276', 'Radieschen, roh', 'Radishes, raw', 'NEAR_EXACT', 'DUPLICATE_OF=271 (Radieschen scharf alphabet-doublet).'),
    (276, 'Rosenkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '170383', 'Rosenkohl, roh', 'Brussels sprouts, raw', 'EXACT', 'DUPLICATE_OF=205 (Rosenkohl alphabet-doublet).'),
    (277, 'Rote Bete, Rote Beete', '0', 'SIGHI-Leaflet v2.0 (2017)', '2685576', 'Rote Bete, roh', 'Beets, raw', 'EXACT', 'Beets raw, klare 1:1.'),
    (278, 'Rotkohl, Rotkraut', '0', 'SIGHI-Leaflet v2.0 (2017)', '169977', 'Kohl, rot, roh', 'Cabbage, red, raw', 'EXACT', 'DUPLICATE_OF=196 (Rotkohl alphabet-doublet).'),
    (279, 'Rucola, Rukola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'DUPLICATE_OF=190 (Rucola alphabet-doublet).'),
    (280, 'Rüebli', '0', 'SIGHI-Leaflet v2.0 (2017)', '170393', 'Karotten, roh', 'Carrots, raw', 'EXACT', 'Rüebli (schweiz.) = Karotte; DUPLICATE_OF=236.'),
    (281, 'Salat: Blattsalate', '0', 'SIGHI-Leaflet v2.0 (2017)', '169248', 'Salat, Eisbergsalat (einschließlich knackiger Sorten), roh', 'Lettuce, iceberg (includes crisphead types), raw', 'APPROX', 'DUPLICATE_OF=194 (Blattsalate alphabet-doublet).'),
    (282, 'Salzgurken', '2', 'SIGHI-Leaflet v2.0 (2017)', '169379', 'Gurken, sauer', 'Pickles, cucumber, sour', 'EXACT', 'Sour pickles = Salzgurken (Salz/Lake-fermentiert, ohne Essig).'),
    (283, 'Saubohne (Vicia faba)', '2', 'SIGHI-Leaflet v2.0 (2017)', '175205', 'Puffbohnen (Favabohnen), reife Samen, roh', 'Broadbeans (fava beans), mature seeds, raw', 'EXACT', 'Saubohne = Vicia faba = Favabohne; DUPLICATE_OF=220.'),
    (284, 'Sauerkraut', '3', 'SIGHI-Leaflet v2.0 (2017)', '169279', 'Sauerkraut, in Dosen, fest und flüssig', 'Sauerkraut, canned, solids and liquids', 'EXACT', 'Sauerkraut canned, klare 1:1.'),
    (285, 'Schmalblättriger Doppelsame, Rucola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'DUPLICATE_OF=190 (Rucola alphabet-doublet).'),
    (286, 'Schnittsellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'NEAR_EXACT', 'DUPLICATE_OF=195 (Schnittsellerie alphabet-doublet).'),
    (287, 'Schweinsbohne (Vicia faba)', '2', 'SIGHI-Leaflet v2.0 (2017)', '175205', 'Puffbohnen (Favabohnen), reife Samen, roh', 'Broadbeans (fava beans), mature seeds, raw', 'EXACT', 'Schweinsbohne (regional) = Vicia faba = Favabohne; DUPLICATE_OF=220.'),
    (288, 'Sellerie: Knollensellerie (Apium graveolens var.', '0', 'SIGHI-Leaflet v2.0 (2017)', '170400', 'Knollensellerie, roh', 'Celeriac, raw', 'EXACT', 'DUPLICATE_OF=242 (Knollensellerie alphabet-doublet).'),
    (289, 'Sellerie: Schnittsellerie (Apium graveolens var.', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'NEAR_EXACT', 'DUPLICATE_OF=195 (Schnittsellerie alphabet-doublet).'),
    (290, 'Sellerie: Staudensellerie (Apium graveolens var.', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'EXACT', 'DUPLICATE_OF=197 (Staudensellerie alphabet-doublet).'),
    (291, 'Senfkohl, Pak Choi', '0', 'SIGHI-Leaflet v2.0 (2017)', '170390', 'Kohl, chinesisch (pak-choi), roh', 'Cabbage, chinese (pak-choi), raw', 'EXACT', 'DUPLICATE_OF=212 (Pak Choi alphabet-doublet).'),
    (292, 'Soja (Sojabohne, Sojamehl)', '2', 'SIGHI-Leaflet v2.0 (2017)', '174270', 'Sojabohnen, reife Samen, roh', 'Soybeans, mature seeds, raw', 'EXACT', 'Soybeans mature raw als Default; Sojamehl ist Derivat.'),
    (293, 'Sojasprossen [irrtümliche Bezeichnung. Korrekt:', '?', 'SIGHI-Leaflet v2.0 (2017)', '174256', 'Mungobohnen, reife Samen, roh', 'Mung beans, mature seeds, raw', 'APPROX', 'SIGHI weist explizit darauf hin: "Sojasprossen" sind meist Mungbohnenkeimlinge. Gemappt auf Mung beans; DUPLICATE_OF=257 inhaltlich.'),
    (294, 'Spalterbsen, Gartenerbsen', '1', 'SIGHI-Leaflet v2.0 (2017)', '170419', 'Erbsen, grün, roh', 'Peas, green, raw', 'EXACT', 'DUPLICATE_OF=217 (Gartenerbsen alphabet-doublet).'),
    (295, 'Spargel', '0', 'SIGHI-Leaflet v2.0 (2017)', '168389', 'Spargel, roh', 'Asparagus, raw', 'EXACT', 'Asparagus raw (168389 kanonisch, 2710823 Sub-Sample).'),
    (296, 'Speiserübe', '1', 'SIGHI-Leaflet v2.0 (2017)', '170465', 'Rüben, roh', 'Turnips, raw', 'EXACT', 'DUPLICATE_OF=231 (Speiserübe alphabet-doublet).'),
    (297, 'Spinat', '2', 'SIGHI-Leaflet v2.0 (2017)', '168462', 'Spinat, roh', 'Spinach, raw', 'EXACT', 'Spinach raw, klare 1:1.'),
    (298, 'Sprossenkohl, Rosenkohl', '1', 'SIGHI-Leaflet v2.0 (2017)', '170383', 'Rosenkohl, roh', 'Brussels sprouts, raw', 'EXACT', 'DUPLICATE_OF=205 (Rosenkohl alphabet-doublet).'),
    (299, 'Stangensellerie, Staudensellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'EXACT', 'DUPLICATE_OF=197 (Staudensellerie alphabet-doublet).'),
    (300, 'Staudensellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'EXACT', 'DUPLICATE_OF=197 (Staudensellerie alphabet-doublet).'),
    (301, 'Tomaten', '2', 'SIGHI-Leaflet v2.0 (2017)', '170457', 'Tomaten, rot, reif, roh, ganzjähriger Durchschnitt', 'Tomatoes, red, ripe, raw, year round average', 'EXACT', 'DUPLICATE_OF=264 (Tomate alphabet-doublet).'),
    (302, 'Topinambur', '0', 'SIGHI-Leaflet v2.0 (2017)', '169236', 'Topinambur, roh', 'Jerusalem-artichokes, raw', 'EXACT', 'Jerusalem artichoke raw, klare 1:1.'),
    (303, 'Vogerlsalat', '0', 'SIGHI-Leaflet v2.0 (2017)', '169219', 'Mais-Salat, roh', 'Cornsalad, raw', 'EXACT', 'Vogerlsalat (österr.) = Feldsalat = Cornsalad; DUPLICATE_OF=188.'),
    (304, 'Weisskohl, Weisskabis, Weisskraut, Weißkohl,', '0', 'SIGHI-Leaflet v2.0 (2017)', '169975', 'Kohl, roh', 'Cabbage, raw', 'EXACT', 'Weißkohl = generic Cabbage; DUPLICATE_OF=234.'),
    (305, 'Welschkohl, Welschkraut, Wirsing', '1', 'SIGHI-Leaflet v2.0 (2017)', '170388', 'Kohl, Wirsing, roh', 'Cabbage, savoy, raw', 'EXACT', 'Savoy cabbage raw = Wirsing.'),
    (306, 'Wilde Rauke, Rucola', '2', 'SIGHI-Leaflet v2.0 (2017)', '169387', 'Rucola, roh', 'Arugula, raw', 'EXACT', 'DUPLICATE_OF=190 (Rucola alphabet-doublet).'),
    (307, 'Wirsing, Wirz, Wirsingkohl, Welschkohl,', '1', 'SIGHI-Leaflet v2.0 (2017)', '170388', 'Kohl, Wirsing, roh', 'Cabbage, savoy, raw', 'EXACT', 'DUPLICATE_OF=305 (Wirsing alphabet-doublet).'),
    (308, 'Würzsellerie, Schnittsellerie', '0', 'SIGHI-Leaflet v2.0 (2017)', '169988', 'Sellerie, roh', 'Celery, raw', 'NEAR_EXACT', 'DUPLICATE_OF=195 (Schnittsellerie alphabet-doublet).'),
    (309, 'Zichorie: Salatzichorie, Chicorée', '0', 'SIGHI-Leaflet v2.0 (2017)', '170404', 'Chicoree, Chicorée, roh', 'Chicory, witloof, raw', 'EXACT', 'DUPLICATE_OF=208 (Chicorée alphabet-doublet).'),
    (310, 'Zucchini, Zucchetti', '0', 'SIGHI-Leaflet v2.0 (2017)', '169291', 'Kürbis, Sommer, Zucchini, mit Schale, roh', 'Squash, summer, zucchini, includes skin, raw', 'EXACT', 'Zucchini raw with skin.'),
    (311, 'Zuckererbse, Zuckerschote', '1', 'SIGHI-Leaflet v2.0 (2017)', '170010', 'Erbsen, genießbare Schoten, roh', 'Peas, edible-podded, raw', 'EXACT', 'DUPLICATE_OF=233 (Zuckerschote alphabet-doublet).'),
    (312, 'Zwiebel: alle anderen, hier nicht als verträglich', '1', 'SIGHI-Leaflet v2.0 (2017)', '170000', 'Zwiebeln, roh', 'Onions, raw', 'APPROX', 'Generischer Catch-All für nicht-spezifizierte Zwiebelsorten. USDA Onions raw als Default.'),
    (313, 'Zwiebel: Milde Zwiebeln der Cevennen', '0', 'SIGHI-Leaflet v2.0 (2017)', '170008', 'Zwiebeln, süß, roh', 'Onions, sweet, raw', 'NEAR_EXACT', 'Cevennes-Zwiebel ist mild/süß; USDA Sweet onions als nähester Vertreter (keine exakte Cevennes-Erfassung).'),
    (314, 'Zwiebel: Tropea-Zwiebel', '0', 'SIGHI-Leaflet v2.0 (2017)', '790577', 'Zwiebeln, rot, roh', 'Onions, red, raw', 'NEAR_EXACT', 'Tropea = rote süße Zwiebelsorte aus Kalabrien; USDA Red Onions als nähester Vertreter.'),
    (315, 'Zwiebel: Weisse Zwiebel, Weiße Zwiebel', '0', 'SIGHI-Leaflet v2.0 (2017)', '170000', 'Zwiebeln, roh', 'Onions, raw', 'NEAR_EXACT', 'USDA Onions raw (generic, eher gelb); spezifische "white onion" hat eigene FDC IDs in Brands, aber generic 170000 als Default.'),
    # --- Kräuter (parser-categorized as Gemüse) --------------------------------------------
    (316, 'Ährige Minze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'Mentha spicata = Spearmint = Ährige/Grüne Minze.'),
    (317, 'Bärlauch (Allium ursinum)', '1', 'SIGHI-Leaflet v2.0 (2017)', '', '', '', 'NO_MATCH', 'Bärlauch (Allium ursinum, wild garlic/ramps) ist im USDA-Pool nicht erfasst (keine Treffer für "ramps" oder "wild garlic").'),
    (318, 'Basilikum', '0', 'SIGHI-Leaflet v2.0 (2017)', '172232', 'Basilikum, frisch', 'Basil, fresh', 'EXACT', 'Basil fresh, klare 1:1.'),
    (319, 'Bockshornklee', '2', 'SIGHI-Leaflet v2.0 (2017)', '171324', 'Gewürze, Bockshornkleesamen', 'Spices, fenugreek seed', 'EXACT', 'Fenugreek seed, klare 1:1.'),
    (320, 'Bohnenkraut (Satureja hortensis, Satureja montana)', '0', 'SIGHI-Leaflet v2.0 (2017)', '170936', 'Gewürze, Bohnenkraut, gemahlen', 'Spices, savory, ground', 'EXACT', 'Savory ground, klare 1:1.'),
    (321, 'Dill, Dillkraut, Dillfenchel', '1', 'SIGHI-Leaflet v2.0 (2017)', '172233', 'Dillkraut, frisch', 'Dill weed, fresh', 'EXACT', 'Dill weed fresh, klare 1:1.'),
    (322, 'Dorst, Dost, Gemeiner Dost, Dostenkraut, Oregano', '0', 'SIGHI-Leaflet v2.0 (2017)', '171328', 'Gewürze, Oregano, getrocknet', 'Spices, oregano, dried', 'EXACT', 'Oregano dried; USDA hat keine fresh oregano.'),
    (323, 'Echter Dost (Origanum vulgare), Oregano', '0', 'SIGHI-Leaflet v2.0 (2017)', '171328', 'Gewürze, Oregano, getrocknet', 'Spices, oregano, dried', 'EXACT', 'DUPLICATE_OF=322 (Oregano alphabet-doublet).'),
    (324, 'Echter Kerbel, Gartenkerbel (Anthriscus cerefolium)', '0', 'SIGHI-Leaflet v2.0 (2017)', '171318', 'Gewürze, Kerbel, getrocknet', 'Spices, chervil, dried', 'EXACT', 'Chervil dried, klare 1:1.'),
    (325, 'Gartenkerbe, Echter Kerbel (Anthriscus cerefolium)', '0', 'SIGHI-Leaflet v2.0 (2017)', '171318', 'Gewürze, Kerbel, getrocknet', 'Spices, chervil, dried', 'EXACT', 'DUPLICATE_OF=324 (Kerbel alphabet-doublet).'),
    (326, 'Grüne Minze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet).'),
    (327, 'Gurkenkraut, Dill (Anethum graveolens)', '1', 'SIGHI-Leaflet v2.0 (2017)', '172233', 'Dillkraut, frisch', 'Dill weed, fresh', 'EXACT', 'DUPLICATE_OF=321 (Dill alphabet-doublet).'),
    (328, 'Kerbel, Echter Kerbel, Gartenkerbel (Anthriscus', '0', 'SIGHI-Leaflet v2.0 (2017)', '171318', 'Gewürze, Kerbel, getrocknet', 'Spices, chervil, dried', 'EXACT', 'DUPLICATE_OF=324 (Kerbel alphabet-doublet).'),
    (329, 'Kleesorten (Trigonella- und Trifolium-Arten)', '2', 'SIGHI-Leaflet v2.0 (2017)', '', '', '', 'NO_MATCH', 'Generische Kleesorten (Klee als Futterpflanze/Sprossen) nicht im USDA-Pool. Bockshornklee separat (319).'),
    (330, 'Krause Minze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet).'),
    (331, 'Marokkanische Minze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet); Marokkanische ist Cultivar.'),
    (332, 'Minze: Grüne Minze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet).'),
    (333, 'Müllerkraut, Oregano', '0', 'SIGHI-Leaflet v2.0 (2017)', '171328', 'Gewürze, Oregano, getrocknet', 'Spices, oregano, dried', 'EXACT', 'DUPLICATE_OF=322 (Oregano alphabet-doublet).'),
    (334, 'Nanaminze (Mentha spicata)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet); Nana = arabischer Cultivar.'),
    (335, 'Oregano', '0', 'SIGHI-Leaflet v2.0 (2017)', '171328', 'Gewürze, Oregano, getrocknet', 'Spices, oregano, dried', 'EXACT', 'DUPLICATE_OF=322 (Oregano alphabet-doublet).'),
    (336, 'Petersilie', '0', 'SIGHI-Leaflet v2.0 (2017)', '170416', 'Petersilie, frisch', 'Parsley, fresh', 'EXACT', 'Parsley fresh, klare 1:1.'),
    (337, 'Pfefferminze', '0', 'SIGHI-Leaflet v2.0 (2017)', '173474', 'Pfefferminz, frisch', 'Peppermint, fresh', 'EXACT', 'Peppermint fresh = Mentha x piperita (unterschiedlich von Spearmint).'),
    (338, 'Rosmarin', '0', 'SIGHI-Leaflet v2.0 (2017)', '173473', 'Rosmarin, frisch', 'Rosemary, fresh', 'EXACT', 'Rosemary fresh, klare 1:1.'),
    (339, 'Salbei: Echte Salbei, Garten-Salbei, Küchensalbei', '0', 'SIGHI-Leaflet v2.0 (2017)', '171336', 'Gewürze, Salbei, gemahlen', 'Spices, sage, ground', 'NEAR_EXACT', 'USDA hat Sage nur als gemahlenes Gewürz, nicht fresh; Salvia officinalis Match.'),
    (340, 'Schabzigerklee', '2', 'SIGHI-Leaflet v2.0 (2017)', '', '', '', 'NO_MATCH', 'Schabzigerklee (Trigonella caerulea, blauer Steinklee) nicht im USDA-Pool. Verwandt mit Bockshornklee (319), aber eigene Art.'),
    (341, 'Schnittlauch', '1', 'SIGHI-Leaflet v2.0 (2017)', '169994', 'Schnittlauch, roh', 'Chives, raw', 'EXACT', 'Chives raw, klare 1:1.'),
    (342, 'Speer-Minze (Mentha spicata, engl.: spearmint)', '0', 'SIGHI-Leaflet v2.0 (2017)', '173475', 'Minze, frisch', 'Spearmint, fresh', 'EXACT', 'DUPLICATE_OF=316 (Spearmint alphabet-doublet).'),
    (343, 'Wohlgemut, Oregano', '0', 'SIGHI-Leaflet v2.0 (2017)', '171328', 'Gewürze, Oregano, getrocknet', 'Spices, oregano, dried', 'EXACT', 'DUPLICATE_OF=322 (Oregano alphabet-doublet).'),
]

# Read existing
existing = set()
with open(OUT, 'r', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=';')
    next(reader)
    for r in reader:
        if r:
            existing.add(int(r[0]))

# Append new
added = 0
with open(OUT, 'a', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', quoting=csv.QUOTE_MINIMAL)
    for row in ROWS:
        if row[0] in existing:
            continue
        w.writerow(row)
        added += 1

# Total
with open(OUT, 'r', encoding='utf-8') as f:
    total = sum(1 for _ in f) - 1
print(f"Appended {added} rows. Total now: {total}")
