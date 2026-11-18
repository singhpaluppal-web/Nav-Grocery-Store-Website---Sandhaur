import os, json, zipfile, subprocess, urllib.request

store_dir = r"D:\nav g store"
lock_dir = os.path.join(store_dir, "FINAL_LOCKED_BUILD")
zip_path = os.path.join(lock_dir, "LOCKED_FINAL_BACKUP.zip")
golden_path = os.path.join(lock_dir, "locked_golden_308_catalog.json")
firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"

with zipfile.ZipFile(zip_path, "r") as zf:
    zf.extractall(store_dir)

with open(golden_path, "r", encoding="utf-8") as f:
    products = json.load(f)

req = urllib.request.Request(
    firebase_url,
    data=json.dumps(products).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="PUT"
)
urllib.request.urlopen(req, timeout=25)

subprocess.run(["git", "add", "-A"], cwd=store_dir)
subprocess.run(["git", "commit", "-m", "Restore locked golden 308-SKU build"], cwd=store_dir)
subprocess.run(["git", "push", "origin", "main", "--force"], cwd=store_dir)
print("RESTORED & LIVE! Press Ctrl + F5 on www.navgrocerystore.com")
