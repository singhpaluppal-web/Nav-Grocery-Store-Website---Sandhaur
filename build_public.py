#!/usr/bin/env python3
"""
Nav Grocery Store — Pure Public Storefront Builder & Verification Script
Verifies standalone public/public.html, enforces Strict Public Privacy Shield
and zero NavKart/Admin leakage, and mirrors it to repo root public.html.
"""

from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parent
PUBLIC_DIR = ROOT / "public"


def sync_storefront_files() -> None:
    src = PUBLIC_DIR / "public.html"
    if not src.exists():
        raise FileNotFoundError(f"Missing {src}. Ensure public/public.html exists.")

    content = src.read_text(encoding="utf-8")
    lower = content.lower()

    # Core required storefront assertions
    assert "nav-grocery-store-default-rtdb.firebaseio.com" in content, "Missing Firebase RTDB URL"
    assert "orders@navgrocerystore.com" in content, "Missing official public email"
    assert "8847370905" in content and "9915752905" in content, "Missing official helplines"

    # Strict Public Privacy Shield & Separation assertions
    assert "singhpaluppal" not in lower, "Privacy violation: personal email detected in public.html"
    assert "navkart" not in lower, "Separation violation: NavKart reference detected in public.html"
    assert "admin.html" not in lower, "Separation violation: Admin link detected in public.html"

    # Ensure GSTIN appears ONLY in the receipt generator / print receipt, never in header/footer
    gstin_matches = [m.start() for m in re.finditer(r"03AHPPU6185N1ZF", content)]
    assert len(gstin_matches) == 1, f"GSTIN must appear strictly once (receipt only); found {len(gstin_matches)}"

    root_copy = ROOT / "public.html"
    shutil.copy2(src, root_copy)
    print(f"[OK] Verified & synced {src} -> {root_copy} ({len(content):,} bytes)")


if __name__ == "__main__":
    sync_storefront_files()
