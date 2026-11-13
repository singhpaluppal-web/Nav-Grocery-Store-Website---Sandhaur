@echo off
cd /d "D:\nav g store"
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
