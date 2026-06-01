"""Inspect SIGHI food table layout starting from page 4."""
import pdfplumber

PDF = r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf"

with pdfplumber.open(PDF) as pdf:
    for i in [3, 4, 5, 10, 15, 20]:
        if i >= len(pdf.pages): continue
        page = pdf.pages[i]
        print(f"\n=== PAGE {i+1} ===")
        text = page.extract_text() or ""
        print(text[:2000])
        print("---")
