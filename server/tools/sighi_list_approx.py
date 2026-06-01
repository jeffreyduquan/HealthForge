import csv
rows = list(csv.reader(open(r'C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv', encoding='utf-8'), delimiter=';'))
h = rows[0]
qcol = h.index('match_quality')
rcol = h.index('reasoning')
approx = [r for r in rows[1:] if r[qcol] == 'APPROX']
print(f'=== APPROX ({len(approx)}) ===')
for r in approx:
    print(f'#{r[0]:>4s} score={r[2]:>2s}  SIGHI="{r[1]}"')
    print(f'        USDA[{r[4]}] "{r[5]}"')
    print(f'        REASON: {r[rcol]}')
    print()
