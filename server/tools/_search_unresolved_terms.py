import csv
p=r'C:\\Users\\jawra\\Documents\\Projects\\HealthForge\\server\\src\\main\\resources\\seed\\usda_fdc.csv'
rows=list(csv.DictReader(open(p,encoding='utf-8'),delimiter=';'))
terms=['rose','rosé','pawpaw','papaw','asimina','chlorella','algae','sea lettuce','maggi','seasoning sauce','palm sugar','jaggery','caramelized sugar','burnt sugar']
for t in terms:
    m=[r for r in rows if t in (r['name_en'] or '').lower() or t in (r['name_de'] or '').lower()]
    if m:
        print(f'\n=== {t} ({len(m)}) ===')
        for r in m[:20]:
            print(f"{r['fdc_id']} | {r['name_de']} | {r['name_en']}")
