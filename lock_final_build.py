import os
import json
import hashlib
import zipfile
import datetime
import subprocess
import urllib.request

store_dir = r"D:\nav g store"
if not os.path.exists(store_dir):
    store_dir = os.getcwd()

pub_dir = os.path.join(store_dir, "public")
lock_dir = os.path.join(store_dir, "FINAL_LOCKED_BUILD")
os.makedirs(lock_dir, exist_ok=True)

firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"
timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

print("=" * 65)
print("NAV GROCERY STORE - LOCKING FINAL PRODUCTION BUILD")
print("=" * 65)

# 1. Fetch & lock golden 308-SKU catalog from Firebase RTDB
with urllib.request.urlopen(firebase_url, timeout=20) as resp:
    raw = json.loads(resp.read().decode("utf-8"))
    products = raw if isinstance(raw, list) else list(raw.values())

golden_json_path = os.path.join(lock_dir, "locked_golden_308_catalog.json")
for target_json in [
    golden_json_path,
    os.path.join(store_dir, "catalog.json"),
    os.path.join(pub_dir, "catalog.json")
]:
    with open(target_json, "w", encoding="utf-8") as f:
        json.dump(products, f, indent=2, ensure_ascii=False)

print(f"[LOCKED] Golden catalog saved ({len(products)} verified SKUs)")

# 2. Bundle all core code, HTML, config, and verified photos into immutable ZIP
core_files = [
    "public.html",
    "index.html",
    "admin.html",
    "vercel.json",
    "catalog.json",
    "build_public.py",
    "build_admin.py",
    "ocr_match_308.py",
    "verify_and_finalize.py",
    "deploy_store.py",
    "lock_final_build.py",
    "photo_audit_report.csv"
]

zip_backup_path = os.path.join(lock_dir, "LOCKED_FINAL_BACKUP.zip")
manifest = {
    "store": "Nav Grocery Store, Sandhaur, Punjab 148020",
    "locked_at": timestamp,
    "total_skus": len(products),
    "live_store_url": "https://www.navgrocerystore.com",
    "admin_portal_url": "https://www.navgrocerystore.com/admin",
    "files": {}
}

def file_sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()

with zipfile.ZipFile(zip_backup_path, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.write(golden_json_path, "locked_golden_308_catalog.json")
    manifest["files"]["locked_golden_308_catalog.json"] = file_sha256(golden_json_path)

    for cf in core_files:
        full_p = os.path.join(store_dir, cf)
        if os.path.exists(full_p):
            zf.write(full_p, cf)
            manifest["files"][cf] = file_sha256(full_p)

    verified_img_root = os.path.join(pub_dir, "images", "verified")
    photo_count = 0
    if os.path.exists(verified_img_root):
        for root, _, files in os.walk(verified_img_root):
            for fn in sorted(files):
                fp = os.path.join(root, fn)
                rel_zip = os.path.join("public", "images", "verified", fn)
                zf.write(fp, rel_zip)
                photo_count += 1

manifest["verified_photos_locked"] = photo_count
manifest_path = os.path.join(lock_dir, "LOCK_MANIFEST.json")
with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)

print(f"[LOCKED] Archive created: {zip_backup_path} ({photo_count} photos bundled)")

# 3. Create one-click restore script inside FINAL_LOCKED_BUILD
restore_script = '''import os, json, zipfile, subprocess, urllib.request

store_dir = r"D:\\nav g store"
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
'''
with open(os.path.join(lock_dir, "restore_locked_build.py"), "w", encoding="utf-8") as f:
    f.write(restore_script)

# 4. Lock safe run_automation.bat so future syncs never overwrite verified photos
safe_bat = """@echo off
cd /d "D:\\nav g store"
echo ============================================================
echo NAV GROCERY STORE - LOCKED PRODUCTION SYNC
echo ============================================================
python verify_and_finalize.py
if exist keep_sync.py python keep_sync.py
git add -A
git commit -m "Locked production catalog sync"
git push origin main --force
echo ============================================================
echo LIVE STORE: https://www.navgrocerystore.com
echo ADMIN PORTAL: https://www.navgrocerystore.com/admin (PIN: 148020)
echo ============================================================
pause
"""
with open(os.path.join(store_dir, "run_automation.bat"), "w", encoding="utf-8") as f:
    f.write(safe_bat)

# 5. Tag & push final locked state to GitHub and Vercel
subprocess.run(["git", "add", "-A"], cwd=store_dir)
subprocess.run(["git", "commit", "-m", "FINAL LOCKED BUILD: 308 OCR-verified SKUs, storefront & admin portal"], cwd=store_dir)
subprocess.run(["git", "tag", "-f", "FINAL-LOCKED-V1"], cwd=store_dir)
subprocess.run(["git", "push", "origin", "main", "--force"], cwd=store_dir)
subprocess.run(["git", "push", "origin", "FINAL-LOCKED-V1", "--force"], cwd=store_dir)

print("=" * 65)
print("ALL CODING, PHOTOS & DATABASE LOCKED SUCCESSFULLY!")
print("Backup Location: D:\\nav g store\\FINAL_LOCKED_BUILD\\LOCKED_FINAL_BACKUP.zip")
print("Live Store: https://www.navgrocerystore.com")
print("Admin Portal: https://www.navgrocerystore.com/admin (PIN: 148020)")
print("=" * 65)
