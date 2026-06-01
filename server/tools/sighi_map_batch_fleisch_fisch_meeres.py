"""Append Fleisch+Fisch+Meeresfrüchte batch (sighi_idx 53..102) to mapping CSV."""
import csv
from pathlib import Path

OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv")

ROWS = [
    # ====== FLEISCH ======
    (53, "Dörrfleisch, Trockenfleisch", "3", "SIGHI_FoodList_2024-08-29_p6_r13", "167536", "Snacks, Beef Jerky, gehackt und geformt", "Snacks, beef jerky, chopped and formed",
     "NEAR_EXACT", "Dörrfleisch=luftgetrocknetes Rindfleisch; USDA Beef Jerky ist die direkte Entsprechung (gleicher Trocknungs-/Salz-/Reifeprozess, Makros vergleichbar)."),
    (54, "Ente, Entenfleisch", "0", "SIGHI_FoodList_2024-08-29_p6_r14", "174468", "Ente, wild, Fleisch und Haut, roh", "Duck, wild, meat and skin, raw",
     "NEAR_EXACT", "USDA hat keine 'Ente, domestiziert, roh' generisch (nur gekocht/gebraten). Wild-Ente roh ist die einzige rohe Variante; Makros etwas magerer als Pekin aber nahe."),
    (55, "Faschiertes (=Hackfleisch), bei Verzehr unmittelbar", "0", "SIGHI_FoodList_2024-08-29_p6_r15", "2514744", "Rindfleisch, gemahlen, 80% mageres Fleisch / 20% Fett, roh", "Beef, ground, 80% lean meat / 20% fat, raw",
     "EXACT", "Faschiertes/Hackfleisch standard; USDA 80/20-Mischung ist Mittelfettstandard."),
    (56, "Faschiertes (=Hackfleisch), Offenverkauf oder unter", "2", "SIGHI_FoodList_2024-08-29_p6_r16", "2514744", "Rindfleisch, gemahlen, 80% mageres Fleisch / 20% Fett, roh", "Beef, ground, 80% lean meat / 20% fat, raw",
     "EXACT", "Selbes Lebensmittel wie #55, nur Histamin-Risiko unterscheidet (Lagerung); Nährwerte identisch."),
    (57, "Geflügelfleisch", "0", "SIGHI_FoodList_2024-08-29_p6_r19", "2646170", "Huhn, Brust, ohne Knochen, ohne Haut, roh", "Chicken, breast, boneless, skinless, raw",
     "NEAR_EXACT", "Geflügelfleisch ist Sammelbegriff; Hähnchenbrust ist die nährwertliche Standardreferenz für mageres Geflügel."),
    (58, "geräucherter Fisch, Räucherfisch", "3", "SIGHI_FoodList_2024-08-29_p6_r21", "173687", "Fisch, Lachs, Chinook, geräuchert", "Fish, salmon, chinook, smoked",
     "NEAR_EXACT", "Generischer Räucherfisch; Räucherlachs ist die häufigste Vertretung und in USDA verfügbar."),
    (59, "Geselchtes, Rauchfleisch", "3", "SIGHI_FoodList_2024-08-29_p6_r22", "170607", "Rindfleisch, zerkleinert, gepökelt, geräuchert", "Beef, chopped, cured, smoked",
     "NEAR_EXACT", "Geselchtes = österr./bayr. für gepökeltes geräuchertes Fleisch; USDA-Eintrag passt direkt."),
    (60, "Hackfleisch, bei Verzehr unmittelbar nach", "0", "SIGHI_FoodList_2024-08-29_p6_r24", "2514744", "Rindfleisch, gemahlen, 80% mageres Fleisch / 20% Fett, roh", "Beef, ground, 80% lean meat / 20% fat, raw",
     "EXACT", "Synonym zu Faschiertes #55; identische USDA-Quelle. DUPLICATE_OF=55."),
    (61, "Hackfleisch, Offenverkauf oder unter", "2", "SIGHI_FoodList_2024-08-29_p6_r25", "2514744", "Rindfleisch, gemahlen, 80% mageres Fleisch / 20% Fett, roh", "Beef, ground, 80% lean meat / 20% fat, raw",
     "EXACT", "Synonym zu Faschiertes #56; identische USDA-Quelle. DUPLICATE_OF=56."),
    (62, "Hähnchen", "0", "SIGHI_FoodList_2024-08-29_p6_r27", "2646170", "Huhn, Brust, ohne Knochen, ohne Haut, roh", "Chicken, breast, boneless, skinless, raw",
     "EXACT", "Standardreferenz für Hähnchen-/Hühnerfleisch in USDA."),
    (63, "Hendl, Hühnchen", "0", "SIGHI_FoodList_2024-08-29_p6_r28", "2646170", "Huhn, Brust, ohne Knochen, ohne Haut, roh", "Chicken, breast, boneless, skinless, raw",
     "EXACT", "Synonym zu Hähnchen; selbe USDA-Quelle. DUPLICATE_OF=62."),
    (64, "Huhn", "0", "SIGHI_FoodList_2024-08-29_p6_r29", "2646170", "Huhn, Brust, ohne Knochen, ohne Haut, roh", "Chicken, breast, boneless, skinless, raw",
     "EXACT", "Synonym zu Hähnchen; selbe USDA-Quelle. DUPLICATE_OF=62."),
    (65, "Innereien", "2", "SIGHI_FoodList_2024-08-29_p6_r30", "172396", "Huhn, Braten, Innereien, roh", "Chicken, roasting, giblets, raw",
     "NEAR_EXACT", "Innereien als Sammelbegriff; Hühner-Innereien (giblets) sind USDA-Standardreferenz."),
    (66, "Kalbfleisch, frisch", "0", "SIGHI_FoodList_2024-08-29_p6_r32", "175290", "Kalbfleisch, gemahlen, roh", "Veal, ground, raw",
     "EXACT", "Frisches Kalbfleisch generisch; gemahlen-roh ist USDA-Standardreferenz für Kalb-Nährwerte."),
    (67, "Poulet", "0", "SIGHI_FoodList_2024-08-29_p6_r43", "2646170", "Huhn, Brust, ohne Knochen, ohne Haut, roh", "Chicken, breast, boneless, skinless, raw",
     "EXACT", "Poulet = franz./schweiz. für Hähnchen; selbe USDA-Quelle. DUPLICATE_OF=62."),
    (68, "Pute", "0", "SIGHI_FoodList_2024-08-29_p6_r44", "171505", "Pute, gemahlen, roh", "Turkey, Ground, raw",
     "EXACT", "Standardreferenz für Truthahn/Pute roh."),
    (69, "Räucherfisch, geräucherter Fisch", "3", "SIGHI_FoodList_2024-08-29_p6_r46", "173687", "Fisch, Lachs, Chinook, geräuchert", "Fish, salmon, chinook, smoked",
     "NEAR_EXACT", "Alphabet-Synonym-Dublette zu #58. DUPLICATE_OF=58."),
    (70, "Rauchfleisch, Räucherfleisch, geräuchertes Fleisch", "3", "SIGHI_FoodList_2024-08-29_p6_r48", "170607", "Rindfleisch, zerkleinert, gepökelt, geräuchert", "Beef, chopped, cured, smoked",
     "EXACT", "Generisches geräuchertes Fleisch; USDA-Eintrag passt direkt. DUPLICATE_OF=59."),
    (71, "Rindfleisch, frisch", "0", "SIGHI_FoodList_2024-08-29_p6_r49", "2514744", "Rindfleisch, gemahlen, 80% mageres Fleisch / 20% Fett, roh", "Beef, ground, 80% lean meat / 20% fat, raw",
     "EXACT", "Standardreferenz für frisches Rindfleisch; gemahlen 80/20 ist USDA-Default."),
    (72, "Rohschinken", "3", "SIGHI_FoodList_2024-08-29_p6_r51", "168282", "Schweinefleisch, gepökelt, Schinken, mittlere Scheibe, nach Art des Landes, nur magere Teile, roh", "Pork, cured, ham, center slice, country-style, separable lean only, raw",
     "NEAR_EXACT", "USDA hat keinen Prosciutto-Eintrag; country-style cured ham raw ist die nächstliegende Variante (luftgetrocknet/gepökelt)."),
    (73, "Salami", "3", "SIGHI_FoodList_2024-08-29_p6_r52", "174603", "Salami, italienisch, Schweinefleisch", "Salami, Italian, pork",
     "EXACT", "Italienische Schweine-Salami als Standardvertretung."),
    (74, "Schweinefleisch, frisch, unbehandelt", "1", "SIGHI_FoodList_2024-08-29_p6_r53", "167902", "Schweinefleisch, frisch, gemahlen, roh", "Pork, fresh, ground, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (75, "Selchfleisch, Rauchfleisch", "3", "SIGHI_FoodList_2024-08-29_p6_r55", "170607", "Rindfleisch, zerkleinert, gepökelt, geräuchert", "Beef, chopped, cured, smoked",
     "EXACT", "Selchfleisch = österr. für Geselchtes/Rauchfleisch; selbe USDA-Quelle wie #59/#70. DUPLICATE_OF=59."),
    (76, "Strauss, Strauß, Straußenfleisch", "0", "SIGHI_FoodList_2024-08-29_p6_r56", "172844", "Strauß, rund, roh", "Ostrich, round, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (77, "Trockenfleisch", "3", "SIGHI_FoodList_2024-08-29_p6_r58", "167536", "Snacks, Beef Jerky, gehackt und geformt", "Snacks, beef jerky, chopped and formed",
     "NEAR_EXACT", "Synonym zu Dörrfleisch #53; selbe USDA-Quelle. DUPLICATE_OF=53."),
    (78, "Trute, Pute", "0", "SIGHI_FoodList_2024-08-29_p6_r59", "171505", "Pute, gemahlen, roh", "Turkey, Ground, raw",
     "EXACT", "Synonym zu Pute; selbe USDA-Quelle. DUPLICATE_OF=68."),
    (79, "Wachtel", "0", "SIGHI_FoodList_2024-08-29_p6_r60", "172419", "Wachteln, nur Fleisch, roh", "Quail, meat only, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (80, "Wildbret", "1", "SIGHI_FoodList_2024-08-29_p6_r62", "167622", "Hirsch (Wildbret), Sitka, roh (Alaska Native)", "Deer (venison), sitka, raw (Alaska Native)",
     "EXACT", "Wildbret = Hirsch; USDA Sitka-Hirsch ist die einzige rohe Venison-Variante."),
    (81, "Wildfleisch", "1", "SIGHI_FoodList_2024-08-29_p6_r63", "167622", "Hirsch (Wildbret), Sitka, roh (Alaska Native)", "Deer (venison), sitka, raw (Alaska Native)",
     "NEAR_EXACT", "Wildfleisch ist Sammelbegriff (Hirsch/Reh/Wildschwein); Hirsch ist die häufigste Vertretung. DUPLICATE_OF=80."),
    (82, "Wurstwaren", "2", "SIGHI_FoodList_2024-08-29_p6_r65", "172013", "Wurstwaren, Rind- und Schweinefleisch", "Bologna, beef and pork",
     "EXACT", "Generische Wurstwaren = gemischte Brühwurst (Bologna) als USDA-Standardreferenz."),
    (83, "Zunge, Rindszunge, Kalbszunge", "2", "SIGHI_FoodList_2024-08-29_p6_r67", "170196", "Rindfleisch, verschiedene Fleischsorten und Nebenerzeugnisse, Zunge, roh", "Beef, variety meats and by-products, tongue, raw",
     "EXACT", "Rindszunge roh; direkte 1:1-Übereinstimmung."),

    # ====== FISCH ======
    (84, "Anchovis, Sardellen-Konserven", "3", "SIGHI_FoodList_2024-08-29_p7_r5", "174183", "Fisch, Sardellen, europäisch, in Öl konserviert, abgetropfte feste Bestandteile", "Fish, anchovy, european, canned in oil, drained solids",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (85, "Fisch, 'Frischfisch' vom Kühlregal / auf Eis", "3", "SIGHI_FoodList_2024-08-29_p7_r7", "175153", "Fisch, Forelle, gemischte Arten, roh", "Fish, trout, mixed species, raw",
     "APPROX", "SIGHI meint generischen rohen Fisch der nicht fangfrisch ist (Histamin-Aufbau bei Lagerung). Forelle als neutraler Süßwasser-Fisch dient als Nährwert-Approximation."),
    (86, "Fisch, fangfrisch oder tiefgekühlt", "0", "SIGHI_FoodList_2024-08-29_p7_r8", "175153", "Fisch, Forelle, gemischte Arten, roh", "Fish, trout, mixed species, raw",
     "APPROX", "Generischer fangfrischer Fisch; selbe USDA-Quelle wie #85. DUPLICATE_OF=85."),
    (87, "Forellen (Süsswasser): Seeforelle, Bachforelle,", "0", "SIGHI_FoodList_2024-08-29_p7_r9", "175153", "Fisch, Forelle, gemischte Arten, roh", "Fish, trout, mixed species, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung (gemischte Forellenarten roh)."),
    (88, "Lachs geräuchert, Räucherlachs", "2", "SIGHI_FoodList_2024-08-29_p7_r10", "173687", "Fisch, Lachs, Chinook, geräuchert", "Fish, salmon, chinook, smoked",
     "EXACT", "Direkte 1:1-Übereinstimmung; Chinook ist Standardreferenz für Räucherlachs."),
    (89, "Räucherlachs, Lachs geräuchert", "2", "SIGHI_FoodList_2024-08-29_p7_r11", "173687", "Fisch, Lachs, Chinook, geräuchert", "Fish, salmon, chinook, smoked",
     "EXACT", "Alphabet-Synonym-Dublette zu #88. DUPLICATE_OF=88."),
    (90, "Sardellen-Konserven, Sardellenpaste, Anchovis", "3", "SIGHI_FoodList_2024-08-29_p7_r12", "174183", "Fisch, Sardellen, europäisch, in Öl konserviert, abgetropfte feste Bestandteile", "Fish, anchovy, european, canned in oil, drained solids",
     "EXACT", "Alphabet-Synonym-Dublette zu #84. DUPLICATE_OF=84."),
    (91, "Thunfisch", "3", "SIGHI_FoodList_2024-08-29_p7_r13", "173706", "Fisch, Thunfisch, frisch, Roter Thun, roh", "Fish, tuna, fresh, bluefin, raw",
     "EXACT", "Standardreferenz für frischen rohen Thunfisch (Bluefin)."),

    # ====== MEERESFRÜCHTE ======
    (92, "Austern", "2", "SIGHI_FoodList_2024-08-29_p7_r15", "174219", "Weichtiere, Austern, Pazifik, roh", "Mollusks, oyster, Pacific, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung (Pacific oyster als Standardvertretung)."),
    (93, "Crevetten", "2", "SIGHI_FoodList_2024-08-29_p7_r16", "175179", "Krustentiere, Garnelen, roh", "Crustaceans, shrimp, raw",
     "NEAR_EXACT", "Crevetten = franz./schweiz. für Garnelen. DUPLICATE_OF=94."),
    (94, "Garnelen", "2", "SIGHI_FoodList_2024-08-29_p7_r17", "175179", "Krustentiere, Garnelen, roh", "Crustaceans, shrimp, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (95, "Hummer", "2", "SIGHI_FoodList_2024-08-29_p7_r18", "174208", "Krustentiere, Hummer, nördlich, roh", "Crustaceans, lobster, northern, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (96, "Krabben", "2", "SIGHI_FoodList_2024-08-29_p7_r19", "174204", "Krustentiere, Krabben, blau, roh", "Crustaceans, crab, blue, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung (Blue crab als USDA-Standardvertretung)."),
    (97, "Krebse", "2", "SIGHI_FoodList_2024-08-29_p7_r20", "174206", "Krebstiere, Flusskrebse, gemischte Arten, wild, roh", "Crustaceans, crayfish, mixed species, wild, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (98, "Krevetten", "2", "SIGHI_FoodList_2024-08-29_p7_r21", "175179", "Krustentiere, Garnelen, roh", "Crustaceans, shrimp, raw",
     "NEAR_EXACT", "Krevetten = Schreibvariante zu Crevetten = Garnelen; selbe USDA-Quelle. DUPLICATE_OF=94."),
    (99, "Krustentiere und Schalentiere", "2", "SIGHI_FoodList_2024-08-29_p7_r22", "175179", "Krustentiere, Garnelen, roh", "Crustaceans, shrimp, raw",
     "APPROX", "Sammelbegriff für alle Krustentiere; Garnelen als häufigste Vertretung."),
    (100, "Langusten", "2", "SIGHI_FoodList_2024-08-29_p7_r23", "174211", "Krustentiere, Languste, gemischte Arten, roh", "Crustaceans, spiny lobster, mixed species, raw",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (101, "Meeresfrüchte (=wirbellose Meerestiere)", "2", "SIGHI_FoodList_2024-08-29_p7_r24", "174219", "Weichtiere, Austern, Pazifik, roh", "Mollusks, oyster, Pacific, raw",
     "APPROX", "Sammelbegriff für wirbellose Meerestiere (Mollusken+Krustentiere); Austern als generischer Mollusken-Vertreter."),
    (102, "Muscheln (Miesmuschel, Austern, Venusmuschel,", "2", "SIGHI_FoodList_2024-08-29_p7_r25", "174216", "Mollusken, Miesmuscheln, roh", "Mollusks, mussel, blue, raw",
     "EXACT", "Miesmuschel als USDA-Standardvertretung für Muscheln."),
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
