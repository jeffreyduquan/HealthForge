"""Dedupliziere SIGHI-Liste für USDA-Mapping.

- Drop E-Nummern (Zusatzstoffe, separate Kategorie)
- Drop Score `?` und `-` (uncertain — würden NULL erhalten)
- Markiere Alphabet-Synonym-Dubletten (z.B. "Lachs geräuchert, Räucherlachs"
  vs "Räucherlachs, Lachs geräuchert") — canonical = erste Vorkommen.

Output: sighi_deduped.csv mit Spalten:
  dedupe_id | sighi_idx | sighi_keyword | sighi_score | sighi_category | sighi_source_ref | duplicate_of_id (oder leer)
"""
import csv, re
from pathlib import Path

SIGHI = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_parsed.csv")
OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_deduped.csv")

ENUM = re.compile(r"\bE\d{3,4}\b")
def is_enum(name: str) -> bool:
    return bool(ENUM.search(name))

def canonical(name: str) -> str:
    """Token-Set in normalisierter Form: lowercase, no umlauts, sortierte Tokens.
    'Lachs geräuchert, Räucherlachs' und 'Räucherlachs, Lachs geräuchert' → gleich."""
    s = name.lower()
    s = s.replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
    s = re.sub(r"[^\w\s]", " ", s)
    tokens = sorted(s.split())
    return " ".join(tokens)

def main():
    rows = list(csv.reader(open(SIGHI, encoding="utf-8"), delimiter=";"))[1:]
    canonical_to_first = {}  # canonical → dedupe_id of first occurrence
    out_rows = []
    next_id = 1
    skipped_enum = 0
    skipped_uncertain = 0
    duplicates = 0
    for sighi_idx, r in enumerate(rows):
        keyword, score, category, src = r[0], r[1], r[2], r[3]
        if is_enum(keyword):
            skipped_enum += 1; continue
        if score in ("?", "-"):
            skipped_uncertain += 1; continue
        c = canonical(keyword)
        dup_of = canonical_to_first.get(c)
        if dup_of is None:
            dedupe_id = next_id; next_id += 1
            canonical_to_first[c] = dedupe_id
            out_rows.append([dedupe_id, sighi_idx, keyword, score, category, src, ""])
        else:
            dedupe_id = next_id; next_id += 1
            out_rows.append([dedupe_id, sighi_idx, keyword, score, category, src, dup_of])
            duplicates += 1

    with open(OUT, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        w.writerow(["dedupe_id","sighi_idx","sighi_keyword","sighi_score","sighi_category","sighi_source_ref","duplicate_of_id"])
        for r in out_rows:
            w.writerow(r)

    total = len(rows)
    canonical_count = len(canonical_to_first)
    print(f"SIGHI raw: {total}")
    print(f"  Skipped E-Nummern: {skipped_enum}")
    print(f"  Skipped Score ?/-: {skipped_uncertain}")
    print(f"  Remaining: {total - skipped_enum - skipped_uncertain}")
    print(f"  Davon Alphabet-Duplikate: {duplicates}")
    print(f"  → Canonical Eintraege zu mappen: {canonical_count}")
    print(f"\nOutput: {OUT}")

    # Sample dups
    print("\nBeispiel Duplikate (erste 10):")
    for r in out_rows:
        if r[6]:
            print(f"  #{r[0]} {r[2]!r} → DUPLICATE_OF #{r[6]}")
            if r[0] > 50: break

if __name__ == "__main__":
    main()
