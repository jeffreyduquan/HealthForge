"""Analyze Sammelbegriffe in NEAR_EXACT/APPROX list:
- For each generic SIGHI keyword, search SIGHI list for specific sub-variants.
- For #176 Margarine, list all USDA margarine entries to assess match quality.
"""
import csv, os, re

TOOLS = os.path.dirname(__file__)
MAPPING = os.path.join(TOOLS, 'sighi_usda_mapping.csv')
SIGHI = os.path.join(TOOLS, '..', 'src', 'main', 'resources', 'seed', 'sighi_foodlist_parsed.csv')
USDA = os.path.join(TOOLS, '..', 'src', 'main', 'resources', 'seed', 'usda_fdc.csv')

# Load mapping
rows = list(csv.reader(open(MAPPING, encoding='utf-8'), delimiter=';'))
h = rows[0]; data = rows[1:]

# Sammelbegriffe / Kategorien / Generic to investigate (from current 116 NEAR_EXACT+APPROX)
GENERICS = [
    (24, 'Käsezubereitungen', ['käse', 'cheese']),
    (35, 'Raclette', ['raclette']),
    (39, 'Rohmilchkäse', ['rohmilch', 'rohmilchkäse']),
    (85, 'Frischfisch', ['fisch', 'frischfisch']),
    (99, 'Krustentiere und Schalentiere', ['krustentier', 'schalentier', 'garnele', 'krabbe', 'hummer']),
    (101, 'Meeresfrüchte', ['meeresfrüchte', 'auster', 'muschel', 'tintenfisch']),
    (105, 'Backwaren', ['backware', 'gebäck']),
    (106, 'Brot', ['brot']),
    (149, 'Süsskartoffel (FEHLER)', ['süsskartoffel', 'süßkartoffel']),
    (176, 'Margarine', ['margarine']),
    (194, 'Blattsalate', ['salat', 'eisberg', 'lollo', 'romana', 'kopfsalat', 'feldsalat', 'rucola']),
    (199, 'Bohnen allgemein', ['bohne', 'kidney', 'borlotti']),
    (244, 'Kohlsorten', ['kohl', 'rotkohl', 'weißkohl', 'wirsing']),
    (250, 'Kürbisse', ['kürbis']),
    (260, 'Oliven', ['olive']),
    (263, 'Paprika scharf', ['paprika', 'chili', 'jalapeno', 'serrano']),
    (271, 'Radieschen scharf', ['radieschen', 'rettich']),
    (312, 'Zwiebel andere', ['zwiebel']),
    (451, 'Zitrusfrüchte', ['zitrone', 'orange', 'mandarine', 'grapefruit', 'limette']),
    (459, 'Algen', ['alge', 'seetang', 'nori', 'wakame', 'kombu']),
    (462, 'Braunalgen', ['braunalge', 'kombu', 'wakame']),
    (466, 'Grünalgen', ['grünalge', 'chlorella']),
    (471, 'Nori', ['nori']),
    (472, 'Pilze diverse', ['pilz', 'champignon', 'steinpilz', 'pfifferling']),
    (474, 'Rotalgen', ['rotalge', 'agar']),
    (517, 'Bouillon', ['bouillon', 'brühe']),
    (519, 'Brühe', ['brühe', 'bouillon']),
    (560, 'Würze', ['würze']),
    (566, 'Alkoholhaltige Getränke', ['wein', 'bier', 'schnaps', 'spirituose', 'alkohol']),
    (612, 'Limonadengetränke', ['limonade', 'cola']),
]

# Load SIGHI parsed list
sighi_rows = list(csv.DictReader(open(SIGHI, encoding='utf-8')))
# Detect column names
print('SIGHI columns:', sighi_rows[0].keys() if sighi_rows else 'none')

if sighi_rows:
    # Find the keyword/name column
    name_col = None
    for cand in ['keyword', 'name', 'sighi_keyword', 'food_name']:
        if cand in sighi_rows[0]:
            name_col = cand
            break
    if name_col is None:
        # take first non-idx col
        for k in sighi_rows[0].keys():
            if k != 'idx':
                name_col = k
                break
    print(f'Using SIGHI name column: {name_col}')
    print()

    # For each generic, find SIGHI matches
    print('=' * 80)
    print('SAMMELBEGRIFFE: Welche spezifischen Varianten gibt es in SIGHI?')
    print('=' * 80)
    for idx, label, patterns in GENERICS:
        print(f'\n### #{idx} {label}')
        matches = []
        for row in sighi_rows:
            name = row.get(name_col, '') or ''
            name_low = name.lower()
            for p in patterns:
                if p in name_low:
                    matches.append((row.get('idx', '?'), name))
                    break
        # Exclude the generic itself
        matches = [m for m in matches if str(m[0]) != str(idx)]
        if not matches:
            print('  (keine spezifischen SIGHI-Varianten)')
        else:
            print(f'  {len(matches)} spezifische SIGHI-Eintraege:')
            for mi, mn in matches[:25]:
                print(f'    #{mi:>4s}  "{mn}"')
            if len(matches) > 25:
                print(f'    ... +{len(matches)-25} weitere')
