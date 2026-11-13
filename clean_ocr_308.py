import os
import re
import json
import subprocess
import urllib.request
import fitz
from rapidocr_onnxruntime import RapidOCR

store_dir = r"D:\nav g store"
pub_dir = os.path.join(store_dir, "public")
firebase_url = "https://nav-grocery-store-default-rtdb.firebaseio.com/products.json"

ocr = RapidOCR()

GROCERY_RULES = [
    (["chana", "masala"], "MDH Chana Masala 100g", "MDH", "Spices", 88),
    (["garam", "masala"], "Special Garam Masala 100g", "MDH", "Spices", 98),
    (["deggi", "mirch"], "MDH Deggi Mirch Powder 100g", "MDH", "Spices", 92),
    (["red", "chilli"], "Red Chilli Powder 200g", "CATCH", "Spices", 95),
    (["turmeric"], "Pure Turmeric Haldi Powder 200g", "CATCH", "Spices", 65),
    (["haldi"], "Pure Turmeric Haldi Powder 200g", "CATCH", "Spices", 65),
    (["coriander"], "Coriander Dhania Powder 200g", "CATCH", "Spices", 68),
    (["dhania"], "Coriander Dhania Powder 200g", "CATCH", "Spices", 68),
    (["chaat", "masala"], "Chunky Chaat Masala 100g", "MDH", "Spices", 76),
    (["chat", "masala"], "Chunky Chaat Masala 100g", "MDH", "Spices", 76),
    (["kitchen", "king"], "MDH Kitchen King Masala 100g", "MDH", "Spices", 90),
    (["shahi", "paneer"], "MDH Shahi Paneer Masala 100g", "MDH", "Spices", 88),
    (["pav", "bhaji"], "MDH Pav Bhaji Masala 100g", "MDH", "Spices", 84),
    (["sambhar"], "MDH Sambhar Masala 100g", "MDH", "Spices", 82),
    (["kasuri", "methi"], "Kasuri Methi Dried Leaves 50g", "MDH", "Spices", 45),
    (["kasoori", "methi"], "Kasuri Methi Dried Leaves 50g", "MDH", "Spices", 45),
    (["black", "pepper"], "Black Pepper Sprinkler 100g", "CATCH", "Spices", 115),
    (["black", "salt"], "Black Salt Kala Namak Sprinkler 200g", "CATCH", "Spices", 55),
    (["rock", "salt"], "Himalayan Pink Rock Salt 1kg", "TATA", "Spices", 99),
    (["iodized", "salt"], "Tata Iodized Salt 1kg", "TATA", "Spices", 28),
    (["salt"], "Iodized Table Salt 1kg", "TATA", "Spices", 28),
    (["ajwain"], "Carom Seeds Ajwain 100g", "CATCH", "Spices", 55),
    (["carom"], "Carom Seeds Ajwain 100g", "CATCH", "Spices", 55),
    (["jeera"], "Whole Cumin Seeds Jeera 200g", "PATANJALI", "Spices", 120),
    (["cumin"], "Whole Cumin Seeds Jeera 200g", "PATANJALI", "Spices", 120),
    (["mustard", "seed"], "Mustard Seeds Rai 200g", "PATANJALI", "Spices", 45),
    (["hing"], "Compounded Asafoetida Hing 50g", "LG", "Spices", 88),
    (["eyed", "peas"], "Premium Black Eyed Peas Lobia 500g", "TATA", "Atta & Dals", 95),
    (["lobia"], "Premium Black Eyed Peas Lobia 500g", "TATA", "Atta & Dals", 95),
    (["kabuli"], "Premium Kabuli Safed Chana 1kg", "TATA", "Atta & Dals", 195),
    (["kala", "chana"], "Natural Kala Chana 1kg", "TATA", "Atta & Dals", 130),
    (["urad", "dal"], "Premium Urad Dal 1kg", "TATA", "Atta & Dals", 185),
    (["toor", "dal"], "Tata Sampann Toor Arhar Dal 1kg", "TATA", "Atta & Dals", 185),
    (["arhar", "dal"], "Tata Sampann Toor Arhar Dal 1kg", "TATA", "Atta & Dals", 185),
    (["moong", "dal"], "Tata Sampann Moong Dal 1kg", "TATA", "Atta & Dals", 175),
    (["chana", "dal"], "Tata Sampann Chana Dal 1kg", "TATA", "Atta & Dals", 140),
    (["masoor"], "Red Masoor Malka Dal 1kg", "TATA", "Atta & Dals", 135),
    (["rajma"], "Kashmiri Red Rajma 1kg", "TATA", "Atta & Dals", 210),
    (["sharbati", "atta"], "Aashirvaad MP Sharbati Atta 10kg", "AASHIRVAAD", "Atta & Dals", 520),
    (["chakki", "atta"], "Whole Wheat Chakki Atta 10kg", "AASHIRVAAD", "Atta & Dals", 460),
    (["atta"], "Whole Wheat Chakki Atta 5kg", "AASHIRVAAD", "Atta & Dals", 245),
    (["basmati", "rice"], "Royal Basmati Rice 5kg", "INDIA GATE", "Atta & Dals", 495),
    (["rice"], "Premium Basmati Rice 5kg", "DAAWAT", "Atta & Dals", 450),
    (["besan"], "Pure Gram Flour Besan 1kg", "RAJDHANI", "Atta & Dals", 125),
    (["sooji"], "Coarse Semolina Sooji Rava 500g", "RAJDHANI", "Atta & Dals", 45),
    (["semolina"], "Coarse Semolina Sooji Rava 500g", "RAJDHANI", "Atta & Dals", 45),
    (["maida"], "Refined Wheat Flour Maida 500g", "RAJDHANI", "Atta & Dals", 40),
    (["oats"], "Rolled White Oats 1kg", "QUAKER", "Atta & Dals", 199),
    (["corn", "flakes"], "Golden Corn Flakes 875g", "KELLOGGS", "Atta & Dals", 340),
    (["pasta"], "Durum Wheat Penne Pasta 500g", "DEL MONTE", "Grocery & Staples", 135),
    (["noodle"], "Instant Masala Noodles Multipack", "MAGGI", "Grocery & Staples", 140),
    (["maggi"], "Maggi 2-Minute Masala Noodles 840g", "MAGGI", "Grocery & Staples", 168),
    (["soup"], "Chinese Hot & Sour Soup Pack 70g", "KNORR", "Grocery & Staples", 65),
    (["desi", "ghee"], "Pure Cow Desi Ghee 1L", "VERKA", "Desi Ghee & Oils", 640),
    (["ghee"], "Pure Milk Desi Ghee 1L", "AMUL", "Desi Ghee & Oils", 630),
    (["mustard", "oil"], "Kachi Ghani Mustard Oil 1L", "FORTUNE", "Desi Ghee & Oils", 165),
    (["sunflower", "oil"], "Refined Sunflower Oil 1L", "FORTUNE", "Desi Ghee & Oils", 155),
    (["soyabean", "oil"], "Refined Soyabean Oil 1L", "FORTUNE", "Desi Ghee & Oils", 145),
    (["oil"], "Refined Cooking Oil 1L", "FORTUNE", "Desi Ghee & Oils", 155),
    (["green", "peas"], "Safal Frozen Green Peas 1kg", "SAFAL", "Frozen Foods", 160),
    (["sweet", "corn"], "Safal Frozen Sweet Corn 500g", "SAFAL", "Frozen Foods", 85),
    (["french", "fries"], "McCain Crispy French Fries 420g", "MCCAIN", "Frozen Foods", 125),
    (["aloo", "tikki"], "McCain Aloo Tikki 400g", "MCCAIN", "Frozen Foods", 130),
    (["paneer"], "Malai Frozen Paneer Block 500g", "AMUL", "Frozen Foods", 210),
    (["ice", "cream"], "Chocolate Real Cocoa Ice Cream Tub 750ml", "AMUL", "Frozen Foods", 195),
    (["chocolate"], "Chocolate Cocoa Pack 375g", "CADBURY", "Grocery & Staples", 180),
    (["lemon"], "Cloudy Lemon Refreshing Drink 750ml", "SPRITE", "Grocery & Staples", 45),
    (["cola"], "Original Cola Soft Drink 750ml", "COCA COLA", "Grocery & Staples", 45),
    (["juice"], "Mixed Fruit Juice Pack 1L", "REAL", "Grocery & Staples", 120),
    (["sharbat"], "Herbal Rose Sharbat 750ml", "HAMDARD", "Grocery & Staples", 185),
    (["tea"], "Premium Strong Leaf Tea 500g", "TATA", "Grocery & Staples", 295),
    (["coffee"], "Classic Instant Coffee Jar 100g", "NESCAFE", "Grocery & Staples", 360),
    (["biscuit"], "Butter Cream Biscuits Family Pack", "BRITANNIA", "Grocery & Staples", 120),
    (["cookie"], "Cashew Butter Cookies Pack", "BRITANNIA", "Grocery & Staples", 140),
    (["namkeen"], "Aloo Bhujia Namkeen Pack 1kg", "HALDIRAMS", "Grocery & Staples", 260),
    (["bhujia"], "Aloo Bhujia Namkeen Pack 1kg", "HALDIRAMS", "Grocery & Staples", 260),
    (["chips"], "Classic Salted Potato Chips Pack", "LAYS", "Grocery & Staples", 50),
    (["ketchup"], "Fresh Tomato Ketchup Bottle 1kg", "KISSAN", "Grocery & Staples", 145),
    (["jam"], "Mixed Fruit Jam Jar 500g", "KISSAN", "Grocery & Staples", 170),
    (["honey"], "100% Pure Honey Bottle 500g", "DABUR", "Grocery & Staples", 235),
    (["sugar"], "Sulphur Free Sparkling Sugar 5kg", "MADHUR", "Grocery & Staples", 260),
    (["detergent"], "Matic Wash Detergent Powder 4kg", "SURF EXCEL", "Grocery & Staples", 580),
    (["surf"], "Surf Excel Easy Wash Powder 5kg", "SURF EXCEL", "Grocery & Staples", 680),
    (["dishwash"], "Lemon Dishwash Liquid Gel 2L", "VIM", "Grocery & Staples", 399),
    (["cleaner"], "Disinfectant Surface Cleaner 2L", "LIZOL", "Grocery & Staples", 395),
    (["soap"], "Germ Protection Bathing Soap Multipack", "DETTOL", "Grocery & Staples", 220),
    (["shampoo"], "Smooth & Silky Shampoo Bottle 650ml", "DOVE", "Grocery & Staples", 540),
    (["toothpaste"], "Strong Teeth Dental Toothpaste 500g", "COLGATE", "Grocery & Staples", 250),
]

def smart_clean_name(ocr_text, idx):
    low = ocr_text.lower()
    for keys, title, brand, cat, mrp in GROCERY_RULES:
        if all(k in low for k in keys):
            return title, brand, cat, mrp

    # Fallback: keep only real English words >= 3 chars without OCR gibberish
    junk = {
        "premium", "quality", "pure", "natural", "original", "taste", "fresh",
        "nutrition", "facts", "calories", "calorles", "tear", "seal", "net",
        "weight", "ingredients", "best", "before", "pack", "india", "fssai",
        "maharajas", "maharaja", "spiceco", "coinstgirotos", "bmenstennnte",
        "metiftenfacts", "ruerscaurlerrrsl", "sagusk", "pastanatura", "latradizione"
    }
    words = [w.capitalize() for w in re.findall(r"[A-Za-z]{3,}", ocr_text) if w.lower() not in junk]
    wt_match = re.search(r"\b(\d+\s*(?:kg|g|gm|ml|l))\b", low)
    wt = f" {wt_match.group(1)}" if wt_match else ""

    if words:
        clean_title = " ".join(words[:4]) + wt
        return clean_title, words[0].upper(), "Grocery & Staples", 120
    return f"Packaged Grocery Item {idx}", "NAV STORE", "Grocery & Staples", 100

with urllib.request.urlopen(firebase_url, timeout=15) as resp:
    data = json.loads(resp.read().decode("utf-8"))
    products = data if isinstance(data, list) else list(data.values())

total = min(308, len(products))
print(f"Processing {total} photos with fast resize + clean naming...", flush=True)

updated = []
seen_titles = {}

for i in range(total):
    num = i + 1
    sku = f"NAV{str(num).zfill(3)}"
    img_rel = products[i].get("image", "")
    disk_img = os.path.join(pub_dir, img_rel.replace("/", os.sep))

    ocr_str = ""
    if os.path.exists(disk_img):
        try:
            # Fast resize via PyMuPDF Pixmap so OCR finishes in 0.1s per image
            doc = fitz.open(disk_img)
            pix = doc[0].get_pixmap(matrix=fitz.Matrix(0.6, 0.6))
            tmp_bytes = pix.tobytes("png")
            doc.close()
            res, _ = ocr(tmp_bytes)
            if res:
                ocr_str = " ".join([r[1] for r in res])
        except Exception:
            pass

    title, brand, cat, mrp = smart_clean_name(ocr_str, num)
    if title in seen_titles:
        seen_titles[title] += 1
        title = f"{title} Pack {seen_titles[title]}"
    else:
        seen_titles[title] = 1

    sale = int(round(mrp * 0.88))
    cost = int(round(mrp * 0.75))

    updated.append({
        "id": sku,
        "barcode": f"890100000{str(num).zfill(3)}",
        "name": title,
        "brand": brand,
        "category": cat,
        "cost": cost,
        "mrp": mrp,
        "sale": sale,
        "price": sale,
        "gst": "5%",
        "stock": 25,
        "image": img_rel
    })
    print(f"[{num}/{total}] {sku} -> {title} (Sale Rs.{sale})", flush=True)

for jp in [os.path.join(store_dir, "catalog.json"), os.path.join(pub_dir, "catalog.json")]:
    with open(jp, "w", encoding="utf-8") as f:
        json.dump(updated, f, indent=2, ensure_ascii=False)

req = urllib.
