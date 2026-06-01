"""Debug row grouping on page 4."""
import pdfplumber
from collections import Counter

PDF = r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf"

with pdfplumber.open(PDF) as pdf:
    page = pdf.pages[3]
    words = page.extract_words()
    # sort by top
    sorted_w = sorted(words, key=lambda w: (w["top"], w["x0"]))
    # group with bigger tol
    rows, current, top = [], [], None
    for w in sorted_w:
        if top is None or abs(w["top"] - top) <= 2.5:
            current.append(w)
            if top is None: top = w["top"]
        else:
            rows.append(current); current = [w]; top = w["top"]
    if current: rows.append(current)
    print(f"Total rows on page 4: {len(rows)}")
    for i, row in enumerate(rows[:30]):
        # show first 8 words: x0, text
        items = [(round(w["x0"],1), w["text"]) for w in sorted(row, key=lambda w: w["x0"])]
        print(f"Row {i:3d}: {items[:10]}")
