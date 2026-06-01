"""Parse SIGHI alphabetical FoodList PDF using word coordinates.

Column boundaries (from inspection of page 4):
- Score column:       x0 ~ 52
- Marker codes:       x0 ~ 60-105 (H, A, L, !, ?)
- Bezeichnung DE:     x0 ~ 109-340
- Bemerkungen DE:     x0 ~ 341-563

Row grouping: words with same `top` (within 2pt) belong to one row.
Output CSV: keyword;score;category;source_ref
"""
import pdfplumber
import re
import csv
from collections import Counter
from pathlib import Path

PDF = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf")
OUT = Path(r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_parsed.csv")

SCORE_X_MIN, SCORE_X_MAX = 48, 60
NAME_X_MIN, NAME_X_MAX = 108, 340
VALID_SCORES = {"0", "1", "2", "3", "-", "?"}

KNOWN_CATEGORIES = {
    "Tierisch", "Pflanzlich", "Sonstiges", "Zusatzstoffe", "Getränke",
    "Eier", "Milchprodukte", "Fleisch", "Fisch", "Meeresfrüchte", "Honig",
    "Gemüse", "Obst", "Früchte", "Pilze", "Algen", "Nüsse", "Samen",
    "Hülsenfrüchte", "Getreide", "Pseudogetreide",
    "Brot", "Backwaren", "Süßwaren", "Süsswaren",
    "Kräuter", "Gewürze", "Fette", "Öle",
    "Sojaprodukte", "Fermentiertes",
    "Alkohol", "Alkoholische Getränke", "Nicht-alkoholische Getränke",
}

def is_pdf_artifact_text(s):
    if not s or s.startswith("Lebensmittel-Verträglichkeitsliste"): return True
    if "©" in s or "histaminintoleranz" in s.lower(): return True
    if s in ("Bezeichnung DE", "Bemerkungen DE"): return True
    return False

def group_words_by_row(words, y_tol=4.0):
    sorted_words = sorted(words, key=lambda w: (w["top"], w["x0"]))
    rows, current, top = [], [], None
    for w in sorted_words:
        if top is None or abs(w["top"] - top) <= y_tol:
            current.append(w)
            if top is None: top = w["top"]
        else:
            rows.append(current); current = [w]; top = w["top"]
    if current: rows.append(current)
    return rows

def parse_row(row_words):
    row_words = sorted(row_words, key=lambda w: w["x0"])
    if row_words:
        first = row_words[0]
        ft = first["text"].strip()
        if ft in KNOWN_CATEGORIES and first["x0"] < 50:
            return {"is_category": True, "name": ft}

    score = None
    for w in row_words:
        if SCORE_X_MIN <= w["x0"] <= SCORE_X_MAX:
            t = w["text"].strip()
            if t in VALID_SCORES:
                score = t; break
    if score is None: return None

    name_words = [w["text"] for w in row_words if NAME_X_MIN <= w["x0"] <= NAME_X_MAX]
    if not name_words: return None
    name = " ".join(name_words).strip()
    if is_pdf_artifact_text(name): return None
    return {"is_category": False, "score": score, "name": name}

def main():
    rows_out = []
    current_category = ""
    with pdfplumber.open(PDF) as pdf:
        for page_idx in range(3, len(pdf.pages)):  # skip intro pages 1-3
            page = pdf.pages[page_idx]
            words = page.extract_words()
            for ri, row in enumerate(group_words_by_row(words)):
                parsed = parse_row(row)
                if not parsed: continue
                if parsed.get("is_category"):
                    current_category = parsed["name"]; continue
                rows_out.append({
                    "keyword": parsed["name"],
                    "score": parsed["score"],
                    "category": current_category,
                    "source_ref": f"SIGHI_FoodList_2024-08-29_p{page_idx+1}_r{ri}",
                })

    seen, deduped = set(), []
    for r in rows_out:
        key = (r["keyword"].lower(), r["score"])
        if key in seen: continue
        seen.add(key); deduped.append(r)

    with open(OUT, "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        w.writerow(["keyword", "score", "category", "source_ref"])
        for r in deduped:
            w.writerow([r["keyword"], r["score"], r["category"], r["source_ref"]])

    dist = Counter(r["score"] for r in deduped)
    print(f"Parsed {len(deduped)} rows (raw {len(rows_out)})")
    print(f"Score dist: {dict(sorted(dist.items()))}")
    cat_counts = Counter(r["category"] for r in deduped)
    print(f"Categories ({len(cat_counts)}):")
    for c, n in sorted(cat_counts.items(), key=lambda kv: -kv[1]):
        print(f"  {n:4d}  {c!r}")
    print("\n--- Score 2 samples (first 30) ---")
    for r in deduped[:0]: pass
    twos = [r for r in deduped if r["score"] == "2"]
    for r in twos[:30]:
        print(f"  {r['keyword']!r} [{r['category']}]  ({r['source_ref']})")

if __name__ == "__main__":
    main()
