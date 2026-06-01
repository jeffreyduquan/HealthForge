"""USDA search helper. Usage: python sighi_usda_search.py <de_keyword> [<de_keyword2> ...]
Returns top 15 USDA matches where ALL keywords appear in DE or EN name."""
import csv, sys
from pathlib import Path

USDA = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\usda_fdc.csv")

def main():
    if len(sys.argv) < 2:
        print("Usage: python sighi_usda_search.py <kw1> [<kw2> ...]")
        return
    needles = [n.lower() for n in sys.argv[1:]]
    usda = list(csv.reader(open(USDA, encoding="utf-8"), delimiter=";"))[1:]
    hits = []
    for r in usda:
        de = r[1].lower()
        en = (r[2] or "").lower()
        combined = de + " | " + en
        if all(n in combined for n in needles):
            hits.append(r)
    print(f"# {len(hits)} hits for {needles}")
    for r in hits[:20]:
        print(f"  {r[0]:>9} | {r[1]} | {r[2]}")

if __name__ == "__main__":
    main()
