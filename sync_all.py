import json, os, shutil, urllib.request

src = 'public/catalog.json' if os.path.exists('public/catalog.json') else 'FINAL_LOCKED_BUILD/locked_golden_308_catalog.json'
items = json.load(open(src, encoding='utf-8'))
if isinstance(items, dict):
    items = items.get('products', [])

# 1. Overwrite public/data/products_308.json (the exact file public.html loads!)
os.makedirs('public/data', exist_ok=True)
with open('public/data/products_308.json', 'w', encoding='utf-8') as f:
    json.dump(items, f, indent=2, ensure_ascii=False)

# 2. Also overwrite root catalog.json if present
if os.path.exists('catalog.json'):
    shutil.copyfile(src, 'catalog.json')

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

print("SYNCED_ALL_308_TO_PRODUCTS_308_JSON")
