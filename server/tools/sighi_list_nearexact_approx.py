import csv
PATH = r'C:\Users\jawra\Documents\Projects\HealthForge\server\tools\sighi_usda_mapping.csv'
rows = list(csv.reader(open(PATH, encoding='utf-8'), delimiter=';'))
h = rows[0]; data = rows[1:]
qcol = h.index('match_quality')
for label in ('NEAR_EXACT', 'APPROX'):
    sub = [r for r in data if r[qcol] == label]
    print(f'=== {label} ({len(sub)}) ===')
    for r in sub:
        print(f'  #{r[0]:>4s} score={r[2]:>2s}  SIGHI="{r[1]}"  -> USDA[{r[4]}] "{r[5]}"')
    print()
