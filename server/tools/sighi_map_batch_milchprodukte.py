"""Append a batch of SIGHI→USDA mapping decisions to sighi_usda_mapping.csv.
Decisions are made by the agent (me) with reasoning per row.
Categories processed in this batch: Milchprodukte (sighi_idx 5..52).
"""
import csv
from pathlib import Path

OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv")

# Each row: (sighi_idx, sighi_keyword, sighi_score, sighi_source_ref, usda_fdc_id, usda_name_de, usda_name_en, match_quality, reasoning)
ROWS = [
    (5, "Blauschimmelkäse", "2", "SIGHI_FoodList_2024-08-29_p4_r22", "172175", "Käse, blau", "Cheese, blue",
     "EXACT", "Direkte Entsprechung: blauer Schimmelkäse generisch."),
    (6, "Butter: Sauerrahmbutter, mildgesäuerte Butter", "1", "SIGHI_FoodList_2024-08-29_p4_r24", "173430", "Butter, ohne Salz", "Butter, without salt",
     "NEAR_EXACT", "USDA hat keine gesäuerte Butter; Makros sind nahezu identisch zu Süßrahmbutter (Fettgehalt ~82%); nur Aroma und Histamin unterscheiden sich."),
    (7, "Butter: Süßrahmbutter, Süssrahmbutter", "0", "SIGHI_FoodList_2024-08-29_p4_r25", "173430", "Butter, ohne Salz", "Butter, without salt",
     "EXACT", "Süßrahmbutter ist die Standard-Butter weltweit; USDA-Default ohne Salz."),
    (8, "Butterkäse", "0", "SIGHI_FoodList_2024-08-29_p4_r26", "171241", "Käse, Gouda", "Cheese, gouda",
     "APPROX", "USDA hat keinen Butterkäse; Gouda ist ein milder Schnittkäse mit ähnlichem Fett-/Wassergehalt - beste verfügbare Approximation für Nährwerte."),
    (9, "Buttermilch, angesäuert", "1", "SIGHI_FoodList_2024-08-29_p4_r27", "172225", "Milch, Buttermilch, flüssig, voll", "Milk, buttermilk, fluid, whole",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (10, "Camembert", "2", "SIGHI_FoodList_2024-08-29_p4_r29", "172178", "Käse, Camembert", "Cheese, camembert",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (11, "Cheddar Käse", "2", "SIGHI_FoodList_2024-08-29_p4_r30", "173414", "Käse, Cheddar", "Cheese, cheddar",
     "EXACT", "Direkte 1:1-Übereinstimmung; USDA-Standard-Cheddar (vollfett)."),
    (12, "crème fraïche", "1", "SIGHI_FoodList_2024-08-29_p4_r31", "2346387", "Sahne, sauer, vollfett", "Cream, sour, full fat",
     "NEAR_EXACT", "Crème fraîche ist eine vollfette gesäuerte Sahne (~30-40% Fett). USDA 'Cream, sour, full fat' ist die beste Entsprechung; europäische Crème fraîche hat tendenziell etwas höheren Fettgehalt aber das ist innerhalb der USDA-Varianz."),
    (13, "Edelschimmelkäse", "2", "SIGHI_FoodList_2024-08-29_p4_r32", "172175", "Käse, blau", "Cheese, blue",
     "NEAR_EXACT", "Edelschimmelkäse ist Sammelbegriff für Schimmel-Käse; USDA 'Käse, blau' ist die generische Entsprechung; Roquefort wäre eine spezifische Wahl, blau ist neutraler."),
    (14, "Feta, Feta-Käse", "1", "SIGHI_FoodList_2024-08-29_p4_r33", "173420", "Käse, Feta", "Cheese, feta",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (15, "Fontina Käse", "2", "SIGHI_FoodList_2024-08-29_p4_r34", "170843", "Käse, Fontina", "Cheese, fontina",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (16, "Frischkäse", "0", "SIGHI_FoodList_2024-08-29_p4_r37", "2346385", "Frischkäse, Vollfett, Block", "Cream cheese, full fat, block",
     "EXACT", "Direkte 1:1-Übereinstimmung; USDA-Vollfett-Frischkäse."),
    (17, "Geheimratskäse", "0", "SIGHI_FoodList_2024-08-29_p4_r38", "171241", "Käse, Gouda", "Cheese, gouda",
     "APPROX", "Geheimratskäse ist ein milder halbharter Schnittkäse, ähnlich jungem Gouda; USDA hat keinen Eintrag; Gouda ist die nächstliegende Nährwert-Approximation."),
    (18, "Ghee", "1", "SIGHI_FoodList_2024-08-29_p5_r10", "171314", "Butter, Butterschmalz (Ghee)", "Butter, Clarified butter (ghee)",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (19, "Gouda Käse, gereift", "2", "SIGHI_FoodList_2024-08-29_p5_r12", "171241", "Käse, Gouda", "Cheese, gouda",
     "EXACT", "Direkte Entsprechung; USDA macht keine Reifegrad-Differenzierung."),
    (20, "Gouda Käse, jung", "0", "SIGHI_FoodList_2024-08-29_p5_r13", "171241", "Käse, Gouda", "Cheese, gouda",
     "EXACT", "Direkte Entsprechung; selbe USDA-Quelle wie gereifter Gouda (USDA differenziert nicht), Histamin-Unterschied ist auf SIGHI-Seite abgebildet."),
    (21, "Haltbarmilch, H-Milch, UHT-Milch, Up-Milch", "0", "SIGHI_FoodList_2024-08-29_p5_r14", "172217", "Milch, Vollmilch, 3,25% Milchfett, ohne Zusatz von Vitamin A und Vitamin D", "Milk, whole, 3.25% milkfat, without added vitamin A and vitamin D",
     "NEAR_EXACT", "UHT-Behandlung ändert Makro-Nährwerte minimal (~10% Vitaminverlust). Vollmilch ist die nährstoffliche Basis."),
    (22, "Joghurt nature", "1", "SIGHI_FoodList_2024-08-29_p5_r15", "173410", "Joghurt, natur, vollfett", "Yogurt, plain, whole milk",
     "EXACT", "Direkte 1:1-Übereinstimmung; vollfetter Naturjoghurt."),
    (23, "Käse: lange gereifte Sorten und Hartkäse (z.B.", "3", "SIGHI_FoodList_2024-08-29_p5_r17", "170848", "Käse, Parmesan, Hartkäse", "Cheese, parmesan, hard",
     "EXACT", "Parmesan ist der archetypische lange gereifte Hartkäse; SIGHI-Klammer beginnt mit Parmesan/Bergkäse/etc."),
    (24, "Käsezubereitungen (=Mischungen mit weiteren", "2", "SIGHI_FoodList_2024-08-29_p5_r19", "173448", "KRAFT VELVEETA Pasteurisierter Schmelzkäseaufstrich", "KRAFT VELVEETA Pasteurized Process Cheese Spread",
     "APPROX", "Käsezubereitungen sind verarbeitete Käsemischungen mit Zusatzstoffen; VELVEETA als USDA-Standardreferenz für solche Produkte."),
    (25, "Kefir", "1", "SIGHI_FoodList_2024-08-29_p5_r20", "170904", "Kefir, fettarm, einfach, LIFEWAY", "Kefir, lowfat, plain, LIFEWAY",
     "NEAR_EXACT", "USDA hat nur fettarme Kefir-Variante einer Marke (LIFEWAY); klassischer Kefir ist vollfett. Makros sind ähnlich genug; Fettanteil-Differenz ~2g/100g."),
    (26, "Mascarpone Käse", "0", "SIGHI_FoodList_2024-08-29_p5_r22", "2346385", "Frischkäse, Vollfett, Block", "Cream cheese, full fat, block",
     "APPROX", "USDA hat keinen Mascarpone; Mascarpone ist ein italienischer Frischkäse mit ~44% Fett, Cream cheese ~34% Fett - ähnlicher Typus, etwas niedrigerer Fettanteil; beste verfügbare Approximation."),
    (27, "Milch, laktosefrei", "1", "SIGHI_FoodList_2024-08-29_p5_r24", "172217", "Milch, Vollmilch, 3,25% Milchfett", "Milk, whole, 3.25% milkfat",
     "NEAR_EXACT", "Laktose-Hydrolyse ändert Makros/Mikros nicht; reine Vollmilch ist der nährwertliche Zwilling."),
    (28, "Milch, pasteurisiert (PAST-Milch)", "0", "SIGHI_FoodList_2024-08-29_p5_r25", "172217", "Milch, Vollmilch, 3,25% Milchfett", "Milk, whole, 3.25% milkfat",
     "EXACT", "Pasteurisierte Vollmilch ist USDA-Default."),
    (29, "Milchpulver", "1", "SIGHI_FoodList_2024-08-29_p5_r26", "170876", "Milch, trocken, voll, mit Zusatz von Vitamin D", "Milk, dry, whole, with added vitamin D",
     "EXACT", "Direkte 1:1-Übereinstimmung; getrocknete Vollmilch."),
    (30, "Molke: Sauermolke", "0", "SIGHI_FoodList_2024-08-29_p5_r28", "170885", "Molke, Säure, Flüssigkeit", "Whey, acid, fluid",
     "EXACT", "Direkte 1:1-Übereinstimmung; saure Molke flüssig."),
    (31, "Molke: Süßmolke, Süssmolke, Labmolke", "0", "SIGHI_FoodList_2024-08-29_p5_r29", "171282", "Molke, süß, flüssig", "Whey, sweet, fluid",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (32, "Mozzarella Käse", "0", "SIGHI_FoodList_2024-08-29_p5_r30", "170845", "Käse, Mozzarella, Vollmilch", "Cheese, mozzarella, whole milk",
     "EXACT", "Direkte 1:1-Übereinstimmung; Vollmilch-Variante."),
    (33, "Obers, Sahne (wenn ohne Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p5_r31", "2346386", "Sahne, schwer", "Cream, heavy",
     "EXACT", "Obers = österr. für Sahne; Heavy cream entspricht Schlagsahne/Schlagobers (~36% Fett)."),
    (34, "Quark", "0", "SIGHI_FoodList_2024-08-29_p5_r34", "2346384", "Hüttenkäse, Vollfett, großer oder kleiner Quark", "Cottage cheese, full fat, large or small curd",
     "EXACT", "Quark ↔ Cottage cheese full fat. USDA verwendet 'Quark' im Namen."),
    (35, "Raclette Käse", "2", "SIGHI_FoodList_2024-08-29_p5_r36", "171241", "Käse, Gouda", "Cheese, gouda",
     "APPROX", "Raclette ist ein halbharter Schweizer Schmelzkäse; USDA hat ihn nicht; Gouda ist nährwertlich nächstliegend (Fett ~28%, Protein ~25%)."),
    (36, "Rahm, süß (wenn ohne Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p5_r38", "2346386", "Sahne, schwer", "Cream, heavy",
     "EXACT", "Rahm = Sahne; süßer Rahm = Schlagsahne (heavy cream)."),
    (37, "Ricotta Käse", "0", "SIGHI_FoodList_2024-08-29_p5_r40", "170851", "Käse, Ricotta, Vollmilch", "Cheese, ricotta, whole milk",
     "EXACT", "Direkte 1:1-Übereinstimmung; Vollmilch-Variante."),
    (38, "Rohmilch", "0", "SIGHI_FoodList_2024-08-29_p5_r41", "172217", "Milch, Vollmilch, 3,25% Milchfett", "Milk, whole, 3.25% milkfat",
     "NEAR_EXACT", "Rohmilch unterscheidet sich nährwertlich kaum von pasteurisierter Vollmilch; Vitamin-Verlust durch Pasteurisierung gering."),
    (39, "Rohmilchkäse", "2", "SIGHI_FoodList_2024-08-29_p5_r42", "170848", "Käse, Parmesan, Hartkäse", "Cheese, parmesan, hard",
     "APPROX", "Rohmilchkäse ist ein Sammelbegriff, meist lange gereifte Hartkäse (Parmigiano, Comté, Gruyère); Parmesan ist die archetypische Vertretung."),
    (40, "Rohmilchprodukte", "2", "SIGHI_FoodList_2024-08-29_p5_r43", "", "", "",
     "NO_MATCH", "Sammelbegriff ohne spezifisches Lebensmittel; nicht ein einzelner USDA-Eintrag abbildbar. Score wird gesetzt, Nährwerte bleiben NULL."),
    (41, "Roquefort Käse", "2", "SIGHI_FoodList_2024-08-29_p5_r44", "171250", "Käse, Roquefort", "Cheese, roquefort",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (42, "Sahne, süß (wenn ohne Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p5_r46", "2346386", "Sahne, schwer", "Cream, heavy",
     "EXACT", "Süße Sahne = unfermentierte Schlagsahne; heavy cream."),
    (43, "Sauerrahm", "1", "SIGHI_FoodList_2024-08-29_p5_r48", "2346387", "Sahne, sauer, vollfett", "Cream, sour, full fat",
     "EXACT", "Direkte 1:1-Übereinstimmung; vollfette saure Sahne."),
    (44, "saure Sahne", "1", "SIGHI_FoodList_2024-08-29_p5_r50", "2346387", "Sahne, sauer, vollfett", "Cream, sour, full fat",
     "EXACT", "Synonym zu Sauerrahm; selbe USDA-Quelle wie #43."),
    (45, "Schafmilch, Schafsmilch", "0", "SIGHI_FoodList_2024-08-29_p5_r53", "170882", "Milch, Schafe, flüssig", "Milk, sheep, fluid",
     "EXACT", "Direkte 1:1-Übereinstimmung."),
    (46, "Schimmelkäse", "2", "SIGHI_FoodList_2024-08-29_p5_r55", "172175", "Käse, blau", "Cheese, blue",
     "NEAR_EXACT", "Schimmelkäse ist Sammelbegriff für Blauschimmel- und Weißschimmel-Käse; USDA 'Käse, blau' ist die generische Vertretung."),
    (47, "Schlagobers, Schlagsahne (wenn ohne Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p5_r56", "2346386", "Sahne, schwer", "Cream, heavy",
     "EXACT", "Schlagobers = österr. für Schlagsahne; heavy cream (~36% Fett)."),
    (48, "Schmelzkäse", "2", "SIGHI_FoodList_2024-08-29_p5_r57", "173448", "KRAFT VELVEETA Pasteurisierter Schmelzkäseaufstrich", "KRAFT VELVEETA Pasteurized Process Cheese Spread",
     "NEAR_EXACT", "USDA hat keinen generischen Schmelzkäse; VELVEETA ist die Standard-USDA-Referenz für Pasteurisierter Schmelzkäse."),
    (49, "Süssrahm, Süßrahm (wenn ohne Zusatzstoffe)", "0", "SIGHI_FoodList_2024-08-29_p6_r5", "2346386", "Sahne, schwer", "Cream, heavy",
     "EXACT", "Süßrahm = süße ungesäuerte Sahne (Synonym zu Rahm, süß); heavy cream."),
    (50, "Topfen", "0", "SIGHI_FoodList_2024-08-29_p6_r6", "2346384", "Hüttenkäse, Vollfett, großer oder kleiner Quark", "Cottage cheese, full fat, large or small curd",
     "EXACT", "Topfen = österr. für Quark; selbe USDA-Quelle wie Quark (#34)."),
    (51, "UHT-Milch, Up-Milch, H-Milch, Haltbarmilch", "0", "SIGHI_FoodList_2024-08-29_p6_r7", "172217", "Milch, Vollmilch, 3,25% Milchfett", "Milk, whole, 3.25% milkfat",
     "NEAR_EXACT", "Alphabet-Synonym-Dublette zu #21 (selbe Bezeichnung, andere Sortierung). Selbes USDA-Mapping; DUPLICATE_OF=21."),
    (52, "Ziegenmilch", "0", "SIGHI_FoodList_2024-08-29_p6_r9", "171278", "Milch, Ziege, flüssig, mit Zusatz von Vitamin D", "Milk, goat, fluid, with added vitamin D",
     "EXACT", "Direkte 1:1-Übereinstimmung; einzige Ziegenmilch-Variante in USDA."),
]

def main():
    # Read existing rows to skip duplicates
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
            sighi_idx = str(r[0])
            if sighi_idx in existing:
                print(f"  skip (exists): #{sighi_idx} {r[1]}"); continue
            w.writerow(r)
            appended += 1
    print(f"Appended {appended} rows. Mapping file now contains {len(existing)+appended} entries.")

if __name__ == "__main__":
    main()
