"""Append Pflanzlich+Nüsse+Fette batch (sighi_idx 104..186, 83 entries) to mapping CSV."""
import csv
from pathlib import Path

OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv")

ROWS = [
    # ============ PFLANZLICH (50) ============
    (104, "Amarant", "0", "SIGHI_FoodList_2024-08-29_p8_r5", "170682", "Amaranth-Körner, ungekocht", "Amaranth grain, uncooked", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (105, "Backwaren", "1", "SIGHI_FoodList_2024-08-29_p8_r6", "167936", "Keikitos (Muffins), Latino-Backwaren", "Keikitos (muffins), Latino bakery item", "APPROX", "Backwaren ist breiter Sammelbegriff; USDA hat keinen generischen Eintrag, Muffin-Beispiel als nährwertliche Approximation."),
    (106, "Brot", "1", "SIGHI_FoodList_2024-08-29_p8_r7", "168013", "Brot, Mehrkornbrot (einschließlich Vollkornbrot)", "Bread, multi-grain (includes whole-grain)", "APPROX", "Generisches Brot; Mehrkornbrot als nährwertlicher Mittelwert zwischen Weiß- und Vollkornbrot."),
    (107, "Buchweizen: Echter Buchweizen, Gemeiner", "2", "SIGHI_FoodList_2024-08-29_p8_r9", "170286", "Buchweizen", "Buckwheat", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (108, "Bulgur, Boulghour, Bulghur", "1", "SIGHI_FoodList_2024-08-29_p8_r10", "170688", "Bulgur, trocken", "Bulgur, dry", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (109, "Cornflakes (ohne unverträgliche Zutaten wie Malz", "0", "SIGHI_FoodList_2024-08-29_p8_r12", "174648", "Verzehrfertige Getreideflocken, RALSTON Corn Flakes", "Cereals ready-to-eat, RALSTON Corn Flakes", "EXACT", "RALSTON Corn Flakes ist USDA-Standardreferenz für Cornflakes."),
    (110, "Dinkel", "0", "SIGHI_FoodList_2024-08-29_p8_r13", "169745", "Dinkel, ungekocht", "Spelt, uncooked", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (111, "Edelkastanien, frisch oder tiefgekühlt", "0", "SIGHI_FoodList_2024-08-29_p8_r14", "170574", "Nüsse, Kastanien, europäisch, roh, ungeschält", "Nuts, chestnuts, european, raw, unpeeled", "EXACT", "Edelkastanien = Esskastanien = europäische Kastanien."),
    (112, "Einkorn, Blicken, Kleiner Spelz", "0", "SIGHI_FoodList_2024-08-29_p8_r15", "2710827", "Einkorn, Getreide, trocken, roh", "Einkorn, grain, dry, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (113, "Emmer, Zweikorn", "0", "SIGHI_FoodList_2024-08-29_p8_r16", "2710827", "Einkorn, Getreide, trocken, roh", "Einkorn, grain, dry, raw", "APPROX", "USDA hat keinen Emmer (Triticum dicoccum); Einkorn ist nächstverwandtes Urgetreide mit vergleichbaren Makros."),
    (114, "Erdäpfel, frisch geerntet, mit Schale", "0", "SIGHI_FoodList_2024-08-29_p8_r17", "170026", "Kartoffeln, Fruchtfleisch und Schale, roh", "Potatoes, flesh and skin, raw", "EXACT", "Erdäpfel = österr. für Kartoffeln; mit Schale roh."),
    (115, "Erdäpfel, gelagert, geschält", "0", "SIGHI_FoodList_2024-08-29_p8_r18", "2346401", "Kartoffeln, rotbraun, ohne Schale, roh", "Potatoes, russet, without skin, raw", "EXACT", "Geschälte rohe Kartoffel; russet als USDA-Standard."),
    (116, "Erdäpfel, gelagert, mit Schale", "0", "SIGHI_FoodList_2024-08-29_p8_r19", "170026", "Kartoffeln, Fruchtfleisch und Schale, roh", "Potatoes, flesh and skin, raw", "EXACT", "Mit Schale roh; selbe USDA-Quelle wie #114. DUPLICATE_OF=114."),
    (117, "Esskastanien, frisch oder tiefgekühlt", "0", "SIGHI_FoodList_2024-08-29_p8_r20", "170574", "Nüsse, Kastanien, europäisch, roh, ungeschält", "Nuts, chestnuts, european, raw, unpeeled", "EXACT", "Synonym zu Edelkastanien #111. DUPLICATE_OF=111."),
    (118, "Gerste", "1", "SIGHI_FoodList_2024-08-29_p8_r21", "170283", "Gerste, geschält", "Barley, hulled", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (119, "Gerstenmalz, Malz, Malzextrakt", "2", "SIGHI_FoodList_2024-08-29_p8_r22", "169740", "Gerstenmalzmehl", "Barley malt flour", "NEAR_EXACT", "USDA hat kein gemälztes Korn direkt, nur Malzmehl; Makros vergleichbar."),
    (120, "Grünkern, Grünkorn", "0", "SIGHI_FoodList_2024-08-29_p8_r24", "169745", "Dinkel, ungekocht", "Spelt, uncooked", "NEAR_EXACT", "Grünkern = unreif geernteter Dinkel, geröstet; nährwertlich ähnlich zu Dinkel."),
    (121, "Hafer, Haferflocken, Hafermehl", "0", "SIGHI_FoodList_2024-08-29_p8_r25", "2346396", "Hafer, Vollkorn, gewalzt, altmodisch", "Oats, whole grain, rolled, old fashioned", "EXACT", "Klassische gewalzte Haferflocken."),
    (122, "Hanfproteinpulver", "0", "SIGHI_FoodList_2024-08-29_p8_r26", "", "", "", "NO_MATCH", "USDA hat kein Hanfprotein-Pulver; auch keine vergleichbare Nährstoff-Matrix (>40% Protein, Hanf-spezifisch). Score gesetzt, Nährwerte NULL."),
    (123, "Hanfsamen (Cannabis sativa)", "0", "SIGHI_FoodList_2024-08-29_p8_r27", "170148", "Samen, Hanfsamen, geschält", "Seeds, hemp seed, hulled", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (124, "Hirse", "0", "SIGHI_FoodList_2024-08-29_p8_r28", "169702", "Hirse, roh", "Millet, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (125, "KAMUT®, Khorasan-Weizen", "0", "SIGHI_FoodList_2024-08-29_p8_r30", "169743", "Weizen, Khorasan, ungekocht", "Wheat, khorasan, uncooked", "EXACT", "Direkte 1:1-Übereinstimmung (KAMUT® ist Marke für Khorasan-Weizen)."),
    (126, "Kartoffel, frisch geerntet, mit Schale", "0", "SIGHI_FoodList_2024-08-29_p8_r31", "170026", "Kartoffeln, Fruchtfleisch und Schale, roh", "Potatoes, flesh and skin, raw", "EXACT", "Synonym zu Erdäpfel #114. DUPLICATE_OF=114."),
    (127, "Kartoffel, gelagert, geschält", "0", "SIGHI_FoodList_2024-08-29_p8_r32", "2346401", "Kartoffeln, rotbraun, ohne Schale, roh", "Potatoes, russet, without skin, raw", "EXACT", "Synonym zu Erdäpfel #115. DUPLICATE_OF=115."),
    (128, "Kartoffel, gelagert, mit Schale", "0", "SIGHI_FoodList_2024-08-29_p8_r33", "170026", "Kartoffeln, Fruchtfleisch und Schale, roh", "Potatoes, flesh and skin, raw", "EXACT", "Synonym zu #114/#116. DUPLICATE_OF=114."),
    (129, "Khorasan-Weizen, KAMUT®", "0", "SIGHI_FoodList_2024-08-29_p8_r34", "169743", "Weizen, Khorasan, ungekocht", "Wheat, khorasan, uncooked", "EXACT", "Alphabet-Dublette zu #125. DUPLICATE_OF=125."),
    (130, "Kukuruz, getrocknet: Maisgriess, Maisgrieß,", "0", "SIGHI_FoodList_2024-08-29_p8_r35", "170290", "Maismehl, Vollkorn, gelb", "Corn flour, whole-grain, yellow", "EXACT", "Kukuruz = österr. für Mais; getrocknet/gemahlen = Maismehl."),
    (131, "Kukuruz: Mais aus der Dose, Dosenmais", "0", "SIGHI_FoodList_2024-08-29_p8_r36", "169346", "Mais, süß, gelb, in Dosen, cremefarben, ohne Salzzusatz", "Corn, sweet, yellow, canned, cream style, no salt added", "EXACT", "Dosenmais ohne Salzzusatz als USDA-Default."),
    (132, "Kukuruz: Maiskörner, Maiskolben frisch/pasteurisiert", "0", "SIGHI_FoodList_2024-08-29_p8_r37", "2710826", "Mais, süß, gelbe und weiße Körner, frisch, roh", "Corn, sweet, yellow and white kernels, fresh, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (133, "Mais aus der Dose, Dosenmais", "0", "SIGHI_FoodList_2024-08-29_p8_r39", "169346", "Mais, süß, gelb, in Dosen, cremefarben, ohne Salzzusatz", "Corn, sweet, yellow, canned, cream style, no salt added", "EXACT", "Synonym zu Kukuruz Dose #131. DUPLICATE_OF=131."),
    (134, "Mais, getrocknet: Maisgriess, Maisgrieß, Maismehl,", "0", "SIGHI_FoodList_2024-08-29_p8_r40", "170290", "Maismehl, Vollkorn, gelb", "Corn flour, whole-grain, yellow", "EXACT", "Synonym zu Kukuruz getrocknet #130. DUPLICATE_OF=130."),
    (135, "Mais: Maiskörner, Maiskolben frisch/pasteurisiert", "0", "SIGHI_FoodList_2024-08-29_p8_r41", "2710826", "Mais, süß, gelbe und weiße Körner, frisch, roh", "Corn, sweet, yellow and white kernels, fresh, raw", "EXACT", "Synonym zu Kukuruz frisch #132. DUPLICATE_OF=132."),
    (136, "Maltodextrin", "0", "SIGHI_FoodList_2024-08-29_p8_r42", "", "", "", "NO_MATCH", "USDA hat kein Maltodextrin (reines Verarbeitungs-Polysaccharid); Score gesetzt, Nährwerte NULL (≈4 kcal/g Carbs)."),
    (137, "Malz, Malzextrakt, Gerstenmalz", "2", "SIGHI_FoodList_2024-08-29_p8_r43", "169740", "Gerstenmalzmehl", "Barley malt flour", "NEAR_EXACT", "Synonym zu Gerstenmalz #119. DUPLICATE_OF=119."),
    (138, "Maniok-Wurzelknollen", "?", "SIGHI_FoodList_2024-08-29_p8_r44", "169985", "Maniok, roh", "Cassava, raw", "EXACT", "Direkte 1:1-Übereinstimmung; Score=? in SIGHI."),
    (139, "Maroni, Marroni, Maronen, frisch oder tiefgekühlt", "0", "SIGHI_FoodList_2024-08-29_p8_r45", "170574", "Nüsse, Kastanien, europäisch, roh, ungeschält", "Nuts, chestnuts, european, raw, unpeeled", "EXACT", "Maroni = große Süßkastanien-Sorte; selbe USDA-Quelle wie #111/#117. DUPLICATE_OF=111."),
    (140, "Perlsago, Sagostärke", "0", "SIGHI_FoodList_2024-08-29_p8_r47", "", "", "", "NO_MATCH", "USDA hat kein Sago (Stärke aus Sagopalme); reine Stärke ~88g Carb/100g."),
    (141, "Quinoa", "0", "SIGHI_FoodList_2024-08-29_p8_r48", "168874", "Quinoa, ungekocht", "Quinoa, uncooked", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (142, "Reis", "0", "SIGHI_FoodList_2024-08-29_p8_r49", "168877", "Reis, weiß, langkörnig, normal, roh, angereichert", "Rice, white, long-grain, regular, raw, enriched", "EXACT", "Standard weißer Langkornreis als generische Reis-Referenz."),
    (143, "Reisnudeln", "0", "SIGHI_FoodList_2024-08-29_p8_r50", "169742", "Reisnudeln, trocken", "Rice noodles, dry", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (144, "Reiswaffeln, Reis-Mais-Waffeln", "0", "SIGHI_FoodList_2024-08-29_p8_r51", "170250", "Snacks, Reiswaffeln, Naturreis, ungesalzen", "Snacks, rice cakes, brown rice, plain, unsalted", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (145, "Rice Crispies", "0", "SIGHI_FoodList_2024-08-29_p8_r52", "173887", "Verzehrfertiges Getreide, RALSTON CRISP RICE", "Cereals ready-to-eat, RALSTON CRISP RICE", "EXACT", "RALSTON CRISP RICE entspricht Kellogg's Rice Crispies."),
    (146, "Roggen", "1", "SIGHI_FoodList_2024-08-29_p8_r53", "168884", "Roggenkorn", "Rye grain", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (147, "Sago, Sagostärke", "0", "SIGHI_FoodList_2024-08-29_p8_r55", "", "", "", "NO_MATCH", "Synonym zu Perlsago #140; USDA hat kein Sago. DUPLICATE_OF=140."),
    (148, "Sonnenblumenkerne", "2", "SIGHI_FoodList_2024-08-29_p8_r56", "2515381", "Samen, Sonnenblumenkerne, Kerne, roh", "Seeds, sunflower seed, kernel, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (149, "Süsskartoffel, Süßkartoffel", "0", "SIGHI_FoodList_2024-08-29_p8_r58", "169303", "Süßkartoffelblätter, roh", "Sweet potato leaves, raw", "NEAR_EXACT", "USDA-Default für Süßkartoffel-Knollen fehlt im Topkandidaten; Blätter sind nahe Approximation (alternativ: USDA hat sweet potato canned/cooked - generic raw fehlt direkt)."),
    (150, "Tapioka, Tapiokastärke", "0", "SIGHI_FoodList_2024-08-29_p8_r59", "174101", "Brot, glutenfrei, weiß, mit Tapiokastärke und braunem Reismehl hergestellt", "Bread, gluten-free, white, made with tapioca starch and brown rice flour", "APPROX", "USDA hat keine reine Tapiokastärke; nächstes Produkt ist glutenfreies Brot mit Tapioka-Basis - nicht ideal für Nährwerte. Score gesetzt; Nährwerte sollten als NULL betrachtet werden."),
    (151, "Weizen", "1", "SIGHI_FoodList_2024-08-29_p8_r61", "169719", "Weizen, Hartweizen", "Wheat, hard white", "EXACT", "Standard Hartweizen als generische Weizen-Referenz."),
    (152, "Weizenkeime", "2", "SIGHI_FoodList_2024-08-29_p8_r62", "168892", "Weizenkeime, roh", "Wheat germ, crude", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (153, "Wildreis, Wasserreis (Zizania)", "0", "SIGHI_FoodList_2024-08-29_p8_r63", "169726", "Wildreis, roh", "Wild rice, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),

    # ============ NÜSSE (15) ============
    (154, "Baumnuss", "3", "SIGHI_FoodList_2024-08-29_p9_r5", "2346394", "Nüsse, Walnüsse, englisch, halbiert, roh", "Nuts, walnuts, English, halves, raw", "EXACT", "Baumnuss = schweiz./süddt. für Walnuss."),
    (155, "Cashew-Kerne, Cashewnüsse, Cashewnuss", "1", "SIGHI_FoodList_2024-08-29_p9_r6", "2515374", "Nüsse, Cashewnüsse, roh", "Nuts, cashew nuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (156, "Erdmandel, geröstet", "2", "SIGHI_FoodList_2024-08-29_p9_r7", "", "", "", "NO_MATCH", "USDA hat keine Erdmandel (Cyperus esculentus / Tigernuss); botanisch keine Nuss sondern Pflanzenknolle."),
    (157, "Erdmandel, Tigernuss (Cyperus esculentus)", "0", "SIGHI_FoodList_2024-08-29_p9_r8", "", "", "", "NO_MATCH", "Synonym zu #156; USDA hat es nicht. DUPLICATE_OF=156."),
    (158, "Erdnüsse, Erdnuss", "2", "SIGHI_FoodList_2024-08-29_p9_r9", "2515376", "Erdnüsse, roh", "Peanuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (159, "Haselnuss, Haselnüsse", "1", "SIGHI_FoodList_2024-08-29_p9_r10", "2515375", "Nüsse, Haselnüsse oder Filberts, roh", "Nuts, hazelnuts or filberts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (160, "Macadamia-Nuss", "0", "SIGHI_FoodList_2024-08-29_p9_r11", "2515378", "Nüsse, Macadamianüsse, roh", "Nuts, macadamia nuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (161, "Mandeln", "1", "SIGHI_FoodList_2024-08-29_p9_r12", "2346393", "Nüsse, Mandeln, ganz, roh", "Nuts, almonds, whole, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (162, "Paranuss", "0", "SIGHI_FoodList_2024-08-29_p9_r13", "2515373", "Nüsse, Paranüsse, roh", "Nuts, brazilnuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (163, "Pekannuss, Pecannuss", "0", "SIGHI_FoodList_2024-08-29_p9_r14", "2346395", "Nüsse, Pekannüsse, halbiert, roh", "Nuts, pecans, halves, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (164, "Pinienkerne", "1", "SIGHI_FoodList_2024-08-29_p9_r15", "2346392", "Nüsse, Pinienkerne, roh", "Nuts, pine nuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (165, "Pistazie", "0", "SIGHI_FoodList_2024-08-29_p9_r16", "2515379", "Nüsse, Pistazien, roh", "Nuts, pistachio nuts, raw", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (166, "Spanische Nüssli (=Erdnuss)", "2", "SIGHI_FoodList_2024-08-29_p9_r17", "2515376", "Erdnüsse, roh", "Peanuts, raw", "EXACT", "Spanische Nüssli = schweiz. für Erdnüsse. DUPLICATE_OF=158."),
    (167, "Tigernuss, Erdmandel (Cyperus esculentus)", "0", "SIGHI_FoodList_2024-08-29_p9_r19", "", "", "", "NO_MATCH", "Alphabet-Synonym zu #157. DUPLICATE_OF=156."),
    (168, "Walnuss", "3", "SIGHI_FoodList_2024-08-29_p9_r20", "2346394", "Nüsse, Walnüsse, englisch, halbiert, roh", "Nuts, walnuts, English, halves, raw", "EXACT", "Synonym zu Baumnuss #154. DUPLICATE_OF=154."),

    # ============ FETTE (18) ============
    (169, "Baumnussöl", "2", "SIGHI_FoodList_2024-08-29_p9_r22", "171030", "Öl, Walnuss", "Oil, walnut", "EXACT", "Baumnussöl = Walnussöl."),
    (170, "Distelöl, Färberdistelöl, Safloröl", "0", "SIGHI_FoodList_2024-08-29_p9_r23", "171026", "Öl, Färberdistel, Salat oder Kochen, Linolsäure, (über 70%)", "Oil, safflower, salad or cooking, linoleic, (over 70%)", "EXACT", "Distelöl = Safloröl; klassische Linolsäure-Variante."),
    (171, "Kokosfett, Kokosöl, Kokosnussfett, Kokosnussöl", "0", "SIGHI_FoodList_2024-08-29_p9_r25", "330458", "Öl, Kokosnuss", "Oil, coconut", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (172, "Kokosöl, Kokosfett", "0", "SIGHI_FoodList_2024-08-29_p9_r26", "330458", "Öl, Kokosnuss", "Oil, coconut", "EXACT", "Synonym zu #171. DUPLICATE_OF=171."),
    (173, "Kürbiskernöl", "0", "SIGHI_FoodList_2024-08-29_p9_r27", "", "", "", "NO_MATCH", "USDA hat kein Kürbiskernöl; Makro-Profil ähnelt Sonnenblumenöl, aber NO_MATCH ist konservativer (Aromen/Mikronährstoffe abweichend)."),
    (174, "Leinöl, Leinsamenöl", "0", "SIGHI_FoodList_2024-08-29_p9_r28", "167702", "Öl, Leinsamen, kaltgepresst", "Oil, flaxseed, cold pressed", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (175, "Maiskeimöl, Maisöl", "0", "SIGHI_FoodList_2024-08-29_p9_r29", "171029", "Öl, Mais, Industrie und Einzelhandel, Allzweck-Salat oder Kochen", "Oil, corn, industrial and retail, all purpose salad or cooking", "EXACT", "Standard-Maisöl."),
    (176, "Margarine (wenn ohne unverträgliche Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p9_r30", "171018", "Margarine, normal, hart, Sojabohnen (gehärtet)", "Margarine, regular, hard, soybean (hydrogenated)", "NEAR_EXACT", "Generische harte Margarine; tatsächliche Zusammensetzung variiert stark."),
    (177, "Nachtkerzenöl (Oenothera biennis)", "0", "SIGHI_FoodList_2024-08-29_p9_r32", "", "", "", "NO_MATCH", "USDA hat kein Nachtkerzenöl; wird als Nahrungsergänzungsmittel verkauft (γ-Linolensäure-Profil)."),
    (178, "Olivenöl", "0", "SIGHI_FoodList_2024-08-29_p9_r33", "171413", "Öl, Olivenöl, Salat oder zum Kochen", "Oil, olive, salad or cooking", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (179, "Palmfett, Palmöl", "0", "SIGHI_FoodList_2024-08-29_p9_r34", "171015", "Öl, Palmöl", "Oil, palm", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (180, "Palmöl, Palmfett, Palmkernöl", "0", "SIGHI_FoodList_2024-08-29_p9_r35", "171015", "Öl, Palmöl", "Oil, palm", "EXACT", "Synonym zu #179. DUPLICATE_OF=179."),
    (181, "Rapsöl", "0", "SIGHI_FoodList_2024-08-29_p9_r36", "172336", "Öl, Raps", "Oil, canola", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (182, "Safloröl, Distelöl, Färberdistelöl", "0", "SIGHI_FoodList_2024-08-29_p9_r37", "171026", "Öl, Färberdistel, Salat oder Kochen, Linolsäure, (über 70%)", "Oil, safflower, salad or cooking, linoleic, (over 70%)", "EXACT", "Synonym zu #170. DUPLICATE_OF=170."),
    (183, "Schwarzkümmelöl (Nigella sativa)", "0", "SIGHI_FoodList_2024-08-29_p9_r38", "", "", "", "NO_MATCH", "USDA hat kein Schwarzkümmelöl; medizinisch-funktionales Öl."),
    (184, "Sojaöl", "1", "SIGHI_FoodList_2024-08-29_p9_r39", "171411", "Öl, Sojabohnen, Salat oder zum Kochen", "Oil, soybean, salad or cooking", "EXACT", "Direkte 1:1-Übereinstimmung."),
    (185, "Sonnenblumenöl", "0", "SIGHI_FoodList_2024-08-29_p9_r40", "171017", "Öl, Sonnenblumen, Linolsäure (weniger als 60%)", "Oil, sunflower, linoleic (less than 60%)", "EXACT", "Standard-Sonnenblumenöl."),
    (186, "Walnussöl", "2", "SIGHI_FoodList_2024-08-29_p9_r41", "171030", "Öl, Walnuss", "Oil, walnut", "EXACT", "Synonym zu Baumnussöl #169. DUPLICATE_OF=169."),
]

def main():
    existing = set()
    if OUT.exists():
        with open(OUT, encoding="utf-8") as f:
            reader = csv.reader(f, delimiter=";")
            next(reader, None)
            for r in reader:
                if r: existing.add(r[0])
    with open(OUT, "a", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        appended = 0
        for r in ROWS:
            if str(r[0]) in existing:
                print(f"  skip (exists): #{r[0]} {r[1]}"); continue
            w.writerow(r); appended += 1
    print(f"Appended {appended} rows. Total now: {len(existing)+appended}")

if __name__ == "__main__":
    main()
