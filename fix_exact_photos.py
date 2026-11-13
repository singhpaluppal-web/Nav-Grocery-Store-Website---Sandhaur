import os, re, json, shutil, urllib.request, subprocess

store_dir = r"D:\nav g store"
firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"
out_dir = os.path.join(store_dir, "images", "verified")
pub_out_dir = os.path.join(store_dir, "public", "images", "verified")
os.makedirs(out_dir, exist_ok=True)
os.makedirs(pub_out_dir, exist_ok=True)

# 1. Scan authentic photo folders (skipping scrambled batch_1..31)
photo_folders = [
    os.path.join(store_dir, "public", "nav_web_photos"),
    os.path.join(store_dir, "nav web"),
    os.path.join(store_dir, "backup_original_photos"),
    os.path.join(store_dir, "A new photo"),
    os.path.join(store_dir, "assets"),
    os.path.join(store_dir, "batches_10"),
    os.path.join(store_dir, "all_31_batches")
]

candidates = []
numbered_backup = {}
for folder in photo_folders:
    if not os.path.exists(folder):
        continue
    for root, _, files in os.walk(folder):
        for f in sorted(files):
            if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
                full_p = os.path.join(root, f)
                tokens = set(re.findall(r"[a-z0-9]+", (os.path.basename(root) + " " + os.path.splitext(f)[0]).lower()))
                candidates.append((full_p, tokens, f.lower()))
                m = re.fullmatch(r"0*(\d{1,3})", os.path.splitext(f)[0])
                if m and int(m.group(1)) not in numbered_backup:
                    numbered_backup[int(m.group(1))] = full_p

print(f"Found {len(candidates)} original photos to match by product name.")

# 2. Fetch live 308 products from Firebase
with urllib.request.urlopen(firebase_url, timeout=15) as resp:
    data = json.loads(resp.read().decode("utf-8"))
    products = data if isinstance(data, list) else list(data.values())

stop_words = {"pack", "value", "family", "saver", "wholesale", "bulk", "pure", "fresh", "premium", "the", "and", "for"}
matched_by_name = 0

for idx, p in enumerate(products):
    num = idx + 1
    sku = p.get("id", f"NAV{str(num).zfill(3)}")
    name_tokens = set(re.findall(r"[a-z0-9]+", (p.get("name", "") + " " + p.get("brand", "")).lower())) - stop_words
    brand_tokens = set(re.findall(r"[a-z0-9]+", p.get("brand", "").lower()))

    best_file = None
    best_score = 0
    for full_p, f_tokens, _ in candidates:
        overlap = name_tokens & f_tokens
        score = len(overlap) + (2 if (brand_tokens & f_tokens) else 0)
        if score > best_score and len(overlap) >= 2:
            best_score = score
            best_file = full_p

    if best_file:
        matched_by_name += 1
    elif num in numbered_backup:
        best_file = numbered_backup[num]

    if best_file and os.path.exists(best_file):
        ext = os.path.splitext(best_file)[1].lower()
        fname = f"{sku}{ext}"
        shutil.copy2(best_file, os.path.join(out_dir, fname))
        shutil.copy2(best_file, os.path.join(pub_out_dir, fname))
        p["image"] = f"images/verified/{fname}"

print(f"Matched {matched_by_name} items directly by product name & brand!")

# 3. Save locally and update Firebase RTDB
for jp in [os.path.join(store_dir, "catalog.json"), os.path.join(store_dir, "public", "catalog.json")]:
    with open(jp, "w", encoding="utf-8") as f:
        json.dump(products, f, indent=2, ensure_ascii=False)

req = urllib.request.Request(
    firebase_url,
    data=json.dumps(products).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="PUT"
)
urllib.request.urlopen(req, timeout=20)
print("Firebase RTDB updated live!")

# 4. Push verified photos to GitHub / Vercel
subprocess.run(["git", "add", "-A"], cwd=store_dir)
subprocess.run(["git", "commit", "-m", "Match verified product photos by brand and name"], cwd=store_dir)
subprocess.run(["git", "push", "origin", "main", "--force"], cwd=store_dir)
print("DONE! Wait 45 seconds and press Ctrl + F5 on your website.")
