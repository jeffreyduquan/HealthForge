"""Remove all DUPLICATE_OF=N rows from sighi_usda_mapping.csv, keeping only originals."""
import csv, shutil, os
from collections import Counter

PATH = os.path.join(os.path.dirname(__file__), 'sighi_usda_mapping.csv')
BACKUP = PATH + '.bak_predupe'

shutil.copy2(PATH, BACKUP)
print(f'Backup written: {BACKUP}')

rows = list(csv.reader(open(PATH, encoding='utf-8'), delimiter=';'))
header = rows[0]
data = rows[1:]
rcol = header.index('reasoning')
qcol = header.index('match_quality')

kept = [r for r in data if 'DUPLICATE_OF=' not in r[rcol]]
removed = [r for r in data if 'DUPLICATE_OF=' in r[rcol]]

print(f'Original: {len(data)}')
print(f'Removed (duplicates): {len(removed)}')
print(f'Kept: {len(kept)}')

# Verify each removed row's DUPLICATE_OF target exists in kept
kept_idxs = {int(r[0]) for r in kept}
broken = []
import re
for r in removed:
    m = re.search(r'DUPLICATE_OF=(\d+)', r[rcol])
    if m:
        target = int(m.group(1))
        if target not in kept_idxs:
            broken.append((int(r[0]), target, r[1]))

if broken:
    print(f'\n!! WARNUNG: {len(broken)} Dubletten verweisen auf einen Eintrag, der selbst entfernt wurde:')
    for src, tgt, kw in broken[:20]:
        print(f'  #{src} "{kw}" -> #{tgt} (target nicht mehr da)')
    print('\nFalls die targets selbst Dubletten sind (Dublettenkette), Auto-Resolve aktivieren? Hier: NICHT geschrieben, manuelle Pruefung noetig.')
else:
    print('OK: alle DUPLICATE_OF-Verweise zeigen auf erhaltene Originale.')
    with open(PATH, 'w', encoding='utf-8', newline='') as f:
        w = csv.writer(f, delimiter=';', quoting=csv.QUOTE_MINIMAL)
        w.writerow(header)
        for r in kept:
            w.writerow(r)
    print(f'\nGeschrieben: {len(kept)} Zeilen.')

    # New stats
    print('\n=== Neue match_quality Verteilung ===')
    q = Counter(r[qcol] for r in kept)
    for k in ['EXACT', 'NEAR_EXACT', 'APPROX', 'NO_MATCH']:
        n = q.get(k, 0)
        print(f'  {k:12s} {n:5d}  ({n*100/len(kept):5.1f}%)')
