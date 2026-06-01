"""Relink #150 Tapioka and #240 Kiefelerbse to better USDA pool entries."""
import csv, os, shutil

PATH = os.path.join(os.path.dirname(__file__), 'sighi_usda_mapping.csv')
BACKUP = PATH + '.bak_relink_150_240'
shutil.copy2(PATH, BACKUP)

FIXES = {
    150: {
        'usda_fdc_id': '169717',
        'usda_name_de': 'Tapioka, Perlen, trocken',
        'usda_name_en': 'Tapioca, pearl, dry',
        'match_quality': 'EXACT',
        'reasoning': 'Tapioca pearl dry; USDA-Eintrag im Pool entdeckt. Vorher faelschlich auf glutenfreies Brot mit Tapioka gemappt.'
    },
    240: {
        'usda_fdc_id': '173756',
        'usda_name_de': 'Kichererbsen (Bengal-Gram), reife Samen, roh',
        'usda_name_en': 'Chickpeas (garbanzo beans, bengal gram), mature seeds, raw',
        'match_quality': 'EXACT',
        'reasoning': '"Kiefelerbse" als regional/dialektale Schreibung von Kichererbse interpretiert (User-Bestaetigung). USDA chickpeas raw.'
    },
}

rows = list(csv.reader(open(PATH, encoding='utf-8'), delimiter=';'))
h = rows[0]; data = rows[1:]
col = {n: h.index(n) for n in h}

changed = 0
for r in data:
    idx = int(r[0])
    if idx in FIXES:
        f = FIXES[idx]
        print(f'#{idx} "{r[1]}"')
        print(f'  vorher:  [{r[col["usda_fdc_id"]]}] "{r[col["usda_name_de"]]}" ({r[col["match_quality"]]})')
        for k, v in f.items():
            r[col[k]] = v
        print(f'  nachher: [{r[col["usda_fdc_id"]]}] "{r[col["usda_name_de"]]}" ({r[col["match_quality"]]})')
        print()
        changed += 1

with open(PATH, 'w', encoding='utf-8', newline='') as f:
    w = csv.writer(f, delimiter=';', quoting=csv.QUOTE_MINIMAL)
    w.writerow(h)
    for r in data:
        w.writerow(r)
print(f'Relinks: {changed}')
