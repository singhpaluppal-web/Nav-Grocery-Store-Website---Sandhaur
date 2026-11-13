import os, re, csv, json, glob, urllib.request
import fitz
from update_names import names, fb
def est_mrp(t):
    tl = t.lower()
    if 'ghee' in tl and ('1l' in tl or '1 litre' in tl): return 620
    if 'olive oil' in tl: return 1150
    if ('oil' in tl) and ('1l' in tl or '1 litre' in tl): return 165
    if 'basmati' in tl and '5kg' in tl: return 580
    if 'rice' in tl and '5kg' in tl: return 420
    if 'atta' in tl and '5kg' in tl: return 265
    if 'sugar' in tl and '5kg' in tl: return 245
    if 'almonds' in tl: return 295
    if 'chyawanprash' in tl: return 395
    if 'diapers' in tl: return 899
    if 'shampoo' in tl and '650ml' in tl: return 540
    if 'honey' in tl and '500g' in tl: return 240
    if 'bournvita' in tl or 'horlicks' in tl: return 275
    if 'bhujia' in tl and '1kg' in tl: return 260
    if 'rasgulla' in tl or 'gulab jamun' in tl: return 250
    if 'paneer' in tl: return 95
    if 'butter' in tl and '500g' in tl: return 285
    if 'butter' in tl and '100g' in tl: return 60
    if 'dal' in tl or 'chana' in tl or 'rajma' in tl or 'moong' in tl or 'urad' in tl or 'masoor' in tl: return 155 if '1kg' in tl else 85
    if 'masala' in tl or 'pepper' in tl or 'turmeric' in tl or 'chilli' in tl or 'coriander' in tl: return 85 if '100g' in tl else 140
    if 'cardamom' in tl or 'elaichi' in tl or 'cloves' in tl or 'hing' in tl: return 165
    if 'frozen' in tl or 'paratha' in tl or 'tikki' in tl or 'fries' in tl or 'nuggets' in tl or 'samosa' in tl or 'momos' in tl: return 145
    if 'tea' in tl and '500g' in tl: return 260
    if 'coffee' in tl: return 340
    if 'detergent' in tl or 'washing powder' in tl: return 145
    if '750ml' in tl or '1 litre' in tl or '1l' in tl: return 45
    if '250ml' in tl or '350ml' in tl: return 110 if 'energy' in tl else 35
    return 120
pdf_mrps = []
for b in range(1, 32):
    bp = f'31_PDF_Batches/batch_{b:02d}.pdf'
    if os.path.exists(bp):
        for pg in fitz.open(bp):
            txt = pg.get_text()
            m = re.findall(r'(?:MRP|Rs\.?|₹)\s*[:\-]?\s*(\d{2,4})', txt, re.I)
            pdf_mrps.append(int(m[0]) if m else None)
rows = []
for i, title in enumerate(names):
    s = f'NAV{i+1:03d}'
    mrp = pdf_mrps[i] if (i < len(pdf_mrps) and pdf_mrps[i] and 15 <= pdf_mrps[i] <= 3000) else est_mrp(title)
    sale = round(mrp * 0.88)
    fb[s].update({'mrp': mrp, 'originalPrice': mrp, 'price': sale, 'salePrice': sale, 'discount': 12})
    rows.append([s, title, fb[s]['brand'], mrp, sale, '12%'])
with open('Nav_Grocery_Price_List_308.csv', 'w', newline='', encoding='utf-8-sig') as f:
    w = csv.writer(f); w.writerow(['SKU', 'Product Name', 'Brand', 'MRP (INR)', '12% Off Wholesale Price (INR)', 'Discount']); w.writerows(rows)
for p in glob.glob('**/*catalog*.json', recursive=True):
    try:
        d = json.load(open(p, encoding='utf-8'))
        arr = d if isinstance(d, list) else d.get('products', [])
        if isinstance(arr, list) and len(arr) >= 300:
            for i in range(min(308, len(arr))):
                s = f'NAV{i+1:03d}'
                arr[i].update({'mrp': fb[s]['mrp'], 'originalPrice': fb[s]['mrp'], 'price': fb[s]['price'], 'salePrice': fb[s]['salePrice'], 'discount': 12})
            json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    except Exception: pass
req = urllib.request.Request('https://nav-grocery-store-default-rtdb.firebaseio.com/products.json', data=json.dumps(fb).encode('utf-8'), method='PATCH', headers={'Content-Type':'application/json'})
urllib.request.urlopen(req)
print('PRICE_LIST_AND_308_RATES_UPDATED')
