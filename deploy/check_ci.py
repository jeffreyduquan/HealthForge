import sys, json
d = json.load(sys.stdin)
for r in d.get('check_runs', []):
    print(f"{r['name']}: {r.get('conclusion') or r.get('status')}")
