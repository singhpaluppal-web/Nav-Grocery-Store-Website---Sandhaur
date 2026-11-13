#!/usr/bin/env python3
"""
Nav Grocery Store — Image Optimization & Compression Utility
Backs up product images in public/images/verified/ and public/images/batch_*
and compresses every photo to max 400x400px (< 35 KB) while keeping exact filenames.

Usage:
  pip install Pillow
  python optimize_images.py
"""

import os
import shutil
from pathlib import Path

try:
    from PIL import Image, ImageOps
except ImportError:
    raise SystemExit("Pillow is required. Install it with: pip install Pillow")

ROOT_DIR = Path(__file__).resolve().parent
PUBLIC_IMAGES = ROOT_DIR / "public" / "images"
BACKUP_DIR = ROOT_DIR / "public" / "images_backup_originals"

MAX_DIMENSION = 400
TARGET_MAX_BYTES = 35 * 1024
SUPPORTED_EXTS = {".png", ".jpg", ".jpeg", ".webp"}


def is_product_image(path: Path) -> bool:
    if path.suffix.lower() not in SUPPORTED_EXTS:
        return False
    rel = path.relative_to(PUBLIC_IMAGES).as_posix()
    # Target verified/, batch_*, or product thumbnails; skip store hero photos & logo
    if rel.startswith("store/") or rel == "logo.png":
        return False
    return True


def backup_file(src: Path) -> None:
    rel = src.relative_to(PUBLIC_IMAGES)
    dst = BACKUP_DIR / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    if not dst.exists():
        shutil.copy2(src, dst)


def compress_image_in_place(path: Path) -> tuple[int, int]:
    before_bytes = path.stat().st_size
    backup_file(path)

    with Image.open(path) as im:
        im = ImageOps.exif_transpose(im)
        im.thumbnail((MAX_DIMENSION, MAX_DIMENSION), Image.Resampling.LANCZOS)

        ext = path.suffix.lower()
        if ext == ".png":
            # Quantize RGBA/RGB PNGs cleanly to keep exact .png filename under 35 KB
            if im.mode not in ("RGB", "RGBA"):
                im = im.convert("RGBA")
            for colors in (256, 128, 64):
                quantized = im.quantize(colors=colors, method=Image.Quantize.FASTOCTREE)
                quantized.save(path, format="PNG", optimize=True)
                if path.stat().st_size <= TARGET_MAX_BYTES:
                    break
        elif ext in (".jpg", ".jpeg"):
            if im.mode != "RGB":
                im = im.convert("RGB")
            for quality in (82, 72, 62, 50):
                im.save(path, format="JPEG", quality=quality, optimize=True, progressive=True)
                if path.stat().st_size <= TARGET_MAX_BYTES:
                    break
        elif ext == ".webp":
            for quality in (80, 70, 58):
                im.save(path, format="WEBP", quality=quality, method=6)
                if path.stat().st_size <= TARGET_MAX_BYTES:
                    break

    after_bytes = path.stat().st_size
    return before_bytes, after_bytes


def main() -> None:
    if not PUBLIC_IMAGES.exists():
        print(f"Directory not found: {PUBLIC_IMAGES}")
        return

    files = [p for p in PUBLIC_IMAGES.rglob("*") if p.is_file() and is_product_image(p)]
    if not files:
        print("No product images found under public/images/verified/ or public/images/batch_*/.")
        return

    total_before = 0
    total_after = 0
    processed = 0

    for img_path in sorted(files):
        try:
            b, a = compress_image_in_place(img_path)
            total_before += b
            total_after += a
            processed += 1
            print(f"[OK] {img_path.relative_to(PUBLIC_IMAGES)} : {b/1024:.1f} KB -> {a/1024:.1f} KB")
        except Exception as exc:
            print(f"[ERR] {img_path}: {exc}")

    print("\n--- Summary ---")
    print(f"Processed images : {processed}")
    print(f"Backup location  : {BACKUP_DIR}")
    print(f"Total before     : {total_before / (1024 * 1024):.2f} MB")
    print(f"Total after      : {total_after / (1024 * 1024):.2f} MB")


if __name__ == "__main__":
    main()
