import json, os, re, urllib.request

src = 'public/catalog.json' if os.path.exists('public/catalog.json') else 'catalog.json'
data = json.load(open(src, encoding='utf-8'))
items = data if isinstance(data, list) else data.get('products', [])
by_id = { (x.get('id') or x.get('sku')): x for x in items if (x.get('id') or x.get('sku')) }
print(f"Loaded {len(by_id)} verified SKUs from {src}")

# 1. Sync Firebase RTDB directly
try:
    req = urllib.request.Request(
        "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json",
        data=json.dumps(items, ensure_ascii=False).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='PUT'
    )
    with urllib.request.urlopen(req, timeout=20) as resp:
        print("Firebase RTDB updated:", resp.status)
except Exception as e:
    print("Firebase sync note:", e)

# 2. Scan every HTML/JS/TS/JSON file in repo for old product strings
for root, dirs, files in os.walk('.'):
    if '.git' in dirs:
        dirs.remove('.git')
    if 'node_modules' in dirs:
        dirs.remove('node_modules')
    for f in files:
        if not f.endswith(('.html', '.js', '.jsx', '.ts', '.tsx', '.json')):
            continue
        fp = os.path.join(root, f)
        if os.path.abspath(fp) == os.path.abspath(src):
            continue
        try:
            txt = open(fp, encoding='utf-8').read()
        except Exception:
            continue
        if 'NAV001' not in txt and 'Aashirvaad Superior MP' not in txt:
            continue

        orig = txt
        m = re.search(r'(\[\s*\{[^;]*?\bNAV001\b[^;]*?Aashirvaad Superior MP[^;]*?\}\s*\])', txt, flags=re.S)
        if m:
            txt = txt[:m.start(1)] + json.dumps(items, ensure_ascii=False) + txt[m.end(1):]
        else:
            for s, it in by_id.items():
                nm = json.dumps(it.get('name') or it.get('title', ''))[1:-1]
                br = json.dumps(it.get('brand', ''))[1:-1]
                mr = int(float(it.get('mrp') or it.get('originalPrice', 100)))
                pr = int(float(it.get('price') or it.get('salePrice') or round(mr * 0.88)))
                txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,450}?["\']?(?:name|title)["\']?\s*:\s*["\'`])[^"\'`]*?(["\'`])', r'\g<1>' + nm + r'\g<2>', txt, flags=re.S)
                txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,450}?["\']?brand["\']?\s*:\s*["\'`])[^"\'`]*?(["\'`])', r'\g<1>' + br + r'\g<2>', txt, flags=re.S)
                txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,450}?["\']?(?:price|salePrice)["\']?\s*:\s*)\d+', r'\g<1>' + str(pr), txt, flags=re.S)
                txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,450}?["\']?(?:mrp|originalPrice)["\']?\s*:\s*)\d+', r'\g<1>' + str(mr), txt, flags=re.S)

        txt = txt.replace('306 verified grocery items', f'{len(items)} verified grocery items')
        if txt != orig:
            open(fp, 'w', encoding='utf-8').write(txt)
            print("UPDATED FILE:", fp)
