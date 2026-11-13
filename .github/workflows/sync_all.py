import json, os, re, urllib.request

src = 'public/catalog.json' if os.path.exists('public/catalog.json') else 'catalog.json'
items = json.load(open(src, encoding='utf-8'))
if isinstance(items, dict):
    items = items.get('products', [])
by_id = {x['id']: x for x in items if 'id' in x}

try:
    req = urllib.request.Request(
        "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json",
        data=json.dumps(items).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='PUT'
    )
    urllib.request.urlopen(req, timeout=20)
except Exception as e:
    print("Firebase:", e)

for fn in ['public.html', 'index.html', 'public/public.html', 'public/index.html']:
    if not os.path.exists(fn):
        continue
    txt = open(fn, encoding='utf-8').read()
    m = re.search(r'(\[\s*\{[^;]*?\bNAV001\b[^;]*?Aashirvaad Superior MP[^;]*?\}\s*\])', txt, flags=re.S)
    if m:
        txt = txt[:m.start(1)] + json.dumps(items, ensure_ascii=False) + txt[m.end(1):]
    txt = txt.replace('306 verified grocery items', f'{len(items)} verified grocery items')
    open(fn, 'w', encoding='utf-8').write(txt)
    print("DONE:", fn)
