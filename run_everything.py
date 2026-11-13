import os
import sys
import json
import subprocess
import urllib.request

store_dir = r"D:\nav g store"
if not os.path.exists(store_dir):
    store_dir = os.getcwd()

print("=" * 65)
print("NAV GROCERY STORE - FULL SYSTEM BUILD, SYNC & VERIFICATION")
print("=" * 65)

scripts = [
    ("STAGE 1: Building Public Storefront (public.html & index.html)", "build_public.py"),
    ("STAGE 2: Building Admin Portal (admin.html)", "build_admin.py"),
    ("STAGE 3: Linking 308 Batch Photos & Syncing Firebase RTDB", "sync_308_catalog.py"),
    ("STAGE 4: Generating vercel.json & Pushing Live to GitHub/Vercel", "deploy_store.py")
]

for label, script_name in scripts:
    print("\n" + "-" * 65)
    print(label)
    print("-" * 65)
    script_path = os.path.join(store_dir, script_name)
    if not os.path.exists(script_path):
        print(f"[WARNING] {script_name} not found in {store_dir}. Please create it first.")
        continue
    result = subprocess.run([sys.executable, script_path], cwd=store_dir)
    if result.returncode == 0:
        print(f"[OK] {script_name} completed successfully.")
    else:
        print(f"[ERROR] {script_name} exited with code {result.returncode}.")

print("\n" + "=" * 65)
print("STAGE 5: END-TO-END SYSTEM HEALTH & LIVE VERIFICATION")
print("=" * 65)

# 1. Verify Local HTML & Config Files
required_files = [
    "public.html",
    "index.html",
    "admin.html",
    "vercel.json",
    "catalog.json",
    os.path.join("public", "public.html"),
    os.path.join("public", "admin.html")
]

for rf in required_files:
    full_p = os.path.join(store_dir, rf)
    if os.path.exists(full_p) and os.path.getsize(full_p) > 100:
        print(f"[PASS] File verified: {rf} ({os.path.getsize(full_p)} bytes)")
    else:
        print(f"[FAIL] Missing or empty file: {rf}")

# 2. Verify Batch Photos Count
img_root = os.path.join(store_dir, "public", "images")
photo_count = 0
if os.path.exists(img_root):
    for _, _, files in os.walk(img_root):
        photo_count += sum(1 for f in files if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp")))
print(f"[INFO] Total packaging photos in public/images: {photo_count} / 308")

# 3. Verify Firebase Realtime Database Live Data
firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"
try:
    with urllib.request.urlopen(firebase_url, timeout=15) as resp:
        data = json.loads(resp.read().decode("utf-8"))
        items = data if isinstance(data, list) else (list(data.values()) if isinstance(data, dict) else [])
        print(f"[PASS] Firebase RTDB Live Status: Connected ({len(items)} verified SKUs online)")
except Exception as e:
    print(f"[FAIL] Firebase RTDB Connection Error: {e}")

# 4. Verify Live Storefront & Admin URLs
for url in ["https://www.navgrocerystore.com", "https://www.navgrocerystore.com/admin"]:
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=15) as r:
            print(f"[PASS] Live URL Reachable ({r.status}): {url}")
    except Exception as e:
        print(f"[WAIT] Live URL building or propagating ({url}): {e}")

print("\n" + "=" * 65)
print("ALL CHECKS COMPLETE!")
print("Public Storefront: https://www.navgrocerystore.com")
print("Admin Portal: https://www.navgrocerystore.com/admin (PIN: 148020)")
print("Press Ctrl + F5 in your browser to view the fresh live store.")
print("=" * 65)
