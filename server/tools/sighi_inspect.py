"""Inspect SIGHI alphabetical FoodList PDF to understand layout/columns."""
import pdfplumber
import sys

PDF = r"C:\Users\jawra\Documents\Projects\HealthForge\server\src\main\resources\seed\sighi_foodlist_alphabetisch.pdf"

with pdfplumber.open(PDF) as pdf:
    print(f"Total pages: {len(pdf.pages)}")
    for i, page in enumerate(pdf.pages):
        print(f"\n=== PAGE {i+1} ===")
        text = page.extract_text() or ""
        # print first 1500 chars of each page to gauge layout
        print(text[:1500])
        print("---")
        if i >= 2:
            break
