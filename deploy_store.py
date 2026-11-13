import os
import json
import subprocess

store_dir = r"D:\nav g store"
if not os.path.exists(store_dir):
    store_dir = os.getcwd()

print("=" * 60)
print("1. CREATING VERCEL.JSON WITH CLEAN URLS & NO-CACHE HEADERS...")

vercel_config = {
    "cleanUrls": True,
    "rewrites": [
        {"source": "/", "destination": "/public.html"},
        {"source": "/admin", "destination": "/admin.html"},
        {"source": "/pos-billing/(.*)", "destination": "/public.html"}
    ],
    "headers": [
        {
            "source": "/(.*)",
            "headers": [
                {"key": "Cache-Control", "value": "no-cache, no-store, must-revalidate"},
                {"key": "Pragma", "value": "no-cache"},
                {"key": "Expires", "value": "0"},
                {"key": "X-Content-Type-Options", "value": "nosniff"},
                {"key": "X-Frame-Options", "value": "SAMEORIGIN"}
            ]
        }
    ]
}

vercel_path = os.path.join(store_dir, "vercel.json")
with open(vercel_path, "w", encoding="utf-8") as f:
    json.dump(vercel_config, f, indent=2)
print("Created:", vercel_path)

print("=" * 60)
print("2. CREATING ONE-CLICK RUN_AUTOMATION.BAT...")

bat_content = """@echo off
cd /d "D:\\nav g store"
echo ============================================================
echo NAV GROCERY STORE - AUTOMATED SYNC AND LIVE DEPLOYMENT
echo ============================================================
python sync_308_catalog.py
git add -A
git commit -m "Automated catalog and photo sync update"
git push origin main --force
echo ============================================================
echo DEPLOYMENT COMPLETE!
echo Live Store: https://www.navgrocerystore.com
echo Admin Portal: https://www.navgrocerystore.com/admin (PIN: 148020)
echo ============================================================
pause
"""

bat_path = os.path.join(store_dir, "run_automation.bat")
with open(bat_path, "w", encoding="utf-8") as f:
    f.write(bat_content)
print("Created:", bat_path)

print("=" * 60)
print("3. DEPLOYING COMPLETE WEBSITE TO GITHUB AND VERCEL...")

repo_url = "https://github.com/singhpaluppal-web/Nav-Grocery-Store-Website---Sandhaur.git"

try:
    if not os.path.exists(os.path.join(store_dir, ".git")):
        subprocess.run(["git", "init"], cwd=store_dir, check=True)
        subprocess.run(["git", "branch", "-M", "main"], cwd=store_dir, check=True)
        subprocess.run(["git", "remote", "add", "origin", repo_url], cwd=store_dir, check=True)

    subprocess.run(["git", "add", "-A"], cwd=store_dir, check=True)
    subprocess.run(["git", "commit", "-m", "Deploy complete Nav Grocery Store 308 SKU storefront and admin portal"], cwd=store_dir)
    subprocess.run(["git", "branch", "-M", "main"], cwd=store_dir)
    subprocess.run(["git", "push", "-u", "origin", "main", "--force"], cwd=store_dir, check=True)
    print("=" * 60)
    print("SUCCESS: ALL FILES PUSHED LIVE TO GITHUB AND VERCEL!")
    print("Wait 45 seconds for Vercel build to finish, then open:")
    print("Public Store: https://www.navgrocerystore.com")
    print("Admin Portal: https://www.navgrocerystore.com/admin (PIN: 148020)")
except Exception as e:
    print("Git deployment notice:", e)
    print("Run these commands manually in CMD if needed:")
    print("git add -A")
    print('git commit -m "Deploy Nav Grocery Store"')
    print("git push origin main --force")
