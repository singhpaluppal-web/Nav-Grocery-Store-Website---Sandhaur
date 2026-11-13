import json, os, re, shutil, urllib.request

src = 'public/catalog.json' if os.path.exists('public/catalog.json') else 'FINAL_LOCKED_BUILD/locked_golden_308_catalog.json'
items = json.load(open(src, encoding='utf-8'))
if isinstance(items, dict):
    items = items.get('products', [])
by_id = { (x.get('id') or x.get('sku')): x for x in items if (x.get('id') or x.get('sku')) }

# 1. Update static JSON files
os.makedirs('public/data', exist_ok=True)
with open('public/data/products_308.json', 'w', encoding='utf-8') as f:
    json.dump(items, f, indent=2, ensure_ascii=False)
if os.path.exists('catalog.json'):
    shutil.copyfile(src, 'catalog.json')

# 2. Update EVERY source file (.ts, .tsx, .js, .jsx, .html, .json) that has the old Atta data
for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in ('.git', 'node_modules', 'dist', 'build')]
    for fn in files:
        if not fn.endswith(('.ts', '.tsx', '.js', '.jsx', '.html', '.json')):
            continue
        fp = os.path.join(root, fn)
        if os.path.abspath(fp) in (os.path.abspath(src), os.path.abspath('public/data/products_308.json')):
            continue
        try:
            txt = open(fp, encoding='utf-8').read()
        except Exception:
            continue
        if 'Aashirvaad Superior MP' not in txt and 'NAV001' not in txt:
            continue

        orig = txt
        for s, it in by_id.items():
            nm = json.dumps(it.get('name') or it.get('title', ''))[1:-1]
            br = json.dumps(it.get('brand', ''))[1:-1]
            cat = json.dumps(it.get('category', 'Packaged Groceries'))[1:-1]
            mr = int(float(it.get('mrp') or it.get('originalPrice', 100)))
            pr = int(float(it.get('price') or it.get('salePrice') or round(mr * 0.88)))
            txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,500}?["\']?(?:name|title)["\']?\s*:\s*["\'`])[^"\'`]*?(["\'`])', r'\g<1>' + nm + r'\g<2>', txt, flags=re.S)
            txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,500}?["\']?brand["\']?\s*:\s*["\'`])[^"\'`]*?(["\'`])', r'\g<1>' + br + r'\g<2>', txt, flags=re.S)
            txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,500}?["\']?category["\']?\s*:\s*["\'`])[^"\'`]*?(["\'`])', r'\g<1>' + cat + r'\g<2>', txt, flags=re.S)
            txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,500}?["\']?(?:price|salePrice)["\']?\s*:\s*)\d+', r'\g<1>' + str(pr), txt, flags=re.S)
            txt = re.sub(r'(\b' + re.escape(s) + r'\b.{0,500}?["\']?(?:mrp|originalPrice)["\']?\s*:\s*)\d+', r'\g<1>' + str(mr), txt, flags=re.S)

        txt = txt.replace('100% Whole Wheat Grain (Chakki-ground MP Sharbati/ whole wheat flour,zero maida added).', 'Sealed factory-packed grocery item. Check pack label for ingredients.')
        txt = txt.replace('CONTAINS WHEAT (GLUTEN).', 'See physical packaging for allergen details.')
        if txt != orig:
            open(fp, 'w', encoding='utf-8').write(txt)
            print("PATCHED SOURCE FILE:", fp)

# 3. Sync Firebase RTDB
try:
    req = urllib.request.Request(
        "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json",
        data=json.dumps(items, ensure_ascii=False).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='PUT'
    )
    urllib.request.urlopen(req, timeout=20)
except Exception as e:
    print("Firebase:", e)
