"""Print exact top values to determine row tolerance."""
import pdfplumber

PDF = r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf"
with pdfplumber.open(PDF) as pdf:
    page = pdf.pages[3]
    words = sorted(page.extract_words(), key=lambda w: (w["top"], w["x0"]))
    # Print all words on page 4 between tops 200-400 to see logical row structure
    for w in words:
        if 200 < w["top"] < 380:
            print(f"top={w['top']:7.2f}  x0={w['x0']:6.1f}  text={w['text']!r}")
