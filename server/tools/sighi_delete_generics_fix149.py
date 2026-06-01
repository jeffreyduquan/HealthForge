"""Delete 15 Sammelbegriffe + fix Suesskartoffel mapping."""
import csv, os, shutil
from collections import Counter

PATH = os.path.join(os.path.dirname(__file__), 'sighi_usda_mapping.csv')
BACKUP = PATH + '.bak_predelete_generics'
shutil.copy2(PATH, BACKUP)
print(f'Backup: {BACKUP}')

# Indices to delete (15 Sammelbegriffe)
DELETE = {24, 85, 99, 101, 194, 199, 244, 312, 451, 459, 462, 472, 519, 566, 612}

# Suesskartoffel fix: idx 149, change USDA_ID 169303 -> 168482
FIX_149 = {
    'usda_fdc_id': '168482',
    'usda_name_de': 'Süßkartoffel, roh, unzubereitet',
    'usda_name_en': 'Sweet potato, raw, unprepared',
    'match_quality': 'EXACT',
    'reasoning': 'Sweet potato raw — Knolle (vorher faelschlich auf Blaetter 169303 gemappt, korrigiert).'
}

rows = list(csv.reader(open(PATH, encoding='utf-8'), delimiter=';'))
h = rows[0]; data = rows[1:]
col = {name: h.index(name) for name in h}

kept = []
deleted_log = []
fixed_log = []
for r in data:
    idx = int(r[0])
    if idx in DELETE:
        deleted_log.append((idx, r[1]))
        continue
    if idx == 149:
        before = (r[col['usda_fdc_id']], r[col['usda_name_de']], r[col['match_quality']])
        r[col['usda_fdc_id']] = FIX_149['usda_fdc_id']
        r[col['usda_name_de']] = FIX_149['usda_name_de']
        r[col['usda_name_en']] = FIX_149['usda_name_en']
        r[col['match_quality']] = FIX_149['match_quality']
        r[col['reasoning']] = FIX_149['reasoning']
        after = (r[col['usda_fdc_id']], r[col['usda_name_de']], r[col['match_quality']])
        fixed_log.append((idx, r[1], before, after))
    kept.append(r)

with open(PATH, 'w', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', quoting=csv.QUOTE_MINIMAL)
    w.writerow(h)
    for r in kept:
        w.writerow(r)

print(f'Vorher: {len(data)}  Nachher: {len(kept)}  (entfernt: {len(deleted_log)})')
print()
print('=== Geloescht ===')
for idx, kw in deleted_log:
    print(f'  #{idx:>4d}  "{kw}"')
print()
print('=== Korrigiert ===')
for idx, kw, before, after in fixed_log:
    print(f'  #{idx} "{kw}"')
    print(f'    vorher: {before}')
    print(f'    nachher: {after}')
print()

# Neue Verteilung
qcol = h.index('match_quality')
q = Counter(r[qcol] for r in kept)
print('=== Neue Verteilung ===')
for k in ['EXACT', 'NEAR_EXACT', 'APPROX', 'NO_MATCH']:
    n = q.get(k, 0)
    print(f'  {k:12s} {n:5d}  ({n*100/len(kept):5.1f}%)')
