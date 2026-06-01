"""Inspect word positions to find column boundaries."""
import pdfplumber

PDF = r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf"

with pdfplumber.open(PDF) as pdf:
    page = pdf.pages[3]  # page 4
    words = page.extract_words()
    print(f"Page size: width={page.width}, height={page.height}")
    print(f"Total words on page 4: {len(words)}")
    # Show first 50 words with their x0
    for w in words[:80]:
        print(f"  x0={w['x0']:6.1f}  x1={w['x1']:6.1f}  text={w['text']!r}")
