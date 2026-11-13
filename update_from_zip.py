import os
import re
import json
import zipfile
import urllib.request
import subprocess
import fitz

store_dir = r"D:\nav g store"
zip_path = os.path.join(store_dir, "nav_31_batches.zip")
extract_dir = os.path.join(store_dir, "extracted_31_pdfs")
out_img_dir = os.path.join(store_dir, "images", "verified")
pub_img_dir = os.path.join(store_dir, "public", "images", "verified")
firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"

os.makedirs(extract_dir, exist_ok=True)
os.makedirs(out_img_dir, exist_ok=True)
os.makedirs(pub_img_dir, exist_ok=True)

if not os.path.exists(zip_path):
    for f in os.listdir(store_dir):
        if f.lower().endswith(".zip"):
            zip_path = os.path.join(store_dir, f)
            break

print("Opening ZIP:", zip_path)
with zipfile.ZipFile(zip_path, "r") as zf:
    zf.extractall(extract_dir)

pdf_map = {}
for root, _, files in os.walk(extract_dir):
    for f in sorted(files):
        if f.lower().endswith(".pdf"):
            nums = re.findall(r"\d+", f)
            b_num = int(nums[-1]) if nums else len(pdf_map) + 100
            if b_num not in pdf_map:
                pdf_map[b_num] = os.path.join(root, f)

sorted_pdfs = [pdf_map[k] for k in sorted(pdf_map.keys())]
print(f"Found {len(sorted_pdfs)} PDF batch files inside ZIP.")

with urllib.request.urlopen(firebase_url, timeout=15) as resp:
    data = json.loads(resp.read().decode("utf-8"))
    products = data if isinstance(data, list) else list(data.values())

extracted_images = []
extracted_rows = []
junk_words = ("sandhaur", "helpline", "8847370905", "gstin", "invoice", "subtotal", "page", "nav grocery", "price list")

for pdf_path in sorted_pdfs:
    doc = fitz.open(pdf_path)
    for page in doc:
        lines = [ln.strip() for ln in page.get_text("text").splitlines() if ln.strip()]
        for ln in lines:
            low = ln.lower()
            if any(j in low for j in junk_words):
                continue
            nums = re.findall(r"\b\d{2,5}\b", ln)
            title = re.sub(r"\b\d+\b", "", ln).replace("₹", "").replace(":", "").strip(" -|,.")
            if len(title) >= 4 and nums:
                mrp_val = int(nums[-1])
                if 10 <= mrp_val <= 15000:
                    extracted_rows.append((title, mrp_val))

        for img_info in page.get_images(full=True):
            xref = img_info[0]
            base_img = doc.extract_image(xref)
            img_bytes = base_img["image"]
            ext = base_img["ext"]
            if len(img_bytes) > 3000:
                extracted_images.append((img_bytes, ext))
    doc.close()

print(f"Extracted {len(extracted_images)} photos and {len(extracted_rows)} product rows.")

for idx in range(min(308, len(products))):
    sku = products[idx].get("id", f"NAV{str(idx+1).zfill(3)}")
    if idx < len(extracted_rows):
        title, mrp_val = extracted_rows[idx]
        products[idx]["name"] = title
        products[idx]["brand"] = title.split()[0].upper()
        products[idx]["mrp"] = mrp_val
        products[idx]["sale"] = int(round(mrp_val * 0.88))
        products[idx]["price"] = products[idx]["sale"]
        products[idx]["cost"] = int(round(mrp_val * 0.75))
    if idx < len(extracted_images):
        img_bytes, ext = extracted_images[idx]
        fname = f"{sku}.{ext}"
        with open(os.path.join(out_img_dir, fname), "wb") as f_out:
            f_out.write(img_bytes)
        with open(os.path.join(pub_img_dir, fname), "wb") as f_pub:
            f_pub.write(img_bytes)
        products[idx]["image"] = f"images/verified/{fname}"

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
print("SUCCESS: Firebase RTDB updated live with all 31 PDF batches!")

subprocess.run(["git", "add", "-A"], cwd=store_dir)
subprocess.run(["git", "commit", "-m", "Update 308 products and photos from nav_31_batches.zip"], cwd=store_dir)
subprocess.run(["git", "push", "origin", "main", "--force"], cwd=store_dir)
print("LIVE DEPLOYMENT COMPLETE! Wait 45 seconds and press Ctrl + F5 on www.navgrocerystore.com")
