"""Strict ingredient ↔ SIGHI mapping.

Conservative matching: only accept EXACT name match (after normalization),
either against the full SIGHI keyword or one of its comma-separated synonyms.
Everything else → NULL (user demand: only trustworthy matches).

Output: sighi_mapping.csv
Columns: ingredient_id|ingredient_name|current_score|new_score|match_type|matched_keyword|source_ref|reasoning
"""
import csv
import re
from pathlib import Path
from collections import Counter

INGREDIENTS = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\ingredients_current.csv")
SIGHI = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_parsed.csv")
OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_mapping.csv")

def normalize(s: str) -> str:
    s = s.lower().strip()
    s = s.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
    s = re.sub(r"[^\w\s]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s

def synonyms_of(keyword: str) -> list[str]:
    """Split 'Lachs geräuchert, Räucherlachs' into ['Lachs geräuchert','Räucherlachs','Lachs geräuchert, Räucherlachs']."""
    parts = [p.strip() for p in keyword.split(",") if p.strip()]
    # Heuristic: keep parts that have no parentheses and don't start with lowercase descriptor
    variants = set()
    variants.add(keyword.strip())
    for p in parts:
        if "(" in p or ")" in p: continue
        if not p: continue
        if len(p) < 3: continue
        variants.add(p)
    return list(variants)

def load_sighi():
    """Returns list of (variant_normalized, original_keyword, score, source_ref)."""
    entries = []
    with open(SIGHI, encoding="utf-8") as f:
        reader = csv.reader(f, delimiter=";")
        next(reader)  # header
        for row in reader:
            if len(row) < 4: continue
            keyword, score, _cat, source_ref = row[0], row[1], row[2], row[3]
            if score not in ("0", "1", "2", "3"): continue  # skip ? and -
            for variant in synonyms_of(keyword):
                entries.append((normalize(variant), keyword, int(score), source_ref))
    return entries

def load_ingredients():
    rows = []
    with open(INGREDIENTS, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n").rstrip("\r")
            if not line: continue
            parts = line.split("|")
            if len(parts) < 3: continue
            ing_id, name, score = parts[0], parts[1], parts[2]
            current = None if score == "NULL" else int(score)
            rows.append((ing_id, name, current))
    return rows

def main():
    sighi_entries = load_sighi()
    print(f"SIGHI variants: {len(sighi_entries)}")
    # Build lookup: normalized → list of (keyword, score, source_ref)
    lookup: dict[str, list] = {}
    for n, k, s, src in sighi_entries:
        lookup.setdefault(n, []).append((k, s, src))

    ingredients = load_ingredients()
    print(f"Ingredients: {len(ingredients)}")

    out_rows = []
    matched = 0
    changed = 0
    for ing_id, ing_name, current in ingredients:
        n = normalize(ing_name)
        hits = lookup.get(n, [])
        if not hits:
            out_rows.append([ing_id, ing_name, "" if current is None else current,
                             "NULL", "NONE", "", "", "kein exakter SIGHI-Match"])
            continue
        # Max-Score-Wins
        hits_sorted = sorted(hits, key=lambda x: -x[1])
        keyword, score, src = hits_sorted[0]
        match_type = "EXACT" if normalize(keyword) == n else "EXACT_SYNONYM"
        reasoning = f"normalisiert ingredient='{n}' == sighi variant von '{keyword}' (Max-Score-Wins über {len(hits)} Treffer)"
        out_rows.append([ing_id, ing_name, "" if current is None else current,
                         score, match_type, keyword, src, reasoning])
        matched += 1
        if current != score:
            changed += 1

    with open(OUT, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter="|", quoting=csv.QUOTE_MINIMAL)
        w.writerow(["ingredient_id","ingredient_name","current_score","new_score","match_type","matched_keyword","source_ref","reasoning"])
        for r in out_rows:
            w.writerow(r)

    print(f"\nMatched: {matched}/{len(ingredients)}  ({100*matched/len(ingredients):.1f}%)")
    print(f"Score changes from current state: {changed}")
    print(f"NULL (no trustworthy match): {len(ingredients)-matched}")
    new_dist = Counter(r[3] for r in out_rows)
    print(f"\nNew score distribution: {dict(sorted(new_dist.items(), key=lambda kv: str(kv[0])))}")
    # Currently-set but now NULL (data loss warning)
    losing = [r for r in out_rows if r[3]=="NULL" and r[2] != ""]
    print(f"\n⚠ Items losing score (currently set → now NULL): {len(losing)}")
    for r in losing[:20]:
        print(f"  {r[1]!r} (war {r[2]})")
    if len(losing) > 20: print(f"  ... +{len(losing)-20} more")

if __name__ == "__main__":
    main()
