"""Check if better USDA matches exist in our pool for the 8 problematic APPROX entries."""
import csv, os, re

USDA = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'seed', 'usda_fdc.csv')

queries = {
    '#150 Tapioka': ['tapioka', 'tapioca'],
    '#240 Kiefelerbse/Kichererbse?': ['kichererbse', 'chickpea', 'kaeferbohne', 'käferbohne', 'runner bean', 'feuerbohne', 'scarlet'],
    '#367 Pawpaw': ['pawpaw', 'asimina', 'papau', 'indianerbanane'],
    '#466 Grünalgen': ['chlorella', 'sea lettuce', 'ulva', 'meersalat'],
    '#491 gebrannter Zucker': ['karamell', 'caramel'],
    '#505 Palmzucker': ['palm sugar', 'palmzucker', 'jaggery', 'palmsirup'],
    '#560 Würze/Maggi': ['maggi', 'wuerze', 'würze', 'seasoning sauce', 'hydrolyzed'],
    '#579 Schilcherwein/Rosé': ['rose wine', 'rosé', 'pink wine'],
}

rows = list(csv.DictReader(open(USDA, encoding='utf-8'), delimiter=';'))
print(f'USDA pool size: {len(rows)}')
print()

for label, terms in queries.items():
    print(f'=== {label} ===')
    matches = set()
    for r in rows:
        nd = (r.get('name_de') or '').lower()
        ne = (r.get('name_en') or '').lower()
        for t in terms:
            if t.lower() in nd or t.lower() in ne:
                matches.add((r['fdc_id'], r['name_de'], r['name_en']))
                break
    if not matches:
        print('  KEIN Treffer im USDA-Pool')
    else:
        for fid, nd, ne in sorted(matches):
            print(f'  [{fid}] {nd}  ({ne})')
    print()
