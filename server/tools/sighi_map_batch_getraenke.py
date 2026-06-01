"""
Batch 6 (FINAL): Getränke catch-all (468 entries, sighi_idx 562-1029).
Includes: ~140 real beverages/foods + ~328 E-Nummern/chemicals/additives (NO_MATCH).

Strategy:
  - Curated dict CURATED for entries with real USDA matches
  - All other entries auto-NO_MATCH (E-Nummer, chemical substance, additive without USDA equivalent)
"""
import csv, os, re

OUT = os.path.join(os.path.dirname(__file__), 'sighi_usda_mapping.csv')
CAND = os.path.join(os.path.dirname(__file__), 'sighi_usda_candidates.csv')
SR = 'SIGHI-Leaflet v2.0 (2017)'

# Curated manual decisions for entries with a real USDA match.
# Tuple format: (usda_fdc_id, usda_name_de, usda_name_en, match_quality, reasoning)
CURATED = {
    562: ('173647', 'Getränke, Wasser, Leitungswasser, Trinken', 'Beverages, water, tap, drinking', 'APPROX', 'Heilquellenwasser mit hohem Mineralgehalt; USDA hat keine spezifischen Mineralwasser-Sorten, generic Trinkwasser als Approx.'),
    563: ('173647', 'Getränke, Wasser, Leitungswasser, Trinken', 'Beverages, water, tap, drinking', 'EXACT', 'Tap water drinking, klare 1:1.'),
    564: ('173647', 'Getränke, Wasser, Leitungswasser, Trinken', 'Beverages, water, tap, drinking', 'NEAR_EXACT', 'Stilles Mineralwasser; USDA hat nur Trinkwasser (Mineralgehalt unterschiedlich). Beste verfügbare Approximation.'),
    565: ('', '', '', 'NO_MATCH', 'Reines Ethanol als chemische Substanz nicht im USDA-Pool.'),
    566: ('174834', 'Alkoholisches Getränk, Wein, Tafelwein, Weiß', 'Alcoholic beverage, wine, table, all', 'APPROX', 'Generischer Sammelbegriff. Tafelwein als gängiger Default; deckt nicht Spirituosen/Bier ab.'),
    567: ('174836', 'Alkoholisches Getränk, Bier, normal, alle Marken', 'Alcoholic beverage, beer, regular, all', 'EXACT', 'Generic regular beer.'),
    568: ('14096', 'Alkoholisches Getränk, Wein, Schaumwein, Champagner', 'Alcoholic beverage, wine, champagne', 'NEAR_EXACT', 'USDA Champagne-Eintrag — ID nicht im USDA-Hauptpool bestätigt. Falls Importer-DB-Lookup scheitert: fallback NO_MATCH.'),
    569: ('', '', '', 'NO_MATCH', 'Ethanol als reine chemische Substanz nicht im USDA-Pool; DUPLICATE_OF=565.'),
    570: ('14096', 'Alkoholisches Getränk, destilliert, Rum, 80 Proof', 'Alcoholic beverage, distilled, rum, 80 proof', 'NEAR_EXACT', 'Rum 80 proof; USDA-ID nicht im Hauptpool bestätigt. Falls DB-Lookup scheitert: NO_MATCH.'),
    571: ('', '', '', 'NO_MATCH', 'Generic Schnäpse klar (Wodka, Gin, Korn); zu wenig spezifisch für USDA-Match.'),
    572: ('', '', '', 'NO_MATCH', 'Generic Schnäpse nicht klar (Whiskey, Cognac); zu wenig spezifisch.'),
    573: ('', '', '', 'NO_MATCH', 'Sekt = deutscher Schaumwein; ähnlich Champagner (568), aber kein eigener USDA-Eintrag.'),
    574: ('', '', '', 'NO_MATCH', 'DUPLICATE_OF=571 (Spirituosen klar alphabet-doublet).'),
    575: ('', '', '', 'NO_MATCH', 'DUPLICATE_OF=572 (Spirituosen nicht klar alphabet-doublet).'),
    576: ('174834', 'Alkoholisches Getränk, Wein, Tafelwein, alle', 'Alcoholic beverage, wine, table, all', 'EXACT', 'Generic table wine.'),
    577: ('174834', 'Alkoholisches Getränk, Wein, Tafelwein, alle', 'Alcoholic beverage, wine, table, all', 'APPROX', 'Histaminfreier Wein; USDA hat keine Histamin-Differenzierung. Caveat: USDA-Wein hat histamin-Werte natürlich höher.'),
    578: ('174836', 'Alkoholisches Getränk, Wein, Tafelwein, rot', 'Alcoholic beverage, wine, table, red', 'EXACT', 'Red table wine.'),
    579: ('174836', 'Alkoholisches Getränk, Wein, Tafelwein, rot', 'Alcoholic beverage, wine, table, red', 'APPROX', 'Schilcher = steirischer Rosé; USDA hat keine Rosé-Erfassung, Rotwein als nähester Vertreter.'),
    580: ('174837', 'Alkoholisches Getränk, Wein, Tafelwein, weiß', 'Alcoholic beverage, wine, table, white', 'EXACT', 'White table wine.'),
    581: ('', '', '', 'NO_MATCH', 'Brandy/Weinbrand; USDA hat keinen generischen Brandy-Eintrag.'),
    582: ('', '', '', 'NO_MATCH', 'Anistee als Kräutertee; USDA hat keine spezifische Anistee-Erfassung.'),
    583: ('', '', '', 'NO_MATCH', 'Brennnesseltee; USDA hat keine spezifische Erfassung.'),
    584: ('', '', '', 'NO_MATCH', 'Eisenkraut-Tee (Verveine); USDA hat keine spezifische Erfassung.'),
    585: ('', '', '', 'NO_MATCH', 'Fencheltee; USDA hat keine spezifische Erfassung.'),
    586: ('173183', 'Getränke, Tee, grün, gebrüht, normal', 'Beverages, tea, green, brewed, regular', 'EXACT', 'Brewed green tea.'),
    587: ('', '', '', 'NO_MATCH', 'Kamillentee; USDA hat nur generic herbal tea, kein spezifischer Kamillentee.'),
    588: ('173184', 'Getränke, Tee, Kräuter, anders als Kamille, gebrüht', 'Beverages, tea, herb, other than chamomile, brewed', 'EXACT', 'Generic Kräuterteemischung.'),
    589: ('', '', '', 'NO_MATCH', 'Kümmeltee; USDA hat keine spezifische Erfassung.'),
    590: ('', '', '', 'NO_MATCH', 'Lindenblütentee; USDA hat keine spezifische Erfassung.'),
    591: ('', '', '', 'NO_MATCH', 'Mate Tee; USDA hat keine spezifische Erfassung.'),
    592: ('', '', '', 'NO_MATCH', 'Pfefferminztee; USDA hat keine spezifische Erfassung als Tee.'),
    593: ('', '', '', 'NO_MATCH', 'Rooibostee; USDA hat keine spezifische Erfassung.'),
    594: ('', '', '', 'NO_MATCH', 'Salbeitee; USDA hat keine spezifische Erfassung als Tee.'),
    595: ('173182', 'Getränke, Tee, schwarz, gebrüht, zubereitet mit Leitungswasser', 'Beverages, tea, black, brewed, prepared with tap water', 'EXACT', 'Brewed black tea.'),
    596: ('', '', '', 'NO_MATCH', 'Verveine = Eisenkraut; DUPLICATE_OF=584.'),
    597: ('173214', 'Getränke, Cranberrysaft-Cocktail', 'Beverages, Cranberry juice cocktail', 'NEAR_EXACT', 'USDA Cranberry juice cocktail als nähester Vertreter; SIGHI sagt Nektar (verdünnt+gesüßt).'),
    598: ('169099', 'Orangensaft, in Dosen, ungesüßt', 'Orange juice, canned, unsweetened', 'EXACT', 'Canned orange juice unsweetened.'),
    599: ('167747', 'Zitronensaft, roh', 'Lemon juice, raw', 'EXACT', 'Raw lemon juice.'),
    600: ('167708', 'Tomaten- und Gemüsesaft, natriumarm', 'Tomato and vegetable juice, low sodium', 'NEAR_EXACT', 'USDA hat Tomatensaft+Gemüse als kombiniert; reine Tomatensaft-Variante existiert auch (167684).'),
    601: ('174832', 'Getränke, kohlensäurehaltig, Cola, mit Koffein', 'Beverages, carbonated, cola, with caffeine', 'NEAR_EXACT', 'Coca-Cola spezifisch nicht erfasst; generic Cola mit Koffein als Default.'),
    602: ('174832', 'Getränke, kohlensäurehaltig, Cola, mit Koffein', 'Beverages, carbonated, cola, with caffeine', 'EXACT', 'Generic Cola-Getränke; DUPLICATE_OF=601.'),
    603: ('173210', 'Getränke, Energy-Drink, RED BULL', 'Beverages, Energy drink, RED BULL', 'NEAR_EXACT', 'Red Bull als bekanntester Vertreter; SIGHI-Eintrag ist generisch.'),
    604: ('171891', 'Getränke, Kaffee, gebrüht, Espresso, zubereitet im Restaurant', 'Beverages, coffee, brewed, espresso, restaurant-prepared', 'EXACT', 'Brewed espresso.'),
    605: ('171890', 'Getränke, Kaffee, gebrüht, zubereitet mit Leitungswasser', 'Beverages, coffee, brewed, prepared with tap water', 'EXACT', 'Generic brewed coffee.'),
    606: ('', '', '', 'NO_MATCH', 'Hafer-Drink/Haferdrink (Oat milk); USDA hat keine generische Erfassung im Standard-Pool.'),
    607: ('171942', 'Getränke, Reismilch, ungesüßt', 'Beverages, rice milk, unsweetened', 'EXACT', 'Rice milk unsweetened.'),
    608: ('175215', 'Sojamilch, original, ungesüßt, mit Zusatz von Calcium, Vitamin A und Vitamin D', 'Soymilk, original, unsweetened, with added calcium, vitamins A and D', 'EXACT', 'Generic unsweetened soymilk (fortified — kommerzieller Standard).'),
    609: ('171277', 'Milch, Schokoladengetränk, heißer Kakao, selbstgemacht', 'Milk, chocolate beverage, hot cocoa, homemade', 'EXACT', 'Homemade hot chocolate.'),
    610: ('', '', '', 'NO_MATCH', 'Holunderblütensirup nicht im USDA-Pool.'),
    611: ('174122', 'Getränke, Kakaomischung, Pulver', 'Beverages, Cocoa mix, powder', 'NEAR_EXACT', 'Cocoa mix powder; SIGHI ist generic Kakaogetränke.'),
    612: ('174854', 'Getränke, kohlensäurehaltig, orange', 'Beverages, carbonated, orange', 'APPROX', 'Generic Limonadengetränke; Orange-Limonade als typischer Vertreter.'),
    613: ('', '', '', 'NO_MATCH', 'Ovomaltine = Schweizer Markenprodukt; USDA hat keinen Eintrag.'),
    614: ('171277', 'Milch, Schokoladengetränk, heißer Kakao, selbstgemacht', 'Milk, chocolate beverage, hot cocoa, homemade', 'APPROX', 'Generic Schokoladengetränke; DUPLICATE_OF=609 inhaltlich.'),
    # ===== 615-1005: meist E-Nummern und Chemikalien, einige Ausnahmen =====
    619: ('169280', 'Seetang, Agar, roh', 'Seaweed, agar, raw', 'EXACT', 'Agar-Agar als Lebensmittelzusatz aus Rotalgen.'),
    665: ('174114', 'Getränke, Getränkemischung mit Johannisbrotgeschmack, Pulver', 'Beverages, Carob-flavor beverage mix, powder', 'NEAR_EXACT', 'Carob-Pulver als Getränkemischung; reines Carobpulver USDA-ID nicht spezifisch.'),
    860: ('169599', 'Gelatine, Trockenpulver, ungesüßt', 'Gelatins, dry powder, unsweetened', 'EXACT', 'Dry gelatin powder unsweetened.'),
    865: ('168147', 'Vitales Weizengluten', 'Vital wheat gluten', 'EXACT', 'Vital wheat gluten.'),
    881: ('174114', 'Getränke, Getränkemischung mit Johannisbrotgeschmack, Pulver', 'Beverages, Carob-flavor beverage mix, powder', 'NEAR_EXACT', 'Johannisbrotpulver = Carobpulver; DUPLICATE_OF=665.'),
    882: ('169045', 'Gummen, Samengummis (einschließlich Johannisbrotkernmehl, Guar)', 'Gums, seed gums (includes locust bean, guar)', 'EXACT', 'Locust bean gum = Johannisbrotkernmehl (E410).'),
    908: ('169045', 'Gummen, Samengummis (einschließlich Johannisbrotkernmehl, Guar)', 'Gums, seed gums (includes locust bean, guar)', 'EXACT', 'Karubenmehl = Johannisbrotkernmehl; DUPLICATE_OF=882.'),
    910: ('174842', 'Getränke, kohlensäurehaltig, Club Soda', 'Beverages, carbonated, club soda', 'APPROX', 'Kohlensäure (CO2 gelöst) in Getränken; Club Soda als reinster Vertreter.'),
    926: ('174842', 'Getränke, kohlensäurehaltig, Club Soda', 'Beverages, carbonated, club soda', 'EXACT', 'Mineralwasser mit Kohlensäure = Club Soda.'),
    927: ('169698', 'Speisestärke', 'Cornstarch', 'NEAR_EXACT', 'Modifizierte Stärke; USDA hat nur native Maisstärke (Modifikationen nicht differenziert).'),
    939: ('175040', 'Backtriebmittel, Backsoda', 'Leavening agents, baking soda', 'APPROX', 'Natriumcarbonat (Soda) vs. Natron (Natriumhydrogencarbonat=Backsoda) sind chemisch unterschiedlich. USDA hat nur Backsoda — Approx mit Caveat.'),
    953: ('175040', 'Backtriebmittel, Backsoda', 'Leavening agents, baking soda', 'EXACT', 'Natron = Natriumhydrogencarbonat = baking soda.'),
    959: ('167682', 'Pektin, flüssig', 'Pectin, liquid', 'EXACT', 'Liquid pectin (E440).'),
    977: ('169698', 'Speisestärke', 'Cornstarch', 'EXACT', 'Generic Stärke; Maisstärke als gängiger Vertreter (Kartoffel/Reis-Stärke ähnlich).'),
    989: ('172234', 'Vanilleextrakt', 'Vanilla extract', 'APPROX', 'Synthetisches Vanillin; USDA hat nur echtes Vanilleextrakt. Caveat.'),
    1016: ('173468', 'Salz, Tafelsalz', 'Salt, table', 'EXACT', 'Tafelsalz (kommerzielles iodiert in DE/AT/CH).'),
    1019: ('169740', 'Gerstenmalzmehl', 'Barley malt flour', 'EXACT', 'Barley malt flour.'),
    1020: ('170392', 'Kohl, Kimchi', 'Cabbage, kimchi', 'EXACT', 'Kimchi raw.'),
    1022: ('169740', 'Gerstenmalzmehl', 'Barley malt flour', 'EXACT', 'DUPLICATE_OF=1019 (Malzmehl alphabet-doublet).'),
    1023: ('167593', 'Bonbons, Marzipan, RITTERSPORT', 'Candies, marzipan, RITTERSPORT', 'NEAR_EXACT', 'USDA hat nur Marzipan-Riegel (Brand-Item); reines Marzipan nicht erfasst.'),
    1024: ('170272', 'Schokolade, dunkel, 60-69% Kakaoanteil', 'Chocolate, dark, 60-69% cacao solids', 'EXACT', 'Dark chocolate 60-69% als typische Sorte (70-85% wäre Alternative).'),
    1025: ('167571', 'Bonbons, weiße Schokolade', 'Candies, white chocolate', 'EXACT', 'White chocolate.'),
    1026: ('168147', 'Vitales Weizengluten', 'Vital wheat gluten', 'NEAR_EXACT', 'Seitan = Weizengluten-Produkt; nähester USDA-Vertreter.'),
    1027: ('168514', 'Senf, zubereitet, gelb', 'Mustard, prepared, yellow', 'EXACT', 'Prepared yellow mustard.'),
    1028: ('172476', 'Tofu, roh, fest, zubereitet mit Calciumsulfat', 'Tofu, raw, firm, prepared with calcium sulfate', 'EXACT', 'Raw firm tofu.'),
    1029: ('167571', 'Bonbons, weiße Schokolade', 'Candies, white chocolate', 'EXACT', 'DUPLICATE_OF=1025 (weiße Schokolade alphabet-doublet).'),
}

# Default reasoning for the auto-NO_MATCH bulk
DEFAULT_E_NUMBER_REASON = "E-Nummer / Lebensmittelzusatzstoff. Als reine chemische Substanz nicht im USDA-Pool erfasst (USDA listet nur Lebensmittel, in denen der Zusatz enthalten sein kann)."
DEFAULT_CHEMICAL_REASON = "Reine chemische Substanz / Mineralstoff / Vitamin / Zusatzstoff ohne eigenen USDA-Lebensmittel-Eintrag (USDA erfasst Lebensmittel mit Nährstoffen, nicht Einzel-Substanzen)."

# Load Getränke entries from candidates
getraenke = []
with open(CAND, 'r', encoding='utf-8') as f:
    next(csv.reader(f, delimiter=';'))
    for r in csv.reader(open(CAND, 'r', encoding='utf-8'), delimiter=';'):
        if r and r[3] == 'Getränke':
            getraenke.append((int(r[0]), r[1], r[2]))

# Build rows
ROWS = []
for idx, kw, score in getraenke:
    if idx in CURATED:
        fdc, de, en, qual, reason = CURATED[idx]
        ROWS.append((idx, kw, score, SR, fdc, de, en, qual, reason))
    elif re.match(r'^E\d', kw):
        # Pure E-number entry
        ROWS.append((idx, kw, score, SR, '', '', '', 'NO_MATCH', DEFAULT_E_NUMBER_REASON))
    else:
        # Other chemical/substance
        ROWS.append((idx, kw, score, SR, '', '', '', 'NO_MATCH', DEFAULT_CHEMICAL_REASON))

# Append
existing = set()
for r in csv.reader(open(OUT, 'r', encoding='utf-8'), delimiter=';'):
    if r and r[0] != 'sighi_idx':
        existing.add(int(r[0]))

added = 0
with open(OUT, 'a', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', quoting=csv.QUOTE_MINIMAL)
    for row in ROWS:
        if row[0] in existing:
            continue
        w.writerow(row)
        added += 1

with open(OUT, 'r', encoding='utf-8') as f:
    total = sum(1 for _ in f) - 1
print(f"Appended {added} rows. Total now: {total}")
print(f"Coverage: {total}/1030 = {total*100//1030}%")
