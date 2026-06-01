import csv
from collections import Counter

PATH = r'C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv'
rows = list(csv.reader(open(PATH, encoding='utf-8'), delimiter=';'))
header = rows[0]
data = rows[1:]

qcol = header.index('match_quality')
rcol = header.index('reasoning')

print(f'Total rows: {len(data)}')
qual = Counter(r[qcol] for r in data)
print()
print('=== match_quality Verteilung ===')
for k in ['EXACT', 'NEAR_EXACT', 'APPROX', 'NO_MATCH']:
    n = qual.get(k, 0)
    print(f'  {k:12s} {n:5d}  ({n*100/len(data):5.1f}%)')
print(f'  SUM           {sum(qual.values()):5d}')
print()

dup_total = sum(1 for r in data if 'DUPLICATE_OF=' in r[rcol])
dup_by_q = Counter(r[qcol] for r in data if 'DUPLICATE_OF=' in r[rcol])
print('=== Dubletten (DUPLICATE_OF=N in reasoning) ===')
print(f'  Gesamt: {dup_total}')
for k, v in sorted(dup_by_q.items()):
    print(f'    davon {k}: {v}')
print()

matched = sum(1 for r in data if r[qcol] in ('EXACT', 'NEAR_EXACT', 'APPROX'))
matched_unique = sum(1 for r in data if r[qcol] in ('EXACT', 'NEAR_EXACT', 'APPROX') and 'DUPLICATE_OF=' not in r[rcol])
nomatch = qual.get('NO_MATCH', 0)
nomatch_unique = sum(1 for r in data if r[qcol] == 'NO_MATCH' and 'DUPLICATE_OF=' not in r[rcol])

print('=== Effektive USDA-Coverage ===')
print(f'  Gemapped (EXACT+NEAR_EXACT+APPROX): {matched}  ({matched*100//len(data)}%)')
print(f'    davon Erstvorkommen (ohne Dubletten): {matched_unique}')
print(f'  Ohne USDA-Match (NO_MATCH): {nomatch}  ({nomatch*100//len(data)}%)')
print(f'    davon Erstvorkommen (ohne Dubletten): {nomatch_unique}')
