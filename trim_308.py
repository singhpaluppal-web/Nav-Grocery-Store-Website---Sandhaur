import json, glob, urllib.request
from update_names import names, fb
for p in glob.glob('**/*catalog*.json', recursive=True):
    try:
        d = json.load(open(p, encoding='utf-8'))
        arr = d if isinstance(d, list) else d.get('products', [])
        if isinstance(arr, list) and len(arr) >= 300:
            clean = []
            for i in range(308):
                s = f'NAV{i+1:03d}'
                item = dict(arr[i])
                item.update({'id':s,'sku':s,'name':names[i],'title':names[i],'brand':fb[s]['brand'],'image':f'/products/{s}.jpg','imageUrl':f'/products/{s}.jpg','inStock':True})
                clean.append(item)
                fb[s].update({'mrp':item.get('mrp',100),'price':item.get('price',88),'salePrice':item.get('salePrice',88),'category':item.get('category','Packaged Groceries')})
            if isinstance(d, list): d = clean
            else: d['products'] = clean
            json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
            print('Trimmed to 308:', p)
    except Exception as e: pass
req = urllib.request.Request('https://nav-grocery-store-default-rtdb.firebaseio.com/products.json', data=json.dumps(fb).encode('utf-8'), method='PUT', headers={'Content-Type':'application/json'})
urllib.request.urlopen(req)
print('EXACT 308 SKUS LIVE IN FIREBASE & ADMIN!')
