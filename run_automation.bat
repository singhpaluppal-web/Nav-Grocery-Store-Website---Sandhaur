@echo off
cd /d "D:\nav g store"
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
