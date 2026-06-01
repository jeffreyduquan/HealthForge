"""Generiert pro SIGHI-Eintrag eine Liste von Top-USDA-Kandidaten
(reine Substring-Überlapp-Heuristik, KEINE Auto-Entscheidung).

Der Agent entscheidet pro Eintrag manuell welcher Kandidat passt
oder ob NO_MATCH gesetzt wird, und schreibt das Ergebnis ins
finale sighi_usda_mapping.csv.

Output: sighi_usda_candidates.csv
Spalten: dedupe_id | sighi_keyword | sighi_score | sighi_category | candidates_top5
wobei candidates_top5 = "fdc_id|name_de|name_en ; fdc_id|name_de|name_en ; ..."
"""
import csv, re
from pathlib import Path

SIGHI_FULL = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_parsed.csv")
USDA = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\usda_fdc.csv")
OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_candidates.csv")

STOPWORDS = {"der","die","das","und","oder","mit","ohne","von","bei","im","in","zu","aus","wenn","z.b."}

# Häufige zusammengesetzte Nachsilben in deutschen Lebensmittelnamen.
# Wenn ein Token länger als 6 Zeichen ist und mit einem dieser Suffixe endet,
# splitten wir in (Präfix, Suffix). Beispiel: "Wachteleier" → "wachtel" + "eier".
COMPOUND_SUFFIXES = [
    "kaese","milch","eier","fleisch","saft","wurst","mehl","oel","salz",
    "wein","bier","brot","kuchen","sirup","zucker","creme","mark","pulver",
    "butter","produkte","produkt","essig","apfel","beere","beeren","kohl",
    "frucht","fruechte","kerne","kern","nuss","nuesse","samen","blatt",
    "blueten","blueten","wurzel","wurzeln","gewuerz","gewuerze","sauce",
]
def split_compound(tok: str):
    if len(tok) <= 6: return [tok]
    for suf in COMPOUND_SUFFIXES:
        if tok.endswith(suf) and len(tok) > len(suf) + 2:
            prefix = tok[:-len(suf)]
            return [prefix, suf]
    return [tok]

def tokens(s: str):
    s = s.lower()
    s = s.replace("ä","ae").replace("ö","oe").replace("ü","ue").replace("ß","ss")
    s = re.sub(r"[^\w\s]"," ", s)
    base = [t for t in s.split() if len(t) >= 3 and t not in STOPWORDS]
    out = []
    for t in base:
        out.extend(split_compound(t))
    # Deduplicate while preserving order
    seen = set(); result = []
    for t in out:
        if t in seen: continue
        seen.add(t); result.append(t)
    return result

def score_match(query_tokens, usda_de, usda_en):
    """Count how many query tokens appear in USDA combined text."""
    combined = (usda_de + " " + usda_en).lower()
    combined = combined.replace("ä","ae").replace("ö","oe").replace("ü","ue").replace("ß","ss")
    return sum(1 for t in query_tokens if t in combined)

def main():
    sighi = list(csv.reader(open(SIGHI_FULL, encoding="utf-8"), delimiter=";"))[1:]
    print(f"SIGHI gesamt (alle 1030 inkl. E-Nummern, ?/-, Dubletten): {len(sighi)}")

    usda = list(csv.reader(open(USDA, encoding="utf-8"), delimiter=";"))[1:]
    print(f"USDA pool: {len(usda)}")

    out_rows = []
    for sighi_idx, r in enumerate(sighi):
        keyword, score, category, src = r[0], r[1], r[2], r[3]
        q_tokens = tokens(keyword)
        if not q_tokens:
            out_rows.append([sighi_idx, keyword, score, category, src, ""])
            continue
        scored = []
        for u in usda:
            de, en = u[1], (u[2] or "")
            m = score_match(q_tokens, de, en)
            if m == 0: continue
            scored.append((m, -len(de), u))
        scored.sort(key=lambda x: (-x[0], -x[1]))
        top = scored[:5]
        cand_str = " ;; ".join(f"{u[0]}|{u[1]}|{u[2]}" for _,_,u in top)
        out_rows.append([sighi_idx, keyword, score, category, src, cand_str])

    with open(OUT, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        w.writerow(["sighi_idx","sighi_keyword","sighi_score","sighi_category","sighi_source_ref","candidates_top5"])
        for r in out_rows:
            w.writerow(r)
    print(f"\nWritten {len(out_rows)} candidate-rows → {OUT}")
    no_cand = [r for r in out_rows if not r[5]]
    print(f"  → ohne USDA-Kandidaten (Agent muss kreativ suchen): {len(no_cand)}")

if __name__ == "__main__":
    main()
